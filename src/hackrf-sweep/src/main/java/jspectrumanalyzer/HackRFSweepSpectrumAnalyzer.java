package jspectrumanalyzer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
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
import jspectrumanalyzer.core.FmChannelPlan;
import jspectrumanalyzer.core.FmStationHit;
import jspectrumanalyzer.core.FmStationTracker;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyAllocations;
import jspectrumanalyzer.core.FrequencyBand;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.GainPolicy;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.PersistentDisplay;
import jspectrumanalyzer.core.RadioIdentity;
import jspectrumanalyzer.core.RuntimePerformanceWatch;
import jspectrumanalyzer.core.SpectrumPowerScale;
import jspectrumanalyzer.core.SpectrumSweepEngine;
import jspectrumanalyzer.core.SpectrumZoom;
import jspectrumanalyzer.core.SpectrumZoomHistory;
import jspectrumanalyzer.core.SpurFilter;
import jspectrumanalyzer.core.WifiChannelPlan;
import jspectrumanalyzer.core.jfc.XYSeriesCollectionImmutable;
import jspectrumanalyzer.nativebridge.HackRFDeviceQuery;
import jspectrumanalyzer.nativebridge.HackRFSweepDataCallback;
import jspectrumanalyzer.nativebridge.HackRFSweepNativeBridge;
import jspectrumanalyzer.ui.HackRFSweepSettingsUI;
import jspectrumanalyzer.ui.SweepStatusBar;
import jspectrumanalyzer.ui.WaterfallPlot;
import jspectrumanalyzer.ui.FmChannelOverlay;
import jspectrumanalyzer.ui.QuickSelectBandOverlay;
import jspectrumanalyzer.ui.SpectrumZoomOverlay;
import jspectrumanalyzer.ui.WifiChannelOverlay;
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
			"Frequency range", new FrequencyRange(WifiChannelPlan.WIFI_24_VIEW_START_MHZ,
					WifiChannelPlan.WIFI_24_VIEW_END_MHZ));
	private volatile List<FmStationHit>				fmStations							= List.of();
	private final FmStationTracker					fmTracker							= new FmStationTracker();
	private final SpectrumZoomHistory				spectrumZoomHistory					= new SpectrumZoomHistory();
	private final SpectrumZoomOverlay				spectrumZoomOverlay					= new SpectrumZoomOverlay();
	private boolean									applyingSpectrumZoom;
	private boolean									spectrumZoomDragging;
	private int										spectrumZoomAnchorX;

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
	private ModelValue<RadioIdentity>				parameterRadioIdentity				= new ModelValue<>(
			"Radio", RadioIdentity.ABSENT);
	private ModelValue<String>						parameterSelectedSerial				= new ModelValue<>(
			"Serial", "");
	private ModelValueBoolean						parameterClkoutEnable				= new ModelValueBoolean(
			"CLKOUT", false);
	private ModelValueBoolean						parameterRadioReleased				= new ModelValueBoolean(
			"Radio released", false);

	private ModelValueInt							parameterPersistentDisplayPersTime 		= new ModelValueInt("Persistence time", 30, 1, 1, 60);
	private ModelValueInt							parameterPeakFallRateSecs			= new ModelValueInt(
			"Peak fall rate", 15);
	private ModelValueBoolean						parameterPersistentDisplay			= new ModelValueBoolean(
			"Persistent display", true);

	private ModelValueInt							parameterSamples					= new ModelValueInt("Samples",
			8192);

	private ModelValueBoolean						parameterShowPeaks					= new ModelValueBoolean(
			"Show peaks", true);
	private ModelValueBoolean						parameterPowerAutoScale				= new ModelValueBoolean(
			"Auto-scale dB axis", false);

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
	private SweepStatusBar sweepStatusBar;

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
		setupSpectrumZoom();

		waterfallPlot = new WaterfallPlot(chartPanel, 300);
		waterfallPaletteStartMarker = new ValueMarker(waterfallPlot.getSpectrumPaletteStart(), colors.palette2,
				new BasicStroke(1f));
		waterfallPaletteEndMarker = new ValueMarker(
				waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize(), colors.palette2,
				new BasicStroke(1f));
		//		chart.getXYPlot().addRangeMarker(waterfallPaletteStartMarker);
		//		chart.getXYPlot().addRangeMarker(waterfallPaletteEndMarker);

		printInit(2);

		refreshRadioIdentity();
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
		uiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		uiFrame.setLayout(new BorderLayout());
		RadioIdentity bootId = parameterRadioIdentity.getValue();
		uiFrame.setTitle(bootId != null && bootId.present
				? "Spectrum Analyzer — " + bootId.displayBoard()
				: "Spectrum Analyzer");
		((javax.swing.JComponent) uiFrame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 8));
		uiFrame.add(splitPanePanel, BorderLayout.CENTER);
		uiFrame.setResizable(true);
		uiFrame.setMinimumSize(new Dimension(900, 560));
		JScrollPane settingsScroll = new JScrollPane(settingsPanel);
		settingsScroll.setBorder(null);
		settingsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		settingsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
		settingsScroll.setMinimumSize(new Dimension(260, 200));
		uiFrame.add(settingsScroll, BorderLayout.EAST);
		sweepStatusBar = new SweepStatusBar();
		uiFrame.add(sweepStatusBar, BorderLayout.SOUTH);
		applyAppIcons(uiFrame);
		
		printInit(4);
		setupFrequencyAllocationTable();
		printInit(5);
		
		uiFrame.pack();
		uiFrame.setMinimumSize(new Dimension(900, 560));
		uiFrame.setResizable(true);
		placeInitialWindow(uiFrame);
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
	public ModelValue<RadioIdentity> getRadioIdentity() {
		return parameterRadioIdentity;
	}

	@Override
	public ModelValue<String> getSelectedSerial() {
		return parameterSelectedSerial;
	}

	@Override
	public ModelValueBoolean getClkoutEnable() {
		return parameterClkoutEnable;
	}

	@Override
	public ModelValueBoolean isRadioReleased() {
		return parameterRadioReleased;
	}

	@Override
	public void restartSweep() {
		parameterRadioReleased.setValue(false);
		restartHackrfSweep();
	}

	@Override
	public void releaseRadio() {
		parameterRadioReleased.setValue(true);
		forceStopSweep = true;
		if (sweepEngine != null)
			sweepEngine.requestStop();
		HackRFSweepNativeBridge.stop();
		if (threadHackrfSweep != null) {
			try {
				threadHackrfSweep.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		refreshRadioIdentity();
	}

	@Override
	public java.util.List<String> listRadioSerials() {
		return HackRFDeviceQuery.listSerials();
	}

	@Override
	public ModelValueBoolean isChartsPeaksVisible() {
		return parameterShowPeaks;
	}

	@Override
	public ModelValueBoolean isPowerAutoScale() {
		return parameterPowerAutoScale;
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

	private static void applyAppIcons(JFrame frame) {
		List<Image> icons = loadAppIcons();
		if (icons.isEmpty())
			return;
		frame.setIconImages(icons);
		if (Taskbar.isTaskbarSupported()) {
			try {
				Taskbar.getTaskbar().setIconImage(icons.get(0));
			} catch (Exception ignored) {
			}
		}
	}

	private static List<Image> loadAppIcons() {
		List<Image> icons = new ArrayList<Image>();
		String[] resources = {
				"/jspectrumanalyzer/icon-256.png",
				"/jspectrumanalyzer/icon-128.png",
				"/jspectrumanalyzer/icon-64.png",
				"/jspectrumanalyzer/icon-48.png",
				"/jspectrumanalyzer/icon-32.png",
				"/jspectrumanalyzer/icon-16.png"
		};
		for (int i = 0; i < resources.length; i++) {
			URL url = HackRFSweepSpectrumAnalyzer.class.getResource(resources[i]);
			if (url != null)
				icons.add(new ImageIcon(url).getImage());
		}
		if (icons.isEmpty()) {
			File[] fallbacks = { new File("lib/program.png"), new File("program.png") };
			for (int i = 0; i < fallbacks.length; i++) {
				if (fallbacks[i].isFile()) {
					icons.add(new ImageIcon(fallbacks[i].getAbsolutePath()).getImage());
					break;
				}
			}
		}
		return icons;
	}

	/**
	 * WSLg often reports one huge virtual desktop (e.g. 15360x2160). Maximizing
	 * there yields a gray empty frame. Size to the default screen and sit at
	 * its origin instead.
	 */
	private static void placeInitialWindow(JFrame frame) {
		Rectangle screen;
		try {
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			screen = gd.getDefaultConfiguration().getBounds();
		} catch (Exception e) {
			screen = new Rectangle(0, 0, 1920, 1080);
		}
		int w = 1600;
		int h = 900;
		if (screen.width > 0 && screen.width <= 2560)
			w = Math.max(1000, screen.width - 80);
		if (screen.height > 0 && screen.height <= 1440)
			h = Math.max(700, screen.height - 80);
		if (screen.width > 2560 || screen.height > 1440) {
			w = 1600;
			h = 900;
		}
		frame.setExtendedState(Frame.NORMAL);
		frame.setSize(w, h);
		int x = screen.x + 40;
		int y = screen.y + 40;
		if (x + w > screen.x + screen.width && screen.width > w)
			x = screen.x + Math.max(0, (screen.width - w) / 2);
		frame.setLocation(x, y);
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

	private void refreshRadioIdentity() {
		RadioIdentity identity = RadioIdentity.ABSENT;
		try {
			identity = HackRFDeviceQuery.query().toIdentity();
		} catch (Throwable t) {
			identity = RadioIdentity.ABSENT;
		}
		parameterRadioIdentity.setValue(identity);
		if (uiFrame != null) {
			String title = identity.present ? "Spectrum Analyzer — " + identity.displayBoard() : "Spectrum Analyzer";
			final String frameTitle = title;
			SwingUtilities.invokeLater(() -> uiFrame.setTitle(frameTitle));
		}
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
		private SpectrumPowerScale powerScale;

		@Override
		public void onPacketAccepted() {
			fireHardwareStateChanged(true);
		}

		@Override
		public void onFirstDataset(DatasetSpectrumPeak ds, float fftBinHz) {
			datasetSpectrum = ds;
			fmTracker.reset();
			fmStations = List.of();
			powerScale = null;
			spurFilter = sweepEngine.getSpurFilter();
			chart.getXYPlot().getDomainAxis().setRange(getFreq().getStartMHz(), getFreq().getEndMHz());
		}

		@Override
		public void onFullSweepProcessed(DatasetSpectrumPeak ds) {
			datasetSpectrum = ds;
			synchronized (perfWatch) {
				perfWatch.hwFullSpectrumRefreshes++;
			}
			// Narrow windows (FM 20 MHz) finish 400+ sweeps/s. Updating the
			// waterfall / EDT that often freezes the plot. Keep ingesting
			// bins; only paint at the chart frame rate.
			if (System.currentTimeMillis() - lastChartUpdated <= 1000 / limitChartRefreshFPS)
				return;
			lastChartUpdated = System.currentTimeMillis();
			frameCounterChart++;

			FrequencyRange sweepRange = getFreq();
			fmStations = fmTracker.update(ds, sweepRange.getStartMHz(), sweepRange.getEndMHz());

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

			XYSeries spectrumSeries = datasetSpectrum.createSpectrumDataset("spectrum");
			XYSeries spectrumPeaks = parameterShowPeaks.getValue() ? datasetSpectrum.createPeaksDataset("peaks")
					: spectrumPeaksEmpty;
			final double yLow;
			final double yHigh;
			if (parameterPowerAutoScale.getValue()) {
				SpectrumPowerScale target = SpectrumPowerScale.fromDataset(datasetSpectrum);
				long now = System.currentTimeMillis();
				if (powerScale == null || powerScale.isUnset())
					powerScale = (target.isUnset() ? SpectrumPowerScale.defaults() : target.displayTicks())
							.stamped(now);
				else
					powerScale = powerScale.follow(target, now);
				yLow = powerScale.lowDb;
				yHigh = powerScale.highDb;
			} else {
				powerScale = SpectrumPowerScale.defaults();
				yLow = SpectrumPowerScale.DEFAULT_LOW;
				yHigh = SpectrumPowerScale.DEFAULT_HIGH;
			}

			if (parameterPersistentDisplay.getValue()) {
				long start = System.nanoTime();
				boolean redraw = frameCounterChart % limitPersistentRefreshEveryChartFrame == 0;
				persistentDisplay.drawSpectrum2(datasetSpectrum, (float) yLow, (float) yHigh, redraw);
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
				waterfallPlot.repaint();
			}

			final double rbwHz = datasetSpectrum.getFFTBinSizeHz();
			final int bins = datasetSpectrum.spectrumLength();
			final double fps = waterfallPlot.getFps();
			final Double peakDbm = Double.valueOf(datasetSpectrum.calculateSpectrumPeakPower());
			SwingUtilities.invokeLater(() -> {
				if (sweepStatusBar != null)
					sweepStatusBar.setSweepInfo(rbwHz, bins, fps, peakDbm);
				chart.setNotify(false);
				NumberAxis yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
				if (yAxis.getLowerBound() != yLow || yAxis.getUpperBound() != yHigh)
					yAxis.setRange(yLow, yHigh);
				chartDataset.removeAllSeries();
				chartDataset.addSeries(spectrumPeaks);
				chartDataset.addSeries(spectrumSeries);
				chart.setNotify(true);
				if (gifCap != null) {
					gifCap.captureFrame();
				}
			});
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
		if (parameterRadioReleased.getValue())
			return;
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
		rangeAxis.setRange(SpectrumPowerScale.DEFAULT_LOW, SpectrumPowerScale.DEFAULT_HIGH);
		rangeAxis.setTickUnit(new NumberTickUnit(SpectrumPowerScale.TICK_DB, new DecimalFormat("###")));

		domainAxis.setAutoRange(false);
		domainAxis.setLowerMargin(0);
		domainAxis.setUpperMargin(0);
		domainAxis.setRange(getFreq().getStartMHz(), getFreq().getEndMHz());
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
				Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
				if (img != null) {
					g2.drawImage(img, (int) area.getX(), (int) area.getY(), null);
				}
				XYPlot xy = chart.getXYPlot();
				FrequencyRange range = getFreq();
				QuickSelectBandOverlay.paint(g2, area, xy.getDomainAxis(), xy.getDomainAxisEdge(),
						range.getStartMHz(), range.getEndMHz());
				WifiChannelOverlay.paint(g2, area, xy.getDomainAxis(), xy.getDomainAxisEdge(),
						range.getStartMHz(), range.getEndMHz());
				FmChannelOverlay.paint(g2, area, xy.getDomainAxis(), xy.getDomainAxisEdge(),
						range.getStartMHz(), range.getEndMHz(), fmStations);
				spectrumZoomOverlay.paint(g2, area);
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

	/**
	 * Grafana-style frequency zoom: drag a span to zoom in, double-click or
	 * scroll out to zoom out. Updates the sweep start/end so the radio
	 * retunes (same as changing the digits).
	 */
	private void setupSpectrumZoom() {
		chartPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || !inPlot(e))
					return;
				spectrumZoomAnchorX = e.getX();
				spectrumZoomDragging = true;
				spectrumZoomOverlay.setSelection(spectrumZoomAnchorX, spectrumZoomAnchorX);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!spectrumZoomDragging)
					return;
				spectrumZoomDragging = false;
				spectrumZoomOverlay.clear();
				chartPanel.repaint();
				Rectangle2D area = plotArea();
				if (area == null)
					return;
				FrequencyRange current = getFreq();
				SpectrumZoom.fromDrag(spectrumZoomAnchorX, e.getX(), area, current.getStartMHz(), current.getEndMHz())
						.ifPresent(HackRFSweepSpectrumAnalyzer.this::zoomIn);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && inPlot(e))
					zoomOut();
			}
		});
		chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!spectrumZoomDragging)
					return;
				spectrumZoomOverlay.setSelection(spectrumZoomAnchorX, e.getX());
				chartPanel.repaint();
			}
		});
		chartPanel.addMouseWheelListener((MouseWheelEvent e) -> {
			if (!inPlot(e))
				return;
			e.consume();
			Rectangle2D area = plotArea();
			if (area == null)
				return;
			FrequencyRange current = getFreq();
			double mhz = chart.getXYPlot().getDomainAxis().java2DToValue(e.getX(), area,
					chart.getXYPlot().getDomainAxisEdge());
			if (e.getWheelRotation() < 0)
				zoomIn(SpectrumZoom.around(current, mhz, SpectrumZoom.ZOOM_IN_FACTOR));
			else
				zoomOutAround(mhz);
		});
	}

	private boolean inPlot(MouseEvent e) {
		Rectangle2D area = plotArea();
		return area != null && area.contains(e.getX(), e.getY());
	}

	private Rectangle2D plotArea() {
		if (chartPanel == null)
			return null;
		return chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
	}

	private void zoomIn(FrequencyRange next) {
		FrequencyRange current = getFreq();
		if (next == null || next.equals(current))
			return;
		spectrumZoomHistory.push(current);
		applySpectrumZoom(next);
	}

	private void zoomOut() {
		FrequencyRange current = getFreq();
		FrequencyRange next = spectrumZoomHistory.pop().orElseGet(() -> SpectrumZoom.expand(current));
		if (next.equals(current))
			return;
		applySpectrumZoom(next);
	}

	private void zoomOutAround(double centerMHz) {
		if (spectrumZoomHistory.canZoomOut()) {
			zoomOut();
			return;
		}
		FrequencyRange current = getFreq();
		FrequencyRange next = SpectrumZoom.around(current, centerMHz, SpectrumZoom.ZOOM_OUT_FACTOR);
		if (next.equals(current))
			return;
		applySpectrumZoom(next);
	}

	private void applySpectrumZoom(FrequencyRange next) {
		applyingSpectrumZoom = true;
		try {
			parameterFrequency.setValue(next);
		} finally {
			applyingSpectrumZoom = false;
		}
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
			});

		});
		parameterFrequencyAllocationTable.addListener(this::redrawFrequencySpectrumTable);
	}

	private void setupParameterObservers() {
		Runnable restartHackrf = this::restartHackrfSweep;
		parameterFrequency.addListener(restartHackrf);
		parameterFrequency.addListener((range) -> {
			if (chart != null)
				chart.getXYPlot().getDomainAxis().setRange(range.getStartMHz(), range.getEndMHz());
			if (!applyingSpectrumZoom)
				spectrumZoomHistory.clear();
		});
		parameterAntPower.addListener(restartHackrf);
		parameterAntennaLNA.addListener(restartHackrf);
		parameterFFTBinHz.addListener(restartHackrf);
		parameterSamples.addListener(restartHackrf);
		parameterSelectedSerial.addListener(restartHackrf);
		parameterClkoutEnable.addListener(restartHackrf);
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
		parameterPowerAutoScale.addListener((enabled) -> {
			SwingUtilities.invokeLater(() -> {
				if (chart == null || enabled)
					return;
				chart.getXYPlot().getRangeAxis().setRange(SpectrumPowerScale.DEFAULT_LOW,
						SpectrumPowerScale.DEFAULT_HIGH);
			});
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
			SwingUtilities.invokeLater(() -> {
				if (parameterPersistentDisplay.getValue())
					chart.getXYPlot().setBackgroundImage(image);
			});
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

			refreshRadioIdentity();
			System.out.println(
					"Starting sweep... " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz() + "MHz ");
			System.out.println("sweep params:  freq " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz()
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
