package jspectrumanalyzer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class AnalyzerSettingsTest {

	@Test
	void defaultsMatchTheOperatorApp() {
		AnalyzerSettings s = new AnalyzerSettings();
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_START_MHZ, s.getFrequency().getValue().getStartMHz());
		assertEquals(WifiChannelPlan.WIFI_24_VIEW_END_MHZ, s.getFrequency().getValue().getEndMHz());
		assertEquals(100000, s.getFFTBinHz().getValue());
		assertEquals(8192, s.getSamples().getValue());
		assertTrue(s.isChartsPeaksVisible().getValue());
		assertTrue(s.isPowerAutoScale().getValue());
		assertTrue(s.isAutoGain().getValue());
		assertTrue(s.isPersistentDisplayVisible().getValue());
		assertTrue(s.isWaterfallVisible().getValue());
		assertEquals(RadioIdentity.ABSENT, s.getRadioIdentity().getValue());
	}

	@Test
	void radioVersusDisplaySettings() {
		AnalyzerSettings s = new AnalyzerSettings();
		assertTrue(s.isRadioSetting(s.getFrequency()));
		assertTrue(s.isRadioSetting(s.getFFTBinHz()));
		assertTrue(s.isRadioSetting(s.getGainLNA()));
		assertTrue(s.isRadioSetting(s.getClkoutEnable()));
		assertFalse(s.isRadioSetting(s.isChartsPeaksVisible()));
		assertFalse(s.isRadioSetting(s.isPowerAutoScale()));
		assertFalse(s.isRadioSetting(s.isAutoGain()));
		assertFalse(s.isRadioSetting(s.isPersistentDisplayVisible()));
		assertFalse(s.isRadioSetting(s.getSpectrumPaletteStart()));
	}

	@Test
	void hardwareHooksRestartAndReleaseWithoutOwningUsb() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger restarts = new AtomicInteger();
		AtomicInteger releases = new AtomicInteger();
		List<String> serials = new ArrayList<String>();
		serials.add("aabbccdd");
		s.setHardware(new AnalyzerSettings.Hardware()
		{
			@Override
			public void restartSweep()
			{
				restarts.incrementAndGet();
			}

			@Override
			public void releaseRadio()
			{
				releases.incrementAndGet();
			}

			@Override
			public List<String> listRadioSerials()
			{
				return serials;
			}
		});
		s.releaseRadio();
		assertTrue(s.isRadioReleased().getValue());
		assertEquals(1, releases.get());
		s.restartSweep();
		assertFalse(s.isRadioReleased().getValue());
		assertEquals(1, restarts.get());
		assertEquals(List.of("aabbccdd"), s.listRadioSerials());
	}

	@Test
	void hardwareEventsReachRegisteredListeners() {
		AnalyzerSettings s = new AnalyzerSettings();
		AtomicInteger hw = new AtomicInteger();
		AtomicInteger cap = new AtomicInteger();
		s.registerListener(new HackRFSettings.HackRFEventAdapter()
		{
			@Override
			public void hardwareStatusChanged(boolean hardwareSendingData)
			{
				if (hardwareSendingData)
					hw.incrementAndGet();
			}

			@Override
			public void captureStateChanged(boolean isCapturing)
			{
				if (isCapturing)
					cap.incrementAndGet();
			}
		});
		s.fireHardwareStatusChanged(true);
		s.fireCaptureStateChanged(true);
		assertEquals(1, hw.get());
		assertEquals(1, cap.get());
	}

	@Test
	void sweepConfigIgnoresDisplayOnlyChanges() {
		AnalyzerSettings s = new AnalyzerSettings();
		SweepConfig a = SweepConfig.from(s);
		s.isPowerAutoScale().setValue(true);
		s.isChartsPeaksVisible().setValue(false);
		assertEquals(a, SweepConfig.from(s));
		s.getFFTBinHz().setValue(50000);
		assertNotEquals(a, SweepConfig.from(s));
	}
}
