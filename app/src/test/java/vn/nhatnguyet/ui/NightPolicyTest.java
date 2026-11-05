package vn.nhatnguyet.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vn.nhatnguyet.Prefs;

public class NightPolicyTest {
    private static final long MIN = 60_000L;

    @Test
    public void daytimeAlwaysOnWithSystemOrFixedBrightness() {
        NightPolicy p = NightPolicy.decide(false, Prefs.NIGHT_OFF, 99 * MIN, 2 * MIN, 0, 15, -1, false, false);
        assertTrue(p.keepOn);
        assertEquals(NightPolicy.BRIGHT_SYSTEM, p.brightness, 0f);
        assertFalse(p.lockNow);
        p = NightPolicy.decide(false, Prefs.NIGHT_OFF, 0, 2 * MIN, 80, 15, -1, false, false);
        assertEquals(0.8f, p.brightness, 0.001f);
    }

    @Test
    public void nightDimKeepsScreenOnAtNightBrightness() {
        NightPolicy p = NightPolicy.decide(true, Prefs.NIGHT_DIM, 99 * MIN, 2 * MIN, 0, 15, -1, false, false);
        assertTrue(p.keepOn);
        assertEquals(0.15f, p.brightness, 0.001f);
        assertFalse(p.lockNow);
        // Độ sáng đêm không bao giờ về 0 (màn vẫn phải thấy).
        assertEquals(0.01f, NightPolicy.decide(true, Prefs.NIGHT_DIM, 0, MIN, 0, 0, -1, false, false).brightness, 0.001f);
    }

    @Test
    public void nightOffLocksOnceAfterIdle() {
        NightPolicy active = NightPolicy.decide(true, Prefs.NIGHT_OFF, MIN, 2 * MIN, 0, 15, -1, false, false);
        assertTrue(active.keepOn);
        assertFalse(active.lockNow);
        NightPolicy idle = NightPolicy.decide(true, Prefs.NIGHT_OFF, 3 * MIN, 2 * MIN, 0, 15, -1, false, false);
        assertFalse(idle.keepOn);
        assertTrue(idle.lockNow);
        NightPolicy again = NightPolicy.decide(true, Prefs.NIGHT_OFF, 4 * MIN, 2 * MIN, 0, 15, -1, false, true);
        assertFalse(again.keepOn);
        assertFalse("chỉ khoá một lần mỗi đợt rảnh", again.lockNow);
        // Chế độ "theo thời gian chờ của máy": không giữ sáng, không gọi lockNow.
        NightPolicy timeout = NightPolicy.decide(true, Prefs.NIGHT_TIMEOUT, 3 * MIN, 2 * MIN, 0, 15, -1, false, false);
        assertFalse(timeout.keepOn);
        assertFalse(timeout.lockNow);
    }

    @Test
    public void liveDragAndBlackoutOverride() {
        NightPolicy drag = NightPolicy.decide(true, Prefs.NIGHT_DIM, 0, MIN, 0, 15, 60, false, false);
        assertEquals(0.6f, drag.brightness, 0.001f);
        NightPolicy dark = NightPolicy.decide(false, Prefs.NIGHT_DIM, 0, MIN, 50, 15, 60, true, false);
        assertFalse(dark.keepOn);
        assertEquals(NightPolicy.BRIGHT_OFF, dark.brightness, 0f);
        assertFalse(dark.lockNow);
    }
}
