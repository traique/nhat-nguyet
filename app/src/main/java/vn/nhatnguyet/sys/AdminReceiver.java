package vn.nhatnguyet.sys;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Quyền "Quản trị thiết bị" chỉ dùng để tắt màn hình (lockNow): giờ đêm hoặc khi che cảm biến. */
public class AdminReceiver extends DeviceAdminReceiver {

    public static ComponentName component(Context c) {
        return new ComponentName(c.getApplicationContext(), AdminReceiver.class);
    }

    public static boolean isActive(Context c) {
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(component(c));
    }

    public static Intent requestIntent(Context c) {
        return new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(c))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Chỉ dùng để tắt màn hình vào giờ đêm hoặc khi che cảm biến. Đưa tay qua cảm biến hoặc bấm nút nguồn để bật lại.");
    }

    public static void remove(Context c) {
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) dpm.removeActiveAdmin(component(c));
    }

    /** @return true nếu đã tắt màn hình được. */
    public static boolean lockNow(Context c) {
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null || !dpm.isAdminActive(component(c))) return false;
        try {
            dpm.lockNow();
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
