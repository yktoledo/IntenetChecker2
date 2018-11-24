package com.example.yendry.internetchecker2.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.example.yendry.internetchecker2.eventbus.CancelRing;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by yendry.calana on 3/27/18.
 */

public class NotificationIntentService extends IntentService {

    private NotificationManager notificationManager;

    public NotificationIntentService() {
        super("mi");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(0);
        EventBus.getDefault().post(new CancelRing());
    }
}