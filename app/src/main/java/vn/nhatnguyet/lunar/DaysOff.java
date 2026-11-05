package vn.nhatnguyet.lunar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ngày nghỉ hưởng nguyên lương theo pháp luật Việt Nam: dùng để tô đỏ lịch và biết hôm nào cả nhà nghỉ ở nhà.
 *
 * Căn cứ:
 * - Bộ luật Lao động 2019, khoản 1 Điều 112: Tết Dương lịch 1 ngày (1/1); Tết Âm lịch 5 ngày; Ngày Chiến thắng (30/4);
 *   Ngày Quốc tế lao động (1/5); Quốc khánh 2 ngày (2/9 và 1 ngày liền kề trước hoặc sau); Giỗ Tổ Hùng Vương (10/3 âm).
 *   Khoản 3 Điều 112: Thủ tướng quyết định cụ thể ngày nghỉ Tết Âm lịch và ngày liền kề Quốc khánh hằng năm.
 * - Khoản 3 Điều 111: ngày nghỉ hằng tuần trùng ngày lễ, tết ở khoản 1 Điều 112 thì nghỉ bù vào ngày làm việc kế tiếp.
 * - Nghị quyết 28/2026/QH16 (hiệu lực 1/7/2026): 24/11 hằng năm là Ngày Văn hóa Việt Nam, người lao động được nghỉ
 *   hưởng nguyên lương. Ngày này chưa nằm trong Điều 112 (dự thảo sửa Bộ luật Lao động đang trình) nên chưa tính nghỉ bù.
 *
 * Lịch tuần theo khối cơ quan nhà nước: nghỉ thứ Bảy và Chủ nhật. Doanh nghiệp tự chọn phương án nghỉ Tết (1, 2 hoặc 3 ngày
 * cuối năm) và ngày liền kề Quốc khánh, nghỉ hằng tuần cũng có thể khác, nên lịch của họ có thể lệch.
 * Năm đã có lịch chính thức dùng đúng lịch đó; năm chưa có thì chỉ đánh dấu những ngày chắc chắn nghỉ.
 */
public final class DaysOff {
    /** Lịch nghỉ chính thức của khối nhà nước đã công bố. Thêm năm mới khi Bộ Nội vụ thông báo (thường tháng 10 năm trước). */
    static final class Official {
        final int year;
        /** Số ngày cuối năm âm lịch trong 5 ngày nghỉ Tết (1, 2 hoặc 3); còn lại là mùng 1 trở đi. */
        final int tetDaysBefore;
        /** Ngày liền kề Quốc khánh trong tháng 9: 1 (trước) hoặc 3 (sau). */
        final int nationalDayExtra;
        /** Ngày làm việc được nghỉ do hoán đổi, {ngày, tháng}. */
        final int[][] swapOff;
        /** Ngày nghỉ hằng tuần phải đi làm bù do hoán đổi, {ngày, tháng}. */
        final int[][] swapWork;

        Official(int year, int tetDaysBefore, int nationalDayExtra, int[][] swapOff, int[][] swapWork) {
            this.year = year;
            this.tetDaysBefore = tetDaysBefore;
            this.nationalDayExtra = nationalDayExtra;
            this.swapOff = swapOff;
            this.swapWork = swapWork;
        }
    }

    static final Official[] OFFICIAL = {
            // Tết Bính Ngọ: 29 tháng Chạp đến mùng 4 (16–20/2), Thủ tướng đồng ý đề xuất của Bộ Nội vụ 10/2025.
            // Quốc khánh: 1/9 và 2/9, nghỉ 31/8 làm bù thứ Bảy 22/8 (Thông báo 9441/TB-BNV).
            // Ngày Văn hóa Việt Nam 24/11 nghỉ 1 ngày, không hoán đổi (Công văn 10065/VPCP-KGVX ngày 2/10/2026).
            new Official(2026, 1, 1, new int[][]{{31, 8}}, new int[][]{{22, 8}}),
            // Tết Đinh Mùi: 2 ngày cuối năm và mùng 1–3 (4–8/2), nghỉ bù 9–10/2; Quốc khánh 2/9 và 3/9
            // (Thông báo 11483/TB-BNV ngày 9/10/2026).
            new Official(2027, 2, 3, new int[0][], new int[0][]),
    };

    private static final class Year {
        final Map<Integer, String> off;
        final Set<Integer> work;
        final Set<Integer> likely;

        Year(Map<Integer, String> off, Set<Integer> work, Set<Integer> likely) {
            this.off = off;
            this.work = work;
            this.likely = likely;
        }
    }

    private static final Map<Integer, Year> cache = new HashMap<Integer, Year>();

    private DaysOff() {}

    /**
     * Tên ngày nghỉ (ngày lễ, ngày nghỉ Tết, nghỉ bù, nghỉ hoán đổi) nếu ngày d/m/y là ngày nghỉ hưởng lương của khối nhà
     * nước; null nếu là ngày thường hoặc ngày nghỉ hằng tuần.
     */
    public static String name(int d, int m, int y) {
        return year(y).off.get(Lunar.jdFromDate(d, m, y));
    }

    /** Thứ Bảy hoặc Chủ nhật phải đi làm bù do hoán đổi ngày nghỉ. */
    public static boolean isMakeUpWorkday(int d, int m, int y) {
        return year(y).work.contains(Lunar.jdFromDate(d, m, y));
    }

    /**
     * Có thể là ngày nghỉ: năm đã có lịch chính thức thì đúng như name(); năm chưa có thì gồm mọi phương án
     * luật cho phép (Tết nghỉ 1, 2 hoặc 3 ngày cuối năm; ngày liền kề Quốc khánh 1/9 hoặc 3/9) và nghỉ bù kèm theo.
     */
    public static boolean likelyOff(int d, int m, int y) {
        return year(y).likely.contains(Lunar.jdFromDate(d, m, y));
    }

    /**
     * Ngày tô đỏ trên lịch (ngoài Chủ nhật): ngày lễ được nghỉ theo luật, mùng 1–3 Tết, ngày nghỉ Tết, nghỉ bù, nghỉ hoán đổi.
     * Ngày kỷ niệm không được nghỉ (8/3, 20/10, 20/11, Rằm, Trung thu, Giáng sinh…) chỉ ghi tên, không tô đỏ.
     */
    public static boolean isRed(int d, int m, int y, LunarDate ld) {
        Lunar.Holiday h = Lunar.holidayOn(d, m, ld);
        return (h != null && h.major) || name(d, m, y) != null;
    }

    public static boolean hasOfficial(int y) {
        return official(y) != null;
    }

    /** Thứ Bảy hoặc Chủ nhật (số ngày Julius chia 7 dư 0 là thứ Hai). */
    static boolean weekend(int jd) {
        int w = jd % 7;
        return w == 5 || w == 6;
    }

    private static Official official(int y) {
        for (Official o : OFFICIAL) if (o.year == y) return o;
        return null;
    }

    private static synchronized Year year(int y) {
        Year c = cache.get(y);
        if (c == null) {
            c = build(y);
            cache.put(y, c);
        }
        return c;
    }

    private static Year build(int y) {
        Official o = official(y);
        if (o != null) {
            TreeMap<Integer, String> off = withCompensation(legal(y, o.tetDaysBefore, o.nationalDayExtra), new HashSet<Integer>());
            for (int[] s : o.swapOff) off.put(Lunar.jdFromDate(s[0], s[1], y), "Nghỉ hoán đổi");
            Set<Integer> work = new HashSet<Integer>();
            for (int[] s : o.swapWork) work.add(Lunar.jdFromDate(s[0], s[1], y));
            return new Year(off, work, new HashSet<Integer>(off.keySet()));
        }
        // Chưa có lịch chính thức: chắc chắn nghỉ các ngày cố định (kèm nghỉ bù) và 3 ngày Tết có trong mọi phương án
        // (ngày cuối năm, mùng 1, mùng 2). Không tính nghỉ bù cho Tết và Quốc khánh vì còn tuỳ phương án.
        TreeMap<Integer, String> fixed = legal(y, 0, 0);
        int m1 = tetDay(y);
        Set<Integer> noComp = new HashSet<Integer>();
        for (int i = -1; i <= 1; i++) {
            fixed.put(m1 + i, TET);
            noComp.add(m1 + i);
        }
        noComp.add(Lunar.jdFromDate(2, 9, y));
        TreeMap<Integer, String> off = withCompensation(fixed, noComp);
        Set<Integer> likely = new HashSet<Integer>(off.keySet());
        for (int before = 1; before <= 3; before++) {
            for (int extra = 1; extra <= 3; extra += 2) {
                likely.addAll(withCompensation(legal(y, before, extra), new HashSet<Integer>()).keySet());
            }
        }
        return new Year(off, new HashSet<Integer>(), likely);
    }

    private static final String TET = "Nghỉ Tết Nguyên đán";

    /** Ngày lễ theo luật (Điều 112 và Nghị quyết 28/2026/QH16); tetDaysBefore/nationalDayExtra = 0 là chưa biết. */
    private static TreeMap<Integer, String> legal(int y, int tetDaysBefore, int nationalDayExtra) {
        TreeMap<Integer, String> m = new TreeMap<Integer, String>();
        m.put(Lunar.jdFromDate(1, 1, y), "Tết Dương lịch");
        if (tetDaysBefore > 0) {
            int m1 = tetDay(y);
            for (int i = -tetDaysBefore; i < 5 - tetDaysBefore; i++) m.put(m1 + i, TET);
        }
        int[] g = Lunar.lunarToSolar(10, 3, y, false);
        if (g != null) m.put(Lunar.jdFromDate(g[0], g[1], g[2]), "Giỗ Tổ Hùng Vương");
        m.put(Lunar.jdFromDate(30, 4, y), "Giải phóng miền Nam");
        m.put(Lunar.jdFromDate(1, 5, y), "Quốc tế Lao động");
        m.put(Lunar.jdFromDate(2, 9, y), "Quốc khánh");
        if (nationalDayExtra > 0) m.put(Lunar.jdFromDate(nationalDayExtra, 9, y), "Quốc khánh");
        if (y >= 2026) m.put(Lunar.jdFromDate(24, 11, y), CULTURE_DAY);
        return m;
    }

    private static final String CULTURE_DAY = "Ngày Văn hóa Việt Nam";

    /** Mùng 1 Tết năm âm lịch y (rơi vào tháng 1 hoặc 2 dương lịch năm y), dạng số ngày Julius. */
    private static int tetDay(int y) {
        int[] t = Lunar.lunarToSolar(1, 1, y, false);
        return Lunar.jdFromDate(t[0], t[1], t[2]);
    }

    /**
     * Thêm ngày nghỉ bù (khoản 3 Điều 111): ngày lễ rơi vào thứ Bảy, Chủ nhật thì nghỉ vào ngày làm việc kế tiếp
     * chưa phải ngày nghỉ. Duyệt theo thứ tự ngày nên hai ngày lễ liền nhau cuối tuần được bù thứ Hai rồi thứ Ba.
     */
    private static TreeMap<Integer, String> withCompensation(TreeMap<Integer, String> legal, Set<Integer> noComp) {
        TreeMap<Integer, String> out = new TreeMap<Integer, String>(legal);
        for (Map.Entry<Integer, String> e : legal.entrySet()) {
            int jd = e.getKey();
            if (!weekend(jd) || noComp.contains(jd) || CULTURE_DAY.equals(e.getValue())) continue;
            int k = jd + 1;
            while (weekend(k) || out.containsKey(k)) k++;
            String what = e.getValue().startsWith("Nghỉ ") ? e.getValue().substring(5) : e.getValue();
            out.put(k, "Nghỉ bù " + what);
        }
        return out;
    }
}
