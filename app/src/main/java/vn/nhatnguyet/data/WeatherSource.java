package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Thời tiết từ Open-Meteo (miễn phí, không cần khoá API). */
public final class WeatherSource extends Source {

    public WeatherSource() {
        super("weather", "thời tiết");
    }

    @Override
    public boolean enabled(Prefs prefs) {
        return true; // luôn cần cho cột trái
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
        JSONObject r = new JSONObject(Http.getText(url));
        return normalize(r, prefs.placeName());
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
}
