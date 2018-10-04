package com.asuscomm.chrihuc.schaltzentrale;


import java.util.Date;

public class ValueList {
    public ValueList(String HKS, String Value, String TS) {
        this.HKS = HKS;
        this.Value = Value;
        this.ts = TS;
    }

    public String getHKS () { return this.HKS; }
    public String getValue () {
        return this.Value; }
    public String getTS () { return this.ts; }

    private String HKS;
    private String Value;
    private String ts;


}
