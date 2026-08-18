package hackrfsweep;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
/**
 * JNA bindings for {@code libhackrf-sweep}. Hand-maintained — do not regenerate
 * with JNAerator. Keep in sync with {@code src-c/hackrf_sweep.h}.
 */
public class HackrfSweepLibrary implements Library {
	public interface hackrf_sweep_lib_start__fft_power_callback_callback extends Callback {
		void apply(byte full_sweep_done, int bins, DoubleByReference freqStart, float fft_bin_Hz, FloatByReference powerdBm);
	};
	/**
	 * only ONE instance running is supported at any time<br>
	 * Original signature : <code>int hackrf_sweep_lib_start(hackrf_sweep_lib_start__fft_power_callback_callback*, uint32_t, uint32_t, uint32_t, uint32_t, unsigned int, unsigned int, unsigned int, unsigned int)</code>
	 */
	public static native int hackrf_sweep_lib_start(HackrfSweepLibrary.hackrf_sweep_lib_start__fft_power_callback_callback _fft_power_callback, int freq_min, int freq_max, int fft_bin_width, int num_samples, int lna_gain, int vga_gain, int _antennaPowerEnable, int _enableAntennaLNA);
	/** Original signature : <code>void hackrf_sweep_lib_stop()</code> */
	public static native void hackrf_sweep_lib_stop();
}
