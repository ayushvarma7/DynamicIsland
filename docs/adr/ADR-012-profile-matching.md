# ADR-012: Device profile matching (Gate D12)

- Status: Draft (Phase 0). Final in Phase 1 after the `shiba` ring check.
- Date: 2026-09-25

## Options

| | Option |
|---|---|
| A | `Build.DEVICE` codename lookup in `assets/device_profiles.json` plus live measured geometry. The profile only adds corrections and never overrides a measurement |
| B | Measured geometry only |

## Phase 0 evidence

- The live measurement is precise enough to be the authority. On the Pixel 8, `DisplayCutout.getCutoutPath()` from an app gives exactly the device overlay's spec `m 576,65.75 a 36.25,36.25 … @left` (px, left-anchored): center (539.75, 65.75), r 36.25 px. That's 0.25 px left of the geometric screen center, a real hardware offset, not an error to correct.
- Measurement can go wrong at runtime: on the emulator, after switching the cutout configuration, apps got **no cutout at all** while the display device still reported one. The profile's `cutoutKind` gives a sanity check ("`shiba` should have a centered circle; if the live probe says none, re-probe before degrading to G3").
- Quirks that measurement can't see: the gap preference, curved edges, whether a refresh boost is worth it.

## Provisional decision

A. The `shiba` entry, pending the Phase 1 on-device ring check before `verified: true`:

```json
"shiba": { "name": "Pixel 8", "tier": "primary", "verified": false,
  "cutoutKind": "center", "ringGapDp": 3.0, "cutoutOffsetPx": [0, 0],
  "curvedEdges": false, "refreshBoost": true,
  "notes": "LTPS 60/120 Hz (modes 1/2); cutout r=36.25px @ (539.75,65.75); top corner r=104px; status bar 132px" }
```

The `notes` field is documentation only. Geometry always comes from the live probe (rule: never geometry in profiles).

Seed codenames, all verified in Phase 0 (RESEARCH V21): oriole, raven, bluejay, panther, cheetah, lynx, felix, tangorpro, shiba, husky, akita, tokay, caiman, komodo, comet, tegu, frankel, blazer, mustang, rango, plus **stallion** (10a), **cubs** (11), **grizzly** (11 Pro), **kodiak** (11 Pro XL), **yogi** (11 Pro Fold). All but `shiba` start as tier `supported`, `verified: false`.
