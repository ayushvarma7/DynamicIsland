package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.DisplayCutout;

import org.json.JSONObject;

/** Section 7.2 inputs measured from the live cutout: center (cx, cy), radius r, ring gap g. Display pixels. */
final class Geometry {
    final boolean hasCutout;
    final float cx;
    final float cy;
    final float r;
    final float gapPx;
    final float density;
    final int screenWidth;

    private Geometry(boolean hasCutout, float cx, float cy, float r, float gapPx, float density, int screenWidth) {
        this.hasCutout = hasCutout;
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.gapPx = gapPx;
        this.density = density;
        this.screenWidth = screenWidth;
    }

    static Geometry measure(Context ctx) {
        Display d = ctx.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        float density = ctx.getResources().getDisplayMetrics().density;
        int width = d.getMode().getPhysicalWidth();
        if (d.getRotation() % 2 == 1) width = d.getMode().getPhysicalHeight();
        float gap = 3f * density;
        DisplayCutout dc = d.getCutout();
        if (dc != null) {
            RectF b = new RectF();
            Path p = dc.getCutoutPath();
            Rect top = dc.getBoundingRectTop();
            if (p != null) {
                p.computeBounds(b, true);
                if (!top.isEmpty()) b.intersect(new RectF(top));
            } else if (!top.isEmpty()) {
                b.set(top);
            }
            if (!b.isEmpty()) {
                return new Geometry(true, b.centerX(), b.centerY(), Math.min(b.width(), b.height()) / 2f, gap, density, width);
            }
        }
        // Rule G3: no cutout -> virtual island centered in the status bar.
        int sbId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
        float sb = sbId == 0 ? 24 * density : ctx.getResources().getDimensionPixelSize(sbId);
        float hc = sb - 6 * density;
        return new Geometry(false, width / 2f, sb / 2f, hc / 2f - gap, gap, density, width);
    }

    /** Compact height Hc = 2r + 2g. */
    float compactHeight() {
        return 2 * r + 2 * gapPx;
    }

    /** Compact total width = Hc + 2 * round(1.4 * Hc). */
    float compactWidth() {
        float hc = compactHeight();
        return hc + 2 * Math.round(1.4f * hc);
    }

    JSONObject toJson() {
        JSONObject o = obj();
        put(o, "hasCutout", hasCutout);
        put(o, "cx", cx);
        put(o, "cy", cy);
        put(o, "r", r);
        put(o, "gapPx", gapPx);
        put(o, "density", density);
        put(o, "screenWidth", screenWidth);
        put(o, "compactHeight", compactHeight());
        put(o, "compactWidth", compactWidth());
        return o;
    }
}
