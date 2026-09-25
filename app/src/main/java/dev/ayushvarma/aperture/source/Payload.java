package dev.ayushvarma.aperture.source;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;

/** Platform objects behind an {@code IslandActivity}. Every field is optional. */
public final class Payload {
    /** Album art, contact photo or large icon. */
    public Bitmap art;
    /** Owning app's icon (or the notification's small icon). */
    public Drawable appIcon;
    public CharSequence appName;
    /** Tap target: notification content intent or the media session's launch activity. */
    public PendingIntent open;
    /** Notification key, for dismiss and reply. */
    public String notificationKey;

    // Media
    public MediaController controller;
    public long durationMs;
    public long positionMs;
    public long positionUpdateElapsedMs;
    public float speed = 1f;
    public Drawable customActionIcon;
    public String customAction;
    public CharSequence customActionName;

    // Notification actions
    public Notification.Action replyAction;
    public PendingIntent answer;
    public PendingIntent decline;
    public PendingIntent hangUp;
    /** True when the text was replaced by the public version for the lock screen. */
    public boolean redacted;
}
