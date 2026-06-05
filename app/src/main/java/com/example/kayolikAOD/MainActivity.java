package com.example.kayolikAOD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String BG = "#0d0e10";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Color.parseColor(BG));
        w.setNavigationBarColor(Color.parseColor(BG));

        webView = new WebView(this);
        setContentView(webView);
        webView.setBackgroundColor(Color.parseColor(BG));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    public class AndroidBridge {
        @JavascriptInterface public String getDeviceModel()     { return android.os.Build.MODEL; }
        @JavascriptInterface public String getDeviceCodename()  { return android.os.Build.DEVICE; }
        @JavascriptInterface public String getManufacturer()    { return android.os.Build.MANUFACTURER; }
        @JavascriptInterface public String getAndroidVersion()  { return "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")"; }

        @JavascriptInterface
        public String getAodStatus() {
            try {
                int v = Settings.Secure.getInt(getContentResolver(), "doze_always_on", -1);
                if (v == 1) return "active";
                if (v == 0) return "off";
                return "unset";
            } catch (Exception e) {
                return "error";
            }
        }

        @JavascriptInterface
        public boolean openLSPosed() {
            Intent i = getPackageManager().getLaunchIntentForPackage("org.lsposed.manager");
            if (i == null) {
                try {
                    i = getPackageManager().getLaunchIntentForPackage("org.meowcat.edxposed.manager");
                } catch (Exception ignored) {}
            }
            if (i == null) return false;
            startActivity(i);
            return true;
        }
    }
}
