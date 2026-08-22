/*
 * Standalone ATSC 1.0 receive pipeline. DSP inner loops are GNU Radio
 * gr-dtv (GPL-3.0-or-later), driven without the GR runtime.
 *
 * Front-end matches gr-dtv atsc_rx.py: RRC (vestigial ~6 MHz) → FPLL →
 * dc_blocker_ff(4096) → agc_ff(1e-5, 4.0) → sync…derandomizer.
 * Field-sync segments train the equalizer and are not fed to trellis.
 * IQ must be processed in realtime or RS never locks.
 */
#include "atsc_rx.h"

#include "atsc_deinterleaver_impl.h"
#include "atsc_derandomizer_impl.h"
#include "atsc_equalizer_impl.h"
#include "atsc_fpll_impl.h"
#include "atsc_fs_checker_impl.h"
#include "atsc_rs_decoder_impl.h"
#include "atsc_sync_impl.h"
#include "atsc_viterbi_decoder_impl.h"
#include <gnuradio/dtv/atsc_consts.h>
#include <gnuradio/math.h>

#include <algorithm>
#include <cmath>
#include <complex>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <vector>

using gr::dtv::ATSC_DATA_SEGMENT_LENGTH;
using gr::dtv::ATSC_MPEG_PKT_LENGTH;
using gr::dtv::ATSC_MPEG_RS_ENCODED_LENGTH;
using gr::dtv::ATSC_SYMBOL_RATE;
using gr::dtv::plinfo;

static constexpr double SPS = 1.1;
static constexpr int LOCK_WIN = 64;
static constexpr int DC_D = 4096;
static constexpr float AGC_RATE = 1e-5f;
static constexpr float AGC_REF = 4.f;
static constexpr double RRC_ALPHA = 0.1152;
static constexpr int RRC_SYMS = 8;

static std::vector<float> rrc_taps(double fs, double baud, double alpha, int ntaps)
{
	if ((ntaps & 1) == 0)
		ntaps++;
	std::vector<float> t(ntaps);
	const double spb = fs / baud;
	double scale = 0;
	const double mid = ntaps / 2.0;
	for (int i = 0; i < ntaps; i++) {
		const double xindx = i - mid;
		const double x1 = GR_M_PI * xindx / spb;
		const double x2 = 4 * alpha * xindx / spb;
		if (xindx == 0) {
			t[i] = (float) (1 + alpha * (4 / GR_M_PI - 1));
		} else if (std::fabs(std::fabs(x1) - GR_M_PI / (4 * alpha)) < 1e-8) {
			double v = (1 + 2 / GR_M_PI) * std::sin(GR_M_PI / (4 * alpha)) +
				   (1 - 2 / GR_M_PI) * std::cos(GR_M_PI / (4 * alpha));
			t[i] = (float) (alpha / std::sqrt(2.0) * v);
		} else {
			const double num = std::cos((1 + alpha) * x1) +
					   std::sin((1 - alpha) * x1) / (4 * alpha * xindx / spb);
			const double den = 1 - x2 * x2;
			t[i] = (float) (4 * alpha / GR_M_PI * num / den);
		}
		scale += t[i];
	}
	if (std::fabs(scale) < 1e-12)
		scale = 1;
	for (float& v : t)
		v = (float) (v / scale);
	return t;
}

struct AtscRx
{
	double in_rate;
	double out_rate;
	double acc = 0;
	float agc_ff = 1.f;
	std::vector<float> rrc;
	std::vector<std::complex<float>> rrc_hist;
	int rrc_i = 0;
	std::vector<float> dc_d;
	int dc_i = 0;
	double dc_acc = 0;
	gr::dtv::atsc_fpll_impl fpll;
	gr::dtv::atsc_sync_impl sync;
	gr::dtv::atsc_fs_checker_impl fsc;
	gr::dtv::atsc_equalizer_impl equ;
	gr::dtv::atsc_viterbi_decoder_impl vit;
	gr::dtv::atsc_deinterleaver_impl dei;
	gr::dtv::atsc_rs_decoder_impl rsd;
	gr::dtv::atsc_derandomizer_impl der;
	std::vector<std::complex<float>> in_iq;
	size_t in_off = 0;
	std::vector<std::complex<float>> rs;
	std::vector<float> fpll_out;
	std::vector<float> bb;
	size_t bb_off = 0;
	std::vector<float> segs;
	size_t segs_off = 0;
	std::vector<plinfo> segs_pl;
	int last_segno = -2;
	int locked = 0;
	int packets = 0;
	int good = 0;
	int segs_out = 0;
	int fsc_out = 0;
	int good_at_log = 0;
	int pid_pat = 0;
	int pid_psip = 0;
	int pid_null = 0;
	int pid_other = 0;
	uint8_t lock_win[LOCK_WIN]{};
	int lock_i = 0;
	int lock_n = 0;
	int lock_good = 0;
	long log_in = 0;
	double sum_iq2 = 0;
	double sum_bb2 = 0;
	long n_iq = 0;
	long n_bb = 0;
	int lvl[4]{};
	int invert = 0;

	explicit AtscRx(double rate)
		: in_rate(rate > 1e6 ? rate : 20e6), out_rate(ATSC_SYMBOL_RATE * SPS),
		  fpll((float) (ATSC_SYMBOL_RATE * SPS)), sync((float) (ATSC_SYMBOL_RATE * SPS))
	{
		const double baud = ATSC_SYMBOL_RATE / 2.0;
		int ntaps = (int) ((2 * RRC_SYMS + 1) * (out_rate / baud));
		rrc = rrc_taps(out_rate, baud, RRC_ALPHA, ntaps);
		rrc_hist.assign(rrc.size(), { 0.f, 0.f });
		dc_d.assign(DC_D, 0.f);
		bb.reserve(1 << 16);
		in_iq.reserve(1 << 16);
		rs.reserve(1 << 16);
	}
};

extern "C" void atsc_rx_set_invert(void* rx, int invert)
{
	if (rx)
		static_cast<AtscRx*>(rx)->invert = invert ? 1 : 0;
}

extern "C" void* atsc_rx_create(double input_rate_hz)
{
	try
	{
		return new AtscRx(input_rate_hz);
	}
	catch (...)
	{
		return nullptr;
	}
}

extern "C" void atsc_rx_destroy(void* rx)
{
	delete static_cast<AtscRx*>(rx);
}

extern "C" int atsc_rx_locked(void* rx)
{
	return rx ? static_cast<AtscRx*>(rx)->locked : 0;
}

extern "C" int atsc_rx_packets(void* rx)
{
	return rx ? static_cast<AtscRx*>(rx)->packets : 0;
}

extern "C" int atsc_rx_bad_packets(void* rx)
{
	return rx ? static_cast<AtscRx*>(rx)->rsd.num_bad_packets() : 0;
}

static void compact(std::vector<std::complex<float>>& v, size_t& off)
{
	if (off == 0)
		return;
	if (off >= v.size())
	{
		v.clear();
		off = 0;
		return;
	}
	v.erase(v.begin(), v.begin() + (long) off);
	off = 0;
}

static void bb_consume(AtscRx* r, int n)
{
	r->bb_off += (size_t) n;
	if (r->bb_off > 16384 && r->bb_off * 2 >= r->bb.size())
	{
		r->bb.erase(r->bb.begin(), r->bb.begin() + (long) r->bb_off);
		r->bb_off = 0;
	}
}

static void drop_pending_segments(AtscRx* r)
{
	r->segs.clear();
	r->segs_off = 0;
	r->segs_pl.clear();
}

static int pump_segments(AtscRx* r, uint8_t* ts_out, int ts_cap)
{
	int written = 0;
	while ((int) (r->bb.size() - r->bb_off) > 2500)
	{
		int nin = (int) (r->bb.size() - r->bb_off);
		float seg[ATSC_DATA_SEGMENT_LENGTH];
		gr::gr_vector_int ninput{ nin };
		gr::gr_vector_const_void_star ins{ r->bb.data() + r->bb_off };
		gr::gr_vector_void_star outs{ seg };
		r->sync.d_nconsumed = 0;
		int nseg = r->sync.general_work(1, ninput, ins, outs);
		int cons = r->sync.d_nconsumed > 0 ? r->sync.d_nconsumed : 832;
		if (cons > nin)
			cons = nin;
		if (cons < 1)
			break;
		bb_consume(r, cons);
		if (nseg <= 0)
			continue;
		r->segs_out += nseg;
		float fsc_out[ATSC_DATA_SEGMENT_LENGTH];
		plinfo pli;
		gr::gr_vector_int n1{ 1 };
		gr::gr_vector_const_void_star ins1{ seg };
		gr::gr_vector_void_star outs1{ fsc_out, &pli };
		int nf = r->fsc.general_work(1, n1, ins1, outs1);
		if (nf <= 0)
			continue;
		r->fsc_out += nf;
		float eq_out[ATSC_DATA_SEGMENT_LENGTH];
		plinfo eq_pl;
		gr::gr_vector_const_void_star ins2{ fsc_out, &pli };
		gr::gr_vector_void_star outs2{ eq_out, &eq_pl };
		int ne = r->equ.general_work(1, n1, ins2, outs2);
		if (ne <= 0)
			continue;
		int s = (int16_t) eq_pl.segno();
		/* Field sync trains the equalizer; it is not a trellis data segment.
		 * GNU Radio does not reset Viterbi/deinterleaver on field boundaries. */
		if (s < 0)
		{
			r->last_segno = s;
			continue;
		}
		if (r->last_segno >= 0 && s != r->last_segno + 1)
			drop_pending_segments(r);
		r->last_segno = s;
		if ((s % 12) != 0 && r->segs_pl.empty())
			continue;
		r->segs.insert(r->segs.end(), eq_out, eq_out + ATSC_DATA_SEGMENT_LENGTH);
		r->segs_pl.push_back(eq_pl);
		while ((int) r->segs_pl.size() >= 12)
		{
			int start = (int16_t) r->segs_pl[0].segno();
			if (start < 0 || (start % 12) != 0)
			{
				r->segs_off += ATSC_DATA_SEGMENT_LENGTH;
				r->segs_pl.erase(r->segs_pl.begin());
				if (r->segs_off > 12 * ATSC_DATA_SEGMENT_LENGTH)
				{
					r->segs.erase(r->segs.begin(), r->segs.begin() + (long) r->segs_off);
					r->segs_off = 0;
				}
				continue;
			}
			unsigned char vit_out[12 * ATSC_MPEG_RS_ENCODED_LENGTH];
			plinfo vit_pl[12];
			gr::gr_vector_const_void_star vins{ r->segs.data() + r->segs_off, r->segs_pl.data() };
			gr::gr_vector_void_star vouts{ vit_out, vit_pl };
			int nv = r->vit.work(12, vins, vouts);
			r->segs_off += (size_t) (12 * ATSC_DATA_SEGMENT_LENGTH);
			r->segs_pl.erase(r->segs_pl.begin(), r->segs_pl.begin() + 12);
			if (r->segs_off > 12 * ATSC_DATA_SEGMENT_LENGTH * 4)
			{
				r->segs.erase(r->segs.begin(), r->segs.begin() + (long) r->segs_off);
				r->segs_off = 0;
			}
			if (nv <= 0)
				continue;
			unsigned char dei_out[12 * ATSC_MPEG_RS_ENCODED_LENGTH];
			plinfo dei_pl[12];
			gr::gr_vector_const_void_star dins{ vit_out, vit_pl };
			gr::gr_vector_void_star douts{ dei_out, dei_pl };
			r->dei.work(12, dins, douts);
			unsigned char rs_out[12 * ATSC_MPEG_PKT_LENGTH];
			plinfo rs_pl[12];
			gr::gr_vector_const_void_star rins{ dei_out, dei_pl };
			gr::gr_vector_void_star routs{ rs_out, rs_pl };
			r->rsd.work(12, rins, routs);
			unsigned char der_out[12 * ATSC_MPEG_PKT_LENGTH];
			gr::gr_vector_const_void_star erins{ rs_out, rs_pl };
			gr::gr_vector_void_star erouts{ der_out };
			r->der.work(12, erins, erouts);
			for (int i = 0; i < 12; i++)
			{
				r->packets++;
				int ok = !rs_pl[i].transport_error_p();
				if (r->lock_n == LOCK_WIN)
					r->lock_good -= r->lock_win[r->lock_i];
				else
					r->lock_n++;
				r->lock_win[r->lock_i] = (uint8_t) ok;
				r->lock_good += ok;
				r->lock_i = (r->lock_i + 1) % LOCK_WIN;
				r->locked = (r->lock_n == LOCK_WIN && r->lock_good >= 16);
				if (!ok)
					continue;
				r->good++;
				{
					const unsigned char* p = der_out + i * ATSC_MPEG_PKT_LENGTH;
					int pid = ((p[1] & 0x1f) << 8) | p[2];
					if (pid == 0)
						r->pid_pat++;
					else if (pid == 0x1ffb)
						r->pid_psip++;
					else if (pid == 0x1fff)
						r->pid_null++;
					else
						r->pid_other++;
					if (r->good <= 4)
						fprintf(stderr, "atsc ts #%d %02x %02x %02x %02x pid=%d\n", r->good, p[0],
								p[1], p[2], p[3], pid);
				}
				if (written + ATSC_MPEG_PKT_LENGTH > ts_cap)
					return written;
				memcpy(ts_out + written, der_out + i * ATSC_MPEG_PKT_LENGTH, ATSC_MPEG_PKT_LENGTH);
				written += ATSC_MPEG_PKT_LENGTH;
			}
		}
	}
	if (r->bb.size() - r->bb_off > 1 << 20)
		bb_consume(r, (int) ((r->bb.size() - r->bb_off) / 2));
	return written;
}

extern "C" int atsc_rx_process(void* rx, const int8_t* iq, int nbytes, uint8_t* ts_out, int ts_cap,
		float* snr_db)
{
	if (!rx || !iq || nbytes < 2 || !ts_out || ts_cap < 188)
		return 0;
	AtscRx* r = static_cast<AtscRx*>(rx);
	int n = nbytes & ~1;
	int npairs = n / 2;
	r->in_iq.reserve(r->in_iq.size() + (size_t) npairs);
	for (int i = 0; i < npairs; i++)
	{
		float qi = iq[2 * i + 1] / 128.f;
		if (r->invert)
			qi = -qi;
		std::complex<float> c(iq[2 * i] / 128.f, qi);
		r->sum_iq2 += (double) (c.real() * c.real() + c.imag() * c.imag());
		r->n_iq++;
		r->in_iq.push_back(c);
	}

	double step = r->in_rate / r->out_rate;
	r->rs.clear();
	double acc = r->acc + (double) r->in_off;
	int nsrc = (int) r->in_iq.size();
	while (acc + 1 < nsrc)
	{
		int i0 = (int) acc;
		float mu = (float) (acc - i0);
		r->rs.push_back((1.f - mu) * r->in_iq[i0] + mu * r->in_iq[i0 + 1]);
		acc += step;
	}
	r->in_off = (size_t) acc;
	r->acc = acc - (double) r->in_off;
	if (r->in_off > 8192)
		compact(r->in_iq, r->in_off);

	if (r->rs.empty())
		return 0;

	const int nt = (int) r->rrc.size();
	for (size_t i = 0; i < r->rs.size(); i++)
	{
		r->rrc_hist[r->rrc_i] = r->rs[i];
		std::complex<float> y(0.f, 0.f);
		int hi = r->rrc_i;
		for (int k = 0; k < nt; k++)
		{
			y += r->rrc_hist[hi] * r->rrc[k];
			if (--hi < 0)
				hi = nt - 1;
		}
		r->rrc_i++;
		if (r->rrc_i >= nt)
			r->rrc_i = 0;
		r->rs[i] = y;
	}

	r->fpll_out.resize(r->rs.size());
	gr::gr_vector_const_void_star fins{ r->rs.data() };
	gr::gr_vector_void_star fouts{ r->fpll_out.data() };
	r->fpll.work((int) r->rs.size(), fins, fouts);
	for (float s : r->fpll_out)
	{
		/* GNU Radio atsc_rx: dc_blocker_ff(4096) then agc_ff(1e-5, 4.0). */
		float old = r->dc_d[r->dc_i];
		r->dc_d[r->dc_i] = s;
		r->dc_i = (r->dc_i + 1) % DC_D;
		r->dc_acc += (double) s - (double) old;
		float ac = s - (float) (r->dc_acc / DC_D);
		float out = ac * r->agc_ff;
		r->agc_ff += AGC_RATE * (AGC_REF - std::fabs(out));
		if (r->agc_ff < 1e-4f)
			r->agc_ff = 1e-4f;
		if (r->agc_ff > 400.f)
			r->agc_ff = 400.f;
		r->bb.push_back(out);
		r->sum_bb2 += (double) (out * out);
		r->n_bb++;
		float a = std::fabs(out);
		int b = (int) (a * 0.5f);
		if (b < 0)
			b = 0;
		if (b > 3)
			b = 3;
		r->lvl[b]++;
	}
	int w = pump_segments(r, ts_out, ts_cap);
	r->log_in += npairs;
	if (r->log_in > (long) r->in_rate * 2)
	{
		int dgood = r->good - r->good_at_log;
		r->good_at_log = r->good;
		float snr = 0;
		if (r->lock_n > 8)
			snr = 10.f * std::log10(std::max(1e-3f, (float) r->lock_good / (float) r->lock_n));
		float rms_iq = r->n_iq ? (float) std::sqrt(r->sum_iq2 / r->n_iq) : 0;
		float rms_bb = r->n_bb ? (float) std::sqrt(r->sum_bb2 / r->n_bb) : 0;
		fprintf(stderr,
				"atsc_rx: locked=%d packets=%d good=%d (+%d) segs=%d fsc=%d snr=%.1f rms_iq=%.4f rms_bb=%.2f bb=%zu pat=%d psip=%d null=%d other=%d lvl=%d/%d/%d/%d\n",
				r->locked, r->packets, r->good, dgood, r->segs_out, r->fsc_out, snr, rms_iq, rms_bb,
				r->bb.size() - r->bb_off, r->pid_pat, r->pid_psip, r->pid_null, r->pid_other,
				r->lvl[0], r->lvl[1], r->lvl[2], r->lvl[3]);
		r->log_in = 0;
		r->sum_iq2 = 0;
		r->sum_bb2 = 0;
		r->n_iq = 0;
		r->n_bb = 0;
		r->pid_pat = 0;
		r->pid_psip = 0;
		r->pid_null = 0;
		r->pid_other = 0;
		r->lvl[0] = r->lvl[1] = r->lvl[2] = r->lvl[3] = 0;
	}
	if (snr_db)
	{
		if (r->lock_n > 8)
			*snr_db = 10.f * std::log10(std::max(1e-3f, (float) r->lock_good / (float) r->lock_n));
		else
			*snr_db = 0;
	}
	return w;
}
