package vn.nhatnguyet.ui;

import org.junit.Test;

import java.io.PrintWriter;

import vn.nhatnguyet.lunar.DaysOff;
import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.LunarDate;

/**
 * Xuất âm lịch, ngày lễ, ngày nghỉ và dòng sự kiện 2020–2040 ra tệp LICH_DUMP để docs/lich.test.mjs đối chiếu
 * với bản JavaScript của trang giới thiệu (CI chạy cả hai). Không đặt biến môi trường thì bỏ qua.
 */
public class LichDumpTest {
    @Test
    public void dump() throws Exception {
        String path = System.getenv("LICH_DUMP");
        if (path == null) return;
        PrintWriter w = new PrintWriter(path, "UTF-8");
        int end = Lunar.jdFromDate(31, 12, 2040);
        for (int jd = Lunar.jdFromDate(1, 1, 2020); jd <= end; jd++) {
            int[] s = Lunar.jdToDate(jd);
            LunarDate ld = Lunar.solarToLunar(s[0], s[1], s[2]);
            Lunar.Holiday h = Lunar.holidayOn(s[0], s[1], ld);
            String off = DaysOff.name(s[0], s[1], s[2]);
            MainActivity.Note n = MainActivity.dayNote(s[0], s[1], s[2], ld, 45, 60, false);
            StringBuilder gio = new StringBuilder();
            for (String g : Lunar.gioHoangDao(jd)) gio.append(gio.length() > 0 ? "," : "").append(g.substring(0, g.indexOf(' ')));
            w.println(s[0] + "/" + s[1] + "/" + s[2] + "|" + ld.day + "/" + ld.month + "/" + ld.year + (ld.leap ? "L" : "")
                    + "|" + Lunar.dayCanChi(jd) + "|" + Lunar.yearCanChi(ld.year) + "|" + (h == null ? "" : h.name)
                    + "|" + (off == null ? "" : off) + "|" + (DaysOff.isMakeUpWorkday(s[0], s[1], s[2]) ? 1 : 0)
                    + "|" + (DaysOff.isRed(s[0], s[1], s[2], ld) || jd % 7 == 6 ? 1 : 0)
                    + "|" + Lunar.isHoangDao(jd, ld.month) + "|" + gio + "|" + (n == null ? "" : join(n.text)));
        }
        w.close();
    }

    private static String join(String[] parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) b.append(b.length() > 0 ? "~" : "").append(p);
        return b.toString();
    }
}
