package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.R;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.core.engine.IslandActivity;
import dev.ayushvarma.aperture.source.NotificationSource;
import dev.ayushvarma.aperture.source.Payload;

/**
 * Inline reply (P0.4) through the app's own RemoteInput action, exactly like replying from the shade.
 * A small dialog activity because the island window is deliberately never focusable.
 */
public final class ReplyActivity extends Activity {
    private static final String EXTRA_KEY = "key";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_ID = "id";

    public static Intent intent(Context ctx, IslandActivity a) {
        Payload p = a.payload instanceof Payload ? (Payload) a.payload : null;
        return new Intent(ctx, ReplyActivity.class)
                .putExtra(EXTRA_ID, a.id)
                .putExtra(EXTRA_KEY, p == null ? null : p.notificationKey)
                .putExtra(EXTRA_TITLE, a.title)
                .putExtra(EXTRA_TEXT, a.text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String key = getIntent().getStringExtra(EXTRA_KEY);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, pad, pad, pad);
        col.addView(Ui.text(this, getIntent().getCharSequenceExtra(EXTRA_TITLE), R.style.TextAppearance_Material3_TitleMedium));
        col.addView(Ui.text(this, getIntent().getCharSequenceExtra(EXTRA_TEXT), R.style.TextAppearance_Material3_BodyMedium));
        EditText input = new EditText(this);
        input.setHint(dev.ayushvarma.aperture.R.string.reply_hint);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setSingleLine(false);
        input.setMaxLines(4);
        col.addView(input);
        Ui.button(col, getString(dev.ayushvarma.aperture.R.string.reply_send), v -> send(key, input.getText()));
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEND) return false;
            send(key, input.getText());
            return true;
        });
        setContentView(col);
        input.requestFocus();
    }

    private void send(String key, CharSequence message) {
        if (message == null || message.toString().trim().isEmpty()) return;
        Notification.Action action = NotificationSource.replyAction(key);
        if (action == null) {
            Toast.makeText(this, dev.ayushvarma.aperture.R.string.reply_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Intent fill = new Intent();
        Bundle results = new Bundle();
        for (RemoteInput ri : action.getRemoteInputs()) {
            if (ri.getAllowFreeFormInput()) results.putCharSequence(ri.getResultKey(), message);
        }
        RemoteInput.addResultsToIntent(action.getRemoteInputs(), fill, results);
        try {
            action.actionIntent.send(this, 0, fill);
        } catch (PendingIntent.CanceledException e) {
            Toast.makeText(this, dev.ayushvarma.aperture.R.string.reply_unavailable, Toast.LENGTH_SHORT).show();
        }
        ApertureApp.of(this).hub().engine().dismissTransient();
        finish();
    }
}
