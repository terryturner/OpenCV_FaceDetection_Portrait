package com.goldtek.demo.protocol.client;

import android.graphics.Bitmap;
import android.util.Log;

import com.goldtek.demo.logistics.face.Utils;

import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Terry on 2018/4/24 0024.
 */
public class DataPacket {
    protected final static String TAG = "DataPacket";
    protected final static int HEADERSIZE = 512;

    protected byte[] m_Header = new byte[HEADERSIZE];
    protected ByteArrayOutputStream m_baos = null;
    protected byte[] m_data = null;

    public DataPacket() {
        m_baos = new ByteArrayOutputStream();
    }

    public DataPacket(String szID, String szName, Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0, baos);
        ComposePacket(szID, szName, baos.toByteArray());
    }

    public DataPacket(String szID, String szName, List<Float> features, boolean big_endian) {
        byte[] bValues = Utils.transFloatList2ByteArray(features, big_endian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        ComposePacket(szID, szName, bValues);
    }

    public DataPacket(String szID, String szName, Mat embedds) {

        //float dArr[] =  new float[embedds.cols()];
        List<Float> dArr = new ArrayList<>();
        for (int i = 0; i < embedds.cols(); i++) {
            dArr.add((float)embedds.get(0, i)[0]) ;
        }

        byte[] byteArray = Utils.transFloatList2ByteArray(dArr, ByteOrder.BIG_ENDIAN);
        int sizeEmbeddsImage = byteArray.length;

        String szHEADER = ComposeHeader(szName, szID, sizeEmbeddsImage);

        System.arraycopy(szHEADER.getBytes(), 0, m_Header, 0, szHEADER.getBytes().length);
        Log.d(TAG, "EmbeddsImageSize: " + String.valueOf(sizeEmbeddsImage) + ", HeaderSize: " + szHEADER.getBytes().length);

        // [DATA] Protocol [HEADER][IMAGE...]
        int TOTAL = HEADERSIZE + sizeEmbeddsImage;
        m_data = new byte[TOTAL];
        System.arraycopy(m_Header, 0, m_data, 0, HEADERSIZE);
        System.arraycopy(byteArray, 0, m_data, HEADERSIZE, sizeEmbeddsImage);
    }

    public byte[] getByteArray(){
        return m_data;
    }

    protected String ComposeHeader(String szName, String szID, int nImageSize){
        String szMSG = String.format(IClientProtocol.PROTOCOL_SENDHEADER,
                String.valueOf(nImageSize),  szID, szName);
        return szMSG;
    }

    protected void ComposePacket(String szID, String szName, byte[] byteArray) {
        int sizeFeature = byteArray.length;

        String szHEADER = ComposeHeader(szName, szID, sizeFeature);
        Log.i(TAG, szHEADER);
        System.arraycopy(szHEADER.getBytes(), 0, m_Header, 0, szHEADER.getBytes().length);
        Log.d(TAG, "Data Size: " + String.valueOf(sizeFeature) + " HeaderSize: " + szHEADER.getBytes().length);

        // [DATA] GtProtocol [HEADER][IMAGE...]
        int TOTAL = HEADERSIZE + sizeFeature;
        m_data = new byte[TOTAL];
        System.arraycopy(m_Header, 0, m_data, 0, HEADERSIZE);
        System.arraycopy(byteArray, 0, m_data, HEADERSIZE, sizeFeature);
    }


}
