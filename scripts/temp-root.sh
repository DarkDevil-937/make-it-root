#!/usr/bin/env bash
# Phone-only temporary root for Fire TV Stick 3rd gen (sheldonp / AFTSSS).
#
# Launches the bundled GhostLock binary from an ADB shell instead of the app.
# The daemon then lives in the uid_0 cgroup, so Android killing the app does
# NOT kill root. This is the most stable temporary-root path.
#
# Usage: bash scripts/temp-root.sh [firestick-ip:5555]
set -euo pipefail

HOST="${1:-10.42.239.197:5555}"
BIN=/data/local/tmp/libglroot.so
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="${BIN_SRC:-$HERE/../dist/libglroot.so}"

adb connect "$HOST" >/dev/null 2>&1 || true

if ! adb -s "$HOST" shell "test -x $BIN" 2>/dev/null; then
    [ -f "$SRC" ] || { echo "missing $SRC"; exit 1; }
    echo "[*] pushing $(basename "$SRC") -> $BIN"
    adb -s "$HOST" push "$SRC" "$BIN" >/dev/null
    adb -s "$HOST" shell "chmod 755 $BIN"
fi

if adb -s "$HOST" shell "$BIN --su -c id" 2>/dev/null | grep -q 'uid=0'; then
    echo "already rooted"
else
    echo "[*] starting exploit (detached, uid_0 cgroup)"
    adb -s "$HOST" shell "setsid $BIN --tries 6 >/data/local/tmp/glroot.log 2>&1 &"
    for _ in $(seq 1 20); do
        sleep 2
        if adb -s "$HOST" shell "$BIN --su -c id" 2>/dev/null | grep -q 'uid=0'; then
            echo "[+] ROOTED (stable ADB path, uid_0 cgroup)"
            break
        fi
    done
fi

if ! adb -s "$HOST" shell "$BIN --su -c id" 2>/dev/null | grep -q 'uid=0'; then
    echo "[-] root failed; see /data/local/tmp/glroot.log"
    exit 1
fi

cat <<EOF
Run root commands:
  adb -s $HOST shell '$BIN --su -c "<command>"'
EOF
