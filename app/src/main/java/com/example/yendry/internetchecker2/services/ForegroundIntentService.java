package com.example.yendry.internetchecker2.services;

import android.app.IntentService;
import android.content.Intent;

import com.example.yendry.internetchecker2.MainActivity;

public class ForegroundIntentService extends IntentService {

    public ForegroundIntentService() {
        super("ForegroundIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startActivity(new Intent(this, MainActivity.class));
    }
}
