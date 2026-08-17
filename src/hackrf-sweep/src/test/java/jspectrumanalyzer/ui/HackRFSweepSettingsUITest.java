package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.FakeHackRFSettings;

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
        assertEquals("Pause", ui.pauseButton().getText());
        assertTrue(ui.peakFallSpinner().isVisible());
        assertTrue(ui.decayRateCombo().isVisible());
        assertEquals("HackRF disconnected", ui.connectedLabel().getText());

        SwingUtilities.invokeAndWait(() -> ui.pauseButton().doClick());
        flushEdt();
        assertTrue(settings.isCapturingPaused().getValue());
        assertEquals("Resume", ui.pauseButton().getText());

        settings.isChartsPeaksVisible().setValue(false);
        flushEdt();
        assertFalse(ui.peakFallSpinner().isVisible());

        settings.isPersistentDisplayVisible().setValue(false);
        flushEdt();
        assertFalse(ui.decayRateCombo().isVisible());

        settings.fireHardwareStatusChanged(true);
        assertEquals("HackRF connected", ui.connectedLabel().getText());
    }
}
