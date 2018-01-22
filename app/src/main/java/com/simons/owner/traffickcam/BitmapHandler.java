package com.simons.owner.traffickcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class BitmapHandler {

    Bitmap bitmap;

    public BitmapHandler(){}

    public Bitmap getBitmapFromFile(String filepath)
    {
        Bitmap bitmap = null;
        File f = new File(filepath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.bitmap = bitmap;
        return bitmap;
    }

}

