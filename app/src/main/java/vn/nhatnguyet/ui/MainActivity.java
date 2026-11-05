package vn.nhatnguyet.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.nhatnguyet.BuildConfig;
import vn.nhatnguyet.Fmt;
import vn.nhatnguyet.Prefs;
import vn.nhatnguyet.Theme;
import vn.nhatnguyet.data.DataHub;
import vn.nhatnguyet.data.Source;
import vn.nhatnguyet.data.WeatherCodes;
import vn.nhatnguyet.data.WeatherSource;
import vn.nhatnguyet.lunar.Lunar;
import vn.nhatnguyet.lunar.LunarDate;
import vn.nhatnguyet.net.ClockCheck;
import vn.nhatnguyet.sys.AdminReceiver;
import vn.nhatnguyet.sys.Scheduler;
import vn.nhatnguyet.sys.WakeService;
import vn.nhatnguyet.update.Updater;

/**
 * Bảng lịch toàn màn hình, nằm ngang. Bố cục tự chọn theo khung thật:
 * 2 cột (giờ + âm lịch | trang xoay vòng) hoặc 3 cột trên máy dài (thêm lịch tháng cố định).
 */
public class MainActivity extends Activity implements DataHub.Listener, Updater.Listener {
    public static final String EXTRA_WAKE = "wake";
    private static final long TOUCH_PAUSE_MS = 60_000L;
    private static final long POLICY_MS = 15_000L;
    private static final long SHIFT_MS = 120_000L;
    private static final String[] DATA_KEYS = {"weather", "gold", "fuel", "fx", "stocks", "news"};
    /** Giờ máy trước ngày build (trừ 1 ngày) chắc chắn sai (máy cũ hết pin hay về năm 2012). */
    private static final long MIN_SANE_TIME = BuildConfig.BUILD_TIME - 86_400_000L;
    private static final long BACK_TWICE_MS = 2_000L;
    private static final int PIN_MAX_FAILS = 5;
    private static final long PIN_LOCK_MS = 30_000L;

    /** Cài đặt bấm "Thoát bảng lịch": đóng app khi quay lại (phím Back bị chặn để không thoát nhầm). */
    public static volatile boolean exitRequested;

    private Prefs prefs;
    private DataHub hub;
    private Updater updater;
    private final Handler handler = new Handler();

    private BoardRoot root;
    private FrameLayout content;
    private Ui ui;
    private Pages pages;
    private int cols;
    private boolean narrowRight;

    private TextView clock, tempText;
    private FitLine dateLine, lunarLine, canChiLine, eventLine, descText, detailText, alertLine, airLine, statusText;
    private boolean clockWrong;
    private WeatherIcon nowIcon;
    private BatteryView battView;
    private boolean flipMode;
    private boolean flipNarrow;
    private FlipCard flipHours, flipMinutes;
    private TextView flipDate, flipLunar, flipTemp, flipDesc;
    private WeatherIcon flipIcon;
    private TextView battText;
    private int battPct = -1;
    private boolean battCharging;
    private float battTemp = Float.NaN;
    private LinearLayout hoursRow;
    private int hoursCount;

    private MonthView midMonth;
    private FitLine midTitle;
    private LinearLayout midExtra;

    private FitLine pageTitle;
    private LinearLayout headerExtra;
    private PageDots dots;
    private FrameLayout pageBox;
    private final List<String> pageKeys = new ArrayList<String>();
    private int page;
    private String currentKey = "";
    private int newsOffset;
    private FitColumn newsView;
    private int monthShift;

    private BrightnessHud hud;
    /** Phủ đen giả tắt màn (khi che cảm biến mà chưa có quyền quản trị); máy tự tắt theo thời gian chờ. */
    private View shade;
    private boolean blackout;
    private boolean swallowTouch;
    /** Độ sáng (%) đang kéo trên đồng hồ, -1 khi không kéo. */
    private int liveBrightness = -1;

    private long lastInteraction;
    private long lastTouch;
    private boolean resumed;
    private boolean lockedThisIdle;
    /** Trạng thái FLAG_KEEP_SCREEN_ON đã đặt, để không gọi addFlags/clearFlags thừa mỗi 15 giây. */
    private Boolean keepOnApplied;
    private long lastBack;
    /** Android 10+ chưa cấp "hiện trên ứng dụng khác" trong khi có tuỳ chọn tự bật lại đang bật. */
    private boolean overlayMissing;
    private boolean setupDialogShown;
    private int shiftStep;
    private String dayKey = "";
    private final Map<String, String> rendered = new HashMap<String, String>();

    // ------------------------------------------------------------------ vòng đời

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        Theme.loadFonts(this);
        hub = DataHub.get(this);
        updater = Updater.get(this);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT < 27) {
            // Từ API 27 dùng showWhenLocked/turnScreenOn trong manifest.
            w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        // Activity mới thì chưa phủ đen; cờ tĩnh của WakeService phải theo.
        blackout = false;
        WakeService.blackout = false;
        root = new BoardRoot(this);
        root.setBackgroundColor(Theme.BG);
        root.onSize = new Runnable() {
            @Override
            public void run() {
                rebuild();
            }
        };
        setContentView(root);
        hideSystemUi();

        WakeService.sync(this);
        Scheduler.schedule(this);
        handleIntent(getIntent());

    }

    /** Lần đầu mở app: hỏi nơi ở (thời tiết đúng chỗ ngay từ đầu) và chỉ cách mở Cài đặt. */
    private void maybeShowSetup() {
        if (prefs.setupDone() || setupDialogShown) return;
        setupDialogShown = true;
        final EditText city = new EditText(this);
        city.setHint("Thành phố, vd Hà Nội, Cần Thơ");
        city.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        city.setSingleLine(true);
        city.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        FrameLayout box = new FrameLayout(this);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(city);
        AlertDialog d = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Nhà mình ở đâu?")
                .setMessage("Để hiện thời tiết đúng nơi. Sau này đổi trong Cài đặt (bánh răng góc trên bên phải, "
                        + "hoặc giữ lâu vào đồng hồ). Vuốt dọc trên đồng hồ để chỉnh độ sáng.")
                .setView(box)
                .setPositiveButton("Xong", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.setSetupDone();
                        String name = city.getText().toString().trim();
                        if (name.length() > 0) geocodeFirstPlace(name);
                    }
                })
                .setNeutralButton("Dùng vị trí hiện tại", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.setSetupDone();
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class).putExtra(SettingsActivity.EXTRA_LOCATE, true));
                    }
                })
                .setNegativeButton("Để sau", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.setSetupDone();
                    }
                })
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                touch();
            }
        });
        d.show();
    }

    private void geocodeFirstPlace(final String name) {
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Object[] r = WeatherSource.geocode(name);
                    new Prefs(app).setPlace((String) r[0], (Double) r[1], (Double) r[2]);
                    DataHub.get(app).settingsChanged();
                    msg = "Thời tiết theo " + r[0];
                } catch (Exception e) {
                    msg = "Chưa tìm được \"" + name + "\". Vào Cài đặt để chọn lại nơi ở.";
                }
                final String m = msg;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(app, m, Toast.LENGTH_LONG).show();
                        if (resumed) renderStatus();
                    }
                });
            }
        }, "nn-geocode").start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exitRequested) {
            exitRequested = false;
            finish();
            return;
        }
        resumed = true;
        hub.addListener(this);
        updater.addListener(this);
        WakeService.onWave = new Runnable() {
            @Override
            public void run() {
                touch();
            }
        };
        WakeService.onCover = new Runnable() {
            @Override
            public void run() {
                sleepScreen();
            }
        };
        hub.start();
        try {
            // ACTION_BATTERY_CHANGED là broadcast "dính": nhận ngay giá trị hiện tại.
            readBattery(registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        } catch (Throwable ignored) {
        }
        try {
            IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_CHANGED);
            tf.addAction(Intent.ACTION_DATE_CHANGED);
            tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            registerReceiver(timeReceiver, tf);
        } catch (Throwable ignored) {
        }
        keepOnApplied = null;
        checkOverlay();
        touch();
        lastTouch = 0;
        if (root.getWidth() > 0) rebuild();
        handler.removeCallbacksAndMessages(null);
        scheduleClock();
        handler.postDelayed(policyTick, POLICY_MS);
        handler.postDelayed(rotateTick, prefs.rotateSeconds() * 1000L);
        handler.postDelayed(shiftTick, SHIFT_MS);
        maybeShowSetup();
    }

    /** Android 10+ chặn mở Activity từ nền nếu thiếu quyền "hiện trên ứng dụng khác". */
    @SuppressLint("NewApi")
    private void checkOverlay() {
        boolean need = prefs.autoStart() || prefs.wakeMorning() || prefs.proximityWake();
        overlayMissing = Build.VERSION.SDK_INT >= 29 && need && !Settings.canDrawOverlays(this);
    }

    /** Người dùng chỉnh giờ máy: bỏ các mẫu đo cũ, kiểm lại ngay. */
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ClockCheck.reset();
            clockWrong = false;
            if (resumed) {
                renderClock();
                hub.refreshAll();
            }
        }
    };

    @Override
    public void onBackPressed() {
        // Máy treo tường: một lần bấm nhầm Back không được làm bảng lịch biến mất.
        if (prefs.hasSettingsPin()) {
            Toast.makeText(this, "Cài đặt đang khoá. Thoát bảng lịch trong Cài đặt > Nâng cao.", Toast.LENGTH_SHORT).show();
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastBack < BACK_TWICE_MS) {
            super.onBackPressed();
            return;
        }
        lastBack = now;
        Toast.makeText(this, "Bấm Back lần nữa để thoát bảng lịch", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        resumed = false;
        hub.removeListener(this);
        updater.removeListener(this);
        hub.stop();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Throwable ignored) {
        }
        try {
            unregisterReceiver(timeReceiver);
        } catch (Throwable ignored) {
        }
        WakeService.onWave = null;
        WakeService.onCover = null;
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_WAKE, false)) touch();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideSystemUi();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // Đang phủ đen: lần chạm này chỉ để bật lại màn, không bấm vào gì bên dưới.
            swallowTouch = blackout;
            lastTouch = SystemClock.elapsedRealtime();
            touch();
        }
        if (swallowTouch) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) swallowTouch = false;
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        View d = getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        if (Build.VERSION.SDK_INT >= 19) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        d.setSystemUiVisibility(flags);
    }

    private void openSettings() {
        if (!prefs.hasSettingsPin()) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        long wait = prefs.pinLockUntil() - System.currentTimeMillis();
        if (wait > 0) {
            Toast.makeText(this, "Sai mã PIN nhiều lần, thử lại sau " + (wait / 1000 + 1) + " giây", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText pin = new EditText(this);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pin.setHint("Mã PIN");
        FrameLayout box = new FrameLayout(this);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(pin);
        final AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Cài đặt đang khoá")
                .setView(box)
                .setPositiveButton("Mở", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        unlockSettings(pin.getText().toString());
                    }
                })
                .setNegativeButton("Huỷ", null)
                .create();
        pin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                dialog.dismiss();
                unlockSettings(pin.getText().toString());
                return true;
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        // Đang nhập PIN cũng là đang dùng máy: không để ban đêm tắt màn giữa chừng.
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                touch();
            }
        });
        touch();
        dialog.show();
    }

    private void unlockSettings(String pin) {
        if (prefs.checkSettingsPin(pin)) {
            prefs.setPinFails(0, 0);
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        int fails = prefs.pinFails() + 1;
        if (fails >= PIN_MAX_FAILS) {
            prefs.setPinFails(0, System.currentTimeMillis() + PIN_LOCK_MS);
            Toast.makeText(this, "Sai mã PIN " + fails + " lần, chờ " + (PIN_LOCK_MS / 1000) + " giây", Toast.LENGTH_SHORT).show();
        } else {
            prefs.setPinFails(fails, 0);
            Toast.makeText(this, "Sai mã PIN", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ hẹn giờ

    private void scheduleClock() {
        long now = System.currentTimeMillis();
        handler.postDelayed(clockTick, 60_000L - (now % 60_000L) + 30);
    }

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (!resumed) return;
            renderClock();
            scheduleClock();
        }
    };

    private final Runnable policyTick = new Runnable() {
        @Override
        public void run() {
            if (!resumed) return;
            applyScreenPolicy();
            if (Theme.setWarm(wantWarm())) rebuild();
            updater.maybeAutoCheck();
            maybeAutoInstall();
            handler.postDelayed(this, POLICY_MS);
        }
    };

    private final Runnable rotateTick = new Runnable() {
        @Override
        public void run() {
            if (!resumed) return;
            boolean idle = SystemClock.elapsedRealtime() - lastTouch >= TOUCH_PAUSE_MS;
            if (idle) {
                if (monthShift != 0 && cols == 3) {
                    monthShift = 0;
                    renderMiddle();
                }
                if (pageKeys.size() > 1) nextPage();
                else if (monthShift != 0) {
                    monthShift = 0;
                    renderPage();
                }
            }
            handler.postDelayed(this, Math.max(5, prefs.rotateSeconds()) * 1000L);
        }
    };

    private final Runnable shiftTick = new Runnable() {
        @Override
        public void run() {
            if (!resumed) return;
            if (content != null && ui != null) {
                if (prefs.burnInShift()) {
                    shiftStep = (shiftStep + 1) % 9;
                    int dx = shiftStep % 3 - 1;
                    int dy = shiftStep / 3 - 1;
                    content.setTranslationX(dx * ui.px(2));
                    content.setTranslationY(dy * ui.px(2));
                } else {
                    content.setTranslationX(0);
                    content.setTranslationY(0);
                }
            }
            handler.postDelayed(this, SHIFT_MS);
        }
    };

    // ------------------------------------------------------------------ màn hình ngày/đêm

    private void touch() {
        lastInteraction = SystemClock.elapsedRealtime();
        lockedThisIdle = false;
        if (blackout) setBlackout(false);
        applyScreenPolicy();
    }

    /** Che cảm biến khi màn đang sáng: có quyền quản trị thì tắt hẳn, không thì phủ đen rồi để máy tự tắt. */
    private void sleepScreen() {
        if (!prefs.proximitySleep() || AdminReceiver.lockNow(this)) return;
        setBlackout(true);
        applyScreenPolicy();
    }

    private void setBlackout(boolean on) {
        if (blackout == on && shade != null && (shade.getVisibility() == View.VISIBLE) == on) return;
        blackout = on;
        WakeService.blackout = on;
        if (shade != null) shade.setVisibility(on ? View.VISIBLE : View.GONE);
        // Phủ đen chờ máy tự tắt: không xoay trang, không dịch chống lưu ảnh cho đỡ tốn CPU.
        handler.removeCallbacks(rotateTick);
        handler.removeCallbacks(shiftTick);
        if (!on && resumed) {
            handler.postDelayed(rotateTick, Math.max(5, prefs.rotateSeconds()) * 1000L);
            handler.postDelayed(shiftTick, SHIFT_MS);
        }
    }

    private void applyScreenPolicy() {
        boolean night = prefs.isNight(Calendar.getInstance());
        if (!night) lockedThisIdle = false;
        NightPolicy p = NightPolicy.decide(night, prefs.nightMode(), SystemClock.elapsedRealtime() - lastInteraction,
                prefs.idleMinutes() * 60_000L, prefs.dayBrightness(), prefs.nightBrightness(), liveBrightness,
                blackout, lockedThisIdle);
        if (p.lockNow && resumed) {
            lockedThisIdle = true;
            AdminReceiver.lockNow(this);
        }
        boolean keepOn = p.keepOn;
        float bright = p.brightness;
        Window w = getWindow();
        if (keepOnApplied == null || keepOnApplied != keepOn) {
            // Mỗi lần addFlags/clearFlags là một lượt bố cục lại cửa sổ: chỉ gọi khi cờ đổi.
            if (keepOn) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            keepOnApplied = keepOn;
        }
        WindowManager.LayoutParams lp = w.getAttributes();
        if (lp.screenBrightness != bright) {
            lp.screenBrightness = bright;
            w.setAttributes(lp);
        }
    }

    /** Vuốt dọc: lên sáng hơn, xuống tối hơn; lưu vào độ sáng ngày hoặc đêm tuỳ lúc đang vuốt. */
    private View.OnTouchListener brightnessSwipe() {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        return new View.OnTouchListener() {
            private float startY;
            private int startPct;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = e.getRawY();
                        startPct = currentBrightness();
                        dragging = false;
                        // View giữ lâu được (mở Cài đặt) tự nhận chuỗi chạm; view thường thì nhận ở đây.
                        return !v.isLongClickable();
                    case MotionEvent.ACTION_MOVE:
                        float dy = e.getRawY() - startY;
                        if (!dragging && Math.abs(dy) > slop) {
                            dragging = true;
                            v.cancelLongPress();
                        }
                        if (dragging) {
                            int pct = Math.round(startPct - dy / (root.getHeight() * 0.7f) * 100f);
                            liveBrightness = Math.max(1, Math.min(100, pct));
                            applyScreenPolicy();
                            hud.show(liveBrightness);
                        }
                        return dragging;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!dragging) return false;
                        dragging = false;
                        v.setPressed(false);
                        prefs.putInt(prefs.isNight(Calendar.getInstance()) ? "night_brightness" : "day_brightness", liveBrightness);
                        liveBrightness = -1;
                        hud.hideSoon();
                        return true;
                    default:
                        return dragging;
                }
            }
        };
    }

    private int currentBrightness() {
        if (prefs.isNight(Calendar.getInstance())) return prefs.nightBrightness();
        if (prefs.dayBrightness() > 0) return prefs.dayBrightness();
        // Đang theo độ sáng của máy: bắt đầu kéo từ mức hệ thống (0–255).
        int sys = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
        return Math.max(1, Math.min(100, Math.round(sys * 100f / 255f)));
    }

    private boolean wantWarm() {
        return prefs.warmNight() && !prefs.isDaytime(Calendar.getInstance());
    }

    @SuppressLint("NewApi")
    private void maybeAutoInstall() {
        if (!updater.ready()) return;
        // Chưa được phép cài: không tự nhảy sang màn Cài đặt của máy (máy treo tường sẽ kẹt ở đó);
        // dòng trạng thái vẫn ghi "chạm để cài", người dùng chạm mới mở màn cấp quyền.
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) return;
        int hour = prefs.updateInstallHour();
        Calendar c = Calendar.getInstance();
        if (hour < 0 || c.get(Calendar.HOUR_OF_DAY) != hour) return;
        String today = Fmt.ddmmyyyy(c);
        if (today.equals(prefs.raw().getString("update_prompt_day", ""))) return;
        prefs.putString("update_prompt_day", today);
        updater.install(this);
    }

    // ------------------------------------------------------------------ dựng bố cục

    private int decideCols(float wDp, float hDp) {
        String l = prefs.layout();
        if (Prefs.LAYOUT_2.equals(l)) return 2;
        if (Prefs.LAYOUT_3.equals(l)) return wDp >= 560 ? 3 : 2;
        return (wDp >= 740 && wDp / hDp >= 1.85f) ? 3 : 2;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void rebuild() {
        int w = root.getWidth();
        int h = root.getHeight();
        if (w == 0 || h == 0) return;
        float density = getResources().getDisplayMetrics().density;
        float wDp = w / density;
        float hDp = h / density;
        boolean portrait = hDp > wDp * 1.05f;
        float shortSide = portrait ? wDp : hDp;
        float u = clamp(shortSide / 320f, 0.85f, 1.75f);
        if (!portrait && wDp / hDp < 1.45f) u *= 0.94f;
        cols = portrait ? 1 : decideCols(wDp, hDp);
        if (cols == 3) u *= 0.92f;
        u *= clamp(prefs.fontScale() / 100f, 0.7f, 1.6f);
        ui = new Ui(this, u);
        Theme.setWarm(wantWarm());

        root.removeAllViews();
        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        hud = new BrightnessHud(this, ui);
        root.addView(hud, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        shade = new View(this);
        shade.setBackgroundColor(Theme.BG);
        shade.setVisibility(blackout ? View.VISIBLE : View.GONE);
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        clearViews();
        flipMode = prefs.clockOnly();
        if (flipMode) {
            buildFlip(w, h);
            pageKeys.clear();
            rendered.clear();
            dayKey = "";
            renderClock();
            renderWeather();
            renderStatus();
            return;
        }
        int pad = ui.px(8);
        int gap = ui.px(8);
        LinearLayout board = new LinearLayout(this);
        board.setOrientation(portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        board.setPadding(pad, pad, pad, pad);
        content.addView(board, new FrameLayout.LayoutParams(-1, -1));

        float innerW = w - 2 * pad;
        float innerH = h - 2 * pad;
        float leftFrac;
        if (portrait) leftFrac = 1f;
        else if (cols == 3) leftFrac = 0.28f;
        else leftFrac = wDp / hDp >= 1.9f ? 0.38f : 0.42f;
        float leftW = portrait ? innerW : (innerW - gap * (cols - 1)) * leftFrac;
        float rightW;
        if (portrait) rightW = innerW;
        else if (cols == 3) rightW = (innerW - 2 * gap) * 0.39f;
        else rightW = (innerW - gap) * (1f - leftFrac);
        rightW -= 2 * ui.px(10);
        pages = new Pages(ui, hub, rightW);
        narrowRight = rightW / (ui.density * ui.u) < 280;
        float clockH = portrait ? innerH * 0.42f * 0.38f : innerH * (cols == 3 ? 0.27f : 0.29f);
        View left = buildLeft(leftW, clockH);
        View right = buildRight();

        if (portrait) {
            board.addView(left, new LinearLayout.LayoutParams(-1, 0, 0.42f));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, 0, 0.58f);
            rp.topMargin = gap;
            board.addView(right, rp);
        } else if (cols == 3) {
            board.addView(left, new LinearLayout.LayoutParams(0, -1, 0.28f));
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -1, 0.33f);
            mp.leftMargin = gap;
            board.addView(buildMiddle(), mp);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, 0.39f);
            rp.leftMargin = gap;
            board.addView(right, rp);
        } else {
            board.addView(left, new LinearLayout.LayoutParams(0, -1, leftFrac));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, 1f - leftFrac);
            rp.leftMargin = gap;
            board.addView(right, rp);
        }

        pageKeys.clear();
        for (String k : prefs.enabledPages()) {
            if (cols == 3 && "month".equals(k)) continue;
            pageKeys.add(k);
        }
        if (pageKeys.isEmpty()) pageKeys.add("sheet");
        if (page >= pageKeys.size()) page = 0;
        rendered.clear();
        dayKey = "";
        renderClock();
        renderWeather();
        renderStatus();
        renderPage();
    }

    private static float fitText(Typeface tf, String sample, float maxW, float maxH) {
        Paint p = new Paint();
        p.setTypeface(tf);
        p.setTextSize(100);
        float w100 = p.measureText(sample);
        Paint.FontMetrics fm = p.getFontMetrics();
        float h100 = fm.descent - fm.ascent;
        return Math.max(10f, Math.min(100f * maxW / w100, 100f * maxH / h100));
    }

    private FitLine line(float sizeDp, boolean bold) {
        return new FitLine(this, ui.pxf(sizeDp), bold);
    }

    private View buildLeft(float widthPx, float clockH) {
        FitColumn col = new FitColumn(this);
        clock = new TextView(this);
        clock.setTypeface(Theme.bold());
        clock.setTextColor(Theme.TEXT);
        clock.setIncludeFontPadding(false);
        clock.setSingleLine(true);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, fitText(Theme.bold(), "88:88", widthPx * 0.97f, clockH));
        clock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openSettings();
                return true;
            }
        });
        col.add(clock, 0);

        // Mỗi dữ kiện một dòng; thiếu chỗ thì ẩn dòng kém quan trọng (số ưu tiên lớn) trước.
        dateLine = line(19, false);
        col.add(dateLine, 0, ui.px(2));
        lunarLine = line(18, true);
        col.add(lunarLine, 1, ui.px(6));
        canChiLine = line(14, false);
        col.add(canChiLine, 7, ui.px(4));
        eventLine = line(14.5f, false);
        col.add(eventLine, 3, ui.px(4));

        LinearLayout wx = ui.row();
        nowIcon = new WeatherIcon(this, ui.px(40));
        wx.addView(nowIcon);
        tempText = ui.text(30, Theme.TEXT, true);
        tempText.setPadding(ui.px(6), 0, ui.px(8), 0);
        wx.addView(tempText);
        descText = line(15, false);
        wx.addView(descText, Ui.weight(1));
        col.add(wx, 2, ui.px(10));

        alertLine = line(14, true);
        alertLine.setVisibility(View.GONE);
        col.add(alertLine, 3, ui.px(4));
        detailText = line(13.5f, false);
        col.add(detailText, 5, ui.px(4));
        airLine = line(13.5f, false);
        airLine.setVisibility(View.GONE);
        col.add(airLine, 4, ui.px(3));

        hoursRow = ui.row();
        hoursCount = (int) clamp(widthPx / ui.pxf(78), 2, 6);
        col.add(hoursRow, 6, ui.px(6));

        col.addFill(ui.spacer());
        col.add(buildBottomRow(), 0, ui.px(4));
        View.OnTouchListener swipe = brightnessSwipe();
        clock.setOnTouchListener(swipe);
        col.setOnTouchListener(swipe);
        return col;
    }

    /** Dòng trạng thái + pin (dùng chung cho bảng lịch và đồng hồ lật). */
    private View buildBottomRow() {
        LinearLayout bottom = ui.row();
        statusText = line(13, false);
        statusText.setClickable(true);
        statusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStatusClick();
            }
        });
        bottom.addView(statusText, Ui.weight(1));
        // Pin: app chạy toàn màn hình nên thanh trạng thái của máy bị ẩn.
        battView = new BatteryView(this, ui.px(13));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.leftMargin = ui.px(8);
        bottom.addView(battView, bp);
        battText = ui.text(13, Theme.DIM, true);
        battText.setPadding(ui.px(4), 0, 0, 0);
        bottom.addView(battText);
        renderBattery();
        return bottom;
    }

    private void clearViews() {
        clock = tempText = null;
        dateLine = lunarLine = canChiLine = eventLine = descText = detailText = alertLine = airLine = statusText = null;
        nowIcon = null;
        hoursRow = null;
        midMonth = null;
        midTitle = null;
        midExtra = null;
        pageTitle = null;
        headerExtra = null;
        dots = null;
        pageBox = null;
        newsView = null;
        battView = null;
        battText = null;
        flipHours = flipMinutes = null;
        flipDate = flipLunar = flipTemp = flipDesc = null;
        flipIcon = null;
    }

    // ------------------------------------------------------------------ đồng hồ lật

    private static TextView sized(TextView t, float px) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        return t;
    }

    /** Chế độ chỉ đồng hồ: hai thẻ lật giờ/phút kiểu HTC, thông tin phụ bên dưới. */
    private void buildFlip(int w, int h) {
        int pad = ui.px(10);
        LinearLayout col = ui.col();
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(pad, pad, pad, pad);
        col.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openSettings();
                return true;
            }
        });
        col.setOnTouchListener(brightnessSwipe());
        content.addView(col, new FrameLayout.LayoutParams(-1, -1));

        float innerW = w - 2f * pad;
        float innerH = h - 2f * pad;
        flipNarrow = innerW / ui.density < 560;
        boolean info = prefs.flipInfo();
        float bottomH = ui.pxf(22);
        float infoH = info ? innerH * 0.24f : 0f;
        float avail = innerH - bottomH - infoH;
        // Hai thẻ rộng 1,1 lần chiều cao, khe giữa 0,08 lần chiều cao.
        float ch = Math.min(avail * 0.92f, innerW * 0.96f / (2 * 1.1f + 0.08f));
        int cardH = Math.round(ch);
        int cardW = Math.round(ch * 1.1f);
        int gapC = Math.round(ch * 0.08f);

        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        LinearLayout cards = ui.row();
        cards.setGravity(Gravity.CENTER);
        cards.setClipChildren(false);
        flipHours = new FlipCard(this);
        flipMinutes = new FlipCard(this);
        cards.addView(flipHours, new LinearLayout.LayoutParams(cardW, cardH));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(cardW, cardH);
        mp.leftMargin = gapC;
        cards.addView(flipMinutes, mp);
        col.addView(cards, new LinearLayout.LayoutParams(-2, -2));

        if (info) {
            float big = Math.max(ui.pxf(14), Math.min(ch * 0.12f, infoH * 0.3f));
            float small = big * 0.86f;
            LinearLayout r1 = ui.row();
            r1.setGravity(Gravity.CENTER);
            flipDate = sized(ui.text(16, Theme.TEXT, false), big);
            r1.addView(flipDate);
            r1.addView(sized(ui.text("  ·  ", 16, Theme.META, false), big));
            flipLunar = sized(ui.text(16, Theme.LUNAR, true), big);
            r1.addView(flipLunar);
            LinearLayout.LayoutParams r1p = new LinearLayout.LayoutParams(-2, -2);
            r1p.topMargin = Math.round(ch * 0.07f);
            col.addView(r1, r1p);

            LinearLayout r2 = ui.row();
            r2.setGravity(Gravity.CENTER);
            flipIcon = new WeatherIcon(this, Math.round(small * 1.5f));
            r2.addView(flipIcon);
            flipTemp = sized(ui.text(16, Theme.TEXT, true), small * 1.15f);
            flipTemp.setPadding(Math.round(small * 0.3f), 0, Math.round(small * 0.4f), 0);
            r2.addView(flipTemp);
            flipDesc = sized(ui.text(14, Theme.DIM, false), small);
            r2.addView(flipDesc);
            LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(-2, -2);
            r2p.topMargin = Math.round(small * 0.35f);
            col.addView(r2, r2p);
        }
        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        col.addView(buildBottomRow(), new LinearLayout.LayoutParams(-1, -2));

        content.addView(gearButton(Theme.META), new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT));
    }

    private void renderFlipClock(Calendar now) {
        if (flipHours == null) return;
        flipHours.setText(Fmt.two(now.get(Calendar.HOUR_OF_DAY)), true);
        flipMinutes.setText(Fmt.two(now.get(Calendar.MINUTE)), true);
        if (flipDate != null) {
            // Màn hẹp (3,5"–4"): bỏ năm để dòng ngày + âm lịch vừa một hàng.
            flipDate.setText(Fmt.WEEKDAYS[now.get(Calendar.DAY_OF_WEEK)] + ", "
                    + (flipNarrow ? Fmt.ddmm(now) : Fmt.ddmmyyyy(now)));
            LunarDate ld = Lunar.solarToLunar(now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR));
            Lunar.Holiday hol = Lunar.eventOn(now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, ld);
            String lunar = "Âm lịch " + ld.day + "/" + ld.month + (ld.leap ? " nhuận" : "") + " " + Lunar.yearCanChi(ld.year);
            if (hol != null) lunar += " · " + hol.name;
            flipLunar.setText(lunar);
            flipLunar.setTextColor(Theme.LUNAR);
        }
    }

    private void renderFlipWeather() {
        if (flipIcon == null) return;
        JSONObject w = hub.data("weather");
        JSONObject now = w == null ? null : w.optJSONObject("now");
        if (now == null) {
            flipIcon.setVisibility(View.GONE);
            flipTemp.setText("");
            DataHub.Entry e = hub.entry("weather");
            flipDesc.setText(e != null && e.error != null ? "Chưa lấy được thời tiết" : "Đang tải thời tiết…");
            return;
        }
        int code = now.optInt("code");
        flipIcon.setVisibility(View.VISIBLE);
        flipIcon.set(code, now.optInt("day", 1) == 1, Theme.BG);
        flipTemp.setText(Fmt.temp(now.optDouble("t")));
        // Tên thành phố đã có ở dòng trạng thái; thêm báo sắp mưa nếu có.
        String rain = WeatherSource.rainSoon(w);
        flipDesc.setText(rain == null ? WeatherCodes.text(code)
                : WeatherCodes.text(code) + " · " + rain.replaceFirst(" \\(.*\\)$", "").toLowerCase(new java.util.Locale("vi")));
    }

    // ------------------------------------------------------------------ pin

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            readBattery(intent);
        }
    };

    private void readBattery(Intent i) {
        if (i == null) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        battPct = level < 0 || scale <= 0 ? -1 : Math.round(level * 100f / scale);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        battCharging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        battTemp = temp == Integer.MIN_VALUE ? Float.NaN : temp / 10f;
        renderBattery();
    }

    private void renderBattery() {
        if (battView == null || battText == null) return;
        battView.set(battPct, battCharging);
        if (battPct < 0) {
            battView.setVisibility(View.GONE);
            battText.setVisibility(View.GONE);
            return;
        }
        battView.setVisibility(View.VISIBLE);
        battText.setVisibility(View.VISIBLE);
        String s = battPct + "%";
        int color = battPct < 20 && !battCharging ? Theme.DOWN : Theme.DIM;
        // Máy cắm sạc liên tục dễ nóng/phồng pin: cảnh báo khi pin quá nóng.
        if (!Float.isNaN(battTemp) && battTemp >= 45f) {
            s += " · " + Math.round(battTemp) + "°C";
            color = Theme.WARN;
        }
        battText.setText(s);
        battText.setTextColor(color);
        battView.setContentDescription("Pin " + battPct + "%" + (battCharging ? ", đang sạc" : ""));
    }

    private TextView textButton(String label, View.OnClickListener l) {
        TextView t = ui.text(label, 15, Theme.ACCENT, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(ui.px(36));
        t.setPadding(ui.px(10), 0, ui.px(10), 0);
        t.setOnClickListener(l);
        return t;
    }

    private IconButton gearButton(int color) {
        IconButton b = new IconButton(this, IconButton.GEAR, ui.pxf(18), ui.px(44), ui.px(38), "Cài đặt", color);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        return b;
    }

    private View pageHeader(View title, LinearLayout extra, boolean withMenu) {
        LinearLayout header = ui.row();
        header.setMinimumHeight(ui.px(38));
        header.addView(title, Ui.weight(1));
        header.addView(extra);
        if (withMenu) {
            dots = new PageDots(this, ui.pxf(3f));
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
            dp.leftMargin = ui.px(8);
            header.addView(dots, dp);
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-2, -2);
            gp.rightMargin = -ui.px(12);
            header.addView(gearButton(Theme.DIM), gp);
        }
        return header;
    }

    private LinearLayout panel() {
        LinearLayout p = ui.col();
        p.setBackgroundColor(Theme.PANEL);
        p.setPadding(ui.px(12), ui.px(2), ui.px(12), ui.px(10));
        p.setClipToPadding(false);
        return p;
    }

    private View buildRight() {
        LinearLayout panel = panel();
        pageTitle = line(16, true);
        headerExtra = ui.row();
        panel.addView(pageHeader(pageTitle, headerExtra, true));
        pageBox = new FrameLayout(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bp.topMargin = ui.px(4);
        panel.addView(pageBox, bp);

        final GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                nextPage();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > ui.px(40)) {
                    if (dx < 0) nextPage();
                    else prevPage();
                    return true;
                }
                return false;
            }
        });
        panel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });
        return panel;
    }

    private View buildMiddle() {
        LinearLayout panel = panel();
        midTitle = line(16, true);
        midExtra = ui.row();
        panel.addView(pageHeader(midTitle, midExtra, false));
        midMonth = new MonthView(this);
        midMonth.setScale(ui.u);
        midMonth.setLegend(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = ui.px(4);
        panel.addView(midMonth, lp);
        renderMiddle();
        return panel;
    }

    // ------------------------------------------------------------------ hiển thị

    private void renderClock() {
        if (flipMode) {
            renderFlipClock(Calendar.getInstance());
            renderStatus();
            return;
        }
        if (clock == null) return;
        Calendar now = Calendar.getInstance();
        clock.setText(Fmt.hhmm(now));
        String wd = Fmt.WEEKDAYS[now.get(Calendar.DAY_OF_WEEK)];
        dateLine.set(Theme.TEXT, wd + ", " + Fmt.ddmmyyyy(now), wd + ", " + Fmt.ddmm(now),
                Fmt.WEEKDAYS_SHORT[now.get(Calendar.DAY_OF_WEEK)] + ", " + Fmt.ddmm(now));
        renderStatus();
        String key = Fmt.ddmmyyyy(now);
        if (!key.equals(dayKey)) {
            boolean first = dayKey.length() == 0;
            dayKey = key;
            renderLunar();
            if (!first) {
                renderMiddle();
                if (dataKeyOf(currentKey).length() == 0) renderPage();
            }
        }
    }

    private void renderLunar() {
        if (lunarLine == null) return;
        Calendar now = Calendar.getInstance();
        int d = now.get(Calendar.DAY_OF_MONTH);
        int m = now.get(Calendar.MONTH) + 1;
        int y = now.get(Calendar.YEAR);
        LunarDate ld = Lunar.solarToLunar(d, m, y);
        String lm = ld.day + "/" + ld.month + (ld.leap ? " nhuận" : "");
        lunarLine.set(Theme.LUNAR, "Âm lịch " + lm + " · " + Lunar.yearCanChi(ld.year), "Âm lịch " + lm, "Âm " + lm);
        String day = "Ngày " + Lunar.dayCanChi(ld.jd);
        canChiLine.set(Theme.DIM, day + " · tháng " + Lunar.monthCanChi(ld.month, ld.year), day);

        Lunar.Holiday hol = Lunar.eventOn(d, m, ld);
        // Ngày riêng (giỗ, sinh nhật) được nhắc trước một tuần, trước cả mùng 1, rằm.
        Lunar.Upcoming soon = hol == null ? Lunar.nextPersonal(d, m, y, 7) : null;
        String[] ev;
        int color = Theme.LUNAR;
        if (hol != null) {
            ev = new String[]{"Hôm nay: " + hol.name, hol.name};
        } else if (soon != null) {
            ev = new String[]{(soon.days == 1 ? "Ngày mai: " : "Còn " + soon.days + " ngày: ") + soon.holiday.name, soon.holiday.name};
        } else if (ld.day == 1 && !ld.leap) {
            ev = new String[]{"Mùng 1 tháng " + Lunar.MONTH_NAME[ld.month]};
        } else if (ld.day == 15) {
            ev = new String[]{"Rằm tháng " + Lunar.MONTH_NAME[ld.month]};
        } else if (Lunar.solarTermStarts(ld.jd)) {
            ev = new String[]{"Bắt đầu tiết " + Lunar.solarTerm(ld.jd), "Tiết " + Lunar.solarTerm(ld.jd)};
            color = Theme.TEXT;
        } else {
            List<Lunar.Upcoming> up = Lunar.upcoming(d, m, y, 1, true);
            int tet = Lunar.daysToTet(d, m, y);
            if (!up.isEmpty() && up.get(0).days <= 45) {
                Lunar.Upcoming u = up.get(0);
                String when = u.days == 1 ? "Ngày mai" : "Còn " + u.days + " ngày";
                ev = new String[]{when + ": " + u.holiday.name, u.holiday.name};
                color = Theme.TEXT;
            } else if (tet > 0 && tet <= 60) {
                ev = new String[]{"Còn " + tet + " ngày đến Tết", "Tết: còn " + tet + " ngày"};
            } else {
                boolean good = Lunar.isHoangDao(ld.jd, ld.month);
                String kind = good ? "Ngày hoàng đạo" : "Ngày hắc đạo";
                ev = new String[]{kind + " · tiết " + Lunar.solarTerm(ld.jd), kind};
                color = good ? Theme.AUSPICIOUS : Theme.DIM;
            }
        }
        eventLine.set(color, ev);
    }

    private void renderWeather() {
        if (flipMode) {
            renderFlipWeather();
            return;
        }
        if (tempText == null) return;
        JSONObject w = hub.data("weather");
        hoursRow.removeAllViews();
        if (w == null) {
            nowIcon.setVisibility(View.INVISIBLE);
            tempText.setText("--°");
            DataHub.Entry e = hub.entry("weather");
            if (e != null && e.error != null) descText.set(Theme.WARN, "Chưa lấy được thời tiết", "Chưa có");
            else descText.set(Theme.DIM, "Đang tải thời tiết…", "Đang tải…");
            detailText.set(Theme.DIM, prefs.placeName());
            alertLine.setVisibility(View.GONE);
            airLine.setVisibility(View.GONE);
            return;
        }
        JSONObject now = w.optJSONObject("now");
        if (now == null) now = new JSONObject();
        int code = now.optInt("code");
        nowIcon.setVisibility(View.VISIBLE);
        nowIcon.set(code, now.optInt("day", 1) == 1, Theme.BG);
        String desc = WeatherCodes.text(code);
        nowIcon.setContentDescription(desc);
        tempText.setText(Fmt.temp(now.optDouble("t")));
        int comma = desc.indexOf(',');
        descText.set(Theme.TEXT, desc, comma > 0 ? desc.substring(0, comma) : desc);
        String range = "";
        JSONArray days = w.optJSONArray("days");
        if (days != null && days.length() > 0) {
            JSONObject t = days.optJSONObject(0);
            range = Fmt.range(t.optDouble("min"), t.optDouble("max"));
        }
        String feels = "cảm giác " + Fmt.temp(now.optDouble("feels"));
        double hum = now.optDouble("hum");
        String humid = Double.isNaN(hum) ? "" : " · ẩm " + Math.round(hum) + "%";
        String head = range.length() > 0 ? range + " · " : "";
        detailText.set(Theme.DIM, head + feels + humid, head + feels, range.length() > 0 ? range : feels);

        String rain = WeatherSource.rainSoon(w);
        alertLine.setVisibility(rain == null ? View.GONE : View.VISIBLE);
        if (rain != null) {
            alertLine.set(rain.startsWith("Sắp có dông") ? Theme.WARN : Theme.RAIN, rain, rain.replaceFirst(" \\(.*\\)$", ""));
        }
        JSONObject air = w.optJSONObject("air");
        airLine.setVisibility(air == null ? View.GONE : View.VISIBLE);
        if (air != null) {
            int aqi = air.optInt("aqi");
            int level = WeatherCodes.aqiLevel(aqi);
            String label = WeatherCodes.AQI_LABEL[level];
            double pm = air.optDouble("pm25");
            String airHead = "Không khí " + label + " · AQI " + aqi;
            airLine.set(Theme.aqi(level), Double.isNaN(pm) ? airHead : airHead + " · PM2.5 " + Math.round(pm), airHead,
                    "AQI " + aqi + " · " + label);
        }

        JSONArray hours = w.optJSONArray("hours");
        if (hours != null) {
            int start = WeatherSource.firstUpcomingHour(w, System.currentTimeMillis());
            for (int i = start; i < start + hoursCount && i < hours.length(); i++) {
                JSONObject h = hours.optJSONObject(i);
                LinearLayout c = ui.row();
                c.setGravity(Gravity.CENTER_VERTICAL);
                c.addView(ui.text(h.optString("h") + "h", 13, Theme.DIM, false));
                WeatherIcon ic = new WeatherIcon(this, ui.px(20));
                ic.set(h.optInt("code"), h.optInt("day", 1) == 1, Theme.BG);
                c.addView(ic);
                c.addView(ui.text(Fmt.temp(h.optDouble("t")), 13.5f, Theme.TEXT, true));
                hoursRow.addView(c, Ui.weight(1));
            }
        }
    }

    private void renderStatus() {
        if (statusText == null) return;
        if (updater.ready()) {
            String v = updater.readyName();
            statusText.set(Theme.ACCENT, "Có bản mới " + v + " · chạm để cài", "Bản mới " + v + " · chạm để cài", "Chạm để cài bản mới");
            return;
        }
        if (updater.state() == Updater.State.DOWNLOADING) {
            statusText.set(Theme.DIM, updater.message());
            return;
        }
        // Giờ máy sai làm đồng hồ sai và hỏng cả kết nối HTTPS, nên báo trước các lỗi tải.
        long off = ClockCheck.offsetMs();
        boolean ancient = System.currentTimeMillis() < MIN_SANE_TIME;
        clockWrong = ancient || Math.abs(off) >= 2 * 60_000L;
        if (clockWrong) {
            String what;
            if (ancient) {
                what = "Giờ máy sai (năm " + Calendar.getInstance().get(Calendar.YEAR) + ")";
            } else {
                long min = Math.round(Math.abs(off) / 60_000.0);
                what = "Giờ máy " + (off > 0 ? "chậm " : "nhanh ") + (min >= 90 ? Math.round(min / 60.0) + " giờ" : min + " phút");
            }
            statusText.set(Theme.WARN, what + " · chạm để chỉnh", what);
            return;
        }
        StringBuilder err = new StringBuilder();
        for (String k : DATA_KEYS) {
            Source s = hub.source(k);
            DataHub.Entry e = hub.entry(k);
            if (s == null || e == null || !s.enabled(prefs) || e.error == null) continue;
            if (err.length() > 0) err.append(", ");
            err.append(s.title);
        }
        if (err.length() > 0) {
            statusText.set(Theme.WARN, "Lỗi tải " + err + " · chạm để thử lại", "Lỗi tải " + err, "Lỗi tải dữ liệu");
            return;
        }
        if (overlayMissing) {
            statusText.set(Theme.WARN, "Để tự bật lại, cho phép hiện trên ứng dụng khác · chạm để cấp",
                    "Chưa cho phép hiện trên ứng dụng khác · chạm để cấp", "Thiếu quyền tự bật lại · chạm");
            return;
        }
        DataHub.Entry e = hub.entry("weather");
        String place = prefs.placeName();
        if (e == null || e.at <= 0) {
            statusText.set(Theme.META, place);
            return;
        }
        long now = System.currentTimeMillis();
        String fresh = now - e.at < 60_000L ? "vừa cập nhật" : "cập nhật " + Fmt.agoPhrase(e.at, now);
        statusText.set(Theme.META, place + " · " + fresh, fresh);
    }

    @SuppressLint("NewApi")
    private void onStatusClick() {
        if (updater.ready()) {
            updater.install(this);
        } else if (clockWrong) {
            try {
                startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
            } catch (Throwable t) {
                Toast.makeText(this, "Mở Cài đặt của máy > Ngày và giờ, bật Tự động", Toast.LENGTH_LONG).show();
            }
        } else if (overlayMissing && errorCount() == 0) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName())));
            } catch (Throwable t) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
        } else {
            hub.refreshAll();
            Toast.makeText(this, "Đang tải lại dữ liệu…", Toast.LENGTH_SHORT).show();
        }
    }

    private int errorCount() {
        int n = 0;
        for (String k : DATA_KEYS) {
            Source s = hub.source(k);
            DataHub.Entry e = hub.entry(k);
            if (s != null && e != null && s.enabled(prefs) && e.error != null) n++;
        }
        return n;
    }

    private void renderMiddle() {
        if (midMonth == null || cols != 3) return;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, monthShift);
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        midMonth.setMonth(y, m);
        midTitle.set(Theme.TEXT, "Tháng " + m + "/" + y, m + "/" + y);
        fillMonthNav(midExtra, true);
    }

    private void fillMonthNav(LinearLayout box, final boolean middle) {
        box.removeAllViews();
        if (monthShift != 0) {
            box.addView(textButton("Hôm nay", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shiftMonth(-monthShift, middle);
                }
            }));
        }
        IconButton prev = new IconButton(this, IconButton.PREV, ui.pxf(16), ui.px(40), ui.px(38), "Tháng trước", Theme.ACCENT);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(-1, middle);
            }
        });
        box.addView(prev);
        IconButton next = new IconButton(this, IconButton.NEXT, ui.pxf(16), ui.px(40), ui.px(38), "Tháng sau", Theme.ACCENT);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(1, middle);
            }
        });
        box.addView(next);
    }

    private void shiftMonth(int delta, boolean middle) {
        monthShift += delta;
        lastTouch = SystemClock.elapsedRealtime();
        if (middle) renderMiddle();
        else renderPage();
    }

    private void nextPage() {
        if (pageKeys.isEmpty()) return;
        leavePage();
        page = (page + 1) % pageKeys.size();
        renderPage();
    }

    private void prevPage() {
        if (pageKeys.isEmpty()) return;
        leavePage();
        page = (page - 1 + pageKeys.size()) % pageKeys.size();
        renderPage();
    }

    private void leavePage() {
        if ("news".equals(currentKey) && newsView != null) {
            newsOffset += Math.max(1, newsView.shownCount() - 1);
        }
        if (cols != 3) monthShift = 0;
    }

    private static String dataKeyOf(String page) {
        if ("sheet".equals(page) || "month".equals(page) || "lunar".equals(page)) return "";
        return page;
    }

    private void renderPage() {
        if (pageBox == null || pageKeys.isEmpty()) return;
        String key = pageKeys.get(Math.min(page, pageKeys.size() - 1));
        currentKey = key;
        pageBox.removeAllViews();
        headerExtra.removeAllViews();
        newsView = null;
        View v;
        String[] title;
        if ("sheet".equals(key)) {
            v = pages.sheet();
            title = new String[]{"Tờ lịch"};
        } else if ("month".equals(key)) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, monthShift);
            int y = c.get(Calendar.YEAR);
            int m = c.get(Calendar.MONTH) + 1;
            MonthView mv = new MonthView(this);
            mv.setScale(ui.u);
            mv.setLegend(true);
            mv.setMonth(y, m);
            v = mv;
            title = new String[]{"Tháng " + m + "/" + y, m + "/" + y};
            fillMonthNav(headerExtra, false);
            // Khung hẹp: nhường chỗ cho tên tháng và nút chuyển tháng.
            dots.setVisibility(narrowRight ? View.GONE : View.VISIBLE);
        } else if ("lunar".equals(key)) {
            v = pages.lunar();
            title = new String[]{"Âm lịch"};
        } else if ("weather".equals(key)) {
            v = pages.weather();
            title = new String[]{"Dự báo thời tiết", "Thời tiết"};
        } else if ("gold".equals(key)) {
            v = pages.gold();
            title = new String[]{"Giá vàng"};
        } else if ("fuel".equals(key)) {
            v = pages.fuel();
            title = new String[]{"Giá xăng dầu", "Xăng dầu"};
        } else if ("fx".equals(key)) {
            v = pages.fx();
            title = new String[]{"Tỷ giá"};
        } else if ("stocks".equals(key)) {
            v = pages.stocks();
            title = new String[]{"Chứng khoán"};
        } else {
            newsView = pages.news(newsOffset);
            v = newsView;
            title = new String[]{"Tin tức"};
        }
        pageBox.addView(v, new FrameLayout.LayoutParams(-1, -1));
        pageTitle.set(Theme.TEXT, title);
        if (!"month".equals(key)) dots.setVisibility(View.VISIBLE);
        dots.set(pageKeys.size(), page);
        String dk = dataKeyOf(key);
        if (dk.length() > 0) rendered.put("page:" + dk, signature(dk));
    }

    private String signature(String key) {
        DataHub.Entry e = hub.entry(key);
        if (e == null) return "";
        return e.at + "|" + e.error + "|" + (e.data == null);
    }

    // ------------------------------------------------------------------ sự kiện dữ liệu

    @Override
    public void onSectionChanged(String key) {
        if (!resumed || ui == null) return;
        String sig = signature(key);
        if (!sig.equals(rendered.get(key))) {
            rendered.put(key, sig);
            if ("weather".equals(key)) renderWeather();
            renderStatus();
        }
        if (key.equals(dataKeyOf(currentKey)) && !sig.equals(rendered.get("page:" + key))) {
            renderPage();
        }
    }

    @Override
    public void onUpdateChanged() {
        if (resumed) renderStatus();
    }

    // ------------------------------------------------------------------ root

    /** FrameLayout báo khi đổi kích thước để dựng lại bố cục. */
    static final class BoardRoot extends FrameLayout {
        Runnable onSize;

        BoardRoot(Context c) {
            super(c);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if ((w != oldw || h != oldh) && onSize != null) post(onSize);
        }
    }
}
