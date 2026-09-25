# Phase 0 probe (throwaway spike)

Not product code. It measures the facts `docs/RESEARCH.md` and the ADR drafts depend on, on the emulator and on the Pixel 8. The product rewrites the useful parts properly (`:core:device`, `:overlay`, `:lab`); delete this directory once Phase 1 has equivalents.

It is deliberately zero-dependency Java against the AGP 8.13.2 already in the local Gradle cache, so it builds offline and never touches the product's version pins.

## Build

```bash
cd spikes/phase0-probe
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew --offline assembleDebug
```

## What it contains

| Component | Measures |
|---|---|
| `ProbeActivity` | Every Section 9.2 value from a normal app context (`DeviceProbe`), written to `files/probe_activity.json`. Also a full-screen touch target, a ProgressStyle Live Update poster, a heads-up poster, and the output-switcher checks |
| `ProbeA11yService` | The overlay: `TYPE_ACCESSIBILITY_OVERLAY` ring at the measured cutout; window modes for Gate D2; theme, contrast, config, thermal, power, camera, mic and display watchers from a long-lived context; the Gate D11 animation cadence test |
| `ProbeListener` | What a notification listener can read: promoted flag, ProgressStyle via `recoverBuilder`, extras keys, ranking, media sessions. Other apps' text is logged as length only |
| `touch_test.sh` | 50 outside + 10 inside taps per overlay mode |

Everything logs to logcat tag `APERTURE_PROBE` as `event {json}`.

## Commands

```bash
adb shell am start -n dev.aperture.spike.probe/.ProbeActivity --es action probe --ez finish true
adb exec-out run-as dev.aperture.spike.probe cat files/probe_activity.json > probe.json
adb shell am start -n dev.aperture.spike.probe/.ProbeActivity --es action post_live_update --ei progress 55
adb shell am broadcast -a dev.aperture.spike.probe.CMD --es cmd overlay --es mode resize   # resize|fixed|fixed_noregion|off
adb shell am broadcast -a dev.aperture.spike.probe.CMD --es cmd anim_test --es mode view_high   # none|lp_rate|lp_mode|view_high|view_hz
adb shell am broadcast -a dev.aperture.spike.probe.CMD --es cmd probe    # service-context probe -> files/probe_service.json
adb logcat -s APERTURE_PROBE
```

## Granting on a real phone (you do this, not a script)

1. Notifications: run `--es action request_post_notifications` and tap Allow.
2. Notification access: Settings > Notifications > Device & app notifications > Notification read, reply & control > Aperture Probe listener.
3. Accessibility: Settings > Accessibility > Aperture Probe overlay. If Android says the setting is restricted: Settings > Apps > Aperture Probe (Phase 0) > ⋮ > Allow restricted settings, then try again.

## Remove

```bash
adb uninstall dev.aperture.spike.probe
```

Uninstalling also removes its accessibility and listener grants.

## Emulator notes

- The Studio "Medium Phone API 36.1" AVD has animator scales set to 0, so rule A1 (crossfades only) fires there.
- Switching `com.android.internal.display.cutout.emulation.*` overlays at runtime can leave the logical display with **no cutout** while the display device still reports one. Every app then measures "no cutout". Reboot the AVD after switching emulation overlays.
- The emulator does not paint emulated cutouts black, so ring alignment has to be judged on real hardware.
