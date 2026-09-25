#!/usr/bin/env bash
# One-pass Phase 0 device session. Read-only except for installing/running the probe itself.
# It never grants permissions or changes global settings; it reports which grants are missing and skips
# the tests that need them. Screenshots are cropped to the camera area (no screen content is kept).
# Usage: device_session.sh <adb-serial> <output-dir>
set -euo pipefail
SERIAL="$1"; OUT="$2"; mkdir -p "$OUT"
HERE="$(cd "$(dirname "$0")" && pwd)"
ADB=("$HOME/Library/Android/sdk/platform-tools/adb" -s "$SERIAL")
PKG=dev.aperture.spike.probe
TAG=APERTURE_PROBE
log() { printf '== %s\n' "$*"; }
cmd() { "${ADB[@]}" shell am broadcast -a $PKG.CMD --es cmd "$@" >/dev/null; }

log "install probe"
"${ADB[@]}" install -r "$HERE/app/build/outputs/apk/debug/app-debug.apk" >/dev/null

log "activity probe (Section 9.2 values)"
"${ADB[@]}" shell run-as $PKG rm -f files/probe_activity.json
"${ADB[@]}" shell am start -W -n $PKG/.ProbeActivity --es action probe --ez finish true >/dev/null
for _ in $(seq 1 20); do "${ADB[@]}" shell run-as $PKG test -f files/probe_activity.json && break; sleep 1; done
"${ADB[@]}" exec-out run-as $PKG cat files/probe_activity.json > "$OUT/probe_activity.json"

a11y_on=$("${ADB[@]}" shell settings get secure enabled_accessibility_services | grep -c "$PKG/" || true)
listener_on=$("${ADB[@]}" shell settings get secure enabled_notification_listeners | tr ':' '\n' | grep -c "^$PKG/" || true)
notif_on=$("${ADB[@]}" shell dumpsys package $PKG | grep -c "POST_NOTIFICATIONS: granted=true" || true)
log "grants: accessibility=$a11y_on listener=$listener_on post_notifications=$notif_on"

if [ "$listener_on" -gt 0 ]; then
  log "listener: active notifications (structure only) + media sessions"
  "${ADB[@]}" logcat -c
  "${ADB[@]}" shell am broadcast -a $PKG.LISTENER_CMD >/dev/null
  sleep 3
  "${ADB[@]}" logcat -d -s $TAG | grep -E "listener_(connected|active)|media_sessions" > "$OUT/listener_active.log" || true
  if [ "$notif_on" -gt 0 ]; then
    log "post a test ProgressStyle Live Update and read it back"
    "${ADB[@]}" logcat -c
    "${ADB[@]}" shell am start -W -n $PKG/.ProbeActivity --es action post_live_update --ei progress 55 >/dev/null
    sleep 3
    "${ADB[@]}" logcat -d -s $TAG | grep -E "live_update_posting|listener_posted" > "$OUT/live_update.log" || true
    "${ADB[@]}" shell am start -W -n $PKG/.ProbeActivity --es action cancel_live_update >/dev/null
  fi
fi

if [ "$a11y_on" -gt 0 ]; then
  log "overlay: ring alignment (product style, then debug style), 200% crops around the cutout"
  cmd overlay --es mode resize; cmd style --es style product; sleep 2
  "${ADB[@]}" exec-out screencap -p > "$OUT/.full.png"
  python3 "$HERE/tools/crop.py" "$OUT/.full.png" "$OUT/ring_product_200.png" 480 6 120 120 2
  cmd style --es style debug; sleep 1
  "${ADB[@]}" exec-out screencap -p > "$OUT/.full.png"
  python3 "$HERE/tools/crop.py" "$OUT/.full.png" "$OUT/ring_debug_200.png" 480 6 120 120 2
  rm -f "$OUT/.full.png"
  log "service-context probe"
  "${ADB[@]}" shell run-as $PKG rm -f files/probe_service.json; cmd probe; sleep 3
  "${ADB[@]}" exec-out run-as $PKG cat files/probe_service.json > "$OUT/probe_service.json"
  log "Gate D2 pass-through"
  "$HERE/touch_test.sh" "$SERIAL" | tee "$OUT/touch_test.txt"
  log "Gate D11 cadence, 3 runs per strategy, no touch"
  cmd overlay --es mode fixed; sleep 2
  "${ADB[@]}" logcat -c
  for mode in none lp_rate lp_mode view_high view_hz; do
    for _ in 1 2 3; do sleep 4; cmd anim_test --es mode "$mode"; sleep 4; done
  done
  "${ADB[@]}" logcat -d -s $TAG | grep -E "anim_test|display_changed" > "$OUT/anim_test.log" || true
  cmd overlay --es mode resize; cmd style --es style product
fi
log "done -> $OUT"
