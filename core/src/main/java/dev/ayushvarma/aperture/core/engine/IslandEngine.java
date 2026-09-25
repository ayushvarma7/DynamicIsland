package dev.ayushvarma.aperture.core.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Section 6.3 arbitration. Not thread-safe: the app drives it from the main thread only.
 * Callers must invoke {@link #tick()} at {@link #nextDeadline()} so timed rules (transient end,
 * pin expiry, timer urgency, 8 h expiry) take effect.
 */
public final class IslandEngine {
    public interface Listener {
        void onStateChanged(IslandState state);
    }

    static final long DEDUPE_WINDOW_MS = 2_000;
    static final long PIN_MS = 30 * 60_000L;
    static final long STALE_MS = 8 * 60 * 60_000L;
    static final long TIMER_URGENT_MS = 10_000;
    static final int TRANSIENT_QUEUE_MAX = 5;
    static final int MAX_MINIMAL = 2;

    private final LongSupplier clock;
    private final Map<String, IslandActivity> persistent = new LinkedHashMap<>();
    private final Deque<Pending> queue = new ArrayDeque<>();
    private final List<Listener> listeners = new ArrayList<>();
    private Pending showing;
    private long showingUntil;
    private String pinnedId;
    private long pinnedUntil;
    private int lastTransientHash;
    private long lastTransientAt = Long.MIN_VALUE / 2;
    private IslandState state = IslandState.EMPTY;

    private static final class Pending {
        final IslandActivity activity;
        final long durationMs;

        Pending(IslandActivity activity, long durationMs) {
            this.activity = activity;
            this.durationMs = durationMs;
        }
    }

    public IslandEngine(LongSupplier clock) {
        this.clock = clock;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public IslandState state() {
        return state;
    }

    /** Adds or replaces a persistent activity. */
    public void upsert(IslandActivity a) {
        if (!a.kind.persistent) throw new IllegalArgumentException(a.kind + " is duration-bound; use postTransient");
        persistent.put(a.id, a);
        recompute();
    }

    public void remove(String id) {
        boolean changed = persistent.remove(id) != null;
        if (showing != null && showing.activity.id.equals(id)) {
            showing = null;
            changed = true;
        }
        for (Iterator<Pending> it = queue.iterator(); it.hasNext(); ) {
            if (it.next().activity.id.equals(id)) {
                it.remove();
                changed = true;
            }
        }
        if (id.equals(pinnedId)) pinnedId = null;
        if (changed) recompute();
    }

    public IslandActivity get(String id) {
        IslandActivity a = persistent.get(id);
        if (a != null) return a;
        return showing != null && showing.activity.id.equals(id) ? showing.activity : null;
    }

    /**
     * Queues a duration-bound activity. Returns false when rejected as a duplicate (same id or same content
     * within 2 s). Same-id transients coalesce: an update replaces the queued or showing one.
     */
    public boolean postTransient(IslandActivity a, long durationMs) {
        long now = clock.getAsLong();
        int hash = a.contentHash();
        if (now - lastTransientAt < DEDUPE_WINDOW_MS && hash == lastTransientHash) return false;
        lastTransientHash = hash;
        lastTransientAt = now;
        if (showing != null && showing.activity.id.equals(a.id)) {
            showing = new Pending(a, durationMs);
            showingUntil = now + durationMs;
            recompute();
            return true;
        }
        for (Iterator<Pending> it = queue.iterator(); it.hasNext(); ) {
            if (it.next().activity.id.equals(a.id)) it.remove();
        }
        queue.addLast(new Pending(a, durationMs));
        while (queue.size() > TRANSIENT_QUEUE_MAX) queue.removeFirst();
        recompute();
        return true;
    }

    /** Ends the showing transient early (swipe up / swipe away). */
    public void dismissTransient() {
        if (showing == null) return;
        showing = null;
        recompute();
    }

    /** Keeps the current transient on screen, e.g. while the user reads or replies. */
    public void holdTransient(long extraMs) {
        if (showing == null) return;
        showingUntil = Math.max(showingUntil, clock.getAsLong() + extraMs);
    }

    /** Rule 4: horizontal swipe on compact cycles persistent activities and pins the pick for 30 min. */
    public void cycle(int direction) {
        List<IslandActivity> ranked = ranked(clock.getAsLong());
        if (ranked.size() < 2 || state.compact == null) return;
        int i = ranked.indexOf(state.compact);
        int next = Math.floorMod(i + (direction >= 0 ? 1 : -1), ranked.size());
        pinnedId = ranked.get(next).id;
        pinnedUntil = clock.getAsLong() + PIN_MS;
        recompute();
    }

    public void tick() {
        recompute();
    }

    /** Earliest time a timed rule can change the state, or {@link Long#MAX_VALUE}. */
    public long nextDeadline() {
        long next = Long.MAX_VALUE;
        long now = clock.getAsLong();
        if (showing != null) next = Math.min(next, showingUntil);
        if (pinnedId != null) next = Math.min(next, pinnedUntil);
        for (IslandActivity a : persistent.values()) {
            next = Math.min(next, a.updatedAt + STALE_MS);
            if (a.kind == ActivityKind.TIMER && a.countDown && a.chronometerBaseMs - now > TIMER_URGENT_MS) {
                next = Math.min(next, a.chronometerBaseMs - TIMER_URGENT_MS);
            }
        }
        return next;
    }

    public int priorityOf(IslandActivity a, long now) {
        if (a.kind == ActivityKind.MEDIA) return a.playing ? 60 : 10;
        if (a.kind == ActivityKind.TIMER && a.countDown && a.chronometerBaseMs - now <= TIMER_URGENT_MS) return 90;
        return a.kind.basePriority;
    }

    private List<IslandActivity> ranked(long now) {
        List<IslandActivity> list = new ArrayList<>(persistent.values());
        list.sort(Comparator.comparingInt((IslandActivity a) -> priorityOf(a, now)).reversed()
                .thenComparing(Comparator.comparingLong((IslandActivity a) -> a.updatedAt).reversed()));
        return list;
    }

    private void recompute() {
        long now = clock.getAsLong();
        persistent.values().removeIf(a -> now - a.updatedAt >= STALE_MS);
        if (showing != null && now >= showingUntil) showing = null;
        if (showing == null && !queue.isEmpty()) {
            showing = queue.removeFirst();
            showingUntil = now + showing.durationMs;
        }
        if (pinnedId != null && (now >= pinnedUntil || !persistent.containsKey(pinnedId))) pinnedId = null;

        List<IslandActivity> ranked = ranked(now);
        IslandActivity compact = null;
        if (!ranked.isEmpty()) {
            compact = ranked.get(0);
            if (pinnedId != null && priorityOf(compact, now) < 90) compact = persistent.get(pinnedId);
        }
        List<IslandActivity> minimal = new ArrayList<>(MAX_MINIMAL);
        for (IslandActivity a : ranked) {
            if (a == compact) continue;
            if (minimal.size() == MAX_MINIMAL) break;
            minimal.add(a);
        }
        IslandState next = new IslandState(compact, minimal, showing == null ? null : showing.activity);
        if (!next.sameAs(state)) {
            state = next;
            for (Listener l : new ArrayList<>(listeners)) l.onStateChanged(next);
        }
    }
}
