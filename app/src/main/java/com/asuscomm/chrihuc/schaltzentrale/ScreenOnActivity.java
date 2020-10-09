package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.io.FileInputStream;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class ScreenOnActivity extends Activity {
    public int mode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack

//        KeyguardManager.KeyguardLock lock = ((KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE))
//                .newKeyguardLock(KEYGUARD_SERVICE);
//        PowerManager powerManager = ((PowerManager) getSystemService(Context.POWER_SERVICE));
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Tag:TAG");
//
//        lock.disableKeyguard();
//        wakeLock.acquire();


        String execVoid = getIntent().getStringExtra("execVoid");

//        if (execVoid == null){
//        if (Build.VERSION.SDK_INT >= 27) {
//            setShowWhenLocked(true);
//        } else {
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        }
//            Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
//            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            openMe.putExtra("execVoid","switchDispOn");
//            finish();
//            startActivity(openMe);
//        } else if (execVoid.equals("showStopWecker")){
        mode = 0;
        if (execVoid.equals("showStopWecker")){
            mode = 1;
            if (Build.VERSION.SDK_INT >= 27) {
                setTurnScreenOn(true);
                setShowWhenLocked(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            showStopWecker();
        } else if (execVoid.equals("showTuerSpion")){
            if (Build.VERSION.SDK_INT >= 27) {
                setShowWhenLocked(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
            showTuerSpion();
        } else if (execVoid.equals("showStopAlarm")){
            mode = 1;
            if (Build.VERSION.SDK_INT >= 27) {
                setShowWhenLocked(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
            showStopAlarm();
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
//        openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
//        openMe.putExtra("execVoid","switchDispOn");
//        //        openMe.putExtra("picNr",picNr);
//        startActivity(openMe);
//    }

    @Override
    public void onBackPressed()
    {
        ;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) & mode == 1) {
        } else if((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
        }

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("showTuerSpion")){
            showTuerSpion();
        }
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("showStopWecker")){
            showStopWecker();
        }
    }

    public void showStopWecker(){
        setContentView(R.layout.stop_wecker);
        try {
            Button bu = findViewById(R.id.button3);
            bu.setText("Stop Wecker");
            bu.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(ScreenOnActivity.this, MqttConnectionManagerService.class);
                    serviceIntent.putExtra("execVoid", "stopWecker");
                    startService(serviceIntent);
                    finish();
                }
            });
        }catch (Exception e) {

        }
//        finish();
    }

    public void showStopAlarm(){
        setContentView(R.layout.stop_wecker);
        try {
            Button bu = findViewById(R.id.button3);
            bu.setText("Stop Alarm");
            bu.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(ScreenOnActivity.this, MqttConnectionManagerService.class);
                    serviceIntent.putExtra("execVoid", "stopWecker");
                    startService(serviceIntent);
                    send_mqtt_serv("Command/Szene/QuitAlarm", "{\"Szene\":\"QuitAlarm\"}");
                    finish();
                }
            });
            Button bu1 = findViewById(R.id.button4);
            bu1.setText("Ton Aus");
            bu1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(ScreenOnActivity.this, MqttConnectionManagerService.class);
                    serviceIntent.putExtra("execVoid", "stopWecker");
                    startService(serviceIntent);
                    finish();
                }
            });
        }catch (Exception e) {

        }
//        finish();
    }

    public void showTuerSpion(){

        // Convert bytes data into a Bitmap
        setContentView(R.layout.tuercam);
        LinearLayout layout = (LinearLayout) this.findViewById(R.id.tuercam);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
        final LinearLayout ll = new LinearLayout(this);
        ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);
        layout.addView(sv);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Bitmap bmp;
        for (int i = 1; i < 10; i++) {
            bmp = loadImageBitmap(this.getApplicationContext(), "tuerspion" + String.valueOf(i) + ".jpg");
            if (bmp != null) {
                ImageView img = new ImageView(this);
                img.setImageBitmap(bmp);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                img.setAdjustViewBounds(true);
                img.setLayoutParams(layoutParams);
                ll.addView(img);
            }
        }

    }

    public Bitmap loadImageBitmap(Context context, String imageName) {
        Bitmap bitmap = null;
        FileInputStream fiStream;
        try {
            fiStream    = context.openFileInput(imageName);
            bitmap      = BitmapFactory.decodeStream(fiStream);
            fiStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 3, Something went wrong!");
            e.printStackTrace();
        }
        return bitmap;
    }

    }