package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.goldtek.demo.protocol.client.IClientProtocol;

/**
 * Created by Terry on 2017/12/25 0025.
 */

public class DummyProtocol implements IClientProtocol {
    private final Handler mHandler;
    private Thread mThread;

    public DummyProtocol(Handler h) {
        mHandler = h;
    }

    private boolean m_bInterrupt = false;
    private boolean m_bProcess = false;

    @Override
    public void start() {
        if (mThread != null) return;

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
                    if (m_bProcess) mHandler.sendMessage(new Message());
                    m_bProcess = false;
                }
            }
        });
        mThread.start();
    }

    @Override
    public void onStop() {
        if (mThread != null) mThread.interrupt();
        m_bInterrupt = true;
        mThread = null;
    }

    @Override
    public boolean sendImage(String szName, Bitmap bmp) {
        m_bProcess = true;
        return true;
    }

    @Override
    public boolean isReady() {
        return (mThread != null) ? mThread.isAlive() : false;
    }

    public boolean isProcessing() {
        return m_bProcess;
    }

}
