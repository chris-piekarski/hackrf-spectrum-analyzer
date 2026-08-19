package jspectrumanalyzer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Live FM station hits as {@link BandMark}s. Hidden when the view is
 * wider than a single FM-scale window.
 */
public final class FmBandLayer
{
	public static final double MAX_VIEW_SPAN_MHZ = 30;

	private FmBandLayer()
	{
	}

	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz)
			return false;
		if (endMHz - startMHz > MAX_VIEW_SPAN_MHZ)
			return false;
		return Math.min(endMHz, FmChannelPlan.VIEW_END_MHZ) > Math.max(startMHz, FmChannelPlan.VIEW_START_MHZ);
	}

	public static boolean tagsReadable(FrequencyAxis axis)
	{
		return axis != null && axis.usable() && tagsReadable(axis.startMHz, axis.endMHz);
	}

	public static List<BandMark> marks(FrequencyAxis axis, List<FmStationHit> stations)
	{
		if (!tagsReadable(axis) || stations == null || stations.isEmpty())
			return List.of();
		List<FmStationHit> hits = new ArrayList<>();
		for (FmStationHit hit : stations)
		{
			if (hit == null || hit.channel == null)
				continue;
			if (!hit.channel.occupancyOverlaps(axis.startMHz, axis.endMHz))
				continue;
			hits.add(hit);
		}
		hits.sort((a, b) -> {
			int byConf = Float.compare(b.confidence, a.confidence);
			return byConf != 0 ? byConf : Float.compare(b.powerDbm, a.powerDbm);
		});
		List<BandMark> out = new ArrayList<>();
		for (FmStationHit hit : hits)
		{
			FmChannel ch = hit.channel;
			out.add(new BandMark(ch.lowMHz(), ch.highMHz(), ch.centerMHz(), hit.label(), BandMark.Style.PRIMARY,
					false, false, ch.centerIn(axis.startMHz, axis.endMHz), BandMark.LabelFit.DROP_IF_OVERLAP,
					hit.confidence));
		}
		return out;
	}
}
