package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.function.BooleanSupplier;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.theme.Fonts;

/**
 * First-run introduction: what Aperture is, what it shows, how it treats your privacy, then one page per
 * permission (each explains why before sending you to Settings, and moves on by itself once granted), app
 * choice, staying on after a restart, and done. Back steps back; the introduction can be skipped.
 */
public final class OnboardingActivity extends Activity {
    private static final int[][] STORY_ROWS = {
            {dev.ayushvarma.aperture.R.drawable.ic_music, dev.ayushvarma.aperture.R.string.ob_story_music},
            {dev.ayushvarma.aperture.R.drawable.ic_timer, dev.ayushvarma.aperture.R.string.ob_story_timer},
            {dev.ayushvarma.aperture.R.drawable.ic_delivery, dev.ayushvarma.aperture.R.string.ob_story_delivery},
            {dev.ayushvarma.aperture.R.drawable.ic_call, dev.ayushvarma.aperture.R.string.ob_story_call},
            {dev.ayushvarma.aperture.R.drawable.ic_eye_off, dev.ayushvarma.aperture.R.string.ob_story_quiet},
    };
    private static final int[][] PRIVACY_ROWS = {
            {dev.ayushvarma.aperture.R.drawable.ic_wifi_off, dev.ayushvarma.aperture.R.string.ob_privacy_offline},
            {dev.ayushvarma.aperture.R.drawable.ic_eye_off, dev.ayushvarma.aperture.R.string.ob_privacy_screen},
            {dev.ayushvarma.aperture.R.drawable.ic_shield, dev.ayushvarma.aperture.R.string.ob_privacy_ads},
            {dev.ayushvarma.aperture.R.drawable.ic_restart, dev.ayushvarma.aperture.R.string.ob_privacy_leave},
    };

    /** One page of the introduction. {@code done} is null for pages without a requirement. */
    private static final class Page {
        final int icon;
        final int title;
        final int body;
        final int[][] rows;
        final int action;
        final Runnable onAction;
        final BooleanSupplier done;
        final boolean optional;

        Page(int icon, int title, int body, int[][] rows, int action, Runnable onAction, BooleanSupplier done,
             boolean optional) {
            this.icon = icon;
            this.title = title;
            this.body = body;
            this.rows = rows;
            this.action = action;
            this.onAction = onAction;
            this.done = done;
            this.optional = optional;
        }
    }

    private IslandSettings settings;
    private Page[] pages;
    private int index;
    private FrameLayout stage;
    private LinearLayout dots;
    private MaterialButton primary;
    private MaterialButton secondary;
    private MaterialButton skip;
    private TextView status;
    private final Runnable autoAdvance = () -> go(index + 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApertureApp.of(this).settings();
        pages = new Page[]{
                new Page(0, dev.ayushvarma.aperture.R.string.ob_welcome_title, dev.ayushvarma.aperture.R.string.ob_welcome_body,
                        null, dev.ayushvarma.aperture.R.string.ob_get_started, null, null, false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_music, dev.ayushvarma.aperture.R.string.ob_story_title,
                        dev.ayushvarma.aperture.R.string.ob_story_body, STORY_ROWS, dev.ayushvarma.aperture.R.string.ob_next, null, null, false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_shield, dev.ayushvarma.aperture.R.string.ob_privacy_title,
                        dev.ayushvarma.aperture.R.string.ob_privacy_body, PRIVACY_ROWS, dev.ayushvarma.aperture.R.string.ob_next, null, null, false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_layers, dev.ayushvarma.aperture.R.string.ob_overlay_title,
                        dev.ayushvarma.aperture.R.string.ob_overlay_body, null, dev.ayushvarma.aperture.R.string.ob_overlay_action,
                        () -> Permissions.openOverlaySettings(this), () -> Permissions.overlay(this), false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_bell, dev.ayushvarma.aperture.R.string.ob_listener_title,
                        dev.ayushvarma.aperture.R.string.ob_listener_body, null, dev.ayushvarma.aperture.R.string.ob_listener_action,
                        () -> Permissions.openListenerSettings(this), () -> Permissions.listener(this), false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_apps, dev.ayushvarma.aperture.R.string.ob_apps_title,
                        dev.ayushvarma.aperture.R.string.ob_apps_body, null, dev.ayushvarma.aperture.R.string.ob_apps_action,
                        () -> startActivity(new Intent(this, AppsActivity.class)), settings::appsChosen, false),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_restart, dev.ayushvarma.aperture.R.string.ob_restart_title,
                        dev.ayushvarma.aperture.R.string.ob_restart_body, null, dev.ayushvarma.aperture.R.string.ob_restart_action,
                        () -> Permissions.requestKeepRunning(this), () -> Permissions.keepsRunning(this), true),
                new Page(dev.ayushvarma.aperture.R.drawable.ic_check, dev.ayushvarma.aperture.R.string.ob_done_title,
                        dev.ayushvarma.aperture.R.string.ob_done_body, null, dev.ayushvarma.aperture.R.string.ob_open_app, null, null, false),
        };
        buildShell();
        show(resumePage(), 0);
    }

    /**
     * Where to (re)start: the welcome page on a first visit, otherwise the first step still to do, so leaving
     * for Settings and coming back never starts the introduction over.
     */
    private int resumePage() {
        boolean anyDone = false;
        for (Page p : pages) if (p.done != null && p.done.getAsBoolean()) anyDone = true;
        if (!anyDone) return 0;
        for (int i = 0; i < pages.length; i++) {
            Page p = pages[i];
            if (p.done != null && !p.optional && !p.done.getAsBoolean()) return i;
        }
        return pages.length - 1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from Settings: if this page's permission is now granted, confirm it and move on.
        Page p = pages[index];
        refreshStatus(p);
        if (p.done != null && p.done.getAsBoolean()) {
            stage.removeCallbacks(autoAdvance);
            stage.postDelayed(autoAdvance, 700);
        }
    }

    @Override
    public void onBackPressed() {
        if (index > 0) go(index - 1);
        else super.onBackPressed();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int side = Ui.dp(this, 24);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(side + bars.left, bars.top + Ui.dp(this, 8), side + bars.right, bars.bottom + Ui.dp(this, 16));
            return WindowInsets.CONSUMED;
        });

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.END);
        skip = textButton(dev.ayushvarma.aperture.R.string.ob_skip, v -> finishOnboarding());
        top.addView(skip);
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));

        stage = new FrameLayout(this);
        root.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < pages.length; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 8), Ui.dp(this, 8));
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            dots.addView(d, lp);
        }
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 24));
        dlp.bottomMargin = Ui.dp(this, 12);
        root.addView(dots, dlp);

        status = Ui.text(this, "", R.style.TextAppearance_Material3_LabelLarge);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        primary = new MaterialButton(this);
        primary.setTypeface(Fonts.medium());
        primary.setTextSize(16);
        primary.setOnClickListener(v -> onPrimary());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56));
        plp.topMargin = Ui.dp(this, 8);
        root.addView(primary, plp);
        secondary = textButton(dev.ayushvarma.aperture.R.string.ob_back, v -> onSecondary());
        root.addView(secondary, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        setContentView(root);
    }

    private MaterialButton textButton(int label, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(this, null, android.R.attr.borderlessButtonStyle);
        b.setText(label);
        b.setAllCaps(false);
        b.setTypeface(Fonts.medium());
        b.setOnClickListener(l);
        return b;
    }

    private void onPrimary() {
        Page p = pages[index];
        boolean granted = p.done != null && p.done.getAsBoolean();
        if (p.onAction != null && !granted) {
            p.onAction.run();
        } else if (index == pages.length - 1) {
            finishOnboarding();
        } else {
            go(index + 1);
        }
    }

    /** Back on most pages; "Not now" on the optional restart page. */
    private void onSecondary() {
        if (pages[index].optional) go(index + 1);
        else if (index > 0) go(index - 1);
    }

    private void go(int target) {
        if (target < 0 || target >= pages.length || target == index) return;
        stage.removeCallbacks(autoAdvance);
        show(target, target > index ? 1 : -1);
    }

    /** Slides the old page out and the new one in (40 dp, 250 ms, decelerating). */
    private void show(int target, int direction) {
        index = target;
        View old = stage.getChildCount() > 0 ? stage.getChildAt(0) : null;
        View page = buildPage(pages[target]);
        float shift = Ui.dp(this, 40) * direction;
        if (old != null) {
            old.animate().translationX(-shift).alpha(0f).setDuration(150)
                    .withEndAction(() -> stage.removeView(old)).start();
        }
        page.setAlpha(direction == 0 ? 1f : 0f);
        page.setTranslationX(shift);
        stage.addView(page);
        page.animate().translationX(0).alpha(1f).setStartDelay(direction == 0 ? 0 : 90).setDuration(250)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        for (int i = 0; i < dots.getChildCount(); i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(i == target ? color(android.R.attr.colorPrimary) : color(R.attr.colorOutlineVariant));
            dots.getChildAt(i).setBackground(d);
        }
        Page p = pages[target];
        skip.setVisibility(target < 3 ? View.VISIBLE : View.INVISIBLE);
        secondary.setVisibility(target == 0 || target == pages.length - 1 ? View.INVISIBLE : View.VISIBLE);
        secondary.setText(p.optional ? dev.ayushvarma.aperture.R.string.ob_not_now : dev.ayushvarma.aperture.R.string.ob_back);
        refreshStatus(p);
    }

    private void refreshStatus(Page p) {
        boolean granted = p.done != null && p.done.getAsBoolean();
        primary.setText(granted ? getString(dev.ayushvarma.aperture.R.string.ob_continue) : getString(p.action));
        if (p.done == null) {
            status.setVisibility(View.GONE);
            return;
        }
        status.setVisibility(View.VISIBLE);
        status.setText(granted ? dev.ayushvarma.aperture.R.string.ob_granted : dev.ayushvarma.aperture.R.string.ob_not_granted);
        status.setTextColor(granted ? 0xFF1E8E3E : color(R.attr.colorOnSurfaceVariant));
    }

    private View buildPage(Page p) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        if (p.icon == 0) {
            LivePreview preview = new LivePreview(this, settings);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(this, 8);
            col.addView(preview, lp);
        } else {
            col.addView(badge(p.icon));
        }
        TextView title = Ui.text(this, getString(p.title), R.style.TextAppearance_Material3_HeadlineMedium);
        title.setTypeface(Fonts.medium());
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 10));
        col.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView body = Ui.text(this, getString(p.body), R.style.TextAppearance_Material3_BodyLarge);
        body.setGravity(Gravity.CENTER);
        body.setTextColor(color(R.attr.colorOnSurfaceVariant));
        col.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (p.rows != null) {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(0, Ui.dp(this, 24), 0, 0);
            for (int[] row : p.rows) list.addView(row(row[0], row[1]));
            col.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(col);
        return scroll;
    }

    /** A 96 dp tonal circle with the page's icon. */
    private View badge(int icon) {
        FrameLayout f = new FrameLayout(this);
        int size = Ui.dp(this, 96);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color(R.attr.colorPrimaryContainer));
        f.setBackground(bg);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(color(R.attr.colorOnPrimaryContainer));
        int is = Ui.dp(this, 44);
        f.addView(iv, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.topMargin = Ui.dp(this, 32);
        f.setLayoutParams(lp);
        return f;
    }

    private View row(int icon, int text) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        FrameLayout chip = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color(R.attr.colorSecondaryContainer));
        chip.setBackground(bg);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(color(R.attr.colorOnSecondaryContainer));
        int is = Ui.dp(this, 22);
        chip.addView(iv, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
        int cs = Ui.dp(this, 44);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(cs, cs);
        clp.setMarginEnd(Ui.dp(this, 16));
        r.addView(chip, clp);
        TextView t = Ui.text(this, getString(text), R.style.TextAppearance_Material3_BodyLarge);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    private int color(int attr) {
        return MaterialColors.getColor(stage != null ? stage : getWindow().getDecorView(), attr);
    }

    private void finishOnboarding() {
        settings.setOnboarded(true);
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
