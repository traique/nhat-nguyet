package vn.nhatnguyet.sys;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.nhatnguyet.net.Http;

/**
 * Thống kê pin để người dùng biết máy tụt pin nhanh cỡ nào (đặt lịch ổ cắm hẹn giờ, phát hiện pin chai):
 * lưu mẫu % pin, đang sạc hay không, nhiệt độ, màn sáng hay tắt, tối đa 48 giờ, trong một tệp nhỏ.
 * Phần tính toán thuần Java để kiểm thử được.
 */
public final class BatteryStats {
    private static final String TAG = "BatteryStats";
    static final long MIN_GAP_MS = 5 * 60_000L;
    static final long KEEP_MS = 48 * 3_600_000L;
    static final long WINDOW_MS = 24 * 3_600_000L;
    /** Khoảng cách giữa hai mẫu lớn hơn mức này (khi màn sáng hoặc đang sạc) là thiếu số liệu, bỏ qua. */
    static final long MAX_PAIR_MS = 30 * 60_000L;
    /** Màn tắt có thể kéo dài cả đêm. */
    static final long MAX_OFF_PAIR_MS = 16 * 3_600_000L;

    public static final class Sample {
        final long t;
        final int pct;
        final boolean charging;
        final float temp;
        final boolean screenOn;

        Sample(long t, int pct, boolean charging, float temp, boolean screenOn) {
            this.t = t;
            this.pct = pct;
            this.charging = charging;
            this.temp = temp;
            this.screenOn = screenOn;
        }
    }

    /** Tốc độ tính bằng %/giờ; NaN khi chưa đủ số liệu. */
    public static final class Summary {
        public double drainScreenOn = Double.NaN;
        public double drainScreenOff = Double.NaN;
        public double chargeRate = Double.NaN;
        public float maxTemp = Float.NaN;
        public int samples;
    }

    private static BatteryStats instance;

    private final File file;
    private final List<Sample> samples = new ArrayList<Sample>();

    public static synchronized BatteryStats get(Context c) {
        if (instance == null) instance = new BatteryStats(new File(c.getApplicationContext().getFilesDir(), "battery_stats.txt"));
        return instance;
    }

    /** file = null: chỉ giữ trong bộ nhớ (kiểm thử). */
    BatteryStats(File file) {
        this.file = file;
        load();
    }

    /** Ghi một mẫu; bỏ qua nếu vừa ghi trong 5 phút mà trạng thái sạc/màn không đổi. */
    public synchronized void record(long t, int pct, boolean charging, float temp, boolean screenOn) {
        if (pct < 0 || pct > 100) return;
        Sample last = samples.isEmpty() ? null : samples.get(samples.size() - 1);
        if (last != null && t < last.t - 60_000L) samples.clear(); // giờ máy bị chỉnh lùi
        else if (last != null && t - last.t < MIN_GAP_MS && last.charging == charging && last.screenOn == screenOn) return;
        samples.add(new Sample(t, pct, charging, temp, screenOn));
        while (!samples.isEmpty() && t - samples.get(0).t > KEEP_MS) samples.remove(0);
        save();
    }

    public synchronized Summary summarize(long now) {
        Summary s = new Summary();
        long onTime = 0, offTime = 0, chgTime = 0;
        double onDrop = 0, offDrop = 0, chgGain = 0;
        Sample prev = null;
        for (Sample b : samples) {
            if (now - b.t > WINDOW_MS || b.t > now + 60_000L) {
                prev = null;
                continue;
            }
            s.samples++;
            if (!Float.isNaN(b.temp) && (Float.isNaN(s.maxTemp) || b.temp > s.maxTemp)) s.maxTemp = b.temp;
            Sample a = prev;
            prev = b;
            if (a == null) continue;
            long dt = b.t - a.t;
            if (dt <= 0) continue;
            if (!a.charging && !b.charging) {
                if (a.screenOn && dt <= MAX_PAIR_MS) {
                    onDrop += a.pct - b.pct;
                    onTime += dt;
                } else if (!a.screenOn && dt <= MAX_OFF_PAIR_MS) {
                    offDrop += a.pct - b.pct;
                    offTime += dt;
                }
            } else if (a.charging && b.charging && dt <= MAX_PAIR_MS && a.pct < 99) {
                chgGain += b.pct - a.pct;
                chgTime += dt;
            }
        }
        if (onTime >= 20 * 60_000L) s.drainScreenOn = Math.max(0, onDrop) / (onTime / 3_600_000.0);
        if (offTime >= 60 * 60_000L) s.drainScreenOff = Math.max(0, offDrop) / (offTime / 3_600_000.0);
        if (chgTime >= 15 * 60_000L) s.chargeRate = Math.max(0, chgGain) / (chgTime / 3_600_000.0);
        return s;
    }

    /** Đoạn mô tả cho màn Cài đặt. */
    public static String describe(Summary s, int pct, boolean charging, float temp) {
        StringBuilder sb = new StringBuilder();
        if (pct >= 0) {
            sb.append("Pin ").append(pct).append('%').append(charging ? ", đang sạc" : ", không sạc");
            if (!Float.isNaN(temp)) sb.append(", ").append(Math.round(temp)).append("°C");
            sb.append('.');
        }
        if (!Float.isNaN(s.maxTemp)) {
            sb.append(" Nóng nhất 24 giờ qua: ").append(Math.round(s.maxTemp)).append("°C.");
        }
        if (!Double.isNaN(s.drainScreenOn)) {
            sb.append("\nMàn sáng, rút sạc: tụt khoảng ").append(rate(s.drainScreenOn)).append("%/giờ");
            if (s.drainScreenOn >= 0.5) sb.append(", đầy pin dùng được khoảng ").append(hours(100 / s.drainScreenOn));
            sb.append('.');
        }
        if (!Double.isNaN(s.drainScreenOff)) {
            sb.append("\nMàn tắt, rút sạc: tụt khoảng ").append(rate(s.drainScreenOff)).append("%/giờ.");
        }
        if (!Double.isNaN(s.chargeRate)) {
            sb.append("\nĐang sạc: lên khoảng ").append(rate(s.chargeRate)).append("%/giờ");
            if (s.chargeRate >= 1) sb.append(", từ 50% lên 80% mất khoảng ").append(hours(30 / s.chargeRate));
            sb.append('.');
        }
        if (Double.isNaN(s.drainScreenOn) && Double.isNaN(s.chargeRate)) {
            sb.append("\nChưa đủ số liệu. Để đo tốc độ tụt pin, rút sạc khoảng nửa giờ khi màn đang sáng.");
        }
        return sb.toString().trim();
    }

    static String rate(double v) {
        return v < 10 ? String.format(Locale.US, "%.1f", v).replace('.', ',') : Long.toString(Math.round(v));
    }

    static String hours(double h) {
        if (h < 1) return Math.max(1, Math.round(h * 60)) + " phút";
        if (h < 10) return String.format(Locale.US, "%.1f", h).replace('.', ',').replace(",0", "") + " giờ";
        return Math.round(h) + " giờ";
    }

    // ------------------------------------------------------------ lưu tệp

    private void load() {
        if (file == null || !file.exists()) return;
        try {
            FileInputStream in = new FileInputStream(file);
            String text;
            try {
                text = new String(Http.readAll(in, 256 * 1024), Http.UTF8);
            } finally {
                Http.closeQuietly(in);
            }
            for (String line : text.split("\n")) {
                String[] p = line.trim().split(",");
                if (p.length < 5) continue;
                try {
                    int t10 = Integer.parseInt(p[3]);
                    samples.add(new Sample(Long.parseLong(p[0]), Integer.parseInt(p[1]), "1".equals(p[2]),
                            t10 == Integer.MIN_VALUE ? Float.NaN : t10 / 10f, "1".equals(p[4])));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "load", e);
        }
    }

    private void save() {
        if (file == null) return;
        StringBuilder sb = new StringBuilder(samples.size() * 32);
        for (Sample s : samples) {
            sb.append(s.t).append(',').append(s.pct).append(',').append(s.charging ? '1' : '0').append(',')
                    .append(Float.isNaN(s.temp) ? Integer.MIN_VALUE : Math.round(s.temp * 10)).append(',')
                    .append(s.screenOn ? '1' : '0').append('\n');
        }
        try {
            File tmp = new File(file.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                out.write(sb.toString().getBytes(Http.UTF8));
            } finally {
                Http.closeQuietly(out);
            }
            if (!tmp.renameTo(file)) Log.w(TAG, "rename failed");
        } catch (Exception e) {
            Log.w(TAG, "save", e);
        }
    }
}
