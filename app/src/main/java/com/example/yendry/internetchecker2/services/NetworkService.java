package com.example.yendry.internetchecker2.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.example.yendry.internetchecker2.R;
import com.example.yendry.internetchecker2.eventbus.CancelRing;
import com.example.yendry.internetchecker2.eventbus.HasInternet;
import com.example.yendry.internetchecker2.eventbus.ServiceMessage;
import com.example.yendry.internetchecker2.utils.BroadcastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.example.yendry.internetchecker2.App.CHANNEL_ID;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_OFF;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_NOTIFICATION_ON;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_START;
import static com.example.yendry.internetchecker2.utils.Constants.CHANGE_WIFI_ICON_SERVER_STOP;
import static com.example.yendry.internetchecker2.utils.Constants.DEFAULT_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.FIRST_TIME;
import static com.example.yendry.internetchecker2.utils.Constants.IS_FOREGROUND_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.MUSIC_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.PERIOD;
import static com.example.yendry.internetchecker2.utils.Constants.SHPF;


public class NetworkService extends Service {

    private static final String TAG = "kaka NetworkService";
    private Disposable interval;
    private int period = 5;
    SharedPreferences pref;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private MediaPlayer player;
    private boolean ring = false;
    private boolean default_notification = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pref = getApplicationContext().getSharedPreferences(SHPF, Context.MODE_PRIVATE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onStartCommand: ");
                if (!EventBus.getDefault().isRegistered(this)) {
                    EventBus.getDefault().register(NetworkService.this);
                }

                period = pref.getInt(PERIOD, 5);
                Log.d(TAG, "run: "+period);
                default_notification = pref.getBoolean(DEFAULT_NOTIFICATION, false);
                ring = pref.getBoolean(MUSIC_NOTIFICATION, false);
                interval = getSubscribe();
                BroadcastUtil.sendWidgetBroadcast(NetworkService.this, CHANGE_WIFI_ICON_SERVER_START);
            }
        }).start();
        if (pref.getBoolean(IS_FOREGROUND_SERVICE, false)){
            startFor();
        }
        return START_STICKY;
    }

    private void startFor() {
        Intent yepIntent = new Intent(this, ForegroundIntentService.class);
        PendingIntent yepPendingIntent = PendingIntent.getService(this, 0, yepIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle("Internet Checker")
                .setContentText("Service is running...")
                .setContentIntent(yepPendingIntent)
                .build();
        startForeground(123, notification);
    }




    private void ringsTone() {
        player = MediaPlayer.create(this, R.raw.alan_walker_fade);
        player.start();
    }

    private void showNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setSmallIcon(R.drawable.ic_wifi_black_24dp)
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

    @NonNull
    private Disposable getSubscribe() {
        return Observable.interval(period, TimeUnit.SECONDS)
                .subscribe(aLong -> {
                    Log.d(TAG, "getSubscribe: " + pref.getBoolean(FIRST_TIME, true));
                    Log.d(TAG, "getSubscribe: " + period);
                    if (isInternetAvailable2()) {
                        BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_NOTIFICATION_ON);
                        if (pref.getBoolean(FIRST_TIME, true)) {
                            showNotification();
                            if (pref.getBoolean(MUSIC_NOTIFICATION, false)) {
                                ringsTone();
                            }
                            pref.edit().putBoolean(FIRST_TIME, false).apply();
                        }
                        EventBus.getDefault().post(new HasInternet(true));
                    } else {
                        EventBus.getDefault().post(new HasInternet(false));
                        BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_NOTIFICATION_OFF);
                        pref.edit().putBoolean(FIRST_TIME, true).apply();
                    }
                });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        BroadcastUtil.sendWidgetBroadcast(this, CHANGE_WIFI_ICON_SERVER_STOP);
        if (player != null) {
            player.stop();
        }
        if (!interval.isDisposed()) {
            interval.dispose();
        }
        if (notificationManager != null) {
            notificationManager.cancel(0);
        }
        if (pref != null)
            pref.edit().putBoolean(FIRST_TIME, true).apply();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe()
    public void onMessageEvent(ServiceMessage event) {
        Log.d(TAG, "onMessageEvent: " + event.getInterval());
        ring = pref.getBoolean(MUSIC_NOTIFICATION, false);
        default_notification = pref.getBoolean(DEFAULT_NOTIFICATION, false);

        if (event.getInterval() > 0) {
            period = event.getInterval();
            if (!interval.isDisposed()) {
                interval.dispose();
            }
            interval = getSubscribe();
        }


    }

    public boolean isInternetAvailable2() {
        try {
            InetAddress address = InetAddress.getByName("www.stackoverflow.com");
            Log.d(TAG, "Internet: On ");
            //Connected to working internet connection
            return true;
        } catch (UnknownHostException e) {
            Log.d(TAG, "Internet: Off ");
            e.printStackTrace();
            //Internet not available
            return false;
        }
    }

    @Subscribe()
    public void onMessageEvent(CancelRing event) {
        if (player != null) {
            player.stop();
        }
    }

}
