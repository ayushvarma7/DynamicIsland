# ADR-013: Where Material 3 Expressive comes from (new gate D13, ASK)

- Status: Draft (Phase 0). **ASK**: it conflicts with rule 4's "current stable versions".
- Date: 2026-09-25

## Problem

Section 7.3 requires `MotionScheme.expressive()` / `standard()` tokens ("never hardcode durations"). Section 3.3.7 and rule 7.3.7 rely on `MaterialShapes` and shape morphing. Checked at the source on 2026-09-25:

| Artifact | Latest stable | Has the Expressive APIs? |
|---|---|---|
| `androidx.compose.material3:material3` | **1.4.0** (BOM 2026.09.00) | **No.** 1.4.0-beta01 removed every `@ExperimentalMaterial3ExpressiveApi` API |
| same | 1.5.0-alpha29 (2026-09-23) | `MotionScheme.expressive()` / `standard()` promoted out of experimental in alpha15; `MaterialExpressiveTheme` in alpha18. `MaterialShapes` and `LoadingIndicator` are still experimental |
| `androidx.graphics:graphics-shapes` | 1.1.0 | `RoundedPolygon`, `Morph` (the primitives `MaterialShapes` is built on) |

Sources: Google Maven metadata for material3 and compose-bom, fetched 2026-09-25; the material3 release notes via the library research.

## Options

| | Option | For | Against |
|---|---|---|---|
| A | Pin the exact alpha `material3:1.5.0-alpha29` in `:core:theme` only (and the `:app` settings UI), with an upgrade policy: bump alphas deliberately and move to 1.5.0 stable when it ships | Real Google motion and shape tokens, identical to what Pixel SystemUI is moving to. Satisfies "use the scheme's tokens" | Alpha API churn; breaks the letter of rule 4 |
| B | Stable 1.4.0 + graphics-shapes 1.1.0, with an in-repo motion token table transcribed from the published M3 Expressive spring spec and in-repo `RoundedPolygon` shape definitions | All stable | We would be hardcoding tokens, which Section 7.3 forbids and principle 1 discourages |

## Recommendation

A, isolated so that only `:core:theme` imports Expressive symbols, and the rest of the app sees our own `IslandMotion` / `IslandShapes` interfaces. If you'd rather stay stable-only, B works with the same interfaces, and swapping to the official tokens later is a one-module change.
