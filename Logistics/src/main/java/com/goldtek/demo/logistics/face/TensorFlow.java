/* Copyright 2016 Michael Sladoje and Mike Schälchli. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.goldtek.demo.logistics.face;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/***************************************************************************************
 *    Title: TensorFlowAndroidDemo
 *    Author: miyosuda
 *    Date: 23.04.2016
 *    Code version: -
 *    Availability: https://github.com
 *
 ***************************************************************************************/

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TensorFlow {
    public final static String TAG = "TensorFlow";

    private String inputLayer = "input";
    private String outputLayer = "embeddings";
    private int inputSize = 160;
    private int channels = 3;
    private int imageMean = 128;
    private int imageStd = 128;
    private int outputSize = 128;

    private TensorFlowInferenceInterface inferenceInterface;
    private Processor mProcessor = new Processor();
    private boolean logStats = false;
    private Handler mHandler = null;

    // Only return this many results with at least this confidence.
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.2f;

    public TensorFlow(Context context, Handler handler) {

        String modelFile = "optimized_facenet.pb";
        AssetManager assetMgr = context.getAssets();
        try {
            String [] filelist = assetMgr.list("");
            for(String s : filelist){
                Log.i(TAG, s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelFile);
        mHandler = handler;
        mProcessor.start();
    }

    public TensorFlow(Context context, int inputSize, int outputSize, String inputLayer, String outputLayer, String modelFile){
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.inputLayer = inputLayer;
        this.outputLayer = outputLayer;

        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelFile);
        mProcessor.start();
    }

    public void stop() {
        mProcessor.onStop();
    }

    public void getFeatureMat(Mat img){
        mProcessor.getFeatures(img, 2);
    }

    public void getFeatureList(Mat img) {
        mProcessor.getFeatures(img, 1);
    }


    private class Processor extends Thread implements Runnable {
        private boolean bInterrupt = false;
        private Object mLock = new Object();
        private Boolean mProcessing = false;
        private Mat mImage = null;
        private int mFeatureType = -1;

        @Override
        public synchronized void run() {
            while(!bInterrupt) {
                synchronized (mProcessing) {
                    try {
                        mProcessing.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mProcessing = true;
                }

                if (!bInterrupt && mImage != null) {
                    Mat image = mImage;

                    try {
                        Imgproc.resize(image, image, new Size(inputSize, inputSize));
                        // Copy the input data into TensorFlow.
                        inferenceInterface.feed(inputLayer, getPixels(image), 1, inputSize, inputSize, channels);
                        // Run the inference call.
                        inferenceInterface.run(new String[]{outputLayer}, logStats);
                        float[] outputs = new float[outputSize];
                        // Copy the output Tensor back into the output array.
                        inferenceInterface.fetch(outputLayer, outputs);

                        List<Float> fVector = new ArrayList<>();
                        for(float o : outputs){
                            fVector.add(o);
                        }

                        if (!bInterrupt) {
                            Message msg = null;
                            if (mFeatureType == 1) {
                                msg = Message.obtain(mHandler, GTMessage.MSG_PROCESSED_TF_FV, fVector);
                            } else if (mFeatureType == 2) {
                                msg = Message.obtain(mHandler, GTMessage.MSG_PROCESSED_TF_FV, Converters.vector_float_to_Mat(fVector));
                            }
                            if (msg != null) {
                                mHandler.sendMessage(msg);
                            }
                        }
                    } catch (CvException e) {
                        e.printStackTrace();
                    }

                }

                synchronized (mProcessing) {
                    mProcessing = false;
                }
            }
        }

        public void onStop() {
            this.interrupt();
            bInterrupt = true;
        }

        public void getFeatures(Mat image, int type) {
            synchronized (mProcessing) {
                if (!mProcessing) {
                    mImage = image;
                    mFeatureType = type;

                    mProcessing.notify();
                }
            }
        }

        private float[] getPixels(Mat img){
            // Preprocess the image data from 0-255 int to normalized float based
            // on the provided parameters.
            Bitmap bmp = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
            int[] intValues = new int[inputSize * inputSize];
            bmp.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);

            float[] floatValues = new float[inputSize * inputSize * channels];
            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3 + 0] = (((float)((val >> 16) & 0xFF)) - imageMean) / imageStd;
                floatValues[i * 3 + 1] = (((float)((val >> 8) & 0xFF)) - imageMean) / imageStd;
                floatValues[i * 3 + 2] = (((float)(val & 0xFF)) - imageMean) / imageStd;
            }

            return floatValues;
        }
    }
}
