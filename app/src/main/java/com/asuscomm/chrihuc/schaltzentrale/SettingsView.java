package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsView {
    private Activity activity;

    public SettingsView(Activity activity) {
        this.activity = activity;
    }
    //@SuppressWarnings("unused")
    //@Override
    public void settings_show(String szSettings) throws JSONException {

        int x1 = 1000;
        int x2 = 1000;
        int y1 = 200;
        int y2 = 160;

        // list received not json object
        JSONObject jMqtt = new JSONObject(szSettings);
        String jValue = jMqtt.optString("payload").toString();
        JSONArray jArr = new JSONArray(jMqtt.optString("payload"));
        //JSONArray jsonMainNode = jObject.optJSONArray("Wecker");
        final int lengthJsonArr = jArr.length();
        //String Name = jObject.getString("Name");

        LinearLayout layout = (LinearLayout) this.activity.findViewById(R.id.wecker);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this.activity);
        sv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT));
        final LinearLayout ll = new LinearLayout(this.activity);
        ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        //ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        sv.addView(ll);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);


        for (int i = 0; i < lengthJsonArr; i++) {
            JSONObject jsonChildNode = jArr.getJSONObject(i);
            final String Name = jsonChildNode.optString("Name").toString();
            final String Descr = jsonChildNode.optString("Description").toString();
            String Value = jsonChildNode.optString("Value").toString();
            final String execSzene = jsonChildNode.optString("execSzene").toString();
            String inApp = jsonChildNode.optString("inApp").toString();
            String Typ = jsonChildNode.optString("Typ").toString();
            if (inApp.equals("True")){
                RelativeLayout row = new RelativeLayout(this.activity);
                //row.setOrientation(2);
                row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                TextView t = new TextView(this.activity);
                t.setId((i * 20) + 1);
                t.setText(Descr);
                t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.addView(t);
                params = new RelativeLayout.LayoutParams(x2, y2);
                params.addRule(RelativeLayout.BELOW, ((i*20)+1));
                if (Typ.equals("bool")){
                    final ToggleButton b = new ToggleButton(this.activity);
                    b.setId((i * 20) + 2);
                    Boolean ena = Boolean.valueOf(Value);
                    b.setChecked(ena);
                    b.setLayoutParams(params);
                    b.setOnClickListener(new View.OnClickListener() {

                         @Override
                         public void onClick(View v) {
                             // TODO Auto-generated method stub
                             JSONObject json = new JSONObject();
                             try {
                                 json.put("Name", Name);
                                 if (b.isChecked()) {
                                     json.put("Value", "True");
                                 } else {
                                     json.put("Value", "False");
                                 }
                                 MqttConnectionManagerService.send_mqtt_serv("DataRequest/SetSettings/",json.toString());
                             } catch (JSONException e) {
                                 e.printStackTrace();
                             }
                                             }
                                         });
                    row.addView(b);
                } else if (Typ.equals("float")){
                    EditText et = new EditText(this.activity);
                    et.setId((i * 20) + 3);
                    et.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    et.setText(Value);
                    et.setLayoutParams(params);
                    row.addView(et);
                } else if (Typ.contains("list")){
                    Float valFloat = Float.parseFloat(Value);
                    String[] separated = Typ.split(",");
                    final Float min = Float.parseFloat(separated[1]);
                    final Float max = Float.parseFloat(separated[2]);
                    Integer pos = Math.round(100 * (valFloat-min)/(max-min));
                    SeekBar sBar = new SeekBar(this.activity);
                    sBar.setMin(0);
                    sBar.setMax(100);
                    sBar.setProgress(pos);
                    SeekBar.OnSeekBarChangeListener abc = new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            int progress = seekBar.getProgress();
                            Double newvalFloat = progress / 100.0 *(max - min) + min;
                            JSONObject json = new JSONObject();
                            try {
                                json.put("Name", Name);
                                json.put("Value", newvalFloat.toString());
                                MqttConnectionManagerService.send_mqtt_serv("DataRequest/SetSettings/",json.toString());
                                MqttConnectionManagerService.send_mqtt_serv("Command/Szene/" + execSzene, "{\"Szene\":\"" + execSzene + "\"}");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            //Executed when progress is changed
//                            System.out.println(progress);
                        }
                    };
                    sBar.setOnSeekBarChangeListener(abc);
                    sBar.setLayoutParams(params);

//                    LinearLayout.LayoutParams sBarLayParams=new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
                    row.addView(sBar);
                }
                ll.addView(row);
            }
        }

        layout.addView(sv);
    }

    public String send_wecker_to_server(String Befehl){
        String msg = "{\"payload\":" + Befehl +"}";
        MqttConnectionManagerService.send_mqtt_serv("DataRequest/SetTable/SetSettings",msg);
        return "";
    }

    public String bool_to_string(boolean wert){
        if (wert){
            return "True";
        } else {
            return "False";
        }
    }
}
