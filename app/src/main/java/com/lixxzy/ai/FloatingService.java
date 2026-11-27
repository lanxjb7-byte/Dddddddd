package com.lixxzy.ai;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class FloatingService extends Service {

    private WindowManager wm;
    private View bubble;
    private View panel;

    private WindowManager.LayoutParams bubbleParams;

    private static final String CHANNEL = "lixxzy_channel";

    private int dp(int n) {
        return (int)(n * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private WindowManager.LayoutParams makeParams(int w, int h) {

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        return new WindowManager.LayoutParams(
                w,
                h,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        notification();
        showBubble();
    }

    private void notification() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
                            "LIXXZY AI",
                            NotificationManager.IMPORTANCE_LOW
                    );

            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }

        Notification.Builder builder =
                Build.VERSION.SDK_INT >= 26
                        ? new Notification.Builder(this, CHANNEL)
                        : new Notification.Builder(this);

        builder.setContentTitle("LIXXZY AI aktif")
                .setContentText("Floating window berjalan")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true);

        startForeground(1, builder.build());
    }

    private void showBubble() {

        ImageView logo = new ImageView(this);

        logo.setImageResource(R.drawable.lixxzy_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setBackground(bg(0xFF191919, 100));
        logo.setClipToOutline(true);

        bubble = logo;

        bubbleParams = makeParams(dp(60), dp(60));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(12);
        bubbleParams.y = dp(180);

        logo.setOnTouchListener(new View.OnTouchListener() {

            float sx, sy;
            int ox, oy;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {

                if (e.getAction() == MotionEvent.ACTION_DOWN) {

                    sx = e.getRawX();
                    sy = e.getRawY();

                    ox = bubbleParams.x;
                    oy = bubbleParams.y;

                    moved = false;

                    return true;
                }

                if (e.getAction() == MotionEvent.ACTION_MOVE) {

                    int dx = (int)(e.getRawX() - sx);
                    int dy = (int)(e.getRawY() - sy);

                    if (Math.abs(dx) + Math.abs(dy) > 8) {
                        moved = true;
                    }

                    bubbleParams.x = ox + dx;
                    bubbleParams.y = oy + dy;

                    wm.updateViewLayout(bubble, bubbleParams);

                    return true;
                }

                if (e.getAction() == MotionEvent.ACTION_UP) {

                    if (!moved) openWindow();

                    return true;
                }

                return true;
            }
        });

        wm.addView(bubble, bubbleParams);
    }

    private void openWindow() {

        if (panel != null) {
            wm.removeView(panel);
            panel = null;
            return;
        }

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackground(bg(0xFF15151B, 20));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.lixxzy_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);

        header.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView name = new TextView(this);
        name.setText("  LIXXZY AI");
        name.setTextColor(-1);
        name.setTextSize(16);

        header.addView(name, new LinearLayout.LayoutParams(0, dp(45), 1));

        Button close = new Button(this);
        close.setText("×");

        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(45)));

        root.addView(header);

        WebView web = new WebView(this);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());

        web.loadUrl("https://lixxzy-ai.netlify.app");

        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView label = new TextView(this);
        label.setText("TRANSPARAN");
        label.setTextColor(-1);

        root.addView(label);

        SeekBar slider = new SeekBar(this);
        slider.setMax(99);
        slider.setProgress(99);

        root.addView(slider);

        slider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(
                    SeekBar s,
                    int progress,
                    boolean user
            ) {
                root.setAlpha((progress + 1) / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });

        panel = root;

        WindowManager.LayoutParams p =
                makeParams(dp(330), dp(500));

        p.gravity = Gravity.CENTER;
        p.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        wm.addView(panel, p);

        close.setOnClickListener(v -> {

            if (panel != null) {
                wm.removeView(panel);
                panel = null;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        if (panel != null) wm.removeView(panel);
        if (bubble != null) wm.removeView(bubble);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
          }
