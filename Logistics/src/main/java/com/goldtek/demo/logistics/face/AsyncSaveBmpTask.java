package com.goldtek.demo.logistics.face;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.opencv.android.*;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Created by Terry on 2018/1/8 0008.
 */

public class AsyncSaveBmpTask extends AsyncTask<Rect, Void, Void> {
    private final Mat m_Mat;

    public AsyncSaveBmpTask(Mat mat) {
        m_Mat = mat;
    }

//    @Override
//    protected Void doInBackground(Bitmap... bitmaps) {
//        if (bitmaps[0] != null) Utils.saveTempBitmap(bitmaps[0]);
//        return null;
//    }

    @Override
    protected Void doInBackground(Rect... rects) {
        if (rects != null) {
            if (rects.length == 1 && rects[0].x == 0 && rects[0].y == 0 && rects[0].width == 0 && rects[0].height == 0) {
                write(m_Mat);
            } else {
                for (Rect rect : rects) {
                    Mat cropped = new Mat(m_Mat, rect);
                    write(cropped);
                }
            }
        } else write(m_Mat);
        return null;
    }

    private void write(Mat mat) {
        try {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(mat, bmp);
            Utils.saveTempBitmap(bmp);
            bmp.recycle();
            mat.release();
        } catch (CvException e) {}
    }
}
