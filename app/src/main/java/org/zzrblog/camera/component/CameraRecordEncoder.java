package org.zzrblog.camera.component;

import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.zzrblog.camera.gles.EglCore;
import org.zzrblog.camera.gles.WindowSurface;
import org.zzrblog.camera.objects.FrameRect;
import org.zzrblog.camera.objects.WaterSignature;
import org.zzrblog.camera.program.FrameRectSProgram;
import org.zzrblog.camera.program.WaterSignSProgram;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by zzr on 2018/5/31.
 */

public class CameraRecordEncoder implements Runnable {
    private static final String TAG = "CameraRecordEncoder";

    /**
     * 编码器设置的bean，为啥不通过构造函数传递。
     * 因为通常情况下，构造的时候都还没清楚设置，和还没获取到EGLContext~2333
     */
    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    // ----- 外部线程通信访问 -----
    private volatile EncoderHandler mHandler;
    private final Object mSyncLock = new Object();
    private boolean mReady;
    private boolean mRunning;

    /**
     * 利用handler机制处理外部线程请求编码器的操作。
     * 嫌弃自己搭建Thread+Handler麻烦的同学可以 HandlerThread
     */
    class EncoderHandler extends Handler {
        static final int MSG_START_RECORDING = 0;
        private WeakReference<CameraRecordEncoder> mWeakEncoder;

        public EncoderHandler(CameraRecordEncoder encoder) {
            mWeakEncoder = new WeakReference<CameraRecordEncoder>(encoder);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraRecordEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }
            int what = msg.what;
            Object obj = msg.obj;
            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((CameraRecordEncoder.EncoderConfig) obj);
                    break;
            }
        }
    }

    /**
     * 开始视频录制。（一般是从其他非录制现场调用的）
     * 我们创建一个新线程，并且根据传入的录制配置EncoderConfig创建编码器。
     * 我们挂起线程等待正式启动后才返回。
     */
    public void startRecording(EncoderConfig encoderConfig) {
        Log.d(TAG, "CameraRecordEncoder: startRecording()");
        synchronized (mSyncLock) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "CameraRecordEncoder").start();
            while (!mReady) {
                try {
                    // 等待编码器线程的启动
                    mSyncLock.wait();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(EncoderHandler.MSG_START_RECORDING, encoderConfig));
    }



    // ----- 仅由编码器线程访问 -----
    private int mTextureId;
    private CameraRecordEncoderCore mRecordEncoder;
    private EglCore mEglCore;
    private WindowSurface mRecorderInputSurface;
    private FrameRect mFrameRect;
    private WaterSignature mWaterSign;

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mSyncLock) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mSyncLock.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mSyncLock) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    // handle Start Recording.
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        try {
            mRecordEncoder = new CameraRecordEncoderCore(config.mWidth, config.mHeight,
                    config.mBitRate, config.mOutputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
        mRecorderInputSurface = new WindowSurface(mEglCore, mRecordEncoder.getInputSurface(), true);
        mRecorderInputSurface.makeCurrent();

        mFrameRect = new FrameRect();
        mFrameRect.setShaderProgram(new FrameRectSProgram());

        mWaterSign = new WaterSignature();
        mWaterSign.setShaderProgram(new WaterSignSProgram());
    }
}
