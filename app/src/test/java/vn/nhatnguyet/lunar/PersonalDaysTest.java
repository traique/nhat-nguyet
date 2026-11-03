package vn.nhatnguyet.lunar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PersonalDaysTest {
    @After
    public void clear() {
        Lunar.setPersonal(Collections.<Lunar.Holiday>emptyList());
    }

    @Test
    public void parsesLunarAndSolarLines() {
        List<Integer> bad = new ArrayList<Integer>();
        List<Lunar.Holiday> days = PersonalDays.parse(
                "15/8 âm Giỗ ông nội\n20/11: Sinh nhật mẹ\n\n1-3 ÂL Giỗ bà\n3/3 am lich - Giỗ cụ\n20/11 Amy\nabc\n31/2 Sai\n10/13 âm Sai", bad);
        assertEquals(5, days.size());
        assertTrue(days.get(0).lunar);
        assertEquals("Giỗ ông nội", days.get(0).name);
        assertEquals(15, days.get(0).day);
        assertEquals(8, days.get(0).month);
        assertFalse(days.get(1).lunar);
        assertEquals("Sinh nhật mẹ", days.get(1).name);
        assertTrue(days.get(2).lunar);
        assertEquals("Giỗ bà", days.get(2).name);
        assertTrue(days.get(3).lunar);
        assertEquals("Giỗ cụ", days.get(3).name);
        assertFalse("\"Am\" đầu tên không phải chữ âm", days.get(4).lunar);
        assertEquals("Amy", days.get(4).name);
        assertEquals(Arrays.asList(7, 8, 9), bad);
    }

    @Test
    public void lunarDayFindsNextOccurrence() {
        // 15/8 âm năm 2026 là Tết Trung thu, rơi vào 25/9/2026.
        Lunar.Upcoming u = Lunar.next(Lunar.Holiday.personal("Giỗ", true, 15, 8), 1, 9, 2026);
        assertEquals(25, u.solar[0]);
        assertEquals(9, u.solar[1]);
        assertEquals(24, u.days);
    }

    @Test
    public void day30FallsBackInShortMonth() {
        Lunar.Holiday h = Lunar.Holiday.personal("Giỗ", true, 30, 9);
        for (int y = 2024; y <= 2030; y++) {
            int[] last = Lunar.lunarToSolar(29, 9, y, false);
            LunarDate ld = Lunar.solarToLunar(last[0], last[1], last[2]);
            boolean shortMonth = Lunar.lunarMonthLength(ld) == 29;
            assertEquals("năm " + y, shortMonth, h.on(last[0], last[1], ld));
        }
    }

    @Test
    public void personalComesFirstAndCountsDown() {
        Lunar.setPersonal(PersonalDays.parse("2/9 Sinh nhật bố\n5/9 Kỷ niệm cưới", null));
        LunarDate ld = Lunar.solarToLunar(2, 9, 2026);
        assertEquals("Sinh nhật bố", Lunar.eventOn(2, 9, ld).name);
        assertEquals("Quốc khánh", Lunar.holidayOn(2, 9, ld).name);
        Lunar.Upcoming soon = Lunar.nextPersonal(1, 9, 2026, 7);
        assertEquals("Sinh nhật bố", soon.holiday.name);
        assertEquals(1, soon.days);
        assertNull(Lunar.nextPersonal(6, 9, 2026, 7));
    }
}
