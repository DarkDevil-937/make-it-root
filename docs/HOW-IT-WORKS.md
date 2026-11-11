# How it works

## Overview

Make it ROOT is a thin Android wrapper around the **GhostLock** kernel exploit
(CVE-2026-43499). The exploit is compiled as a native armv7 binary and shipped in
the APK as `lib/armeabi-v7a/libglroot.so`.

The app itself does not contain kernel code. It:

1. starts the bundled binary as a child process;
2. waits for the binary to escalate its own credentials and start a root daemon;
3. talks to that daemon using the same binary in client mode (`--su ...`).

## The app flow

```
MainActivity  --ROOT NOW-->  RootService
                                  |
                                  | exec libglroot.so --tries 6
                                  v
                         GhostLock exploit
                                  |
                    patch creds -> uid 0
                                  |
                    set SELinux permissive
                                  |
                    install /data/local/tmp/gl/su
                                  |
                    fork root daemon (@ghostlock_root)
                    fork bind shell (localhost only)
                                  |
                                  v
                   RootService polls: libglroot.so --su -c id
                                  |
                            "ROOTED" shown
```

The app polls for up to two minutes and refreshes the on-screen status every two
seconds for the first 30 seconds.

## Why the exploit is unstable

GhostLock is a temporary privilege-escalation exploit. It works by corrupting
kernel state:

- it redirects the `ashmem_misc_fops` pointer to a fake fops page backed by
  AF_UNIX socket buffers;
- it abuses PI-futex state to get a write primitive.

After it grants root, the kernel still contains fake/corrupted objects. Those
objects "decay" as the system uses the affected paths (notably `/dev/ashmem`,
which Android uses for graphics). This is why:

- the device can hang and the MTK watchdog reboots it after a minute or two;
- keeping the exploit parent alive keeps the fake page valid but the state still
  decays;
- killing the daemons frees the fake page and can trigger the reboot sooner.

**STOP ROOT** kills the root daemons and removes `su`. The resulting reboot is
the clean way to return to a normal, unrooted device.

Auto-root on boot is intentionally disabled: because of the instability above,
auto-root produces an endless root -> hang -> reboot loop.

## Permanent root

For **permanent, reboot-proof root** you need to unlock the bootloader. On this
device that means the **kamakiri** bootrom exploit, which requires a real USB
host (a PC, laptop, Raspberry Pi, etc.) with:

- raw root or udev access to `/dev/bus/usb`;
- a CDC-ACM serial driver;
- Python with `pyserial` and `pyusb`.

A phone whose root is confined (no `CAP_SYS_ADMIN`, no `/dev/bus/usb`) cannot be
the USB host. High-level procedure (not included in this repository):

1. Back up the boot chain (lk, tee, boot, recovery, preloader, GPT).
2. Downgrade the preloader to the vulnerable version for your build.
3. Short the bootrom test point and run the kamakiri `bootrom-step.sh`, then
   `fastboot-step.sh`.
4. Flash TWRP, then **Magisk v25.2** (v26+ watchdog-bootloops on the Fire OS 7
   4.4 kernel).

This is risky and can brick the device. The device-specific preloader image and
partition backups are **not** included in this repository.

## References

- CVE-2026-43499 (GhostLock)
- Fire TV Stick 3rd gen: `sheldonp` / `AFTSSS`
- Fire OS 7.7.1.7 build `PS7717/5746` (`0036408029828`)
