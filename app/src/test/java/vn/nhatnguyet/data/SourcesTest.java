package vn.nhatnguyet.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import vn.nhatnguyet.Fmt;

public class SourcesTest {

    private static byte[] res(String name) throws Exception {
        InputStream in = SourcesTest.class.getClassLoader().getResourceAsStream(name);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int r;
        while ((r = in.read(b)) >= 0) bos.write(b, 0, r);
        in.close();
        return bos.toByteArray();
    }

    private static String text(String name) throws Exception {
        return new String(res(name), Charset.forName("UTF-8"));
    }

    @Test
    public void gold() throws Exception {
        JSONObject o = GoldSource.parse(text("vang.json"));
        JSONArray rows = o.getJSONArray("rows");
        assertTrue(rows.length() >= 6);
        assertEquals("SJL1L10", rows.getJSONObject(0).getString("code"));
        assertEquals("SJC miếng", rows.getJSONObject(0).getString("label"));
        JSONObject last = rows.getJSONObject(rows.length() - 1);
        assertEquals("XAUUSD", last.getString("code"));
        assertTrue(last.getBoolean("usd"));
        assertEquals("13:00 02/11", o.getString("updated"));
    }

    @Test
    public void fuel() throws Exception {
        JSONObject o = FuelSource.parse(text("fuel.html"));
        JSONArray rows = o.getJSONArray("rows");
        assertTrue(rows.length() >= 5);
        JSONObject first = rows.getJSONObject(0);
        assertEquals("Xăng E5 RON 92", first.getString("name"));
        assertEquals(24220, first.getDouble("zone1"), 0.1);
        assertEquals(24700, first.getDouble("zone2"), 0.1);
        assertEquals("02/11/2026", o.getString("updated"));
        boolean foundKerosene = false;
        for (int i = 0; i < rows.length(); i++) {
            if (rows.getJSONObject(i).getString("name").startsWith("Dầu hỏa")) {
                foundKerosene = true;
                assertEquals(-1060, rows.getJSONObject(i).getDouble("change"), 0.1);
            }
        }
        assertTrue(foundKerosene);
    }

    @Test
    public void weather() throws Exception {
        JSONObject o = WeatherSource.normalize(new JSONObject(text("wx.json")), "TP. Hồ Chí Minh");
        assertEquals("TP. Hồ Chí Minh", o.getString("place"));
        JSONObject now = o.getJSONObject("now");
        assertTrue(now.has("t"));
        assertTrue(now.has("code"));
        JSONArray hours = o.getJSONArray("hours");
        assertTrue(hours.length() >= 12);
        assertEquals(2, hours.getJSONObject(0).getString("h").length());
        JSONArray days = o.getJSONArray("days");
        assertEquals(6, days.length());
        assertEquals(5, days.getJSONObject(0).getString("sunrise").length());
        assertTrue(days.getJSONObject(0).getDouble("max") >= days.getJSONObject(0).getDouble("min"));
        assertEquals("Dông", WeatherCodes.text(95));
        assertEquals(WeatherCodes.RAIN, WeatherCodes.kind(81));
    }

    @Test
    public void fx() throws Exception {
        JSONObject o = FxSource.parse(res("vcb.xml"));
        JSONArray rows = o.getJSONArray("rows");
        assertTrue(rows.length() >= 5);
        assertEquals("USD", rows.getJSONObject(0).getString("code"));
        assertTrue(rows.getJSONObject(0).getDouble("sell") > 20000);
        assertEquals("13:15 02/11", o.getString("updated"));
    }

    @Test
    public void rss() throws Exception {
        List<NewsSource.Item> tt = NewsSource.parse(res("tuoitre.xml"), "https://tuoitre.vn/rss/tin-moi-nhat.rss");
        assertEquals(3, tt.size());
        assertEquals("Tuổi Trẻ", tt.get(0).source);
        Calendar c = Calendar.getInstance(Fmt.vnTimeZone());
        c.setTimeInMillis(tt.get(0).time);
        assertEquals(13, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, c.get(Calendar.MINUTE));
        assertTrue(tt.get(0).title.length() > 10);

        List<NewsSource.Item> vn = NewsSource.parse(res("vnexpress.xml"), "https://vnexpress.net/rss/kinh-doanh.rss");
        assertEquals(3, vn.size());
        assertEquals("VnExpress", vn.get(0).source);
        assertTrue(vn.get(0).link.startsWith("https://"));
    }

    @Test
    public void rssWithTrailingComment() throws Exception {
        // GenK thêm chú thích sau </rss>; bộ đọc XML của Android 4.x lỗi ở đó.
        String xml = text("vnexpress.xml") + "\n<!--u: 02/11/2026 14:08:44 -->\n";
        List<NewsSource.Item> items = NewsSource.parse(xml.getBytes("UTF-8"), "https://genk.vn/rss/home.rss");
        assertEquals(3, items.size());
        assertEquals("GenK", items.get(0).source);
    }

    @Test
    public void dates() {
        long a = NewsSource.parseDate("Mon, 02 Nov 26 13:25:56 +0700");
        long b = NewsSource.parseDate("Mon, 02 Nov 2026 13:25:56 +07");
        long c = NewsSource.parseDate("2026-11-02T06:25:56Z");
        long d = NewsSource.parseDate("2026-11-02T13:25:56+07:00");
        assertTrue(a > 0);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test
    public void numbers() {
        assertEquals("24.220", Fmt.group(24220));
        assertEquals("1.705,69", Fmt.dec(1705.69, 2));
        assertEquals("+1,0", Fmt.signed(1.0, 1));
        assertEquals("-0,45", Fmt.signed(-0.45, 2));
        assertEquals("0,00", Fmt.signed(0.001, 2));
        assertEquals("140,0", Fmt.million(140_000_000));
        assertEquals(17667.95, Fmt.parseNumber("17,667.95"), 0.001);
        assertEquals(-1060, Fmt.parseNumber("-1,060"), 0.001);
        assertEquals(990, Fmt.parseNumber("+990"), 0.001);
        assertEquals("Asia/Ho_Chi_Minh", Fmt.vnTimeZone().getID());
        assertTrue(TimeZone.getTimeZone("Asia/Ho_Chi_Minh").getRawOffset() == 7 * 3600_000);
    }

    @Test
    public void foldText() {
        assertEquals("gia vung 1", Html.fold("Giá vùng 1"));
        assertEquals("dau hoa 2-k", Html.fold("Dầu hỏa 2-K"));
    }
}
