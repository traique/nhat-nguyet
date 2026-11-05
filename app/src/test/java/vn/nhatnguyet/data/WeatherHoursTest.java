package vn.nhatnguyet.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Dãy giờ dự báo phải tính theo giờ hiện tại, kể cả khi dữ liệu là bộ nhớ đệm cũ. */
public class WeatherHoursTest {
    private static JSONObject weather(long firstTs, int... codes) throws Exception {
        JSONObject w = new JSONObject();
        JSONObject now = new JSONObject();
        now.put("code", 1);
        w.put("now", now);
        JSONArray hours = new JSONArray();
        for (int i = 0; i < codes.length; i++) {
            JSONObject h = new JSONObject();
            h.put("h", String.format("%02d", (10 + i) % 24));
            h.put("ts", firstTs + i * 3_600_000L);
            h.put("code", codes[i]);
            h.put("rain", codes[i] >= 61 ? 80 : 0);
            hours.put(h);
        }
        w.put("hours", hours);
        return w;
    }

    @Test
    public void epochParsesVietnamTime() {
        // 2026-11-04T10:00 giờ VN = 03:00 UTC.
        assertEquals(1793761200000L, WeatherSource.epochOf("2026-11-04T10:00"));
        assertEquals(0, WeatherSource.epochOf("xx"));
        assertEquals(0, WeatherSource.epochOf(null));
    }

    @Test
    public void firstUpcomingHourFollowsClock() throws Exception {
        long t0 = 1_700_000_000_000L;
        JSONObject w = weather(t0, 1, 1, 1, 1, 61, 1);
        assertEquals(1, WeatherSource.firstUpcomingHour(w, t0 + 1_000));          // vừa tải: giờ kế tiếp
        assertEquals(3, WeatherSource.firstUpcomingHour(w, t0 + 2 * 3_600_000L + 1)); // 2 giờ sau: bỏ qua giờ đã qua
        assertEquals(6, WeatherSource.firstUpcomingHour(w, t0 + 9 * 3_600_000L));     // dữ liệu quá cũ: không còn giờ nào
        JSONObject old = new JSONObject();
        old.put("hours", new JSONArray().put(new JSONObject().put("h", "10")));
        assertEquals(1, WeatherSource.firstUpcomingHour(old, t0));                 // bản cũ không có mốc: như trước
    }

    @Test
    public void rainSoonLooksAtUpcomingHoursOnly() throws Exception {
        long t0 = 1_700_000_000_000L;
        JSONObject w = weather(t0, 1, 1, 1, 1, 61, 1);
        assertNull(WeatherSource.rainSoon(w, t0 + 1_000));                  // mưa ở giờ thứ 5, ngoài 3 giờ tới
        String s = WeatherSource.rainSoon(w, t0 + 3_600_000L + 1);           // giờ sau: mưa nằm trong 3 giờ tới
        assertTrue(s, s != null && s.startsWith("Sắp mưa khoảng 14h"));
        assertNull(WeatherSource.rainSoon(w, t0 + 5 * 3_600_000L));         // đã qua giờ mưa
    }
}
