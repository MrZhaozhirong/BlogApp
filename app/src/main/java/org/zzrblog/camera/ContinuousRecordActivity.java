package org.zzrblog.camera;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.utils.TextureHelper;
import org.zzrblog.camera.component.CameraRecordEncoder;
import org.zzrblog.camera.gles.EglCore;
import org.zzrblog.camera.gles.GlUtil;
import org.zzrblog.camera.gles.WindowSurface;
import org.zzrblog.camera.objects.FrameRect;
import org.zzrblog.camera.objects.WaterSignature;
import org.zzrblog.camera.program.FrameRectSProgram;
import org.zzrblog.camera.program.WaterSignSProgram;
import org.zzrblog.camera.util.AspectFrameLayout;
import org.zzrblog.camera.util.CameraUtils;

import java.io.File;
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

    private File outputFile;
    private volatile boolean mRequestRecord;
    private CameraRecordEncoder mRecordEncoder;
    private boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.continuous_record);

        sv = (SurfaceView) findViewById(R.id.continuousRecord_surfaceView);
        SurfaceHolder sh = sv.getHolder();
        sh.setFormat(PixelFormat.RGBA_8888);
        sh.addCallback(this);

        mHandler = new MainHandler(this);
        mFrameRect = new FrameRect();
        mWaterSign = new WaterSignature();

        mRecordEncoder = new CameraRecordEncoder();
        ImageView btnRecord = (ImageView) findViewById(R.id.btn_record);
        btnRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_UP:
                        mRequestRecord = false;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        mRequestRecord = true;
                        break;
                }
                return false;
            }
        });

        outputFile = new File(Environment.getExternalStorageDirectory().getPath(), "camera-test.mp4");
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
        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
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

        // 设置预览view的比例，注意摄像头默认是横着的
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


    //private IntBuffer mSignTexBuffer;
    //private int mSignTexWidth;
    //private int mSignTexHeight;
    //private void loadSignBitmap() {
    //    final BitmapFactory.Options options = new BitmapFactory.Options();
    //    options.inScaled = false;   //指定需要的是原始数据，非压缩数据
    //    Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.mipmap.name, options);
    //    if(bitmap == null){
    //        throw new IllegalStateException("SignTexBuffer not load in bitmap!");
    //    }
    //    mSignTexWidth = bitmap.getWidth();
    //    mSignTexHeight = bitmap.getHeight();
    //    int[] pixels = new int[mSignTexWidth * mSignTexHeight];
    //    bitmap.getPixels(pixels,0,mSignTexWidth, 0,0,mSignTexWidth,mSignTexHeight);
    //    mSignTexBuffer = IntBuffer.wrap(pixels);
    //}
    //**mSignTexId = GlUtil.bitmapBuffer2Texture(mSignTexBuffer, mSignTexWidth, mSignTexHeight,
    //**        GLES20.GL_UNSIGNED_BYTE, GLES20.GL_RGBA, GLES20.GL_RGBA);



    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private int mTextureId;
    private SurfaceTexture mCameraTexture;
    private FrameRect mFrameRect;
    private WaterSignature mWaterSign;
    private int mSignTexId;

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

        mFrameRect.setShaderProgram(new FrameRectSProgram());
        mWaterSign.setShaderProgram(new WaterSignSProgram());
        mSignTexId = TextureHelper.loadTexture(ContinuousRecordActivity.this, R.mipmap.name);

        try {
            Log.d(TAG, "starting camera preview");
            mCamera.setPreviewTexture(mCameraTexture);
            mCamera.startPreview();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        recording = mRecordEncoder.isRecording();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }


    private final float[] mTmpMatrix = new float[16];
    private void drawFrame() {
        if (mEglCore == null) {
            Log.d(TAG, "Skipping drawFrame after shutdown");
            return;
        }
        mDisplaySurface.makeCurrent();
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTmpMatrix);
        int viewWidth = sv.getWidth();
        int viewHeight = sv.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mFrameRect.drawFrame(mTextureId, mTmpMatrix);
        GLES20.glViewport(0, 0, 288, 144);
        mWaterSign.drawFrame(mSignTexId);
        mDisplaySurface.swapBuffers();


        // 水印录制 状态设置
        if(mRequestRecord) {
            if(!recording) {
                mRecordEncoder.startRecording(new CameraRecordEncoder.EncoderConfig(
                        outputFile, VIDEO_HEIGHT, VIDEO_WIDTH, 1000000,
                        EGL14.eglGetCurrentContext(), ContinuousRecordActivity.this));

                mRecordEncoder.setTextureId(mTextureId);
                recording = mRecordEncoder.isRecording();
            }
            // mRecordEncoder.setTextureId(mTextureId);
            mRecordEncoder.frameAvailable(mCameraTexture);
        } else {
            if(recording) {
                mRecordEncoder.stopRecording();
                recording = false;
            }
        }
    }
}
