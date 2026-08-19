package jspectrumanalyzer.core;

public class FrequencyRange{
	private final int startMHz, endMHz;

	public FrequencyRange(int startMHz, int endMHz) {
		this.startMHz = startMHz;
		this.endMHz = endMHz;
	}
	public int getEndMHz() {
		return endMHz;
	}
	public int getStartMHz() {
		return startMHz;
	}

	/**
	 * Native INTERLEAVED hops (20 MHz) export [f,f+5] and [f+10,f+15].
	 * Pad ±10 MHz so the requested window is actually filled (otherwise
	 * FM 88–108 misses 93–98 and 103–108, and 97.3 sits in a hole).
	 */
	public static final int INTERLEAVED_PAD_MHZ = 10;
	public static final int MIN_MHZ = 1;
	public static final int MAX_MHZ = 7250;

	public FrequencyRange forInterleavedNativeSweep() {
		int start = Math.max(MIN_MHZ, startMHz - INTERLEAVED_PAD_MHZ);
		int end = Math.min(MAX_MHZ, endMHz + INTERLEAVED_PAD_MHZ);
		if (end <= start)
			end = Math.min(MAX_MHZ, start + 20);
		return new FrequencyRange(start, end);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FrequencyRange) {
			FrequencyRange fr	= (FrequencyRange)obj;
			if (fr.endMHz == endMHz && fr.startMHz == startMHz)
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * startMHz + endMHz;
	}

	@Override
	public String toString() {
		return startMHz + "–" + endMHz + " MHz";
	}
}