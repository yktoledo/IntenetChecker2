package com.example.yendry.internetchecker2.utils;

import android.content.Context;
import android.content.Intent;

import com.example.yendry.internetchecker2.broadcast.BroadcastWidget;

import static com.example.yendry.internetchecker2.utils.Constants.ACTION_BROADCAST_WIDGET;
import static com.example.yendry.internetchecker2.utils.Constants.WIDGET_NOTIFICATION;

/**
 * Created by yendry.calana on 3/28/18.
 */

public class BroadcastUtil {
   public static void sendWidgetBroadcast(Context context, String extra){
       Intent intent = new Intent(context, BroadcastWidget.class);
       intent.putExtra(WIDGET_NOTIFICATION, extra);
       intent.setAction(ACTION_BROADCAST_WIDGET);
       context.sendBroadcast(intent);
   }
}
