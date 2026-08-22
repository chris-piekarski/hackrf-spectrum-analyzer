/*
 * Offline lock check: synthetic 8VSB-like IF → atsc_rx_process.
 * Not a full ATSC modulator (no trellis/RS), so MPEG packets may stay 0;
 * segment outputs from atsc_sync should be > 0 if FPLL+timing work.
 */
#include "atsc_rx.h"
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <vector>

static constexpr double PI = 3.14159265358979323846;
static constexpr double SYM = 4.5e6 / 286 * 684;
static constexpr double PILOT = -3e6 + 0.309e6;

int main()
{
	const double fs = 12e6;
	const int seconds = 3;
	const int n = (int) (fs * seconds);
	void* rx = atsc_rx_create(fs);
	if (!rx)
	{
		fprintf(stderr, "create failed\n");
		return 1;
	}
	std::vector<int8_t> iq(4096);
	std::vector<uint8_t> ts(188 * 64);
	float snr = 0;
	int wrote = 0;
	int off = 0;
	for (int i = 0; i < n; i++)
	{
		double t = i / fs;
		int si = (int) (t * SYM);
		int pos = si % 832;
		float lvl;
		if (pos == 0)
			lvl = 5.f;
		else if (pos == 1 || pos == 2)
			lvl = -5.f;
		else if (pos == 3)
			lvl = 5.f;
		else
			lvl = ((si >> 2) & 1) ? 3.f : -3.f;
		lvl += 1.25f;
		double w = 2.0 * PI * PILOT * t;
		float I = lvl * (float) std::cos(w);
		float Q = lvl * (float) std::sin(w);
		int i8 = (int) std::lround(I * 12.f);
		int q8 = (int) std::lround(Q * 12.f);
		if (i8 > 127)
			i8 = 127;
		if (i8 < -127)
			i8 = -127;
		if (q8 > 127)
			q8 = 127;
		if (q8 < -127)
			q8 = -127;
		iq[off++] = (int8_t) i8;
		iq[off++] = (int8_t) q8;
		if (off == (int) iq.size())
		{
			wrote += atsc_rx_process(rx, iq.data(), off, ts.data(), (int) ts.size(), &snr);
			off = 0;
		}
	}
	if (off)
		wrote += atsc_rx_process(rx, iq.data(), off, ts.data(), (int) ts.size(), &snr);
	fprintf(stderr, "selftest: packets_from_process=%d locked=%d packets=%d\n", wrote,
			atsc_rx_locked(rx), atsc_rx_packets(rx));
	atsc_rx_destroy(rx);
	return 0;
}
