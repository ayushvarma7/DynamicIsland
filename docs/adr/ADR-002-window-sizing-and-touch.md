# ADR-002: Window sizing and touch pass-through (Gate D2)

- Status: Draft (Phase 0). Final in Phase 1 (pass-through on the Pixel 8), with frame and power numbers added in Phase 2.
- Date: 2026-09-25
- Question: how do touches outside the island reach the app underneath?

## What changed in Phase 0

The spec describes option B as "internal API". **That is false on our minSdk.** `AttachedSurfaceControl.setTouchableRegion(Region)` is public since API 33, reached through `View.getRootSurfaceControl()`. The SDK 36 Javadoc says it allows "pass-through in others" (areas outside the region). So B is a supported option, and a third option combining both is worth measuring.

## Options

| | Option | Idle cost | Mid-animation behaviour |
|---|---|---|---|
| A | Resize the window only at state boundaries (grow before expand, shrink after collapse) | Tiny window (about 88 × 88 px on Pixel 8) | Window is at its maximum size while the card grows, so taps in the not-yet-covered area are swallowed for about 300 ms |
| B | One fixed maximum window plus a public touchable region that follows the island's bounds | A 1080 × ~700 px mostly transparent layer, always composited | No relayouts ever; the region updates per state (or per frame if needed) |
| C | A at idle/compact, B during an expanded session (grow once, then track the region until collapse completes) | Same as A | Same as B |

## Measurements so far (emulator API 36.1, 50 taps outside + 10 inside per mode)

| Mode | Outside taps that reached the app below | Inside taps the island caught |
|---|---|---|
| A (window hugs the ring) | 50/50 | 10/10 |
| B (fixed window + `setTouchableRegion`) | **50/50** | 10/10 |
| Control: fixed window, no region | 0/50 | 60/60 |

Reproduce with `spikes/phase0-probe/touch_test.sh <serial>`.

Still needed on Pixel 8: the same 50/50 test; P95 frame time for expand/collapse under A, B and C at 120 Hz; SurfaceFlinger composition strategy with B's large layer while another app scrolls (`dumpsys SurfaceFlinger` client vs device composition); 24 h battery delta.

## Provisional decision

**C.** Pass-through ties, so by the tie rule this defaults to A. C is A plus the region during the only phase where A swallows taps. B is rejected unless the Pixel 8 shows no composition or power cost from the always-on large layer.

## Consequences

- The renderer must expose the island's current outer bounds (plus 6 dp slop, clipped away from status-bar icon zones) as a `Region` at every state change.
- Region updates call `requestLayout` internally. Push them at state boundaries, not per frame, unless Phase 2 profiling says per-frame is free.
