# ADR-010: Expanded body default, Black vs Tonal (Gate D10, ASK)

- Status: Draft (Phase 0). **ASK**: needs screenshots of both on your wallpaper, which requires the renderer (Phase 2). Default until then: Black.
- Date: 2026-09-25

## Options

Black (`#000000`) or Tonal (dark `surfaceContainerHigh`). Tinted and Follow light/dark remain user options regardless.

## Phase 0 input: what these resolve to on your phone today

Your theme is **Monochromatic** (`theme_style=MONOCHROMATIC`, preset palette `#333333`, dark mode on), read from `theme_customization_overlay_packages`.

| Token (dark) | Value on your Pixel 8 |
|---|---|
| `primary` | `#FFFFFF` |
| `onPrimary` | `#1B1B1B` |
| `primaryContainer` | `#D4D4D4` |
| `surfaceContainerHigh` | `#2A2A2A` |
| `surfaceContainerHighest` | `#353535` |
| `onSurface` / `onSurfaceVariant` | `#E2E2E2` / `#C6C6C6` |
| `error` | `#FFB4AB` |

So with your theme, Tonal is a neutral dark grey (`#2A2A2A`), and the Black body's keyline (`primary` at 32%) is a light grey ring. In Monochromatic, "Tinted" (8% primary over `surfaceContainerHigh`) is barely different from Tonal. On a wallpaper-derived or colourful style the three differ much more, so the screenshots should be taken on your actual setup.

## Decision

Not yet. Black stays the default until you pick from the Phase 2 screenshots.
