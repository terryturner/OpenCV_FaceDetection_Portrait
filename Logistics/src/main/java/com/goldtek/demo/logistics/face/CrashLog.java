package com.goldtek.demo.logistics.face;

import com.goldtek.demo.protocol.client.Common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by darwinhu on 2016/6/20.
 */
public class CrashLog implements Thread.UncaughtExceptionHandler{
    private Thread.UncaughtExceptionHandler defaultUEH;
    private String app = null;

    public CrashLog(String app) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.app = app;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e == null) return;

        StackTraceElement[] arr = e.getStackTrace();

        String report =  e.toString() + "\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i = 0; i < arr.length; i++) {
            report += "    " + arr[i].toString() + "\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report += "    " + arr[i].toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";

        try {
            File file = File.createTempFile(app + "_trace" , ".txt", new File(Common.m_szCrashLogPath));
            FileOutputStream trace = new FileOutputStream(file, true);
            trace.write(report.getBytes());
            trace.close();
        } catch (IOException ioe) {
            // ...
        }

        defaultUEH.uncaughtException(t, e);
    }
}

