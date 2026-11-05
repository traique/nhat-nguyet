package vn.nhatnguyet.sys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

import vn.nhatnguyet.Prefs;

public class SchedulerTest {
    private static Calendar at(int y, int m, int d, int h, int min) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, h, min, 0);
        return c;
    }

    private static String fmt(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format("%d/%d %02d:%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    @Test
    public void morningOnly() {
        // Thứ Năm 5/11/2026, 23:00: báo thức 6:00 sáng hôm sau.
        assertEquals("6/11 06:00", fmt(Scheduler.nextWake(at(2026, 11, 5, 23, 0), true, 6, false, 8, 17, true)));
        assertEquals(0, Scheduler.nextWake(at(2026, 11, 5, 23, 0), false, 6, false, 8, 17, true));
    }

    @Test
    public void awayEndOnWorkdaysOnly() {
        // Thứ Năm 10:00 đang vắng: báo thức 17:00 cùng ngày (trước 6:00 hôm sau).
        assertEquals("5/11 17:00", fmt(Scheduler.nextWake(at(2026, 11, 5, 10, 0), true, 6, true, 8, 17, true)));
        // Thứ Bảy 7/11 10:00: không vắng, báo thức kế tiếp là 6:00 Chủ nhật.
        assertEquals("8/11 06:00", fmt(Scheduler.nextWake(at(2026, 11, 7, 10, 0), true, 6, true, 8, 17, true)));
        // Chỉ hết giờ vắng (không bật đầu giờ ngày): từ thứ Sáu 18:00 nhảy qua cuối tuần sang thứ Hai 9/11.
        assertEquals("9/11 17:00", fmt(Scheduler.nextWake(at(2026, 11, 6, 18, 0), false, 6, true, 8, 17, true)));
        // Ngày Văn hóa Việt Nam 24/11/2026 (thứ Ba, nghỉ lễ): không tính giờ vắng, báo thức 17h ngày 25/11.
        assertEquals("25/11 17:00", fmt(Scheduler.nextWake(at(2026, 11, 23, 18, 0), false, 6, true, 8, 17, true)));
    }

    @Test
    public void awayDaysAndHours() {
        assertTrue(Prefs.isAwayDay(at(2026, 11, 5, 10, 0), true));   // thứ Năm
        assertFalse(Prefs.isAwayDay(at(2026, 11, 8, 10, 0), true));  // Chủ nhật
        assertFalse(Prefs.isAwayDay(at(2026, 11, 24, 10, 0), true)); // Ngày Văn hóa Việt Nam
        assertFalse(Prefs.isAwayDay(at(2026, 9, 2, 10, 0), true));   // Quốc khánh
        assertTrue(Prefs.isAwayDay(at(2026, 11, 20, 10, 0), true));  // 20/11 không phải ngày nghỉ
        assertTrue(Prefs.isAwayDay(at(2026, 11, 8, 10, 0), false));  // không giới hạn ngày
        assertTrue(Prefs.inHours(8, 8, 17));
        assertFalse(Prefs.inHours(17, 8, 17));
        assertTrue(Prefs.inHours(23, 22, 6));
        assertTrue(Prefs.inHours(2, 22, 6));
        assertFalse(Prefs.inHours(10, 22, 6));
        assertFalse(Prefs.inHours(10, 9, 9));
    }
}
