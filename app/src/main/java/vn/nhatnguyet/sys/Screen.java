package vn.nhatnguyet.sys;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import vn.nhatnguyet.ui.MainActivity;

/** Bật màn hình và đưa bảng lịch lên trước. */
public final class Screen {
    private Screen() {}

    @SuppressWarnings("deprecation")
    public static boolean isOn(PowerManager pm) {
        if (pm == null) return true;
        if (Build.VERSION.SDK_INT >= 20) return pm.isInteractive();
        return pm.isScreenOn();
    }

    @SuppressWarnings("deprecation")
    public static void wakeAndShow(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            try {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "nhatnguyet:wake");
                wl.acquire(10_000L);
            } catch (Throwable ignored) {
            }
        }
        Intent i = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_WAKE, true);
        try {
            context.startActivity(i);
        } catch (Throwable ignored) {
            // Android 10+ có thể chặn mở Activity từ nền nếu chưa cấp quyền "hiện trên ứng dụng khác".
        }
    }
}
