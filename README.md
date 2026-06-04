AOD Hook
https://www.android.com/
https://github.com/LSPosed/LSPosed
https://www.motorola.com/
LICENSE
https://developer.android.com/studio
LSPosed Xposed Module for Always On Display (AOD) customization on Motorola Hello UI.
Modify your AOD layout, behavior, and appearance without touching system partitions. Works entirely in-memory via the LSPosed framework.
📋 Table of Contents
Features
Screenshots
Compatibility
Requirements
Installation
Building from Source
Security & Transparency
Troubleshooting
Disclaimer
License
Credits
✨ Features
Custom AOD Layout — Modify the Always On Display clock position, style, and arrangement
Burn-in Protection — Pixel shifting and color adjustments to prevent OLED burn-in
Brightness & Color Tweaks — Adjust AOD brightness levels and accent colors beyond stock limits
Web-based Config Panel — Clean local HTML configuration interface served via WebView (no internet required)
Hello UI Optimized — Tailored for Motorola's Hello UI AOD implementation on Android 15
Zero Background Services — Only active when AOD is triggered; no persistent battery drain
📸 Screenshots
Screenshots and screen recordings will be added soon.
To add your own: create a screenshots/ folder in this repo and link images here.
plain
screenshots/
├── aod_preview.png
├── config_panel.png
└── settings_menu.png
📱 Compatibility
Table
Device	ROM	Android	Status
Motorola Moto Edge 40 Neo	Hello UI	15	✅ Tested & Working
Other Motorola (Hello UI)	Hello UI	15	🟡 Should Work
Other AOSP-based devices	AOSP / Custom	15+	🟡 Untested
Note: This module hooks SystemUI AOD classes specific to Motorola Hello UI. While it may work on other Android 15 devices, your mileage may vary.
📋 Requirements
Table
Component	Minimum Version
Android	15 (API 35)
LSPosed	1.9.2+ (Zygisk recommended)
Magisk	26+ (Zygisk enabled)
Root	Required (via Magisk)
🚀 Installation
Method 1: Pre-built APK (Releases)
Download the latest APK from Releases
Install the APK but do NOT open it yet
Open LSPosed Manager → Modules → enable AOD Hook
Select scope:
✅ System UI (required)
✅ System Framework (optional, for deeper hooks)
Reboot your device
Open the AOD Hook app and configure your settings via the built-in panel
Method 2: Build from Source (Auditable)
This is the recommended method if you want to verify every line of code before installing a system-level hook.
See Building from Source below.
🛠️ Building from Source
This project is 100% reproducible. Anyone can clone and build it to verify the released APK matches the source code exactly.
Prerequisites
Android Studio (latest stable)
Android SDK (auto-downloads via Gradle Wrapper)
Steps
bash
# Clone the repository
git clone https://github.com/Kayolik/AOD.git
cd AOD

# Build debug APK
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug      # Windows (CMD)

# Output:
# app/build/outputs/apk/debug/app-debug.apk
The project includes Gradle Wrapper — no local Gradle installation needed.
🔐 Security & Transparency
This module is fully open source. There is absolutely no:
Network access / internet permissions
Data collection, telemetry, or analytics
Obfuscated or native code blobs
Hardcoded credentials or backdoors
You can audit every line of Java code in app/src/main/java/com/example/kayolikAOD/.
The configuration UI is a local HTML file (assets/index.html) served via WebView — it never connects to any external server.
🐛 Troubleshooting
Table
Issue	Solution
Module not showing in LSPosed	Make sure you installed the APK before enabling it in LSPosed. Reboot after enabling.
AOD not changing after config	Force-stop System UI or reboot. Some Hello UI builds cache AOD layouts aggressively.
SystemUI crashes / bootloop	Boot into Safe Mode (hold volume down during boot) to disable LSPosed modules.
Config panel not loading	Ensure WebView implementation is up to date in Android settings.
⚠️ Disclaimer
Use at your own risk. This module hooks Android SystemUI at runtime. While it does not modify /system partitions directly, a buggy hook can cause SystemUI crashes or soft-bootloops.
Always have a recovery plan before testing: know how to enter Safe Mode or disable LSPosed via file manager in recovery.
The author is not responsible for any damage, data loss, voided warranty, or bricked devices.
This is an unofficial modification not endorsed by Motorola or Google.
📄 License
plain
MIT License

Copyright (c) 2024 Kayolik

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
Full text available in LICENSE file.
🙋 Credits
Author: Kayolik
Framework: LSPosed — the modern Xposed framework for Android
Development assistance: Code structure and logic developed with AI assistance, refined by human testing on real hardware
Built for the Moto Edge 40 Neo. Powered by Java, LSPosed, and late-night debugging.
