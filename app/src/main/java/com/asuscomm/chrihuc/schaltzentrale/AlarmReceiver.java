package com.asuscomm.chrihuc.schaltzentrale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Recurring alarm; Checking Wifi Connection");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String homeWifi = prefs.getString("homeWifi", "");
        String mqttId = prefs.getString("mqttId", "");

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.contains(homeWifi)){
            send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 1}");
        } else {//if (info != null && !info.isConnected()){
            send_mqtt_serv("Inputs/Satellite/Handy/" + mqttId, "{\"Value\": 0}");
        }
    }

}
