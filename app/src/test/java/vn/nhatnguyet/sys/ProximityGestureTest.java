package vn.nhatnguyet.sys;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProximityGestureTest {
    private static final int NONE = ProximityGesture.NONE;
    private static final int TOUCH = ProximityGesture.TOUCH;
    private static final int WAKE = ProximityGesture.WAKE;
    private static final int SLEEP = ProximityGesture.SLEEP;

    @Test
    public void tapWhileAwakeSleeps() {
        ProximityGesture g = new ProximityGesture();
        assertEquals(TOUCH, g.onSample(true, 10_000, true));
        assertEquals(SLEEP, g.onSample(false, 10_300, true));
    }

    @Test
    public void coverThatWakesDoesNotAlsoSleep() {
        ProximityGesture g = new ProximityGesture();
        assertEquals(WAKE, g.onSample(true, 10_000, false));
        assertEquals(NONE, g.onSample(false, 10_300, true));
        // Che lại ngay sau khi vừa bật: chưa tắt, tránh bật rồi tắt liên tục.
        assertEquals(TOUCH, g.onSample(true, 11_000, true));
        assertEquals(NONE, g.onSample(false, 11_300, true));
        assertEquals(TOUCH, g.onSample(true, 13_000, true));
        assertEquals(SLEEP, g.onSample(false, 13_300, true));
    }

    @Test
    public void longCoverIsIgnored() {
        ProximityGesture g = new ProximityGesture();
        assertEquals(TOUCH, g.onSample(true, 10_000, true));
        assertEquals(NONE, g.onSample(false, 12_000, true));
    }

    @Test
    public void repeatedNearSamplesCountOnce() {
        ProximityGesture g = new ProximityGesture();
        assertEquals(TOUCH, g.onSample(true, 10_000, true));
        assertEquals(NONE, g.onSample(true, 10_100, true));
        assertEquals(SLEEP, g.onSample(false, 10_400, true));
        assertEquals(NONE, g.onSample(false, 10_500, true));
    }

    @Test
    public void wakeIsDebounced() {
        ProximityGesture g = new ProximityGesture();
        assertEquals(WAKE, g.onSample(true, 10_000, false));
        assertEquals(NONE, g.onSample(false, 10_200, false));
        assertEquals(NONE, g.onSample(true, 11_000, false));
        assertEquals(NONE, g.onSample(false, 11_200, false));
        assertEquals(WAKE, g.onSample(true, 12_000, false));
    }
}
