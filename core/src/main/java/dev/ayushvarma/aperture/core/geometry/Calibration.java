package dev.ayushvarma.aperture.core.geometry;

/** User fine-tune on top of the measured cutout (Section 5 P0.1). Offsets and sizes in dp. */
public final class Calibration {
    public static final Calibration DEFAULT = new Calibration(0f, 0f, 0f, 3f, 1f);

    public final float offsetXDp;
    public final float offsetYDp;
    /** Added to the cutout radius. */
    public final float sizeDp;
    /** Ring gap {@code g}, 0 to 8 dp. */
    public final float gapDp;
    /** Scales the compact leading/trailing widths, 0.6 to 1.3. */
    public final float compactWidthScale;

    public Calibration(float offsetXDp, float offsetYDp, float sizeDp, float gapDp, float compactWidthScale) {
        this.offsetXDp = offsetXDp;
        this.offsetYDp = offsetYDp;
        this.sizeDp = sizeDp;
        this.gapDp = clamp(gapDp, 0f, 8f);
        this.compactWidthScale = clamp(compactWidthScale, 0.6f, 1.3f);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
