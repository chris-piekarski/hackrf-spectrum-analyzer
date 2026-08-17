package jspectrumanalyzer;

import java.math.BigDecimal;
import java.util.ArrayList;

import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.HackRFSettings;
import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

/**
 * In-memory {@link HackRFSettings} for UI tests. No native / JFrame.
 */
public class FakeHackRFSettings implements HackRFSettings {
	private final ModelValueBoolean antennaPower = new ModelValueBoolean("Ant power", false);
	private final ModelValueBoolean antennaLNA = new ModelValueBoolean("Antenna LNA +14dB", false);
	private final ModelValueInt fftBinHz = new ModelValueInt("FFT Bin [Hz]", 100000);
	private final ModelValueBoolean filterSpectrum = new ModelValueBoolean("Filter", false);
	private final ModelValue<FrequencyRange> frequency = new ModelValue<FrequencyRange>("Frequency range",
			new FrequencyRange(2400, 2500));
	private final ModelValue<FrequencyAllocationTable> frequencyAllocationTable = new ModelValue<FrequencyAllocationTable>(
			"Frequency allocation table", null);
	private final ModelValueInt gainLNA = new ModelValueInt("LNA Gain", 0, 8, 0, 40);
	private final ModelValueInt gainTotal = new ModelValueInt("Gain [dB]", 40);
	private final ModelValueInt gainVGA = new ModelValueInt("VGA Gain", 0, 2, 0, 60);
	private final ModelValueBoolean capturingPaused = new ModelValueBoolean("Capturing paused", false);
	private final ModelValueInt persistentDisplayPersTime = new ModelValueInt("Persistence time", 30, 1, 1, 60);
	private final ModelValueInt peakFallRateSecs = new ModelValueInt("Peak fall rate", 15);
	private final ModelValueBoolean persistentDisplay = new ModelValueBoolean("Persistent display", true);
	private final ModelValueInt samples = new ModelValueInt("Samples", 8192);
	private final ModelValueBoolean showPeaks = new ModelValueBoolean("Show peaks", true);
	private final ModelValueBoolean debugDisplay = new ModelValueBoolean("Debug", false);
	private final ModelValue<BigDecimal> spectrumLineThickness = new ModelValue<BigDecimal>("Spectrum line thickness",
			new BigDecimal("1"));
	private final ModelValueInt spectrumPaletteSize = new ModelValueInt("Spectrum palette size", 65);
	private final ModelValueInt spectrumPaletteStart = new ModelValueInt("Spectrum palette start", -90);
	private final ModelValueBoolean spurRemoval = new ModelValueBoolean("Spur removal", false);
	private final ModelValueBoolean waterfallVisible = new ModelValueBoolean("Waterfall visible", true);
	private final ArrayList<HackRFEventListener> listeners = new ArrayList<HackRFEventListener>();

	@Override
	public ModelValueBoolean getAntennaPowerEnable() {
		return antennaPower;
	}

	@Override
	public ModelValueBoolean getAntennaLNA() {
		return antennaLNA;
	}

	@Override
	public ModelValueInt getFFTBinHz() {
		return fftBinHz;
	}

	@Override
	public ModelValue<FrequencyRange> getFrequency() {
		return frequency;
	}

	@Override
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable() {
		return frequencyAllocationTable;
	}

	@Override
	public ModelValueInt getGain() {
		return gainTotal;
	}

	@Override
	public ModelValueInt getGainLNA() {
		return gainLNA;
	}

	@Override
	public ModelValueInt getPersistentDisplayDecayRate() {
		return persistentDisplayPersTime;
	}

	@Override
	public ModelValueBoolean isDebugDisplay() {
		return debugDisplay;
	}

	@Override
	public ModelValueInt getSamples() {
		return samples;
	}

	@Override
	public ModelValueInt getSpectrumPaletteSize() {
		return spectrumPaletteSize;
	}

	@Override
	public ModelValueBoolean isPersistentDisplayVisible() {
		return persistentDisplay;
	}

	@Override
	public ModelValueBoolean isWaterfallVisible() {
		return waterfallVisible;
	}

	@Override
	public ModelValueInt getSpectrumPaletteStart() {
		return spectrumPaletteStart;
	}

	@Override
	public ModelValueInt getPeakFallRate() {
		return peakFallRateSecs;
	}

	@Override
	public ModelValue<BigDecimal> getSpectrumLineThickness() {
		return spectrumLineThickness;
	}

	@Override
	public ModelValueInt getGainVGA() {
		return gainVGA;
	}

	@Override
	public ModelValueBoolean isCapturingPaused() {
		return capturingPaused;
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible() {
		return showPeaks;
	}

	@Override
	public ModelValueBoolean isFilterSpectrum() {
		return filterSpectrum;
	}

	@Override
	public ModelValueBoolean isSpurRemoval() {
		return spurRemoval;
	}

	@Override
	public void registerListener(HackRFEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(HackRFEventListener listener) {
		listeners.remove(listener);
	}

	public void fireHardwareStatusChanged(boolean hardwareSendingData) {
		for (HackRFEventListener listener : listeners) {
			listener.hardwareStatusChanged(hardwareSendingData);
		}
	}

	public void fireCaptureStateChanged(boolean isCapturing) {
		for (HackRFEventListener listener : listeners) {
			listener.captureStateChanged(isCapturing);
		}
	}
}
