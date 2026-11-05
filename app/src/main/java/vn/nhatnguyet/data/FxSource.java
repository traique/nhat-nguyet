package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Tỷ giá Vietcombank (XML công khai, tối đa 1 lần/5 phút). */
public final class FxSource extends Source {
    static final String[] ORDER = {"USD", "EUR", "JPY", "GBP", "CNY", "AUD", "SGD", "KRW", "CAD", "CHF", "THB", "HKD"};

    public FxSource() {
        super("fx", "tỷ giá");
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return 30 * 60_000L;
    }

    @Override
    public long staleMs(Prefs prefs) {
        return 12 * 3600_000L;
    }

    /** Vietcombank ghi ngay trong XML: "Only one request every 5 minutes". */
    @Override
    public long minGapMs(Prefs prefs) {
        return 5 * 60_000L;
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        return parse(Http.get("https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx"));
    }

    static JSONObject parse(byte[] xml) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        XmlPullParser p = f.newPullParser();
        p.setInput(new ByteArrayInputStream(xml), null);
        Map<String, JSONObject> map = new HashMap<String, JSONObject>();
        String updated = "";
        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                String name = p.getName();
                if ("DateTime".equals(name)) {
                    updated = p.nextText();
                } else if ("Exrate".equals(name)) {
                    String code = p.getAttributeValue(null, "CurrencyCode");
                    if (code != null) {
                        JSONObject o = new JSONObject();
                        o.put("code", code.trim());
                        J.num(o, "buy", Fmt.parseNumber(p.getAttributeValue(null, "Buy")));
                        J.num(o, "transfer", Fmt.parseNumber(p.getAttributeValue(null, "Transfer")));
                        J.num(o, "sell", Fmt.parseNumber(p.getAttributeValue(null, "Sell")));
                        map.put(code.trim(), o);
                    }
                }
            }
            ev = p.next();
        }
        JSONArray rows = new JSONArray();
        for (String c : ORDER) {
            JSONObject o = map.get(c);
            if (o != null && !Double.isNaN(o.optDouble("sell"))) rows.put(o);
        }
        if (rows.length() == 0) throw new IOException("Vietcombank không trả tỷ giá");
        JSONObject out = new JSONObject();
        out.put("rows", rows);
        out.put("updated", shortTime(updated));
        return out;
    }

    /** "11/2/2026 1:15:42 PM" -> "13:15 02/11". */
    static String shortTime(String raw) {
        try {
            String[] parts = raw.trim().split("\\s+");
            String[] d = parts[0].split("/");
            String[] t = parts[1].split(":");
            int h = Integer.parseInt(t[0]);
            if (parts.length > 2) {
                boolean pm = parts[2].equalsIgnoreCase("PM");
                if (pm && h < 12) h += 12;
                if (!pm && h == 12) h = 0;
            }
            return Fmt.two(h) + ":" + t[1] + " " + Fmt.two(Integer.parseInt(d[1])) + "/" + Fmt.two(Integer.parseInt(d[0]));
        } catch (Exception e) {
            return raw;
        }
    }
}
