# ADR-009: minSdk, compileSdk, targetSdk (Gate D9)

- Status: Draft (Phase 0).
- Date: 2026-09-25

## Options

minSdk 34 (keeps the API 34 contrast APIs, covers every Pixel 8 build ever shipped) or minSdk 36 (ProgressStyle natively, no SDK checks).

## Phase 0 facts

| Fact | Value | Source |
|---|---|---|
| Your Pixel 8 | Android 17, **API 37.0** (`SDK_INT_FULL` = 3700000), build CP2A.260805.005, patch 2026-08-05 | Probe, `getprop` |
| Emulator image | Android 16 QPR2, API 36.1 (`SDK_INT_FULL` = 3600001) | Probe |
| `Notification.ProgressStyle`, `FLAG_PROMOTED_ONGOING` (0x40000), `hasPromotableCharacteristics()`, `setShortCriticalText`, `NotificationManager.canPostPromotedNotifications()`, `AdvancedProtectionManager` | API 36 | `api-versions.xml` (platform 36) |
| `Notification.Builder.setRequestPromotedOngoing(boolean)`, `Manifest.permission.POST_PROMOTED_NOTIFICATIONS` | **Not in the API 36.0 SDK**; `setRequestPromotedOngoing` exists at runtime on 36.1 | javap on platform 36; reflection on the emulator |
| `View.setRequestedFrameRate`, `WindowInsets.getBoundingRects` | API 35 | `api-versions.xml` |
| `UiModeManager.getContrast/addContrastChangeListener`, `Display.getShape`, `Path.getPathIterator`, `Typeface.getSystemFontFamilyName` | API 34 | `api-versions.xml` |
| Compose UI 1.12 (current stable) | Requires **compileSdk 37** and AGP ≥ 9.2 | Release notes (library research) |
| SDK Platform 37 | Final (37.0 r02, June 2026) | Google SDK repository |

## Decision

**minSdk 34, compileSdk 37, targetSdk 37.** Gate by `SDK_INT` for major levels and by `SDK_INT_FULL` for minor levels (36.1 = 3600001) wherever a minor-SDK API is used. The Live Updates reader needs only API 36 classes; MetricStyle rendering needs 37. The `:lab` app posts promoted notifications, so it declares `POST_PROMOTED_NOTIFICATIONS` and calls `setRequestPromotedOngoing` only on ≥ 36.1.
