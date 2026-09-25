#!/usr/bin/env bash
# Gate D2 spike: 50 taps outside the island + 10 inside, per overlay mode, counting which window got each tap.
# Usage: touch_test.sh <adb-serial> [mode ...]   (modes: resize fixed fixed_noregion; default all three)
# Requires the probe installed with its accessibility service enabled, and ProbeActivity as the touch target.
set -euo pipefail
SERIAL="$1"; shift
MODES=("$@"); [ ${#MODES[@]} -eq 0 ] && MODES=(resize fixed fixed_noregion)
ADB=("$HOME/Library/Android/sdk/platform-tools/adb" -s "$SERIAL")
TAG=APERTURE_PROBE

"${ADB[@]}" shell am start -W -n dev.aperture.spike.probe/.ProbeActivity --es action touch_target >/dev/null
# Safety: injected taps go to whatever is on screen, so refuse unless our own touch target is the resumed activity.
if ! "${ADB[@]}" shell dumpsys activity activities | grep -E "(mResumedActivity|topResumedActivity)" | grep -q "dev.aperture.spike.probe/.ProbeActivity"; then
  echo "ABORT: ProbeActivity is not in front (locked screen or another app?); no taps were sent." >&2
  exit 1
fi
for mode in "${MODES[@]}"; do
  "${ADB[@]}" logcat -c
  "${ADB[@]}" shell am broadcast -a dev.aperture.spike.probe.CMD --es cmd overlay --es mode "$mode" >/dev/null
  "${ADB[@]}" shell sleep 1.5
  geo=$("${ADB[@]}" logcat -d -s "$TAG" | grep overlay_attached | tail -1 | sed -E 's/.*"geometry":\{([^}]*)\}.*/\1/')
  cx=$(echo "$geo" | sed -E 's/.*"cx":([0-9.]+).*/\1/'); cy=$(echo "$geo" | sed -E 's/.*"cy":([0-9.]+).*/\1/')
  r=$(echo "$geo" | sed -E 's/.*"r":([0-9.]+).*/\1/'); g=$(echo "$geo" | sed -E 's/.*"gapPx":([0-9.]+).*/\1/')
  # 50 outside points: a 10x5 grid below the status bar but inside the fixed window's bounds; 10 inside points on the ring.
  cmds=$(python3 - "$cx" "$cy" "$r" "$g" <<'EOF'
import math, sys
cx, cy, r, g = map(float, sys.argv[1:5])
pts = [(int(60 + i * 106), int(260 + j * 150)) for i in range(10) for j in range(5)]
ring = r + g
pts += [(int(cx + 0.6 * ring * math.cos(a)), int(cy + 0.6 * ring * math.sin(a))) for a in [k * math.pi / 5 for k in range(10)]]
print("; ".join(f"input tap {x} {y}" for x, y in pts))
EOF
)
  "${ADB[@]}" logcat -c
  "${ADB[@]}" shell "$cmds"
  "${ADB[@]}" shell sleep 1
  log=$("${ADB[@]}" logcat -d -s "$TAG")
  act=$(echo "$log" | grep -c "touch_activity" || true)
  ovl=$(echo "$log" | grep -c "touch_overlay" || true)
  echo "mode=$mode  outside_taps=50 inside_taps=10  -> activity_received=$act overlay_received=$ovl"
done
