package vn.nhatnguyet.sys;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.R;
import vn.nhatnguyet.ui.MainActivity;

/**
 * Dịch vụ chạy nền (có thông báo) giữ cảm biến tiệm cận: đưa tay lại gần thì
 * bật màn hình và mở bảng lịch. Cần giữ CPU thức để nhận sự kiện khi màn tắt.
 */
public class WakeService extends Service implements SensorEventListener {
    private static final String TAG = "WakeService";
    private static final String CHANNEL = "wake";
    private static final int NOTIFICATION_ID = 1;

    /** MainActivity đăng ký để biết có người vẫy tay khi màn đang sáng. */
    public static volatile Runnable onWave;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SensorManager sensors;
    private Sensor proximity;
    private PowerManager power;
    private PowerManager.WakeLock cpuLock;
    private boolean lastNear;
    private long lastWakeAt;

    /** Bật/tắt dịch vụ theo cài đặt. */
    public static void sync(Context context) {
        Context app = context.getApplicationContext();
        Prefs prefs = new Prefs(app);
        Intent i = new Intent(app, WakeService.class);
        boolean want = prefs.proximityWake() && proximitySensor(app) != null;
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
        proximity = proximitySensor(this);
        if (proximity == null || sensors == null) {
            stopSelf();
            return;
        }
        cpuLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nhatnguyet:proximity");
        cpuLock.setReferenceCounted(false);
        cpuLock.acquire();
        sensors.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensors != null) sensors.unregisterListener(this);
        if (cpuLock != null && cpuLock.isHeld()) cpuLock.release();
        super.onDestroy();
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
        if (near && !lastNear) wave();
        lastNear = near;
    }

    private void wave() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastWakeAt < 1500) return;
        lastWakeAt = now;
        main.post(new Runnable() {
            @Override
            public void run() {
                Runnable r = onWave;
                if (r != null) r.run();
            }
        });
        if (!Screen.isOn(power)) Screen.wakeAndShow(this);
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
                .setContentText("Đưa tay lại gần cảm biến để bật màn hình")
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
