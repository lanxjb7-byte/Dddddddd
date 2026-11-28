package com.lixxzy.ai;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class FloatingService extends Service {
    public static final String ACTION_TRANSPARENCY = "com.lixxzy.ai.TRANSPARENCY";
    private static final String CHANNEL = "lixxzy_channel";
    private WindowManager wm;
    private View bubble, panel;
    private WindowManager.LayoutParams bubbleParams, panelParams;
    private LinearLayout panelRoot;
    private int alphaPercent = 100;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), 0x55FFFFFF); return d;
    }
    private int overlayType() { return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private WindowManager.LayoutParams params(int w, int h, int flags) {
        return new WindowManager.LayoutParams(w, h, overlayType(), flags, PixelFormat.TRANSLUCENT);
    }
    @Override public void onCreate() {
        super.onCreate(); wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        alphaPercent = getSharedPreferences("lixxzy", MODE_PRIVATE).getInt("transparency", 100);
        createNotification(); showBubble();
    }
    private void createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "LIXXZY AI", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setContentTitle("LIXXZY AI aktif").setContentText("Jendela mengambang sedang berjalan")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true);
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, b.build()); else startForeground(1, b.build());
    }
    private ImageView logoView() {
        ImageView img = new ImageView(this);
        int id = getResources().getIdentifier("lixxzy_logo", "drawable", getPackageName());
        if (id == 0) id = getApplicationInfo().icon;
        if (id != 0) img.setImageResource(id);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP); img.setBackground(bg(0xFF191919, 100));
        if (Build.VERSION.SDK_INT >= 21) img.setClipToOutline(true);
        return img;
    }
    private void showBubble() {
        ImageView icon = logoView(); bubble = icon;
        bubbleParams = params(dp(60), dp(60), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        bubbleParams.gravity = Gravity.TOP | Gravity.START; bubbleParams.x = dp(12); bubbleParams.y = dp(180);
        icon.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; moved=false; return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { int dx=(int)(e.getRawX()-sx), dy=(int)(e.getRawY()-sy); if(Math.abs(dx)+Math.abs(dy)>8)moved=true; bubbleParams.x=ox+dx; bubbleParams.y=oy+dy; wm.updateViewLayout(bubble,bubbleParams); return true; }
                if (e.getAction() == MotionEvent.ACTION_UP) { if(!moved) togglePanel(); return true; } return true;
            }
        });
        wm.addView(bubble, bubbleParams);
    }
    private void togglePanel() { if (panel != null) removePanel(); else openPanel(); }
    private void openPanel() {
        panelRoot = new LinearLayout(this); panelRoot.setOrientation(LinearLayout.VERTICAL); panelRoot.setPadding(dp(9),dp(7),dp(9),dp(7)); panelRoot.setBackground(bg(0xF0181820,18));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(4),0,0,0);
        ImageView logo = logoView(); header.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(34)));
        TextView title = new TextView(this); title.setText("  LIXXZY AI"); title.setTextColor(0xFFFFFFFF); title.setTextSize(15); header.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        Button enlarge = new Button(this); enlarge.setText("□"); enlarge.setTextSize(17); header.addView(enlarge,new LinearLayout.LayoutParams(dp(48),dp(42)));
        Button close = new Button(this); close.setText("×"); close.setTextSize(20); header.addView(close,new LinearLayout.LayoutParams(dp(44),dp(42)));
        panelRoot.addView(header);
        WebView web = new WebView(this); WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true); ws.setMediaPlaybackRequiresUserGesture(false); ws.setSupportZoom(false);
        web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient()); web.loadUrl(MainActivity.SITE);
        panelRoot.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        TextView resize = new TextView(this); resize.setText("⋰  UBAH UKURAN"); resize.setTextColor(0xFFFFFFFF); resize.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); resize.setPadding(0,dp(5),dp(5),0); panelRoot.addView(resize,new LinearLayout.LayoutParams(-1,dp(28)));
        panel = panelRoot;
        panelParams = params(dp(330),dp(500),WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        panelParams.gravity=Gravity.CENTER; wm.addView(panel,panelParams); applyAlpha();
        header.setOnTouchListener(movePanel());
        enlarge.setOnClickListener(v -> { panelParams.width = panelParams.width < getResources().getDisplayMetrics().widthPixels-dp(30) ? getResources().getDisplayMetrics().widthPixels-dp(30) : dp(330); panelParams.height = panelParams.height < getResources().getDisplayMetrics().heightPixels-dp(100) ? getResources().getDisplayMetrics().heightPixels-dp(100) : dp(500); panelParams.gravity=Gravity.CENTER; wm.updateViewLayout(panel,panelParams); });
        close.setOnClickListener(v -> removePanel());
        resize.setOnTouchListener(new View.OnTouchListener() { float x,y; int w,h; public boolean onTouch(View v,MotionEvent e) { if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();w=panelParams.width;h=panelParams.height;return true;} if(e.getAction()==MotionEvent.ACTION_MOVE){panelParams.width=Math.max(dp(240),Math.min(getResources().getDisplayMetrics().widthPixels-dp(20),w+(int)(e.getRawX()-x)));panelParams.height=Math.max(dp(220),Math.min(getResources().getDisplayMetrics().heightPixels-dp(80),h+(int)(e.getRawY()-y)));wm.updateViewLayout(panel,panelParams);return true;} return true; }});
    }
    private View.OnTouchListener movePanel() { return new View.OnTouchListener() { float x,y; int ox,oy; public boolean onTouch(View v,MotionEvent e) { if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();ox=panelParams.x;oy=panelParams.y;return true;} if(e.getAction()==MotionEvent.ACTION_MOVE){panelParams.gravity=Gravity.TOP|Gravity.START;panelParams.x=ox+(int)(e.getRawX()-x);panelParams.y=oy+(int)(e.getRawY()-y);wm.updateViewLayout(panel,panelParams);return true;} return true; }}; }
    private void applyAlpha() { if(panel!=null) panel.setAlpha(Math.max(0.01f,Math.min(1f,alphaPercent/100f))); if(bubble!=null) bubble.setAlpha(Math.max(0.01f,Math.min(1f,alphaPercent/100f))); }
    private void removePanel() { if(panel!=null){ try{wm.removeView(panel);}catch(Exception ignored){} panel=null; panelRoot=null; } }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_TRANSPARENCY.equals(intent.getAction())) { alphaPercent=Math.max(1,Math.min(100,intent.getIntExtra("value",100))); applyAlpha(); }
        return START_STICKY;
    }
    @Override public void onDestroy() { removePanel(); if(bubble!=null){try{wm.removeView(bubble);}catch(Exception ignored){} bubble=null;} super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
