package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MqttConnectionManagerService extends Service {

    private MqttAndroidClient client;
    private MqttConnectOptions options;
    private NotificationManager mNotificationManager;
    public static int NOTIFICATION_ID = 1;
    public Date connectDate = new Date();
    public Date conLostDate = new Date();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            options = createMqttConnectOptions();
            client = createMqttAndroidClient();
        } catch (Exception e){
            Toast.makeText(getBaseContext(), "Konnte MQTT service nicht starten.", Toast.LENGTH_LONG).show();
        }
    }


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
        mqttConnectOptions.setKeepAliveInterval(300);
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
                        addToHistory("Client as service connected");
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
                        subscribeToTopic("Message/" + clientId);
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        conLostDate = new Date();
                        addToHistory("Connection lost");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        sendNotification(new String(message.getPayload()), message.isRetained());
                        addToHistory("Notification received " + message.toString() );
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        } catch (MqttException e) {
            //handle e
            addToHistory("Crash on MqttException");
            makeNotification("Crash", "in service");
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
            makeNotification("Crash", "in service");
            ex.printStackTrace();
        } catch (Exception e){
            addToHistory("Crash during subscribing");
            makeNotification("Crash", "in service");
        }}

    private void sendNotification(String msg, Boolean retained) {
        //PowerManager pm = (PowerManager) MainActivity.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //pm.wakeUp(SystemClock.uptimeMillis());
        String ts = "";
        String desc = "";
        String titelm = "";
        Integer Mins = 1000*60;
        try {
            JSONObject jInpts = new JSONObject(msg);
            ts = jInpts.optString("ts").toString();
            desc = jInpts.optString("message").toString();
            titelm = jInpts.optString("titel").toString();

        } catch (JSONException e) {
            makeNotification("Crash", "in service");
        }
        Date date = new Date(0);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try {
            date = format.parse(ts);
        } catch (ParseException e) {
            e.printStackTrace();
            makeNotification("Crash", "in service");
        }
        Date jetzt = new Date();
        String body = ts + ", " + titelm + ": " + desc;
        long diff = jetzt.getTime() - date.getTime();
        boolean missedMess = (date.getTime() >= (conLostDate.getTime() - 2*Mins)) & (date.getTime() <= (connectDate.getTime() + 2*Mins));
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
                makeNotification("TaskerCon", "Failed");
            }
        }
        if ((missedMess | !retained) & mess & (!task | tasdeb)) {
            makeNotification(titelm, body);
            addToHistory("Nachricht ausgegeben (Service)");
        }
    }

    private void makeNotification(String topic, String body){
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.steuerzen_icon)
                        .setContentTitle(topic)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(body))
                        .setContentText(body);

        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_LIGHTS|Notification.DEFAULT_VIBRATE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        NOTIFICATION_ID++;
    }

    private void addToHistory(String mainText){
        System.out.println("Service: " + mainText);

    }
}
