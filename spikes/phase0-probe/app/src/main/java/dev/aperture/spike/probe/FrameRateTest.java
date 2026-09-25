package dev.aperture.spike.probe;

import static dev.aperture.spike.probe.Probe.obj;
import static dev.aperture.spike.probe.Probe.put;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Gate D11 spike: animates the overlay without any touch (an "event-triggered" animation) and records the
 * vsync interval Choreographer actually delivers, under each refresh-rate request strategy:
 * none | lp_rate (preferredRefreshRate) | lp_mode (preferredDisplayModeId) | view_high (API 35 category HIGH) | view_hz.
 */
final class FrameRateTest {
    private static final long DURATION_NS = 2_000_000_000L;
    private static final long SETTLE_MS = 400;

    private FrameRateTest() {}

    static void run(WindowManager wm, IslandTestView view, WindowManager.LayoutParams lp, String mode) {
        if (view == null || !view.isAttachedToWindow()) {
            Probe.log("anim_test_error", "reason", "overlay not attached; send cmd=overlay mode=fixed first");
            return;
        }
        Display d = view.getDisplay();
        Display.Mode cur = d.getMode();
        Display.Mode best = cur;
        for (Display.Mode m : d.getSupportedModes()) {
            if (m.getPhysicalWidth() == cur.getPhysicalWidth() && m.getPhysicalHeight() == cur.getPhysicalHeight()
                    && m.getRefreshRate() > best.getRefreshRate()) {
                best = m;
            }
        }
        final String strategy = mode == null ? "none" : mode;
        final float hzBefore = d.getRefreshRate();
        switch (strategy) {
            case "lp_rate":
                lp.preferredRefreshRate = best.getRefreshRate();
                wm.updateViewLayout(view, lp);
                break;
            case "lp_mode":
                lp.preferredDisplayModeId = best.getModeId();
                wm.updateViewLayout(view, lp);
                break;
            case "view_high":
                view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH);
                break;
            case "view_hz":
                view.setRequestedFrameRate(best.getRefreshRate());
                break;
            default:
                break;
        }
        final Display.Mode target = best;
        final long[] frames = new long[2000];
        final int[] n = {0};
        final float[] hzMid = {0f};
        Choreographer ch = Choreographer.getInstance();
        Choreographer.FrameCallback cb = new Choreographer.FrameCallback() {
            long start = -1;

            @Override
            public void doFrame(long frameTimeNanos) {
                if (start < 0) start = frameTimeNanos;
                if (n[0] < frames.length) frames[n[0]++] = frameTimeNanos;
                long t = frameTimeNanos - start;
                float phase = (t % 1_000_000_000L) / 1_000_000_000f;
                view.expandFraction = phase < 0.5f ? phase * 2f : (1f - phase) * 2f;
                view.invalidate();
                if (hzMid[0] == 0f && t > DURATION_NS / 2) hzMid[0] = d.getRefreshRate();
                if (t < DURATION_NS) {
                    ch.postFrameCallback(this);
                } else {
                    view.expandFraction = 0f;
                    view.invalidate();
                    report(strategy, target, hzBefore, hzMid[0], d.getRefreshRate(), frames, n[0]);
                    restore(wm, view, lp, strategy);
                }
            }
        };
        new Handler(Looper.getMainLooper()).postDelayed(() -> ch.postFrameCallback(cb), SETTLE_MS);
    }

    private static void restore(WindowManager wm, IslandTestView view, WindowManager.LayoutParams lp, String strategy) {
        switch (strategy) {
            case "lp_rate":
                lp.preferredRefreshRate = 0f;
                wm.updateViewLayout(view, lp);
                break;
            case "lp_mode":
                lp.preferredDisplayModeId = 0;
                wm.updateViewLayout(view, lp);
                break;
            case "view_high":
            case "view_hz":
                view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT);
                break;
            default:
                break;
        }
    }

    private static void report(String strategy, Display.Mode target, float hzBefore, float hzMid, float hzAfter,
                               long[] frames, int count) {
        JSONObject o = obj();
        put(o, "strategy", strategy);
        put(o, "targetModeId", target.getModeId());
        put(o, "targetHz", target.getRefreshRate());
        put(o, "displayHzBefore", hzBefore);
        put(o, "displayHzMid", hzMid);
        put(o, "displayHzAfter", hzAfter);
        put(o, "frames", count);
        if (count > 2) {
            double[] deltas = new double[count - 1];
            for (int i = 1; i < count; i++) deltas[i - 1] = (frames[i] - frames[i - 1]) / 1e6;
            double[] sorted = deltas.clone();
            Arrays.sort(sorted);
            double sum = 0;
            int under10 = 0;
            for (double v : deltas) {
                sum += v;
                if (v < 10.0) under10++;
            }
            put(o, "meanIntervalMs", round(sum / deltas.length));
            put(o, "p50IntervalMs", round(sorted[sorted.length / 2]));
            put(o, "p95IntervalMs", round(sorted[(int) Math.floor(sorted.length * 0.95)]));
            put(o, "maxIntervalMs", round(sorted[sorted.length - 1]));
            put(o, "shareUnder10ms", round(under10 / (double) deltas.length));
            JSONArray first = new JSONArray();
            for (int i = 0; i < Math.min(12, deltas.length); i++) Probe.add(first, round(deltas[i]));
            put(o, "firstIntervalsMs", first);
        }
        Probe.log("anim_test", o);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
