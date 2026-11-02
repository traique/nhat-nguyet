package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import vn.nhatnguyet.Theme;

/** Chấm báo trang hiện tại. */
public class PageDots extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int count;
    private int current;
    private final float radius;
    private final float gap;

    public PageDots(Context context, float radiusPx) {
        super(context);
        this.radius = radiusPx;
        this.gap = radiusPx * 1.6f;
    }

    public void set(int count, int current) {
        this.count = count;
        this.current = current;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int) Math.ceil(count * radius * 2 + Math.max(0, count - 1) * gap);
        int h = (int) Math.ceil(radius * 2 + 2);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cy = getHeight() / 2f;
        float x = radius;
        for (int i = 0; i < count; i++) {
            paint.setColor(i == current ? Theme.ACCENT : Theme.META);
            canvas.drawCircle(x, cy, i == current ? radius : radius * 0.7f, paint);
            x += radius * 2 + gap;
        }
    }
}
