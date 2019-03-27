package com.asuscomm.chrihuc.schaltzentrale;

import android.app.AlarmManager;
import android.app.IntentService;
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
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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

public class MQTTReceiver extends IntentService {

    static MqttAsyncClient client; // changed from public
    private MqttConnectOptions options;
    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;
    public Date connectDate = new Date();
    public Date conLostDate = new Date();
    public Boolean timerSet = false;
    private static final String NOTIFICATION_CHANNEL_ID = "MyChannel2";
    Context context;

    public MQTTReceiver() {
        super("DisplayNotification");
    }


//    @Override
//    public void onCreate() {
//        super.onCreate();
//        context = getApplicationContext();
//
//        // start new thread and you your work there
//        new Thread(runnable).start();
////
//        // prepare a notification for user and start service foreground
////        Notification notification = ...
//        // this will ensure your service won't be killed by Android
////        startForeground(R.id.notification, notification);
//
//
//    }

    public int picNr = 1;


//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }

//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
//        Toast.makeText(this, "MyCustomService Handling Intent", Toast.LENGTH_LONG).show();
        try {
            options = createMqttConnectOptions();
            client = createMqttAndroidClient();
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
                LocalBroadcastManager.getInstance(MQTTReceiver.this).registerReceiver(mMessageReceiver, new IntentFilter("screenstatus"));

            }
            if (!timerSet) {
//                    Context context = this.getApplicationContext();
//                callAsynchronousTask();
            }
        } catch (Exception e){
            Toast.makeText(getBaseContext(), "Konnte MQTT service nicht starten.", Toast.LENGTH_LONG).show();
        }
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

    private MqttAsyncClient createMqttAndroidClient() {
        //create and return client
        MqttAsyncClient mqttAsyncClient = null;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String clientId = prefs.getString("mqttId", "");

        String mqttServer = prefs.getString("mqttServer", "");
        String mqttPort = prefs.getString("mqttPort", "");

        String clientId2 = clientId + "v2" + System.currentTimeMillis();
        mqttServer = "ssl://" + mqttServer + ":" + mqttPort;
        try {
            //mqttAsyncClient = new MqttAsyncClient(getApplicationContext(), mqttServer, clientId2);
            mqttAsyncClient = new MqttAsyncClient(mqttServer, clientId2, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        addToHistory("Client created");
        return mqttAsyncClient;
    }

    public void connect(final MqttAsyncClient client, MqttConnectOptions options) {

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
                        subscribeToTopic("Inputs/HKS/#");
//                        subscribeToTopic("Message/" + clientId);
                        subscribeToTopic("Settings/#");
//                        addToHistory("Client as service connected");
                        Toast.makeText(getBaseContext(), "Connected to MQTT as Service", Toast.LENGTH_SHORT).show();
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
//                        subscribeToTopic("Inputs/HKS/#");
//                        subscribeToTopic("Settings/#");
//                        subscribeToTopic("Message/" + clientId);
//                        subscribeToTopic("Image/Satellite/#");
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        conLostDate = new Date();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        addToHistory("Connection lost");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        if (topic.toLowerCase().contains("Message".toLowerCase())){
//
                        } else if (topic.toLowerCase().contains("Image".toLowerCase())) {

                        } else {
                            addToHistory("Notification received " + message.toString() );
                            sendBroadcast(topic, message.toString());
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
        }
    }

    public void sendBroadcast(String topic, String message){
        Intent intent = new Intent("MQTTMessage");
        intent.putExtra("message", message);
        intent.putExtra("topic", topic);
        LocalBroadcastManager.getInstance(MQTTReceiver.this).sendBroadcast(intent);
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
            ex.printStackTrace();
        } catch (Exception e){
            addToHistory("Crash during subscribing");
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message.equals("ScreenOff")) {
                unSubscribe("Inputs/HKS/#");
                unSubscribe("Settings/#");
            } else if (message.equals("ScreenOn")) {
                subscribeToTopic("Inputs/HKS/#");
//                subscribeToTopic("Inputs/A00#");
                subscribeToTopic("Settings/#");
            }
        }
    };

    private void addToHistory(String mainText){
        System.out.println("Service: " + mainText);

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

}
