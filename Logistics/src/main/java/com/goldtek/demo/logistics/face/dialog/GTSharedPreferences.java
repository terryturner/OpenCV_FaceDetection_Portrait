package com.goldtek.demo.logistics.face.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.goldtek.demo.logistics.face.R;
import com.goldtek.demo.protocol.client.GTFaceRecogSol;

/**
 * Created by Terry on 2018/4/26 0026.
 */
public class GTSharedPreferences {
    private final static String KEY_SERVER_RECOGNIZE = "recognize-server-address";
    private final static String KEY_SERVER_PALM = "palm-server-address";
    private final static String KEY_PLAY_MEDIA = "demo-play-media";
    private final static String KEY_CASCADE = "cascade_file";
    private final static String KEY_FRSOLUTION = "fr_solution";

    private final Context mContext;
    private final SharedPreferences sharedPrefs;

    public GTSharedPreferences(Context context) {
        mContext = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getRecognizeServerAddr() {
        return sharedPrefs.getString(KEY_SERVER_RECOGNIZE, "127.0.0.1");
    }

    public String getPalmServerAddr() {
        return sharedPrefs.getString(KEY_SERVER_PALM, "127.0.0.1");
    }

    public boolean getPlayMainMedia() {
        return sharedPrefs.getBoolean(KEY_PLAY_MEDIA, true);
    }

    public int getCascadeUIResource() {
        return sharedPrefs.getInt(KEY_CASCADE, R.id.radio_cascade_win);
    }

    public int getCascadeResource() {
        int resourceID = sharedPrefs.getInt(KEY_CASCADE, R.id.radio_cascade_win);
        String filename = "haarcascade_frontalface_alt2.xml";
        switch (resourceID) {
            case R.id.radio_cascade_sample:
                resourceID = org.opencv.samples.facedetect.R.raw.lbpcascade_frontalface;
                filename = "lbpcascade_frontalface.xml";
                break;
            case R.id.radio_cascade_win:
                resourceID = org.opencv.samples.facedetect.R.raw.haarcascade_frontalface_alt2;
                filename = "haarcascade_frontalface_alt2.xml";
                break;
            case R.id.radio_cascade_rockchip:
                resourceID = org.opencv.samples.facedetect.R.raw.rockchip_cascade_frontalface;
                filename = "rockchip_cascade_frontalface.xml";
                break;
        }
        return resourceID;
    }

    public int getRecognizeProtocolID() {
        return sharedPrefs.getInt(KEY_FRSOLUTION, R.id.radio_pytensor_img);
    }

    public GTFaceRecogSol getGTFaceRecogSolution() {
        int protocolID = getRecognizeProtocolID();
        switch (protocolID) {
            case R.id.radio_pytensor_img:
                return GTFaceRecogSol.PyTensor;
            case R.id.radio_pytensor_fv:
                return GTFaceRecogSol.PyTensorFV;
            case R.id.radio_lbph:
                return GTFaceRecogSol.LBPH;
            case R.id.radio_lbphist:
                return GTFaceRecogSol.LBPHIST;
        }
        return GTFaceRecogSol.PyTensor;
    }

    public boolean setOfServerDiaglog(String recogAddr, String palmAddr, boolean play, int cascade, int protocol) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_SERVER_RECOGNIZE, recogAddr);
        editor.putString(KEY_SERVER_PALM, palmAddr);
        editor.putBoolean(KEY_PLAY_MEDIA, play);
        editor.putInt(KEY_CASCADE, cascade);
        editor.putInt(KEY_FRSOLUTION, protocol);

        return editor.commit();
    }
}
