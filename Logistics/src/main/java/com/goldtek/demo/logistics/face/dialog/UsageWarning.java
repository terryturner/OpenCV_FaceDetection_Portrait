package com.goldtek.demo.logistics.face.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;

import com.marcoscg.easylicensesdialog.EasyLicensesDialogCompat;

/**
 * Created by Terry on 2018/1/9 0009.
 */

public class UsageWarning extends EasyLicensesDialogCompat implements DialogInterface.OnClickListener {
    private final int mCode;
    private DialogInterface.OnClickListener mListener;

    public UsageWarning(Context context, int code) {
        super(context);
        mCode = code;
        setCancelable(false);
    }

    public AlertDialog.Builder setPositiveButton(@StringRes int textId, final DialogInterface.OnClickListener listener) {
        mListener = listener;
        return super.setPositiveButton(textId, this);
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (mListener != null) {
            mListener.onClick(dialogInterface, mCode);
        }
    }
}
