package jspectrumanalyzer.core;

import java.math.BigDecimal;

import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

public interface HackRFSettings {
	public static abstract class HackRFEventAdapter implements HackRFEventListener {
		@Override
		public void captureStateChanged(boolean isCapturing) {

		}

		@Override
		public void hardwareStatusChanged(boolean hardwareSendingData) {

		}
	}

	public static interface HackRFEventListener {
		public void captureStateChanged(boolean isCapturing);

		public void hardwareStatusChanged(boolean hardwareSendingData);
	}

	public ModelValueBoolean getAntennaPowerEnable();

	public ModelValueBoolean getAntennaLNA();

	public ModelValueInt getFFTBinHz();

	public ModelValue<FrequencyRange> getFrequency();

	public ModelValueInt getGain();

	public ModelValueInt getGainLNA();
	
	public ModelValueInt getPersistentDisplayDecayRate();
	
	public ModelValueBoolean isDebugDisplay();

	public ModelValueInt getSamples();

	public ModelValueInt getSpectrumPaletteSize();
	
	public ModelValueBoolean isPersistentDisplayVisible();
	public ModelValueBoolean isWaterfallVisible();

	public ModelValueInt getSpectrumPaletteStart();
	
	public ModelValueInt getPeakFallRate();
	
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable();

	public ModelValue<BigDecimal> getSpectrumLineThickness();
	
	public ModelValueInt getGainVGA();

	public ModelValueBoolean isCapturingPaused();

	/** Attached radio board / serial / firmware. {@link RadioIdentity#ABSENT} when none. */
	public ModelValue<RadioIdentity> getRadioIdentity();

	/** Empty string = first radio found. */
	public ModelValue<String> getSelectedSerial();

	/** Drive 10 MHz CLKOUT (CLKIN is selected automatically when present). */
	public ModelValueBoolean getClkoutEnable();

	/** True when the native sweep is stopped and USB is released. */
	public ModelValueBoolean isRadioReleased();

	public void restartSweep();

	public void releaseRadio();

	/** USB serials currently visible to libhackrf (may be empty). */
	public java.util.List<String> listRadioSerials();

	public ModelValueBoolean isChartsPeaksVisible();

	/** Live dB-axis auto-scale. Off = fixed −100…+20. */
	public ModelValueBoolean isPowerAutoScale();

	public ModelValueBoolean isFilterSpectrum();

	public ModelValueBoolean isSpurRemoval();

	public void registerListener(HackRFEventListener listener);

	public void removeListener(HackRFEventListener listener);
}
