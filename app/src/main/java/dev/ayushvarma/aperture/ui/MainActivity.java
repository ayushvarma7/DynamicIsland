package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.view.Gravity;
import android.view.View;
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
    private LinearLayout setupSection;
    private com.google.android.material.materialswitch.MaterialSwitch islandSwitch;
    private MaterialButton reviewSetup;
    private boolean setupExpanded;
    private MaterialButton keepRunning;
    private LivePreview preview;

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
        if (!settings.onboarded()) {
            // First run: the introduction owns the screen until it is finished or skipped.
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        LinearLayout page = Ui.page(this);
        TextView name = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.app_name), R.style.TextAppearance_Material3_DisplaySmall);
        name.setTypeface(Fonts.medium());
        page.addView(name);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_subtitle), R.style.TextAppearance_Material3_BodyLarge));

        buildBanner(page);
        aapm = Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_aapm), R.style.TextAppearance_Material3_BodyMedium);
        aapm.setTextColor(RED);
        page.addView(aapm);
        // The setup steps live in their own section so they can fold away once everything is granted.
        setupSection = new LinearLayout(this);
        setupSection.setOrientation(LinearLayout.VERTICAL);
        page.addView(setupSection);
        LinearLayout setup = setupSection;
        Ui.title(setup, getString(dev.ayushvarma.aperture.R.string.main_setup));
        overlayStep = new Step(setup, dev.ayushvarma.aperture.R.string.main_a11y_title,
                v -> Permissions.confirmOverlay(this));
        overlayStep.card.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_disclosure),
                R.style.TextAppearance_Material3_BodySmall));
        listenerStep = new Step(setup, dev.ayushvarma.aperture.R.string.main_listener_title, v -> Permissions.openListenerSettings(this));
        appsStep = new Step(setup, dev.ayushvarma.aperture.R.string.main_apps_title,
                v -> startActivity(new Intent(this, AppsActivity.class)));

        buildAppearance(page);
        buildBehavior(page);

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_try));
        LinearLayout demo = Ui.card(page);
        demo.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.main_try_hint), R.style.TextAppearance_Material3_BodySmall));
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_notification), v -> demoMessage());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_timer), v -> demoTimer());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_call), v -> demoCall());
        Ui.button(demo, getString(dev.ayushvarma.aperture.R.string.main_demo_charging), v -> demoCharging());
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.help_open),
                v -> startActivity(new Intent(this, HelpActivity.class)));
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
        // Once set up: a plain on/off switch (pause without revoking anything) and a way back to the steps.
        islandSwitch = new com.google.android.material.materialswitch.MaterialSwitch(this);
        islandSwitch.setText(dev.ayushvarma.aperture.R.string.main_island_on);
        islandSwitch.setTypeface(Fonts.medium());
        islandSwitch.setChecked(settings.enabled());
        islandSwitch.setOnCheckedChangeListener((b, on) -> settings.setBoolean("enabled", on));
        inner.addView(islandSwitch);
        reviewSetup = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reviewSetup.setTypeface(Fonts.medium());
        reviewSetup.setOnClickListener(v -> {
            setupExpanded = !setupExpanded;
            refreshSetup();
        });
        inner.addView(reviewSetup);
        keepRunning = new MaterialButton(this, null, android.R.attr.borderlessButtonStyle);
        keepRunning.setTypeface(Fonts.medium());
        keepRunning.setAllCaps(false);
        keepRunning.setText(dev.ayushvarma.aperture.R.string.main_keep_running);
        keepRunning.setOnClickListener(v -> Permissions.requestKeepRunning(this));
        inner.addView(keepRunning);
        banner.addView(inner);
        page.addView(banner);
    }

    private void buildAppearance(LinearLayout page) {
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.main_appearance));
        LinearLayout look = Ui.card(page);
        preview = new LivePreview(this, settings);
        look.addView(preview);
        rows.label(look, dev.ayushvarma.aperture.R.string.main_body);
        Ui.chips(look, settings.body().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.main_body_black,
                dev.ayushvarma.aperture.R.string.main_body_glass, dev.ayushvarma.aperture.R.string.main_body_clear,
                dev.ayushvarma.aperture.R.string.main_body_artwork, dev.ayushvarma.aperture.R.string.main_body_tonal),
                i -> settings.setEnum("body", IslandSettings.Body.values()[i]));
        rows.hint(look, dev.ayushvarma.aperture.R.string.main_body_hint);
        rows.toggle(look, dev.ayushvarma.aperture.R.string.main_artwork_accent, "artwork_accent", settings.artworkAccent());
        rows.label(look, dev.ayushvarma.aperture.R.string.main_app_theme);
        Ui.chips(look, settings.appTheme().ordinal(), rows.labels(dev.ayushvarma.aperture.R.string.theme_system,
                dev.ayushvarma.aperture.R.string.theme_light, dev.ayushvarma.aperture.R.string.theme_dark),
                i -> settings.setAppTheme(this, IslandSettings.AppTheme.values()[i]));
        Ui.button(look, getString(dev.ayushvarma.aperture.R.string.look_try), v -> tryOnIsland());
    }

    /** Plays the sample song on the real island and opens the expanded player in the chosen style. */
    private void tryOnIsland() {
        hub().upsert(DemoContent.song(this));
        hub().requestExpand(DemoContent.SONG_ID);
        getWindow().getDecorView().postDelayed(() -> hub().remove(DemoContent.SONG_ID), 10_000);
    }

    @Override
    protected void onPause() {
        preview.setRunning(false);
        super.onPause();
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
        preview.setRunning(true);
        refreshSetup();
    }

    /** Recomputes every setup step and the banner from the live permission state. */
    private void refreshSetup() {
        boolean overlay = Permissions.overlay(this);
        boolean listener = Permissions.listener(this);
        boolean apps = settings.appsChosen();
        overlayStep.set(overlay, overlay ? dev.ayushvarma.aperture.R.string.main_a11y_on : dev.ayushvarma.aperture.R.string.main_a11y_off,
                overlay ? dev.ayushvarma.aperture.R.string.main_manage : dev.ayushvarma.aperture.R.string.main_grant);
        listenerStep.set(listener, listener ? dev.ayushvarma.aperture.R.string.main_listener_on : dev.ayushvarma.aperture.R.string.main_listener_off,
                listener ? dev.ayushvarma.aperture.R.string.main_manage : dev.ayushvarma.aperture.R.string.main_grant);
        appsStep.set(apps, dev.ayushvarma.aperture.R.string.main_apps_todo,
                apps ? dev.ayushvarma.aperture.R.string.main_apps_change : dev.ayushvarma.aperture.R.string.main_apps_choose);
        if (apps) {
            int n = installedAllowed();
            appsStep.status.setText(getResources().getQuantityString(dev.ayushvarma.aperture.R.plurals.main_apps_done, n, n));
        }
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
        // Setup folds away when complete; it comes back on its own if anything goes missing.
        setupSection.setVisibility(!complete || setupExpanded ? View.VISIBLE : View.GONE);
        islandSwitch.setVisibility(complete ? View.VISIBLE : View.GONE);
        reviewSetup.setVisibility(complete ? View.VISIBLE : View.GONE);
        // Optional: without it a battery saver could stop Aperture after a restart.
        keepRunning.setVisibility(complete && !Permissions.keepsRunning(this) ? View.VISIBLE : View.GONE);
        reviewSetup.setText(setupExpanded ? dev.ayushvarma.aperture.R.string.main_hide_setup
                : dev.ayushvarma.aperture.R.string.main_review_setup);
        if (complete) {
            bannerText.setText(settings.enabled() ? dev.ayushvarma.aperture.R.string.banner_done_text
                    : dev.ayushvarma.aperture.R.string.banner_paused_text);
        }
        // Once both permissions are granted, go straight to the last step.
        if (overlay && listener && !apps && !pickerOffered && settings.onboarded()) {
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

    /** A pretend incoming call, so people can see the call card; its buttons do nothing. */
    private void demoCall() {
        long now = System.currentTimeMillis();
        hub().upsert(IslandActivity.builder("demo:call", getPackageName(), ActivityKind.INCOMING_CALL)
                .title(getString(dev.ayushvarma.aperture.R.string.demo_caller))
                .payload(demoPayload()).createdAt(now).updatedAt(now).build());
        getWindow().getDecorView().postDelayed(() -> hub().remove("demo:call"), 8_000);
    }

    private void demoCharging() {
        long now = System.currentTimeMillis();
        hub().postTransient(IslandActivity.builder("sys:charging", getPackageName(), ActivityKind.SYSTEM)
                .title(getString(dev.ayushvarma.aperture.R.string.island_charging)).shortText("82%").progress(0.82f)
                .createdAt(now).updatedAt(now).build(), 3_000);
    }
}
