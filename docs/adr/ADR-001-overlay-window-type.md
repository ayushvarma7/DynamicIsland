# ADR-001: Overlay window type (Gate D1)

- Status: Draft (Phase 0). Becomes final in Phase 1 after the Pixel 8 ring check.
- Date: 2026-09-25
- Question: how do we draw over the status bar and around the camera cutout?

## Options

| | Option | Notes |
|---|---|---|
| A | `TYPE_ACCESSIBILITY_OVERLAY` added from our `AccessibilityService` | Needs accessibility permission. Blocked while Android 17 Advanced Protection is on. |
| B | `TYPE_APPLICATION_OVERLAY` with `SYSTEM_ALERT_WINDOW` | Normal overlay permission. |

## Measurements

| Check | A | B | Evidence |
|---|---|---|---|
| Draws above the status bar | Yes: the debug ring renders over status-bar icons and over the system Live Update chip | **No**: status-bar icons stay crisp on top of a 50% red overlay | Emulator API 36.1 screenshots, `spikes/phase0-probe` `cmd app_overlay` (screenshots kept in the session scratchpad, reproducible with the probe) |
| Draws above system heads-up and dialogs | Yes: stays above the HUN card and the MediaOutputDialog dim | n/a | Emulator screenshots |
| Trusted overlay for Android 12+ untrusted-touch blocking | Yes: `TYPE_ACCESSIBILITY_OVERLAY` is listed as trusted | No: `TYPE_APPLICATION_OVERLAY` is untrusted, max obscuring opacity 0.8 | SDK 36 `WindowManager.java` `FLAG_NOT_TOUCHABLE` Javadoc (lines ~2800-2857) |
| Foreground package without window content | Yes: `typeWindowStateChanged` gives package + class with `canRetrieveWindowContent="false"`; `getSource()` is null | n/a | Emulator log `a11y_window_state` (camera app, launcher, SystemUI dialog) |
| Works from a sideloaded build | Yes (granted via adb on the emulator). On the phone the user grants it in Settings | Yes | Probe |
| Pixel 8 (Android 17) | Pending: needs the user to enable the probe's accessibility service | n/a | Phase 1 |

## Decision

A, the listed default. Option B cannot reach the status bar, so it cannot be an island.

## Consequences

- Android 17 Advanced Protection revokes our accessibility grant and blocks re-granting it. That's been true since 17 stable, for the whole package whenever any service in it isn't `isAccessibilityTool`, with no per-app override. Onboarding and the health screen must detect it and explain it honestly (rule S3). Use `AdvancedProtectionManager.isAdvancedProtectionEnabled()` plus its callback (API 36, `QUERY_ADVANCED_PROTECTION_MODE` is a normal permission); it's callable on the Pixel 8 and currently `false`. After AAPM is switched off, the user has to re-enable the service by hand, so the health screen should offer the deep link at that moment.
- **Force-stopping the app removes its service from `enabled_accessibility_services`** (measured). It does not come back by itself. The health screen must detect this and deep-link to Accessibility settings.
- Every window must be added through the service's own `WindowManager`. **Any window added through that `WindowManager` is attached to the accessibility token and inherits its layer, even a `TYPE_APPLICATION_OVERLAY`** (measured). That's fine for the island, but it matters if we ever add a secondary non-accessibility window: use a separate `WindowContext` for it.
- `AccessibilityService` is not a UI context (`isUiContext() == false`), but `WindowManager.getCurrentWindowMetrics()` still works from it (measured).
