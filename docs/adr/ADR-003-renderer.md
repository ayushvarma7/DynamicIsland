# ADR-003: Renderer (Gate D3)

- Status: Draft (Phase 0). Measured in Phase 2.
- Date: 2026-09-25
- Question: Compose everywhere, or a custom View container hosting Compose content?

## Options

| | Option |
|---|---|
| A | `ComposeView` as the overlay root, with lifecycle, saved-state and ViewModel owners supplied by the service |
| B | A custom `View` container that draws the island body (shape, keyline, metaball) on Canvas, with Compose only for content |

## Criteria

Zero janky frames over 100 expand/collapse cycles (JankStats + Macrobenchmark), P95 frame time under the R1/R2 budget (8.3 ms at 120 Hz on Pixel 8), idle RSS under 60 MB (the Section 12 budget is 45 MB).

## Phase 0 findings that constrain this

- Compose UI 1.12.x (BOM 2026.09.00) **requires compileSdk 37 and AGP ≥ 9.2** (release notes). This is fine for us: target 37, AGP 9.4.1.
- The Material 3 Expressive APIs the motion spec relies on are not in stable material3 (see ADR-013). That affects which Compose artifacts we pull, not A vs B.
- Measured reference: the zero-dependency Java probe's overlay costs nothing notable. It isn't a Compose number; Phase 2 must measure A directly.

## Provisional decision

A, the listed default. Switch to B only if A misses the frame or RSS budget on the Pixel 8.
