package org.zzrblog.ffmp;

/**
 * Created by zzr on 2019/2/14.
 */

public class RtmpPusher {

    static {
        System.loadLibrary("rtmp_push");
    }

    public native void startPush(String url);

    public native void stopPush();

    public native void release();

    /**
     * 设置视频参数
     * @param width
     * @param height
     * @param bitrate
     * @param fps
     */
    public native void setVideoOptions(int width, int height, int bitrate, int fps);

    /**
     * 设置音频参数
     * @param sampleRateInHz
     * @param channel
     */
    public native void setAudioOptions(int sampleRateInHz, int channel);

    /**
     * 发送视频数据
     * @param data nv21数据
     */
    public native void feedVideoData(byte[] data);

    /**
     * 发送音频数据
     * @param data pcm数据
     * @param len 数据长度
     */
    public native void feedAudioData(byte[] data, int len);

}
