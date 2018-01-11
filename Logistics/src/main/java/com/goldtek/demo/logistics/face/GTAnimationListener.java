package com.goldtek.demo.logistics.face;

import android.view.View;
import android.view.animation.Animation;

/**
 * Created by Terry on 2018/1/11 0011.
 */

public class GTAnimationListener implements Animation.AnimationListener {
    private final View view;
    private final int visible;

    public GTAnimationListener(View view, int visible) {
        this.view = view;
        this.visible = visible;
    }
    public void onAnimationEnd(Animation animation) {
        view.setVisibility(visible);
    }
    public void onAnimationRepeat(Animation animation) {
    }
    public void onAnimationStart(Animation animation) {
    }
}
