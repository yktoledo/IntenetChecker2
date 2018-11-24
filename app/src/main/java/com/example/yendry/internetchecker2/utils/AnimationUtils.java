package com.example.yendry.internetchecker2.utils;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AnimationUtils {
    public static ObjectAnimator translationY(View view, float from, float to) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "translationY", from, to).setDuration(300);
        objectAnimator.setInterpolator(new LinearInterpolator());
        return objectAnimator;
    }
    public static ObjectAnimator rotation(View view, float from, float to) {
        view.setPivotX(view.getWidth() / 2);
        view.setPivotY(view.getHeight()  / 2);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "rotation", from, to).setDuration(1500);
        objectAnimator.setInterpolator(new LinearInterpolator());
        return objectAnimator;
    }
}
