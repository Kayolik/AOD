package com.example.kayolikAOD;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Odczyt statusu: /data/local/tmp/aod_enabler_*.txt → prefs → Settings.Global.
 */
public final class HookStatus {

    public static final String PREF_FILE = HookStatusStore.PREF_FILE;

    private static Context appContext;
    private static String lastError = "";

    private HookStatus() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static String getLastError() {
        return lastError == null || lastError.isEmpty() ? "brak" : lastError;
    }

    private static TmpStatus readTmp(String scope) {
        File f = HookStatusStore.tmpFile(scope);
        if (!f.exists() || !f.canRead()) {
            return TmpStatus.missing();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line == null) {
                return TmpStatus.missing();
            }
            boolean ok = line.contains("ok=1");
            int success = parseIntField(line, "success");
            int fail = parseIntField(line, "fail");
            return new TmpStatus(true, ok, success, fail, line);
        } catch (Throwable t) {
            lastError = "tmp: " + t.getMessage();
            return TmpStatus.missing();
        }
    }

    private static int parseIntField(String line, String key) {
        try {
            int i = line.indexOf(key + "=");
            if (i < 0) {
                return 0;
            }
            int end = line.indexOf(',', i);
            String num = end > i ? line.substring(i + key.length() + 1, end) : line.substring(i + key.length() + 1);
            return Integer.parseInt(num.trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean readGlobalFlag(String scope) {
        if (appContext == null) {
            return false;
        }
        try {
            return Settings.Global.getInt(appContext.getContentResolver(), HookStatusStore.globalKey(scope), 0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean readPrefsFlag(String scope) {
        if (appContext == null) {
            return false;
        }
        try {
            SharedPreferences p = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            return p.getBoolean("hook_" + scope, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isScopeOk(String scope) {
        TmpStatus tmp = readTmp(scope);
        if (tmp.present && tmp.ok) {
            return true;
        }
        if (readGlobalFlag(scope)) {
            return true;
        }
        if (readPrefsFlag(scope)) {
            return true;
        }
        if (tmp.present && !tmp.ok) {
            lastError = scope + ": hooki uruchomione, ale success=0 (patrz logcat AODHook)";
        } else {
            lastError = scope + ": brak pliku " + HookStatusStore.tmpFile(scope).getName()
                + " — zly scope LSPosed lub stary APK";
        }
        return false;
    }

    public static boolean isLsposedModuleReachable() {
        return isFrameworkHooked() || isSystemUiHooked()
            || readTmp("android").present || readTmp("systemui").present;
    }

    public static boolean isFrameworkHooked() {
        return isScopeOk("android");
    }

    public static boolean isSystemUiHooked() {
        return isScopeOk("systemui");
    }

    public static boolean isSettingsHooked() {
        return isScopeOk("settings");
    }

    public static boolean isModuleActive() {
        return isFrameworkHooked() && isSystemUiHooked();
    }

    public static String getSeenPackagesTail() {
        File f = HookStatusStore.seenPackagesFile();
        if (!f.exists() || !f.canRead()) {
            return "brak (zrestartuj po scope)";
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = "";
            String last = "";
            int n = 0;
            while ((line = br.readLine()) != null) {
                last = line;
                n++;
            }
            return n + " wpisow, ostatni: " + last;
        } catch (Throwable t) {
            return "blad: " + t.getMessage();
        }
    }

    public static String getStatusSummary() {
        TmpStatus a = readTmp("android");
        TmpStatus s = readTmp("systemui");
        TmpStatus set = readTmp("settings");
        return "tmp[android]=" + (a.present ? a.line : "brak")
            + " | tmp[systemui]=" + (s.present ? s.line : "brak")
            + " | tmp[settings]=" + (set.present ? set.line : "brak");
    }

    public static String getWrongScopeMessage() {
        File f = HookStatusStore.wrongScopeFile();
        if (!f.exists() || !f.canRead()) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String wrong = "";
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("wrong_pkg=")) {
                    wrong = line.substring("wrong_pkg=".length()).trim();
                }
            }
            if (!wrong.isEmpty()) {
                if (HookStatusStore.PKG.equals(wrong)) {
                    return "ZLY SCOPE: odznacz SAMA apke AOD Enabler! "
                        + "Zaznacz: System Framework, SystemUI, Ustawienia.";
                }
                return "ZLY SCOPE! Odznacz: " + wrong
                    + ". Zaznacz tylko: System Framework, SystemUI, Ustawienia.";
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static String getScopeHint() {
        String wrong = getWrongScopeMessage();
        if (!wrong.isEmpty()) {
            return wrong;
        }
        TmpStatus a = readTmp("android");
        TmpStatus s = readTmp("systemui");
        if (!a.present && !s.present) {
            return "LSPosed: wlacz modul + scope Framework + SystemUI + pelny restart";
        }
        if (a.present && !a.ok) {
            return "Framework: hooki sie nie zainstalowaly — wyslij logcat AODHook";
        }
        if (s.present && !s.ok) {
            return "SystemUI: hooki sie nie zainstalowaly — wyslij logcat AODHook";
        }
        return "Scope OK — szukaj AOD w Ustawieniach > Wyswietlacz";
    }

    private static final class TmpStatus {
        final boolean present;
        final boolean ok;
        final int success;
        final int fail;
        final String line;

        TmpStatus(boolean present, boolean ok, int success, int fail, String line) {
            this.present = present;
            this.ok = ok;
            this.success = success;
            this.fail = fail;
            this.line = line;
        }

        static TmpStatus missing() {
            return new TmpStatus(false, false, 0, 0, null);
        }
    }
}
