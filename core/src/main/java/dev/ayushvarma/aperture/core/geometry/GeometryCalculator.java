package dev.ayushvarma.aperture.core.geometry;

/**
 * Section 7.2 formulas plus adaptation rules G1-G5 and the Gate D4 width cap (ADR-004).
 * Live measurement is the authority; calibration nudges it; nothing here is device-specific.
 */
public final class GeometryCalculator {
    /** A cutout within this share of the width from the middle counts as centered (rule G1). */
    private static final float CENTER_TOLERANCE = 0.04f;
    private static final float COMPACT_RATIO = 1.4f;

    private GeometryCalculator() {}

    public static IslandGeometry compute(DisplayInfo d, Calibration cal) {
        float gap = d.dp(cal.gapDp);
        CutoutInfo c = d.cutout;
        IslandGeometry.Layout layout;
        float cx;
        float cy;
        float r;
        if (c.kind == CutoutInfo.Kind.NONE) {
            // G3: pill centered in the status bar, height = status bar - 6 dp.
            layout = IslandGeometry.Layout.VIRTUAL;
            float height = Math.max(d.dp(16), d.statusBarPx - d.dp(6));
            cx = d.widthPx / 2f;
            cy = d.statusBarPx / 2f;
            r = height / 2f - gap;
        } else {
            cx = c.centerX();
            cy = c.centerY();
            r = c.radius();
            boolean centered = Math.abs(cx - d.widthPx / 2f) <= CENTER_TOLERANCE * d.widthPx;
            layout = centered ? IslandGeometry.Layout.SYMMETRIC : IslandGeometry.Layout.OFFSET;
        }
        cx += d.dp(cal.offsetXDp);
        cy += d.dp(cal.offsetYDp);
        r = Math.max(d.dp(4), r + d.dp(cal.sizeDp));

        float hc = 2 * r + 2 * gap;
        float side = Math.round(COMPACT_RATIO * hc) * cal.compactWidthScale;
        float leading = side;
        float trailing = side;
        if (layout == IslandGeometry.Layout.OFFSET) {
            // G2: both compact slots on the side facing the screen center.
            boolean holeOnLeft = cx < d.widthPx / 2f;
            leading = holeOnLeft ? 0 : 2 * side;
            trailing = holeOnLeft ? 2 * side : 0;
        }
        // D4: never reach into the privacy-indicator zone; keep the same budget on the other side.
        if (d.privacyLeftPx > 0 && layout != IslandGeometry.Layout.OFFSET) {
            float maxHalf = d.privacyLeftPx - d.dp(8) - cx;
            float half = hc / 2f + trailing;
            if (half > maxHalf && maxHalf > hc / 2f) {
                float capped = maxHalf - hc / 2f;
                leading = capped;
                trailing = capped;
            }
        }

        float margin = d.dp(12) + d.waterfallSidePx; // G4 adds curved-edge insets to m.
        float expandedWidth = d.widthPx - 2 * margin;
        float screenCorner = d.topCornerRadiusPx > 0 ? d.topCornerRadiusPx : d.dp(36);
        float expandedCorner = Math.max(d.dp(16), screenCorner - margin);
        float expandedTop = Math.max(0, cy - hc / 2f);
        return new IslandGeometry(layout, cx, cy, r, gap, leading, trailing, r + d.dp(4), margin, expandedWidth,
                expandedTop, expandedCorner, d.dp(16), d.dp(6), !d.isLandscape());
    }
}
