package dev.ayushvarma.aperture.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Small geometric glyphs drawn in a box of side {@code s} centered at (cx, cy). */
final class Glyphs {
    private final Path path = new Path();
    private final RectF tmp = new RectF();

    void play(Canvas c, float cx, float cy, float s, Paint p) {
        path.reset();
        path.moveTo(cx - s * 0.30f, cy - s * 0.38f);
        path.lineTo(cx + s * 0.40f, cy);
        path.lineTo(cx - s * 0.30f, cy + s * 0.38f);
        path.close();
        c.drawPath(path, p);
    }

    void pause(Canvas c, float cx, float cy, float s, Paint p) {
        float w = s * 0.22f;
        float h = s * 0.72f;
        tmp.set(cx - s * 0.30f, cy - h / 2, cx - s * 0.30f + w, cy + h / 2);
        c.drawRoundRect(tmp, w / 3, w / 3, p);
        tmp.set(cx + s * 0.30f - w, cy - h / 2, cx + s * 0.30f, cy + h / 2);
        c.drawRoundRect(tmp, w / 3, w / 3, p);
    }

    void next(Canvas c, float cx, float cy, float s, Paint p, boolean forward) {
        float d = forward ? 1 : -1;
        path.reset();
        path.moveTo(cx - d * s * 0.34f, cy - s * 0.32f);
        path.lineTo(cx + d * s * 0.18f, cy);
        path.lineTo(cx - d * s * 0.34f, cy + s * 0.32f);
        path.close();
        c.drawPath(path, p);
        float bx = cx + d * s * 0.26f;
        tmp.set(Math.min(bx, bx + d * s * 0.1f), cy - s * 0.32f, Math.max(bx, bx + d * s * 0.1f), cy + s * 0.32f);
        c.drawRect(tmp, p);
    }

    // Material Icons "call" and "call_end", in their 24 x 24 viewport.
    private static final Path CALL = androidx.core.graphics.PathParser.createPathFromPathData(
            "M20.01 15.38c-1.23 0-2.42-.2-3.53-.56-.35-.12-.74-.03-1.01.24l-1.57 1.97c-2.83-1.35-5.48-3.9-6.89-6.83"
                    + "l1.95-1.66c.27-.28.35-.67.24-1.02-.37-1.11-.56-2.3-.56-3.53 0-.54-.45-.99-.99-.99H4.19"
                    + "C3.65 3 3 3.24 3 3.99 3 13.28 10.73 21 20.01 21c.71 0 .99-.63.99-1.18v-3.45c0-.54-.45-.99-.99-.99z");
    private static final Path CALL_END = androidx.core.graphics.PathParser.createPathFromPathData(
            "M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28"
                    + "-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7"
                    + "s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29"
                    + "-.27 0-.52-.11-.7-.28-.79-.74-1.69-1.36-2.67-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z");
    private final android.graphics.Matrix scale = new android.graphics.Matrix();

    /** Handset (Material "call"); the lowered handset ("call_end") for hang up and decline. */
    void phone(Canvas c, float cx, float cy, float s, Paint p, boolean hangUp) {
        float k = s / 24f;
        scale.setScale(k, k);
        scale.postTranslate(cx - 12 * k, cy - 12 * k);
        path.reset();
        path.addPath(hangUp ? CALL_END : CALL, scale);
        Paint.Style old = p.getStyle();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(path, p);
        p.setStyle(old);
    }

    void bolt(Canvas c, float cx, float cy, float s, Paint p) {
        path.reset();
        path.moveTo(cx + s * 0.08f, cy - s * 0.48f);
        path.lineTo(cx - s * 0.30f, cy + s * 0.06f);
        path.lineTo(cx - s * 0.02f, cy + s * 0.06f);
        path.lineTo(cx - s * 0.10f, cy + s * 0.48f);
        path.lineTo(cx + s * 0.30f, cy - s * 0.08f);
        path.lineTo(cx + s * 0.02f, cy - s * 0.08f);
        path.close();
        c.drawPath(path, p);
    }

    /** Output device (speaker with a wave) for the media output switcher. */
    void output(Canvas c, float cx, float cy, float s, Paint p) {
        tmp.set(cx - s * 0.40f, cy - s * 0.14f, cx - s * 0.20f, cy + s * 0.14f);
        c.drawRect(tmp, p);
        path.reset();
        path.moveTo(cx - s * 0.22f, cy - s * 0.14f);
        path.lineTo(cx + s * 0.02f, cy - s * 0.36f);
        path.lineTo(cx + s * 0.02f, cy + s * 0.36f);
        path.lineTo(cx - s * 0.22f, cy + s * 0.14f);
        path.close();
        c.drawPath(path, p);
        Paint.Style old = p.getStyle();
        float oldW = p.getStrokeWidth();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.08f);
        tmp.set(cx - s * 0.12f, cy - s * 0.30f, cx + s * 0.40f, cy + s * 0.30f);
        c.drawArc(tmp, -50, 100, false, p);
        p.setStyle(old);
        p.setStrokeWidth(oldW);
    }

    /** Output device: a speaker cabinet (rounded outline with a woofer and a tweeter). */
    void cast(Canvas c, float cx, float cy, float s, Paint p) {
        Paint.Style old = p.getStyle();
        float oldW = p.getStrokeWidth();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.09f);
        tmp.set(cx - s * 0.32f, cy - s * 0.44f, cx + s * 0.32f, cy + s * 0.44f);
        c.drawRoundRect(tmp, s * 0.12f, s * 0.12f, p);
        c.drawCircle(cx, cy + s * 0.12f, s * 0.15f, p);
        p.setStyle(old);
        p.setStrokeWidth(oldW);
        c.drawCircle(cx, cy - s * 0.24f, s * 0.06f, p);
    }
}
