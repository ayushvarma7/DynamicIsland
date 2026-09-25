package dev.ayushvarma.aperture.core.engine;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** What the renderer should draw right now. */
public final class IslandState {
    public static final IslandState EMPTY = new IslandState(null, Collections.emptyList(), null);

    /** Owner of the compact pill, or null when idle. */
    public final IslandActivity compact;
    /** Up to two minimal bubbles: first on the trailing side, second on the leading side. */
    public final List<IslandActivity> minimal;
    /** A transient preempting compact for its duration, or null. */
    public final IslandActivity transientActivity;

    IslandState(IslandActivity compact, List<IslandActivity> minimal, IslandActivity transientActivity) {
        this.compact = compact;
        this.minimal = Collections.unmodifiableList(minimal);
        this.transientActivity = transientActivity;
    }

    public boolean isIdle() {
        return compact == null && transientActivity == null;
    }

    /** True when both states show the same objects (a fresh object for an update counts as a change). */
    public boolean sameAs(IslandState o) {
        return o != null && compact == o.compact && transientActivity == o.transientActivity && minimal.equals(o.minimal);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IslandState && sameAs((IslandState) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compact, minimal, transientActivity);
    }

    @Override
    public String toString() {
        return "IslandState{compact=" + compact + ", minimal=" + minimal + ", transient=" + transientActivity + "}";
    }
}
