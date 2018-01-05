package com.goldtek.demo.logistics.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Terry on 2017/12/26 0026.
 */

public class Utils {
    private static Bitmap getResizedBitmap(String imagePath) {
        final int MAX_WIDTH = 1024; // 新圖的寬要小於等於這個值

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //只讀取寬度和高度
        BitmapFactory.decodeFile(imagePath, options);
        int width = options.outWidth, height = options.outHeight;

        // 求出要縮小的 scale 值，必需是2的次方，ex: 1,2,4,8,16...
        int scale = 1;
        while(width > MAX_WIDTH*2){
            width /= 2;
            height /= 2;
            scale *= 2;
        }

        // 使用 scale 值產生縮小的圖檔
        BitmapFactory.Options scaledOptions = new BitmapFactory.Options();
        scaledOptions.inSampleSize = scale;
        Bitmap scaledBitmap = BitmapFactory.decodeFile(imagePath, scaledOptions);

        float resize = 1; //縮小值 resize 可為任意小數
        if(width>MAX_WIDTH){
            resize = ((float) MAX_WIDTH) / width;
        }

        Matrix matrix = new Matrix(); // 產生縮圖需要的參數 matrix
        matrix.postScale(resize, resize); // 設定寬與高的縮放比例

        // 產生縮小後的圖
        return Bitmap.createBitmap(scaledBitmap, 0, 0, width, height, matrix, true);
    }


    public static void saveTempBitmap(Bitmap bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap);
        }else{
            //prompt the user or do something
        }
    }

    private static void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "Shutta_"+ timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
