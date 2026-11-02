package vn.nhatnguyet.data;

import org.json.JSONException;
import org.json.JSONObject;

/** org.json của Android ném lỗi khi put NaN, nên bỏ qua giá trị không hợp lệ. */
final class J {
    private J() {}

    static void num(JSONObject o, String key, double v) throws JSONException {
        if (!Double.isNaN(v) && !Double.isInfinite(v)) o.put(key, v);
    }
}
