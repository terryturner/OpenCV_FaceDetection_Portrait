package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;

/**
 * Created by Terry on 2018/1/3 0003.
 */

public interface IClientProtocol {
    class CMDTYPE {
        public static final String REG = "REGISTER";
        public static final String REG_DONE = "REGISTER_DONE";
        public static final String LOGIN = "LOGIN";
        public static final String LOGIN_DONE = "LOGIN_DONE";
    }
    class XML {
        public static final String INFO = "info";
        public static final String RESULT = "result";
    }
    class RESULT {
        public static final String SUCCESS = "1";
        public static final String FAIL = "-1";
    }
    String Hndl_MSG             = "MSG";
    String Hndl_MSGTYPE         = "MSGTYPE";
    class MSGTYPE {
        public static final String STATUS = "STATUS";
        public static final String RECV   = "RECV";
        public static final String ERR   = "ERROR";
        public static final String OTHER  = "OTHER";
    }

    void start();
    void onStop();
    boolean sendImage(String szName, Bitmap bmp);
    boolean isReady();
    boolean isProcessing();

}
