# Aperture: Phase 0 research and verification

- Date: 2026-09-25
- Status: **in progress.** Everything that can be measured without your grants on the phone is done. The remaining items are in [section 13](#13-open-items-blocking-phase-0-acceptance).
- Spec: [`docs/SPEC.md`](SPEC.md). ADR drafts: [`docs/adr/`](adr/).
- Method: a throwaway probe app ([`spikes/phase0-probe`](../spikes/phase0-probe/README.md)) run on the Pixel 8 and on an API 36.1 emulator, plus source-checked research. Raw device dumps stay outside the repo because they contain personal device data. Only the values below are recorded.

## 1. Inputs

| Input | Value | Source |
|---|---|---|
| `[MY_PIXEL_MODEL]` | Pixel 8, `shiba` | adb (`ro.product.device`) |
| `[MY_ANDROID_BUILD]` | **Android 17 (API 37.0), build CP2A.260805.005**, security patch 2026-08-05, `user` / `release-keys` | adb `getprop` |
| `[DISTRIBUTION]` | `personal-sideload` | Default ([ADR-008](adr/ADR-008-distribution.md)) |
| `[APP_NAME]` | Aperture | Default |
| `[PACKAGE]` | **Not set.** Proposal: `dev.ayushvarma.aperture` | Needed before Phase 1 |

## 2. Test environment

| Item | Value |
|---|---|
| Host | macOS 26.6.2, Apple M4, 16 GB RAM, **13 GiB free disk (94% full)** |
| Android SDK | `~/Library/Android/sdk` (not on PATH): platforms 34 and 36 (36.0 stubs), build-tools up to 36.1.0, emulator 36.3.10, platform-tools 36.0.2. **Missing: platform 37** (required, [ADR-009](adr/ADR-009-min-sdk.md)) and cmdline-tools |
| JDK | Android Studio's bundled JBR 21. The JDK 11 on PATH is too old for AGP 9 |
| Android Studio | 2025.2.2 installed. The current stable is Quail 4 (2026.1.4), which supports AGP 7.1–9.4. Command-line builds don't care; opening the product in the IDE likely needs the update |
| Emulator | AVD `Medium_Phone_API_36.1`: Android 16 QPR2, build BE4B.251210.005, 1080 × 2400 at 420 dpi (the same panel resolution as the Pixel 8), Google Play image. Its animator scales are 0, so rule A1 fires there |
| Probe | Zero-dependency Java against the AGP 8.13.2 already cached, so it built offline with no downloads |

## 3. Library versions (checked at the source on 2026-09-25)

Items marked ✓ I re-read myself from Google Maven metadata. The rest come from Maven Central, Google Maven and release notes via the research pass; the source column says where.

| Artifact | Latest stable | Notes |
|---|---|---|
| Android Gradle Plugin | **9.4.1** ✓ (2026-09-18) | Built-in Kotlin (don't apply `org.jetbrains.kotlin.android`). Needs Gradle ≥ 9.6.0 and JDK 17; supports API 37 |
| Gradle | 9.8.0 (2026-09-24); **use 9.7.1** | Kotlin 2.4.20 officially supports up to Gradle 9.7 |
| Kotlin + Compose compiler plugin | **2.4.20** (2026-09-07) | Plugin id `org.jetbrains.kotlin.plugin.compose`, same version |
| KSP | 2.3.12 | KSP2; no longer tied to the Kotlin version; ≥ 2.3.10 needed for Kotlin 2.4 |
| SDK Platform | **37** final (37.0 r02, 2026-06-04) | Android 17 released 2026-06-16 |
| Compose BOM | **2026.09.00** ✓ | ui/foundation 1.12.1, material3 1.4.0. **Compose 1.12 requires compileSdk 37 and AGP ≥ 9.2** |
| material3 | **1.4.0** ✓ stable; 1.5.0-alpha29 ✓ latest | **Stable has no Material 3 Expressive API.** See [ADR-013](adr/ADR-013-material3-expressive-dependency.md) |
| graphics-shapes | 1.1.0 | `RoundedPolygon`, `Morph` |
| core / core-ktx | 1.19.1 | core-ktx is now an empty shim; use `androidx.core:core` |
| activity-compose | 1.13.0 | |
| lifecycle (runtime, service, viewmodel-compose) | 2.11.0 | |
| savedstate | 1.5.0 | |
| window (`WindowInfoTracker`) | 1.5.1 | |
| datastore (+ core) | 1.2.1 | |
| Room | 2.8.5 (Room 3 is stable as `androidx.room3:room3-runtime` 3.0.3) | Room 3: Kotlin-only, coroutine-only, needs a SQLiteDriver. Decide in Phase 1 |
| JankStats (`metrics-performance`) | 1.0.0 | |
| benchmark-macro-junit4 / baselineprofile plugin | 1.5.0 / 1.5.0 | Both work with AGP 9's new DSL |
| profileinstaller | 1.4.1 | |
| test.ext:junit / espresso / uiautomator | 1.3.0 / 3.7.0 / 2.4.0 | |
| Hilt (android, compiler, plugin) | **2.60.1** | First version reading Kotlin 2.4 metadata; plugin requires AGP ≥ 9 |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-serialization | 1.11.0 | For settings export (P2) |
| protobuf (javalite / kotlin-lite / protoc) | 4.36.2 | |
| protobuf Gradle plugin | **0.10.0** | First version that works with AGP 9 |
| material-color-utilities | **No official Maven artifact** | MDC-Android 1.14.0 ships `@RestrictTo` copies without the 2025 spec. **MaterialKolor `com.materialkolor:material-color-utilities` 5.0.1** has QuantizerCelebi, Score, all nine schemes and `SpecVersion.SPEC_2025` |
| Robolectric | 4.17 | Supports SDK 37 |
| Roborazzi | 1.75.0 | Works with AGP 9. On SDK 37 native graphics only the first capture renders (#912): pin screenshot tests to `sdk = 36` |
| Paparazzi | 1.3.5 (incompatible with AGP 9); 2.0.0-alpha05 | Prefer Roborazzi |
| Turbine / Truth / MockK / Kover | 1.2.1 / 1.4.5 / 1.14.11 / 0.9.9 | Kover fine on the pure-Kotlin modules |
| detekt | 1.23.8 stable **doesn't work with AGP 9 built-in Kotlin**; 2.0.0-alpha.6 does, with open issues | Decide in Phase 1 (lint + ktlint may be enough) |
| ktlint / Spotless / ktlint-gradle | 1.8.0 / 8.10.2 / 14.2.0 | |

A working set: AGP 9.4.1, Gradle 9.7.1, Kotlin 2.4.20, KSP 2.3.12, Hilt 2.60.1, protobuf plugin 0.10.0, compileSdk 37, JDK 21 toolchain.

## 4. Pixel 8 device record (Section 9.2)

Measured on 2026-09-25 from an ordinary third-party app context (the probe activity), except rows marked *dumpsys*.

### 4.1 Identity and Pixel check

| Value | Pixel 8 | Emulator (API 36.1) |
|---|---|---|
| Manufacturer / brand / model / device | Google / google / Pixel 8 / shiba | Google / google / sdk_gphone64_arm64 / emu64a |
| SoC | Tensor G3 (`zuma`) | n/a |
| `SDK_INT` / `SDK_INT_FULL` / minor | 37 / 3700000 / 0 | 36 / 3600001 / 1 |
| First API level | 34 | n/a |
| Media performance class | 34 | n/a |
| `PIXEL_EXPERIENCE` feature | **true** (also `PIXEL_2017…2019_EXPERIENCE`, `PIXEL_2021…2023_MIDYEAR_EXPERIENCE`, `GOOGLE_BUILD`) | false (`GOOGLE_EXPERIENCE`, `GOOGLE_BUILD` only) |

### 4.2 Display, cutout, corners

| Value | Pixel 8 | Source |
|---|---|---|
| Resolution / density | 1080 × 2400 px, 420 dpi (2.625), 411 × 914 dp, xdpi 428.6 / ydpi 426.3, about 6.17 in | `DisplayMetrics`, `Configuration` |
| Cutout spec | `m 576,65.75 a 36.25,36.25 0 0 0 -72.5,0 36.25,36.25 0 0 0 72.5,0 z @left` (px, left-anchored) | *dumpsys display*; also readable via `Resources.getSystem()` `config_mainBuiltInDisplayCutout` |
| Cutout circle | **center (539.75, 65.75) px, r = 36.25 px (13.81 dp)**, 0.25 px left of the screen center | `DisplayCutout.getCutoutPath()` bounds 503.5–576 × 29.5–102 |
| Cutout bounding rect (top) | 479–601 × 0–132 | `getBoundingRectTop()`; spec rect `M 478.5,0 h 122 v 132 h -122 Z @left` |
| Safe inset / status bar height | 132 px top (`status_bar_height` 132 portrait, 74 default and landscape) | `WindowInsets`, framework dimens |
| Screen corners | **top radius 104 px (39.6 dp)**, bottom 102 px; centers (104, 104), (976, 104), (978, 2298), (102, 2298) | `Display.getRoundedCorner` |
| Display shape | A continuous-curvature (non-circular) corner path, `config_mainDisplayShape` starting `M 96,0 C 91,0 83,2 79,2 …` | `Display.getShape()` |
| Waterfall insets | 0 | `DisplayCutout.getWaterfallInsets()` |
| Privacy indicator zone | **x 810–1026, y 0–132** | `WindowInsets.getPrivacyIndicatorBounds()` (public, API 31) |
| Nav bar | 63 px gesture bar (126 px frame) | Insets |

### 4.3 Refresh

| Value | Pixel 8 |
|---|---|
| Modes | id 1: 1080 × 2400 at 60 Hz; id 2: 1080 × 2400 at 120 Hz (seamless alternatives of each other) |
| Smooth Display | **On**: `peak_refresh_rate = Infinity`, `min_refresh_rate = 0.0` (both readable by apps) |
| ARR | `hasArrSupport() = false`; `getSuggestedFrameRate(NORMAL/HIGH) = 60 / 90` |
| Refresh policy (*dumpsys*) | Default mode id 2; DisplayModeDirector user-setting vote max 120; `mDefaultPeakRefreshRate` 60; brightness low and high zones both allow 120 |

### 4.4 Theme, text, motion, accessibility display, locale

| Value | Pixel 8 |
|---|---|
| Night mode | On (`uiMode` night, `UiModeManager` night mode 2) |
| Contrast | 0.0 (standard) |
| `theme_customization_overlay_packages` | readable: `theme_style=MONOCHROMATIC`, `color_source=preset`, `system_palette=333333` |
| System colors (dark) | primary `#FFFFFF`, onPrimary `#1B1B1B`, primaryContainer `#D4D4D4`, surfaceContainerHigh `#2A2A2A`, surfaceContainerHighest `#353535`, onSurface `#E2E2E2`, onSurfaceVariant `#C6C6C6`, error `#FFB4AB` |
| Wallpaper colors | primary `#3A8CBD`, secondary `#022443`, tertiary `#000000` (unused in System mode, which reads OS tokens) |
| Font scale / weight adjustment | 1.0 / 0 |
| Animator / transition / window scales | 1 / 1 / 1 |
| Inversion / color correction | readable, unset (off) |
| Extra dim (`reduce_bright_colors_activated`) | **DENIED** (not `@Readable`) |
| High text contrast | readable, `0` |
| 24-hour / layout direction / locale | false / LTR / `en-US-u-mu-celsius` |
| Recommended a11y timeout for 6 s content | 6000 ms (no user timeout set) |

### 4.5 Power, memory, haptics, flashlight, audio

| Value | Pixel 8 |
|---|---|
| Battery saver / thermal / headroom (10 s) | off / 0 (none) / 0.58 |
| Standby bucket / background-restricted | 10 (active) / false |
| RAM | 7573 MB total; memory class 256 MB (large 512); not low-RAM |
| Vibrator | 1 vibrator, amplitude control, resonant 147.4 Hz, Q 12.9, envelope effects unsupported |
| Primitives (all supported) | CLICK 26 ms, **THUD 300 ms**, SPIN 133, QUICK_RISE 150, SLOW_RISE 500, QUICK_FALL 100, TICK 5, LOW_TICK 12 |
| Predefined effects | CLICK, DOUBLE_CLICK, TICK, HEAVY_CLICK all supported |
| Touch feedback setting | `haptic_feedback_enabled = 1` |
| Flashlight | back camera: **strength max 45, default 21** (rule F1: slider) |
| Audio | ringer mode **vibrate**; 4 outputs; spatializer unavailable on speaker |

### 4.6 Notifications, System UI, security

| Value | Pixel 8 |
|---|---|
| DND filter | 1 (all) |
| `canPostPromotedNotifications()` | true (for the probe) |
| Lock screen | `lock_screen_show_notifications = 1`, **`lock_screen_allow_private_notifications = 0`** (sensitive content hidden), silent hidden; device secure |
| Heads-up | `heads_up_notifications_enabled = 1` (readable) |
| Notification history | on |
| Status bar | `status_bar_show_battery_percent = 1`; `icon_blacklist` and `clock_seconds` readable |
| Themed icons provider | **denied**: Pixel Launcher's provider needs `BIND_WALLPAPER` or `…nexuslauncher.permission.GRID_CONTROL` |
| QPR2 status bar settings | Not on this build (17.0) |
| Advanced Protection | `AdvancedProtectionManager` present; `isAdvancedProtectionEnabled() = false` |
| Install source (adb) | initiating `com.android.shell`, `PACKAGE_SOURCE_OTHER` |
| Media output dialog receiver | SystemUI `MediaOutputDialogReceiver` **exported, no permission** |
| Readable settings the app can enumerate | 81 system, 244 secure, 295 global keys (names only, via the content provider) |

### 4.7 Fonts

| Family | Pixel 8 | Emulator |
|---|---|---|
| `google-sans`, `-text`, `-medium`, `-bold`, `-flex`, `-clock`, `-text-medium` | **resolve** (non-default, `getSystemFontFamilyName()` returns the family) | fall back to default |
| `google-sans-display` | falls back | falls back |
| `roboto-flex` | resolves | resolves |
| Files | `/product/fonts/GoogleSans-Regular.ttf`, `-Italic`, `GoogleSansFlex-Regular.ttf`, `GoogleSansClock-Regular.ttf`, `GoogleSansFlexClock-Regular.ttf` | Roboto, RobotoFlex only |
| `/product/etc/fonts_customization.xml` | readable; defines Material 3 Expressive role families `variable-body-*`, `variable-display-*` | not readable |

## 5. Pixel 8 geometry from the Section 7.2 formulas

All in px at density 2.625, from the measured inputs above.

| Element | Formula | Pixel 8 |
|---|---|---|
| Ring gap g | 3 dp | 7.875 |
| Idle/compact height Hc | 2r + 2g | **88.25** (33.6 dp), fits inside the 132 px status bar |
| Lw = Tw | round(1.4 × Hc) | 124 |
| Compact width | Hc + Lw + Tw | **336.25**, x 371.6–707.9 |
| Camera keep-out | r + 4 dp | radius 46.75 |
| Minimal bubble | diameter Hc, gap 6 dp | 88.25, gap 15.75 |
| Expanded width We | screen − 2 × 12 dp | 1017 |
| Detents S / M / L | 0.23 / 0.43 / 0.52 × We | 233.9 / 437.3 / 528.8 |
| Expanded top edge | cy − Hc / 2 | 21.6 |
| Expanded corner Re | R − m | 72.5 (27.6 dp) |
| Inner radius | Re − 16 dp | 30.5 |

The default compact overlaps the status-bar content regions by 107 px on each side (see [ADR-004](adr/ADR-004-status-bar-collision.md)).

## 6. [VERIFY] resolution

| # | Claim (spec section) | Result | Evidence | Consequence |
|---|---|---|---|---|
| V1 | Camera in use by any app is detectable with `CameraManager.AvailabilityCallback` (§6.6.3) | **TRUE** | Emulator: `onCameraUnavailable` fired when the Camera app opened camera 1; no permission needed | Use it. Also use the public `getPrivacyIndicatorBounds()` for where the dot is |
| V2 | Mic in use by any app is detectable with `getActiveRecordingConfigurations()` (§6.6.3) | **PENDING** (device) | Callback is registered; no cross-app recording could be triggered on the emulator | Test with the Recorder app on the Pixel |
| V3 | A long-lived service context sees new system colors (§7.1) | **TRUE, with two corrections** | Emulator: after a palette change the service got `onConfigurationChanged` (diff `0x80000000`, asset paths) about 190 ms later, and its own `Resources`, the app context and a fresh `createConfigurationContext` all returned the new colors | (a) **The contrast listener fires about 110 ms before resources update** (colors are still old), so treat contrast and wallpaper callbacks as hints and rebuild on the configuration change, with a +500 ms re-read. (b) **`Resources.getSystem()` never sees theme overlays**; don't use it |
| V4 | Themed-icons state is readable (§7.5) | **FALSE** | Pixel 8 and emulator: the launcher provider needs `BIND_WALLPAPER` or `GRID_CONTROL`; no Settings key for it in SDK 36 | Offer a user toggle (the spec's own fallback). P1.11 is unaffected |
| V5 | Resume last media via `MediaBrowserService` recent root (§8) | **FALSE as a dependable path; alternative found** | Media3's docs call `onPlaybackResumption()` only for "a Bluetooth device or the Android System UI resumption feature"; the recent-root hint targets System UI and apps may reject other callers | Resume with the public `AudioManager.dispatchMediaKeyEvent(KEYCODE_MEDIA_PLAY)`, which the system routes to the last media-button app (the headset path, survives reboot). Verify with Spotify / YouTube Music / Pocket Casts / Libby in Phase 3. P0.3 is kept |
| V6 | Output-switcher intent (§8) | **PARTLY TRUE** | Public `MediaRouter2.showSystemOutputSwitcher()` (API 34) returns true and opens SystemUI's output dialog, but **scoped to the caller** (emulator: our own package, not the media app). For another app, SystemUI's `LAUNCH_MEDIA_OUTPUT_DIALOG` broadcast with `package_name` works: the emulator opened YouTube's output dialog. The receiver is exported with no permission on Android 16 QPR2 and 17. `Settings.Panel.ACTION_VOLUME` opens the public volume panel on both | Use the broadcast with the media session's package; fall back to the volume panel. It isn't public API, so wrap it and health-check it |
| V7 | Live Update promoted flag and ProgressStyle extras (§8, API 36) | **TRUE** | Emulator 36.1: the system sets `FLAG_PROMOTED_ONGOING` (0x40000) and the listener sees it. `Notification.Builder.recoverBuilder(ctx, n).getStyle()` rebuilds `ProgressStyle` completely. Keys: `android.progress`, `android.progressMax`, `android.progressIndeterminate`, `android.progressSegments`, `android.progressPoints`, `android.progressTrackerIcon`, `android.styledByProgress`, `android.shortCriticalText`, `android.requestPromotedOngoing`, `android.template = android.app.Notification$ProgressStyle` | Parse through public getters, not raw keys |
| V8 | MetricStyle extras (§8, API 37) | **PARTLY** | `Notification.MetricStyle` exists, with value types `Notification.Metric.FixedInt`, `FixedFloat`, `FixedText`, `FixedDate`, `FixedTime`, `TimeDifference` (developer.android.com reference). Exact extras keys and the metric limit need the API 37 SDK | Read it through `recoverBuilder().getStyle()` as with ProgressStyle. Confirm in Phase 1 with `:lab` compiled against 37, on the Pixel 8 |
| V9 | `AdvancedProtectionManager` access (§8) | **TRUE** | API 36; `isAdvancedProtectionEnabled()` works with `QUERY_ADVANCED_PROTECTION_MODE` declared; false on the Pixel 8 and emulator | Onboarding and rule S3 |
| V10 | `Build.VERSION.SDK_INT_FULL` is API 36 (§9.2) | **TRUE** | `api-versions.xml`; 3700000 on Pixel 8, 3600001 on the emulator | Use it for minor-SDK gates |
| V11 | `PIXEL_EXPERIENCE` feature check (§9.2) | **TRUE** | Pixel 8 true, emulator false | Pixel check = Google manufacturer AND that feature |
| V12 | Peak refresh setting readable (§9.2) | **TRUE** | `peak_refresh_rate` and `min_refresh_rate` are `@Readable`; Pixel 8 reads `Infinity` / `0.0` | Rule R1 |
| V13 | Inversion, color correction, extra dim readable (§9.2) | **PARTLY**: inversion and daltonizer TRUE; **extra dim FALSE** | `@Readable` in SDK 36 source; `reduce_bright_colors_activated` throws on the Pixel 8 and emulator | No rule depends on extra dim; drop it from the probe and Device Report |
| V14 | Lock-screen notification privacy keys readable (§9.2) | **TRUE** | `lock_screen_show_notifications`, `lock_screen_allow_private_notifications` (`@Readable @SystemApi`); Pixel 8 reads `1` / `0` | §6.6.6 privacy path is live on your phone |
| V15 | QPR2 status-bar settings detectable (§9.2) | **TRUE via SDK level; per-toggle state FALSE** | QPR2 is minor SDK **37.2** (QPR2 API diff "37.1 → 37.2"), so `SDK_INT_FULL >= 3700002` detects it. In AOSP 17 only `status_bar_show_battery_percent` is `@Readable`; `status_bar_show_vibrate_icon` exists but isn't; the notification-icon mode key and a deep-link intent are unknown (QPR2 source isn't public) | Rule S2 shows the card on 37.2+ and opens Settings (deep link resolved at runtime if one exists). Your phone is on 37.0 |
| V16 | Themed icons detectable (§9.2) | **FALSE** | Same as V4 | Toggle |
| V17 | Advanced Protection detectable (§9.2) | **TRUE** | Same as V9 | Rule S3 |
| V18 | Global heads-up toggle restores across crash/uninstall on Android 17 (D7) | **PARTLY** | SystemUI honours `heads_up_notifications_enabled = 0` on 16 QPR2 (emulator screenshots). Restoring after uninstall is impossible (no callback). Android 17 not flipped on your phone | [ADR-007](adr/ADR-007-heads-up-duplication.md): stays P2 with a warning |
| V19 | `preferredDisplayModeId` / `preferredRefreshRate` honoured for accessibility overlays (D11) | **PENDING** (device) | Harness built and dry-run on the emulator (60 Hz only) | [ADR-011](adr/ADR-011-refresh-boost.md) |
| V20 | `cmd thermalservice override-status` (Phase 7) | **TRUE** | Emulator: statuses 3 and 1, then `reset` (back to 0), each reached the app's `OnThermalStatusChangedListener` | Phase 7 forced-condition runs |
| V21 | Seed codenames (§9.3) | **TRUE** (all 20) | Google's Android 17 / QPR2 download tables | Add Pixel 10a `stallion`; Pixel 11 / 11 Pro / 11 Pro XL / 11 Pro Fold `cubs` / `grizzly` / `kodiak` / `yogi` (press reading of Google's factory-image list). Pixel 11a `formosan` is an unreleased leak, so it is left out |

## 7. Section 3.3 platform facts

| # | Fact | Result | Notes |
|---|---|---|---|
| 1 | Android 16 Live Updates and ProgressStyle; QPR1 status-bar chip | TRUE | ProgressStyle, `FLAG_PROMOTED_ONGOING`, `hasPromotableCharacteristics()` are API 36. The emulator (16 QPR2) shows the status-bar chip for our promoted notification. **Media is not eligible for Live Updates** (press) |
| 2 | Android 17 MetricStyle | **TRUE** (class and value types) | See V8; the three-metric limit is still to confirm against the SDK |
| 3 | Promotion criteria incl. `POST_PROMOTED_NOTIFICATIONS` | PARTLY | Our promoted notification (ongoing, title, ProgressStyle, importance DEFAULT, requested) was promoted. **`POST_PROMOTED_NOTIFICATIONS` and `Builder.setRequestPromotedOngoing` are not in the API 36.0 SDK**; the method exists at runtime on 36.1 |
| 4 | Android 17 AAPM revokes accessibility from non-tool apps | **TRUE** | Since Android 17 stable (2026-06-16; first in Beta 2). Turning AAPM on adds the user restriction `no_non_tool_accessibility_service`, removes blocked services from `enabled_accessibility_services` and refuses to bind them. **The whole package is blocked if any of its services isn't a tool.** No per-app override (enterprise allowlists excepted). After AAPM is turned off, services have to be re-enabled by hand (read from code, untested). Play only allows `isAccessibilityTool` for disability-assistance apps |
| 5 | Android 17 QPR2 status bar settings | **TRUE** | Added in QPR2 Beta 4 (2026-08-28): Settings > System > Status bar; notification icons All / Priority only (default) / None; Mute, Vibrate, Battery % toggles. QPR2 is at Beta 6 (CP41.260831.007, 2026-09-24); stable expected December |
| 6 | QPR2 Dynamic Color expansion | **TRUE** | Added in QPR2 Beta 3 (2026-08-14): color slider plus Neutral / Soft / Bright / Bold. SystemUI in AOSP 17 accepts `theme_style` EXPRESSIVE, SPRITZ, TONAL_SPOT, FRUIT_SALAD, RAINBOW, VIBRANT, MONOCHROMATIC (anything else falls back to TONAL_SPOT). How the four new names map to those values is unconfirmed, and it doesn't matter to us: System mode reads the generated `android.R.color.system_*` tokens |
| 7 | Compose-first, M3 Expressive MotionScheme, 35 shapes, morphing | PARTLY | The APIs exist but **only in material3 1.5.0 alphas**; stable 1.4.0 has none. [ADR-013](adr/ADR-013-material3-expressive-dependency.md) |
| 8 | Pixel 11 launched 2026-08-12, centered punch-hole | **TRUE** (date); centered hole seen on 11 and 11 Pro XL | Announced 2026-08-12, on shelves 2026-08-20, Tensor G6. Google's spec pages don't describe the cutout; review photos show a top-center hole on the 11 and 11 Pro XL (11 Pro and Fold not checked). Rule G1/G2 decide at runtime anyway |
| 9 | Only `TYPE_ACCESSIBILITY_OVERLAY` draws over the status bar | **TRUE** | Emulator screenshots: an app overlay renders below the status-bar icons; the accessibility ring renders above them. [ADR-001](adr/ADR-001-overlay-window-type.md) |
| 10 | Pixel 8 display and hardware | **TRUE** | Measured: 1080 × 2400, 420 dpi (xdpi 428.6), about 6.17 in; 60/120 Hz two-mode panel; Tensor G3; 7.4 GB usable of 8 GB; first API 34. Google's specs: 6.2 in, 428 ppi, 8 GB, 7 years of updates (to October 2030). Google lists plain OLED for the 8 and LTPO only for the 8 Pro; XDA's teardown says LTPS |

## 8. Corrections to the spec

1. **Gate D2 option B isn't internal API.** `AttachedSurfaceControl.setTouchableRegion` is public since API 33 and gives exact pass-through for accessibility overlays (50/50 on the emulator). [ADR-002](adr/ADR-002-window-sizing-and-touch.md)
2. **Stable material3 has no Expressive API** (`MotionScheme`, `MaterialShapes`, `MaterialExpressiveTheme`); only 1.5.0 alphas do. [ADR-013](adr/ADR-013-material3-expressive-dependency.md)
3. **Ratings in §3.1**: Material Capsule is 4.7 (not 4.4), dynamicSpot 4.4 (not 4.6).
4. `POST_PROMOTED_NOTIFICATIONS` and `setRequestPromotedOngoing` are 36.1 (minor SDK), not 36.0.
5. §7.1's "rebuild on any event from a fresh configuration context" reads stale colors when triggered by the contrast listener. Rebuild on the configuration change instead (V3).
6. §7.5 themed icons can't be detected (V4); extra dim can't be read (V13).
7. The Material 3 color utilities in §8's stack have no official artifact; MaterialKolor 5.0.1 is the maintained port.

## 9. New findings that change the design

| # | Finding | Evidence | Where it lands |
|---|---|---|---|
| F1 | Public touchable region works for accessibility overlays | 50/50, 50/50, control 0/50 | ADR-002 option C |
| F2 | `WindowInsets.getPrivacyIndicatorBounds()` is public and gives the privacy-dot zone (Pixel 8: x 810–1026) | Probe | Guard §6.6.3, ADR-004 |
| F3 | SystemUI's status-bar dimensions are readable without window content (Pixel 8: content inset 70 + 11 px per side, icon size 58, clock 37, privacy dot 16) | Probe | ADR-004 option C |
| F4 | SystemUI draws its own Live Update chip at the start (left) of the status bar whenever a notification is promoted (x 158–332 on the emulator; its position next to the clock on the Pixel 8 is still to be measured). It takes status-bar width and duplicates what the island shows | Emulator screenshot | ADR-004; Settings explains the duplication |
| F5 | Force-stopping the app disables its accessibility service (removed from `enabled_accessibility_services`) | Emulator | Health screen (§8) must detect and deep-link |
| F6 | Windows added through the accessibility `WindowManager` inherit the accessibility layer, even `TYPE_APPLICATION_OVERLAY` | Emulator screenshots | ADR-001 |
| F7 | Contrast callback precedes the resource update; `Resources.getSystem()` is stale | Emulator logs | Theme engine (Phase 5) |
| F8 | `Display.getShape()` exposes the real, non-circular screen corner curve | Pixel 8 probe | Option: offset that path for a truly concentric expanded outline (Phase 2) |
| F9 | Google Clock 9.0 (2026-08-03) posts timers and stopwatches as Live Updates on Pixel | Press, via competitor research | Phase 4 timers: parse them as Live Updates first |
| F10 | Cutout can vanish at runtime from every app's view while the device still has one (emulator overlay switch) | Emulator | Rule G3 fallback + profile `cutoutKind` sanity check (ADR-012) |
| F11 | Your lock screen hides sensitive content; your theme is Monochromatic; you are in vibrate mode | Pixel 8 probe | Test the §6.6.6 path; D10 screenshots; the right-side icon budget |
| F12 | `AccessibilityService.isUiContext()` is false, yet `getCurrentWindowMetrics()` works from it | Emulator | `:core:device` can probe from the service |
| F13 | The window-state event gives the foreground package with `getSource()` null, which is enough for camera-app auto-hide | Emulator (`com.android.camera2`) | Auto-hide (§6.6.5) |
| F14 | Google Sans and Google Sans Flex are OFL on Google Fonts (Flex since 2025-11-18) | Research pass | Generic-tier fallback can bundle Google Sans Flex instead of Roboto Flex (ADR-006) |
| F15 | `adb install` is not a restricted-settings source (package source OTHER); only local-file and downloaded-file installs are. Android 16 blocks granting accessibility during a call with a non-contact | AOSP 17 code via mirror; probe install source | Sideload via adb (ADR-008); onboarding explains the call-time block |
| F16 | The Android 17 system contact picker (`ContactsPickerSessionContract.ACTION_PICK_CONTACTS`, API 37) returns selected fields without `READ_CONTACTS`; below 37, `ACTION_PICK` grants temporary access to the picked contact | developer.android.com | Tap Card contact shortcuts (P1.7) need no contacts permission on any API level |
| F18 | An accessibility overlay above the status bar receives **no status-bar insets** (`isVisible(statusBars())` false, top 0, even ignoring visibility). Its own insets can't detect fullscreen apps | Emulator, prototype | Fullscreen auto-hide (§6.6.5) needs another signal; open item. Camera-app and landscape auto-hide work |
| F17 | Your phone runs the August build; the September stable build for Pixel 8 is CP3A.260905.009 (Android 17 QPR1, released 2026-09-15) | Pixel Community post | Measurements here are on 37.0. Re-probe after you update (rule 9.5 handles it) |

## 10. Competitor teardown (Phase 0 extension)

Data from Play listings, Data safety pages and 2025–2026 reviews, read on 2026-09-25. Reddit was not reachable, so XDA and Pixel Community threads stand in until you paste the thread.

| | Material Capsule | dynamicSpot | Dynamic Notch |
|---|---|---|---|
| Package | `com.pryshedko.mtisland` | `com.jamworks.dynamicspot` | `com.dynamicisland.notchscreenview` |
| Installs / rating | 500K+ / **4.7** (6.16K) | 10M+ / **4.4** (71.1K) | 5M+ / 4.6 (227K) |
| Last update / version | 2026-08-16 / 15.7 | **2025-08-20 / 2.01 (stale)** | 2026-09-14 / 6.4 |
| Money | IAP (about $3.30 lifetime), no ads label | Ads + IAP (weekly subscription pushed) | Ads + IAP up to $104.99; ads after purchase |
| Data safety | Shares crash logs and device IDs (analytics, advertising); collects contacts; **can't delete** | Shares diagnostics, device IDs, app interactions for advertising | Claims nothing shared or collected, but **not encrypted** and can't delete; privacy policy contradicts it |
| Android 17 Advanced Protection | Blocked; developer refuses to claim `isAccessibilityTool` (Play risk) | Blocked | Blocked |
| Renders Live Updates / ProgressStyle / MetricStyle | No | No | No |

Claim checks against §3.1 (details in the research transcript):
- **Material Capsule:** most weaknesses confirmed by single reviews (Spotify has no compact progress bar, a flash on expand, no "play last media", a thick border). Call support is the top request (about two dozen of 180 reviews). Also no Maps or Live Updates, no multiple activities, and a timer card that broke after Google Clock 9.0.
- **dynamicSpot:** the visualizer fails with Bluetooth, Libby and Adaptive Sound (confirmed by its developer); it covers status icons; it stops working after days. The screen-record overlap is unconfirmed; the banking-app refusal has only 2024 evidence. Also: 400–600 MB RAM and a 120 → 90 Hz drop reported.
- **Dynamic Notch:** ads after lifetime purchase, "aggressive" permissions, and the not-encrypted declaration are confirmed. Play Protect warnings are only partly confirmed ("Google warns it's dangerous").

Top user wishes across sources: calls; Maps, ride and delivery Live Updates; several activities with swipe between them; an always-on pill; foldable positions; landscape; adjustable timings; timers from any clock app; hiding status icons while expanded; settings backup.

Top complaints: Advanced Protection; the system popup plus the island (duplicates); covered status icons; the visualizer; timers breaking with Clock changes; purchase restore; ad-heavy onboarding; abandonment; RAM, refresh and battery cost; banking-app flags.

Related projects worth knowing: LiveBridge (GPL-3.0; re-posts any app's notifications as system Live Updates, so it works without accessibility), LiveMedia (media in the system chip), and M3 Island (a new open-source Material 3 island; first PR merged 2026-09-25, unvetted).

**Behavior matrix (media, notification, call, timer, charging, expand/collapse recordings): not started.** It needs the three apps installed on your Pixel 8 from Play, which only you can do (see section 13).

## 11. Proposal from the research (needs your call, not in scope yet)

**An Advanced Protection mode.** Every accessibility-based island dies under AAPM, but promoted Live Updates don't. While AAPM is on, Aperture could re-post the activities it would have shown (media, calls, timers) as its own promoted Live Updates, using `POST_PROMOTED_NOTIFICATIONS`. They'd render in SystemUI's chip, with no accessibility and no overlay. This keeps "honest degradation" (the island itself is still off and says why), but the user still gets glanceable status. It would be P1 at the earliest. The spec currently says AAPM is unfixable, so this is flagged here, not built.

## 12. Emulator plan for Phase 1 (disk is the constraint)

Phase 1 needs Pixel 9, Pixel Tablet (G3) and corner-cutout (G2) AVDs. The only system image installed is API 36.1, and each extra image is about 1.5–2 GB plus up to 6 GB of AVD data, against 13 GiB free. Plan: reuse the one API 36.1 image for all three, as hardware profiles (Pixel 9 size and density; Tablet with no cutout; phone with the corner-cutout overlay), each created fresh and deleted after use. Reboot the AVD after switching cutout overlays (F10). Installing platform 37 (about 70 MB) is also needed for compileSdk 37.

## 13. Open items blocking Phase 0 acceptance

1. **Device session on the Pixel 8** (V2, V8, V19, the ring check and the font check): reconnect the phone, give the probe notification access and accessibility, and tap Allow for notifications. Then `spikes/phase0-probe/device_session.sh` runs the rest in one pass.
2. **MetricStyle details** (V8): exact keys and limit, from the API 37 SDK in Phase 1 (installing platform 37 is about 70 MB).
3. **Competitor behavior matrix**: needs the three apps installed from Play on your phone.
4. **Reddit thread**: needs you to paste it.
5. **`[PACKAGE]`**: needs your choice.
