package com.example.gt0p.ciu196project;

import android.content.Context;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Created by gt0p on 17/10/2016.
 */

public final class Utils {
    public static String getRotation(Context context){
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return "portrait";
            case Surface.ROTATION_90:
                return "landscape";
            case Surface.ROTATION_180:
                return "reverse portrait";
            default:
                return "reverse landscape";
        }
    }
}
