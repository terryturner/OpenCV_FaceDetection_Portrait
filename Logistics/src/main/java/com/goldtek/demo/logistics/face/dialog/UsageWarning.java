package com.goldtek.demo.logistics.face.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;

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
    }

    public AlertDialog.Builder setPositiveButton(@StringRes int textId, final DialogInterface.OnClickListener listener) {
        mListener = listener;
        return super.setPositiveButton(textId, this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (mListener != null) {
            mListener.onClick(dialogInterface, mCode);
        }
    }
}
