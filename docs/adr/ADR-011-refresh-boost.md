# ADR-011: 120 Hz boost for event-triggered animations on Pixel 8 (Gate D11)

- Status: Draft (Phase 0). The harness is built; the Pixel 8 run is pending.
- Date: 2026-09-25

## Problem

Pixel 8 is LTPS with two modes. Smooth Display lets it reach 120 Hz, but it drops to 60 Hz when idle. Touch-driven animations get the touch boost; event-driven ones (a notification arrives, a timer ends) may render at 60 Hz.

## Options

| | Option |
|---|---|
| A | Request a high rate on the overlay only while animating. Candidates: `LayoutParams.preferredRefreshRate`, `LayoutParams.preferredDisplayModeId`, API 35 `View.setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_HIGH)` or an explicit rate |
| B | No boost |

The criterion: A only if event-triggered animations measure at 120 Hz on the Pixel 8 and the battery cost is under 0.1% per day.

## Measured so far (Pixel 8)

| Item | Value |
|---|---|
| Display modes | id 1 = 1080 × 2400 at 60 Hz, id 2 = 1080 × 2400 at 120 Hz (same group, seamless) |
| Smooth Display | On: `peak_refresh_rate = Infinity`, `min_refresh_rate = 0.0` (readable); DisplayModeDirector user vote max 120 |
| Current mode at probe time | 120 Hz (the probe had just launched; that's the touch/launch boost) |
| `Display.hasArrSupport()` | false |
| `Display.getSuggestedFrameRate(NORMAL/HIGH)` | 60 / 90 |
| DisplayModeDirector | `mDefaultPeakRefreshRate` 60, brightness and thermal observers allow 120 in both zones |

SDK 36 source reading: `View.setRequestedFrameRate` votes are written to the window's own surface by `ViewRootImpl` with no window-type check, so they reach an accessibility overlay's surface. `preferredRefreshRate` is documented as equivalent to `Surface.setFrameRate(..., CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS)`. The framework touch boost only applies to the window that handled the touch, and it expires 3 s after the gesture ends. Whether WindowManager's RefreshRatePolicy honours `preferredDisplayModeId` for a non-focused overlay is unconfirmed without the device run.

## Test harness (ready)

`adb shell am broadcast -a dev.aperture.spike.probe.CMD --es cmd anim_test --es mode none|lp_rate|lp_mode|view_high|view_hz` animates the overlay for 2 s with no touch. It logs the Choreographer vsync interval (mean, p50, p95, share under 10 ms) and the display rate before, during and after.

## Provisional decision

A via `View.setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_HIGH)` set only for the life of an animation, if the run shows about 8.3 ms intervals. Otherwise try `preferredDisplayModeId`, else B. The battery criterion is measured in Phase 7.
