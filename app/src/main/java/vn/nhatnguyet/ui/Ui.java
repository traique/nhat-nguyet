package vn.nhatnguyet.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import vn.nhatnguyet.Theme;

/** Tiện ích dựng view bằng code, mọi kích thước nhân hệ số u theo màn hình. */
public final class Ui {
    public final Context ctx;
    public final float density;
    public final float u;

    public Ui(Context ctx, float u) {
        this.ctx = ctx;
        this.density = ctx.getResources().getDisplayMetrics().density;
        this.u = u;
    }

    /** dp đã nhân hệ số màn hình -> px. */
    public int px(float dp) {
        return Math.round(dp * density * u);
    }

    public float pxf(float dp) {
        return dp * density * u;
    }

    public TextView text(float sizeDp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, pxf(sizeDp));
        t.setTextColor(color);
        t.setTypeface(bold ? Theme.bold() : Theme.regular());
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    public TextView text(String s, float sizeDp, int color, boolean bold) {
        TextView t = text(sizeDp, color, bold);
        t.setText(s);
        return t;
    }

    public TextView multi(String s, float sizeDp, int color, boolean bold, int maxLines) {
        TextView t = text(sizeDp, color, bold);
        t.setSingleLine(false);
        t.setMaxLines(maxLines);
        t.setText(s);
        t.setLineSpacing(0, 1.05f);
        return t;
    }

    public LinearLayout row() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public LinearLayout col() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    /** Thêm ô vào hàng bảng với trọng số và căn lề. */
    public TextView cell(LinearLayout row, String s, float weight, boolean right, float sizeDp, int color, boolean bold) {
        TextView t = text(s, sizeDp, color, bold);
        t.setGravity(right ? Gravity.RIGHT : Gravity.LEFT);
        row.addView(t, weight(weight));
        return t;
    }

    public View spacer() {
        return new View(ctx);
    }

    public View divider() {
        View v = new View(ctx);
        v.setBackgroundColor(Theme.PANEL_LINE);
        v.setMinimumHeight(Math.max(1, Math.round(density)));
        return v;
    }
}
