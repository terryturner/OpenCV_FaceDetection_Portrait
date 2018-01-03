package androidclient;

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
    private static final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");
    private static final int BUFFSIZE = 512;
    public static final int PORT = 6666;

    private int m_nPort;
    private String m_szSvrIP;

    private Socket m_socket = null;
    private String m_szCmd;
    private String m_szName;
    private String m_szID;

    private Handler m_Handler;


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
    public static final String Hndl_MSG             = "MSG";
    public static final String Hndl_MSGTYPE         = "MSGTYPE";
    public static class MSGTYPE {
        public static final String STATUS = "STATUS";
        public static final String RECV   = "RECV";
        public static final String OTHER  = "OTHER";
    }

    private boolean m_bRunning = false;
    private boolean m_bSending = false;
    private Listener mListener = new Listener();
    private Sender   mSender   = new Sender();

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
        this.m_nPort    = nPort;
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
        private boolean m_bInterrupt = false;
        private boolean m_bIsConnect = false;

        @Override
        public void run() {
            super.run();

            while(!m_bInterrupt){
                m_bIsConnect = Connecting();
                if (m_bIsConnect)
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

            if(m_bIsConnect) {
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

        public void OnStop() {
            this.interrupt();
            m_bInterrupt = true;
        }

        public boolean isReady() {
            return m_bIsConnect;
        }
    }


    private class Sender extends Thread implements Runnable {
        private boolean m_bInterrupt = false;
        private byte[] m_Packets = null;

        @Override
        public void run() {
            super.run();

            while(!m_bInterrupt){
                if (m_bRunning && m_Packets != null) {
                    try {
                        DataOutputStream output = new DataOutputStream(m_socket.getOutputStream());
                        if(m_socket.isConnected()){
                            output.write(m_Packets);
                            Log.d(TAG, "Sending Packet ... " + String.valueOf(m_Packets.length));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                    m_Packets = null;
                }
            }
        }

        public void OnStop() {
            this.interrupt();
            m_bInterrupt = true;
        }

        public boolean Sending(byte[] packet) {
            if (m_bSending) return false;
            m_bSending = true;
            m_Packets = packet;
            return true;
        }
    }

    public void OnStop() {
        Log.d(TAG, "OnStop");
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
        return mListener.isReady();
    }

    public boolean isProcessing() {
        return m_bSending;
    }

    private synchronized boolean Connecting(){
        boolean ret = false;
        try {
            m_socket = new Socket(m_szSvrIP, m_nPort);
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
    }

    //==============================================================================================

    private String ComposeAuth(String szCmd, String szName, String szID){
        String szInfo = String.format("<Info><cmd>%s</cmd><name>%s</name><id>%s</id></Info>",
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
