package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import vn.nhatnguyet.Theme;

/** Biểu tượng pin tự vẽ: vỏ, mức pin theo màu, tia sét khi đang sạc. */
public class BatteryView extends View {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bolt = new Path();
    private final RectF rect = new RectF();
    private final int heightPx;
    private int level = -1;
    private boolean charging;

    public BatteryView(Context context, int heightPx) {
        super(context);
        this.heightPx = heightPx;
        stroke.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
    }

    public void set(int level, boolean charging) {
        if (level == this.level && charging == this.charging) return;
        this.level = level;
        this.charging = charging;
        invalidate();
    }

    /** Màu theo mức pin: đỏ dưới 20% (khi không sạc), vàng dưới 50%, xanh còn lại. */
    public static int colorFor(int level, boolean charging) {
        if (level < 0) return Theme.META;
        if (level < 20 && !charging) return Theme.DOWN;
        if (level < 50) return Theme.FLAT;
        return Theme.UP;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = Math.round(heightPx * 1.9f);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(heightPx, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight();
        float w = getWidth();
        float sw = Math.max(1f, h * 0.09f);
        float nubW = h * 0.14f;
        float top = h * 0.18f;
        float bottom = h * 0.82f;
        float left = sw / 2f;
        float right = w - nubW - sw / 2f;
        float r = h * 0.12f;

        int color = colorFor(level, charging);
        stroke.setStrokeWidth(sw);
        stroke.setColor(Theme.DIM);
        rect.set(left, top, right, bottom);
        c.drawRoundRect(rect, r, r, stroke);

        fill.setColor(Theme.DIM);
        float nubTop = top + (bottom - top) * 0.3f;
        float nubBottom = bottom - (bottom - top) * 0.3f;
        rect.set(right + sw / 2f, nubTop, w, nubBottom);
        c.drawRect(rect, fill);

        if (level >= 0) {
            float pad = sw * 1.4f;
            float innerW = (right - left) - 2 * pad;
            float lw = Math.max(sw, innerW * Math.min(100, level) / 100f);
            fill.setColor(color);
            rect.set(left + pad, top + pad, left + pad + lw, bottom - pad);
            c.drawRoundRect(rect, r * 0.5f, r * 0.5f, fill);
        }

        if (charging) {
            float cx = (left + right) / 2f;
            float cy = (top + bottom) / 2f;
            float bh = (bottom - top) * 0.95f;
            float bw = bh * 0.62f;
            bolt.reset();
            bolt.moveTo(cx + bw * 0.10f, cy - bh * 0.50f);
            bolt.lineTo(cx - bw * 0.45f, cy + bh * 0.08f);
            bolt.lineTo(cx - bw * 0.02f, cy + bh * 0.08f);
            bolt.lineTo(cx - bw * 0.12f, cy + bh * 0.50f);
            bolt.lineTo(cx + bw * 0.45f, cy - bh * 0.10f);
            bolt.lineTo(cx + bw * 0.02f, cy - bh * 0.10f);
            bolt.close();
            fill.setColor(Theme.AUSPICIOUS);
            c.drawPath(bolt, fill);
            stroke.setColor(Theme.BG);
            stroke.setStrokeWidth(Math.max(1f, sw * 0.6f));
            c.drawPath(bolt, stroke);
        }
    }
}
