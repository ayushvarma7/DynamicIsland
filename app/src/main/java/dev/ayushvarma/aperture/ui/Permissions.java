package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dev.ayushvarma.aperture.R;
import dev.ayushvarma.aperture.overlay.IslandAccessibilityService;
import dev.ayushvarma.aperture.source.NotificationSource;

/** Live permission state and the system screens that grant it; shared by onboarding and the home screen. */
final class Permissions {
    private Permissions() {}

    /**
     * Reads the enabled-services setting itself: AccessibilityManager's list can briefly omit a service while
     * Android rebinds it (for example right after an app update), which would show a false red step.
     */
    static boolean overlay(Context c) {
        ComponentName me = new ComponentName(c, IslandAccessibilityService.class);
        String enabled = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        for (String id : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(id))) return true;
        }
        return false;
    }

    static boolean listener(Context c) {
        return c.getSystemService(NotificationManager.class)
                .isNotificationListenerAccessGranted(new ComponentName(c, NotificationSource.class));
    }

    /** Exempt from battery optimisation, so no battery saver stops the services after a restart. */
    static boolean keepsRunning(Context c) {
        return c.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(c.getPackageName());
    }

    static void openOverlaySettings(Activity a) {
        a.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    /**
     * Play's prominent-disclosure rule for apps that are not accessibility tools: say what the service sees and
     * why, and get an explicit Agree before sending the user to Accessibility settings.
     */
    static void confirmOverlay(Activity a) {
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.a11y_consent_title)
                .setMessage(R.string.a11y_consent_body)
                .setPositiveButton(R.string.a11y_consent_agree, (d, w) -> openOverlaySettings(a))
                .setNegativeButton(R.string.ob_not_now, null)
                .show();
    }

    static void openListenerSettings(Activity a) {
        Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(a, NotificationSource.class).flattenToString());
        try {
            a.startActivity(detail);
        } catch (android.content.ActivityNotFoundException e) {
            a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    /**
     * Aperture's App info page, where "App battery usage" can be set to Unrestricted. Play only allows the direct
     * exemption dialog (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) for a short list of app types, so the user flips it.
     */
    static void requestKeepRunning(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + a.getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }
}
