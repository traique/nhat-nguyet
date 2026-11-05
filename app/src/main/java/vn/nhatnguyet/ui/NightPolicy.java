package vn.nhatnguyet.ui;

import vn.nhatnguyet.Prefs;

/**
 * Quyết định màn hình theo giờ ngày/đêm, giờ vắng nhà và mức rảnh: giữ sáng hay không, độ sáng bao nhiêu,
 * có cần tắt hẳn (quản trị thiết bị) hay không. Thuần Java để kiểm thử được; MainActivity chỉ áp dụng.
 */
public final class NightPolicy {
    /** screenBrightness: -1 = theo máy (BRIGHTNESS_OVERRIDE_NONE), 0 = tắt (BRIGHTNESS_OVERRIDE_OFF). */
    public static final float BRIGHT_SYSTEM = -1f;
    public static final float BRIGHT_OFF = 0f;

    public final boolean keepOn;
    public final float brightness;
    /** Hết thời gian rảnh lúc cần tắt màn (đêm chế độ tắt, hoặc giờ vắng nhà): gọi lockNow() một lần. */
    public final boolean lockNow;

    NightPolicy(boolean keepOn, float brightness, boolean lockNow) {
        this.keepOn = keepOn;
        this.brightness = brightness;
        this.lockNow = lockNow;
    }

    /** Các giá trị đầu vào; dùng lớp này để lời gọi đọc được thay vì hàm chục tham số. */
    public static final class Input {
        /** Ngoài giờ ngày (và không bật "luôn sáng"). */
        public boolean night;
        /** Đang trong giờ vắng nhà: tắt màn khi không ai chạm, kể cả ban ngày. */
        public boolean away;
        /** Prefs.NIGHT_DIM / NIGHT_TIMEOUT / NIGHT_OFF. */
        public String mode = Prefs.NIGHT_OFF;
        public long idleMs;
        public long idleLimitMs;
        /** Độ sáng ngày 0..100, 0 = theo máy. */
        public int dayPercent;
        /** Độ sáng đêm 1..100. */
        public int nightPercent = 15;
        /** Độ sáng đang kéo tay (>0) hoặc -1. */
        public int livePercent = -1;
        /** Trần độ sáng khi pin nóng hoặc yếu (1..100), 0 = không giới hạn. */
        public int capPercent;
        /** Đang phủ đen chờ máy tự tắt. */
        public boolean blackout;
        /** Đã gọi lockNow trong đợt rảnh này. */
        public boolean lockedAlready;
    }

    public static NightPolicy decide(Input in) {
        boolean keepOn;
        float bright;
        boolean lock = false;
        boolean active = in.idleMs < in.idleLimitMs;
        float day = in.dayPercent <= 0 ? BRIGHT_SYSTEM : Math.min(1f, in.dayPercent / 100f);
        if (in.away) {
            // Giờ vắng nhà: có người chạm thì sáng như ban ngày, rảnh thì tắt bất kể chế độ đêm.
            keepOn = active;
            bright = in.night ? nightBright(in) : day;
            lock = !active && !in.lockedAlready;
        } else if (!in.night) {
            keepOn = true;
            bright = day;
        } else {
            bright = nightBright(in);
            if (Prefs.NIGHT_DIM.equals(in.mode) || active) {
                keepOn = true;
            } else {
                keepOn = false;
                lock = Prefs.NIGHT_OFF.equals(in.mode) && !in.lockedAlready;
            }
        }
        if (in.livePercent > 0) {
            bright = in.livePercent / 100f;
        } else if (in.capPercent > 0) {
            float cap = in.capPercent / 100f;
            bright = bright == BRIGHT_SYSTEM ? cap : Math.min(bright, cap);
        }
        if (in.blackout) {
            keepOn = false;
            bright = BRIGHT_OFF;
            lock = false;
        }
        return new NightPolicy(keepOn, bright, lock);
    }

    private static float nightBright(Input in) {
        return Math.max(0.01f, Math.min(1f, in.nightPercent / 100f));
    }
}
