# Building

You can rebuild the APK from source. Rebuilding the native exploit is optional:
a prebuilt `dist/libglroot.so` is included.

## Prerequisites

- JDK 8 or newer (`javac`, `java`, `keytool`)
- Android SDK build tools: `aapt2`, `zipalign`, `apksigner`
- `d8` (bundled in `r8.jar`)
- `android.jar` for **API 28**
- Optional, only to rebuild the exploit: a clang cross-compiler targeting
  `armv7a-linux-androideabi` (Android NDK, or Termux clang plus an NDK sysroot)

On Termux, install the basics with:

```sh
pkg install openjdk-17 aapt2 zipalign apksigner
```

## Get `android.jar` and `r8.jar`

Put both files in a directory, for example `sdk/`:

- `android.jar` (API 28): from your Android SDK
  (`$ANDROID_HOME/platforms/android-28/android.jar`), or the
  [Sable/android-platforms](https://github.com/Sable/android-platforms) mirror.
- `r8.jar`: from Maven Central / Google Maven,
  `com.android.tools:r8` (the version used here is `9.4.34`).

Example:

```sh
mkdir -p sdk
# android.jar -> sdk/android.jar
# r8.jar      -> sdk/r8.jar
```

## Build the exploit (optional)

If you want to rebuild `libglroot.so` from `exploit/`:

```sh
# Put the cross toolchain where scripts/build-exploit.sh expects it, or edit
# the CC / TC variables at the top of the script.
bash scripts/build-exploit.sh
# -> build/libglroot.so
```

If you skip this step, `scripts/build-apk.sh` uses `dist/libglroot.so`.

## Build the APK

```sh
SDK_DIR="$PWD/sdk" bash scripts/build-apk.sh
# -> build/make-it-root.apk
```

Useful environment variables:

| Variable | Default | Meaning |
|---|---|---|
| `SDK_DIR` | `./sdk` | directory containing `android.jar` and `r8.jar` |
| `BIN_SRC` | `./dist/libglroot.so` | native exploit binary to bundle |
| `OUT` | `./build` | output directory |

The script creates a throwaway debug keystore at `build/debug.keystore` if one
does not exist. **Do not commit keystores.** If you want stable update signing,
create your own key and edit the script, but never publish the private key.

## Install the build

```sh
adb connect <firestick-ip>:5555
adb install -r -g build/make-it-root.apk
```
