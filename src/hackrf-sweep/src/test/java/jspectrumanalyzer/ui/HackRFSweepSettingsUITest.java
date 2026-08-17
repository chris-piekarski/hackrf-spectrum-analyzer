package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HackRFSweepSettingsUITest {

    @Test
    void noArgConstructorDoesNotThrow() {
        // Upstream WindowBuilder/designer path: construct the panel with no settings.
        assertDoesNotThrow(() -> { new HackRFSweepSettingsUI(); });
    }
}
