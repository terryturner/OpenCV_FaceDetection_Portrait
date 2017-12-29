package com.goldtek.demo.logistics.face;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Terry on 2017/12/29 0029.
 */

public class RegisterImage extends ImageView {

    public RegisterImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        int x = canvas.getWidth()/2;
        int y = canvas.getHeight()/2;

        getDrawable().setBounds(0,0,x,y);
        getDrawable().draw(canvas);
    }
}
