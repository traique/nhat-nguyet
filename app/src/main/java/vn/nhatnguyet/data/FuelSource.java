package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Giá xăng dầu Petrolimex, đọc từ bảng trên giaxanghomnay.com. */
public final class FuelSource extends Source {
    private static final Pattern UPDATED = Pattern.compile("Petrolimex\\s+cập nhật\\s*(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern UPDATED2 = Pattern.compile(
            "(?:cập nhật|điều chỉnh)[^0-9]{0,30}(?:\\d{1,2}:\\d{2}\\s*(?:ngày\\s*)?)?(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    public FuelSource() {
        super("fuel", "xăng");
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return 60 * 60_000L;
    }

    @Override
    public long staleMs(Prefs prefs) {
        return 2 * 86_400_000L;
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        return parse(Http.getText("https://giaxanghomnay.com/"));
    }

    static JSONObject parse(String html) throws Exception {
        JSONArray rows = new JSONArray();
        for (String table : Html.tables(html)) {
            if (!Html.fold(table).contains("vung 1")) continue;
            List<List<String>> trs = Html.rows(table);
            int name = 0, change = -1, z1 = -1, z2 = -1;
            boolean header = false;
            for (List<String> cells : trs) {
                if (!header) {
                    for (int i = 0; i < cells.size(); i++) {
                        String f = Html.fold(cells.get(i));
                        if (f.contains("vung 1")) z1 = i;
                        else if (f.contains("vung 2")) z2 = i;
                        else if (f.contains("tang giam") && !f.contains("truoc") && change < 0) change = i;
                        else if (f.contains("xang") || f.contains("mat hang")) name = i;
                    }
                    if (z1 >= 0) header = true;
                    continue;
                }
                if (cells.size() <= z1) continue;
                String n = cells.get(name);
                double p1 = Fmt.parseNumber(cells.get(z1));
                if (n.length() == 0 || Double.isNaN(p1) || p1 < 1000) continue;
                JSONObject o = new JSONObject();
                o.put("name", n);
                J.num(o, "zone1", p1);
                J.num(o, "zone2", z2 >= 0 && z2 < cells.size() ? Fmt.parseNumber(cells.get(z2)) : Double.NaN);
                double ch = change >= 0 && change < cells.size() ? Fmt.parseNumber(cells.get(change)) : 0;
                o.put("change", Double.isNaN(ch) ? 0 : ch);
                rows.put(o);
            }
            if (rows.length() > 0) break;
        }
        if (rows.length() == 0) throw new IOException("không thấy bảng giá xăng Petrolimex");
        JSONObject out = new JSONObject();
        out.put("rows", rows);
        String text = Html.text(html);
        Matcher m = UPDATED.matcher(text);
        if (m.find()) {
            out.put("updated", two(m.group(3)) + "/" + two(m.group(2)) + "/" + m.group(1));
        } else {
            Matcher m2 = UPDATED2.matcher(text);
            if (m2.find()) out.put("updated", two(m2.group(1)) + "/" + two(m2.group(2)) + "/" + m2.group(3));
        }
        return out;
    }

    private static String two(String s) {
        return s.length() == 1 ? "0" + s : s;
    }
}
