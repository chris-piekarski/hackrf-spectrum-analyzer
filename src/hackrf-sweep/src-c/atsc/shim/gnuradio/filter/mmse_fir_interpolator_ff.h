#ifndef ATSC_SHIM_MMSE_INTERP_H
#define ATSC_SHIM_MMSE_INTERP_H
namespace gr {
namespace filter {
class mmse_fir_interpolator_ff
{
public:
	unsigned ntaps() const { return 8; }
	float interpolate(const float* input, float mu) const
	{
		if (mu < 0)
			mu = 0;
		if (mu > 1)
			mu = 1;
		float y0 = input[2];
		float y1 = input[3];
		float y2 = input[4];
		float y3 = input[5];
		float a = -0.5f * y0 + 1.5f * y1 - 1.5f * y2 + 0.5f * y3;
		float b = y0 - 2.5f * y1 + 2.f * y2 - 0.5f * y3;
		float c = -0.5f * y0 + 0.5f * y2;
		return ((a * mu + b) * mu + c) * mu + y1;
	}
};
} // namespace filter
} // namespace gr
#endif
