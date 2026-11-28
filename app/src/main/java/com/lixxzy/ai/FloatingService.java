
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
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
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
        return START_STICKY;
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private GradientDrawable roundedBackground(
            String color,
            int radius
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(color));
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private ImageView createLogo() {
        ImageView image = new ImageView(this);

        image.setImageResource(R.drawable.lixxzy_logo);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        image.setPadding(dp(3), dp(3), dp(3), dp(3));

        return image;
    }

    private void createBubble() {
        if (bubble != null) return;

        bubble = createLogo();
        bubble.setBackground(
                roundedBackground("#171717", 100)
        );
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
        bubbleParams.alpha = 1.0f;

        bubble.setOnTouchListener(new View.OnTouchListener() {

            private int startX;
            private int startY;
            private float touchX;
            private float touchY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        bubbleParams.x = startX +
                                (int) (event.getRawX() - touchX);

                        bubbleParams.y = startY +
                                (int) (event.getRawY() - touchY);

                        windowManager.updateViewLayout(
                                bubble,
                                bubbleParams
                        );
                        return true;

                    case MotionEvent.ACTION_UP:
                        float dx = Math.abs(event.getRawX() - touchX);
                        float dy = Math.abs(event.getRawY() - touchY);

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
        panel.setBackground(
                roundedBackground("#151515", 18)
        );
        panel.setClipToOutline(true);
        panel.setElevation(dp(10));

        createHeader();

        panelWebView = new WebView(this);

        WebSettings settings = panelWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        panelWebView.setWebViewClient(new WebViewClient());
        panelWebView.setWebChromeClient(new WebChromeClient());

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
                PixelFormat.OPAQUE
        );

        panelParams.gravity = Gravity.CENTER;
        panelParams.alpha = 1.0f;

        windowManager.addView(panel, panelParams);
    }

    private void createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(8), dp(8));
        header.setBackgroundColor(Color.rgb(30, 30, 30));

        ImageView logo = createLogo();

        header.addView(
                logo,
                new LinearLayout.LayoutParams(dp(36), dp(36))
        );

        TextView title = new TextView(this);
        title.setText("LIXXZY AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(null, 1);
        title.setPadding(dp(10), 0, 0, 0);

        header.addView(
                title,
                new LinearLayout.LayoutParams(0, dp(42), 1)
        );

        TextView maximize = new TextView(this);
        maximize.setText("↗");
        maximize.setTextColor(Color.WHITE);
        maximize.setTextSize(23);
        maximize.setGravity(Gravity.CENTER);

        header.addView(
                maximize,
                new LinearLayout.LayoutParams(dp(42), dp(42))
        );

        maximize.setOnClickListener(v -> toggleMaximize());

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(25);
        close.setGravity(Gravity.CENTER);

        header.addView(
                close,
                new LinearLayout.LayoutParams(dp(38), dp(42))
        );

        close.setOnClickListener(v -> closePanel());

        panel.addView(header);
    }

    private void toggleMaximize() {
        if (panel == null || panelParams == null) return;

        maximized = !maximized;

        if (maximized) {
            panelParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            panelParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        } else {
            panelParams.width = dp(340);
            panelParams.height = dp(540);
        }

        panelParams.gravity = Gravity.CENTER;
        panelParams.alpha = 1.0f;

        windowManager.updateViewLayout(panel, panelParams);
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
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lixzzy Floating Window",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Jendela mengambang Lixxzy AI"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Lixxzy AI")
                    .setContentText("Jendela mengambang aktif")
                    .setSmallIcon(R.drawable.lixxzy_logo)
                    .setOngoing(true)
                    .build();
        }

        return new Notification.Builder(this)
                .setContentTitle("Lixxzy AI")
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
            
