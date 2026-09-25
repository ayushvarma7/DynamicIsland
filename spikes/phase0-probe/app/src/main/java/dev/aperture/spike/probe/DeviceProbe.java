package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.hex;
import static dev.aperture.spike.probe.Probe.insets;
import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;
import static dev.aperture.spike.probe.Probe.putSafe;
import static dev.aperture.spike.probe.Probe.rect;
import static dev.aperture.spike.probe.Probe.rectF;
import static dev.aperture.spike.probe.Probe.rects;
import static dev.aperture.spike.probe.Probe.safe;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Path;
import android.graphics.PathIterator;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.SystemFonts;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.Spatializer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayShape;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads every Section 9.2 value once. Throwaway; the product rewrites this as :core:device. */
final class DeviceProbe {
    private DeviceProbe() {}

    static JSONObject collect(Context ctx) {
        JSONObject root = obj();
        put(root, "probeVersion", 1);
        put(root, "wallClockMs", System.currentTimeMillis());
        put(root, "contextClass", ctx.getClass().getName());
        putSafe(root, "isUiContext", ctx::isUiContext);
        putSafe(root, "identity", DeviceProbe::identity);
        putSafe(root, "pixel", () -> pixel(ctx));
        putSafe(root, "display", () -> display(ctx));
        putSafe(root, "displays", () -> displays(ctx));
        putSafe(root, "refresh", () -> refresh(ctx));
        putSafe(root, "frameworkResources", DeviceProbe::frameworkResources);
        putSafe(root, "systemUiResources", () -> systemUiResources(ctx));
        putSafe(root, "theme", () -> theme(ctx));
        putSafe(root, "text", () -> text(ctx));
        putSafe(root, "motion", () -> motion(ctx));
        putSafe(root, "accessibilityDisplay", () -> accessibilityDisplay(ctx));
        putSafe(root, "localeTime", () -> localeTime(ctx));
        putSafe(root, "power", () -> power(ctx));
        putSafe(root, "memory", () -> memory(ctx));
        putSafe(root, "haptics", () -> haptics(ctx));
        putSafe(root, "flashlight", () -> flashlight(ctx));
        putSafe(root, "audio", () -> audio(ctx));
        putSafe(root, "notifications", () -> notifications(ctx));
        putSafe(root, "systemUi", () -> systemUi(ctx));
        putSafe(root, "security", () -> security(ctx));
        putSafe(root, "fonts", DeviceProbe::fonts);
        putSafe(root, "mediaOutput", () -> mediaOutput(ctx));
        putSafe(root, "settingsTables", () -> settingsTables(ctx));
        return root;
    }

    // ---- Identity --------------------------------------------------------------------------

    static JSONObject identity() {
        JSONObject o = obj();
        put(o, "manufacturer", Build.MANUFACTURER);
        put(o, "brand", Build.BRAND);
        put(o, "model", Build.MODEL);
        put(o, "device", Build.DEVICE);
        put(o, "product", Build.PRODUCT);
        put(o, "hardware", Build.HARDWARE);
        put(o, "board", Build.BOARD);
        put(o, "socManufacturer", Build.SOC_MANUFACTURER);
        put(o, "socModel", Build.SOC_MODEL);
        put(o, "sdkInt", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 36) {
            put(o, "sdkIntFull", Build.VERSION.SDK_INT_FULL);
            put(o, "majorSdk", Build.getMajorSdkVersion(Build.VERSION.SDK_INT_FULL));
            put(o, "minorSdk", Build.getMinorSdkVersion(Build.VERSION.SDK_INT_FULL));
        }
        put(o, "release", Build.VERSION.RELEASE);
        put(o, "releaseOrPreviewDisplay", Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY);
        put(o, "codename", Build.VERSION.CODENAME);
        put(o, "previewSdkInt", Build.VERSION.PREVIEW_SDK_INT);
        put(o, "incremental", Build.VERSION.INCREMENTAL);
        put(o, "securityPatch", Build.VERSION.SECURITY_PATCH);
        put(o, "mediaPerformanceClass", Build.VERSION.MEDIA_PERFORMANCE_CLASS);
        put(o, "buildId", Build.ID);
        put(o, "display", Build.DISPLAY);
        put(o, "fingerprint", Build.FINGERPRINT);
        put(o, "type", Build.TYPE);
        put(o, "tags", Build.TAGS);
        put(o, "supportedAbis", new JSONArray(List.of(Build.SUPPORTED_ABIS)));
        return o;
    }

    static JSONObject pixel(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        JSONObject o = obj();
        put(o, "PIXEL_EXPERIENCE", pm.hasSystemFeature("com.google.android.feature.PIXEL_EXPERIENCE"));
        JSONArray feats = new JSONArray();
        int total = 0;
        for (FeatureInfo fi : pm.getSystemAvailableFeatures()) {
            total++;
            if (fi.name == null) continue;
            String n = fi.name.toLowerCase(Locale.US);
            if (n.contains("google") || n.contains("pixel")) feats.put(fi.name);
        }
        put(o, "googleOrPixelFeatures", feats);
        put(o, "systemFeatureCount", total);
        return o;
    }

    // ---- Display, cutout, corners ----------------------------------------------------------

    static JSONObject display(Context ctx) {
        JSONObject o = obj();
        Display d = ctx.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        putSafe(o, "windowMetrics", () -> windowMetrics(ctx));
        putSafe(o, "displayCutoutDirect", () -> d.getCutout() == null ? null : cutout(d.getCutout()));
        JSONObject rc = obj();
        for (int pos = 0; pos < 4; pos++) {
            final int p = pos;
            putSafe(rc, cornerName(p), () -> corner(d.getRoundedCorner(p)));
        }
        put(o, "roundedCorners", rc);
        putSafe(o, "displayShape", () -> shape(d.getShape()));
        put(o, "rotation", d.getRotation());
        put(o, "name", d.getName());
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        JSONObject m = obj();
        put(m, "widthPixels", dm.widthPixels);
        put(m, "heightPixels", dm.heightPixels);
        put(m, "density", dm.density);
        put(m, "densityDpi", dm.densityDpi);
        put(m, "xdpi", dm.xdpi);
        put(m, "ydpi", dm.ydpi);
        put(m, "densityDeviceStable", DisplayMetrics.DENSITY_DEVICE_STABLE);
        put(o, "resourceMetrics", m);
        Configuration c = ctx.getResources().getConfiguration();
        JSONObject conf = obj();
        put(conf, "densityDpi", c.densityDpi);
        put(conf, "screenWidthDp", c.screenWidthDp);
        put(conf, "screenHeightDp", c.screenHeightDp);
        put(conf, "smallestScreenWidthDp", c.smallestScreenWidthDp);
        put(conf, "orientation", c.orientation);
        put(o, "configuration", conf);
        Display.Mode mode = d.getMode();
        double wIn = mode.getPhysicalWidth() / (double) dm.xdpi;
        double hIn = mode.getPhysicalHeight() / (double) dm.ydpi;
        put(o, "diagonalInchesEstimate", Math.sqrt(wIn * wIn + hIn * hIn));
        return o;
    }

    static JSONObject windowMetrics(Context ctx) {
        WindowManager wm = ctx.getSystemService(WindowManager.class);
        WindowMetrics max = wm.getMaximumWindowMetrics();
        WindowMetrics cur = wm.getCurrentWindowMetrics();
        JSONObject o = obj();
        put(o, "maxBounds", rect(max.getBounds()));
        put(o, "currentBounds", rect(cur.getBounds()));
        put(o, "density", max.getDensity());
        put(o, "maxInsets", insetsInfo(max.getWindowInsets()));
        put(o, "currentInsets", insetsInfo(cur.getWindowInsets()));
        return o;
    }

    /** Insets as seen by a real attached window (activity or overlay root). */
    static JSONObject rootInsets(View v) {
        WindowInsets wi = v.getRootWindowInsets();
        return wi == null ? null : insetsInfo(wi);
    }

    static JSONObject insetsInfo(WindowInsets wi) {
        JSONObject o = obj();
        JSONObject ins = obj();
        put(ins, "statusBars", insets(wi.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())));
        put(ins, "navigationBars", insets(wi.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())));
        put(ins, "displayCutout", insets(wi.getInsets(WindowInsets.Type.displayCutout())));
        put(ins, "systemGestures", insets(wi.getInsets(WindowInsets.Type.systemGestures())));
        put(ins, "mandatorySystemGestures", insets(wi.getInsets(WindowInsets.Type.mandatorySystemGestures())));
        put(ins, "tappableElement", insets(wi.getInsetsIgnoringVisibility(WindowInsets.Type.tappableElement())));
        put(ins, "captionBar", insets(wi.getInsetsIgnoringVisibility(WindowInsets.Type.captionBar())));
        put(o, "insets", ins);
        putSafe(o, "privacyIndicatorBounds", () -> rect(wi.getPrivacyIndicatorBounds()));
        putSafe(o, "boundingRectsStatusBars", () -> rects(wi.getBoundingRects(WindowInsets.Type.statusBars())));
        putSafe(o, "boundingRectsCaptionBar", () -> rects(wi.getBoundingRects(WindowInsets.Type.captionBar())));
        putSafe(o, "boundingRectsDisplayCutout", () -> rects(wi.getBoundingRects(WindowInsets.Type.displayCutout())));
        putSafe(o, "cutout", () -> wi.getDisplayCutout() == null ? null : cutout(wi.getDisplayCutout()));
        putSafe(o, "roundedCornerTopLeft", () -> corner(wi.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)));
        putSafe(o, "displayShapeBounds", () -> wi.getDisplayShape() == null ? null : shape(wi.getDisplayShape()));
        return o;
    }

    static JSONObject cutout(DisplayCutout dc) {
        JSONObject o = obj();
        JSONObject safe = obj();
        put(safe, "l", dc.getSafeInsetLeft());
        put(safe, "t", dc.getSafeInsetTop());
        put(safe, "r", dc.getSafeInsetRight());
        put(safe, "b", dc.getSafeInsetBottom());
        put(o, "safeInsets", safe);
        put(o, "boundingRectTop", rect(dc.getBoundingRectTop()));
        put(o, "boundingRectLeft", rect(dc.getBoundingRectLeft()));
        put(o, "boundingRectRight", rect(dc.getBoundingRectRight()));
        put(o, "boundingRectBottom", rect(dc.getBoundingRectBottom()));
        put(o, "waterfallInsets", insets(dc.getWaterfallInsets()));
        Path p = dc.getCutoutPath();
        put(o, "path", p == null ? null : path(p, 48));
        return o;
    }

    static JSONObject path(Path p, int maxSegments) {
        JSONObject o = obj();
        RectF bounds = new RectF();
        p.computeBounds(bounds, true);
        put(o, "bounds", rectF(bounds));
        put(o, "centerX", bounds.centerX());
        put(o, "centerY", bounds.centerY());
        put(o, "radiusEstimate", (bounds.width() + bounds.height()) / 4f);
        JSONArray segs = new JSONArray();
        int count = 0;
        PathIterator it = p.getPathIterator();
        while (it.hasNext()) {
            PathIterator.Segment s = it.next();
            if (s.getVerb() == PathIterator.VERB_DONE) break;
            count++;
            if (segs.length() >= maxSegments) continue;
            JSONObject so = obj();
            put(so, "verb", verbName(s.getVerb()));
            JSONArray pts = new JSONArray();
            float[] raw = s.getPoints();
            int n = pointCount(s.getVerb()) * 2;
            for (int i = 0; i < n && i < raw.length; i++) Probe.add(pts, Math.round(raw[i] * 100) / 100.0);
            put(so, "pts", pts);
            if (s.getVerb() == PathIterator.VERB_CONIC) put(so, "w", s.getConicWeight());
            segs.put(so);
        }
        put(o, "segmentCount", count);
        put(o, "segments", segs);
        return o;
    }

    private static int pointCount(int verb) {
        switch (verb) {
            case PathIterator.VERB_MOVE:
                return 1;
            case PathIterator.VERB_LINE:
                return 2;
            case PathIterator.VERB_QUAD:
            case PathIterator.VERB_CONIC:
                return 3;
            case PathIterator.VERB_CUBIC:
                return 4;
            default:
                return 0;
        }
    }

    private static String verbName(int verb) {
        switch (verb) {
            case PathIterator.VERB_MOVE:
                return "M";
            case PathIterator.VERB_LINE:
                return "L";
            case PathIterator.VERB_QUAD:
                return "Q";
            case PathIterator.VERB_CONIC:
                return "K";
            case PathIterator.VERB_CUBIC:
                return "C";
            case PathIterator.VERB_CLOSE:
                return "Z";
            default:
                return "?" + verb;
        }
    }

    private static String cornerName(int pos) {
        switch (pos) {
            case RoundedCorner.POSITION_TOP_LEFT:
                return "topLeft";
            case RoundedCorner.POSITION_TOP_RIGHT:
                return "topRight";
            case RoundedCorner.POSITION_BOTTOM_RIGHT:
                return "bottomRight";
            default:
                return "bottomLeft";
        }
    }

    static JSONObject corner(RoundedCorner rc) {
        if (rc == null) return null;
        JSONObject o = obj();
        put(o, "radius", rc.getRadius());
        put(o, "centerX", rc.getCenter().x);
        put(o, "centerY", rc.getCenter().y);
        return o;
    }

    static JSONObject shape(DisplayShape ds) {
        if (ds == null) return null;
        return path(ds.getPath(), 8);
    }

    static JSONArray displays(Context ctx) {
        DisplayManager dm = ctx.getSystemService(DisplayManager.class);
        JSONArray a = new JSONArray();
        for (Display d : dm.getDisplays()) {
            JSONObject o = obj();
            put(o, "id", d.getDisplayId());
            put(o, "name", d.getName());
            put(o, "flagsHex", Integer.toHexString(d.getFlags()));
            put(o, "state", d.getState());
            Display.Mode m = d.getMode();
            put(o, "mode", m.getPhysicalWidth() + "x" + m.getPhysicalHeight() + "@" + m.getRefreshRate());
            a.put(o);
        }
        return a;
    }

    static JSONObject refresh(Context ctx) {
        JSONObject o = obj();
        Display d = ctx.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        JSONArray modes = new JSONArray();
        for (Display.Mode m : d.getSupportedModes()) {
            JSONObject mo = obj();
            put(mo, "id", m.getModeId());
            put(mo, "w", m.getPhysicalWidth());
            put(mo, "h", m.getPhysicalHeight());
            put(mo, "hz", m.getRefreshRate());
            JSONArray alt = new JSONArray();
            for (float f : m.getAlternativeRefreshRates()) Probe.add(alt, Math.round(f * 100) / 100.0);
            put(mo, "alternativeHz", alt);
            modes.put(mo);
        }
        put(o, "supportedModes", modes);
        put(o, "currentModeId", d.getMode().getModeId());
        put(o, "currentHz", d.getRefreshRate());
        if (Build.VERSION.SDK_INT >= 36) {
            putSafe(o, "hasArrSupport", d::hasArrSupport);
            putSafe(o, "suggestedFrameRateNormal", () -> d.getSuggestedFrameRate(Display.FRAME_RATE_CATEGORY_NORMAL));
            putSafe(o, "suggestedFrameRateHigh", () -> d.getSuggestedFrameRate(Display.FRAME_RATE_CATEGORY_HIGH));
        }
        put(o, "peak_refresh_rate", SettingsReader.system(ctx, "peak_refresh_rate"));
        put(o, "min_refresh_rate", SettingsReader.system(ctx, "min_refresh_rate"));
        return o;
    }

    static JSONObject frameworkResources() {
        Resources sys = Resources.getSystem();
        JSONObject o = obj();
        String[] dimens = {"status_bar_height", "status_bar_height_default", "status_bar_height_portrait",
                "status_bar_height_landscape", "rounded_corner_radius", "rounded_corner_radius_top",
                "rounded_corner_radius_bottom", "navigation_bar_height", "navigation_bar_frame_height"};
        for (String name : dimens) {
            int id = sys.getIdentifier(name, "dimen", "android");
            putSafe(o, "dimen/" + name, () -> id == 0 ? "<absent>" : sys.getDimensionPixelSize(id));
        }
        String[] strings = {"config_mainBuiltInDisplayCutout", "config_mainBuiltInDisplayCutoutRectApproximation",
                "config_mainDisplayShape"};
        for (String name : strings) {
            int id = sys.getIdentifier(name, "string", "android");
            putSafe(o, "string/" + name, () -> id == 0 ? "<absent>" : sys.getString(id));
        }
        String[] bools = {"config_fillMainBuiltInDisplayCutout", "config_maskMainBuiltInDisplayCutout"};
        for (String name : bools) {
            int id = sys.getIdentifier(name, "bool", "android");
            putSafe(o, "bool/" + name, () -> id == 0 ? "<absent>" : sys.getBoolean(id));
        }
        return o;
    }

    /** SystemUI's own status bar dimensions: input for the Gate D4 collision budget. */
    static JSONObject systemUiResources(Context ctx) throws Exception {
        Resources r = ctx.getPackageManager().getResourcesForApplication("com.android.systemui");
        JSONObject o = obj();
        String[] dimens = {"status_bar_padding_start", "status_bar_padding_end", "status_bar_padding_top",
                "status_bar_icon_size", "status_bar_icon_size_sp", "status_bar_clock_size",
                "status_bar_clock_starting_padding", "status_bar_clock_end_padding",
                "status_bar_left_clock_starting_padding", "status_bar_left_clock_end_padding",
                "ongoing_appops_dot_diameter", "ongoing_appops_chip_max_width", "ongoing_appops_chip_height",
                "ongoing_activity_chip_max_width", "ongoing_activity_chip_height",
                "rounded_corner_content_padding", "status_bar_system_icons_padding_start",
                "status_bar_system_icons_padding_end", "status_bar_icon_horizontal_margin",
                "status_bar_battery_icon_height", "status_bar_battery_icon_width", "signal_cluster_battery_padding",
                "status_bar_icon_drawing_size", "notification_icon_area_padding_start", "status_bar_height"};
        for (String name : dimens) {
            int id = r.getIdentifier(name, "dimen", "com.android.systemui");
            putSafe(o, name, () -> id == 0 ? "<absent>" : r.getDimensionPixelSize(id));
        }
        return o;
    }

    // ---- Theme, text, motion, accessibility ------------------------------------------------

    static JSONObject theme(Context ctx) {
        JSONObject o = obj();
        Configuration c = ctx.getResources().getConfiguration();
        put(o, "nightModeActive", c.isNightModeActive());
        UiModeManager um = ctx.getSystemService(UiModeManager.class);
        put(o, "uiModeManagerNightMode", um.getNightMode());
        put(o, "contrast", um.getContrast());
        put(o, "theme_customization_overlay_packages",
                SettingsReader.secure(ctx, "theme_customization_overlay_packages"));
        put(o, "systemColors", colors(ctx));
        putSafe(o, "wallpaperColors", () -> wallpaperColors(ctx));
        return o;
    }

    private static final int[] COLOR_IDS = {
            android.R.color.system_accent1_100, android.R.color.system_accent1_200,
            android.R.color.system_accent1_500, android.R.color.system_accent1_600,
            android.R.color.system_accent2_500, android.R.color.system_accent3_500,
            android.R.color.system_neutral1_10, android.R.color.system_neutral1_900,
            android.R.color.system_neutral2_500,
            android.R.color.system_primary_dark, android.R.color.system_on_primary_dark,
            android.R.color.system_primary_container_dark, android.R.color.system_tertiary_dark,
            android.R.color.system_error_dark, android.R.color.system_surface_container_high_dark,
            android.R.color.system_surface_container_highest_dark, android.R.color.system_on_surface_dark,
            android.R.color.system_on_surface_variant_dark, android.R.color.system_primary_light,
            android.R.color.system_surface_container_high_light,
    };
    private static final String[] COLOR_NAMES = {
            "accent1_100", "accent1_200", "accent1_500", "accent1_600", "accent2_500", "accent3_500",
            "neutral1_10", "neutral1_900", "neutral2_500",
            "primary_dark", "on_primary_dark", "primary_container_dark", "tertiary_dark", "error_dark",
            "surface_container_high_dark", "surface_container_highest_dark", "on_surface_dark",
            "on_surface_variant_dark", "primary_light", "surface_container_high_light",
    };

    static JSONObject colors(Context ctx) {
        Resources r = ctx.getResources();
        JSONObject o = obj();
        for (int i = 0; i < COLOR_IDS.length; i++) {
            final int id = COLOR_IDS[i];
            putSafe(o, COLOR_NAMES[i], () -> hex(r.getColor(id, null)));
        }
        return o;
    }

    static JSONObject wallpaperColors(Context ctx) {
        WallpaperColors wc = WallpaperManager.getInstance(ctx).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        if (wc == null) return null;
        JSONObject o = obj();
        put(o, "primary", hex(wc.getPrimaryColor().toArgb()));
        put(o, "secondary", wc.getSecondaryColor() == null ? null : hex(wc.getSecondaryColor().toArgb()));
        put(o, "tertiary", wc.getTertiaryColor() == null ? null : hex(wc.getTertiaryColor().toArgb()));
        return o;
    }

    static JSONObject text(Context ctx) {
        JSONObject o = obj();
        Configuration c = ctx.getResources().getConfiguration();
        put(o, "fontScale", c.fontScale);
        put(o, "fontWeightAdjustment", c.fontWeightAdjustment);
        put(o, "font_scale", SettingsReader.system(ctx, Settings.System.FONT_SCALE));
        put(o, "high_text_contrast_enabled", SettingsReader.secure(ctx, "high_text_contrast_enabled"));
        return o;
    }

    static JSONObject motion(Context ctx) {
        JSONObject o = obj();
        ContentResolver cr = ctx.getContentResolver();
        put(o, "animator_duration_scale", Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f));
        put(o, "transition_animation_scale", Settings.Global.getFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f));
        put(o, "window_animation_scale", Settings.Global.getFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE, 1f));
        put(o, "areAnimatorsEnabled", ValueAnimator.areAnimatorsEnabled());
        return o;
    }

    static JSONObject accessibilityDisplay(Context ctx) {
        JSONObject o = obj();
        String[] secureKeys = {"accessibility_display_inversion_enabled", "accessibility_display_daltonizer_enabled",
                "accessibility_display_daltonizer", "reduce_bright_colors_activated", "reduce_bright_colors_level",
                "even_dimmer_activated", "accessibility_display_magnification_enabled", "high_text_contrast_enabled",
                "accessibility_enabled"};
        for (String k : secureKeys) put(o, k, SettingsReader.secure(ctx, k));
        AccessibilityManager am = ctx.getSystemService(AccessibilityManager.class);
        put(o, "managerEnabled", am.isEnabled());
        put(o, "touchExploration", am.isTouchExplorationEnabled());
        put(o, "recommendedTimeoutMs6000", am.getRecommendedTimeoutMillis(6000,
                AccessibilityManager.FLAG_CONTENT_TEXT | AccessibilityManager.FLAG_CONTENT_CONTROLS
                        | AccessibilityManager.FLAG_CONTENT_ICONS));
        JSONArray services = new JSONArray();
        for (AccessibilityServiceInfo info : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            services.put(info.getId());
        }
        put(o, "enabledServices", services);
        return o;
    }

    static JSONObject localeTime(Context ctx) {
        JSONObject o = obj();
        put(o, "is24Hour", DateFormat.is24HourFormat(ctx));
        put(o, "time_12_24", SettingsReader.system(ctx, Settings.System.TIME_12_24));
        Configuration c = ctx.getResources().getConfiguration();
        put(o, "layoutDirection", c.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? "rtl" : "ltr");
        put(o, "locales", c.getLocales().toLanguageTags());
        return o;
    }

    // ---- Power, memory, haptics, flashlight, audio -----------------------------------------

    static JSONObject power(Context ctx) {
        JSONObject o = obj();
        PowerManager pm = ctx.getSystemService(PowerManager.class);
        put(o, "powerSaveMode", pm.isPowerSaveMode());
        put(o, "thermalStatus", pm.getCurrentThermalStatus());
        putSafe(o, "thermalHeadroom10s", () -> pm.getThermalHeadroom(10));
        put(o, "interactive", pm.isInteractive());
        put(o, "deviceIdle", pm.isDeviceIdleMode());
        put(o, "ignoringBatteryOptimizations", pm.isIgnoringBatteryOptimizations(ctx.getPackageName()));
        put(o, "sustainedPerformanceSupported", pm.isSustainedPerformanceModeSupported());
        putSafe(o, "appStandbyBucket", () -> ctx.getSystemService(UsageStatsManager.class).getAppStandbyBucket());
        putSafe(o, "backgroundRestricted", () -> ctx.getSystemService(ActivityManager.class).isBackgroundRestricted());
        Intent b = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            JSONObject bat = obj();
            put(bat, "level", b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            put(bat, "scale", b.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
            put(bat, "status", b.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
            put(bat, "plugged", b.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1));
            put(bat, "temperatureDeciC", b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));
            put(bat, "cycleCount", b.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1));
            put(bat, "chargingStatus", b.getIntExtra(BatteryManager.EXTRA_CHARGING_STATUS, -1));
            put(o, "batteryIntent", bat);
        }
        BatteryManager bm = ctx.getSystemService(BatteryManager.class);
        put(o, "capacity", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        put(o, "isCharging", bm.isCharging());
        return o;
    }

    static JSONObject memory(Context ctx) {
        JSONObject o = obj();
        ActivityManager am = ctx.getSystemService(ActivityManager.class);
        put(o, "isLowRamDevice", am.isLowRamDevice());
        put(o, "memoryClassMb", am.getMemoryClass());
        put(o, "largeMemoryClassMb", am.getLargeMemoryClass());
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        put(o, "totalMemMb", mi.totalMem / (1024 * 1024));
        put(o, "availMemMb", mi.availMem / (1024 * 1024));
        put(o, "lowMemory", mi.lowMemory);
        return o;
    }

    static JSONObject haptics(Context ctx) {
        JSONObject o = obj();
        VibratorManager vm = ctx.getSystemService(VibratorManager.class);
        JSONArray ids = new JSONArray();
        for (int id : vm.getVibratorIds()) ids.put(id);
        put(o, "vibratorIds", ids);
        Vibrator v = vm.getDefaultVibrator();
        put(o, "hasVibrator", v.hasVibrator());
        put(o, "hasAmplitudeControl", v.hasAmplitudeControl());
        int[] prims = {VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_SPIN, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_LOW_TICK};
        String[] names = {"CLICK", "THUD", "SPIN", "QUICK_RISE", "SLOW_RISE", "QUICK_FALL", "TICK", "LOW_TICK"};
        boolean[] sup = v.arePrimitivesSupported(prims);
        int[] dur = v.getPrimitiveDurations(prims);
        JSONObject p = obj();
        for (int i = 0; i < prims.length; i++) put(p, names[i], sup[i] ? dur[i] + "ms" : "unsupported");
        put(o, "primitives", p);
        int[] eff = v.areEffectsSupported(VibrationEffect.EFFECT_CLICK, VibrationEffect.EFFECT_DOUBLE_CLICK,
                VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_HEAVY_CLICK);
        JSONObject e = obj();
        String[] effNames = {"CLICK", "DOUBLE_CLICK", "TICK", "HEAVY_CLICK"};
        for (int i = 0; i < eff.length; i++) put(e, effNames[i], eff[i]);
        put(o, "effectsSupport(0=unknown,1=yes,2=no)", e);
        put(o, "resonantFrequency", v.getResonantFrequency());
        put(o, "qFactor", v.getQFactor());
        if (Build.VERSION.SDK_INT >= 36) putSafe(o, "envelopeEffectsSupported", v::areEnvelopeEffectsSupported);
        put(o, "haptic_feedback_enabled", SettingsReader.system(ctx, Settings.System.HAPTIC_FEEDBACK_ENABLED));
        return o;
    }

    static JSONArray flashlight(Context ctx) throws Exception {
        CameraManager cm = ctx.getSystemService(CameraManager.class);
        JSONArray a = new JSONArray();
        for (String id : cm.getCameraIdList()) {
            CameraCharacteristics ch = cm.getCameraCharacteristics(id);
            JSONObject c = obj();
            put(c, "id", id);
            put(c, "facing", ch.get(CameraCharacteristics.LENS_FACING));
            put(c, "flashAvailable", ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            put(c, "strengthMax", ch.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL));
            put(c, "strengthDefault", ch.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL));
            a.put(c);
        }
        return a;
    }

    static JSONObject audio(Context ctx) {
        JSONObject o = obj();
        AudioManager am = ctx.getSystemService(AudioManager.class);
        put(o, "mode", am.getMode());
        put(o, "ringerMode", am.getRingerMode());
        put(o, "isMusicActive", am.isMusicActive());
        JSONArray play = new JSONArray();
        for (AudioPlaybackConfiguration pc : am.getActivePlaybackConfigurations()) {
            JSONObject p = obj();
            put(p, "usage", pc.getAudioAttributes().getUsage());
            put(p, "contentType", pc.getAudioAttributes().getContentType());
            play.put(p);
        }
        put(o, "activePlayback", play);
        put(o, "activeRecording", recordings(am.getActiveRecordingConfigurations()));
        JSONArray outs = new JSONArray();
        for (AudioDeviceInfo di : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            outs.put(di.getType() + ":" + di.getProductName());
        }
        put(o, "outputs", outs);
        Spatializer sp = am.getSpatializer();
        put(o, "spatializerAvailable", sp.isAvailable());
        put(o, "spatializerEnabled", sp.isEnabled());
        put(o, "musicVolume", am.getStreamVolume(AudioManager.STREAM_MUSIC) + "/" + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        return o;
    }

    static JSONArray recordings(List<AudioRecordingConfiguration> list) {
        JSONArray rec = new JSONArray();
        for (AudioRecordingConfiguration rc : list) {
            JSONObject r = obj();
            put(r, "clientSource", rc.getClientAudioSource());
            put(r, "silenced", rc.isClientSilenced());
            put(r, "sessionId", rc.getClientAudioSessionId());
            rec.put(r);
        }
        return rec;
    }

    // ---- Notifications, System UI, security ------------------------------------------------

    static JSONObject notifications(Context ctx) {
        JSONObject o = obj();
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        put(o, "interruptionFilter", nm.getCurrentInterruptionFilter());
        put(o, "areNotificationsEnabled", nm.areNotificationsEnabled());
        if (Build.VERSION.SDK_INT >= 36) putSafe(o, "canPostPromotedNotifications", nm::canPostPromotedNotifications);
        put(o, "listenerAccessGranted", nm.isNotificationListenerAccessGranted(new ComponentName(ctx, ProbeListener.class)));
        put(o, "policyAccessGranted", nm.isNotificationPolicyAccessGranted());
        put(o, "zen_mode", SettingsReader.global(ctx, "zen_mode"));
        put(o, "heads_up_notifications_enabled", SettingsReader.global(ctx, "heads_up_notifications_enabled"));
        String[] secureKeys = {"lock_screen_show_notifications", "lock_screen_allow_private_notifications",
                "lock_screen_show_silent_notifications", "notification_history_enabled", "notification_badging"};
        for (String k : secureKeys) put(o, k, SettingsReader.secure(ctx, k));
        KeyguardManager km = ctx.getSystemService(KeyguardManager.class);
        put(o, "keyguardLocked", km.isKeyguardLocked());
        put(o, "deviceLocked", km.isDeviceLocked());
        put(o, "deviceSecure", km.isDeviceSecure());
        return o;
    }

    static JSONObject systemUi(Context ctx) {
        JSONObject o = obj();
        PackageManager pm = ctx.getPackageManager();
        ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY);
        String homePkg = home == null ? null : home.activityInfo.packageName;
        put(o, "homePackage", homePkg);
        put(o, "icon_blacklist", SettingsReader.secure(ctx, "icon_blacklist"));
        put(o, "navigation_mode", SettingsReader.secure(ctx, "navigation_mode"));
        put(o, "status_bar_show_battery_percent", SettingsReader.system(ctx, "status_bar_show_battery_percent"));
        LinkedHashSet<String> auths = new LinkedHashSet<>();
        if (homePkg != null) auths.add(homePkg + ".grid_control");
        auths.add("com.google.android.apps.nexuslauncher.grid_control");
        auths.add("com.android.launcher3.grid_control");
        JSONObject themed = obj();
        for (String auth : auths) putSafe(themed, auth, () -> themedIconsQuery(ctx, pm, auth));
        put(o, "themedIconsProvider", themed);
        return o;
    }

    private static JSONObject themedIconsQuery(Context ctx, PackageManager pm, String auth) {
        JSONObject r = obj();
        ProviderInfo pi = pm.resolveContentProvider(auth, 0);
        put(r, "providerFound", pi != null);
        if (pi != null) {
            put(r, "readPermission", pi.readPermission);
            put(r, "exported", pi.exported);
        }
        put(r, "query", safe(() -> {
            try (Cursor c = ctx.getContentResolver().query(Uri.parse("content://" + auth + "/icon_themed"),
                    null, null, null, null)) {
                if (c == null) return "null cursor";
                JSONObject row = obj();
                if (c.moveToFirst()) {
                    for (int i = 0; i < c.getColumnCount(); i++) put(row, c.getColumnName(i), c.getString(i));
                }
                return row;
            }
        }));
        return r;
    }

    static JSONObject security(Context ctx) {
        JSONObject o = obj();
        if (Build.VERSION.SDK_INT >= 36) {
            putSafe(o, "advancedProtectionServicePresent",
                    () -> ctx.getSystemService(AdvancedProtectionManager.class) != null);
            putSafe(o, "advancedProtectionEnabled",
                    () -> ctx.getSystemService(AdvancedProtectionManager.class).isAdvancedProtectionEnabled());
        }
        // Android 17 AAPM applies this (hidden-key) user restriction to block non-tool accessibility services.
        putSafe(o, "restriction_no_non_tool_accessibility_service", () -> ctx.getSystemService(android.os.UserManager.class)
                .hasUserRestriction("no_non_tool_accessibility_service"));
        putSafe(o, "installSource", () -> {
            InstallSourceInfo i = ctx.getPackageManager().getInstallSourceInfo(ctx.getPackageName());
            JSONObject s = obj();
            put(s, "installing", i.getInstallingPackageName());
            put(s, "initiating", i.getInitiatingPackageName());
            put(s, "packageSource", i.getPackageSource());
            return s;
        });
        putSafe(o, "accessRestrictedSettingsOpMode", () -> ctx.getSystemService(AppOpsManager.class)
                .unsafeCheckOpNoThrow("android:access_restricted_settings", Process.myUid(), ctx.getPackageName()));
        return o;
    }

    // ---- Fonts -------------------------------------------------------------------------------

    static JSONObject fonts() {
        JSONObject o = obj();
        JSONObject fam = obj();
        String[] names = {"google-sans", "google-sans-text", "google-sans-medium", "google-sans-bold",
                "google-sans-flex", "google-sans-display", "google-sans-clock", "google-sans-text-medium",
                "roboto-flex", "sans-serif", "sans-serif-medium", "sans-serif-flex"};
        for (String name : names) {
            Typeface t = Typeface.create(name, Typeface.NORMAL);
            JSONObject f = obj();
            put(f, "isDefault", t == Typeface.DEFAULT || t.equals(Typeface.DEFAULT));
            putSafe(f, "systemFontFamilyName", t::getSystemFontFamilyName);
            put(f, "weight", t.getWeight());
            put(fam, name, f);
        }
        put(o, "families", fam);
        putSafe(o, "tabularFigures", DeviceProbe::tabularFigures);
        putSafe(o, "defaultSystemFamily", Typeface.DEFAULT::getSystemFontFamilyName);
        putSafe(o, "fontFiles", () -> {
            TreeSet<String> files = new TreeSet<>();
            for (Font f : SystemFonts.getAvailableFonts()) {
                File file = f.getFile();
                if (file == null) continue;
                String n = file.getName().toLowerCase(Locale.US);
                if (n.contains("google") || n.contains("flex") || n.startsWith("roboto")) files.add(file.getAbsolutePath());
            }
            return new JSONArray(files);
        });
        for (String path : new String[]{"/system/etc/fonts.xml", "/system/etc/font_fallback.xml",
                "/product/etc/fonts_customization.xml"}) {
            putSafe(o, path, () -> familyNames(new File(path)));
        }
        return o;
    }

    /**
     * Section 7.4: digit advance widths with and without 'tnum' (equal widths = tabular figures work), and whether
     * the variable axes the island would animate are accepted.
     */
    private static JSONObject tabularFigures() {
        JSONObject o = obj();
        for (String name : new String[]{"google-sans-flex", "google-sans-text", "google-sans", "google-sans-clock", "sans-serif"}) {
            android.graphics.Paint p = new android.graphics.Paint();
            p.setTypeface(Typeface.create(name, Typeface.NORMAL));
            p.setTextSize(100f);
            JSONObject f = obj();
            put(f, "proportionalDigitWidthSpread", digitSpread(p));
            p.setFontFeatureSettings("'tnum'");
            put(f, "tnumDigitWidthSpread", digitSpread(p));
            p.setFontFeatureSettings(null);
            put(f, "wght650Accepted", p.setFontVariationSettings("'wght' 650"));
            put(f, "rondAccepted", p.setFontVariationSettings("'ROND' 100"));
            put(f, "wdthAccepted", p.setFontVariationSettings("'wdth' 90"));
            put(o, name, f);
        }
        return o;
    }

    private static double digitSpread(android.graphics.Paint p) {
        float min = Float.MAX_VALUE;
        float max = 0f;
        for (char c = '0'; c <= '9'; c++) {
            float w = p.measureText(String.valueOf(c));
            min = Math.min(min, w);
            max = Math.max(max, w);
        }
        return Math.round((max - min) * 100) / 100.0;
    }

    private static JSONObject familyNames(File f) throws IOException {
        JSONObject o = obj();
        put(o, "readable", f.canRead());
        if (!f.canRead()) return o;
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        put(o, "families", new JSONArray(matches(xml, "<family[^>]*\\bname=\"([^\"]+)\"")));
        put(o, "aliases", new JSONArray(matches(xml, "<alias[^>]*\\bname=\"([^\"]+)\"")));
        return o;
    }

    private static TreeSet<String> matches(String text, String regex) {
        TreeSet<String> out = new TreeSet<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    // ---- Media output switcher, settings tables ---------------------------------------------

    static JSONObject mediaOutput(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        JSONObject o = obj();
        String[] receivers = {"com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG",
                "com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG",
                "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_BROADCAST_DIALOG"};
        for (String action : receivers) {
            JSONArray a = new JSONArray();
            for (ResolveInfo ri : pm.queryBroadcastReceivers(new Intent(action), 0)) a.put(component(ri.activityInfo));
            put(o, "receiver:" + action, a);
        }
        String[] activities = {"com.android.settings.panel.action.MEDIA_OUTPUT", Settings.Panel.ACTION_VOLUME,
                "com.android.settings.panel.action.VOLUME"};
        for (String action : activities) {
            JSONArray a = new JSONArray();
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(action), 0)) a.put(component(ri.activityInfo));
            put(o, "activity:" + action, a);
        }
        return o;
    }

    private static JSONObject component(ActivityInfo ai) {
        JSONObject o = obj();
        put(o, "component", ai.packageName + "/" + ai.name);
        put(o, "exported", ai.exported);
        put(o, "permission", ai.permission);
        put(o, "enabled", ai.enabled);
        return o;
    }

    /** Key names only (never values): shows which settings a third-party app can enumerate. */
    static JSONObject settingsTables(Context ctx) {
        JSONObject o = obj();
        Uri[] uris = {Settings.System.CONTENT_URI, Settings.Secure.CONTENT_URI, Settings.Global.CONTENT_URI};
        String[] names = {"system", "secure", "global"};
        for (int i = 0; i < uris.length; i++) {
            final Uri u = uris[i];
            putSafe(o, names[i], () -> {
                try (Cursor c = ctx.getContentResolver().query(u, new String[]{"name"}, null, null, null)) {
                    if (c == null) return "null cursor";
                    JSONArray keys = new JSONArray();
                    while (c.moveToNext()) keys.put(c.getString(0));
                    return keys;
                }
            });
        }
        return o;
    }
}
