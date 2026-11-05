package vn.nhatnguyet.sys;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import vn.nhatnguyet.Prefs;

/**
 * Hẹn giờ lúc bắt đầu "giờ ngày" (vd. 6:00 sáng) và lúc hết giờ vắng nhà: bật màn hình (nếu chọn tự bật)
 * và bật lại cảm biến vẫy tay vốn nghỉ ban đêm. Trên Android 10+ việc mở bảng lịch cần quyền
 * "hiện trên ứng dụng khác".
 */
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
        boolean dayWake = (prefs.wakeMorning() || prefs.proximityWake()) && !prefs.alwaysOn()
                && prefs.dayStart() != prefs.dayEnd();
        boolean awayWake = prefs.awayEnabled() && (prefs.wakeMorning() || prefs.proximityWake());
        long at = nextWake(Calendar.getInstance(), dayWake, prefs.dayStart(), awayWake,
                prefs.awayStart(), prefs.awayEnd(), prefs.awayWeekdaysOnly());
        if (at <= 0) return;
        // Bật màn không cần đúng tới giây: dùng báo thức thường (không cần quyền SCHEDULE_EXACT_ALARM/
        // USE_EXACT_ALARM). Máy cắm sạc không vào Doze nên lệch tối đa vài phút.
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    /**
     * Mốc báo thức kế tiếp (ms) sau now: giờ bắt đầu ngày (dayWake) hoặc giờ hết vắng nhà (awayWake, chỉ những
     * ngày áp dụng giờ vắng); 0 nếu không cần báo thức. Báo thức đặt lệch 5 giây sau mốc giờ.
     */
    public static long nextWake(Calendar now, boolean dayWake, int dayStart, boolean awayWake,
                                int awayStart, int awayEnd, boolean weekdaysOnly) {
        if (!dayWake && !(awayWake && awayStart != awayEnd)) return 0;
        long nowMs = now.getTimeInMillis();
        long best = 0;
        for (int i = 0; i <= 8; i++) {
            Calendar day = (Calendar) now.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            if (dayWake) best = earliest(best, at(day, dayStart % 24), nowMs);
            if (awayWake && awayStart != awayEnd) {
                Calendar end = at(day, awayEnd % 24);
                // Khoảng vắng qua nửa đêm (vd. 22h–6h) kết thúc vào hôm sau: xét ngày bắt đầu khoảng vắng.
                Calendar startDay = (Calendar) end.clone();
                if (awayEnd <= awayStart) startDay.add(Calendar.DAY_OF_MONTH, -1);
                if (Prefs.isAwayDay(startDay, weekdaysOnly)) best = earliest(best, end, nowMs);
            }
            if (best > 0 && best - nowMs < 86_400_000L * i) break;
        }
        return best;
    }

    private static Calendar at(Calendar day, int hour) {
        Calendar c = (Calendar) day.clone();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 5);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static long earliest(long best, Calendar c, long nowMs) {
        long t = c.getTimeInMillis();
        if (t <= nowMs + 1000) return best;
        return best == 0 || t < best ? t : best;
    }

    private static PendingIntent pending(Context app) {
        Intent i = new Intent(app, ScheduleReceiver.class).setAction(ACTION_MORNING);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(app, 1, i, flags);
    }
}
