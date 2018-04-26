package com.goldtek.demo.logistics.face;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.goldtek.demo.protocol.client.CClientConnection;
import com.goldtek.demo.protocol.client.DummyProtocol;
import com.goldtek.demo.protocol.client.GtClient;
import com.goldtek.demo.protocol.client.IClientProtocol;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.WeakReference;
import java.util.Vector;

public class RegisterActivity extends FaceRecogActivity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Register";
    public static final String     KEY_NAME            = "register_name";
    public static final String     KEY_LEVEL           = "register_level";

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int                    mSendFrame          = 0;
    private boolean                mRegisterDone       = false;

    private ProgressBar            mSendingProgressBar;
    private SpinKitView            mLearningProgress;
    private RestrictBox            mRestrictBox;

    private static class MainHandler extends Handler {
        private final WeakReference<RegisterActivity> mActivity;

        public MainHandler(RegisterActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RegisterActivity activity = mActivity.get();
            switch (msg.what) {
                case PROTOCOL_CREATE:
                    activity.CreateNew();
                    break;
                case PROTOCOL_RELEASE:
                    activity.Release();
                    break;
                case SET_SENDING_PROGRESS_VISIBLE:
                    activity.setProgress(true, false);
                    break;
                case SET_SENDING_PROGRESS_INVISIBLE:
                    activity.setProgress(false, false);
                    break;
                case SET_LEARNING_PROGRESS_VISIBLE:
                    activity.setProgress(false, true);
                    break;
                default:
                    String szMsgType = msg.getData().getString(IClientProtocol.Hndl_MSGTYPE, "");
                    String szMsg = msg.getData().getString(IClientProtocol.Hndl_MSG, "");
                    if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.RECV)) {
                        String szInfo = CClientConnection.getTagValue(szMsg, IClientProtocol.XML.INFO);
                        String szResult = CClientConnection.getTagValue(szMsg, IClientProtocol.XML.RESULT);

                        if (szInfo.equalsIgnoreCase(IClientProtocol.CMDTYPE.REG_DONE)) {
                            activity.mRegisterDone = true;
                            Intent returnIntent = new Intent();
                            if (szResult.equalsIgnoreCase(IClientProtocol.RESULT.SUCCESS))
                                activity.setResult(Activity.RESULT_OK, returnIntent);
                            else
                                activity.setResult(Activity.RESULT_CANCELED, returnIntent);
                            activity.finish();
                        } // Sending register data, it will count only success
                        else if (!szInfo.equalsIgnoreCase(IClientProtocol.CMDTYPE.REG) &&
                                szResult.equalsIgnoreCase(IClientProtocol.RESULT.SUCCESS)) {
                            activity.onRegister();
                        }
                    } else if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.ERR)) {
                        if (!activity.mRegisterDone) Toast.makeText(activity, szMsg, Toast.LENGTH_LONG).show();
                        activity.finish();
                    } else if (activity != null && szMsgType.equalsIgnoreCase(IClientProtocol.MSGTYPE.STATUS) &&
                            szMsg.equalsIgnoreCase(IClientProtocol.RESULT.FAIL)) {
                        if (!activity.mRegisterDone) Toast.makeText(activity, activity.getString(R.string.unreachable_recognition_server), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }
    }

    public RegisterActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
        setHandler(new MainHandler(this));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        mRegisterID = mRegisterName = getIntent().getStringExtra(KEY_NAME);

        if (com.goldtek.demo.logistics.face.Utils.isTargetDevice()) {
            mCameraFront = false;
            setContentView(R.layout.backcam_register);
        }
        else
            setContentView(R.layout.frontcam_register);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(org.opencv.samples.facedetect.R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mLearningProgress = findViewById(R.id.spin_kit);
        mSendingProgressBar = findViewById(R.id.progressBar);
        mSendingProgressBar.getIndeterminateDrawable().setColorFilter(getResources()
                .getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        mRestrictBox = findViewById(R.id.overlay_surface_view);
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Mat tempMat;
        boolean isExistFace = false;

        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? -1 : 1);
        tempMat.release();

        if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() && mSendFrame < REGISTER_LIMIT)
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

            if (mNativeDetector != null) {
                //TODO: get face for all solution
                mNativeDetector.detect(tempMat, faces);
                mGray.release();
                tempMat.release();

                Rect[] facesArray = faces.toArray();
                if (facesArray.length > 0) {
                    isExistFace = true;
                    tempMat = mRgba.clone();

                    for (Rect rect: facesArray) {
                        rect.x += (cy - dy);
                        rect.y += (cx - dx);
                        Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
                    }
                }

                if (isExistFace) {
                    Core.flip(tempMat, tempMat, 1);
                    Rect resizeRect = new Rect(facesArray[0].x - 100, facesArray[0].y - 50, facesArray[0].width + 150, facesArray[0].height + 150);
                    Mat cropped = new Mat(tempMat, resizeRect);
//                    AsyncSaveBmpTask task = new AsyncSaveBmpTask(cropped);
//                    task.execute(new Rect());

                    switch (mSolution) {
                        case PyTensor:
                        case LBPH:
                            Bitmap resized = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(cropped, resized);

                            if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() && !mProtocol.sendImage(String.format("login_%d", System.currentTimeMillis()), resized)) {
                                Release();
                                // TODO: error happened! tip some msg for user
                                finish();
                            }
                            resized.recycle();

                            break;
                        case PyTensorFV:
                            if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() && !mProtocol.sendVector(mTensor.getFeatureList(cropped), true)) {
                                Release();
                                // TODO: error happened!
                            }
                            break;
                        case LBPHIST:
                            Mat output = new Mat();
                            //TODO: try catch
                            mNativeDetector.getVecOfLBPHIST(cropped, output);
                            if (output != null && output.size().width == 4096 && output.size().height == 1) {
                                //Log.i(TAG, "mat: " + output.dump());
                                Vector<Float> features = new Vector<>();
                                for (int i = 0; i < 4096; i++) features.add((float) output.get(0, i)[0]);
                                if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() && !mProtocol.sendVector(features, false)) {
                                    //Log.i(TAG, "vec: " + features.toString());
                                    Release();
                                    // TODO: error happened!
                                }
                            } else {
                                Log.i(TAG, "no face");
                            }
                            break;
                    }


                    cropped.release();
                    tempMat.release();
                    mHandler.sendEmptyMessage(SET_SENDING_PROGRESS_VISIBLE);
                }

            }

        }

        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? 1 : -1);
        tempMat.release();

        return mRgba;
    }


    private void setProgress(boolean sending, boolean learning) {
        mSendingProgressBar.setVisibility(sending ? View.VISIBLE : View.INVISIBLE);
        mLearningProgress.setVisibility(learning ? View.VISIBLE : View.INVISIBLE);
    }

    private void onRegister() {

        if (mSendFrame >= REGISTER_LIMIT || mProtocol == null) {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        } else {
            if (mCacheBitmap != null && !mCacheBitmap.isRecycled()) ((ImageView)findViewById(R.id.registerPhoto)).setImageBitmap(mCacheBitmap);
            mSendFrame++;

            switch (mSendFrame % 11) {
                case 1:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.one);
                    break;
                case 2:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.two);
                    break;
                case 3:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.three);
                    break;
                case 4:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.four);
                    break;
                case 5:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.five);
                    break;
                case 6:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.six);
                    break;
                case 7:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.seven);
                    break;
                case 8:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.eight);
                    break;
                case 9:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.nine);
                    break;
                case 10:
                    ((ImageView)findViewById(R.id.registerCount)).setImageResource(R.drawable.ten);
                    break;
            }

            if (mSendFrame < REGISTER_LIMIT) {
                Log.i(TAG, "send done " + mSendFrame);
                mHandler.sendEmptyMessage(SET_SENDING_PROGRESS_INVISIBLE);
            }
            else {
                Log.i(TAG, "start learn " + mSendFrame);
                mHandler.removeMessages(SET_SENDING_PROGRESS_VISIBLE);
                mHandler.removeMessages(SET_SENDING_PROGRESS_INVISIBLE);
                mHandler.sendEmptyMessage(SET_LEARNING_PROGRESS_VISIBLE);
            }
        }

    }

    public void CreateNew() {
        if(mProtocol == null) {
            if (FLAG_DEBUG) mProtocol = new DummyProtocol(mHandler, IClientProtocol.CMDTYPE.REG);
            else mProtocol = new GtClient(
                    mHandler, -1, mServerAddr, mSolution,
                    IClientProtocol.CMDTYPE.REG, mRegisterName, mRegisterID, REGISTER_LIMIT);
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
