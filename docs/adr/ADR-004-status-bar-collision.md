# ADR-004: Status bar collision budget (Gate D4)

- Status: Draft (Phase 0). Final in Phase 5 (placement guards).
- Date: 2026-09-25
- Question: how wide can compact be without covering the clock, status icons, privacy dot or recording chip?

## Options

| | Option |
|---|---|
| A | Slider-only budget with a safe default |
| B | Read SystemUI bounds from window info. Allowed only if it works without window content retrieval |
| C (new) | Public and readable signals, no content retrieval: `WindowInsets.getPrivacyIndicatorBounds()` (public, API 31), SystemUI's own dimension resources (readable through `getResourcesForApplication("com.android.systemui")` with a `<queries>` entry), and known system state that predicts right-side icons (ringer mode, DND, Bluetooth, next alarm, `status_bar_show_battery_percent`, `icon_blacklist`, all readable) |

## Pixel 8 measurements (Android 17, CP2A.260805.005, 1080 × 2400, density 2.625)

| Item | Value (px) | Source |
|---|---|---|
| Status bar height / top inset | 132 | `WindowInsets`, `dimen/status_bar_height` |
| Status content horizontal inset | 70 (`rounded_corner_content_padding`) + 11 (`status_bar_padding_start/end`), so content spans x 81–999 | SystemUI resources |
| Cutout rect SystemUI splits around | x 478.5–600.5 | `config_mainBuiltInDisplayCutoutRectApproximation` |
| Left status region / right status region | 81–478.5 / 600.5–999 | Derived |
| Privacy indicator zone | x 810–1026, y 0–132 | `WindowInsets.getPrivacyIndicatorBounds()` |
| Default compact island (Section 7.2 formula) | x 371.6–707.9 (336 px wide) | Derived: Hc = 88.25, Lw = Tw = 124 |
| Overlap of default compact with status regions | 107 px on each side | Derived |

So the default compact width **does** reach into status-bar space on the Pixel 8. The left side is free unless 3 or more notification icons follow the clock. The right side collides once the vibrate, alarm, Bluetooth or DND icons join Wi-Fi, cell and battery. Your phone is in vibrate mode right now.

A second, new collision source: **SystemUI's own Live Update chip.** It appears at the start (left) of the status bar whenever a promoted notification is showing (emulator: x 158–332; where it sits relative to the clock on the Pixel 8 still needs measuring). Its width varies with its text, and there is no API to read it.

## Provisional decision

C, with A as the user override and the QPR2 recommendation:

1. The default trailing width is computed so the island ends before the first predicted right-side icon, and never enters `getPrivacyIndicatorBounds()`.
2. The default leading width assumes the clock plus two notification icons. If a promoted Live Update is active, render it in the island's leading slot and accept that the system chip shows too; note the duplication in Settings.
3. A slider (Section 4, principle 6) always wins.
4. Onboarding card (rule S2): on QPR2 builds, recommend status bar notification icons "None" or "Priority". Your build is 17.0, which doesn't have these settings yet.

Option B as written (reading SystemUI window bounds) is rejected: `WindowInsets.getBoundingRects(statusBars())` returns the whole bar (0–1080), which is useless for this, and anything finer needs window content.
