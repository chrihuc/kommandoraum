package com.asuscomm.chrihuc.schaltzentrale;

/**
 * Created by christoph on 23.08.17.
 */

public class ButtonFeatures {
    String Name;
    String Command;
    String Text;
    Integer x_value;
    Integer y_value;

    public ButtonFeatures (String Name, String Command, String Text, Integer x_value, Integer y_value)
    {
        this.Name = Name;
        this.Command = Command;
        this.Text = Text;
        this.x_value = x_value;
        this.y_value = y_value;
    }
    public String getCommandByName(String Name, ButtonFeatures args[])
    {
        for (int i = 0; i < args.length; i++){
            if (args[i].Name.equals(Name)){
                Command = args[i].Command;
                break;
            }
        }
        return Command;
    }
}
