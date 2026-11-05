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
    private CheckBox awayEnabled, awayWeekdays, batterySaver, clockDetails;
    private HourChoice awayStart, awayEnd, dayStart, dayEnd;
    private LinearLayout awayBox, dayBox, pagesBox, stocksBox, newsBox;
    private ScrollView content;
    private final List<String> sectionTitles = new ArrayList<String>();
    private final List<View> sectionViews = new ArrayList<View>();
    private final List<TextView> navItems = new ArrayList<TextView>();
    private final java.util.Map<LinearLayout, View> subBoxes = new java.util.HashMap<LinearLayout, View>();
    private StatusRow permOverlay, permBattery, permNotify, permAdmin;
    private TextView permTitle;
    private TextView batteryState;
    /** Đang nạp giá trị đã lưu vào giao diện: bộ lắng nghe không được coi đó là người dùng chọn. */
    private boolean loading;
    private RadioGroup layoutGroup, nightGroup, displayGroup;
    private Slider rotate, fontScale, idle, nightBright, dayBright;
    private final List<CheckBox> pageChecks = new ArrayList<CheckBox>();
    private final List<CheckBox> feedChecks = new ArrayList<CheckBox>();
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
        float widthDp = getResources().getDisplayMetrics().widthPixels / density;
        boolean withNav = widthDp >= 600;
        float contentDp = widthDp - (withNav ? NAV_DP : 0) - 40;
        int cols = contentDp >= 400 ? 2 : 1;

        // ---- Nơi ở
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
        Button here = plainButton("Dùng vị trí hiện tại", new Runnable() {
            @Override
            public void run() {
                useCurrentLocation();
            }
        });
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
        hp.leftMargin = dp(8);
        if (cols > 1) {
            placeRow.addView(here, hp);
            root.addView(placeRow, new LinearLayout.LayoutParams(-1, -2));
        } else {
            root.addView(placeRow, new LinearLayout.LayoutParams(-1, -2));
            root.addView(here, new LinearLayout.LayoutParams(-1, -2));
        }
        placeState = noteAuto(root);
        note(root, "Dùng để lấy thời tiết và chất lượng không khí.");

        // ---- Màn hình: kiểu màn chính, trang xoay vòng, cỡ chữ
        section(root, "Màn hình");
        displayGroup = radios(root, 300, new String[]{"Bảng lịch", "Chỉ đồng hồ lật cỡ lớn"});
        displayGroup.setOrientation(RadioGroup.HORIZONTAL);
        for (int i = 0; i < displayGroup.getChildCount(); i++) {
            displayGroup.getChildAt(i).setLayoutParams(new RadioGroup.LayoutParams(0, -2, 1f));
        }
        flipInfo = check(root, "Dưới đồng hồ lật hiện ngày, âm lịch và thời tiết");
        pagesBox = vbox(root);
        label(pagesBox, "Trang xoay vòng bên phải, chạm để sang trang");
        List<CheckBox> pages = new ArrayList<CheckBox>();
        for (int i = 0; i < Prefs.PAGE_KEYS.length; i++) {
            CheckBox c = checkView(PAGE_SHORT[i]);
            c.setTag(Prefs.PAGE_KEYS[i]);
            pageChecks.add(c);
            pages.add(c);
        }
        grid(pagesBox, pages, cols);
        // Tuỳ chọn riêng của từng trang hiện ngay dưới, chỉ khi trang đó được bật.
        stocksBox = subBox(pagesBox, "Chứng khoán: mã theo dõi, cách nhau dấu phẩy");
        watchlist = input(stocksBox, Prefs.DEFAULT_WATCHLIST, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        note(stocksBox, "Chỉ số: VNINDEX, VN30, HNX, HNX30, UPCOM. Để trống thì dùng mặc định. Số liệu DNSE/Entrade, chỉ để tham khảo.");
        newsBox = subBox(pagesBox, "Tin tức: chọn báo");
        List<CheckBox> feedList = new ArrayList<CheckBox>();
        for (String[] f : Prefs.FEED_CHOICES) {
            CheckBox c = checkView(f[0]);
            c.setTag(f[1]);
            feedChecks.add(c);
            feedList.add(c);
        }
        grid(newsBox, feedList, cols);
        feeds = input(newsBox, "Nguồn RSS khác, mỗi dòng một địa chỉ https://", InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        feeds.setSingleLine(false);
        rotate = new Slider(pagesBox, "Đổi trang sau", 5, 120, 5, new Label() {
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
        displayGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateDependents();
            }
        });
        for (CheckBox c : pageChecks) c.setOnCheckedChangeListener(dependentsListener);

        // ---- Ngày giỗ, sinh nhật
        section(root, "Ngày giỗ, sinh nhật");
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
        note(root, "Mỗi dòng: ngày/tháng, thêm \"âm\" nếu theo âm lịch, rồi tên. Bảng lịch nhắc trước 7 ngày.");

        // ---- Ngày và đêm
        section(root, "Ngày và đêm");
        alwaysOn = check(root, "Luôn sáng cả ngày lẫn đêm");
        alwaysOn.setOnCheckedChangeListener(dependentsListener);
        dayBox = vbox(root);
        LinearLayout dayRow = row(dayBox);
        rowText(dayRow, "Ban ngày từ");
        dayStart = new HourChoice(dayRow, "Ban ngày bắt đầu lúc", 0, 23);
        rowText(dayRow, "đến");
        dayEnd = new HourChoice(dayRow, "Ban ngày kết thúc lúc", 1, 24);
        note(dayBox, "Ban ngày màn hình luôn sáng. Ngoài giờ này là ban đêm.");
        label(dayBox, "Ban đêm");
        nightGroup = radios(dayBox, 200, new String[]{
                "Tắt màn hình khi không ai chạm (khuyên dùng)",
                "Chỉ giảm độ sáng, màn vẫn sáng"});
        nightGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (!loading && checkedId == 200 && !AdminReceiver.isActive(SettingsActivity.this)) askAdmin();
                updateDependents();
            }
        });
        note(dayBox, "Tắt màn hình cần quyền quản trị thiết bị, app sẽ hỏi khi chọn.");

        awayEnabled = check(root, "Tắt màn hình trong giờ cả nhà vắng (đi làm, đi học)");
        awayBox = subBox(root, null);
        LinearLayout awayRow = row(awayBox);
        rowText(awayRow, "Vắng từ");
        awayStart = new HourChoice(awayRow, "Vắng nhà từ", 0, 23);
        rowText(awayRow, "đến");
        awayEnd = new HourChoice(awayRow, "Vắng nhà đến", 1, 24);
        awayWeekdays = check(awayBox, "Chỉ ngày làm việc: thứ Hai đến thứ Sáu, trừ ngày nghỉ lễ, Tết, nghỉ bù");
        note(awayBox, "Trong giờ này màn tắt khi không ai chạm. Về sớm thì bấm nút nguồn.");
        awayEnabled.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                if (!loading && on && !AdminReceiver.isActive(SettingsActivity.this)) askAdmin();
                updateDependents();
            }
        });

        // ---- Tự bật lại
        section(root, "Tự bật lại");
        boolean sensor = WakeService.proximitySensor(this) != null;
        proximity = check(root, "Vẫy tay qua cảm biến để bật màn hình (trừ ban đêm, giờ vắng nhà)");
        proximitySleep = check(root, "Che cảm biến khi màn đang sáng để tắt màn hình");
        if (!sensor) {
            proximity.setVisibility(View.GONE);
            proximitySleep.setVisibility(View.GONE);
        }
        wakeMorning = check(root, "Tự bật màn hình đầu giờ ngày và khi hết giờ vắng nhà");
        autoStart = check(root, "Tự mở bảng lịch khi khởi động máy");
        proximity.setOnCheckedChangeListener(dependentsListener);
        proximitySleep.setOnCheckedChangeListener(dependentsListener);
        wakeMorning.setOnCheckedChangeListener(dependentsListener);
        autoStart.setOnCheckedChangeListener(dependentsListener);
        note(root, sensorInfo());
        permTitle = label(root, "");
        if (Build.VERSION.SDK_INT >= 29) {
            permOverlay = new StatusRow(root, "Hiện trên ứng dụng khác", new Runnable() {
                @Override
                public void run() {
                    askOverlay();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 23) {
            permBattery = new StatusRow(root, "Chạy nền không bị tối ưu pin", new Runnable() {
                @Override
                public void run() {
                    askIgnoreBattery();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 33) {
            permNotify = new StatusRow(root, "Thông báo của dịch vụ cảm biến", new Runnable() {
                @Override
                public void run() {
                    askNotifications();
                }
            });
        }
        permAdmin = new StatusRow(root, "Quản trị thiết bị (tắt hẳn màn)", new Runnable() {
            @Override
            public void run() {
                toggleAdmin();
            }
        });
        note(root, "Khoá màn hình của máy nên đặt \"Không\" hoặc \"Vuốt\" để bảng lịch tự hiện lại. "
                + "Samsung, Xiaomi, Oppo: cho Nhật Nguyệt chạy nền và tự khởi động trong phần quản lý pin của máy.");

        // ---- Pin
        section(root, "Pin");
        batterySaver = check(root, "Tiết kiệm pin");
        note(root, "Tải dữ liệu 30 phút một lần, đổi trang chậm hơn, bỏ hiệu ứng lật.");
        batteryState = label(root, "");
        batteryState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        note(root, "Pin nóng từ " + Math.round(BatteryGuard.HOT_C) + "°C hoặc dưới " + BatteryGuard.LOW_PCT
                + "% khi không sạc thì bảng lịch tự giảm sáng.");

        if (BuildConfig.OTA) {
            section(root, "Cập nhật");
            autoUpdate = check(root, "Tự kiểm tra và tải bản mới từ GitHub");
            updateState = noteAuto(root);
            LinearLayout ur = row(root);
            Button checkNow = plainButton("Kiểm tra ngay", new Runnable() {
                @Override
                public void run() {
                    save();
                    updater.check();
                }
            });
            ur.addView(checkNow, new LinearLayout.LayoutParams(0, -2, 1f));
            installButton = plainButton("Cài bản mới", new Runnable() {
                @Override
                public void run() {
                    updater.install(SettingsActivity.this);
                }
            });
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1f);
            ip.leftMargin = dp(8);
            ur.addView(installButton, ip);
            if (Build.VERSION.SDK_INT < 26) {
                note(root, "Lần đầu cần bật: Cài đặt > Bảo mật > Nguồn không xác định. Mỗi lần cập nhật bấm \"Cài đặt\" một lần.");
            }
        }

        section(root, "Khoá Cài đặt");
        note(root, "Đặt mã PIN để người khác không đổi được cài đặt. Quên mã: Cài đặt của máy > Ứng dụng > "
                + "Nhật Nguyệt > Xoá dữ liệu (mất hết cài đặt của app).");
        LinearLayout pr = row(root);
        pinButton = plainButton("", new Runnable() {
            @Override
            public void run() {
                askNewPin();
            }
        });
        pr.addView(pinButton, new LinearLayout.LayoutParams(0, -2, 1f));
        pinRemove = plainButton("Bỏ mã PIN", new Runnable() {
            @Override
            public void run() {
                prefs.setSettingsPin(null);
                refreshPin();
                Toast.makeText(SettingsActivity.this, "Đã bỏ khoá Cài đặt", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
        rp.leftMargin = dp(8);
        pr.addView(pinRemove, rp);

        // ---- Nâng cao: thu gọn, ít khi cần chỉnh
        section(root, "Nâng cao");
        advancedToggle = note(root, "");
        advancedToggle.setAlpha(1f);
        advancedToggle.setTextColor(ACCENT);
        advancedToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        advancedToggle.setMinHeight(dp(48));
        advancedToggle.setGravity(Gravity.CENTER_VERTICAL);
        advancedBox = vbox(root);
        advancedBox.setVisibility(View.GONE);
        advancedToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean open = advancedBox.getVisibility() != View.VISIBLE;
                advancedBox.setVisibility(open ? View.VISIBLE : View.GONE);
                advancedToggle.setText(open ? "▾ Ẩn bớt" : ADVANCED_HINT);
            }
        });
        advancedToggle.setText(ADVANCED_HINT);
        buildAdvanced(advancedBox);

        section(root, "Giới thiệu");
        buildAbout(root);

        content = new ScrollView(this);
        content.addView(root);
        if (!withNav) {
            content.setFitsSystemWindows(true); // Android 15+: không bị thanh trạng thái, thanh điều hướng che
            return content;
        }
        return withNav(content);
    }

    private static final float NAV_DP = 180;
    private static final String ADVANCED_HINT = "▸ Độ sáng, bố cục, chi tiết dưới đồng hồ, thoát bảng lịch";
    /** Tên ngắn của các trang để xếp hai cột (cùng thứ tự Prefs.PAGE_KEYS). */
    private static final String[] PAGE_SHORT = {"Tờ lịch", "Lịch tháng", "Âm lịch, ngày lễ", "Dự báo thời tiết",
            "Giá vàng", "Giá xăng dầu", "Tỷ giá Vietcombank", "Chứng khoán", "Tin tức"};

    private final android.widget.CompoundButton.OnCheckedChangeListener dependentsListener =
            new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                    updateDependents();
                }
            };

    /** Ẩn, hiện các tuỳ chọn phụ theo tuỳ chọn cha: chỉ thấy cái đang có tác dụng. */
    private void updateDependents() {
        if (pagesBox == null) return;
        int did = displayGroup.getCheckedRadioButtonId();
        boolean flip = did == 301;
        flipInfo.setVisibility(flip ? View.VISIBLE : View.GONE);
        pagesBox.setVisibility(flip ? View.GONE : View.VISIBLE);
        showBox(stocksBox, pageOn("stocks"));
        showBox(newsBox, pageOn("news"));
        dayBox.setVisibility(alwaysOn.isChecked() ? View.GONE : View.VISIBLE);
        showBox(awayBox, awayEnabled.isChecked());
        refreshPermissions();
    }

    /** Khối phụ có vạch bên trái: ẩn cả khung ngoài. */
    private void showBox(LinearLayout box, boolean on) {
        View outer = subBoxes.get(box);
        (outer != null ? outer : box).setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private boolean pageOn(String key) {
        for (CheckBox c : pageChecks) if (key.equals(c.getTag())) return c.isChecked();
        return false;
    }

    /** Mục lục bên trái (màn ngang rộng): chạm để nhảy tới mục, mục đang xem được tô màu. */
    private View withNav(final ScrollView content) {
        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.HORIZONTAL);
        frame.setFitsSystemWindows(true);
        ScrollView navScroll = new ScrollView(this);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(dp(8), dp(8), dp(4), dp(8));
        navScroll.addView(nav);
        for (int i = 0; i < sectionTitles.size(); i++) {
            final View target = sectionViews.get(i);
            TextView t = new TextView(this);
            t.setText(sectionTitles.get(i));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            t.setMinHeight(dp(44));
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(dp(12), 0, dp(8), 0);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    content.smoothScrollTo(0, Math.max(0, target.getTop() - dp(8)));
                }
            });
            nav.addView(t, new LinearLayout.LayoutParams(-1, -2));
            navItems.add(t);
        }
        frame.addView(navScroll, new LinearLayout.LayoutParams(dp(NAV_DP), -1));
        View line = new View(this);
        line.setBackgroundColor(0x22FFFFFF);
        frame.addView(line, new LinearLayout.LayoutParams(Math.max(1, dp(1)), -1));
        frame.addView(content, new LinearLayout.LayoutParams(0, -1, 1f));
        content.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                highlightNav();
            }
        });
        highlightNav();
        return frame;
    }

    private void highlightNav() {
        if (navItems.isEmpty() || content == null) return;
        int y = content.getScrollY() + dp(40);
        int active = 0;
        for (int i = 0; i < sectionViews.size(); i++) if (sectionViews.get(i).getTop() <= y) active = i;
        // Cuộn tới đáy thì mục cuối là mục đang xem.
        View child = content.getChildAt(0);
        if (child != null && content.getScrollY() + content.getHeight() >= child.getHeight() - dp(4)) active = sectionViews.size() - 1;
        for (int i = 0; i < navItems.size(); i++) {
            TextView t = navItems.get(i);
            boolean on = i == active;
            t.setTextColor(on ? ACCENT : 0xB3FFFFFF);
            t.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.setBackgroundColor(on ? 0x1A5FD3B0 : 0);
        }
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
        idle = new Slider(box, "Tắt màn sau khi không chạm", 1, 60, 1, new Label() {
            @Override
            public String of(int v) {
                return v + " phút";
            }
        });
        warmNight = check(box, "Ban đêm chữ tông ấm, bớt ánh sáng xanh");
        burnIn = check(box, "Chống lưu ảnh: dịch nội dung vài điểm ảnh mỗi 2 phút");

        label(box, "Bố cục (3 cột hợp máy màn dài từ 6 inch)");
        layoutGroup = radios(box, 100, new String[]{"Tự động", "2 cột", "3 cột"});
        layoutGroup.setOrientation(RadioGroup.HORIZONTAL);
        for (int i = 0; i < layoutGroup.getChildCount(); i++) {
            layoutGroup.getChildAt(i).setLayoutParams(new RadioGroup.LayoutParams(0, -2, 1f));
        }
        clockDetails = check(box, "Dưới đồng hồ hiện thêm chi tiết (can chi, độ ẩm, dự báo theo giờ…)");

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
        note(root, "Mở lại trang này: bánh răng góc trên bên phải bảng lịch, hoặc giữ lâu vào đồng hồ. Cài đặt tự lưu.");
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
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ACCENT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(firstSection ? 12 : 16);
        lp.bottomMargin = dp(4);
        root.addView(t, lp);
        firstSection = false;
        sectionTitles.add(s);
        sectionViews.add(t);
    }

    private LinearLayout vbox(LinearLayout root) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
        return b;
    }

    /** Khối tuỳ chọn phụ thụt lề, có vạch màu bên trái để thấy nó thuộc tuỳ chọn ngay trên. */
    private LinearLayout subBox(LinearLayout root, String title) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        View bar = new View(this);
        bar.setBackgroundColor(0x665FD3B0);
        outer.addView(bar, new LinearLayout.LayoutParams(dp(3), -1));
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(14), 0, 0, dp(4));
        outer.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        lp.leftMargin = dp(12);
        root.addView(outer, lp);
        if (title != null) label(b, title).setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        subBoxes.put(b, outer);
        return b;
    }

    private LinearLayout row(LinearLayout root) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(r, lp);
        return r;
    }

    private void rowText(LinearLayout row, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setPadding(0, 0, dp(8), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        if (row.getChildCount() > 0) lp.leftMargin = dp(8);
        row.addView(t, lp);
    }

    /** Xếp các ô chọn thành lưới cols cột. */
    private void grid(LinearLayout root, List<CheckBox> items, int cols) {
        LinearLayout r = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % cols == 0) {
                r = new LinearLayout(this);
                r.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(r, new LinearLayout.LayoutParams(-1, -2));
            }
            r.addView(items.get(i), new LinearLayout.LayoutParams(0, -2, 1f));
        }
        if (r != null) for (int k = items.size() % cols; k > 0 && k < cols; k++) r.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));
    }

    /** Ghi chú tự ẩn khi không có chữ (dòng trạng thái tìm nơi ở, cập nhật). */
    private TextView noteAuto(LinearLayout root) {
        TextView t = new TextView(this) {
            @Override
            public void setText(CharSequence text, BufferType type) {
                super.setText(text, type);
                setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
            }
        };
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setAlpha(0.85f);
        t.setText("");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        root.addView(t, lp);
        return t;
    }

    /** Ô chọn giờ: bấm để chọn trong danh sách giờ, thay cho thanh trượt khó kéo đúng trên màn nhỏ. */
    private final class HourChoice {
        final TextView view;
        final int min, max;
        int value;

        HourChoice(LinearLayout row, final String title, int min, int max) {
            this.min = min;
            this.max = max;
            view = new TextView(SettingsActivity.this);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            view.setTypeface(Typeface.DEFAULT_BOLD);
            view.setTextColor(ACCENT);
            view.setGravity(Gravity.CENTER);
            view.setMinHeight(dp(44));
            view.setMinWidth(dp(76));
            view.setPadding(dp(12), 0, dp(12), 0);
            view.setBackgroundColor(0x1F5FD3B0);
            view.setContentDescription(title);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pick(title);
                }
            });
            row.addView(view, new LinearLayout.LayoutParams(-2, -2));
        }

        void pick(String title) {
            final String[] items = new String[max - min + 1];
            for (int i = 0; i < items.length; i++) items[i] = (min + i) + ":00";
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(title)
                    .setSingleChoiceItems(items, value - min, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            set(min + which);
                            d.dismiss();
                        }
                    })
                    .show();
        }

        int get() {
            return value;
        }

        void set(int v) {
            value = Math.max(min, Math.min(max, v));
            view.setText(value + ":00");
        }
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
        CheckBox c = checkView(s);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(2);
        root.addView(c, lp);
        return c;
    }

    private CheckBox checkView(String s) {
        CheckBox c = new CheckBox(this);
        c.setText(s);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        c.setMinHeight(dp(48));
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
            this.title = title;
            set(false, title);
        }

        final String title;

        /** need = tính năng đang bật có cần quyền này; không cần thì ẩn, đã cấp thì hiện ✓. */
        void show(boolean need, boolean granted) {
            view.setVisibility(need ? View.VISIBLE : View.GONE);
            set(granted, title);
        }

        void set(boolean ok, String title) {
            this.ok = ok;
            view.setText(ok ? "✓  " + title : "●  " + title + " · chạm để cấp");
            view.setTextColor(ok ? ACCENT : 0xFFF2B24A);
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
        rotate.set(prefs.rotateSeconds());
        String l = prefs.layout();
        layoutGroup.check(Prefs.LAYOUT_2.equals(l) ? 101 : Prefs.LAYOUT_3.equals(l) ? 102 : 100);
        fontScale.set(prefs.fontScale());
        alwaysOn.setChecked(prefs.alwaysOn());
        dayStart.set(prefs.dayStart());
        dayEnd.set(prefs.dayEnd());
        // "Tắt theo thời gian chờ" (bản cũ) và "Tắt hẳn" nay là một lựa chọn: tắt được thì tắt, không thì chờ máy.
        nightGroup.check(Prefs.NIGHT_DIM.equals(prefs.nightMode()) ? 201 : 200);
        idle.set(prefs.idleMinutes());
        nightBright.set(prefs.nightBrightness());
        dayBright.set(prefs.dayBrightness());
        warmNight.setChecked(prefs.warmNight());
        burnIn.setChecked(prefs.burnInShift());
        proximity.setChecked(prefs.proximityWake());
        proximitySleep.setChecked(prefs.proximitySleep());
        wakeMorning.setChecked(prefs.wakeMorning());
        awayEnabled.setChecked(prefs.awayEnabled());
        awayStart.set(prefs.awayStart());
        awayEnd.set(prefs.awayEnd());
        awayWeekdays.setChecked(prefs.awayWeekdaysOnly());
        batterySaver.setChecked(prefs.batterySaver());
        clockDetails.setChecked(prefs.clockDetails());
        autoStart.setChecked(prefs.autoStart());
        if (autoUpdate != null) autoUpdate.setChecked(prefs.autoUpdate());
        updateDependents();
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
        return "Đã chọn " + prefs.placeName() + ".";
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
        prefs.putString("night_mode", nid == 201 ? Prefs.NIGHT_DIM : Prefs.NIGHT_OFF);
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
        prefs.putBool("clock_details", clockDetails.isChecked());
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
        refreshPermissions();
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

    /**
     * Chỉ liệt kê quyền mà tính năng đang bật thật sự cần: tự mở lại (Android 10+) cần "hiện trên ứng dụng khác";
     * cảm biến, báo thức cần chạy nền; dịch vụ cảm biến cần thông báo (Android 13+); tắt hẳn màn cần quản trị.
     */
    private void refreshPermissions() {
        if (permAdmin == null || alwaysOn == null) return;
        boolean wave = proximity.getVisibility() == View.VISIBLE && (proximity.isChecked() || proximitySleep.isChecked());
        boolean reopen = autoStart.isChecked() || wakeMorning.isChecked() || (wave && proximity.isChecked());
        int nid = nightGroup.getCheckedRadioButtonId();
        boolean screenOff = (!alwaysOn.isChecked() && nid == 200) || awayEnabled.isChecked();
        int need = 0, missing = 0;
        if (permOverlay != null) {
            boolean ok = canDrawOverlays();
            permOverlay.show(reopen, ok);
            if (reopen) { need++; if (!ok) missing++; }
        }
        if (permBattery != null) {
            boolean n = wave || wakeMorning.isChecked();
            boolean ok = ignoringBattery();
            permBattery.show(n, ok);
            if (n) { need++; if (!ok) missing++; }
        }
        if (permNotify != null) {
            boolean ok = hasNotifications();
            permNotify.show(wave, ok);
            if (wave) { need++; if (!ok) missing++; }
        }
        boolean admin = AdminReceiver.isActive(this);
        permAdmin.show(screenOff || admin, admin);
        if (screenOff) { need++; if (!admin) missing++; }
        permTitle.setVisibility(need > 0 || admin ? View.VISIBLE : View.GONE);
        permTitle.setText(missing == 0 ? "Quyền cần thiết: đã đủ" : "Quyền cần thiết: còn " + missing + " quyền chưa cấp");
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
        if (s == null) return "Máy này không có cảm biến tiệm cận: chạm màn hình hoặc bấm nút nguồn để bật màn.";
        return "Cảm biến nằm cạnh loa nghe. Một số máy đời mới chỉ bật cảm biến khi đang gọi.";
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
