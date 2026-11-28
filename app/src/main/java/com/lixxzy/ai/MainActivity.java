
package com.lixxzy.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_REQUEST = 1001;
    private static final String WEBSITE =
            "https://lixxzy-ai.netlify.app";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
                new AndroidBridge(),
                "LixxzyAndroid"
        );

        webView.loadUrl(WEBSITE);
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void setFloating(boolean enabled) {
            runOnUiThread(() -> {
                if (enabled) {
                    enableFloating();
                } else {
                    stopService(new Intent(
                            MainActivity.this,
                            FloatingService.class
                    ));
                }
            });
        }
    }

    private void enableFloating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivityForResult(intent, OVERLAY_REQUEST);

            Toast.makeText(
                    this,
                    "Izinkan tampil di atas aplikasi lain",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        startFloatingService();
    }

    private void startFloatingService() {
        Intent intent = new Intent(
                this,
                FloatingService.class
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_REQUEST) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(
                        this,
                        "Izin overlay belum diberikan",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
