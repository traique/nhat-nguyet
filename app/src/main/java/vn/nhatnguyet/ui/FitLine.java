package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import vn.nhatnguyet.Theme;

/**
 * Một dòng chữ có vài phương án từ dài đến ngắn: hiện phương án dài nhất vừa khung,
 * nên không bao giờ xuống dòng để lại một chữ lẻ loi.
 */
final class FitLine extends View {
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private String[] variants = {""};
    private boolean center;

    FitLine(Context c, float sizePx, boolean bold) {
        super(c);
        paint.setTextSize(sizePx);
        paint.setTypeface(bold ? Theme.bold() : Theme.regular());
    }

    void setCenter(boolean on) {
        center = on;
    }

    void set(int color, String... variants) {
        paint.setColor(color);
        this.variants = variants.length == 0 ? new String[]{""} : variants;
        setContentDescription(this.variants[0]);
        requestLayout();
        invalidate();
    }

    private String pick(float w) {
        for (String v : variants) if (paint.measureText(v) <= w) return v;
        String last = variants[variants.length - 1];
        return TextUtils.ellipsize(last, paint, Math.max(0, w), TextUtils.TruncateAt.END).toString();
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int h = fm.descent - fm.ascent + getPaddingTop() + getPaddingBottom();
        int mode = MeasureSpec.getMode(ws);
        int size = MeasureSpec.getSize(ws);
        int pad = getPaddingLeft() + getPaddingRight();
        int w;
        if (mode == MeasureSpec.EXACTLY) {
            w = size;
        } else {
            float avail = mode == MeasureSpec.AT_MOST ? size - pad : Float.MAX_VALUE;
            w = (int) Math.ceil(paint.measureText(pick(avail))) + pad;
            if (mode == MeasureSpec.AT_MOST) w = Math.min(w, size);
        }
        setMeasuredDimension(w, resolveSize(h, hs));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        String s = pick(w);
        float x = getPaddingLeft() + (center ? (w - paint.measureText(s)) / 2f : 0);
        c.drawText(s, x, getPaddingTop() - paint.getFontMetricsInt().ascent, paint);
    }
}
