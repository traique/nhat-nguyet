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
    private final float[] gx = new float[32], gy = new float[32];
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
            // Bánh răng tô đặc, góc răng bo bằng đường cong, lỗ giữa khoét bằng EVEN_ODD.
            // Không dùng FILL_AND_STROKE: Skia tô kín cả lỗ khi vừa tô vừa viền.
            double step = Math.PI / 4;
            for (int i = 0; i < 8; i++) {
                double a = step * i;
                gearPoint(i * 4, cx, cy, r * 0.66f, a - step * 0.31);
                gearPoint(i * 4 + 1, cx, cy, r * 0.94f, a - step * 0.17);
                gearPoint(i * 4 + 2, cx, cy, r * 0.94f, a + step * 0.17);
                gearPoint(i * 4 + 3, cx, cy, r * 0.66f, a + step * 0.31);
            }
            path.reset();
            float round = r * 0.09f;
            for (int i = 0; i < 32; i++) {
                int prev = (i + 31) % 32, next = (i + 1) % 32;
                float t1 = cut(i, prev, round), t2 = cut(i, next, round);
                float ax = gx[i] + (gx[prev] - gx[i]) * t1, ay = gy[i] + (gy[prev] - gy[i]) * t1;
                if (i == 0) path.moveTo(ax, ay);
                else path.lineTo(ax, ay);
                path.quadTo(gx[i], gy[i], gx[i] + (gx[next] - gx[i]) * t2, gy[i] + (gy[next] - gy[i]) * t2);
            }
            path.close();
            path.addCircle(cx, cy, r * 0.3f, Path.Direction.CW);
            path.setFillType(Path.FillType.EVEN_ODD);
            paint.setStyle(Paint.Style.FILL);
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
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

    private void gearPoint(int i, float cx, float cy, float rad, double a) {
        gx[i] = cx + (float) (Math.cos(a) * rad);
        gy[i] = cy + (float) (Math.sin(a) * rad);
    }

    /** Tỉ lệ trên cạnh từ đỉnh i tới đỉnh j để bo góc dài khoảng len, không quá nửa cạnh. */
    private float cut(int i, int j, float len) {
        float d = (float) Math.hypot(gx[j] - gx[i], gy[j] - gy[i]);
        return d <= 0 ? 0 : Math.min(0.5f, len / d);
    }
}
