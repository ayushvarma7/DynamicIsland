package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.device.DeviceReport;
import dev.ayushvarma.aperture.settings.IslandSettings;

/**
 * Help, feedback and leaving. Common problems are one line each and open on tap; feedback goes straight to the
 * developer; pausing and uninstalling are one tap each, with no screens trying to talk the user out of it.
 */
public final class HelpActivity extends Activity {
    private static final int[][] FAQ = {
            {dev.ayushvarma.aperture.R.string.faq_stopped_q, dev.ayushvarma.aperture.R.string.faq_stopped_a},
            {dev.ayushvarma.aperture.R.string.faq_nothing_q, dev.ayushvarma.aperture.R.string.faq_nothing_a},
            {dev.ayushvarma.aperture.R.string.faq_double_q, dev.ayushvarma.aperture.R.string.faq_double_a},
            {dev.ayushvarma.aperture.R.string.faq_protection_q, dev.ayushvarma.aperture.R.string.faq_protection_a},
            {dev.ayushvarma.aperture.R.string.faq_privacy_q, dev.ayushvarma.aperture.R.string.faq_privacy_a},
    };

    private IslandSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApertureApp.of(this).settings();
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.help_title), R.style.TextAppearance_Material3_HeadlineMedium));

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.help_common));
        LinearLayout faq = Ui.card(page);
        for (int[] qa : FAQ) {
            TextView q = Ui.text(this, "▸  " + getString(qa[0]), R.style.TextAppearance_Material3_TitleSmall);
            int pad = Ui.dp(this, 12);
            q.setPadding(0, pad, 0, pad);
            TextView a = Ui.text(this, getString(qa[1]), R.style.TextAppearance_Material3_BodyMedium);
            a.setVisibility(View.GONE);
            a.setPadding(Ui.dp(this, 20), 0, 0, pad);
            q.setOnClickListener(v -> {
                boolean open = a.getVisibility() != View.VISIBLE;
                a.setVisibility(open ? View.VISIBLE : View.GONE);
                q.setText((open ? "▾  " : "▸  ") + getString(qa[0]));
            });
            faq.addView(q);
            faq.addView(a);
        }

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.help_feedback_title));
        LinearLayout fb = Ui.card(page);
        fb.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.help_feedback_how), R.style.TextAppearance_Material3_BodyMedium));
        Ui.button(fb, getString(dev.ayushvarma.aperture.R.string.help_feedback), v -> sendFeedback());

        Ui.button(fb, getString(dev.ayushvarma.aperture.R.string.help_replay), v -> {
            settings.setOnboarded(false);
            startActivity(new Intent(this, OnboardingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.help_leave));
        LinearLayout leave = Ui.card(page);
        leave.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.help_leave_text), R.style.TextAppearance_Material3_BodyMedium));
        Ui.button(leave, getString(settings.enabled() ? dev.ayushvarma.aperture.R.string.help_pause
                : dev.ayushvarma.aperture.R.string.help_resume), v -> {
            settings.setBoolean("enabled", !settings.enabled());
            recreate();
        });
        Ui.button(leave, getString(dev.ayushvarma.aperture.R.string.help_revoke),
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Ui.button(leave, getString(dev.ayushvarma.aperture.R.string.help_uninstall), v -> uninstall());
    }

    /**
     * Opens the email app addressed to the developer with the device report and settings in the body. Nothing
     * is sent unless the user presses send (the app itself has no internet).
     */
    private void sendFeedback() {
        String body = getString(dev.ayushvarma.aperture.R.string.feedback_body_intro) + "\n\n\n---\n"
                + DeviceReport.build(this, settings) + "\nSettings\n" + settings.export();
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
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

    /** Android's own uninstall confirmation; falls back to App info, which has Uninstall. */
    private void uninstall() {
        Uri pkg = Uri.parse("package:" + getPackageName());
        try {
            startActivity(new Intent(Intent.ACTION_DELETE, pkg));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg));
        }
    }
}
