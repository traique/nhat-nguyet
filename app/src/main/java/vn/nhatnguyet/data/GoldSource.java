package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Giá vàng từ vang.today (tổng hợp SJC, DOJI, PNJ, Bảo Tín, thế giới). */
public final class GoldSource extends Source {
    static final String[][] ROWS = {
            {"SJL1L10", "SJC miếng", "SJC"},
            {"SJ9999", "Nhẫn SJC 9999", "Nhẫn SJC"},
            {"DOHNL", "DOJI Hà Nội", "DOJI HN"},
            {"DOHCML", "DOJI TP.HCM", "DOJI HCM"},
            {"PQHNVM", "PNJ Hà Nội", "PNJ HN"},
            {"BTSJC", "Bảo Tín SJC", "BT SJC"},
            {"BT9999NTT", "Bảo Tín 9999", "BT 9999"},
            {"XAUUSD", "Thế giới (USD/oz)", "Thế giới"},
    };

    public GoldSource() {
        super("gold", "vàng");
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return 10 * 60_000L;
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        return parse(Http.getText("https://www.vang.today/api/prices"));
    }

    static JSONObject parse(String text) throws Exception {
        JSONObject root = new JSONObject(text);
        if (!root.optBoolean("success", true)) throw new IOException("vang.today báo lỗi");
        JSONObject prices = root.optJSONObject("prices");
        if (prices == null) throw new IOException("vang.today không có bảng giá");
        JSONArray rows = new JSONArray();
        for (String[] r : ROWS) {
            JSONObject p = prices.optJSONObject(r[0]);
            if (p == null) continue;
            double buy = p.optDouble("buy", Double.NaN);
            if (Double.isNaN(buy) || buy <= 0) continue;
            JSONObject o = new JSONObject();
            o.put("code", r[0]);
            o.put("label", r[1]);
            o.put("short", r[2]);
            o.put("buy", buy);
            J.num(o, "sell", p.optDouble("sell", 0));
            J.num(o, "changeBuy", p.optDouble("change_buy", 0));
            J.num(o, "changeSell", p.optDouble("change_sell", 0));
            o.put("usd", "USD".equals(p.optString("currency")) || "XAUUSD".equals(r[0]));
            rows.put(o);
        }
        if (rows.length() == 0) throw new IOException("vang.today không có dòng giá");
        JSONObject out = new JSONObject();
        out.put("rows", rows);
        String date = root.optString("date", "");
        String time = root.optString("time", "");
        String updated = time;
        if (date.length() == 10) updated = (time + " " + date.substring(8, 10) + "/" + date.substring(5, 7)).trim();
        out.put("updated", updated);
        return out;
    }
}
