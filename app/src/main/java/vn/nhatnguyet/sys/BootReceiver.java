package vn.nhatnguyet.sys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.ui.MainActivity;

/** Khởi động máy hoặc vừa cập nhật app xong: bật lại dịch vụ và mở bảng lịch. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        // QUICKBOOT_POWERON (HTC, Asus) không được hệ thống bảo vệ: chỉ nhận trong 10 phút đầu sau khi bật máy.
        boolean boot = a.equals(Intent.ACTION_BOOT_COMPLETED)
                || (a.endsWith("QUICKBOOT_POWERON") && SystemClock.elapsedRealtime() < 10 * 60_000L);
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
