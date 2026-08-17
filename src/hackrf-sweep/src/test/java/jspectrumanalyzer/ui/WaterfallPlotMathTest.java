package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WaterfallPlotMathTest {

    @Test
    void normalizePowerClampsToPalette() {
        assertEquals(0.0, WaterfallPlot.normalizePower(-120, -90, 65), 1e-9);
        assertEquals(0.0, WaterfallPlot.normalizePower(-90, -90, 65), 1e-9);
        assertEquals(1.0, WaterfallPlot.normalizePower(0, -90, 65), 1e-9);
        assertEquals(0.5, WaterfallPlot.normalizePower(-57.5, -90, 65), 1e-9);
        assertEquals(0.0, WaterfallPlot.normalizePower(-50, -90, 0), 1e-9);
    }

    @Test
    void clampPixelXStaysInBuffer() {
        assertEquals(0, WaterfallPlot.clampPixelX(-3, 10));
        assertEquals(9, WaterfallPlot.clampPixelX(99, 10));
        assertEquals(4, WaterfallPlot.clampPixelX(4, 10));
        assertEquals(0, WaterfallPlot.clampPixelX(5, 0));
    }

    @Test
    void translateXToFrequencyMapsAndClamps() {
        assertEquals(-1.0, WaterfallPlot.translateXToFrequency(10, 0, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.4e9, WaterfallPlot.translateXToFrequency(0, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.5e9, WaterfallPlot.translateXToFrequency(100, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.45e9, WaterfallPlot.translateXToFrequency(50, 100, 2.4e9, 2.5e9), 1e5);
        assertEquals(2.4e9, WaterfallPlot.translateXToFrequency(-10, 100, 2.4e9, 2.5e9), 1.0);
        assertEquals(2.5e9, WaterfallPlot.translateXToFrequency(200, 100, 2.4e9, 2.5e9), 1.0);
    }
}
