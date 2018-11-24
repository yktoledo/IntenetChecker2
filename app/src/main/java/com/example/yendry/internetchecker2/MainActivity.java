package com.example.yendry.internetchecker2;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.yendry.internetchecker2.broadcast.MyReceiver;
import com.example.yendry.internetchecker2.broadcast.WifiJobSchedule;
import com.example.yendry.internetchecker2.eventbus.CancelRing;
import com.example.yendry.internetchecker2.eventbus.HasInternet;
import com.example.yendry.internetchecker2.services.NetworkService;
import com.example.yendry.internetchecker2.services.Worker;
import com.github.florent37.viewtooltip.ViewTooltip;
import com.github.rahatarmanahmed.cpv.CircularProgressView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.example.yendry.internetchecker2.utils.AnimationUtils.translationY;
import static com.example.yendry.internetchecker2.utils.Constants.DEFAULT_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.FIRST_TIME;
import static com.example.yendry.internetchecker2.utils.Constants.IS_FOREGROUND_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.IS_SERVICE;
import static com.example.yendry.internetchecker2.utils.Constants.MUSIC_NOTIFICATION;
import static com.example.yendry.internetchecker2.utils.Constants.SHPF;
import static com.example.yendry.internetchecker2.utils.Constants.WORKER_RUNNING;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "kaka";

    ImageSwitcher switcher;
    private static boolean isCheckingOn;
    private boolean isService;
    SharedPreferences pref;
    TextView state, internet;
    CircularProgressView progressView;
    private SwitchCompat music_notification_switch, foregroundSwitcher;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    private MyReceiver receiver;
    LinearLayout foreground_service_container;
    ImageView foreground_info;
    private long startMillis = 0;
    private int count = 0;
    private boolean startCounting = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = getApplicationContext().getSharedPreferences(SHPF, Context.MODE_PRIVATE);
        sharedListener();
        isService = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
        pref.edit().putBoolean(DEFAULT_NOTIFICATION, true).apply();
        pref.edit().putBoolean(IS_SERVICE, isService).apply();

        foreground_info = findViewById(R.id.foreground_info);
        foreground_info.setOnClickListener(v -> {
            //https://android-arsenal.com/details/1/5878
            ViewTooltip
                    .on(this, foreground_info)
                    .autoHide(true, 3000)
                    .corner(30)
                    .clickToHide(true)
                    .color(R.color.foreground_info_background_color)
                    .position(ViewTooltip.Position.TOP)
                    .text(R.string.foreground_info_text)
                    .show();
        });
        foreground_service_container = findViewById(R.id.foreground_service_container);
        music_notification_switch = findViewById(R.id.ring_switch);
        state = findViewById(R.id.txt_state);
        internet = findViewById(R.id.txt_internet);
        switcher = findViewById(R.id.switcher);
        progressView = findViewById(R.id.progress_view);
        initSwitcher();
        initForegroundSwitcher();
        initMusicSwitcher();
        registerReci();
        initHideTouchView();
//        scheduleJob();

    }

    @Override
    protected void onResume() {
        pref.registerOnSharedPreferenceChangeListener(listener);
        super.onResume();
        initText();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !pref.getBoolean(IS_FOREGROUND_SERVICE, false)) {
            isCheckingOn = isJobServiceOn(this, 123);
        } else {
            isCheckingOn = isMyServiceRunning(NetworkService.class);
        }
        switcher.setImageDrawable(isCheckingOn ? getResources().getDrawable(R.drawable.green2) : getResources().getDrawable(R.drawable.red2));
        if (!isCheckingOn) {
            changeInternetText(false);
        } else {
            internet.setText(getString(R.string.updating));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pref.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void initHideTouchView() {
        state.setOnClickListener(v -> {
            if (startCounting) {
                startCounting = false;
                Observable.timer(3, TimeUnit.SECONDS)
                        .subscribe(aLong -> {
                            startCounting = true;
                            count = 0;
                        });
            } else if (count > 5) {
                count = 0;
                foreground_service_container.setVisibility(View.VISIBLE);
            } else {
                count++;
            }
        });
    }


    private void initText() {

        ObjectAnimator disappear = translationY(state, 0, 200);
        disappear.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                state.setText(isCheckingOn ? getString(R.string.checking_on) : getString(R.string.checking_off));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    state.setTextColor(isCheckingOn ? ContextCompat.getColor(MainActivity.this, R.color.green) : ContextCompat.getColor(MainActivity.this, R.color.gray));
                } else {
                    state.setTextColor(isCheckingOn ? getResources().getColor(R.color.green) : getResources().getColor(R.color.gray));
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        ObjectAnimator appear = translationY(state, -200, 0);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(disappear, appear);
        animatorSet.start();

    }

    private void initMusicSwitcher() {
        if (isService) {
            music_notification_switch.setVisibility(View.VISIBLE);
            music_notification_switch.setChecked(pref.getBoolean(MUSIC_NOTIFICATION, false));
            music_notification_switch.setOnCheckedChangeListener((compoundButton, b) -> {
                pref.edit().putBoolean(MUSIC_NOTIFICATION, b).apply();
                if (!b) {
                    EventBus.getDefault().post(new CancelRing());
                }
            });
        } else {
            music_notification_switch.setVisibility(View.GONE);
        }
    }

    private void initForegroundSwitcher() {
        foregroundSwitcher = findViewById(R.id.foreground_service_switcher);
        foregroundSwitcher.setChecked(pref.getBoolean(IS_FOREGROUND_SERVICE, false));
        foreground_service_container.setVisibility(pref.getBoolean(IS_FOREGROUND_SERVICE, false) ? View.VISIBLE : View.INVISIBLE);
        foregroundSwitcher.setOnCheckedChangeListener((compoundButton, b) -> {
            pref.edit().putBoolean(IS_FOREGROUND_SERVICE, b).apply();
            if (!b && isMyServiceRunning(NetworkService.class)) {
                stopService(new Intent(this, NetworkService.class));
                isCheckingOn = false;
                replaceImage();
            }
        });
    }



    @SuppressLint("ClickableViewAccessibility")
    private void initSwitcher() {

        switcher.setFactory(() -> {
            ImageView imageView = new ImageView(getApplicationContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return imageView;
        });

        switcher.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    switcher.setScaleX(1f);
                    switcher.setScaleY(1f);
                    vibrate();
                    startChecking();
                    return true;
                case MotionEvent.ACTION_DOWN:
                    switcher.setScaleX(0.99f);
                    switcher.setScaleY(0.99f);
                    vibrate();
                    return true;
            }

            return false;
        });

    }

    private void registerReci() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.Broadcast.internet.notification");
        receiver = new MyReceiver();
        registerReceiver(receiver, filter);
    }

    @SuppressLint("CheckResult")
    private void sharedListener() {
        getSharedObservable().subscribe(s -> {
            Log.d("kaka", "sharedListener: " + s);
        });
    }

    public Observable<String> getSharedObservable() {
        return Observable.create(emitter -> listener = (sharedPreferences, key) -> emitter.onNext(key));
    }




    private void startChecking() {
        if (isService || pref.getBoolean(IS_FOREGROUND_SERVICE, false)) {
            startMyService();
            pref.edit().putBoolean(WORKER_RUNNING, false).apply();
        } else {
            startJobIntentService();
        }
        replaceImage();
    }

    private void startJobIntentService() {
        if (isJobServiceOn(this, 123)) {
            pref.edit().putBoolean(WORKER_RUNNING, false).apply();
//            Worker.run = false;
        } else {
            Worker.enqueueWork(this);
        }
        isCheckingOn = !isCheckingOn;
    }

    private void startMyService() {
        if (!isCheckingOn) {
            startService(new Intent(this, NetworkService.class));
        } else {
            stopService(new Intent(this, NetworkService.class));
        }
        isCheckingOn = !isCheckingOn;
    }

    @SuppressLint("CheckResult")
    private void replaceImage() {
        switcher.setImageDrawable(getResources().getDrawable(R.drawable.yellow));
        switcher.setEnabled(false);
        progressView.setVisibility(View.VISIBLE);
        Observable.timer(1500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    switcher.setEnabled(true);
                    switcher.setImageDrawable(isCheckingOn ? getResources().getDrawable(R.drawable.green2) : getResources().getDrawable(R.drawable.red2));
                    initText();
                    if (!isCheckingOn)
                        cancelNotification();
                    progressView.setVisibility(View.INVISIBLE);
                    if (!isCheckingOn) {
                        changeInternetText(false);
                        pref.edit().putBoolean(FIRST_TIME, true).apply();
                    }
                });
    }



    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(40);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean isJobServiceOn(Context context, int jobId) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                hasBeenScheduled = true;
                break;
            }
        }

        return hasBeenScheduled;
    }

    private void changeInternetText(boolean hasInternet) {
        internet.setText(hasInternet ? getString(R.string.internet_ready) : getString(R.string.no_internet));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            internet.setTextColor(hasInternet ? ContextCompat.getColor(MainActivity.this, R.color.green) : ContextCompat.getColor(MainActivity.this, R.color.gray));
        } else {
            internet.setTextColor(hasInternet ? getResources().getColor(R.color.green) : getResources().getColor(R.color.gray));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(HasInternet event) {
        Log.d(TAG, "onMessageEvent: " + event.isHasInternet());
        changeInternetText(event.isHasInternet());
    }

    public void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(0);
        EventBus.getDefault().post(new CancelRing());
    }



    //not used right now
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scheduleJob() {
        if (!isJobServiceOn(getApplicationContext(), 321)) {
            ComponentName componentName = new ComponentName(this, WifiJobSchedule.class);
            JobInfo info = new JobInfo.Builder(321, componentName)
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPersisted(true)
                    .build();

            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            int resultCode = scheduler.schedule(info);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Job scheduled");
            } else {
                Log.d(TAG, "Job scheduling failed");
            }
        }else {
            Log.d(TAG, "scheduleJob: is running ");
        }
    }
}
