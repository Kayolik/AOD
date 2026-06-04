package com.example.kayolikAOD;

import android.content.res.Resources;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AODHook implements IXposedHookLoadPackage {

    private static final String TAG = "AODHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String pkg = lpparam.packageName;

        // --- Framework / SystemUI ---
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) {
            hookAmbientDisplay(lpparam);
            hookSettingsSecure(lpparam);
            hookResources(lpparam);
        }

        // --- SystemUI ---
        if (pkg.equals("com.android.systemui")) {
            hookDozeParameters(lpparam);
            hookBatteryController(lpparam);
            hookMotorolaDoze(lpparam); // specyficzne dla Motoroli
        }

        // --- Settings ---
        if (pkg.equals("com.android.settings")) {
            hookSettingsControllers(lpparam);
        }
    }

    private void hookAmbientDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] classes = {
            "android.hardware.display.AmbientDisplayConfiguration",
            "com.android.hardware.display.AmbientDisplayConfiguration" // fallback
        };

        for (String clsName : classes) {
            try {
                Class<?> cls = XposedHelpers.findClass(clsName, lpparam.classLoader);

                // Hookujemy wszystkie metody zaczynające się na "alwaysOn"
                for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                    String name = m.getName();
                    if (name.startsWith("alwaysOn") || name.contains("AlwaysOn")) {
                        try {
                            XposedHelpers.findAndHookMethod(cls, name, m.getParameterTypes(),
                                XC_MethodReplacement.returnConstant(true));
                            log("OK " + clsName + "." + name);
                        } catch (Throwable t) {
                            log("SKIP " + name + ": " + t.getMessage());
                        }
                    }
                }
                return; // sukces, nie szukaj dalej
            } catch (Throwable ignored) {}
        }
    }

    private void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        // Settings.Secure.getInt(ContentResolver, String, int)
        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getInt",
                android.content.ContentResolver.class, String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[1];
                        if ("doze_always_on".equals(key) || "doze_pulse_on_pick_up".equals(key)) {
                            param.setResult(1);
                        }
                    }
                });
            log("OK Secure.getInt");
        } catch (Throwable t) {
            log("FAIL Secure.getInt: " + t.getMessage());
        }

        // getIntForUser
        try {
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getIntForUser",
                android.content.ContentResolver.class, String.class, int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[1];
                        if ("doze_always_on".equals(key)) param.setResult(1);
                    }
                });
            log("OK Secure.getIntForUser");
        } catch (Throwable t) {
            log("FAIL Secure.getIntForUser: " + t.getMessage());
        }
    }

    private void hookResources(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("android.content.res.Resources", lpparam.classLoader,
                "getBoolean", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Resources res = (Resources) param.thisObject;
                        int id = (Integer) param.args[0];
                        try {
                            String name = res.getResourceName(id);
                            if (name != null && (
                                name.contains("config_dozeAlwaysOn") ||
                                name.contains("config_dozeAfterScreenOff") ||
                                name.contains("config_ambientDisplayAvailable"))) {
                                param.setResult(true);
                                log("RES " + name + " -> true");
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            log("OK Resources.getBoolean");
        } catch (Throwable t) {
            log("FAIL Resources: " + t.getMessage());
        }
    }

    private void hookDozeParameters(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] classes = {
            "com.android.systemui.statusbar.phone.DozeParameters",
            "com.android.systemui.doze.DozeParameters", // fallback
            "com.motorola.systemui.doze.DozeParameters"  // Motorola-specific
        };

        for (String cls : classes) {
            try {
                XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, "getAlwaysOn",
                    XC_MethodReplacement.returnConstant(true));
                log("OK " + cls + ".getAlwaysOn");
                return;
            } catch (Throwable t) {
                log("SKIP " + cls);
            }
        }
    }

    private void hookBatteryController(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.policy.BatteryControllerImpl",
                lpparam.classLoader, "isAodPowerSave",
                XC_MethodReplacement.returnConstant(false));
            log("OK BatteryControllerImpl.isAodPowerSave=false");
        } catch (Throwable t) {
            log("SKIP BatteryController");
        }
    }

    private void hookMotorolaDoze(XC_LoadPackage.LoadPackageParam lpparam) {
        // Motorola często używa własnych klas w SystemUI
        String[] motorolaClasses = {
            "com.motorola.systemui.doze.MotoDozeService",
            "com.motorola.systemui.doze.MotoDozeParameters",
            "com.android.systemui.doze.DozeServiceHost",
            "com.android.systemui.statusbar.phone.DozeServiceHost"
        };

        String[] methods = {"isAlwaysOn", "getAlwaysOn", "isAodActive", "shouldShowAod", "isAmbientDisplayAvailable"};

        for (String cls : motorolaClasses) {
            for (String method : methods) {
                try {
                    XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, method,
                        XC_MethodReplacement.returnConstant(true));
                    log("OK " + cls + "." + method);
                } catch (Throwable t) {
                    // silent skip — OEM klasy często nie istnieją
                }
            }
        }
    }

    private void hookSettingsControllers(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] controllers = {
            "com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController",
            "com.android.settings.display.AmbientDisplayWhenToShowPreferenceController",
            "com.motorola.settings.display.AmbientDisplayPreferenceController" // Motorola
        };

        for (String controller : controllers) {
            try {
                XposedHelpers.findAndHookMethod(controller, lpparam.classLoader,
                    "getAvailabilityStatus",
                    XC_MethodReplacement.returnConstant(0)); // AVAILABLE = 0
                log("OK " + controller);
            } catch (Throwable t) {
                log("SKIP " + controller);
            }
        }
    }

    private void log(String msg) {
        XposedBridge.log(TAG + " " + msg);
    }
}