package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import dev.ayushvarma.aperture.theme.Fonts;

/** Small builders so the settings screens stay readable in plain Java. */
final class Ui {
    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Edge-to-edge scrolling page (target SDK 37 is always edge-to-edge). */
    static LinearLayout page(Activity a) {
        ScrollView scroll = new ScrollView(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(a, 16);
        col.setPadding(pad, pad, pad, pad);
        scroll.addView(col);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        a.setContentView(scroll);
        return col;
    }

    static TextView text(Context c, CharSequence s, int appearance) {
        TextView t = new TextView(c);
        t.setTextAppearance(appearance);
        t.setText(s);
        // Google Sans (the Pixel system font) instead of the Material default Roboto.
        boolean strong = appearance == com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
                || appearance == com.google.android.material.R.style.TextAppearance_Material3_TitleSmall
                || appearance == com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
                || appearance == com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium;
        t.setTypeface(strong ? Fonts.medium() : Fonts.regular());
        return t;
    }

    static TextView title(LinearLayout parent, CharSequence s) {
        TextView t = text(parent.getContext(), s, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        t.setPadding(0, dp(parent.getContext(), 20), 0, dp(parent.getContext(), 8));
        parent.addView(t);
        return t;
    }

    static LinearLayout card(LinearLayout parent) {
        Context c = parent.getContext();
        MaterialCardView card = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        card.setLayoutParams(lp);
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 16);
        inner.setPadding(pad, pad, pad, pad);
        card.addView(inner);
        parent.addView(card);
        return inner;
    }

    static MaterialButton button(LinearLayout parent, CharSequence label, View.OnClickListener l) {
        MaterialButton b = new MaterialButton(parent.getContext());
        b.setText(label);
        b.setTypeface(Fonts.medium());
        b.setOnClickListener(l);
        parent.addView(b);
        return b;
    }

    static MaterialButtonToggleGroup toggles(LinearLayout parent, int checked, CharSequence[] labels,
                                              OnChoice listener) {
        Context c = parent.getContext();
        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(c);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        int[] ids = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            MaterialButton b = new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(labels[i]);
            b.setTypeface(Fonts.medium());
            b.setMaxLines(1);
            ids[i] = View.generateViewId();
            b.setId(ids[i]);
            group.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        group.check(ids[checked]);
        group.addOnButtonCheckedListener((g, id, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < ids.length; i++) if (ids[i] == id) listener.onChoice(i);
        });
        group.setGravity(Gravity.CENTER);
        parent.addView(group, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return group;
    }

    interface OnChoice {
        void onChoice(int index);
    }
}
