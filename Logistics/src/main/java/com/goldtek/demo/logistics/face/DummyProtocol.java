package com.goldtek.demo.logistics.face;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidclient.IClientProtocol;

/**
 * Created by Terry on 2017/12/25 0025.
 */

public class DummyProtocol implements IClientProtocol {
    private final Handler mHandler;
    private Thread mThread;
    private int limit = 10;
    private int result = 0;

    public DummyProtocol(Handler h) {
        mHandler = h;

    }

    public void start(final Bitmap bitmap) {
        result++;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Utils.saveTempBitmap(bitmap);
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

    private boolean m_bInterrupt = false;
    @Override
    public void start() {
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!m_bInterrupt) {
                    try {
                        //Utils.saveTempBitmap(bitmap);
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //Log.i("terry", "thread send msg");
                    mHandler.sendMessage(new Message());
                }
            }
        });
        mThread.start();
    }

    @Override
    public void OnStop() {
        mThread.interrupt();
        m_bInterrupt = true;
    }

    @Override
    public boolean sendImage(String szName, Bitmap bmp) {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    public boolean isProcessing() {
        return (mThread != null) ? mThread.isAlive() : false;
    }

    public int get() {
        return result;
    }

    public boolean complete() {
        return result >= limit;
    }
}
