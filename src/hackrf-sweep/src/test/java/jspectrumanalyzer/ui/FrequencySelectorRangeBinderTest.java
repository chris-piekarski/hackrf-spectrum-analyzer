package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyVetoException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.core.FrequencyRange;

class FrequencySelectorRangeBinderTest {

    private static JButton buttonNamed(QuickFrequencySelectorPanel panel, String text) {
        JButton button = panel.findButton(text);
        assertNotNull(button, "No button labeled " + text);
        return button;
    }

    @Test
    void testConstructionAndInitialRange() throws PropertyVetoException {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 2400);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 2500);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();

        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(start, end, quick);

        FrequencyRange range = binder.getFrequencyRange();
        assertEquals(2400, range.getStartMHz());
        assertEquals(2500, range.getEndMHz());
    }

    @Test
    void quickSelectButtonsSetKnownRanges() {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 1000);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 3000);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(start, end, quick);

        for (QuickSelectPreset preset : QuickSelectPreset.values()) {
            buttonNamed(quick, preset.label).doClick();
            assertEquals(preset.startMHz, binder.getFrequencyRange().getStartMHz(), preset.label + " start");
            assertEquals(preset.endMHz, binder.getFrequencyRange().getEndMHz(), preset.label + " end");
            assertEquals(preset.label, quick.getValue());
        }
    }

    @Test
    void clickingSamePresetAgainRestoresRange() {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 1000);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 3000);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(start, end, quick);

        buttonNamed(quick, QuickSelectPreset.WIFI_2.label).doClick();
        start.setValue(2412);
        end.setValue(2462);
        buttonNamed(quick, QuickSelectPreset.WIFI_2.label).doClick();

        assertEquals(QuickSelectPreset.WIFI_2.startMHz, binder.getFrequencyRange().getStartMHz());
        assertEquals(QuickSelectPreset.WIFI_2.endMHz, binder.getFrequencyRange().getEndMHz());
    }

    @Test
    void quickSelectNotifiesRangeListenerOnce() {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 2400);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 2500);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(start, end, quick);

        AtomicInteger notifications = new AtomicInteger();
        binder.addPropertyChangeListener(evt -> notifications.incrementAndGet());
        buttonNamed(quick, "LTE-1").doClick();

        assertEquals(1, notifications.get(), "preset must be one sweep restart, not start+end");
        assertEquals(QuickSelectPreset.LTE_1.startMHz, binder.getFrequencyRange().getStartMHz());
        assertEquals(QuickSelectPreset.LTE_1.endMHz, binder.getFrequencyRange().getEndMHz());
    }

    @Test
    void startEndVetoKeepsOrderByNudgingTheOtherSelector() {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 2000);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 3000);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        FrequencySelectorRangeBinder binder = new FrequencySelectorRangeBinder(start, end, quick);

        assertTrue(start.setValue(3500));
        assertEquals(3500, binder.getFrequencyRange().getStartMHz());
        assertEquals(4500, binder.getFrequencyRange().getEndMHz());

        assertTrue(end.setValue(2500));
        assertEquals(1500, binder.getFrequencyRange().getStartMHz());
        assertEquals(2500, binder.getFrequencyRange().getEndMHz());
    }

    @Test
    void startAtMaxCannotCrossEnd() {
        FrequencySelectorPanel start = new FrequencySelectorPanel(1, 7250, 1, 2000);
        FrequencySelectorPanel end = new FrequencySelectorPanel(1, 7250, 1, 7250);
        QuickFrequencySelectorPanel quick = new QuickFrequencySelectorPanel();
        new FrequencySelectorRangeBinder(start, end, quick);

        assertFalse(start.setValue(7250));
        assertEquals(2000, start.getValue());
        assertEquals(7250, end.getValue());
    }
}
