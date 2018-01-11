package com.asuscomm.chrihuc.schaltzentrale;

import java.util.Arrays;
import java.util.List;

/**
 * Created by christoph on 10.01.18.
 */

public class SzenenButton {
    String Name;
    List Szenen;
    Integer x_value;
    Integer y_value;
    public SzenenButton (String Name, List Szenen, Integer x_value, Integer y_value)
    {
        this.Name = Name;
        this.Szenen = Szenen;
        this.x_value = x_value;
        this.y_value = y_value;
    }
}
