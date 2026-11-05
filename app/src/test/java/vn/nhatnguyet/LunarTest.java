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
    public void tetHolidaysCoverThreeDaysAndNewYearsEve() {
        // Tết Bính Ngọ: mùng 1 là 17/2/2026; tháng Chạp Ất Tỵ thiếu nên 29 Tết (16/2/2026) là Giao thừa.
        assertEquals("Tết Nguyên đán", Lunar.holidayOn(17, 2, Lunar.solarToLunar(17, 2, 2026)).name);
        assertEquals("Mùng 2 Tết", Lunar.holidayOn(18, 2, Lunar.solarToLunar(18, 2, 2026)).name);
        assertEquals("Mùng 3 Tết", Lunar.holidayOn(19, 2, Lunar.solarToLunar(19, 2, 2026)).name);
        assertNull(Lunar.holidayOn(20, 2, Lunar.solarToLunar(20, 2, 2026)));
        assertEquals("Giao thừa", Lunar.holidayOn(16, 2, Lunar.solarToLunar(16, 2, 2026)).name);
        // Tháng Chạp Giáp Thìn thiếu (29 ngày): 29 Tết 28/1/2025 tính là Giao thừa.
        assertEquals("Giao thừa", Lunar.holidayOn(28, 1, Lunar.solarToLunar(28, 1, 2025)).name);
        assertTrue(Lunar.isTet(Lunar.holidayOn(18, 2, Lunar.solarToLunar(18, 2, 2026))));
        assertFalse(Lunar.isTet(Lunar.HOLIDAYS[0]));
    }

    @Test
    public void vietnamCultureDayFrom2026() {
        Lunar.Holiday h = Lunar.holidayOn(24, 11, Lunar.solarToLunar(24, 11, 2026));
        assertEquals("Ngày Văn hóa Việt Nam", h.name);
        assertTrue("ngày nghỉ hưởng lương như 30/4, 2/9", h.major);
        assertEquals("Ngày Văn hóa Việt Nam", Lunar.holidayOn(24, 11, Lunar.solarToLunar(24, 11, 2030)).name);
        // Trước khi có Nghị quyết 28/2026/QH16 thì không phải ngày lễ.
        assertNull(Lunar.holidayOn(24, 11, Lunar.solarToLunar(24, 11, 2025)));
        // Bảng lịch đếm ngược lễ lớn: từ 1/11/2026 thì lễ lớn kế tiếp là 24/11.
        List<Lunar.Upcoming> up = Lunar.upcoming(1, 11, 2026, 1, true);
        assertEquals("Ngày Văn hóa Việt Nam", up.get(0).holiday.name);
        assertEquals(23, up.get(0).days);
    }

    @Test
    public void solarTermsNearMidnight() {
        // Bạch lộ 2014 bắt đầu 00:01 ngày 8/9 giờ VN: không có hiệu chỉnh chương động sẽ lệch sang 7/9.
        assertTrue(Lunar.solarTermStarts(Lunar.jdFromDate(8, 9, 2014)));
        assertFalse(Lunar.solarTermStarts(Lunar.jdFromDate(7, 9, 2014)));
        // Sương giáng 2019: 24/10.
        assertTrue(Lunar.solarTermStarts(Lunar.jdFromDate(24, 10, 2019)));
        assertFalse(Lunar.solarTermStarts(Lunar.jdFromDate(23, 10, 2019)));
    }

    @Test
    public void farDatesStillConvert() {
        // Hai ngày từng trả về ngày âm 0 vì ước lượng tháng lệch quá một bước.
        for (int[] d : new int[][]{{7, 5, 2054}, {9, 4, 2062}}) {
            LunarDate ld = Lunar.solarToLunar(d[0], d[1], d[2]);
            assertTrue(ld.day >= 1 && ld.day <= 30);
            assertArrayEquals(d, Lunar.lunarToSolar(ld.day, ld.month, ld.year, ld.leap));
        }
        // 2030: sóc lúc 23:08 giờ Việt Nam ngày 2/2 nên Tết là 2/2 (Trung Quốc 3/2).
        assertTet(2030, 2, 2, 2030);
        assertTet(2033, 31, 1, 2033);
    }

    @Test
    public void tetCountdown() {
        assertEquals(1, Lunar.daysToTet(16, 2, 2026));
        assertEquals(0, Lunar.daysToTet(17, 2, 2026));
        assertEquals(353, Lunar.daysToTet(18, 2, 2026));
    }
}
