package com.example.yendry.internetchecker2.broadcast;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.yendry.internetchecker2.R;
import com.example.yendry.internetchecker2.eventbus.CancelRing;
import com.example.yendry.internetchecker2.services.NetworkService;
import com.example.yendry.internetchecker2.services.Worker;
import com.example.yendry.internetchecker2.utils.SwitchSubstring;


import org.greenrobot.eventbus.EventBus;

import static com.example.yendry.internetchecker2.utils.Constants.ACTION_BROADCAST_WIDGET;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_OFF;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_ON;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_START;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_STOP;
import static com.example.yendry.internetchecker2.utils.Constants.IS_FOREGROUND_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.IS_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.SHPF;
import static com.example.yendry.internetchecker2.utils.Constants.START_ON_TOUCH;
import static com.example.yendry.internetchecker2.utils.Constants.WIDGET_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.WORKER_RUNNING;


/**
 * Implementation of App Widget functionality.
 */
public class BroadcastWidget extends AppWidgetProvider {
    private static final String TAG = "kaka BroadcastWidget";
    private static int mCounter = 0;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.broadcast_widget);
        // Construct an Intent which is pointing this class.
        Intent intent = new Intent(context, BroadcastWidget.class);
        intent.putExtra(WIDGET_NOTIFICATION, START_ON_TOUCH);
        intent.setAction(ACTION_BROADCAST_WIDGET);
        // And this time we are sending a broadcast with getBroadcast
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_content, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String notification ="";
        if (intent.getExtras() != null) {
            notification = intent.getExtras().getString(WIDGET_NOTIFICATION, "");
        }

        if (ACTION_BROADCAST_WIDGET.equals(intent.getAction())) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.broadcast_widget);

            if (!TextUtils.isEmpty(notification)) {
                SwitchSubstring.of(notification)
                        .when(CHANGE_WIFI_ICON_NOTIFICATION_ON, () -> views.setImageViewResource(R.id.image_in_widget, R.drawable.wifi_on))
                        .when(CHANGE_WIFI_ICON_NOTIFICATION_OFF, () -> views.setImageViewResource(R.id.image_in_widget, R.drawable.wifi_off))
                        .when(CHANGE_WIFI_ICON_SERVER_START, () -> views.setInt(R.id.indicator_id, "setBackgroundResource", R.drawable.background_green))
                        .when(CHANGE_WIFI_ICON_SERVER_STOP, () -> {
                            Log.d(TAG, "CHANGE_WIFI_ICON_SERVER_STOP: ");
                            views.setInt(R.id.indicator_id, "setBackgroundResource", R.drawable.background_red);
                            views.setImageViewResource(R.id.image_in_widget, R.drawable.stop_icon);
                        })
                        .when(START_ON_TOUCH, () -> startService(context));
            }


            // This time we dont have widgetId. Reaching our widget with that way.
            ComponentName appWidget = new ComponentName(context, BroadcastWidget.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidget, views);

        }
    }

    private void startService(Context context) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(SHPF, Context.MODE_PRIVATE);
        if (pref.getBoolean(IS_SERVICE, true) || pref.getBoolean(IS_FOREGROUND_SERVICE, false)){
            if (!isMyServiceRunning(NetworkService.class, context)){
                context.startService(new Intent(context, NetworkService.class));
            }else {
                context.stopService(new Intent(context, NetworkService.class));
            }
        } else {
            if (isJobServiceOn(context, 123)) {
//                Worker.run = false;
                pref.edit().putBoolean(WORKER_RUNNING, false).apply();
                cancelNotification(context);
            } else {
                Worker.enqueueWork(context);
            }
        }


    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public  boolean isJobServiceOn(Context context , int jobId) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService( Context.JOB_SCHEDULER_SERVICE ) ;

        boolean hasBeenScheduled = false ;

        for ( JobInfo jobInfo : scheduler.getAllPendingJobs() ) {
            if ( jobInfo.getId() == jobId ) {
                hasBeenScheduled = true ;
                break ;
            }
        }

        return hasBeenScheduled ;
    }
    public void cancelNotification(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(0);
        EventBus.getDefault().post(new CancelRing());
    }
}

