package vn.nhatnguyet;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Bảng màu theo vai trò (mỗi màu một nghĩa) và font dùng chung.
 * ACCENT chỉ cho thứ bấm được hoặc "hiện tại"; LUNAR cho âm lịch, ngày lễ, Chủ nhật;
 * AUSPICIOUS cho ngày/giờ hoàng đạo; UP/DOWN/FLAT chỉ cho giá tăng/giảm.
 * Ban đêm có thể chuyển sang tông ấm (bớt ánh sáng xanh), nên các màu không phải hằng số.
 */
public final class Theme {
    public static final int BG = 0xFF000000;

    public static int PANEL, PANEL_LINE, TEXT, DIM, META, ACCENT, TODAY_BG, LUNAR, AUSPICIOUS, UP, DOWN, FLAT, WARN, RAIN;

    private static boolean warm;
    private static Typeface regular;
    private static Typeface bold;

    static {
        apply();
    }

    private Theme() {}

    /** Bật/tắt tông ấm; trả về true nếu có đổi (khi đó cần dựng lại giao diện). */
    public static boolean setWarm(boolean on) {
        if (on == warm) return false;
        warm = on;
        apply();
        return true;
    }

    private static void apply() {
        PANEL = tint(0xFF0D1217);
        PANEL_LINE = tint(0xFF1F2A33);
        TEXT = tint(0xFFF3F5F4);
        DIM = tint(0xFFA9B6B2);
        META = tint(0xFF8A9894);
        ACCENT = tint(0xFF5FD3B0);
        TODAY_BG = tint(0xFF1F6F5A);
        LUNAR = tint(0xFFFF7F6A);
        AUSPICIOUS = tint(0xFFE9B95B);
        UP = tint(0xFF3DD68C);
        DOWN = tint(0xFFFF6B6B);
        FLAT = tint(0xFFE9B95B);
        WARN = tint(0xFFFFB84D);
        RAIN = tint(0xFF7FC4FF);
    }

    /** Màu ở tông hiện tại: ban đêm giảm kênh xanh lá và xanh dương. */
    public static int tint(int c) {
        if (!warm) return c;
        int g = Math.round(((c >> 8) & 0xFF) * 0.86f);
        int b = Math.round((c & 0xFF) * 0.6f);
        return (c & 0xFFFF0000) | (g << 8) | b;
    }

    public static synchronized void loadFonts(Context c) {
        if (regular != null) return;
        try {
            regular = Typeface.createFromAsset(c.getAssets(), "fonts/inter_regular.ttf");
            bold = Typeface.createFromAsset(c.getAssets(), "fonts/inter_semibold.ttf");
        } catch (Throwable t) {
            regular = Typeface.DEFAULT;
            bold = Typeface.DEFAULT_BOLD;
        }
    }

    public static Typeface regular() {
        return regular != null ? regular : Typeface.DEFAULT;
    }

    public static Typeface bold() {
        return bold != null ? bold : Typeface.DEFAULT_BOLD;
    }
}
