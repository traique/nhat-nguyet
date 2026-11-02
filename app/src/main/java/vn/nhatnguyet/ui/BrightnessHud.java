package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import vn.nhatnguyet.Theme;

/** Thanh báo độ sáng khi vuốt trên đồng hồ: mặt trời, thanh mức và số phần trăm. */
final class BrightnessHud extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int w, h;
    private int pct;

    BrightnessHud(Context c, Ui ui) {
        super(c);
        w = ui.px(240);
        h = ui.px(54);
        paint.setTypeface(Theme.bold());
        setAlpha(0f);
    }

    void show(int percent) {
        pct = percent;
        animate().cancel();
        setAlpha(1f);
        setContentDescription("Độ sáng " + percent + "%");
        invalidate();
    }

    void hideSoon() {
        animate().alpha(0f).setStartDelay(800).setDuration(300);
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        setMeasuredDimension(resolveSize(w, ws), resolveSize(h, hs));
    }

    @Override
    protected void onDraw(Canvas c) {
        float cw = getWidth(), ch = getHeight(), cy = ch / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Theme.PANEL);
        paint.setAlpha(240);
        rect.set(0, 0, cw, ch);
        c.drawRoundRect(rect, cy, cy, paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, ch * 0.025f));
        paint.setColor(Theme.PANEL_LINE);
        c.drawRoundRect(rect, cy, cy, paint);

        float sx = ch * 0.62f, sr = ch * 0.13f;
        paint.setColor(Theme.AUSPICIOUS);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(ch * 0.05f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            c.drawLine(sx + dx * sr * 1.6f, cy + dy * sr * 1.6f, sx + dx * sr * 2.15f, cy + dy * sr * 2.15f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(sx, cy, sr, paint);

        float x0 = ch * 1.2f, x1 = cw - ch * 1.45f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ch * 0.11f);
        paint.setColor(Theme.PANEL_LINE);
        c.drawLine(x0, cy, x1, cy, paint);
        paint.setColor(Theme.ACCENT);
        c.drawLine(x0, cy, x0 + (x1 - x0) * pct / 100f, cy, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Theme.TEXT);
        paint.setTextSize(ch * 0.36f);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText(pct + "%", cw - ch * 0.4f, cy - (paint.descent() + paint.ascent()) / 2f, paint);
    }
}
