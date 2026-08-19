package jspectrumanalyzer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;

import jspectrumanalyzer.core.DatasetSpectrum;
import jspectrumanalyzer.core.EMA;

public class WaterfallPlot extends JPanel {
	/**
	 * 
	 */
	private static final long	serialVersionUID		= 3249110968962287324L;
	private BufferedImage		bufferedImages[]		= new BufferedImage[2];
	private int					chartXOffset			= 0, chartWidth = 100;
	private boolean				displayMarker			= false;
	private double				displayMarkerFrequency	= 0;
	private int					displayMarkerX			= 0;
	private int					displayMarkerY			= 0;
	private int					drawIndex				= 0;
	/**
	 * stores max value in pixel
	 */
	private float				drawMaxBuffer[];
	private EMA					fps						= new EMA(3);
	private int					fpsRenderedFrames		= 0;
	private long				lastFPSRecalculated		= 0;
	private DatasetSpectrum		lastSpectrum			= null;
	private ColorPalette		palette					= new HotIronBluePalette();
	private Rectangle2D.Float	rect					= new Rectangle2D.Float(0f, 0f, 1f, 1f);
	private int					lastBinCount			= 0;
	private int					screenWidth;
	private double				spectrumPaletteSize		= 65;
	private double				spectrumPaletteStart	= -90;
	private long[]				rowEpochMs;
	private static final Color	TIME_AXIS_COLOR			= new Color(0xBB, 0xBB, 0xBB);
	private static final int	TIME_AXIS_MIN_GUTTER	= 28;

	public WaterfallPlot(ChartPanel chartPanel, int maxHeight) {
		setPreferredSize(new Dimension(100, 200));
		setMinimumSize(new Dimension(100, 200));

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				setHistorySize(getHeight());
			}
		});

		screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		drawMaxBuffer = new float[screenWidth];

		bufferedImages[0] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, maxHeight);
		bufferedImages[1] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, maxHeight);
		rowEpochMs = new long[Math.max(1, maxHeight)];

		/**
		 * setup frequency marker
		 */
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				displayMarker = false;
				int x = e.getX();
				if (x < chartXOffset || x > chartXOffset + chartWidth) {
					return;
				}
				double freq = translateChartXToFrequency(x - chartXOffset);
				if (freq != -1) {
					displayMarker = true;
					displayMarkerFrequency = freq;
					displayMarkerX = x;
					displayMarkerY = e.getY();
				}
				WaterfallPlot.this.repaint();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				displayMarker = false;
			}
		});
	}

	private EMA newDataTimeEMA =	 new EMA(100);
	/**
	 * Adds new data to the waterfall plot and renders it
	 * 
	 * @param spectrum
	 */
	public synchronized void addNewData(DatasetSpectrum spectrum) {
		long start	= System.nanoTime();

		int size = spectrum.spectrumLength();
		double startFreq = spectrum.getFreqStartMHz() * 1000000d;
		double freqRange = (spectrum.getFreqStopMHz() - spectrum.getFreqStartMHz()) * 1000000d;
		double width = bufferedImages[0].getWidth();
		this.lastSpectrum = spectrum;

		/**
		 * shift image by one pixel down
		 */
		BufferedImage previousImage = bufferedImages[drawIndex];
		drawIndex = (drawIndex + 1) % 2;
		Graphics2D g = bufferedImages[drawIndex].createGraphics();
		g.drawImage(previousImage, 0, 1, null);
		g.setColor(Color.black);
		g.fillRect(0, 0, (int) width, 1);
		shiftRowTimes(System.currentTimeMillis());

		float binWidth = (float) (spectrum.getFFTBinSizeHz() / freqRange * width);
		rect.x = 0;
		rect.y = 0;
		rect.height = 0;
		rect.width = binWidth;

		float minimumValueDrawBuffer = -150;
		Arrays.fill(drawMaxBuffer, minimumValueDrawBuffer);

		/**
		 * draw in two passes - first determines maximum power for the pixel,
		 * second draws it
		 */
		if (true) {
			//optimized drawing
			double widthDivSize = (double)width / size;
			for (int i = 0; i < size; i++) {
				double power = spectrum.getPower(i);
				double percentagePower = normalizePower(power, spectrumPaletteStart, spectrumPaletteSize);
				int pixelX = clampPixelX((int) Math.round(widthDivSize * i), drawMaxBuffer.length);
				if (percentagePower > drawMaxBuffer[pixelX])
					drawMaxBuffer[pixelX] = (float) percentagePower;
			}
		} else {
			//unoptimized drawing
			for (int i = 0; i < size; i++) {
				double freq = spectrum.getFrequency(i);
				double power = spectrum.getPower(i);
				double percentageFreq = (freq - startFreq) / freqRange;
				double percentagePower = normalizePower(power, spectrumPaletteStart, spectrumPaletteSize);
				int pixelX = clampPixelX((int) Math.round(width * percentageFreq), drawMaxBuffer.length);
				if (percentagePower > drawMaxBuffer[pixelX])
					drawMaxBuffer[pixelX] = (float) percentagePower;
			}
		}

		/**
		 * fill in pixels that do not have power with last bin's color in order
		 * to smooth the spectrum
		 */
		Color lastValidColor = palette.getColor(0);
		for (int x = 0; x < drawMaxBuffer.length; x++) {
			Color color;
			if (drawMaxBuffer[x] == minimumValueDrawBuffer)
				color = lastValidColor;
			else {
				color = palette.getColorNormalized(drawMaxBuffer[x]);
				lastValidColor = color;
			}
			rect.x = x;
			g.setColor(color);
			g.draw(rect);
		}

		lastBinCount = size;
		fpsRenderedFrames++;
		if (System.currentTimeMillis() - lastFPSRecalculated > 1000) {
			double rawfps = fpsRenderedFrames / ((System.currentTimeMillis() - (double) lastFPSRecalculated) / 1000d);
			fps.addNewValue(rawfps);
			lastFPSRecalculated = System.currentTimeMillis();
			fpsRenderedFrames = 0;
		}
		g.dispose();

//		double time	= newDataTimeEMA.addNewValue(((System.nanoTime()-start)/1000));
//		System.out.println("draw "+(int)time+"us");

//		repaint();
	}

	/**
	 * Draws color palette into given area from bottom (0%) to top (100%)
	 * 
	 * @param g
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public void drawScale(Graphics2D g, int x, int y, int w, int h) {
		g = (Graphics2D) g.create(x, y, w, h);
		int step = 3;
		for (int i = 0; i < h; i += step) {
			Color c = palette.getColorNormalized(1 - (double) i / h);
			g.setColor(c);
			g.fillRect(0, i, w, step);
		}

		/**
		 * draw border around the scale
		 */
		int thickness = 2;
		g.setColor(Color.darkGray);
		g.fillRect(0, 0, w, thickness);
		g.fillRect(w - thickness, 0, thickness, h);
		g.fillRect(0, h - thickness, w, thickness);
		g.dispose();
	}

	public int getHistorySize() {
		return bufferedImages[0].getHeight();
	}

	public double getSpectrumPaletteSize() {
		return spectrumPaletteSize;
	}

	public double getSpectrumPaletteStart() {
		return spectrumPaletteStart;
	}

	public void setDrawingOffsets(int xOffsetLeft, int width) {
		this.chartXOffset = xOffsetLeft;
		this.chartWidth = width;
	}

	public double getLastRbwHz() {
		return lastSpectrum == null ? 0 : lastSpectrum.getFFTBinSizeHz();
	}

	public int getLastBinCount() {
		return lastBinCount;
	}

	public double getFps() {
		return fps.getEma();
	}

	/** Drop scrolled history (used on retune so old MHz mapping is not reused). */
	public synchronized void clearHistory() {
		for (BufferedImage image : bufferedImages) {
			if (image == null)
				continue;
			Graphics2D g = image.createGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		}
		lastSpectrum = null;
		lastBinCount = 0;
		if (rowEpochMs != null)
			Arrays.fill(rowEpochMs, 0L);
	}

	public synchronized void setHistorySize(int historyInPixels) {
		BufferedImage bufferedImages[] = new BufferedImage[2];
		bufferedImages[0] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, historyInPixels);
		bufferedImages[1] = GraphicsToolkit.createAcceleratedImageOpaque(screenWidth, historyInPixels);
		copyImage(this.bufferedImages[0], bufferedImages[0]);
		copyImage(this.bufferedImages[1], bufferedImages[1]);
		this.bufferedImages = bufferedImages;
		ensureRowTimes(Math.max(1, historyInPixels));
	}

	public void setSpectrumPaletteSize(int dB) {
		this.spectrumPaletteSize = dB;
	}

	/**
	 * Sets start and end of the color scale
	 * 
	 * @param minFreqency
	 * @param maxFrequency
	 */
	public void setSpectrumPaletteStart(int dB) {
		this.spectrumPaletteStart = dB;
	}

	/**
	 * Map the waterfall palette onto a live dB window (same bounds as the
	 * spectrum Y-axis) so a 15 dB FM band is not crushed into the blue
	 * third of a fixed −90…−25 scale.
	 */
	public void applyPowerWindow(double lowDb, double highDb) {
		setSpectrumPaletteStart(paletteStartDb(lowDb));
		setSpectrumPaletteSize(paletteSizeDb(lowDb, highDb));
	}

	public static int paletteStartDb(double lowDb) {
		return (int) Math.round(lowDb);
	}

	public static int paletteSizeDb(double lowDb, double highDb) {
		int span = (int) Math.round(highDb - lowDb);
		return Math.max(1, span);
	}

	private void ensureRowTimes(int historyInPixels) {
		int n = Math.max(1, historyInPixels);
		if (rowEpochMs != null && rowEpochMs.length == n)
			return;
		long[] next = new long[n];
		if (rowEpochMs != null)
			System.arraycopy(rowEpochMs, 0, next, 0, Math.min(rowEpochMs.length, n));
		rowEpochMs = next;
	}

	private void shiftRowTimes(long nowMs) {
		int hist = bufferedImages[drawIndex].getHeight();
		ensureRowTimes(hist);
		if (rowEpochMs.length > 1)
			System.arraycopy(rowEpochMs, 0, rowEpochMs, 1, rowEpochMs.length - 1);
		rowEpochMs[0] = nowMs;
	}

	private void copyImage(BufferedImage src, BufferedImage dst) {
		Graphics2D g = dst.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
	}

	/**
	 * Maps a power sample onto the waterfall palette, 0 (at or below start) to 1 (at or above start+size).
	 */
	public static double normalizePower(double power, double paletteStart, double paletteSize) {
		if (paletteSize <= 0)
			return 0;
		if (power <= paletteStart)
			return 0;
		if (power >= paletteStart + paletteSize)
			return 1;
		return (power - paletteStart) / paletteSize;
	}

	public static int clampPixelX(int pixelX, int bufferLength) {
		if (bufferLength <= 0)
			return 0;
		if (pixelX >= bufferLength)
			return bufferLength - 1;
		if (pixelX < 0)
			return 0;
		return pixelX;
	}

	public static double translateXToFrequency(int x, int chartWidth, double startFreqHz, double stopFreqHz) {
		if (chartWidth <= 0)
			return -1;
		jspectrumanalyzer.core.FrequencyAxis axis = jspectrumanalyzer.core.FrequencyAxis.of(startFreqHz / 1_000_000d,
				stopFreqHz / 1_000_000d, chartWidth);
		return axis.xToMhz(x) * 1_000_000d;
	}

	private double translateChartXToFrequency(int x) {
		if (lastSpectrum != null) {
			double startFreq = lastSpectrum.getFreqStartMHz() * 1000000d;
			double stopFreq = lastSpectrum.getFreqStopMHz() * 1000000d;
			return translateXToFrequency(x, chartWidth, startFreq, stopFreq);
		}
		return -1;
	}

	void drawTimeAxis(Graphics2D g, long[] times, int height) {
		int gutter = chartXOffset;
		if (gutter < TIME_AXIS_MIN_GUTTER || height < 8)
			return;
		List<WaterfallTimeScale.Tick> ticks = WaterfallTimeScale.ticks(times, height,
				WaterfallTimeScale.DEFAULT_MAX_TICKS);
		if (ticks.isEmpty())
			return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = getFont() == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 11) : getFont().deriveFont(Font.PLAIN, 11f);
		g.setFont(font);
		g.setColor(TIME_AXIS_COLOR);
		FontMetrics fm = g.getFontMetrics();
		int axisX = gutter - 1;
		g.drawLine(axisX, 0, axisX, height);
		int ascent = fm.getAscent();
		for (WaterfallTimeScale.Tick tick : ticks) {
			int y = tick.y;
			if (y < 0)
				y = 0;
			if (y > height - 1)
				y = height - 1;
			g.drawLine(axisX - 4, y, axisX, y);
			int tw = fm.stringWidth(tick.label);
			int tx = axisX - 6 - tw;
			if (tx < 2)
				tx = 2;
			int ty = y + ascent / 2;
			if (ty < ascent)
				ty = ascent;
			if (ty > height - 2)
				ty = height - 2;
			g.drawString(tick.label, tx, ty);
		}
	}

	@Override
	protected void paintComponent(Graphics arg0) {
		long drawStart	= System.nanoTime();
		Graphics2D g = (Graphics2D) arg0;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int w = chartWidth;
		int h = getHeight();
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.drawImage(bufferedImages[drawIndex], chartXOffset, 0, w, h, null);

		long[] times;
		synchronized (this) {
			times = rowEpochMs == null ? null : rowEpochMs.clone();
		}
		drawTimeAxis(g, times, h);

		if (displayMarker) {
			g.setColor(Color.gray);
			g.drawLine(displayMarkerX, 0, displayMarkerX, h);
			double age = WaterfallTimeScale.ageAtY(times, h, displayMarkerY);
			g.drawString(String.format("%.1f MHz  %s", displayMarkerFrequency / 1000000.0,
					WaterfallTimeScale.formatAge(age)), displayMarkerX + 5, Math.max(14, displayMarkerY - 6));
		} 

		long drawingTime	= System.nanoTime()-drawStart;
		drawingTimeSum	+= drawingTime;
		drawingCounter++;
	}
	private volatile long drawingTimeSum	= 0;
	private volatile int drawingCounter	= 0;
	public int getDrawingCounterAndReset() {
		int val	= drawingCounter;
		drawingCounter	= 0;
		return val;
	}
	/**
	 * Retrieves time in nanos the component spent in drawing itself and resets
	 * the counter to zero.
	 * @return
	 */
	public long getDrawTimeSumAndReset() {
		long val	= drawingTimeSum;
		drawingTimeSum	= 0;
		return val;
	}
}
