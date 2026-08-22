package jspectrumanalyzer.ui;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import jspectrumanalyzer.core.FrequencyAxis;
import jspectrumanalyzer.core.TvBandLayer;
import jspectrumanalyzer.core.TvStationHit;

/**
 * Live US ATSC 6 MHz occupants. Policy is {@link TvBandLayer}.
 */
public final class TvChannelOverlay
{
	private TvChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz, List<TvStationHit> stations, Integer selectedFcc)
	{
		FrequencyAxis axis = FrequencyAxis.fromArea(area, startMHz, endMHz);
		BandHeaderPainter.paint(g0, area, axis, TvBandLayer.marks(axis, stations, selectedFcc));
	}
}
