package vn.nhatnguyet.lunar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Đối chiếu âm lịch với dữ liệu độc lập, 1900–2100: bộ ca kiểm của Nhat Nguyet (nhatnguyet.org, CC BY 4.0)
 * và giờ bắt đầu tiết khí nội suy từ lịch thiên văn IMCCE. Nguồn, giấy phép và cách rút dữ liệu:
 * app/src/test/resources/doi-chieu/README.md.
 */
public class LunarReferenceTest {
    /**
     * Mốc tiết khí app đặt lệch một ngày so với IMCCE (ngày theo giờ Việt Nam). Cả tám đều cách nửa đêm
     * dưới 5 phút, nằm ngoài sai số của công thức Mặt Trời rút gọn. Danh sách phải khớp đúng: sửa công thức
     * mà có mốc mới lệch, hoặc mốc cũ hết lệch, đều phải cập nhật ở đây.
     */
    static final List<String> KNOWN_TERM_MISSES = Arrays.asList(
            "1908-01-07 Tiểu hàn", "1946-04-21 Cốc vũ", "1948-03-20 Xuân phân", "1981-03-21 Xuân phân",
            "1989-06-06 Mang chủng", "2003-10-09 Hàn lộ", "2082-12-07 Đại tuyết", "2098-10-07 Hàn lộ");

    private static List<String[]> rows(String name) throws Exception {
        InputStream in = LunarReferenceTest.class.getClassLoader().getResourceAsStream("doi-chieu/" + name);
        if (in == null) throw new IllegalStateException("thiếu tệp doi-chieu/" + name);
        List<String[]> out = new ArrayList<String[]>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                out.add(line.split("\t"));
            }
        } finally {
            r.close();
        }
        return out;
    }

    private static int i(String s) {
        return Integer.parseInt(s);
    }

    private static void report(String what, int total, List<String> fails) {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < Math.min(5, fails.size()); k++) b.append("\n  ").append(fails.get(k));
        assertEquals(what + ": " + fails.size() + "/" + total + " ca lệch" + b, 0, fails.size());
    }

    @Test
    public void solarToLunarMatchesReference() throws Exception {
        List<String[]> cases = rows("duong-sang-am.tsv");
        assertEquals(5975, cases.size());
        List<String> fails = new ArrayList<String>();
        for (String[] p : cases) {
            LunarDate ld = Lunar.solarToLunar(i(p[0]), i(p[1]), i(p[2]));
            String got = ld.day + "/" + ld.month + "/" + ld.year + (ld.leap ? "n" : "")
                    + " | " + Lunar.dayCanChi(ld.jd) + " | " + Lunar.monthCanChi(ld.month, ld.year)
                    + " | " + Lunar.yearCanChi(ld.year) + " | " + Lunar.dayMansion(ld.jd);
            String want = p[3] + "/" + p[4] + "/" + p[5] + ("1".equals(p[6]) ? "n" : "")
                    + " | " + p[7] + " | " + p[8] + " | " + p[9] + " | " + p[10];
            if (!got.equals(want)) fails.add(p[0] + "/" + p[1] + "/" + p[2] + ": app " + got + ", ca kiểm " + want);
        }
        report("Dương sang âm", cases.size(), fails);
    }

    @Test
    public void lunarToSolarMatchesReference() throws Exception {
        List<String[]> cases = rows("am-sang-duong.tsv");
        assertEquals(2784, cases.size());
        List<String> fails = new ArrayList<String>();
        int strict = 0;
        for (String[] p : cases) {
            boolean leap = "1".equals(p[3]);
            int[] s = Lunar.lunarToSolar(i(p[0]), i(p[1]), i(p[2]), leap);
            String in = p[0] + "/" + p[1] + (leap ? "n" : "") + "/" + p[2];
            if ("0".equals(p[4])) {
                if (s != null) fails.add(in + ": ca kiểm báo không tồn tại, app ra " + s[0] + "/" + s[1] + "/" + s[2]);
                continue;
            }
            int[] want = {i(p[5]), i(p[6]), i(p[7])};
            if (s == null) {
                // App chặt hơn: hỏi tháng nhuận ở năm không nhuận tháng ấy thì trả null. Bộ ca kiểm (như mã gốc
                // Hồ Ngọc Đức) bỏ qua cờ nhuận: đáp án của nó phải trùng đáp án cho tháng thường cùng số.
                if (leap && Arrays.equals(want, Lunar.lunarToSolar(i(p[0]), i(p[1]), i(p[2]), false))) strict++;
                else fails.add(in + ": app trả null, ca kiểm ra " + want[0] + "/" + want[1] + "/" + want[2]);
                continue;
            }
            if (!Arrays.equals(want, s)) {
                fails.add(in + ": app " + s[0] + "/" + s[1] + "/" + s[2] + ", ca kiểm " + want[0] + "/" + want[1] + "/" + want[2]);
            }
        }
        report("Âm sang dương", cases.size(), fails);
        assertEquals("số ca tháng nhuận không có thật mà app từ chối", 864, strict);
    }

    @Test
    public void solarTermStartDaysMatchImcce() throws Exception {
        List<String[]> marks = rows("imcce-tiet-khi.tsv");
        assertEquals(4824, marks.size());
        TreeSet<String> misses = new TreeSet<String>();
        for (String[] p : marks) {
            String[] d = p[0].split("-");
            String[] t = p[1].split(":");
            // Giờ quốc tế sang giờ Việt Nam (UTC+7): từ 17 giờ UT là sang ngày hôm sau.
            int jd = Lunar.jdFromDate(i(d[2]), i(d[1]), i(d[0])) + (i(t[0]) + 7 >= 24 ? 1 : 0);
            int index = (i(p[2]) / 15) % 24;
            if (!Lunar.solarTermStarts(jd) || Lunar.solarTermIndex(jd) != index) {
                int[] s = Lunar.jdToDate(jd);
                misses.add(s[2] + "-" + (s[1] < 10 ? "0" : "") + s[1] + "-" + (s[0] < 10 ? "0" : "") + s[0]
                        + " " + Lunar.TIET_KHI[index]);
            }
        }
        assertEquals(new TreeSet<String>(KNOWN_TERM_MISSES), misses);
    }

    @Test
    public void napAmFollowsSexagenaryCycle() {
        // 1/1/2000 là ngày Mậu Ngọ (vị trí 54), nên 7/1/2000 là Giáp Tý.
        assertEquals("Mậu Ngọ", Lunar.dayCanChi(Lunar.jdFromDate(1, 1, 2000)));
        int jd = Lunar.jdFromDate(7, 1, 2000);
        assertEquals("Giáp Tý", Lunar.dayCanChi(jd));
        assertEquals(0, Lunar.dayCycle(jd));
        for (int k = 0; k < 60; k++) {
            int c = Lunar.dayCycle(jd + k);
            assertEquals(k, c);
            assertEquals(Lunar.CAN[c % 10] + " " + Lunar.CHI[c % 12], Lunar.dayCanChi(jd + k));
        }
        assertEquals("Hải Trung Kim", Lunar.dayNapAm(jd));
        assertEquals("Hải Trung Kim", Lunar.dayNapAm(jd + 1));
        assertEquals("Lư Trung Hỏa", Lunar.dayNapAm(jd + 2));
        assertEquals("Thiên Hà Thủy", Lunar.dayNapAm(jd + 42)); // Bính Ngọ
        assertEquals("Đại Hải Thủy", Lunar.dayNapAm(jd + 59)); // Quý Hợi
        assertEquals("Hải Trung Kim", Lunar.dayNapAm(jd + 60));
        // Hành nạp âm (chữ cuối) đi theo chuỗi 15 mục rồi lặp lại: Giáp Ngọ cùng hành với Giáp Tý, v.v.
        StringBuilder elements = new StringBuilder();
        for (String n : Lunar.NAP_AM) {
            String e = n.substring(n.lastIndexOf(' ') + 1);
            elements.append("Kim".equals(e) ? 'K' : "Hỏa".equals(e) ? 'H' : "Mộc".equals(e) ? 'M'
                    : "Thổ".equals(e) ? 'T' : "Thủy".equals(e) ? 'N' : '?');
        }
        assertEquals("KHMTKHNTKMNTHMN" + "KHMTKHNTKMNTHMN", elements.toString());
    }

    @Test
    public void mansionsLockToWeekdays() {
        assertEquals("Tâm", Lunar.dayMansion(Lunar.jdFromDate(21, 9, 2026)));
        assertEquals("Vĩ", Lunar.dayMansion(Lunar.jdFromDate(22, 9, 2026)));
        assertEquals("Phòng", Lunar.dayMansion(Lunar.jdFromDate(20, 9, 2026)));
        // Vòng 28 sao lặp đúng bốn tuần: sao Tâm luôn rơi vào thứ Hai.
        for (int w = -520; w <= 520; w++) {
            int jd = Lunar.jdFromDate(21, 9, 2026) + 28 * w;
            assertEquals("Tâm", Lunar.dayMansion(jd));
            assertTrue("thứ Hai", jd % 7 == 0);
        }
    }
}
