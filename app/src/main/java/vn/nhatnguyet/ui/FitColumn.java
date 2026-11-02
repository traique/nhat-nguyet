package vn.nhatnguyet.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Cột dọc tự co: đo các con, nếu không đủ chỗ thì ẩn bớt các con có độ ưu tiên
 * thấp (số priority lớn) thay vì thu nhỏ chữ. Con "fill" chia phần chỗ trống còn lại.
 */
public class FitColumn extends ViewGroup {

    public static class LP extends MarginLayoutParams {
        public int priority;
        public boolean fill;

        public LP(int priority) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            this.priority = priority;
        }
    }

    private boolean[] hidden = new boolean[0];
    private int shown;

    public FitColumn(Context context) {
        super(context);
    }

    public void add(View v, int priority) {
        addView(v, new LP(priority));
    }

    public void add(View v, int priority, int topMarginPx) {
        LP lp = new LP(priority);
        lp.topMargin = topMarginPx;
        addView(v, lp);
    }

    public void addFill(View v) {
        LP lp = new LP(0);
        lp.fill = true;
        addView(v, lp);
    }

    /** Số con thường (không phải fill) đang được hiển thị. */
    public int shownCount() {
        return shown;
    }

    public boolean isChildShown(int index) {
        return index < hidden.length && !hidden[index];
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LP(0);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LP;
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LP(0);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int hMode = MeasureSpec.getMode(heightSpec);
        int hAvail = hMode == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE / 4 : MeasureSpec.getSize(heightSpec);
        int innerW = Math.max(0, w - getPaddingLeft() - getPaddingRight());
        int innerH = Math.max(0, hAvail - getPaddingTop() - getPaddingBottom());
        int n = getChildCount();
        if (hidden.length != n) hidden = new boolean[n];
        int[] need = new int[n];
        int total = 0;
        int fills = 0;
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            hidden[i] = c.getVisibility() == GONE;
            if (hidden[i]) continue;
            LP lp = (LP) c.getLayoutParams();
            if (lp.fill) {
                fills++;
                continue;
            }
            int cw = innerW - lp.leftMargin - lp.rightMargin;
            int ws = lp.width == LayoutParams.MATCH_PARENT
                    ? MeasureSpec.makeMeasureSpec(Math.max(0, cw), MeasureSpec.EXACTLY)
                    : MeasureSpec.makeMeasureSpec(Math.max(0, cw), MeasureSpec.AT_MOST);
            c.measure(ws, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            need[i] = c.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            total += need[i];
        }
        // Ẩn dần con kém quan trọng nhất (ưu tiên lớn nhất, nếu bằng nhau thì con ở dưới).
        while (total > innerH) {
            int worst = -1;
            int worstP = Integer.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                if (hidden[i]) continue;
                LP lp = (LP) getChildAt(i).getLayoutParams();
                if (lp.fill) continue;
                if (lp.priority >= worstP) {
                    worstP = lp.priority;
                    worst = i;
                }
            }
            if (worst < 0) break;
            hidden[worst] = true;
            total -= need[worst];
        }
        shown = 0;
        for (int i = 0; i < n; i++) {
            if (hidden[i]) continue;
            LP lp = (LP) getChildAt(i).getLayoutParams();
            if (!lp.fill) shown++;
        }
        int rest = Math.max(0, innerH - total);
        if (fills > 0 && hMode != MeasureSpec.UNSPECIFIED) {
            int each = rest / fills;
            for (int i = 0; i < n; i++) {
                View c = getChildAt(i);
                if (hidden[i]) continue;
                LP lp = (LP) c.getLayoutParams();
                if (!lp.fill) continue;
                int cw = innerW - lp.leftMargin - lp.rightMargin;
                c.measure(MeasureSpec.makeMeasureSpec(Math.max(0, cw), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(Math.max(0, each - lp.topMargin - lp.bottomMargin), MeasureSpec.EXACTLY));
            }
        }
        int h = hMode == MeasureSpec.EXACTLY ? hAvail
                : Math.min(hAvail, total + getPaddingTop() + getPaddingBottom());
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int y = getPaddingTop();
        int x0 = getPaddingLeft();
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View c = getChildAt(i);
            if (i < hidden.length && hidden[i]) {
                c.layout(0, 0, 0, 0);
                continue;
            }
            LP lp = (LP) c.getLayoutParams();
            if (lp.fill && c.getMeasuredHeight() == 0 && c.getMeasuredWidth() == 0) {
                c.layout(0, 0, 0, 0);
                continue;
            }
            y += lp.topMargin;
            int cx = x0 + lp.leftMargin;
            c.layout(cx, y, cx + c.getMeasuredWidth(), y + c.getMeasuredHeight());
            y += c.getMeasuredHeight() + lp.bottomMargin;
        }
    }
}
