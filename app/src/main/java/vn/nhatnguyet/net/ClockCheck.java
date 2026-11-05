package vn.nhatnguyet.net;

/**
 * Ước lượng giờ máy lệch bao nhiêu so với giờ chuẩn, dựa vào tiêu đề Date của các máy chủ dữ liệu.
 * Lấy độ lệch nhỏ nhất trong vài lần gần đây để một phản hồi cũ trong bộ đệm không gây báo nhầm.
 */
public final class ClockCheck {
    private static final long[] recent = new long[5];
    private static int count;
    private static int next;

    private ClockCheck() {}

    /** @param localMs giờ máy lúc nhận phản hồi (giữa lúc gửi và lúc nhận). */
    public static synchronized void sample(long serverMs, long localMs) {
        recent[next] = serverMs - localMs;
        next = (next + 1) % recent.length;
        if (count < recent.length) count++;
    }

    /** Độ lệch (ms): dương là giờ máy chậm hơn giờ chuẩn; 0 khi chưa đủ 3 lần đo. */
    public static synchronized long offsetMs() {
        if (count < 3) return 0;
        long best = recent[0];
        for (int i = 1; i < count; i++) if (Math.abs(recent[i]) < Math.abs(best)) best = recent[i];
        return best;
    }

    /** Gọi khi người dùng chỉnh giờ máy: bỏ các mẫu đo cũ. */
    public static synchronized void reset() {
        count = 0;
        next = 0;
    }
}
