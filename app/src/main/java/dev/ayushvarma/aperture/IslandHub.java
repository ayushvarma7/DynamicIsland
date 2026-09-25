package dev.ayushvarma.aperture;

import android.os.Handler;
import android.os.Looper;

import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.core.engine.IslandEngine;
import dev.ayushvarma.aperture.core.engine.IslandState;

/**
 * Owns the pure engine and runs it on the main thread with the wall clock, scheduling
 * {@link IslandEngine#tick()} exactly at the engine's next deadline (no polling when idle).
 */
public final class IslandHub {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final IslandEngine engine = new IslandEngine(System::currentTimeMillis);
    private final Runnable tick = this::tick;

    IslandHub() {
        engine.addListener(s -> schedule());
    }

    public IslandEngine engine() {
        return engine;
    }

    public IslandState state() {
        return engine.state();
    }

    public void upsert(IslandActivity a) {
        run(() -> engine.upsert(a));
    }

    public void remove(String id) {
        run(() -> engine.remove(id));
    }

    public void postTransient(IslandActivity a, long durationMs) {
        run(() -> engine.postTransient(a, durationMs));
    }

    /** Asks the island to open the expanded view of an activity (used by "Try it out"). */
    public interface ExpandListener {
        void onExpandRequest(String activityId);
    }

    private final java.util.List<ExpandListener> expandListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addExpandListener(ExpandListener l) {
        expandListeners.add(l);
    }

    public void removeExpandListener(ExpandListener l) {
        expandListeners.remove(l);
    }

    public void requestExpand(String activityId) {
        run(() -> {
            for (ExpandListener l : expandListeners) l.onExpandRequest(activityId);
        });
    }

    public void addListener(IslandEngine.Listener l) {
        engine.addListener(l);
    }

    public void removeListener(IslandEngine.Listener l) {
        engine.removeListener(l);
    }

    private void run(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
            schedule();
        } else {
            main.post(() -> {
                r.run();
                schedule();
            });
        }
    }

    private void tick() {
        engine.tick();
        schedule();
    }

    private void schedule() {
        main.removeCallbacks(tick);
        long next = engine.nextDeadline();
        if (next == Long.MAX_VALUE) return;
        main.postDelayed(tick, Math.max(0, next - System.currentTimeMillis()) + 5);
    }
}
