package org.zzrblog.camera.component;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.R.attr.format;

/**
 * Created by zzr on 2017/12/18.
 * 在 CameraRecordEncoderCore的基础上增加音频混合
 * 学习了 AAC的 ADTS/ADIF 头
 * 利用PTS同步音视频帧
 */

public class CameraRecordEncoderCore2 {
    private static final String TAG = "RecordEncoderCore2";
    private static final boolean DEBUG = true;

    private static final String VIDEO_MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private MediaCodec mVideoEncoder;
    private Surface mInputSurface;


    private int bufferSize;
    private AudioRecord mAudioRecorder;   //录音器
    private static final int SAMPLE_RATE = 48000;   //音频采样率
    private static final int AUDIO_RATE = 128000;   //音频编码的密钥比特率
    private static final int CHANNEL_COUNT = 2;     //音频编码通道数
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private MediaCodec mAudioEncoder;   //编码器，用于音频编码
    private MediaCodec.BufferInfo mAudioBufferInfo;
    private static final String AUDIO_MINE_TYPE = "audio/mp4a-latm";   // Low-overhead MPEG-4 Audio TransportMultiplex


    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private boolean mMuxerStarted;
    private MediaMuxer mMuxer;

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public CameraRecordEncoderCore2(int width, int height, int bitRate, File outputFile)
            throws IOException {
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        mAudioBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (DEBUG) Log.d(TAG, "format: " + format);
        // Create a MediaCodec encoder, and configure it with our format.
        // Get a Surface we can use for input and wrap it with a class that handles the EGL work.
        mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        mVideoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mVideoEncoder.createInputSurface();
        mVideoEncoder.start();
        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mMuxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mMuxerStarted = false;


        // About Audio by ZZR
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                                    CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MINE_TYPE, SAMPLE_RATE, CHANNEL_COUNT);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_RATE);

        mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MINE_TYPE);
        mAudioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioEncoder.start();
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
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
        if (mAudioEncoder != null ) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }
        if (mMuxer != null && mMuxerStarted
                && mVideoTrackIndex != -1
                && mAudioTrackIndex != -1) {
            // stop() throws an exception if you haven't fed it any data.
            // Keep track of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     *
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     *
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream) {
            if (DEBUG) Log.d(TAG, "sending EOS to encoder");
            mVideoEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    if (DEBUG) Log.d(TAG, "Video no output available, out of while(true) ");
                    break;      // out of while(true){}
                } else {
                    if (DEBUG) Log.d(TAG, "Video no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat videoFormat = mVideoEncoder.getOutputFormat();
                // now that we have the Magic Goodies, start the muxer
                mVideoTrackIndex = mMuxer.addTrack(videoFormat);
                if (DEBUG) Log.d(TAG, "mVideoEncoder INFO_OUTPUT_FORMAT_CHANGED, mVideoTrackIndex:"+mVideoTrackIndex);
                if(mVideoTrackIndex!=-1 && mAudioTrackIndex!=-1){
                    Log.w(TAG, "MediaMuxer addVideoTrackIndex "+mVideoTrackIndex);
                    Log.w(TAG, "MediaMuxer addAudioTrackIndex "+mAudioTrackIndex);
                    mMuxer.start();
                    mMuxerStarted = true;
                }
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from VideoEncoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // Ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (DEBUG) Log.d(TAG, "ignoring Video BUFFER_FLAG_CODEC_CONFIG");
                    mVideoBufferInfo.size = 0;
                }
                if (mVideoBufferInfo.size != 0) {
                    if (mMuxerStarted) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        if((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
                            if (DEBUG) Log.i(TAG, "sent BUFFER_FLAG_KEY_FRAME Video bytes to mMuxer");
                        }
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                        mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mVideoBufferInfo);
                        if (DEBUG) {
                            Log.d(TAG, "sent " + mVideoBufferInfo.size + " Video bytes to muxer, ts=" +
                                    mVideoBufferInfo.presentationTimeUs);
                        }
                    } else {
                        Log.w(TAG, "Video muxer hasn't started");
                    }
                }

                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (DEBUG) Log.d(TAG, "Video end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }





    private volatile boolean isRecording;
    private Thread mSetupAudioBufferThread;
    // En/Disable Audio Record
    public void audioRecord(boolean endOfStream) {
        if( !endOfStream ) {
            mAudioRecorder.startRecording();
            isRecording = true;
            mSetupAudioBufferThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    if (isRecording){
                        setupAudioBuffer();
                    }
                }
            });
            mSetupAudioBufferThread.start();
        } else {
            mAudioRecorder.stop();
            isRecording = false;
            try {
                mSetupAudioBufferThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupAudioBuffer() {
        final int TIMEOUT_USEC = 10000;
        long firstInputTimeNsec = -1;
        boolean inputDone = false;
        while ( !inputDone ) {
            // Feed Audio data to the encoder.
            //if (!inputDone) {
                int inputBufIndex = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    if (firstInputTimeNsec == -1) {
                        firstInputTimeNsec = System.nanoTime();
                    }
                    ByteBuffer[] encoderInputBuffers = mAudioEncoder.getInputBuffers();
                    ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];
                    int length = mAudioRecorder.read(inputBuf, bufferSize);
                    if(length > 0 && isRecording){
                        mAudioEncoder.queueInputBuffer(inputBufIndex, 0, length,
                                mVideoBufferInfo.presentationTimeUs, 0);
                                //(System.nanoTime()-firstInputTimeNsec)/1000, 0);
                    } else if(!isRecording){
                        mAudioEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        Log.e(TAG,"Input Audio buffer length--> "+length);
                    }
                } else if(!isRecording){
                    inputDone = true;
                } else {
                    if (DEBUG) Log.d(TAG, "AudioEncoder input buffer not available");
                }
            //}
        }
    }

    public void drainAudioEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        ByteBuffer[] encoderOutputBuffers = mAudioEncoder.getOutputBuffers();
        while( true ){
            int encoderStatus = mAudioEncoder.dequeueOutputBuffer(mAudioBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    if (DEBUG) Log.d(TAG, "Audio no output available, out of while(true) ");
                    break;      // out of while(true){}
                } else {
                    if (DEBUG) Log.d(TAG, "Audio no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mAudioEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat audioFormat = mAudioEncoder.getOutputFormat();
                mAudioTrackIndex = mMuxer.addTrack(audioFormat);
                if (DEBUG) Log.d(TAG, "mAudioEncoder INFO_OUTPUT_FORMAT_CHANGED, mAudioTrackIndex:"+mAudioTrackIndex);
                if(mAudioTrackIndex!=-1 && mVideoTrackIndex!=-1){
                    Log.w(TAG, "MediaMuxer addAudioTrackIndex "+mAudioTrackIndex);
                    Log.w(TAG, "MediaMuxer addVideoTrackIndex "+mVideoTrackIndex);
                    mMuxer.start();
                    mMuxerStarted = true;
                }
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from AudioEncoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // Ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }
                if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (DEBUG) Log.d(TAG, "ignoring Audio BUFFER_FLAG_CODEC_CONFIG");
                    mAudioBufferInfo.size = 0;
                }
                if (mAudioBufferInfo.size != 0) {
                    if (mMuxerStarted) {
                        if((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
                            if (DEBUG) Log.i(TAG, "sent BUFFER_FLAG_KEY_FRAME Audio bytes to mMuxer");
                        }
                        encodedData.position(mAudioBufferInfo.offset);
                        encodedData.limit(mAudioBufferInfo.offset + mAudioBufferInfo.size);
                        // ///////////////////////////////////////////
                        // byte[] temp=new byte[mAudioBufferInfo.size+7];
                        // encodedData.get(temp,7,mAudioBufferInfo.size);
                        // generateADTSPacket(temp,temp.length);
                        // ///////////////////////////////////////////
                        mMuxer.writeSampleData(mAudioTrackIndex, encodedData/*ByteBuffer.wrap(temp)*/, mAudioBufferInfo);
                        if (DEBUG) {
                            Log.d(TAG, "sent " + mAudioBufferInfo.size + " Audio bytes to muxer, ts=" +
                                    mAudioBufferInfo.presentationTimeUs);
                        }
                    } else {
                        Log.w(TAG, "Audio muxer hasn't started");
                    }
                }
                mAudioEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mAudioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (DEBUG) Log.d(TAG, "Audio end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }


    private void generateADTSPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

}
