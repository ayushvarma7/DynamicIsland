# ADR-005: Music visualizer (Gate D5)

- Status: Draft (Phase 0). Measured when the P2 visualizer is built.
- Date: 2026-09-25

## Options

| | Option |
|---|---|
| A | Procedural "rhythm" bars driven by playback state (playing/paused, speed), with no audio access |
| B | `android.media.audiofx.Visualizer` on session 0 (output mix), opt-in, needs `RECORD_AUDIO` + `MODIFY_AUDIO_SETTINGS` |

## Criteria

B must produce non-zero FFT within 2 s on Bluetooth, on the speaker, and with Pixel Adaptive Sound on. Otherwise fall back to A automatically and show a hint.

## Phase 0 evidence

- SDK 36 `Visualizer.java`: session 0 is still supported and needs `MODIFY_AUDIO_SETTINGS` on top of `RECORD_AUDIO`. There's no deprecation note (only equalizer, bass boost and virtualizer on session 0 are deprecated, in `AudioEffect.java`).
- Competitor evidence (Play reviews and the developer's XDA posts, 2025–2026): dynamicSpot's session-0 visualizer goes flat with Bluetooth or AirPods, with Libby, and with Pixel Adaptive Sound. Its developer confirms Adaptive Sound breaks it. That's exactly the frozen-bars failure Section 15 forbids.
- Pixel 8 audio state measured: 4 outputs, spatializer unavailable on the built-in speaker.

## Provisional decision

A by default, B opt-in behind a runtime test: after 2 s of playback, if every FFT frame is zero, switch to A and say why in Settings. This needs a Pixel 8 run with Bluetooth, speaker and Adaptive Sound in P2.
