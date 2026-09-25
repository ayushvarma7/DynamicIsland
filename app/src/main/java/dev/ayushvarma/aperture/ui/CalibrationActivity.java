package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.slider.Slider;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.core.geometry.Calibration;
import dev.ayushvarma.aperture.settings.IslandSettings;

/** P0.1 manual fine-tune: X/Y/size/gap in 0.5 dp steps plus compact width, applied to the live island. */
public final class CalibrationActivity extends Activity {
    private IslandSettings settings;
    private Slider x;
    private Slider y;
    private Slider size;
    private Slider gap;
    private Slider width;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApertureApp.of(this).settings();
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.calibrate_title), R.style.TextAppearance_Material3_HeadlineMedium));
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.calibrate_hint), R.style.TextAppearance_Material3_BodyMedium));
        LinearLayout card = Ui.card(page);
        Calibration c = settings.calibration();
        x = slider(card, dev.ayushvarma.aperture.R.string.calibrate_x, -10, 10, 0.5f, c.offsetXDp);
        y = slider(card, dev.ayushvarma.aperture.R.string.calibrate_y, -10, 10, 0.5f, c.offsetYDp);
        size = slider(card, dev.ayushvarma.aperture.R.string.calibrate_size, -6, 6, 0.5f, c.sizeDp);
        gap = slider(card, dev.ayushvarma.aperture.R.string.calibrate_gap, 0, 8, 0.5f, c.gapDp);
        width = slider(card, dev.ayushvarma.aperture.R.string.calibrate_width, 60, 130, 5, c.compactWidthScale * 100);
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.calibrate_reset), v -> {
            settings.resetCalibration();
            Calibration d = Calibration.DEFAULT;
            x.setValue(d.offsetXDp);
            y.setValue(d.offsetYDp);
            size.setValue(d.sizeDp);
            gap.setValue(d.gapDp);
            width.setValue(d.compactWidthScale * 100);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings.setCalibrationPreview(true);
    }

    @Override
    protected void onPause() {
        settings.setCalibrationPreview(false);
        super.onPause();
    }

    private Slider slider(LinearLayout parent, int label, float from, float to, float step, float value) {
        TextView t = Ui.text(this, "", R.style.TextAppearance_Material3_LabelLarge);
        parent.addView(t);
        Slider s = new Slider(this);
        s.setValueFrom(from);
        s.setValueTo(to);
        s.setStepSize(step);
        s.setValue(Math.max(from, Math.min(to, Math.round(value / step) * step)));
        parent.addView(s);
        Runnable update = () -> {
            if (label == dev.ayushvarma.aperture.R.string.calibrate_width) t.setText(getString(label, Math.round(s.getValue())));
            else t.setText(getString(label, s.getValue()));
        };
        update.run();
        s.addOnChangeListener((sl, v, fromUser) -> {
            update.run();
            if (fromUser) save();
        });
        return s;
    }

    private void save() {
        settings.setCalibration(new Calibration(x.getValue(), y.getValue(), size.getValue(), gap.getValue(),
                width.getValue() / 100f));
    }
}
