package shared.mvc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

class ModelValueTest {

    @Test
    void testBasicSetGetAndListener() {
        ModelValueInt mv = new ModelValueInt("test", 10);
        assertEquals(10, mv.getValue());
        assertEquals("test", mv.getName());

        AtomicInteger calls = new AtomicInteger(0);
        mv.addListener(v -> calls.incrementAndGet());

        mv.setValue(20);
        assertEquals(20, mv.getValue());
        assertEquals(1, calls.get());
    }

    @Test
    void testBooleanModel() {
        ModelValueBoolean b = new ModelValueBoolean("flag", true);
        assertTrue(b.getValue());

        b.setValue(false);
        assertFalse(b.getValue());
    }

    @Test
    void testIntWithBounds() {
        ModelValueInt i = new ModelValueInt("gain", 5, 1, 0, 10);
        assertEquals(5, i.getValue());
        assertEquals(1, i.getStep());
        assertEquals(0, i.getMin());
        assertEquals(10, i.getMax());
    }

    @Test
    void testListenerRemovalAndMultipleListeners() {
        ModelValueInt mv = new ModelValueInt("val", 0);
        java.util.concurrent.atomic.AtomicInteger c1 = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger c2 = new java.util.concurrent.atomic.AtomicInteger(0);

        ModelValue.Listener<Integer> l1 = v -> c1.incrementAndGet();
        ModelValue.Listener<Integer> l2 = v -> c2.incrementAndGet();

        mv.addListener(l1);
        mv.addListener(l2);
        mv.setValue(1);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());

        mv.removeListener(l1);
        mv.setValue(2);
        assertEquals(1, c1.get());
        assertEquals(2, c2.get());
    }
}
