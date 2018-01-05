package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Created by darwinhu on 2017/12/25.
 */

/***
 * PROTOCOL
 * 1. REGISTER
 *      example:
 *      // Authorization
 *          send: <Info><cmd>REGISTER</cmd><name>Fred</name><id>Fred</id></Info>
 *          recv_Success: <GOLDTEK><info>REGISTER</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>REGISTER</info><result>-1</result></GOLDTEK>
 *
 *      // Asynchronize Sending Image, Every Image [PACKET] will included [HEADER][IMAGE]
 *          send: <GOLDTEK><size>27192</size><id>Fred</id><name>Fred_0</name></GOLDTEK>...IMAGE...
 *          recv_Success: <GOLDTEK><info>Fred_0</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>Fred_0</info><result>-1</result></GOLDTEK>
 *
 *      // Sending the next Image with different name
 *          send: <GOLDTEK><size>27192</size><id>Fred</id><name>Fred_1</name></GOLDTEK>...IMAGE...
 *          . . .
 *      // Client will receive the last Message from Server that register is success
 *          recv_Success: <GOLDTEK><info>REGISTER</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>REGISTER</info><result>-1</result></GOLDTEK>
 *
 *
 * 2. LOGIN
 *      example:
 *      // Authorization
 *          send: <Info><cmd>LOGIN</cmd><name>Fred</name><id>Fred</id></Info>
 *          recv_Success: <GOLDTEK><info>LOGIN</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>LOGIN</info><result>-1</result></GOLDTEK>
 *
 *      // Asynchronize Sending Image, Every Image [PACKET] will included [HEADER][IMAGE]
 *          send: <GOLDTEK><size>27192</size><id>Fred</id><name>Fred_0</name></GOLDTEK>...IMAGE...
 *          recv_Success: <GOLDTEK><info>Fred_0</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>Fred_0</info><result>-1</result></GOLDTEK>
 *
 *      // Client will receive the last Message from Server that recognition is success
 *          recv_Success: <GOLDTEK><info>LOGIN</info><result>Fred 100%</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>LOGIN</info><result>UNKNOWN</result></GOLDTEK>
 *
 */

public class GtClient implements IClientProtocol {

    private static final String TAG = "CClientConnection";
    private static final int BUFFSIZE = 512;
    private static final int TIMEOUT_CONNECT = 1000;
    public static final int PORT = 6666;

    private int m_nPort;
    private String m_szSvrIP;

    private Socket m_socket = null;
    private String m_szCmd;
    private String m_szName;
    private String m_szID;

    private Handler m_Handler;
    private Listener mListener = new Listener();
    private Sender   mSender   = new Sender();

    private boolean m_bRunning = false;
    private boolean m_bReady = false;
    private boolean m_bSending = false;

    /**
     *
     * @param handler handler interface for callback
     * @param nPort Server Port Number
     * @param szSvrIP Server IP Address
     * @param szCmd use CMDTYPE.REG or CMDTYPE.LOGIN
     * @param szName Client Name
     * @param szID Client ID
     */
    public GtClient(Handler handler, int nPort, String szSvrIP, String szCmd,
                    String szName, String szID){
        this.m_Handler = handler;
        this.m_nPort    = (nPort > 0) ? nPort : PORT;
        this.m_szSvrIP  = szSvrIP;
        this.m_szCmd    = szCmd;
        this.m_szName   = szName;
        this.m_szID     = szID;
    }

    public void start() {
        mSender.start();
        mListener.start();
    }

    private class Listener extends Thread implements Runnable {
        private boolean bInterrupt = false;
        private boolean bIsConnect = false;

        @Override
        public void run() {
            super.run();

            while(!bInterrupt){
                bIsConnect = Connecting();
                if (bIsConnect)
                    break;
                else {
                    Disconnecting();
                    callback_Status(false);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(bIsConnect) {
                try {
                    DataInputStream mInput = new DataInputStream(m_socket.getInputStream());

                    byte[] inputData = null;
                    while (m_bRunning) {
                        if(!m_socket.isConnected())
                            break;

                        try {
                            inputData = new byte[BUFFSIZE];
                            int nRecvSize = mInput.read(inputData);
                            if (nRecvSize <= 0) {
                                Thread.sleep(1000);
                                continue;
                            } else {
                                String szMsg = Common.readByteToString(inputData);
                                String[] separated = szMsg.split("</GOLDTEK><GOLDTEK>");
                                for(String split : separated){
                                    Log.d(TAG, split);
                                    // Handler send receive msg
                                    callback_Receive(szMsg);
                                }

                            }
                        } catch (InterruptedException e) {
                            Log.e(TAG, "InterruptedException");
                            callback_Error(e.getClass().getCanonicalName());
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback_Error(e.getClass().getCanonicalName());
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    callback_Error(e.getClass().getCanonicalName());
                }
            }
        }

        public void OnStop() {
            this.interrupt();
            bInterrupt = true;
        }

    }


    private class Sender extends Thread implements Runnable {
        private boolean bInterrupt = false;
        private byte[] btsPackets = null;

        @Override
        public void run() {
            super.run();

            while(!bInterrupt){
                if (m_bRunning && btsPackets != null) {
                    try {
                        DataOutputStream output = new DataOutputStream(m_socket.getOutputStream());
                        if(m_socket.isConnected()){
                            output.write(btsPackets);
                            Log.d(TAG, "Sending Packet ... " + String.valueOf(btsPackets.length));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getLocalizedMessage());

                        callback_Error(e.getClass().getCanonicalName());
                    }
                    btsPackets = null;
                }
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void OnStop() {
            this.interrupt();
            bInterrupt = true;
        }

        public boolean Sending(byte[] packet) {
            if (m_bSending) return false;
            m_bSending = true;
            btsPackets = packet;
            return true;
        }
    }

    public void onStop() {
        Log.d(TAG, "onStop");
        mListener.OnStop();
        mSender.OnStop();
        Disconnecting();
        m_Handler.removeCallbacksAndMessages(null);
    }

    public boolean sendImage(String szName, Bitmap bmp) {
        DataPacket oPacket = new DataPacket(m_szID, szName, bmp);
        mSender.Sending(oPacket.getM_data());
        return true;
    }

    public boolean isReady() {
        return m_bReady;
    }

    public boolean isProcessing() {
        return m_bSending;
    }

    private synchronized boolean Connecting(){
        boolean ret = false;
        try {
            Log.e(TAG, "Trying Connecting");
            m_socket = new Socket();
            m_socket.connect(new InetSocketAddress(m_szSvrIP, m_nPort), TIMEOUT_CONNECT);

            m_bRunning = true;
            // Send Handler status
            callback_Status(true);
            String szMsg = ComposeAuth(m_szCmd, m_szName, m_szID);
            Log.d(TAG, "--> " + szMsg);
            ret = mSender.Sending(szMsg.getBytes());
        } catch (IOException e) {
            // Send Handler status
            callback_Status(false);
            e.printStackTrace();
            Log.e(TAG, "Not Connected");
        }
        return ret;
    }

    private synchronized void Disconnecting(){
        m_bRunning = false;
        if(m_socket != null) {
            try {
                m_socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            m_socket = null;
        }
        // Send Handler status
    }

    //==============================================================================================
    private void callback_Status(boolean isConnectedNow){
        // Send Message Into Main Thread
        Log.d(TAG,"callback_Status " + String.valueOf(isConnectedNow));

        Message msg = m_Handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(Hndl_MSGTYPE, MSGTYPE.STATUS);
        b.putString(Hndl_MSG, (isConnectedNow ? "1" : "-1"));
        msg.setData(b);
        m_Handler.sendMessage(msg);
    }

    private void callback_Receive(String message){
        // Send Message Into Main Thread
        Log.d(TAG,"<-- " + message);
        m_bSending = false;

        Message msg = m_Handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(Hndl_MSGTYPE, MSGTYPE.RECV);
        b.putString(Hndl_MSG, message);
        msg.setData(b);
        m_Handler.sendMessage(msg);

        if (Common.getTagValue(message, IClientProtocol.XML.INFO).equalsIgnoreCase(m_szCmd)) {
            if (Common.getTagValue(message, IClientProtocol.XML.RESULT).equalsIgnoreCase(RESULT.SUCCESS))
                m_bReady = true;
            else
                m_bReady = false;
        }
    }

    private void callback_Error(String message) {
        m_bReady = false;
        m_bSending = false;
        m_bRunning = false;

        Message msg = m_Handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(Hndl_MSGTYPE, MSGTYPE.ERR);
        b.putString(Hndl_MSG, message);
        msg.setData(b);
        m_Handler.sendMessage(msg);
    }

    //==============================================================================================

    private String ComposeAuth(String szCmd, String szName, String szID){
        String szInfo = String.format("<Info><cmd>%s</cmd><name>%s</name><id>%s</id></Info>",
                szCmd, szName, szID);
        return szInfo;
    }


}
