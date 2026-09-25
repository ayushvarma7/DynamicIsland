package dev.ayushvarma.aperture.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import dev.ayushvarma.aperture.theme.Fonts;

/**
 * Setup and settings. Setup is three steps (overlay, notification access, app choice); until all three are done
 * the banner is red and says what is missing, then it turns green (Section 8 health status).
 */
public final class MainActivity extends Activity {
    private static final int RED = 0xFFD93025;
    private static final int GREEN = 0xFF1E8E3E;

    private IslandSettings settings;
    private MaterialCardView banner;
    private TextView bannerTitle;
    private TextView bannerText;
    private Step overlayStep;
    private Step listenerStep;
    private Step appsStep;
    private TextView aapm;
    private boolean pickerOffered;

    /** One setup step: a status dot, a title, a line of status and an action button. */
    private final class Step {
        final LinearLayout card;
        final View dot;
        final TextView status;
        final MaterialButton action;

        Step(LinearLayout parent, int title, View.OnClickListener onClick) {
            card = Ui.card(parent);
            LinearLayout head = new LinearLayout(MainActivity.this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            dot = new View(MainActivity.this);
            int d = Ui.dp(MainActivity.this, 12);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(d, d);
            dlp.setMarginEnd(Ui.dp(MainActivity.this, 10));
            head.addView(dot, dlp);
            head.addView(Ui.text(MainActivity.this, getString(title), R.style.TextAppearance_Material3_TitleSmall));
            card.addView(head);
            status = Ui.text(MainActivity.this, "", R.style.TextAppearance_Material3_BodyMedium);
            card.addView(status);
            action = Ui.button(card, "", onClick);
        }

        void set(boolean done, int statusText, int buttonText) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(done ? GREEN : RED);
            dot.setBackground(circle);
            status.setText(statusText);
            status.setTextColor(done ? GREEN : RED);
            action.setText(buttonText);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApertureApp.of(this).settings();
        LinearLayout page = Ui.page(this);
        TextView name = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.app_name), R.style.TextAppearance_Material3_DisplaySmall);
        name.setTypeface(Fonts.medium());
        page.addView(name);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_subtitle), R.style.TextAppearance_Material3_BodyLarge));

        buildBanner(page);
        aapm = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_aapm), R.style.TextAppearance_Material3_BodyMedium);
        aapm.setTextColor(RED);
        page.addView(aapm);
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_setup));
        overlayStep = new Step(page, dev.ayushvarma.aperture.R.string.main_a11y_title,
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        overlayStep.card.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_disclosure),
                R.style.TextAppearance_Material3_BodySmall));
        listenerStep = new Step(page, dev.ayushvarma.aperture.R.string.main_listener_title, v -> openListenerSettings());
        appsStep = new Step(page, dev.ayushvarma.aperture.R.string.main_apps_title,
                v -> startActivity(new Intent(this, AppsActivity.class)));

        buildAppearance(page);
        buildBehavior(page);

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_try));
        LinearLayout demo = Ui.card(page);
        demo.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_try_hint), R.style.TextAppearance_Material3_BodySmall));
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_notification), v -> demoMessage());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_timer), v -> demoTimer());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_charging), v -> demoCharging());
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.main_device_report),
                v -> startActivity(new Intent(this, DeviceReportActivity.class)));
    }

    private void buildBanner(LinearLayout page) {
        banner = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 16);
        banner.setLayoutParams(lp);
        banner.setStrokeWidth(Ui.dp(this, 2));
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        inner.setPadding(pad, pad, pad, pad);
        bannerTitle = Ui.text(this, "", R.style.TextAppearance_Material3_TitleMedium);
        bannerText = Ui.text(this, "", R.style.TextAppearance_Material3_BodyMedium);
        inner.addView(bannerTitle);
        inner.addView(bannerText);
        banner.addView(inner);
        page.addView(banner);
    }

    private void buildAppearance(LinearLayout page) {
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_appearance));
        LinearLayout look = Ui.card(page);
        label(look, dev.ayushvarma.aperture.R.string.main_idle);
        Ui.toggles(look, settings.idleStyle().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_idle_ring,
                dev.ayushvarma.aperture.R.string.main_idle_invisible, dev.ayushvarma.aperture.R.string.main_idle_hidden),
                i -> settings.setEnum("idle", IslandSettings.IdleStyle.values()[i]));
        label(look, dev.ayushvarma.aperture.R.string.main_body);
        Ui.toggles(look, settings.body().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_body_black,
                dev.ayushvarma.aperture.R.string.main_body_tonal),
                i -> settings.setEnum("body", IslandSettings.Body.values()[i]));
        label(look, dev.ayushvarma.aperture.R.string.main_motion);
        Ui.toggles(look, settings.motion().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_motion_expressive,
                dev.ayushvarma.aperture.R.string.main_motion_standard, dev.ayushvarma.aperture.R.string.main_motion_fluid),
                i -> settings.setEnum("motion", IslandSettings.Motion.values()[i]));
        Ui.button(look, getString(dev.ayushvarma.aperture.R.string.main_calibrate),
                v -> startActivity(new Intent(this, CalibrationActivity.class)));
    }

    private void buildBehavior(LinearLayout page) {
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_behavior));
        LinearLayout b = Ui.card(page);
        label(b, dev.ayushvarma.aperture.R.string.main_tap);
        Ui.toggles(b, settings.tapAction().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_tap_expand,
                dev.ayushvarma.aperture.R.string.main_tap_open),
                i -> settings.setEnum("tap_action", IslandSettings.TapAction.values()[i]));
        slider(b, dev.ayushvarma.aperture.R.string.main_glance, "glance_seconds", settings.glanceSeconds(), 0, 30);
        b.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_glance_hint), R.style.TextAppearance_Material3_BodySmall));
        label(b, dev.ayushvarma.aperture.R.string.main_lock);
        Ui.toggles(b, settings.lockScreen().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_lock_brief,
                dev.ayushvarma.aperture.R.string.main_lock_calls),
                i -> settings.setEnum("lock_screen", IslandSettings.LockScreen.values()[i]));
        toggle(b, dev.ayushvarma.aperture.R.string.main_show_media, "media", settings.showMedia());
        toggle(b, dev.ayushvarma.aperture.R.string.main_show_notifications, "notifications", settings.showNotifications());
        b.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_notifications_hint), R.style.TextAppearance_Material3_BodySmall));
        toggle(b, dev.ayushvarma.aperture.R.string.main_show_system, "system_events", settings.showSystemEvents());
        toggle(b, dev.ayushvarma.aperture.R.string.main_haptics, "haptics", settings.haptics());
        slider(b, dev.ayushvarma.aperture.R.string.main_notification_time, "notif_seconds", settings.notificationSeconds(), 1, 12);
        label(b, dev.ayushvarma.aperture.R.string.main_double_tap);
        Ui.toggles(b, settings.doubleTap().ordinal(), labels(dev.ayushvarma.aperture.R.string.main_double_torch,
                dev.ayushvarma.aperture.R.string.main_double_resume, dev.ayushvarma.aperture.R.string.main_double_nothing),
                i -> settings.setEnum("double_tap", IslandSettings.DoubleTap.values()[i]));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean overlay = isOverlayEnabled();
        boolean listener = getSystemService(NotificationManager.class)
                .isNotificationListenerAccessGranted(new ComponentName(this, NotificationSource.class));
        boolean apps = settings.appsChosen();
        overlayStep.set(overlay, overlay ? dev.ayushvarma.aperture.R.string.main_a11y_on : dev.ayushvarma.aperture.R.string.main_a11y_off,
                overlay ? dev.ayushvarma.aperture.R.string.main_manage : dev.ayushvarma.aperture.R.string.main_grant);
        listenerStep.set(listener, listener ? dev.ayushvarma.aperture.R.string.main_listener_on : dev.ayushvarma.aperture.R.string.main_listener_off,
                listener ? dev.ayushvarma.aperture.R.string.main_manage : dev.ayushvarma.aperture.R.string.main_grant);
        appsStep.set(apps, apps ? dev.ayushvarma.aperture.R.string.main_apps_done : dev.ayushvarma.aperture.R.string.main_apps_todo,
                apps ? dev.ayushvarma.aperture.R.string.main_apps_change : dev.ayushvarma.aperture.R.string.main_apps_choose);
        appsStep.status.setText(apps ? getString(dev.ayushvarma.aperture.R.string.main_apps_done, installedAllowed())
                : getString(dev.ayushvarma.aperture.R.string.main_apps_todo));
        int done = (overlay ? 1 : 0) + (listener ? 1 : 0) + (apps ? 1 : 0);
        boolean complete = done == 3;
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int accent = complete ? GREEN : RED;
        banner.setStrokeColor(accent);
        banner.setCardBackgroundColor(complete ? (night ? 0xFF0F2E18 : 0xFFE6F4EA) : (night ? 0xFF3B1210 : 0xFFFCE8E6));
        bannerTitle.setTextColor(accent);
        bannerTitle.setText(complete ? getString(dev.ayushvarma.aperture.R.string.banner_done)
                : getString(dev.ayushvarma.aperture.R.string.banner_todo, done));
        bannerText.setText(complete ? dev.ayushvarma.aperture.R.string.banner_done_text : dev.ayushvarma.aperture.R.string.banner_todo_text);
        aapm.setVisibility(isAdvancedProtectionOn() ? View.VISIBLE : View.GONE);
        // Once both permissions are granted, go straight to the last step.
        if (overlay && listener && !apps && !pickerOffered) {
            pickerOffered = true;
            startActivity(new Intent(this, AppsActivity.class));
        }
    }

    /** Allowed apps that are actually installed (the suggested list includes apps you may not have). */
    private int installedAllowed() {
        int n = 0;
        for (String pkg : settings.allowedApps()) {
            if (getPackageManager().getLaunchIntentForPackage(pkg) != null) n++;
        }
        return n;
    }

    private boolean isOverlayEnabled() {
        ComponentName me = new ComponentName(this, IslandAccessibilityService.class);
        for (AccessibilityServiceInfo info : getSystemService(AccessibilityManager.class)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (me.flattenToString().equals(info.getId()) || me.flattenToShortString().equals(info.getId())) return true;
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

    private void label(LinearLayout parent, int text) {
        TextView t = Ui.text(this, getString(text), R.style.TextAppearance_Material3_LabelLarge);
        t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        parent.addView(t);
    }

    private void toggle(LinearLayout parent, int label, String key, boolean value) {
        MaterialSwitch s = new MaterialSwitch(this);
        s.setText(label);
        s.setTypeface(Fonts.regular());
        s.setChecked(value);
        s.setOnCheckedChangeListener((b, checked) -> settings.setBoolean(key, checked));
        parent.addView(s);
    }

    private void slider(LinearLayout parent, int label, String key, int value, int from, int to) {
        TextView t = Ui.text(this, sliderText(label, key, value), R.style.TextAppearance_Material3_LabelLarge);
        t.setPadding(0, Ui.dp(this, 10), 0, 0);
        parent.addView(t);
        Slider s = new Slider(this);
        s.setValueFrom(from);
        s.setValueTo(to);
        s.setStepSize(1);
        s.setValue(Math.max(from, Math.min(to, value)));
        s.addOnChangeListener((sl, v, fromUser) -> {
            settings.setInt(key, Math.round(v));
            t.setText(sliderText(label, key, Math.round(v)));
        });
        parent.addView(s);
    }

    private String sliderText(int label, String key, int value) {
        if (key.equals("glance_seconds") && value == 0) return getString(dev.ayushvarma.aperture.R.string.main_glance_never);
        return getString(label, value);
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
