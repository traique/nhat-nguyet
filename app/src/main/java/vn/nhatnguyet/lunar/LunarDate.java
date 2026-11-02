package vn.nhatnguyet.lunar;

/** Một ngày âm lịch. jd là số ngày Julius của ngày dương tương ứng. */
public final class LunarDate {
    public final int day;
    public final int month;
    public final int year;
    public final boolean leap;
    public final int jd;

    public LunarDate(int day, int month, int year, boolean leap, int jd) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.leap = leap;
        this.jd = jd;
    }

    /** "12/9" hoặc "12/6n" cho tháng nhuận. */
    public String shortText() {
        return day + "/" + month + (leap ? "n" : "");
    }

    @Override
    public String toString() {
        return day + "/" + month + "/" + year + (leap ? " (nhuận)" : "");
    }
}
