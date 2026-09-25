package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;
import static dev.aperture.spike.probe.Probe.safe;

import android.accessibilityservice.AccessibilityService;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.AttachedSurfaceControl;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Phase 0 overlay spike. Commands (adb):
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd overlay --es mode resize|fixed|fixed_noregion|off
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd style --es style debug|product
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd app_overlay --ez on true|false
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd anim_test --es mode none|lp_rate|lp_mode|view_high|view_hz
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd probe
 *   am broadcast -a dev.aperture.spike.probe.CMD --es cmd colors
 */
public class ProbeA11yService extends AccessibilityService {
    static final String ACTION_CMD = "dev.aperture.spike.probe.CMD";

    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private IslandTestView island;
    private WindowManager.LayoutParams islandLp;
    private String overlayMode = "resize";
    private boolean debugStyle = true;
    private View appOverlay;
    private WindowManager appOverlayWm;
    private Configuration lastConfig;
    private BroadcastReceiver cmdReceiver;
    private BroadcastReceiver powerReceiver;

    @Override
    protected void onServiceConnected() {
        wm = getSystemService(WindowManager.class);
        lastConfig = new Configuration(getResources().getConfiguration());
        JSONObject o = obj();
        put(o, "isUiContext", safe(this::isUiContext));
        put(o, "colors", DeviceProbe.colors(this));
        put(o, "windowMetricsFromService", safe(() -> DeviceProbe.windowMetrics(this)));
        Probe.log("a11y_connected", o);
        registerWatchers();
        cmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                handleCommand(i);
            }
        };
        registerReceiver(cmdReceiver, new IntentFilter(ACTION_CMD), Context.RECEIVER_EXPORTED);
        setOverlay(overlayMode);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        JSONObject o = obj();
        put(o, "pkg", String.valueOf(e.getPackageName()));
        put(o, "cls", String.valueOf(e.getClassName()));
        put(o, "sourceIsNull", e.getSource() == null);
        Probe.log("a11y_window_state", o);
    }

    @Override
    public void onInterrupt() {
        Probe.log("a11y_interrupt", obj());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int diff = lastConfig.diff(newConfig);
        JSONObject o = obj();
        put(o, "diffHex", Integer.toHexString(diff));
        put(o, "uiModeNight", newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK);
        put(o, "densityDpi", newConfig.densityDpi);
        put(o, "fontScale", newConfig.fontScale);
        put(o, "fontWeightAdjustment", newConfig.fontWeightAdjustment);
        put(o, "orientation", newConfig.orientation);
        lastConfig = new Configuration(newConfig);
        Probe.log("a11y_config_changed", o);
        themeSignal("config_changed", Integer.toHexString(diff));
        if (!"off".equals(overlayMode)) setOverlay(overlayMode);
    }

    @Override
    public void onDestroy() {
        if (island != null) wm.removeViewImmediate(island);
        if (appOverlay != null) appOverlayWm.removeViewImmediate(appOverlay);
        if (cmdReceiver != null) unregisterReceiver(cmdReceiver);
        if (powerReceiver != null) unregisterReceiver(powerReceiver);
        super.onDestroy();
    }

    // ---- Theme / environment watchers (Section 7.1 and 9.5 [VERIFY]) ------------------------

    private void registerWatchers() {
        WallpaperManager.getInstance(this).addOnColorsChangedListener(
                (colors, which) -> themeSignal("wallpaper_colors_changed", "which=" + which), main);
        getSystemService(UiModeManager.class).addContrastChangeListener(getMainExecutor(),
                contrast -> themeSignal("contrast_changed", String.valueOf(contrast)));
        getSystemService(PowerManager.class).addThermalStatusListener(getMainExecutor(),
                status -> Probe.log("thermal_status", "status", status));
        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                Probe.log("power_save_changed", "on", getSystemService(PowerManager.class).isPowerSaveMode());
            }
        };
        registerReceiver(powerReceiver, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                Context.RECEIVER_NOT_EXPORTED);
        getSystemService(CameraManager.class).registerAvailabilityCallback(getMainExecutor(),
                new CameraManager.AvailabilityCallback() {
                    @Override
                    public void onCameraAvailable(String id) {
                        Probe.log("camera_available", "id", id);
                    }

                    @Override
                    public void onCameraUnavailable(String id) {
                        Probe.log("camera_unavailable", "id", id);
                    }

                    @Override
                    public void onCameraAccessPrioritiesChanged() {
                        Probe.log("camera_access_priorities_changed", obj());
                    }
                });
        AudioManager am = getSystemService(AudioManager.class);
        am.registerAudioRecordingCallback(new AudioManager.AudioRecordingCallback() {
            @Override
            public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                Probe.log("recording_configs", "configs", DeviceProbe.recordings(configs));
            }
        }, main);
        am.registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                Probe.log("playback_configs", "count", configs.size());
            }
        }, main);
        getSystemService(DisplayManager.class).registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int id) {
                Probe.log("display_added", "id", id);
            }

            @Override
            public void onDisplayRemoved(int id) {
                Probe.log("display_removed", "id", id);
            }

            @Override
            public void onDisplayChanged(int id) {
                Display d = getSystemService(DisplayManager.class).getDisplay(id);
                JSONObject o = obj();
                put(o, "id", id);
                if (d != null) {
                    put(o, "hz", d.getRefreshRate());
                    put(o, "modeId", d.getMode().getModeId());
                    put(o, "rotation", d.getRotation());
                }
                Probe.log("display_changed", o);
            }
        }, main);
        observe(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE));
        observe(Settings.Secure.getUriFor("accessibility_display_inversion_enabled"));
        observe(Settings.Secure.getUriFor("theme_customization_overlay_packages"));
        observe(Settings.System.getUriFor("peak_refresh_rate"));
        observe(Settings.Global.getUriFor("heads_up_notifications_enabled"));
        observe(Settings.Secure.getUriFor("ui_night_mode"));
    }

    private void observe(Uri uri) {
        try {
            getContentResolver().registerContentObserver(uri, false, new ContentObserver(main) {
                @Override
                public void onChange(boolean selfChange, Uri changed) {
                    Probe.log("setting_changed", "uri", String.valueOf(changed));
                }
            });
        } catch (SecurityException e) {
            Probe.log("setting_observe_denied", "uri", uri.toString());
        }
    }

    /** Records the system colors every context sees, immediately and after the system has settled. */
    private void themeSignal(String kind, String detail) {
        JSONObject o = obj();
        put(o, "kind", kind);
        put(o, "detail", detail);
        put(o, "serviceRes", DeviceProbe.colors(this));
        put(o, "appRes", DeviceProbe.colors(getApplicationContext()));
        put(o, "freshConfigCtx", DeviceProbe.colors(createConfigurationContext(getResources().getConfiguration())));
        put(o, "systemResAccent1_500", safe(() -> Probe.hex(Resources.getSystem().getColor(android.R.color.system_accent1_500, null))));
        Probe.log("theme_signal", o);
        main.postDelayed(() -> {
            JSONObject later = obj();
            put(later, "kind", kind);
            put(later, "serviceRes", DeviceProbe.colors(this));
            put(later, "freshConfigCtx", DeviceProbe.colors(createConfigurationContext(getResources().getConfiguration())));
            Probe.log("theme_signal_1500ms", later);
        }, 1500);
    }

    // ---- Commands ----------------------------------------------------------------------------

    private void handleCommand(Intent i) {
        String cmd = i.getStringExtra("cmd");
        Probe.log("cmd", "cmd", String.valueOf(cmd));
        if (cmd == null) return;
        switch (cmd) {
            case "overlay":
                setOverlay(i.getStringExtra("mode"));
                break;
            case "style":
                debugStyle = !"product".equals(i.getStringExtra("style"));
                if (island != null) {
                    island.debugStyle = debugStyle;
                    island.invalidate();
                }
                break;
            case "app_overlay":
                toggleAppOverlay(i.getBooleanExtra("on", true));
                break;
            case "anim_test":
                FrameRateTest.run(wm, island, islandLp, i.getStringExtra("mode"));
                break;
            case "probe":
                runServiceProbe();
                break;
            case "colors":
                themeSignal("manual", "cmd");
                break;
            default:
                Probe.log("cmd_unknown", "cmd", cmd);
                break;
        }
    }

    private void setOverlay(String mode) {
        if (island != null) {
            wm.removeViewImmediate(island);
            island = null;
        }
        overlayMode = mode == null ? "resize" : mode;
        if ("off".equals(overlayMode)) return;
        Geometry g = Geometry.measure(this);
        int w;
        int h;
        int x;
        int y;
        if ("resize".equals(overlayMode)) {
            // Window hugs the idle ring only (Gate D2 option A at idle).
            int size = (int) Math.ceil(2 * (g.r + g.gapPx)) + 2;
            w = size;
            h = size;
            x = Math.round(g.cx - size / 2f);
            y = Math.round(g.cy - size / 2f);
        } else {
            // Fixed maximum window (Gate D2 option B), optionally with a public touchable region (API 33).
            w = WindowManager.LayoutParams.MATCH_PARENT;
            h = Math.round(g.cy + g.r + 360 * g.density);
            x = 0;
            y = 0;
        }
        IslandTestView v = new IslandTestView(this, g, x, y);
        v.debugStyle = debugStyle;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = x;
        lp.y = y;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("ApertureProbeIsland");
        wm.addView(v, lp);
        island = v;
        islandLp = lp;
        final String m = overlayMode;
        v.post(() -> {
            if ("fixed".equals(m)) applyTouchableRegion(v, g);
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            JSONObject o = obj();
            put(o, "mode", m);
            put(o, "geometry", g.toJson());
            put(o, "windowX", loc[0]);
            put(o, "windowY", loc[1]);
            put(o, "windowW", v.getWidth());
            put(o, "windowH", v.getHeight());
            put(o, "rootInsets", safe(() -> DeviceProbe.rootInsets(v)));
            Probe.log("overlay_attached", o);
        });
    }

    private void applyTouchableRegion(View v, Geometry g) {
        AttachedSurfaceControl sc = v.getRootSurfaceControl();
        float ring = g.r + g.gapPx;
        Region region = new Region(Math.round(g.cx - ring), Math.round(g.cy - ring),
                Math.round(g.cx + ring), Math.round(g.cy + ring));
        if (sc == null) {
            Probe.log("touchable_region_error", "reason", "no root surface control");
            return;
        }
        sc.setTouchableRegion(region);
        Probe.log("touchable_region_set", "bounds", Probe.rect(region.getBounds()));
    }

    /**
     * Verifies Section 3.3.9: TYPE_APPLICATION_OVERLAY should render below the status bar. Uses its own
     * WindowContext: adding it through this service's WindowManager would attach it to the accessibility
     * window token and inherit the accessibility layer, which is not what a normal overlay app gets.
     */
    private void toggleAppOverlay(boolean on) {
        if (on && appOverlay == null) {
            if (!Settings.canDrawOverlays(this)) {
                Probe.log("app_overlay_denied", obj());
                return;
            }
            Context wc = getApplicationContext().createWindowContext(
                    getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
            appOverlayWm = wc.getSystemService(WindowManager.class);
            View v = new View(wc);
            v.setBackgroundColor(0x80FF0000);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, Math.round(120 * getResources().getDisplayMetrics().density),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            lp.setFitInsetsTypes(0);
            lp.setTitle("ApertureProbeAppOverlay");
            try {
                appOverlayWm.addView(v, lp);
                appOverlay = v;
                v.post(() -> {
                    int[] loc = new int[2];
                    v.getLocationOnScreen(loc);
                    Probe.log("app_overlay_attached", put(put(obj(), "x", loc[0]), "y", loc[1]));
                });
            } catch (RuntimeException e) {
                Probe.log("app_overlay_error", "error", e.toString());
            }
        } else if (!on && appOverlay != null) {
            appOverlayWm.removeViewImmediate(appOverlay);
            appOverlay = null;
        }
    }

    private void runServiceProbe() {
        JSONObject r = DeviceProbe.collect(this);
        put(r, "overlayRootInsets", safe(() -> island == null ? null : DeviceProbe.rootInsets(island)));
        write("probe_service.json", r);
    }

    private void write(String name, JSONObject r) {
        File f = new File(getFilesDir(), name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(r.toString(2).getBytes(StandardCharsets.UTF_8));
            Probe.log("probe_written", put(put(obj(), "path", f.getAbsolutePath()), "bytes", f.length()));
        } catch (Exception e) {
            Probe.log("probe_write_error", "error", e.toString());
        }
    }
}
