#ifndef ATSC_SHIM_VOLK_H
#define ATSC_SHIM_VOLK_H
#include <algorithm>
inline int volk_get_alignment() { return 16; }
inline void volk_32f_x2_dot_prod_32f(float* out, const float* a, const float* b, unsigned n)
{
	float s = 0;
	for (unsigned i = 0; i < n; i++)
		s += a[i] * b[i];
	*out = s;
}
inline void volk_32f_s32f_multiply_32f(float* o, const float* a, float s, unsigned n)
{
	for (unsigned i = 0; i < n; i++)
		o[i] = a[i] * s;
}
inline void volk_32f_x2_subtract_32f(float* o, const float* a, const float* b, unsigned n)
{
	for (unsigned i = 0; i < n; i++)
		o[i] = a[i] - b[i];
}
#endif
