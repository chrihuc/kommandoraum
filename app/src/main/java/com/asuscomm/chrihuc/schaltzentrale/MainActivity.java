package com.asuscomm.chrihuc.schaltzentrale;
import com.asuscomm.chrihuc.schaltzentrale.SocketClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.google.android.gms.iid.InstanceID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    Context context;
    String regid;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String TAG = "Schaltzentrale";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    TextView mDisplay;

    String SENDER_ID = "568669847245";
    GoogleCloudMessaging gcm;

    public Boolean server_online = true;


    @Override
    public void onBackPressed()
    {
        showMain();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            send_to_server("{'Szene':'EGLeiser'}");
        }
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            send_to_server("{'Szene':'EGLauter'}");
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            showMain();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.
                ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);

        //setHasOptionsMenu(true);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        context = getApplicationContext();

        //INSERT ping or IP check to see what is online

        //Google play services for receiving messages

        Activity act = this;
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (checkPlayServices(act) && server_online) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            if (regid.isEmpty()) {registerInBackground();} else{sendRegistrationIdToBackend(regid);}
        } else { Log.i(TAG, "No valid Google Play Services APK found.");}

        showMain();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();

        if (id == R.id.og) {
            showOG();
            return true;
        }
        if (id == R.id.dg) {
            showDG();
            return true;
        }
        if (id == R.id.ug) {
            showUG();
            return true;
        }
        if (id == R.id.settings) {
            startActivity(new Intent(this, EinstellungenActivity.class));
            return true;
        }
        if (id == R.id.wecker) {
            showWecker();
            return true;
        }
        return true;
    }

    public void showWecker() {
        setContentView(R.layout.wecker);
        String setWeckers = req_from_server("Wecker");
        try {
            WeckerView wV = new WeckerView(this);
            wV.schlafen_show(setWeckers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void showMain() {
        //String sets = req_from_server("Settings");
        String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.activity_main);

        final ButtonFeatures[] inptList = new ButtonFeatures[4];
        inptList[0] = new ButtonFeatures("V00WOH1RUM1TE01", "V00WOH1RUM1TE01", "V00WOH1RUM1TE01", 600, 450);
        inptList[1] = new ButtonFeatures("V00WOH1RUM1CO01", "V00WOH1RUM1CO01", "V00WOH1RUM1CO01", 600, 500);
        inptList[2] = new ButtonFeatures("A00TER1GEN1TE01", "A00TER1GEN1TE01", "A00TER1GEN1TE01", 420, 0);
        inptList[3] = new ButtonFeatures("V00KUE1RUM1TE02", "V00KUE1RUM1TE02", "V00KUE1RUM1TE02", 300, 1200);
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
        setInptLabels(inptList, mrl);

        final Button but = new Button(this);
        RelativeLayout.LayoutParams rlt = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlt.addRule(RelativeLayout.ALIGN_BOTTOM);
        rlt.leftMargin = 50;
        rlt.topMargin = 300;
        rlt.width = 210;
        //rl.height = buttonH;
        but.setLayoutParams(rlt);
        but.setText("A/V");
        but.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, but);
                //popup.getMenu().add(groupId, itemId, order, title);
                popup.getMenu().add("TV");
                popup.getMenu().add("SonosEG");
                popup.getMenu().add("Radio");
                popup.getMenu().add("AVaus");
                popup.getMenu().add("EGLauter");
                popup.getMenu().add("EGLeiser");
                popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        send_to_server("{'Szene':'" + item.getTitle() + "'}");
                        return true;
                    }
                });
                popup.show();
            }
        });
        RelativeLayout mrlt  = (RelativeLayout) findViewById(R.id.relLayout);
        mrlt.addView(but);


        final ButtonFeatures[] Blist = new ButtonFeatures[3];
        Blist[0] = new ButtonFeatures("V00KUE1DEK1LI01", "Off", "Aus", 100, 1400);
        Blist[1] = new ButtonFeatures("V00KUE1DEK1LI02", "Off", "Aus", 100, 1000);
        Blist[2] = new ButtonFeatures("V00ESS1DEK1LI01", "Off", "Aus", 170, 750);
        for (int i = 0; i < 3; i++) {

            Button bu = new Button(this);
            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            rl.addRule(RelativeLayout.ALIGN_BOTTOM);
            rl.leftMargin = Blist[i].x_value;
            rl.topMargin = Blist[i].y_value;
            rl.width = 180;
            //rl.height = buttonH;
            bu.setLayoutParams(rl);
            bu.setText(Blist[i].Text);
            final String name = Blist[i].Name;
            final String command = Blist[i].Command;
            bu.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    send_to_server("{'Device':'" + name + "', 'Command':'" + command + "'}");
                }
            });
            //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
            mrl.addView(bu);
        }
    }

    public void setInptLabels(ButtonFeatures[] bfList, RelativeLayout mrl){
        String werte = req_from_server("Inputs_hks");
        try {
            JSONObject jInpts = new JSONObject(werte);
            for (int i = 0; i < bfList.length; i++) {
                TextView tv = new TextView(this);
                RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                rl.addRule(RelativeLayout.ALIGN_BOTTOM);
                rl.leftMargin = bfList[i].x_value;
                rl.topMargin = bfList[i].y_value;
                //rl.width = 160;
                //rl.height = buttonH;
                tv.setLayoutParams(rl);
                JSONObject jInpt = jInpts.getJSONObject(bfList[i].Name);
                String valueread = jInpt.optString("last_Value").toString();
                tv.setText(valueread);
                tv.setBackgroundColor(Color.WHITE);
                //RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayout);
                mrl.addView(tv);
            }

        } catch (JSONException e) {

        }
    }

    public void showOG() {
        //String sets = req_from_server("Settings");
        String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.obergeschoss);

        final ButtonFeatures[] inptList = new ButtonFeatures[3];
        inptList[0] = new ButtonFeatures("V01BAD1RUM1TE01", "V01BAD1RUM1TE01", "V01BAD1RUM1TE01", 600, 1400);
        inptList[1] = new ButtonFeatures("V01SCH1RUM1TE01", "V01SCH1RUM1TE01", "V01SCH1RUM1TE01", 200, 1200);
        inptList[2] = new ButtonFeatures("V01KID1RUM1TE01", "V01KID1RUM1TE01", "V01KID1RUM1TE01", 700, 300);
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutOG);
        setInptLabels(inptList, mrl);
    }

    public void showDG() {
        //String sets = req_from_server("Settings");
        String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.dachgeschoss);

        final ButtonFeatures[] inptList = new ButtonFeatures[1];
        inptList[0] = new ButtonFeatures("V02ZIM1RUM1TE02", "V02ZIM1RUM1TE02", "V02ZIM1RUM1TE02", 300, 1000);
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutDG);
        setInptLabels(inptList, mrl);
    }

    public void showUG() {
        //String sets = req_from_server("Settings");
        String inpts = req_from_server("Inputs_hks");
        setContentView(R.layout.untergeschoss);

        final ButtonFeatures[] inptList = new ButtonFeatures[1];
        inptList[0] = new ButtonFeatures("Vm1ZIM1RUM1TE01", "Vm1ZIM1RUM1TE01", "Vm1ZIM1RUM1TE01", 600, 400);
        RelativeLayout mrl  = (RelativeLayout) findViewById(R.id.relLayoutUG);
        setInptLabels(inptList, mrl);
    }


    public boolean checkPlayServices(Activity act) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(act);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, act,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                //finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        //here crashes
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    public void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend(regid);

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    public void sendRegistrationIdToBackend(String regid) {
        String android_id = Settings.Secure.getString(getBaseContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String User_name = prefs.getString("User_name", "");
        if (User_name.equals("")){
            startActivity(new Intent(this, EinstellungenActivity.class));
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            User_name = prefs.getString("@string/preference_name_key", "");
        }

        String msg = "{'GCM-Client':'";
        String ende = "'}";
        //send_to_server(msg + android_id + ende);
        msg = "{'Android_id':'" + android_id;
        msg = msg + "', 'Name':'" + User_name;
        msg = msg + "', 'Reg_id':'" + regid;
        ende = "'}";
        //Toast.makeText(this, "Register with Server.", Toast.LENGTH_SHORT).show();
        send_to_server(msg + ende);
    }

    public void send_to_server(String Befehl){
        try {
            SocketClient.broadc(Befehl);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String req_from_server(String Befehl){
        try {
            String msg = "{'GCM-Client':'";
            msg = "{'Request_js':'" + Befehl;
            msg = msg +  "'}";
            String antwort = SocketClient.request(msg);
            return antwort;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

}
