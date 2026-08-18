package jspectrumanalyzer.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner.ListEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.UIManager;

import jspectrumanalyzer.HackRFSweepSpectrumAnalyzer;
import jspectrumanalyzer.Version;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyAllocations;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.HackRFSettings.HackRFEventAdapter;
import net.miginfocom.swing.MigLayout;
import shared.mvc.MVCController;
import javax.swing.border.EmptyBorder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;

public class HackRFSweepSettingsUI extends JPanel
{
	/**
	 * 
	 */
	private HackRFSettings hRF;
	private static final long serialVersionUID = 7721079457485020637L;
	private JLabel txtHackrfConnected;
	private FrequencySelectorPanel frequencySelectorStart;
	private FrequencySelectorPanel frequencySelectorEnd;
	private QuickFrequencySelectorPanel quickFrequencySelector;
	private JSpinner spinnerFFTBinHz;
	private JSlider sliderGain;
	private JSpinner spinner_numberOfSamples;
	private JCheckBox chckbxAntennaPower;
	private JCheckBox chckbxAntennaLNA;
	private JSlider slider_waterfallPaletteStart;
	private JSlider slider_waterfallPaletteSize;
	private JCheckBox chckbxShowPeaks;
	private JCheckBox chckbxRemoveSpurs;
	private JButton btnPause;
	private SpinnerListModel spinnerModelFFTBinHz;
	private FrequencySelectorRangeBinder frequencyRangeSelector;
	private JCheckBox chckbxFilterSpectrum;
	private JSpinner spinnerPeakFallSpeed;
	private JComboBox<FrequencyAllocationTable> comboBoxFrequencyAllocationBands;
	private JSlider sliderGainVGA;
	private JSlider sliderGainLNA;
	private JLabel lblPeakFall;
	private JComboBox<BigDecimal> comboBoxLineThickness;
	private JLabel lblPersistentDisplay;
	private JCheckBox checkBoxPersistentDisplay;
	private JCheckBox checkBoxWaterfallEnabled;
	private JLabel lblDecayRate;
	private JComboBox comboBoxDecayRate;
	private JLabel lblDebugDisplay;
	private JCheckBox checkBoxDebugDisplay;

	public HackRFSweepSettingsUI()
	{
		this(null);
	}

	/**
	 * Create the panel.
	 */
	public HackRFSweepSettingsUI(HackRFSettings hackRFSettings)
	{
		this.hRF	= hackRFSettings;
		AnalyzerLookAndFeel.install();
		int minFreq = 1;
		int maxFreq = 7250;
		int freqStep = 1;

		JPanel panelMainSettings	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][grow]"));

		panelMainSettings.setBorder(new EmptyBorder(UIManager.getInsets("TabbedPane.tabAreaInsets")));;

		JLabel lblQuickSelect = new JLabel("Quick Select");
		panelMainSettings.add(lblQuickSelect, "cell 0 0,alignx left,aligny center");

		quickFrequencySelector = new QuickFrequencySelectorPanel();
		panelMainSettings.add(quickFrequencySelector, "cell 0 1,growx,aligny center");

		JLabel lblNewLabel = new JLabel("Frequency start [MHz]");
		panelMainSettings.add(lblNewLabel, "cell 0 2,growx,aligny center");

		frequencySelectorStart = new FrequencySelectorPanel(minFreq, maxFreq, freqStep, minFreq);
		panelMainSettings.add(frequencySelectorStart, "cell 0 3,growx,aligny center");

		JLabel lblFrequencyEndmhz = new JLabel("Frequency end [MHz]");
		panelMainSettings.add(lblFrequencyEndmhz, "cell 0 15,alignx left,aligny center");

		frequencySelectorEnd = new FrequencySelectorPanel(minFreq, maxFreq, freqStep, maxFreq);
		panelMainSettings.add(frequencySelectorEnd, "cell 0 16,grow");
		
		txtHackrfConnected = new JLabel();
		txtHackrfConnected.setText("HackRF disconnected");
		panelMainSettings.add(txtHackrfConnected, "cell 0 24,growx");
		txtHackrfConnected.setBorder(null);
		
		btnPause = new JButton("Pause");
		panelMainSettings.add(btnPause, "cell 0 26,growx");

		
		
		
		JTabbedPane tabbedPane	= new JTabbedPane(JTabbedPane.TOP);
		setLayout(new BorderLayout());
		add(panelMainSettings, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);

		JPanel tab1	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][][0][][][0][][][0][][][0][][][0][][0][][grow,fill]"));
		
		JPanel tab2	= new JPanel(new MigLayout("", "[123.00px,grow,leading]", "[][0][][][0][][][0][][0][][][0][][0][][][0][0][][][0][][0][grow,fill]"));
		
		tabbedPane.addTab("HackRF Settings", tab1);
		tabbedPane.addTab("Chart options", tab2);

		
		JLabel lblNewLabel_2 = new JLabel("LNA Gain [dB]");
		tab1.add(lblNewLabel_2, "cell 0 3");
		
		sliderGainLNA = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
		sliderGainLNA.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGainLNA, "cell 0 4,growx");
		
		JLabel lblVgfaGaindb = new JLabel("VGA Gain [dB]");
		tab1.add(lblVgfaGaindb, "cell 0 6");
		
		sliderGainVGA = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 2);
		sliderGainVGA.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGainVGA, "cell 0 7,growx");

		

		JLabel lblFftBinhz = new JLabel("FFT Bin [Hz]");
		tab1.add(lblFftBinhz, "cell 0 9");

		spinnerFFTBinHz = new JSpinner();
		spinnerFFTBinHz.setFont(new Font("Monospaced", Font.BOLD, 16));
		spinnerModelFFTBinHz = new SpinnerListModel(new String[] { "2 445", "5 000", "10 000", "20 000", 
				"50 000", "100 000", "200 000", "500 000", "1 000 000", "2 000 000", "5 000 000" });
		spinnerFFTBinHz.setModel(spinnerModelFFTBinHz);
		tab1.add(spinnerFFTBinHz, "cell 0 10,growx");
		((ListEditor) spinnerFFTBinHz.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		

		JLabel lblGain = new JLabel("Gain [dB]");
		tab1.add(lblGain, "cell 0 0");

		sliderGain = new JSlider(JSlider.HORIZONTAL, 0, 100, 2);
		sliderGain.setFont(new Font("Monospaced", Font.BOLD, 16));
		tab1.add(sliderGain, "flowy,cell 0 1,growx");

		JLabel lbl_gainValue = new JLabel(hRF == null ? "0" : hRF.getGain() + "dB");
		tab1.add(lbl_gainValue, "cell 0 1,alignx right");

		if (hRF != null) {
			hRF.getGain().addListener((gain) -> lbl_gainValue.setText(String.format(" %ddB  [LNA: %ddB  VGA: %ddB]", 
					gain, hRF.getGainLNA().getValue(), hRF.getGainVGA().getValue())));
		}
		

		JLabel lblNumberOfSamples = new JLabel("Number of samples");
		tab1.add(lblNumberOfSamples, "cell 0 12");

		spinner_numberOfSamples = new JSpinner();
		spinner_numberOfSamples.setModel(new SpinnerListModel(new String[] { "8192", "16384", "32768", "65536", "131072", "262144" }));
		spinner_numberOfSamples.setFont(new Font("Monospaced", Font.BOLD, 16));
		((ListEditor) spinner_numberOfSamples.getEditor()).getTextField().setHorizontalAlignment(JTextField.RIGHT);
		((ListEditor) spinner_numberOfSamples.getEditor()).getTextField().setEditable(false);
		tab1.add(spinner_numberOfSamples, "cell 0 13,growx");

		JButton btnAbout = new JButton("Visit homepage");
		btnAbout.addActionListener(e -> {
			 if (Desktop.isDesktopSupported()) {
		            Desktop desktop = Desktop.getDesktop();
		            try {
		                URI uri = new URI(Version.url);
		                desktop.browse(uri);
		            } catch (Exception ex) {
		                ex.printStackTrace();
		            }
		    }
		});
		
		JLabel labelVersion = new JLabel("Version: "+Version.version);
		tab1.add(labelVersion, "flowx,cell 0 17");
		tab1.add(btnAbout, "cell 0 17,alignx right");
		
		JLabel lblAntennaPower = new JLabel("Antenna power");
		tab1.add(lblAntennaPower, "flowx,cell 0 15,growx");
		
		chckbxAntennaPower = new JCheckBox("");
		chckbxAntennaPower.setHorizontalTextPosition(SwingConstants.LEADING);
		tab1.add(chckbxAntennaPower, "cell 0 15,alignx right");

		JLabel lblLNAEnable = new JLabel("Antenna LNA +14dB");
		tab1.add(lblLNAEnable, "flowx,cell 0 16,growx");

		chckbxAntennaLNA = new JCheckBox("");
		chckbxAntennaLNA.setHorizontalTextPosition(SwingConstants.LEADING);
		tab1.add(chckbxAntennaLNA, "cell 0 16,alignx right");
		
		chckbxFilterSpectrum = new JCheckBox("Filter spectrum");
		
		JLabel lblWaterfallEnabled = new JLabel("Waterfall enabled");
		tab2.add(lblWaterfallEnabled, "flowx,cell 0 0,growx");

		


		JLabel lblWaterfallPaletteStart = new JLabel("Waterfall palette start [dB]");
		tab2.add(lblWaterfallPaletteStart, "cell 0 2");

		slider_waterfallPaletteStart = new JSlider();
		slider_waterfallPaletteStart.setMinimum(-100);
		slider_waterfallPaletteStart.setMaximum(0);
		slider_waterfallPaletteStart.setValue(-30);
		tab2.add(slider_waterfallPaletteStart, "cell 0 3,growx");


		JLabel lblWaterfallPaletteLength = new JLabel("Waterfall palette length [dB]");
		tab2.add(lblWaterfallPaletteLength, "cell 0 5");

		slider_waterfallPaletteSize = new JSlider(HackRFSweepSpectrumAnalyzer.SPECTRUM_PALETTE_SIZE_MIN, 100);
		tab2.add(slider_waterfallPaletteSize, "cell 0 6,growx");
		
		JLabel lblSpectrLineThickness = new JLabel("Spectr. Line Thickness");
		tab2.add(lblSpectrLineThickness, "flowx,cell 0 8,growx");
		
		JLabel lblShowPeaks = new JLabel("Show peaks");
		tab2.add(lblShowPeaks, "flowx,cell 0 10,growx");
		
		
		chckbxShowPeaks = new JCheckBox("");
		tab2.add(chckbxShowPeaks, "cell 0 10,alignx right");
		
		JLabel lblSpurFiltermay = new JLabel("Spur filter (may distort real signals)");
		tab2.add(lblSpurFiltermay, "flowx,cell 0 13,growx");
		
		chckbxRemoveSpurs = new JCheckBox("");
		tab2.add(chckbxRemoveSpurs, "cell 0 13,alignx right");
		
		lblPeakFall = new JLabel("  Fall speed [s]");
		tab2.add(lblPeakFall, "flowx,cell 0 11,growx");
		
		spinnerPeakFallSpeed = new JSpinner();
		spinnerPeakFallSpeed.setModel(new SpinnerNumberModel(10, 0, 500, 1));
		tab2.add(spinnerPeakFallSpeed, "cell 0 11,alignx right");
		
		lblPersistentDisplay = new JLabel("Persistent Display");
		tab2.add(lblPersistentDisplay, "flowx,cell 0 15,growx");
		
		lblDecayRate = new JLabel("  Persistence time [s]");
		tab2.add(lblDecayRate, "flowx,cell 0 16,growx");
		
		JLabel lblDisplayFrequencyAllocation = new JLabel("Frequency Allocation Bands");
		tab2.add(lblDisplayFrequencyAllocation, "cell 0 19");
		
		
		FrequencyAllocations frequencyAllocations	= new FrequencyAllocations();
		Vector<FrequencyAllocationTable> freqAllocValues	= new Vector<>();
		freqAllocValues.add(null);
		freqAllocValues.addAll(frequencyAllocations.getTable().values());
		DefaultComboBoxModel<FrequencyAllocationTable> freqAllocModel	= new  DefaultComboBoxModel<>(freqAllocValues);
		comboBoxFrequencyAllocationBands = new JComboBox<FrequencyAllocationTable>(freqAllocModel);
		tab2.add(comboBoxFrequencyAllocationBands, "cell 0 20,growx");
		
		comboBoxLineThickness = new JComboBox(new BigDecimal[] {
				new BigDecimal("1"), new BigDecimal("1.5"), new BigDecimal("2"), new BigDecimal("3")
				});
		tab2.add(comboBoxLineThickness, "cell 0 8,alignx right");
		
		checkBoxPersistentDisplay = new JCheckBox("");
		tab2.add(checkBoxPersistentDisplay, "cell 0 15,alignx right");
		
		checkBoxWaterfallEnabled = new JCheckBox("");
		tab2.add(checkBoxWaterfallEnabled, "cell 0 0,alignx right");
		
		comboBoxDecayRate = new JComboBox(
				new Vector<>(IntStream.rangeClosed(hRF != null ? hRF.getPersistentDisplayDecayRate().getMin() : 0, hRF != null ? hRF.getPersistentDisplayDecayRate().getMax() : 1).
						boxed().collect(Collectors.toList())));
		tab2.add(comboBoxDecayRate, "cell 0 16,alignx right");
		
		lblDebugDisplay = new JLabel("Debug display");
		tab2.add(lblDebugDisplay, "flowx,cell 0 22,growx");
		
		checkBoxDebugDisplay = new JCheckBox("");
		tab2.add(checkBoxDebugDisplay, "cell 0 22,alignx right");
		
		
		if (hRF != null)
			bindViewToModel();
	}

	private void bindViewToModel() {
		frequencyRangeSelector = new FrequencySelectorRangeBinder(frequencySelectorStart, frequencySelectorEnd, quickFrequencySelector);

		new MVCController(spinnerFFTBinHz, hRF.getFFTBinHz(), 
				viewValue -> Integer.parseInt(viewValue.toString().replaceAll("\\s", "")), 
				modelValue -> {
					Optional<?> val = spinnerModelFFTBinHz.getList().stream().filter(value -> modelValue <= Integer.parseInt(value.toString().replaceAll("\\s", ""))).findFirst();
					if (val.isPresent())
						return val.get();
					else
						return spinnerModelFFTBinHz.getList().get(0);
				});
		new MVCController(sliderGain, hRF.getGain());
		new MVCController(spinner_numberOfSamples, hRF.getSamples(), val -> Integer.parseInt(val.toString()), val -> val.toString());
		new MVCController(chckbxAntennaPower, hRF.getAntennaPowerEnable());
		new MVCController(chckbxAntennaLNA, hRF.getAntennaLNA());
		new MVCController(slider_waterfallPaletteStart, hRF.getSpectrumPaletteStart());
		new MVCController(slider_waterfallPaletteSize, hRF.getSpectrumPaletteSize());
		new MVCController(	(Consumer<FrequencyRange> valueChangedCall) ->  
								frequencyRangeSelector.addPropertyChangeListener((PropertyChangeEvent evt) -> valueChangedCall.accept(frequencyRangeSelector.getFrequencyRange()) ) ,
							(FrequencyRange newComponentValue) -> {
								if(frequencyRangeSelector.selFreqStart.getValue() != newComponentValue.getStartMHz())
									frequencyRangeSelector.selFreqStart.setValue(newComponentValue.getStartMHz());
								if(frequencyRangeSelector.selFreqEnd.getValue() != newComponentValue.getEndMHz())
									frequencyRangeSelector.selFreqEnd.setValue(newComponentValue.getEndMHz());
							},
							hRF.getFrequency()
		); 
		new MVCController(chckbxShowPeaks, hRF.isChartsPeaksVisible());
		new MVCController(chckbxFilterSpectrum, hRF.isFilterSpectrum());
		new MVCController(chckbxRemoveSpurs, hRF.isSpurRemoval());
		
		new MVCController((valueChangedCall) -> btnPause.addActionListener((event) -> valueChangedCall.accept(!hRF.isCapturingPaused().getValue())), 
				isCapt -> btnPause.setText(!isCapt ? "Pause"  : "Resume"), 
				hRF.isCapturingPaused());
	
		new MVCController(spinnerPeakFallSpeed, hRF.getPeakFallRate(), in -> (Integer)in, in -> in);
	
		new MVCController(comboBoxFrequencyAllocationBands, hRF.getFrequencyAllocationTable());
		
		sliderGainLNA.setModel(new DefaultBoundedRangeModel(hRF.getGainLNA().getValue(), 0, hRF.getGainLNA().getMin(), hRF.getGainLNA().getMax()));
		sliderGainVGA.setModel(new DefaultBoundedRangeModel(hRF.getGainVGA().getValue(), 0, hRF.getGainVGA().getMin(), hRF.getGainVGA().getMax()));
		
		sliderGainLNA.setSnapToTicks(true);
		sliderGainLNA.setMinorTickSpacing(hRF.getGainLNA().getStep());
		
		sliderGainVGA.setSnapToTicks(true);
		sliderGainVGA.setMinorTickSpacing(hRF.getGainVGA().getStep());
		
		new MVCController(sliderGainLNA, hRF.getGainLNA());
		new MVCController(sliderGainVGA, hRF.getGainVGA());

		new MVCController(comboBoxLineThickness, hRF.getSpectrumLineThickness());
		
		new MVCController(checkBoxPersistentDisplay, hRF.isPersistentDisplayVisible());
		
		new MVCController(checkBoxWaterfallEnabled, hRF.isWaterfallVisible());
		
		new MVCController(checkBoxDebugDisplay, hRF.isDebugDisplay());
		
		hRF.isChartsPeaksVisible().addListener((enabled) -> {
			SwingUtilities.invokeLater(()->{
				spinnerPeakFallSpeed.setEnabled(enabled);
				spinnerPeakFallSpeed.setVisible(enabled);
				lblPeakFall.setVisible(enabled);
			});
		});
		hRF.isChartsPeaksVisible().callObservers();
		
		new MVCController(comboBoxDecayRate, hRF.getPersistentDisplayDecayRate());
		hRF.isPersistentDisplayVisible().addListener((visible) -> {
			SwingUtilities.invokeLater(()->{
				comboBoxDecayRate.setVisible(visible);
				lblDecayRate.setVisible(visible);
			});
		});
		hRF.isPersistentDisplayVisible().callObservers();
		
		hRF.registerListener(new HackRFSettings.HackRFEventAdapter()
		{
			@Override public void captureStateChanged(boolean isCapturing)
			{
//				btnPause.setText(isCapturing ? "Pause"  : "Resume");
			}
			@Override public void hardwareStatusChanged(boolean hardwareSendingData)
			{
				txtHackrfConnected.setText("HackRF "+(hardwareSendingData ? "connected":"disconnected"));
			}
		});;
		
	}

	JButton pauseButton() {
		return btnPause;
	}

	JLabel connectedLabel() {
		return txtHackrfConnected;
	}

	JSpinner fftBinSpinner() {
		return spinnerFFTBinHz;
	}

	JCheckBox showPeaksCheckbox() {
		return chckbxShowPeaks;
	}

	JSpinner peakFallSpinner() {
		return spinnerPeakFallSpeed;
	}

	JCheckBox persistentDisplayCheckbox() {
		return checkBoxPersistentDisplay;
	}

	JComboBox decayRateCombo() {
		return comboBoxDecayRate;
	}

}
