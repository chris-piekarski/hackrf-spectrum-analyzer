#ifndef ATSC_SHIM_MATH_H
#define ATSC_SHIM_MATH_H
#include <cmath>
#include <complex>
#ifndef GR_M_PI
#define GR_M_PI 3.14159265358979323846
#endif
namespace gr {
using gr_complex = std::complex<float>;
inline void fast_cc_multiply(gr_complex& out, const gr_complex& a, const gr_complex& b)
{
	out = a * b;
}
inline float fast_atan2f(float y, float x) { return std::atan2(y, x); }
} // namespace gr
#endif
