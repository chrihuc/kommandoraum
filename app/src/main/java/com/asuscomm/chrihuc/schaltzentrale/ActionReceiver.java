package com.asuscomm.chrihuc.schaltzentrale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class ActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //Toast.makeText(context,"recieved",Toast.LENGTH_SHORT).show();

        String action=intent.getStringExtra("payload");
        send_mqtt_serv("Command/Szene/" + action, "{\"Szene\":\""+action+"\"}");
//        if(action.equals("action1")){
//            performAction1();
//        }
//        else if(action.equals("action2")){
//            performAction2();
//
//        }
        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }

    public void performAction1(){

    }

    public void performAction2(){

    }

}
