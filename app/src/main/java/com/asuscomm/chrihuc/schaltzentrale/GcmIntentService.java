package com.asuscomm.chrihuc.schaltzentrale;

/**
 * Created by christoph on 23.12.16.
 */


        import com.google.android.gms.gcm.GoogleCloudMessaging;

        import android.app.IntentService;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.os.PowerManager;
        import android.os.SystemClock;
        import android.preference.PreferenceManager;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import org.json.JSONObject;
        import org.json.JSONException;



/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "Homecontrol";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String titel = extras.getString("titel");
        String message = extras.getString("message");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Error", "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Error", "Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i = 0; i < 5; i++) {
                    Log.i(TAG, "Working... " + (i + 1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                if (titel.equals("Setting")){
                    Boolean rec_taskmes = prefs.getBoolean("checkbox_pref_showtaskmess", true);
                    Boolean rec_tasks = prefs.getBoolean("checkbox_pref_task", true);
                    if (rec_tasks) {
                        if ( TaskerIntent.testStatus( this ).equals( TaskerIntent.Status.OK ) ) {
                            TaskerIntent i = new TaskerIntent( message );
                            sendBroadcast( i );
                        }
                        if (rec_taskmes) {
                            sendNotification(titel, message);
                        }
                    } else {
                        if (rec_taskmes) {
                            sendNotification(titel, "Tasker intent not ok");
                        }
                    }
                }else{
                    Boolean rec_mes = prefs.getBoolean("checkbox_pref_mes", true);
                    if (rec_mes) {
                        sendNotification(titel, message);
                    }
                }
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String titel, String msg) {
        //PowerManager pm = (PowerManager) MyApplication.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //pm.wakeUp(SystemClock.uptimeMillis());
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.steuerzen_icon)
                        .setContentTitle(titel)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}