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
    private SettingRows rows;
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
        rows = new SettingRows(this, settings);
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
        buildHelp(page);
    }

    /** Help and feedback: answers to the questions people actually hit, and a direct line to the developer. */
    private void buildHelp(LinearLayout page) {
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.help_title));
        LinearLayout faq = Ui.card(page);
        int[][] items = {
                {dev.ayushvarma.aperture.R.string.faq_stopped_q, dev.ayushvarma.aperture.R.string.faq_stopped_a},
                {dev.ayushvarma.aperture.R.string.faq_double_q, dev.ayushvarma.aperture.R.string.faq_double_a},
                {dev.ayushvarma.aperture.R.string.faq_nothing_q, dev.ayushvarma.aperture.R.string.faq_nothing_a},
                {dev.ayushvarma.aperture.R.string.faq_protection_q, dev.ayushvarma.aperture.R.string.faq_protection_a},
                {dev.ayushvarma.aperture.R.string.faq_privacy_q, dev.ayushvarma.aperture.R.string.faq_privacy_a},
        };
        for (int[] qa : items) {
            TextView q = Ui.text(this, getString(qa[0]), R.style.TextAppearance_Material3_TitleSmall);
            q.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 2));
            faq.addView(q);
            faq.addView(Ui.text(this, getString(qa[1]), R.style.TextAppearance_Material3_BodyMedium));
        }
        LinearLayout actions = Ui.card(page);
        Ui.button(actions, getString(dev.ayushvarma.aperture.R.string.help_feedback), v -> sendFeedback());
    }

    /**
     * Opens the user's email app addressed to the developer, with the device report and settings in the body so
     * a bug report is actionable. Nothing is sent unless the user presses send (the app has no internet).
     */
    private void sendFeedback() {
        String body = getString(dev.ayushvarma.aperture.R.string.feedback_body_intro) + "\n\n\n---\n"
                + dev.ayushvarma.aperture.device.DeviceReport.build(this, settings) + "\nSettings\n" + settings.export();
        Intent mail = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_EMAIL, new String[]{getString(dev.ayushvarma.aperture.R.string.feedback_email)})
                .putExtra(Intent.EXTRA_SUBJECT, getString(dev.ayushvarma.aperture.R.string.feedback_subject))
                .putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(mail);
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, body), null));
        }
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
        rows.label(look, dev.ayushvarma.aperture.R.string.main_body);
        Ui.toggles(look, settings.body().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_body_black,
                dev.ayushvarma.aperture.R.string.main_body_glass, dev.ayushvarma.aperture.R.string.main_body_clear,
                dev.ayushvarma.aperture.R.string.main_body_artwork, dev.ayushvarma.aperture.R.string.main_body_tonal),
                i -> settings.setEnum("body", IslandSettings.Body.values()[i]));
        rows.hint(look, dev.ayushvarma.aperture.R.string.main_body_hint);
        rows.toggle(look, dev.ayushvarma.aperture.R.string.main_artwork_accent, "artwork_accent", settings.artworkAccent());
    }

    /** The few choices most people want; everything else lives on the Advanced page. */
    private void buildBehavior(LinearLayout page) {
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_behavior));
        LinearLayout b = Ui.card(page);
        rows.toggle(b, dev.ayushvarma.aperture.R.string.main_show_media, "media", settings.showMedia());
        rows.toggle(b, dev.ayushvarma.aperture.R.string.main_show_notifications, "notifications", settings.showNotifications());
        rows.hint(b, dev.ayushvarma.aperture.R.string.main_notifications_hint);
        rows.toggle(b, dev.ayushvarma.aperture.R.string.main_hide_foreground, "hide_foreground", settings.hideInForeground());
        rows.hint(b, dev.ayushvarma.aperture.R.string.main_hide_foreground_hint);
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.advanced_open),
                v -> startActivity(new Intent(this, AdvancedActivity.class)));
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
