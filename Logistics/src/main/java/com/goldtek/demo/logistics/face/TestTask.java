package com.goldtek.demo.logistics.face;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.opencv.android.*;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by Terry on 2018/1/8 0008.
 */

public class TestTask extends AsyncTask<Rect, Void, Void> {
    private final Mat m_Mat;

    public TestTask(Mat mat) {
        m_Mat = mat;
    }

//    @Override
//    protected Void doInBackground(Bitmap... bitmaps) {
//        if (bitmaps[0] != null) Utils.saveTempBitmap(bitmaps[0]);
//        return null;
//    }

    @Override
    protected Void doInBackground(Rect... rects) {
        for (Rect rect : rects) {
            Mat cropped = new Mat(m_Mat, rect);
            Bitmap bmp = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(cropped, bmp);
            Utils.saveTempBitmap(bmp);
            bmp.recycle();
            cropped.release();
        }
        return null;
    }
}
