# Make it ROOT

One-tap **temporary root** for the **Amazon Fire TV Stick 3rd generation** (`sheldonp` / `AFTSSS`) on **Fire OS 7.7.1.7**.

Make it ROOT wraps the GhostLock kernel exploit (CVE-2026-43499) in a small Android app, so you can root your own device without a PC: open the app, press **ROOT NOW**, and a temporary `su` is available.

> **Tested on exactly one model:** Fire TV Stick 3rd gen, model number **AFTSSS** (codename `sheldonp`), Fire OS **7.7.1.7**, build `PS7717/5746` (incremental `0036408029828`). This is the only model and firmware this has been tested on. Do not run it on any other Fire TV model or Fire OS build.

> This is **temporary root**, not a permanent unlock. The device may hang and the watchdog may reboot it a minute or two after rooting. Press **STOP ROOT** when you are done; the stick reboots to clear the exploit state. For permanent, reboot-proof root you need the bootloader unlock (kamakiri + TWRP + Magisk) from a real PC/USB host.

Read [DISCLAIMER.md](DISCLAIMER.md) before using this.

## Features

- One-tap root from the TV: **ROOT NOW**, **CHECK ROOT**, **STOP ROOT**
- No PC required for the temporary root
- Ships the exploit inside the APK (armv7, `sheldonp`)
- Works on a fresh, SELinux-enforcing boot:
  - the app uses its own bundled binary as the root client (`libglroot.so --su ...`), so it does not depend on an external `su` file;
  - the exploit sets SELinux permissive before installing `/data/local/tmp/gl/su`;
  - after opening its ashmem handle, the exploit restores the original `ashmem_misc_fops` pointer so new system `/dev/ashmem` opens are safe.
- No auto-root on boot. This is deliberate: because the exploit is unstable, auto-root causes an endless root -> hang -> reboot loop.

## Supported device

| | |
|---|---|
| Device | Fire TV Stick 3rd gen (`sheldonp`) |
| Model number | **`AFTSSS`** |
| Fire OS | 7.7.1.7 |
| Build | `PS7717/5746`, incremental `0036408029828` |
| Kernel | 4.4.162+ |
| ABI | armv7 |
| Tested | This exact model/build only |

**Only the exact model and build above have been tested.** Other Fire TV models, Fire OS versions, or build numbers are not supported: the exploit uses version-specific kernel offsets and may hang or brick a different device.

## Requirements

- Fire TV Stick 3rd gen on the build above
- ADB debugging enabled (Settings > My Fire TV > Developer Options > ADB debugging)
- The phone/PC and the stick on the same network (for installation)
- Android 7.0+ (API 24) for the app itself

## Install (prebuilt APK)

1. Enable ADB debugging on the Fire TV.
2. From a terminal on the same network:

```sh
adb connect <firestick-ip>:5555
adb install -r -g dist/make-it-root.apk
```

3. On the TV, open **Make it ROOT**.

Or use the helper script:

```sh
bash scripts/install-autoroot.sh <firestick-ip>:5555
```

## Usage

- **ROOT NOW** runs the exploit and installs a temporary `su` at `/data/local/tmp/gl/su`. Takes about 10-15 seconds.
- **CHECK ROOT** shows the current status.
- **STOP ROOT** stops the root daemons and removes `su`. The stick will usually reboot on its own shortly after; that reboot is the clean reset.

From a shell while rooted:

```sh
adb shell /data/local/tmp/gl/su -c id
# uid=0(root) ...
```

Recommended workflow:

```
ROOT NOW  ->  do your task quickly  ->  STOP ROOT (or reboot)
```

## Important limitations

This exploit gives **temporary** root. It corrupts kernel state (the ashmem fops pointer / PI-futex state) and is not designed to stay rooted:

- The device can still hang and the MTK watchdog can reboot it. On the fops-restore build the stick survived ~9 minutes in testing (the earlier build rebooted in ~1-3 minutes), so treat root as a short-lived window.
- Root does not survive a reboot.
- dm-verity still blocks permanent system changes.
- Auto-root on boot is disabled because it caused an endless root -> hang -> reboot loop.

If you need permanent root, use the bootloader unlock (kamakiri) from a PC. See [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md#permanent-root).

## How it works

See [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md).

Short version: the bundled native binary is the GhostLock exploit. The app starts it, then talks to the root daemon it spawns through the same binary in client mode (`--su`). **STOP ROOT** sends a cleanup command to the daemon.

## Build from source

See [docs/BUILDING.md](docs/BUILDING.md). You need the Android SDK build tools (`aapt2`, `d8`, `zipalign`, `apksigner`), an `android.jar` (API 28), and a clang cross-compiler for armv7 to rebuild the exploit. A prebuilt `libglroot.so` is included in `dist/` so the APK can be rebuilt without the cross-compiler.

## Hashes

| File | SHA-256 |
|---|---|
| `dist/make-it-root.apk` | `d7acfcc2b77043d864e82a9af74aedfdde0316c605234fe24dfb9e9f68cd3616` |
| `dist/libglroot.so` | `0c1527673dd9cedd70abeb57f6627c4144bbb5d05a6ca489e6faa782eff2dd97` |

## Credits

- Exploit: **GhostLock** (CVE-2026-43499). The sources under `exploit/` are third-party research code; see [exploit/NOTICE](exploit/NOTICE).
- App, build scripts, and documentation: **DarkDevil-937** ([@DarkDevil-937](https://github.com/DarkDevil-937)).

## License

The app wrapper, scripts, and documentation are released under the MIT License (see [LICENSE](LICENSE)). The exploit sources retain their upstream terms and are included for interoperability on the owner's device.
