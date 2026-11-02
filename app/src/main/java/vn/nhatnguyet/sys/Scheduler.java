package vn.nhatnguyet.sys;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import vn.nhatnguyet.Prefs;

/** Hẹn giờ bật màn hình lúc bắt đầu "giờ ngày" (vd. 6:00 sáng). */
public final class Scheduler {
    public static final String ACTION_MORNING = "vn.nhatnguyet.MORNING";

    private Scheduler() {}

    public static void schedule(Context context) {
        Context app = context.getApplicationContext();
        Prefs prefs = new Prefs(app);
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(app);
        am.cancel(pi);
        if (!prefs.wakeMorning() || prefs.alwaysOn() || prefs.dayStart() == prefs.dayEnd()) return;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, prefs.dayStart());
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 5);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_MONTH, 1);
        long at = c.getTimeInMillis();
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static PendingIntent pending(Context app) {
        Intent i = new Intent(app, ScheduleReceiver.class).setAction(ACTION_MORNING);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(app, 1, i, flags);
    }
}
