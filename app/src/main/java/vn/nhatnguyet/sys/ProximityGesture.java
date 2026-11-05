package vn.nhatnguyet.sys;

/**
 * Nhận biết thao tác với cảm biến tiệm cận. Màn đang tắt: vừa che là đánh thức.
 * Màn đang sáng: che rồi bỏ tay ra trong 1,5 giây là tắt; che lâu (vd. vật để sát) thì bỏ qua.
 * Lần che vừa đánh thức màn hình, và 2 giây sau đó, không được tính là chạm để tắt.
 */
final class ProximityGesture {
    static final int NONE = 0;
    /** Có người che khi màn đang sáng (giữ màn sáng thêm). */
    static final int TOUCH = 1;
    static final int WAKE = 2;
    static final int SLEEP = 3;

    private static final long TAP_MS = 1500;
    private static final long WAKE_DEBOUNCE_MS = 1500;
    private static final long AFTER_WAKE_MS = 2000;

    private boolean lastNear;
    private boolean coverWhileAwake;
    private long nearAt;
    private long wokeAt = -60_000L;

    /** Cảm biến vừa tắt rồi bật lại: quên trạng thái che dở dang. */
    void reset() {
        lastNear = false;
        coverWhileAwake = false;
    }

    int onSample(boolean near, long now, boolean awake) {
        int action = NONE;
        if (near && !lastNear) {
            nearAt = now;
            coverWhileAwake = awake && now - wokeAt > AFTER_WAKE_MS;
            if (awake) {
                action = TOUCH;
            } else if (now - wokeAt > WAKE_DEBOUNCE_MS) {
                wokeAt = now;
                action = WAKE;
            }
        } else if (!near && lastNear && coverWhileAwake && now - nearAt < TAP_MS) {
            coverWhileAwake = false;
            action = SLEEP;
        }
        lastNear = near;
        return action;
    }
}
