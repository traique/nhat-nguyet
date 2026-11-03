package vn.nhatnguyet.lunar;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Đọc danh sách ngày riêng, mỗi dòng một ngày: "15/8 âm Giỗ ông nội" hoặc "20/11 Sinh nhật mẹ". */
public final class PersonalDays {
    private static final Pattern LINE = Pattern.compile(
            "(\\d{1,2})\\s*[/.-]\\s*(\\d{1,2})\\s*(?:(âm lịch|am lich|âm|am|âl|al)(?=[\\s:,.–-]|$))?\\s*[:,.–-]?\\s*(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final int[] SOLAR_DAYS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private PersonalDays() {}

    /** @param bad nếu khác null thì nhận số thứ tự (từ 1) các dòng không đọc được. */
    public static List<Lunar.Holiday> parse(String text, List<Integer> bad) {
        List<Lunar.Holiday> out = new ArrayList<Lunar.Holiday>();
        if (text == null) return out;
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) continue;
            Lunar.Holiday h = null;
            Matcher m = LINE.matcher(line);
            if (m.matches()) {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                boolean lunar = m.group(3) != null;
                String name = m.group(4).trim();
                boolean valid = month >= 1 && month <= 12 && day >= 1 && day <= (lunar ? 30 : SOLAR_DAYS[month - 1]);
                if (valid && name.length() > 0) h = Lunar.Holiday.personal(name, lunar, day, month);
            }
            if (h != null) out.add(h);
            else if (bad != null) bad.add(i + 1);
        }
        return out;
    }
}
