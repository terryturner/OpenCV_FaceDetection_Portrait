package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.goldtek.demo.logistics.face.dialog.GTSharedPreferences;
import com.goldtek.demo.protocol.client.GTFaceRecogSol;
import com.goldtek.demo.protocol.client.IClientProtocol;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.DetectionBasedTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Terry on 2018/4/26 0026.
 */
public class FaceRecogActivity extends Activity {
    private static final String    TAG                 = "FaceRecogActivity";

    protected static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    protected static final boolean   FLAG_DEBUG          = false;

    protected static final int       PROTOCOL_CREATE     = 0x200;
    protected static final int       PROTOCOL_RELEASE     = 0x201;

    protected static final int       SET_PROGRESS_VISIBLE     = 0x110;
    protected static final int       SET_PROGRESS_INVISIBLE   = 0x111;

    protected static final int       SET_SENDING_PROGRESS_VISIBLE = 0x110;
    protected static final int       SET_SENDING_PROGRESS_INVISIBLE = 0x111;
    protected static final int       SET_LEARNING_PROGRESS_VISIBLE = 0x112;

    protected static final int        JAVA_DETECTOR       = 0;
    protected static final int        NATIVE_DETECTOR     = 1;
    protected static final int        REGISTER_LIMIT      = 10;

    protected Mat mRgba;
    protected Mat                    mGray;
    protected File mCascadeFile;
    protected CascadeClassifier mJavaDetector;
    protected DetectionBasedTracker mNativeDetector;
    protected TensorFlow             mTensor = null;
    protected GTFaceRecogSol mSolution = GTFaceRecogSol.PyTensor;

    protected int                    mDetectorType       = NATIVE_DETECTOR;
    protected String[]               mDetectorName;
    protected boolean                mCameraFront        = true;

    protected CameraBridgeViewBase mOpenCvCameraView;

    protected GTSharedPreferences mPreferences;
    protected Handler mHandler;
    protected Bitmap mCacheBitmap;
    protected IClientProtocol mProtocol;

    protected String                 mServerAddr         = null;
    protected String                 mRegisterName       = null;
    protected String                 mRegisterID         = null;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    mServerAddr = mPreferences.getRecognizeServerAddr();
                    mSolution = mPreferences.getGTFaceRecogSolution();

                    int resourceID = mPreferences.getCascadeResource();
                    String filename = "frontalface.xml";
                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(resourceID);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, filename);
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();

                    mHandler.sendEmptyMessage(PROTOCOL_RELEASE);
                    mHandler.sendEmptyMessage(PROTOCOL_CREATE);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FaceRecogActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPreferences = new GTSharedPreferences(this);
        mTensor = new TensorFlow(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mHandler.sendEmptyMessage(PROTOCOL_RELEASE);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        if (width > 0 && height > 0) mCacheBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }

    protected void setHandler(Handler h) {
        mHandler = h;
    }
}
