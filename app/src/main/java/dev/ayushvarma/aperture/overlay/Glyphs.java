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

    /** Handset: two rounded ends joined by an arc; rotated 135 degrees for "hang up". */
    void phone(Canvas c, float cx, float cy, float s, Paint p, boolean hangUp) {
        c.save();
        c.rotate(hangUp ? 135 : 0, cx, cy);
        Paint.Style old = p.getStyle();
        float oldW = p.getStrokeWidth();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.18f);
        p.setStrokeCap(Paint.Cap.ROUND);
        tmp.set(cx - s * 0.36f, cy - s * 0.36f, cx + s * 0.36f, cy + s * 0.36f);
        c.drawArc(tmp, 100, 160, false, p);
        p.setStyle(old);
        p.setStrokeWidth(oldW);
        c.drawCircle(cx - s * 0.30f, cy + s * 0.22f, s * 0.13f, p);
        c.drawCircle(cx + s * 0.02f, cy - s * 0.38f, s * 0.13f, p);
        c.restore();
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
}
