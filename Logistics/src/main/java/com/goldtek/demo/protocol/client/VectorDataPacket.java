package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;
import android.util.Log;

import com.goldtek.demo.logistics.face.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by darwinhu on 2017/12/20.
 */

public class VectorDataPacket {

    private String TAG = "DataPacket";
    //INFO
    public final static String INFO_START = "<Info>";
    public final static String INFO_END = "</Info>";

    //HEADER
    public final static String HEADER_START = "<GOLDTEK>";
    public final static String HEADER_END = "</GOLDTEK>";

    // <GOLDTEK><id></id><name></name><size></size></GOLDTEK>
    public final static String TAG_ID = "id";
    public final static String TAG_NAME = "name";
    public final static String TAG_SIZE = "size";

    public String _Name = "";
    public String _ID = "";
    public String _Size = "";

    public final static int HEADERSIZE = 512;

    public byte[] m_Header = new byte[HEADERSIZE];
    public ByteArrayOutputStream m_baos = null;
    private byte[] m_data = null;

    public VectorDataPacket(){
        m_baos = new ByteArrayOutputStream();
    }

    public VectorDataPacket(String szName, String szID, Vector<Float> features){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(Float value : features) {
            byte[] bValue = Utils.float2ByteArray(value);
            try {
                baos.write(bValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] byteArray = baos.toByteArray();
        int sizeFeature = byteArray.length;

        String szHEADER = ComposeHeader(szID, szID, sizeFeature);
        Log.i(TAG, szHEADER);
        System.arraycopy(szHEADER.getBytes(), 0, m_Header, 0, szHEADER.getBytes().length);
        Log.d(TAG, "VectorSize: " + String.valueOf(sizeFeature) + " HeaderSize: " + szHEADER.getBytes().length);

        // [DATA] GtProtocol [HEADER][IMAGE...]
        int TOTAL = HEADERSIZE + sizeFeature;
        m_data = new byte[TOTAL];
        System.arraycopy(m_Header, 0, m_data, 0, HEADERSIZE);
        System.arraycopy(byteArray, 0, m_data, HEADERSIZE, sizeFeature);
    }

    public static String ComposeHeader(String szName, String szID, int nImageSize){
        String szMSG = String.format("<GOLDTEK><size>%s</size><id>%s</id><name>%s</name></GOLDTEK>",
                String.valueOf(nImageSize),  szID, szName);
        return szMSG;
    }

    public byte[] getByteArray(){
        return m_data;
    }

}
