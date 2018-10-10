package com.asuscomm.chrihuc.schaltzentrale;


public class DigiObj {
    String HKS;
    Integer x_value;
    Integer y_value;
    Boolean inverse;
    public DigiObj (String HKS, Integer x_value, Integer y_value, Boolean inverse)
    {
        this.HKS = HKS;
        this.x_value = x_value;
        this.y_value = y_value;
        this.inverse = inverse;
    }
}
