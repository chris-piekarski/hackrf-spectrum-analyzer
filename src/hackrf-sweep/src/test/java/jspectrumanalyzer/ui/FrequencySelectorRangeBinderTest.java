package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.beans.PropertyVetoException;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.core.FrequencyRange;

class FrequencySelectorRangeBinderTest {

    private static JButton buttonNamed(QuickFrequencySelectorPanel panel, String text) {
        for (Component child : panel.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
        }
        fail("No button labeled " + text);
        return null;
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

        Object[][] expected = {
                { "WiFi 2", 2401, 2495 },
                { "WiFi 5", 5030, 5875 },
                { "LTE-1", 1890, 2200 },
                { "LTE-2", 663, 915 },
                { "FM", 88, 108 },
                { "NFC", 13, 14 },
                { "HF", 3, 30 },
                { "VHF", 30, 300 },
                { "UHF", 300, 3000 },
                { "V-TV", 54, 216 },
                { "U-TV", 470, 890 },
        };
        for (Object[] row : expected) {
            buttonNamed(quick, (String) row[0]).doClick();
            assertEquals(row[1], binder.getFrequencyRange().getStartMHz(), row[0] + " start");
            assertEquals(row[2], binder.getFrequencyRange().getEndMHz(), row[0] + " end");
            assertEquals(row[0], quick.getValue());
        }
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
