package com.asuscomm.chrihuc.schaltzentrale;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {


    static String SznList[] = {"GarageAuf", "Moe", "Curly"};

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, int[] appWidgetIds) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);

        Intent intent = new Intent(context, ClickIntentService.class);
        intent.setAction(ClickIntentService.ACTION_CLICK);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("KEY_ID", appWidgetId);
        views.setOnClickPendingIntent(R.id.garageAuf, pendingIntent);

//        int i = 0;
//        for (int appWidgetIdi : appWidgetIds) {
//            context.getSharedPreferences("sp", MODE_PRIVATE).edit().putString("szene_" + appWidgetIdi, SznList[i]);
//            i++;
//            if (i > SznList.length -1){
//                i = SznList.length -1;
//            }
//        }

//        int clicks = context.getSharedPreferences("sp", MODE_PRIVATE).getInt("clicks", 0);
//        String szene = context.getSharedPreferences("sp", MODE_PRIVATE).getString("szene_" + appWidgetId, "TemplStr");
//        views.setTextViewText(R.id.garageAuf, "Garagentor AUF");

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them


        int i = 0;
        for (int appWidgetId : appWidgetIds) {
            context.getSharedPreferences("sp", MODE_PRIVATE).edit().putString("szene_" + appWidgetId, SznList[i]);
            updateAppWidget(context, appWidgetManager, appWidgetId, appWidgetIds);
            i++;
            if (i > SznList.length -1){
                i = SznList.length -1;
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

