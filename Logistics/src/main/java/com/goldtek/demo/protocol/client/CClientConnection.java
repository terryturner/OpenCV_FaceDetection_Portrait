package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
 *          send: <GOLDTEK><info><cmd>REGISTER</cmd><name>Fred</name><id>Fred</id></info><solution></solution></GOLDTEK>
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
 *          recv_Success: <GOLDTEK><info>REGISTER_DONE</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>REGISTER_DONE</info><result>-1</result></GOLDTEK>
 *
 *
 * 2. LOGIN
 *      example:
 *      // Authorization
 *          send: <GOLDTEK><info><cmd>LOGIN</cmd><name>Fred</name><id>Fred</id></info><solution></solution></GOLDTEK>
 *          recv_Success: <GOLDTEK><info>LOGIN</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>LOGIN</info><result>-1</result></GOLDTEK>
 *
 *      // Asynchronize Sending Image, Every Image [PACKET] will included [HEADER][IMAGE]
 *          send: <GOLDTEK><size>27192</size><id>Fred</id><name>Fred_0</name></GOLDTEK>...IMAGE...
 *          recv_Success: <GOLDTEK><info>Fred_0</info><result>1</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>Fred_0</info><result>-1</result></GOLDTEK>
 *
 *      // Client will receive the last Message from Server that recognition is success
 *          recv_Success: <GOLDTEK><info>LOGIN_DONE</info><result>Fred 100%</result></GOLDTEK>
 *          recv_Failed: <GOLDTEK><info>LOGIN_DONE</info><result>UNKNOWN</result></GOLDTEK>
 *
 */

public class CClientConnection extends Thread implements Runnable, IClientProtocol {

    private String TAG = "CClientConnection";
    private int m_nPort;
    private String m_szSvrIP;

    private Socket m_socket = null;
    private String m_szCmd;
    private String m_szName;
    private String m_szID;

    private boolean m_bInterrupt = false;
    private boolean m_bIsReady = false;
    private boolean m_bIsProcessing = false;
    private Handler m_Handler;
    private int BUFFSIZE = 512;

    // Server Port
    public static final int PORT = 6666;

    /*** Command TYPE ***/
    public static class CMDTYPE {
        public static final String REG = "REGISTER";
        public static final String LOGIN = "LOGIN";
    }

    /*** Protocol  ***/
    public static class XML {
        public static final String INFO = "info";
        public static final String RESULT = "result";
    }

    /*** Handler ***/
    public static String Hndl_MSG             = "MSG";
    public static String Hndl_MSGTYPE         = "MSGTYPE";
    public static class MSGTYPE {
        static final String STATUS = "STATUS";
        static final String RECV   = "RECV";
        static final String OTHER  = "OTHER";
    }

    private boolean m_bRunning = false;
    private static final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");

    /**
     *
     * @param handler handler interface for callback
     * @param nPort Server Port Number
     * @param szSvrIP Server IP Address
     * @param szCmd use CMDTYPE.REG or CMDTYPE.LOGIN
     * @param szName Client Name
     * @param szID Client ID
     */
    public CClientConnection(Handler handler, int nPort, String szSvrIP, String szCmd,
                             String szName, String szID){
        this.m_Handler = handler;
        this.m_nPort    = (nPort > 0) ? nPort : PORT;
        this.m_szSvrIP  = szSvrIP;
        this.m_szCmd    = szCmd;
        this.m_szName   = szName;
        this.m_szID     = szID;
    }

    @Override
    public void run() {
        super.run();

//        // For Testing
//        for(int i = 0; i< 4 ; i++){
//            callback_Receive("Test" + String.valueOf(i));
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        boolean isConnected = false;
        while(!m_bInterrupt){
            isConnected = Connecting();
            if(isConnected)
                break;
            else{
                Disconnecting();
                callback_Status(false);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(isConnected) {
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
                            inputData = null;
                            continue;
                        } else {
                            String szMsg = readByteToString(inputData);
                            String[] separated = szMsg.split("</GOLDTEK><GOLDTEK>");
                            for(String split : separated){
                                Log.d(TAG, split);
                                // Handler send receive msg
                                callback_Receive(szMsg);
                            }

                        }
                    } catch (InterruptedException exception) {
                        Log.e(TAG, "InterruptedException");
                    } catch (Exception exception) {
                        Log.e(TAG, exception.getLocalizedMessage());
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean Sending(byte[] packet){
        m_bIsProcessing = true;
        boolean ret = false;
        try {
            DataOutputStream output = new DataOutputStream(m_socket.getOutputStream());
            if(m_socket.isConnected()){
                output.write(packet);
                Log.d(TAG, "Sending Packet ... " + String.valueOf(packet.length));
                ret = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getLocalizedMessage());
            ret = false;
        }
        return ret;
    }

    public void onStop() {
        Log.d(TAG, "onStop");
        this.interrupt();
        Disconnecting();
        m_Handler.removeCallbacks(this);
        m_bInterrupt = true;
    }

    public boolean sendImage(String szName, Bitmap bmp) {
        DataPacket oPacket = new DataPacket(m_szID, szName, bmp);
        byte[] packet = oPacket.getM_data();
        boolean isSending = Sending(packet);
        return isSending;
    }

    public boolean isReady() {
        return m_bIsReady;
    }

    public boolean isProcessing() {
        return m_bIsProcessing;
    }

    private boolean Connecting(){
        boolean ret = false;
        try {
            m_socket = new Socket(m_szSvrIP, m_nPort);
            m_bRunning = true;
            // Send Handler status
            callback_Status(true);
            String szMsg = ComposeAuth(m_szCmd, m_szName, m_szID);
            Log.d(TAG, "--> " + szMsg);
            Sending(szMsg.getBytes());
            ret = true;
        } catch (IOException e) {
            // Send Handler status
            callback_Status(false);
            e.printStackTrace();
            Log.e(TAG, "Not Connected");
            ret = false;
        }
        return ret;
    }

    private void Disconnecting(){
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
        m_bIsProcessing = false;

        Message msg = m_Handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(Hndl_MSGTYPE, MSGTYPE.RECV);
        b.putString(Hndl_MSG, message);
        msg.setData(b);
        m_Handler.sendMessage(msg);

        if (getTagValue(message, IClientProtocol.XML.INFO).equalsIgnoreCase(m_szCmd) &&
                getTagValue(message, IClientProtocol.XML.RESULT).equalsIgnoreCase(RESULT.SUCCESS))
            m_bIsReady = true;
    }

    //==============================================================================================

    private String ComposeAuth(String szCmd, String szName, String szID){
        String szInfo = String.format("<GOLDTEK><info><cmd>%s</cmd><name>%s</name><id>%s</id></info></GOLDTEK>",
                szCmd, szName, szID);
        return szInfo;
    }

    public static String getFileNameWithoutExtension(String file) {
        return ext.matcher(file).replaceAll("");
    }

    public static String readByteToString(byte[] szName){
        return new String(szName).replaceAll("[^\\p{Print}]","");
    }

    public static void writeIntToByte(byte[] data, int offset, int value) {
        data[offset] = (byte)((value >>> 24) & 0xFF);
        data[offset + 1] = (byte)((value >>> 16) & 0xFF);
        data[offset + 2] = (byte)((value >>> 8) & 0xFF);
        data[offset + 3] = (byte)((value >>> 0) & 0xFF);
    }

    public static int readByteToInt(byte[] data, int offset) {
        int ch1 = data[offset] & 0xff;
        int ch2 = data[offset + 1] & 0xff;
        int ch3 = data[offset + 2] & 0xff;
        int ch4 = data[offset + 3] & 0xff;
        return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
    }

    public static String getTagValue(String xml, String tagName){
        return xml.split("<"+tagName+">")[1].split("</"+tagName+">")[0];
    }

}
