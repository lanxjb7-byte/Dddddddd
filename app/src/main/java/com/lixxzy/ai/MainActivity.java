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
    public static final String SITE = "https://lixxzy-ai.netlify.app";
    private static final int OVERLAY_REQUEST = 701;
    private WebView web;
    private boolean enableAfterPermission = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new AndroidBridge(), "LixxzyAndroid");
        setContentView(web);
        web.loadUrl(SITE);
    }

    public class AndroidBridge {
        @JavascriptInterface public void setFloatingEnabled(boolean enabled) {
            runOnUiThread(() -> {
                if (enabled) enableFloating(); else stopFloating();
            });
        }
        @JavascriptInterface public void setTransparency(int value) {
            int v = Math.max(1, Math.min(100, value));
            getSharedPreferences("lixxzy", MODE_PRIVATE).edit().putInt("transparency", v).apply();
            Intent i = new Intent(MainActivity.this, FloatingService.class);
            i.setAction(FloatingService.ACTION_TRANSPARENCY);
            i.putExtra("value", v);
            try { startService(i); } catch (Exception ignored) { }
        }
    }

    private void enableFloating() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            enableAfterPermission = true;
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, OVERLAY_REQUEST);
            Toast.makeText(this, "Izinkan LIXXZY AI tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
            return;
        }
        getSharedPreferences("lixxzy", MODE_PRIVATE).edit().putBoolean("floating_enabled", true).apply();
        Intent service = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        Toast.makeText(this, "Jendela mengambang aktif", Toast.LENGTH_SHORT).show();
    }

    private void stopFloating() {
        getSharedPreferences("lixxzy", MODE_PRIVATE).edit().putBoolean("floating_enabled", false).apply();
        stopService(new Intent(this, FloatingService.class));
        Toast.makeText(this, "Jendela mengambang dimatikan", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && enableAfterPermission) {
            enableAfterPermission = false;
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) enableFloating();
            else Toast.makeText(this, "Izin overlay belum diberikan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
