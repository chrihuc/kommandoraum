package com.asuscomm.chrihuc.schaltzentrale;

import java.util.Date;

public class alarmlist implements Comparable<alarmlist> {

    private Date date;
    private String text;
    private String uuid;

    public alarmlist(Date date, String text, String uuid){
        this.date = date;
        this.text = text;
        this.uuid = uuid;
    }

    @Override
    public int compareTo(alarmlist f) {

        if (date.after(f.date)) {
            return 1;
        }
        else if (date.before(f.date)) {
            return -1;
        }
        else {
            return 0;
        }

    }

    @Override
    public String toString(){
        return this.text;

    }

    public String getuuid(){
        return this.uuid;

    }
}
