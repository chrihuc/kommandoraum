package com.asuscomm.chrihuc.schaltzentrale;

import java.util.List;

/**
 * Created by christoph on 17.01.18.
 */

public class DeviceButton {
    String Name;
    String Hks;
    List Szenen;
    Integer x_value;
    Integer y_value;
    public DeviceButton (String Name, String Hks, List Szenen, Integer x_value, Integer y_value)
    {
        this.Name = Name;
        this.Hks = Hks;
        this.Szenen = Szenen;
        this.x_value = x_value;
        this.y_value = y_value;
    }
}
