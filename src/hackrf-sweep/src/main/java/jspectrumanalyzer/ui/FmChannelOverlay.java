package jspectrumanalyzer.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import jspectrumanalyzer.core.FmChannel;
import jspectrumanalyzer.core.FmChannelPlan;
import jspectrumanalyzer.core.FmStationHit;

/**
 * Live US FM stations on the spectrum plot. Only channels present in the
 * current sweep (peaks snapped to the 200 kHz dial) are drawn, labeled
 * like {@code 97.3}. Stronger hits keep their label when numbers collide.
 */
public final class FmChannelOverlay
{
	private static final Color FILL = new Color(80, 160, 230, 120);
	private static final Color LINE = new Color(130, 200, 255, 200);
	private static final Color HEADER_RULE = new Color(170, 170, 170, 80);
	private static final Color LABEL = new Color(230, 230, 230, 230);
	private static final int HEADER_H = 16;
	private static final Stroke DASH = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 8f,
			new float[] { 3f, 4f }, 0f);
	static final int MIN_LABEL_GAP_PX = 10;
	/**
	 * Station pills need an FM-scale window. Past ~1.5× the 20 MHz Quick
	 * Select band they become header jitter on top of the QS names.
	 */
	static final double MAX_VIEW_SPAN_MHZ = 30;

	private FmChannelOverlay()
	{
	}

	/**
	 * True when the plot is zoomed to the FM broadcast band (or a slice
	 * of it), not a multi-band survey.
	 */
	public static boolean tagsReadable(double startMHz, double endMHz)
	{
		if (endMHz <= startMHz)
			return false;
		double view = endMHz - startMHz;
		if (view > MAX_VIEW_SPAN_MHZ)
			return false;
		double low = Math.max(startMHz, FmChannelPlan.VIEW_START_MHZ);
		double high = Math.min(endMHz, FmChannelPlan.VIEW_END_MHZ);
		return high > low;
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz, List<FmStationHit> stations)
	{
		if (area == null || domain == null || stations == null || stations.isEmpty() || area.getWidth() < 8
				|| endMHz <= startMHz || !tagsReadable(startMHz, endMHz))
			return;

		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setClip(area);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			FontMetrics fm = g.getFontMetrics();
			int top = (int) Math.round(area.getMinY());
			int bottom = (int) Math.round(area.getMaxY());
			int headerH = Math.min(HEADER_H, Math.max(1, bottom - top));

			boolean anyFill = false;
			for (FmStationHit hit : stations)
			{
				FmChannel ch = hit.channel;
				if (ch == null || !ch.occupancyOverlaps(startMHz, endMHz))
					continue;
				double low = Math.max(ch.lowMHz(), startMHz);
				double high = Math.min(ch.highMHz(), endMHz);
				if (high <= low)
					continue;
				int x1 = (int) Math.round(domain.valueToJava2D(low, area, edge));
				int x2 = (int) Math.round(domain.valueToJava2D(high, area, edge));
				if (x2 < x1)
				{
					int t = x1;
					x1 = x2;
					x2 = t;
				}
				g.setColor(withConfidence(FILL, hit.confidence));
				g.fillRect(x1, top, Math.max(1, x2 - x1), headerH);
				anyFill = true;
			}
			if (anyFill)
			{
				g.setColor(HEADER_RULE);
				g.drawLine((int) Math.round(area.getMinX()), top + headerH,
						(int) Math.round(area.getMaxX()), top + headerH);
			}

			g.setStroke(DASH);
			for (FmStationHit hit : stations)
			{
				FmChannel ch = hit.channel;
				if (ch == null || !ch.centerIn(startMHz, endMHz))
					continue;
				int xi = (int) Math.round(domain.valueToJava2D(ch.centerMHz(), area, edge));
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.setColor(withConfidence(LINE, hit.confidence));
				g.drawLine(xi, top, xi, bottom);
			}

			List<FmStationHit> labels = new ArrayList<>();
			for (FmStationHit hit : stations)
			{
				if (hit != null && hit.channel != null && hit.channel.centerIn(startMHz, endMHz))
					labels.add(hit);
			}
			labels.sort((a, b) -> {
				int byConf = Float.compare(b.confidence, a.confidence);
				return byConf != 0 ? byConf : Float.compare(b.powerDbm, a.powerDbm);
			});
			List<double[]> placed = new ArrayList<>();
			g.setColor(LABEL);
			for (FmStationHit hit : labels)
			{
				double x = domain.valueToJava2D(hit.channel.centerMHz(), area, edge);
				if (x < area.getMinX() || x > area.getMaxX())
					continue;
				String text = hit.label();
				int w = fm.stringWidth(text);
				double left = x - w / 2.0;
				double right = left + w;
				if (left < area.getMinX() + 1 || right > area.getMaxX() - 1)
					continue;
				if (overlaps(placed, left, right))
					continue;
				g.drawString(text, (float) left, (float) (top + fm.getAscent() + 1));
				placed.add(new double[] { left, right });
			}
		}
		finally
		{
			g.dispose();
		}
	}

	static Color withConfidence(Color base, float confidence)
	{
		float c = Math.max(0f, Math.min(1f, confidence));
		int a = Math.round(base.getAlpha() * (0.45f + 0.55f * c));
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
	}

	static boolean overlaps(List<double[]> placed, double left, double right)
	{
		for (double[] box : placed)
		{
			if (left < box[1] + MIN_LABEL_GAP_PX && right + MIN_LABEL_GAP_PX > box[0])
				return true;
		}
		return false;
	}
}
