package vn.nhatnguyet.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import vn.nhatnguyet.BuildConfig;
import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.R;
import vn.nhatnguyet.data.DataHub;
import vn.nhatnguyet.data.WeatherSource;
import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.PersonalDays;
import vn.nhatnguyet.net.Http;
import vn.nhatnguyet.net.Tls;
import vn.nhatnguyet.sys.AdminReceiver;
import vn.nhatnguyet.sys.BatteryGuard;
import vn.nhatnguyet.sys.BatteryStats;
import vn.nhatnguyet.sys.Scheduler;
import vn.nhatnguyet.sys.WakeService;
import vn.nhatnguyet.update.Updater;

/**
 * Màn Cài đặt dựng bằng code (chạy được trên Android 4.0.3 với giao diện Holo).
 * Phần hay dùng nằm trên, phần ít dùng gom vào nhóm "Nâng cao" thu gọn. Tự lưu khi rời màn.
 */
public class SettingsActivity extends Activity implements Updater.Listener {
    /** Mở Cài đặt và bấm luôn "Dùng vị trí hiện tại" (hộp thoại lần đầu của bảng lịch). */
    public static final String EXTRA_LOCATE = "locate";
    public static final String REPO_URL = "https://github.com/traique/nhat-nguyet";
    private static final int REQ_ADMIN = 7;
    private static final int REQ_LOCATION = 8;
    private static final int ACCENT = 0xFF5FD3B0;
    private static final int PREVIEW_DELAY_MS = 300;

    private Prefs prefs;
    private Updater updater;
    private final Handler handler = new Handler();
    private float density;

    private EditText place, watchlist, feeds, personalDays;
    private TextView placeState, personalState, updateState, versionText, versionDetail;
    private Button installButton, pinButton, pinRemove;
    private CheckBox alwaysOn, burnIn, warmNight, proximity, proximitySleep, wakeMorning, autoStart, autoUpdate, flipInfo;
    private CheckBox awayEnabled, awayWeekdays, batterySaver;
    private Slider awayStart, awayEnd;
    private LinearLayout awayBox;
    private TextView batteryState;
    /** Đang nạp giá trị đã lưu vào giao diện: bộ lắng nghe không được coi đó là người dùng chọn. */
    private boolean loading;
    private RadioGroup layoutGroup, nightGroup, displayGroup;
    private Slider rotate, fontScale, dayStart, dayEnd, idle, nightBright, dayBright;
    private final List<CheckBox> pageChecks = new ArrayList<CheckBox>();
    private final List<CheckBox> feedChecks = new ArrayList<CheckBox>();
    private final List<StatusRow> statusRows = new ArrayList<StatusRow>();
    private LinearLayout advancedBox;
    private TextView advancedToggle;
    private boolean geocoding;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Runnable locateTimeout;
    private Runnable previewRun;
    private boolean firstSection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        updater = Updater.get(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
        load();
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_LOCATE, false)) useCurrentLocation();
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
        save();
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

        section(root, "Nơi ở");
        LinearLayout placeRow = new LinearLayout(this);
        placeRow.setOrientation(LinearLayout.HORIZONTAL);
        placeRow.setGravity(Gravity.CENTER_VERTICAL);
        place = new EditText(this);
        place.setHint("Thành phố, vd Hà Nội, Đà Nẵng");
        place.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        place.setSingleLine(true);
        place.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        place.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                geocode(true);
                return true;
            }
        });
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
        note(root, "Nơi ở chỉ dùng để lấy thời tiết và chất lượng không khí.");

        section(root, "Ngày giỗ, sinh nhật");
        note(root, "Bảng lịch nhắc trước 7 ngày và đánh dấu chấm đỏ trên lịch tháng.");
        button(root, "+ Thêm ngày", new Runnable() {
            @Override
            public void run() {
                askNewPersonalDay();
            }
        });
        personalDays = input(root, "15/8 âm Giỗ ông nội\n20/11 Sinh nhật mẹ", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        personalDays.setSingleLine(false);
        personalDays.setMinLines(2);
        personalState = note(root, "");
        personalDays.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable e) {
                // Mỗi phím gõ tính lại âm lịch cho mọi mục: chờ người dùng gõ xong một nhịp.
                if (previewRun != null) handler.removeCallbacks(previewRun);
                previewRun = new Runnable() {
                    @Override
                    public void run() {
                        showPersonalDays(personalDays.getText().toString());
                    }
                };
                handler.postDelayed(previewRun, PREVIEW_DELAY_MS);
            }
        });
        note(root, "Mỗi dòng một ngày: ngày/tháng, chữ \"âm\" nếu theo âm lịch, rồi tên. Có thể ghi cả năm.");

        section(root, "Trang hiển thị");
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
                "Tắt màn hình khi không ai chạm (khuyên dùng, đỡ tốn pin)"});
        nightGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (!loading && checkedId == 201 && !AdminReceiver.isActive(SettingsActivity.this)) askAdmin();
            }
        });
        note(root, "Tắt màn hình cần quyền quản trị thiết bị (xin ngay khi chọn). Chưa có quyền thì màn tối đen "
                + "rồi tự tắt theo thời gian chờ của máy, nên đặt thời gian chờ khoảng 15–30 giây. "
                + "Để bảng tự hiện lại khi bật màn, đặt khoá màn hình là \"Không\" hoặc \"Vuốt\".");

        label(root, "Giờ vắng nhà");
        awayEnabled = check(root, "Tắt màn hình khi cả nhà vắng (đi làm, đi học)");
        awayBox = new LinearLayout(this);
        awayBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(awayBox, new LinearLayout.LayoutParams(-1, -2));
        awayStart = new Slider(awayBox, "Vắng từ", 0, 23, 1, hour);
        awayEnd = new Slider(awayBox, "Đến", 1, 24, 1, hour);
        awayWeekdays = check(awayBox, "Chỉ thứ Hai đến thứ Sáu, trừ ngày nghỉ lễ");
        note(awayBox, "Trong giờ này màn tắt khi không ai chạm, như ban đêm, và vẫy tay không bật màn để máy ngủ. "
                + "Về sớm thì bấm nút nguồn. Hết giờ vắng, màn tự bật lại nếu chọn \"Tự bật màn hình\" ở mục dưới.");
        awayEnabled.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                awayBox.setVisibility(on ? View.VISIBLE : View.GONE);
                if (!loading && on && !AdminReceiver.isActive(SettingsActivity.this)) askAdmin();
            }
        });

        section(root, "Pin");
        batterySaver = check(root, "Tiết kiệm pin");
        note(root, "Tải dữ liệu gom một lần mỗi 30 phút, đổi trang chậm hơn (ít nhất 1 phút), đồng hồ lật không chạy hiệu ứng. "
                + "Hợp với máy pin yếu hoặc không cắm sạc.");
        note(root, "Pin nóng từ " + Math.round(BatteryGuard.HOT_C) + "°C hoặc còn dưới " + BatteryGuard.LOW_PCT
                + "% khi không sạc thì bảng lịch tự giảm sáng.");
        batteryState = label(root, "");
        batteryState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        section(root, "Để lịch tự bật lại");
        proximity = check(root, "Vẫy tay qua cảm biến tiệm cận để bật màn hình, trừ ban đêm");
        proximitySleep = check(root, "Che cảm biến khi màn đang sáng để tắt màn hình");
        wakeMorning = check(root, "Tự bật màn hình lúc bắt đầu giờ ngày và khi hết giờ vắng nhà");
        autoStart = check(root, "Tự mở bảng lịch khi khởi động máy");
        note(root, sensorInfo());
        note(root, "Ban đêm và giờ vắng nhà, vẫy tay không bật màn và cảm biến được tắt để máy ngủ, đỡ tốn pin. "
                + "Muốn xem giờ thì chạm màn hình hoặc bấm nút nguồn.");
        label(root, "Quyền cần cấp (chạm dòng chưa có ✓)");
        if (Build.VERSION.SDK_INT >= 23) {
            statusRows.add(new StatusRow(root, "Hiện trên ứng dụng khác (để tự bật lại trên Android 10 trở lên)", new Runnable() {
                @Override
                public void run() {
                    askOverlay();
                }
            }));
            statusRows.add(new StatusRow(root, "Chạy nền, không bị tối ưu pin", new Runnable() {
                @Override
                public void run() {
                    askIgnoreBattery();
                }
            }));
        }
        if (Build.VERSION.SDK_INT >= 33) {
            statusRows.add(new StatusRow(root, "Thông báo của dịch vụ đánh thức", new Runnable() {
                @Override
                public void run() {
                    askNotifications();
                }
            }));
        }
        statusRows.add(new StatusRow(root, "Tắt màn hình (quản trị thiết bị)", new Runnable() {
            @Override
            public void run() {
                toggleAdmin();
            }
        }));
        note(root, "Samsung, Xiaomi, Oppo: thêm Nhật Nguyệt vào danh sách được chạy nền và tự khởi động trong phần quản lý pin của máy.");

        section(root, "Màn hình chính");
        displayGroup = radios(root, 300, new String[]{
                "Bảng lịch đầy đủ",
                "Chỉ đồng hồ lật cỡ lớn"});
        flipInfo = check(root, "Dưới đồng hồ lật hiện ngày, âm lịch và thời tiết");
        displayGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                flipInfo.setVisibility(checkedId == 301 ? View.VISIBLE : View.GONE);
            }
        });

        section(root, "Tin tức");
        note(root, "Bật trang Tin tức ở mục Trang hiển thị rồi chọn báo.");
        for (String[] f : Prefs.FEED_CHOICES) {
            CheckBox c = check(root, f[0]);
            c.setTag(f[1]);
            feedChecks.add(c);
        }

        if (BuildConfig.OTA) {
            section(root, "Cập nhật");
            autoUpdate = check(root, "Tự kiểm tra và tải bản mới từ GitHub");
            button(root, "Kiểm tra cập nhật ngay", new Runnable() {
                @Override
                public void run() {
                    save();
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
            } else {
                note(root, "Bản mới tải xong sẽ mời cài lúc 10 giờ sáng, hoặc chạm dòng trạng thái trên bảng lịch.");
            }
        }

        section(root, "Khoá Cài đặt");
        note(root, "Đặt mã PIN để người khác không đổi được cài đặt khi máy treo ở nơi chung. Quên mã: vào Cài đặt "
                + "của máy > Ứng dụng > Nhật Nguyệt > Xoá dữ liệu (mất hết cài đặt của app).");
        pinButton = button(root, "", new Runnable() {
            @Override
            public void run() {
                askNewPin();
            }
        });
        pinRemove = button(root, "Bỏ mã PIN", new Runnable() {
            @Override
            public void run() {
                prefs.setSettingsPin(null);
                refreshPin();
                Toast.makeText(SettingsActivity.this, "Đã bỏ khoá Cài đặt", Toast.LENGTH_SHORT).show();
            }
        });

        // ---- Nâng cao: thu gọn, ít khi cần chỉnh
        section(root, "Nâng cao");
        advancedToggle = note(root, "");
        advancedToggle.setAlpha(1f);
        advancedToggle.setTextColor(ACCENT);
        advancedToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        advancedToggle.setMinHeight(dp(44));
        advancedToggle.setGravity(Gravity.CENTER_VERTICAL);
        advancedBox = new LinearLayout(this);
        advancedBox.setOrientation(LinearLayout.VERTICAL);
        advancedBox.setVisibility(View.GONE);
        root.addView(advancedBox, new LinearLayout.LayoutParams(-1, -2));
        advancedToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean open = advancedBox.getVisibility() != View.VISIBLE;
                advancedBox.setVisibility(open ? View.VISIBLE : View.GONE);
                advancedToggle.setText(open ? "▾ Ẩn bớt" : "▸ Độ sáng, bố cục, chứng khoán, nguồn RSS khác, thoát bảng lịch");
            }
        });
        advancedToggle.setText("▸ Độ sáng, bố cục, chứng khoán, nguồn RSS khác, thoát bảng lịch");
        buildAdvanced(advancedBox);

        section(root, "Giới thiệu");
        buildAbout(root);

        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true); // Android 15+: không bị thanh trạng thái, thanh điều hướng che
        sv.addView(root);
        return sv;
    }

    private void buildAdvanced(LinearLayout box) {
        label(box, "Độ sáng");
        note(box, "Vuốt lên hoặc xuống trên đồng hồ cũng chỉnh được độ sáng.");
        dayBright = new Slider(box, "Độ sáng ban ngày", 0, 100, 5, new Label() {
            @Override
            public String of(int v) {
                return v == 0 ? "Theo máy" : v + "%";
            }
        });
        nightBright = new Slider(box, "Độ sáng ban đêm", 1, 100, 1, new Label() {
            @Override
            public String of(int v) {
                return v + "%";
            }
        });
        idle = new Slider(box, "Ban đêm, tắt màn sau khi không chạm", 1, 60, 1, new Label() {
            @Override
            public String of(int v) {
                return v + " phút";
            }
        });
        warmNight = check(box, "Ban đêm chuyển chữ sang tông ấm (bớt ánh sáng xanh)");
        burnIn = check(box, "Chống lưu ảnh: dịch nội dung vài điểm ảnh mỗi 2 phút");

        label(box, "Bố cục");
        layoutGroup = radios(box, 100, new String[]{"Tự động theo kích thước máy", "Luôn 2 cột", "3 cột (máy màn dài từ 6 inch)"});

        label(box, "Chứng khoán: mã theo dõi, cách nhau dấu phẩy");
        watchlist = input(box, Prefs.DEFAULT_WATCHLIST, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        note(box, "Chỉ số: VNINDEX, VN30, HNX, HNX30, UPCOM. Để trống thì dùng danh sách mặc định. "
                + "Số liệu lấy từ DNSE/Entrade, chỉ để tham khảo.");

        label(box, "Nguồn tin RSS khác, mỗi dòng một địa chỉ https://");
        feeds = input(box, "https://…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        feeds.setSingleLine(false);
        feeds.setMinLines(2);

        label(box, "Khác");
        versionDetail = note(box, "");
        button(box, "Thoát bảng lịch", new Runnable() {
            @Override
            public void run() {
                MainActivity.exitRequested = true;
                finish();
            }
        });
        note(box, "Phím Back trên bảng lịch bị chặn để không thoát nhầm; dùng nút này khi cần đóng app.");
    }

    private void buildAbout(LinearLayout root) {
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
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        versionText.setAlpha(0.8f);
        info.addView(versionText);
        TextView credit = new TextView(this);
        credit.setText("design by LCTA · mã nguồn mở, giấy phép Apache 2.0");
        credit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        credit.setAlpha(0.6f);
        info.addView(credit);
        about.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(8);
        root.addView(about, ap);

        TextView credits = note(root, "");
        credits.setText(Html.fromHtml("Lịch âm theo thuật toán của Hồ Ngọc Đức. "
                + "Thời tiết, không khí: <a href=\"https://open-meteo.com/\">Open-Meteo.com</a> (CC BY 4.0). "
                + "Tên nơi theo vị trí: © <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors (ODbL). "
                + "Giá vàng: vang.today · Xăng dầu: giaxanghomnay.com · Tỷ giá: Vietcombank · Chứng khoán: DNSE/Entrade (tham khảo) · "
                + "Tin tức: RSS của các báo.<br>"
                + "<a href=\"" + REPO_URL + "/blob/main/PRIVACY.md\">Quyền riêng tư</a> · "
                + "<a href=\"" + REPO_URL + "\">Mã nguồn</a>"));
        credits.setMovementMethod(LinkMovementMethod.getInstance());
        credits.setLinkTextColor(ACCENT);
        button(root, "Giấy phép bên thứ ba", new Runnable() {
            @Override
            public void run() {
                showNotice();
            }
        });
        note(root, "Trên bảng lịch: bấm biểu tượng bánh răng ở góc trên bên phải (hoặc giữ lâu vào đồng hồ) để mở lại trang này. "
                + "Cài đặt tự lưu khi rời trang.");
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
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setAlpha(0.78f);
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
        // Màn ngang nhỏ: không cho bàn phím chiếm toàn màn hình che mất phần xem trước.
        e.setImeOptions(e.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        root.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private CheckBox check(LinearLayout root, String s) {
        CheckBox c = new CheckBox(this);
        c.setText(s);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        c.setMinHeight(dp(48));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(2);
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
            b.setMinHeight(dp(48));
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

    /** Dòng trạng thái quyền: ✓ hoặc ✗ ở đầu, chạm để cấp. */
    private final class StatusRow {
        final TextView view;
        boolean ok;

        StatusRow(LinearLayout root, final String title, final Runnable action) {
            view = new TextView(SettingsActivity.this);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            view.setMinHeight(dp(48));
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setText(title);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    action.run();
                }
            });
            root.addView(view, new LinearLayout.LayoutParams(-1, -2));
            set(false, title);
        }

        void set(boolean ok, String title) {
            this.ok = ok;
            view.setText((ok ? "✓  " : "✗  ") + title);
            view.setTextColor(ok ? ACCENT : 0xFFF2645A);
        }
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
        loading = true;
        try {
            loadValues();
        } finally {
            loading = false;
        }
    }

    private void loadValues() {
        place.setText(prefs.placeName());
        placeState.setText(placeLabel());
        watchlist.setText(prefs.watchlistRaw());
        List<String> feedUrls = prefs.feeds();
        StringBuilder other = new StringBuilder();
        for (String u : feedUrls) {
            boolean known = false;
            for (CheckBox c : feedChecks) {
                if (u.equals(c.getTag())) {
                    c.setChecked(true);
                    known = true;
                }
            }
            if (!known) {
                if (other.length() > 0) other.append('\n');
                other.append(u);
            }
        }
        feeds.setText(other.toString());
        personalDays.setText(prefs.personalDaysRaw());
        showPersonalDays(prefs.personalDaysRaw());
        for (CheckBox c : pageChecks) c.setChecked(prefs.pageEnabled((String) c.getTag()));
        displayGroup.check(prefs.clockOnly() ? 301 : 300);
        flipInfo.setChecked(prefs.flipInfo());
        flipInfo.setVisibility(prefs.clockOnly() ? View.VISIBLE : View.GONE);
        rotate.set(prefs.rotateSeconds());
        String l = prefs.layout();
        layoutGroup.check(Prefs.LAYOUT_2.equals(l) ? 101 : Prefs.LAYOUT_3.equals(l) ? 102 : 100);
        fontScale.set(prefs.fontScale());
        alwaysOn.setChecked(prefs.alwaysOn());
        dayStart.set(prefs.dayStart());
        dayEnd.set(prefs.dayEnd());
        // "Tắt theo thời gian chờ" (bản cũ) và "Tắt hẳn" nay là một lựa chọn: tắt được thì tắt, không thì chờ máy.
        nightGroup.check(Prefs.NIGHT_DIM.equals(prefs.nightMode()) ? 200 : 201);
        idle.set(prefs.idleMinutes());
        nightBright.set(prefs.nightBrightness());
        dayBright.set(prefs.dayBrightness());
        warmNight.setChecked(prefs.warmNight());
        burnIn.setChecked(prefs.burnInShift());
        proximity.setChecked(prefs.proximityWake());
        proximitySleep.setChecked(prefs.proximitySleep());
        wakeMorning.setChecked(prefs.wakeMorning());
        awayEnabled.setChecked(prefs.awayEnabled());
        awayBox.setVisibility(prefs.awayEnabled() ? View.VISIBLE : View.GONE);
        awayStart.set(prefs.awayStart());
        awayEnd.set(prefs.awayEnd());
        awayWeekdays.setChecked(prefs.awayWeekdaysOnly());
        batterySaver.setChecked(prefs.batterySaver());
        autoStart.setChecked(prefs.autoStart());
        if (autoUpdate != null) autoUpdate.setChecked(prefs.autoUpdate());
    }

    /** Xem trước các ngày đã đọc được và lần tới của từng ngày, báo dòng ghi sai. */
    private void showPersonalDays(String text) {
        List<Integer> bad = new ArrayList<Integer>();
        Calendar c = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        for (Lunar.Holiday h : PersonalDays.parse(text, bad)) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(h.name).append(": ").append(h.day).append('/').append(h.month).append(h.lunar ? " âm lịch" : "");
            Lunar.Upcoming u = Lunar.next(h, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR));
            if (u != null) {
                sb.append(u.days == 0 ? " · hôm nay" : " · còn " + u.days + " ngày ("
                        + Fmt.two(u.solar[0]) + "/" + Fmt.two(u.solar[1]) + ")");
            }
        }
        if (!bad.isEmpty()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("Chưa đọc được dòng ").append(android.text.TextUtils.join(", ", bad))
                    .append(". Cách ghi: 15/8 âm Giỗ ông nội");
        }
        personalState.setText(sb.length() > 0 ? sb.toString() : "Chưa có ngày nào.");
    }

    private String placeLabel() {
        return "Đang dùng: " + prefs.placeName();
    }

    private void save() {
        prefs.setWatchlist(watchlist.getText().toString());
        List<String> feedUrls = new ArrayList<String>();
        for (CheckBox c : feedChecks) if (c.isChecked()) feedUrls.add((String) c.getTag());
        List<String> rejected = new ArrayList<String>();
        for (String line : feeds.getText().toString().split("\n")) {
            String t = line.trim();
            if (t.length() == 0) continue;
            if (Prefs.isFeedUrl(t)) feedUrls.add(t);
            else rejected.add(t);
        }
        if (!rejected.isEmpty()) {
            Toast.makeText(this, "Bỏ qua nguồn RSS không phải địa chỉ https://: " + rejected.get(0), Toast.LENGTH_LONG).show();
        }
        prefs.setFeeds(feedUrls);
        String own = personalDays.getText().toString().trim();
        prefs.setPersonalDays(own);
        Lunar.setPersonal(PersonalDays.parse(own, null));
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
        prefs.putString("night_mode", nid == 200 ? Prefs.NIGHT_DIM : Prefs.NIGHT_OFF);
        prefs.putInt("idle_minutes", idle.get());
        prefs.putInt("night_brightness", nightBright.get());
        prefs.putInt("day_brightness", dayBright.get());
        prefs.putBool("warm_night", warmNight.isChecked());
        prefs.putBool("burn_in_shift", burnIn.isChecked());
        prefs.putBool("proximity_wake", proximity.isChecked());
        prefs.putBool("proximity_sleep", proximitySleep.isChecked());
        prefs.putBool("wake_morning", wakeMorning.isChecked());
        prefs.putBool("auto_start", autoStart.isChecked());
        prefs.putBool("away_enabled", awayEnabled.isChecked());
        prefs.putInt("away_start", awayStart.get());
        prefs.putInt("away_end", awayEnd.get());
        prefs.putBool("away_weekdays", awayWeekdays.isChecked());
        prefs.putBool("battery_saver", batterySaver.isChecked());
        if (autoUpdate != null) prefs.putBool("auto_update", autoUpdate.isChecked());

        if (!place.getText().toString().trim().equalsIgnoreCase(prefs.placeName())
                && place.getText().toString().trim().length() > 0) {
            geocode(false);
        }
        DataHub.get(this).settingsChanged();
        WakeService.sync(this);
        Scheduler.schedule(this);
    }

    // ------------------------------------------------------------------ ngày riêng

    /** Hộp thoại thêm ngày giỗ, sinh nhật bằng ô số, không phải gõ cú pháp. */
    private void askNewPersonalDay() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        LinearLayout dm = new LinearLayout(this);
        dm.setOrientation(LinearLayout.HORIZONTAL);
        final EditText day = numberField("Ngày");
        final EditText month = numberField("Tháng");
        dm.addView(day, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -2, 1f);
        mp.leftMargin = dp(12);
        dm.addView(month, mp);
        box.addView(dm);
        final RadioGroup kind = radios(box, 400, new String[]{"Âm lịch", "Dương lịch"});
        kind.setOrientation(RadioGroup.HORIZONTAL);
        ((RadioButton) kind.getChildAt(0)).setChecked(true);
        final EditText name = new EditText(this);
        name.setHint("Tên, vd Giỗ ông nội");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setSingleLine(true);
        name.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        box.addView(name);
        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Thêm ngày giỗ, sinh nhật")
                .setView(box)
                .setPositiveButton("Thêm", null)
                .setNegativeButton("Huỷ", null)
                .create();
        d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        d.show();
        // Gắn lại sau show() để hộp thoại không tự đóng khi nhập sai.
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int kid = kind.getCheckedRadioButtonId();
                String line = day.getText().toString().trim() + "/" + month.getText().toString().trim()
                        + (kid == 400 ? " âm " : " ") + name.getText().toString().trim();
                List<Integer> bad = new ArrayList<Integer>();
                if (PersonalDays.parse(line, bad).isEmpty() || !bad.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "Ngày, tháng hoặc tên chưa đúng", Toast.LENGTH_SHORT).show();
                    return;
                }
                String cur = personalDays.getText().toString().trim();
                personalDays.setText(cur.length() == 0 ? line : cur + "\n" + line);
                d.dismiss();
            }
        });
    }

    private EditText numberField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        return e;
    }

    // ------------------------------------------------------------------ vị trí hiện tại

    private void useCurrentLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
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
        placeState.setText("Chưa được phép dùng vị trí. Có thể nhập tên thành phố rồi bấm Tìm, "
                + "hoặc cấp quyền Vị trí trong Cài đặt của máy > Ứng dụng > Nhật Nguyệt.");
    }

    /** Lấy vị trí gần nhất máy đã biết; nếu cũ quá thì xin một lần mới qua mạng (tối đa 30 giây). */
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
            // Thời tiết chỉ cần cấp thành phố: vị trí qua mạng (Wi‑Fi, trạm phát) là đủ, không cần GPS.
            if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                if (best != null) usePlace(best);
                else placeState.setText("Định vị đang tắt. Hãy bật vị trí trong cài đặt của máy rồi thử lại, hoặc nhập tên thành phố.");
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
                    else placeState.setText("Không lấy được vị trí. Hãy nhập tên thành phố rồi bấm Tìm.");
                }
            };
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
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
                DataHub.get(app).settingsChanged(); // tự chuyển về luồng chính
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
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Object[] r = WeatherSource.geocode(name);
                    new Prefs(app).setPlace((String) r[0], (Double) r[1], (Double) r[2]);
                    DataHub.get(app).settingsChanged(); // tự chuyển về luồng chính
                    msg = null;
                } catch (Exception e) {
                    msg = e.getMessage() != null && e.getMessage().startsWith("Không tìm thấy")
                            ? e.getMessage() : "Chưa tìm được: " + Http.describe(e);
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

    private void refreshPin() {
        boolean on = prefs.hasSettingsPin();
        pinButton.setText(on ? "Đổi mã PIN" : "Đặt mã PIN");
        pinRemove.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private EditText pinField(String hint) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setHint(hint);
        e.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        return e;
    }

    private void askNewPin() {
        final EditText first = pinField("Mã PIN mới, 4–8 chữ số");
        final EditText again = pinField("Nhập lại mã PIN");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(first);
        box.addView(again);
        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Đặt mã PIN cho Cài đặt")
                .setView(box)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Huỷ", null)
                .create();
        d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        d.show();
        // Gắn lại sau show() để hộp thoại không tự đóng khi nhập sai.
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = first.getText().toString();
                if (!pin.matches("\\d{4,8}")) {
                    Toast.makeText(SettingsActivity.this, "Mã PIN cần 4–8 chữ số", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pin.equals(again.getText().toString())) {
                    Toast.makeText(SettingsActivity.this, "Hai lần nhập không khớp", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.setSettingsPin(pin);
                prefs.setPinFails(0, 0);
                refreshPin();
                Toast.makeText(SettingsActivity.this, "Đã khoá Cài đặt bằng mã PIN", Toast.LENGTH_SHORT).show();
                d.dismiss();
            }
        });
    }

    private void refreshStates() {
        refreshPin();
        refreshBattery();
        boolean admin = AdminReceiver.isActive(this);
        for (StatusRow r : statusRows) {
            String t = r.view.getText().toString().substring(3);
            if (t.startsWith("Hiện trên")) r.set(Build.VERSION.SDK_INT >= 23 && canDrawOverlays(), t);
            else if (t.startsWith("Chạy nền")) r.set(Build.VERSION.SDK_INT >= 23 && ignoringBattery(), t);
            else if (t.startsWith("Thông báo")) r.set(Build.VERSION.SDK_INT >= 33 && hasNotifications(), t);
            else if (t.startsWith("Tắt màn hình")) r.set(admin, t);
        }
        versionText.setText("Phiên bản " + BuildConfig.VERSION_NAME);
        versionDetail.setText("Phiên bản " + BuildConfig.VERSION_NAME + " · Android " + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ") · TLS " + Tls.engine() + " · " + Build.MODEL);
        if (updateState != null) {
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
            if (!updater.supported()) st = "Bản này không tự cập nhật.";
            updateState.setText(st);
        }
    }

    /** Pin hiện tại (đọc broadcast "dính", không cần đăng ký lâu dài) và thống kê 24 giờ qua. */
    private void refreshBattery() {
        int pct = -1;
        boolean charging = false;
        float temp = Float.NaN;
        try {
            Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i != null) {
                int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                pct = level < 0 || scale <= 0 ? -1 : Math.round(level * 100f / scale);
                int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                        || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                int t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                temp = t == Integer.MIN_VALUE ? Float.NaN : t / 10f;
            }
        } catch (Throwable ignored) {
        }
        BatteryStats.Summary s = BatteryStats.get(this).summarize(System.currentTimeMillis());
        batteryState.setText(BatteryStats.describe(s, pct, charging, temp));
    }

    @TargetApi(23)
    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(this);
    }

    @TargetApi(23)
    private boolean ignoringBattery() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    @TargetApi(23)
    private boolean hasNotifications() {
        return checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
    }

    private void askAdmin() {
        try {
            startActivityForResult(AdminReceiver.requestIntent(this), REQ_ADMIN);
        } catch (Throwable t) {
            Toast.makeText(this, "Máy không hỗ trợ quyền quản trị; màn sẽ tắt theo thời gian chờ của máy", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleAdmin() {
        if (AdminReceiver.isActive(this)) {
            AdminReceiver.remove(this);
            Toast.makeText(this, "Đã gỡ quyền quản trị thiết bị", Toast.LENGTH_SHORT).show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshStates();
                }
            }, 500);
        } else {
            askAdmin();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshStates();
    }

    private String sensorInfo() {
        Sensor s = WakeService.proximitySensor(this);
        if (s == null) return "Máy này không có cảm biến tiệm cận: chạm màn hình hoặc bấm nút nguồn để bật.";
        return "Cảm biến tiệm cận nằm cạnh loa nghe. Một số máy đời mới chỉ bật cảm biến khi đang gọi, "
                + "khi đó vẫy tay không đánh thức được màn đã tắt.";
    }

    @TargetApi(23)
    private void askIgnoreBattery() {
        if (ignoringBattery()) {
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
        if (canDrawOverlays()) {
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

    /** Giấy phép bên thứ ba: đọc tệp NOTICE đóng gói trong APK. */
    private void showNotice() {
        String text;
        try {
            InputStream in = getAssets().open("NOTICE.txt");
            try {
                text = new String(Http.readAll(in, 256 * 1024), Http.UTF8);
            } finally {
                Http.closeQuietly(in);
            }
        } catch (Exception e) {
            text = "Không đọc được tệp NOTICE.";
        }
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setPadding(dp(16), dp(8), dp(16), dp(8));
        ScrollView sv = new ScrollView(this);
        sv.addView(t);
        new AlertDialog.Builder(this).setTitle("Giấy phép bên thứ ba").setView(sv).setPositiveButton("Đóng", null).show();
    }
}
