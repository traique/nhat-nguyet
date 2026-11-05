package vn.nhatnguyet.lunar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Âm lịch Việt Nam (múi giờ +7) theo thuật toán của Hồ Ngọc Đức
 * (https://www.informatik.uni-leipzig.de/~duc/amlich/, mã JavaScript gốc:
 * "Copyright 2004 Ho Ngoc Duc. All Rights Reserved."), dựa trên công thức thiên văn của Jean Meeus;
 * cài đặt lại bằng Java, kèm can chi, tiết khí, ngày/giờ hoàng đạo và ngày lễ.
 * Thuần Java, không phụ thuộc java.time nên chạy được trên Android 4.0.3.
 * Đã đối chiếu với bảng chuẩn của tác giả cho 1900–2100.
 */
public final class Lunar {
    public static final double TZ = 7.0;

    public static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    public static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    public static final String[] TIET_KHI = {
            "Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng",
            "Hạ chí", "Tiểu thử", "Đại thử", "Lập thu", "Xử thử", "Bạch lộ",
            "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông", "Tiểu tuyết", "Đại tuyết",
            "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập"};
    public static final String[] STARS = {
            "Thanh Long", "Minh Đường", "Thiên Hình", "Chu Tước", "Kim Quỹ", "Kim Đường",
            "Bạch Hổ", "Ngọc Đường", "Thiên Lao", "Huyền Vũ", "Tư Mệnh", "Câu Trận"};
    private static final boolean[] GOOD_STAR = {true, true, false, false, true, true, false, true, false, false, true, false};
    private static final String[] GIO_HD = {
            "110100101100", "001101001011", "110011010010",
            "101100110100", "001011001101", "010010110011"};
    public static final String[] HOUR_RANGE = {
            "23-1h", "1-3h", "3-5h", "5-7h", "7-9h", "9-11h",
            "11-13h", "13-15h", "15-17h", "17-19h", "19-21h", "21-23h"};
    public static final String[] MONTH_NAME = {
            "", "Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy", "Tám", "Chín", "Mười", "Một", "Chạp"};

    /** Ngày lễ: lunar=true thì ngày/tháng là âm lịch; personal=true là ngày riêng (giỗ, sinh nhật). */
    public static final class Holiday {
        public final String name;
        public final boolean lunar;
        public final int day;
        public final int month;
        public final boolean major;
        public final boolean personal;

        Holiday(String name, boolean lunar, int day, int month, boolean major) {
            this(name, lunar, day, month, major, false);
        }

        private Holiday(String name, boolean lunar, int day, int month, boolean major, boolean personal) {
            this.name = name;
            this.lunar = lunar;
            this.day = day;
            this.month = month;
            this.major = major;
            this.personal = personal;
        }

        public static Holiday personal(String name, boolean lunar, int day, int month) {
            return new Holiday(name, lunar, day, month, true, true);
        }

        /**
         * Ngày âm không tính tháng nhuận; ngày 30 âm rơi vào tháng thiếu thì tính là ngày 29.
         * Ngày dương 29/2 ở năm không nhuận tính là 28/2.
         */
        public boolean on(int dd, int mm, LunarDate ld) {
            if (!lunar) {
                if (day == dd && month == mm) return true;
                return day == 29 && month == 2 && dd == 28 && mm == 2 && !isLeapYear(jdToDate(ld.jd)[2]);
            }
            if (ld.leap || ld.month != month) return false;
            return ld.day == day || (day == 30 && ld.day == 29 && lunarMonthLength(ld) == 29);
        }
    }

    private static volatile List<Holiday> personalDays = Collections.emptyList();

    public static final Holiday[] HOLIDAYS = {
            new Holiday("Tết Dương lịch", false, 1, 1, true),
            new Holiday("Valentine", false, 14, 2, false),
            new Holiday("Quốc tế Phụ nữ", false, 8, 3, false),
            new Holiday("Giải phóng miền Nam", false, 30, 4, true),
            new Holiday("Quốc tế Lao động", false, 1, 5, true),
            new Holiday("Quốc tế Thiếu nhi", false, 1, 6, false),
            new Holiday("Quốc khánh", false, 2, 9, true),
            new Holiday("Phụ nữ Việt Nam", false, 20, 10, false),
            new Holiday("Nhà giáo Việt Nam", false, 20, 11, false),
            new Holiday("Giáng sinh", false, 25, 12, false),
            new Holiday("Tết Nguyên đán", true, 1, 1, true),
            new Holiday("Mùng 2 Tết", true, 2, 1, true),
            new Holiday("Mùng 3 Tết", true, 3, 1, true),
            new Holiday("Rằm tháng Giêng", true, 15, 1, false),
            new Holiday("Tết Hàn thực", true, 3, 3, false),
            new Holiday("Giỗ Tổ Hùng Vương", true, 10, 3, true),
            new Holiday("Lễ Phật đản", true, 15, 4, false),
            new Holiday("Tết Đoan ngọ", true, 5, 5, false),
            new Holiday("Lễ Vu Lan", true, 15, 7, false),
            new Holiday("Tết Trung thu", true, 15, 8, false),
            new Holiday("Ông Công ông Táo", true, 23, 12, false),
            // Ngày 30 tháng Chạp rơi vào tháng thiếu thì tính là 29 (quy tắc trong Holiday.on).
            new Holiday("Giao thừa", true, 30, 12, false),
    };

    public static boolean isLeapYear(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }

    /** Các ngày Tết âm lịch (mùng 1 đến mùng 3): dùng để lọc danh sách lễ sắp tới. */
    public static boolean isTet(Holiday h) {
        return h.lunar && !h.personal && h.month == 1 && h.day >= 1 && h.day <= 3;
    }

    private Lunar() {}

    // ---------------------------------------------------------------- Julian day

    public static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        return jd;
    }

    /** @return {ngày, tháng, năm} dương lịch. */
    public static int[] jdToDate(int jd) {
        int a, b, c;
        if (jd > 2299160) {
            a = jd + 32044;
            b = (4 * a + 3) / 146097;
            c = a - (b * 146097) / 4;
        } else {
            b = 0;
            c = jd + 32082;
        }
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = b * 100 + d - 4800 + m / 10;
        return new int[]{day, month, year};
    }

    // ---------------------------------------------------------------- astronomy

    private static int newMoonDay(int k) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = Math.PI / 180;
        double jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        jd1 = jd1 + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double jdNew = jd1 + C1 - deltat;
        return (int) Math.floor(jdNew + 0.5 + TZ / 24);
    }

    /** Kinh độ mặt trời (radian, 0..2π) lúc 0h ngày jdn giờ địa phương. */
    private static double sunLongitudeRad(int jdn) {
        double T = (jdn - 2451545.5 - TZ / 24) / 36525;
        double T2 = T * T;
        double dr = Math.PI / 180;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290 * Math.sin(dr * 3 * M);
        // Hiệu chỉnh chương động và quang sai (Meeus 25.8): tránh lệch ngày bắt đầu tiết khí
        // khi tiết rơi trong vài chục phút sau nửa đêm.
        double omega = 125.04 - 1934.136 * T;
        double L = (L0 + DL - 0.00569 - 0.00478 * Math.sin(omega * dr)) * dr;
        L = L - Math.PI * 2 * Math.floor(L / (Math.PI * 2));
        return L;
    }

    private static int sunSector12(int jdn) {
        return (int) Math.floor(sunLongitudeRad(jdn) / Math.PI * 6);
    }

    private static int lunarMonth11(int yy) {
        int off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = newMoonDay(k);
        if (sunSector12(nm) >= 9) nm = newMoonDay(k - 1);
        return nm;
    }

    private static int leapMonthOffset(int a11) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last;
        int i = 1;
        int arc = sunSector12(newMoonDay(k + i));
        do {
            last = arc;
            i++;
            arc = sunSector12(newMoonDay(k + i));
        } while (arc != last && i < 14);
        return i - 1;
    }

    // ---------------------------------------------------------------- conversion

    public static LunarDate solarToLunar(int dd, int mm, int yy) {
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = (int) Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = newMoonDay(k + 1);
        // Ước lượng k có thể lệch quá một tháng ở vài ngày xa (vd. 7/5/2054): lùi tới khi đúng.
        while (monthStart > dayNumber) monthStart = newMoonDay(k--);
        int a11 = lunarMonth11(yy);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = lunarMonth11(yy - 1);
        } else {
            lunarYear = yy + 1;
            b11 = lunarMonth11(yy + 1);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (int) Math.floor((monthStart - a11) / 29.0);
        boolean leap = false;
        int lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = leapMonthOffset(a11);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) leap = true;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
        return new LunarDate(lunarDay, lunarMonth, lunarYear, leap, dayNumber);
    }

    /** @return {ngày, tháng, năm} dương lịch, hoặc null nếu tháng nhuận không tồn tại. */
    public static int[] lunarToSolar(int lunarDay, int lunarMonth, int lunarYear, boolean lunarLeap) {
        int a11, b11;
        if (lunarMonth < 11) {
            a11 = lunarMonth11(lunarYear - 1);
            b11 = lunarMonth11(lunarYear);
        } else {
            a11 = lunarMonth11(lunarYear);
            b11 = lunarMonth11(lunarYear + 1);
        }
        int k = (int) Math.floor(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int off = lunarMonth - 11;
        if (off < 0) off += 12;
        if (b11 - a11 > 365) {
            int leapOff = leapMonthOffset(a11);
            int leapMonth = leapOff - 2;
            if (leapMonth < 0) leapMonth += 12;
            if (lunarLeap && lunarMonth != leapMonth) {
                return null;
            } else if (lunarLeap || off >= leapOff) {
                off += 1;
            }
        } else if (lunarLeap) {
            return null;
        }
        int monthStart = newMoonDay(k + off);
        return jdToDate(monthStart + lunarDay - 1);
    }

    /** Số ngày của tháng âm chứa ngày jd (29 hoặc 30). */
    public static int lunarMonthLength(LunarDate ld) {
        int start = ld.jd - ld.day + 1;
        int k = (int) Math.floor((start - 2415021.076998695) / 29.530588853 + 0.5);
        int next = newMoonDay(k + 1);
        if (next - start < 25) next = newMoonDay(k + 2);
        else if (next - start > 31) next = newMoonDay(k);
        return next - start;
    }

    // ---------------------------------------------------------------- can chi

    public static String yearCanChi(int lunarYear) {
        return CAN[mod(lunarYear + 6, 10)] + " " + CHI[mod(lunarYear + 8, 12)];
    }

    public static String monthCanChi(int lunarMonth, int lunarYear) {
        return CAN[mod(lunarYear * 12 + lunarMonth + 3, 10)] + " " + CHI[mod(lunarMonth + 1, 12)];
    }

    public static String dayCanChi(int jd) {
        return CAN[mod(jd + 9, 10)] + " " + CHI[mod(jd + 1, 12)];
    }

    public static int dayChi(int jd) {
        return mod(jd + 1, 12);
    }

    // ---------------------------------------------------------------- tiết khí

    /** Chỉ số tiết khí (0..23) đang hiệu lực trong ngày jd. */
    public static int solarTermIndex(int jd) {
        return (int) Math.floor(sunLongitudeRad(jd + 1) / Math.PI * 12);
    }

    public static String solarTerm(int jd) {
        return TIET_KHI[solarTermIndex(jd)];
    }

    /** true nếu ngày jd là ngày bắt đầu một tiết khí mới. */
    public static boolean solarTermStarts(int jd) {
        return solarTermIndex(jd) != solarTermIndex(jd - 1);
    }

    // ---------------------------------------------------------------- hoàng đạo

    public static int dayStar(int jd, int lunarMonth) {
        int start = ((lunarMonth - 1) % 6) * 2;
        return mod(dayChi(jd) - start, 12);
    }

    public static boolean isHoangDao(int jd, int lunarMonth) {
        return GOOD_STAR[dayStar(jd, lunarMonth)];
    }

    /** Danh sách giờ hoàng đạo, vd "Tý (23-1h)". */
    public static List<String> gioHoangDao(int jd) {
        String pattern = GIO_HD[dayChi(jd) % 6];
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < 12; i++) {
            if (pattern.charAt(i) == '1') out.add(CHI[i] + " (" + HOUR_RANGE[i] + ")");
        }
        return out;
    }

    // ---------------------------------------------------------------- ngày lễ

    /** Thay danh sách ngày riêng người dùng nhập (giỗ, sinh nhật). */
    public static void setPersonal(List<Holiday> days) {
        personalDays = new ArrayList<Holiday>(days);
    }

    public static Holiday holidayOn(int dd, int mm, LunarDate ld) {
        for (Holiday h : HOLIDAYS) if (h.on(dd, mm, ld)) return h;
        return null;
    }

    public static Holiday personalOn(int dd, int mm, LunarDate ld) {
        for (Holiday h : personalDays) if (h.on(dd, mm, ld)) return h;
        return null;
    }

    /** Sự kiện của ngày: ngày riêng trước, sau đó ngày lễ. */
    public static Holiday eventOn(int dd, int mm, LunarDate ld) {
        Holiday h = personalOn(dd, mm, ld);
        return h != null ? h : holidayOn(dd, mm, ld);
    }

    public static final class Upcoming {
        public final Holiday holiday;
        public final int days;
        public final int[] solar;

        Upcoming(Holiday holiday, int days, int[] solar) {
            this.holiday = holiday;
            this.days = days;
            this.solar = solar;
        }
    }

    /** Các ngày lễ sắp tới (không gồm hôm nay), tối đa count mục, trong vòng 400 ngày. */
    public static List<Upcoming> upcoming(int dd, int mm, int yy, int count, boolean majorOnly) {
        List<Upcoming> out = new ArrayList<Upcoming>();
        int jd0 = jdFromDate(dd, mm, yy);
        for (int i = 1; i <= 400 && out.size() < count; i++) {
            int[] s = jdToDate(jd0 + i);
            LunarDate ld = solarToLunar(s[0], s[1], s[2]);
            Holiday h = eventOn(s[0], s[1], ld);
            if (h != null && (!majorOnly || h.major)) out.add(new Upcoming(h, i, s));
        }
        return out;
    }

    /** Ngày riêng gần nhất trong withinDays ngày tới (không gồm hôm nay), hoặc null. */
    public static Upcoming nextPersonal(int dd, int mm, int yy, int withinDays) {
        if (personalDays.isEmpty()) return null;
        int jd0 = jdFromDate(dd, mm, yy);
        for (int i = 1; i <= withinDays; i++) {
            int[] s = jdToDate(jd0 + i);
            Holiday h = personalOn(s[0], s[1], solarToLunar(s[0], s[1], s[2]));
            if (h != null) return new Upcoming(h, i, s);
        }
        return null;
    }

    /** Lần tới (tính cả hôm nay) của một ngày, trong vòng 400 ngày; null nếu không có. */
    public static Upcoming next(Holiday h, int dd, int mm, int yy) {
        int jd0 = jdFromDate(dd, mm, yy);
        for (int i = 0; i <= 400; i++) {
            int[] s = jdToDate(jd0 + i);
            if (h.on(s[0], s[1], solarToLunar(s[0], s[1], s[2]))) return new Upcoming(h, i, s);
        }
        return null;
    }

    /** Số ngày tới mùng 1 Tết âm lịch kế tiếp (0 nếu hôm nay là mùng 1 Tết). */
    public static int daysToTet(int dd, int mm, int yy) {
        int jd = jdFromDate(dd, mm, yy);
        LunarDate ld = solarToLunar(dd, mm, yy);
        int targetYear = (ld.month == 1 && ld.day == 1 && !ld.leap) ? ld.year : ld.year + 1;
        int[] s = lunarToSolar(1, 1, targetYear, false);
        if (s == null) return -1;
        return jdFromDate(s[0], s[1], s[2]) - jd;
    }

    private static int mod(int a, int n) {
        int r = a % n;
        return r < 0 ? r + n : r;
    }
}
