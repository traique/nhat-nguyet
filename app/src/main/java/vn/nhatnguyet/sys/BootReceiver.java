package vn.nhatnguyet.sys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.ui.MainActivity;

/** Khởi động máy hoặc vừa cập nhật app xong: bật lại dịch vụ và mở bảng lịch. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        boolean boot = a.equals(Intent.ACTION_BOOT_COMPLETED) || a.endsWith("QUICKBOOT_POWERON");
        boolean replaced = a.equals(Intent.ACTION_MY_PACKAGE_REPLACED);
        if (!boot && !replaced) return;
        WakeService.sync(context);
        Scheduler.schedule(context);
        if (new Prefs(context).autoStart()) {
            try {
                context.startActivity(new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable ignored) {
            }
        }
    }
}
