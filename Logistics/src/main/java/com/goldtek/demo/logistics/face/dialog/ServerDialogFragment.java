package com.goldtek.demo.logistics.face.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.goldtek.demo.logistics.face.R;

/**
 * Created by Terry on 2017/12/19 0019.
 */

public class ServerDialogFragment extends DialogFragment {
    private GTSharedPreferences mPreferences;
    private String mRecognizeAddr;
    private String mPalmAddr;
    private int mCascadeResourceID;
    private int mProtocolID;
    private boolean mPlayMedia;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPreferences = new GTSharedPreferences(getContext());

        mRecognizeAddr = mPreferences.getRecognizeServerAddr();
        mPalmAddr = mPreferences.getPalmServerAddr();
        mPlayMedia = mPreferences.getPlayMainMedia();
        mCascadeResourceID = mPreferences.getCascadeUIResource();
        mProtocolID = mPreferences.getRecognizeProtocolID();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setGravity(Gravity.CENTER);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.server_setting_view, null);
        final TextView recognizeServer = view.findViewById(R.id.RecognizeServer);
        recognizeServer.setText(mRecognizeAddr);
        final TextView palmServer = view.findViewById(R.id.PalmServer);
        palmServer.setText(mPalmAddr);
        final ToggleButton toggle = view.findViewById(R.id.PlayMedia);
        toggle.setChecked(mPlayMedia);
        final RadioGroup cascadeGroup = view.findViewById(R.id.RadioCascade);
        cascadeGroup.check(mCascadeResourceID);
        final RadioGroup protocolGroup = view.findViewById(R.id.RadioProtocol);
        protocolGroup.check(mProtocolID);

        setCancelable(false);

        return new AlertDialog.Builder(getActivity())
                //.setIcon(R.drawable.goldtek)
                .setTitle("Config servers")
                .setView(view)
                .setPositiveButton(R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mPreferences.setOfServerDiaglog(
                                        recognizeServer.getText().toString(),
                                        palmServer.getText().toString(),
                                        toggle.isChecked(),
                                        cascadeGroup.getCheckedRadioButtonId(),
                                        protocolGroup.getCheckedRadioButtonId()
                                );

                                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                            }
                        }
                )
                .setNegativeButton(R.string.btn_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                            }
                        }
                )
                .create();
    }
}
