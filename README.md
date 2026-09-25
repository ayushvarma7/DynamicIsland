# Aperture

A Pixel-native dynamic island around your front camera, written in Java.

- **Media:** YouTube Music, Spotify and any app with a media session. Album art, playing bars, progress with tap-to-seek, previous/play/next, the app's own custom action (like / thumbs up), and the system output picker.
- **Messages:** WhatsApp, Messages, Gmail and others pop up in the island with sender and text. Reply inline when the app supports it; tap to open; swipe sideways to dismiss.
- **Calls:** WhatsApp and phone calls with answer, decline and hang up, plus a call timer.
- **Live Updates:** promoted notifications (rides, deliveries), Google Clock timers and stopwatches, downloads.
- **System:** charging and low battery.
- Follows your system colors (Material You, contrast, dark theme) live. Uses Google Sans on Pixel.
- No internet permission, no ads, no analytics, and it never reads screen content.

The spec is [docs/SPEC.md](docs/SPEC.md), research and measurements are in [docs/RESEARCH.md](docs/RESEARCH.md), and decisions are in [docs/adr](docs/adr/README.md).

## Build

Requirements: the Android SDK and a JDK 17+. Android Studio's bundled JBR works.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :core:test assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Install and set up (Pixel)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then, on the phone:

1. Open **Aperture**. Until setup is finished the banner is red and each missing step is marked red.
2. Step 1, **Grant access**: turn on **Aperture island** in Accessibility.
3. Step 2, **Grant access**: allow **Aperture media and notifications**.
4. Step 3 opens by itself: choose which apps may use the island. Suggested apps start switched on.
5. The banner turns green. Optional: **Calibrate** to nudge the ring around your camera in 0.5 dp steps.
6. Try it: the **Message**, **Timer** and **Charging** buttons show sample activities without any other app.

Installing over adb avoids Android's "restricted settings" block. If you install the APK from a file manager instead, first go to App info > ⋮ > **Allow restricted settings**.

## What shows on the island

Only the things worth a glance, and only from the apps you pick in setup:

- **Music and media**, once you leave the player app (nothing shows while the player is on screen).
- **Timers and stopwatches**, **deliveries and rides** (Live Updates), and **downloads**.
- **Calls.**
- Off by default, because they are high-frequency: message pop-ups, and charging/low battery. Turn them on in Behavior.

Nothing from the app you are currently using is shown.

## Screen care (OLED)

A pill that stays on screen for hours wears the OLED around it unevenly. So after a few seconds without change (8 s by default, adjustable, or never), the pill shrinks back to the ring around the camera and shows its live state as a thin arc that moves every second. The compact content also shifts by 1 px each minute. On the lock screen the island shows new events briefly, then steps aside (or shows only calls).

## Gestures

| Gesture | On | Does |
|---|---|---|
| Tap | pill or halo | expand (reversible, keeps you in your app) |
| Long-press | pill or halo | open the app |
| Tap | expanded card | open the app (buttons do their own thing) |
| Swipe down | pill | expand |
| Swipe up | card | collapse or dismiss |
| Swipe sideways | pill | switch between activities (pins your pick for 30 min) |
| Swipe sideways | message card | dismiss the notification |
| Double-tap | idle ring | flashlight (or resume media; set in Behavior) |

Tap and long-press can be swapped in Behavior for iPhone-style behaviour.

## Known limits of this prototype

- **Android 17 Advanced Protection blocks the island.** Android turns off every accessibility service that isn't an accessibility tool. The app detects this and says so.
- **Fullscreen apps:** the island doesn't hide yet in fullscreen video. It does hide in camera apps and in landscape.
- **Duplicate popups:** Android's own heads-up shows too. For apps you watch in the island, turn off Settings > Notifications > the app > Pop on screen.

## Layout

| Path | What |
|---|---|
| `core/` | Pure Java: geometry (Section 7.2 and rules G1-G5), the island engine (priorities, queue, pinning, expiry), playback math. Unit-tested |
| `app/` | The Android app: overlay service and renderer, notification and media sources, settings screens |
| `spikes/phase0-probe/` | Throwaway Phase 0 measuring app |
