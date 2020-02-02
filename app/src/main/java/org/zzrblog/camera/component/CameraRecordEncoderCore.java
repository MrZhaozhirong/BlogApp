package org.zzrblog.camera.component;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;


/**
 * Created by zzr on 2018/5/31.
 */

public class CameraRecordEncoderCore {
    private static final String TAG = "RecordEncoderCore";
    private static final boolean DEBUG = true;
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int I_FRAME_INTERVAL = 5;          // I-frames 间隔 5s

    private MediaCodec mVideoEncoder;
    private Surface mInputSurface;
    private MediaMuxer mMuxer;

    /**
     * 配置 编码器和合成器的各种状态，准备输入源供外部喂养数据。
     * @param width 编码视频的宽度
     * @param height 编码视频的高度
     * @param bitRate 比特率/码率
     * @param outputFile 输出mp4路径
     */
    public CameraRecordEncoderCore(int width, int height, int bitRate, File outputFile)
            throws IOException {
        // 1. 设置编码器类型
        // MediaFormat.MIMETYPE_VIDEO_AVC = "video/avc"; // H.264 Advanced Video Coding
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,//设置输入源类型为原生Surface 重点1 参考下面官网复制过来的说明
                COLOR_FormatSurface);
        //Raw Video Buffers
        // In ByteBuffer mode video buffers are laid out according to their color format.
        // You can get the supported color formats as an array from getCodecInfo().getCapabilitiesForType(…).colorFormats.
        // Video codecs may support three kinds of color formats:
        // I、native raw video format: This is marked by COLOR_FormatSurface and
        //      it can be used with an input or output Surface.
        // II、flexible YUV buffers (such as COLOR_FormatYUV420Flexible): These can be used with an input/output Surface,
        //      as well as in ByteBuffer mode, by using getInput/OutputImage(int).
        // III、other, specific formats: These are normally only supported in ByteBuffer mode.
        //      Some color formats are vendor specific. Others are defined in MediaCodecInfo.CodecCapabilities.
        //      For color formats that are equivalent to a flexible format, you can still use getInput/OutputImage(int).

        // 2. 创建我们的编码器，配置我们以上的设置
        mVideoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 3. 获取编码喂养数据的输入源surface
        mInputSurface = mVideoEncoder.createInputSurface();
        mVideoEncoder.start();
        // 4. 创建混合器，但我们不能在这里start，因为我们还没有编码后的视频数据，
        // 更没有把编码后的数据以track（轨道）的形式加到合成器。
        mMuxer = new MediaMuxer(outputFile.toString(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mBufferInfo = new MediaCodec.BufferInfo();
        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    public Surface getInputSurface() {
        return mInputSurface;
    }


    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private static final int TIMEOUT_USEC = 10000;
    /**
     * 从编码器中提取所有未处理的数据，并将其转发给Muxer。
     * endOfStream是代表是否编码结束的终结符，
     * 如果是false就是正常请求输入数据去编码，按正常流程走这次编码操作。
     * 如果是true我们需要告诉编码器编码工作结束了，发送一个EOS结束标志位到输入源，
     * 然后等到我们在编码输出的数据发现EOS的时候，证明最后的一批编码数据已经编码成功了。
     */
    public void drainEncoder(boolean endOfStream) {

        if (endOfStream) {
            if (DEBUG) Log.d(TAG, "sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
        }
        // 1. 获取编码输出队列
        ByteBuffer[] encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        while (true) {
            // 2. 从编码的输出队列中检索出各种状态，对应处理
            // 参数一是MediaCodec.BufferInfo，主要是用来承载对应buffer的附加信息。
            // 参数二是超时时间，请注意单位是微秒，1毫秒=1000微秒
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if(encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 暂时还没输出的数据能捕获
                if (!endOfStream) {
                    break;      // out of while(true){}
                } else {
                    if (DEBUG) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if(encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // 这个状态说明输出队列对象改变了，请重新获取一遍。
                encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            } else if(encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 当我们接收到编码后的输出数据，会通过格式已转变这个标志触发，而且只会发生一次格式转变
                // 因为不可能从设置指定的格式变成其他，难不成一个视频能有两种编码格式？
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat videoFormat = mVideoEncoder.getOutputFormat();
                // 现在我们已经得到想要的编码数据了，让我们开始合成进mp4容器文件里面吧。
                mTrackIndex = mMuxer.addTrack(videoFormat);
                // 获取track轨道号，等下写入编码数据的时候需要用到
                mMuxer.start();
                mMuxerStarted = true;
            } else if(encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                // Continue while(true)
            } else {
                // 3. 各种状态处理之后，大于0的encoderStatus则是指出了编码数据是在编码队列的具体位置。
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // 这表明，标记为这样的缓冲器包含编解码器初始化/编解码器特定数据而不是媒体数据。
                    if (DEBUG) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    if (DEBUG) {
                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                    }
                }
                // 释放编码器的输出队列中 指定位置的buffer，第二个参数指定是否渲染其buffer到解码Surface
                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (DEBUG) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

    /**
     * produce a sync frame "soon".
     */
    public void requstSyncFrame() {
        Bundle params = new Bundle();
        params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
        mVideoEncoder.setParameters(params);
    }
    /**
     * Releases encoder resources.
     */
    public void release() {
        if (DEBUG) Log.d(TAG, "releasing encoder objects");
        if (mVideoEncoder != null ) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mMuxer != null && mTrackIndex != -1) {
            // stop() throws an exception if you haven't fed it any data.
            // Keep track of frames submitted, and don't call stop() if we haven't written anything.
            // Once the muxer stops, it can not be restarted.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }


    // 判断一下系统是否支持MediaCodec编码h264
    private boolean SupportAvcCodec(){
        if(Build.VERSION.SDK_INT>=18){
            for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);

                String[] types = codecInfo.getSupportedTypes();
                for (int i = 0; i < types.length; i++) {
                    if (types[i].equalsIgnoreCase("video/avc")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
