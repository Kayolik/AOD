package com.example.kayolikAOD;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.provider.Settings;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AODHook implements IXposedHookLoadPackage {

    private static final String TAG = "AODHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String pkg = lpparam.packageName;

        if (pkg.equals("android") || pkg.equals("com.android.systemui")) {
            hookAmbientDisplay(lpparam);
            hookSettingsSecure(lpparam);
            hookResources(lpparam);
        }
        if (pkg.equals("com.android.systemui")) {
            hookDozeParameters(lpparam);
            hookBatteryController(lpparam);
            hookMotorolaDoze(lpparam);
            hookAODBrightness(lpparam);
        }
        if (pkg.equals("com.android.settings")) {
            hookSettingsControllers(lpparam);
        }
    }

    private void hookAmbientDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] classes = {
            "android.hardware.display.AmbientDisplayConfiguration",
            "com.android.hardware.display.AmbientDisplayConfiguration"
        };
        for (String clsName : classes) {
            try {
                Class<?> cls = XposedHelpers.findClass(clsName, lpparam.classLoader);
                for (Method m : cls.getDeclaredMethods()) {
                    String name = m.getName();
                    if (name.startsWith("alwaysOn") || name.contains("AlwaysOn")) {
                        try {
                            XposedHelpers.findAndHookMethod(cls, name, m.getParameterTypes(),
                                XC_MethodReplacement.returnConstant(true));
                            log("OK " + clsName + "." + name);
                        } catch (Throwable t) { log("SKIP " + name + ": " + t.getMessage()); }
                    }
                }
                return;
            } catch (Throwable ignored) {}
        }
    }

    private void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getInt",
                ContentResolver.class, String.class, int.class,
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        String key = (String) p.args[1];
                        if ("doze_always_on".equals(key) || "doze_pulse_on_pick_up".equals(key)) p.setResult(1);
                    }
                });
            log("OK Secure.getInt");
        } catch (Throwable t) { log("FAIL Secure.getInt: " + t.getMessage()); }

        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getIntForUser",
                ContentResolver.class, String.class, int.class, int.class,
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if ("doze_always_on".equals(p.args[1])) p.setResult(1);
                    }
                });
            log("OK Secure.getIntForUser");
        } catch (Throwable t) { log("FAIL Secure.getIntForUser: " + t.getMessage()); }
    }

    private void hookResources(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.content.res.Resources", lpparam.classLoader,
                "getBoolean", int.class,
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        Resources res = (Resources) p.thisObject;
                        try {
                            String name = res.getResourceName((Integer) p.args[0]);
                            if (name != null && (name.contains("config_dozeAlwaysOn")
                                || name.contains("config_dozeAfterScreenOff")
                                || name.contains("config_ambientDisplayAvailable"))) {
                                p.setResult(true);
                                log("RES " + name + " -> true");
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            log("OK Resources.getBoolean");
        } catch (Throwable t) { log("FAIL Resources: " + t.getMessage()); }
    }

    private void hookDozeParameters(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] classes = {
            "com.android.systemui.statusbar.phone.DozeParameters",
            "com.android.systemui.doze.DozeParameters",
            "com.motorola.systemui.doze.DozeParameters"
        };
        for (String cls : classes) {
            try {
                XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, "getAlwaysOn",
                    XC_MethodReplacement.returnConstant(true));
                log("OK " + cls + ".getAlwaysOn");
                return;
            } catch (Throwable t) { log("SKIP " + cls); }
        }
    }

    private void hookBatteryController(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.policy.BatteryControllerImpl",
                lpparam.classLoader, "isAodPowerSave",
                XC_MethodReplacement.returnConstant(false));
            log("OK BatteryControllerImpl.isAodPowerSave=false");
        } catch (Throwable t) { log("SKIP BatteryController"); }
    }

    private void hookMotorolaDoze(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] classes = {
            "com.motorola.systemui.doze.MotoDozeService",
            "com.motorola.systemui.doze.MotoDozeParameters",
            "com.android.systemui.doze.DozeServiceHost",
            "com.android.systemui.statusbar.phone.DozeServiceHost"
        };
        String[] methods = {"isAlwaysOn", "getAlwaysOn", "isAodActive", "shouldShowAod", "isAmbientDisplayAvailable"};
        for (String cls : classes) {
            for (String method : methods) {
                try {
                    XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, method,
                        XC_MethodReplacement.returnConstant(true));
                    log("OK " + cls + "." + method);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void hookSettingsControllers(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] controllers = {
            "com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController",
            "com.android.settings.display.AmbientDisplayWhenToShowPreferenceController",
            "com.motorola.settings.display.AmbientDisplayPreferenceController"
        };
        for (String c : controllers) {
            try {
                XposedHelpers.findAndHookMethod(c, lpparam.classLoader, "getAvailabilityStatus",
                    XC_MethodReplacement.returnConstant(0));
                log("OK " + c);
            } catch (Throwable t) { log("SKIP " + c); }
        }
    }

    private int readBrightnessPref() {
        try {
            XSharedPreferences prefs = new XSharedPreferences("com.example.kayolikAOD", "aod_prefs");
            prefs.makeWorldReadable();
            return prefs.getInt("aod_brightness", 100);
        } catch (Throwable t) {
            return 100;
        }
    }

    private void hookAODBrightness(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] dozeClasses = {
            "com.android.systemui.statusbar.phone.DozeParameters",
            "com.android.systemui.doze.DozeParameters",
            "com.motorola.systemui.doze.DozeParameters"
        };
        for (String cls : dozeClasses) {
            try {
                XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, "getScreenBrightness",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            int v = (int) Math.round(readBrightnessPref() * 2.55);
                            p.setResult(v);
                        }
                    });
                log("OK " + cls + ".getScreenBrightness");
            } catch (Throwable t) { log("SKIP " + cls + ".getScreenBrightness: " + t.getMessage()); }
        }

        try {
            XposedHelpers.findAndHookMethod("android.content.res.Resources", lpparam.classLoader,
                "getInteger", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        Resources res = (Resources) p.thisObject;
                        try {
                            String name = res.getResourceName((Integer) p.args[0]);
                            if (name != null) {
                                String lname = name.toLowerCase();
                                if (lname.contains("doze") && lname.contains("brightness")) {
                                    int v = (int) Math.round(readBrightnessPref() * 2.55);
                                    p.setResult(v);
                                    log("RES int " + name + " -> " + v);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            log("OK Resources.getInteger");
        } catch (Throwable t) { log("SKIP Resources.getInteger: " + t.getMessage()); }
    }

    private void log(String msg) { XposedBridge.log(TAG + " " + msg); }
}
