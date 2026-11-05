package vn.nhatnguyet.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Theme;
import vn.nhatnguyet.data.DataHub;
import vn.nhatnguyet.data.WeatherSource;
import vn.nhatnguyet.data.StockSource;
import vn.nhatnguyet.data.WeatherCodes;
import vn.nhatnguyet.lunar.DaysOff;
import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.LunarDate;

/** Dựng nội dung từng trang bên phải. Mỗi trang tự cắt số dòng cho vừa khung. */
final class Pages {
    static final float BODY = 16f;
    static final float SMALL = 13f;

    /** Ca dao, tục ngữ quen thuộc cho tờ lịch, mỗi ngày một câu. */
    static final String[] SAYINGS = {
            "Uống nước nhớ nguồn.",
            "Có công mài sắt, có ngày nên kim.",
            "Ăn quả nhớ kẻ trồng cây.",
            "Một cây làm chẳng nên non, ba cây chụm lại nên hòn núi cao.",
            "Lá lành đùm lá rách.",
            "Đi một ngày đàng, học một sàng khôn.",
            "Gần mực thì đen, gần đèn thì rạng.",
            "Tốt gỗ hơn tốt nước sơn.",
            "Học thầy không tày học bạn.",
            "Nước chảy đá mòn.",
            "Thất bại là mẹ thành công.",
            "Có chí thì nên.",
            "Bầu ơi thương lấy bí cùng, tuy rằng khác giống nhưng chung một giàn.",
            "Lời nói chẳng mất tiền mua, lựa lời mà nói cho vừa lòng nhau.",
            "Chớ thấy sóng cả mà ngã tay chèo.",
            "Kiến tha lâu cũng đầy tổ.",
            "Không thầy đố mày làm nên.",
            "Công cha như núi Thái Sơn, nghĩa mẹ như nước trong nguồn chảy ra.",
            "Đói cho sạch, rách cho thơm.",
            "Thương người như thể thương thân.",
            "Một miếng khi đói bằng một gói khi no.",
            "Ăn vóc học hay.",
            "Cẩn tắc vô áy náy.",
            "Năng nhặt chặt bị.",
            "Tích tiểu thành đại.",
            "Nhiễu điều phủ lấy giá gương, người trong một nước phải thương nhau cùng.",
            "Mưa dầm thấm lâu.",
            "Ăn có nhai, nói có nghĩ.",
            "Muốn sang thì bắc cầu kiều, muốn con hay chữ thì yêu lấy thầy.",
            "Chuồn chuồn bay thấp thì mưa, bay cao thì nắng, bay vừa thì râm.",
            "Một con ngựa đau, cả tàu bỏ cỏ.",
            "Chim có tổ, người có tông.",
    };

    private final Ui ui;
    private final DataHub hub;
    /** Bề rộng khung trang tính theo dp đã nhân hệ số (để chọn bản gọn). */
    private final float widthU;

    Pages(Ui ui, DataHub hub, float widthPx) {
        this.ui = ui;
        this.hub = hub;
        this.widthU = widthPx / (ui.density * ui.u);
    }

    /** Khung hẹp (màn 3,5"–5", hoặc cột phải của bố cục 3 cột). */
    private boolean narrow() {
        return widthU < 330;
    }

    /** Rất hẹp: bỏ thêm cột phụ. */
    private boolean tiny() {
        return widthU < 240;
    }

    // ------------------------------------------------------------ chung

    private View message(String s, int color) {
        TextView t = ui.multi(s, BODY, color, false, 4);
        t.setGravity(Gravity.CENTER);
        LinearLayout box = ui.col();
        box.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /** Khi chưa có dữ liệu: đang tải / lỗi / trống. */
    private View empty(String key) {
        DataHub.Entry e = hub.entry(key);
        if (e != null && e.error != null && !e.loading) return message("Lỗi: " + e.error + "\nĐang thử lại…", Theme.WARN);
        return message("Đang tải dữ liệu…", Theme.DIM);
    }

    /** Dòng chú thích cuối trang: nguồn, giờ cập nhật, cảnh báo dữ liệu cũ. */
    private TextView note(String key, String text) {
        DataHub.Entry e = hub.entry(key);
        StringBuilder sb = new StringBuilder(text);
        int color = Theme.META;
        if (e != null && e.error != null) {
            sb.append(" · lỗi: ").append(e.error);
            color = Theme.WARN;
        } else if (hub.isStale(key)) {
            sb.append(" · dữ liệu cũ (").append(Fmt.ago(e.at, System.currentTimeMillis())).append(")");
            color = Theme.WARN;
        }
        return ui.text(sb.toString(), SMALL, color, false);
    }

    private LinearLayout header(String... cols) {
        LinearLayout r = ui.row();
        for (int i = 0; i < cols.length; i += 2) {
            ui.cell(r, cols[i], Float.parseFloat(cols[i + 1]), i > 0, SMALL, Theme.DIM, false);
        }
        return r;
    }

    private LinearLayout tableRow() {
        LinearLayout row = ui.row();
        row.setPadding(0, ui.px(4), 0, 0);
        return row;
    }

    private FitColumn column() {
        return new FitColumn(ui.ctx);
    }

    private FitLine line(float size, boolean bold) {
        return new FitLine(ui.ctx, ui.pxf(size), bold);
    }

    // ------------------------------------------------------------ tờ lịch

    /** Tờ lịch bloc: ngày dương thật lớn, ngày âm đỏ son, can chi, giờ tốt và một câu ca dao. */
    View sheet() {
        Calendar now = Calendar.getInstance();
        int d = now.get(Calendar.DAY_OF_MONTH);
        int m = now.get(Calendar.MONTH) + 1;
        int y = now.get(Calendar.YEAR);
        LunarDate ld = Lunar.solarToLunar(d, m, y);
        Lunar.Holiday hol = Lunar.holidayOn(d, m, ld);
        Lunar.Holiday own = Lunar.personalOn(d, m, ld);
        String off = DaysOff.name(d, m, y);
        boolean red = DaysOff.isRed(d, m, y, ld) || now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        int solarColor = red ? Theme.LUNAR : Theme.TEXT;

        LinearLayout faces = ui.row();
        faces.setBaselineAligned(false);
        LinearLayout sol = ui.col();
        FitLine sm = line(BODY, false);
        sm.setCenter(true);
        sm.set(Theme.DIM, "Tháng " + m + ", " + y, "Tháng " + m);
        sol.addView(sm, new LinearLayout.LayoutParams(-1, -2));
        BigText sd = new BigText(ui.ctx, ui.pxf(150), "28");
        sd.set(Integer.toString(d), solarColor);
        sol.addView(sd, new LinearLayout.LayoutParams(-1, 0, 1f));
        FitLine wd = line(BODY + 2, true);
        wd.setCenter(true);
        wd.set(solarColor, Fmt.WEEKDAYS[now.get(Calendar.DAY_OF_WEEK)]);
        sol.addView(wd, new LinearLayout.LayoutParams(-1, -2));
        faces.addView(sol, new LinearLayout.LayoutParams(0, -1, 1.15f));

        View div = new View(ui.ctx);
        div.setBackgroundColor(Theme.PANEL_LINE);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Math.max(1, Math.round(ui.density)), -1);
        dp.leftMargin = dp.rightMargin = ui.px(8);
        dp.topMargin = dp.bottomMargin = ui.px(6);
        faces.addView(div, dp);

        LinearLayout lun = ui.col();
        FitLine lm = line(BODY, false);
        lm.setCenter(true);
        String month = "Tháng " + Lunar.MONTH_NAME[ld.month] + (ld.leap ? " nhuận" : "");
        lm.set(Theme.DIM, month + ", " + Lunar.yearCanChi(ld.year), month);
        lun.addView(lm, new LinearLayout.LayoutParams(-1, -2));
        BigText lday = new BigText(ui.ctx, ui.pxf(150), "28");
        lday.setScale(0.72f);
        lday.set(Integer.toString(ld.day), Theme.LUNAR);
        lun.addView(lday, new LinearLayout.LayoutParams(-1, 0, 1f));
        FitLine cc = line(BODY + 2, true);
        cc.setCenter(true);
        cc.set(Theme.LUNAR, "Ngày " + Lunar.dayCanChi(ld.jd), Lunar.dayCanChi(ld.jd));
        lun.addView(cc, new LinearLayout.LayoutParams(-1, -2));
        faces.addView(lun, new LinearLayout.LayoutParams(0, -1, 1f));

        FitColumn col = column();
        col.addFill(faces);

        // Ngày nghỉ không có tên lễ riêng (nghỉ bù, nghỉ Tết, hoán đổi) thì ghi tên ngày nghỉ.
        String holName = hol != null ? hol.name : off;
        if (holName != null || own != null) {
            FitLine h = line(BODY, true);
            h.setCenter(true);
            if (own != null && holName != null) h.set(Theme.LUNAR, own.name + " · " + holName, own.name);
            else h.set(Theme.LUNAR, own != null ? own.name : holName);
            col.add(h, 1, ui.px(8));
        }
        StringBuilder gio = new StringBuilder();
        for (String g : Lunar.gioHoangDao(ld.jd)) {
            if (gio.length() > 0) gio.append(", ");
            gio.append(g.substring(0, g.indexOf(' ')));
        }
        boolean good = Lunar.isHoangDao(ld.jd, ld.month);
        FitLine gl = line(SMALL + 1, false);
        gl.setCenter(true);
        gl.set(good ? Theme.AUSPICIOUS : Theme.DIM, (good ? "Ngày hoàng đạo" : "Ngày hắc đạo") + " · giờ tốt: " + gio,
                "Giờ tốt: " + gio, gio.toString());
        col.add(gl, 2, ui.px(8));

        TextView q = ui.multi("“" + SAYINGS[ld.jd % SAYINGS.length] + "”", SMALL + 1, Theme.DIM, false, 2);
        q.setGravity(Gravity.CENTER);
        col.add(q, 3, ui.px(6));
        return col;
    }

    // ------------------------------------------------------------ âm lịch chi tiết

    View lunar() {
        FitColumn col = column();
        Calendar now = Calendar.getInstance();
        int d = now.get(Calendar.DAY_OF_MONTH);
        int m = now.get(Calendar.MONTH) + 1;
        int y = now.get(Calendar.YEAR);
        LunarDate ld = Lunar.solarToLunar(d, m, y);
        int jd = ld.jd;

        LinearLayout top = ui.row();
        TextView big = ui.text(Integer.toString(ld.day), 44, Theme.LUNAR, true);
        top.addView(big);
        LinearLayout right = ui.col();
        right.setPadding(ui.px(10), 0, 0, 0);
        right.addView(ui.text("Tháng " + Lunar.MONTH_NAME[ld.month] + (ld.leap ? " (nhuận)" : "")
                + (Lunar.lunarMonthLength(ld) == 30 ? " · đủ" : " · thiếu"), BODY + 1, Theme.TEXT, true));
        right.addView(ui.text("Năm " + Lunar.yearCanChi(ld.year), BODY, Theme.DIM, false));
        top.addView(right, Ui.weight(1));
        col.add(top, 0);

        String day = "Ngày " + Lunar.dayCanChi(jd);
        FitLine cc = line(BODY, false);
        cc.set(Theme.TEXT, day + " · tháng " + Lunar.monthCanChi(ld.month, ld.year), day);
        col.add(cc, 1, ui.px(8));

        String star = "sao " + Lunar.STARS[Lunar.dayStar(jd, ld.month)];
        boolean good = Lunar.isHoangDao(jd, ld.month);
        String kind = good ? "Ngày hoàng đạo" : "Ngày hắc đạo";
        FitLine hd = line(BODY, false);
        hd.set(good ? Theme.AUSPICIOUS : Theme.DIM, kind + " · " + star, (good ? "Hoàng đạo · " : "Hắc đạo · ") + star, kind);
        col.add(hd, 2, ui.px(6));

        String term = "Tiết " + Lunar.solarTerm(jd);
        FitLine tl = line(BODY, false);
        tl.set(Theme.TEXT, term + (Lunar.solarTermStarts(jd) ? " (bắt đầu hôm nay)" : ""), term);
        col.add(tl, 4, ui.px(6));

        // Khung hẹp chỉ ghi tên giờ, khung rộng ghi kèm khoảng giờ.
        StringBuilder g = new StringBuilder("Giờ tốt: ");
        List<String> gio = Lunar.gioHoangDao(jd);
        for (int i = 0; i < gio.size(); i++) {
            if (i > 0) g.append(", ");
            String h = gio.get(i);
            g.append(narrow() ? h.substring(0, h.indexOf(' ')) : h);
        }
        col.add(ui.multi(g.toString(), SMALL + 1, Theme.DIM, false, 2), 3, ui.px(6));

        List<Lunar.Upcoming> up = Lunar.upcoming(d, m, y, 4, false);
        int tet = Lunar.daysToTet(d, m, y);
        if (tet > 0) {
            col.add(ui.text("Còn " + tet + " ngày đến Tết " + Lunar.yearCanChi(ld.year + 1), BODY, Theme.LUNAR, true), 2, ui.px(8));
        }
        int p = 5;
        for (Lunar.Upcoming u : up) {
            if (Lunar.isTet(u.holiday)) continue;
            String when = (u.days == 1 ? "Ngày mai" : "Còn " + u.days + " ngày") + " · " + u.holiday.name;
            FitLine ul = line(SMALL + 1, false);
            ul.set(Theme.DIM, when + " (" + Fmt.two(u.solar[0]) + "/" + Fmt.two(u.solar[1]) + ")", when);
            col.add(ul, p++, ui.px(4));
        }
        return col;
    }

    // ------------------------------------------------------------ thời tiết

    View weather() {
        JSONObject w = hub.data("weather");
        if (w == null) return empty("weather");
        FitColumn col = column();
        JSONArray hours = w.optJSONArray("hours");
        int start = WeatherSource.firstUpcomingHour(w, System.currentTimeMillis());
        if (hours != null && hours.length() > start) {
            LinearLayout strip = ui.row();
            int count = Math.min(Math.max(4, Math.min(8, (int) (widthU / 46))), hours.length() - start);
            for (int i = start; i < start + count; i++) {
                JSONObject h = hours.optJSONObject(i);
                LinearLayout c = ui.col();
                c.setGravity(Gravity.CENTER_HORIZONTAL);
                TextView hh = ui.text(h.optString("h") + "h", SMALL, Theme.DIM, false);
                hh.setGravity(Gravity.CENTER);
                c.addView(hh);
                WeatherIcon ic = new WeatherIcon(ui.ctx, ui.px(26));
                ic.set(h.optInt("code"), h.optInt("day", 1) == 1, Theme.PANEL);
                c.addView(ic);
                TextView t = ui.text(Fmt.temp(h.optDouble("t")), SMALL + 1, Theme.TEXT, true);
                t.setGravity(Gravity.CENTER);
                c.addView(t);
                int rain = h.optInt("rain");
                TextView r = ui.text(rain >= 20 ? rain + "%" : " ", SMALL - 1, Theme.RAIN, false);
                r.setGravity(Gravity.CENTER);
                c.addView(r);
                strip.addView(c, Ui.weight(1));
            }
            col.add(strip, 1);
        }
        JSONArray days = w.optJSONArray("days");
        if (days != null) {
            for (int i = 0; i < days.length(); i++) {
                JSONObject d = days.optJSONObject(i);
                LinearLayout r = tableRow();
                String date = d.optString("date");
                boolean nar = narrow();
                String label = i == 0 ? "Hôm nay" : dayLabel(date, !nar);
                ui.cell(r, label, nar ? 1.05f : 1.3f, false, BODY, i == 0 ? Theme.ACCENT : Theme.TEXT, i == 0);
                WeatherIcon ic = new WeatherIcon(ui.ctx, ui.px(22));
                ic.set(d.optInt("code"), true, Theme.PANEL);
                r.addView(ic);
                if (!nar) {
                    TextView desc = ui.text(" " + WeatherCodes.text(d.optInt("code")), SMALL + 1, Theme.DIM, false);
                    r.addView(desc, Ui.weight(2.0f));
                }
                ui.cell(r, Fmt.range(d.optDouble("min"), d.optDouble("max")), 1.3f, true, BODY, Theme.TEXT, true);
                int rain = d.optInt("rain");
                ui.cell(r, rain > 0 ? (nar ? rain + "%" : "mưa " + rain + "%") : "", nar ? 0.8f : 1.0f, true, SMALL, Theme.RAIN, false);
                col.add(r, 2 + i);
            }
            JSONObject air = w.optJSONObject("air");
            if (air != null) {
                int aqi = air.optInt("aqi");
                int level = WeatherCodes.aqiLevel(aqi);
                String head = "Không khí " + WeatherCodes.AQI_LABEL[level] + " · AQI " + aqi;
                double pm = air.optDouble("pm25");
                FitLine al = line(SMALL + 1, false);
                al.set(Theme.aqi(level), Double.isNaN(pm) ? head : head + " · bụi mịn PM2.5 " + Math.round(pm), head);
                col.add(al, 19, ui.px(6));
            }
            if (days.length() > 0) {
                JSONObject today = days.optJSONObject(0);
                String sun = today.optString("sunrise");
                if (sun.length() > 0) {
                    col.add(ui.text("Mặt trời mọc " + sun + " · lặn " + today.optString("sunset"), SMALL, Theme.DIM, false), 20, ui.px(6));
                }
            }
        }
        col.addFill(ui.spacer());
        col.add(note("weather", w.optString("place") + " · Open-Meteo.com"), 0);
        return col;
    }

    private static String dayLabel(String iso, boolean withDate) {
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(y, m - 1, d);
            String wd = Fmt.WEEKDAYS_SHORT[c.get(Calendar.DAY_OF_WEEK)];
            return withDate ? wd + " " + Fmt.two(d) + "/" + Fmt.two(m) : wd;
        } catch (Exception e) {
            return iso;
        }
    }

    // ------------------------------------------------------------ vàng

    View gold() {
        JSONObject g = hub.data("gold");
        if (g == null) return empty("gold");
        FitColumn col = column();
        boolean nar = narrow();
        boolean tiny = tiny();
        float nameW = tiny ? 1.5f : (nar ? 1.55f : 1.7f);
        if (tiny) col.add(header("Loại", "1.5", "Bán", "1", "±", "0.8"), 0);
        else col.add(header("Loại", Float.toString(nameW), "Mua", "1", "Bán", "1", "±", "0.8"), 0);
        JSONArray rows = g.optJSONArray("rows");
        JSONObject world = null;
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r.optBoolean("usd")) {
                world = r;
                continue;
            }
            LinearLayout row = tableRow();
            ui.cell(row, nar ? r.optString("short", r.optString("label")) : r.optString("label"), nameW, false, BODY, Theme.TEXT, false);
            if (!tiny) ui.cell(row, Fmt.million(r.optDouble("buy")), 1f, true, BODY, Theme.DIM, false);
            ui.cell(row, Fmt.million(r.optDouble("sell")), 1f, true, BODY, Theme.TEXT, true);
            double ch = r.optDouble("changeSell", 0);
            ui.cell(row, Fmt.signed(ch / 1_000_000d, 1), 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 1 + i);
        }
        if (world != null) {
            LinearLayout row = tableRow();
            row.setPadding(0, ui.px(8), 0, 0);
            ui.cell(row, "Thế giới", nameW, false, BODY, Theme.TEXT, false);
            ui.cell(row, Fmt.dec(world.optDouble("buy"), 1) + (tiny ? "$" : " $/oz"), tiny ? 1f : 2f, true, BODY, Theme.TEXT, true);
            double ch = world.optDouble("changeBuy", 0);
            ui.cell(row, Fmt.signed(ch, 1), 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 2);
        }
        col.addFill(ui.spacer());
        col.add(note("gold", (nar ? "triệu/lượng · " : "triệu đồng/lượng · ") + g.optString("updated")
                + (nar ? "" : " · vang.today")), 0);
        return col;
    }

    // ------------------------------------------------------------ xăng dầu

    View fuel() {
        JSONObject f = hub.data("fuel");
        if (f == null) return empty("fuel");
        FitColumn col = column();
        boolean nar = narrow();
        if (nar) col.add(header("Mặt hàng", "2.0", "Vùng 1", "1.1", "±", "0.75"), 0);
        else col.add(header("Mặt hàng", "2.1", "Vùng 1", "1", "Vùng 2", "1", "±", "0.8"), 0);
        JSONArray rows = f.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = tableRow();
            String name = r.optString("name");
            ui.cell(row, nar ? shortFuel(name) : name, nar ? 2.0f : 2.1f, false, BODY, Theme.TEXT, false);
            ui.cell(row, Fmt.group(Math.round(r.optDouble("zone1"))), nar ? 1.1f : 1f, true, BODY, Theme.TEXT, true);
            if (!nar) {
                double z2 = r.optDouble("zone2");
                ui.cell(row, Double.isNaN(z2) ? "–" : Fmt.group(Math.round(z2)), 1f, true, BODY, Theme.DIM, false);
            }
            double ch = r.optDouble("change", 0);
            ui.cell(row, ch == 0 ? "0" : Fmt.signed(ch, 0), nar ? 0.75f : 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        String upd = f.optString("updated");
        if (nar && upd.length() == 10) upd = upd.substring(0, 5);
        col.add(note("fuel", "đồng/lít · Petrolimex" + (upd.length() > 0 ? " · " + upd : "") + (narrow() ? "" : " · theo giaxanghomnay.com")), 0);
        return col;
    }

    /** "Xăng E10 RON 95 Mức 5" -> "E10 RON95 M5". */
    static String shortFuel(String n) {
        String s = n.replace("Xăng ", "").replace(" Mức ", " M").replace("RON 9", "RON9");
        if (s.startsWith("Dầu DO")) s = s.substring(4);
        return s.trim();
    }

    // ------------------------------------------------------------ tỷ giá

    View fx() {
        JSONObject f = hub.data("fx");
        if (f == null) return empty("fx");
        FitColumn col = column();
        boolean tiny = tiny();
        if (tiny) col.add(header("Ngoại tệ", "1", "Bán", "1.3"), 0);
        else col.add(header("Ngoại tệ", "1", "Mua", "1.3", "Bán", "1.3"), 0);
        JSONArray rows = f.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = tableRow();
            ui.cell(row, r.optString("code"), 1f, false, BODY, Theme.TEXT, true);
            if (!tiny) ui.cell(row, money(r.optDouble("transfer")), 1.3f, true, BODY, Theme.DIM, false);
            ui.cell(row, money(r.optDouble("sell")), 1.3f, true, BODY, Theme.TEXT, true);
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        String upd = f.optString("updated");
        if (narrow() && upd.length() > 5) upd = upd.substring(0, 5);
        col.add(note("fx", (narrow() ? "VCB · mua chuyển khoản · " : "Vietcombank · giá mua chuyển khoản · ") + upd), 0);
        return col;
    }

    private static String money(double v) {
        if (Double.isNaN(v)) return "–";
        return v >= 1000 ? Fmt.dec(v, 0) : Fmt.dec(v, 2);
    }

    // ------------------------------------------------------------ chứng khoán

    View stocks() {
        JSONObject s = hub.data("stocks");
        if (s == null) {
            if (hub.source("stocks") != null && new vn.nhatnguyet.Prefs(ui.ctx).watchlist().isEmpty()) {
                return message("Chưa có mã theo dõi.\nThêm trong Cài đặt.", Theme.DIM);
            }
            return empty("stocks");
        }
        FitColumn col = column();
        boolean nar = narrow();
        if (nar) col.add(header("Mã", "1.3", "Giá", "1.3", "%", "1"), 0);
        else col.add(header("Mã", "1.1", "Giá", "1.3", "+/-", "1.1", "%", "1"), 0);
        JSONArray rows = s.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = tableRow();
            ui.cell(row, r.optString("symbol"), nar ? 1.3f : 1.1f, false, BODY, Theme.TEXT, true);
            if (r.has("error")) {
                ui.cell(row, r.optString("error"), 3.4f, true, SMALL, Theme.WARN, false);
            } else {
                double ch = r.optDouble("change", 0);
                int color = Fmt.changeColor(ch);
                ui.cell(row, Fmt.dec(r.optDouble("price"), 2), 1.3f, true, BODY, color, true);
                if (!nar) ui.cell(row, Fmt.signed(ch, 2), 1.1f, true, BODY, color, false);
                ui.cell(row, Fmt.signed(r.optDouble("pct", 0), 2) + "%", 1f, true, SMALL + 1, color, false);
            }
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        boolean open = StockSource.marketOpen();
        col.add(note("stocks", narrow() ? (open ? "Trong phiên" : "Phiên gần nhất") + " · nghìn đồng · DNSE"
                : (open ? "Đang trong phiên" : "Ngoài giờ giao dịch · phiên gần nhất") + " · giá nghìn đồng · DNSE/Entrade, tham khảo"), 0);
        return col;
    }

    // ------------------------------------------------------------ tin tức

    /** @param offset vị trí bắt đầu (xoay vòng các tin). */
    FitColumn news(int offset) {
        JSONObject n = hub.data("news");
        FitColumn col = column();
        if (n == null) {
            col.addFill(empty("news"));
            return col;
        }
        JSONArray items = n.optJSONArray("items");
        int count = items == null ? 0 : items.length();
        if (count == 0) {
            col.addFill(message("Chưa có tin.", Theme.DIM));
            return col;
        }
        long now = System.currentTimeMillis();
        for (int k = 0; k < count; k++) {
            JSONObject it = items.optJSONObject((offset + k) % count);
            LinearLayout box = ui.col();
            box.setPadding(0, ui.px(k == 0 ? 0 : 7), 0, 0);
            box.addView(ui.multi(it.optString("title"), BODY, Theme.TEXT, false, 2));
            long t = it.optLong("time");
            TextView meta = ui.text(t > 0 ? it.optString("source") + " · " + Fmt.agoPhrase(t, now) : it.optString("source"),
                    SMALL, Theme.DIM, false);
            meta.setPadding(0, ui.px(2), 0, 0);
            box.addView(meta);
            col.add(box, 1 + k);
        }
        col.addFill(ui.spacer());
        int failed = n.optInt("failed");
        col.add(note("news", failed > 0 ? failed + " nguồn RSS lỗi" : "Tin mới từ RSS"), 0);
        return col;
    }
}
