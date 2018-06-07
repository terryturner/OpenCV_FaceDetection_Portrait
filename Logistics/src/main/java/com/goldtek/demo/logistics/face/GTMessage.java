package com.goldtek.demo.logistics.face;

/**
 * Created by Terry on 2018/4/26 0026.
 */
public class GTMessage {
    public static final int PROTOCOL_CREATE = 0x001;
    public static final int PROTOCOL_RELEASE = 0x002;

    public static final int SET_PROGRESS_VISIBLE = 0x110;
    public static final int SET_PROGRESS_INVISIBLE = 0x111;

    public static final int SET_SENDING_PROGRESS_VISIBLE = 0x110;
    public static final int SET_SENDING_PROGRESS_INVISIBLE = 0x111;
    public static final int SET_LEARNING_PROGRESS_VISIBLE = 0x112;

    public static final int MSG_PROCESSED_TF_FV = 0x201;
}
