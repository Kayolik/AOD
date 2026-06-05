# AOD Enabler

<p align="left">
  <a href="https://github.com/Kayolik/AOD/releases"><img src="https://img.shields.io/badge/version-1.9.0-5cc77a?style=flat-square" /></a>
  <a href="https://developer.android.com/about/versions/15"><img src="https://img.shields.io/badge/android-15%20%28API%2035%29-3ddc84?style=flat-square&logo=android&logoColor=white" /></a>
  <a href="https://github.com/JingMatrix/Vector/releases"><img src="https://img.shields.io/badge/LSPosed-required-orange?style=flat-square" /></a>
  <a href="https://github.com/SukiSU-Ultra/SukiSU-Ultra"><img src="https://img.shields.io/badge/root-SukiSU%20%2F%20KernelSU-red?style=flat-square" /></a>
  <a href="https://github.com/Dr-TSNG/ZygiskNext"><img src="https://img.shields.io/badge/Zygisk-required-blue?style=flat-square" /></a>
</p>

LSPosed module that forces Always-On Display on devices where the OEM hid it.
No custom rendering — hooks only.

---

## What it does

- Adds the **Always-on display** toggle in Settings → Display
- AOD stays active even with Battery Saver enabled
- Works regardless of `doze_always_on` value — reads are intercepted at the framework level, not dependent on Settings.Secure state

---

## Requirements

- Android 15 (API 35)
- Root — [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) or KernelSU
- Zygisk enabled — [ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext)
- [LSPosed Vector build](https://github.com/JingMatrix/Vector/releases)
- AOD-capable display hardware

---

## Install

1. Download `AOD-Enabler-v1.9.0.apk` from [Releases](https://github.com/Kayolik/AOD/releases)
2. Install the APK — enable "Install unknown apps" if prompted
3. Open **LSPosed Manager**
4. Go to **Modules** → enable **AOD Enabler**
5. Open its scope settings:
   - Tick **System Framework**
   - Tick **SystemUI**
   - Do **not** tick AOD Enabler itself
   - Do **not** tick Settings
6. Reboot
7. Settings → Display → **Always-on display** toggle should appear
8. Turn it on

---

## Build from source

```bash
git clone https://github.com/Kayolik/AOD
cd AOD
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

| Requirement | Version |
|---|---|
| JDK | 21 |
| compileSdk | 35 |
| minSdk | 31 (build requirement only) |
| Gradle | 9.4.1 |
| Xposed API | 82 |

---

## How it works

Six hooks across two packages — `android` (framework) and `com.android.systemui`.

### Settings reads

`Settings.Secure.getInt` and `getIntForUser` are intercepted.
Any read of `doze_always_on` or `doze_pulse_on_pick_up` returns `1` — regardless of actual stored value.
The module never writes to Settings.Secure itself; if the user flips the toggle, the system writes normally.

### Resource booleans

`Resources.getBoolean` is intercepted for three config keys:

| Key | Forced value |
|---|---|
| `config_dozeAlwaysOn` | `true` |
| `config_dozeAfterScreenOff` | `true` |
| `config_ambientDisplayAvailable` | `true` |

This makes both the framework and SystemUI treat AOD as available on this device.

### AmbientDisplayConfiguration

All methods whose name starts with `alwaysOn` or contains `AlwaysOn` are replaced to return `true`.
Covers both `android.hardware.display` and `com.android.hardware.display` class paths.

### DozeParameters

`getAlwaysOn` is forced to `true` across three class paths — first found wins:

```
com.android.systemui.statusbar.phone.DozeParameters
com.android.systemui.doze.DozeParameters
com.motorola.systemui.doze.DozeParameters
```

### Battery Saver bypass

`BatteryControllerImpl.isAodPowerSave` → `false`

Without this, Battery Saver would disable AOD regardless of other hooks.

### Motorola-specific doze

Five methods forced to `true` across four classes:

| Class | Methods |
|---|---|
| `com.motorola.systemui.doze.MotoDozeService` | `isAlwaysOn` `isAodActive` `shouldShowAod` |
| `com.motorola.systemui.doze.MotoDozeParameters` | `getAlwaysOn` `isAmbientDisplayAvailable` |
| `com.android.systemui.doze.DozeServiceHost` | `isAlwaysOn` `shouldShowAod` |
| `com.android.systemui.statusbar.phone.DozeServiceHost` | `isAlwaysOn` `shouldShowAod` |

Missing classes are silently skipped — no crash.

---

## Debug

```bash
adb logcat -s AODHook:*
```

| Prefix | Meaning |
|---|---|
| `OK` | Hook registered successfully |
| `SKIP` | Class or method not found — silently ignored |
| `FAIL` | Hook failed (permissions or reflection error) |
| `RES` | Resource boolean intercepted and overridden |

---

## Tested device

| Field | Value |
|---|---|
| Model | motorola edge 40 neo |
| Codename | `manaus` |
| Android | 15 (API 35) |
| Build | `V1TMS35H.3-45-3-2-3` |
| Fingerprint | `motorola/manaus_g_syse/manaus:15/V1TMS35H.3-45-3-2-3/4b40ea-84fcb:user/release-keys` |

---

## Compatibility

Tested on Motorola Edge 40 Neo running Hello UI (Android 15). Hello UI is a Motorola-specific shell introduced in Android 15 — earlier Android versions are not a target.

May work on other Motorola devices on Android 15 where AOD hardware exists but the toggle is hidden. Does not work on devices without AOD-capable display hardware.

---

## Contact

Discord: **kayolikk**

---

[Releases](https://github.com/Kayolik/AOD/releases) · [Source](https://github.com/Kayolik/AOD)
