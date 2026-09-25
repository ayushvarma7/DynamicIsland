package dev.ayushvarma.aperture.core.geometry;

/** Live display measurements that feed the Section 7.2 formulas. All lengths in pixels. */
public final class DisplayInfo {
    public final int widthPx;
    public final int heightPx;
    public final float density;
    public final int statusBarPx;
    /** Top-left screen corner radius, or {@code <= 0} when the display reports none. */
    public final int topCornerRadiusPx;
    /** Left edge of the privacy indicator zone, or {@code <= 0} when unknown. */
    public final int privacyLeftPx;
    public final int waterfallSidePx;
    public final CutoutInfo cutout;

    public DisplayInfo(int widthPx, int heightPx, float density, int statusBarPx, int topCornerRadiusPx,
                       int privacyLeftPx, int waterfallSidePx, CutoutInfo cutout) {
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.density = density;
        this.statusBarPx = statusBarPx;
        this.topCornerRadiusPx = topCornerRadiusPx;
        this.privacyLeftPx = privacyLeftPx;
        this.waterfallSidePx = waterfallSidePx;
        this.cutout = cutout;
    }

    public float dp(float value) {
        return value * density;
    }

    public boolean isLandscape() {
        return widthPx > heightPx;
    }
}
