package vn.nhatnguyet.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/**
 * Điều phối các nguồn dữ liệu: tải định kỳ khi màn hình sáng, lưu bộ nhớ đệm
 * ra file để mất mạng/khởi động lại vẫn có dữ liệu cũ hiển thị.
 * Mọi thay đổi trạng thái đều diễn ra trên luồng chính.
 */
public final class DataHub {
    private static final String TAG = "DataHub";
    private static final long TICK_MS = 15_000L;
    /** nextAt = 0: chưa hẹn (dùng hạn của bộ nhớ đệm); FORCE: tải ngay. */
    private static final long FORCE = -1L;

    public interface Listener {
        void onSectionChanged(String key);
    }

    public static final class Entry {
        public JSONObject data;
        public long at;
        public String error;
        public long errorAt;
        public boolean loading;
        long nextAt;
        int fails;
        String config;
    }

    private static DataHub instance;

    private final Context app;
    private final Prefs prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "nn-net");
            t.setPriority(Thread.MIN_PRIORITY + 1);
            return t;
        }
    });
    private final Map<String, Source> sources = new LinkedHashMap<String, Source>();
    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    private final List<Listener> listeners = new ArrayList<Listener>();
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            check();
            if (running) main.postDelayed(this, TICK_MS);
        }
    };

    public static synchronized DataHub get(Context context) {
        if (instance == null) instance = new DataHub(context.getApplicationContext());
        return instance;
    }

    private DataHub(Context app) {
        this.app = app;
        this.prefs = new Prefs(app);
        add(new WeatherSource());
        add(new GoldSource());
        add(new FuelSource());
        add(new FxSource());
        add(new StockSource());
        add(new NewsSource());
        for (Source s : sources.values()) {
            Entry e = new Entry();
            loadCache(s, e);
            entries.put(s.key, e);
        }
    }

    private void add(Source s) {
        sources.put(s.key, s);
    }

    public Source source(String key) {
        return sources.get(key);
    }

    public Entry entry(String key) {
        return entries.get(key);
    }

    public JSONObject data(String key) {
        Entry e = entries.get(key);
        return e == null ? null : e.data;
    }

    public boolean isStale(String key) {
        Entry e = entries.get(key);
        Source s = sources.get(key);
        if (e == null || s == null || e.data == null) return false;
        return System.currentTimeMillis() - e.at > s.staleMs(prefs);
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    /** Bắt đầu tải định kỳ (gọi khi màn hình chính hiện). */
    public void start() {
        running = true;
        main.removeCallbacks(tick);
        main.post(tick);
    }

    public void stop() {
        running = false;
        main.removeCallbacks(tick);
    }

    /** Tải lại ngay mọi nguồn (người dùng bấm dòng trạng thái). */
    public void refreshAll() {
        if (!onMain()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    refreshAll();
                }
            });
            return;
        }
        long now = System.currentTimeMillis();
        for (Source s : sources.values()) {
            Entry e = entries.get(s.key);
            // Nguồn có giới hạn tần suất (vd. Vietcombank) không được ép tải sớm hơn.
            if (e.data != null && now - e.at < s.minGapMs(prefs)) continue;
            e.nextAt = FORCE;
        }
        check();
    }

    /** Gọi sau khi đổi cài đặt (thành phố, mã CK, RSS…). Gọi được từ luồng bất kỳ. */
    public void settingsChanged() {
        if (!onMain()) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    settingsChanged();
                }
            });
            return;
        }
        for (Source s : sources.values()) {
            Entry e = entries.get(s.key);
            String cfg = s.configKey(prefs);
            if (e.config != null && !e.config.equals(cfg)) {
                e.data = null;
                e.error = null;
                e.at = 0;
                e.nextAt = FORCE;
                deleteCache(s);
                notifyChanged(s.key);
            }
        }
        check();
    }

    private static boolean onMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private void check() {
        long now = System.currentTimeMillis();
        for (final Source s : sources.values()) {
            final Entry e = entries.get(s.key);
            if (e.loading || !s.enabled(prefs)) continue;
            if (e.config != null && !e.config.equals(s.configKey(prefs))) {
                e.data = null;
                e.config = null;
                e.nextAt = FORCE;
            }
            if (e.nextAt == 0 && e.data != null && e.error == null && now - e.at < s.intervalMs(prefs) && now >= e.at) {
                // Dữ liệu từ bộ nhớ đệm còn mới: hẹn lần tải kế tiếp.
                e.nextAt = e.at + s.intervalMs(prefs);
            }
            if (e.data != null && e.error == null && e.nextAt > 0) {
                // Chu kỳ có thể ngắn lại (vd. chứng khoán vừa mở cửa): không chờ hết chu kỳ cũ.
                long due = e.at + s.intervalMs(prefs);
                if (e.nextAt > due) e.nextAt = Math.max(due, e.at + s.minGapMs(prefs));
            }
            if (now < e.nextAt) continue;
            load(s, e);
        }
    }

    private void load(final Source s, final Entry e) {
        e.loading = true;
        final String cfg = s.configKey(prefs);
        notifyChanged(s.key);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject result = null;
                Throwable error = null;
                try {
                    result = s.fetch(app, prefs);
                } catch (Throwable t) {
                    error = t;
                    Log.w(TAG, s.key + " failed: " + t);
                }
                final JSONObject r = result;
                final Throwable err = error;
                if (r != null) saveCache(s, r, cfg);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        done(s, e, r, err, cfg);
                    }
                });
            }
        });
    }

    private void done(Source s, Entry e, JSONObject r, Throwable err, String cfg) {
        long now = System.currentTimeMillis();
        e.loading = false;
        if (!cfg.equals(s.configKey(prefs))) {
            // Cài đặt đã đổi trong lúc tải: bỏ kết quả, tải lại ngay.
            e.nextAt = FORCE;
            notifyChanged(s.key);
            if (running) check();
            return;
        }
        if (r != null) {
            e.data = r;
            e.at = now;
            e.error = null;
            e.fails = 0;
            e.config = cfg;
            e.nextAt = now + s.intervalMs(prefs);
        } else {
            e.error = Http.describe(err);
            e.errorAt = now;
            e.fails++;
            long retry = Math.min(s.intervalMs(prefs), 60_000L * (1L << Math.min(e.fails - 1, 4)));
            e.nextAt = now + Math.max(Math.max(30_000L, s.minGapMs(prefs)), retry);
        }
        notifyChanged(s.key);
    }

    private void notifyChanged(String key) {
        for (Listener l : new ArrayList<Listener>(listeners)) l.onSectionChanged(key);
    }

    // ------------------------------------------------------------ cache

    private File cacheFile(Source s) {
        return new File(app.getFilesDir(), "cache_" + s.key + ".json");
    }

    private void saveCache(Source s, JSONObject data, String cfg) {
        try {
            JSONObject o = new JSONObject();
            o.put("at", System.currentTimeMillis());
            o.put("config", cfg);
            o.put("data", data);
            File tmp = new File(app.getFilesDir(), "cache_" + s.key + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                out.write(o.toString().getBytes(Http.UTF8));
            } finally {
                Http.closeQuietly(out);
            }
            if (!tmp.renameTo(cacheFile(s))) Log.w(TAG, "rename failed " + s.key);
        } catch (Exception ex) {
            Log.w(TAG, "save cache " + s.key, ex);
        }
    }

    private void loadCache(Source s, Entry e) {
        File f = cacheFile(s);
        if (!f.exists()) return;
        try {
            FileInputStream in = new FileInputStream(f);
            byte[] b;
            try {
                b = Http.readAll(in, 4 * 1024 * 1024);
            } finally {
                Http.closeQuietly(in);
            }
            JSONObject o = new JSONObject(new String(b, Http.UTF8));
            e.data = o.optJSONObject("data");
            e.at = o.optLong("at");
            e.config = o.optString("config", "");
        } catch (Exception ex) {
            Log.w(TAG, "load cache " + s.key, ex);
        }
    }

    private void deleteCache(Source s) {
        //noinspection ResultOfMethodCallIgnored
        cacheFile(s).delete();
    }
}
