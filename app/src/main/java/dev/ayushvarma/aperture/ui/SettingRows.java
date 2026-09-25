package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.theme.Fonts;

/** Setting rows shared by the main and Advanced screens; every row writes through and applies live. */
final class SettingRows {
    private final Activity a;
    private final IslandSettings settings;

    SettingRows(Activity a, IslandSettings settings) {
        this.a = a;
        this.settings = settings;
    }

    void label(LinearLayout parent, int text) {
        TextView t = Ui.text(a, a.getString(text), R.style.TextAppearance_Material3_LabelLarge);
        t.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 4));
        parent.addView(t);
    }

    void hint(LinearLayout parent, int text) {
        parent.addView(Ui.text(a, a.getString(text), R.style.TextAppearance_Material3_BodySmall));
    }

    void toggle(LinearLayout parent, int label, String key, boolean value) {
        MaterialSwitch s = new MaterialSwitch(a);
        s.setText(label);
        s.setTypeface(Fonts.regular());
        s.setChecked(value);
        s.setOnCheckedChangeListener((b, checked) -> settings.setBoolean(key, checked));
        parent.addView(s);
    }

    /** A 1-step slider; {@code neverLabel} (or 0) names the 0 position, e.g. "never". */
    void slider(LinearLayout parent, int label, int neverLabel, String key, int value, int from, int to) {
        TextView t = Ui.text(a, text(label, neverLabel, value), R.style.TextAppearance_Material3_LabelLarge);
        t.setPadding(0, Ui.dp(a, 10), 0, 0);
        parent.addView(t);
        Slider s = new Slider(a);
        s.setValueFrom(from);
        s.setValueTo(to);
        s.setStepSize(1);
        s.setValue(Math.max(from, Math.min(to, value)));
        s.addOnChangeListener((sl, v, fromUser) -> {
            settings.setInt(key, Math.round(v));
            t.setText(text(label, neverLabel, Math.round(v)));
        });
        parent.addView(s);
    }

    private String text(int label, int neverLabel, int value) {
        return value == 0 && neverLabel != 0 ? a.getString(neverLabel) : a.getString(label, value);
    }

    CharSequence[] labels(int... ids) {
        CharSequence[] out = new CharSequence[ids.length];
        for (int i = 0; i < ids.length; i++) out[i] = a.getString(ids[i]);
        return out;
    }
}
