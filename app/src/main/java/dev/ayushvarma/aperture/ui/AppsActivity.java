package dev.ayushvarma.aperture.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.R;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.ayushvarma.aperture.ApertureApp;
import dev.ayushvarma.aperture.settings.IslandSettings;
import dev.ayushvarma.aperture.theme.Fonts;

/** Setup step 3: choose which apps may use the island. Suggested apps come first and start switched on. */
public final class AppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IslandSettings settings = ApertureApp.of(this).settings();
        LinearLayout page = Ui.page(this);
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.apps_title), R.style.TextAppearance_Material3_HeadlineMedium));
        page.addView(Ui.text(this, getString(dev.ayushvarma.aperture.R.string.apps_hint), R.style.TextAppearance_Material3_BodyMedium));
        Ui.button(page, getString(dev.ayushvarma.aperture.R.string.apps_done), v -> finish());

        PackageManager pm = getPackageManager();
        List<ResolveInfo> launchable = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        Set<String> seen = new HashSet<>();
        List<ResolveInfo> suggested = new ArrayList<>();
        List<ResolveInfo> others = new ArrayList<>();
        for (ResolveInfo ri : launchable) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            (IslandSettings.SUGGESTED_APPS.contains(pkg) ? suggested : others).add(ri);
        }
        java.util.Comparator<ResolveInfo> byLabel = (a, b) -> a.loadLabel(pm).toString().toLowerCase(Locale.ROOT)
                .compareTo(b.loadLabel(pm).toString().toLowerCase(Locale.ROOT));
        suggested.sort(byLabel);
        others.sort(byLabel);
        Set<String> allowed = settings.allowedApps();
        if (!suggested.isEmpty()) {
            Ui.title(page, getString(dev.ayushvarma.aperture.R.string.apps_suggested));
            LinearLayout card = Ui.card(page);
            for (ResolveInfo ri : suggested) row(card, ri, pm, allowed, settings);
        }
        Ui.title(page, getString(dev.ayushvarma.aperture.R.string.apps_all));
        LinearLayout card = Ui.card(page);
        for (ResolveInfo ri : others) row(card, ri, pm, allowed, settings);
    }

    @Override
    public void finish() {
        ApertureApp.of(this).settings().setAppsChosen();
        super.finish();
    }

    private void row(LinearLayout parent, ResolveInfo ri, PackageManager pm, Set<String> allowed,
                     IslandSettings settings) {
        String pkg = ri.activityInfo.packageName;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Ui.dp(this, 6);
        row.setPadding(0, pad, 0, pad);
        ImageView icon = new ImageView(this);
        Drawable d = ri.loadIcon(pm);
        icon.setImageDrawable(d);
        int size = Ui.dp(this, 36);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
        ilp.setMarginEnd(Ui.dp(this, 14));
        row.addView(icon, ilp);
        MaterialSwitch sw = new MaterialSwitch(this);
        sw.setText(ri.loadLabel(pm));
        sw.setTypeface(Fonts.regular());
        sw.setChecked(allowed.contains(pkg));
        sw.setOnCheckedChangeListener((b, checked) -> settings.setAllowed(pkg, checked));
        row.addView(sw, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
    }
}
