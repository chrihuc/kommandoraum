package com.asuscomm.chrihuc.schaltzentrale;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.asuscomm.chrihuc.schaltzentrale.GoogleMessaging;
import com.google.android.gms.iid.InstanceID;

public class MainActivity extends Activity {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    static final String TAG = "Schaltzentrale";
    Context context;
    String regid;

    String SENDER_ID = "192696193588";
    GoogleCloudMessaging gcm;

    public Boolean server_online = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        context = getApplicationContext();

        //INSERT ping or IP check to see what is online

        //Google play services for receiving messages
        //Attention needs MySQL Server
        Activity act = this;
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        GoogleMessaging gmes = new GoogleMessaging();
        if (gmes.checkPlayServices(act) && server_online) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = gmes.getRegistrationId(context);
            if (regid.isEmpty()) {gmes.registerInBackground();} else{gmes.sendRegistrationIdToBackend(regid);}
        } else { Log.i(TAG, "No valid Google Play Services APK found.");}


            setContentView(R.layout.activity_main);
    }
}
