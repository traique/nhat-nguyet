package vn.nhatnguyet.update;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import vn.nhatnguyet.net.Http;

/**
 * Cài APK qua PackageInstaller (Android 7+). Tách riêng để máy Android 4.x
 * không phải nạp các lớp không tồn tại khi dùng Updater.
 */
@TargetApi(24)
final class SessionInstall {
    private SessionInstall() {}

    static void install(Context app, File apk) throws Exception {
        PackageInstaller installer = app.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(app.getPackageName());
        if (Build.VERSION.SDK_INT >= 31) {
            // Android 12+: từ lần cập nhật thứ hai có thể không cần bấm xác nhận.
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int id = installer.createSession(params);
        PackageInstaller.Session session = installer.openSession(id);
        try {
            OutputStream out = session.openWrite("base.apk", 0, apk.length());
            InputStream in = new FileInputStream(apk);
            try {
                byte[] buf = new byte[64 * 1024];
                int r;
                while ((r = in.read(buf)) >= 0) out.write(buf, 0, r);
                session.fsync(out);
            } finally {
                Http.closeQuietly(in);
                Http.closeQuietly(out);
            }
            Intent cb = new Intent(app, InstallReceiver.class).setAction(InstallReceiver.ACTION);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(app, id, cb, flags);
            session.commit(pi.getIntentSender());
        } finally {
            session.close();
        }
    }
}
