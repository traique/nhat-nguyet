package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Thời tiết từ Open-Meteo (miễn phí, không cần khoá API). */
public final class WeatherSource extends Source {

    public WeatherSource() {
        super("weather", "thời tiết");
    }

    @Override
    public boolean enabled(Prefs prefs) {
        // Luôn cần cho cột đồng hồ; đồng hồ lật tắt dòng thông tin thì không hiện thời tiết nên không tải cho đỡ pin.
        return !prefs.clockOnly() || prefs.flipInfo();
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return 15 * 60_000L;
    }

    @Override
    public String configKey(Prefs prefs) {
        return String.format(Locale.US, "%.3f,%.3f", prefs.placeLat(), prefs.placeLon());
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + String.format(Locale.US, "?latitude=%.4f&longitude=%.4f", prefs.placeLat(), prefs.placeLon())
                + "&timezone=Asia%2FHo_Chi_Minh&forecast_days=6&forecast_hours=25"
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day"
                + "&hourly=temperature_2m,weather_code,precipitation_probability,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset";
        JSONObject out = normalize(new JSONObject(Http.getText(url)), prefs.placeName());
        // Chất lượng không khí chỉ là phần thêm: lỗi thì vẫn hiện thời tiết.
        try {
            String aq = "https://air-quality-api.open-meteo.com/v1/air-quality"
                    + String.format(Locale.US, "?latitude=%.4f&longitude=%.4f", prefs.placeLat(), prefs.placeLon())
                    + "&current=us_aqi,pm2_5&timezone=Asia%2FHo_Chi_Minh";
            JSONObject air = air(new JSONObject(Http.getText(aq)));
            if (air != null) out.put("air", air);
        } catch (Exception ignored) {
        }
        return out;
    }

    /** {aqi, pm25} từ Open-Meteo Air Quality; null nếu thiếu số liệu. */
    static JSONObject air(JSONObject r) throws Exception {
        JSONObject c = r.optJSONObject("current");
        if (c == null || c.isNull("us_aqi")) return null;
        JSONObject o = new JSONObject();
        o.put("aqi", c.optInt("us_aqi"));
        J.num(o, "pm25", c.optDouble("pm2_5"));
        return o;
    }

    /** Mốc (ms) của chuỗi giờ "yyyy-MM-ddTHH:mm" theo giờ Việt Nam; 0 nếu không đọc được. */
    static long epochOf(String ts) {
        if (ts == null || ts.length() < 16) return 0;
        try {
            Calendar c = Calendar.getInstance(Fmt.vnTimeZone());
            c.clear();
            c.set(Integer.parseInt(ts.substring(0, 4)), Integer.parseInt(ts.substring(5, 7)) - 1,
                    Integer.parseInt(ts.substring(8, 10)), Integer.parseInt(ts.substring(11, 13)),
                    Integer.parseInt(ts.substring(14, 16)));
            return c.getTimeInMillis();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * Chỉ số giờ dự báo đầu tiên còn ở phía trước, để dữ liệu cũ (mất mạng) vẫn hiện đúng giờ sắp tới.
     * Trả về 1 với dữ liệu không có mốc; bằng số phần tử nếu mọi giờ đã qua.
     */
    public static int firstUpcomingHour(JSONObject w, long nowMs) {
        JSONArray hours = w == null ? null : w.optJSONArray("hours");
        if (hours == null) return 1;
        boolean stamped = false;
        for (int i = 0; i < hours.length(); i++) {
            JSONObject h = hours.optJSONObject(i);
            long ts = h == null ? 0 : h.optLong("ts");
            if (ts > 0) stamped = true;
            if (ts > nowMs) return i;
        }
        return stamped ? hours.length() : 1;
    }

    /** "Sắp mưa khoảng 15h (80%)" nếu 3 giờ tới có mưa mà lúc này chưa mưa; null nếu không. */
    public static String rainSoon(JSONObject w) {
        return rainSoon(w, System.currentTimeMillis());
    }

    static String rainSoon(JSONObject w, long nowMs) {
        JSONObject now = w.optJSONObject("now");
        JSONArray hours = w.optJSONArray("hours");
        if (now == null || hours == null || wet(now.optInt("code"))) return null;
        int start = firstUpcomingHour(w, nowMs);
        for (int i = start; i < start + 3 && i < hours.length(); i++) {
            JSONObject h = hours.optJSONObject(i);
            int code = h.optInt("code");
            int rain = h.optInt("rain");
            if ((wet(code) && rain >= 40) || rain >= 70) {
                String what = WeatherCodes.kind(code) == WeatherCodes.STORM ? "Sắp có dông" : "Sắp mưa";
                return what + " khoảng " + h.optString("h").replaceFirst("^0(?=\\d)", "") + "h (" + rain + "%)";
            }
        }
        return null;
    }

    private static boolean wet(int code) {
        int k = WeatherCodes.kind(code);
        return k == WeatherCodes.DRIZZLE || k == WeatherCodes.RAIN || k == WeatherCodes.STORM;
    }

    static JSONObject normalize(JSONObject r, String place) throws Exception {
        JSONObject out = new JSONObject();
        out.put("place", place);
        JSONObject cur = r.optJSONObject("current");
        if (cur == null) throw new IOException("thiếu dữ liệu hiện tại");
        JSONObject now = new JSONObject();
        J.num(now, "t", cur.optDouble("temperature_2m"));
        J.num(now, "feels", cur.optDouble("apparent_temperature"));
        J.num(now, "hum", cur.optDouble("relative_humidity_2m"));
        J.num(now, "wind", cur.optDouble("wind_speed_10m"));
        now.put("code", cur.optInt("weather_code"));
        now.put("day", cur.optInt("is_day", 1));
        out.put("now", now);

        JSONArray hours = new JSONArray();
        JSONObject h = r.optJSONObject("hourly");
        if (h != null) {
            JSONArray time = h.optJSONArray("time");
            JSONArray t = h.optJSONArray("temperature_2m");
            JSONArray code = h.optJSONArray("weather_code");
            JSONArray rain = h.optJSONArray("precipitation_probability");
            JSONArray day = h.optJSONArray("is_day");
            int n = time == null ? 0 : time.length();
            for (int i = 0; i < n; i++) {
                String ts = time.optString(i);
                JSONObject o = new JSONObject();
                o.put("h", ts.length() >= 13 ? ts.substring(11, 13) : ts);
                long epoch = epochOf(ts);
                if (epoch > 0) o.put("ts", epoch);
                J.num(o, "t", t == null ? Double.NaN : t.optDouble(i));
                o.put("code", code == null ? 0 : code.optInt(i));
                o.put("rain", rain == null ? 0 : rain.optInt(i));
                o.put("day", day == null ? 1 : day.optInt(i, 1));
                hours.put(o);
            }
        }
        out.put("hours", hours);

        JSONArray days = new JSONArray();
        JSONObject d = r.optJSONObject("daily");
        if (d != null) {
            JSONArray time = d.optJSONArray("time");
            JSONArray code = d.optJSONArray("weather_code");
            JSONArray max = d.optJSONArray("temperature_2m_max");
            JSONArray min = d.optJSONArray("temperature_2m_min");
            JSONArray rain = d.optJSONArray("precipitation_probability_max");
            JSONArray rise = d.optJSONArray("sunrise");
            JSONArray set = d.optJSONArray("sunset");
            int n = time == null ? 0 : time.length();
            for (int i = 0; i < n; i++) {
                JSONObject o = new JSONObject();
                o.put("date", time.optString(i));
                o.put("code", code == null ? 0 : code.optInt(i));
                J.num(o, "max", max == null ? Double.NaN : max.optDouble(i));
                J.num(o, "min", min == null ? Double.NaN : min.optDouble(i));
                o.put("rain", rain == null ? 0 : rain.optInt(i));
                o.put("sunrise", hm(rise == null ? "" : rise.optString(i)));
                o.put("sunset", hm(set == null ? "" : set.optString(i)));
                days.put(o);
            }
        }
        out.put("days", days);
        return out;
    }

    private static String hm(String iso) {
        return iso != null && iso.length() >= 16 ? iso.substring(11, 16) : "";
    }

    /** Tìm toạ độ theo tên thành phố. @return {tên, lat, lon} */
    public static Object[] geocode(String name) throws Exception {
        String q = URLEncoder.encode(name.trim(), "UTF-8");
        JSONObject best = null;
        for (String extra : new String[]{"&countryCode=VN", ""}) {
            String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + q
                    + "&count=1&language=vi&format=json" + extra;
            JSONObject r = new JSONObject(Http.getText(url));
            JSONArray res = r.optJSONArray("results");
            if (res != null && res.length() > 0) {
                best = res.getJSONObject(0);
                break;
            }
        }
        if (best == null) throw new IOException("Không tìm thấy địa điểm \"" + name.trim() + "\"");
        String label = best.optString("name", name.trim());
        return new Object[]{label, best.getDouble("latitude"), best.getDouble("longitude")};
    }

    /** Tên thành phố/tỉnh tại toạ độ (OpenStreetMap Nominatim), vd "TP. Hồ Chí Minh". */
    public static String placeName(double lat, double lon) throws Exception {
        String url = String.format(Locale.US, "https://nominatim.openstreetmap.org/reverse?format=jsonv2"
                + "&lat=%.5f&lon=%.5f&zoom=10&accept-language=vi", lat, lon);
        return placeName(new JSONObject(Http.getText(url)));
    }

    static String placeName(JSONObject reverse) throws IOException {
        JSONObject a = reverse.optJSONObject("address");
        if (a == null) throw new IOException("Không xác định được tên nơi");
        for (String k : new String[]{"city", "town", "village", "state", "county"}) {
            String v = a.optString(k).trim();
            if (v.length() > 0) return v.replaceFirst("^Thành phố ", "TP. ").replaceFirst("^Tỉnh ", "");
        }
        throw new IOException("Không xác định được tên nơi");
    }
}
