package dev.ayushvarma.aperture.core.engine;

import java.util.Objects;

/**
 * One thing the island can show. Immutable; sources build a new one on every update.
 * {@link #payload} carries platform objects (PendingIntents, bitmaps, controllers) the pure engine never reads.
 */
public final class IslandActivity {
    public final String id;
    public final String pkg;
    public final ActivityKind kind;
    public final CharSequence title;
    public final CharSequence text;
    /** Short text for the compact trailing slot (e.g. "4 min"); may be null. */
    public final CharSequence shortText;
    /** 0..1, or negative when there is no determinate progress. */
    public final float progress;
    /** Wall-clock base for a chronometer (call timer, countdown), 0 when none. */
    public final long chronometerBaseMs;
    public final boolean countDown;
    public final boolean playing;
    public final int accentColor;
    public final long createdAt;
    public final long updatedAt;
    public final Object payload;

    private IslandActivity(Builder b) {
        this.id = b.id;
        this.pkg = b.pkg;
        this.kind = b.kind;
        this.title = b.title;
        this.text = b.text;
        this.shortText = b.shortText;
        this.progress = b.progress;
        this.chronometerBaseMs = b.chronometerBaseMs;
        this.countDown = b.countDown;
        this.playing = b.playing;
        this.accentColor = b.accentColor;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.payload = b.payload;
    }

    public static Builder builder(String id, String pkg, ActivityKind kind) {
        return new Builder(id, pkg, kind);
    }

    /** Identity of what is displayed, for the Section 6.3 rule 6 duplicate guard. */
    public int contentHash() {
        return Objects.hash(kind, pkg, str(title), str(text), str(shortText), playing,
                progress < 0 ? -1 : Math.round(progress * 100));
    }

    private static String str(CharSequence s) {
        return s == null ? null : s.toString();
    }

    @Override
    public String toString() {
        return kind + "(" + id + ", " + str(title) + ")";
    }

    public static final class Builder {
        private final String id;
        private final String pkg;
        private final ActivityKind kind;
        private CharSequence title;
        private CharSequence text;
        private CharSequence shortText;
        private float progress = -1f;
        private long chronometerBaseMs;
        private boolean countDown;
        private boolean playing;
        private int accentColor;
        private long createdAt;
        private long updatedAt;
        private Object payload;

        private Builder(String id, String pkg, ActivityKind kind) {
            this.id = Objects.requireNonNull(id);
            this.pkg = pkg;
            this.kind = Objects.requireNonNull(kind);
        }

        public Builder title(CharSequence v) { title = v; return this; }
        public Builder text(CharSequence v) { text = v; return this; }
        public Builder shortText(CharSequence v) { shortText = v; return this; }
        public Builder progress(float v) { progress = v; return this; }
        public Builder chronometer(long baseMs, boolean down) { chronometerBaseMs = baseMs; countDown = down; return this; }
        public Builder playing(boolean v) { playing = v; return this; }
        public Builder accentColor(int v) { accentColor = v; return this; }
        public Builder createdAt(long v) { createdAt = v; return this; }
        public Builder updatedAt(long v) { updatedAt = v; return this; }
        public Builder payload(Object v) { payload = v; return this; }

        public IslandActivity build() {
            return new IslandActivity(this);
        }
    }
}
