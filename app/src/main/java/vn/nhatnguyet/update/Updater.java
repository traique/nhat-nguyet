package vn.nhatnguyet.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import vn.nhatnguyet.BuildConfig;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.net.Http;

/**
 * Cập nhật OTA từ GitHub Release: đọc update.json, tải APK ngầm, kiểm tra SHA-256
 * và tên gói, rồi mở trình cài đặt. Trên Android 4.x luôn cần người bấm "Cài đặt".
 */
public final class Updater {
    private static final String TAG = "Updater";
    private static final long AUTO_CHECK_MS = 6 * 3600_000L;
    private static final long MAX_APK = 60L * 1024 * 1024;

    public enum State { IDLE, CHECKING, UP_TO_DATE, DOWNLOADING, READY, ERROR }

    public interface Listener {
        void onUpdateChanged();
    }

    private static Updater instance;

    private final Context app;
    private final Prefs prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<Listener>();

    private State state = State.IDLE;
    private String message = "";
    private int progress;
    private int readyCode;
    private String readyName = "";
    private String readyNotes = "";

    public static synchronized Updater get(Context c) {
        if (instance == null) instance = new Updater(c.getApplicationContext());
        return instance;
    }

    private Updater(Context app) {
        this.app = app;
        this.prefs = new Prefs(app);
        readyCode = prefs.raw().getInt("update_ready_code", 0);
        readyName = prefs.raw().getString("update_ready_name", "");
        readyNotes = prefs.raw().getString("update_ready_notes", "");
        if (readyCode > BuildConfig.VERSION_CODE && apkFile().exists()) {
            state = State.READY;
        } else {
            clearReady();
        }
    }

    public boolean supported() {
        return !BuildConfig.DEBUG;
    }

    public State state() {
        return state;
    }

    public String message() {
        return message;
    }

    public int progress() {
        return progress;
    }

    public String readyName() {
        return readyName;
    }

    public String readyNotes() {
        return readyNotes;
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private File apkFile() {
        return new File(app.getFilesDir(), "update.apk");
    }

    private void clearReady() {
        readyCode = 0;
        readyName = "";
        readyNotes = "";
        prefs.raw().edit().remove("update_ready_code").remove("update_ready_name").remove("update_ready_notes").apply();
        //noinspection ResultOfMethodCallIgnored
        apkFile().delete();
    }

    private void set(final State s, final String msg) {
        main.post(new Runnable() {
            @Override
            public void run() {
                state = s;
                message = msg == null ? "" : msg;
                for (Listener l : new ArrayList<Listener>(listeners)) l.onUpdateChanged();
            }
        });
    }

    private boolean busy() {
        return state == State.CHECKING || state == State.DOWNLOADING;
    }

    /** Tự kiểm tra (gọi định kỳ khi màn hình sáng). */
    public void maybeAutoCheck() {
        if (!supported() || !prefs.autoUpdate() || busy() || state == State.READY) return;
        if (System.currentTimeMillis() - prefs.lastUpdateCheck() < AUTO_CHECK_MS) return;
        check();
    }

    /** Kiểm tra và tải bản mới nếu có. */
    public void check() {
        if (!supported()) {
            set(State.ERROR, "Bản debug không tự cập nhật");
            return;
        }
        if (busy()) return;
        state = State.CHECKING;
        set(State.CHECKING, "Đang kiểm tra…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                runCheck();
            }
        }, "nn-update").start();
    }

    private void runCheck() {
        try {
            prefs.setLastUpdateCheck(System.currentTimeMillis());
            JSONObject info = new JSONObject(Http.getText(BuildConfig.UPDATE_URL));
            int code = info.getInt("versionCode");
            String name = info.optString("versionName", String.valueOf(code));
            int minSdk = info.optInt("minSdk", 15);
            if (code <= BuildConfig.VERSION_CODE) {
                set(State.UP_TO_DATE, "Đang dùng bản mới nhất (" + BuildConfig.VERSION_NAME + ")");
                return;
            }
            if (Build.VERSION.SDK_INT < minSdk) {
                set(State.UP_TO_DATE, "Bản " + name + " không hỗ trợ Android này");
                return;
            }
            if (state == State.READY && readyCode == code && apkFile().exists()) {
                set(State.READY, "");
                return;
            }
            final File tmp = new File(app.getFilesDir(), "update.part");
            final String label = "Đang tải bản " + name + "…";
            set(State.DOWNLOADING, label);
            Http.download(info.getString("apk"), tmp, MAX_APK, new Http.Progress() {
                @Override
                public void onProgress(long done, long total) {
                    progress = total > 0 ? (int) (done * 100 / total) : -1;
                    set(State.DOWNLOADING, label + (progress >= 0 ? " " + progress + "%" : ""));
                }
            });
            String want = info.optString("sha256", "").trim().toLowerCase();
            String got = sha256(tmp);
            if (want.length() > 0 && !want.equals(got)) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
                set(State.ERROR, "Tệp tải về bị lỗi (sai SHA-256)");
                return;
            }
            PackageInfo pi = app.getPackageManager().getPackageArchiveInfo(tmp.getAbsolutePath(), 0);
            if (pi == null || !app.getPackageName().equals(pi.packageName)) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
                set(State.ERROR, "Tệp cập nhật không đúng ứng dụng");
                return;
            }
            File apk = apkFile();
            //noinspection ResultOfMethodCallIgnored
            apk.delete();
            if (!tmp.renameTo(apk)) throw new java.io.IOException("không lưu được tệp");
            // Trình cài đặt của Android < 7 đọc tệp qua đường dẫn: cho phép đọc.
            //noinspection ResultOfMethodCallIgnored
            apk.setReadable(true, false);
            //noinspection ResultOfMethodCallIgnored
            app.getFilesDir().setExecutable(true, false);
            readyCode = code;
            readyName = name;
            readyNotes = info.optString("notes", "");
            prefs.raw().edit().putInt("update_ready_code", code).putString("update_ready_name", name)
                    .putString("update_ready_notes", readyNotes).apply();
            set(State.READY, "Bản " + name + " đã tải xong");
        } catch (Throwable t) {
            Log.w(TAG, "update check failed", t);
            set(State.ERROR, "Lỗi cập nhật: " + Http.describe(t));
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[16 * 1024];
            int r;
            while ((r = in.read(buf)) >= 0) md.update(buf, 0, r);
        } finally {
            Http.closeQuietly(in);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    /** Có bản mới đã tải xong, chờ cài. */
    public boolean ready() {
        return state == State.READY && readyCode > BuildConfig.VERSION_CODE && apkFile().exists();
    }

    /** Mở trình cài đặt cho bản đã tải. */
    public void install(final Activity activity) {
        if (!ready()) return;
        if (Build.VERSION.SDK_INT >= 26 && !app.getPackageManager().canRequestPackageInstalls()) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + app.getPackageName())));
            } catch (Throwable ignored) {
            }
            set(State.READY, "Hãy bật \"Cho phép cài ứng dụng\" cho Nhật Nguyệt rồi bấm cài lại");
            return;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        installSession();
                    } catch (Throwable t) {
                        Log.w(TAG, "session install failed", t);
                        set(State.READY, "Không mở được trình cài đặt: " + Http.describe(t));
                    }
                }
            }, "nn-install").start();
        } else {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(apkFile()), "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                activity.startActivity(i);
            } catch (Throwable t) {
                set(State.READY, "Không mở được trình cài đặt: " + Http.describe(t));
            }
        }
    }

    private void installSession() throws Exception {
        SessionInstall.install(app, apkFile());
        set(State.READY, "Đang cài bản " + readyName + "…");
    }

    void installFailed(String why) {
        set(State.READY, "Cài chưa xong: " + (why == null ? "đã huỷ" : why));
    }
}
