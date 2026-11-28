package com.lixxzy.ai;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {

    private static final String CHANNEL_ID = "lixxzy_floating";
    private static final int NOTIFICATION_ID = 10;

    private WindowManager windowManager;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    private ImageView bubble;
    private LinearLayout panel;
    private WebView panelWebView;

    private boolean panelOpen = false;
    private boolean maximized = false;

    private int alphaValue = 255;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        startForeground(
                NOTIFICATION_ID,
                createNotification()
        );

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        createBubble();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        if (intent != null
                && "UPDATE_ALPHA".equals(intent.getAction())) {

            alphaValue = intent.getIntExtra("alpha", 255);
            alphaValue = Math.max(30, Math.min(255, alphaValue));

            updateAlpha();
        }

        return START_STICKY;
    }

    private int dp(float value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    private GradientDrawable createBackground(
            String color,
            int radius
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createBubble() {
        if (bubble != null) return;

        bubble = new ImageView(this);
        bubble.setImageResource(R.drawable.lixxzy_logo);
        bubble.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bubble.setBackground(createBackground("#171717", 100));
        bubble.setClipToOutline(true);
        bubble.setElevation(dp(8));

        bubbleParams = new WindowManager.LayoutParams(
                dp(62),
                dp(62),
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(20);
        bubbleParams.y = dp(180);

        bubble.setOnTouchListener(new View.OnTouchListener() {

            private int startX;
            private int startY;
            private float touchX;
            private float touchY;

            @Override
            public boolean onTouch(
                    View view,
                    MotionEvent event
            ) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        bubbleParams.x = startX
                                + (int) (event.getRawX() - touchX);

                        bubbleParams.y = startY
                                + (int) (event.getRawY() - touchY);

                        windowManager.updateViewLayout(
                                bubble,
                                bubbleParams
                        );
                        return true;

                    case MotionEvent.ACTION_UP:
                        float dx = Math.abs(
                                event.getRawX() - touchX
                        );

                        float dy = Math.abs(
                                event.getRawY() - touchY
                        );

                        if (dx < dp(8) && dy < dp(8)) {
                            togglePanel();
                        }

                        return true;
                }

                return false;
            }
        });

        windowManager.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panelOpen) {
            closePanel();
        } else {
            openPanel();
        }
    }

    private void openPanel() {
        if (panelOpen) return;

        panelOpen = true;

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(createBackground("#151515", 18));
        panel.setClipToOutline(true);
        panel.setElevation(dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(
                dp(12),
                dp(8),
                dp(8),
                dp(8)
        );
        header.setBackgroundColor(Color.rgb(30, 30, 30));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.lixxzy_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);

        header.addView(
                logo,
                new LinearLayout.LayoutParams(
                        dp(32),
                        dp(32)
                )
        );

        TextView title = new TextView(this);
        title.setText("LIXXZY AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(null, 1);
        title.setPadding(dp(10), 0, 0, 0);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(40),
                        1
                )
        );

        TextView maximize = new TextView(this);
        maximize.setText("↗");
        maximize.setTextColor(Color.WHITE);
        maximize.setTextSize(23);
        maximize.setGravity(Gravity.CENTER);

        header.addView(
                maximize,
                new LinearLayout.LayoutParams(
                        dp(42),
                        dp(42)
                )
        );

        maximize.setOnClickListener(v -> toggleMaximize());

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(25);
        close.setGravity(Gravity.CENTER);

        header.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(38),
                        dp(42)
                )
        );

        close.setOnClickListener(v -> closePanel());

        panel.addView(header);

        panelWebView = new WebView(this);

        WebSettings settings = panelWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        panelWebView.setWebViewClient(
                new android.webkit.WebViewClient()
        );

        panelWebView.setWebChromeClient(
                new android.webkit.WebChromeClient()
        );

        panelWebView.loadUrl("https://lixxzy-ai.netlify.app");

        panel.addView(
                panelWebView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        panelParams = new WindowManager.LayoutParams(
                dp(340),
                dp(540),
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        panelParams.gravity = Gravity.CENTER;
        panelParams.alpha = alphaValue / 255f;

        windowManager.addView(panel, panelParams);
    }

    private void toggleMaximize() {
        if (panel == null || panelParams == null) return;

        maximized = !maximized;

        if (maximized) {
            panelParams.width = -1;
            panelParams.height = -1;
        } else {
            panelParams.width = dp(340);
            panelParams.height = dp(540);
        }

        panelParams.gravity = Gravity.CENTER;

        windowManager.updateViewLayout(
                panel,
                panelParams
        );
    }

    private void updateAlpha() {
        if (panel != null && panelParams != null) {
            panelParams.alpha = alphaValue / 255f;

            windowManager.updateViewLayout(
                    panel,
                    panelParams
            );
        }
    }

    private void closePanel() {
        if (panelWebView != null) {
            panelWebView.stopLoading();
            panelWebView.destroy();
            panelWebView = null;
        }

        if (panel != null) {
            windowManager.removeView(panel);
            panel = null;
        }

        panelOpen = false;
        maximized = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "LIXXZY Floating Window",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Notifikasi jendela mengambang LIXXZY AI"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("LIXXZY AI")
                    .setContentText("Jendela mengambang aktif")
                    .setSmallIcon(R.drawable.lixxzy_logo)
                    .setOngoing(true)
                    .build();
        }

        return new Notification.Builder(this)
                .setContentTitle("LIXXZY AI")
                .setContentText("Jendela mengambang aktif")
                .setSmallIcon(R.drawable.lixxzy_logo)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        closePanel();

        if (bubble != null) {
            windowManager.removeView(bubble);
            bubble = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                }
                                 
