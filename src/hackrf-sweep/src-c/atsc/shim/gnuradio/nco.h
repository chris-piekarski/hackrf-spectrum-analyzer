#ifndef ATSC_SHIM_NCO_H
#define ATSC_SHIM_NCO_H
#include <cmath>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
namespace gr {
template <typename FREQ, typename PHASE>
class nco
{
	double d_phase = 0;
	double d_freq = 0;

public:
	void set_freq(double f) { d_freq = f; }
	void set_phase(double p) { d_phase = p; }
	void step()
	{
		d_phase += d_freq;
		if (d_phase > 2 * M_PI)
			d_phase -= 2 * M_PI;
		else if (d_phase < -2 * M_PI)
			d_phase += 2 * M_PI;
	}
	void sincos(float* s, float* c)
	{
		*s = std::sin((float) d_phase);
		*c = std::cos((float) d_phase);
	}
	void adjust_phase(float x) { d_phase += x; }
	void adjust_freq(float x) { d_freq += x; }
};
} // namespace gr
#endif
