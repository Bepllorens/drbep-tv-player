package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class U7dNavigationMemoryTest {
    @Test public void keepsDayAndRowSeparatelyForEachChannel() {
        U7dNavigationMemory memory = new U7dNavigationMemory();
        memory.save("a", "Friday", "23:30 programme");
        memory.save("b", "Saturday", "12:00 news");
        assertEquals("Friday", memory.get("a").day);
        assertEquals("23:30 programme", memory.get("a").rowKey);
        assertEquals("Saturday", memory.get("b").day);
        assertNull(memory.get("missing"));
    }
    @Test public void selectingDayCanClearOldRowWithoutRemovingDay() {
        U7dNavigationMemory memory = new U7dNavigationMemory();
        memory.save("a", "Friday", "row");
        memory.save("a", "Thursday", "");
        assertEquals("Thursday", memory.get("a").day);
        assertEquals("", memory.get("a").rowKey);
    }
    @Test public void historyIsBoundedAndRecentlyUpdatedEntryIsKept() {
        U7dNavigationMemory memory = new U7dNavigationMemory();
        for (int i=0; i<16; i++) memory.save("c"+i, "day", "row");
        memory.save("c0", "day", "new");
        memory.save("c16", "day", "row");
        assertNull(memory.get("c1"));
        assertEquals("new", memory.get("c0").rowKey);
    }
}
