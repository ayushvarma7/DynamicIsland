# Aperture privacy policy

Effective date: 25 September 2026

Aperture is a free app for Pixel phones that shows a small live island around the front camera. It is built so that your information stays on your phone.

## The short version

- Aperture collects no personal data and shares nothing with anyone.
- It has no internet permission, so it cannot send anything off your phone.
- It has no account, no ads, no analytics and no tracking.

## What Aperture uses on your phone, and why

**Accessibility service.** Aperture uses the Accessibility API for two things only:

1. To draw the island above the status bar, around the camera. On Android, only an accessibility service can draw there.
2. To learn the name of the app that is in front, so the island can step aside in camera apps and while you are in the app it is showing.

Aperture does not read, collect or store what is on your screen or what you type. Its accessibility configuration turns off access to window content (`canRetrieveWindowContent="false"`) and it listens only for "window changed" events. Before you turn the service on, the app explains this and asks you to agree.

**Notification access and media sessions.** Aperture reads notifications from the apps you choose, and the media sessions of apps that play music, so it can show what is playing, timers, deliveries, rides, downloads and calls. It reads them in memory to draw the island. It never stores their content and never sends it anywhere. Buttons on the island (play, pause, answer, reply) pass your action straight to the app that posted the notification.

**Your app list.** The app picker shows the apps installed on your phone so you can choose which may appear on the island. The list is read on your phone and is not stored or shared.

**Settings.** Your choices (style, timing, gestures, chosen apps) are saved in Aperture's private storage on your phone. They are excluded from Android cloud backup and device-to-device transfer. Uninstalling Aperture deletes them.

## Feedback

If you tap **Send feedback to the developer**, Aperture opens your own email app with a draft addressed to the developer. The draft includes a short device report: phone model, Android version, the island's measurements and your Aperture settings. Nothing is sent unless you choose to send that email, and you can read and edit it first. Emails you send are used only to answer you and fix problems.

## Children

Aperture is not directed at children and collects no data from anyone.

## Changes

If this policy changes, the new version will be published here with a new effective date.

## Contact

Ayush Varma, varma.ay@northeastern.edu
