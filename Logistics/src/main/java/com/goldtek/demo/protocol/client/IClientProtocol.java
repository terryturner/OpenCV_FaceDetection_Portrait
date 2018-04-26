package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created by Terry on 2018/1/3 0003.
 */

public interface IClientProtocol {
    String PROTOCOL_CONNECT =
            "<GOLDTEK>" +
                "<service>M</service>" +
                "<cmd>%s</cmd>" +
                "<name>%s</name>" +
                "<id>%s</id>" +
                "<info>" +
                    "<regnum>%d</regnum>" +
                    "<type>%d</type>" +
                    "<solution>%d</solution>" +
                "</info>" +
            "</GOLDTEK>";
    String PROTOCOL_SENDHEADER =
            "<GOLDTEK>" +
                    "<size>%s</size>" +
                    "<id>%s</id>" +
                    "<name>%s</name>" +
            "</GOLDTEK>";

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
        public static final String UNKNOWN = "UNKNOWN";
    }
    String Hndl_MSG             = "MSG";
    String Hndl_MSGTYPE         = "MSGTYPE";
    class MSGTYPE {
        public static final String STATUS = "STATUS";
        public static final String RECV   = "RECV";
        public static final String ERR   = "ERROR";
        public static final String OTHER  = "OTHER";
    }
    class SVRSOLUTION {
        public static final int SOL_TENSORFLOW = 0;
        public static final int SOL_LBPH = 2;
        public static final int SOL_LBPHIST = 3;
    }
    class SVRSOLTYPE {
        public static final int RECV_PICTURE = 0;
        public static final int RECV_FEATURE = 1;
    }

    void start();
    void onStop();
    boolean sendImage(String szName, Bitmap bmp);
    boolean sendVector(List<Float> features, boolean big_endian);
    boolean isReady();
    boolean isProcessing();

}
