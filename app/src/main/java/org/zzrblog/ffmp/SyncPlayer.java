package org.zzrblog.ffmp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.Surface;

/**
 * 音视频同步播放-PThread非阻塞
 * Created by zzr on 2019/1/3.
 */

public class SyncPlayer {

    private Context context;
    private String media_input_str;
    private Surface surface;

    public SyncPlayer(Context context) {
        this.context = context;
        nativeInit();
    }
    public void setMediaSource(String media_input_str){
        this.media_input_str = media_input_str;
    }
    public void setRender(Surface surface){
        this.surface = surface;
    }

    public void prepare() {
        nativePrepare(media_input_str, surface);
    }

    public void play() {
        nativePlay();
    }

    public void release() {
        nativeRelease();
        media_input_str = null;
        surface = null;
    }

    public native void nativeInit();
    public native void nativePrepare(String media_input_str, Surface surface);
    public native int nativePlay();
    public native void nativeRelease();

    static
    {
        try {
            System.loadLibrary("yuv");
            System.loadLibrary("avutil");
            System.loadLibrary("swscale");
            System.loadLibrary("swresample");
            System.loadLibrary("avcodec");
            System.loadLibrary("avformat");
            System.loadLibrary("postproc");
            System.loadLibrary("avfilter");
            System.loadLibrary("avdevice");

            System.loadLibrary("sync-player");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建一个AudioTrack对象，用于播放
     * @param sampleRateInHz 采样率
     * @param nb_channels 声道数
     * @return AudioTrack_obj
     *
     * // 使用流程
     * AudioTrack audioTrack = new AudioTrack
     * audioTrack.play();
     * audioTrack.write(audioData, offsetInBytes, sizeInBytes);
     */
    public AudioTrack createAudioTrack(int sampleRateInHz, int nb_channels){
        //音频码流编码格式
        int encodingFormat = AudioFormat.ENCODING_PCM_16BIT;
        //声道布局
        int channelConfig;
        if(nb_channels == 1){
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_MONO;
        } else {
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;
        }

        //int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, encodingFormat);
        ////此方法已经deprecated，正式的可参考下方的代码。
        //AudioTrack audioTrack = new AudioTrack(
        //        AudioManager.STREAM_MUSIC,
        //        sampleRateInHz, channelConfig,
        //        encodingFormat,
        //        bufferSizeInBytes, AudioTrack.MODE_STREAM);

        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, encodingFormat);
        int sessionId = mAudioManager.generateAudioSessionId();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(sampleRateInHz)
                .setEncoding(encodingFormat)
                .setChannelMask(channelConfig)
                .build();
        AudioTrack mAudioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSize + 2048, AudioTrack.MODE_STREAM, sessionId);
        return mAudioTrack;
    }

}
