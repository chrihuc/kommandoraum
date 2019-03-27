package com.asuscomm.chrihuc.schaltzentrale;


public class DropDListContainer {
    String Name;
    DropDList Szenen[];
    Integer x_value;
    Integer y_value;
    public DropDListContainer (String Name, DropDList Szenen[], Integer x_value, Integer y_value)
    {
        this.Name = Name;
        this.Szenen = Szenen;
        this.x_value = x_value;
        this.y_value = y_value;
    }

    public String getCommandById(Integer ItemId, DropDList Szenen[])
    {   String Befehl = new String();
        for (int i = 0; i < Szenen.length; i++){
            if (Szenen[i].ItemId.equals(ItemId)){
                Befehl = Szenen[i].Befehl;
                break;
            }
        }
        return Befehl;
    }
}
