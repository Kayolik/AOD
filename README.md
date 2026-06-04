# AOD Enabler

[![Release v1.8.1](https://img.shields.io/badge/release-v1.8.1-d4922a)](../../releases/latest)
[![Android 15](https://img.shields.io/badge/Android-15-3ddc84?logo=android)](https://www.android.com/)
[![LSPosed](https://img.shields.io/badge/LSPosed-Vector-2196f3)](https://github.com/JingMatrix/Vector/releases)
[![Moto Edge 40 Neo](https://img.shields.io/badge/tested-Moto%20Edge%2040%20Neo-orange)](https://www.motorola.com/)
[![License](https://img.shields.io/badge/license-credit--required-yellow)]()

Xposed module that unlocks the native Always-On Display on Motorola Hello UI devices where the OEM has hidden it. Patches `Settings.Secure`, `Resources`, and the SystemUI Doze pipeline so the AOD toggle appears in **Settings → Display** and actually sticks.

Built and tested daily on a **Moto Edge 40 Neo** running Android 15 (SukiSU + ZygiskNext + LSPosed Vector).

---

## Contents

- [What it does](#what-it-does)
- [What's new in 1.8.1](#whats-new-in-181)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [How it works](#how-it-works)
- [Build it yourself](#build-it-yourself)
- [Troubleshooting](#troubleshooting)
- [Security](#security)
- [License](#license)
- [Credits](#credits)

---

## What it does

Forces AOD on, even when Motorola's framework reports it as unavailable. Concretely:

| Layer | What gets patched |
|-------|-------------------|
| `Settings.Secure` | `doze_always_on` and `doze_pulse_on_pick_up` always read as `1` |
| `AmbientDisplayConfiguration` | Every `alwaysOn*` / `*AlwaysOn*` method returns `true` |
| `Resources.getBoolean` | `config_dozeAlwaysOn*`, `config_dozeAfterScreenOff`, `config_ambientDisplayAvailable` always `true` |
| `DozeParameters.getAlwaysOn` | Always `true` (stock + Motorola variant) |
| `BatteryControllerImpl.isAodPowerSave` | Always `false` |
| `AmbientDisplay*PreferenceController` | `getAvailabilityStatus` always `0` (AVAILABLE) — surfaces the toggle in Settings |

No background services, no native blobs, no config files. Hooks only fire when the hooked process loads — zero battery cost when the phone is awake.

---

## What's new in 1.8.1

- Removed dead diagnostic code (`HookStatus`, `HookStatusStore`) that was never wired up.
- Rewrote the in-app panel: no more neon "ring + lock" hero, no debug rows. Clean dark layout with device info, force-AOD button, and logcat helper.
- Panel text translated to English.
- Dropped `android.permission.INTERNET` from the manifest — the panel is 100% local now.
- Trimmed `MainActivity` to the methods the panel actually calls.
- All previous functionality preserved.

---

## Requirements

- **Android 15** (API 35)
- **Root** — tested on **[SukiSU](https://github.com/SukiSU-Ultra/SukiSU-Ultra)**. *Should* work on Magisk / KernelSU / APatch, but I only run SukiSU, so your mileage may vary.
- **[ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext)** — required for Zygisk module loading under SukiSU
- **[LSPosed Vector](https://github.com/JingMatrix/Vector/releases)** — the LSPosed fork that actually loads modules on modern setups. Grab the latest release APK.

If your setup is anything other than SukiSU + ZygiskNext + LSPosed Vector, you're in uncharted territory. PRs welcome if you make it work elsewhere.

---

## Installation

1. Grab `AOD-Enabler-v1.8.1.apk` from [Releases](../../releases).
2. Install it. **Don't open it yet.**
3. Open **LSPosed Manager** → **Modules** → enable **AOD Enabler**.
4. **Scope** — check all three:
   - ✅ **System Framework** (`android`)
   - ✅ **SystemUI** (`com.android.systemui`)
   - ✅ **Settings** (`com.android.settings`)

   Don't tick the AOD Enabler app itself — that's only the config panel.
5. **Reboot.** Hooks load during system startup, not at runtime.
6. Open the app. Verify the panel loads with your device info.

If the **Force AOD** button in the panel returns `No permission`, grant `WRITE_SECURE_SETTINGS` from a PC:

```bash
adb shell pm grant com.example.kayolikAOD android.permission.WRITE_SECURE_SETTINGS
```

### Build it yourself

```bash
git clone https://github.com/Kayolik/AOD.git
cd AOD
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug     # Windows
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Gradle Wrapper is included — no local Gradle install needed. Android Studio also works: just open the folder and let it sync.

---

## Usage

1. Open the AOD Enabler app.
2. Check the **Device** card — `doze_always_on` should be `1` after boot.
3. If not, tap **Write doze_always_on = 1**. (Requires `WRITE_SECURE_SETTINGS`, see above.)
4. Go to **Settings → Display → Always-on display** — the toggle should be there and on.
5. Done. The phone now shows AOD with your stock clock.

To uninstall cleanly: disable the module in LSPosed → reboot → uninstall the APK. Don't just remove the APK — orphaned hooks in SystemUI can cause weirdness.

---

## How it works

The module implements `IXposedHookLoadPackage` and dispatches on `lpparam.packageName`:

```
android  (System Framework)  → AmbientDisplayConfiguration, Settings.Secure, Resources
com.android.systemui         → DozeParameters, BatteryControllerImpl, Motorola Doze
com.android.settings         → AmbientDisplay*PreferenceController
```

Each hook is wrapped in its own `try/catch` — a missing class on a future build of SystemUI logs a `SKIP` line and moves on, instead of crashing boot.

Debug logs are tagged `AODHook`:

```bash
adb logcat -s AODHook:*
```

You should see lines like:

```
AODHook OK android.hardware.display.AmbientDisplayConfiguration.alwaysOnAvailable
AODHook OK Secure.getInt
AODHook OK com.android.systemui.statusbar.phone.DozeParameters.getAlwaysOn
AODHook OK com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController
```

If you only see `SKIP` lines for one scope, that scope's classes don't match your SystemUI build. Open an issue with the logcat output.

---

## Troubleshooting

**Module doesn't show up in LSPosed Manager**
Install the APK first, *then* open LSPosed. Order matters.

**Hooks don't fire**
Check `adb logcat -s AODHook:*`. If you see nothing at all, the module is probably disabled or scope is wrong. If you see only `SKIP` lines, your SystemUI build has different class names — the hook targets need adapting.

**AOD toggle still missing in Settings**
Make sure scope includes `com.android.settings`. That's what makes the preference controller report `AVAILABLE`.

**AOD turns off after reboot**
The hook should make it stick. If it doesn't, run the **Write doze_always_on = 1** button in the panel.

**Bootloop after enabling**
Reboot into recovery → wipe Dalvik cache, or boot to safe mode (hold volume down) → disable the module in LSPosed → reboot. Always have a recovery plan before testing.

**Panel is blank**
Update Android System WebView from the Play Store. Some OEM ROMs ship broken WebView implementations.

---

## Security

- ✅ Fully open source. No obfuscation, no native blobs.
- ✅ Zero network permissions (removed in 1.8.1).
- ✅ Zero telemetry, analytics, or third-party SDKs.
- ✅ No persistent services, no background activity, no broadcast receivers.
- The panel is a single local HTML file served from `assets/`.

Every Java source file is under 250 lines. Read them:
[`app/src/main/java/com/example/kayolikAOD/`](app/src/main/java/com/example/kayolikAOD/)

If you don't trust a prebuilt APK, build it yourself — the output will match the source bit-for-bit.

---

## License

Do whatever you want with this code. Fork it, modify it, ship it in your ROM, sell it — I don't care.

**One condition:** if you use this code or any part of it, **credit the original work**. Don't pretend you wrote it from scratch.

No warranty. No liability. It's a system-level hook — if it bricks your phone, that's on you. You knew the risks when you installed LSPosed.

---

## Credits

- **Kayolik** — code, testing on real hardware, frustration
- **[SukiSU](https://github.com/SukiSU-Ultra/SukiSU-Ultra)** — the root solution I run daily
- **[ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext)** — Zygisk injection that makes LSPosed work on SukiSU
- **[LSPosed Vector](https://github.com/JingMatrix/Vector)** — the LSPosed framework fork that loads this module
- **AI tools** — assisted with code structure and logic, but every hook was tested on an actual Moto Edge 40 Neo

---

*Made for the Moto Edge 40 Neo. Java, LSPosed, SukiSU, and too much coffee.*
