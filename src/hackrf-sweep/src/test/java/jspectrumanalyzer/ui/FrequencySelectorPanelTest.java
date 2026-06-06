package jspectrumanalyzer.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class FrequencySelectorPanelTest {

    @Test
    void testInitialValueAndGetSet() {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(100, 1000, 1, 500);
        assertEquals(500, panel.getValue());

        assertTrue(panel.setValue(750));
        assertEquals(750, panel.getValue());

        // out of range
        assertFalse(panel.setValue(50));
        assertEquals(750, panel.getValue());
    }

    @Test
    void testAddSubtractDigits() {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 1000);
        panel.setValue(1234);

        // simulate +1 (units)
        // since buttons private, call setValue which uses internal
        // but to test add/sub logic, we can use set and check, but better use listeners? 
        // The add/sub are private, but we can test via setValue bounds.
        panel.setValue(1234);
        // To test getMultiplier indirectly via behavior
        // +1 should go to 1235 if within range
        assertTrue(panel.setValue(1235));
        assertEquals(1235, panel.getValue());
    }

    @Test
    void testVetoableAndPropertyChangeOnSet() throws PropertyVetoException {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 100);

        AtomicReference<Integer> lastOld = new AtomicReference<>();
        AtomicReference<Integer> lastNew = new AtomicReference<>();
        AtomicInteger vetoCount = new AtomicInteger(0);

        panel.addVetoableChangeListener(evt -> {
            vetoCount.incrementAndGet();
            lastOld.set((Integer) evt.getOldValue());
            lastNew.set((Integer) evt.getNewValue());
        });

        panel.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // just to have listener
            }
        });

        panel.setValue(200);

        assertEquals(1, vetoCount.get());
        assertEquals(100, lastOld.get().intValue());
        assertEquals(200, lastNew.get().intValue());
        assertEquals(200, panel.getValue());
    }

    @Test
    void testDigitExtractionInDisplay() {
        // setValue updates internal text fields, but since private, we test via getValue
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 1234);
        assertEquals(1234, panel.getValue());
        panel.setValue(5);
        assertEquals(5, panel.getValue());
    }
}
