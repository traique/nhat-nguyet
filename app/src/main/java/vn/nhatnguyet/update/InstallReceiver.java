package vn.nhatnguyet.update;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Nhận kết quả phiên cài đặt (Android 7+): mở màn xác nhận nếu hệ thống yêu cầu. */
@TargetApi(21)
public class InstallReceiver extends BroadcastReceiver {
    public static final String ACTION = "vn.nhatnguyet.INSTALL_STATUS";

    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirm);
                } catch (Throwable t) {
                    Updater.get(context).installFailed("không mở được màn xác nhận");
                }
            }
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            Updater.get(context).installFailed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        }
    }
}
