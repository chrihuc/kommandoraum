package com.asuscomm.chrihuc.schaltzentrale;

public class HkzLabel {
    String Name;
    Integer x_value;
    Integer y_value;
    String unit;
    Integer id;
    Integer level;
    String text;
    Boolean devel;

    public HkzLabel (String Name, Integer x_value, Integer y_value, String unit, Integer id, Integer level, String text, Boolean devel)
    {
        this.Name = Name;
        this.x_value = x_value;
        this.y_value = y_value;
        this.unit = unit;
        this.id = id;
        this.level = level;
        this.text = text;
        this.devel = devel;
    }
    public Integer getLevelbyName(String Name, HkzLabel args[])
    {
        for (int i = 0; i < args.length; i++){
            if (args[i].Name.equals(Name)){
                level = args[i].level;
                break;
            }
        }
        return level;
    }

    public Integer getIdbyName(String Name, HkzLabel args[])
    {
        for (int i = 0; i < args.length; i++){
            if (args[i].Name.equals(Name)){
                id = args[i].id;
                break;
            }
        }
        return id;
    }
}
