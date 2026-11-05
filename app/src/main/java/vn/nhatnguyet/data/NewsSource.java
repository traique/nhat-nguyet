package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/** Tin tức từ các nguồn RSS/Atom người dùng chọn. */
public final class NewsSource extends Source {
    private static final int MAX_ITEMS = 60;
    private static final String[] PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
    };
    private static final String[] LOCAL_PATTERNS = {
            "M/d/yyyy h:mm:ss a",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "EEE, dd MMM yyyy HH:mm:ss",
    };

    static final class Item {
        String title;
        String link;
        String source;
        long time;
    }

    public NewsSource() {
        super("news", "tin");
    }

    @Override
    public long intervalMs(Prefs prefs) {
        return 10 * 60_000L;
    }

    @Override
    public String configKey(Prefs prefs) {
        return prefs.feedsRaw();
    }

    @Override
    public JSONObject fetch(Context context, Prefs prefs) throws Exception {
        List<String> feeds = prefs.feeds();
        List<Item> all = new ArrayList<Item>();
        int ok = 0;
        Exception last = null;
        // Mỗi nguồn lấy tối đa perFeed tin mới nhất để các báo được trộn đều.
        int perFeed = Math.max(10, MAX_ITEMS / Math.max(1, feeds.size()));
        for (String url : feeds) {
            try {
                List<Item> items = parse(Http.get(url), url);
                sortNewest(items);
                all.addAll(items.size() > perFeed ? items.subList(0, perFeed) : items);
                ok++;
            } catch (Exception e) {
                last = e;
                android.util.Log.w("NewsSource", url + " failed: " + e);
            }
        }
        if (ok == 0) {
            if (last != null) throw new IOException("không đọc được nguồn RSS nào (" + Http.describe(last) + ")");
            throw new IOException("chưa có nguồn RSS");
        }
        sortNewest(all);
        JSONArray items = new JSONArray();
        Set<String> seen = new HashSet<String>();
        for (Item it : all) {
            String k = Html.fold(it.title);
            if (k.length() == 0 || !seen.add(k)) continue;
            JSONObject o = new JSONObject();
            o.put("title", it.title);
            o.put("source", it.source);
            o.put("time", it.time);
            o.put("link", it.link == null ? "" : it.link);
            items.put(o);
            if (items.length() >= MAX_ITEMS) break;
        }
        JSONObject out = new JSONObject();
        out.put("items", items);
        out.put("feeds", ok);
        out.put("failed", feeds.size() - ok);
        return out;
    }

    private static void sortNewest(List<Item> items) {
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return a.time == b.time ? 0 : (a.time > b.time ? -1 : 1);
            }
        });
    }

    static List<Item> parse(byte[] xml, String feedUrl) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(false);
        XmlPullParser p = f.newPullParser();
        try {
            p.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        } catch (Exception ignored) {
        }
        p.setInput(new ByteArrayInputStream(xml), null);
        String source = sourceName(feedUrl);
        List<Item> out = new ArrayList<Item>();
        try {
            readItems(p, source, out);
        } catch (Exception e) {
            // Bộ đọc XML của Android 4.x lỗi với vài phần lạ ở cuối file (vd. chú thích sau </rss>):
            // vẫn dùng các tin đã đọc được.
            if (out.isEmpty()) throw e;
        }
        return out;
    }

    private static void readItems(XmlPullParser p, String source, List<Item> out) throws Exception {
        Item cur = null;
        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                if ("item".equalsIgnoreCase(n) || "entry".equalsIgnoreCase(n)) {
                    cur = new Item();
                    cur.source = source;
                } else if (cur != null) {
                    if ("title".equalsIgnoreCase(n)) {
                        cur.title = clean(safeText(p));
                    } else if ("link".equalsIgnoreCase(n)) {
                        String href = p.getAttributeValue(null, "href");
                        if (href != null) {
                            if (cur.link == null) cur.link = href;
                        } else {
                            cur.link = safeText(p).trim();
                        }
                    } else if (cur.time == 0 && ("pubDate".equalsIgnoreCase(n) || "dc:date".equalsIgnoreCase(n)
                            || "published".equalsIgnoreCase(n) || "updated".equalsIgnoreCase(n))) {
                        cur.time = parseDate(safeText(p));
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if (cur != null && ("item".equalsIgnoreCase(n) || "entry".equalsIgnoreCase(n))) {
                    if (cur.title != null && cur.title.length() > 0) {
                        // Không có ngày: giữ 0, hiện không kèm "… trước" và xếp sau tin có ngày.
                        out.add(cur);
                    }
                    cur = null;
                } else if ("channel".equalsIgnoreCase(n) || "rss".equalsIgnoreCase(n) || "feed".equalsIgnoreCase(n)) {
                    return; // hết nội dung chính, không đọc phần đuôi
                }
            }
            ev = p.next();
        }
    }

    private static String safeText(XmlPullParser p) {
        try {
            return p.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private static String clean(String s) {
        return Html.text(s);
    }

    static String sourceName(String url) {
        String host;
        try {
            host = new URL(url).getHost().toLowerCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
        if (host.startsWith("www.")) host = host.substring(4);
        if (host.contains("vnexpress")) return "VnExpress";
        if (host.contains("tuoitre")) return "Tuổi Trẻ";
        if (host.contains("thanhnien")) return "Thanh Niên";
        if (host.contains("dantri")) return "Dân trí";
        if (host.contains("cafef")) return "CafeF";
        if (host.contains("cafebiz")) return "CafeBiz";
        if (host.contains("genk")) return "GenK";
        if (host.contains("vietnamnet")) return "VietNamNet";
        if (host.contains("znews") || host.contains("zingnews")) return "Znews";
        if (host.contains("vietstock")) return "Vietstock";
        if (host.contains("nld.com")) return "Người Lao Động";
        if (host.contains("laodong")) return "Lao Động";
        if (host.contains("24h.com")) return "24h";
        if (host.contains("vtv")) return "VTV";
        int dot = host.indexOf('.');
        return dot > 0 ? host.substring(0, dot) : host;
    }

    static long parseDate(String raw) {
        if (raw == null) return 0;
        // Tuổi Trẻ dùng khoảng trắng hẹp (U+202F) trước AM/PM.
        String s = raw.replace(' ', ' ').replace(' ', ' ').trim();
        if (s.length() == 0) return 0;
        String z = s;
        if (z.endsWith("Z")) z = z.substring(0, z.length() - 1) + "+0000";
        if (z.endsWith(" GMT") || z.endsWith(" UT") || z.endsWith(" UTC")) z = z.substring(0, z.lastIndexOf(' ')) + " +0000";
        // "+07:00" -> "+0700", "+07" -> "+0700"
        z = z.replaceAll("([+-]\\d{2}):(\\d{2})$", "$1$2");
        z = z.replaceAll("([+-]\\d{2})$", "$100");
        for (String pat : PATTERNS) {
            long t = tryParse(z, pat, null);
            if (t > 0) return t;
        }
        for (String pat : LOCAL_PATTERNS) {
            long t = tryParse(s, pat, Fmt.vnTimeZone());
            if (t > 0) return t;
        }
        return 0;
    }

    private static long tryParse(String s, String pattern, TimeZone tz) {
        try {
            SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
            f.setLenient(true);
            if (tz != null) f.setTimeZone(tz);
            ParsePosition pos = new ParsePosition(0);
            Date d = f.parse(s, pos);
            if (d == null || pos.getIndex() < s.length() - 1) return 0;
            return d.getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
