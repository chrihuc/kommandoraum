package com.asuscomm.chrihuc.schaltzentrale;
import com.asuscomm.chrihuc.schaltzentrale.SocketClient;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
//import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Context context;

    static final String TAG = "Schaltzentrale";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    TextView mDisplay;

    Map<String, ValueList> sensoren = new HashMap<String, ValueList>();

    MqttAndroidClient mqttAndroidClient;

    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;

    public Boolean server_online = true;
    public int height = 1920;
    public int width = 1080;
    public int level = 0;

    public int concounter = 0;
    public int mescounter = 0;
    public boolean ison = true;

    private ArrayList<String> items;
    private ArrayAdapter<String> itemsAdapter;
    private ListView lvItems;
    public ArrayList<String> messages;

    private BroadcastReceiver mReceiver = null;

    @Override
    public void onBackPressed()
    {
        showMain();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            send_to_server("{'Szene':'EGLeiser'}");
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            send_to_server("{'Szene':'EGLauter'}");
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            showMain();
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

        // initialize receiver
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        //setHasOptionsMenu(true);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        context = getApplicationContext();

        Activity act = this;

        messages = new ArrayList<String>();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        height = size.y; //S7 1920 => /1920 * height /moto 888
        width = size.x; //S7 1080 => /1080 * width /moto 540

        Intent i = new Intent(this, EinstellungenActivity.class);
        startActivity(i);

        getMqttConnection();
        showMain();
    }

    @Override
    protected void onPause() {
        // when the screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            // this is the case when onPause() is called by the system due to a screen state change
            unSubscribe("Inputs/#");
            unSubscribe("Settings/Status");
            Log.e("MYAPP", "SCREEN TURNED OFF");
        } else {
            // this is when onPause() is called when the screen state has not changed
            unSubscribe("Inputs/#");
            unSubscribe("Settings/Status");
        }
        //Toast.makeText(getBaseContext(), "Unsub to Inputs", Toast.LENGTH_LONG).show();
        ison = false;
        concounter = 0;
        mescounter = 0;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // only when screen turns on
        //Toast.makeText(getBaseContext(), "Subscribing to Inputs " + concounter + " reconnects, " + mescounter + " messages.", Toast.LENGTH_LONG).show();
        concounter = 0;
        mescounter = 0;
        if (!ScreenReceiver.wasScreenOn) {
            // this is when onResume() is called due to a screen state change
            Log.e("MYAPP", "SCREEN TURNED ON");
            subscribeToTopic("Inputs/#");
            subscribeToTopic("Settings/Status");
        } else {
            // this is when onResume() is called when the screen state has not changed
            subscribeToTopic("Inputs/#");
            subscribeToTopic("Settings/Status");
        }
        ison = true;
        startService(new Intent(this, MqttConnectionManagerService.class));
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();

        if (id == R.id.og) {
            showOG();
            return true;
        }
        if (id == R.id.dg) {
            showDG();
            return true;
        }
        if (id == R.id.eg) {
            showMain();
            return true;
        }
        if (id == R.id.ug) {
            showUG();
            return true;
        }
        if (id == R.id.settings) {
            startActivity(new Intent(this, EinstellungenActivity.class));
            return true;
        }
        if (id == R.id.wecker) {
            showWecker();
            return true;
        }
        if (id == R.id.notifications) {
            showNachrichten();
            return true;
        }
        return true;
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
        }
    }

    public void showWecker() {
        setContentView(R.layout.wecker);
        String setWeckers = req_from_server("Wecker");
        try {
            WeckerView wV = new WeckerView(this);
            wV.schlafen_show(setWeckers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void showMain() {
        //String sets = req_from_server("Settings");
        setContentView(R.layout.activity_main);

        level = 0;

        final ButtonFeatures[] inptList = new ButtonFeatures[6];
        inptList[0] = new ButtonFeatures("V00WOH1RUM1TE01", "V00WOH1RUM1TE01", "V00WOH1RUM1TE01", 600, 450, "°C");
        inptList[1] = new ButtonFeatures("V00WOH1RUM1CO01", "V00WOH1RUM1CO01", "V00WOH1RUM1CO01", 600, 500, " ppm");
        inptList[2] = new ButtonFeatures("A00TER1GEN1TE01", "A00TER1GEN1TE01", "A00TER1GEN1TE01", 420, 0, "°C");
        inptList[3] = new ButtonFeatures("V00KUE1RUM1TE02", "V00KUE1RUM1TE02", "V00KUE1RUM1TE02", 300, 1200, "°C");
        inptList[4] = new ButtonFeatures("V00KUE1RUM1ST01", "V00KUE1RUM1ST01", "V00KUE1RUM1ST01", 300, 1250, "°C");

        inptList[5] = new ButtonFeatures("Status", "Status", "Status", 0, 0, "");
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        setInptLabels(inptList, mrl);

        final DigiObj[] doList = new DigiObj[3];
        doList[0] = new DigiObj("V00WOH1TUE1DI01",600, 0);
        doList[1] = new DigiObj("V00KUE1TUE1DI01",0, 750);
        doList[2] = new DigiObj("V00FLU1TUE1DI01",650, 1450);
        setDigiObj(doList, mrl);

        final ButtonFeatures[] SettList = new ButtonFeatures[1];
        SettList[0] = new ButtonFeatures("Status", "Status", "Status", 0, 0, "");
        setSettLabels(SettList, mrl);


        final SzenenButton[] SzList = new SzenenButton[2];
        SzList[0] = new SzenenButton("A/V", Arrays.asList("TV", "SonosEG", "Radio", "AVaus", "Kino", "KinoAus", "LesenEG"), 50, 300);
        SzList[1] = new SzenenButton("Status", Arrays.asList("Wach", "Leise", "SchlafenGehen", "SchlafenGehenLeise", "Schlafen", "Gehen", "Gegangen"), 50, 450);
        setSzenenButton(SzList, mrl);

        final DeviceButton[] DvList = new DeviceButton[1];
        DvList[0] = new DeviceButton("Temp", "V00WOH1RUM1ST01", Arrays.asList("17", "Aus", "20.0", "20.5", "21.0", "21.5", "22.0", "22.5", "23.0"), 300, 300);
        setDeviceButton(DvList, mrl);

        final ButtonFeatures[] Blist = new ButtonFeatures[3];
        Blist[0] = new ButtonFeatures("V00KUE1DEK1LI01", "Off", "Aus", 100, 1400, "");
        Blist[1] = new ButtonFeatures("V00KUE1DEK1LI02", "Off", "Aus", 100, 1000,"");
        Blist[2] = new ButtonFeatures("V00ESS1DEK1LI01", "Off", "Aus", 170, 750, "");
        for (int i = 0; i < 3; i++) {

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
                    send_to_server("{'Device':'" + name + "', 'Command':'" + command + "'}");
                }
            });
            //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
            mrl.addView(bu);

        }
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
                // your swipe left here.
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                // your swipe right here.
            }
        });
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
                            send_to_server("{'Szene':'" + item.getTitle() + "'}");
                            return true;
                        }
                    });
                    popup.show();
                }
            });
            mrl.addView(but);
        }
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
                            send_to_server("{'Device':'" + SBList[k].Hks + "', 'Command':'" + item.getTitle() + "'}" );
                            return true;
                        }
                    });
                    popup.show();
                }
            });
            mrl.addView(but);
        }
    }

    public void getMqttConnection(){

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final String clientId = prefs.getString("mqttId", "");

        String mqttServer = prefs.getString("mqttServer", "");
        String mqttPort = prefs.getString("mqttPort", "");
        String mqttUser = prefs.getString("mqttUser", "");
        String mqttPass = prefs.getString("mqttPass", "");

        if (clientId.equals(new String()) || (mqttPort.equals(new String())) || (mqttServer.equals(new String()))
                || (mqttUser.equals(new String())) || (mqttPass.equals(new String()))){
            startActivity(new Intent(this, EinstellungenActivity.class));
        } else {

            String clientId2 = clientId + "_app_" + System.currentTimeMillis();
            mqttServer = "ssl://" + mqttServer + ":" + mqttPort;

            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), mqttServer, clientId2);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {

                    if (reconnect) {
                        addToHistory("Reconnected to : " + serverURI);
                        // Because Clean Session is true, we need to re-subscribe
                        if (ison){
                            subscribeToTopic("Inputs/#");
                            subscribeToTopic("Settings/Status");
                        }
                        subscribeToTopic("Message/" + clientId);
                    } else {
                        addToHistory("Reconnected to: " + serverURI);
                        if (ison){
                            subscribeToTopic("Inputs/#");
                            subscribeToTopic("Settings/Status");
                        }
                        subscribeToTopic("Message/" + clientId);
                    }
                    //Toast.makeText(getBaseContext(), "Connected to old MQTT", Toast.LENGTH_SHORT).show();
                    concounter++;
                }

                @Override
                public void connectionLost(Throwable cause) {
                    addToHistory("The Connection was lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    addToHistory("Incoming message: " + new String(message.getPayload()));
                    mescounter++;
                    if (topic.toLowerCase().contains("Message".toLowerCase())) {
                        sendNotification("Nachricht", new String(message.getPayload()), message.isRetained());
                        messages.add(new String(message.getPayload()));
                    } else if (topic.toLowerCase().contains("Prio".toLowerCase())){
                        messages.add(new String(message.getPayload()));
                    } else if (ison){
                        getValuesMqtt(message);
                        showViews(level);
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
            mqttConnectOptions.setKeepAliveInterval(300);

            try {
                //addToHistory("Connecting to " + serverUri);
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        if (ison){
                            subscribeToTopic("Inputs/#");
                            subscribeToTopic("Settings/Status");
                        }
                        subscribeToTopic("Message/" + clientId);
                        //Toast.makeText(getBaseContext(), "Connected to old MQTT", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        addToHistory("Failed to connect");
                    }
                });


            } catch (MqttException ex){
                ex.printStackTrace();
            }

        }
    }

    public void getValuesMqtt(MqttMessage message){
        String telegram = message.toString();
        try{
            JSONObject jMqtt = new JSONObject(telegram);
            String jValue = jMqtt.optString("Value").toString();
            String jKey = "leer";
            if (jMqtt.has("HKS")) {
                jKey = jMqtt.optString("HKS");
            }
            if (jMqtt.has("Setting")) {
                jKey = jMqtt.optString("Setting");
            }
            String TS = jMqtt.optString("ts");
            ValueList sensor = new ValueList(jKey, jValue, TS);
            sensoren.put(jKey, sensor);
        } catch (JSONException e) {

        }
    }

    public void setInptLabels(ButtonFeatures[] bfList, RelativeLayout mrl){
        //String werte = req_from_server("Inputs_hks");
        String werte = "";
        for (int i = 0; i < bfList.length; i++) {
            TextView tv = new TextView(this);
            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.addRule(RelativeLayout.ALIGN_BOTTOM);
            rl.leftMargin = (int) (bfList[i].x_value/1080.0 * width);
            rl.topMargin = (int) (bfList[i].y_value/1920.0 * height);
            tv.setLayoutParams(rl);
            tv.setTag(bfList[i].Name);
            ValueList sensor = sensoren.get(bfList[i].Name);
            if (sensor != null){
                String valueread = sensor.getValue();
                tv.setText(valueread + bfList[i].unit);
                String lastRec_s = sensor.getTS();
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.ENGLISH);
                Date date = new Date(0);
                Date jetzt = new Date();
                try {
                    date = format.parse(lastRec_s);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long diff = jetzt.getTime() - date.getTime();
                if (diff > (1000*60*60)) {
                    tv.setBackgroundColor(Color.DKGRAY);
                } else if (diff > (1000*60*240)) { // 60 to 240
                    tv.setBackgroundColor(Color.RED);
                } else if (diff > (1000*60*30)) { // 30 to 60
                    tv.setBackgroundColor(Color.YELLOW);
                } else if (diff > (1000*60*10)) { // 10 to 30
                    tv.setBackgroundColor(Color.BLUE);
                } else if (diff > (1000*60*5)) { // 5 to 10 min
                    tv.setBackgroundColor(Color.CYAN);
                }else {  // less than 5 min
                    tv.setBackgroundColor(Color.GREEN);
                }
                mrl.addView(tv);
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
                //tv.setText("x");
                if (value > 0) { // 60 to 240
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

    public void showOG() {
        //String sets = req_from_server("Settings");
        //String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.obergeschoss);
        level = 1;

        final ButtonFeatures[] inptList = new ButtonFeatures[3];
        inptList[0] = new ButtonFeatures("V01BAD1RUM1TE01", "V01BAD1RUM1TE01", "V01BAD1RUM1TE01", 600, 1400, "°C");
        inptList[1] = new ButtonFeatures("V01SCH1RUM1TE01", "V01SCH1RUM1TE01", "V01SCH1RUM1TE01", 200, 1200, "°C");
        inptList[2] = new ButtonFeatures("V01KID1RUM1TE01", "V01KID1RUM1TE01", "V01KID1RUM1TE01", 700, 300, "°C");
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutOG);
        setInptLabels(inptList, mrl);
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

        final ButtonFeatures[] inptList = new ButtonFeatures[1];
        inptList[0] = new ButtonFeatures("V02ZIM1RUM1TE02", "V02ZIM1RUM1TE02", "V02ZIM1RUM1TE02", 300, 1000, "°C");
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutDG);
        setInptLabels(inptList, mrl);
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
        //String sets = req_from_server("Settings");
        //String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.untergeschoss);
        level = -1;
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutUG);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean devel = prefs.getBoolean("checkbox_pref_devel", true);

        final ButtonFeatures[] inptList = new ButtonFeatures[11];
        inptList[0] = new ButtonFeatures("Vm1ZIM1RUM1TE01", "Vm1ZIM1RUM1TE01", "Vm1ZIM1RUM1TE01", 600, 400, "°C");
        inptList[1] = new ButtonFeatures("Vm1ZIM1PFL1TE01", "Vm1ZIM1PFL1TE01", "Vm1ZIM1PFL1TE01", 300, 300, "°C");
        inptList[2] = new ButtonFeatures("Vm1ZIM1RUM1BA01", "Vm1ZIM1RUM1BA01", "Vm1ZIM1RUM1BA01", 600, 450, " mbar");
        inptList[3] = new ButtonFeatures("Vm1ZIM1RUM1VO01", "Vm1ZIM1RUM1VO01", "Vm1ZIM1RUM1VO01", 600, 500, "V");
        inptList[4] = new ButtonFeatures("Vm1ZIM1RUM1CU01", "Vm1ZIM1RUM1CU01", "Vm1ZIM1RUM1CU01", 600, 550, "mA");

        inptList[5] = new ButtonFeatures("Vm1ZIM2RUM1TE01", "Vm1ZIM2RUM1TE01", "Vm1ZIM2RUM1TE01", 300, 1100, "°C");
        inptList[6] = new ButtonFeatures("Vm1ZIM2RUM1HU01", "Vm1ZIM2RUM1HU01", "Vm1ZIM2RUM1HU01", 300, 1150, "%");
        inptList[7] = new ButtonFeatures("Vm1ZIM2RUM1TE02", "Vm1ZIM2RUM1TE02", "Vm1ZIM2RUM1TE02", 300, 1300, "°C");

        inptList[8] = new ButtonFeatures("Vm1ZIM3RUM1TE01", "Vm1ZIM3RUM1TE01", "Vm1ZIM3RUM1TE01", 800, 1100, "°C");
        inptList[9] = new ButtonFeatures("Vm1ZIM3RUM1TE02", "Vm1ZIM3RUM1TE02", "Vm1ZIM3RUM1TE02", 800, 1150, "°C");
        inptList[10] = new ButtonFeatures("Vm1ZIM3RUM1TE03", "Vm1ZIM3RUM1TE03", "Vm1ZIM3RUM1TE03", 800, 1250, "°C");
        setInptLabels(inptList, mrl);

        Boolean rec_mes = prefs.getBoolean("checkbox_pref_showtaskmess", true);
        final Boolean task = prefs.getBoolean("checkbox_pref_task", true);
        if (rec_mes){
            final Button but = new Button(this);
            RelativeLayout.LayoutParams rlt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlt.addRule(RelativeLayout.ALIGN_BOTTOM);
            rlt.leftMargin = (int) (0/1080.0 * width);
            rlt.topMargin = (int) (1400/1920.0 * height);
            rlt.width = (int) (400.0/1080 * width);
            but.setLayoutParams(rlt);
            but.setText("Tasker Test");
            but.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //if ( TaskerIntent.testStatus( this ).equals( TaskerIntent.Status.OK ) ) {
                    TaskerIntent i = new TaskerIntent( "Test" );
                    if (task) {
                        sendBroadcast(i);
                    }
                //}
            }
            });
            mrl.addView(but);
        }
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

    public void showNachrichten() {
        level = 3;
        setContentView(R.layout.notifications);

        lvItems = (ListView) findViewById(R.id.lvItems);
        //items = new ArrayList<String>();
        itemsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, messages);
        lvItems.setAdapter(itemsAdapter);
        //items.add("First Item");
        //items.add("Second Item");
        setupListViewListener();
    }

    private void setupListViewListener() {
        lvItems.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapter,
                                                   View item, int pos, long id) {
                        // Remove the item within array at position
                        messages.remove(pos);
                        // Refresh the adapter
                        itemsAdapter.notifyDataSetChanged();
                        // Return true consumes the long click event (marks it handled)
                        return true;
                    }

                });
    }

    public void subscribeToTopic(String topic){
        try {
            mqttAndroidClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
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
        System.out.println("LOG: " + mainText);

    }

    private void sendNotification(String titel, String msg, Boolean retained) {
        //PowerManager pm = (PowerManager) MainActivity.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //pm.wakeUp(SystemClock.uptimeMillis());
        String ts = "";
        String desc = "";
        String titelm = titel;
        try {
            JSONObject jInpts = new JSONObject(msg);
            ts = jInpts.optString("ts").toString();
            desc = jInpts.optString("message").toString();
            titelm = jInpts.optString("titel").toString();

        } catch (JSONException e) {

        }
        Date date = new Date(0);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            date = format.parse(ts);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date jetzt = new Date();
        String body = ts + ", " + titelm + ": " + desc;
        long diff = jetzt.getTime() - date.getTime();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean mess = prefs.getBoolean("checkbox_pref_mes", true);
        Boolean task = prefs.getBoolean("checkbox_pref_task", true);
        Boolean tasdeb = prefs.getBoolean("checkbox_pref_showtaskmess", true);
        if (titelm.toLowerCase().contains("Setting".toLowerCase()) & task){
            TaskerIntent i = new TaskerIntent( desc);
            sendBroadcast( i );
        }
        if ((diff < (1000*60*30) | !retained) & mess & (!task | tasdeb)) {
            mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.steuerzen_icon)
                            .setContentTitle(titelm)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(body))
                            .setContentText(body);

            mBuilder.setContentIntent(contentIntent);
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            NOTIFICATION_ID++;
        }
    }

    public void send_to_server(String Befehl){
        try {
            SocketClient.broadc(Befehl);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String req_from_server(String Befehl){
        try {
            String msg = "{'GCM-Client':'";
            msg = "{'Request_js':'" + Befehl;
            msg = msg +  "'}";
            String antwort = SocketClient.request(msg);
            return antwort;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

}
