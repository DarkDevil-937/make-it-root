#!/usr/bin/env bash
set -euo pipefail

HERE=$(cd "$(dirname "$0")/.." && pwd)
SDK_DIR=${SDK_DIR:-$HERE/sdk}
OUT=${OUT:-$HERE/build}
BIN_SRC=${BIN_SRC:-$HERE/dist/libglroot.so}
AJAR=$SDK_DIR/android.jar
R8=$SDK_DIR/r8.jar
SRC=$HERE/app/src
RES=$HERE/app/res
MANIFEST=$HERE/app/AndroidManifest.xml

[ -f "$AJAR" ] || { echo "missing $AJAR (see docs/BUILDING.md)"; exit 1; }
[ -f "$R8" ]   || { echo "missing $R8 (see docs/BUILDING.md)"; exit 1; }
[ -f "$BIN_SRC" ] || { echo "missing $BIN_SRC"; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/apk" "$OUT/gen"

echo "[1/6] javac"
javac --release 8 -classpath "$AJAR" \
  -d "$OUT/classes" $(find "$SRC" -name '*.java')

echo "[2/6] d8"
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 24 --lib "$AJAR" \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

echo "[3/6] aapt2"
aapt2 compile --dir "$RES" -o "$OUT/res.zip"
aapt2 link -o "$OUT/apk/base.apk" -I "$AJAR" \
  --manifest "$MANIFEST" \
  --java "$OUT/gen" \
  --min-sdk-version 24 --target-sdk-version 28 \
  "$OUT/res.zip"

echo "[4/6] package dex + native lib"
python3 - "$OUT/apk/base.apk" "$OUT/dex/classes.dex" "$BIN_SRC" "$OUT/apk/unsigned.apk" <<'PY'
import sys, zipfile, shutil
base, dex, binpath, outp = sys.argv[1:5]
shutil.copyfile(base, outp)
with zipfile.ZipFile(outp, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(dex, 'classes.dex')
    z.write(binpath, 'lib/armeabi-v7a/libglroot.so')
PY

echo "[5/6] zipalign"
zipalign -p -f 4 "$OUT/apk/unsigned.apk" "$OUT/apk/aligned.apk"

echo "[6/6] sign"
KS=$OUT/debug.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/make-it-root.apk" "$OUT/apk/aligned.apk"
apksigner verify --print-certs "$OUT/make-it-root.apk" | head -5

echo "APK: $OUT/make-it-root.apk"
ls -l "$OUT/make-it-root.apk"
