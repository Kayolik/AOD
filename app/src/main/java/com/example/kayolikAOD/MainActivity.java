package com.example.kayolikAOD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final String PREFS = "aod_prefs";
    private static final String KEY_BRIGHTNESS = "aod_brightness";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#101114"));
        window.setNavigationBarColor(Color.parseColor("#101114"));

        webView = new WebView(this);
        setContentView(webView);
        webView.setBackgroundColor(Color.parseColor("#101114"));

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
        public int getAODBrightness() {
            return getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_BRIGHTNESS, 100);
        }

        @JavascriptInterface
        public String setAODBrightness(int percent) {
            int v = Math.max(0, Math.min(100, percent));
            boolean ok = getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_BRIGHTNESS, v)
                .commit();
            return ok ? "OK" : "FAILED";
        }
    }
}
