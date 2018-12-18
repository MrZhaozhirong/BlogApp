package org.zzrblog.mp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.Surface;

/**
 * Created by zzr on 2018/12/11.
 */

public class ZzrFFPlayer {

    public native void init(String media_input_str,Surface surface);
    public native int play();
    public native void release();

    public native int playMusic(String media_input_str);

    /**
     * 创建一个AudioTrac对象，用于播放
     * @param sampleRateInHz 采样率
     * @param nb_channels 声道数
     * @return AudioTrack_obj
     */
    public AudioTrack createAudioTrack(int sampleRateInHz, int nb_channels){
        //固定格式的音频码流
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //声道布局
        int channelConfig;
        if(nb_channels == 1){
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_MONO;
        } else {
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;
        }

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        //此方法已经deprecated，可以参考下方的代码。
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelConfig,
                audioFormat,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);
        //播放
        //audioTrack.play();
        //写入PCM
        //audioTrack.write(audioData, offsetInBytes, sizeInBytes);


        //AudioManager mAudioManager = (AudioManager) Context.getSystemService(Context.AUDIO_SERVICE);
        //int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        //int sessionId = mAudioManager.generateAudioSessionId();
        //AudioAttributes audioAttributes = new AudioAttributes.Builder()
        //        .setUsage(AudioAttributes.USAGE_MEDIA)
        //        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        //        .build();
        //AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(sampleRateInHz)
        //        .setEncoding(audioFormat)
        //        .setChannelMask(channelConfig)
        //        .build();
        //AudioTrack mAudioTrack = new AudioTrack(audioAttributes, audioFormat, bufferSize * 2, AudioTrack.MODE_STREAM, sessionId);
        return audioTrack;
    }

    static
    {
        // Try loading libraries...
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

            System.loadLibrary("zzr-ffmpeg-player");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
