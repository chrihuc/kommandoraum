package com.asuscomm.chrihuc.schaltzentrale;

import android.service.quicksettings.TileService;

import static com.asuscomm.chrihuc.schaltzentrale.MqttConnectionManagerService.send_mqtt_serv;

public class TileTuerAuf extends TileService {
    public TileTuerAuf() {
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
////        throw new UnsupportedOperationException("Not yet implemented");
//        return null;
//    }

    public void onClick (){
        send_mqtt_serv("Command/Szene/RiegelAuf", "{\"Szene\":\"RiegelAuf\"}");
//        send_mqtt_serv("Command/Szene/Test", "{\"Szene\":\"Test\"}");
    }
}
