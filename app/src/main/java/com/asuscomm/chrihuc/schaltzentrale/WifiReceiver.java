package com.asuscomm.chrihuc.schaltzentrale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class WifiReceiver extends BroadcastReceiver {

    int connected = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String action = intent.getAction();
        String homeWifi = prefs.getString("homeWifi", "");
        String mqttId = prefs.getString("mqttId", "");
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

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (ssid.contains(homeWifi) & !(connected == 2)){
                send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 1}");
                connected = 2;
        } else if (!(connected==1)){//if (info != null && !info.isConnected()){
            send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 0}");
            connected = 1;
        }
    }

}
