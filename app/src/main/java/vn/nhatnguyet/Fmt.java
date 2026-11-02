package vn.nhatnguyet;

import java.util.Calendar;
import java.util.TimeZone;

/** Định dạng số/giờ kiểu Việt Nam (dấu chấm phân nghìn, dấu phẩy thập phân). */
public final class Fmt {
    public static final String[] WEEKDAYS = {"", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    public static final String[] WEEKDAYS_SHORT = {"", "CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    private static TimeZone vnTz;

    private Fmt() {}

    public static synchronized TimeZone vnTimeZone() {
        if (vnTz == null) {
            TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            if (!"Asia/Ho_Chi_Minh".equals(tz.getID())) tz = TimeZone.getTimeZone("GMT+07:00");
            vnTz = tz;
        }
        return vnTz;
    }

    public static Calendar vnNow() {
        return Calendar.getInstance(vnTimeZone());
    }

    /** 24220 -> "24.220". */
    public static String group(long v) {
        boolean neg = v < 0;
        String s = Long.toString(Math.abs(v));
        StringBuilder sb = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            sb.append(s.charAt(i));
            int rest = n - 1 - i;
            if (rest > 0 && rest % 3 == 0) sb.append('.');
        }
        return neg ? "-" + sb : sb.toString();
    }

    /** Số thập phân kiểu VN: dec(1705.69, 2) -> "1.705,69". */
    public static String dec(double v, int digits) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "–";
        boolean neg = v < 0;
        double a = Math.abs(v);
        long scale = 1;
        for (int i = 0; i < digits; i++) scale *= 10;
        long total = Math.round(a * scale);
        long ip = total / scale;
        long fp = total % scale;
        StringBuilder sb = new StringBuilder(group(ip));
        if (digits > 0) {
            String f = Long.toString(fp);
            sb.append(',');
            for (int i = f.length(); i < digits; i++) sb.append('0');
            sb.append(f);
        }
        if (neg && total != 0) sb.insert(0, '-');
        return sb.toString();
    }

    public static String signed(double v, int digits) {
        String s = dec(v, digits);
        if (v > 0 && !s.equals(dec(0, digits))) return "+" + s;
        return s;
    }

    /** VND/lượng -> "140,0" (triệu). */
    public static String million(double vnd) {
        return dec(vnd / 1_000_000d, 1);
    }

    public static String temp(double c) {
        if (Double.isNaN(c)) return "--°";
        return Math.round(c) + "°";
    }

    /** "29°–34°" (bỏ qua nếu thiếu số liệu). */
    public static String range(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max)) return "";
        return Math.round(min) + "°–" + Math.round(max) + "°";
    }

    public static String two(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    public static String hhmm(Calendar c) {
        return two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE));
    }

    public static String hhmm(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return hhmm(c);
    }

    public static String ddmm(Calendar c) {
        return two(c.get(Calendar.DAY_OF_MONTH)) + "/" + two(c.get(Calendar.MONTH) + 1);
    }

    public static String ddmmyyyy(Calendar c) {
        return ddmm(c) + "/" + c.get(Calendar.YEAR);
    }

    /** "vừa xong", "5 phút", "3 giờ", "2 ngày". */
    public static String ago(long ms, long now) {
        long d = Math.max(0, now - ms) / 1000;
        if (d < 60) return "vừa xong";
        if (d < 3600) return (d / 60) + " phút";
        if (d < 86400) return (d / 3600) + " giờ";
        return (d / 86400) + " ngày";
    }

    /** "vừa xong", "5 phút trước", "3 giờ trước". */
    public static String agoPhrase(long ms, long now) {
        String a = ago(ms, now);
        return now - ms < 60_000L ? a : a + " trước";
    }

    /** Màu tăng/giảm kiểu bảng điện VN. */
    public static int changeColor(double change) {
        if (change > 0.000001) return Theme.UP;
        if (change < -0.000001) return Theme.DOWN;
        return Theme.FLAT;
    }

    public static double parseNumber(String raw) {
        if (raw == null) return Double.NaN;
        String s = raw.replaceAll("[^0-9,.+\\-]", "");
        if (s.length() == 0) return Double.NaN;
        // "24,220" hoặc "17,667.95" (kiểu Mỹ) -> bỏ dấu phẩy phân nghìn.
        if (s.indexOf('.') >= 0 && s.indexOf(',') >= 0) {
            s = s.replace(",", "");
        } else if (s.indexOf(',') >= 0) {
            int last = s.lastIndexOf(',');
            if (s.length() - last - 1 == 3) s = s.replace(",", "");
            else s = s.replace(",", ".");
        } else if (s.indexOf('.') >= 0) {
            int last = s.lastIndexOf('.');
            if (s.length() - last - 1 == 3 && s.indexOf('.') != last) s = s.replace(".", "");
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
