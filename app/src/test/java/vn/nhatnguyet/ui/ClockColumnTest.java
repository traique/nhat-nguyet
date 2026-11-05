package vn.nhatnguyet.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import vn.nhatnguyet.lunar.Lunar;

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
}
