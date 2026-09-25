package dev.ayushvarma.aperture.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.ayushvarma.aperture.core.geometry.Calibration;

/** Every user-adjustable value, with its default. Changes notify listeners so the overlay updates live. */
public final class IslandSettings {
    public enum IdleStyle { RING, INVISIBLE, HIDDEN }
    public enum Motion { EXPRESSIVE, STANDARD, FLUID }
    /** Expanded card style. The compact pill is always black so it blends with the camera. */
    public enum Body { BLACK, GLASS, CLEAR, ARTWORK, TONAL }
    public enum DoubleTap { FLASHLIGHT, RESUME_MEDIA, NOTHING }
    /** What a tap on the pill does; long-press does the other. */
    public enum TapAction { EXPAND, OPEN_APP }
    /** Lock screen: show new events briefly then step aside, or show only calls. */
    public enum LockScreen { BRIEF, CALLS_ONLY }

    /** Pre-selected in the app picker: the apps an island is most useful for, when installed. */
    public static final List<String> SUGGESTED_APPS = Collections.unmodifiableList(Arrays.asList(
            "com.google.android.apps.youtube.music", "com.spotify.music", "com.google.android.youtube",
            "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging", "com.google.android.dialer",
            "com.google.android.deskclock", "com.google.android.apps.maps", "com.google.android.gm",
            "org.telegram.messenger", "au.com.shiftyjelly.pocketcasts", "com.audible.application",
            "com.amazon.mp3", "com.soundcloud.android", "com.apple.android.music", "deezer.android.app"));

    public interface Listener {
        void onSettingsChanged();
    }

    private static final String CAL_X = "cal_x";
    private static final String CAL_Y = "cal_y";
    private static final String CAL_SIZE = "cal_size";
    private static final String CAL_GAP = "cal_gap";
    private static final String CAL_WIDTH = "cal_width";

    private final SharedPreferences prefs;
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private boolean calibrationPreview;

    public IslandSettings(Context ctx) {
        prefs = ctx.getSharedPreferences("island", Context.MODE_PRIVATE);
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void changed() {
        for (Listener l : listeners) l.onSettingsChanged();
    }

    public Calibration calibration() {
        return new Calibration(prefs.getFloat(CAL_X, 0f), prefs.getFloat(CAL_Y, 0f), prefs.getFloat(CAL_SIZE, 0f),
                prefs.getFloat(CAL_GAP, 3f), prefs.getFloat(CAL_WIDTH, 1f));
    }

    public void setCalibration(Calibration c) {
        prefs.edit().putFloat(CAL_X, c.offsetXDp).putFloat(CAL_Y, c.offsetYDp).putFloat(CAL_SIZE, c.sizeDp)
                .putFloat(CAL_GAP, c.gapDp).putFloat(CAL_WIDTH, c.compactWidthScale).apply();
        changed();
    }

    public void resetCalibration() {
        setCalibration(Calibration.DEFAULT);
    }

    /** While the calibration screen is open the island draws the measured cutout outline. Not persisted. */
    public boolean calibrationPreview() {
        return calibrationPreview;
    }

    public void setCalibrationPreview(boolean on) {
        calibrationPreview = on;
        changed();
    }

    public IdleStyle idleStyle() {
        return IdleStyle.valueOf(prefs.getString("idle", IdleStyle.RING.name()));
    }

    public Motion motion() {
        return Motion.valueOf(prefs.getString("motion", Motion.EXPRESSIVE.name()));
    }

    public Body body() {
        return Body.valueOf(prefs.getString("body", Body.BLACK.name()));
    }

    public DoubleTap doubleTap() {
        return DoubleTap.valueOf(prefs.getString("double_tap", DoubleTap.FLASHLIGHT.name()));
    }

    /** Apps allowed to use the island. Until the user chooses, the suggested apps are allowed. */
    public Set<String> allowedApps() {
        return new HashSet<>(prefs.getStringSet("allowed_apps", new HashSet<>(SUGGESTED_APPS)));
    }

    public boolean isAllowed(String pkg) {
        return pkg != null && allowedApps().contains(pkg);
    }

    public void setAllowed(String pkg, boolean allowed) {
        Set<String> apps = allowedApps();
        if (allowed) apps.add(pkg);
        else apps.remove(pkg);
        prefs.edit().putStringSet("allowed_apps", apps).apply();
        changed();
    }

    /** True once the user has confirmed the app list in setup. */
    public boolean appsChosen() {
        return prefs.getBoolean("apps_chosen", false);
    }

    public void setAppsChosen() {
        prefs.edit().putBoolean("apps_chosen", true).apply();
        changed();
    }

    public TapAction tapAction() {
        return TapAction.valueOf(prefs.getString("tap_action", TapAction.EXPAND.name()));
    }

    public LockScreen lockScreen() {
        // Default: only calls on the lock screen. Android's own lock-screen player already shows music.
        return LockScreen.valueOf(prefs.getString("lock_screen", LockScreen.CALLS_ONLY.name()));
    }

    /**
     * Seconds a pill stays fully drawn after its last change before it shrinks into the camera halo (burn-in
     * protection). 0 means never.
     */
    public int glanceSeconds() {
        return prefs.getInt("glance_seconds", 8);
    }

    /** Tint progress, bars and the play button with a color picked from the album art. */
    public boolean artworkAccent() {
        return prefs.getBoolean("artwork_accent", true);
    }

    /** Theme of the Aperture app itself (the island always follows the system palette). */
    public enum AppTheme { SYSTEM, LIGHT, DARK }

    public AppTheme appTheme() {
        return AppTheme.valueOf(prefs.getString("app_theme", AppTheme.SYSTEM.name()));
    }

    /**
     * Applies the app theme through Android's per-app night mode (API 31), which the system persists and applies
     * before our activities start, so there is no flash of the wrong theme.
     */
    public void setAppTheme(Context ctx, AppTheme theme) {
        prefs.edit().putString("app_theme", theme.name()).apply();
        int mode = theme == AppTheme.LIGHT ? android.app.UiModeManager.MODE_NIGHT_NO
                : theme == AppTheme.DARK ? android.app.UiModeManager.MODE_NIGHT_YES
                : android.app.UiModeManager.MODE_NIGHT_AUTO;
        ctx.getSystemService(android.app.UiModeManager.class).setApplicationNightMode(mode);
        changed();
    }

    /** True once the first-run introduction has been completed or skipped. */
    public boolean onboarded() {
        return prefs.getBoolean("onboarded", false);
    }

    public void setOnboarded(boolean done) {
        prefs.edit().putBoolean("onboarded", done).apply();
    }

    /** Master switch: pause the island without revoking any permission. */
    public boolean enabled() {
        return prefs.getBoolean("enabled", true);
    }

    /** Hide an app's activity while that app is on screen (like iPhone). */
    public boolean hideInForeground() {
        return prefs.getBoolean("hide_foreground", true);
    }

    /** Seconds before an expanded card collapses on its own; 0 = stay open until dismissed. */
    public int expandedSeconds() {
        return prefs.getInt("expanded_seconds", 6);
    }

    public boolean collapseOnOutsideTouch() {
        return prefs.getBoolean("collapse_outside", true);
    }

    /** Sideways swipe on the music pill skips tracks instead of switching activities. */
    public boolean swipeChangesSong() {
        return prefs.getBoolean("swipe_song", true);
    }

    /** Don't queue message pop-ups that arrive while the screen is off (they'd be stale when you look). */
    public boolean ignoreWhileScreenOff() {
        return prefs.getBoolean("ignore_screen_off", true);
    }

    /** Skip notifications Android marks as sensitive, such as one-time codes. */
    public boolean hideSensitive() {
        return prefs.getBoolean("hide_sensitive", true);
    }

    /** Every stored value, for "Share settings" and feedback. */
    public String export() {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, ?> e : new java.util.TreeMap<>(prefs.getAll()).entrySet()) {
            sb.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    public boolean showSystemEvents() {
        return prefs.getBoolean("system_events", false);
    }

    public boolean showNotifications() {
        // Off by default: message pop-ups are high-frequency. The island is for music, timers, deliveries and calls.
        return prefs.getBoolean("notifications", false);
    }

    public boolean showMedia() {
        return prefs.getBoolean("media", true);
    }

    public boolean haptics() {
        return prefs.getBoolean("haptics", true);
    }

    /** Base heads-up time; Section 6.5 adds 1 s per 40 characters, capped at base + 3 s. */
    public int notificationSeconds() {
        return prefs.getInt("notif_seconds", 4);
    }

    public void setEnum(String key, Enum<?> value) {
        prefs.edit().putString(key, value.name()).apply();
        changed();
    }

    public void setBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
        changed();
    }

    public void setInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
        changed();
    }
}
