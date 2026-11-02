package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/**
 * Chứng khoán: nến ngày + nến phút từ Entrade (DNSE), giá khớp lệnh từ DNSE.
 * Giá cổ phiếu tính theo nghìn đồng, chỉ số tính theo điểm.
 */
public final class StockSource extends Source {
    private static final String CHART = "https://services.entrade.com.vn/chart-api/v2/ohlcs/";
    private static final Map<String, String> INDEX = new HashMap<String, String>();

    static {
        INDEX.put("VNINDEX", "VNINDEX");
        INDEX.put("VN30", "VN30");
        INDEX.put("VN100", "VN100");
        INDEX.put("HNX", "HNX");
        INDEX.put("HNXINDEX", "HNX");
        INDEX.put("HNX30", "HNX30");
        INDEX.put("UPCOM", "UPCOM");
        INDEX.put("UPCOMINDEX", "UPCOM");
        INDEX.put("UPINDEX", "UPCOM");
    }

    private static final class Daily {
        String day;
        long fetchedAt;
        double[] close;
        String[] date;
    }

    private final Map<String, Daily> daily = new HashMap<String, Daily>();

    public StockSource() {
        super("stocks", "chứng khoán");
    }

    public static boolean marketOpen() {
        Calendar c = Fmt.vnNow();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false;
        int m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return m >= 9 * 60 && m < 15 * 60;
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return marketOpen() ? 60_000L : 30 * 60_000L;
    }

    @Override
    public long staleMs(Prefs prefs) {
        return marketOpen() ? 10 * 60_000L : 3 * 86_400_000L;
    }

    @Override
    public String configKey(Prefs prefs) {
        return prefs.watchlistRaw();
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        List<String> syms = prefs.watchlist();
        JSONArray rows = new JSONArray();
        int ok = 0;
        Exception last = null;
        for (String s : syms) {
            try {
                rows.put(quote(s));
                ok++;
            } catch (Exception e) {
                last = e;
                JSONObject o = new JSONObject();
                o.put("symbol", s);
                o.put("error", Http.describe(e));
                rows.put(o);
            }
        }
        if (ok == 0 && last != null && !syms.isEmpty()) throw last;
        JSONObject out = new JSONObject();
        out.put("rows", rows);
        out.put("open", marketOpen());
        return out;
    }

    private JSONObject quote(String sym) throws Exception {
        String api = INDEX.get(sym);
        boolean isIndex = api != null;
        if (!isIndex) api = sym;
        String kind = isIndex ? "index" : "stock";
        Calendar now = Fmt.vnNow();
        String today = key(now);

        Daily d = daily(api, kind, today);
        // Giá đóng cửa phiên trước hôm nay, và phiên gần nhất.
        int n = d.close.length;
        if (n == 0) throw new IOException("không có dữ liệu " + sym);
        int lastIdx = n - 1;
        int prevIdx = -1;
        for (int i = n - 1; i >= 0; i--) {
            if (d.date[i].compareTo(today) < 0) {
                prevIdx = i;
                break;
            }
        }

        Double current = null;
        long at = 0;
        // Nến phút hôm nay (có cả chỉ số lẫn cổ phiếu, cập nhật trong phiên).
        try {
            Calendar start = (Calendar) now.clone();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            long from = start.getTimeInMillis() / 1000;
            long to = now.getTimeInMillis() / 1000 + 60;
            JSONObject m = new JSONObject(Http.getText(CHART + kind + "?from=" + from + "&to=" + to
                    + "&symbol=" + api + "&resolution=1"));
            JSONArray c = m.optJSONArray("c");
            JSONArray t = m.optJSONArray("t");
            if (c != null && c.length() > 0) {
                current = c.getDouble(c.length() - 1);
                if (t != null && t.length() > 0) at = t.getLong(t.length() - 1) * 1000;
            }
        } catch (Exception ignored) {
        }
        if (current == null && !isIndex) {
            Double dnse = dnse(api, today);
            if (dnse != null) current = dnse;
        }

        double price, prev;
        if (current != null && prevIdx >= 0) {
            price = current;
            prev = d.close[prevIdx];
        } else {
            // Ngoài phiên/ngày nghỉ: hiện phiên gần nhất so với phiên trước đó.
            price = d.close[lastIdx];
            prev = lastIdx > 0 ? d.close[lastIdx - 1] : price;
        }
        JSONObject o = new JSONObject();
        o.put("symbol", sym);
        J.num(o, "price", price);
        J.num(o, "change", price - prev);
        J.num(o, "pct", prev != 0 ? (price - prev) / prev * 100 : 0);
        o.put("index", isIndex);
        if (at > 0) o.put("at", at);
        return o;
    }

    private Daily daily(String api, String kind, String today) throws Exception {
        synchronized (daily) {
            Daily d = daily.get(api);
            long now = System.currentTimeMillis();
            if (d != null && today.equals(d.day) && now - d.fetchedAt < 30 * 60_000L) return d;
        }
        long to = System.currentTimeMillis() / 1000 + 86400;
        long from = to - 30L * 86400;
        JSONObject r = new JSONObject(Http.getText(CHART + kind + "?from=" + from + "&to=" + to
                + "&symbol=" + api + "&resolution=1D"));
        if (r.has("status") && !r.has("c")) throw new IOException("mã không hợp lệ: " + api);
        JSONArray c = r.optJSONArray("c");
        JSONArray t = r.optJSONArray("t");
        int n = c == null || t == null ? 0 : Math.min(c.length(), t.length());
        Daily d = new Daily();
        d.day = today;
        d.fetchedAt = System.currentTimeMillis();
        d.close = new double[n];
        d.date = new String[n];
        Calendar cal = Calendar.getInstance(Fmt.vnTimeZone());
        for (int i = 0; i < n; i++) {
            d.close[i] = c.getDouble(i);
            cal.setTimeInMillis(t.getLong(i) * 1000);
            d.date[i] = key(cal);
        }
        synchronized (daily) {
            daily.put(api, d);
        }
        return d;
    }

    private static Double dnse(String sym, String today) {
        try {
            String date = today.substring(0, 4) + "-" + today.substring(4, 6) + "-" + today.substring(6, 8);
            String q = "query GetKrxTicksBySymbols {GetKrxTicksBySymbols(symbols: \"" + sym + "\", date: \""
                    + date + "\", limit: 1, board: 2) {ticks {matchPrice}}}";
            JSONObject body = new JSONObject();
            body.put("operationName", "GetKrxTicksBySymbols");
            body.put("query", q);
            JSONObject r = new JSONObject(Http.postJson("https://api.dnse.com.vn/price-api/query", body.toString()));
            JSONObject data = r.optJSONObject("data");
            JSONObject g = data == null ? null : data.optJSONObject("GetKrxTicksBySymbols");
            JSONArray ticks = g == null ? null : g.optJSONArray("ticks");
            if (ticks == null || ticks.length() == 0) return null;
            double p = ticks.getJSONObject(0).optDouble("matchPrice");
            return Double.isNaN(p) || p <= 0 ? null : p;
        } catch (Exception e) {
            return null;
        }
    }

    private static String key(Calendar c) {
        return String.format(Locale.US, "%04d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }
}
