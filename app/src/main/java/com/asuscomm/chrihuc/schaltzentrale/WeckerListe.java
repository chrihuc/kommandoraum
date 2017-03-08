package com.asuscomm.chrihuc.schaltzentrale;

/**
 * Created by christoph on 22.02.17.
 */

public class WeckerListe {
    String Name;
    boolean Wert;

    public WeckerListe(String Name, boolean i)
    {
        this.Name = Name;
        this.Wert = i;
    }
    public boolean getValueByName(String Name, WeckerListe args[])
    {
        for (int i = 0; i < args.length; i++){
            if (args[i].Name.equals(Name)){
                Wert = args[i].Wert;
                break;
            }
        }
        return Wert;
    }
}
