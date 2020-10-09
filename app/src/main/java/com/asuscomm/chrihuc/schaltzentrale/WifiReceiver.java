package com.asuscomm.chrihuc.schaltzentrale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class WifiReceiver extends BroadcastReceiver {

    public int connected = 0;
    public int btconn    = 0;

    Handler handler1 = new Handler();

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String action = intent.getAction();
        final String homeWifi = prefs.getString("homeWifi", "");
        final String mqttId = prefs.getString("mqttId", "");
        final boolean cb_auto_tor = prefs.getBoolean("cb_auto_tor", true);
//        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
//            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
//                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                String ssid = wifiInfo.getSSID();
//                if (ssid.contains(homeWifi)){
//                    send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 1}");
//                }
//            } else {
//                send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 0}");
//            }
//        }

//        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//        if(info != null && info.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();

            Runnable r = new Runnable() {
                public void run() {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String ssid = wifiInfo.getSSID();
                    if (!ssid.contains(homeWifi) & connected == 1){
                        send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 0}");
                    }
                }
            };


            if (ssid.contains(homeWifi) & !(connected == 2)){
                connected = 2;
                handler1.removeCallbacksAndMessages(null);
                send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 1}");
                if (btconn == 1 && cb_auto_tor){
                    send_mqtt_serv("Command/Szene/GarageAufLicht", "{\"Szene\":\"GarageAufLicht\"}");
//                    MqttConnectionManagerService.makeNotification("Bluetooth verbunden", device.toString(),9);
                }
//                Toast.makeText(context, "Wieder im Heimnetz", Toast.LENGTH_LONG).show();
            } else if (!(connected==1)){//if (info != null && !info.isConnected()){
                handler1.postDelayed(r, 30000);
                connected = 1;
            }
    }





}
