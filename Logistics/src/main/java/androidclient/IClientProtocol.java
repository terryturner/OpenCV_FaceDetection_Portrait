package androidclient;

import android.graphics.Bitmap;

/**
 * Created by Terry on 2018/1/3 0003.
 */

public interface IClientProtocol {
    void start();
    void OnStop();
    boolean sendImage(String szName, Bitmap bmp);
    boolean isReady();
    boolean isProcessing();

}
