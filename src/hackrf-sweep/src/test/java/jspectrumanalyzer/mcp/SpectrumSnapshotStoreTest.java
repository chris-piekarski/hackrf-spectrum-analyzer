package jspectrumanalyzer.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.core.AnalyzerSettings;
import jspectrumanalyzer.core.DatasetSpectrum;
import jspectrumanalyzer.core.FmChannelPlan;
import jspectrumanalyzer.core.FmStationHit;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.RadioIdentity;

class SpectrumSnapshotStoreTest {

	@Test
	void latestOverwriteAndRingCap() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(3);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 108, -150f);
		ds.getSpectrumArray()[4] = -40f;
		for (int i = 0; i < 8; i++)
			store.publishSweep(SpectrumSnapshot.fromDataset(ds, i, 100, null), i);
		assertEquals(3, store.ringSize());
		assertEquals(7L, store.latest().timestampMs);
	}

	@Test
	void shouldPublishRespectsInterval() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		assertTrue(store.shouldPublish(1000));
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 88, 89, -150f);
		ds.getSpectrumArray()[0] = -50f;
		store.publishSweep(SpectrumSnapshot.fromDataset(ds, 1000, 10, null), 1000);
		assertFalse(store.shouldPublish(1050));
		assertTrue(store.shouldPublish(1000 + SpectrumSnapshotStore.MIN_PUBLISH_INTERVAL_MS));
	}

	@Test
	void concurrentReadersSeeACompleteSnapshot() throws Exception {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore(8);
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2400, 2410, -150f);
		ds.getSpectrumArray()[1] = -33f;
		AtomicInteger bad = new AtomicInteger();
		Thread writer = new Thread(() -> {
			for (int i = 0; i < 200; i++)
				store.publishSweep(SpectrumSnapshot.fromDataset(ds, i, 50, null), i);
		});
		List<Thread> readers = new ArrayList<Thread>();
		for (int r = 0; r < 4; r++)
		{
			readers.add(new Thread(() -> {
				for (int i = 0; i < 200; i++)
				{
					SpectrumSnapshot s = store.latest();
					if (s.mhz.length != s.dbm.length)
						bad.incrementAndGet();
				}
			}));
		}
		writer.start();
		for (Thread t : readers)
			t.start();
		writer.join();
		for (Thread t : readers)
			t.join();
		assertEquals(0, bad.get());
	}

	@Test
	void contextKeepsRadioAndDisplaySeparateAndHidesFmWhenZoomedOut() {
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getFrequency().setValue(new FrequencyRange(1, 7250));
		settings.getRadioIdentity().setValue(RadioIdentity.of("HackRF One", "aabbccddeeff0011", "v2026.01.3", "1.16"));
		settings.isPowerAutoScale().setValue(true);
		settings.isChartsPeaksVisible().setValue(false);
		FmStationHit hit = new FmStationHit(FmChannelPlan.nearest(97.3), -40f, 0.8f);
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		store.publishContext(settings, List.of(hit), 12.5);
		assertTrue(store.context().autoScale);
		assertFalse(store.context().peaks);
		assertTrue(store.context().sweepConfigJson().contains("\"autoScale\":true"));
		assertTrue(store.context().sweepConfigJson().contains("\"radio\""));
		assertFalse(store.context().sweepConfigJson().contains("97.3"));
		assertEquals("[]", store.context().fmStationsJson());
		assertEquals("HackRF One", store.context().board);
		assertEquals("eeff0011", store.context().serial);

		settings.getFrequency().setValue(new FrequencyRange(88, 108));
		store.publishContext(settings, List.of(hit), 12.5);
		assertTrue(store.context().fmStationsJson().contains("97.3"));
	}
}
