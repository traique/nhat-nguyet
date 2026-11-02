package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import vn.nhatnguyet.Theme;

/** Một chuỗi ngắn (số ngày) vẽ to nhất vừa khung, căn giữa theo nét chữ thật. */
final class BigText extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private final float maxPx;
    private final String sample;
    private String text = "";
    private float scale = 1f;

    /** @param sample chuỗi rộng nhất có thể gặp (vd "28"), để các ngày cùng một cỡ. */
    BigText(Context c, float maxPx, String sample) {
        super(c);
        this.maxPx = maxPx;
        this.sample = sample;
        paint.setTypeface(Theme.bold());
        paint.setTextAlign(Paint.Align.CENTER);
    }

    /** Thu nhỏ so với cỡ vừa khung (để số phụ nhỏ hơn số chính). */
    void setScale(float s) {
        scale = s;
        invalidate();
    }

    void set(String s, int color) {
        text = s;
        paint.setColor(color);
        setContentDescription(s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;
        paint.setTextSize(100f);
        paint.getTextBounds(sample, 0, sample.length(), bounds);
        float size = scale * Math.min(maxPx, Math.min(100f * w / Math.max(1, bounds.width()), 100f * h / Math.max(1, bounds.height())));
        paint.setTextSize(size);
        paint.getTextBounds(text, 0, text.length(), bounds);
        float x = getPaddingLeft() + w / 2f;
        float y = getPaddingTop() + h / 2f - bounds.exactCenterY();
        c.drawText(text, x, y, paint);
    }
}
