# Architecture decision records

One per Section 10 gate (plus D13, found in Phase 0). Every record lists options, measurements, choice and reason; on a tie the gate's listed default wins.

| ADR | Gate | Status | Choice so far | Waiting on |
|---|---|---|---|---|
| [001](ADR-001-overlay-window-type.md) | D1 overlay type | Draft | A: accessibility overlay | Pixel 8 ring check (Phase 1) |
| [002](ADR-002-window-sizing-and-touch.md) | D2 window sizing | Draft | C: resize at idle, public touchable region while expanded | Pixel 8 pass-through, frame and power numbers |
| [003](ADR-003-renderer.md) | D3 renderer | Draft | A: Compose | Phase 2 jank and RSS |
| [004](ADR-004-status-bar-collision.md) | D4 status bar collision | Draft | C: public/readable signals + slider + QPR2 tip | Live Update chip position on Pixel 8 |
| [005](ADR-005-visualizer.md) | D5 visualizer | Draft | A default, B opt-in with auto-fallback | P2 device run |
| [006](ADR-006-font.md) | D6 font | Draft | A: `google-sans-flex` (resolves on Pixel 8) | `tnum` check on device |
| [007](ADR-007-heads-up-duplication.md) | D7 heads-up duplication | Draft | A in P0, B in P2 with warning | Android 17 check (P2) |
| [008](ADR-008-distribution.md) | D8 distribution | Draft | personal-sideload, Play-compliant | Restricted-settings behaviour on your phone |
| [009](ADR-009-min-sdk.md) | D9 SDK levels | Draft | minSdk 34, compile/target 37 | Nothing |
| [010](ADR-010-expanded-body.md) | D10 expanded body | **ASK** | Black (default) | Your pick from Phase 2 screenshots |
| [011](ADR-011-refresh-boost.md) | D11 refresh boost | Draft | A via `setRequestedFrameRate(HIGH)` if it measures | Pixel 8 cadence run |
| [012](ADR-012-profile-matching.md) | D12 profile matching | Draft | A: codename + measured geometry | Phase 1 `shiba` verification |
| [013](ADR-013-material3-expressive-dependency.md) | D13 (new) M3 Expressive source | Superseded by 014 | Compose-only APIs; not usable from Java | Nothing |
| [014](ADR-014-java-and-views.md) | Language and UI toolkit | **Accepted** | Java + Android Views + dynamicanimation springs | Nothing |
