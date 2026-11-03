package vn.nhatnguyet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrefsTest {
    @Test
    public void pinIsStoredAsStableHash() {
        String h = Prefs.hashPin("1234");
        assertEquals(64, h.length());
        assertTrue(h.matches("[0-9a-f]+"));
        assertEquals(h, Prefs.hashPin("1234"));
        assertFalse(h.equals(Prefs.hashPin("4321")));
        assertFalse(h.contains("1234"));
    }
}
