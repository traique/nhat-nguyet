package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import vn.nhatnguyet.Theme;
import vn.nhatnguyet.data.WeatherCodes;

/** Biểu tượng thời tiết tự vẽ (Android 4.0 không có emoji màu). */
public class WeatherIcon extends View {
    private static final int SUN = 0xFFFFC94D;
    private static final int MOON = 0xFFEDE6C4;
    private static final int CLOUD = 0xFFD5DDE2;
    private static final int CLOUD_DARK = 0xFF8E9AA2;
    private static final int RAIN = 0xFF5AB0FF;
    private static final int BOLT = 0xFFFFD54A;
    private static final int SNOW = 0xFFFFFFFF;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private int kind = WeatherCodes.CLEAR;
    private boolean day = true;
    private int bg = 0xFF000000;
    private int sizePx;

    public WeatherIcon(Context context, int sizePx) {
        super(context);
        this.sizePx = sizePx;
    }

    public void set(int code, boolean isDay, int background) {
        kind = WeatherCodes.kind(code);
        day = isDay;
        bg = background;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(sizePx, widthMeasureSpec), resolveSize(sizePx, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        float ox = (getWidth() - s) / 2f;
        float oy = (getHeight() - s) / 2f;
        c.save();
        c.translate(ox, oy);
        switch (kind) {
            case WeatherCodes.CLEAR:
                if (day) sun(c, s * 0.5f, s * 0.5f, s * 0.24f);
                else moon(c, s * 0.5f, s * 0.5f, s * 0.3f);
                break;
            case WeatherCodes.PARTLY:
                if (day) sun(c, s * 0.36f, s * 0.36f, s * 0.17f);
                else moon(c, s * 0.38f, s * 0.36f, s * 0.2f);
                cloud(c, s, s * 0.08f, s * 0.12f, CLOUD);
                break;
            case WeatherCodes.CLOUDY:
                cloud(c, s, 0, 0, CLOUD);
                break;
            case WeatherCodes.FOG:
                fog(c, s);
                break;
            case WeatherCodes.DRIZZLE:
                cloud(c, s, 0, -s * 0.12f, CLOUD);
                drops(c, s, false);
                break;
            case WeatherCodes.RAIN:
                cloud(c, s, 0, -s * 0.12f, CLOUD_DARK);
                drops(c, s, true);
                break;
            case WeatherCodes.SNOW:
                cloud(c, s, 0, -s * 0.12f, CLOUD);
                flakes(c, s);
                break;
            case WeatherCodes.STORM:
                cloud(c, s, 0, -s * 0.12f, CLOUD_DARK);
                bolt(c, s);
                break;
            default:
                cloud(c, s, 0, 0, CLOUD);
        }
        c.restore();
    }

    private void sun(Canvas c, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Theme.tint(SUN));
        c.drawCircle(cx, cy, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, r * 0.22f));
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float x1 = cx + (float) Math.cos(a) * r * 1.45f;
            float y1 = cy + (float) Math.sin(a) * r * 1.45f;
            float x2 = cx + (float) Math.cos(a) * r * 1.9f;
            float y2 = cy + (float) Math.sin(a) * r * 1.9f;
            c.drawLine(x1, y1, x2, y2, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void moon(Canvas c, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Theme.tint(MOON));
        c.drawCircle(cx, cy, r, p);
        p.setColor(bg);
        c.drawCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, p);
    }

    private void cloud(Canvas c, float s, float dx, float dy, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(Theme.tint(color));
        float bx = s * 0.18f + dx;
        float by = s * 0.42f + dy;
        float bw = s * 0.66f;
        float bh = s * 0.26f;
        rect.set(bx, by + bh * 0.25f, bx + bw, by + bh);
        c.drawRoundRect(rect, bh * 0.38f, bh * 0.38f, p);
        c.drawCircle(bx + bw * 0.30f, by + bh * 0.45f, bh * 0.55f, p);
        c.drawCircle(bx + bw * 0.58f, by + bh * 0.22f, bh * 0.78f, p);
        c.drawCircle(bx + bw * 0.82f, by + bh * 0.55f, bh * 0.45f, p);
    }

    private void drops(Canvas c, float s, boolean heavy) {
        p.setColor(Theme.tint(RAIN));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.5f, s * (heavy ? 0.06f : 0.045f)));
        int n = heavy ? 3 : 3;
        for (int i = 0; i < n; i++) {
            float x = s * (0.32f + 0.18f * i);
            float y = s * 0.66f;
            float len = heavy ? s * 0.18f : s * 0.08f;
            c.drawLine(x, y, x - len * 0.35f, y + len, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void flakes(Canvas c, float s) {
        p.setColor(Theme.tint(SNOW));
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 3; i++) {
            float x = s * (0.32f + 0.18f * i);
            c.drawCircle(x, s * (0.72f + (i % 2) * 0.08f), s * 0.045f, p);
        }
    }

    private void bolt(Canvas c, float s) {
        p.setColor(Theme.tint(BOLT));
        p.setStyle(Paint.Style.FILL);
        path.reset();
        path.moveTo(s * 0.52f, s * 0.56f);
        path.lineTo(s * 0.38f, s * 0.78f);
        path.lineTo(s * 0.50f, s * 0.78f);
        path.lineTo(s * 0.42f, s * 0.96f);
        path.lineTo(s * 0.64f, s * 0.70f);
        path.lineTo(s * 0.52f, s * 0.70f);
        path.lineTo(s * 0.60f, s * 0.56f);
        path.close();
        c.drawPath(path, p);
    }

    private void fog(Canvas c, float s) {
        p.setColor(Theme.tint(CLOUD));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.5f, s * 0.07f));
        for (int i = 0; i < 4; i++) {
            float y = s * (0.3f + 0.14f * i);
            float x1 = s * (i % 2 == 0 ? 0.18f : 0.26f);
            float x2 = s * (i % 2 == 0 ? 0.74f : 0.82f);
            c.drawLine(x1, y, x2, y, p);
        }
        p.setStyle(Paint.Style.FILL);
    }
}
