package com.asuscomm.chrihuc.schaltzentrale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.media.AudioAttributes;
//import android.media.Ringtone;
import android.media.AudioManager;
import android.media.RingtoneManager;
//import android.net.Network;
//import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.Settings;
//import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
//import android.text.style.BackgroundColorSpan;
//import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;
//import android.widget.LinearLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
//import android.widget.Toast;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
//import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.json.JSONArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import java.io.ByteArrayInputStream;
//import java.io.File;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Calendar;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
//import java.util.Timer;
//import java.util.TimerTask;

//import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;


public class MainActivity extends AppCompatActivity {

    private static final String NOTIFICATION_CHANNEL_ID = "MyChannel";
    Context context;
    Intent starterIntent;

    static final String TAG = "Schaltzentrale";
//    private static final String PROPERTY_APP_VERSION = "appVersion";
//    TextView mDisplay;

    Map<String, ValueList> sensoren = new HashMap<String, ValueList>();

    MqttAndroidClient mqttAndroidClient;
    Handler myHandler;

    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;

//    public Boolean server_online = true;
    public int height = 1920;
    public int width = 1080;
    public int level = 0;

    public int picNr = 1;

    public int concounter = 0;
    public int mescounter = 0;
    public boolean ison = true;

    public boolean alleSettings = false;
    public boolean killme = false;
    private static final int TIME_DELAY = 500;
    private static long back_pressed;

//    private ArrayList<String> items;
    private ArrayAdapter<String> itemsAdapter;
    private ListView lvItems;
    public ArrayList<String> messages;

    Map<String, String> uuids = new HashMap<String, String>();

    public static BufferedWriter out;

    private BroadcastReceiver mReceiver = null;
//    private BroadcastReceiver wReceiver = null;

    Integer listSize = 33;
    public ArrayList<String> inpList;
    final HkzLabel[] hkzList = new HkzLabel[listSize];

    @Override
    public void onBackPressed()
    {
        showMain();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
//            send_to_server("{'Szene':'EGLeiser'}");
            send_mqtt("Command/Szene/EGLeiser", "{\"Szene\":\"EGLeiser\"}");
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
//            send_to_server("{'Szene':'EGLauter'}");
            send_mqtt("Command/Szene/EGLauter", "{\"Szene\":\"EGLauter\"}");
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
                finish();
                startActivity(starterIntent);
            } else {
                if (killme){
                    finish();
                }
                showMain();
            }
            back_pressed = System.currentTimeMillis();

            //unSubscribe("AES/Prio2");
            //Toast.makeText(getBaseContext(), "Unsub to Prio2", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.
                ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        starterIntent = getIntent();
        String execVoid = getIntent().getStringExtra("execVoid");

        Boolean vordergrund = prefs.getBoolean("checkbox_vordergrund", false);
        if (execVoid != null && execVoid.equals("vordergrund")){
            vordergrund = true;
            killme = true;
        }

        if (vordergrund) {
            if (Build.VERSION.SDK_INT >= 27) {
//                setTurnScreenOn(true);
                setShowWhenLocked(true);
            } else {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }

//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//        WifiReceiver wifiReceiver= new WifiReceiver();
//        registerReceiver(wifiReceiver, intentFilter);

        startService(new Intent(this, MQTTReceiver.class));
        if (savedInstanceState == null || hkzList[0] == null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                }
                NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if(!n.isNotificationPolicyAccessGranted()) {
                    // Ask the user to grant access
                    Intent intent2 = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent2);
                }
            }

            // initialize receiver
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            mReceiver = new ScreenReceiver();
            registerReceiver(mReceiver, filter);
            //setHasOptionsMenu(true);

//            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            registerConnectivityNetworkMonitorForAPI21AndUp();
//            callAsynchronousTask();



            Activity act = this;
            myHandler = new Handler();

            messages = new ArrayList<String>();
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            height = size.y; //S7 1920 => /1920 * height /moto 888
            width = size.x; //S7 1080 => /1080 * width /moto 540


//            IntentFilter intentFilter = new IntentFilter();
//            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//            wReceiver = new WifiReceiver();
//            registerReceiver(wReceiver, intentFilter);

            //Intent i = new Intent(this, EinstellungenActivity.class);
            //startActivity(i);
//        itemsAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, messages);
//            AsyncTaskActivity mqRunner = new AsyncTaskActivity();
//            mqRunner.execute();
            getMqttConnection();

//            Thread t = new Thread(){
//                public void run(){
////                    ServiceConnection serviceConnection = null;
//                    getApplicationContext().bindService(
//                            new Intent(getApplicationContext(), MQTTReceiver.class),
//                            DatapointActivity.connection,
//                            Context.BIND_AUTO_CREATE
//                    );
//                }
//            };
//            t.start();

//
//            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//            String ssid = wifiInfo.getSSID();


            hkzList[0] = new HkzLabel("V00WOH1RUM1TE01", 600, 450, "°C",1,0,"", false,true);
            hkzList[1] = new HkzLabel("V00WOH1RUM1CO01", 600, 500, " ppm",2,0,"", false,true);
            hkzList[2] = new HkzLabel("A00TER1GEN1TE01", 420, 0, "°C",3,0,"", false,true);
            hkzList[3] = new HkzLabel("V00KUE1RUM1TE02", 300, 1200, "°C",4,0,"", false,true);
            hkzList[4] = new HkzLabel("V00KUE1RUM1ST01", 300, 1250, "°C",5,0,"", false,true);
            hkzList[5] = new HkzLabel("Status", 0, 0, "",6,0,"", false,true);
            hkzList[31] = new HkzLabel("CountdownAct", 0, 55, "sec",7,0,"", true,true);
            hkzList[16] = new HkzLabel("A00EIN1GEN1TE01",  600, 1480, "°C",8,0,"", false,true);

            hkzList[6] = new HkzLabel("V01BAD1RUM1TE01",  600, 1400, "°C",101,1,"", false,true);
            hkzList[7] = new HkzLabel("V01BAD1RUM1HU01",  600, 1450, "%",102,1,"", false,true);
            hkzList[8] = new HkzLabel("V01SCH1RUM1TE01",  200, 1200, "°C",103,1,"", false,true);
            hkzList[9] = new HkzLabel("V01KID1RUM1TE01", 700, 300, "°C",104,1,"", false,true);
            hkzList[10] = new HkzLabel("V01SCH1RUM1HE01",  200, 1300, "Lux",105,1,"", true,true);
            hkzList[11] = new HkzLabel("V01SCH1RUM1HU01",  200, 1250, "%",106,1,"", true,true);
            hkzList[12] = new HkzLabel("V01SCH1RUM1AQ01",  200, 1350, "",107,1,"", true, true);
            hkzList[13] = new HkzLabel("V01BUE1RUM1TE01",  200, 300, "°C",108,1,"", false,true);

            hkzList[14] = new HkzLabel("V02ZIM1RUM1TE02", 300, 950, "°C",201,2,"", false,true);
            hkzList[32] = new HkzLabel("V02ZIM1RUM1ST01", 300, 1000, "°C",202,2,"", false,true);

            hkzList[15] = new HkzLabel("Vm1ZIM1RUM1TE01", 600, 400, "°C",901,-1,"", false,true);
            hkzList[17] = new HkzLabel("Vm1ZIM1RUM1BA01",  600, 450, " mbar",903,-1,"", true,true);
            hkzList[18] = new HkzLabel("Vm1GEB1POW1PO01",  600, 550, "Watt",905,-1,"", true,true);
            hkzList[19] = new HkzLabel("V00WOH1POW2PO02",   600, 500, "Watt",906,-1,"", true,true);
            hkzList[20] = new HkzLabel("VIRINF1SAT02", 600, 800, "",907,-1, "Disp Pi", true,false);
            hkzList[21] = new HkzLabel("VIRINF1SAT03", 400, 900, "",908,-1, "Kell Pi",true,false);
            hkzList[22] = new HkzLabel("VIRINF1SAT04", 600, 900, "",909,-1, "Tür  Pi",true,false);
            hkzList[23] = new HkzLabel("Vm1ZIM2RUM1TE01",  300, 1100, "°C",910,-1,"", false,true);
            hkzList[24] = new HkzLabel("Vm1ZIM2RUM1HU01", 300, 1150, "%",911,-1,"", false,true);
            hkzList[25] = new HkzLabel("Vm1ZIM2RUM1TE02",  300, 1300, "°C",912,-1,"", true,true);
            hkzList[26] = new HkzLabel("Vm1ZIM3RUM1TE01",  800, 1100, "°C",913,-1,"", true,true);
            hkzList[27] = new HkzLabel("Vm1ZIM3RUM1TE02",  800, 1150, "°C",914,-1,"", true,true);
            hkzList[28] = new HkzLabel("Vm1ZIM3RUM1TE03", 800, 1250, "°C",915,-1,"", false,true);

            hkzList[29] = new HkzLabel("Vm1ZIM2WAS1QF06", 400, 300, "l/min",816,-2, "",true,true);
            hkzList[30] = new HkzLabel("VIRBEW2PNG01", 600, 950, "",917,-1, "SabH",true,false);
            inpList= new ArrayList<String>();
            for (int i = 0; i < hkzList.length; i++) {
                inpList.add(hkzList[i].Name);
            }
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("MQTTMessage"));
            showMain();
//            Intent intent = new Intent("screenstatus");
//            intent.putExtra("message", "ScreenOn");
//            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            Thread t = new Thread(){
                public void run(){
                    Intent intent = new Intent("screenstatus");
                    intent.putExtra("message", "ScreenOn");
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                }
            };
            t.start();
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            String topic = intent.getStringExtra("topic");
            String[] bits = topic.split("/");
            String lastOne = bits[bits.length - 1];
            if (topic.contains("Settings")){
                getValuesMqtt(message);
            }
            if (inpList != null){//&& inpList.contains(lastOne)) {
                getValuesMqtt(message);
            }
        }
    };

    @Override
    protected void onPause() {
        // when the screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            // this is the case when onPause() is called by the system due to a screen state change
//            unSubscribe("Inputs/HKS/#");
//            unSubscribe("Settings/#");
            Log.e("MYAPP", "SCREEN TURNED OFF");
        } else {
            // this is when onPause() is called when the screen state has not changed
//            unSubscribe("Inputs/HKS/#");
//            unSubscribe("Settings/#");
        }
        //Toast.makeText(getBaseContext(), "Unsub to Inputs", Toast.LENGTH_LONG).show();
        ison = false;
        concounter = 0;
        mescounter = 0;
        Intent intent = new Intent("screenstatus");
        intent.putExtra("message", "ScreenOff");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // only when screen turns on
        //Toast.makeText(getBaseContext(), "Subscribing to Inputs " + concounter + " reconnects, " + mescounter + " messages.", Toast.LENGTH_LONG).show();
        concounter = 0;
        mescounter = 0;

        ison = true;
        startService(new Intent(this, MqttConnectionManagerService.class));
        Intent intent = new Intent("screenstatus");
        intent.putExtra("message", "ScreenOn");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean devel = prefs.getBoolean("checkbox_pref_devel", true);
        Boolean besuch = prefs.getBoolean("cb_besucher_app", false);
        if (!besuch) {
            menu.add(Menu.NONE, 103, 101, "Wecker");
            menu.add(Menu.NONE, 104, 101, "Schaltuhr");
            menu.add(Menu.NONE, 105, 101, "Nachrichten");
            menu.add(Menu.NONE, 106, 101, "App Settings");
            menu.add(Menu.NONE, 98, 101, "Reset Erinnerungen");
        }
        if (devel) {
            menu.add(Menu.NONE, 99, 101, "Haus Szenen");
            menu.add(Menu.NONE, 100, 101, "BDQs");
            menu.add(Menu.NONE, 102, 101, "Alle Settings");
            menu.add(Menu.NONE, 107, 101, "Garten");
        }
        if (besuch & devel) {
            menu.add(Menu.NONE, 106, 101, "App Settings");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();

//        if (id == R.id.og) {
//            showOG();
//            return true;
//        }
//        if (id == R.id.dg) {
//            showDG();
//            return true;
//        }
//        if (id == R.id.eg) {
//            showMain();
//            return true;
//        }
        if (id == 107) {
            showGarten();
            return true;
        }
        if (id == 106) {
            startActivity(new Intent(this, EinstellungenActivity.class));
            return true;
        }
        if (id == 103) {
            subscribeToTopic("DataRequest/Answer/Cron");
            send_mqtt("DataRequest/Request","{\"request\": \"Wecker\"}");
            return true;
        }
        if (id == 104) {
            subscribeToTopic("DataRequest/Answer/Cron");
            send_mqtt("DataRequest/Request","{\"request\": \"Schaltuhr\"}");
            return true;
        }
        if (id == 105) {
            showNachrichten();
            return true;
        }
        if (id == 100) {
            send_mqtt("DataRequest/Request","{\"request\": \"BDQs\"}");
            showBDQs();
            return true;
        }
        if (id == 99) {
            subscribeToTopic("DataRequest/Answer/SzenenGruppen");
            send_mqtt("DataRequest/Request","{\"request\": \"SzenenGruppen\"}");
            return true;
        }
        if (id == 98) {
            subscribeToTopic("DataRequest/Answer/SzenenErinnerung");
            send_mqtt("DataRequest/Request","{\"request\": \"SzenenErinnerung\"}");
            send_mqtt("DataRequest/Request","{\"request\": \"InputsErinnerungen\"}");
            return true;
        }
        if (id == R.id.szsettings) {
            alleSettings = false;
            subscribeToTopic("DataRequest/Answer/Settings");
            send_mqtt("DataRequest/Request","{\"request\": \"GetSettings\"}");
            return true;
        }
        if (id == 102) {
            alleSettings = true;
            subscribeToTopic("DataRequest/Answer/Settings");
            send_mqtt("DataRequest/Request","{\"request\": \"GetSettings\"}");
            return true;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("showTuerSpion")){
            showTuerSpion();
        }
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("setAudioNormal")){
            setAudioNormal();
        }
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("showStopWecker")){
            showStopWecker();
        }
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("switchDispOn")){
            switchDispOn();
        }
        if(intent.getStringExtra("execVoid") != null && intent.getStringExtra("execVoid").equals("startOnTop")){
            finish();
            starterIntent.putExtra("execVoid","vordergrund");
            startActivity(starterIntent);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Screen On
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    public void switchDispOn(){
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                setTurnScreenOn(true);
                setShowWhenLocked(true);
            } else {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }catch (Exception e) {

        }
        onAttachedToWindow();
    }

    public void showStopWecker(){
        level = -2;
        try {
            Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            startActivity(openMe);
            setContentView(R.layout.stop_wecker);
            Button bu = findViewById(R.id.button3);
            bu.setText("Stop Wecker");
            bu.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(MainActivity.this, MqttConnectionManagerService.class);
                    serviceIntent.putExtra("execVoid", "stopWecker");
                    startService(serviceIntent);

                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                    Boolean vordergrund = prefs.getBoolean("checkbox_vordergrund", false);
                    if (!vordergrund) {
                        if (Build.VERSION.SDK_INT >= 27) {
                            //            setTurnScreenOn(true);
                            setShowWhenLocked(false);
                        } else {
                            Window window = getWindow();
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                            //            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                        }
                    }
                    showMain();

                }
            });
//            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
//            final KeyguardManager.KeyguardLock kl = km.newKeyguardLock("MyKeyguardLock");
//            kl.disableKeyguard();
//
//            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
//                    | PowerManager.ON_AFTER_RELEASE, "steuerzentrale:MyWakeLock");
//            wakeLock.acquire();
//            Intent screenOnIntent = new Intent(this, ScreenOnActivity.class);
//            screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //experiment with the flags
//            screenOnIntent.putExtra("execVoid","showStopWecker");
//            startActivity(screenOnIntent);
        }catch (Exception e) {

        }
    }

    public void showViews(int level){
        switch(level) {
            case 0:
                showMain();
                break;
            case -1:
                showUG();
                break;
            case 1:
                showOG();
                break;
            case 2:
                showDG();
                break;
            case -2:
                showGarten();
                break;
        }
    }

    public void showWecker_DataRec(MqttMessage message) {
        unSubscribe("DataRequest/Answer/Cron");
        setContentView(R.layout.wecker);
        level = 10;
        String setWeckers = message.toString();
        try {
            WeckerView wV = new WeckerView(this);
            wV.schlafen_show(setWeckers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void showSZsettings(MqttMessage message) {

        unSubscribe("DataRequest/Answer/Settings");
        setContentView(R.layout.wecker);
        level = 10;
        String szsettings = message.toString();
        try {
            SettingsView sV = new SettingsView(this);
            sV.settings_show(szsettings, alleSettings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ResourceType")
    public void showMain() {
        //String sets = req_from_server("Settings");
        setContentView(R.layout.activity_main);

        level = 0;


        final RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);

        final DigiObj[] doList = new DigiObj[4];
        doList[0] = new DigiObj("V00WOH1TUE1DI01",600, 0, true);
        doList[1] = new DigiObj("V00KUE1TUE1DI01",0, 750, true);
        doList[2] = new DigiObj("V00FLU1TUE1DI01",650, 1450, true);
        doList[3] = new DigiObj("V00FLU1TUE1DI02",700, 1450, true);
        setDigiObj(doList, mrl);

        final ButtonFeatures[] SettList = new ButtonFeatures[1];
        SettList[0] = new ButtonFeatures("Status", "Status", "Status", 0, 0, "",900);
        setSettLabels(SettList, mrl);


        final SzenenButton[] SzList = new SzenenButton[1];
        SzList[0] = new SzenenButton("A/V", Arrays.asList("TV", "SonosEG", "Radio", "AVaus", "Kino", "KinoAus", "LesenEG"), 50, 300);
        setSzenenButton(SzList, mrl);
        final SzenenButton[] SzList1 = new SzenenButton[1];
        SzList1[0] = new SzenenButton("Status", Arrays.asList("Wach", "Leise", "SchlafenGehen", "SchlafenGehenLeise", "Schlafen", "Gehen", "Abwesend", "Besuch"), 50, 450);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean besuch = prefs.getBoolean("cb_besucher_app", false);
        if (!besuch) {
            setSzenenButton(SzList1, mrl);
        }

//        final DeviceButton[] DvList = new DeviceButton[1];
//        DvList[0] = new DeviceButton("Temp", "V00WOH1RUM1ST01", Arrays.asList("17", "Aus", "20.0", "20.5", "21.0", "21.5", "22.0", "22.5", "23.0"), 300, 300);
//        setDeviceButton(DvList, mrl);

        final ButtonFeatures[] Blist = new ButtonFeatures[6];
        Blist[0] = new ButtonFeatures("V00KUE1DEK1LI01", "Toggle", "0 - 1", 100, 1400, "",901);
        Blist[1] = new ButtonFeatures("V00KUE1DEK1LI02", "Toggle", "0 - 1", 100, 1000,"",902);
        Blist[2] = new ButtonFeatures("V00ESS1DEK1LI01", "Toggle", "0 - 1", 170, 750, "",903);
        Blist[3] = new ButtonFeatures("V00FLU1DEK1LI01", "Toggle", "0 - 1", 600, 1100, "", 904);
        Blist[4] = new ButtonFeatures("V00WOH1DEK1LI01", "Toggle", "0 - 1", 300, 400, "", 905);
        Blist[5] = new ButtonFeatures("V00WOH1RUM1DO10", "Toggle", "0 - 1", 700, 300, "", 906);

        Button bu = new Button(this);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.ALIGN_BOTTOM);
        rl.leftMargin = (int) (720/1080.0 * width);
        rl.topMargin = (int) (1400/1920.0 * height);
        bu.setId(1);
        bu.setLayoutParams(rl);
        bu.setBackgroundColor(Color.TRANSPARENT);
        bu.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lampe_aus, 0, 0, 0);
//        bu.setText("Lichter");
        bu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View but = (View) findViewById(1);
                ((ViewManager)but.getParent()).removeView(v);
                setDeviceCommandBut(Blist,mrl);
            }
        });
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        mrl.addView(bu);

        // stummschaltung
        Button qt = new Button(this);
        RelativeLayout.LayoutParams rlqt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlqt.addRule(RelativeLayout.ALIGN_BOTTOM);
        rlqt.leftMargin = (int) (250/1080.0 * width);
        rlqt.topMargin = (int) (1400/1920.0 * height);
        //qt.setId(2);
        qt.setLayoutParams(rlqt);
        qt.setBackgroundColor(Color.TRANSPARENT);

        Boolean stumm = prefs.getBoolean("cb_stumm", true);
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (stumm){
            qt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.mute, 0, 0, 0);
//            mNotificationManager.setInterruptionFilter(3);
        }else{
            qt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.loud, 0, 0, 0);
        }
        qt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Boolean stumm = prefs.getBoolean("cb_stumm", true);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cb_stumm", !stumm);
                editor.commit();
//                if (!stumm) {
//                    mNotificationManager.setInterruptionFilter(3);
//                }else{
//                    mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
//                }
                showMain();
            }
        });
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        if (!besuch) {
            mrl.addView(qt);
        }

        Button st = new Button(this);
        RelativeLayout.LayoutParams rlst = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlst.addRule(RelativeLayout.ALIGN_BOTTOM);
        rlst.leftMargin = (int) (400/1080.0 * width);
        rlst.topMargin = (int) (1400/1920.0 * height);
        //st.setId(2);
        st.setLayoutParams(rlst);
        st.setBackgroundColor(Color.TRANSPARENT);
        st.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon, 0, 0, 0);
//        bu.setText("Lichter");
        st.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alleSettings = false;
                subscribeToTopic("DataRequest/Answer/Settings");
                send_mqtt("DataRequest/Request","{\"request\": \"GetSettings\"}");
            }
        });
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        if (!besuch) {
            mrl.addView(st);
        }

        updateView("");


        mrl.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            @Override
            public void onClick() {
                super.onClick();
                // your on click here
            }

            @Override
            public void onDoubleClick() {
                super.onDoubleClick();
                showTuerSpion();
                // your on onDoubleClick here
            }

            @Override
            public void onLongClick() {
                super.onLongClick();
                // your on onLongClick here
            }

            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                level--;
                if (level <-1){
                    level = 2;
                }
                showViews(level);
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                level++;
                if (level > 2){
                    level = -1;
                }
                showViews(level);
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                level=-2;
                showViews(level);
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                level=-2;
                showViews(level);
            }
        });

        }

    public void updateView(final String hks) {

        RelativeLayout mrl  = null;
        switch(level) {
            case 0:
                mrl = findViewById(R.id.relLayout);
                if (hks.equals("") || hks.contains("Alarmanlage") || hks.contains("Status")) {
                    updateMain("Alarmanlage");
                }
                break;
            case -1:
                mrl  = findViewById(R.id.relLayoutUG);
                break;
            case 1:
                mrl  = findViewById(R.id.relLayoutOG);
                break;
            case 2:
                mrl  = findViewById(R.id.relLayoutDG);
                break;
            case -2:
                mrl  = findViewById(R.id.relLayoutOS);
                break;
        }

        boolean found = false;
        final HkzLabel[] updList = new HkzLabel[listSize];
        Integer j = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean devel = prefs.getBoolean("checkbox_pref_devel", true);
        if (hks.equals("")){
            for (int i = 0; i < hkzList.length; i++) {
                    if (hkzList[i] != null && hkzList[i].level != null && hkzList[i].level == level){
                        if (hkzList[i].enabled  && (! hkzList[i].devel || devel)) {
                            updList[j] = hkzList[i];
                            found = true;
                            j++;
                        }
                    }
            }
        }else{
            for (int i = 0; i < hkzList.length; i++) {
                if (hkzList[i] != null && hkzList[i].Name != null && hkzList[i].Name.equals(hks)) {
                    if (hkzList[i].level == level){
                        if (hkzList[i].enabled  && (! hkzList[i].devel || devel)) {
                            updList[0] = hkzList[i];
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        if (found) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {

                    setInptLabels(updList, mrl);
//                }
//            });
        }
        if ((hks.contains("Alarmanlage") || hks.contains("Status") || hks.contains("TUE1DI0")) && level == 0){
            updateMain(hks);
        }
        if ((hks.contains("TUE1DI0")) && level == -1){
            showUG();
        }
        if ((hks.contains("FEN")) && level == 2){
            showDG();
        }
        if ((hks.contains("FEN")) && level == 1){
            showOG();
        }
        if ((hks.contains("DO")) && level == -2){
            showGarten();
        }

    }


    @SuppressLint("ResourceType")
    public void updateMain(String hks) {
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        boolean found;

        final DigiObj[] doList = new DigiObj[4];
        doList[0] = new DigiObj("V00WOH1TUE1DI01",600, 0, true);
        doList[1] = new DigiObj("V00KUE1TUE1DI01",0, 750, true);
        doList[2] = new DigiObj("V00FLU1TUE1DI01",650, 1450, true);
        doList[3] = new DigiObj("V00FLU1TUE1DI02",700, 1450, true);

        final DigiObj[] updListdo = new DigiObj[1];
        found = false;
        for (int i = 0; i < doList.length; i++) {
            if (doList[i].HKS.equals(hks)){
                updListdo[0] = doList[i];
                found = true;
            }
        }
        if (found) {
            setDigiObj(updListdo, mrl);
        }
        if (hks.contains("Alarmanlage") || hks.contains("Status")) {
            ValueList sensor = sensoren.get("Alarmanlage");
            ValueList status = sensoren.get("Status");
            if (sensor != null) {
                String valueread = sensor.getValue();
                if (valueread.equals("True")) {
                    View but = (View) findViewById(2);
                    if (but != null) {
                        ((ViewManager) but.getParent()).removeView(but);
                    }
                    Button bu2 = new Button(this);
                    RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    rl.addRule(RelativeLayout.ALIGN_BOTTOM);
                    rl.leftMargin = (int) (350 / 1080.0 * width);
                    rl.topMargin = (int) (700 / 1920.0 * height);
                    bu2.setId(2);
                    bu2.setLayoutParams(rl);
                    bu2.setBackgroundColor(Color.TRANSPARENT);
                    if (status != null && status.getValue().equals("Alarm")) {
                        bu2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ausnahmez, 0, 0, 0);
                        //        bu.setText("Lichter");
                        bu2.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                View but = (View) findViewById(2);
                                ((ViewManager) but.getParent()).removeView(v);
                                send_mqtt("Command/Szene/AlarmAus", "{\"Szene\":\"AlarmAus\"}");
                            }
                        });
                        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
                        mrl.addView(bu2);
                    } else {
                        bu2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.alarmscharf, 0, 0, 0);
                        //        bu.setText("Lichter");
                        bu2.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                View but = (View) findViewById(2);
                                ((ViewManager) but.getParent()).removeView(v);
                                // send_mqtt("DataRequest/SetSettings/", "{\"Name\":\"Alarmanlage\",\"Value\":\"False\"}");
                                send_mqtt("Command/Szene/AlarmanlageAus", "{\"Szene\":\"AlarmanlageAus\"}");
                            }
                        });
                        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
                        mrl.addView(bu2);
                    }
                } else {
                    View but = (View) findViewById(2);
                    if (but != null) {
                        ((ViewManager) but.getParent()).removeView(but);
                    }
                }
            }
        }
    }

    public void setDeviceCommandBut(final ButtonFeatures[] Blist, RelativeLayout mrl){
        for (int i = 0; i < Blist.length; i++) {

            Button bu = new Button(this);
            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.addRule(RelativeLayout.ALIGN_BOTTOM);
            rl.leftMargin = (int) (Blist[i].x_value/1080.0 * width);
            rl.topMargin = (int) (Blist[i].y_value/1920.0 * height);
            rl.width = (int) (180.0/1080 * width);
            //rl.height = buttonH;
            bu.setLayoutParams(rl);
            bu.setText(Blist[i].Text);
            final String name = Blist[i].Name;
            final String command = Blist[i].Command;
            bu.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
//                    send_to_server("{'Device':'" + name + "', 'Command':'" + command + "'}");
                    send_mqtt("Command/Device/" + name, "{\"Device\":\"" + name + "\", \"Command\":\"" + command + "\"}");
                }
            });
            //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
            mrl.addView(bu);

        }
    }

    public void setSzenenButton(final SzenenButton[] SBList, RelativeLayout mrl){
        for (int i = 0; i < SBList.length; i++) {
            final int k = i;
            final Button but = new Button(this);
            RelativeLayout.LayoutParams rlt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlt.addRule(RelativeLayout.ALIGN_BOTTOM);
            rlt.leftMargin = (int) (SBList[i].x_value/1080.0 * width);
            rlt.topMargin = (int) (SBList[i].y_value/1920.0 * height);
            rlt.width = (int) (240.0/1080 * width);
            but.setLayoutParams(rlt);
            but.setText(SBList[i].Name);
            but.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MainActivity.this, but);
                    for (int j = 0; j < SBList[k].Szenen.size(); j++) {
                        popup.getMenu().add((String) (SBList[k].Szenen.get(j)));
                    }
                    popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
//                            send_to_server("{'Szene':'" + item.getTitle() + "'}");
                            send_mqtt("Command/Szene/" + item.getTitle(), "{\"Szene\":\"" + item.getTitle() + "\"}");
                            return true;
                        }
                    });
                    popup.show();
                }
            });
            mrl.addView(but);
        }
    }

    public void setDropDButton(final DropDListContainer DropDListC, RelativeLayout mrl){
            final Button but = new Button(this);
            RelativeLayout.LayoutParams rlt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlt.addRule(RelativeLayout.ALIGN_BOTTOM);
            rlt.leftMargin = (int) (DropDListC.x_value/1080.0 * width);
            rlt.topMargin = (int) (DropDListC.y_value/1920.0 * height);
            rlt.width = (int) (300.0/1080 * width);
            but.setLayoutParams(rlt);
            but.setText(DropDListC.Name);
            but.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MainActivity.this, but);
                    for (int j = 0; j < DropDListC.Szenen.length; j++) {
                        popup.getMenu().add(DropDListC.Szenen[j].GroupId, DropDListC.Szenen[j].ItemId, DropDListC.Szenen[j].Position, DropDListC.Szenen[j].Title);
                    }
                    popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
//                            send_to_server("{'Szene':'" + item.getTitle() + "'}");
                            Integer id = item.getItemId();
                            String command = DropDListC.getCommandById(id,DropDListC.Szenen);
                            send_mqtt("Command/Szene/" + command, "{\"Szene\":\"" + command + "\"}");
                            return true;
                        }
                    });
                    popup.show();
                }
            });
            mrl.addView(but);

    }

    public void setDeviceButton(final DeviceButton[] SBList, RelativeLayout mrl){
        for (int i = 0; i < SBList.length; i++) {
            final int k = i;
            final Button but = new Button(this);
            RelativeLayout.LayoutParams rlt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlt.addRule(RelativeLayout.ALIGN_BOTTOM);
            rlt.leftMargin = (int) (SBList[i].x_value/1080.0 * width);
            rlt.topMargin = (int) (SBList[i].y_value/1920.0 * height);
            rlt.width = (int) (210.0/1080 * width);
            but.setLayoutParams(rlt);
            but.setText(SBList[i].Name);
            but.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MainActivity.this, but);
                    for (int j = 0; j < SBList[k].Szenen.size(); j++) {
                        popup.getMenu().add((String) (SBList[k].Szenen.get(j)));
                    }
                    popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
//                            send_to_server("{'Device':'" + SBList[k].Hks + "', 'Command':'" + item.getTitle() + "'}" );
                            send_mqtt("Command/Device/" + SBList[k].Hks, "{\"Device\":\"" + SBList[k].Hks + "\", \"Command\":\"" + item.getTitle() + "\"}");
                            return true;
                        }
                    });
                    popup.show();
                }
            });
            mrl.addView(but);
        }
    }

//
    public void getMqttConnection() {

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            final String clientId = prefs.getString("mqttId", "");

            final String mqttServer = prefs.getString("mqttServer", "");
            final String mqttPort = prefs.getString("mqttPort", "");
            final String mqttUser = prefs.getString("mqttUser", "");
            final String mqttPass = prefs.getString("mqttPass", "");

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {


            if (clientId.equals(new String()) || (mqttPort.equals(new String())) || (mqttServer.equals(new String()))
                    || (mqttUser.equals(new String())) || (mqttPass.equals(new String()))) {
                startActivity(new Intent(MainActivity.this, EinstellungenActivity.class));
            } else {

                String clientId2 = clientId + "_app_" + System.currentTimeMillis();
                String mqttServer2 = "ssl://" + mqttServer + ":" + mqttPort;

                mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), mqttServer2, clientId2);
                mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {

                        if (reconnect) {
                            addToHistory("Reconnected to : " + serverURI);
                            // Because Clean Session is true, we need to re-subscribe
                            if (ison) {
//                                subscribeToTopic("Inputs/HKS/#");
//                                subscribeToTopic("Settings/#");
                            }
                        } else {
                            addToHistory("Reconnected to: " + serverURI);
                            if (ison) {
//                                subscribeToTopic("Inputs/HKS/#");
//                                subscribeToTopic("Settings/#");
                            }
                        }
//                        concounter++;
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        addToHistory("The Connection was lost.");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                    addToHistory("Incoming message: " + new String(message.getPayload()));
//                        mescounter++;
                        if (topic.toLowerCase().contains("Alarmliste".toLowerCase())) {
                            zerlegeAlarmList(message);
                            itemsAdapter.notifyDataSetChanged();
                        } else if (topic.toLowerCase().contains("Image".toLowerCase())) {
                            showPic(message);
                            if (!message.isRetained() & !ison) {
                                makeImageNotific("Klingel", "Bild", message);
                            }
                        } else if (topic.toLowerCase().contains("Cron".toLowerCase())) {
                            showWecker_DataRec(message);
                        } else if (topic.toLowerCase().contains("Answer/SzenenGruppen".toLowerCase())) {
                            showSzenen(message);
                        } else if (topic.toLowerCase().contains("Answer/SzenenErinnerung".toLowerCase())) {
                            showSzenen(message);
                        } else if (topic.toLowerCase().contains("Answer/Settings".toLowerCase())) {
                            showSZsettings(message);
                        } else if (topic.toLowerCase().contains("Answer/BDQs".toLowerCase())) {
                            zerlegeBDQs(message);
                            itemsAdapter.notifyDataSetChanged();
                        } else if (ison && (topic != null)) {
//                            String[] bits = topic.split("/");
//                            String lastOne = bits[bits.length - 1];
//                            if (inpList != null && inpList.contains(lastOne)) {
//                                getValuesMqtt(message);
//                            }
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });

                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setAutomaticReconnect(true);
                mqttConnectOptions.setCleanSession(true);
                mqttConnectOptions.setUserName(mqttUser);
                mqttConnectOptions.setPassword(mqttPass.toCharArray());
                mqttConnectOptions.setKeepAliveInterval(600);

                try {
                    mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                            disconnectedBufferOptions.setBufferEnabled(true);
                            disconnectedBufferOptions.setBufferSize(100);
                            disconnectedBufferOptions.setPersistBuffer(false);
                            disconnectedBufferOptions.setDeleteOldestMessages(false);
                            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                            if (ison) {
//                                subscribeToTopic("Inputs/HKS/#");
//                                subscribeToTopic("Settings/#");
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            addToHistory("Failed to connect");
                        }
                    });


                } catch (MqttException ex) {
                    ex.printStackTrace();
                }

            }


    }

    public void getValuesMqtt(String message){
        String telegram = message.toString();
        addToHistory("Decoding: " + message.toString());
        try{
            JSONObject jMqtt = new JSONObject(telegram);
            String jValue = jMqtt.optString("Value");
//            String jDesc = jMqtt.optString("Description").toString();
            String jKey = "leer";
            if (jMqtt.has("HKS")) {
                jKey = jMqtt.optString("HKS");
            }
            if (jMqtt.has("Setting")) {
                jKey = jMqtt.optString("Setting");
            }
            String TS = jMqtt.optString("ts");
            ValueList sensor = new ValueList(jKey, jValue, TS, "");
            sensoren.put(jKey, sensor);
            updateView(jKey);
        } catch (JSONException e) {
            addToHistory("Could not decode: " + message.toString());
        }
    }

    public void zerlegeAlarmList(MqttMessage message){
        String telegram = message.toString();
        messages.clear();
        try{
            JSONObject jMqtt = new JSONObject(telegram);
//            JSONArray arr = jMqtt.getJSONArray("value");

            Iterator<String> keys = jMqtt.keys();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");

            List<alarmlist> alarme = new ArrayList<alarmlist>();
            while(keys.hasNext()) {
                String key = keys.next();
                if (jMqtt.get(key) instanceof JSONObject) {
                    JSONObject jKey = jMqtt.getJSONObject(key);
                    String ts = jKey.optString("ts").toString();
                    String Text = ts + ": " + jKey.optString("Text").toString();
                    try {
                        Date date = dateFormat.parse(ts);
                        alarmlist newalarm = new alarmlist(date, Text, jKey.optString("uuid").toString());
                        alarme.add(newalarm);
                    } catch (ParseException e) {
                    }
                }
            }
            Collections.sort(alarme);

            for(alarmlist alarm : alarme){
                uuids.put(alarm.toString(),alarm.getuuid());
                messages.add(new String(alarm.toString()));
            }

//            while(keys.hasNext()) {
//                String key = keys.next();
//                if (jMqtt.get(key) instanceof JSONObject) {
//                    JSONObject jKey = jMqtt.getJSONObject(key);
//                    String ts = jKey.optString("ts").toString();
//                    String Text = ts + ": " + jKey.optString("Text").toString();
//                    uuids.put(Text,jKey.optString("uuid").toString());
//                    messages.add(new String(Text));
//                }
//            }
            itemsAdapter.notifyDataSetChanged();
        } catch (JSONException e) {

        }
        unSubscribe("Message/Alarmliste");
    }

    public void zerlegeBDQs(MqttMessage message){
        String telegram = message.toString();
        messages.clear();
        try{
            JSONObject jMqtt = new JSONObject(telegram);
            JSONArray arr = jMqtt.getJSONArray("payload");
            final int lengthJsonArr = arr.length();
            for (int i = 0; i < lengthJsonArr; i++) {
                messages.add(new String(arr.getString(i)));
            }

//            List<alarmlist> alarme = new ArrayList<alarmlist>();
//            while(keys.hasNext()) {
//                String key = keys.next();
//                if (jMqtt.get(key) instanceof JSONObject) {
//                    JSONObject jKey = jMqtt.getJSONObject(key);
//                    String ts = jKey.optString("ts").toString();
//                    String Text = ts + ": " + jKey.optString("Text").toString();
//                    try {
//                        Date date = dateFormat.parse(ts);
//                        alarmlist newalarm = new alarmlist(date, Text, jKey.optString("uuid").toString());
//                        alarme.add(newalarm);
//                    } catch (ParseException e) {
//                    }
//                }
//            }
//            Collections.sort(alarme);
//
//            for(alarmlist alarm : alarme){
//                uuids.put(alarm.toString(),alarm.getuuid());
//                messages.add(new String(alarm.toString()));
//            }

            itemsAdapter.notifyDataSetChanged();
        } catch (JSONException e) {

        }
        unSubscribe("DataRequest/Answer/BDQs");
    }

    public void showPic(MqttMessage message){
        // Convert bytes data into a Bitmap
        byte[] bytearray = message.getPayload();

        Bitmap bmp= BitmapFactory.decodeByteArray(bytearray,0,bytearray.length);
//        ImageView image=new ImageView(this);
        if (picNr == 1) {
//            saveImage(this, bmp, "tuerspion1.jpg");
            picNr = 2;
        } else {
//            saveImage(this, bmp, "tuerspion2.jpg");
            picNr = 1;
        }
        if (!message.isRetained()) {
            showTuerSpion();
        }
    }

//    public void saveImage(Context context, Bitmap b, String imageName) {
//        FileOutputStream foStream;
//        try {
//            foStream = context.openFileOutput(imageName, Context.MODE_PRIVATE);
//            b.compress(Bitmap.CompressFormat.PNG, 100, foStream);
//            foStream.close();
//        } catch (Exception e) {
//            Log.d("saveImage", "Exception 2, Something went wrong!");
//            e.printStackTrace();
//        }
//    }

    public void showTuerSpion(){

        Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
        openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
        startActivity(openMe);
        // Convert bytes data into a Bitmap
        setContentView(R.layout.tuercam);
        level = 10;
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
                img.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

                    @Override
                    public void onClick() {
                        super.onClick();
                        // your on click here
                        //Command/TuerSPIon/V00FLU1TUE1PC01 {"Device": "V00FLU1TUE1PC01", "Id": 1, "ts": "2018-11-21 09:22:08", "Name": "Take_Pic"}

                    }

                    @Override
                    public void onDoubleClick() {
                        super.onDoubleClick();
                        send_mqtt("Command/TuerSPIon/V00FLU1TUE1PC01", "{\"Device\":\"V00FLU1TUE1PC01\", \"Name\":\"Take_Pic\"}");
                        // your on onDoubleClick here
                    }

                });
            }
        }


//        bmp= loadImageBitmap(this.getApplicationContext(), "tuerspion1.jpg");
//        ImageView image = (ImageView) findViewById(R.id.image1);
//        image.setImageBitmap(bmp);
//        bmp= loadImageBitmap(this.getApplicationContext(), "tuerspion2.jpg");
//        image = (ImageView) findViewById(R.id.image2);
//        image.setImageBitmap(bmp);
//        LinearLayout mrl  = (LinearLayout) findViewById(R.id.linLayout);


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

    public void setInptLabels(HkzLabel[] bfList, RelativeLayout mrl){

        for (int i = 0; i < bfList.length; i++) {
            if (bfList[i] != null) {
                TextView tv = null;
                TextView tvBack = null;
                try {
                    tv = findViewById(bfList[i].id + 123000);
                    tvBack = findViewById(bfList[i].id + 124000);
                } catch (Exception e) {

                }
                ValueList sensor = sensoren.get(bfList[i].Name);
                RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                rl.addRule(RelativeLayout.ALIGN_BOTTOM);
                rl.leftMargin = (int) (bfList[i].x_value / 1080.0 * width);
                rl.topMargin = (int) (bfList[i].y_value / 1920.0 * height);
                if (tv == null) {
                    tv = new TextView(this);
                    tv.setTag(bfList[i].Name);
                    tv.setId(bfList[i].id + 123000);
                }
                if (tvBack == null) {
                    tvBack = new TextView(this);
                    tvBack.setTag(bfList[i].Name);
                    tvBack.setId(bfList[i].id + 124000);
                }


                if (sensor != null) {
                    try {
                        tv.setLayoutParams(rl);
                        tvBack.setLayoutParams(rl);
                        String valueread = sensor.getValue();
                        if (bfList[i].Name.toLowerCase().contains("VIR".toLowerCase())) {
                            tv.setText(bfList[i].text);
                            tvBack.setText(bfList[i].text);
                        }else if (bfList[i].unit.contains("sec")){
                            tv.setText(valueread + bfList[i].unit);
                            tvBack.setText(valueread + bfList[i].unit);
                        } else {
                            tv.setText(valueread + bfList[i].unit);
                            tvBack.setText(valueread + bfList[i].unit);
                        }
                        String lastRec_s = sensor.getTS();
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ENGLISH);
                        Date date = new Date(0);
                        Date jetzt = new Date();
                        try {
                            date = format.parse(lastRec_s);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        tv.setBackgroundColor(Color.WHITE);
                        tvBack.setBackgroundColor(Color.WHITE);
                        long diff = jetzt.getTime() - date.getTime();
                        // Initializing a ShapeDrawable
                        ShapeDrawable sd = new ShapeDrawable();
                        // Specify the shape of ShapeDrawable
                        sd.setShape(new RectShape());
                        // Set the border width
                        sd.getPaint().setStyle(Paint.Style.FILL);
                        sd.getPaint().setColor(Color.WHITE);
                        sd.getPaint().setStrokeWidth(10f);
                        // Specify the style is a Stroke
                        sd.getPaint().setStyle(Paint.Style.STROKE);
                        if (diff > (1000 * 60 * 60)) {
                            sd.getPaint().setColor(Color.DKGRAY);
                        } else if (diff > (1000 * 60 * 240)) { // 60 to 240
                            sd.getPaint().setColor(Color.RED);
                        } else if (diff > (1000 * 60 * 30)) { // 30 to 60
                            sd.getPaint().setColor(Color.YELLOW);
                        } else if (diff > (1000 * 60 * 10)) { // 10 to 30
                            sd.getPaint().setColor(Color.BLUE);
                        } else if (diff > (1000 * 60 * 5)) { // 5 to 10 min
                            sd.getPaint().setColor(Color.CYAN);
                        } else {  // less than 5 min
                            sd.getPaint().setColor(Color.GREEN);
                        }
                        // Finally, add the drawable background to TextView
                        tv.setBackground(sd);
                        mrl.addView(tvBack);
                        mrl.addView(tv);
                    } catch (Exception e) {

                    }
                }
            }
        }
    }

    public void setDigiObj(DigiObj[] doList, RelativeLayout mrl){

        for (int i = 0; i < doList.length; i++) {
            View tv = new View(this);
            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.addRule(RelativeLayout.ALIGN_BOTTOM);
            rl.leftMargin = (int) (doList[i].x_value/1080.0 * width);
            rl.topMargin = (int) (doList[i].y_value/1920.0 * height);
            rl.height = (int) (30/1920.0 * height);
            rl.width = (int) (30/1920.0 * height);
            tv.setLayoutParams(rl);
            ValueList sensor = sensoren.get(doList[i].HKS);
            if (sensor != null){
                String valueread = sensor.getValue();
                Float value = Float.parseFloat(valueread);
                Boolean inv = doList[i].inverse;
                //tv.setText("x");
                if (((value > 0) & (!inv)) || ((value == 0) & (inv))) { // 60 to 240
                    tv.setBackgroundColor(Color.GREEN);
                }else {  // less than 5 min
                    tv.setBackgroundColor(Color.RED);
                }
                mrl.addView(tv);
            }
        }
    }

    public void setSettLabels(ButtonFeatures[] bfList, RelativeLayout mrl){
        //String werte = req_from_server("Settings");
        String werte = "";
        try {
            JSONObject jInpts = new JSONObject(werte);
            for (int i = 0; i < bfList.length; i++) {
                TextView tv = new TextView(this);
                RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                rl.addRule(RelativeLayout.ALIGN_BOTTOM);
                rl.leftMargin = (int) (bfList[i].x_value/1080.0 * width);
                rl.topMargin = (int) (bfList[i].y_value/1920.0 * height);
                //rl.width = 160;
                //rl.height = buttonH;
                tv.setLayoutParams(rl);
                //JSONObject jInpt = jInpts.getJSONObject(bfList[i].Name);
                String valueread = jInpts.optString(bfList[i].Name).toString();
                tv.setText(valueread);
                tv.setBackgroundColor(Color.WHITE);
                //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
                mrl.addView(tv);
            }

        } catch (JSONException e) {

        }
    }

    @SuppressLint("ResourceType")
    public void showOG() {
        //String sets = req_from_server("Settings");
        //String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.obergeschoss);
        level = 1;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean devel = prefs.getBoolean("checkbox_pref_devel", true);

        final RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutOG);

//        final ButtonFeatures[] Blist = new ButtonFeatures[2];
//        Blist[0] = new ButtonFeatures("V01FLU1DEK1LI01", "Off", "Aus", 700, 750, "",14);
//        Blist[1] = new ButtonFeatures("V01SCH1DEK1LI01", "off", "Aus", 200, 1000, "",15);
//        setDeviceCommandBut(Blist,mrl);

        final DigiObj[] doList = new DigiObj[5];
        doList[0] = new DigiObj("V01BAD1FEN1DI01",700, 1500, true);
        doList[1] = new DigiObj("V01SCH1FEN1DI01",190, 1500, true);
        doList[2] = new DigiObj("V01SCH1FEN2DI01",20, 900, true);
        doList[3] = new DigiObj("V01BUE1FEN1DI01",20, 600, true);
        doList[4] = new DigiObj("V01BUE1FEN2DI01",190, 50, true);
        setDigiObj(doList, mrl);

        final ButtonFeatures[] Blist = new ButtonFeatures[6];
        Blist[0] = new ButtonFeatures("V01FLU1DEK1LI01", "Toggle", "0 - 1", 700, 750, "",914);
        Blist[1] = new ButtonFeatures("V01SCH1DEK1LI01", "Toggle", "0 - 1", 200, 1000, "",915);
        Blist[2] = new ButtonFeatures("V01BAD1DEK1LI01", "Toggle", "0 - 1", 700, 1100, "",916);
        Blist[3] = new ButtonFeatures("V01BAD1DEK1LI02", "Toggle", "0 - 1", 700, 1200, "", 917);
        Blist[4] = new ButtonFeatures("V01KID1DEK1LI01", "Toggle", "0 - 1", 700, 150, "", 918);
        Blist[5] = new ButtonFeatures("V01BUE1DEK1LI01", "Toggle", "0 - 1", 200, 200, "", 919);

        Button bu = new Button(this);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.ALIGN_BOTTOM);
        rl.leftMargin = (int) (720/1080.0 * width);
        rl.topMargin = (int) (1400/1920.0 * height);
        bu.setId(1);
        bu.setLayoutParams(rl);
        bu.setBackgroundColor(Color.TRANSPARENT);
        bu.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lampe_aus, 0, 0, 0);
        bu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View but = (View) findViewById(1);
                ((ViewManager)but.getParent()).removeView(v);
                setDeviceCommandBut(Blist,mrl);
            }
        });
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        mrl.addView(bu);

        updateView("");
        mrl.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                level--;
                if (level <-1){
                    level = 2;
                }
                showViews(level);
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                level++;
                if (level > 2){
                    level = -1;
                }
                showViews(level);
            }
        });
    }

    public void showDG() {
        //String sets = req_from_server("Settings");
        //String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.dachgeschoss);
        level = 2;
        updateView("");
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutDG);

        final ButtonFeatures[] Blist = new ButtonFeatures[4];
        Blist[0] = new ButtonFeatures("V00ZIM0RUM0DO01", "on", "Ein", 700, 400, "",14);
        Blist[1] = new ButtonFeatures("V02BAD1DEK1LI01", "Toggle", "0 - 1", 650, 1050, "",15);
        Blist[2] = new ButtonFeatures("V02ZIM1DEK1LI01", "Toggle", "0 - 1", 350, 700, "",16);
        Blist[3] = new ButtonFeatures("V02TRE1DEK1LI01", "Toggle", "0 - 1", 700, 700, "",17);
        setDeviceCommandBut(Blist,mrl);

        final DigiObj[] doList = new DigiObj[3];
        doList[0] = new DigiObj("V02BAD1FEN1DI01",500, 1150, true);
        doList[1] = new DigiObj("V02ZIM1FEN1DI01",0, 600, true);
        doList[2] = new DigiObj("V02ZIM1FEN2DI01",0, 850, true);
        setDigiObj(doList, mrl);

        mrl.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                level--;
                if (level <-1){
                    level = 2;
                }
                showViews(level);
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                level++;
                if (level > 2){
                    level = -1;
                }
                showViews(level);
            }
        });
    }

    public void showUG() {

        setContentView(R.layout.untergeschoss);
        level = -1;
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutUG);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean besuch = prefs.getBoolean("cb_besucher_app", true);

        final DropDList[] DropDEl = new DropDList[2];
        DropDEl[0] = new DropDList(1,1,1,"Server reboot", "RebootProliant");
        DropDEl[1] = new DropDList(1,2,2,"Steuerzentrale neustarten", "SysReboot");
        final DropDListContainer DropDCont = new DropDListContainer("System",DropDEl,300,300);
        if (!besuch) {
            setDropDButton(DropDCont, mrl);
        }

        final DropDList[] DropFWl = new DropDList[6];
        DropFWl[0] = new DropDList(1,1,1,"Firewall Schliessen", "FWClose");
        DropFWl[1] = new DropDList(1,2,2,"Dormi öffnen", "OpenFWDormi");
        DropFWl[2] = new DropDList(1,3,3,"Shellies öffnen", "OpenFWShellies");
        DropFWl[3] = new DropDList(1,4,4,"TVs öffnen", "OpenFWTVs");
        DropFWl[4] = new DropDList(1,5,5,"Küchengeräte öffnen", "OpenHAFWs");
        DropFWl[5] = new DropDList(1,6,6,"Gateways öffnen", "OpenGateWFWs");
        final DropDListContainer DropFWCont = new DropDListContainer("Firewall",DropFWl,600,200);
        if (!besuch) {
            setDropDButton(DropFWCont, mrl);
        }

        final ButtonFeatures[] Blist = new ButtonFeatures[5];
        Blist[0] = new ButtonFeatures("Vm1FLU1DEK1LI01", "Toggle", "0 - 1", 700, 750, "",31);
        Blist[1] = new ButtonFeatures("Vm1ZIM1DEK1LI01", "Toggle", "0 - 1", 300, 400,"",32);
        Blist[2] = new ButtonFeatures("Vm1ZIM2DEK1LI01", "Toggle", "0 - 1", 150, 900,"",33);
        Blist[3] = new ButtonFeatures("Vm1ZIM3DEK1LI01", "Toggle", "0 - 1", 600, 1250,"",34);
        Blist[4] = new ButtonFeatures("Vm1ZIM2WAS1DO01", "Ein", "Ein", 100, 1250,"",34);
        setDeviceCommandBut(Blist,mrl);

        final DigiObj[] doList = new DigiObj[2];
        doList[0] = new DigiObj("Vm1ZIM1TUE1DI02",950, 150, false);
        doList[1] = new DigiObj("Vm1ZIM1TUE1DI03",950, 200, false);
        setDigiObj(doList, mrl);

        Button bu  = (Button) findViewById(R.id.button2);
        bu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setAudioNormal();
            }
        });

        updateView("");
        mrl.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
                level--;
                if (level <-1){
                    level = 2;
                }
                showViews(level);
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                level++;
                if (level > 2){
                    level = -1;
                }
                showViews(level);
            }
        });

    }
    public void showGarten() {
        setContentView(R.layout.garten);
        level = -2;
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutOS);
        final ButtonFeatures[] Blist = new ButtonFeatures[3];
        Blist[0] = new ButtonFeatures("A00GAR1WAS1DO01", "Toggle", "0 - 1", 600, 400, "",801);
        Blist[1] = new ButtonFeatures("A00GAR1WAS2DO01", "Toggle", "0 - 1", 400, 400, "",802);
        Blist[2] = new ButtonFeatures("A00GAR1WAS3DO01", "Toggle", "0 - 1", 200, 400, "",803);
//        Blist[3] = new ButtonFeatures("Vm1ZIM3DEK1LI01", "Toggle", "0 - 1", 600, 1250,"",34);
//        Blist[4] = new ButtonFeatures("Vm1ZIM2WAS1DO01", "Ein", "Ein", 100, 1250,"",34);
        setDeviceCommandBut(Blist,mrl);

        final DigiObj[] doList = new DigiObj[3];
        doList[0] = new DigiObj("A00GAR1WAS1DO01",600, 380, true);
        doList[1] = new DigiObj("A00GAR1WAS2DO01",400, 380, true);
        doList[2] = new DigiObj("A00GAR1WAS3DO01",200, 380, true);
        setDigiObj(doList, mrl);

        updateView("");

        mrl.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {

            @Override
            public void onClick() {
                super.onClick();
                // your on click here
            }

            @Override
            public void onDoubleClick() {
                super.onDoubleClick();
                // your on onDoubleClick here
            }

            @Override
            public void onLongClick() {
                super.onLongClick();
                // your on onLongClick here
            }

            @Override
            public void onSwipeUp() {
                super.onSwipeUp();
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                level=0;
                showViews(level);
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                level=0;
                showViews(level);
            }
        });

    }

    public void setAudioNormal(){
        AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0);
        manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 10, 0);
        manager.setStreamVolume(AudioManager.STREAM_SYSTEM, 10, 0);
        manager.setStreamVolume(AudioManager.STREAM_RING, 10, 0);
    }

    public void showSzenen(MqttMessage message){

        unSubscribe("DataRequest/Answer/SzenenGruppen");
        unSubscribe("DataRequest/Answer/SzenenErinnerung");
        setContentView(R.layout.szenen);
        level = 10;

        JSONObject jMqtt = null;
        final Map<String, Map<String, String>> szenen = new HashMap<String, Map<String, String>>();
//        ArrayList<String> typen = new ArrayList<String>();
        ArrayList<String> hilfsliste = new ArrayList<String>();

        try {
            jMqtt = new JSONObject(message.toString());
            JSONArray jArr = new JSONArray(jMqtt.optString("payload"));
            final int lengthJsonArr = jArr.length();
            for (int i = 0; i < lengthJsonArr; i++) {
                Map<String, String> hilfsliste2 = new HashMap<String, String>();
                JSONObject jsonChildNode = jArr.getJSONObject(i);
                String Name = jsonChildNode.optString("Name").toString();
                String Gruppe = jsonChildNode.optString("Gruppe").toString();
                String Desc = jsonChildNode.optString("Beschreibung").toString();
//                try {
//                    typen.add(Gruppe);
//                } catch (Exception e) {
//
//                }
                if (Gruppe != null && !Gruppe.equals("") && !Gruppe.equals("null"))
                    if (szenen.get(Gruppe) == null){
                        hilfsliste.add(Name);
                        hilfsliste2.put(Name,Desc);
                        szenen.put(Gruppe, hilfsliste2);
                    } else{
                        hilfsliste2 = szenen.get(Gruppe);
                        hilfsliste.add(Name);
                        if (Desc != null) {
                            hilfsliste2.put(Name, Desc);
                        }else{
                            hilfsliste2.put(Name, Name);
                        }
                        szenen.put(Gruppe, hilfsliste2);
                    }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.spinner1);
//create a list of items for the spinner.
//        String[] items = null;//new String[]{"1", "2", "three"};
        final ArrayList<String> items2 = new ArrayList<String>();
        int i = 0;
        for ( String key : szenen.keySet() ) {
            items2.add(key);
            i = i + 1;
        }
//create an adapter to describe how the items are displayed, adapters are used in several places in android.
//There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items2);
//set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selected = items2.get(position);
                Map<String, String> hilfsliste3 = new HashMap<String, String>();
                hilfsliste3 = szenen.get(selected);
                LinearLayout ll = findViewById(R.id.linlayout);
                ll.removeAllViews();
                for (final String temp : hilfsliste3.keySet()) {
                    String descri = hilfsliste3.get(temp);
                    if (descri != null && !descri.equals("") && !descri.equals("null")){
                        final Button b = new Button(MainActivity.this);
                        b.setText(descri);
                        ll.addView(b);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                send_mqtt("Command/Szene/" + temp, "{\"Szene\":\""+temp+"\"}");
                            }
                        });
                    }

                }
                // your code here
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        int spinnerPosition = adapter.getPosition("Favorit");
        if (spinnerPosition != 0) {
            dropdown.setSelection(spinnerPosition);
        }

    }

    public void showNachrichten() {
        level = 10;
        subscribeToTopic("Message/Alarmliste");
        setContentView(R.layout.notifications);

        lvItems = (ListView) findViewById(R.id.lvItems);
        //items = new ArrayList<String>();
        itemsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, messages);
        lvItems.setAdapter(itemsAdapter);
        //items.add("First Item");
        //items.add("Second Item");
        Button bu = findViewById(R.id.button);
        bu.setText("Alle Nachrichten löschen");
        bu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                send_mqtt("Message/AlarmListClear", "{\"Szene\":\"AlarmListClear\"}");
            }
        });
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);

        setupListViewListener();
    }
    public void showBDQs() {
        level = 10;
        subscribeToTopic("DataRequest/Answer/BDQs");
        setContentView(R.layout.notifications);

        lvItems = (ListView) findViewById(R.id.lvItems);
        //items = new ArrayList<String>();
        itemsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, messages);
        lvItems.setAdapter(itemsAdapter);
        //items.add("First Item");
        //items.add("Second Item");
        //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);

    }

    private void setupListViewListener() {
        lvItems.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapter,
                                                   View item, int pos, long id) {
                        // Remove the item within array at position
                        String text = messages.toArray()[pos].toString();
                        messages.remove(pos);
                         send_mqtt("Message/AlarmOk", "{\"uuid\": \"" + uuids.get(text) + "\"}");
                        // Refresh the adapter
                        itemsAdapter.notifyDataSetChanged();
                        // Return true consumes the long click event (marks it handled)
                        return true;
                    }

                });
    }

    public void subscribeToTopic(String topic){
        try {
//            if (mqttAndroidClient == null){
//                getMqttConnection();
//            }
            mqttAndroidClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });


        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        } catch (Exception e){

        }}

    public void unSubscribe(@NonNull final String topic) {
        try{
        IMqttToken token = mqttAndroidClient.unsubscribe(topic);
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "UnSubscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "UnSubscribe Failed " + topic);
            }
        } );}
        catch (MqttException ex){
                System.err.println("Exception whilst unsubscribing");
                ex.printStackTrace();
        } catch (Exception e){

        };
    }

    private void addToHistory(String mainText){
        System.out.println("App: " + mainText);

    }


//    private void makeNotification(String topic, String body){
//        mNotificationManager = (NotificationManager)
//                this.getSystemService(Context.NOTIFICATION_SERVICE);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.mipmap.steuerzen_icon)
//                        .setContentTitle(topic)
//                        .setStyle(new NotificationCompat.BigTextStyle()
//                                .bigText(body))
//                        .setContentText(body);
//        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);
//        // Configure the notification channel.
//        AudioAttributes att = new AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                .build();
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        notificationChannel.setSound(alarmSound,att);
//        notificationChannel.setDescription(body);
//        notificationChannel.enableLights(true);
//        notificationChannel.setLightColor(Color.RED);
//        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
//        notificationChannel.enableVibration(true);
//        mNotificationManager.createNotificationChannel(notificationChannel);
////        if (imageThumbnail != null) {
////            mBuilder.setStyle(new Notification.BigPictureStyle()
////                    .bigPicture(imageThumbnail).setSummaryText(messageBody));
////        }
//        mBuilder.setContentIntent(contentIntent);
//        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//        NOTIFICATION_ID++;
//    }

    private void makeImageNotific(String topic, String body, MqttMessage message){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean mess = prefs.getBoolean("checkbox_pref_tuerspimess", true);
        if (mess) {
            byte[] bytearray = message.getPayload();

            Bitmap bmp= BitmapFactory.decodeByteArray(bytearray,0,bytearray.length);
            mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.steuerzen_icon)
                            .setContentTitle(topic)
                            .setStyle(new NotificationCompat.BigPictureStyle()
                                    .bigPicture(bmp)
                                    .bigLargeIcon(null))
                            .setContentText(body)
                            .setLargeIcon(bmp);
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            NOTIFICATION_ID++;
        }

    }

//    public void send_to_server(String Befehl){
//        try {
//            SocketClient.broadc(Befehl);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    public void send_mqtt(String topic, String body){
        String sendingMessage = body;
        String sendingTopic = topic;
        try {
//                    connectToBroker();
//            if (mqttAndroidClient == null){
//                getMqttConnection();
//            }
            if (mqttAndroidClient.isConnected()) {
                MqttMessage message = new MqttMessage();
                message.setPayload(sendingMessage.getBytes());
                mqttAndroidClient.publish(sendingTopic, message);
                // System.out.println("XSONDIN sendingTopic = " + sendingTopic);
                // System.out.println("XSONDIN message = " + message);
//                        addDebug("Message Published");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e){

        }
    }


    private void createFileOnDevice(Boolean append) throws IOException {
        /*
         * Function to initially create the log file and it also writes the time of creation to file.
         */
        File Root = Environment.getExternalStorageDirectory();
        if(Root.canWrite()){
            File  LogFile = new File(Root, "kommandoLog.txt");
            FileWriter LogWriter = new FileWriter(LogFile, append);
            out = new BufferedWriter(LogWriter);
            Date date = new Date();
            out.write("Logged at" + String.valueOf(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + "\n"));
            out.close();

        }
    }
    public void writeToFile(String message){
        try {
            out.write(message+"\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }}


//    public String req_from_server(String Befehl){
//        try {
//            String msg = "{'GCM-Client':'";
//            msg = "{'Request_js':'" + Befehl;
//            msg = msg +  "'}";
//            String antwort = SocketClient.request(msg);
//            return antwort;
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return "";
//    }


//    private void registerConnectivityNetworkMonitorForAPI21AndUp() {
//
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkRequest.Builder builder = new NetworkRequest.Builder();
//
//        connectivityManager.registerNetworkCallback(
//                builder.build(),
//                new ConnectivityManager.NetworkCallback() {
//                    /**
//                     * @param network
//                     */
//                    @Override
//                    public void onAvailable(Network network) {
//
//                        sendBroadcast(
//                                getConnectivityIntent(false)
//                        );
//
//                    }
//
//                    /**
//                     * @param network
//                     */
//                    @Override
//                    public void onLost(Network network) {
//
//                        sendBroadcast(
//                                getConnectivityIntent(true)
//                        );
//
//                    }
//                }
//
//        );
//
//    }

    /**
     * @param noConnection
     * @return
     */
//    private Intent getConnectivityIntent(boolean noConnection) {
//
//        Intent intent = new Intent(this,WifiReceiver.class);
//        String packageName = getPackageName();
//        intent.setAction(packageName +".WifiReceiver.onReceive");
//        intent.putExtra(WifiManager.EXTRA_NETWORK_INFO, noConnection);
//
//        return intent;
//
//    }
//
//    public void callAsynchronousTask() {
//        final Handler handler = new Handler();
//        Timer timer = new Timer();
//        TimerTask doAsynchronousTask = new TimerTask() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    public void run() {
//                        try {
//                            PerformBackgroundTask performBackgroundTask = new PerformBackgroundTask();
//                            // PerformBackgroundTask this class is the class that extends AsynchTask
//                            performBackgroundTask.execute(context);
//                        } catch (Exception e) {
//                            // TODO Auto-generated catch block
//                        }
//                    }
//                });
//            }
//        };
//        timer.schedule(doAsynchronousTask, 0, 5 * 60 * 1000); //execute in every 50000 ms
//    }
}
