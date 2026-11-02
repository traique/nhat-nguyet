package vn.nhatnguyet.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import vn.nhatnguyet.BuildConfig;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.R;
import vn.nhatnguyet.data.DataHub;
import vn.nhatnguyet.data.WeatherSource;
import vn.nhatnguyet.net.Http;
import vn.nhatnguyet.net.Tls;
import vn.nhatnguyet.sys.AdminReceiver;
import vn.nhatnguyet.sys.Scheduler;
import vn.nhatnguyet.sys.WakeService;
import vn.nhatnguyet.update.Updater;

/** Màn Cài đặt dựng bằng code (chạy được trên Android 4.0.3 với giao diện Holo). */
public class SettingsActivity extends Activity implements Updater.Listener {
    private static final int REQ_ADMIN = 7;
    private static final int REQ_LOCATION = 8;
    private static final int ACCENT = 0xFF5FD3B0;

    private Prefs prefs;
    private Updater updater;
    private final Handler handler = new Handler();
    private float density;

    private EditText place, watchlist, feeds;
    private TextView placeState, adminState, updateState, versionText;
    private Button adminButton, installButton;
    private CheckBox alwaysOn, burnIn, warmNight, proximity, wakeMorning, autoStart, autoUpdate;
    private RadioGroup layoutGroup, nightGroup, displayGroup;
    private CheckBox flipInfo;
    private Slider rotate, fontScale, dayStart, dayEnd, idle, nightBright, dayBright, installHour;
    private final List<CheckBox> pageChecks = new ArrayList<CheckBox>();
    private boolean geocoding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Runnable locateTimeout;
    private boolean firstSection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        updater = Updater.get(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updater.addListener(this);
        refreshStates();
    }

    @Override
    protected void onDestroy() {
        stopLocating();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        updater.removeListener(this);
        save(false);
        super.onPause();
    }

    @Override
    public void onUpdateChanged() {
        refreshStates();
    }

    // ------------------------------------------------------------------ dựng giao diện

    private int dp(float v) {
        return Math.round(v * density);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(4), dp(20), dp(40));

        section(root, "Màn hình chính");
        displayGroup = radios(root, 300, new String[]{
                "Bảng lịch đầy đủ",
                "Chỉ đồng hồ lật cỡ lớn"});
        flipInfo = check(root, "Dưới đồng hồ lật hiện ngày, âm lịch và thời tiết");

        section(root, "Thời tiết");
        LinearLayout placeRow = new LinearLayout(this);
        placeRow.setOrientation(LinearLayout.HORIZONTAL);
        placeRow.setGravity(Gravity.CENTER_VERTICAL);
        place = new EditText(this);
        place.setHint("Thành phố, vd Hà Nội, Đà Nẵng");
        place.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        place.setSingleLine(true);
        placeRow.addView(place, new LinearLayout.LayoutParams(0, -2, 1f));
        Button find = plainButton("Tìm", new Runnable() {
            @Override
            public void run() {
                geocode(true);
            }
        });
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-2, -2);
        fp.leftMargin = dp(8);
        placeRow.addView(find, fp);
        root.addView(placeRow, new LinearLayout.LayoutParams(-1, -2));
        button(root, "Dùng vị trí hiện tại", new Runnable() {
            @Override
            public void run() {
                useCurrentLocation();
            }
        });
        placeState = note(root, "");
        note(root, "Vị trí chỉ dùng để lấy thời tiết; tên nơi tra từ OpenStreetMap.");

        section(root, "Trang thông tin");
        note(root, "Các trang xoay vòng bên phải bảng lịch. Chạm để sang trang, vuốt ngang để tiến hoặc lùi.");
        for (int i = 0; i < Prefs.PAGE_KEYS.length; i++) {
            CheckBox c = check(root, Prefs.PAGE_NAMES[i]);
            c.setTag(Prefs.PAGE_KEYS[i]);
            pageChecks.add(c);
        }
        rotate = new Slider(root, "Đổi trang sau", 5, 120, 5, new Label() {
            @Override
            public String of(int v) {
                return v + " giây";
            }
        });
        label(root, "Bố cục");
        layoutGroup = radios(root, 100, new String[]{"Tự động theo kích thước máy", "Luôn 2 cột", "3 cột (máy màn dài từ 6 inch)"});
        fontScale = new Slider(root, "Cỡ chữ", 70, 160, 5, new Label() {
            @Override
            public String of(int v) {
                return v + "%";
            }
        });

        section(root, "Ngày và đêm");
        alwaysOn = check(root, "Luôn sáng cả ngày lẫn đêm");
        Label hour = new Label() {
            @Override
            public String of(int v) {
                return v + ":00";
            }
        };
        dayStart = new Slider(root, "Giờ ngày bắt đầu", 0, 23, 1, hour);
        dayEnd = new Slider(root, "Giờ ngày kết thúc", 1, 24, 1, hour);
        note(root, "Trong giờ ngày màn hình luôn sáng. Ngoài giờ này là ban đêm.");
        label(root, "Ban đêm");
        nightGroup = radios(root, 200, new String[]{
                "Chỉ giảm độ sáng, màn vẫn sáng",
                "Tắt theo thời gian chờ của máy khi không chạm",
                "Tắt hẳn màn hình khi không chạm (cần quyền quản trị)"});
        idle = new Slider(root, "Tắt sau khi không chạm", 1, 60, 1, new Label() {
            @Override
            public String of(int v) {
                return v + " phút";
            }
        });
        nightBright = new Slider(root, "Độ sáng ban đêm", 1, 100, 1, new Label() {
            @Override
            public String of(int v) {
                return v + "%";
            }
        });
        dayBright = new Slider(root, "Độ sáng ban ngày", 0, 100, 5, new Label() {
            @Override
            public String of(int v) {
                return v == 0 ? "Theo máy" : v + "%";
            }
        });
        warmNight = check(root, "Ban đêm chuyển chữ sang tông ấm (bớt ánh sáng xanh)");
        burnIn = check(root, "Chống lưu ảnh: dịch nội dung vài điểm ảnh mỗi 2 phút");
        adminButton = button(root, "", new Runnable() {
            @Override
            public void run() {
                toggleAdmin();
            }
        });
        adminState = note(root, "");
        note(root, "\"Tắt theo thời gian chờ\" dùng thời gian chờ màn hình của máy (Cài đặt > Màn hình), nên đặt 15–30 giây. "
                + "Để bảng tự hiện lại khi bật màn, đặt khoá màn hình là \"Không\" hoặc \"Vuốt\".");

        section(root, "Đánh thức và tự chạy");
        proximity = check(root, "Vẫy tay qua cảm biến tiệm cận để bật màn hình");
        note(root, sensorInfo());
        wakeMorning = check(root, "Tự bật màn hình lúc bắt đầu giờ ngày");
        autoStart = check(root, "Tự mở bảng lịch khi khởi động máy");
        if (Build.VERSION.SDK_INT >= 23) {
            button(root, "Cho app chạy nền (tắt tối ưu pin)", new Runnable() {
                @Override
                public void run() {
                    askIgnoreBattery();
                }
            });
            button(root, "Cho phép hiện trên ứng dụng khác (để tự bật lại)", new Runnable() {
                @Override
                public void run() {
                    askOverlay();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 33) {
            button(root, "Cho phép thông báo của dịch vụ đánh thức", new Runnable() {
                @Override
                public void run() {
                    askNotifications();
                }
            });
        }
        note(root, "Samsung, Xiaomi, Oppo: thêm Nhật Nguyệt vào danh sách được chạy nền và tự khởi động trong phần quản lý pin của máy.");

        section(root, "Chứng khoán và tin tức");
        label(root, "Mã theo dõi, cách nhau dấu phẩy");
        watchlist = input(root, Prefs.DEFAULT_WATCHLIST, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        note(root, "Chỉ số: VNINDEX, VN30, HNX, HNX30, UPCOM.");
        label(root, "Nguồn tin RSS, mỗi dòng một đường dẫn");
        feeds = input(root, "https://…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        feeds.setSingleLine(false);
        feeds.setMinLines(3);
        feeds.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        section(root, "Cập nhật");
        autoUpdate = check(root, "Tự kiểm tra và tải bản mới");
        installHour = new Slider(root, "Tự mở màn hình cài bản mới lúc", -1, 23, 1, new Label() {
            @Override
            public String of(int v) {
                return v < 0 ? "Không tự mở" : v + ":00";
            }
        });
        button(root, "Kiểm tra cập nhật ngay", new Runnable() {
            @Override
            public void run() {
                save(false);
                updater.check();
            }
        });
        installButton = button(root, "Cài bản mới", new Runnable() {
            @Override
            public void run() {
                updater.install(SettingsActivity.this);
            }
        });
        updateState = note(root, "");
        if (Build.VERSION.SDK_INT < 26) {
            note(root, "Lần đầu cần bật: Cài đặt > Bảo mật > Nguồn không xác định. Mỗi lần cập nhật cần bấm \"Cài đặt\" một lần.");
        }

        section(root, "Giới thiệu");
        LinearLayout about = new LinearLayout(this);
        about.setOrientation(LinearLayout.HORIZONTAL);
        about.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setContentDescription(getString(R.string.app_name));
        about.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), 0, 0, 0);
        TextView name = new TextView(this);
        name.setText(R.string.app_name);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(name);
        versionText = new TextView(this);
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        versionText.setAlpha(0.75f);
        info.addView(versionText);
        TextView credit = new TextView(this);
        credit.setText("design by LCTA");
        credit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        credit.setAlpha(0.6f);
        info.addView(credit);
        about.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(8);
        root.addView(about, ap);
        note(root, "Trên bảng lịch: bấm biểu tượng bánh răng ở góc trên bên phải (hoặc giữ lâu vào đồng hồ) để mở lại trang này.");

        Button saveBtn = button(root, "Lưu và đóng", new Runnable() {
            @Override
            public void run() {
                save(true);
                finish();
            }
        });
        ((LinearLayout.LayoutParams) saveBtn.getLayoutParams()).topMargin = dp(24);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        return sv;
    }

    private void section(LinearLayout root, String s) {
        if (!firstSection) {
            View line = new View(this);
            line.setBackgroundColor(0x33FFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(1, dp(1)));
            lp.topMargin = dp(24);
            root.addView(line, lp);
        }
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ACCENT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(firstSection ? 12 : 16);
        root.addView(t, lp);
        firstSection = false;
    }

    private TextView label(LinearLayout root, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        root.addView(t, lp);
        return t;
    }

    private TextView note(LinearLayout root, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setAlpha(0.72f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        root.addView(t, lp);
        return t;
    }

    private EditText input(LinearLayout root, String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        if ((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) e.setSingleLine(true);
        root.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private CheckBox check(LinearLayout root, String s) {
        CheckBox c = new CheckBox(this);
        c.setText(s);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        root.addView(c, lp);
        return c;
    }

    private RadioGroup radios(LinearLayout root, int baseId, String[] labels) {
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            RadioButton b = new RadioButton(this);
            b.setId(baseId + i);
            b.setText(labels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            b.setMinHeight(dp(44));
            g.addView(b);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        root.addView(g, lp);
        return g;
    }

    private Button plainButton(String s, final Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        return b;
    }

    private Button button(LinearLayout root, String s, Runnable action) {
        Button b = plainButton(s, action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return b;
    }

    private interface Label {
        String of(int v);
    }

    /** Thanh trượt có giá trị hiện ngay bên phải tên (SeekBar API 15 không có setMin nên tự cộng lệch). */
    private final class Slider {
        final SeekBar bar;
        final TextView value;
        final int min, max, step;
        final Label label;

        Slider(LinearLayout root, String title, int min, int max, int step, Label label) {
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            LinearLayout head = new LinearLayout(SettingsActivity.this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            TextView t = new TextView(SettingsActivity.this);
            t.setText(title);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            value = new TextView(SettingsActivity.this);
            value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            value.setTypeface(Typeface.DEFAULT_BOLD);
            value.setTextColor(ACCENT);
            head.addView(value);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
            hp.topMargin = dp(16);
            root.addView(head, hp);
            bar = new SeekBar(SettingsActivity.this);
            bar.setMax((max - min) / step);
            bar.setContentDescription(title);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    value.setText(Slider.this.label.of(get()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(40));
            root.addView(bar, bp);
        }

        int get() {
            return min + bar.getProgress() * step;
        }

        void set(int v) {
            int c = Math.max(min, Math.min(max, v));
            bar.setProgress(Math.round((c - min) / (float) step));
            value.setText(label.of(get()));
        }
    }

    // ------------------------------------------------------------------ nạp / lưu

    private void load() {
        place.setText(prefs.placeName());
        placeState.setText(placeLabel());
        watchlist.setText(prefs.watchlistRaw());
        feeds.setText(prefs.feedsRaw());
        for (CheckBox c : pageChecks) c.setChecked(prefs.pageEnabled((String) c.getTag()));
        displayGroup.check(prefs.clockOnly() ? 301 : 300);
        flipInfo.setChecked(prefs.flipInfo());
        rotate.set(prefs.rotateSeconds());
        String l = prefs.layout();
        layoutGroup.check(Prefs.LAYOUT_2.equals(l) ? 101 : Prefs.LAYOUT_3.equals(l) ? 102 : 100);
        fontScale.set(prefs.fontScale());
        alwaysOn.setChecked(prefs.alwaysOn());
        dayStart.set(prefs.dayStart());
        dayEnd.set(prefs.dayEnd());
        String n = prefs.nightMode();
        nightGroup.check(Prefs.NIGHT_DIM.equals(n) ? 200 : Prefs.NIGHT_OFF.equals(n) ? 202 : 201);
        idle.set(prefs.idleMinutes());
        nightBright.set(prefs.nightBrightness());
        dayBright.set(prefs.dayBrightness());
        warmNight.setChecked(prefs.warmNight());
        burnIn.setChecked(prefs.burnInShift());
        proximity.setChecked(prefs.proximityWake());
        wakeMorning.setChecked(prefs.wakeMorning());
        autoStart.setChecked(prefs.autoStart());
        autoUpdate.setChecked(prefs.autoUpdate());
        installHour.set(prefs.updateInstallHour());
    }

    private String placeLabel() {
        return String.format(java.util.Locale.US, "Đang dùng: %s (%.3f, %.3f)", prefs.placeName(), prefs.placeLat(), prefs.placeLon());
    }

    private void save(boolean announce) {
        prefs.setWatchlist(watchlist.getText().toString().trim().length() == 0
                ? "" : watchlist.getText().toString().trim());
        String f = feeds.getText().toString().trim();
        prefs.setFeeds(f);
        List<String> pagesOn = new ArrayList<String>();
        for (CheckBox c : pageChecks) if (c.isChecked()) pagesOn.add((String) c.getTag());
        if (pagesOn.isEmpty()) pagesOn.add("sheet");
        prefs.setPages(pagesOn);
        int did = displayGroup.getCheckedRadioButtonId();
        prefs.putString("display_mode", did == 301 ? Prefs.DISPLAY_FLIP : Prefs.DISPLAY_BOARD);
        prefs.putBool("flip_info", flipInfo.isChecked());
        prefs.putInt("rotate_seconds", rotate.get());
        int lid = layoutGroup.getCheckedRadioButtonId();
        prefs.putString("layout", lid == 101 ? Prefs.LAYOUT_2 : lid == 102 ? Prefs.LAYOUT_3 : Prefs.LAYOUT_AUTO);
        prefs.putInt("font_scale", fontScale.get());
        prefs.putBool("always_on", alwaysOn.isChecked());
        prefs.putInt("day_start", dayStart.get());
        prefs.putInt("day_end", dayEnd.get());
        int nid = nightGroup.getCheckedRadioButtonId();
        prefs.putString("night_mode", nid == 200 ? Prefs.NIGHT_DIM : nid == 202 ? Prefs.NIGHT_OFF : Prefs.NIGHT_TIMEOUT);
        prefs.putInt("idle_minutes", idle.get());
        prefs.putInt("night_brightness", nightBright.get());
        prefs.putInt("day_brightness", dayBright.get());
        prefs.putBool("warm_night", warmNight.isChecked());
        prefs.putBool("burn_in_shift", burnIn.isChecked());
        prefs.putBool("proximity_wake", proximity.isChecked());
        prefs.putBool("wake_morning", wakeMorning.isChecked());
        prefs.putBool("auto_start", autoStart.isChecked());
        prefs.putBool("auto_update", autoUpdate.isChecked());
        prefs.putInt("update_install_hour", installHour.get());

        if (!place.getText().toString().trim().equalsIgnoreCase(prefs.placeName())
                && place.getText().toString().trim().length() > 0) {
            geocode(false);
        }
        DataHub.get(this).settingsChanged();
        WakeService.sync(this);
        Scheduler.schedule(this);
        if (announce) Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ vị trí hiện tại

    private void useCurrentLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        locate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQ_LOCATION) return;
        for (int r : results) {
            if (r == PackageManager.PERMISSION_GRANTED) {
                locate();
                return;
            }
        }
        placeState.setText("Chưa được phép dùng vị trí. Có thể nhập tên thành phố rồi bấm Tìm.");
    }

    /** Lấy vị trí gần nhất máy đã biết; nếu cũ quá thì xin một lần mới (tối đa 30 giây). */
    @SuppressLint("MissingPermission") // đã kiểm tra quyền ở useCurrentLocation()
    private void locate() {
        if (locationListener != null) return;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            placeState.setText("Máy không hỗ trợ định vị.");
            return;
        }
        try {
            Location best = null;
            for (String provider : lm.getProviders(true)) {
                try {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (SecurityException ignored) {
                    // vd. GPS khi chỉ có quyền vị trí gần đúng
                }
            }
            // Máy treo tường ít di chuyển: vị trí trong vài giờ gần đây là đủ.
            if (best != null && System.currentTimeMillis() - best.getTime() < 6 * 3_600_000L) {
                usePlace(best);
                return;
            }
            String provider = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER
                    : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
            if (provider == null) {
                if (best != null) usePlace(best);
                else placeState.setText("Định vị đang tắt. Hãy bật vị trí (GPS hoặc Wi‑Fi) trong cài đặt của máy rồi thử lại.");
                return;
            }
            final Location fallback = best;
            locationManager = lm;
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location l) {
                    stopLocating();
                    usePlace(l);
                }

                @Override
                public void onStatusChanged(String p, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String p) {
                }

                @Override
                public void onProviderDisabled(String p) {
                }
            };
            locateTimeout = new Runnable() {
                @Override
                public void run() {
                    stopLocating();
                    if (fallback != null) usePlace(fallback);
                    else placeState.setText("Không lấy được vị trí. Hãy thử lại ở gần cửa sổ, hoặc nhập tên thành phố.");
                }
            };
            lm.requestSingleUpdate(provider, locationListener, Looper.getMainLooper());
            handler.postDelayed(locateTimeout, 30_000L);
            placeState.setText("Đang lấy vị trí…");
        } catch (SecurityException e) {
            stopLocating();
            placeState.setText("Chưa được phép dùng vị trí. Có thể nhập tên thành phố rồi bấm Tìm.");
        }
    }

    private void stopLocating() {
        if (locateTimeout != null) handler.removeCallbacks(locateTimeout);
        if (locationListener != null) locationManager.removeUpdates(locationListener);
        locateTimeout = null;
        locationListener = null;
    }

    /** Lưu toạ độ, tra tên nơi ở luồng nền (không được thì ghi "Vị trí hiện tại"). */
    private void usePlace(Location loc) {
        final double lat = loc.getLatitude();
        final double lon = loc.getLongitude();
        final Context app = getApplicationContext();
        geocoding = true;
        placeState.setText("Đang xác định tên nơi…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String name;
                try {
                    name = WeatherSource.placeName(lat, lon);
                } catch (Exception e) {
                    name = "Vị trí hiện tại";
                }
                new Prefs(app).setPlace(name, lat, lon);
                DataHub.get(app).settingsChanged();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        geocoding = false;
                        if (isFinishing()) return;
                        place.setText(prefs.placeName());
                        placeState.setText(placeLabel());
                        Toast.makeText(SettingsActivity.this, "Đã chọn " + prefs.placeName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void geocode(final boolean announce) {
        final String name = place.getText().toString().trim();
        if (name.length() == 0 || geocoding) return;
        geocoding = true;
        placeState.setText("Đang tìm \"" + name + "\"…");
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Object[] r = WeatherSource.geocode(name);
                    new Prefs(app).setPlace((String) r[0], (Double) r[1], (Double) r[2]);
                    DataHub.get(app).settingsChanged();
                    msg = null;
                } catch (Exception e) {
                    msg = e.getMessage() != null && e.getMessage().startsWith("Không tìm thấy")
                            ? e.getMessage() : "Lỗi: " + Http.describe(e);
                }
                final String m = msg;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        geocoding = false;
                        if (isFinishing()) {
                            if (m != null) Toast.makeText(app, m, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (m == null) {
                            place.setText(prefs.placeName());
                            placeState.setText(placeLabel());
                            if (announce) Toast.makeText(SettingsActivity.this, "Đã chọn " + prefs.placeName(), Toast.LENGTH_SHORT).show();
                        } else {
                            placeState.setText(m);
                        }
                    }
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------ trạng thái, quyền

    private void refreshStates() {
        boolean admin = AdminReceiver.isActive(this);
        adminButton.setText(admin ? "Gỡ quyền tắt màn hình (quản trị thiết bị)" : "Cấp quyền tắt màn hình (quản trị thiết bị)");
        adminState.setText(admin ? "Đã cấp quyền: có thể tắt hẳn màn hình ban đêm. Nhớ gỡ quyền trước khi gỡ app."
                : "Chưa cấp quyền: chế độ \"Tắt hẳn\" sẽ dùng thời gian chờ của máy.");
        versionText.setText("Phiên bản " + BuildConfig.VERSION_NAME + " · Android " + Build.VERSION.RELEASE + " · TLS " + Tls.engine());
        String st = updater.message();
        if (updater.ready()) {
            installButton.setVisibility(View.VISIBLE);
            installButton.setText("Cài bản " + updater.readyName());
            String notes = updater.readyNotes();
            if (st.length() == 0) st = "Bản " + updater.readyName() + " đã tải xong.";
            if (notes.length() > 0) st += "\n" + notes;
        } else {
            installButton.setVisibility(View.GONE);
        }
        if (!updater.supported()) st = "Bản debug (vn.nhatnguyet.debug) không tự cập nhật.";
        updateState.setText(st);
    }

    private void toggleAdmin() {
        if (AdminReceiver.isActive(this)) {
            AdminReceiver.remove(this);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshStates();
                }
            }, 500);
        } else {
            try {
                startActivityForResult(AdminReceiver.requestIntent(this), REQ_ADMIN);
            } catch (Throwable t) {
                Toast.makeText(this, "Máy không hỗ trợ quyền quản trị", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshStates();
    }

    private String sensorInfo() {
        Sensor s = WakeService.proximitySensor(this);
        if (s == null) return "Máy không có cảm biến tiệm cận – chạm màn hình hoặc bấm nút nguồn để bật.";
        return "Cảm biến nằm cạnh loa thoại: " + s.getName() + " (tối đa " + Math.round(s.getMaximumRange()) + " cm). "
                + "Một số máy đời mới chỉ bật cảm biến khi đang gọi nên có thể không đánh thức được khi màn đã tắt.";
    }

    @TargetApi(23)
    private void askIgnoreBattery() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "App đã được phép chạy nền.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    @TargetApi(23)
    private void askOverlay() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Đã được phép.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        } catch (Throwable ignored) {
        }
    }

    @TargetApi(33)
    private void askNotifications() {
        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
    }
}
