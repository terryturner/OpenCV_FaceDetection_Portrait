package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import com.github.ybq.android.spinkit.SpinKitView;
import com.goldtek.demo.protocol.client.CClientConnection;
import com.goldtek.demo.protocol.client.DummyProtocol;
import com.goldtek.demo.protocol.client.GtClient;
import com.goldtek.demo.protocol.client.IClientProtocol;

import static com.goldtek.demo.logistics.face.dialog.ServerDialogFragment.KEY_SERVER_RECOGNIZE;

public class IdentifyActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Identify";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final boolean   FLAG_DEBUG          = false;
    private static final int       SET_PROGRESS_VISIBLE     = 0x110;
    private static final int       SET_PROGRESS_INVISIBLE   = 0x111;
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
    private boolean                mIdentifiedDone     = false;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private ProgressBar            mProgress;
    private SpinKitView            mSpinKit;
    private RestrictBox            mRestrictBox;
    private TextView               mIdentifiedFrameText;

    private MainHandler            mHandler            = new MainHandler(this);
    private Bitmap                 mCacheBitmap;
    private IClientProtocol        mProtocol;

    private String                 mServerAddr         = null;


    private static class MainHandler extends Handler {
        private final WeakReference<IdentifyActivity> mActivity;

        public MainHandler(IdentifyActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            IdentifyActivity activity = mActivity.get();
            switch (msg.what) {
                case SET_PROGRESS_VISIBLE:
                    activity.setProgress(true);
                    break;
                case SET_PROGRESS_INVISIBLE:
                    activity.setProgress(false);
                    break;
                default:
                    String szMsgType = msg.getData().getString(IClientProtocol.Hndl_MSGTYPE, "");
                    String szMsg = msg.getData().getString(IClientProtocol.Hndl_MSG, "");
                    if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.RECV)) {
                        String szInfo = CClientConnection.getTagValue(szMsg, IClientProtocol.XML.INFO);
                        String szResult = CClientConnection.getTagValue(szMsg, IClientProtocol.XML.RESULT);

                        if (szInfo.equalsIgnoreCase(IClientProtocol.CMDTYPE.LOGIN_DONE)) {
                            activity.onIdentify(true, szResult);
                        } else
                            activity.onIdentify(false, szResult);
                    } else if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.ERR)) {
                        if (!activity.mIdentifiedDone) Toast.makeText(activity, szMsg, Toast.LENGTH_LONG).show();
                    } else if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.STATUS) &&
                            szMsg.equalsIgnoreCase(IClientProtocol.RESULT.FAIL)) {
                        if (!activity.mIdentifiedDone) Toast.makeText(activity, activity.getString(R.string.unreachable_recognition_server), Toast.LENGTH_SHORT).show();
                    }
                    break;
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

        if (com.goldtek.demo.logistics.face.Utils.isTargetDevice()) {
            mCameraFront = false;
            setContentView(R.layout.backcam_identify);
        }
        else { // TODO: finish identify process due to design only on fc11501
            setContentView(R.layout.frontcam_identify);
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(org.opencv.samples.facedetect.R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mSpinKit = findViewById(R.id.spin_kit);
        mProgress = findViewById(R.id.progressBar);
        mProgress.getIndeterminateDrawable().setColorFilter(getResources()
                .getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        mRestrictBox = findViewById(R.id.overlay_surface_view);
        mIdentifiedFrameText = findViewById(R.id.frame_count);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mServerAddr = sharedPrefs.getString(KEY_SERVER_RECOGNIZE, "127.0.0.1");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Release();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
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

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Mat tempMat;
        boolean isExistFace = false;

        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? -1 : 1);
        tempMat.release();

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

            int cx = (int) (mGray.cols() * mRestrictBox.getCenterRatioX());
            int cy = (int) (mGray.rows() * mRestrictBox.getCenterRatioY());
            int dx = (int) (mGray.cols() * mRestrictBox.getDistanceRatioX());
            int dy = (int) (mGray.rows() * mRestrictBox.getDistanceRatioY());

            tempMat = mGray.submat(cy - dy, cy + dy, cx - dx, cx + dx);
            if (mNativeDetector != null) mNativeDetector.detect(tempMat, faces);
            mGray.release();
            tempMat.release();

            Rect[] facesArray = faces.toArray();
            if (facesArray.length > 0) {
                isExistFace = true;
                tempMat = mRgba.clone();

//                TestTask task = new TestTask(mRgba);
//                task.execute(facesArray);
            }
            for (Rect rect: facesArray) {
                rect.x += (cy - dy);
                rect.y += (cx - dx);
                Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
            }


            if (isExistFace)
            {
                Core.flip(tempMat, tempMat, 1);

                //Utils.matToBitmap(tempMat, mCacheBitmap);
                //Bitmap resized = Bitmap.createScaledBitmap(mCacheBitmap, 480, 640, true);

                Rect resizeRect = new Rect(facesArray[0].x - 50, facesArray[0].y - 50, facesArray[0].width + 100, facesArray[0].height + 100);
                Mat cropped = new Mat(tempMat, resizeRect);
                Bitmap resized = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, resized);
                if(mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() &&
                        !mProtocol.sendImage(String.format("login_%d", System.currentTimeMillis()), resized)) {
                    Release();
                    // TODO: error happened!
                    //CreateNew();
                }
                resized.recycle();
                cropped.release();
                tempMat.release();
                mHandler.sendEmptyMessage(SET_PROGRESS_VISIBLE);
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

    private void setProgress(boolean visible) {
        mProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mSpinKit.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void onIdentify(boolean finish, String name) {
        Log.i(TAG, "onIdentify " + mIdentifiedFrame + ": " + name);

        if (mIdentifiedFrame > 10 || mProtocol == null) {
            mIdentifiedDone = true;
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        } else if (finish && !name.equalsIgnoreCase(IClientProtocol.RESULT.UNKNOWN)) {
            mIdentifiedDone = true;
            Intent returnIntent = new Intent();
            returnIntent.putExtra(KEY_NAME, name);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
        mIdentifiedFrame++;
        mIdentifiedFrameText.setText(String.valueOf(mIdentifiedFrame));
        mHandler.sendEmptyMessage(SET_PROGRESS_INVISIBLE);
    }

    public void CreateNew() {

        if(mProtocol == null) {
            if (FLAG_DEBUG) mProtocol = new DummyProtocol(mHandler, IClientProtocol.CMDTYPE.LOGIN);
            else mProtocol = new GtClient
                    (mHandler, -1, mServerAddr,
                    IClientProtocol.CMDTYPE.LOGIN,
                    "", "");
            mProtocol.start();
        }
    }

    public void Release(){
        if (mProtocol != null) {
            mProtocol.onStop();
            mProtocol = null;
        }
    }

}
