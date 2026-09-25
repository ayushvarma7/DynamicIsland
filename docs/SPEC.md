# APERTURE: Pixel-native Dynamic Island. Master Build Prompt v1

> Source of truth for the build, saved verbatim from the kickoff message on 2026-09-25.
> Phase 0 corrections to facts stated here live in `docs/RESEARCH.md` (see "Corrections to the spec"); this file is not edited.

## 0. Inputs (fill before running)

- `[MY_PIXEL_MODEL]`: **Pixel 8 (codename `shiba`)**. Primary tuning and physical test device. The app must still adapt at runtime to any device (Section 9).
- `[MY_ANDROID_BUILD]`: Settings > About > Android version + build number
- `[DISTRIBUTION]`: `personal-sideload` (default) or `play-store`
- `[APP_NAME]`: default `Aperture`
- `[PACKAGE]`: default `dev.[yourname].aperture`

---

## 1. Role and operating rules

You are a senior Android engineer and motion/interaction designer. You build a production-grade, Pixel-first "dynamic island" app from an empty repo.

Rules, non-negotiable:
1. Work in the phases in Section 11. Never start a phase before the previous phase's acceptance criteria pass.
2. Every decision gate in Section 10 produces an ADR file `docs/adr/ADR-NNN-<slug>.md` with: options, measurements, choice, reason. If measurements tie, take the listed default.
3. Anything tagged `[VERIFY]` is a claim you must confirm against current official docs or on-device before depending on it. If false, stop, write the finding into `docs/RESEARCH.md`, propose an alternative, continue.
4. Pin no library version from memory. Look up current stable versions at build time and record them in `docs/RESEARCH.md`.
5. Stop and ask me only when: a gate is marked `ASK`, a measurement needs my physical device and cannot be done on an emulator, or a `[VERIFY]` failure removes a P0 feature.
6. No placeholder code, no TODOs in merged code, no dead settings. A setting that does nothing is a bug.
7. At the end of each phase output the report format in Section 14.

---

## 2. Mission

Turn the Pixel's punch-hole camera into a living, interactive surface that feels like Google shipped it: Material 3 Expressive, driven by the user's own system theme, with Apple Dynamic Island's interaction discipline. Pixel-first, tuned on Pixel 8, and self-adapting at runtime to whatever device and settings it runs on. Take the best of Material Capsule, dynamicSpot, and Dynamic Notch Notification Bar, and fix every weakness listed in Section 3.

---

## 3. Research baseline (already done; extend in Phase 0)

### 3.1 Competitor teardown

| App | Keep (best ideas) | Beat (documented weaknesses) |
|---|---|---|
| **Material Capsule** (Pryshedko, 500K+, 4.4) | Material 3 Expressive look, wallpaper colors, "Robot vs Fruit" animation style toggle, tap cards (sliders, flashlight strength, barcodes, app/contact launch), download tracking, tap/double-tap/long-press/swipe mapping, reply from capsule | Spotify shows no progress bar in compact media. Few color controls. Fixed event show times. Fixed media/event card dimensions. Thick black widget border not adjustable. Expand animation flashes the pill before opening and the pill disappears. No "play last media". Users asked for call integration. Declares sharing device IDs, collects contacts, data cannot be deleted. Closed source. Needs recalibration on foldables. Gesture misfires near status icons. |
| **dynamicSpot** (Jawomo, 10M+, 4.6, ads) | Detached "minimal" bubbles for concurrent events, per-app filters, app shortcuts ("Live Activities"), reply from popup, music custom actions (like/favorite), timer countdown, maps distance, battery alarms | iOS clone look, not Android native. Ads. Music visualizer fails with Bluetooth, with apps like Libby, and with Pixel "Adaptive sound". Covers status bar icons. Overlaps screen-record indicator. Duplicate popups in two-popup mode. Stops working after days. No notification history. Banking apps refuse to run while its accessibility service is on. Shares data with third parties. Fully blocked under Android 17 Advanced Protection Mode. |
| **Dynamic Notch Notification Bar** (5M+, 4.6, ads) | Bluetooth device battery display, per-app background and text colors, charging animation, call UI, face-unlock animation, album art with blurred background, font controls | Banner ads even after lifetime purchase. Reviewers call permissions "aggressive" and do not trust it. Play Protect warnings. Declares data not encrypted. Feature sprawl (sports scores, weather themes) that needs network. |

Note: the Reddit thread could not be fetched. Phase 0 must read it manually (I will paste it if you ask) and append findings.

### 3.2 Apple Dynamic Island principles to adopt

1. **Four presentations**: compact (leading + trailing content split by the camera, reads as ONE piece of info), minimal (when several activities run: one attached, others detached as circle/oval), expanded (touch-and-hold), and a fallback banner.
2. **Tap opens the owning app. Long-press expands.**
3. **Expanded is an enlargement of compact**: the same info stays in the same place when it grows.
4. **Concentric geometry**: island corners mirror the hardware; inner radius = outer radius minus padding. Standard expanded content margin is generous (Apple uses 20pt).
5. **Minimal shows live data, not a logo** (a countdown ring beats an icon).
6. **Activities end automatically after 8 hours.**
7. **iOS 27**: compact and minimal also show in landscape, with a width-limited variant.
8. **iPhone 18 Pro**: up to **three** simultaneous activities (was two).
9. Never draw attention to the island for its own sake. No promo content.
10. Design observation: the island body stays black; color comes from content (glyphs, progress, keyline tint), which is why it blends with the camera.

### 3.3 Android platform facts (2026) that shape this build

1. Android 16 added **Live Updates** and `Notification.ProgressStyle` (segments, points, tracker icons). Android 16 QPR1 on Pixel shows them as a status bar chip, on AOD and lock screen.
2. Android 17 added **MetricStyle** Live Updates: up to three data points (fitness, timers, travel).
3. Promotion criteria: ongoing, promotion requested, non-empty title, standard/BigText/Call/Progress/Metric style, no custom RemoteViews, not a group summary, not colorized, channel importance above MIN, `POST_PROMOTED_NOTIFICATIONS` declared.
4. **Android 17 Advanced Protection Mode revokes AccessibilityService from any app not classified as an accessibility tool.** Every island app (including this one) is unusable while AAPM is on. This is unfixable; handle it honestly (Section 8 onboarding, rule S3 in 9.4).
5. Android 17 QPR2 (beta, stable expected December) adds **Status bar settings**: notification icons All / Priority / None, and toggles for mute, vibrate, battery %.
6. Android 17 QPR2 expands **Dynamic Color**: free color slider plus Neutral / Soft / Bright / Bold styles.
7. Google I/O 2026: Android UI is **Compose-first**. Material 3 Expressive = `MotionScheme` (expressive vs standard; spatial springs with overshoot for size/position, effects springs without overshoot for color/alpha; fast/default/slow speeds), 35-shape library, built-in shape morphing.
8. Pixel 11 series launched August 12, 2026. Pixel phones use a centered punch-hole.
9. Only `TYPE_ACCESSIBILITY_OVERLAY` can draw over the status bar. `TYPE_APPLICATION_OVERLAY` renders below it and is useless for a cutout island.
10. **Pixel 8 (`shiba`)**: 6.2 in 1080 × 2400 OLED, 20:9, 428 ppi, LTPS panel with 60 to 120 Hz ("Smooth Display"), Tensor G3, 8 GB RAM, in-screen fingerprint, launched on Android 14, supported to at least October 2030. The Pro is LTPO 1 to 120 Hz; the base Pixel 8 is not.

---

## 4. Product principles (tie-breakers for every choice)

1. **Pixel-native first**: if the OS already has a token, API, or pattern, use it instead of inventing one.
2. **Zero trust tax**: no `INTERNET` permission, no ads, no analytics, no window content reading (`canRetrieveWindowContent="false"`). Everything local, everything deletable.
3. **Glanceable in 1 second**: compact shows at most 2 data points. If an event does not help me decide or act in 3 seconds, it is off by default.
4. **Motion is continuous**: the container never disappears or flashes during a state change; every animation is interruptible and keeps velocity.
5. **Honest degradation**: when something cannot work (AAPM, missing metadata, no visualizer data), hide it cleanly and say why in settings. Never show a broken element.
6. **Everything that has a number is configurable**, with a sane default and a reset.
7. **Measure, don't assume**: never hardcode a device dimension or capability. Pixel 8 is the tuning target, but every device (Pixel 8 included) goes through the same runtime probe and rules in Section 9.

---

## 5. Scope

### P0 (MVP, must ship)
1. Cutout auto-calibration + manual fine-tune.
2. Idle states: invisible, black camera ring, or hidden.
3. Media: compact + expanded + scrubbing + custom actions + progress extrapolation + resume last media.
4. Notifications: transient heads-up in island, expand to full text, inline reply, actions, swipe dismiss, per-app allowlist, DND aware, lock-screen privacy aware.
5. Calls: incoming (answer/decline) and ongoing (timer).
6. Timers, stopwatch, alarms (Google Clock).
7. Charging and low battery.
8. Priority engine with 1 compact + up to 2 minimal bubbles (3 total, iPhone 18 Pro parity).
9. Gestures (Section 6.4) + haptics.
10. Auto-hide: fullscreen/immersive, camera app, user list.
11. Theme engine: System color mode, live updates on any theme change.
12. Settings app with live, geometry-accurate preview.
13. Service health: rebind, self-heal, status screen.

### P1
1. **Live Updates renderer** (ProgressStyle + MetricStyle) drawn natively in the island.
2. Navigation (Google Maps turn, distance, street).
3. Download/upload progress.
4. Bluetooth connect with battery when available.
5. Ringer / DND / flashlight / hotspot / VPN transients.
6. Screen recording indicator.
7. Tap Card: brightness, volume, flashlight strength, app + contact shortcuts, barcodes.
8. Notification history stack (24h, clearable).
9. Content color mode + per-app color override.
10. Unlock animation.
11. Themed monochrome app icons.

### P2
1. Real audio visualizer (opt-in, mic permission, auto-fallback).
2. Landscape minimal presentation.
3. Pixel Fold inner and cover display support.
4. ADB-granted extras (`WRITE_SECURE_SETTINGS`): global heads-up toggle, status icon hiding.
5. Public intent API so Tasker/Automate can post custom island activities.
6. Quick Settings tile: pause/resume island.
7. Settings export/import (JSON).

### Non-goals (v1)
Weather, sports, anything needing network. Root. Shizuku. AOD rendering (impossible for third-party apps). Non-Pixel OEM tuning.

---

## 6. Interaction model

### 6.1 States

```
Hidden -> Idle -> Compact(main) -> Compact(main + minimal[1..2])
                 \-> Transient(event, restores underlying)
Compact/Minimal -> Expanded(activity)   (long-press or swipe down)
Expanded -> Compact                     (swipe up, tap outside, timeout)
```

### 6.2 Activity model

```kotlin
sealed interface IslandActivity {
  val id: String; val pkg: String; val priority: Int
  val persistent: Boolean; val createdAt: Long; val updatedAt: Long
}
// Kinds: Media, IncomingCall, OngoingCall, Alarm, Timer, Stopwatch, Navigation,
// LiveUpdateProgress, LiveUpdateMetric, Download, Recording,
// Notification, SystemTransient(type)
```

### 6.3 Priority and arbitration (explicit numbers)

| Priority | Kind | Type |
|---|---|---|
| 100 | Incoming call | persistent until answered/declined |
| 95 | Alarm ringing | persistent until dismissed |
| 90 | Timer in last 10 s / timer ended | persistent |
| 85 | Ongoing call | persistent |
| 80 | Navigation, next turn < 300 m | persistent |
| 70 | Promoted Live Update | persistent |
| 60 | Media playing | persistent |
| 55 | Timer running / stopwatch | persistent |
| 50 | Screen recording | persistent |
| 40 | Download / upload | persistent |
| 30 | System transient (charging, BT, ringer, torch, unlock) | duration-bound |
| 20 | Notification heads-up | duration-bound |
| 10 | Media paused < 10 min | persistent, lowest |

Rules:
1. Highest persistent activity owns compact. Next two become minimal bubbles (first on trailing side, second on leading side).
2. Transients preempt compact for their duration, then the previous layout restores with the same spring.
3. Transient queue: FIFO, max 5, same-type events coalesce (two charging events = one).
4. Horizontal swipe on compact cycles persistent activities. A manual pick pins that activity for 30 min over priority, except priority >= 90.
5. Any activity with no update for 8 h ends.
6. Duplicate guard: same notification key or same content hash within 2 s = one event.

### 6.4 Gestures (all remappable)

| Gesture | Target | Default action | Haptic |
|---|---|---|---|
| Tap | compact / minimal | open owning app (contentIntent) | `CONFIRM` |
| Long-press 400 ms | compact / minimal | expand | `GESTURE_START` |
| Swipe down | compact | expand | `GESTURE_START` |
| Swipe up | expanded / transient | collapse / dismiss | `GESTURE_END` |
| Swipe horizontal | compact | cycle activities | `SEGMENT_TICK` |
| Swipe horizontal | expanded notification | dismiss notification | `REJECT` |
| Swipe down | idle | notification history | `GESTURE_START` |
| Long-press | idle | Tap Card | `LONG_PRESS` |
| Double-tap | idle | user action (default: flashlight) | `CONFIRM` |

Touch target = island bounds + 6 dp slop, never extending into status bar icon zones.

### 6.5 Timings (defaults, each configurable 1 to 15 s)

Notification: 4 s + 1 s per 40 chars, max 7 s. Charging: 3 s. Bluetooth: 3 s. Ringer/DND/torch: 1.5 s. Unlock: 0.8 s. Download complete: 3 s. Expanded auto-collapse after 6 s idle (or never).

### 6.6 Placement guards

1. **Camera keep-out**: nothing renders inside the circle of radius `r + 4dp` around the cutout center.
2. **Clock/icon collision budget**: compact width is capped so it never covers the status bar clock or system icons (Gate D4).
3. **Privacy indicator**: when camera or mic is in use by any app (`CameraManager.AvailabilityCallback`, `AudioManager.getActiveRecordingConfigurations()`) [VERIFY], expanded width shrinks so the top-right privacy dot is never covered.
4. **Screen recording**: never cover the recording chip.
5. Hide in immersive/fullscreen, in camera apps, and in user-listed apps. Keyboard visible: keep compact, block expand.
6. Lock screen: persistent activities show; notification content shows `publicVersion` when the notification is private and the device is locked.

---

## 7. Design system spec

### 7.1 Color (the core requirement)

**Source of truth = the OS palette.** In System mode never derive colors from the wallpaper yourself. Read the tokens the OS already generated (`dynamicDarkColorScheme(ctx)` / `dynamicLightColorScheme(ctx)`, backed by `android.R.color.system_*` resources). Result: the island automatically matches Quick Settings and Settings, including Android 17 QPR2's color slider, Neutral/Soft/Bright/Bold styles, and user contrast level.

**Live theme updates** (no restart, no flash):
- `ComponentCallbacks.onConfigurationChanged` (dark mode, font scale, density).
- `WallpaperManager.addOnColorsChangedListener`.
- `UiModeManager.addContrastChangeListener` (API 34).
- On any event: rebuild scheme from a fresh `createConfigurationContext(...)` [VERIFY that a long-lived service context sees new system color values], then animate all colors with the effects spring.

**Color modes (user picks):**
1. **System** (default): OS dynamic scheme.
2. **Content**: seed from album art or app icon via `material-color-utilities` (Celebi quantizer + Score), `SchemeContent` dark, harmonized toward system primary by a "Harmony" slider 0 to 100 %.
3. **Custom**: seed color + style (TonalSpot, Vibrant, Expressive, Fidelity, Content, Monochrome, Neutral, Rainbow, FruitSalad).
4. **Per-app override**: any app can get its own seed or fixed colors.

**Token map:**

| Island token | Value |
|---|---|
| `body.collapsed` | `#000000` always (blends with camera on OLED) |
| `body.expanded` | Style option: **Black** (default, Apple-grade) = `#000000`; **Tonal** = dark `surfaceContainerHigh`; **Tinted** = dark `surfaceContainerHigh` + 8 % `primary`; **Follow light/dark** = light `surfaceContainerHigh` in light mode with a black keep-out disc around the camera |
| `keyline` | `primary` @ 32 % alpha, width 0 to 2 dp (default 1 dp Black, 0 dp Tonal) |
| `text.primary` / `text.secondary` | `onSurface` / `onSurfaceVariant` of the active scheme |
| `accent.active` (progress, play glyph, rings) | `primary` |
| `accent.track` | `primary` @ 24 % |
| `accent.metric2` | `tertiary` |
| `button.primary` | `primaryContainer` / `onPrimaryContainer` |
| `button.secondary` | `surfaceContainerHighest` / `onSurface` |
| `call.accept` / `battery.charging` | green harmonized to `primary` (Semantic toggle on) else `primary` |
| `call.decline` / `battery.low` | `error` |

**Contrast guard**: every text/glyph pair must be >= 4.5:1 (3:1 for large glyphs). If Content or Custom mode fails, fall back to System tokens for that element.

**Widget/card border**: width 0 to 4 dp (default 0). Fixes Material Capsule's thick-border complaint.

### 7.2 Geometry (exact formulas)

Inputs from `DisplayCutout.getCutoutPath()` (fallback `boundingRectTop`): cutout center `(cx, cy)`, radius `r`. Screen corner radius `R` from `Display.getRoundedCorner(POSITION_TOP_LEFT)` (fallback 36 dp).

| Element | Formula / default |
|---|---|
| Ring gap `g` | 3 dp (0 to 8 dp) |
| Idle / compact height `Hc` | `2r + 2g` |
| Compact leading width `Lw`, trailing `Tw` | `round(1.4 × Hc)` each (Apple ratio), capped by Gate D4 budget |
| Compact total width | `Hc + Lw + Tw`, centered on `cx` |
| Minimal bubble | diameter `Hc`, 6 dp gap, splits and merges with a metaball bridge in BOTH directions |
| Expanded width `We` | `screenWidth − 2m`, `m` = 12 dp |
| Expanded heights (detents) | Small `0.23 × We`, Medium `0.43 × We`, Large `0.52 × We` |
| Expanded top edge | `cy − Hc/2` (grows down and sideways from the compact) |
| Expanded corner radius `Re` | `R − m` (concentric with the screen) |
| Inner padding `p` / inner radius | 16 dp / `Re − p` |
| Sizes for media art, buttons, tap-card tiles | user-adjustable scale 80 to 130 % |

Recalculate on rotation, display change (Fold), and density change.

### 7.3 Motion

**Schemes (user toggle):** Expressive (default, `MotionScheme.expressive()`), Standard (`MotionScheme.standard()`), Fluid (Apple-like: near-critically damped springs, ~0.45 s response, tiny overshoot). Use the scheme's tokens; never hardcode durations.

**Spatial springs** for width, height, position, corner radius. **Effects springs** for color and alpha.

**Choreography for every transition (fixes the flash/disappear bug):**
1. Container starts its spring immediately from its current size and velocity.
2. Outgoing content: alpha to 0 and scale to 0.96 on the fast effects spring.
3. Incoming content: starts when the container reaches 60 % of the target size; alpha 0 to 1 with blur 8 dp to 0 (`RenderEffect`).
4. Content anchored to the camera side it lives on (leading content grows leftward, trailing rightward), so "same info, same place" holds.
5. Retargeting mid-flight keeps velocity (`Animatable.animateTo` with current velocity). Never snap, never restart from zero.
6. Collapse ends at the idle pill. With "hide when idle", shrink into the camera ring, then fade.
7. Shape morph (`androidx.graphics.shapes.Morph`, `MaterialShapes`) only on entry of special states (unlock, charging). Max one morph per transition.
8. Numbers: per-digit rolling transition with tabular figures.
9. Reduced motion: if animator scale is 0 or "Remove animations" is on, use crossfades only.

### 7.4 Typography

Gate D6 picks the font. Tabular numerals (`tnum`) for all time and percentages. Compact text caps at 1.3× font scale (geometry is fixed); expanded honors full font scale up to 200 %.

### 7.5 Icons

Material Symbols Rounded, animating fill 0 to 1 for active state. App icons: use the adaptive icon's monochrome layer tinted `primary` when the Pixel "Themed icons" look is on [VERIFY how to read that setting; else offer a toggle]; full-color icon otherwise.

### 7.6 Haptics

`HapticFeedbackConstants` per Section 6.4, plus `VibrationEffect.Composition` primitives for charging connect (`THUD`) and timer end (`CLICK` × 3). Honor system touch-feedback setting.

### 7.7 Accessibility of the island itself

Every element has a content description and action labels. Touch targets >= 48 dp via slop. TalkBack reads activity summary on focus.

---

## 8. Architecture

**Stack**: Kotlin (latest stable), Jetpack Compose + Material 3 (expressive APIs), `androidx.graphics:graphics-shapes`, `material-color-utilities`, Hilt, Coroutines/Flow, Proto DataStore (settings), Room (history only). minSdk 34, target/compile latest stable (Gate D9).

**Modules**:
```
:app                 settings UI, onboarding, live preview
:core:model          IslandActivity, IslandState, events
:core:engine         pure Kotlin state machine + priority arbitration (no Android deps)
:core:geometry       cutout parsing + layout math (pure Kotlin + thin adapter)
:core:theme          color engine, motion, shapes, typography
:source:notifications NotificationListenerService + per-style parsers
:source:media        MediaSessionManager, progress extrapolation, resumption
:source:system       battery, BT, ringer, torch, unlock, camera/mic in-use
:overlay             AccessibilityService, WindowManager host, Compose renderer
:core:device        DeviceProbe, DeviceProfile store, adaptation rule engine (pure Kotlin rules + thin Android adapters)
:lab                 debug app that fires every event type (incl. ProgressStyle, MetricStyle, CallStyle)
```

**Data flow (unidirectional)**: Sources emit `Flow<SourceEvent>` -> Engine reduces to `StateFlow<IslandState>` -> Renderer draws. The renderer never touches sources. Settings flow into engine and renderer.

**Overlay host**:
- `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`, `layoutInDisplayCutoutMode = ALWAYS`.
- `ComposeView` with `ViewTreeLifecycleOwner`, `SavedStateRegistryOwner`, `ViewModelStoreOwner` supplied by the service.
- Accessibility config: `isAccessibilityTool="false"`, `canRetrieveWindowContent="false"`, events limited to `typeWindowStateChanged` (foreground package only).

**Media**:
- `MediaSessionManager.getActiveSessions(listenerComponent)`.
- Progress = `position + (now − lastPositionUpdateTime) × speed`, clamped to duration. If duration missing, hide the bar (fixes Spotify gap cleanly).
- Custom actions from `PlaybackState.customActions`.
- Resume last media via the app's `MediaBrowserService` with the recent root hint [VERIFY].
- Output switcher button opens the system output picker [VERIFY intent].

**Notifications**:
- Skip group summaries and plain foreground-service notifications unless a parser recognizes them.
- Heads-up only for importance >= DEFAULT and not `FLAG_ONLY_ALERT_ONCE` updates.
- DND via ranking `matchesInterruptionFilter()`.
- Reply via `RemoteInput`. Dismiss via `cancelNotification(key)`.
- Live Updates: detect promoted flag and read ProgressStyle / MetricStyle extras [VERIFY exact extras keys on API 36/37].

**Health**: on listener disconnect call `requestRebind`; watchdog checks both services every 60 s while screen is on; status screen shows green/amber/red per service and why.

**Permissions (complete list)**: accessibility service, notification listener, `BLUETOOTH_CONNECT` (device names/battery), `VIBRATE`. Contacts via the Android 17 contact picker (no `READ_CONTACTS`). Flashlight via `CameraManager` (no `CAMERA`). `RECORD_AUDIO` only if P2 real visualizer is enabled. **No `INTERNET`.** Optional ADB grant `WRITE_SECURE_SETTINGS` for P2 extras.

**Onboarding**: prominent disclosure screen stating exactly what the accessibility service does and does not do. If the grant fails or is revoked, detect Advanced Protection [VERIFY `AdvancedProtectionManager` access] and explain it is the cause.

---

## 9. Device adaptation layer (runtime, Pixel-first)

Order of authority: **1. live measurement > 2. device profile quirks > 3. generic defaults > 4. user override (always wins, stored per device + display + rotation).**

### 9.1 Primary target: Pixel 8 (`shiba`)

| Fact | Value | Consequence for the build |
|---|---|---|
| Display | 6.2 in, 1080 × 2400, 20:9, 428 ppi OLED | All geometry in dp from live metrics; verify ring alignment on this exact panel first |
| Refresh | LTPS 60 to 120 Hz, "Smooth Display" toggle | Frame budget 8.3 ms at 120 Hz, 16.6 ms at 60 Hz. The panel idles at 60 Hz, so animations triggered by events (not touches) may render at 60 Hz: boost only while animating (Gate D11) |
| Cutout | Centered punch-hole | Symmetric island (rule G1) |
| SoC / RAM | Tensor G3, 8 GB | Thermal-aware degradation is mandatory (rules T1, T2) |
| Biometrics | In-screen fingerprint | Unlock animation uses the generic lock-open morph, no face-specific art |
| OS | Launched Android 14, supported to at least Oct 2030 | minSdk 34 covers every Pixel 8 build ever shipped |

### 9.2 DeviceProbe: read everything at runtime

| Category | Read | API |
|---|---|---|
| Identity | manufacturer, brand, model, codename, SDK, minor SDK, security patch, fingerprint | `Build.*`, `Build.VERSION.SDK_INT_FULL` [VERIFY API 36] |
| Pixel check | Google manufacturer AND Pixel system feature | `PackageManager.hasSystemFeature("com.google.android.feature.PIXEL_EXPERIENCE")` [VERIFY] |
| Cutout and corners | cutout path, bounding rects, waterfall insets, screen corner radii, status bar inset height | `WindowManager.currentWindowMetrics`, `DisplayCutout`, `Display.getRoundedCorner` |
| Size and density | screen size, density ("Display size" setting), rotation | `WindowMetrics`, `Configuration.densityDpi` |
| Displays and posture | display ids, fold state | `DisplayManager.DisplayListener`, Jetpack `WindowInfoTracker` (`FoldingFeature`) |
| Refresh | supported modes, current rate, Smooth Display state | `Display.getSupportedModes`, `Display.getRefreshRate`, `Settings.System` peak refresh key [VERIFY readable] |
| Theme | dark mode, contrast, dynamic color available | `Configuration.uiMode`, `UiModeManager.getContrast` |
| Text | font scale, bold text | `Configuration.fontScale`, `Configuration.fontWeightAdjustment` |
| Motion | animator and transition scales | `Settings.Global.ANIMATOR_DURATION_SCALE`, `TRANSITION_ANIMATION_SCALE` |
| Accessibility display | color inversion, color correction, extra dim | `Settings.Secure` keys [VERIFY readable] |
| Locale and time | 24 h format, layout direction | `DateFormat.is24HourFormat`, `Configuration.layoutDirection` |
| Power | battery saver, thermal status, app standby bucket | `PowerManager.isPowerSaveMode`, `addThermalStatusListener`, `UsageStatsManager.getAppStandbyBucket` |
| Memory | low-RAM flag, memory class | `ActivityManager` |
| Haptics | vibrator present, amplitude control, supported primitives, system touch feedback | `Vibrator.areAllPrimitivesSupported`, `Settings.System.HAPTIC_FEEDBACK_ENABLED` |
| Flashlight | strength levels | `CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL` (API 33) |
| Audio | visualizer usable | live probe per Gate D5 |
| Notifications | DND filter, lock-screen notification privacy, Live Updates (SDK >= 36), MetricStyle (SDK >= 37) | `NotificationManager`, `Settings.Secure` lock-screen keys [VERIFY] |
| System UI | QPR2 status bar settings present, themed icons on | [VERIFY detection method for both] |
| Security | Advanced Protection on | [VERIFY] |

### 9.3 DeviceProfile store (quirks only, never geometry)

`assets/device_profiles.json`, keyed by `Build.DEVICE`:

```json
{
  "shiba": {
    "name": "Pixel 8", "tier": "primary", "verified": true,
    "cutoutKind": "center", "ringGapDp": 3.0, "cutoutOffsetPx": [0, 0],
    "curvedEdges": false, "refreshBoost": true,
    "notes": "LTPS 60-120 Hz; boost during animation; thermal-aware"
  }
}
```

Tiers: **primary** = Pixel 8, verified on my phone. **supported** = other Pixels, values measured on emulator or by API, marked unverified. **generic** = non-Pixel, measured only, calibration wizard forced on first run.

Seed codenames [VERIFY each in Phase 0; an unknown codename simply falls back to generic]: 6 `oriole`, 6 Pro `raven`, 6a `bluejay`, 7 `panther`, 7 Pro `cheetah`, 7a `lynx`, Fold `felix`, Tablet `tangorpro`, 8 `shiba`, 8 Pro `husky`, 8a `akita`, 9 `tokay`, 9 Pro `caiman`, 9 Pro XL `komodo`, 9 Pro Fold `comet`, 9a `tegu`, 10 `frankel`, 10 Pro `blazer`, 10 Pro XL `mustang`, 10 Pro Fold `rango`. Add 10a and the 11 series in Phase 0.

### 9.4 Adaptation rules (probe result -> behavior)

| ID | Condition | Behavior |
|---|---|---|
| G1 | Cutout is a centered circle | Symmetric island (Pixel 8 path) |
| G2 | Cutout off-center (corner, Fold inner screens) | Asymmetric: expand toward screen center, both compact slots on the center side, bubbles toward center |
| G3 | No cutout (Pixel Tablet, under-display camera, emulator) | Virtual island: pill centered in the status bar, height = status bar inset − 6 dp, hidden when idle |
| G4 | Waterfall or curved edges | Add waterfall inset to side margin `m` |
| G5 | Rotation | Recompute; landscape follows the P2 rule, else hide |
| G6 | Multiple displays or fold posture | Geometry cache per (displayId, rotation); hide on the inner display while half-opened |
| G7 | Density, display size, or resolution change | Recompute all geometry from fresh metrics |
| R1 | Max refresh >= 120 Hz and Smooth Display on | 8.3 ms frame budget; request 120 Hz for the overlay window only during animations (D11) |
| R2 | Otherwise | 16.6 ms budget, identical choreography |
| T1 | Thermal >= MODERATE or battery saver on | Standard motion, no blur, metaball replaced by plain scale, tickers at 1 Hz, visualizer off |
| T2 | Thermal >= SEVERE | Only activities with priority >= 60, no transients |
| M1 | Low-RAM device | No blur, no album-art quantization (use app accent color) |
| A1 | Animator scale 0 or "Remove animations" on | Crossfades only |
| A2 | Animator scale > 1 or < 1 | Use slow or fast motion tokens respectively |
| X1 | Font scale or bold text | Apply Section 7.4 caps; bold uses `fontWeightAdjustment` |
| X2 | Color inversion on | Hide the idle ring (inversion would turn it white); keep active states |
| X3 | High contrast | Keyline forced on; text uses on-color roles only |
| L1 | RTL layout | Mirror leading/trailing and horizontal swipe directions |
| L2 | 24 h setting | Timer and clock text follow it |
| H1 | Haptic primitives supported | Compositions; else predefined effects; system touch feedback off = no haptics |
| F1 | Flashlight strength levels > 1 | Strength slider; else on/off toggle |
| S1 | SDK >= 36 / >= 37 | Enable Live Updates reader / MetricStyle rendering |
| S2 | QPR2 status bar settings present | Onboarding card recommending notification icons "None" or "Priority" |
| S3 | Advanced Protection on | Blocked screen with honest explanation |
| S4 | App standby bucket restricted | Warning on the health screen |

### 9.5 Re-probe triggers

First run, app update, `Build.FINGERPRINT` change (OS update: re-validate calibration; drop saved offsets if the measured cutout changed), configuration change, display added or changed, fold posture change, `ContentObserver` on every read setting, thermal listener, power-save broadcast, contrast listener. Apply adaptation changes live with the effects spring. Never restart the service.

### 9.6 Device Report screen (Settings > Device)

Shows: identity and profile tier; the real cutout path drawn with measured numbers; every probed value; every active rule with its reason (e.g. "Blur off: thermal MODERATE"). Buttons: re-run calibration, toggle debug bounds overlay, export report JSON via share sheet (local only, no network).

---

## 10. Decision gates (step-wise decision making)

Run each gate as a small spike, measure, write the ADR, then build.

| Gate | Question | Options | Criteria | Default |
|---|---|---|---|---|
| **D1** Overlay type | How to draw over the status bar? | A: accessibility overlay. B: `SYSTEM_ALERT_WINDOW` | Must render above status bar | A (B cannot) |
| **D2** Window sizing | How to pass touches through? | A: resize window only at state boundaries (grow before expand, shrink after collapse). B: fixed max window + touchable region (internal API) | P95 frame < 8.3 ms at 120 Hz, zero blocked taps outside island in 50-tap test | A |
| **D3** Renderer | Compose everywhere vs hybrid | A: Compose. B: custom View container + Compose content | Jank, idle RSS < 60 MB | A; B if A misses budget |
| **D4** Status bar collision | How wide can compact be? | A: slider-only budget. B: read SystemUI bounds via window info | B allowed ONLY if it works without content retrieval; else A. Also recommend Android 17 QPR2 status bar "Notifications: None/Priority" in onboarding | A + recommendation |
| **D5** Visualizer | Real audio or procedural? | A: procedural "rhythm" bars driven by playback state. B: `Visualizer` session 0 with mic permission | B must produce non-zero FFT within 2 s on BT, speaker, Adaptive Sound on. If not, auto-fallback to A and show a hint | A default, B opt-in |
| **D6** Font | Pixel font access | A: system families `google-sans`, `google-sans-text`, `google-sans-flex` via `Typeface.create`. B: bundle Roboto Flex | A only if the typeface resolves to a non-default family on my device | A if it resolves |
| **D7** Heads-up duplication | System heads-up shows too | A: guide per-app "Pop on screen" off. B: ADB-granted global heads-up toggle, restored on disable | B must restore reliably across crash/uninstall paths [VERIFY on Android 17] | A in P0, B in P2 |
| **D8** Distribution | Sideload or Play | personal-sideload vs play-store | `[DISTRIBUTION]` | Sideload, but stay Play-policy compliant |
| **D9** minSdk | 34 vs 36 | 34 keeps contrast APIs; 36 unlocks ProgressStyle natively | My device version | 34, gate Live Updates by SDK check |
| **D10** Expanded body default | Black vs Tonal | Show me both on my wallpaper | **ASK** with screenshots | Black |
| **D11** Refresh boost (Pixel 8 LTPS) | Force 120 Hz while animating? | A: set `preferredDisplayModeId` / `preferredRefreshRate` on the overlay window during animations only [VERIFY honored for accessibility overlays]. B: no boost | A only if event-triggered animations measure at 120 Hz on Pixel 8 and battery cost < 0.1 %/day | A if it works |
| **D12** Profile matching | How to apply device quirks | A: codename lookup + measured geometry. B: measured only | A must never override a live measurement, only add corrections | A |

---

## 11. Phases and acceptance criteria

### Phase 0: Research and verification (no product code)
- Resolve every `[VERIFY]`. Record current library versions.
- Install the 3 competitors on `[MY_PIXEL_MODEL]`; screen-record media, notification, call, timer, charging, expand/collapse; fill a behavior matrix in `docs/RESEARCH.md`.
- Dump my cutout: `adb shell dumpsys display` and `adb shell dumpsys window displays`; record path, radius, center, screen corner radius.
- Confirm Live Update notifications are readable by a notification listener on my build.
- Run a throwaway DeviceProbe on my Pixel 8 and record every Section 9.2 value; verify the codename list in 9.3.
- **Accept**: RESEARCH.md complete, ADR drafts for D1 to D12, zero unresolved `[VERIFY]`. **I approve before Phase 1.**

### Phase 1: Overlay skeleton + calibration
- Accessibility service draws the idle black ring exactly over the cutout. Calibration screen: X/Y/size/gap nudges in 0.5 dp steps.
- Build `:core:device` (DeviceProbe + profile store + rules G1 to G7) and a first Device Report screen.
- **Accept**: ring concentric with camera on my Pixel 8 by eye at 200 % zoom screenshot; touches outside ring pass through (50/50); survives reboot and theme change; `shiba` profile written and marked verified; the same build renders correctly on a Pixel 9 emulator, a Pixel Tablet emulator (G3), and a corner-cutout AVD (G2) with zero code changes; D1, D2, D12 ADRs final.

### Phase 2: Engine, geometry, motion
- State machine + priority table + queue + pinning, fully unit-tested with `:lab` events.
- Compact, minimal (split/merge metaball both ways), expanded detents, choreography rules 1 to 9.
- **Accept**: 100 expand/collapse cycles, 0 janky frames (JankStats + Macrobenchmark), P95 frame < the R1/R2 budget (8.3 ms on Pixel 8 at 120 Hz); interrupt mid-animation 20 times with no snap or flash; engine test coverage >= 90 %.

### Phase 3: Media (P0)
- **Accept**: works with Spotify, YouTube Music, Pocket Casts, Libby, Chrome video; progress bar present or cleanly hidden, never frozen; scrubbing accurate within 1 s; custom actions render; resume last media after reboot; Content color mode from album art passes contrast guard.

### Phase 4: Notifications, calls, timers, alarms (P0)
- **Accept**: Messages, WhatsApp, Gmail, Google Clock timer/stopwatch/alarm, Phone app incoming + ongoing; inline reply sends; no duplicates in 30-notification burst; DND respected; locked-device privacy respected.

### Phase 5: System events, theme engine, settings (P0 complete)
- Charging, low battery, auto-hide rules, placement guards 6.6.
- Theme: change wallpaper, dark mode, contrast, and (if on QPR2) color slider and styles; island updates live with animated color, no restart.
- Settings app in M3 Expressive with live geometry-accurate preview, every Section 6 and 7 knob, reset per section.
- **Accept**: theme change reflected < 500 ms; every setting has a visible effect in preview; health screen accurate when I revoke each permission.

### Phase 6: P1 features
- **Accept per feature**: works on my device with at least 2 real apps (or `:lab` for Live Updates if no real app on my phone uses them), plus screenshot tests.

### Phase 7: Hardening and release
- Budgets in Section 12 met; R8 release build; signed APK; README with setup + ADB extras; privacy page stating no network.
- Run the full rule table (9.4) through forced conditions: thermal (`adb shell cmd thermalservice override-status`) [VERIFY], battery saver, font 2.0, bold, RTL pseudo-locale, inversion, Remove animations.
- **Accept**: 72-hour daily-driver run on my Pixel 8 with zero crashes and zero listener dropouts; every rule observed firing in the Device Report.

### Phase 8: P2 features (only after I confirm Phase 7)

---

## 12. Budgets

| Metric | Target | Reference to beat |
|---|---|---|
| Battery (24 h, 2.5 h media) | < 1.0 % | Material Capsule measured 1.2 to 1.8 % |
| Idle RSS | < 45 MB | Material Capsule ~45 to 60 MB, dynamicSpot ~55 to 85 MB |
| Idle CPU | 0 wakeups when idle (tickers run only while a live countdown is visible) | |
| Touch to first animated frame | < 16 ms | |
| Expand/collapse | P95 frame < refresh budget (8.3 ms at 120 Hz on Pixel 8) | |
| Service cold start to idle ring | < 300 ms | |

---

## 13. Testing matrix

1. Unit: engine, geometry, parsers (fixtures captured from real notifications of every listed app).
2. Screenshot: every presentation × light/dark × System/Content/Custom × contrast standard/medium/high × font scale 1.0/1.3/2.0.
3. Macrobenchmark: frame timing for expand, collapse, split, merge, cycle.
4. `:lab` scripted scenarios: 3 concurrent activities + transient burst + call interrupt.
5. Device: Pixel 8 physical (primary) + emulators for Pixel 9, Pixel 10 Pro, Pixel Fold (inner and cover), Pixel Tablet (no cutout), and a generic corner-cutout AVD.
6. Edge cases: rotation mid-expand, keyboard open, split-screen, PiP, screen recording, camera/mic in use, lock/unlock mid-animation, battery saver, theme change mid-animation.

---

## 14. Report format (end of every phase)

```
PHASE: <n> <name>
STATUS: PASS | BLOCKED
ACCEPTANCE: <criterion> -> PASS/FAIL (evidence: test name / metric / screenshot path)
ADRs: <list>
VERIFY RESOLVED: <claim> -> TRUE/FALSE (source)
RISKS: <new risks + mitigation>
NEXT: <phase n+1 plan in 5 lines max>
NEED FROM ME: <only if blocked>
```

---

## 15. Differentiators checklist (must all be true at v1)

- [ ] Island colors follow my exact system theme, contrast, and QPR2 color styles, live.
- [ ] Promoted Live Updates render natively in the island.
- [ ] No network permission, no ads, no screen-content reading.
- [ ] No flash, no disappearing pill, every animation interruptible.
- [ ] Visualizer never shows frozen bars.
- [ ] Never covers clock, status icons, privacy dot, or recording chip.
- [ ] Up to 3 concurrent activities with two-way liquid split/merge.
- [ ] Notification history + swipe dismiss + no duplicates.
- [ ] Every duration, dimension, border, and color adjustable.
- [ ] Media progress always correct or cleanly hidden; last media resumable.
- [ ] Honest AAPM handling and a health screen.
- [ ] Adapts at runtime to device, display, and settings; the Device Report explains every adaptation.
