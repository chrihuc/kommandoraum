package com.asuscomm.chrihuc.schaltzentrale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class ServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String mqttId = prefs.getString("mqttId", "");
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
//                System.out.println("incomingNumber : " + incomingNumber);
//                int myNum = 0;
//
//                try {
//                    myNum = Integer.parseInt(incomingNumber);
//                } catch(NumberFormatException nfe) {
//                    System.out.println("Could not parse " + nfe);
//                }
                try {
                    send_mqtt_serv("Inputs/Satellite/Call/" + mqttId, "{\"Value\": " + incomingNumber.substring(1) + "}");
                }catch(Exception e){}
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }
}
