package vn.nhatnguyet.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.PersonalDays;

/** Dòng sự kiện dưới đồng hồ: bản rút gọn cho màn hẹp không được làm người xem tưởng ngày lễ là hôm nay. */
public class ClockColumnTest {
    @Test
    public void countdownAlwaysKeepsWhen() {
        List<Lunar.Upcoming> up = Lunar.upcoming(5, 11, 2026, 1, true);
        Lunar.Upcoming u = up.get(0);
        assertEquals("Ngày Văn hóa Việt Nam", u.holiday.name);
        String[] v = MainActivity.countdown(u);
        assertEquals("Còn 19 ngày: Ngày Văn hóa Việt Nam", v[0]);
        for (String s : v) assertTrue(s, s.contains("19 ngày") || s.startsWith("24/11"));
        String[] tomorrow = MainActivity.countdown(Lunar.upcoming(23, 11, 2026, 1, true).get(0));
        for (String s : tomorrow) assertTrue(s, s.toLowerCase().contains("mai"));
    }

    private static MainActivity.Note note(int d, int m, int y, int soon) {
        return MainActivity.dayNote(d, m, y, Lunar.solarToLunar(d, m, y), soon, soon, false);
    }

    @Test
    public void flipClockOnlyMentionsWhatIsNear() {
        Lunar.setPersonal(new ArrayList<Lunar.Holiday>());
        // Cột đồng hồ đếm ngược xa; đồng hồ lật (7 ngày) thì 5/11/2026 không có gì để nhắc.
        assertEquals("Còn 19 ngày: Ngày Văn hóa Việt Nam", note(5, 11, 2026, 45).text[0]);
        assertNull(note(5, 11, 2026, 7));
        assertEquals("Ngày mai: Giải phóng miền Nam", note(29, 4, 2026, 7).text[0]);
        assertEquals("Rằm tháng Mười", note(23, 11, 2026, 7).text[0]); // rằm, mùng 1 trước đếm ngược
        assertEquals("Hôm nay: Ngày Văn hóa Việt Nam", note(24, 11, 2026, 7).text[0]);
        assertEquals("Hôm nay: Nghỉ bù Giỗ Tổ Hùng Vương", note(27, 4, 2026, 7).text[0]);
        // Ngày giỗ nhắc trước một tuần ở cả hai nơi.
        Lunar.setPersonal(PersonalDays.parse("7/11 Sinh nhật mẹ", null));
        try {
            assertEquals("Còn 2 ngày: Sinh nhật mẹ", note(5, 11, 2026, 7).text[0]);
        } finally {
            Lunar.setPersonal(new ArrayList<Lunar.Holiday>());
        }
    }
}
