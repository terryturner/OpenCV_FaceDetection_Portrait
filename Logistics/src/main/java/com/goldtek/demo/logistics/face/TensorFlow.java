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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/***************************************************************************************
 *    Title: TensorFlowAndroidDemo
 *    Author: miyosuda
 *    Date: 23.04.2016
 *    Code version: -
 *    Availability: https://github.com
 *
 ***************************************************************************************/

public class TensorFlow {
    public String TAG = "TensorFlow";

    private String inputLayer = "input";
    private String outputLayer = "embeddings";
    private int inputSize = 160;
    private int channels = 3;
    private int imageMean = 128;
    private int imageStd = 128;
    private int outputSize = 128;

    private TensorFlowInferenceInterface inferenceInterface;

    private boolean logStats = false;

    // Only return this many results with at least this confidence.
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.2f;

    public TensorFlow(Context context) {

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

        // Use internal assets file as fallback, if no model file is provided
//        File file = new File(dataPath + modelFile);
//        if(file.exists()){
//            inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), dataPath + modelFile);
//        } else {
//            inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelFile);
//        }
    }

    public TensorFlow(Context context, int inputSize, int outputSize, String inputLayer, String outputLayer, String modelFile){
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.inputLayer = inputLayer;
        this.outputLayer = outputLayer;

        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), modelFile);
    }

//    @Override
//    public boolean train() {
//        return rec.train();
//    }
//
//    @Override
//    public String recognize(Mat img, String expectedLabel) {
//        return rec.recognize(getFeatureVector(img), expectedLabel);
//    }
//
//    @Override
//    public void saveToFile() {
//
//    }
//
//    @Override
//    public void loadFromFile() {
//
//    }
//
//    @Override
//    public void saveTestData() {
//        rec.saveTestData();
//    }
//
//    @Override
//    public void addImage(Mat img, String label, boolean featuresAlreadyExtracted) {
//        if (featuresAlreadyExtracted){
//            rec.addImage(img, label, true);
//        } else {
//            rec.addImage(getFeatureVector(img), label, true);
//        }
//    }

    public Vector<Float> getFeatureVector(Mat img) {
        Vector<Float> fVector = new Vector<>();
        List<Float> fList = getFeatureList(img);
        for (Float v : fList) {
            fVector.add(v);
        }

        return fVector;
    }

    public Mat getFeatureMat(Mat img){
        List<Float> fList = getFeatureList(img);

        return Converters.vector_float_to_Mat(fList);
    }

    public List<Float> getFeatureList(Mat img) {
        Imgproc.resize(img, img, new Size(inputSize, inputSize));
        // Copy the input data into TensorFlow.
        inferenceInterface.feed(inputLayer, getPixels(img), 1, inputSize, inputSize, channels);
        // Run the inference call.
        inferenceInterface.run(new String[]{outputLayer}, logStats);
        float[] outputs = new float[outputSize];
        // Copy the output Tensor back into the output array.
        inferenceInterface.fetch(outputLayer, outputs);

        List<Float> fVector = new ArrayList<>();
        for(float o : outputs){
            fVector.add(o);
        }
        return fVector;
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
