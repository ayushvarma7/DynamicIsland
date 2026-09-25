package dev.ayushvarma.aperture.device;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.View;

import java.util.Locale;

import dev.ayushvarma.aperture.core.geometry.DisplayInfo;
import dev.ayushvarma.aperture.core.geometry.GeometryCalculator;
import dev.ayushvarma.aperture.core.geometry.IslandGeometry;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.theme.Fonts;

/** Section 9.6 Device Report: identity, measured geometry and every active adaptation rule with its reason. */
public final class DeviceReport {
    private DeviceReport() {}

    public static String build(Context ctx, IslandSettings settings) {
        StringBuilder sb = new StringBuilder();
        line(sb, "Device", Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")");
        line(sb, "Android", Build.VERSION.RELEASE + ", SDK " + Build.VERSION.SDK_INT
                + (Build.VERSION.SDK_INT >= 36 ? " (full " + Build.VERSION.SDK_INT_FULL + ")" : "")
                + ", build " + Build.ID + ", patch " + Build.VERSION.SECURITY_PATCH);
        boolean pixel = "Google".equalsIgnoreCase(Build.MANUFACTURER)
                && ctx.getPackageManager().hasSystemFeature("com.google.android.feature.PIXEL_EXPERIENCE");
        line(sb, "Profile tier", "shiba".equals(Build.DEVICE) ? "primary (Pixel 8)" : pixel ? "supported (Pixel)" : "generic");

        DisplayInfo d = DisplayReader.read(ctx);
        IslandGeometry g = GeometryCalculator.compute(d, settings.calibration());
        sb.append("\nMeasured display\n");
        line(sb, "Size", d.widthPx + " x " + d.heightPx + " px, density " + d.density);
        line(sb, "Cutout", d.cutout.toString());
        line(sb, "Status bar", d.statusBarPx + " px");
        line(sb, "Top corner radius", d.topCornerRadiusPx > 0 ? d.topCornerRadiusPx + " px" : "not reported (36 dp fallback)");
        line(sb, "Privacy zone starts", d.privacyLeftPx > 0 ? "x = " + d.privacyLeftPx + " px" : "unknown");
        sb.append("\nIsland geometry\n");
        line(sb, "Layout", g.layout.name());
        line(sb, "Center", fmt(g.centerX) + ", " + fmt(g.centerY) + " px; r = " + fmt(g.cutoutRadius) + " px");
        line(sb, "Compact", fmt(g.compactWidth()) + " x " + fmt(g.compactHeight) + " px");
        line(sb, "Expanded", fmt(g.expandedWidth) + " px wide, corner " + fmt(g.expandedCorner) + " px");
        line(sb, "Detents", fmt(g.detentSmall) + " / " + fmt(g.detentMedium) + " / " + fmt(g.detentLarge) + " px");

        sb.append("\nActive rules\n");
        switch (g.layout) {
            case SYMMETRIC:
                rule(sb, "G1", "centered punch hole: symmetric island");
                break;
            case OFFSET:
                rule(sb, "G2", "off-center cutout: content on the screen-center side");
                break;
            default:
                rule(sb, "G3", "no cutout: virtual island in the status bar");
                break;
        }
        if (d.waterfallSidePx > 0) rule(sb, "G4", "curved edges: margin widened by " + d.waterfallSidePx + " px");
        if (!g.visible) rule(sb, "G5", "landscape: island hidden");
        Display display = ctx.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        float maxHz = 0;
        for (Display.Mode m : display.getSupportedModes()) maxHz = Math.max(maxHz, m.getRefreshRate());
        String peak = readSystem(ctx, "peak_refresh_rate");
        boolean smooth = maxHz >= 119 && !"60.0".equals(peak);
        rule(sb, smooth ? "R1" : "R2", smooth ? "up to " + Math.round(maxHz) + " Hz, 8.3 ms frame budget"
                : "16.6 ms frame budget (peak " + peak + ")");
        PowerManager pm = ctx.getSystemService(PowerManager.class);
        if (pm.isPowerSaveMode() || pm.getCurrentThermalStatus() >= PowerManager.THERMAL_STATUS_MODERATE) {
            rule(sb, "T1", "battery saver or thermal " + pm.getCurrentThermalStatus() + ": lighter motion");
        }
        if (ctx.getSystemService(ActivityManager.class).isLowRamDevice()) rule(sb, "M1", "low-RAM device");
        if (!ValueAnimator.areAnimatorsEnabled()) rule(sb, "A1", "animations off: crossfades only");
        if ("1".equals(readSecure(ctx, "accessibility_display_inversion_enabled"))) {
            rule(sb, "X2", "color inversion on");
        }
        if (ctx.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            rule(sb, "L1", "right-to-left layout");
        }
        rule(sb, "L2", DateFormat.is24HourFormat(ctx) ? "24-hour time" : "12-hour time");
        rule(sb, "S1", Build.VERSION.SDK_INT >= 37 ? "Live Updates and MetricStyle available"
                : Build.VERSION.SDK_INT >= 36 ? "Live Updates available" : "no Live Updates on this Android");
        if (Build.VERSION.SDK_INT >= 36) {
            AdvancedProtectionManager apm = ctx.getSystemService(AdvancedProtectionManager.class);
            boolean on = false;
            try {
                on = apm != null && apm.isAdvancedProtectionEnabled();
            } catch (SecurityException ignored) {
                on = false;
            }
            if (on) rule(sb, "S3", "Advanced Protection on: accessibility overlay blocked by Android");
        }
        int torchLevels = torchLevels(ctx);
        rule(sb, "F1", torchLevels > 1 ? torchLevels + " flashlight levels" : "flashlight on/off only");
        line(sb, "\nFont", Fonts.isGoogleSans() ? "Google Sans (system)" : "system default (Google Sans not available)");
        return sb.toString();
    }

    private static int torchLevels(Context ctx) {
        CameraManager cm = ctx.getSystemService(CameraManager.class);
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                if (Boolean.TRUE.equals(ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    Integer max = ch.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                    return max == null ? 1 : max;
                }
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 0;
    }

    private static String readSystem(Context ctx, String key) {
        try {
            String v = Settings.System.getString(ctx.getContentResolver(), key);
            return v == null ? "default" : v;
        } catch (SecurityException e) {
            return "unreadable";
        }
    }

    private static String readSecure(Context ctx, String key) {
        try {
            return Settings.Secure.getString(ctx.getContentResolver(), key);
        } catch (SecurityException e) {
            return null;
        }
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append(k).append(": ").append(v).append('\n');
    }

    private static void rule(StringBuilder sb, String id, String reason) {
        sb.append("  ").append(id).append("  ").append(reason).append('\n');
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
