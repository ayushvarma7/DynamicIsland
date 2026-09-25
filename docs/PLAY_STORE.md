# Publishing Aperture on Google Play

Everything needed for the Play Console, in the order the Console asks for it.

## 1. Build the release bundle

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :core:test :app:lintRelease :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab`.

Signing: `keystore.properties` in the repo root (gitignored) points at the upload key in `~/.android-keys/aperture-upload.jks`. Enroll in **Play App Signing** when creating the app, so Google holds the app signing key and this is only the upload key. **Back up the .jks file and keystore.properties somewhere safe** (a password manager). If the upload key is lost, it can be reset through Play support, but that takes days.

Before every new upload, raise `versionCode` (and `versionName`) in `app/build.gradle`.

To use your own key instead, create `keystore.properties` with:

```
storeFile=/absolute/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## 2. Store listing

- **App name:** Aperture: Camera Island (avoid "Dynamic Island" in the title or graphics; it is an Apple trademark)
- **Short description (80 max):** A live island around your Pixel camera for music, timers, calls and deliveries.
- **Full description:**

> Aperture turns the camera hole on your Pixel into a small live island for the things worth a glance, in your phone's own colors.
>
> • Music: YouTube Music, Spotify and any player. Album art, progress, play, pause, skip and the output picker, right at the top of the screen.
> • Timers and stopwatches, counting down around the camera.
> • Deliveries, rides and downloads from Android Live Updates.
> • Calls you can answer, decline or hang up from the island.
> • Five looks for the expanded card: Black, Glass, Clear, Album art and Tonal.
> • Screen care for OLED: the pill shrinks back to a thin ring around the camera after a few seconds.
> • Tap to expand, long-press to open the app, swipe to change song.
> • Quiet by design: only music, timers, deliveries and calls. Nothing from the app you are using.
>
> Private by design. Aperture has no internet permission, no account, no ads and no tracking. It never reads what is on your screen. Pause or uninstall it any time in one tap.
>
> Aperture uses the Accessibility API only to draw the island above the status bar and to know which app is in front, so the island can step aside. It does not read screen content.

- **App icon:** `docs/play/icon-512.png`
- **Feature graphic:** `docs/play/feature-graphic-1024x500.png`
- **Phone screenshots:** `docs/play/phone-1.png` to `phone-7.png` (1200 x 2400, 2:1)
- **Category:** Personalization
- **Contact email:** varma.ay@northeastern.edu
- **Privacy policy URL:** https://github.com/ayushvarma7/DynamicIsland/blob/main/PRIVACY.md

## 3. App content

**Privacy policy:** the URL above.

**Ads:** No, the app contains no ads.

**App access:** All functionality is available without special access. No login.

**Content rating:** answer the questionnaire as a Utility / Personalization app with no user-generated content, no sharing, no location, no purchases. Expected rating: Everyone.

**Target audience:** 18 and over (or 13 and over). Not designed for children.

**Data safety:**

- Does the app collect or share any of the required user data types? **No.**
- Reason: the app has no internet permission, so no data leaves the device. Notification content, media info and the foreground app name are processed only in memory on the device (Play's definition of "collected" excludes data processed only on the device).
- Is data encrypted in transit? Not applicable, nothing is transmitted.
- Can users request deletion? Uninstalling deletes all app data.

**Accessibility API declaration** (Policy > App content > Sensitive permissions and APIs):

- Is the app an accessibility tool? **No** (`isAccessibilityTool="false"`).
- Core functionality: drawing a status-bar overlay (the dynamic island) around the front camera. `TYPE_ACCESSIBILITY_OVERLAY` is the only window type Android allows above the status bar.
- Data accessed: the package name of the foreground window, from `TYPE_WINDOW_STATE_CHANGED` events. `canRetrieveWindowContent="false"`, so no screen content is read.
- Why: to hide the island in camera apps and while the user is in the app whose media the island shows.
- Prominent disclosure: shown in onboarding and on the home screen before opening Accessibility settings, with an explicit **Agree** button (`Permissions.confirmOverlay`).
- Video: Play asks for a short video of the disclosure and the feature. Record the phone with `adb shell screenrecord`: open the app, tap Continue on "Let the island draw", show the dialog, tap Agree, enable the service, return, then play music and leave the player so the island appears.

**Notification listener:** no Console declaration is needed, but the purpose is disclosed in onboarding and in the privacy policy.

**Permissions used:**

| Permission | Why |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` (service) | Draw the island above the status bar; foreground app name |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (service) | Music, timers, deliveries and calls for the island |
| `VIBRATE` | Haptic feedback on island gestures |
| `QUERY_ADVANCED_PROTECTION_MODE` | Tell the user when Android 17 Advanced Protection blocks the island |
| `REQUEST_DELETE_PACKAGES` | "Uninstall Aperture" opens Android's own uninstall confirmation for Aperture itself |

There is no `INTERNET` and no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. The "keep running" step opens Aperture's App info page, where the user sets battery use to Unrestricted.

## 4. Release

1. Create the app in the Play Console (free app, default language English).
2. Fill in the Store listing and App content sections above.
3. Testing: new personal developer accounts must run a **closed test with at least 12 testers for 14 days** before production access. Upload the AAB to a closed testing track first.
4. After the closed test, apply for production and roll out.

## Checklist

- [x] targetSdk 37, minSdk 34
- [x] Release build minified and resource-shrunk, lint clean
- [x] Signed with the upload key
- [x] No internet permission, no ads, no analytics
- [x] Prominent disclosure with explicit consent before Accessibility settings
- [x] No restricted battery-optimisation permission
- [x] Backup and device transfer excluded
- [x] Predictive back supported
- [x] Privacy policy published in the repo and linked from Help
- [ ] Play Console: listing, content rating, data safety, accessibility declaration and video
- [ ] Closed test with 12 testers for 14 days
