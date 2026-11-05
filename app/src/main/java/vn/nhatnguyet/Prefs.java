package vn.nhatnguyet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Toàn bộ cài đặt của app (SharedPreferences "settings"). */
public final class Prefs {
    public static final String NIGHT_DIM = "dim";
    public static final String NIGHT_TIMEOUT = "timeout";
    public static final String NIGHT_OFF = "off";

    /** Màn hình chính: bảng lịch đầy đủ hoặc chỉ đồng hồ lật kiểu HTC. */
    public static final String DISPLAY_BOARD = "board";
    public static final String DISPLAY_FLIP = "flip";

    public static final String LAYOUT_AUTO = "auto";
    public static final String LAYOUT_2 = "2";
    public static final String LAYOUT_3 = "3";

    /** Thứ tự và tên các trang bên phải. */
    public static final String[] PAGE_KEYS = {"sheet", "month", "lunar", "weather", "gold", "fuel", "fx", "stocks", "news"};
    public static final String[] PAGE_NAMES = {"Tờ lịch (ngày dương, ngày âm, giờ tốt, ca dao)", "Lịch tháng",
            "Âm lịch chi tiết và ngày lễ sắp tới", "Dự báo thời tiết", "Giá vàng", "Giá xăng dầu", "Tỷ giá Vietcombank",
            "Chứng khoán", "Tin tức"};

    /** Trang bật sẵn lần đầu: đủ cho lịch treo tường gia đình; chứng khoán, tin tức bật thêm trong Cài đặt. */
    public static final String[] DEFAULT_PAGES = {"sheet", "month", "weather", "gold", "fuel"};

    public static final String DEFAULT_WATCHLIST = "VNINDEX,VN30,FPT,VCB,HPG,MWG";
    /** Nguồn tin có tên, chọn bằng ô tick trong Cài đặt: {tên, địa chỉ RSS}. */
    public static final String[][] FEED_CHOICES = {
            {"Tuổi Trẻ", "https://tuoitre.vn/rss/tin-moi-nhat.rss"},
            {"VnExpress", "https://vnexpress.net/rss/tin-moi-nhat.rss"},
            {"Thanh Niên", "https://thanhnien.vn/rss/home.rss"},
            {"Dân Trí", "https://dantri.com.vn/rss/home.rss"},
            {"VnExpress Kinh doanh", "https://vnexpress.net/rss/kinh-doanh.rss"},
            {"CafeF Chứng khoán", "https://cafef.vn/thi-truong-chung-khoan.rss"},
            {"GenK Công nghệ", "https://genk.vn/rss/home.rss"},
    };
    public static final String[] DEFAULT_FEEDS = {
            "https://tuoitre.vn/rss/tin-moi-nhat.rss",
            "https://vnexpress.net/rss/tin-moi-nhat.rss",
    };

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public SharedPreferences raw() {
        return sp;
    }

    // ------------------------------------------------------------ thời tiết

    public String placeName() {
        return sp.getString("place_name", "TP. Hồ Chí Minh");
    }

    public double placeLat() {
        return sp.getFloat("place_lat", 10.8231f);
    }

    public double placeLon() {
        return sp.getFloat("place_lon", 106.6297f);
    }

    public void setPlace(String name, double lat, double lon) {
        sp.edit().putString("place_name", name).putFloat("place_lat", (float) lat)
                .putFloat("place_lon", (float) lon).apply();
    }

    // ------------------------------------------------------------ chứng khoán, tin

    public List<String> watchlist() {
        String raw = sp.getString("watchlist", DEFAULT_WATCHLIST);
        Set<String> out = new LinkedHashSet<String>();
        for (String s : raw.split("[,;\\s]+")) {
            String t = s.trim().toUpperCase(Locale.US);
            if (t.matches("[A-Z0-9]{2,12}")) out.add(t);
        }
        return new ArrayList<String>(out);
    }

    public String watchlistRaw() {
        return sp.getString("watchlist", DEFAULT_WATCHLIST);
    }

    /** Để trống thì quay về danh sách mặc định. */
    public void setWatchlist(String v) {
        if (v == null || v.trim().length() == 0) sp.edit().remove("watchlist").apply();
        else sp.edit().putString("watchlist", v.trim()).apply();
    }

    public List<String> feeds() {
        String raw = sp.getString("feeds", null);
        List<String> out = new ArrayList<String>();
        if (raw == null) {
            for (String f : DEFAULT_FEEDS) out.add(f);
            return out;
        }
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (isFeedUrl(t)) out.add(t);
        }
        return out;
    }

    /** Chỉ nhận địa chỉ https (Android 9+ chặn http thường; máy cũ cũng nên tránh). */
    public static boolean isFeedUrl(String t) {
        return t.startsWith("https://") && t.length() > 10 && !t.contains(" ");
    }

    public String feedsRaw() {
        List<String> f = feeds();
        StringBuilder sb = new StringBuilder();
        for (String s : f) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }

    /** Để trống thì quay về danh sách mặc định. */
    public void setFeeds(String v) {
        if (v == null || v.trim().length() == 0) sp.edit().remove("feeds").apply();
        else sp.edit().putString("feeds", v.trim()).apply();
    }

    public void setFeeds(List<String> urls) {
        StringBuilder sb = new StringBuilder();
        for (String u : urls) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(u);
        }
        setFeeds(sb.toString());
    }

    /** Ngày riêng (giỗ, sinh nhật), mỗi dòng một ngày, vd "15/8 âm Giỗ ông nội". */
    public String personalDaysRaw() {
        return sp.getString("personal_days", "");
    }

    // ------------------------------------------------------------ màn hình

    public boolean alwaysOn() {
        return sp.getBoolean("always_on", false);
    }

    public int dayStart() {
        return sp.getInt("day_start", 6);
    }

    public int dayEnd() {
        return sp.getInt("day_end", 22);
    }

    public String nightMode() {
        return sp.getString("night_mode", NIGHT_TIMEOUT);
    }

    public int idleMinutes() {
        return sp.getInt("idle_minutes", 2);
    }

    public int nightBrightness() {
        return sp.getInt("night_brightness", 15);
    }

    /** 0 = theo hệ thống. */
    public int dayBrightness() {
        return sp.getInt("day_brightness", 0);
    }

    public int rotateSeconds() {
        return sp.getInt("rotate_seconds", 20);
    }

    public String layout() {
        return sp.getString("layout", LAYOUT_AUTO);
    }

    public int fontScale() {
        return sp.getInt("font_scale", 100);
    }

    public String displayMode() {
        return sp.getString("display_mode", DISPLAY_BOARD);
    }

    /** Chế độ chỉ đồng hồ lật. */
    public boolean clockOnly() {
        return DISPLAY_FLIP.equals(displayMode());
    }

    /** Hiện ngày, âm lịch, thời tiết dưới đồng hồ lật. */
    public boolean flipInfo() {
        return sp.getBoolean("flip_info", true);
    }

    /** Ngoài giờ ngày: chữ chuyển tông ấm, bớt ánh sáng xanh. */
    public boolean warmNight() {
        return sp.getBoolean("warm_night", true);
    }

    public boolean burnInShift() {
        return sp.getBoolean("burn_in_shift", true);
    }

    public boolean pageEnabled(String key) {
        String raw = sp.getString("pages", null);
        if (raw == null) {
            for (String k : DEFAULT_PAGES) if (k.equals(key)) return true;
            return false;
        }
        for (String s : raw.split(",")) if (s.equals(key)) return true;
        return false;
    }

    public List<String> enabledPages() {
        List<String> out = new ArrayList<String>();
        for (String k : PAGE_KEYS) if (pageEnabled(k)) out.add(k);
        if (out.isEmpty()) out.add("sheet");
        return out;
    }

    public void setPages(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) sb.append(',');
            sb.append(k);
        }
        sp.edit().putString("pages", sb.toString()).apply();
    }

    // ------------------------------------------------------------ đánh thức

    public boolean proximityWake() {
        return sp.getBoolean("proximity_wake", true);
    }

    /** Che cảm biến tiệm cận khi bảng lịch đang sáng thì tắt màn hình. */
    public boolean proximitySleep() {
        return sp.getBoolean("proximity_sleep", true);
    }

    public boolean wakeMorning() {
        return sp.getBoolean("wake_morning", true);
    }

    public boolean autoStart() {
        return sp.getBoolean("auto_start", true);
    }

    // ------------------------------------------------------------ cập nhật

    public boolean autoUpdate() {
        return sp.getBoolean("auto_update", true);
    }

    /** Giờ tự mở màn hình cài bản mới: cố định 10 giờ sáng (có người ở nhà, máy đã sáng). */
    public int updateInstallHour() {
        return 10;
    }

    // ------------------------------------------------------------ lần đầu dùng

    /** Đã hỏi nơi ở lần đầu mở app chưa. */
    public boolean setupDone() {
        return sp.getBoolean("setup_done", false) || sp.getBoolean("hint_shown", false);
    }

    public void setSetupDone() {
        sp.edit().putBoolean("setup_done", true).apply();
    }

    /** Khoá PIN: số lần nhập sai liên tiếp và mốc được thử lại. */
    public int pinFails() {
        return sp.getInt("pin_fails", 0);
    }

    public long pinLockUntil() {
        return sp.getLong("pin_lock_until", 0);
    }

    public void setPinFails(int fails, long lockUntil) {
        sp.edit().putInt("pin_fails", fails).putLong("pin_lock_until", lockUntil).apply();
    }

    public long lastUpdateCheck() {
        return sp.getLong("last_update_check", 0);
    }

    public void setLastUpdateCheck(long t) {
        sp.edit().putLong("last_update_check", t).apply();
    }

    // ------------------------------------------------------------ khoá cài đặt

    public boolean hasSettingsPin() {
        return sp.getString("settings_pin", "").length() > 0;
    }

    public boolean checkSettingsPin(String pin) {
        return hashPin(pin).equals(sp.getString("settings_pin", ""));
    }

    public void setPersonalDays(String text) {
        sp.edit().putString("personal_days", text == null ? "" : text.trim()).apply();
    }

    /** null = bỏ khoá. Chỉ lưu mã băm, không lưu mã PIN gốc. */
    public void setSettingsPin(String pin) {
        if (pin == null) sp.edit().remove("settings_pin").apply();
        else sp.edit().putString("settings_pin", hashPin(pin)).apply();
    }

    static String hashPin(String pin) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(("nhatnguyet:" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------ tiện ích

    public void putInt(String key, int v) {
        sp.edit().putInt(key, v).apply();
    }

    public void putBool(String key, boolean v) {
        sp.edit().putBoolean(key, v).apply();
    }

    public void putString(String key, String v) {
        sp.edit().putString(key, v).apply();
    }

    /** Có đang trong "giờ ngày" không (màn hình luôn sáng). */
    public boolean isDaytime(Calendar now) {
        int h = now.get(Calendar.HOUR_OF_DAY);
        int s = dayStart();
        int e = dayEnd();
        if (s == e) return true;
        if (s < e) return h >= s && h < e;
        return h >= s || h < e;
    }

    public boolean isNight(Calendar now) {
        return !alwaysOn() && !isDaytime(now);
    }
}
