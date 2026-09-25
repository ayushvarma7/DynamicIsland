package dev.ayushvarma.aperture.core.geometry;

/** Every island dimension for one display state, in pixels. Produced by {@link GeometryCalculator}. */
public final class IslandGeometry {
    /** Adaptation rules G1 (centered circle), G2 (off-center), G3 (no cutout: virtual island). */
    public enum Layout { SYMMETRIC, OFFSET, VIRTUAL }

    public final Layout layout;
    public final float centerX;
    public final float centerY;
    /** Cutout radius {@code r} after calibration. */
    public final float cutoutRadius;
    public final float gap;
    /** {@code Hc = 2r + 2g}. */
    public final float compactHeight;
    public final float leadingWidth;
    public final float trailingWidth;
    public final float keepOutRadius;
    public final float expandedLeft;
    public final float expandedWidth;
    public final float expandedTop;
    public final float expandedCorner;
    public final float innerPadding;
    public final float detentSmall;
    public final float detentMedium;
    public final float detentLarge;
    public final float bubbleGap;
    /** Height the overlay window needs to contain every presentation. */
    public final int windowHeight;
    /** False in landscape (rule G5; the landscape presentation is P2). */
    public final boolean visible;

    IslandGeometry(Layout layout, float centerX, float centerY, float cutoutRadius, float gap, float leadingWidth,
                   float trailingWidth, float keepOutRadius, float expandedLeft, float expandedWidth, float expandedTop,
                   float expandedCorner, float innerPadding, float bubbleGap, boolean visible) {
        this.layout = layout;
        this.centerX = centerX;
        this.centerY = centerY;
        this.cutoutRadius = cutoutRadius;
        this.gap = gap;
        this.compactHeight = 2 * cutoutRadius + 2 * gap;
        this.leadingWidth = leadingWidth;
        this.trailingWidth = trailingWidth;
        this.keepOutRadius = keepOutRadius;
        this.expandedLeft = expandedLeft;
        this.expandedWidth = expandedWidth;
        this.expandedTop = expandedTop;
        this.expandedCorner = expandedCorner;
        this.innerPadding = innerPadding;
        this.detentSmall = 0.23f * expandedWidth;
        this.detentMedium = 0.43f * expandedWidth;
        this.detentLarge = 0.52f * expandedWidth;
        this.bubbleGap = bubbleGap;
        this.windowHeight = (int) Math.ceil(expandedTop + detentLarge + 2 * innerPadding);
        this.visible = visible;
    }

    public float compactWidth() {
        return compactHeight + leadingWidth + trailingWidth;
    }

    /** Left edge of the compact pill; leading content sits left of the camera, trailing to the right. */
    public float compactLeft() {
        return centerX - compactHeight / 2f - leadingWidth;
    }

    public float compactTop() {
        return centerY - compactHeight / 2f;
    }

    @Override
    public String toString() {
        return layout + " center=(" + centerX + "," + centerY + ") r=" + cutoutRadius + " Hc=" + compactHeight
                + " compact=" + compactWidth() + " expanded=" + expandedWidth + "x[" + detentSmall + "," + detentMedium
                + "," + detentLarge + "] Re=" + expandedCorner;
    }
}
