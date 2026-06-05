# AOD Enabler

Xposed module that unlocks Always-On Display on Motorola Hello UI devices where the OEM has hidden it. It does not add a clock layout or burn-in shifting — it just surfaces the toggle that Motorola disabled.

Tested on a Moto Edge 40 Neo running Android 15 with SukiSU + ZygiskNext + LSPosed Vector.

## What it does

- `Settings.Secure.getInt("doze_always_on")` → always `1`
- `AmbientDisplayConfiguration.alwaysOn*` → always `true`
- `Resources.getBoolean(config_dozeAlwaysOn*)` → always `true`
- `DozeParameters.getAlwaysOn` → always `true`
- `BatteryControllerImpl.isAodPowerSave` → always `false`

Each hook is wrapped in its own `try/catch` so a missing class on a future SystemUI build logs a `SKIP` line and moves on instead of crashing boot.

## Install

1. Grab `AOD-Enabler-v1.8.1.apk` from Releases.
2. Install it. Do not open yet.
3. LSPosed Manager → Modules → enable AOD Enabler.
4. Scope: tick **System Framework** and **SystemUI**. Do not tick the AOD Enabler app, do not tick Settings.
5. Reboot.
6. Open the app — you should see your device info load.

That's it. The AOD toggle should now be there in Settings → Display.

### Build it yourself

```
git clone https://github.com/Kayolik/AOD.git
cd AOD
gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The Gradle wrapper is included.

## Debug

If AOD is missing after install, grab the log:

```
adb logcat -s AODHook:*
```

You should see lines like:

```
AODHook OK android.hardware.display.AmbientDisplayConfiguration.alwaysOnAvailable
AODHook OK Secure.getInt
AODHook OK com.android.systemui.statusbar.phone.DozeParameters.getAlwaysOn
```

If you only see `SKIP` lines for one scope, your SystemUI build has different class names. Open an issue with the logcat output.

Bootloop: recovery → wipe Dalvik, or safe mode (volume down at boot) → disable the module in LSPosed → reboot. Have a recovery plan before testing.

## What it does not do

- No clock layout. Stock Motorola AOD rendering.
- No burn-in shifting.
- No brightness control (tried, hooks did not catch on Hello UI).
- No background services, no native blobs, no network.

The panel is a single static HTML file at `app/src/main/assets/index.html`. The whole app is two Java files under 200 lines each. Read it.

## License

Do whatever you want. Fork it, ship it in your ROM, sell it. One condition: credit the original work. No warranty.

## Credits

SukiSU, ZygiskNext, LSPosed Vector. The framework folks did the real work.
