package vn.nhatnguyet.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import vn.nhatnguyet.Theme;
import vn.nhatnguyet.data.WeatherCodes;

/**
 * Biểu tượng thời tiết tự vẽ (Android 4.0 không có emoji màu).
 * Mỗi phần tô chuyển hai sắc, ánh sáng từ góc trên bên trái, nên nhìn có khối và mịn.
 */
public class WeatherIcon extends View {
    private static final int SUN_A = 0xFFFFE9A6, SUN_B = 0xFFFFAD33, RAY = 0xFFFFC651;
    private static final int MOON_A = 0xFFFFF7DF, MOON_B = 0xFFE2CF97;
    private static final int CLOUD_A = 0xFFFFFFFF, CLOUD_B = 0xFFC2CEDB;
    private static final int DARK_A = 0xFFBAC4CF, DARK_B = 0xFF6E7A87;
    private static final int RAIN_A = 0xFF94D6FF, RAIN_B = 0xFF3C8AF0;
    private static final int BOLT_A = 0xFFFFEE8F, BOLT_B = 0xFFFFA91E;
    private static final int SNOW = 0xFFFFFFFF;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
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
                cloud(c, s, s * 0.08f, s * 0.12f, false);
                break;
            case WeatherCodes.CLOUDY:
                cloud(c, s, 0, 0, false);
                break;
            case WeatherCodes.FOG:
                fog(c, s);
                break;
            case WeatherCodes.DRIZZLE:
                cloud(c, s, 0, -s * 0.12f, false);
                drops(c, s, false);
                break;
            case WeatherCodes.RAIN:
                cloud(c, s, 0, -s * 0.12f, true);
                drops(c, s, true);
                break;
            case WeatherCodes.SNOW:
                cloud(c, s, 0, -s * 0.12f, false);
                flakes(c, s);
                break;
            case WeatherCodes.STORM:
                cloud(c, s, 0, -s * 0.12f, true);
                bolt(c, s);
                break;
            default:
                cloud(c, s, 0, 0, false);
        }
        c.restore();
    }

    private static Shader linear(float x0, float y0, float x1, float y1, int a, int b) {
        return new LinearGradient(x0, y0, x1, y1, Theme.tint(a), Theme.tint(b), Shader.TileMode.CLAMP);
    }

    private void sun(Canvas c, float cx, float cy, float r) {
        p.setShader(null);
        p.setColor(Theme.tint(RAY));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.2f, r * 0.17f));
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            c.drawLine(cx + dx * r * 1.42f, cy + dy * r * 1.42f, cx + dx * r * 1.86f, cy + dy * r * 1.86f, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.4f, r * 1.55f, Theme.tint(SUN_A), Theme.tint(SUN_B),
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
    }

    private void moon(Canvas c, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(linear(cx - r, cy - r, cx + r, cy + r, MOON_A, MOON_B));
        c.drawCircle(cx, cy, r, p);
        // Khoét lưỡi liềm bằng màu nền: clipPath không chạy với tăng tốc phần cứng trước Android 4.3.
        p.setShader(null);
        p.setColor(bg);
        c.drawCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, p);
    }

    private void cloud(Canvas c, float s, float dx, float dy, boolean dark) {
        float bx = s * 0.18f + dx;
        float by = s * 0.42f + dy;
        float bw = s * 0.66f;
        float bh = s * 0.26f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(linear(0, by - bh * 0.6f, 0, by + bh, dark ? DARK_A : CLOUD_A, dark ? DARK_B : CLOUD_B));
        rect.set(bx, by + bh * 0.25f, bx + bw, by + bh);
        c.drawRoundRect(rect, bh * 0.38f, bh * 0.38f, p);
        c.drawCircle(bx + bw * 0.30f, by + bh * 0.45f, bh * 0.55f, p);
        c.drawCircle(bx + bw * 0.58f, by + bh * 0.22f, bh * 0.78f, p);
        c.drawCircle(bx + bw * 0.82f, by + bh * 0.55f, bh * 0.45f, p);
        p.setShader(null);
    }

    private void drops(Canvas c, float s, boolean heavy) {
        float len = heavy ? s * 0.18f : s * 0.09f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.4f, s * (heavy ? 0.058f : 0.048f)));
        p.setShader(linear(0, s * 0.66f, 0, s * 0.66f + len, RAIN_A, RAIN_B));
        for (int i = 0; i < 3; i++) {
            float x = s * (0.32f + 0.18f * i);
            c.drawLine(x, s * 0.66f, x - len * 0.35f, s * 0.66f + len, p);
        }
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
    }

    private void flakes(Canvas c, float s) {
        p.setShader(null);
        p.setColor(Theme.tint(SNOW));
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 3; i++) {
            float x = s * (0.32f + 0.18f * i);
            c.drawCircle(x, s * (0.72f + (i % 2) * 0.08f), s * 0.045f, p);
        }
    }

    private void bolt(Canvas c, float s) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(linear(0, s * 0.56f, 0, s * 0.96f, BOLT_A, BOLT_B));
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
        p.setShader(null);
    }

    private void fog(Canvas c, float s) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Math.max(1.5f, s * 0.07f));
        for (int i = 0; i < 4; i++) {
            // Sương nhạt dần xuống dưới.
            p.setColor(Theme.tint(CLOUD_B));
            p.setAlpha(255 - i * 45);
            float y = s * (0.3f + 0.14f * i);
            float x1 = s * (i % 2 == 0 ? 0.18f : 0.26f);
            float x2 = s * (i % 2 == 0 ? 0.74f : 0.82f);
            c.drawLine(x1, y, x2, y, p);
        }
        p.setStyle(Paint.Style.FILL);
    }
}
