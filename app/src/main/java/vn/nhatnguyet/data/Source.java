package vn.nhatnguyet.data;

import android.content.Context;

import org.json.JSONObject;

import vn.nhatnguyet.Prefs;

/** Một nguồn dữ liệu (thời tiết, vàng, …). fetch() chạy trên luồng nền. */
public abstract class Source {
    public final String key;
    public final String title;

    protected Source(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public abstract JSONObject fetch(Context context, Prefs prefs) throws Exception;

    /** Chu kỳ làm mới khi màn hình đang sáng. */
    public abstract long intervalMs(Prefs prefs);

    /** Khoảng cách tối thiểu giữa hai lần gọi (nguồn có giới hạn tần suất); 0 = không giới hạn. */
    public long minGapMs(Prefs prefs) {
        return 0;
    }

    /** Dữ liệu cũ hơn mức này thì hiện nhãn "dữ liệu cũ". */
    public long staleMs(Prefs prefs) {
        return Math.max(3 * intervalMs(prefs), 30 * 60_000L);
    }

    public boolean enabled(Prefs prefs) {
        // Chế độ chỉ đồng hồ không hiện các trang nên không tải (trừ thời tiết).
        return !prefs.clockOnly() && prefs.pageEnabled(key);
    }

    /** Khoá nhận diện cấu hình (vd. thành phố). Đổi khoá thì dữ liệu cũ bị bỏ. */
    public String configKey(Prefs prefs) {
        return "";
    }
}
