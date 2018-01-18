package com.goldtek.demo.logistics.face.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    public final static String KEY_SERVER_RECOGNIZE = "recognize-server-address";
    public final static String KEY_SERVER_PALM = "palm-server-address";
    public final static String KEY_PLAY_MEDIA = "demo-play-media";
    public final static String KEY_CASCADE = "cascade_file";
    private String mRecognizeAddr;
    private String mPalmAddr;
    private int mCascadeResourceID;
    private boolean mPlayMedia;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mRecognizeAddr = sharedPrefs.getString(KEY_SERVER_RECOGNIZE, "127.0.0.1");
        mPalmAddr = sharedPrefs.getString(KEY_SERVER_PALM, "127.0.0.1");
        mPlayMedia = sharedPrefs.getBoolean(KEY_PLAY_MEDIA, true);
        mCascadeResourceID = sharedPrefs.getInt(KEY_CASCADE, R.id.radio_cascade_win);
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

        setCancelable(false);

        return new AlertDialog.Builder(getActivity())
                //.setIcon(R.drawable.goldtek)
                .setTitle("Config servers")
                .setView(view)
                .setPositiveButton(R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putString(KEY_SERVER_RECOGNIZE, recognizeServer.getText().toString());
                                editor.putString(KEY_SERVER_PALM, palmServer.getText().toString());
                                editor.putBoolean(KEY_PLAY_MEDIA, toggle.isChecked());
                                editor.putInt(KEY_CASCADE, cascadeGroup.getCheckedRadioButtonId());
                                editor.commit();

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
