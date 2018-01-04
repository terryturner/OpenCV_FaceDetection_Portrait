package com.goldtek.demo.logistics.face.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.goldtek.demo.logistics.face.R;

/**
 * Created by Terry on 2017/12/18 0018.
 */

public class ProfileDialogFragment extends DialogFragment {
    public static final String RESPONSE_NAME = "response_profile_name";
    public static final String RESPONSE_LEVEL = "response_profile_level";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.profile_create_view, null);

        final EditText name = view.findViewById(R.id.Name);
        final TextView level = view.findViewById(R.id.Level);
        final SeekBar levelBar = view.findViewById(R.id.LevelBar);
        levelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                level.setText("Level " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return new AlertDialog.Builder(getActivity())
                //.setIcon(R.drawable.goldtek)
                .setTitle("Register")
                .setView(view)
                .setPositiveButton(R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (getTargetFragment() != null) {
                                    getActivity().getIntent().putExtra(RESPONSE_NAME, name.getText().toString());
                                    getActivity().getIntent().putExtra(RESPONSE_LEVEL, levelBar.getProgress());

                                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                                }
                            }
                        }
                )
                .setNegativeButton(R.string.btn_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (getTargetFragment() != null)
                                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                            }
                        }
                )
                .create();
    }
}
