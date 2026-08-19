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

import jspectrumanalyzer.core.BandMark;
import jspectrumanalyzer.core.FrequencyAxis;

/**
 * Shared top-header + divider paint for Wi-Fi, FM, and Quick Select marks.
 */
public final class BandHeaderPainter
{
	public static final int HEADER_H = 16;
	public static final int MIN_LABEL_GAP_PX = 10;
	private static final Color FILL_PRIMARY = new Color(80, 160, 230, 110);
	private static final Color FILL_SECONDARY = new Color(160, 160, 160, 70);
	private static final Color FILL_SURVEY = new Color(160, 160, 160, 18);
	private static final Color FILL_SURVEY_HEADER = new Color(160, 160, 160, 70);
	private static final Color FILL_FULL_PRIMARY = new Color(80, 160, 230, 38);
	private static final Color LINE_PRIMARY = new Color(130, 200, 255, 180);
	private static final Color LINE_SECONDARY = new Color(170, 170, 170, 120);
	private static final Color LINE_SURVEY = new Color(170, 170, 170, 80);
	private static final Color HEADER_RULE = new Color(170, 170, 170, 80);
	private static final Color LABEL = new Color(230, 230, 230, 230);
	private static final Stroke DASH = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 8f,
			new float[] { 3f, 4f }, 0f);

	private BandHeaderPainter()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, FrequencyAxis axis, List<BandMark> marks)
	{
		if (g0 == null || area == null || axis == null || !axis.usable() || marks == null || marks.isEmpty())
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
			int h = Math.max(1, bottom - top);
			int headerH = Math.min(HEADER_H, h);

			boolean anyHeader = false;
			for (BandMark mark : marks)
			{
				if (!axis.occupancyVisible(mark.lowMHz, mark.highMHz))
					continue;
				int x1 = axis.mhzToXInt(axis.clipLow(mark.lowMHz));
				int x2 = axis.mhzToXInt(axis.clipHigh(mark.highMHz));
				if (x2 < x1)
				{
					int t = x1;
					x1 = x2;
					x2 = t;
				}
				int bw = Math.max(1, x2 - x1);
				if (mark.fullHeightFill)
				{
					g.setColor(withIntensity(fullFill(mark.style), mark.intensity));
					g.fillRect(x1, top, bw, h);
				}
				g.setColor(withIntensity(headerFill(mark.style), mark.intensity));
				g.fillRect(x1, top, bw, headerH);
				anyHeader = true;
			}
			if (anyHeader)
			{
				g.setColor(HEADER_RULE);
				g.drawLine((int) Math.round(area.getMinX()), top + headerH,
						(int) Math.round(area.getMaxX()), top + headerH);
			}

			g.setStroke(DASH);
			TreeSet<Long> edges = new TreeSet<>();
			for (BandMark mark : marks)
			{
				if (!mark.edgeTicks)
					continue;
				edges.add(Math.round(mark.lowMHz * 1000.0));
				edges.add(Math.round(mark.highMHz * 1000.0));
			}
			for (Long milli : edges)
			{
				double mhz = milli / 1000.0;
				if (!axis.containsMhz(mhz))
					continue;
				int xi = axis.mhzToXInt(mhz);
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.setColor(LINE_SECONDARY);
				g.drawLine(xi, top, xi, bottom);
			}
			for (BandMark mark : marks)
			{
				if (!mark.centerTick || !mark.centerIn(axis))
					continue;
				int xi = axis.mhzToXInt(mark.labelMHz);
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.setColor(withIntensity(tick(mark.style), mark.intensity));
				g.drawLine(xi, top, xi, bottom);
			}

			List<double[]> placed = new ArrayList<>();
			g.setColor(LABEL);
			float labelY = labelBaselineY(area, fm);
			for (BandMark mark : marks)
			{
				if (mark.label.isEmpty() || !mark.centerIn(axis))
					continue;
				double x = axis.mhzToX(mark.labelMHz);
				if (x < area.getMinX() || x > area.getMaxX())
					continue;
				int w = fm.stringWidth(mark.label);
				if (mark.labelFit == BandMark.LabelFit.FIT_OCCUPANCY)
				{
					double occ = Math.abs(axis.mhzToX(axis.clipHigh(mark.highMHz))
							- axis.mhzToX(axis.clipLow(mark.lowMHz)));
					if (w > occ - 4)
						continue;
				}
				double left = x - w / 2.0;
				double right = left + w;
				if (left < area.getMinX() + 1 || right > area.getMaxX() - 1)
					continue;
				if (overlaps(placed, left, right))
					continue;
				g.drawString(mark.label, (float) left, labelY);
				placed.add(new double[] { left, right });
			}
		}
		finally
		{
			g.dispose();
		}
	}

	public static float labelBaselineY(Rectangle2D area, FontMetrics fm)
	{
		return (float) (area.getMinY() + fm.getAscent() + 1);
	}

	public static boolean overlaps(List<double[]> placed, double left, double right)
	{
		for (double[] box : placed)
		{
			if (left < box[1] + MIN_LABEL_GAP_PX && right + MIN_LABEL_GAP_PX > box[0])
				return true;
		}
		return false;
	}

	static Color withIntensity(Color base, float intensity)
	{
		float c = Math.max(0f, Math.min(1f, intensity));
		int a = Math.round(base.getAlpha() * (0.45f + 0.55f * c));
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
	}

	private static Color headerFill(BandMark.Style style)
	{
		if (style == BandMark.Style.PRIMARY)
			return FILL_PRIMARY;
		if (style == BandMark.Style.SURVEY)
			return FILL_SURVEY_HEADER;
		return FILL_SECONDARY;
	}

	private static Color fullFill(BandMark.Style style)
	{
		return style == BandMark.Style.SURVEY ? FILL_SURVEY : FILL_FULL_PRIMARY;
	}

	private static Color tick(BandMark.Style style)
	{
		if (style == BandMark.Style.PRIMARY)
			return LINE_PRIMARY;
		if (style == BandMark.Style.SURVEY)
			return LINE_SURVEY;
		return LINE_SECONDARY;
	}
}
