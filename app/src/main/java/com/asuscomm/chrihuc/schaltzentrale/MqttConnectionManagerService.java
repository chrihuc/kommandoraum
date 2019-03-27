package com.asuscomm.chrihuc.schaltzentrale;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.sleep;

public class MqttConnectionManagerService extends Service {

    static MqttAndroidClient client; // changed from public
    private MqttConnectOptions options;
    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;
    public Date connectDate = new Date();
    public Date conLostDate = new Date();
    public Boolean timerSet = false;
    private static final String NOTIFICATION_CHANNEL_ID = "MyChannel2";
    Context context;

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
                        if (reconnect) {
                            addToHistory("Client reconnected");
                        } else{
                            addToHistory("Normal connection");
                        }
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Boolean devel = prefs.getBoolean("checkbox_mess_devel", true);
                        if (devel){
                            makeNotification("Connected", "Connected", 2);
                        }
                        subscribeToTopic("Message/" + clientId);
                        subscribeToTopic("Image/Satellite/#");
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        conLostDate = new Date();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Boolean devel = prefs.getBoolean("checkbox_mess_devel", true);
                        if (devel){
                            makeNotification("Connection lost", "Connection lost", 2);
                        }
                        addToHistory("Connection lost");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        if (topic.toLowerCase().contains("Message".toLowerCase())){
                            sendNotification(new String(message.getPayload()), message.isRetained());
                            addToHistory("Notification received " + message.toString() );
                        } else if (topic.toLowerCase().contains("Image".toLowerCase())) {
                            showPic(message);
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
            makeNotification("Crash", "in service", 2);
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
            makeNotification("Crash", "in service", 2);
            ex.printStackTrace();
        } catch (Exception e){
            addToHistory("Crash during subscribing");
            makeNotification("Crash", "in service", 2);
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

        } catch (JSONException e) {
            makeNotification("Crash", "in service", prio);
        }
        Date date = new Date(0);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            date = format.parse(ts);
        } catch (ParseException e) {
            e.printStackTrace();
            makeNotification("Crash", "in service", prio);
        }

        Date jetzt = new Date();
        String body = ts + ", " + titelm + ": " + desc;
        long diff = jetzt.getTime() - date.getTime();
        boolean missedMess = (date.getTime() >= (conLostDate.getTime() - 30*Secs)) & (date.getTime() <= (connectDate.getTime() + 30*Secs));
        if (missedMess) {
            body = body + " (verpasste Nachricht)";
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean mess = prefs.getBoolean("checkbox_pref_mes", true);
        Boolean task = prefs.getBoolean("checkbox_pref_task", true);
        Boolean tasdeb = prefs.getBoolean("checkbox_pref_showtaskmess", true);
        if ((missedMess | !retained) & titelm.toLowerCase().contains("Setting".toLowerCase()) & task){
            if ( TaskerIntent.testStatus( MqttConnectionManagerService.this, desc ).equals( TaskerIntent.Status.OK ) ) {
                TaskerIntent i = new TaskerIntent( desc );
                if (task) {
                    sendBroadcast(i);
                }
            } else {
                makeNotification("TaskerCon", "Failed", prio);
            }
        }
        if ((missedMess | !retained) & mess & (!titelm.toLowerCase().contains("Setting".toLowerCase()) | tasdeb)) {
            makeNotification(titelm, body, prio);
            addToHistory("Nachricht ausgegeben (Service)");
        }
    }

    private void makeNotification(String topic, String body, int prio){
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID
                , new Intent(this, NotificationActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.steuerzen_icon)
                        .setContentTitle(topic)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(body))
                        .setContentText(body);
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);
        // Configure the notification channel.
        AudioAttributes att = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
//        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean kling_nachts = prefs.getBoolean("cb_ring_nachts", true);
        // nicht so ganz klar warum das funktioniert....
        // dnd == 0 ist kein dnd
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
        // 1 alles
        // 2 Ausnahmen
        // 3 nicht
        // 4 alarme
        if (prio == 5) {
            try {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.telephone);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                if (dnd <= 1 || (dnd == 2 && kling_nachts)){
                    r.play();
                }else if (false){
                    mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    r.play();
                    sleep(5000);
                    mNotificationManager.setInterruptionFilter(dnd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (prio == 6) {
            try {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bomb_siren);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                r.play();
                sleep(5000);
                mNotificationManager.setInterruptionFilter(dnd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            try {
                Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonar);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                // alternative um die Lautstärke zu setzen:
//                AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//                manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
//
//                Uri notification = RingtoneManager
//                        .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//                MediaPlayer player = MediaPlayer.create(getApplicationContext(), notification);
//                player.start();
                if (dnd <= 1){
                    r.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        notificationChannel.setSound(alarmSound,att);
        notificationChannel.setDescription(body);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
        notificationChannel.enableVibration(true);
        mNotificationManager.createNotificationChannel(notificationChannel);
//        if (imageThumbnail != null) {
//            mBuilder.setStyle(new Notification.BigPictureStyle()
//                    .bigPicture(imageThumbnail).setSummaryText(messageBody));
//        }
        mBuilder.setAutoCancel(true);
//        final Intent notificationIntent = new Intent(context, MainActivity.class);
//        notificationIntent.setAction(Intent.ACTION_MAIN);
//        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilder.setContentIntent(contentIntent);
//        mBuilder.setLatestEventInfo(context, topic, body, notificationIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        NOTIFICATION_ID++;
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

        Intent openMe = new Intent(getApplicationContext(), MainActivity.class);
        openMe.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //experiment with the flags
        openMe.putExtra("execVoid","showTuerSpion");
        openMe.putExtra("picNr",picNr);
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

}
