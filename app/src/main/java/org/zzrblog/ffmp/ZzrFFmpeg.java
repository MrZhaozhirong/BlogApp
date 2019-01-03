package org.zzrblog.ffmp;

import android.util.Log;

/**
 * 此模块主要是演示 视频解码 音频解码的主要流程。
 * ffmpeg代码都运行在主线程上，会阻塞主线程。
 * Created by zzr on 2018/11/28.
 */
public class ZzrFFmpeg {

    public static native int Mp34TOPcm(String input_mp3_str, String output_pcm_str);

    public static native int Mp4TOYuv(String input_mp4_str, String output_yuv_str);

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
