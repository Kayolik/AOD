package com.example.kayolikAOD;

import android.content.ContentResolver;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Zapis statusu hooków — plik w /data/local/tmp (czytelny dla apki) + SharedPreferences LSPosed.
 */
public final class HookStatusStore {

    public static final String PKG = "com.example.kayolikAOD";
    public static final String PREF_FILE = "aod_module_status";

    private static final String TMP_DIR = "/data/local/tmp";

    public static String globalKey(String scope) {
        return "aod_enabler_hook_" + scope;
    }

    public static File tmpFile(String scope) {
        return new File(TMP_DIR, "aod_enabler_" + scope + ".txt");
    }

    public static File wrongScopeFile() {
        return new File(TMP_DIR, "aod_enabler_wrong_scope.txt");
    }

    public static File seenPackagesFile() {
        return new File(TMP_DIR, "aod_enabler_seen_packages.txt");
    }

    /** Każde wywołanie handleLoadPackage — widać po restarcie, co LSPosed faktycznie hookuje. */
    public static void recordSeenPackage(String pkg, boolean isTarget) {
        try {
            File f = seenPackagesFile();
            FileWriter w = new FileWriter(f, true);
            w.write(System.currentTimeMillis() + "\t" + pkg + "\t" + (isTarget ? "TARGET" : "pomin") + "\n");
            w.close();
            chmod644(f);
        } catch (Throwable ignored) {
        }
    }

    /** Wywołane gdy moduł zaladowany w zlej apce (zly scope LSPosed). */
    public static void reportWrongScope(String loadedPackage) {
        try {
            File f = wrongScopeFile();
            FileWriter w = new FileWriter(f, false);
            w.write("wrong_pkg=" + loadedPackage + "\n");
            w.write("need=android,com.android.systemui,com.android.settings\n");
            w.write("ts=" + System.currentTimeMillis() + "\n");
            w.close();
            chmod644(f);
            String hint = PKG.equals(loadedPackage)
                ? "NIE zaznaczaj AOD Enabler na liscie scope! To tylko aplikacja panelu."
                : "Wylacz '" + loadedPackage + "' ze scope modulu.";
            XposedBridge.log("AODHook [SCOPE-ERROR] " + hint
                + " Potrzebne: System Framework (android) + SystemUI + Ustawienia.");
        } catch (Throwable t) {
            XposedBridge.log("AODHook [SCOPE-ERROR] write failed: " + t.getMessage());
        }
    }

    private static void chmod644(File f) {
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "644", f.getAbsolutePath()}).waitFor();
        } catch (Throwable ignored) {
        }
    }

    private HookStatusStore() {}

    public static void report(String scope, int successCount, int failCount, ClassLoader classLoader) {
        boolean ok = successCount > 0;
        writeTmp(scope, ok, successCount, failCount);
        writePrefs(scope, ok, successCount, failCount);
        if (classLoader != null) {
            writeGlobalFlag(scope, classLoader, ok);
        }
    }

    /** Zawsze zapisuje plik — aplikacja widzi, że handleLoadPackage doszedł do końca. */
    private static void writeTmp(String scope, boolean ok, int successCount, int failCount) {
        try {
            File f = tmpFile(scope);
            String body = "ok=" + (ok ? 1 : 0)
                + ",success=" + successCount
                + ",fail=" + failCount
                + ",ts=" + System.currentTimeMillis()
                + ",pkg=" + scope;
            FileWriter w = new FileWriter(f, false);
            w.write(body);
            w.close();
            chmod644(f);
            XposedBridge.log("AODHook [STATUS] tmp " + f.getAbsolutePath() + " -> " + body);
        } catch (Throwable t) {
            XposedBridge.log("AODHook [STATUS] tmp failed " + scope + ": " + t);
        }
    }

    private static void writePrefs(String scope, boolean ok, int successCount, int failCount) {
        try {
            XSharedPreferences prefs = new XSharedPreferences(PKG, PREF_FILE);
            try {
                prefs.makeWorldReadable();
            } catch (Throwable ignored) {
            }
            boolean committed = prefs.edit()
                .putBoolean("hook_" + scope, ok)
                .putInt("success_" + scope, successCount)
                .putInt("fail_" + scope, failCount)
                .putLong("updated_" + scope, System.currentTimeMillis())
                .commit();
            XposedBridge.log("AODHook [STATUS] prefs " + scope + " commit=" + committed);
        } catch (Throwable t) {
            XposedBridge.log("AODHook [STATUS] prefs failed: " + t.getMessage());
        }
    }

    private static void writeGlobalFlag(String scope, ClassLoader classLoader, boolean ok) {
        if (!ok) {
            return;
        }
        try {
            ContentResolver cr = resolveContentResolver(classLoader);
            if (cr == null) {
                return;
            }
            Class<?> global = XposedHelpers.findClass("android.provider.Settings$Global", classLoader);
            XposedHelpers.callStaticMethod(global, "putInt", cr, globalKey(scope), 1);
        } catch (Throwable ignored) {
        }
    }

    private static ContentResolver resolveContentResolver(ClassLoader classLoader) {
        try {
            Class<?> atClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader);
            Object at = XposedHelpers.callStaticMethod(atClass, "currentActivityThread");
            Context ctx = (Context) XposedHelpers.callMethod(at, "getSystemContext");
            if (ctx == null) {
                ctx = (Context) XposedHelpers.callMethod(at, "currentApplication");
            }
            return ctx != null ? ctx.getContentResolver() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
