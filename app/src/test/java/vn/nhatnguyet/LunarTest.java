package vn.nhatnguyet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.LunarDate;

public class LunarTest {

    private static void assertTet(int lunarYear, int d, int m, int y) {
        assertArrayEquals("Tết " + lunarYear, new int[]{d, m, y}, Lunar.lunarToSolar(1, 1, lunarYear, false));
        LunarDate ld = Lunar.solarToLunar(d, m, y);
        assertEquals(1, ld.day);
        assertEquals(1, ld.month);
        assertEquals(lunarYear, ld.year);
        assertFalse(ld.leap);
    }

    @Test
    public void tetDates() {
        assertTet(2020, 25, 1, 2020);
        assertTet(2021, 12, 2, 2021);
        assertTet(2022, 1, 2, 2022);
        assertTet(2023, 22, 1, 2023);
        assertTet(2024, 10, 2, 2024);
        assertTet(2025, 29, 1, 2025);
        assertTet(2026, 17, 2, 2026);
        assertTet(2027, 6, 2, 2027);
        // 1985: Việt Nam khác Trung Quốc (21/1 vs 20/2) – kiểm tra múi giờ +7.
        assertTet(1985, 21, 1, 1985);
    }

    @Test
    public void leapMonths() {
        // 2023 nhuận tháng 2; 2025 nhuận tháng 6.
        LunarDate a = Lunar.solarToLunar(22, 3, 2023);
        assertEquals(2, a.month);
        assertTrue(a.leap);
        assertEquals(1, a.day);
        LunarDate b = Lunar.solarToLunar(25, 7, 2025);
        assertEquals(6, b.month);
        assertTrue(b.leap);
        assertNull(Lunar.lunarToSolar(1, 3, 2025, true));
        assertArrayEquals(new int[]{25, 7, 2025}, Lunar.lunarToSolar(1, 6, 2025, true));
    }

    @Test
    public void midAutumn2026() {
        assertArrayEquals(new int[]{25, 9, 2026}, Lunar.lunarToSolar(15, 8, 2026, false));
    }

    @Test
    public void canChi() {
        assertEquals("Bính Ngọ", Lunar.yearCanChi(2026));
        assertEquals("Ất Tỵ", Lunar.yearCanChi(2025));
        assertEquals("Giáp Thìn", Lunar.yearCanChi(2024));
        // 1/1/2000 là ngày Mậu Ngọ.
        assertEquals("Mậu Ngọ", Lunar.dayCanChi(Lunar.jdFromDate(1, 1, 2000)));
        // Tháng Giêng năm Giáp Thìn 2024 là tháng Bính Dần.
        assertEquals("Bính Dần", Lunar.monthCanChi(1, 2024));
    }

    @Test
    public void solarTerms() {
        // Đông chí 2025 rơi vào 21/12/2025; Lập xuân 2026 là 4/2/2026.
        assertEquals("Đông chí", Lunar.solarTerm(Lunar.jdFromDate(21, 12, 2025)));
        assertTrue(Lunar.solarTermStarts(Lunar.jdFromDate(21, 12, 2025)));
        assertEquals("Lập xuân", Lunar.solarTerm(Lunar.jdFromDate(4, 2, 2026)));
    }

    @Test
    public void hoangDao() {
        List<String> gio = Lunar.gioHoangDao(Lunar.jdFromDate(1, 1, 2000)); // ngày Ngọ
        assertEquals(6, gio.size());
        assertEquals("Tý (23-1h)", gio.get(0));
        int jd = Lunar.jdFromDate(17, 2, 2026);
        LunarDate ld = Lunar.solarToLunar(17, 2, 2026);
        int star = Lunar.dayStar(jd, ld.month);
        assertTrue(star >= 0 && star < 12);
    }

    @Test
    public void roundTripManyDays() {
        int start = Lunar.jdFromDate(1, 1, 1990);
        int end = Lunar.jdFromDate(31, 12, 2060);
        for (int jd = start; jd <= end; jd += 7) {
            int[] s = Lunar.jdToDate(jd);
            LunarDate ld = Lunar.solarToLunar(s[0], s[1], s[2]);
            assertTrue(ld.day >= 1 && ld.day <= 30);
            assertArrayEquals(s, Lunar.lunarToSolar(ld.day, ld.month, ld.year, ld.leap));
            int len = Lunar.lunarMonthLength(ld);
            assertTrue("len " + len, len == 29 || len == 30);
        }
    }

    @Test
    public void tetCountdown() {
        assertEquals(1, Lunar.daysToTet(16, 2, 2026));
        assertEquals(0, Lunar.daysToTet(17, 2, 2026));
        assertEquals(353, Lunar.daysToTet(18, 2, 2026));
    }
}
