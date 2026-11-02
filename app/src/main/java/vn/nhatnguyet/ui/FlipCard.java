package vn.nhatnguyet.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import vn.nhatnguyet.Theme;

/**
 * Một thẻ của đồng hồ lật kiểu HTC (vd. "14" hoặc "09"): nền xám than chia đôi
 * ngang, có bản lề hai bên. Khi đổi số, nửa trên của số cũ gập xuống rồi nửa dưới
 * của số mới mở ra (xoay 3D quanh đường chia).
 */
public class FlipCard extends ViewGroup {
    private static final long DURATION = 620L;

    private final HalfView topStatic;
    private final HalfView bottomStatic;
    private final HalfView topFlap;
    private final HalfView bottomFlap;
    private final Paint hinge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String current = "";
    private ValueAnimator anim;
    private int gap;

    public FlipCard(Context context) {
        super(context);
        setClipChildren(false);
        setWillNotDraw(false);
        text.setTypeface(Theme.bold());
        text.setColor(Theme.TEXT);
        text.setTextAlign(Paint.Align.CENTER);
        topStatic = new HalfView(context, true);
        bottomStatic = new HalfView(context, false);
        topFlap = new HalfView(context, true);
        bottomFlap = new HalfView(context, false);
        addView(topStatic);
        addView(bottomStatic);
        addView(bottomFlap);
        addView(topFlap);
        float cam = context.getResources().getDisplayMetrics().density * 9000f;
        topFlap.setCameraDistance(cam);
        bottomFlap.setCameraDistance(cam);
        topFlap.setVisibility(INVISIBLE);
        bottomFlap.setVisibility(INVISIBLE);
    }

    /** Đổi số hiển thị; animate=false để đặt ngay (vd. lúc mới mở màn hình). */
    public void setText(String value, boolean animate) {
        if (value.equals(current)) return;
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
        String old = current;
        current = value;
        if (!animate || old.length() == 0 || getWidth() == 0) {
            topStatic.set(value);
            bottomStatic.set(value);
            resetFlaps();
            return;
        }
        topStatic.set(value);
        bottomStatic.set(old);
        topFlap.set(old);
        bottomFlap.set(value);
        topFlap.setPivotX(topFlap.getWidth() / 2f);
        topFlap.setPivotY(topFlap.getHeight());
        bottomFlap.setPivotX(bottomFlap.getWidth() / 2f);
        bottomFlap.setPivotY(0);
        topFlap.setRotationX(0);
        bottomFlap.setRotationX(90);
        topFlap.setVisibility(VISIBLE);
        bottomFlap.setVisibility(VISIBLE);

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(DURATION);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float f = (Float) a.getAnimatedValue();
                if (f < 0.5f) {
                    float t = f / 0.5f;
                    t = t * t; // nửa trên rơi nhanh dần
                    topFlap.setRotationX(-90f * t);
                    topFlap.setShade(0.55f * t);
                    bottomStatic.setShade(0.35f * (1 - t));
                } else {
                    float t = (f - 0.5f) / 0.5f;
                    t = 1 - (1 - t) * (1 - t); // nửa dưới chậm dần khi chạm đáy
                    topFlap.setRotationX(-90f);
                    bottomFlap.setRotationX(90f * (1 - t));
                    bottomFlap.setShade(0.45f * (1 - t));
                    bottomStatic.setShade(0f);
                }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
                finish();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) finish();
            }

            private void finish() {
                bottomStatic.set(current);
                topStatic.set(current);
                resetFlaps();
            }
        });
        anim.start();
    }

    private void resetFlaps() {
        topFlap.setVisibility(INVISIBLE);
        bottomFlap.setVisibility(INVISIBLE);
        topFlap.setRotationX(0);
        bottomFlap.setRotationX(0);
        topFlap.setShade(0);
        bottomFlap.setShade(0);
        bottomStatic.setShade(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(w, h);
        gap = Math.max(2, Math.round(h * 0.018f));
        int half = (h - gap) / 2;
        int ws = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int hs = MeasureSpec.makeMeasureSpec(half, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(ws, hs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        int half = (h - gap) / 2;
        topStatic.layout(0, 0, w, half);
        topFlap.layout(0, 0, w, half);
        bottomStatic.layout(0, h - half, w, h);
        bottomFlap.layout(0, h - half, w, h);
        // Cỡ chữ: số chiếm ~78% chiều cao thẻ, không tràn ngang.
        float size = h * 0.78f;
        text.setTextSize(size);
        float tw = text.measureText("88");
        if (tw > w * 0.84f) size *= w * 0.84f / tw;
        text.setTextSize(size);
        float cardCenterTop = half + gap / 2f; // tâm thẻ tính từ mép trên nửa trên
        topStatic.setGeometry(text, cardCenterTop, h * 0.08f, gap);
        topFlap.setGeometry(text, cardCenterTop, h * 0.08f, gap);
        float cardCenterBottom = -gap / 2f; // tâm thẻ tính từ mép trên nửa dưới
        bottomStatic.setGeometry(text, cardCenterBottom, h * 0.08f, gap);
        bottomFlap.setGeometry(text, cardCenterBottom, h * 0.08f, gap);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        // Bản lề hai bên đường chia, giống đồng hồ lật HTC.
        float h = getHeight();
        float w = getWidth();
        float mid = h / 2f;
        float hw = Math.max(3f, w * 0.022f);
        float hh = Math.max(4f, h * 0.075f);
        hinge.setColor(Theme.tint(0xFF4A4A4A));
        rect.set(-hw * 0.35f, mid - hh / 2f, hw * 0.65f, mid + hh / 2f);
        canvas.drawRoundRect(rect, hw * 0.3f, hw * 0.3f, hinge);
        rect.set(w - hw * 0.65f, mid - hh / 2f, w + hw * 0.35f, mid + hh / 2f);
        canvas.drawRoundRect(rect, hw * 0.3f, hw * 0.3f, hinge);
    }

    /** Nửa trên hoặc nửa dưới của thẻ: nền + phần số tương ứng. */
    static final class HalfView extends View {
        private final boolean top;
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private Paint text;
        private String value = "";
        private float center;
        private float radius;
        private float shadeAmount;

        HalfView(Context c, boolean top) {
            super(c);
            this.top = top;
            shade.setColor(0xFF000000);
        }

        void set(String v) {
            value = v;
            invalidate();
        }

        void setShade(float a) {
            if (a == shadeAmount) return;
            shadeAmount = a;
            invalidate();
        }

        void setGeometry(Paint text, float center, float radius, int gap) {
            this.text = text;
            this.center = center;
            this.radius = radius;
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            // Nửa trên sáng hơn một chút, nửa dưới tối hơn – như thẻ nhựa HTC.
            int c1 = Theme.tint(top ? 0xFF3A3A3A : 0xFF262626);
            int c2 = Theme.tint(top ? 0xFF2C2C2C : 0xFF1C1C1C);
            bg.setShader(new LinearGradient(0, 0, 0, h, c1, c2, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            // Bo góc phía ngoài, góc sát đường chia vuông.
            if (top) {
                r.set(0, 0, w, h + radius);
            } else {
                r.set(0, -radius, w, h);
            }
            canvas.save();
            canvas.clipRect(0, 0, w, h);
            canvas.drawRoundRect(r, radius, radius, bg);
            if (text != null && value.length() > 0) {
                Paint.FontMetrics fm = text.getFontMetrics();
                float baseline = center - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(value, w / 2f, baseline, text);
            }
            if (shadeAmount > 0) {
                shade.setAlpha(Math.round(255 * Math.min(1f, shadeAmount)));
                canvas.drawRect(0, 0, w, h, shade);
            }
            canvas.restore();
        }
    }
}
