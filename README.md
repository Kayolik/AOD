# AOD Hook

[![Android](https://img.shields.io/badge/Android-15-green?logo=android)](https://www.android.com/)
[![LSPosed](https://img.shields.io/badge/LSPosed-Required-blue)](https://github.com/LSPosed/LSPosed)
[![Device](https://img.shields.io/badge/Tested-Moto%20Edge%2040%20Neo-orange)](https://www.motorola.com/)

Xposed module for Motorola Hello UI that lets you actually control your Always On Display instead of living with whatever Motorola decided you get.

Built for the **Moto Edge 40 Neo** running Android 15. Might work on other Hello UI devices — if it does, let me know. If it doesn't, logs or PRs welcome.

---

## What it does

- **Custom clock layout** — Move the AOD clock where you want it, not where Motorola put it.
- **Burn-in protection** — OLED shifting so your screen doesn't get ghosted after 6 months.
- **Brightness & color control** — AOD doesn't have to be blinding at 3 AM. Adjust it.
- **Web config panel** — Local HTML interface, no internet, no accounts, no bullshit. Just a clean settings page.
- **Zero background junk** — Only hooks when AOD is actually active. No persistent services eating your battery.

---

## Screenshots

> Coming soon. If you build it before I add them, you'll see the panel yourself.

---

## Compatibility

| Device | ROM | Android | Status |
|--------|-----|---------|--------|
| Moto Edge 40 Neo | Hello UI | 15 | ✅ Daily driver |
| Other Moto (Hello UI) | Hello UI | 15 | 🟡 Should work, untested |
| Anything else | AOSP/Custom | 15+ | 🟡 No guarantees |

This hooks SystemUI AOD classes specific to Hello UI. If Motorola changed the class names on your build, it won't work and you'll need to adapt the hooks.

---

## Requirements

- Android 15
- LSPosed (Zygisk) 1.9.2+
- Magisk 26+ with Zygisk enabled
- Root (obviously, it's LSPosed)

---

## Installation

1. Download APK from [Releases](../../releases) or build it yourself (recommended).
2. Install it. **Don't open yet.**
3. LSPosed Manager → Modules → enable AOD Hook.
4. Scope: check **System UI** (required). System Framework optional if you want deeper hooks.
5. Reboot.
6. Open the app, configure, done.

### Build it yourself

```bash
git clone https://github.com/Kayolik/AOD.git
cd AOD
./gradlew assembleDebug    # Linux/Mac
gradlew.bat assembleDebug  # Windows
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Gradle Wrapper is included. You don't need Gradle installed locally. Android Studio will also handle it if you just open the folder.

---

## Security

This module is **fully open source**. No network permissions. No telemetry. No obfuscated code. No native blobs. Just Java hooks and a local HTML file for settings.

You can read every line in [`app/src/main/java/com/example/kayolikAOD/`](app/src/main/java/com/example/kayolikAOD/). If you don't trust prebuilt APKs, build it yourself — the output should match.

---

## Troubleshooting

**Module not showing in LSPosed?**
Install the APK first, then enable in LSPosed, then reboot. Order matters.

**Changes not applying?**
Force-stop SystemUI or reboot. Hello UI caches AOD layouts aggressively.

**SystemUI crash / bootloop?**
Boot with volume down held to enter Safe Mode, disable the module in LSPosed, reboot normally. Always have a recovery plan before testing new builds.

**Config panel blank?**
Update your WebView implementation in Android settings. Some OEM WebViews are broken.

---

## License / Usage

Do whatever you want with this code. Fork it, modify it, ship it in your ROM, sell it — I don't care.

**One condition:** if you use this code or any part of it, **credit the original work**. Don't pretend you wrote it from scratch. That's it.

No warranty. No liability. If this bricks your phone, that's on you. It's a system-level hook — you knew the risks when you installed LSPosed.

---

## Credits

- **Kayolik** — code, testing on real hardware, frustration
- **LSPosed team** — the framework that makes this possible
- **AI tools** — assisted with code structure and logic, but every hook was tested on an actual Moto Edge 40 Neo

---

*Made for the Moto Edge 40 Neo. Java, LSPosed, and too much coffee.*
