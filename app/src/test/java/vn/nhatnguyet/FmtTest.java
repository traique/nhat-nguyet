package vn.nhatnguyet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class FmtTest {
    @Test
    public void groupsThousandsVietnameseStyle() {
        assertEquals("0", Fmt.group(0));
        assertEquals("999", Fmt.group(999));
        assertEquals("24.220", Fmt.group(24220));
        assertEquals("1.234.567", Fmt.group(1234567));
        assertEquals("-1.000", Fmt.group(-1000));
    }

    @Test
    public void decimalsUseCommaAndRound() {
        assertEquals("1.705,69", Fmt.dec(1705.69, 2));
        assertEquals("1.705,7", Fmt.dec(1705.69, 1));
        assertEquals("2", Fmt.dec(1.5, 0));
        assertEquals("0,00", Fmt.dec(-0.001, 2));
        assertEquals("-0,5", Fmt.dec(-0.5, 1));
        assertEquals("–", Fmt.dec(Double.NaN, 1));
        assertEquals("+1,5", Fmt.signed(1.5, 1));
        assertEquals("0,0", Fmt.signed(0.01, 1));
        assertEquals("-2,0", Fmt.signed(-2, 1));
        assertEquals("140,0", Fmt.million(140_000_000));
    }

    @Test
    public void temperatureAndRange() {
        assertEquals("30°", Fmt.temp(29.6));
        assertEquals("--°", Fmt.temp(Double.NaN));
        assertEquals("24°–33°", Fmt.range(24.4, 32.6));
        assertEquals("", Fmt.range(Double.NaN, 30));
    }

    @Test
    public void clockAndDates() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.FEBRUARY, 7, 6, 5, 0);
        assertEquals("06:05", Fmt.hhmm(c));
        assertEquals("07/02", Fmt.ddmm(c));
        assertEquals("07/02/2026", Fmt.ddmmyyyy(c));
        assertEquals("09", Fmt.two(9));
        assertEquals("10", Fmt.two(10));
    }

    @Test
    public void agoPhrases() {
        long now = 1_000_000_000_000L;
        assertEquals("vừa xong", Fmt.ago(now - 30_000, now));
        assertEquals("vừa xong", Fmt.agoPhrase(now - 30_000, now));
        assertEquals("5 phút", Fmt.ago(now - 5 * 60_000, now));
        assertEquals("5 phút trước", Fmt.agoPhrase(now - 5 * 60_000, now));
        assertEquals("3 giờ trước", Fmt.agoPhrase(now - 3 * 3_600_000, now));
        assertEquals("2 ngày trước", Fmt.agoPhrase(now - 2 * 86_400_000L, now));
        assertEquals("vừa xong", Fmt.ago(now + 60_000, now)); // giờ tương lai không âm
    }

    @Test
    public void parsesVietnameseAndEnglishNumbers() {
        assertEquals(24220, Fmt.parseNumber("24,220"), 0);
        assertEquals(17667.95, Fmt.parseNumber("17,667.95"), 0.0001);
        assertEquals(24220000, Fmt.parseNumber("24.220.000"), 0);
        // Một dấu chấm kèm 3 chữ số không phân biệt được với số thập phân: giữ cách đọc kiểu Mỹ.
        assertEquals(24.22, Fmt.parseNumber("24.220 đ"), 0.0001);
        assertEquals(-3.2, Fmt.parseNumber("-3,2%"), 0.0001);
        assertTrue(Double.isNaN(Fmt.parseNumber("")));
        assertTrue(Double.isNaN(Fmt.parseNumber(null)));
        assertTrue(Double.isNaN(Fmt.parseNumber("abc")));
    }
}
