package com.example.yendry.internetchecker2;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.lang.ref.WeakReference;

public class App extends Application {

    public static final String CHANNEL_ID = "CHANNEL_ID_INTERNET_CHECKER";
    private static WeakReference<Context> context;

    @Override
    public void onCreate() {
        super.onCreate();
       context = new WeakReference<>(this);
        createNotificationChanne();
    }

    private void createNotificationChanne() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "InternetCheckerChannelName",
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);

        }
    }

    public static Context getAppContext() {
        return context.get();
    }

}
