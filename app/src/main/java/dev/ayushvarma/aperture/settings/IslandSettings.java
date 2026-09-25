package dev.ayushvarma.aperture.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.CopyOnWriteArrayList;

import dev.ayushvarma.aperture.core.geometry.Calibration;

/** Every user-adjustable value, with its default. Changes notify listeners so the overlay updates live. */
public final class IslandSettings {
    public enum IdleStyle { RING, INVISIBLE, HIDDEN }
    public enum Motion { EXPRESSIVE, STANDARD, FLUID }
    public enum Body { BLACK, TONAL }
    public enum DoubleTap { FLASHLIGHT, RESUME_MEDIA, NOTHING }

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

    public boolean showNotifications() {
        return prefs.getBoolean("notifications", true);
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
