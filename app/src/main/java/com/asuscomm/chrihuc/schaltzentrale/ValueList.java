package com.asuscomm.chrihuc.schaltzentrale;


import java.util.Date;

public class ValueList {
    public ValueList(String HKS, String Value, String TS, String Description) {
        this.HKS = HKS;
        this.Value = Value;
        this.ts = TS;
        this.desc = Description;
    }

    public String getHKS () { return this.HKS; }
    public String getValue () {
        return this.Value; }
    public String getTS () { return this.ts; }
    public String getDesc () { return this.desc; }

    private String HKS;
    private String Value;
    private String ts;
    private String desc;


}
