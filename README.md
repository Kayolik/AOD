# AOD Enabler

[![Release](https://img.shields.io/badge/release-v1.8.1-2ea44f)](../../releases/latest)
[![Android](https://img.shields.io/badge/Android-15-3ddc84?logo=android)](https://www.android.com/)
[![LSPosed](https://img.shields.io/badge/LSPosed-Vector-2196f3)](https://github.com/JingMatrix/Vector/releases)
[![License](https://img.shields.io/badge/license-credit--required-yellow)]()

Xposed module that unlocks the native Always-On Display on Motorola Hello UI devices where the OEM has hidden it. Patches `Settings.Secure`, `Resources`, and the SystemUI Doze pipeline so the AOD toggle appears in **Settings → Display**.

Built and tested daily on a **Moto Edge 40 Neo** running Android 15 with SukiSU + ZygiskNext + LSPosed Vector.

> [!NOTE]
> The module does **not** add a clock layout, burn-in shifting, or a brightness control. It only unlocks the toggle that Motorola disabled. If you want fancy AOD rendering, use a separate clock app.

---

## What it does

| Layer | What gets patched |
|-------|-------------------|
| `Settings.Secure` | `doze_always_on` and `doze_pulse_on_pick_up` always read as `1` |
| `AmbientDisplayConfiguration` | Every `alwaysOn*` / `*AlwaysOn*` method returns `true` |
| `Resources.getBoolean` | `config_dozeAlwaysOn*`, `config_dozeAfterScreenOff`, `config_ambientDisplayAvailable` always `true` |
| `DozeParameters.getAlwaysOn` | Always `true` (stock + Motorola variant) |
| `BatteryControllerImpl.isAodPowerSave` | Always `false` |
| `AmbientDisplay*PreferenceController` | `getAvailabilityStatus` always `0` (AVAILABLE) — surfaces the toggle in Settings |

No background services. No native blobs. Hooks only fire when the hooked process loads.

---

## Requirements

- **Android 15** (API 35)
- **Root** — tested on **[SukiSU](https://github.com/SukiSU-Ultra/SukiSU-Ultra)**[^1]
- **[ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext)** — required for Zygisk module loading under SukiSU
- **[LSPosed Vector](https://github.com/JingMatrix/Vector/releases)** — the LSPosed fork that loads modules on modern setups

> [!WARNING]
> If your setup is anything other than SukiSU + ZygiskNext + LSPosed Vector, you're in uncharted territory. PRs welcome.

---

## Installation

1. Grab `AOD-Enabler-v1.8.1.apk` from [Releases](../../releases).
2. Install it. **Don't open it yet.**
3. Open **LSPosed Manager** → **Modules** → enable **AOD Enabler**.
4. **Scope** — check all three:
   - ✅ **System Framework** (`android`)
   - ✅ **SystemUI** (`com.android.systemui`)
   - ✅ **Settings** (`com.android.settings`)

   Do not tick the AOD Enabler app itself — that's only the config panel.
5. **Reboot.** Hooks load during system startup, not at runtime.
6. Open the app. Verify the panel loads with your device info.

### Build it yourself

```bash
git clone https://github.com/Kayolik/AOD.git
cd AOD
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug     # Windows
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

The Gradle wrapper is included — no local Gradle install needed.

---

## The panel

After install, the app shows a single local HTML page with three sections:

| Section | What it shows |
|---------|---------------|
| **Device** | Model, codename, manufacturer, Android version. Read directly from `Build.*` via a `JavascriptInterface` bridge. |
| **Setup** | Static three-step instruction. |
| **Debug** | A live `pre`-formatted log box. Useful for pasting adb output without alt-tabbing to a terminal. |

The whole panel is one file: [`app/src/main/assets/index.html`](app/src/main/assets/index.html). It's a `WebView` served from `file:///android_asset/`, so it works completely offline.

---

## Troubleshooting

**Module doesn't show in LSPosed Manager**

Install the APK first, *then* open LSPosed. Order matters.

**Hooks don't fire after reboot**

```bash
adb logcat -s AODHook:*
```

If you see nothing, the module is disabled or scope is wrong. If you see only `SKIP` lines, your SystemUI build has different class names and the hook targets need adapting.

**AOD toggle missing in Settings**

Make sure `com.android.settings` is in scope. That's what makes the preference controller report `AVAILABLE`.

**Bootloop after enabling**

Reboot into recovery → wipe Dalvik, or boot to safe mode (hold volume down) → disable the module in LSPosed → reboot normally. Always have a recovery plan before testing.

**Panel is blank**

Update Android System WebView from the Play Store. Some OEM ROMs ship broken WebView implementations.

---

## What it looks like

The panel is intentionally bare — no hero, no glow, no animated rings:

```
AOD Enabler
v1.8.1 · LSPosed module · Android 15
─────────────────────────────────────────
Device
  Model                moto edge 40 neo
  Codename             manaus
  Manufacturer         motorola
  Android              Android 15 (API 35)
─────────────────────────────────────────
Setup
  1. Enable AOD Enabler in LSPosed Manager.
  2. Scope: System Framework, SystemUI, Settings.
  3. Reboot the device.
  4. Open Settings > Display. The AOD toggle should be there.
─────────────────────────────────────────
Debug
  $ adb logcat -s AODHook:*
  [12:34:56] panel loaded
─────────────────────────────────────────
github.com/Kayolik/AOD · updates
```

---

## Security

- ✅ Fully open source. No obfuscation, no native blobs.
- ✅ Zero network permissions (removed in v1.8).
- ✅ Zero telemetry, analytics, or third-party SDKs.
- ✅ No persistent services, no broadcast receivers, no IPC.
- The panel is a single static HTML file loaded from `assets/`.

Every Java file fits in your head in one sitting — read them here:
[`app/src/main/java/com/example/kayolikAOD/`](app/src/main/java/com/example/kayolikAOD/)

If you don't trust a prebuilt APK, build it yourself.

---

## Changelog

### v1.8.1 (current)

- [x] Removed AOD brightness control (hooks didn't catch on Hello UI — see `AODHook#hookAmbientDisplay` discussion in the [code](app/src/main/java/com/example/kayolikAOD/AODHook.java))
- [x] Removed `Force AOD` card and `forceEnableAOD` / `getDozeAlwaysOnValue` bridge methods (replaced by hooks)
- [x] WebView: external `http(s)` links now open in system browser via `Intent.ACTION_VIEW`
- [x] Panel rewritten in monospace, no AI-dashboard gradients or glow
- [x] `MainActivity` trimmed to 4 bridge methods (down from 11)

### v1.8

- [x] Removed dead `HookStatus` / `HookStatusStore` code (never wired up)
- [x] Dropped `android.permission.INTERNET` — the panel is 100% local
- [x] Panel text translated to English

---

## License

Do whatever you want with this code. Fork it, modify it, ship it in your ROM, sell it — I don't care.

**One condition:** if you use this code or any part of it, **credit the original work**. Don't pretend you wrote it from scratch.

No warranty. No liability. It's a system-level hook — if it bricks your phone, that's on you.

---

## Credits

- **Kayolik** — code, testing on real hardware, frustration
- **[SukiSU](https://github.com/SukiSU-Ultra/SukiSU-Ultra)** — the root solution I run daily
- **[ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext)** — Zygisk injection for SukiSU
- **[LSPosed Vector](https://github.com/JingMatrix/Vector)** — the LSPosed fork that loads this module
- **AI tools** — assisted with code structure, but every hook was tested on an actual Moto Edge 40 Neo

---

*Made for the Moto Edge 40 Neo. Java, LSPosed, SukiSU, and too much coffee.*

[^1]: *Should* work on Magisk / KernelSU / APatch, but I only run SukiSU, so your mileage may vary.
