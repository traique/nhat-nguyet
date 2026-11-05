package vn.nhatnguyet.ui;

import vn.nhatnguyet.Prefs;

/**
 * Quyết định màn hình theo giờ ngày/đêm và mức rảnh: giữ sáng hay không, độ sáng bao nhiêu,
 * có cần tắt hẳn (quản trị thiết bị) hay không. Thuần Java để kiểm thử được; MainActivity chỉ áp dụng.
 */
public final class NightPolicy {
    /** screenBrightness: -1 = theo máy (BRIGHTNESS_OVERRIDE_NONE), 0 = tắt (BRIGHTNESS_OVERRIDE_OFF). */
    public static final float BRIGHT_SYSTEM = -1f;
    public static final float BRIGHT_OFF = 0f;

    public final boolean keepOn;
    public final float brightness;
    /** Đêm, chế độ tắt hẳn, đã hết thời gian rảnh: gọi lockNow() một lần. */
    public final boolean lockNow;

    NightPolicy(boolean keepOn, float brightness, boolean lockNow) {
        this.keepOn = keepOn;
        this.brightness = brightness;
        this.lockNow = lockNow;
    }

    /**
     * @param night         ngoài giờ ngày (và không bật "luôn sáng")
     * @param mode          Prefs.NIGHT_DIM / NIGHT_TIMEOUT / NIGHT_OFF
     * @param idleMs        thời gian từ lần tương tác cuối
     * @param idleLimitMs   ngưỡng rảnh ban đêm
     * @param dayPercent    độ sáng ngày 0..100, 0 = theo máy
     * @param nightPercent  độ sáng đêm 1..100
     * @param livePercent   độ sáng đang kéo tay (>0) hoặc -1
     * @param blackout      đang phủ đen chờ máy tự tắt
     * @param lockedAlready đã gọi lockNow trong đợt rảnh này
     */
    public static NightPolicy decide(boolean night, String mode, long idleMs, long idleLimitMs,
                                     int dayPercent, int nightPercent, int livePercent,
                                     boolean blackout, boolean lockedAlready) {
        boolean keepOn;
        float bright;
        boolean lock = false;
        if (!night) {
            keepOn = true;
            bright = dayPercent <= 0 ? BRIGHT_SYSTEM : Math.min(1f, dayPercent / 100f);
        } else {
            bright = Math.max(0.01f, Math.min(1f, nightPercent / 100f));
            boolean active = idleMs < idleLimitMs;
            if (Prefs.NIGHT_DIM.equals(mode) || active) {
                keepOn = true;
            } else {
                keepOn = false;
                lock = Prefs.NIGHT_OFF.equals(mode) && !lockedAlready;
            }
        }
        if (livePercent > 0) bright = livePercent / 100f;
        if (blackout) {
            keepOn = false;
            bright = BRIGHT_OFF;
            lock = false;
        }
        return new NightPolicy(keepOn, bright, lock);
    }
}
