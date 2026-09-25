package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Debug style: magenta stroke on the measured cutout edge (r), cyan stroke on the ring edge (r + g),
 * yellow outline of the compact pill, crosshair at the center. Product style: solid black ring/pill.
 */
@SuppressLint("ViewConstructor")
final class IslandTestView extends View {
    private final Geometry g;
    private final float originX;
    private final float originY;
    private final Paint cutoutStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    boolean debugStyle = true;
    /** 0 = idle ring, 1 = full compact width; driven by FrameRateTest. */
    float expandFraction;

    IslandTestView(Context ctx, Geometry g, float originX, float originY) {
        super(ctx);
        this.g = g;
        this.originX = originX;
        this.originY = originY;
        cutoutStroke.setStyle(Paint.Style.STROKE);
        cutoutStroke.setStrokeWidth(1f);
        cutoutStroke.setColor(0xFFFF00FF);
        ringStroke.setStyle(Paint.Style.STROKE);
        ringStroke.setStrokeWidth(1f);
        ringStroke.setColor(0xFF00FFFF);
        pillStroke.setStyle(Paint.Style.STROKE);
        pillStroke.setStrokeWidth(1f);
        pillStroke.setColor(0xFFFFEB3B);
        black.setColor(0xFF000000);
    }

    @Override
    protected void onDraw(Canvas c) {
        float x = g.cx - originX;
        float y = g.cy - originY;
        float ringR = g.r + g.gapPx;
        float halfH = g.compactHeight() / 2f;
        float halfW = halfH + (g.compactWidth() / 2f - halfH) * expandFraction;
        if (debugStyle) {
            c.drawCircle(x, y, g.r, cutoutStroke);
            c.drawCircle(x, y, ringR, ringStroke);
            c.drawLine(x - 4, y, x + 4, y, cutoutStroke);
            c.drawLine(x, y - 4, x, y + 4, cutoutStroke);
            tmp.set(x - g.compactWidth() / 2f, y - halfH, x + g.compactWidth() / 2f, y + halfH);
            c.drawRoundRect(tmp, halfH, halfH, pillStroke);
        }
        if (!debugStyle || expandFraction > 0f) {
            tmp.set(x - halfW, y - halfH, x + halfW, y + halfH);
            c.drawRoundRect(tmp, halfH, halfH, black);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Probe.log("touch_overlay", put(put(obj(), "x", e.getRawX()), "y", e.getRawY()));
        }
        return true;
    }
}
