package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import vn.nhatnguyet.Theme;

/** Nút biểu tượng tự vẽ (bánh răng, mũi tên); vùng chạm lớn hơn hình vẽ. */
final class IconButton extends View {
    static final int GEAR = 0;
    static final int PREV = 1;
    static final int NEXT = 2;

    private final int kind;
    private final float icon;
    private final int touchW, touchH;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int color;

    IconButton(Context c, int kind, float iconPx, int touchW, int touchH, String description, int color) {
        super(c);
        this.kind = kind;
        this.icon = iconPx;
        this.touchW = touchW;
        this.touchH = touchH;
        this.color = color;
        setContentDescription(description);
        setClickable(true);
        setFocusable(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    void setColor(int c) {
        color = c;
        invalidate();
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        setMeasuredDimension(resolveSize(touchW, ws), resolveSize(touchH, hs));
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = icon / 2f;
        if (isPressed()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Theme.PANEL_LINE);
            c.drawCircle(cx, cy, r * 1.7f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        paint.setColor(color);
        if (kind == GEAR) {
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(r * 0.36f);
            c.drawCircle(cx, cy, r * 0.52f, paint);
            paint.setStrokeWidth(r * 0.34f);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                c.drawLine(cx + dx * r * 0.62f, cy + dy * r * 0.62f, cx + dx * r, cy + dy * r, paint);
            }
            paint.setStrokeCap(Paint.Cap.ROUND);
        } else {
            float d = kind == PREV ? 1 : -1;
            paint.setStrokeWidth(Math.max(2f, r * 0.26f));
            path.reset();
            path.moveTo(cx + d * r * 0.28f, cy - r * 0.62f);
            path.lineTo(cx - d * r * 0.34f, cy);
            path.lineTo(cx + d * r * 0.28f, cy + r * 0.62f);
            c.drawPath(path, paint);
        }
    }
}
