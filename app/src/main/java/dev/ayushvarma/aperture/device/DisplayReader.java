package dev.ayushvarma.aperture.device;

import android.content.Context;
import android.graphics.Insets;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.RoundedCorner;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import dev.ayushvarma.aperture.core.geometry.CutoutInfo;
import dev.ayushvarma.aperture.core.geometry.DisplayInfo;

/** Live measurement (authority #1 in Section 9): cutout path, corners, status bar, privacy zone. */
public final class DisplayReader {
    private DisplayReader() {}

    public static DisplayInfo read(Context ctx) {
        return read(ctx, null);
    }

    /** @param overlayInsets insets delivered to the overlay window, when available (fresher than metrics). */
    public static DisplayInfo read(Context ctx, WindowInsets overlayInsets) {
        WindowManager wm = ctx.getSystemService(WindowManager.class);
        WindowMetrics metrics = wm.getMaximumWindowMetrics();
        Rect bounds = metrics.getBounds();
        WindowInsets insets = overlayInsets != null ? overlayInsets : metrics.getWindowInsets();
        float density = ctx.getResources().getDisplayMetrics().density;
        Display display = ctx.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);

        DisplayCutout dc = display.getCutout();
        if (dc == null) dc = metrics.getWindowInsets().getDisplayCutout();
        CutoutInfo cutout = CutoutInfo.none();
        int waterfall = 0;
        if (dc != null) {
            Insets w = dc.getWaterfallInsets();
            waterfall = Math.max(w.left, w.right);
            cutout = cutoutFrom(dc);
        }

        int statusBar = metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;
        if (statusBar <= 0) statusBar = Math.round(24 * density);
        RoundedCorner corner = display.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
        int cornerRadius = corner == null ? 0 : corner.getRadius();
        Rect privacy = insets.getPrivacyIndicatorBounds();
        int privacyLeft = privacy == null || privacy.isEmpty() ? 0 : privacy.left;
        return new DisplayInfo(bounds.width(), bounds.height(), density, statusBar, cornerRadius, privacyLeft,
                waterfall, cutout);
    }

    /** Uses the precise path when present; restricts to the top cutout so double cutouts don't merge. */
    static CutoutInfo cutoutFrom(DisplayCutout dc) {
        Rect top = dc.getBoundingRectTop();
        Path path = dc.getCutoutPath();
        RectF b = new RectF();
        if (path != null) {
            path.computeBounds(b, true);
            if (!top.isEmpty() && !b.intersect(new RectF(top))) b.setEmpty();
        } else if (!top.isEmpty()) {
            b.set(top);
        }
        return b.isEmpty() ? CutoutInfo.none() : CutoutInfo.fromBounds(b.left, b.top, b.right, b.bottom);
    }
}
