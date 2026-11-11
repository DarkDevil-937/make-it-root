#!/usr/bin/env bash
set -euo pipefail

HOST="${1:-}"
HERE="$(cd "$(dirname "$0")" && pwd)"
APK="${APK:-$HERE/../dist/make-it-root.apk}"

[ -n "$HOST" ] || { echo "usage: $0 <firestick-ip>:5555"; exit 1; }
[ -f "$APK" ] || { echo "missing $APK"; exit 1; }
command -v adb >/dev/null || { echo "adb missing (pkg install android-tools)"; exit 1; }

echo "[*] connecting to $HOST"
adb connect "$HOST" >/dev/null 2>&1 || true
adb -s "$HOST" wait-for-device

echo "[*] installing Make it ROOT (com.jhajj.autoroot)"
adb -s "$HOST" install -r -g "$APK"

echo "[*] launching app"
adb -s "$HOST" shell am start -n com.jhajj.autoroot/.MainActivity >/dev/null 2>&1 || true

echo
echo "On the TV, press ROOT NOW (~10-15s)."
echo "When finished, press STOP ROOT (the stick reboots to clear exploit state)."
