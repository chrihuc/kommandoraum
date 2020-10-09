package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.quicksettings.TileService;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class MyQSTileService extends TileService {
    public MyQSTileService() {
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
////        throw new UnsupportedOperationException("Not yet implemented");
//        return null;
//    }

    public void onClick (){
        send_mqtt_serv("Command/Szene/GarageAufLicht", "{\"Szene\":\"GarageAufLicht\"}");
//        send_mqtt_serv("Command/Szene/Test", "{\"Szene\":\"Test\"}");
    }
}
