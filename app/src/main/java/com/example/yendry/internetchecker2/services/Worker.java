package com.example.yendry.internetchecker2.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.yendry.internetchecker2.R;
import com.example.yendry.internetchecker2.eventbus.CancelRing;
import com.example.yendry.internetchecker2.eventbus.HasInternet;
import com.example.yendry.internetchecker2.utils.BroadcastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static com.example.yendry.internetchecker2.App.CHANNEL_ID;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_OFF;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_ON;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_START;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_STOP;
import static com.example.yendry.internetchecker2.utils.Constants.DEFAULT_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.FIRST_TIME;
import static com.example.yendry.internetchecker2.utils.Constants.SHPF;
import static com.example.yendry.internetchecker2.utils.Constants.WORKER_RUNNING;

public class Worker extends JobIntentService {

    private static final int DOWNLOAD_JOB_ID = 123;
    private static final String TAG = "kaka Worker";
    public static boolean run = true;
    SharedPreferences pref;
    private boolean default_notification = false;

    public static void enqueueWork(Context context) {
        Intent intent = new Intent(context, Worker.class);
//        intent.putExtra(RECEIVER, workerResultReceiver);
        run = true;
        enqueueWork(context, Worker.class, DOWNLOAD_JOB_ID, intent);

    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        pref = getApplicationContext().getSharedPreferences(SHPF, Context.MODE_PRIVATE);
        pref.edit().putBoolean(WORKER_RUNNING, true).apply();
        default_notification = pref.getBoolean(DEFAULT_NOTIFICATION, false);
        BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_SERVER_START);
        if (isInternetAvailable()) {
            BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_NOTIFICATION_ON);
            if (pref.getBoolean(FIRST_TIME, true)) {
                showNotification();
                pref.edit().putBoolean(FIRST_TIME, false).apply();
            }
            EventBus.getDefault().post(new HasInternet(true));
        } else {
            EventBus.getDefault().post(new HasInternet(false));
            BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_NOTIFICATION_OFF);
                pref.edit().putBoolean(FIRST_TIME, true).apply();
        }
        reSchedule(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_SERVER_STOP);
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "onDestroy: ");

    }

    private void reSchedule(@NonNull Intent intent) {
        try {

            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "run: "+pref.getBoolean(WORKER_RUNNING, false));
//        if (run) {
        if (pref.getBoolean(WORKER_RUNNING, false)) {
            enqueueWork(this, Worker.class, DOWNLOAD_JOB_ID, intent);
        } else {
            pref.edit().putBoolean(FIRST_TIME, true).apply();
        }
    }

    public boolean isInternetAvailable() {
        try {
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("www.yahoo.com", 80);

            sock.connect(sockaddr, 3000); // This will block no more than timeoutMs
            sock.close();
            Log.d(TAG, "Internet: On ");
            return true;

        } catch (IOException e) {
            Log.d(TAG, "Internet: Off ");
            return false;
        }
    }

    private void showNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wifi_black_24dp)
                .setAutoCancel(true)
                .setColor(getResources().getColor(R.color.notification_background))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_content));
        if (default_notification) {
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        Intent resultIntent = new Intent(this, NotificationIntentService.class);
        PendingIntent yepPendingIntent = PendingIntent.getService(this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(yepPendingIntent);

        notificationManager.notify(0, builder.build());
    }

    @Subscribe()
    public void onMessageEvent(CancelRing event) {
        Log.d("llll", ": " + event);
    }

}
