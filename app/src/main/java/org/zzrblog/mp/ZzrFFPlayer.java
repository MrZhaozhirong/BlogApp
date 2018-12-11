package org.zzrblog.mp;

import android.view.Surface;

/**
 * Created by zzr on 2018/12/11.
 */

public class ZzrFFPlayer {

    public native void init(String input_str,Surface surface);
    public native void play();
    public native void release();

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

            System.loadLibrary("zzr-ffmpeg-player");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
