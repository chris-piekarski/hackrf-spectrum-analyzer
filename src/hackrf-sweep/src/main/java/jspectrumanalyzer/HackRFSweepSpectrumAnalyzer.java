package jspectrumanalyzer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.Align;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;

import jspectrumanalyzer.capture.ScreenCapture;
import jspectrumanalyzer.core.DatasetSpectrumPeak;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyAllocations;
import jspectrumanalyzer.core.FrequencyBand;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.GainPolicy;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.PersistentDisplay;
import jspectrumanalyzer.core.RuntimePerformanceWatch;
import jspectrumanalyzer.core.SpectrumSweepEngine;
import jspectrumanalyzer.core.SpurFilter;
import jspectrumanalyzer.core.jfc.XYSeriesCollectionImmutable;
import jspectrumanalyzer.nativebridge.HackRFSweepDataCallback;
import jspectrumanalyzer.nativebridge.HackRFSweepNativeBridge;
import jspectrumanalyzer.ui.HackRFSweepSettingsUI;
import jspectrumanalyzer.ui.WaterfallPlot;
import shared.mvc.MVCController;
import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

public class HackRFSweepSpectrumAnalyzer implements HackRFSettings, HackRFSweepDataCallback {

	/**
	 * Color palette for UI
	 */
	protected static class ColorScheme {
		Color	palette0	= Color.white;
		Color	palette1	= new Color(0xe5e5e5);
		Color	palette2	= new Color(0xFCA311);
		Color	palette3	= new Color(0x14213D);
		Color	palette4	= Color.BLACK;
	}

	public static final int	SPECTRUM_PALETTE_SIZE_MIN	= 5;
	private static boolean	captureGIF					= false;

	private static long		initTime					= System.currentTimeMillis();

	public static void main(String[] args) throws IOException {
		//		System.out.println(new File("").getAbsolutePath());
		if (args.length > 0) {
			if (args[0].equals("capturegif")) {
				captureGIF = true;
			}
		}
		//		try { Thread.sleep(20000); System.out.println("Started..."); } catch (InterruptedException e) {}

		jspectrumanalyzer.ui.AnalyzerLookAndFeel.install();
		new HackRFSweepSpectrumAnalyzer();
	}

	public boolean									flagIsHWSendingData						= false;
	private float									alphaFreqAllocationTableBandsImage	= 0.5f;
	private float									alphaPersistentDisplayImage			= 1.0f;
	private JFreeChart								chart;

	private ModelValue<Rectangle2D>					chartDataArea						= new ModelValue<Rectangle2D>(
			"Chart data area", new Rectangle2D.Double(0, 0, 1, 1));
	private XYSeriesCollectionImmutable				chartDataset								= new XYSeriesCollectionImmutable();
	private XYLineAndShapeRenderer					chartLineRenderer;
	private ChartPanel								chartPanel;
	private ColorScheme								colors								= new ColorScheme();
	private DatasetSpectrumPeak						datasetSpectrum;
	private volatile boolean						flagManualGain						= false;
	private volatile boolean						forceStopSweep						= false;
	/**
	 * Capture a GIF of the program for the GITHUB page
	 */
	private ScreenCapture							gifCap								= null;
	private ArrayList<HackRFEventListener>			hRFlisteners							= new ArrayList<>();
	private BufferedImage							imageFrequencyAllocationTableBands	= null;
	private boolean											isChartDrawing						= false;
	private ReentrantLock							lock								= new ReentrantLock();

	private ModelValueBoolean						parameterAntPower					= new ModelValueBoolean(
			"Ant power", false);
	private ModelValueBoolean						parameterAntennaLNA					= new ModelValueBoolean(
			"Antenna LNA +14dB", false);
	private ModelValueInt							parameterFFTBinHz					= new ModelValueInt(
			"FFT Bin [Hz]", 100000);
	private ModelValueBoolean						parameterFilterSpectrum				= new ModelValueBoolean(
			"Filter", false);
	private ModelValue<FrequencyRange>				parameterFrequency					= new ModelValue<>(
			"Frequency range", new FrequencyRange(2400, 2500));

	private ModelValue<FrequencyAllocationTable>	parameterFrequencyAllocationTable	= new ModelValue<FrequencyAllocationTable>(
			"Frequency allocation table", null);

	private ModelValueInt							parameterGainLNA					= new ModelValueInt("LNA Gain",
			0, 8, 0, 40);
	private ModelValueInt							parameterGainTotal					= new ModelValueInt("Gain [dB]",
			40);
	private ModelValueInt							parameterGainVGA					= new ModelValueInt("VGA Gain",
			0, 2, 0, 60);
	private ModelValueBoolean						parameterIsCapturingPaused			= new ModelValueBoolean(
			"Capturing paused", false);

	private ModelValueInt							parameterPersistentDisplayPersTime 		= new ModelValueInt("Persistence time", 30, 1, 1, 60);
	private ModelValueInt							parameterPeakFallRateSecs			= new ModelValueInt(
			"Peak fall rate", 15);
	private ModelValueBoolean						parameterPersistentDisplay			= new ModelValueBoolean(
			"Persistent display", true);

	private ModelValueInt							parameterSamples					= new ModelValueInt("Samples",
			8192);

	private ModelValueBoolean						parameterShowPeaks					= new ModelValueBoolean(
			"Show peaks", true);

	private ModelValueBoolean 						parameterDebugDisplay				= new ModelValueBoolean("Debug", false);
	
	private ModelValue<BigDecimal>					parameterSpectrumLineThickness		= new ModelValue<>(
			"Spectrum line thickness", new BigDecimal("1"));
	private ModelValueInt							parameterSpectrumPaletteSize		= new ModelValueInt(
			"Spectrum palette size", 0);
	private ModelValueInt							parameterSpectrumPaletteStart		= new ModelValueInt(
			"Spectrum palette start", 0);

	private ModelValueBoolean						parameterSpurRemoval				= new ModelValueBoolean(
			"Spur removal", false);
	private ModelValueBoolean						parameterWaterfallVisible			= new ModelValueBoolean(
			"Waterfall visible", true);
	private PersistentDisplay						persistentDisplay					= new PersistentDisplay();
	private float									spectrumInitValue					= -150;
	private SpurFilter								spurFilter;
	private SpectrumSweepEngine						sweepEngine;
	private Thread									threadHackrfSweep;
	private ArrayBlockingQueue<Integer>				threadLaunchCommands				= new ArrayBlockingQueue<>(1);
	private Thread									threadLauncher;
	private Thread									threadProcessing;
	private TextTitle								titleFreqBand						= new TextTitle("",
			new Font("Dialog", Font.PLAIN, 11));
	private RuntimePerformanceWatch					perfWatch							= new RuntimePerformanceWatch();
	private JFrame									uiFrame;
	private ValueMarker								waterfallPaletteEndMarker;
	private ValueMarker								waterfallPaletteStartMarker;
	private WaterfallPlot							waterfallPlot;
	private JLabel labelMessages;

	public HackRFSweepSpectrumAnalyzer() {
		jspectrumanalyzer.ui.AnalyzerLookAndFeel.install();
		printInit(0);

		if (captureGIF) {
//			parameterFrequency.setValue(new FrequencyRange(700, 2700));
			parameterFrequency.setValue(new FrequencyRange(2400, 2700));
			parameterGainTotal.setValue(60);
			parameterSpurRemoval.setValue(true);
			parameterPersistentDisplay.setValue(true);
			parameterFFTBinHz.setValue(500000);
			parameterFrequencyAllocationTable.setValue(new FrequencyAllocations().getTable().values().stream().findFirst().get());
		}

		recalculateGains(parameterGainTotal.getValue());

		setupChart();

		setupChartMouseMarkers();

		waterfallPlot = new WaterfallPlot(chartPanel, 300);
		waterfallPaletteStartMarker = new ValueMarker(waterfallPlot.getSpectrumPaletteStart(), colors.palette2,
				new BasicStroke(1f));
		waterfallPaletteEndMarker = new ValueMarker(
				waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize(), colors.palette2,
				new BasicStroke(1f));
		//		chart.getXYPlot().addRangeMarker(waterfallPaletteStartMarker);
		//		chart.getXYPlot().addRangeMarker(waterfallPaletteEndMarker);

		printInit(2);

		HackRFSweepSettingsUI settingsPanel = new HackRFSweepSettingsUI(this);

		printInit(3);
		
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, waterfallPlot);
		splitPane.setResizeWeight(0.8);
		splitPane.setBorder(null);

		labelMessages = new JLabel("dsadasd");
		labelMessages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		parameterDebugDisplay.addListener((debug) -> {
			labelMessages.setVisible(debug);
		});
		parameterDebugDisplay.callObservers();
		
		JPanel splitPanePanel	= new JPanel(new BorderLayout());
		splitPanePanel.add(splitPane, BorderLayout.CENTER);
		splitPanePanel.add(labelMessages, BorderLayout.SOUTH);

		uiFrame = new JFrame();
		uiFrame.setUndecorated(captureGIF);
		uiFrame.setExtendedState(uiFrame.getExtendedState() | Frame.MAXIMIZED_BOTH);
		uiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		uiFrame.setLayout(new BorderLayout());
		uiFrame.setTitle("Spectrum Analyzer - hackrf_sweep");
		uiFrame.add(splitPanePanel, BorderLayout.CENTER);
		uiFrame.setMinimumSize(new Dimension(600, 600));
		uiFrame.add(settingsPanel, BorderLayout.EAST);
		try {
			uiFrame.setIconImage(new ImageIcon("program.png").getImage());
		} catch (Exception e) {
			//			e.printStackTrace();
		}
		
		printInit(4);
		setupFrequencyAllocationTable();
		printInit(5);
		
		uiFrame.pack();
		uiFrame.setVisible(true);

		printInit(6);

		sweepEngine = new SpectrumSweepEngine(this, spectrumInitValue, new SweepUiHooks());
		startLauncherThread();
		restartHackrfSweep();

		/**
		 * register parameter observers
		 */
		setupParameterObservers();

		//shutdown on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHackrfSweep()));

		if (captureGIF) {
			try {
				gifCap = new ScreenCapture(uiFrame, 35 * 1, 10, 5, 760, 660, new File("screenshot.gif"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public ModelValueBoolean getAntennaPowerEnable() {
		return parameterAntPower;
	}

	@Override
	public ModelValueBoolean getAntennaLNA() {
		return parameterAntennaLNA;
	}

	@Override
	public ModelValueInt getFFTBinHz() {
		return parameterFFTBinHz;
	}

	@Override
	public ModelValue<FrequencyRange> getFrequency() {
		return parameterFrequency;
	}

	@Override
	public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable() {
		return parameterFrequencyAllocationTable;
	}

	@Override
	public ModelValueInt getGain() {
		return parameterGainTotal;
	}

	@Override
	public ModelValueInt getGainLNA() {
		return parameterGainLNA;
	}

	@Override
	public ModelValueInt getGainVGA() {
		return parameterGainVGA;
	}

	@Override
	public ModelValueInt getPeakFallRate() {
		return parameterPeakFallRateSecs;
	}

	@Override
	public ModelValueInt getSamples() {
		return parameterSamples;
	}

	@Override
	public ModelValue<BigDecimal> getSpectrumLineThickness() {
		return parameterSpectrumLineThickness;
	}
	
	@Override
	public ModelValueInt getPersistentDisplayDecayRate() {
		return parameterPersistentDisplayPersTime;
	}

	@Override
	public ModelValueInt getSpectrumPaletteSize() {
		return parameterSpectrumPaletteSize;
	}

	@Override
	public ModelValueInt getSpectrumPaletteStart() {
		return parameterSpectrumPaletteStart;
	}

	@Override
	public ModelValueBoolean isCapturingPaused() {
		return parameterIsCapturingPaused;
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible() {
		return parameterShowPeaks;
	}
	
	@Override
	public ModelValueBoolean isDebugDisplay() {
		return parameterDebugDisplay;
	}

	@Override
	public ModelValueBoolean isFilterSpectrum() {
		return parameterFilterSpectrum;
	}

	@Override
	public ModelValueBoolean isPersistentDisplayVisible() {
		return parameterPersistentDisplay;
	}

	@Override
	public ModelValueBoolean isSpurRemoval() {
		return this.parameterSpurRemoval;
	}

	@Override
	public ModelValueBoolean isWaterfallVisible() {
		return parameterWaterfallVisible;
	}

	@Override
	public void newSpectrumData(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz,
			float[] signalPowerdBm) {
		fireHardwareStateChanged(true);
		if (sweepEngine != null)
			sweepEngine.accept(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm);
	}

	@Override
	public void registerListener(HackRFEventListener listener) {
		hRFlisteners.add(listener);
	}

	@Override
	public void removeListener(HackRFEventListener listener) {
		hRFlisteners.remove(listener);
	}

	private void fireCapturingStateChanged() {
		SwingUtilities.invokeLater(() -> {
			synchronized (hRFlisteners) {
				for (HackRFEventListener hackRFEventListener : hRFlisteners) {
					try {
						hackRFEventListener.captureStateChanged(!parameterIsCapturingPaused.getValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void fireHardwareStateChanged(boolean sendingData) {
		if (this.flagIsHWSendingData != sendingData) {
			this.flagIsHWSendingData = sendingData;
			SwingUtilities.invokeLater(() -> {
				synchronized (hRFlisteners) {
					for (HackRFEventListener hackRFEventListener : hRFlisteners) {
						try {
							hackRFEventListener.hardwareStatusChanged(sendingData);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
	}

	private FrequencyRange getFreq() {
		return parameterFrequency.getValue();
	}

	private void printInit(int initNumber) {
		//		System.out.println("Startup "+(initNumber++)+" in " + (System.currentTimeMillis() - initTime) + "ms");
	}

	private final class SweepUiHooks implements SpectrumSweepEngine.Hooks {
		private long lastChartUpdated = System.currentTimeMillis();
		private long frameCounterChart = 0;
		private final int limitChartRefreshFPS = 30;
		private final int limitPersistentRefreshEveryChartFrame = 2;
		private final XYSeries spectrumPeaksEmpty = new XYSeries("peaks");

		@Override
		public void onPacketAccepted() {
			fireHardwareStateChanged(true);
		}

		@Override
		public void onFirstDataset(DatasetSpectrumPeak ds, float fftBinHz) {
			datasetSpectrum = ds;
			spurFilter = sweepEngine.getSpurFilter();
			chart.getXYPlot().getDomainAxis().setRange(getFreq().getStartMHz(), getFreq().getEndMHz());
		}

		@Override
		public void onFullSweepProcessed(DatasetSpectrumPeak ds) {
			datasetSpectrum = ds;
			if (parameterShowPeaks.getValue()) {
				waterfallPlot.setStatusMessage(String.format("Total Spectrum Peak Power %.1fdBm",
						datasetSpectrum.calculateSpectrumPeakPower()), 0);
			}
			if (System.currentTimeMillis() - perfWatch.lastStatisticsRefreshed > 1000) {
				synchronized (perfWatch) {
					perfWatch.waterfallDraw.nanosSum = waterfallPlot.getDrawTimeSumAndReset();
					perfWatch.waterfallDraw.count = waterfallPlot.getDrawingCounterAndReset();
					String stats = perfWatch.generateStatistics();
					SwingUtilities.invokeLater(() -> {
						labelMessages.setText(stats);
					});
					perfWatch.reset();
				}
			}

			boolean flagChartRedraw = false;
			if (System.currentTimeMillis() - lastChartUpdated > 1000 / limitChartRefreshFPS) {
				flagChartRedraw = true;
				frameCounterChart++;
				lastChartUpdated = System.currentTimeMillis();
			}

			XYSeries spectrumSeries = datasetSpectrum.createSpectrumDataset("spectrum");
			XYSeries spectrumPeaks = parameterShowPeaks.getValue() ? datasetSpectrum.createPeaksDataset("peaks")
					: spectrumPeaksEmpty;

			if (parameterPersistentDisplay.getValue()) {
				long start = System.nanoTime();
				boolean redraw = flagChartRedraw && frameCounterChart % limitPersistentRefreshEveryChartFrame == 0;
				persistentDisplay.drawSpectrum2(datasetSpectrum,
						(float) chart.getXYPlot().getRangeAxis().getRange().getLowerBound(),
						(float) chart.getXYPlot().getRangeAxis().getRange().getUpperBound(), redraw);
				synchronized (perfWatch) {
					perfWatch.persisentDisplay.addDrawingTime(System.nanoTime() - start);
				}
			}

			if (parameterWaterfallVisible.getValue()) {
				long start = System.nanoTime();
				waterfallPlot.addNewData(datasetSpectrum);
				synchronized (perfWatch) {
					perfWatch.waterfallUpdate.addDrawingTime(System.nanoTime() - start);
				}
			}

			if (flagChartRedraw) {
				if (parameterWaterfallVisible.getValue()) {
					waterfallPlot.repaint();
				}
				SwingUtilities.invokeLater(() -> {
					chart.setNotify(false);
					chartDataset.removeAllSeries();
					chartDataset.addSeries(spectrumPeaks);
					chartDataset.addSeries(spectrumSeries);
					chart.setNotify(true);
					if (gifCap != null) {
						gifCap.captureFrame();
					}
				});
			}

			synchronized (perfWatch) {
				perfWatch.hwFullSpectrumRefreshes++;
			}
		}
	}

	private void recalculateGains(int totalGain) {
		int lnaGain = GainPolicy.lnaGain(totalGain);
		int vgaGain = GainPolicy.vgaGain(totalGain);
		this.parameterGainLNA.setValue(lnaGain);
		this.parameterGainVGA.setValue(vgaGain);
		this.parameterGainTotal.setValue(lnaGain + vgaGain);
	}

	/**
	 * uses fifo queue to process launch commands, only the last launch command
	 * is important, delete others
	 */
	private synchronized void restartHackrfSweep() {
		if (threadLaunchCommands.offer(0) == false) {
			threadLaunchCommands.clear();
			threadLaunchCommands.offer(0);
		}
	}

	/**
	 * no need to synchronize, executes only in the launcher thread
	 */
	private void restartHackrfSweepExecute() {
		stopHackrfSweep();
		threadHackrfSweep = new Thread(() -> {
			Thread.currentThread().setName("hackrf_sweep");
			try {
				forceStopSweep = false;
				if (sweepEngine != null)
					sweepEngine.clearStop();
				sweep();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		threadHackrfSweep.start();
	}

	private void setupChart() {
		int axisWidthLeft = 70;
		int axisWidthRight = 20;

		chart = ChartFactory.createXYLineChart("Spectrum analyzer", "Frequency [MHz]", "Power [dB]", chartDataset,
				PlotOrientation.VERTICAL, false, false, false);
		chart.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		XYPlot plot = chart.getXYPlot();
		NumberAxis domainAxis = ((NumberAxis) plot.getDomainAxis());
		NumberAxis rangeAxis = ((NumberAxis) plot.getRangeAxis());
		chartLineRenderer = new XYLineAndShapeRenderer();
		chartLineRenderer.setDefaultShapesVisible(false);
		chartLineRenderer.setDefaultStroke(new BasicStroke(parameterSpectrumLineThickness.getValue().floatValue()));

		rangeAxis.setAutoRange(false);
		rangeAxis.setRange(-110, 20);
		rangeAxis.setTickUnit(new NumberTickUnit(10, new DecimalFormat("###")));

		domainAxis.setNumberFormatOverride(new DecimalFormat(" #.### "));

		chartLineRenderer.setAutoPopulateSeriesStroke(false);
		chartLineRenderer.setAutoPopulateSeriesPaint(false);
		chartLineRenderer.setSeriesPaint(0, colors.palette2);

		if (false)
			chart.addProgressListener(new ChartProgressListener() {
				StandardTickUnitSource tus = new StandardTickUnitSource();

				@Override
				public void chartProgress(ChartProgressEvent event) {
					if (event.getType() == ChartProgressEvent.DRAWING_STARTED) {
						Range r = domainAxis.getRange();
						domainAxis.setTickUnit((NumberTickUnit) tus.getCeilingTickUnit(r.getLength() / 20));
						domainAxis.setMinorTickCount(2);
						domainAxis.setMinorTickMarksVisible(true);

					}
				}
			});

		plot.setDomainGridlinesVisible(false);
		plot.setRenderer(chartLineRenderer);

		/**
		 * sets empty space around the plot
		 */
		AxisSpace axisSpace = new AxisSpace();
		axisSpace.setLeft(axisWidthLeft);
		axisSpace.setRight(axisWidthRight);
		axisSpace.setTop(0);
		axisSpace.setBottom(50);
		plot.setFixedDomainAxisSpace(axisSpace);//sets width of the domain axis left/right
		plot.setFixedRangeAxisSpace(axisSpace);//sets heigth of range axis top/bottom

		rangeAxis.setAxisLineVisible(false);
		rangeAxis.setTickMarksVisible(false);

		plot.setAxisOffset(RectangleInsets.ZERO_INSETS); //no space between range axis and plot

		Font labelFont = new Font(Font.MONOSPACED, Font.BOLD, 16);
		rangeAxis.setLabelFont(labelFont);
		rangeAxis.setTickLabelFont(labelFont);
		rangeAxis.setLabelPaint(colors.palette1);
		rangeAxis.setTickLabelPaint(colors.palette1);
		domainAxis.setLabelFont(labelFont);
		domainAxis.setTickLabelFont(labelFont);
		domainAxis.setLabelPaint(colors.palette1);
		domainAxis.setTickLabelPaint(colors.palette1);
		chartLineRenderer.setDefaultPaint(Color.white);
		plot.setBackgroundPaint(colors.palette4);
		chart.setBackgroundPaint(colors.palette4);
		chartLineRenderer.setSeriesPaint(1, colors.palette1);

		chartPanel = new ChartPanel(chart);
		chartPanel.setMaximumDrawWidth(4096);
		chartPanel.setMaximumDrawHeight(2160);
		chartPanel.setMouseWheelEnabled(false);
		chartPanel.setDomainZoomable(false);
		chartPanel.setRangeZoomable(false);
		chartPanel.setPopupMenu(null);
		chartPanel.setMinimumSize(new Dimension(200, 200));

		printInit(1);

		/**
		 * Draws overlay of waterfall's color scale next to main spectrum chart
		 * to show
		 */
		chartPanel.addOverlay(new Overlay() {
			@Override
			public void addChangeListener(OverlayChangeListener listener) {
			}

			@Override
			public void paintOverlay(Graphics2D g, ChartPanel chartPanel) {
				Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				int plotStartX = (int) area.getX();
				int plotWidth = (int) area.getWidth();

				Rectangle2D subplotArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();

				int y1 = (int) plot.getRangeAxis().valueToJava2D(waterfallPlot.getSpectrumPaletteStart(), subplotArea,
						plot.getRangeAxisEdge());
				int y2 = (int) plot.getRangeAxis().valueToJava2D(
						waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize(), subplotArea,
						plot.getRangeAxisEdge());

				int x = plotStartX + plotWidth;
				int w = 15;
				int h = y1 - y2;
				waterfallPlot.drawScale(g, x, y2, w, h);
			}

			@Override
			public void removeChangeListener(OverlayChangeListener listener) {
			}
		});

		/**
		 * Draw frequency bands as an overlay
		 */
		if (true)
		chartPanel.addOverlay(new Overlay() {
			@Override
			public void addChangeListener(OverlayChangeListener listener) {
			}

			@Override
			public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
				BufferedImage img = imageFrequencyAllocationTableBands;
				if (img != null) {
					Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
					g2.drawImage(img, (int) area.getX(), (int) area.getY(), null);
				}
			}

			@Override
			public void removeChangeListener(OverlayChangeListener listener) {
			}
		});

		/**
		 * monitors chart data area for change due to no other way to extract
		 * that info from jfreechart when it changes
		 */
		chart.addChangeListener(event -> {
			Rectangle2D aN = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
			Rectangle2D aO = chartDataArea.getValue();
			if (aO.getX() != aN.getX() || aO.getY() != aN.getY() || aO.getWidth() != aN.getWidth()
					|| aO.getHeight() != aN.getHeight()) {
				chartDataArea.setValue(new Rectangle2D.Double(aN.getX(), aN.getY(), aN.getWidth(), aN.getHeight()));
			}
		});

		chart.addProgressListener(new ChartProgressListener() {
			private long chartRedrawStarted;

			@Override
			public void chartProgress(ChartProgressEvent arg0) {
				if (arg0.getType() == ChartProgressEvent.DRAWING_STARTED) {
					chartRedrawStarted = System.nanoTime();
				} else if (arg0.getType() == ChartProgressEvent.DRAWING_FINISHED) {
					synchronized (perfWatch) {
						perfWatch.chartDrawing.addDrawingTime(System.nanoTime() - chartRedrawStarted);
					}
				}
			}
		});
		
		
	}

	/**
	 * Displays a cross marker with current frequency and signal strength when
	 * mouse hovers over the frequency chart
	 */
	private void setupChartMouseMarkers() {
		ValueMarker freqMarker = new ValueMarker(0, Color.WHITE, new BasicStroke(1f));
		freqMarker.setLabelPaint(Color.white);
		freqMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		freqMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
		freqMarker.setLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
		ValueMarker signalMarker = new ValueMarker(0, Color.WHITE, new BasicStroke(1f));
		signalMarker.setLabelPaint(Color.white);
		signalMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		signalMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
		signalMarker.setLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 16));

		chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
			DecimalFormat format = new DecimalFormat("0.#");

			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();

				XYPlot plot = chart.getXYPlot();
				Rectangle2D subplotArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				double crosshairRange = plot.getRangeAxis().java2DToValue(y, subplotArea, plot.getRangeAxisEdge());
				signalMarker.setValue(crosshairRange);
				signalMarker.setLabel(String.format("%.1fdB", crosshairRange));
				double crosshairDomain = plot.getDomainAxis().java2DToValue(x, subplotArea, plot.getDomainAxisEdge());
				freqMarker.setValue(crosshairDomain);
				freqMarker.setLabel(String.format("%.1fMHz", crosshairDomain));

				FrequencyAllocationTable activeTable = parameterFrequencyAllocationTable.getValue();
				if (activeTable != null) {
					FrequencyBand band = activeTable.lookupBand((long) (crosshairDomain * 1000000l));
					if (band == null)
						titleFreqBand.setText(" ");
					else {
						titleFreqBand.setText(String.format("%s - %s MHz  %s", format.format(band.getMHzStartIncl()),
								format.format(band.getMHzEndExcl()), band.getApplications().replaceAll("/", " / ")));
					}
				}
			}
		});
		chartPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				chart.getXYPlot().clearDomainMarkers();
				chart.getXYPlot().clearRangeMarkers();
				chart.getXYPlot().addRangeMarker(signalMarker);
				chart.getXYPlot().addDomainMarker(freqMarker);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				chart.getXYPlot().clearDomainMarkers();
				chart.getXYPlot().clearRangeMarkers();
				titleFreqBand.setText(" ");
			}
		});

		titleFreqBand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		titleFreqBand.setPosition(RectangleEdge.BOTTOM);
		titleFreqBand.setHorizontalAlignment(HorizontalAlignment.LEFT);
		titleFreqBand.setMargin(0.0, 2.0, 0.0, 2.0);
		titleFreqBand.setPaint(Color.white);
		chart.addSubtitle(titleFreqBand);
	}

	private void setupFrequencyAllocationTable() {
		SwingUtilities.invokeLater(() -> {
			chartPanel.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					redrawFrequencySpectrumTable();
				}
			});
			chart.getXYPlot().getDomainAxis().addChangeListener((e) -> {
				redrawFrequencySpectrumTable();
			});
			chart.getXYPlot().getRangeAxis().addChangeListener(event -> {
				redrawFrequencySpectrumTable();
				System.out.println(event);
			});

		});
		parameterFrequencyAllocationTable.addListener(this::redrawFrequencySpectrumTable);
	}

	private void setupParameterObservers() {
		Runnable restartHackrf = this::restartHackrfSweep;
		parameterFrequency.addListener(restartHackrf);
		parameterAntPower.addListener(restartHackrf);
		parameterAntennaLNA.addListener(restartHackrf);
		parameterFFTBinHz.addListener(restartHackrf);
		parameterSamples.addListener(restartHackrf);
		parameterIsCapturingPaused.addListener(this::fireCapturingStateChanged);

		parameterGainTotal.addListener((gainTotal) -> {
			if (flagManualGain) //flag is being adjusted manually by LNA or VGA, do not recalculate the gains
				return;
			recalculateGains(gainTotal);
			restartHackrfSweep();
		});
		Runnable gainRecalc = () -> {
			int totalGain = parameterGainLNA.getValue() + parameterGainVGA.getValue();
			flagManualGain = true;
			try {
				parameterGainTotal.setValue(totalGain);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				flagManualGain = false;
			}
			restartHackrfSweep();
		};
		parameterGainLNA.addListener(gainRecalc);
		parameterGainVGA.addListener(gainRecalc);

		parameterSpurRemoval.addListener(() -> {
			SpurFilter filter = spurFilter;
			if (filter != null) {
				filter.recalibrate();
			}
		});
		parameterShowPeaks.addListener(() -> {
			DatasetSpectrumPeak p = datasetSpectrum;
			if (p != null) {
				p.resetPeaks();
			}
		});
		parameterSpectrumPaletteStart.setValue((int) waterfallPlot.getSpectrumPaletteStart());
		parameterSpectrumPaletteSize.setValue((int) waterfallPlot.getSpectrumPaletteSize());
		parameterSpectrumPaletteStart.addListener((dB) -> {
			waterfallPlot.setSpectrumPaletteStart(dB);
			SwingUtilities.invokeLater(() -> {
				waterfallPaletteStartMarker.setValue(waterfallPlot.getSpectrumPaletteStart());
				waterfallPaletteEndMarker
						.setValue(waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize());
			});
		});
		parameterSpectrumPaletteSize.addListener((dB) -> {
			if (dB < SPECTRUM_PALETTE_SIZE_MIN)
				return;
			waterfallPlot.setSpectrumPaletteSize(dB);
			SwingUtilities.invokeLater(() -> {
				waterfallPaletteStartMarker.setValue(waterfallPlot.getSpectrumPaletteStart());
				waterfallPaletteEndMarker
						.setValue(waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize());
			});

		});
		parameterPeakFallRateSecs.addListener((fallRate) -> {
			datasetSpectrum.setPeakFalloutMillis(fallRate * 1000l);
		});

		parameterSpectrumLineThickness.addListener((thickness) -> {
			SwingUtilities.invokeLater(() -> chartLineRenderer.setDefaultStroke(new BasicStroke(thickness.floatValue())));
		});
		
		parameterPersistentDisplayPersTime.addListener((time) -> {
			persistentDisplay.setPersistenceTime(time);
		});

		int persistentDisplayDownscaleFactor = 4;

		Runnable resetPersistentImage = () -> {
			boolean display = parameterPersistentDisplay.getValue();
			persistentDisplay.reset();
			chart.getXYPlot().setBackgroundImage(display ? persistentDisplay.getDisplayImage().getValue() : null);
			chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
		};
		persistentDisplay.getDisplayImage().addListener((image) -> {
			if (parameterPersistentDisplay.getValue())
				chart.getXYPlot().setBackgroundImage(image);
		});

		registerListener(new HackRFEventAdapter() {
			@Override
			public void hardwareStatusChanged(boolean hardwareSendingData) {
				SwingUtilities.invokeLater(() -> {
					if (hardwareSendingData && parameterPersistentDisplay.getValue()) {
						resetPersistentImage.run();
					}
				});
			}
		});

		parameterPersistentDisplay.addListener((display) -> {
			SwingUtilities.invokeLater(resetPersistentImage::run);
		});

		chartDataArea.addListener((area) -> {
			SwingUtilities.invokeLater(() -> {
				/*
				 * Align the waterfall plot and the spectrum chart
				 */
				if (waterfallPlot != null)
					waterfallPlot.setDrawingOffsets((int) area.getX(), (int) area.getWidth());

				/**
				 * persistent display config
				 */
				persistentDisplay.setImageSize((int) area.getWidth() / persistentDisplayDownscaleFactor,
						(int) area.getWidth() / persistentDisplayDownscaleFactor);
				if (parameterPersistentDisplay.getValue()) {
					chart.getXYPlot().setBackgroundImage(persistentDisplay.getDisplayImage().getValue());
					chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
				}
			});
		});
	}

	private void startLauncherThread() {
		threadLauncher = new Thread(() -> {
			Thread.currentThread().setName("Launcher-thread");
			while (true) {
				try {
					threadLaunchCommands.take();
					restartHackrfSweepExecute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		threadLauncher.start();
	}

	/**
	 * no need to synchronize, executes only in launcher thread
	 */
	private void stopHackrfSweep() {
		forceStopSweep = true;
		if (sweepEngine != null)
			sweepEngine.requestStop();
		if (threadHackrfSweep != null) {
			while (threadHackrfSweep.isAlive()) {
				forceStopSweep = true;
				//				System.out.println("Calling HackRFSweepNativeBridge.stop()");
				HackRFSweepNativeBridge.stop();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
			try {
				threadHackrfSweep.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threadHackrfSweep = null;
		}
		System.out.println("HackRFSweep thread stopped.");
		if (threadProcessing != null) {
			threadProcessing.interrupt();
			try {
				threadProcessing.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			threadProcessing = null;
			System.out.println("Processing thread stopped.");
		}
	}

	private void sweep() throws IOException {
		lock.lock();
		try {
			threadProcessing = new Thread(() -> {
				Thread.currentThread().setName("hackrf_sweep data processing thread");
				sweepEngine.runProcessingLoop();
			});
			threadProcessing.start();

			System.out.println(
					"Starting hackrf_sweep... " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz() + "MHz ");
			System.out.println("hackrf_sweep params:  freq " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz()
					+ "MHz  FFTBin " + parameterFFTBinHz.getValue() + "Hz  samples " + parameterSamples.getValue()
					+ "  lna: " + parameterGainLNA.getValue() + " vga: " + parameterGainVGA.getValue() + " antPwr:"
					+ parameterAntPower.getValue() + " antLNA:" + parameterAntennaLNA.getValue());
			fireHardwareStateChanged(false);
			sweepEngine.runSweepLoop();
			fireHardwareStateChanged(false);
		} finally {
			lock.unlock();
			fireHardwareStateChanged(false);
		}
	}

	protected void redrawFrequencySpectrumTable() {
		Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
		FrequencyAllocationTable activeTable = parameterFrequencyAllocationTable.getValue();
		if (activeTable == null) {
			imageFrequencyAllocationTableBands = null;
		} else if (area.getWidth() > 0 && area.getHeight() > 0) {
			imageFrequencyAllocationTableBands = activeTable.drawAllocationTable((int) area.getWidth(),
					(int) area.getHeight(), alphaFreqAllocationTableBandsImage, getFreq().getStartMHz() * 1000000l,
					getFreq().getEndMHz() * 1000000l,
					//colors.palette4, 
					Color.white,
					//colors.palette1
					Color.DARK_GRAY);
		}
	}
}
