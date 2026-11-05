package vn.nhatnguyet.sys;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.R;
import vn.nhatnguyet.ui.MainActivity;

/**
 * Dịch vụ chạy nền (có thông báo) giữ cảm biến tiệm cận. Màn đang tắt: đưa tay lại gần thì
 * bật màn hình và mở bảng lịch. Màn đang sáng: che rồi bỏ tay ra thì báo bảng lịch tắt màn.
 * Cần giữ CPU thức để nhận sự kiện khi màn tắt.
 */
public class WakeService extends Service implements SensorEventListener {
    private static final String TAG = "WakeService";
    private static final String CHANNEL = "wake";
    private static final int NOTIFICATION_ID = 1;

    /** MainActivity đăng ký để biết có người vẫy tay khi màn đang sáng. */
    public static volatile Runnable onWave;
    /** MainActivity đăng ký (chỉ khi đang hiện) để tắt màn khi có người che cảm biến. */
    public static volatile Runnable onCover;
    /** MainActivity đang phủ đen chờ máy tự tắt màn (chưa có quyền quản trị): coi như màn tắt. */
    public static volatile boolean blackout;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SensorManager sensors;
    private Sensor proximity;
    private PowerManager power;
    private PowerManager.WakeLock cpuLock;
    private boolean listening;
    private static final long RECHECK_MS = 10 * 60_000L;
    private Prefs prefs;
    private final ProximityGesture gesture = new ProximityGesture();

    /** Bật/tắt dịch vụ theo cài đặt. */
    public static void sync(Context context) {
        Context app = context.getApplicationContext();
        Prefs prefs = new Prefs(app);
        Intent i = new Intent(app, WakeService.class);
        boolean want = (prefs.proximityWake() || prefs.proximitySleep()) && proximitySensor(app) != null;
        try {
            if (want) {
                if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(i);
                else app.startService(i);
            } else {
                app.stopService(i);
            }
        } catch (Throwable t) {
            // Android 12+ có thể chặn khởi động dịch vụ nền khi app không hiện.
            Log.w(TAG, "sync failed", t);
        }
    }

    public static Sensor proximitySensor(Context context) {
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) return null;
        Sensor s = null;
        if (Build.VERSION.SDK_INT >= 21) s = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, true);
        if (s == null) s = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        return s;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        goForeground();
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        power = (PowerManager) getSystemService(POWER_SERVICE);
        prefs = new Prefs(this);
        proximity = proximitySensor(this);
        if (proximity == null || sensors == null) {
            stopSelf();
            return;
        }
        cpuLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nhatnguyet:proximity");
        cpuLock.setReferenceCounted(false);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, f);
        syncState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        syncState();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        main.removeCallbacks(recheck);
        if (sensors != null) sensors.unregisterListener(this);
        listening = false;
        try {
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
        }
        if (cpuLock != null && cpuLock.isHeld()) cpuLock.release();
        super.onDestroy();
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            syncState();
        }
    };

    /** Đang giữ CPU thức lúc màn tắt: xem lại định kỳ để thôi giữ khi vào đêm. */
    private final Runnable recheck = new Runnable() {
        @Override
        public void run() {
            syncState();
        }
    };

    /** Vẫy tay bật màn chỉ có hiệu lực ngoài giờ đêm và giờ vắng nhà (lúc đó nghỉ để máy ngủ, đỡ tốn pin). */
    private boolean waveWakeNow() {
        Calendar now = Calendar.getInstance();
        return prefs.proximityWake() && !prefs.isNight(now) && !prefs.isAway(now);
    }

    /**
     * Màn đang sáng: nghe cảm biến (che để tắt, vẫy để giữ sáng), CPU đã thức sẵn.
     * Màn tắt ngoài giờ đêm, có bật vẫy tay: nghe cảm biến và giữ CPU thức để nhận sự kiện.
     * Màn tắt ban đêm hoặc giờ vắng nhà: tắt cảm biến, thả CPU ngủ; báo thức đầu giờ ngày và hết giờ vắng
     * (Scheduler) gọi lại sync().
     */
    private void syncState() {
        if (cpuLock == null || power == null || sensors == null) return;
        boolean screenOn = Screen.isOn(power);
        boolean wave = waveWakeNow();
        boolean listen = screenOn ? (prefs.proximitySleep() || prefs.proximityWake()) : wave;
        boolean hold = !screenOn && wave;
        if (listen && !listening) {
            sensors.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
            listening = true;
        } else if (!listen && listening) {
            sensors.unregisterListener(this);
            listening = false;
            gesture.reset();
        }
        try {
            if (hold && !cpuLock.isHeld()) cpuLock.acquire();
            else if (!hold && cpuLock.isHeld()) cpuLock.release();
        } catch (Throwable t) {
            Log.w(TAG, "wakelock", t);
        }
        main.removeCallbacks(recheck);
        if (hold) main.postDelayed(recheck, RECHECK_MS);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (proximity == null || event.values == null || event.values.length == 0) return;
        float threshold = Math.min(proximity.getMaximumRange(), 5f);
        boolean near = event.values[0] < threshold;
        switch (gesture.onSample(near, SystemClock.elapsedRealtime(), Screen.isOn(power) && !blackout)) {
            case ProximityGesture.TOUCH:
                notifyBoard(false);
                break;
            case ProximityGesture.WAKE:
                if (!waveWakeNow()) break; // ban đêm: không bật màn bằng vẫy tay
                notifyBoard(false); // gỡ lớp phủ đen nếu có
                if (!Screen.isOn(power)) Screen.wakeAndShow(this);
                break;
            case ProximityGesture.SLEEP:
                notifyBoard(true);
                break;
        }
    }

    private void notifyBoard(final boolean cover) {
        main.post(new Runnable() {
            @Override
            public void run() {
                Runnable r = cover ? onCover : onWave;
                if (r != null) r.run();
            }
        });
    }

    @SuppressLint("WrongConstant")
    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            createChannel(nm);
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Intent open = new Intent(this, MainActivity.class);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, piFlags);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Che cảm biến tiệm cận để bật hoặc tắt màn hình")
                .setContentIntent(pi)
                .setOngoing(true);
        Notification n;
        if (Build.VERSION.SDK_INT >= 16) {
            b.setPriority(Notification.PRIORITY_MIN);
            n = b.build();
        } else {
            //noinspection deprecation
            n = b.getNotification();
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, n);
            }
        } catch (Throwable t) {
            Log.w(TAG, "startForeground failed", t);
        }
    }

    @TargetApi(26)
    private void createChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Đánh thức màn hình", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }
}
