package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.material.R;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.settings.IslandSettings;

/** Every technical knob, kept off the main screen so it stays simple for most people. */
public final class AdvancedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IslandSettings s = ApertureApp.of(this).settings();
        SettingRows rows = new SettingRows(this, s);
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.advanced_title), R.style.TextAppearance_Material3_HeadlineMedium));
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.advanced_hint), R.style.TextAppearance_Material3_BodyMedium));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.advanced_look));
        LinearLayout look = Ui.card(page);
        rows.label(look, dev.ayushvarma.aperture.R.string.main_idle);
        Ui.toggles(look, s.idleStyle().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_idle_ring,
                dev.ayushvarma.aperture.R.string.main_idle_invisible, dev.ayushvarma.aperture.R.string.main_idle_hidden),
                i -> s.setEnum("idle", IslandSettings.IdleStyle.values()[i]));
        rows.label(look, dev.ayushvarma.aperture.R.string.main_motion);
        Ui.toggles(look, s.motion().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_motion_expressive,
                dev.ayushvarma.aperture.R.string.main_motion_standard, dev.ayushvarma.aperture.R.string.main_motion_fluid),
                i -> s.setEnum("motion", IslandSettings.Motion.values()[i]));
        Ui.button(look, getString(dev.ayushvarma.aperture.R.string.main_calibrate),
                v -> startActivity(new Intent(this, CalibrationActivity.class)));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.advanced_gestures));
        LinearLayout g = Ui.card(page);
        rows.label(g, dev.ayushvarma.aperture.R.string.main_tap);
        Ui.toggles(g, s.tapAction().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_tap_expand,
                dev.ayushvarma.aperture.R.string.main_tap_open),
                i -> s.setEnum("tap_action", IslandSettings.TapAction.values()[i]));
        rows.toggle(g, dev.ayushvarma.aperture.R.string.main_swipe_song, "swipe_song", s.swipeChangesSong());
        rows.label(g, dev.ayushvarma.aperture.R.string.main_double_tap);
        Ui.toggles(g, s.doubleTap().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_double_torch,
                dev.ayushvarma.aperture.R.string.main_double_resume, dev.ayushvarma.aperture.R.string.main_double_nothing),
                i -> s.setEnum("double_tap", IslandSettings.DoubleTap.values()[i]));
        rows.toggle(g, dev.ayushvarma.aperture.R.string.main_haptics, "haptics", s.haptics());

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.advanced_timing));
        LinearLayout t = Ui.card(page);
        rows.slider(t, dev.ayushvarma.aperture.R.string.main_glance, dev.ayushvarma.aperture.R.string.main_glance_never,
                "glance_seconds", s.glanceSeconds(), 0, 30);
        rows.hint(t, dev.ayushvarma.aperture.R.string.main_glance_hint);
        rows.slider(t, dev.ayushvarma.aperture.R.string.main_expanded_time, dev.ayushvarma.aperture.R.string.main_expanded_never,
                "expanded_seconds", s.expandedSeconds(), 0, 30);
        rows.toggle(t, dev.ayushvarma.aperture.R.string.main_collapse_outside, "collapse_outside", s.collapseOnOutsideTouch());
        rows.slider(t, dev.ayushvarma.aperture.R.string.main_notification_time, 0, "notif_seconds", s.notificationSeconds(), 1, 12);

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.advanced_filters));
        LinearLayout f = Ui.card(page);
        rows.label(f, dev.ayushvarma.aperture.R.string.main_lock);
        Ui.toggles(f, s.lockScreen().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_lock_brief,
                dev.ayushvarma.aperture.R.string.main_lock_calls),
                i -> s.setEnum("lock_screen", IslandSettings.LockScreen.values()[i]));
        rows.toggle(f, dev.ayushvarma.aperture.R.string.main_ignore_screen_off, "ignore_screen_off", s.ignoreWhileScreenOff());
        rows.toggle(f, dev.ayushvarma.aperture.R.string.main_hide_sensitive, "hide_sensitive", s.hideSensitive());
        rows.toggle(f, dev.ayushvarma.aperture.R.string.main_show_system, "system_events", s.showSystemEvents());

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.advanced_diagnostics));
        LinearLayout d = Ui.card(page);
        Ui.button(d, getString(dev.ayushvarma.aperture.R.string.main_device_report),
                v -> startActivity(new Intent(this, DeviceReportActivity.class)));
        Ui.button(d, getString(dev.ayushvarma.aperture.R.string.help_share_settings), v -> startActivity(
                Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, s.export()), null)));
    }
}
