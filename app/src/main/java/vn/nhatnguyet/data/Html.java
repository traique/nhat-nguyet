package vn.nhatnguyet.data;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bóc tách HTML đơn giản bằng regex (đủ cho bảng giá xăng, không cần jsoup). */
public final class Html {
    private static final Pattern TABLE = Pattern.compile("<table\\b.*?</table>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ROW = Pattern.compile("<tr\\b.*?</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CELL = Pattern.compile("<t[dh]\\b[^>]*>(.*?)</t[dh]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT = Pattern.compile("<(script|style)\\b.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private Html() {}

    public static List<String> tables(String html) {
        List<String> out = new ArrayList<String>();
        Matcher m = TABLE.matcher(html);
        while (m.find()) out.add(m.group());
        return out;
    }

    public static List<List<String>> rows(String table) {
        List<List<String>> out = new ArrayList<List<String>>();
        Matcher r = ROW.matcher(table);
        while (r.find()) {
            List<String> cells = new ArrayList<String>();
            Matcher c = CELL.matcher(r.group());
            while (c.find()) cells.add(text(c.group(1)));
            if (!cells.isEmpty()) out.add(cells);
        }
        return out;
    }

    /** Bỏ thẻ, giải mã thực thể, gộp khoảng trắng. */
    public static String text(String html) {
        String s = SCRIPT.matcher(html).replaceAll(" ");
        s = TAG.matcher(s).replaceAll(" ");
        s = unescape(s);
        return s.replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    public static String unescape(String s) {
        if (s.indexOf('&') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (ch == '&') {
                int semi = s.indexOf(';', i);
                if (semi > i && semi - i <= 10) {
                    String ent = s.substring(i + 1, semi);
                    String rep = entity(ent);
                    if (rep != null) {
                        sb.append(rep);
                        i = semi + 1;
                        continue;
                    }
                }
            }
            sb.append(ch);
            i++;
        }
        return sb.toString();
    }

    private static String entity(String e) {
        if (e.startsWith("#x") || e.startsWith("#X")) {
            try {
                return new String(Character.toChars(Integer.parseInt(e.substring(2), 16)));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (e.startsWith("#")) {
            try {
                return new String(Character.toChars(Integer.parseInt(e.substring(1))));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (e.equals("amp")) return "&";
        if (e.equals("lt")) return "<";
        if (e.equals("gt")) return ">";
        if (e.equals("quot")) return "\"";
        if (e.equals("apos")) return "'";
        if (e.equals("nbsp")) return " ";
        if (e.equals("ndash")) return "–";
        if (e.equals("mdash")) return "—";
        if (e.equals("hellip")) return "…";
        if (e.equals("rsquo") || e.equals("lsquo")) return "'";
        if (e.equals("rdquo") || e.equals("ldquo")) return "\"";
        return null;
    }

    /** "Giá vùng 1" -> "gia vung 1" để so khớp không dấu. */
    public static String fold(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = MARKS.matcher(n).replaceAll("");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.US).trim();
    }
}
