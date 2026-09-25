# ADR-006: Typeface (Gate D6)

- Status: Draft (Phase 0). The resolution check is done; the tabular-figure check is pending.
- Date: 2026-09-25

## Options

| | Option |
|---|---|
| A | Pixel system families (`google-sans`, `google-sans-text`, `google-sans-flex`) via `Typeface.create` |
| B | Bundle Roboto Flex |

The criterion: A only if the typeface resolves to a non-default family on the Pixel 8.

## Measurements (Pixel 8, Android 17, third-party app context)

| Family | Resolves? | `getSystemFontFamilyName()` | Weight |
|---|---|---|---|
| `google-sans` | Yes | `google-sans` | 400 |
| `google-sans-text` | Yes | `google-sans-text` | 400 |
| `google-sans-medium` | Yes | `google-sans-medium` | 500 |
| `google-sans-bold` | Yes | `google-sans-bold` | 700 |
| `google-sans-flex` | Yes | `google-sans-flex` | 400 |
| `google-sans-clock` | Yes | `google-sans-clock` | 400 |
| `google-sans-display` | No (falls back to `sans-serif`) | | |
| `roboto-flex` | Yes (system font) | `roboto-flex` | 400 |

- Font files present: `/product/fonts/GoogleSans-Regular.ttf`, `GoogleSans-Italic.ttf`, `GoogleSansFlex-Regular.ttf`, `GoogleSansClock-Regular.ttf`, `GoogleSansFlexClock-Regular.ttf`.
- `/product/etc/fonts_customization.xml` is readable by apps. It also defines the Material 3 Expressive role families `variable-body-*`, `variable-display-*` (and likely the rest of the scale), which map type roles onto Google Sans Flex on Pixel.
- Emulator API 36.1 (Google Play image): no Google Sans at all, so every `google-sans*` family falls back to default. That's the generic-device path.

Pending (probe build ready, the phone disconnected before it ran): digit advance widths with and without `'tnum'`, and whether `wght`, `ROND` and `wdth` are accepted, for each family.

## Provisional decision

**A**: `google-sans-flex` for the island (variable weight for the number roll), with `google-sans-text` for body text in Settings. Fallback chain when a family doesn't resolve: `roboto-flex` (a system font on both images tested), then the default. If the `tnum` check fails for Flex, use `google-sans-text` for numerals.

Option B improves on the spec: Google Sans and Google Sans Flex are OFL-licensed on Google Fonts (Flex since 2025-11-18). So the generic-tier fallback can bundle Google Sans Flex instead of Roboto Flex, keeping one typeface across devices. The cost is APK size; measure it in Phase 5 before committing.
