package com.example.kayolikAOD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;
import android.provider.Settings;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HookStatus.init(this);

        // Make the status bar transparent for immersive look
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#0b0c10"));
        window.setNavigationBarColor(Color.parseColor("#0b0c10"));

        // Create WebView programmatically (no XML layout needed)
        webView = new WebView(this);
        setContentView(webView);

        // WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Prevent WebView from opening links in external browser
        webView.setWebViewClient(new WebViewClient());

        // Set background color to match the dashboard
        webView.setBackgroundColor(Color.parseColor("#0b0c10"));

        // Register JavaScript interface so the HTML page can call Java methods
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        // Load the dashboard from assets
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * JavaScript interface exposed to the WebView.
     * The HTML page calls these methods via window.AndroidBridge.xxx()
     */
    public class AndroidBridge {

        /**
         * Check if the Xposed module is active by reading the doze_always_on setting.
         * If the module is working, this should return 1 even though the device
         * doesn't natively support AOD.
         */
        /**
         * Prawdziwy status: hooki załadowane w framework + SystemUI (XSharedPreferences z LSPosed).
         */
        @JavascriptInterface
        public boolean isModuleActive() {
            return HookStatus.isModuleActive();
        }

        @JavascriptInterface
        public boolean isFrameworkHooked() {
            return HookStatus.isFrameworkHooked();
        }

        @JavascriptInterface
        public boolean isSystemUiHooked() {
            return HookStatus.isSystemUiHooked();
        }

        @JavascriptInterface
        public boolean isSettingsHooked() {
            return HookStatus.isSettingsHooked();
        }

        @JavascriptInterface
        public String getHookStatusSummary() {
            return HookStatus.getStatusSummary();
        }

        @JavascriptInterface
        public String getHookStatusError() {
            return HookStatus.getLastError();
        }

        @JavascriptInterface
        public String getScopeHint() {
            return HookStatus.getScopeHint();
        }

        /** Diagnostyka plików /data/local/tmp/aod_enabler_*.txt */
        @JavascriptInterface
        public String getTmpStatusDump() {
            return HookStatus.getStatusSummary();
        }

        @JavascriptInterface
        public String getSeenPackages() {
            return HookStatus.getSeenPackagesTail();
        }

        @JavascriptInterface
        public boolean isLsposedModuleReachable() {
            return HookStatus.isLsposedModuleReachable();
        }

        /** @deprecated użyj isModuleActive() */
        @JavascriptInterface
        public boolean isXposedActive() {
            return isModuleActive();
        }

        /**
         * Returns the current value of doze_always_on setting as a string.
         */
        @JavascriptInterface
        public String getDozeAlwaysOnValue() {
            try {
                int value = Settings.Secure.getInt(
                    getContentResolver(),
                    "doze_always_on",
                    -1
                );
                if (value == -1) return "null (nie ustawiono)";
                return String.valueOf(value);
            } catch (Exception e) {
                return "Blad: " + e.getMessage();
            }
        }

        /**
         * Returns the device model name.
         */
        @JavascriptInterface
        public String getDeviceModel() {
            return android.os.Build.MODEL;
        }

        /**
         * Returns the Android version.
         */
        @JavascriptInterface
        public String getAndroidVersion() {
            return "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")";
        }

        /**
         * Returns the device manufacturer.
         */
        @JavascriptInterface
        public String getManufacturer() {
            return android.os.Build.MANUFACTURER;
        }

        /**
         * Returns the device codename (ro.product.device).
         */
        @JavascriptInterface
        public String getDeviceCodename() {
            return android.os.Build.DEVICE;
        }

        /**
         * Tries to write doze_always_on = 1 to Settings.Secure.
         * This requires WRITE_SECURE_SETTINGS permission (granted via adb or root).
         */
        @JavascriptInterface
        public String forceEnableAOD() {
            try {
                boolean success = Settings.Secure.putInt(
                    getContentResolver(),
                    "doze_always_on",
                    1
                );
                return success ? "OK" : "FAILED";
            } catch (SecurityException e) {
                return "Brak uprawnien. Uzyj: adb shell pm grant com.example.kayolikAOD android.permission.WRITE_SECURE_SETTINGS";
            } catch (Exception e) {
                return "Blad: " + e.getMessage();
            }
        }
    }
}
