package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;

/**
 * Created by Terry on 2018/1/3 0003.
 */

public interface IClientProtocol {
    class CMDTYPE {
        public static final String REG = "REGISTER";
        public static final String LOGIN = "LOGIN";
    }
    class XML {
        public static final String INFO = "info";
        public static final String RESULT = "result";
    }
    String Hndl_MSG             = "MSG";
    String Hndl_MSGTYPE         = "MSGTYPE";
    class MSGTYPE {
        static final String STATUS = "STATUS";
        static final String RECV   = "RECV";
        static final String OTHER  = "OTHER";
    }

    void start();
    void onStop();
    boolean sendImage(String szName, Bitmap bmp);
    boolean isReady();
    boolean isProcessing();

}
