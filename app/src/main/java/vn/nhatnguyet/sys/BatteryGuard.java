package vn.nhatnguyet.sys;

/**
 * Tự giảm sáng để bảo vệ pin: pin nóng (máy cũ cắm sạc liên tục dễ phồng) hoặc pin yếu khi không sạc.
 * Có khoảng trễ để độ sáng không nhấp nháy quanh ngưỡng. Thuần Java để kiểm thử được.
 */
public final class BatteryGuard {
    /** Bắt đầu giảm sáng khi pin nóng từ mức này, thôi giảm khi nguội dưới COOL_C. */
    public static final float HOT_C = 42f;
    public static final float COOL_C = 40f;
    /** Rất nóng: giảm sâu hơn. */
    public static final float VERY_HOT_C = 45f;
    /** Pin yếu (không sạc) từ mức này trở xuống; thôi khi sạc hoặc lên trên LOW_CLEAR. */
    public static final int LOW_PCT = 20;
    public static final int LOW_CLEAR = 25;

    public static final int CAP_HOT = 30;
    public static final int CAP_VERY_HOT = 15;
    public static final int CAP_LOW = 30;

    private boolean hot;
    private boolean low;
    private float temp = Float.NaN;

    /** @return true nếu trần độ sáng đổi. */
    public boolean update(int pct, boolean charging, float tempC) {
        int before = cap();
        temp = tempC;
        if (!Float.isNaN(tempC)) {
            if (tempC >= HOT_C) hot = true;
            else if (tempC < COOL_C) hot = false;
        }
        if (pct < 0 || charging) low = false;
        else if (pct <= LOW_PCT) low = true;
        else if (pct > LOW_CLEAR) low = false;
        return cap() != before;
    }

    /** Trần độ sáng (%) đang áp dụng, 0 = không giới hạn. */
    public int cap() {
        if (hot) return !Float.isNaN(temp) && temp >= VERY_HOT_C ? CAP_VERY_HOT : CAP_HOT;
        if (low) return CAP_LOW;
        return 0;
    }

    public boolean hot() {
        return hot;
    }

    public boolean low() {
        return low;
    }
}
