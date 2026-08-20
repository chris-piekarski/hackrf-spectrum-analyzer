package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.FakeHackRFSettings;
import jspectrumanalyzer.core.RadioIdentity;

class HackRFSweepSettingsUITest {

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void noArgConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> { new HackRFSweepSettingsUI(); });
    }

    @Test
    void bindsFftBinPausePeaksPersistenceAndHardwareStatus() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();

        assertEquals("100 000", ui.fftBinSpinner().getValue().toString());
        assertTrue(ui.autoScaleCheckbox().isSelected(), "dB auto-scale is on so FM/Wi-Fi peaks fill the axis");
        assertTrue(ui.autoGainCheckbox().isSelected(), "auto gain is the default");
        assertFalse(ui.gainSlider().isEnabled(), "gain sliders stay locked while auto is on");
        assertEquals("Pause", ui.pauseButton().getText());
        assertTrue(ui.peakFallSpinner().isVisible());
        assertTrue(ui.decayRateCombo().isVisible());
        assertTrue(ui.connectedLabel().getText().contains("No radio detected"));

        SwingUtilities.invokeAndWait(() -> ui.pauseButton().doClick());
        flushEdt();
        assertTrue(settings.isCapturingPaused().getValue());
        assertEquals("Resume", ui.pauseButton().getText());

        SwingUtilities.invokeAndWait(() -> ui.autoScaleCheckbox().setSelected(false));
        flushEdt();
        assertFalse(settings.isPowerAutoScale().getValue());

        SwingUtilities.invokeAndWait(() -> ui.autoGainCheckbox().setSelected(false));
        flushEdt();
        assertFalse(settings.isAutoGain().getValue());
        assertTrue(ui.gainSlider().isEnabled(), "unchecking Auto unlocks the gain sliders");

        settings.isChartsPeaksVisible().setValue(false);
        flushEdt();
        assertFalse(ui.peakFallSpinner().isVisible());

        settings.isPersistentDisplayVisible().setValue(false);
        flushEdt();
        assertFalse(ui.decayRateCombo().isVisible());

        settings.getRadioIdentity().setValue(
                RadioIdentity.of("HackRF One", "0000000000000000a1b2c3d4e5f60708", "v2026.01.3", "1.16"));
        settings.fireHardwareStatusChanged(true);
        flushEdt();
        assertTrue(ui.connectedLabel().getText().contains("HackRF One"));
        assertTrue(ui.connectedLabel().getText().contains("SN e5f60708"));
        assertTrue(ui.connectedLabel().getText().contains("FW 2026.01.3"));
        assertFalse(ui.connectedLabel().getText().contains("HackRF connected"));
        assertTrue(ui.connectedLabel().getToolTipText().contains("Sweep running"));
    }

    @Test
    void hardwareStripRestartStopPickerAndClkout() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        settings.listedSerials.add("aabbccdd");
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();

        assertEquals("Restart", ui.restartButton().getText());
        assertEquals("Stop", ui.stopButton().getText());
        assertEquals(HackRFSweepSettingsUI.FIRST_RADIO, ui.radioCombo().getItemAt(0));
        assertEquals("aabbccdd", ui.radioCombo().getItemAt(1));

        SwingUtilities.invokeAndWait(() -> ui.stopButton().doClick());
        flushEdt();
        assertEquals(1, settings.releaseRadioCalls);
        assertTrue(settings.isRadioReleased().getValue());
        assertEquals("Stopped", ui.stopButton().getText());
        assertFalse(ui.pauseButton().isEnabled());

        SwingUtilities.invokeAndWait(() -> ui.restartButton().doClick());
        flushEdt();
        assertEquals(1, settings.restartSweepCalls);
        assertFalse(settings.isRadioReleased().getValue());

        SwingUtilities.invokeAndWait(() -> ui.clkoutCheckBox().setSelected(true));
        flushEdt();
        assertTrue(settings.getClkoutEnable().getValue());
    }

    @Test
    void listenButtonParksTheRadioWithoutRelease() throws Exception {
        FakeHackRFSettings settings = new FakeHackRFSettings();
        HackRFSweepSettingsUI ui = new HackRFSweepSettingsUI(settings);
        flushEdt();
        assertEquals("Listen", ui.listenButton().getText());
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertEquals(1, settings.startListenCalls);
        assertTrue(settings.isListening().getValue());
        assertFalse(settings.isRadioReleased().getValue());
        assertTrue(ui.listenButton().getText().contains("Listening"));
        assertFalse(ui.pauseButton().isEnabled());
        SwingUtilities.invokeAndWait(() -> ui.listenButton().doClick());
        flushEdt();
        assertFalse(settings.isListening().getValue());
        assertEquals(1, settings.restartSweepCalls);
        assertEquals(0, settings.releaseRadioCalls);
    }
}
