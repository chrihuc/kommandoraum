package com.asuscomm.chrihuc.schaltzentrale;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ClickIntentService extends IntentService {
    public static final String ACTION_CLICK = "com.asuscomm.chrihuc.schaltzentrale.widgets.click";

    public ClickIntentService() {
        super("ClickIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            int widgetId = intent.getIntExtra("KEY_ID", -1);

            if (ACTION_CLICK.equals(action)) {
                handleClick(widgetId);
            }
        }
    }

    private void handleClick(int widgetId) {
//        int clicks = getSharedPreferences("sp", MODE_PRIVATE).getInt("clicks", 0);
//        String szene = getSharedPreferences("sp", MODE_PRIVATE).getString("szene_" + widgetId, "");
//        clicks++;
//        getSharedPreferences("sp", MODE_PRIVATE)
//                .edit()
//                .putInt("clicks", clicks)
//                .commit();
        Toast.makeText(getBaseContext(), "Widget geklicked", Toast.LENGTH_LONG).show();
        MqttConnectionManagerService.send_mqtt_serv("Command/Szene/GarageAuf", "{\"Szene\":\"GarageAuf\"}");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, NewAppWidget.class));
        for (int appWidgetId : widgetIds) {
            NewAppWidget.updateAppWidget(getApplicationContext(), appWidgetManager, appWidgetId, widgetIds);
        }
    }
}
