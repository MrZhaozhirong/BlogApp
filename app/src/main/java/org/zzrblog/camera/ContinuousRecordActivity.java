package org.zzrblog.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.zzrblog.blogapp.R;
import org.zzrblog.camera.gles.EglCore;
import org.zzrblog.camera.gles.GlUtil;
import org.zzrblog.camera.gles.WindowSurface;
import org.zzrblog.camera.util.AspectFrameLayout;
import org.zzrblog.camera.util.CameraUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by zzr on 2018/5/9.
 */

public class ContinuousRecordActivity extends Activity implements SurfaceHolder.Callback {

    public static final String TAG = "ContinuousRecord";
    // 因为Androi的摄像头默认是横着方向，所以width>height
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;    // dimensions for 720p video
    private static final int DESIRED_PREVIEW_FPS = 15;

    private Camera mCamera;
    private int mCameraPreviewThousandFps;
    SurfaceView sv;
    private MainHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.continuous_record);

        sv = (SurfaceView) findViewById(R.id.continuousRecord_surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);

        mHandler = new MainHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera(VIDEO_WIDTH, VIDEO_HEIGHT, DESIRED_PREVIEW_FPS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }


        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        // Try to set the frame rate to a constant value.
        mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(parms, desiredFps * 1000);
        // Give the camera a hint that we're recording video.
        // This can have a big impact on frame rate.
        parms.setRecordingHint(true);

        mCamera.setParameters(parms);

        Camera.Size cameraPreviewSize = parms.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Log.d(TAG, "Camera config: " + previewFacts);

        // Set the preview aspect ratio.
        AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.continuousRecord_afl);
        //layout.setAspectRatio((double) cameraPreviewSize.width / cameraPreviewSize.height);
        // Portrait
        layout.setAspectRatio((double) cameraPreviewSize.height / cameraPreviewSize.width);
        mCamera.setDisplayOrientation(90);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }


    private static class MainHandler extends Handler {
        private WeakReference<ContinuousRecordActivity> mWeakActivity;
        public static final int MSG_FRAME_AVAILABLE = 1;

        MainHandler(ContinuousRecordActivity activity) {
            mWeakActivity = new WeakReference<ContinuousRecordActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ContinuousRecordActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.d(TAG, "Got message for dead activity");
                return;
            }
            switch (msg.what) {
                case MSG_FRAME_AVAILABLE:
                    activity.drawFrame();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }


    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private int mTextureId;
    private SurfaceTexture mCameraTexture;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated holder=" + surfaceHolder);
        // 准备好EGL环境，创建渲染介质mDisplaySurface
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
        mDisplaySurface = new WindowSurface(mEglCore, surfaceHolder.getSurface(), false);
        mDisplaySurface.makeCurrent();

        mTextureId = GlUtil.createExternalTextureObject();
        mCameraTexture = new SurfaceTexture(mTextureId);
        mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
            }
        });

        try {
            Log.d(TAG, "starting camera preview");
            mCamera.setPreviewTexture(mCameraTexture);
            mCamera.startPreview();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }


    private void drawFrame() {
        if (mEglCore == null) {
            Log.d(TAG, "Skipping drawFrame after shutdown");
            return;
        }
        Log.d(TAG, " MSG_FRAME_AVAILABLE");
        mDisplaySurface.makeCurrent();
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        mCameraTexture.updateTexImage();
        mDisplaySurface.swapBuffers();
    }
}
