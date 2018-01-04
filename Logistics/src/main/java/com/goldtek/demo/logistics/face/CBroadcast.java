package com.goldtek.demo.logistics.face;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by darwinhu on 2018/1/3.
 */

public class CBroadcast {
    private final String TAG = "CBroadcast";

    private Context m_contx;
    // 普通格口控制器
    private static final String ACTION_IO_CONTROLLER = "android.intent.action.hal.iocontroller";

    private static final String ACTION_IO_CONTROLLER_ONOFF = ACTION_IO_CONTROLLER + ".onoff";
    private static final String ACTION_IO_CONTROLLER_OPEN = ACTION_IO_CONTROLLER + ".open";
    private static final String ACTION_IO_CONTROLLER_QUERY = ACTION_IO_CONTROLLER + ".query";
    private static final String ACTION_IO_CONTROLLER_QUERYALL = ACTION_IO_CONTROLLER + ".queryAll";

    private static final String ACTION_IO_CONTROLLER_QUERYDATA = ACTION_IO_CONTROLLER + ".querydata";
    private static final String ACTION_IO_CONTROLLER_QUERYALLDATA = ACTION_IO_CONTROLLER + ".queryAllData";
    private static final String ACTION_IO_CONTROLLER_ERROR = ACTION_IO_CONTROLLER + ".error";
    // 消息基础参数
    public static final String PARAM_MSG_CLIENT_ID = "msgClientId";
    public static final String PARAM_MSG_TIME = "msgTime";

    private static final String ACTION_PARAM_BOXID = "boxid";


    CBroadcast(Context contx){
        this.m_contx = contx;
    }

    private void sendBroadcastImpl(Intent intent) {
        String msgClientId = intent.getStringExtra(PARAM_MSG_CLIENT_ID);
        if (msgClientId == null) {
            msgClientId = genMsgClientId();
            intent.putExtra(PARAM_MSG_CLIENT_ID, msgClientId);
        }
        intent.putExtra(PARAM_MSG_TIME, generateMsgTime());
        m_contx.sendBroadcast(intent);
    }

    public synchronized String genMsgClientId() {
        StringBuffer buffer = new StringBuffer(16);
        // 获取唯一消息id
        SimpleDateFormat timeFmt = new SimpleDateFormat("yyMMddhhmm", Locale.getDefault());
        timeFmt.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        buffer.append(timeFmt.format(new Date()));
//        int max = 1000000;
//        int min = max / 10;
//
//        int sequence = Runnings.get().spGetHalMsgSeq();
//        sequence = sequence % (max - 1) + 1;
//        Runnings.get().spSetHalMsgSeq(sequence);
//
//        for (int i = sequence; i < min; i = i * 10) {
//            buffer.append("0");
//        }
//        buffer.append(sequence);
        return buffer.toString();
    }

    private String generateMsgTime() {
        SimpleDateFormat timeFmt = new SimpleDateFormat("hhmmssSSS", Locale.getDefault());
        timeFmt.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return timeFmt.format(new Date());
    }

    public void iocontrollerOpen(String boxId, String msgClientId) {
        Intent intent = new Intent(ACTION_IO_CONTROLLER_OPEN);
        intent.putExtra(ACTION_PARAM_BOXID, boxId);
        intent.putExtra(PARAM_MSG_CLIENT_ID, msgClientId);
        sendBroadcastImpl(intent);
        Log.d(TAG, "iocontrollerOpen " + msgClientId);
    }
}