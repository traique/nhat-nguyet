package vn.nhatnguyet.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vn.nhatnguyet.Prefs;

public class NightPolicyTest {
    private static final long MIN = 60_000L;

    private static NightPolicy.Input in(boolean night, String mode, long idleMs) {
        NightPolicy.Input i = new NightPolicy.Input();
        i.night = night;
        i.mode = mode;
        i.idleMs = idleMs;
        i.idleLimitMs = 2 * MIN;
        i.dayPercent = 0;
        i.nightPercent = 15;
        return i;
    }

    @Test
    public void daytimeAlwaysOnWithSystemOrFixedBrightness() {
        NightPolicy p = NightPolicy.decide(in(false, Prefs.NIGHT_OFF, 99 * MIN));
        assertTrue(p.keepOn);
        assertEquals(NightPolicy.BRIGHT_SYSTEM, p.brightness, 0f);
        assertFalse(p.lockNow);
        NightPolicy.Input i = in(false, Prefs.NIGHT_OFF, 0);
        i.dayPercent = 80;
        assertEquals(0.8f, NightPolicy.decide(i).brightness, 0.001f);
    }

    @Test
    public void nightDimKeepsScreenOnAtNightBrightness() {
        NightPolicy p = NightPolicy.decide(in(true, Prefs.NIGHT_DIM, 99 * MIN));
        assertTrue(p.keepOn);
        assertEquals(0.15f, p.brightness, 0.001f);
        assertFalse(p.lockNow);
        NightPolicy.Input zero = in(true, Prefs.NIGHT_DIM, 0);
        zero.nightPercent = 0;
        assertEquals("độ sáng đêm không về 0", 0.01f, NightPolicy.decide(zero).brightness, 0.001f);
    }

    @Test
    public void nightOffLocksOnceAfterIdle() {
        NightPolicy active = NightPolicy.decide(in(true, Prefs.NIGHT_OFF, MIN));
        assertTrue(active.keepOn);
        assertFalse(active.lockNow);
        NightPolicy idle = NightPolicy.decide(in(true, Prefs.NIGHT_OFF, 3 * MIN));
        assertFalse(idle.keepOn);
        assertTrue(idle.lockNow);
        NightPolicy.Input again = in(true, Prefs.NIGHT_OFF, 4 * MIN);
        again.lockedAlready = true;
        assertFalse("chỉ khoá một lần mỗi đợt rảnh", NightPolicy.decide(again).lockNow);
        NightPolicy timeout = NightPolicy.decide(in(true, Prefs.NIGHT_TIMEOUT, 3 * MIN));
        assertFalse(timeout.keepOn);
        assertFalse(timeout.lockNow);
    }

    @Test
    public void awayTurnsOffInDaytimeEvenWhenNightModeIsDim() {
        NightPolicy.Input i = in(false, Prefs.NIGHT_DIM, 3 * MIN);
        i.away = true;
        i.dayPercent = 70;
        NightPolicy idle = NightPolicy.decide(i);
        assertFalse(idle.keepOn);
        assertTrue(idle.lockNow);
        i.idleMs = MIN; // có người vừa chạm: sáng như ban ngày
        NightPolicy active = NightPolicy.decide(i);
        assertTrue(active.keepOn);
        assertEquals(0.7f, active.brightness, 0.001f);
        assertFalse(active.lockNow);
    }

    @Test
    public void batteryCapLimitsAutomaticBrightnessButNotDragging() {
        NightPolicy.Input i = in(false, Prefs.NIGHT_OFF, 0);
        i.capPercent = 30;
        assertEquals("theo máy thì đặt hẳn bằng trần", 0.3f, NightPolicy.decide(i).brightness, 0.001f);
        i.dayPercent = 90;
        assertEquals(0.3f, NightPolicy.decide(i).brightness, 0.001f);
        i.dayPercent = 20;
        assertEquals("đã thấp hơn trần thì giữ", 0.2f, NightPolicy.decide(i).brightness, 0.001f);
        i.livePercent = 80;
        assertEquals("đang kéo tay thì theo tay", 0.8f, NightPolicy.decide(i).brightness, 0.001f);
    }

    @Test
    public void blackoutOverridesEverything() {
        NightPolicy.Input i = in(false, Prefs.NIGHT_DIM, 0);
        i.livePercent = 60;
        i.blackout = true;
        NightPolicy dark = NightPolicy.decide(i);
        assertFalse(dark.keepOn);
        assertEquals(NightPolicy.BRIGHT_OFF, dark.brightness, 0f);
        assertFalse(dark.lockNow);
    }
}
