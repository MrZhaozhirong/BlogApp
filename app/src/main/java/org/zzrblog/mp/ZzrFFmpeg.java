package org.zzrblog.mp;

/**
 * Created by zzr on 2018/11/28.
 */

public class ZzrFFmpeg {

    public static native void init();

    public static native void release();


    static
    {
        // Try loading libraries...
        try {
            System.loadLibrary("fmodL");
            System.loadLibrary("fmod");
            System.loadLibrary("fmod-effect-lib");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
