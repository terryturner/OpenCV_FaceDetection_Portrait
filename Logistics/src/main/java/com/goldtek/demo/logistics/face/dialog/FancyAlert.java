package com.goldtek.demo.logistics.face.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.goldtek.demo.logistics.face.CBroadcast;
import com.goldtek.demo.logistics.face.MainFragment;
import com.goldtek.demo.logistics.face.R;

/**
 * Created by Terry on 2017/12/25 0025.
 */

public class FancyAlert extends Dialog implements
        android.view.View.OnClickListener {

    private Activity mActivity;
    private Builder mBuilder;
    private View.OnClickListener mClickListener;
    private int mEventID;

    private static FancyAlert getInstance(Builder builder, Activity act) {
        FancyAlert dialog = new FancyAlert(act);
        dialog.mBuilder = builder;
        return dialog;
    }


    public FancyAlert(Activity a) {
        super(a);
        // TODO Auto-generated constructor stub
        this.mActivity = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fancy_alert);
        findViewById(R.id.click_btn).setOnClickListener(this);

        if (mBuilder.mIconRes != -1) ((ImageView)findViewById(R.id.fancy_icon)).setImageResource(mBuilder.mIconRes);
        if (mBuilder.mTitle != null) ((TextView)findViewById(R.id.title)).setText(mBuilder.mTitle);
        if (mBuilder.mMessage != null) ((TextView)findViewById(R.id.message)).setText(mBuilder.mMessage);
        if (mBuilder.mClickMessage != null)
            ((Button)findViewById(R.id.click_btn)).setText(mBuilder.mClickMessage);
        else
            ((Button)findViewById(R.id.click_btn)).setVisibility(View.GONE);
        if (mBuilder.mClick != null) mClickListener = mBuilder.mClick;
        if (mBuilder.mEventId > 0) mEventID = mBuilder.mEventId;

        setCancelable(false);
        getWindow().setGravity(Gravity.CENTER);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.click_btn:
                if (mClickListener != null) {
                    v.setTag(mEventID);
                    mClickListener.onClick(v);
                }
                break;
            default:
                break;
        }
        dismiss();
    }

    public static class Builder {
        private int mIconRes = -1;
        private int mEventId = -1;
        private String mTitle = null;
        private String mMessage = null;
        private String mClickMessage = null;
        private View.OnClickListener mClick = null;

        public Builder setIconResource(int res) {
            mIconRes = res;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setMessage(String msg) {
            mMessage = msg;
            return this;
        }

        public Builder setClickMessage(String msg) {
            mClickMessage = msg;
            return this;
        }

        public Builder setOnClickListener(View.OnClickListener listener) {
            mClick = listener;
            return this;
        }

        public FancyAlert create(Activity act, int eventId) {
            mEventId = eventId;
            return FancyAlert.getInstance(this, act);
        }

        public FancyAlert show(final Activity act, final int eventId) {
            final FancyAlert dialog = create(act, eventId);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();



                new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        dialog.dismiss();
                        if (eventId == MainFragment.REQUEST_IDENTIFY) {
                            CBroadcast.iocontrollerOpen(act, "Z08");
                        }
                    }
                }.start();


            return dialog;
        }
    }
}