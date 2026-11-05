package vn.nhatnguyet.sys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatteryTest {
    private static final long MIN = 60_000L;
    private static final long T0 = 1_790_000_000_000L;

    @Test
    public void guardDimsWhenHotWithHysteresis() {
        BatteryGuard g = new BatteryGuard();
        assertFalse(g.update(80, true, 38f));
        assertEquals(0, g.cap());
        assertTrue(g.update(80, true, 42f));
        assertEquals(BatteryGuard.CAP_HOT, g.cap());
        assertTrue(g.hot());
        assertFalse("41°C vẫn trong khoảng trễ", g.update(80, true, 41f));
        assertEquals(BatteryGuard.CAP_HOT, g.cap());
        assertTrue(g.update(80, true, 46f));
        assertEquals(BatteryGuard.CAP_VERY_HOT, g.cap());
        assertTrue(g.update(80, true, 39.5f));
        assertEquals(0, g.cap());
    }

    @Test
    public void guardDimsWhenLowAndNotCharging() {
        BatteryGuard g = new BatteryGuard();
        g.update(21, false, Float.NaN);
        assertEquals(0, g.cap());
        g.update(20, false, Float.NaN);
        assertEquals(BatteryGuard.CAP_LOW, g.cap());
        g.update(23, false, Float.NaN);
        assertEquals("trong khoảng trễ", BatteryGuard.CAP_LOW, g.cap());
        g.update(23, true, Float.NaN);
        assertEquals("cắm sạc là thôi", 0, g.cap());
        g.update(-1, false, Float.NaN);
        assertEquals(0, g.cap());
    }

    @Test
    public void statsSkipSamplesWithinFiveMinutesUnlessStateChanges() {
        BatteryStats s = new BatteryStats(null);
        s.record(T0, 80, false, 35f, true);
        s.record(T0 + MIN, 79, false, 35f, true);           // bỏ qua: chưa đủ 5 phút
        s.record(T0 + 2 * MIN, 79, false, 35f, false);      // màn tắt: ghi
        s.record(T0 + 3 * MIN, 79, true, 35f, false);       // cắm sạc: ghi
        assertEquals(3, s.summarize(T0 + 4 * MIN).samples);
    }

    @Test
    public void statsComputeDrainAndChargeRates() {
        BatteryStats s = new BatteryStats(null);
        long t = T0;
        int pct = 90;
        // 1 giờ màn sáng, không sạc: tụt 12% (mỗi 5 phút 1%).
        for (int i = 0; i <= 12; i++) s.record(t + i * 5 * MIN, pct - i, false, 36f, true);
        t += 60 * MIN;
        pct -= 12;
        // Màn tắt 4 giờ: tụt 8%.
        s.record(t + MIN, pct, false, 34f, false);
        s.record(t + 4 * 60 * MIN + MIN, pct - 8, false, 33f, true);
        t += 4 * 60 * MIN + MIN;
        pct -= 8;
        // Sạc 30 phút: lên 20%.
        for (int i = 0; i <= 6; i++) s.record(t + MIN + i * 5 * MIN, pct + i * 20 / 6, true, 41f, true);
        BatteryStats.Summary sum = s.summarize(t + 40 * MIN);
        assertEquals(12, sum.drainScreenOn, 0.6);
        assertEquals(2, sum.drainScreenOff, 0.1);
        assertEquals(40, sum.chargeRate, 3);
        assertEquals(41f, sum.maxTemp, 0.01f);
        String text = BatteryStats.describe(sum, 90, true, 40f);
        assertTrue(text, text.contains("Màn sáng, rút sạc: tụt khoảng 12%/giờ"));
        assertTrue(text, text.contains("Nóng nhất 24 giờ qua: 41°C"));
        assertTrue(text, text.contains("từ 50% lên 80% mất khoảng"));
    }

    @Test
    public void statsIgnoreLongGapsAndOldSamples() {
        BatteryStats s = new BatteryStats(null);
        s.record(T0, 80, false, 30f, true);
        s.record(T0 + 3 * 60 * MIN, 50, false, 30f, true); // màn "sáng" mà cách 3 giờ: thiếu số liệu, bỏ
        BatteryStats.Summary sum = s.summarize(T0 + 3 * 60 * MIN + MIN);
        assertTrue(Double.isNaN(sum.drainScreenOn));
        assertTrue(BatteryStats.describe(sum, 50, false, Float.NaN).contains("Chưa đủ số liệu"));
        // Quá 24 giờ thì không tính.
        assertEquals(0, s.summarize(T0 + 30 * 60 * MIN).samples);
    }

    @Test
    public void formatsRatesAndDurations() {
        assertEquals("2,5", BatteryStats.rate(2.5));
        assertEquals("14", BatteryStats.rate(14.2));
        assertEquals("45 phút", BatteryStats.hours(0.75));
        assertEquals("2 giờ", BatteryStats.hours(2.0));
        assertEquals("2,5 giờ", BatteryStats.hours(2.5));
        assertEquals("12 giờ", BatteryStats.hours(12.3));
    }
}
