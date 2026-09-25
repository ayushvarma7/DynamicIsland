package dev.ayushvarma.aperture.core.geometry;

/** The measured front-camera cutout in display pixels (from {@code DisplayCutout.getCutoutPath()} bounds). */
public final class CutoutInfo {
    public enum Kind { CIRCLE, RECT, NONE }

    public final Kind kind;
    public final float left;
    public final float top;
    public final float right;
    public final float bottom;

    private CutoutInfo(Kind kind, float left, float top, float right, float bottom) {
        this.kind = kind;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /** Classifies path bounds: a near-square box is a punch hole, anything else a notch rectangle. */
    public static CutoutInfo fromBounds(float left, float top, float right, float bottom) {
        float w = right - left;
        float h = bottom - top;
        if (w <= 0 || h <= 0) return none();
        float ratio = w / h;
        Kind kind = ratio > 0.8f && ratio < 1.25f ? Kind.CIRCLE : Kind.RECT;
        return new CutoutInfo(kind, left, top, right, bottom);
    }

    public static CutoutInfo none() {
        return new CutoutInfo(Kind.NONE, 0, 0, 0, 0);
    }

    public float centerX() {
        return (left + right) / 2f;
    }

    public float centerY() {
        return (top + bottom) / 2f;
    }

    /** Radius for a punch hole; half the height for a notch. */
    public float radius() {
        return Math.min(right - left, bottom - top) / 2f;
    }

    @Override
    public String toString() {
        return kind + "[" + left + "," + top + " - " + right + "," + bottom + "]";
    }
}
