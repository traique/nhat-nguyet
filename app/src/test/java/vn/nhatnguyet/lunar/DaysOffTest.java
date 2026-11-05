package vn.nhatnguyet.lunar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Đối chiếu với Bộ luật Lao động 2019, Nghị quyết 28/2026/QH16 và lịch nghỉ khối nhà nước đã công bố. */
public class DaysOffTest {
    private static boolean red(int d, int m, int y) {
        return DaysOff.isRed(d, m, y, Lunar.solarToLunar(d, m, y));
    }

    @Test
    public void weekdayFromJulianDay() {
        assertTrue("1/1/2000 là thứ Bảy", DaysOff.weekend(Lunar.jdFromDate(1, 1, 2000)));
        assertTrue("26/4/2026 là Chủ nhật", DaysOff.weekend(Lunar.jdFromDate(26, 4, 2026)));
        assertFalse("27/4/2026 là thứ Hai", DaysOff.weekend(Lunar.jdFromDate(27, 4, 2026)));
    }

    @Test
    public void year2026MatchesOfficialSchedule() {
        assertEquals("Tết Dương lịch", DaysOff.name(1, 1, 2026));
        assertNull(DaysOff.name(2, 1, 2026));
        // Tết Bính Ngọ: nghỉ 16/2 (29 tháng Chạp) đến 20/2 (mùng 4); 14–15 và 21–22 là cuối tuần.
        for (int d = 16; d <= 20; d++) assertEquals("Nghỉ Tết Nguyên đán", DaysOff.name(d, 2, 2026));
        assertNull(DaysOff.name(14, 2, 2026));
        assertNull(DaysOff.name(23, 2, 2026));
        // Giỗ Tổ 10/3 âm = Chủ nhật 26/4: nghỉ bù thứ Hai 27/4.
        assertArrayEquals2(new int[]{26, 4, 2026}, Lunar.lunarToSolar(10, 3, 2026, false));
        assertEquals("Giỗ Tổ Hùng Vương", DaysOff.name(26, 4, 2026));
        assertEquals("Nghỉ bù Giỗ Tổ Hùng Vương", DaysOff.name(27, 4, 2026));
        assertEquals("Giải phóng miền Nam", DaysOff.name(30, 4, 2026));
        assertEquals("Quốc tế Lao động", DaysOff.name(1, 5, 2026));
        assertNull(DaysOff.name(4, 5, 2026));
        // Quốc khánh: 1/9 và 2/9; nghỉ hoán đổi thứ Hai 31/8, làm bù thứ Bảy 22/8 (Thông báo 9441/TB-BNV).
        assertEquals("Quốc khánh", DaysOff.name(1, 9, 2026));
        assertEquals("Quốc khánh", DaysOff.name(2, 9, 2026));
        assertNull(DaysOff.name(3, 9, 2026));
        assertEquals("Nghỉ hoán đổi", DaysOff.name(31, 8, 2026));
        assertTrue(DaysOff.isMakeUpWorkday(22, 8, 2026));
        assertFalse(DaysOff.isMakeUpWorkday(29, 8, 2026));
        // Ngày Văn hóa Việt Nam: thứ Ba 24/11, nghỉ 1 ngày, không hoán đổi.
        assertEquals("Ngày Văn hóa Việt Nam", DaysOff.name(24, 11, 2026));
        assertNull(DaysOff.name(23, 11, 2026));
        assertNull(DaysOff.name(25, 11, 2026));
        assertTrue(DaysOff.hasOfficial(2026));
    }

    @Test
    public void year2027MatchesOfficialSchedule() {
        // Tết Đinh Mùi: 2 ngày cuối năm và mùng 1–3 (4–8/2); mùng 1, 2 rơi thứ Bảy, Chủ nhật nên nghỉ bù 9–10/2.
        for (int d = 4; d <= 8; d++) assertEquals("Nghỉ Tết Nguyên đán", DaysOff.name(d, 2, 2027));
        assertEquals("Nghỉ bù Tết Nguyên đán", DaysOff.name(9, 2, 2027));
        assertEquals("Nghỉ bù Tết Nguyên đán", DaysOff.name(10, 2, 2027));
        assertNull(DaysOff.name(3, 2, 2027));
        assertNull(DaysOff.name(11, 2, 2027));
        // Quốc khánh: 2/9 (thứ Năm) và 3/9 (thứ Sáu).
        assertEquals("Quốc khánh", DaysOff.name(3, 9, 2027));
        assertNull(DaysOff.name(1, 9, 2027));
        // 1/5/2027 là thứ Bảy: nghỉ bù thứ Hai 3/5 theo khoản 3 Điều 111.
        assertEquals("Nghỉ bù Quốc tế Lao động", DaysOff.name(3, 5, 2027));
        int[] g = Lunar.lunarToSolar(10, 3, 2027, false);
        assertEquals("Giỗ Tổ Hùng Vương", DaysOff.name(g[0], g[1], g[2]));
    }

    @Test
    public void unannouncedYearMarksOnlyCertainDays() {
        assertFalse(DaysOff.hasOfficial(2028));
        // 1/1/2028 là thứ Bảy: nghỉ bù thứ Hai 3/1.
        assertEquals("Nghỉ bù Tết Dương lịch", DaysOff.name(3, 1, 2028));
        // Tết Mậu Thân: mùng 1 là 26/1/2028. Ngày cuối năm, mùng 1, mùng 2 chắc chắn nghỉ (có trong mọi phương án).
        int[] t = Lunar.lunarToSolar(1, 1, 2028, false);
        assertEquals(26, t[0]);
        assertEquals("Nghỉ Tết Nguyên đán", DaysOff.name(25, 1, 2028));
        assertEquals("Nghỉ Tết Nguyên đán", DaysOff.name(27, 1, 2028));
        assertNull("mùng 3 tuỳ phương án", DaysOff.name(28, 1, 2028));
        assertTrue(DaysOff.likelyOff(28, 1, 2028));
        assertTrue("3 ngày cuối năm là một phương án", DaysOff.likelyOff(23, 1, 2028));
        assertFalse(DaysOff.likelyOff(2, 2, 2028));
        // 2/9/2028 là thứ Bảy: ngày liền kề chưa biết, nhưng 1/9 và thứ Hai 4/9 đều có thể nghỉ.
        assertTrue(DaysOff.likelyOff(1, 9, 2028));
        assertTrue(DaysOff.likelyOff(4, 9, 2028));
        assertNull(DaysOff.name(1, 9, 2028));
    }

    @Test
    public void cultureDayStartsIn2026WithoutCompensation() {
        assertNull(DaysOff.name(24, 11, 2025));
        // 24/11/2029 là thứ Bảy: chưa có nghỉ bù vì ngày này chưa nằm trong Điều 112 Bộ luật Lao động.
        assertEquals("Ngày Văn hóa Việt Nam", DaysOff.name(24, 11, 2029));
        assertNull(DaysOff.name(26, 11, 2029));
    }

    @Test
    public void redOnlyForDaysOff() {
        assertTrue(red(1, 1, 2026));
        assertTrue(red(27, 4, 2026));       // nghỉ bù
        assertTrue(red(17, 2, 2026));       // mùng 1 Tết
        assertFalse(red(14, 2, 2026));      // Valentine: không phải ngày nghỉ
        assertFalse(red(8, 3, 2026));       // Quốc tế Phụ nữ
        assertFalse(red(20, 11, 2026));     // Nhà giáo Việt Nam
        assertFalse(red(25, 12, 2026));     // Giáng sinh
        assertFalse(red(25, 9, 2026));      // Trung thu
        assertTrue(red(24, 11, 2026));      // Ngày Văn hóa Việt Nam
    }

    private static void assertArrayEquals2(int[] want, int[] got) {
        assertEquals(want.length, got.length);
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i]);
    }
}
