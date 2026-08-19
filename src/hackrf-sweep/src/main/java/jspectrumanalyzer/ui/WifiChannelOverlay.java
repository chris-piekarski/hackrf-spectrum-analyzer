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
import java.util.TreeSet;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import jspectrumanalyzer.core.WifiChannel;
import jspectrumanalyzer.core.WifiChannelPlan;

/**
 * 20 MHz 802.11 occupancy on the spectrum plot. 2.4 GHz channels overlap
 * (ch N starts 5 MHz after ch N−1); 5 GHz 20 MHz channels do not. The
 * fill is the occupied slice, not the 5 MHz numbering raster.
 */
public final class WifiChannelOverlay
{
	private static final Color FILL_PRIMARY = new Color(80, 160, 230, 110);
	private static final Color FILL_OTHER = new Color(160, 160, 160, 70);
	private static final Color LINE_PRIMARY = new Color(130, 200, 255, 180);
	private static final Color LINE_EDGE = new Color(170, 170, 170, 120);
	private static final Color HEADER_RULE = new Color(170, 170, 170, 80);
	private static final Color LABEL = new Color(230, 230, 230, 230);
	private static final int HEADER_H = 16;
	private static final Stroke DASH = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 8f,
			new float[] { 3f, 4f }, 0f);
	private static final int MIN_LABEL_GAP_PX = 14;

	private WifiChannelOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz)
	{
		if (area == null || domain == null || area.getWidth() < 8 || endMHz <= startMHz)
			return;
		double span = endMHz - startMHz;
		double width = area.getWidth();
		boolean show24 = 20 * width / span >= 8;
		boolean show5 = 20 * width / span >= 4;
		if (!show24 && !show5)
			return;
		List<WifiChannel> visible = new ArrayList<>();
		for (WifiChannel ch : WifiChannelPlan.visibleOccupancy(startMHz, endMHz))
		{
			if (WifiChannelPlan.BAND_24.equals(ch.band) && show24)
				visible.add(ch);
			else if (WifiChannelPlan.BAND_5.equals(ch.band) && show5)
				visible.add(ch);
		}
		if (visible.isEmpty())
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

			visible.sort((a, b) -> Boolean.compare(a.primary, b.primary));
			for (WifiChannel ch : visible)
			{
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
				g.setColor(ch.primary ? FILL_PRIMARY : FILL_OTHER);
				g.fillRect(x1, top, Math.max(1, x2 - x1), headerH);
			}
			g.setColor(HEADER_RULE);
			g.drawLine((int) Math.round(area.getMinX()), top + headerH,
					(int) Math.round(area.getMaxX()), top + headerH);

			g.setStroke(DASH);
			TreeSet<Long> edgesMilli = new TreeSet<>();
			for (WifiChannel ch : visible)
			{
				edgesMilli.add(Math.round(ch.lowMHz() * 1000));
				edgesMilli.add(Math.round(ch.highMHz() * 1000));
			}
			g.setColor(LINE_EDGE);
			for (Long milli : edgesMilli)
			{
				double mhz = milli / 1000.0;
				if (mhz < startMHz || mhz > endMHz)
					continue;
				int xi = (int) Math.round(domain.valueToJava2D(mhz, area, edge));
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.drawLine(xi, top, xi, bottom);
			}

			for (WifiChannel ch : visible)
			{
				if (!ch.centerIn(startMHz, endMHz))
					continue;
				int xi = (int) Math.round(domain.valueToJava2D(ch.centerMHz, area, edge));
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.setColor(ch.primary ? LINE_PRIMARY : LINE_EDGE);
				g.drawLine(xi, top, xi, bottom);
			}

			List<double[]> placed = new ArrayList<>();
			g.setColor(LABEL);
			for (WifiChannel ch : WifiChannelPlan.labelPriority(visible))
			{
				if (!ch.centerIn(startMHz, endMHz))
					continue;
				double x = domain.valueToJava2D(ch.centerMHz, area, edge);
				if (x < area.getMinX() || x > area.getMaxX())
					continue;
				String text = ch.label();
				int w = fm.stringWidth(text);
				double left = x - w / 2.0;
				double right = left + w;
				if (left < area.getMinX() + 1 || right > area.getMaxX() - 1)
					continue;
				// 2.4 GHz occupancy overlaps, so drop colliding numbers (keep 1/6/11).
				// 5 GHz 20 MHz channels do not overlap; each pill gets its own label
				// when the text fits inside the occupied slice.
				if (WifiChannelPlan.BAND_24.equals(ch.band))
				{
					if (overlaps(placed, left, right))
						continue;
				}
				else
				{
					double occ1 = domain.valueToJava2D(Math.max(ch.lowMHz(), startMHz), area, edge);
					double occ2 = domain.valueToJava2D(Math.min(ch.highMHz(), endMHz), area, edge);
					if (w > Math.abs(occ2 - occ1) - 2)
						continue;
				}
				g.drawString(text, (float) left, (float) (top + fm.getAscent() + 1));
				placed.add(new double[] { left, right });
			}
		}
		finally
		{
			g.dispose();
		}
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
