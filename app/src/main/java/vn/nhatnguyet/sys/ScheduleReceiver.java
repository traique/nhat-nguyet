package vn.nhatnguyet.sys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import vn.nhatnguyet.Prefs;

/** Báo thức đầu giờ ngày / hết giờ vắng nhà: bật lại cảm biến vẫy tay, bật màn nếu chọn, hẹn lần sau. */
public class ScheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Scheduler.ACTION_MORNING.equals(intent.getAction())) {
            WakeService.sync(context);
            if (new Prefs(context).wakeMorning()) Screen.wakeAndShow(context);
        }
        Scheduler.schedule(context);
    }
}
