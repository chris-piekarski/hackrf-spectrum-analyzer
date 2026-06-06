package jspectrumanalyzer.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FrequencyAllocationsTest {

    @Test
    void testLoadsBuiltinTables() {
        FrequencyAllocations allocs = new FrequencyAllocations();
        var tables = allocs.getTable();

        assertTrue(tables.size() >= 2, "Should have at least Europe and USA tables");
        assertTrue(tables.containsKey("Europe"));
        assertTrue(tables.containsKey("USA"));

        FrequencyAllocationTable europe = tables.get("Europe");
        assertNotNull(europe);
        assertTrue(europe.getBandCount() > 10);
    }
}
