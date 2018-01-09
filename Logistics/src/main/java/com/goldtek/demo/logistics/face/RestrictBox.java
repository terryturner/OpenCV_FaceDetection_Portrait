package com.goldtek.demo.logistics.face;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.Rect;

/**
 * Created by Terry on 2017/12/28 0028.
 */

public class RestrictBox extends View {
    private Paint mClrPaint = new Paint();
    private Paint mBoarderPaint = new Paint();
    private final float mCenterRatioX;
    private final float mCenterRatioY;
    private final float mDistanceRatioX;
    private final float mDistanceRatioY;
    private final Rect mRect;

    public RestrictBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.RestrictBox,
                0, 0);

        try {
            a.getBoolean(R.styleable.RestrictBox_showText, false);
            a.getInteger(R.styleable.RestrictBox_labelPosition, 0);
            mCenterRatioX = a.getFloat(R.styleable.RestrictBox_CenterRatioX, 1);
            mCenterRatioY = a.getFloat(R.styleable.RestrictBox_CenterRatioY, 1);
            mDistanceRatioX = a.getFloat(R.styleable.RestrictBox_DistanceRatioX, 1);
            mDistanceRatioY = a.getFloat(R.styleable.RestrictBox_DistanceRatioY, 1);
        } finally {
            a.recycle();
        }


        mBoarderPaint.setStyle(Paint.Style.STROKE);
        mBoarderPaint.setColor(ContextCompat.getColor(getContext(), R.color.transparentLightGrey));
        mBoarderPaint.setStrokeWidth(10);

        mClrPaint.setColor(Color.TRANSPARENT);
        mClrPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);
        //Log.i("terry", "canvas: " + canvas.getWidth() + " " + canvas.getHeight());

        //center
        int cx = (int) (canvas.getWidth() * mCenterRatioX);
        int cy = (int) (canvas.getHeight() * mCenterRatioY);
        int dx = (int) (canvas.getWidth() * mDistanceRatioX);
        int dy = (int) (canvas.getHeight() * mDistanceRatioY);

        //draw guide box
        mRect.x = cx - dx;
        mRect.y = cy - dy;
        mRect.width = 2 * dx;
        mRect.height = 2 * dy;

        canvas.drawRect(mRect.x, mRect.y, mRect.x + mRect.width, mRect.y + mRect.height, mClrPaint);
        canvas.drawRect(mRect.x, mRect.y, mRect.x + mRect.width, mRect.y + mRect.height, mBoarderPaint);

    }

    public float getCenterRatioX() {
        return mCenterRatioX;
    }

    public float getCenterRatioY() {
        return mCenterRatioY;
    }

    public float getDistanceRatioX() {
        return mDistanceRatioX;
    }

    public float getDistanceRatioY() {
        return mDistanceRatioY;
    }
}