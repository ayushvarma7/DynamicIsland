package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.device.DeviceReport;

/** Shows the report and shares it as text through the share sheet (local only, Section 9.6). */
public final class DeviceReportActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.report_title), R.style.TextAppearance_Material3_HeadlineMedium));
        String report = DeviceReport.build(this, ApertureApp.of(this).settings());
        TextView body = Ui.text(this, report, R.style.TextAppearance_Material3_BodySmall);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        Ui.card(page).addView(body);
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.report_share), v -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report), null)));
    }
}
