package org.zzrblog.mp;

import android.util.Log;

/**
 * Created by zzr on 2018/11/28.
 */

public class ZzrFFmpeg {



    public static native int Mp4TOYuv(String input_mp4_str, String output_yuv_str, String output_h264_str );

    static
    {
        // Try loading libraries...
        try {
            System.loadLibrary("avutil");
            System.loadLibrary("swscale");
            System.loadLibrary("swresample");
            System.loadLibrary("avcodec");
            System.loadLibrary("avformat");

            System.loadLibrary("postproc");
            System.loadLibrary("avfilter");
            System.loadLibrary("avdevice");

            System.loadLibrary("zzr-ffmpeg-utils");
            Log.w("ZzrBlogApp", "ZzrFFmpeg System.loadLibrary ...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
