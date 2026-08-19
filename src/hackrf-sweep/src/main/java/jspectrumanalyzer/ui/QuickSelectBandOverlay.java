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

/**
 * Quick Select ranges as vertical bands when the plot is zoomed out past
 * a single preset. Nested ITU envelopes (HF/VHF/UHF) are drawn lighter
 * under the specific buttons (FM, WiFi, amateur, …).
 */
public final class QuickSelectBandOverlay
{
	private static final Color FILL_SPECIFIC = new Color(80, 160, 230, 38);
	private static final Color FILL_SURVEY = new Color(160, 160, 160, 18);
	private static final Color HEADER_SPECIFIC = new Color(80, 160, 230, 110);
	private static final Color HEADER_SURVEY = new Color(160, 160, 160, 70);
	private static final Color LINE_SPECIFIC = new Color(130, 200, 255, 150);
	private static final Color LINE_SURVEY = new Color(170, 170, 170, 80);
	private static final Color HEADER_RULE = new Color(170, 170, 170, 80);
	private static final Color LABEL = new Color(230, 230, 230, 230);
	private static final Stroke DASH = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 8f,
			new float[] { 4f, 4f }, 0f);
	static final int HEADER_H = 16;
	static final int MIN_LABEL_GAP_PX = 10;

	private QuickSelectBandOverlay()
	{
	}

	public static void paint(Graphics2D g0, Rectangle2D area, ValueAxis domain, RectangleEdge edge, double startMHz,
			double endMHz)
	{
		if (area == null || domain == null || area.getWidth() < 8 || endMHz <= startMHz)
			return;
		double span = endMHz - startMHz;
		List<QuickSelectPreset> bands = QuickSelectPreset.visibleInView(startMHz, endMHz);
		if (bands.isEmpty())
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

			for (QuickSelectPreset band : bands)
			{
				double low = band.visibleLowMHz(startMHz, endMHz);
				double high = band.visibleHighMHz(startMHz, endMHz);
				if (high <= low)
					continue;
				if ((high - low) * area.getWidth() / span < 3)
					continue;
				int x1 = (int) Math.round(domain.valueToJava2D(low, area, edge));
				int x2 = (int) Math.round(domain.valueToJava2D(high, area, edge));
				if (x2 < x1)
				{
					int t = x1;
					x1 = x2;
					x2 = t;
				}
				int bw = Math.max(1, x2 - x1);
				g.setColor(band.surveyEnvelope() ? FILL_SURVEY : FILL_SPECIFIC);
				g.fillRect(x1, top, bw, h);
				g.setColor(band.surveyEnvelope() ? HEADER_SURVEY : HEADER_SPECIFIC);
				g.fillRect(x1, top, bw, headerH);
			}
			g.setColor(HEADER_RULE);
			g.drawLine((int) Math.round(area.getMinX()), top + headerH,
					(int) Math.round(area.getMaxX()), top + headerH);

			g.setStroke(DASH);
			TreeSet<Long> edges = new TreeSet<>();
			for (QuickSelectPreset band : bands)
			{
				if (band.surveyEnvelope())
					continue;
				edges.add(Math.round(band.startMHz * 1000.0));
				edges.add(Math.round(band.endMHz * 1000.0));
			}
			g.setColor(LINE_SPECIFIC);
			for (Long milli : edges)
			{
				double mhz = milli / 1000.0;
				if (mhz < startMHz || mhz > endMHz)
					continue;
				int xi = (int) Math.round(domain.valueToJava2D(mhz, area, edge));
				if (xi < area.getMinX() || xi > area.getMaxX())
					continue;
				g.drawLine(xi, top, xi, bottom);
			}
			g.setColor(LINE_SURVEY);
			for (QuickSelectPreset band : bands)
			{
				if (!band.surveyEnvelope())
					continue;
				for (double mhz : new double[] { band.startMHz, band.endMHz })
				{
					if (mhz < startMHz || mhz > endMHz)
						continue;
					int xi = (int) Math.round(domain.valueToJava2D(mhz, area, edge));
					if (xi < area.getMinX() || xi > area.getMaxX())
						continue;
					g.drawLine(xi, top, xi, bottom);
				}
			}

			List<double[]> placed = new ArrayList<>();
			g.setColor(LABEL);
			float labelY = labelBaselineY(area, fm);
			for (QuickSelectPreset band : QuickSelectPreset.labelPriority(bands))
			{
				double low = band.visibleLowMHz(startMHz, endMHz);
				double high = band.visibleHighMHz(startMHz, endMHz);
				double mid = (low + high) / 2.0;
				double x = domain.valueToJava2D(mid, area, edge);
				if (x < area.getMinX() || x > area.getMaxX())
					continue;
				String text = band.label;
				int w = fm.stringWidth(text);
				double occ1 = domain.valueToJava2D(low, area, edge);
				double occ2 = domain.valueToJava2D(high, area, edge);
				if (w > Math.abs(occ2 - occ1) - 4)
					continue;
				double left = x - w / 2.0;
				double right = left + w;
				if (left < area.getMinX() + 1 || right > area.getMaxX() - 1)
					continue;
				if (overlaps(placed, left, right))
					continue;
				g.drawString(text, (float) left, labelY);
				placed.add(new double[] { left, right });
			}
		}
		finally
		{
			g.dispose();
		}
	}

	static float labelBaselineY(Rectangle2D area, FontMetrics fm)
	{
		return (float) (area.getMinY() + fm.getAscent() + 1);
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
