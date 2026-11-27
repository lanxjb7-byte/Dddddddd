package com.lixxzy.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.*;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {

    private static final String SITE =
            "https://lixxzy-ai.netlify.app";

    private static final int REQUEST_OVERLAY = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        WebView web = new WebView(this);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());

        root.addView(
                web,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        web.loadUrl(SITE);

        Button floating = new Button(this);

        floating.setText("✦ JENDELA MENGAMBANG");
        floating.setTextColor(Color.WHITE);
        floating.setBackgroundColor(Color.rgb(190, 20, 45));

        root.addView(floating);

        floating.setOnClickListener(v -> activateFloating());

        setContentView(root);
    }

    private void activateFloating() {

        if (Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)) {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivityForResult(intent, REQUEST_OVERLAY);
            return;
        }

        Intent service = new Intent(this, FloatingService.class);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY &&
                (Build.VERSION.SDK_INT < 23 ||
                Settings.canDrawOverlays(this))) {

            activateFloating();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    }
