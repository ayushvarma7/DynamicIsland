# ADR-008: Distribution (Gate D8)

- Status: Draft (Phase 0).
- Date: 2026-09-25

## Options

`personal-sideload` (the default in Section 0) or `play-store`.

## Phase 0 findings

- The probe installed over adb reports install source `initiating=com.android.shell`, `packageSource=1` (`PACKAGE_SOURCE_OTHER`).
- Android 13+ "restricted settings" (and Android 15's Enhanced Confirmation Mode) block accessibility, notification-listener and overlay grants **only for apps installed from a local file or a downloaded file** (package sources 3 and 4). `adb install` uses source OTHER, which isn't restricted. The Pixel builds inspected ship an empty trusted-installer list. So installing over adb avoids the "Allow restricted settings" step; installing the APK from a file manager would need it. Confirming this on your Android 17 phone happens when you grant the probe.
- Android 16+ blocks granting accessibility during a phone call with someone not in your contacts. Onboarding should say so if the grant fails mid-call.
- Play policy, per the competitor research: only apps that assist people with disabilities may declare `isAccessibilityTool`, and Play Protect warns on misuse. Material Capsule's developer refuses to set it for this reason. We keep `isAccessibilityTool="false"` either way.

## Decision

`personal-sideload`, while staying Play-policy compliant: a prominent disclosure, no `INTERNET`, an honest Data safety answer ("no data collected or shared"), and `isAccessibilityTool="false"`.
