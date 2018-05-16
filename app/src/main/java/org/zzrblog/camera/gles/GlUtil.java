package org.zzrblog.camera.gles;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by zzr on 2018/5/16.
 */

public class GlUtil {
    public static final String TAG = "ZZR-GL";

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }
}
