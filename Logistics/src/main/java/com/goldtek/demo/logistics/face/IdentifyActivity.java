package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.DetectionBasedTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import androidclient.CClientConnection;
import androidclient.IClientProtocol;

public class IdentifyActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Identify";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final String     KEY_NAME            = "identify_name";
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;
    private boolean                mCameraFront        = true;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int                    mIdentifiedFrame    = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private MainHandler            mHandler            = new MainHandler(this);
    private Bitmap                 mCacheBitmap;
    private IClientProtocol        mProtocol;

    private static class MainHandler extends Handler {
        private final WeakReference<IdentifyActivity> mActivity;

        public MainHandler(IdentifyActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            IdentifyActivity activity = mActivity.get();
            String szMsgType = msg.getData().getString(CClientConnection.Hndl_MSGTYPE, "");
            String szMsg = msg.getData().getString(CClientConnection.Hndl_MSG, "");
            if (activity != null) {
                activity.onIdentify(szMsg);
            }
        }
    }

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(org.opencv.samples.facedetect.R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public IdentifyActivity() {
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

        Log.i(TAG, getDeviceName());
        if (getDeviceName().contains("fc11501")) {
            mCameraFront = false;
            setContentView(R.layout.backcam_identify);
        }
        else { // TODO: finish identify process due to design only on fc11501
            setContentView(R.layout.frontcam_identify);
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(org.opencv.samples.facedetect.R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        //if (mProtocol != null) mProtocol.stop();
        Release();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        Release();
        CreateNew();
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mCacheBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Mat tempMat;
        boolean isExistFace = false;

        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? -1 : 1);
        tempMat.release();

        //if (mProtocol != null && !mProtocol.isProcessing())
        if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing())
        {
            mGray = inputFrame.gray();

            if (mAbsoluteFaceSize == 0) {
                int height = mGray.rows();
                if (Math.round(height * mRelativeFaceSize) > 0) {
                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                }
                mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
            }

            MatOfRect faces = new MatOfRect();

            tempMat = mGray.t();
            Core.flip(tempMat, mGray, mCameraFront ? -1 : 1);
            tempMat.release();

            if (mDetectorType == JAVA_DETECTOR) {
                if (mJavaDetector != null) mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            } else if (mDetectorType == NATIVE_DETECTOR) {
                if (mNativeDetector != null) mNativeDetector.detect(mGray, faces);
            } else {
                Log.e(TAG, "Detection method is not selected!");
            }
            mGray.release();

            Rect[] facesArray = faces.toArray();
            for (Rect rect: facesArray)
                Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);

            if (facesArray.length > 0) isExistFace = true;

            //if (mProtocol != null && !mProtocol.isProcessing() && isExistFace)
            if (isExistFace)
            {
                Utils.matToBitmap(mRgba, mCacheBitmap);
                if(!mProtocol.sendImage(String.format("%s_%d", RegisterID, System.currentTimeMillis()), mCacheBitmap)) {
                    Release();
                    // TODO: error happened!
                    //CreateNew();
                }
            }
        }
        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? 1 : -1);
        tempMat.release();



        return mRgba;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }

    private void onIdentify(String name) {
        Log.i(TAG, "onIdentify" + mIdentifiedFrame + " " + name);
        if (mIdentifiedFrame >= 10) {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        } else if (mProtocol == null) {
            finish();
        } else if (name.equalsIgnoreCase("UNKNOWN")) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(KEY_NAME, name);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
        mIdentifiedFrame++;
    }

    private String ServerIP     = "192.168.43.32";
    private String RegisterName = "";
    private String RegisterID   = "";

    public void CreateNew() {
        if(mProtocol == null) {
            mProtocol = new CClientConnection(mHandler, CClientConnection.PORT,
                    ServerIP,
                    CClientConnection.CMDTYPE.LOGIN,
                    RegisterName,
                    RegisterID);
            mProtocol.start();
        }
    }

    public void Release(){
        if (mProtocol != null) {
            mProtocol.OnStop();
            mProtocol = null;
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }
}
