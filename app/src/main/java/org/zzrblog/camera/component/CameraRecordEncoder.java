package org.zzrblog.camera.component;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.utils.TextureHelper;
import org.zzrblog.camera.gles.EglCore;
import org.zzrblog.camera.gles.WindowSurface;
import org.zzrblog.camera.objects.FBOFrameRect;
import org.zzrblog.camera.objects.FrameRect;
import org.zzrblog.camera.objects.WaterSignature;
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
        final Context mContext;
        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext,Context context) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
            mContext = context;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    // ---------------以下代码 供外部线程通信访问 CameraRecordEncoder工作线程不直接使用--------------------------------------


    private volatile EncoderHandler mHandler;
    private final Object mSyncLock = new Object();
    private boolean mReady;
    private boolean mRunning;
    private volatile float[] transform = new float[16];

    public boolean isRecording() {
        synchronized (mSyncLock) {
            return mRunning;
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

    /**
     * 告诉录像渲染线程停止录像  （一般是从其他非录制现场调用的）
     */
    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(EncoderHandler.MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(EncoderHandler.MSG_QUIT));
        // Codec和Muxer感觉不是立刻结束的，我们是不是应该弄个回调？
    }

    public void frameAvailable(SurfaceTexture mSurfaceTexture) {
        synchronized (mSyncLock) {
            if (!mReady) {
                return;
            }
        }
        Matrix.setIdentityM(transform, 0);
        mSurfaceTexture.getTransformMatrix(transform);
        long timestamp = mSurfaceTexture.getTimestamp();
        // This timestamp is in nanoseconds！纳秒为单位
        if (timestamp == 0) {
            // 调试发现当按下开关，关闭打开屏幕的时候，会遇到 PresentationTime=0
            Log.w(TAG, "NOTE: got SurfaceTexture with timestamp of zero");
            return;
        }
        // 我这里是纠结的，如果想startRecord自定义一个传递bean，或者直接传递SurfaceTexture，都显得太重了。
        // 因为此方法是每帧都调用，尽量轻量高效。
        mHandler.sendMessage(mHandler.obtainMessage(EncoderHandler.MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform));
    }

    public void setTextureId(int id) {
        synchronized (mSyncLock) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(EncoderHandler.MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * 利用handler机制处理外部线程请求编码器的操作。
     * 嫌弃自己搭建Thread+Handler麻烦的同学可以用 HandlerThread
     */
    class EncoderHandler extends Handler {
        static final int MSG_START_RECORDING = 0;
        static final int MSG_STOP_RECORDING = 1;
        static final int MSG_QUIT = 2;
        static final int MSG_FRAME_AVAILABLE = 3;
        static final int MSG_SET_TEXTURE_ID = 4;
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
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    mRunning = false;
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    //float[] transform = (float[]) obj;
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(msg.arg1);
                    break;
            }
        }
    }
    // ---------------以上代码 供外部线程通信访问 CameraRecordEncoder工作线程不直接使用--------------------------------------








    // ---------------以下部分代码 仅由编码器线程访问 ---------------------------------------------------------------------
    private int mFrameTextureId;
    private int mSignTexId;
    private CameraRecordEncoderCore mRecordEncoder;
    private EglCore mEglCore;
    private WindowSurface mRecorderInputSurface;
    private FrameRect mFrameRect;
    private WaterSignature mWaterSign;
    private FBOFrameRect mFBOFrameRect;
    private EncoderConfig mConfig;

    // handle Start Recording.
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        try {
            mRecordEncoder = new CameraRecordEncoderCore(config.mWidth, config.mHeight,
                    config.mBitRate, config.mOutputFile);
            mConfig = config;
            mSignTexId = TextureHelper.loadTexture(config.mContext, R.mipmap.name);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
        mRecorderInputSurface = new WindowSurface(mEglCore, mRecordEncoder.getInputSurface(), true);
        mRecorderInputSurface.makeCurrent();

        //mFrameRect = new FrameRect();
        //mFrameRect.setShaderProgram(new FrameRectSProgram());

        //mWaterSign = new WaterSignature();
        //mWaterSign.setShaderProgram(new WaterSignSProgram());

        mFBOFrameRect = new FBOFrameRect();
        mFBOFrameRect.setShaderProgram(new WaterSignSProgram());
    }

    // Handles a request to stop encoding.
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mRecordEncoder.drainEncoder(true);
        releaseEncoder();
    }

    private void releaseEncoder() {
        mRecordEncoder.release();
        if (mRecorderInputSurface != null) {
            mRecorderInputSurface.release();
            mRecorderInputSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private void handleSetTexture(int id) {
        Log.d(TAG, "handleSetTexture " + id);
        mFrameTextureId = id;
    }

    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        //先推动一次编码器工作，把编码后的数据写入Muxer
        mRecordEncoder.drainEncoder(false);

        mRecorderInputSurface.makeCurrent();
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //GLES20.glViewport(0, 0, mConfig.mWidth, mConfig.mHeight );
        //mFrameRect.drawFrame(mFrameTextureId, transform);
        //GLES20.glViewport(0, 0, 288, 144);
        //mWaterSign.drawFrame(mSignTexId);
        GLES20.glViewport(0, 0, mConfig.mWidth, mConfig.mHeight );
        mFBOFrameRect.drawFrame(mFrameTextureId);

        // mRecorderInputSurface是 获取编码器的输入Surface 创建的EGLSurface，
        // 以上的draw直接渲染到mRecorderInputSurface，喂养数据到编码器当中，非常方便。
        mRecorderInputSurface.setPresentationTime(timestampNanos);
        mRecorderInputSurface.swapBuffers();
    }
}
