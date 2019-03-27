package com.asuscomm.chrihuc.schaltzentrale;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class PerformBackgroundTask extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] objects) {
        return null;
    }

    public void execute(Context context){
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
