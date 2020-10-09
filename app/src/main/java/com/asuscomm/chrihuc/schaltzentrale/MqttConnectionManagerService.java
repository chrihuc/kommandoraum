package com.asuscomm.chrihuc.schaltzentrale;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.sleep;
import static java.security.AccessController.getContext;

public class MqttConnectionManagerService extends Service {

    static MqttAndroidClient client; // changed from public
    private MqttConnectOptions options;
    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;
    public Date connectDate = new Date();
    public Date conLostDate = new Date();
    public Boolean connected = false;
    public Boolean connectionLost = false;
    public Boolean connectionLosts = false;
    public Boolean timeRunning = false;
    public Boolean timerSet = false;
    private static final String NOTIFICATION_CHANNEL_ID = "MyChannel2";
    private MediaPlayer mp;
    Context context;
    WifiReceiver wifiReceiver= new WifiReceiver();
    Random generator = new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        try {
            options = createMqttConnectOptions();
            client = createMqttAndroidClient();
            if (!timerSet) {
//                    Context context = this.getApplicationContext();
                setRecurringAlarm(context);
//                callAsynchronousTask();
            }
        } catch (Exception e){
            Toast.makeText(getBaseContext(), "Konnte MQTT service nicht starten.", Toast.LENGTH_LONG).show();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, intentFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

    }

    public int picNr = 1;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String clientId = prefs.getString("mqttId", "");
        String mqttServer = prefs.getString("mqttServer", "");
        String mqttPort = prefs.getString("mqttPort", "");
        String mqttUser = prefs.getString("mqttUser", "");
        String mqttPass = prefs.getString("mqttPass", "");

        try {
            String userID = intent.getStringExtra("execVoid");
            if (userID != null && userID.equals("stopWecker")) {
                stopPlaying();
            }
        }catch (Exception e){

        }


        if (clientId.equals(new String()) || (mqttPort.equals(new String())) || (mqttServer.equals(new String()))
                || (mqttUser.equals(new String())) || (mqttPass.equals(new String()))){
            Toast.makeText(getBaseContext(), "Unvollständige MQTT Konfiguration", Toast.LENGTH_LONG).show();
        } else {
            this.connect(client, options);
            return START_STICKY;
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private MqttConnectOptions createMqttConnectOptions() {
        //create and return options
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String mqttUser = prefs.getString("mqttUser", "");
        String mqttPass = prefs.getString("mqttPass", "");

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(mqttUser);
        mqttConnectOptions.setPassword(mqttPass.toCharArray());
        mqttConnectOptions.setKeepAliveInterval(600); // 300 was working then not anymore, 10 has high battery usage
        addToHistory("Options created");
        return mqttConnectOptions;
    }

    private MqttAndroidClient createMqttAndroidClient() {
        //create and return client
        MqttAndroidClient mqttAndroidClient;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String clientId = prefs.getString("mqttId", "");

        String mqttServer = prefs.getString("mqttServer", "");
        String mqttPort = prefs.getString("mqttPort", "");

        String clientId2 = clientId + System.currentTimeMillis();
        mqttServer = "ssl://" + mqttServer + ":" + mqttPort;
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), mqttServer, clientId2);
        addToHistory("Client created");
        return mqttAndroidClient;
    }

    public void connect(final MqttAndroidClient client, MqttConnectOptions options) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String clientId = prefs.getString("mqttId", "");
        final Boolean devel = prefs.getBoolean("checkbox_mess_devel", false);
        final Handler timehandler = new Handler();

        try {
            if (!client.isConnected()) {
                IMqttToken token = client.connect(options);
                //on successful connection, publish or subscribe as usual
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        //mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        //subscribeToTopic("Inputs/#");
                        subscribeToTopic("Message/" + clientId);
                        subscribeToTopic("Image/Satellite/#");
                        if (devel){
                            subscribeToTopic("Time");
                        }
                        addToHistory("Client as service connected");
//                        Toast.makeText(getBaseContext(), "Connected to MQTT as Service", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        addToHistory("Connection failure");
                    }
                });
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        connectDate = new Date();
                        connected = true;
                        if (reconnect) {
                            addToHistory("Client reconnected");
                        } else{
                            addToHistory("Normal connection");
                        }
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                        Boolean devel = prefs.getBoolean("checkbox_mess_devel", true);
                        if (devel && connectionLosts){
                            makeNotification("Connected", "Developer Verbindung wieder hergestellt", 2, "");
                        }
                        if (connectionLost){
                            makeNotification("MQTT", "Verbindung nach Hause wieder hergestellt", 2, "");
                        }
                        connectionLost = false;
                        connectionLosts = false;
                        subscribeToTopic("Message/" + clientId);
                        subscribeToTopic("Image/Satellite/#");
                        if (devel){
                            subscribeToTopic("Time");
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        conLostDate = new Date();
                        connected = false;
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Boolean devel = prefs.getBoolean("checkbox_mess_devel", true);
                        if (devel){
                            final Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!connected){
                                        makeNotification("Achtung", "Verbindung nach Hause seit 20sec verloren", 2, "");
                                        connectionLosts = true;
                                    }
                                }
                            }, 20000);
                        }
                        addToHistory("Connection lost");
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!connected){
                                    makeNotification("Achtung", "Verbindung nach Hause seit 5min verloren", 2, "");
                                    connectionLost = true;
                                }
                            }
                        }, 300000);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        if (topic.toLowerCase().contains("Message".toLowerCase())){
                            sendNotification(new String(message.getPayload()), message.isRetained());
//                            addToHistory("Notification received " + message.toString() );
                        } else if (topic.toLowerCase().contains("Image".toLowerCase())) {
                            showPic(message);
                        } else if (topic.equals("Time")){
                            if (! timeRunning){
                                makeNotification("Hinweis", "Zeit empfangen, überwachung läuft", 2, "");
                                timeRunning = true;
                            }
                            timehandler.removeCallbacksAndMessages(null);
                            timehandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    timeRunning = false;
                                    makeNotification("Achtung", "Steuerzentrale scheint nicht zu laufen", 2, "");
                                }
                            }, 3 * 60 * 1000);
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        } catch (MqttException e) {
            //handle e
            addToHistory("Crash on MqttException");
            makeNotification("Crash", "in service", 2, "");
        }
    }

    public void subscribeToTopic(String topic){
        try {
            client.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("successfully subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Subscription failure");
                }
            });


        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            addToHistory("Error during subscribing");
            makeNotification("Crash", "in service", 2, "");
            ex.printStackTrace();
        } catch (Exception e){
            addToHistory("Crash during subscribing");
            makeNotification("Crash", "in service", 2, "");
        }}

    public void unSubscribe(@NonNull final String topic) {
        try{
            IMqttToken token = client.unsubscribe(topic);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
//                    Log.d(TAG, "UnSubscribe Successfully " + topic);
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
//                    Log.e(TAG, "UnSubscribe Failed " + topic);
                }
            } );}
        catch (MqttException ex){
            System.err.println("Exception whilst unsubscribing");
            ex.printStackTrace();
        } catch (Exception e){

        };
    }


    private void sendNotification(String msg, Boolean retained) {
        //PowerManager pm = (PowerManager) MainActivity.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //pm.wakeUp(SystemClock.uptimeMillis());
        String ts = "";
        String desc = "";
        String titelm = "";
        String payload = "";
        Integer prio = 2;
//        Double prio = 2.0;
        Integer Mins = 1000*60;
        Integer Secs = 1000;
        try {
            JSONObject jInpts = new JSONObject(msg);
            ts = jInpts.optString("ts").toString();
            desc = jInpts.optString("message").toString();
            titelm = jInpts.optString("titel").toString();
            prio = jInpts.optInt("prio");
            payload = jInpts.optString("payload");

        } catch (JSONException e) {
            makeNotification("Crash", "in service", prio, payload);
        }
        Date date = new Date(0);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            date = format.parse(ts);
        } catch (ParseException e) {
            e.printStackTrace();
            makeNotification("Crash", "in service", prio, payload);
        }

        Date jetzt = new Date();
        String body = "";
        if (!desc.equals("")){
            body = desc + ", " + ts ;
        }
        long diff = jetzt.getTime() - date.getTime();
        boolean missedMess = (date.getTime() >= (conLostDate.getTime() - 30*Secs)) & (date.getTime() <= (connectDate.getTime() + 30*Secs));
        if (missedMess) {
            body = body + " (verpasste Nachricht)";
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean mess = prefs.getBoolean("checkbox_pref_mes", true);
        Boolean task = prefs.getBoolean("checkbox_pref_task", true);
        Boolean tasdeb = prefs.getBoolean("checkbox_pref_showtaskmess", true);
        if ((missedMess | !retained) & titelm.toLowerCase().contains("DirektSetting".toLowerCase()) & task){
            AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 10, 0);
        }
        else if ((missedMess | !retained) & titelm.toLowerCase().contains("Setting".toLowerCase()) & task){
            if ( TaskerIntent.testStatus( MqttConnectionManagerService.this, desc ).equals( TaskerIntent.Status.OK ) ) {
                TaskerIntent i = new TaskerIntent( desc );
                if (task) {
                    sendBroadcast(i);
//                    if (desc.contains("Nacht")){
//
//                    }
                }
            } else {
                makeNotification("TaskerCon", "Failed", prio, payload);
            }
        }
        if ((missedMess | !retained) & mess & (!titelm.toLowerCase().contains("Setting".toLowerCase()) | tasdeb)) {
            makeNotification(titelm, body, prio, payload);
            addToHistory("Nachricht ausgegeben (Service)");
        }
    }

    void makeNotification(String topic, String body, int prio, String payload){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);



        PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID
                , new Intent(this, NotificationActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);


//        Intent i = new Intent("do_something");
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, i, 0);

//        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);



        // Configure the notification channel.
//        AudioAttributes att = new AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                .build();
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean kling_nachts = prefs.getBoolean("cb_ring_nachts", true);
        Boolean stumm = prefs.getBoolean("cb_stumm", false);
        Boolean alarm_immer = prefs.getBoolean("cb_alarm_imer", true);
        Boolean tuer_immer = prefs.getBoolean("cb_ring_immer", true);
        Boolean debug = prefs.getBoolean("checkbox_pref_devel",false);
        Boolean vibr = prefs.getBoolean("cb_vibrate",false);
        Integer tuer_vol = prefs.getInt("cb_ring_vol",10);
        Integer alarm_vol = prefs.getInt("cb_alarm_vol",5);
        // nicht so ganz klar warum das funktioniert....
        // dnd == 0 ist unbekannt, 1 == kein filter, 2 == priorität, 3 == gar nichts, 4 == Alarme
        // wir initialisieren dnd mit 1, also erstmal kein Ton
        // sind wir sicher, das der Ton eingeschaltet ist dann gibt das Ding laut
        // wenn es klingeln sollte dann intialisieren wir mit 0, also Ton
        // sind wir dann sicher das der Ton ausgeschaltet ist, dann klingelts nicht, ansonsten schon
        // scheinbar sind wir nachts nicht sicher was genau los ist.

//        int dnd = 1;
//        if (kling_nachts && (prio == 5)) {
//            dnd = 0;
//        }
//        try {
//            dnd = Settings.Global.getInt(getContentResolver(), "zen_mode");
//        } catch  (Exception e) {
//            e.printStackTrace();
//        }

        // neuer versuch mit:
        int dnd = mNotificationManager.getCurrentInterruptionFilter();
        AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int slent = manager.getRingerMode();
        Integer cur_vol_not = manager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        Integer cur_vol_mus = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Integer cur_vol_sys = manager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        Integer cur_vol_rin = manager.getStreamVolume(AudioManager.STREAM_RING);
        // 1 alles
        // 2 Ausnahmen
        // 3 nicht
        // 4 alarme
        // dnd und mute ausschalten:
        Integer muteoff = prio / 100;
        prio = prio - muteoff * 100;
        boolean tonaus = false;
        boolean tonein = false;
        boolean ton = true;
        boolean wecker = false;



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);

            for (int i = 2; i < 20; i++) {
                String NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_00";
                String DESCRIPTION = "Prio 00";
                switch (i) {
                    case 2:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_00";
                        DESCRIPTION = "Prio 00";
                        break;
                    case 3:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kill);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_03";
                        DESCRIPTION = "Prio 03";
                        break;
                    case 4:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ohpickme);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_04";
                        DESCRIPTION = "Prio 04";
                        break;
                    case 5:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dingdong);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_05";
                        DESCRIPTION = "Prio 05";
                        break;
                    case 6:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.icquhoh);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_06";
                        DESCRIPTION = "Prio 06";
                        break;
                    case 7:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mute);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_07";
                        DESCRIPTION = "Prio 07";
                        break;
                    case 8:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_08";
                        DESCRIPTION = "Prio 08";
                        break;
                    case 9:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_09";
                        DESCRIPTION = "Prio 09";
                        break;
                    case 10:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.countdown);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_10";
                        DESCRIPTION = "Prio 10";
                        break;
                    case 11:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.countdownstart);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_11";
                        DESCRIPTION = "Prio 11";
                        break;
                    case 12:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.burp);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_12";
                        DESCRIPTION = "Prio 12";
                        break;
                    case 13:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.play);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_13";
                        DESCRIPTION = "Prio 13";
                        break;
                    case 14:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.cookie);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_14";
                        DESCRIPTION = "Prio 14";
                        break;
                    case 15:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.groot);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_15";
                        DESCRIPTION = "Prio 15";
                        break;
                    case 16:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.whatwasthat);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_16";
                        DESCRIPTION = "Prio 16";
                        break;
                    case 17:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.minions_yeah);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_17";
                        DESCRIPTION = "Prio 17";
                        break;
                    case 18:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.papoi);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_18";
                        DESCRIPTION = "Prio 18";
                        break;
                    case 19:
                        notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.suckers);
                        NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_19";
                        DESCRIPTION = "Prio 19";
                        break;
                }
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Steuerzentrale Notifications", NotificationManager.IMPORTANCE_HIGH);

                // Configure the notification channel.
                //notificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription(DESCRIPTION);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.YELLOW);
                notificationChannel.setVibrationPattern(new long[] {2000});
                notificationChannel.enableVibration(true);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                notificationChannel.setSound(notification, audioAttributes);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
        }



//        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

        if (muteoff == 1 && !stumm && alarm_immer) {
            tonein = true;
            tonaus = false;
        }
        if (muteoff == 1 && dnd <= NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            tonein = true;
            tonaus = true;
        }
        if (muteoff == 2 && kling_nachts && dnd <= NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            tonein = true;
            tonaus = true;
        }
        if (muteoff == 2 && tuer_immer) {
            tonein = true;
            tonaus = true;
        }
        if (muteoff == 3) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            mBluetoothAdapter.disable();
        }
        if (muteoff == 4) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            mBluetoothAdapter.disable();
        }
        if (muteoff == 5) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            mBluetoothAdapter.disable();
        }
        if (muteoff == 6) {
            tonein = true;
            mBluetoothAdapter.enable();
        }
        if (muteoff == 8) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            mBluetoothAdapter.enable();
        }
        if (muteoff == 9) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            mBluetoothAdapter.enable();
        }

        if (dnd == NotificationManager.INTERRUPTION_FILTER_PRIORITY || prio == 8) {
            ton = false;
        }
        if (muteoff == 1 ) {
            ton = true;
        }

        Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);

        if (muteoff == 7) {
            tuer_vol = alarm_vol;
            tonein = false;
            ton = true;
            mBluetoothAdapter.enable();
            wecker = true;

            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }
        if (tonein) {
            mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_ALARM, alarm_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_RING, tuer_vol, 0);
        }

        if (((prio > 1 && prio < 9) || (prio > 9 && prio < 20) || (prio == 9 && debug) || (prio == 99))) {

            String NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_00";
            notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);
            switch (prio) {
                case 3:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kill);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_03";
                    break;
                case 4:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ohpickme);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_04";
                    break;
                case 5:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dingdong);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_05";
                    break;
                case 6:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.icquhoh);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_06";
                    break;
                case 7:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mute);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_07";
                    wecker = false;
                    break;
                case 9:
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_00";
                    break;
                case 10:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.countdown);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_10";
                    break;
                case 11:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.countdownstart);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_11";
                    break;
                case 12:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.burp);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_12";
                    break;
                case 13:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.play);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_13";
                    break;
                case 14:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.cookie);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_14";
                    break;
                case 15:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.groot);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_15";
                    break;
                case 16:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.whatwasthat);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_16";
                    break;
                case 17:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.minions_yeah);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_17";
                    break;
                case 18:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.papoi);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_18";
                    break;
                case 19:
                    notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.suckers);
                    NOTIFICATION_CHANNEL_ID = "Steuerzentrale_prio_19";
                    break;
                case 99:
                    stopPlaying();
                    break;
            }

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

            if (body != null && !body.equals("")) {

                notificationBuilder.setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.steuerzen_icon)
//                .setTicker("Hearty365")
//                .setPriority(Notification.PRIORITY_MAX) // this is deprecated in API 26 but you can still use for below 26. check below update for 26 API
                        .setContentTitle(topic)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(body))
                        .setAutoCancel(false)
                        .setContentInfo("Info")
                        .setVibrate(new long[] {2000});
                if (payload != null && !payload.equals("") && !payload.equals("null")){
                    //This is the intent of PendingIntent
                    Intent intentAction = new Intent(context,ActionReceiver.class);

                    //This is optional if you have more than one buttons and want to differentiate between two
                    intentAction.putExtra("payload",payload);

                    PendingIntent pIntentlogin = PendingIntent.getBroadcast(context, Math.abs(generator.nextInt()), intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
//                    PendingIntent pIntentlogin = PendingIntent.getBroadcast(context, 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
                    notificationBuilder.addAction(R.mipmap.steuerzen_icon, "Sende Bestätigung", pIntentlogin);
                }

                notificationBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                NOTIFICATION_ID++;
            }else {
                mp = MediaPlayer.create (getApplicationContext(), notification);
                mp.start();
//                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//                r.play();
            }

        }

        if (((prio > 1 && prio < 9) || (prio == 9 && debug) || (prio > 9 && prio < 20)) && ton){
            if (wecker){
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mp = MediaPlayer.create (getApplicationContext(), notification);
                mp.start();
                showStopWecker();
            }else{
//                r.play();
//                try {
//                    sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
            if (prio == 7){// && mNotificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_NONE){
//                Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
//                openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
//                openMe.putExtra("execVoid", "startOnTop");
//                startActivity(openMe);
                notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.minions_bido);
                mp = MediaPlayer.create (getApplicationContext(), notification);
                mp.setLooping(true);
                showStopAlarm();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mp.start();
                    }
                }, 10000);
            }
            if (prio == 9){
//                WindowManager mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
//
////                View mView = mInflater.inflate(R.layout.stop_wecker, null);
//                View mView = View.inflate(getApplicationContext(), R.layout.stop_wecker, null);
//
//                WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0,
//                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
//                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                        /* | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON */,
//                        PixelFormat.RGBA_8888);
//
//                mWindowManager.addView(mView, mLayoutParams);
//                showTuerSpionOnTop();
//                showStopWecker();

            }
        }
        if (vibr && (mNotificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_PRIORITY || tonein)){
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
        }
        if (tonaus) {
            mNotificationManager.setInterruptionFilter(dnd);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, cur_vol_not, 0);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, cur_vol_mus, 0);
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, cur_vol_sys, 0);
            manager.setStreamVolume(AudioManager.STREAM_RING, cur_vol_rin, 0);
            manager.setRingerMode(slent);
        }


    }


    public void stopPlaying() {
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Integer tuer_vol = prefs.getInt("cb_ring_vol",10);
            Integer alarm_vol = prefs.getInt("cb_alarm_vol",5);
            AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_ALARM, alarm_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_SYSTEM, tuer_vol, 0);
            manager.setStreamVolume(AudioManager.STREAM_RING, tuer_vol, 0);
        }
    }

    public void showPic(MqttMessage message){
        // Convert bytes data into a Bitmap
        byte[] bytearray = message.getPayload();

        Bitmap bmp= BitmapFactory.decodeByteArray(bytearray,0,bytearray.length);
//        ImageView image=new ImageView(this);
//        if (picNr == 1) {
//            saveImage(this.getApplicationContext(), bmp, "tuerspion1.jpg");
//            picNr = 2;
//        } else {
//            saveImage(this.getApplicationContext(), bmp, "tuerspion2.jpg");
//            picNr = 1;
//        }
        for (int i = 9; i > 0; i--) {
            Bitmap bmp1 = loadImageBitmap(this.getApplicationContext(), "tuerspion" + String.valueOf(i) + ".jpg");
            if (bmp1 == null){
                saveImage(this.getApplicationContext(), bmp, "tuerspion" + String.valueOf(i + 1) + ".jpg");
            } else {
                saveImage(this.getApplicationContext(), bmp1, "tuerspion" + String.valueOf(i + 1) + ".jpg");
            }
        }
        saveImage(this.getApplicationContext(), bmp, "tuerspion1.jpg");
        showTuerSpion();
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
            return null;
        }
        return bitmap;
    }

    public void saveImage(Context context, Bitmap b, String imageName) {
        FileOutputStream foStream;
        try {
            foStream = context.openFileOutput(imageName, Context.MODE_PRIVATE);
            b.compress(Bitmap.CompressFormat.PNG, 100, foStream);
            foStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 2, Something went wrong!");
            e.printStackTrace();
        }
    }

    public void showTuerSpion(){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean cb_tuer_fg = prefs.getBoolean("cb_tuer_fg", false);

        if (cb_tuer_fg){
            Intent openMe = new Intent(this, ScreenOnActivity.class);
            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            openMe.putExtra("execVoid","showTuerSpion");
            openMe.putExtra("picNr",picNr);
            startActivity(openMe);
        } else {
            Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            openMe.putExtra("execVoid", "showTuerSpion");
            openMe.putExtra("picNr", picNr);
            startActivity(openMe);
        }
    }

    public void showTuerSpionOnTop(){

        Intent openMe = new Intent(this, ScreenOnActivity.class);
        openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
        openMe.putExtra("execVoid","showTuerSpion");
        openMe.putExtra("picNr",picNr);
        startActivity(openMe);
    }

    public void showStopWecker(){
        try{
            Intent screenOnIntent = new Intent(this, ScreenOnActivity.class);
            screenOnIntent.putExtra("execVoid","showStopWecker");
//            screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //experiment with the flags
            screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            startActivity(screenOnIntent);

//            Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
//            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
//            openMe.putExtra("execVoid","showStopWecker");
//    //        openMe.putExtra("picNr",picNr);
//            startActivity(openMe);
        } catch (Exception e) {

        }

    }

    public void showStopAlarm(){
        try{
            Intent screenOnIntent = new Intent(this, ScreenOnActivity.class);
            screenOnIntent.putExtra("execVoid","showStopAlarm");
//            screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //experiment with the flags
            screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            startActivity(screenOnIntent);

        } catch (Exception e) {

        }

    }

    public void switchDispOn(){
        try{
            Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
            openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
            openMe.putExtra("execVoid","switchDispOn");
            //        openMe.putExtra("picNr",picNr);
            startActivity(openMe);
        } catch (Exception e) {

        }

    }

    public void setAudioNormal(){

        Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
        openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
        openMe.putExtra("execVoid","setAudioNormal");
        startActivity(openMe);
    }

    public static void send_mqtt_serv(String topic, String body){
        String sendingMessage = body;
        String sendingTopic = topic;
        try {
//                    connectToBroker();
            int i = 0;
            while (! client.isConnected() && i < 10*30){
                i += 1;
                sleep(100);
            }
            if (client.isConnected()) {
                MqttMessage message = new MqttMessage();
                message.setPayload(sendingMessage.getBytes());
                client.publish(sendingTopic, message);
                // System.out.println("XSONDIN sendingTopic = " + sendingTopic);
                // System.out.println("XSONDIN message = " + message);
//                        addDebug("Message Published");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Error Sleeping: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e){

        }
    }

    public void send_toast(String message){
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    private void addToHistory(String mainText){
//        System.out.println("Service: " + mainText);

    }
//    /**
//     * @param noConnection
//     * @return
//     */
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

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            PerformBackgroundTask performBackgroundTask = new PerformBackgroundTask();
                            // PerformBackgroundTask this class is the class that extends AsynchTask
                            performBackgroundTask.execute(context);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 30 * 1000, 5 * 60 * 1000); //execute in every 5 min
        timerSet = true;
    }

    private void setRecurringAlarm(Context context) {

        // we know mobiletuts updates at right around 1130 GMT.
        // let's grab new stuff at around 11:45 GMT, inexactly
        Calendar updateTime = Calendar.getInstance();
//        updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
//        updateTime.set(Calendar.HOUR_OF_DAY, 11);
//        updateTime.set(Calendar.MINUTE, 45);

        Intent checker = new Intent(context, AlarmReceiver.class);
        PendingIntent checkWifi = PendingIntent.getBroadcast(context,
                0, checker, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService(
                Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                updateTime.getTimeInMillis() + 30*1000, 5*60*1000, checkWifi);
        timerSet = true;
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
              //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                if (device.toString().equals("00:0E:9F:EF:6E:D5") || device.toString().equals("00:14:09:77:A0:F2")) {
                    wifiReceiver.btconn = 1;
//                    makeNotification("Bluetooth verbunden", device.toString(),9);
                }
              //Device is now connected
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
              //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
              //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//                makeNotification("Bluetooth getrennt", device.toString(),9);
                if (device.toString().equals("00:0E:9F:EF:6E:D5") || device.toString().equals("00:14:09:77:A0:F2")) {
                    wifiReceiver.btconn = 0;
//                    makeNotification("Bluetooth getrennt", device.toString(),9);
                }
              //Device has disconnected
            }
        }
    };
}
