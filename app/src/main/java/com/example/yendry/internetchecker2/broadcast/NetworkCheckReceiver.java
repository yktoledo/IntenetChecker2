package com.example.yendry.internetchecker2.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.yendry.internetchecker2.services.NetworkService;
import com.example.yendry.internetchecker2.services.Worker;

import static com.example.yendry.internetchecker2.utils.Constants.IS_FOREGROUND_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.SHPF;
import static com.example.yendry.internetchecker2.utils.Constants.WORKER_RUNNING;

public class NetworkCheckReceiver extends BroadcastReceiver {

    private static final String TAG = "kaka NetworkCheck";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences(SHPF, Context.MODE_PRIVATE);
        Log.d(TAG, "NetworkCheckReceiver:+++++++++ ");
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//        if(info != null && "CONNECTED".equalsIgnoreCase(info.getDetailedState().name())) {
//            // Do your work.
//            Log.d(TAG, "onReceive: on");
//            // e.g. To check the Network Name or other info:
//            getCxnInfo(context);
//        }else if(info != null && "DISCONNECTED".equalsIgnoreCase(info.getDetailedState().name())){
//            Log.d(TAG, "onReceive: off");
//
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !pref.getBoolean(IS_FOREGROUND_SERVICE, false)) {
////                Worker.run = false;
//                pref.edit().putBoolean(WORKER_RUNNING, false).apply();
//            } else {
//                context.stopService(new Intent(context, NetworkService.class));
//            }
//
//        }
    }

    private void getCxnInfo(Context context) {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.d(TAG, "ssid: "+ssid);
    }
}