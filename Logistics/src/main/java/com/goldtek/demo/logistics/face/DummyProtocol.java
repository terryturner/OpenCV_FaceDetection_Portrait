package com.goldtek.demo.logistics.face;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Terry on 2017/12/25 0025.
 */

public class DummyProtocol {
    private final Handler mHandler;
    private Thread mThread;
    private int result = 0;

    public DummyProtocol(Handler h) {
        mHandler = h;

    }

    public void start() {
        result++;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Log.i("terry", "thread send msg");
                mHandler.sendMessage(new Message());
            }
        });
        mThread.start();
    }

    public void stop() {
        if (mThread != null) mThread.interrupt();
    }

    public boolean isProcessing() {
        return (mThread != null) ? mThread.isAlive() : false;
    }

    public int get() {
        return result;
    }
}
