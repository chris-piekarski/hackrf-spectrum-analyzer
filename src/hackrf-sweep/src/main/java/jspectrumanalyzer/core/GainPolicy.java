package jspectrumanalyzer.core;

/**
 * Splits a requested total gain into HackRF LNA (step 8, 0–40) and VGA
 * (step 2, only after LNA is already 40).
 */
public final class GainPolicy {
	public static final int LNA_MAX = 40;
	public static final int LNA_STEP = 8;

	private GainPolicy() {
	}

	public static int lnaGain(int totalGain) {
		int lnaGain = totalGain / LNA_STEP * LNA_STEP;
		if (lnaGain > LNA_MAX)
			lnaGain = LNA_MAX;
		if (lnaGain < 0)
			lnaGain = 0;
		return lnaGain;
	}

	public static int vgaGain(int totalGain) {
		int lnaGain = lnaGain(totalGain);
		if (lnaGain != LNA_MAX)
			return 0;
		return (totalGain - lnaGain) & ~1;
	}
}
