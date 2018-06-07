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
import android.widget.ProgressBar;
import android.widget.TextView;
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
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

public class IdentifyActivity extends FaceRecogActivity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Identify";
    public static final String     KEY_NAME            = "identify_name";

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int                    mIdentifiedFrame    = 0;
    private boolean                mIdentifiedDone     = false;

    private ProgressBar            mProgress;
    private SpinKitView            mSpinKit;
    private RestrictBox            mRestrictBox;
    private TextView               mIdentifiedFrameText;

    private static class MainHandler extends Handler {
        private final WeakReference<IdentifyActivity> mActivity;

        public MainHandler(IdentifyActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            IdentifyActivity activity = mActivity.get();
            switch (msg.what) {
                case GTMessage.PROTOCOL_CREATE:
                    activity.CreateNew();
                    break;
                case GTMessage.PROTOCOL_RELEASE:
                    activity.Release();
                    break;
                case GTMessage.SET_PROGRESS_VISIBLE:
                    activity.setProgress(true);
                    break;
                case GTMessage.SET_PROGRESS_INVISIBLE:
                    activity.setProgress(false);
                    break;
                case GTMessage.MSG_PROCESSED_TF_FV:
                    activity.sendVector((List<Float>) msg.obj);
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

    public IdentifyActivity() {
        super();
        Log.i(TAG, "Instantiated new " + this.getClass());
        setHandler(new MainHandler(this));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

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

        showWelcome(false);
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
                    Mat cropped = null;
                    try {
                        cropped = new Mat(tempMat, resizeRect);
                    } catch (CvException e) {
                        e.printStackTrace();
                    }

//                    AsyncSaveBmpTask task = new AsyncSaveBmpTask(cropped);
//                    task.execute(new Rect());
                    if (cropped != null) {
                        switch (mSolution) {
                            case PyTensor:
                            case LBPH:
                                Bitmap resized = Bitmap.createBitmap(cropped.width(), cropped.height(), Bitmap.Config.ARGB_8888);

                                Utils.matToBitmap(cropped, resized);

                                if (mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() && !mProtocol.sendImage(String.format("login_%d", System.currentTimeMillis()), resized)) {
                                    Release();
                                    // TODO: error happened!
                                    finish();
                                }
                                resized.recycle();

                                break;
                            case PyTensorFV:
                                mTensor.getFeatureList(cropped);
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
                    }
                    tempMat.release();
                    mHandler.sendEmptyMessage(GTMessage.SET_PROGRESS_VISIBLE);
                }

            }

        }
        tempMat = mRgba.t();
        Core.flip(tempMat, mRgba, mCameraFront ? 1 : -1);
        tempMat.release();

        return mRgba;
    }

    private void sendVector(List<Float> vector) {
        if(mProtocol != null && mProtocol.isReady() && !mProtocol.isProcessing() &&
                mTensor != null && !mProtocol.sendVector(vector, true)) {
            Release();
            // TODO: error happened!
        }
    }

    private void setProgress(boolean visible) {
        mProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        mSpinKit.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void onIdentify(boolean finish, String name) {
        Log.i(TAG, "onIdentify " + mIdentifiedFrame + ": " + name);

        if (finish && !name.equalsIgnoreCase(IClientProtocol.RESULT.UNKNOWN)) {
            mIdentifiedDone = true;

            Intent returnIntent = new Intent();
            returnIntent.putExtra(KEY_NAME, name);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } else if (mIdentifiedFrame > 10 || mProtocol == null) {
            mIdentifiedDone = true;
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }

        mIdentifiedFrame++;
        mIdentifiedFrameText.setText(String.valueOf(mIdentifiedFrame));
        mHandler.sendEmptyMessage(GTMessage.SET_PROGRESS_INVISIBLE);
    }

    public void CreateNew() {

        if(mProtocol == null) {
            if (FLAG_DEBUG) mProtocol = new DummyProtocol(mHandler, IClientProtocol.CMDTYPE.LOGIN);
            else mProtocol = new GtClient(mHandler, -1, mServerAddr, mSolution,
                    IClientProtocol.CMDTYPE.LOGIN,"", "", 0);
            mProtocol.start();
        }
    }

    private void showWelcome(boolean visible) {
        ((TextView)findViewById(R.id.welcome_text)).setVisibility(visible ? View.VISIBLE :View.INVISIBLE);
        ((TextView)findViewById(R.id.welcome_name_text)).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }
}
