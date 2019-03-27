package com.asuscomm.chrihuc.schaltzentrale;

import java.util.List;

public class DropDList {
    Integer GroupId;
    Integer ItemId;
    Integer Position;
    String Title;
    String Befehl;

    public DropDList (Integer GroupId, Integer ItemId, Integer Position, String Title, String Befehl)
    {
        this.GroupId = GroupId;
        this.ItemId = ItemId;
        this.Position = Position;
        this.Title = Title;
        this.Befehl = Befehl;
    }
    public String getCommandById(Integer ItemId, DropDList args[])
    {
        for (int i = 0; i < args.length; i++){
            if (args[i].ItemId.equals(ItemId)){
                Befehl = args[i].Befehl;
                break;
            }
        }
        return Befehl;
    }
}

