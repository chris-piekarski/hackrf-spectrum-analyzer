package jspectrumanalyzer.mcp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import jspectrumanalyzer.core.AnalyzerSettings;
import jspectrumanalyzer.core.FmBandLayer;
import jspectrumanalyzer.core.FmStationHit;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.RadioIdentity;
import jspectrumanalyzer.core.SweepConfig;
import jspectrumanalyzer.mcp.SpectrumSnapshot.FmHit;
import jspectrumanalyzer.mcp.SpectrumSnapshot.RadioContext;

/**
 * Latest sweep plus a short summary ring. Writers are the processing
 * hook; MCP tools only read.
 */
public final class SpectrumSnapshotStore
{
	public static final int DEFAULT_RING = 32;
	public static final long MIN_PUBLISH_INTERVAL_MS = 100L;

	private final Object lock = new Object();
	private final int ringCap;
	private final ArrayDeque<SpectrumSnapshot> ring;
	private SpectrumSnapshot latest = SpectrumSnapshot.empty(0L);
	private RadioContext context;
	private long lastPublishMs;

	public SpectrumSnapshotStore()
	{
		this(DEFAULT_RING);
	}

	public SpectrumSnapshotStore(int ringCap)
	{
		this.ringCap = Math.max(1, ringCap);
		this.ring = new ArrayDeque<SpectrumSnapshot>(this.ringCap);
		this.context = new RadioContext(false, false, 0, null, null, null, null, false, 0, 0, 0, 0, 0, 0, false, false,
				false, "", false, false, List.of());
	}

	public boolean shouldPublish(long nowMs)
	{
		synchronized (lock)
		{
			return nowMs - lastPublishMs >= MIN_PUBLISH_INTERVAL_MS;
		}
	}

	public void publishSweep(SpectrumSnapshot snap, long nowMs)
	{
		if (snap == null)
			return;
		synchronized (lock)
		{
			latest = snap;
			lastPublishMs = nowMs;
			if (ring.size() >= ringCap)
				ring.removeFirst();
			ring.addLast(snap);
		}
	}

	public void publishContext(HackRFSettings settings, List<FmStationHit> stations, double sweepsPerSec)
	{
		if (settings == null)
			return;
		RadioIdentity id = settings.getRadioIdentity() != null ? settings.getRadioIdentity().getValue()
				: RadioIdentity.ABSENT;
		if (id == null)
			id = RadioIdentity.ABSENT;
		SweepConfig radio = SweepConfig.from(settings);
		List<FmHit> fm = new ArrayList<FmHit>();
		if (stations != null && FmBandLayer.tagsReadable(radio.startMHz, radio.endMHz))
		{
			for (FmStationHit hit : stations)
			{
				if (hit == null || hit.channel == null)
					continue;
				fm.add(new FmHit(hit.label(), (float) hit.channel.centerMHz(), hit.powerDbm, hit.confidence));
			}
		}
		boolean paused = settings.isCapturingPaused() != null && Boolean.TRUE.equals(settings.isCapturingPaused().getValue());
		boolean released = settings.isRadioReleased() != null && Boolean.TRUE.equals(settings.isRadioReleased().getValue());
		boolean peaks = settings.isChartsPeaksVisible() != null
				&& Boolean.TRUE.equals(settings.isChartsPeaksVisible().getValue());
		boolean auto = settings.isPowerAutoScale() != null && Boolean.TRUE.equals(settings.isPowerAutoScale().getValue());
		RadioContext next = new RadioContext(paused, released, sweepsPerSec, id.displayBoard(), id.shortSerial(),
				id.displayFirmware(), id.usbApi, id.present, radio.startMHz, radio.endMHz, radio.fftBinHz, radio.samples,
				radio.lnaGain, radio.vgaGain, radio.antennaPower, radio.antennaLna, radio.clkout, radio.serial, peaks,
				auto, fm);
		synchronized (lock)
		{
			context = next;
		}
	}

	/** Convenience for tests that already have {@link AnalyzerSettings}. */
	public void publishContext(AnalyzerSettings settings, List<FmStationHit> stations, double sweepsPerSec)
	{
		publishContext((HackRFSettings) settings, stations, sweepsPerSec);
	}

	public SpectrumSnapshot latest()
	{
		synchronized (lock)
		{
			return latest;
		}
	}

	public RadioContext context()
	{
		synchronized (lock)
		{
			return context;
		}
	}

	public int ringSize()
	{
		synchronized (lock)
		{
			return ring.size();
		}
	}
}
