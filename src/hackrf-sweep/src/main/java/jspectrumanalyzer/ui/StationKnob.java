package jspectrumanalyzer.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.JComponent;

import jspectrumanalyzer.core.FmChannelPlan;

/**
 * Rotary tuner. Clockwise / drag-right / scroll-up is the next higher
 * station. Detents are one digital jump; the pointer snaps to the
 * current station among {@link #setDetents}.
 */
public final class StationKnob extends JComponent
{
	public static final double DETENT_RAD = Math.toRadians(22);
	private static final Color FACE = new Color(36, 36, 40);
	private static final Color RIM = new Color(110, 108, 102);
	private static final Color WELL = new Color(22, 22, 24);
	private static final Color POINTER = new Color(255, 196, 64);
	private static final Color TICK = new Color(200, 198, 190);
	private static final Color TICK_ON = new Color(255, 210, 80);

	private int kHz = 97300;
	private final List<Integer> detents = new ArrayList<Integer>();
	private IntConsumer onStep;
	private boolean dragging;
	private double lastAng;
	private double accum;

	public StationKnob()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(108, 108));
		setMinimumSize(new Dimension(88, 88));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText("Turn clockwise for a higher station. Each click is one detected FM hit.");
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!SwingUtilitiesLeft(e) || !inKnob(e))
					return;
				dragging = true;
				lastAng = angle(e);
				accum = 0;
				requestFocusInWindow();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = false;
				accum = 0;
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (!dragging)
					return;
				double a = angle(e);
				double d = a - lastAng;
				while (d > Math.PI)
					d -= 2 * Math.PI;
				while (d < -Math.PI)
					d += 2 * Math.PI;
				lastAng = a;
				accum += d;
				while (accum >= DETENT_RAD)
				{
					nudge(+1);
					accum -= DETENT_RAD;
				}
				while (accum <= -DETENT_RAD)
				{
					nudge(-1);
					accum += DETENT_RAD;
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				nudge(e.getWheelRotation() < 0 ? +1 : -1);
				e.consume();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	private static boolean SwingUtilitiesLeft(MouseEvent e)
	{
		return javax.swing.SwingUtilities.isLeftMouseButton(e);
	}

	public void setOnStep(IntConsumer onStep)
	{
		this.onStep = onStep;
	}

	public void setKHz(int kHz)
	{
		int next = FmChannelPlan.clamp(kHz / 1000.0).centerKHz;
		if (this.kHz == next)
			return;
		this.kHz = next;
		repaint();
	}

	public int getKHz()
	{
		return kHz;
	}

	public void setDetents(List<Integer> stationKHz)
	{
		detents.clear();
		if (stationKHz != null)
		{
			for (Integer k : stationKHz)
			{
				if (k != null && !detents.contains(k))
					detents.add(k);
			}
			detents.sort(Integer::compareTo);
		}
		repaint();
	}

	public void nudge(int direction)
	{
		if (onStep != null)
			onStep.accept(direction < 0 ? -1 : 1);
	}

	@Override
	protected void paintComponent(Graphics g0)
	{
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			int size = Math.min(w, h);
			int cx = w / 2;
			int cy = h / 2;
			int r = size / 2 - 4;
			g.setColor(WELL);
			g.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);
			g.setColor(RIM);
			g.fillOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);
			g.setColor(FACE);
			g.fillOval(cx - r + 5, cy - r + 5, (r - 5) * 2, (r - 5) * 2);
			int n = Math.max(1, detents.size());
			int idx = detentIndex();
			for (int i = 0; i < detents.size(); i++)
			{
				double ang = tickAngle(i, n);
				int inner = r - 16;
				int outer = r - 6;
				int x0 = cx + (int) Math.round(Math.cos(ang) * inner);
				int y0 = cy + (int) Math.round(Math.sin(ang) * inner);
				int x1 = cx + (int) Math.round(Math.cos(ang) * outer);
				int y1 = cy + (int) Math.round(Math.sin(ang) * outer);
				g.setColor(i == idx ? TICK_ON : TICK);
				g.setStroke(new BasicStroke(i == idx ? 2.4f : 1.2f));
				g.drawLine(x0, y0, x1, y1);
			}
			double pang = tickAngle(idx, n);
			g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(POINTER);
			int px = cx + (int) Math.round(Math.cos(pang) * (r - 18));
			int py = cy + (int) Math.round(Math.sin(pang) * (r - 18));
			g.drawLine(cx, cy, px, py);
			g.fillOval(cx - 5, cy - 5, 10, 10);
		}
		finally
		{
			g.dispose();
		}
	}

	private int detentIndex()
	{
		if (detents.isEmpty())
			return 0;
		int best = 0;
		int bestD = Integer.MAX_VALUE;
		for (int i = 0; i < detents.size(); i++)
		{
			int d = Math.abs(detents.get(i) - kHz);
			if (d < bestD)
			{
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	private static double tickAngle(int i, int n)
	{
		double start = Math.toRadians(210);
		double span = Math.toRadians(300);
		if (n <= 1)
			return start + span / 2;
		return start + span * i / (n - 1);
	}

	private double angle(MouseEvent e)
	{
		return Math.atan2(e.getY() - getHeight() / 2.0, e.getX() - getWidth() / 2.0);
	}

	private boolean inKnob(MouseEvent e)
	{
		double dx = e.getX() - getWidth() / 2.0;
		double dy = e.getY() - getHeight() / 2.0;
		int r = Math.min(getWidth(), getHeight()) / 2 - 2;
		return dx * dx + dy * dy <= (double) r * r;
	}
}
