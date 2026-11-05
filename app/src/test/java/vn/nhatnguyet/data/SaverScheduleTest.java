package vn.nhatnguyet.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Chế độ tiết kiệm pin: các nguồn dồn về cùng mốc :00 và :30 để sóng mạng chỉ thức một lần. */
public class SaverScheduleTest {
    private static final long MIN = 60_000L;
    /** Một mốc tròn 30 phút làm gốc. */
    private static final long BASE = 1_790_000_000_000L / DataHub.SAVER_MS * DataHub.SAVER_MS;

    @Test
    public void shortIntervalsGoToNextBoundary() {
        // Tải lúc :05 với chu kỳ 1, 10, 15 phút: cùng tải lại lúc :30.
        assertEquals(BASE + 30 * MIN, DataHub.alignedNext(BASE + 5 * MIN, MIN));
        assertEquals(BASE + 30 * MIN, DataHub.alignedNext(BASE + 5 * MIN, 10 * MIN));
        assertEquals(BASE + 30 * MIN, DataHub.alignedNext(BASE + 5 * MIN, 15 * MIN));
        // Tải đúng lúc :30 thì lần sau là :00 kế tiếp, không tải lại ngay.
        assertEquals(BASE + 60 * MIN, DataHub.alignedNext(BASE + 30 * MIN, 15 * MIN));
    }

    @Test
    public void longIntervalsKeepRoughlyTheirPeriod() {
        // Chu kỳ 1 giờ tải lúc :05 thì lần sau ở mốc 1 giờ sau (:00 giờ kế), không sớm hơn 30 phút.
        assertEquals(BASE + 60 * MIN, DataHub.alignedNext(BASE + 5 * MIN, 60 * MIN));
        assertEquals(BASE + 90 * MIN, DataHub.alignedNext(BASE + 35 * MIN, 60 * MIN));
    }
}
