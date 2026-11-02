package vn.nhatnguyet.sys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Báo thức buổi sáng: bật màn hình, mở bảng lịch, hẹn lại ngày mai. */
public class ScheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Scheduler.ACTION_MORNING.equals(intent.getAction())) {
            Screen.wakeAndShow(context);
        }
        Scheduler.schedule(context);
    }
}
