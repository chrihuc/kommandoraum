package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by christoph on 22.02.17.
 */

public class WeckerView {

    private Activity activity;

    public WeckerView(Activity activity) {
        this.activity = activity;
    }
    //@SuppressWarnings("unused")
    //@Override
    public void schlafen_show(String setWeckers) throws JSONException {

        int x1 = 600;
        int x2 = 121;
        int y1 = 100;
        int y2 = 80;
        if (false) {
            x1 = 600;
            y1 = 100;
        } else {
            x1 = 1000;
            x2 = 210;
            y1 = 200;
            y2 = 160;
        }
        final WeckerListe[] liste = new WeckerListe[11];
        String result = null;
        liste[1] = new WeckerListe( "Eingeschaltet", false);
        liste[2] = new WeckerListe( "Mo", false);
        liste[3] = new WeckerListe( "Di", false);
        liste[4] = new WeckerListe( "Mi", false);
        liste[5] = new WeckerListe( "Do", false);
        liste[6] = new WeckerListe( "Fr", false);
        liste[7] = new WeckerListe( "Sa", false);
        liste[8] = new WeckerListe( "So", false);
        liste[9] = new WeckerListe( "Licht", false);
        liste[10] = new WeckerListe( "Audio", false);

        // list received not json object
        JSONObject jMqtt = new JSONObject(setWeckers);
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
        boolean enab = false;

        for (int i = 0; i < lengthJsonArr; i++) {
            JSONObject jsonChildNode = jArr.getJSONObject(i);
            String Name = jsonChildNode.optString("Name").toString();

            RelativeLayout row = new RelativeLayout(this.activity);
            //row.setOrientation(2);
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView t = new TextView(this.activity);
            t.setId((i * 20) + 1);
            t.setText(Name);
            t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            row.addView(t);


           for(int j = 1; j < 9; j++)
            {
                ToggleButton b = new ToggleButton(this.activity);
                Boolean ena = Boolean.valueOf(jsonChildNode.optString(liste[j].Name).toString().toLowerCase());
                params = new RelativeLayout.LayoutParams(x2, y2);
                //b.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                b.setText(liste[j].Name);
                b.setTextOn(liste[j].Name);
                b.setTextOff(liste[j].Name);
                b.setId(j + 1 + (i * 20));
                //b.setText("N" +j + 1 + (i * 9));
                if ((j < 6)) {
                    params.addRule(RelativeLayout.BELOW, ((i*20)+1));
                }else{
                    params.addRule(RelativeLayout.BELOW, (j - 4 + (i * 20)));
                }
                if ((j != 1) && (j < 6)) {
                    params.addRule(RelativeLayout.RIGHT_OF, (j  + (i * 20)));
                }else{
                    if (j > 6) {
                        params.addRule(RelativeLayout.RIGHT_OF, (j + (i * 20)));
                    }
                }
                b.setChecked(ena);
                b.setLayoutParams(params);
                row.addView(b);
            }
            //TimePicker
            TextView tt = new TextView(this.activity);
            tt.setId(18 + (i * 20));
            tt.setText("Zeit");
            params = new RelativeLayout.LayoutParams(x1, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL);
            params.topMargin = 400;
            tt.setLayoutParams(params);
            //row.addView(tt);
            TimePicker startTime = new TimePicker(this.activity);
            startTime.setIs24HourView(true);
            startTime.setId(19+ (i * 20));
            startTime.setHour(Integer.parseInt(jsonChildNode.optString("Time").toString()) / 3600);
            startTime.setMinute((Integer.parseInt(jsonChildNode.optString("Time").toString())%3600)/60);
            startTime.setLayoutParams(params);
            row.addView(startTime);
            ll.addView(row);
        }
        Button bu = new Button(this.activity);
        bu.setLayoutParams(new LinearLayout.LayoutParams(x1, y1));
        bu.setText("Speichere Wecker");
        bu.setId(20 + (lengthJsonArr * 20));

        bu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    JSONArray jArrRet = new JSONArray();
                    for (int k = 0; k < lengthJsonArr; k++) {
                        JSONObject json = new JSONObject();
                        TextView tt = (TextView) ll.findViewById((k * 20) + 1);
                        CharSequence nm = tt.getText();
                        for (int j = 1; j < 9; j++) {
                            ToggleButton b2 = (ToggleButton) ll.findViewById(j + 1 + (k * 20));
                            liste[j].Wert = b2.isChecked();
                        }
                        TimePicker startTimer = (TimePicker) ll.findViewById(19 + (k * 20));
                        int Hour = startTimer.getHour();
                        int Min = startTimer.getMinute();
                        int Sec = Hour * 3600 + Min * 60;
                        int Id = 1;
                        String Uhrzeit = String.valueOf(Hour) + ":" + String.valueOf(Min) + ":" + "00";
                        json.put("Name", nm.toString());
                        json.put("Eingeschaltet", bool_to_string(liste[1].Wert));
                        json.put("Mo", bool_to_string(liste[2].Wert));
                        json.put("Di", bool_to_string(liste[3].Wert));
                        json.put("Mi", bool_to_string(liste[4].Wert));
                        json.put("Do", bool_to_string(liste[5].Wert));
                        json.put("Fr", bool_to_string(liste[6].Wert));
                        json.put("Sa", bool_to_string(liste[7].Wert));
                        json.put("So", bool_to_string(liste[8].Wert));
                        //json.put("Licht", liste[9].Wert);
                        //json.put("Audio", liste[10].Wert);
                        json.put("Time", Uhrzeit);
                        //json.put("Id", k + 1);

                        //Toast.makeText(getApplicationContext(), (CharSequence) "here", Toast.LENGTH_SHORT).show();
                        /*
                        HttpClient client = new DefaultHttpClient();
                        String url = "http://192.168.192.10:8080/wecker_change.php";

                        HttpPost request = new HttpPost(url);
                        request.setEntity(new ByteArrayEntity(json.toString().getBytes("UTF8")));
                        request.setHeader("json", json.toString());
                        HttpResponse response = client.execute(request);
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                            //InputStream instream = entity.getContent();
                            String derText = reader.readLine();

                            //Toast.makeText(getApplicationContext(), (CharSequence) derText, Toast.LENGTH_SHORT).show();

                        }

                    }
                } catch (Throwable t) {

                    Toast.makeText(getApplicationContext(),  "Request failed: " + t.toString(), Toast.LENGTH_LONG).show();

                }		send_to_server("wecker_gestellt");
                Toast.makeText(getApplicationContext(),  "Wecker gespeichert", Toast.LENGTH_LONG).show();
                try {
                    synchronized(this){
                        wait(1000);
                    }
                }
                catch(InterruptedException ex){
                }
                String text = "";
                try {
                    String next_alarm = Setting(32);
                    if (next_alarm.equals("[]")){
                        text = "Kein Wecker in den n�chsten 12h.";
                    }else{
                        text = next_alarm;
                    }
                    Toast.makeText(getApplicationContext(),  text, Toast.LENGTH_LONG).show();
                        */
                        jArrRet.put(json);
                    }
                    send_wecker_to_server(jArrRet.toString());
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        layout.addView(bu);
        /*
        Button bs = new Button(this.activity);
        params = new RelativeLayout.LayoutParams(x1, y1);
        params.addRule(RelativeLayout.RIGHT_OF, (20 + (lengthJsonArr * 20)));
        bs.setLayoutParams(params);
        bs.setText("Schlafen");
        bs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Schlafen?");
                String AlarmText = null;
                AlarmText = "Schlafen gehen?";
                builder.setMessage(AlarmText);
                builder.setPositiveButton("OK, gehe schlafen.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        send_to_server("Schlafen");
                    }
                });
                builder.setNeutralButton("Ohne Licht", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        send_to_server("Schlafen_leise");
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        Toast.makeText(getApplicationContext(), "Cancel is clicked", Toast.LENGTH_LONG).show();
                    }
                });
                builder.show(); //To show the AlertDialog
            }
        });
        layout.addView(bs);*/
        layout.addView(sv);
    }

    public String send_wecker_to_server(String Befehl){
//        try {
//            String msg = "{'GCM-Client':'";
            String msg = "{\"payload\":" + Befehl +"}";
//            msg = msg +  "'}";
            MqttConnectionManagerService.send_mqtt_serv("DataRequest/SetTable/Wecker",msg);
//            String antwort = SocketClient.request(msg);
//            return antwort;
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
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
