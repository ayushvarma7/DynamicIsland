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

1. Open **Aperture**.
2. Setup > Island overlay > **Open settings**, then turn on **Aperture island**.
3. Setup > Media and notifications > **Open settings**, then allow **Aperture media and notifications**.
4. Optional: **Calibrate** to nudge the ring around your camera, in 0.5 dp steps.
5. Try it: the **Message**, **Timer** and **Charging** buttons show sample activities without any other app.

Installing over adb avoids Android's "restricted settings" block. If you install the APK from a file manager instead, first go to App info > ⋮ > **Allow restricted settings**.

## Gestures

| Gesture | On | Does |
|---|---|---|
| Tap | pill or card | open the app |
| Long-press or swipe down | pill | expand |
| Swipe up | card | collapse or dismiss |
| Swipe sideways | pill | switch between activities (pins your pick for 30 min) |
| Swipe sideways | message card | dismiss the notification |
| Double-tap | idle ring | flashlight (or resume media; set in Behavior) |

## Known limits of this prototype

- **Android 17 Advanced Protection blocks the island.** Android turns off every accessibility service that isn't an accessibility tool. The app detects this and says so.
- **Fullscreen apps:** the island doesn't hide yet in fullscreen video. It does hide in camera apps and in landscape.
- **Duplicate popups:** Android's own heads-up shows too. For apps you watch in the island, turn off Settings > Notifications > the app > Pop on screen.

## Layout

| Path | What |
|---|---|
| `core/` | Pure Java: geometry (Section 7.2 and rules G1–G5), the island engine (priorities, queue, pinning, expiry), playback math. Unit-tested |
| `app/` | The Android app: overlay service and renderer, notification and media sources, settings screens |
| `spikes/phase0-probe/` | Throwaway Phase 0 measuring app |
