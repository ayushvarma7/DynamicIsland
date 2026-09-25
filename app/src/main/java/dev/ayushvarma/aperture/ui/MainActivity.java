package dev.ayushvarma.aperture.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.IslandHub;
import dev.ayushvarma.aperture.core.engine.ActivityKind;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.overlay.IslandAccessibilityService;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.source.NotificationSource;
import dev.ayushvarma.aperture.source.Payload;

/** Onboarding, health status (Section 8) and every island setting, applied live. */
public final class MainActivity extends Activity {
    private TextView a11yStatus;
    private TextView listenerStatus;
    private TextView aapm;
    private IslandSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApertureApp.of(this).settings();
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.app_name), R.style.TextAppearance_Material3_DisplaySmall));
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_subtitle), R.style.TextAppearance_Material3_BodyLarge));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_setup));
        aapm = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_aapm), R.style.TextAppearance_Material3_BodyMedium);
        aapm.setTextColor(getColor(android.R.color.system_error_light));
        page.addView(aapm);
        LinearLayout a11y = Ui.card(page);
        a11y.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_a11y_title), R.style.TextAppearance_Material3_TitleSmall));
        a11yStatus = Ui.text(this, "", R.style.TextAppearance_Material3_BodyMedium);
        a11y.addView(a11yStatus);
        a11y.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_disclosure), R.style.TextAppearance_Material3_BodySmall));
        Ui.button(a11y, getString(dev.ayushvarma.aperture.R.string.main_open_settings),
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout listener = Ui.card(page);
        listener.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_listener_title), R.style.TextAppearance_Material3_TitleSmall));
        listenerStatus = Ui.text(this, "", R.style.TextAppearance_Material3_BodyMedium);
        listener.addView(listenerStatus);
        Ui.button(listener, getString(dev.ayushvarma.aperture.R.string.main_open_settings), v -> openListenerSettings());

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_appearance));
        LinearLayout look = Ui.card(page);
        look.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_idle), R.style.TextAppearance_Material3_LabelLarge));
        Ui.toggles(look, settings.idleStyle().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_idle_ring,
                dev.ayushvarma.aperture.R.string.main_idle_invisible, dev.ayushvarma.aperture.R.string.main_idle_hidden),
                i -> settings.setEnum("idle", IslandSettings.IdleStyle.values()[i]));
        look.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_body), R.style.TextAppearance_Material3_LabelLarge));
        Ui.toggles(look, settings.body().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_body_black,
                dev.ayushvarma.aperture.R.string.main_body_tonal),
                i -> settings.setEnum("body", IslandSettings.Body.values()[i]));
        look.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_motion), R.style.TextAppearance_Material3_LabelLarge));
        Ui.toggles(look, settings.motion().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_motion_expressive,
                dev.ayushvarma.aperture.R.string.main_motion_standard, dev.ayushvarma.aperture.R.string.main_motion_fluid),
                i -> settings.setEnum("motion", IslandSettings.Motion.values()[i]));
        Ui.button(look, getString(dev.ayushvarma.aperture.R.string.main_calibrate),
                v -> startActivity(new Intent(this, CalibrationActivity.class)));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_behavior));
        LinearLayout behavior = Ui.card(page);
        toggle(behavior, dev.ayushvarma.aperture.R.string.main_show_media, "media", settings.showMedia());
        toggle(behavior, dev.ayushvarma.aperture.R.string.main_show_notifications, "notifications", settings.showNotifications());
        toggle(behavior, dev.ayushvarma.aperture.R.string.main_haptics, "haptics", settings.haptics());
        TextView timeLabel = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_notification_time,
                settings.notificationSeconds()), R.style.TextAppearance_Material3_LabelLarge);
        behavior.addView(timeLabel);
        Slider time = new Slider(this);
        time.setValueFrom(1);
        time.setValueTo(12);
        time.setStepSize(1);
        time.setValue(settings.notificationSeconds());
        time.addOnChangeListener((s, value, fromUser) -> {
            settings.setInt("notif_seconds", Math.round(value));
            timeLabel.setText(getString(dev.ayushvarma.aperture.R.string.main_notification_time, Math.round(value)));
        });
        behavior.addView(time);
        behavior.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_double_tap), R.style.TextAppearance_Material3_LabelLarge));
        Ui.toggles(behavior, settings.doubleTap().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_double_torch,
                dev.ayushvarma.aperture.R.string.main_double_resume, dev.ayushvarma.aperture.R.string.main_double_nothing),
                i -> settings.setEnum("double_tap", IslandSettings.DoubleTap.values()[i]));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_try));
        LinearLayout demo = Ui.card(page);
        demo.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_try_hint), R.style.TextAppearance_Material3_BodySmall));
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_notification), v -> demoMessage());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_timer), v -> demoTimer());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_charging), v -> demoCharging());
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.main_device_report),
                v -> startActivity(new Intent(this, DeviceReportActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean overlay = isOverlayEnabled();
        boolean listener = getSystemService(NotificationManager.class)
                .isNotificationListenerAccessGranted(new ComponentName(this, NotificationSource.class));
        a11yStatus.setText(overlay ? dev.ayushvarma.aperture.R.string.main_a11y_on : dev.ayushvarma.aperture.R.string.main_a11y_off);
        listenerStatus.setText(listener ? dev.ayushvarma.aperture.R.string.main_listener_on : dev.ayushvarma.aperture.R.string.main_listener_off);
        aapm.setVisibility(isAdvancedProtectionOn() ? TextView.VISIBLE : TextView.GONE);
    }

    private boolean isOverlayEnabled() {
        ComponentName me = new ComponentName(this, IslandAccessibilityService.class);
        for (AccessibilityServiceInfo info : getSystemService(AccessibilityManager.class)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (me.flattenToString().equals(info.getId())
                    || me.flattenToShortString().equals(info.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Rule S3: Android 17 Advanced Protection disables every non-tool accessibility service. */
    private boolean isAdvancedProtectionOn() {
        if (Build.VERSION.SDK_INT < 36) return false;
        AdvancedProtectionManager apm = getSystemService(AdvancedProtectionManager.class);
        try {
            return apm != null && apm.isAdvancedProtectionEnabled();
        } catch (SecurityException e) {
            return false;
        }
    }

    private void openListenerSettings() {
        Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(this, NotificationSource.class).flattenToString());
        try {
            startActivity(detail);
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    private void toggle(LinearLayout parent, int label, String key, boolean value) {
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(label);
        s.setChecked(value);
        s.setOnCheckedChangeListener((b, checked) -> settings.setBoolean(key, checked));
        parent.addView(s);
    }

    private CharSequence[] labels(int... ids) {
        CharSequence[] out = new CharSequence[ids.length];
        for (int i = 0; i < ids.length; i++) out[i] = getString(ids[i]);
        return out;
    }

    private IslandHub hub() {
        return ApertureApp.of(this).hub();
    }

    private Payload demoPayload() {
        Payload p = new Payload();
        p.appIcon = getApplicationInfo().loadIcon(getPackageManager());
        p.appName = getString(dev.ayushvarma.aperture.R.string.app_name);
        return p;
    }

    private void demoMessage() {
        long now = System.currentTimeMillis();
        hub().postTransient(IslandActivity.builder("demo:msg:" + now, getPackageName(), ActivityKind.NOTIFICATION)
                .title(getString(dev.ayushvarma.aperture.R.string.demo_sender))
                .text(getString(dev.ayushvarma.aperture.R.string.demo_message))
                .payload(demoPayload()).createdAt(now).updatedAt(now).build(), 6_000);
    }

    private void demoTimer() {
        long now = System.currentTimeMillis();
        hub().upsert(IslandActivity.builder("demo:timer", getPackageName(), ActivityKind.TIMER)
                .title(getString(dev.ayushvarma.aperture.R.string.demo_timer)).chronometer(now + 25_000, true)
                .payload(demoPayload()).createdAt(now).updatedAt(now).build());
        getWindow().getDecorView().postDelayed(() -> hub().remove("demo:timer"), 27_000);
    }

    private void demoCharging() {
        long now = System.currentTimeMillis();
        hub().postTransient(IslandActivity.builder("sys:charging", getPackageName(), ActivityKind.SYSTEM)
                .title(getString(dev.ayushvarma.aperture.R.string.island_charging)).shortText("82%").progress(0.82f)
                .createdAt(now).updatedAt(now).build(), 3_000);
    }
}
