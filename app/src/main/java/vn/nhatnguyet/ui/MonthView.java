package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Calendar;

import vn.nhatnguyet.Theme;
import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.LunarDate;

/**
 * Lịch tháng tự vẽ: ngày dương lớn, ngày âm nhỏ, ngày lễ và Chủ nhật tô đỏ,
 * chấm vàng = ngày hoàng đạo. Tuần bắt đầu từ Thứ Hai.
 */
public class MonthView extends View {
    private static final String[] HEAD = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint solar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lunar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int year;
    private int month; // 1..12
    private int todayD, todayM, todayY;
    private int firstOffset;
    private int daysInMonth;
    private int prevDays;
    private boolean legend;
    private LunarDate[] lunars = new LunarDate[0];
    private boolean[] holiday = new boolean[0];
    private boolean[] good = new boolean[0];
    private boolean[] personal = new boolean[0];
    private boolean anyPersonal;
    private float scale = 1f;

    public MonthView(Context context) {
        super(context);
        head.setTypeface(Theme.bold());
        solar.setTypeface(Theme.bold());
        lunar.setTypeface(Theme.regular());
        note.setTypeface(Theme.regular());
        head.setTextAlign(Paint.Align.CENTER);
        solar.setTextAlign(Paint.Align.CENTER);
        lunar.setTextAlign(Paint.Align.RIGHT);
    }

    /** Hệ số cỡ chữ theo cài đặt người dùng. */
    public void setScale(float s) {
        scale = s;
        invalidate();
    }

    /** Hiện dòng chú thích dưới lịch khi còn chỗ. */
    public void setLegend(boolean on) {
        legend = on;
        invalidate();
    }

    public void setMonth(int year, int month) {
        this.year = year;
        this.month = month;
        Calendar today = Calendar.getInstance();
        todayD = today.get(Calendar.DAY_OF_MONTH);
        todayM = today.get(Calendar.MONTH) + 1;
        todayY = today.get(Calendar.YEAR);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1);
        firstOffset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        c.add(Calendar.MONTH, -1);
        prevDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        lunars = new LunarDate[daysInMonth + 1];
        holiday = new boolean[daysInMonth + 1];
        good = new boolean[daysInMonth + 1];
        personal = new boolean[daysInMonth + 1];
        anyPersonal = false;
        for (int d = 1; d <= daysInMonth; d++) {
            LunarDate ld = Lunar.solarToLunar(d, month, year);
            lunars[d] = ld;
            holiday[d] = Lunar.holidayOn(d, month, ld) != null;
            good[d] = Lunar.isHoangDao(ld.jd, ld.month);
            personal[d] = Lunar.personalOn(d, month, ld) != null;
            anyPersonal |= personal[d];
        }
        invalidate();
    }

    public int year() {
        return year;
    }

    public int month() {
        return month;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (daysInMonth == 0) return;
        float w = getWidth();
        float h = getHeight();
        int rows = (firstOffset + daysInMonth + 6) / 7;
        float cw = w / 7f;
        float headH = Math.min(h * 0.12f, cw * 0.55f);
        float legendH = Math.min(cw * 0.5f, h * 0.09f);
        boolean showLegend = legend && (h - headH - legendH) / rows >= cw * 0.62f;
        float ch = (h - headH - (showLegend ? legendH : 0)) / rows;

        float headSize = Math.min(headH * 0.62f, cw * 0.36f);
        head.setTextSize(headSize);
        for (int i = 0; i < 7; i++) {
            head.setColor(i == 6 ? Theme.LUNAR : Theme.DIM);
            canvas.drawText(HEAD[i], (i + 0.5f) * cw, headH * 0.72f, head);
        }

        float solarSize = Math.min(ch * 0.50f, cw * 0.44f) * Math.min(1.15f, scale);
        float lunarSize = Math.max(solarSize * 0.46f, Math.min(ch, cw) * 0.22f);
        solar.setTextSize(solarSize);
        lunar.setTextSize(lunarSize);
        float pad = Math.min(cw, ch) * 0.06f;
        float radius = Math.min(cw, ch) * 0.14f;

        // Ngày của tháng trước/sau lấp các ô trống, mờ hẳn để không lẫn với tháng đang xem.
        solar.setColor(Theme.META);
        solar.setAlpha(110);
        for (int idx = 0; idx < rows * 7; idx++) {
            int d;
            if (idx < firstOffset) d = prevDays - firstOffset + idx + 1;
            else if (idx >= firstOffset + daysInMonth) d = idx - firstOffset - daysInMonth + 1;
            else continue;
            canvas.drawText(Integer.toString(d), (idx % 7 + 0.5f) * cw, headH + (idx / 7) * ch + ch * 0.52f, solar);
        }
        solar.setAlpha(255);

        for (int d = 1; d <= daysInMonth; d++) {
            int idx = firstOffset + d - 1;
            int col = idx % 7;
            int row = idx / 7;
            float x = col * cw;
            float y = headH + row * ch;
            boolean isToday = d == todayD && month == todayM && year == todayY;
            if (isToday) {
                fill.setColor(Theme.TODAY_BG);
                rect.set(x + pad, y + pad, x + cw - pad, y + ch - pad);
                canvas.drawRoundRect(rect, radius, radius, fill);
            }
            solar.setColor(isToday ? Theme.TEXT : (holiday[d] || col == 6) ? Theme.LUNAR : Theme.TEXT);
            float sy = y + ch * 0.52f;
            canvas.drawText(Integer.toString(d), x + cw / 2f, sy, solar);

            LunarDate ld = lunars[d];
            String lt = ld.day == 1 || d == 1 ? ld.day + "/" + ld.month + (ld.leap ? "n" : "") : Integer.toString(ld.day);
            lunar.setColor(isToday ? Theme.TEXT : ld.day == 1 || ld.day == 15 ? Theme.LUNAR : Theme.DIM);
            canvas.drawText(lt, x + cw - pad * 2f, y + ch - pad * 2.2f, lunar);

            float dot = Math.max(2f, Math.min(cw, ch) * 0.055f);
            if (good[d]) {
                fill.setColor(Theme.AUSPICIOUS);
                canvas.drawCircle(x + pad * 2.2f + dot, y + pad * 2.2f + dot, dot, fill);
            }
            if (personal[d]) {
                fill.setColor(Theme.LUNAR);
                canvas.drawCircle(x + pad * 2.2f + dot, y + ch - pad * 2.2f - dot, dot * 1.2f, fill);
            }
        }

        if (showLegend) {
            float ly = h - legendH / 2f;
            float r = Math.max(2f, Math.min(cw, ch) * 0.055f);
            float x = pad * 2.2f + r;
            fill.setColor(Theme.AUSPICIOUS);
            canvas.drawCircle(x, ly, r, fill);
            float noteSize = Math.max(lunarSize, legendH * 0.6f);
            note.setTextSize(noteSize);
            note.setColor(Theme.DIM);
            float base = ly - (note.descent() + note.ascent()) / 2f;
            x += r + noteSize * 0.45f;
            String a = "ngày hoàng đạo";
            String b = "số nhỏ: ngày âm lịch";
            canvas.drawText(a, x, base, note);
            x += note.measureText(a) + noteSize * 1.6f;
            String own = "ngày riêng";
            if (anyPersonal && x + r * 2 + noteSize * 0.45f + note.measureText(own) <= w - pad) {
                fill.setColor(Theme.LUNAR);
                canvas.drawCircle(x + r, ly, r * 1.2f, fill);
                x += r * 2 + noteSize * 0.45f;
                canvas.drawText(own, x, base, note);
                x += note.measureText(own) + noteSize * 1.6f;
            }
            if (x + note.measureText(b) <= w - pad) canvas.drawText(b, x, base, note);
        }
    }
}
