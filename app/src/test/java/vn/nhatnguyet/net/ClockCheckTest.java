package vn.nhatnguyet.net;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

public class ClockCheckTest {
    @After
    public void clear() {
        ClockCheck.reset();
    }

    @Test
    public void needsThreeSamples() {
        ClockCheck.sample(1_000_000 + 300_000, 1_000_000);
        ClockCheck.sample(2_000_000 + 300_000, 2_000_000);
        assertEquals(0, ClockCheck.offsetMs());
        ClockCheck.sample(3_000_000 + 300_000, 3_000_000);
        assertEquals(300_000, ClockCheck.offsetMs());
    }

    @Test
    public void oneStaleServerDoesNotWin() {
        // Một máy chủ trả Date cũ 10 phút, hai máy chủ đúng giờ: coi như máy đúng giờ.
        ClockCheck.sample(1_000_000 - 600_000, 1_000_000);
        ClockCheck.sample(2_000_000 + 1_000, 2_000_000);
        ClockCheck.sample(3_000_000 - 500, 3_000_000);
        assertEquals(-500, ClockCheck.offsetMs());
    }
}
