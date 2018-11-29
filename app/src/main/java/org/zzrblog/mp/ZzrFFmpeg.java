package org.zzrblog.mp;

import android.util.Log;

/**
 * Created by zzr on 2018/11/28.
 */

public class ZzrFFmpeg {



    public static native int Mp4TOYuv(String input_path_str, String output_path_str );

    static
    {
        // Try loading libraries...
        try {
            System.loadLibrary("avcodec");
            System.loadLibrary("avformat");
            System.loadLibrary("avutil");
            System.loadLibrary("zzr-ffmpeg-utils");
            Log.w("ZzrFFmpeg", "ZzrFFmpeg System.loadLibrary ...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
