# ADR-007: System heads-up duplication (Gate D7)

- Status: Draft (Phase 0). A for P0; B revisited in P2.
- Date: 2026-09-25

## Options

| | Option |
|---|---|
| A | Guide the user to turn off per-app "Pop on screen" for apps the island handles |
| B | ADB-granted `WRITE_SECURE_SETTINGS`: turn off `Settings.Global.heads_up_notifications_enabled` globally while the island runs, and restore it on disable |

B's criterion: it must restore reliably across crash and uninstall paths on Android 17.

## Measurements

| Check | Result | Evidence |
|---|---|---|
| Readable by apps | Yes (`@Readable`); `"1"` on the Pixel 8 | SDK 36 `Settings.java`; Pixel 8 probe |
| SystemUI honours `0` on Android 16 QPR2 | **Yes**: HUN card shows at `1`, only the status-bar icon at `0` | Emulator API 36.1 screenshots |
| SystemUI honours `0` on Android 17 | Pending. I didn't flip this global setting on your phone | Phase 7 / P2, with your go-ahead |
| Restore after crash | Possible: the next service start (or the watchdog) restores the saved value | Design |
| Restore after uninstall | **Impossible**: there's no uninstall callback for the app being removed | Platform behaviour |
| Restore after force-stop | Only at next launch; force-stop also disables the accessibility service (see ADR-001) | Measured side effect |

Also relevant: the accessibility overlay draws above the system HUN, which is exactly why users see "two popups".

## Provisional decision

A in P0 (listed default). B stays P2, and only with a visible warning that uninstalling without first disabling leaves system heads-ups off, plus a one-tap "restore" in Settings and in the QS tile.
