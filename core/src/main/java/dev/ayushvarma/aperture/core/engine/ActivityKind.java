package dev.ayushvarma.aperture.core.engine;

/** Section 6.2 activity kinds with their Section 6.3 base priorities. */
public enum ActivityKind {
    INCOMING_CALL(100, true),
    ALARM(95, true),
    ONGOING_CALL(85, true),
    NAVIGATION(80, true),
    LIVE_UPDATE(70, true),
    /** 60 while playing, 10 while paused (see {@link IslandEngine#priorityOf}). */
    MEDIA(60, true),
    TIMER(55, true),
    STOPWATCH(55, true),
    RECORDING(50, true),
    DOWNLOAD(40, true),
    SYSTEM(30, false),
    NOTIFICATION(20, false);

    public final int basePriority;
    public final boolean persistent;

    ActivityKind(int basePriority, boolean persistent) {
        this.basePriority = basePriority;
        this.persistent = persistent;
    }
}
