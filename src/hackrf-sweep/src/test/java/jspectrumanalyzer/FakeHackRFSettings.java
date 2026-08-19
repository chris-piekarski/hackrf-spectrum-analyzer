package jspectrumanalyzer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jspectrumanalyzer.core.AnalyzerSettings;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.RadioIdentity;
import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

/**
 * {@link AnalyzerSettings} plus call counters for UI tests. No native / JFrame.
 */
public class FakeHackRFSettings implements HackRFSettings {
	public final AnalyzerSettings inner = new AnalyzerSettings();
	public int restartSweepCalls;
	public int releaseRadioCalls;
	public List<String> listedSerials = new ArrayList<String>();

	public FakeHackRFSettings() {
		inner.setHardware(new AnalyzerSettings.Hardware() {
			@Override
			public void restartSweep() {
				restartSweepCalls++;
			}

			@Override
			public void releaseRadio() {
				releaseRadioCalls++;
			}

			@Override
			public List<String> listRadioSerials() {
				return listedSerials;
			}
		});
	}

	@Override
	public ModelValueBoolean getAntennaPowerEnable() {
		return inner.getAntennaPowerEnable();
	}

	@Override
	public ModelValueBoolean getAntennaLNA() {
		return inner.getAntennaLNA();
	}

	@Override
	public ModelValueInt getFFTBinHz() {
		return inner.getFFTBinHz();
	}

	@Override
	public ModelValue<FrequencyRange> getFrequency() {
		return inner.getFrequency();
	}

	@Override
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable() {
		return inner.getFrequencyAllocationTable();
	}

	@Override
	public ModelValueInt getGain() {
		return inner.getGain();
	}

	@Override
	public ModelValueInt getGainLNA() {
		return inner.getGainLNA();
	}

	@Override
	public ModelValueInt getPersistentDisplayDecayRate() {
		return inner.getPersistentDisplayDecayRate();
	}

	@Override
	public ModelValueBoolean isDebugDisplay() {
		return inner.isDebugDisplay();
	}

	@Override
	public ModelValueInt getSamples() {
		return inner.getSamples();
	}

	@Override
	public ModelValueInt getSpectrumPaletteSize() {
		return inner.getSpectrumPaletteSize();
	}

	@Override
	public ModelValueBoolean isPersistentDisplayVisible() {
		return inner.isPersistentDisplayVisible();
	}

	@Override
	public ModelValueBoolean isWaterfallVisible() {
		return inner.isWaterfallVisible();
	}

	@Override
	public ModelValueInt getSpectrumPaletteStart() {
		return inner.getSpectrumPaletteStart();
	}

	@Override
	public ModelValueInt getPeakFallRate() {
		return inner.getPeakFallRate();
	}

	@Override
	public ModelValue<BigDecimal> getSpectrumLineThickness() {
		return inner.getSpectrumLineThickness();
	}

	@Override
	public ModelValueInt getGainVGA() {
		return inner.getGainVGA();
	}

	@Override
	public ModelValueBoolean isCapturingPaused() {
		return inner.isCapturingPaused();
	}

	@Override
	public ModelValue<RadioIdentity> getRadioIdentity() {
		return inner.getRadioIdentity();
	}

	@Override
	public ModelValue<String> getSelectedSerial() {
		return inner.getSelectedSerial();
	}

	@Override
	public ModelValueBoolean getClkoutEnable() {
		return inner.getClkoutEnable();
	}

	@Override
	public ModelValueBoolean isRadioReleased() {
		return inner.isRadioReleased();
	}

	@Override
	public void restartSweep() {
		inner.restartSweep();
	}

	@Override
	public void releaseRadio() {
		inner.releaseRadio();
	}

	@Override
	public List<String> listRadioSerials() {
		return inner.listRadioSerials();
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible() {
		return inner.isChartsPeaksVisible();
	}

	@Override
	public ModelValueBoolean isPowerAutoScale() {
		return inner.isPowerAutoScale();
	}

	@Override
	public ModelValueBoolean isAutoGain() {
		return inner.isAutoGain();
	}

	@Override
	public ModelValueBoolean isFilterSpectrum() {
		return inner.isFilterSpectrum();
	}

	@Override
	public ModelValueBoolean isSpurRemoval() {
		return inner.isSpurRemoval();
	}

	@Override
	public void registerListener(HackRFEventListener listener) {
		inner.registerListener(listener);
	}

	@Override
	public void removeListener(HackRFEventListener listener) {
		inner.removeListener(listener);
	}

	public void fireHardwareStatusChanged(boolean hardwareSendingData) {
		inner.fireHardwareStatusChanged(hardwareSendingData);
	}

	public void fireCaptureStateChanged(boolean isCapturing) {
		inner.fireCaptureStateChanged(isCapturing);
	}
}
