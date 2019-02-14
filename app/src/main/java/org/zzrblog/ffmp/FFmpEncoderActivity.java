package org.zzrblog.ffmp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import org.zzrblog.blogapp.R;
import org.zzrblog.util.CameraHelper;
import org.zzrblog.util.CameraListener;

/**
 * Created by zzr on 2019/2/14.
 */

public class FFmpEncoderActivity extends Activity implements ViewTreeObserver.OnGlobalLayoutListener {

    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
    };
    private static final String TAG = "FFmpegTest";
    // ffmpeg编码+原生摄像头视频音频采集+推流
    private SurfaceView cameraView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_encoder_test);

        cameraView = findViewById(R.id.camera_view);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        //在布局结束后才做初始化操作
        cameraView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    private Integer cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private CameraHelper cameraHelper;

    @Override
    public void onGlobalLayout() {
        cameraView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, 0x1234);
        } else {
            initCamera();
            initRecordAudio();
        }
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this.getApplicationContext(), neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x1234) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                initCamera();
                initRecordAudio();
            } else {
                Toast.makeText(this.getApplicationContext(),
                        "Need to allow Camera, Phone State, Record Audio in Settings->Apps.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {

            }

            @Override
            public void onPreview(byte[] data, Camera camera) {

            }

            @Override
            public void onCameraClosed() {

            }

            @Override
            public void onCameraError(Exception e) {

            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {

            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(cameraView.getMeasuredWidth(),cameraView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(cameraID != null ? cameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(true)
                .previewOn(cameraView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
    }

    private int audioRecordChannel = 1;
    private int sampleRateInHz = 44100;
    private int minBufferSize;
    private boolean isAudioRecord;
    private AudioRecord audioRecord;

    private void initRecordAudio() {
        // 单声道 44100
        int channelConfig = audioRecordChannel == 1 ?
                AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        //启动录音子线程
        isAudioRecord = true;
        new Thread(new AudioRecordTask()).start();
    }

    private class AudioRecordTask implements Runnable{

        @Override
        public void run() {
            //开始录音
            audioRecord.startRecording();

            while(isAudioRecord){
                //通过AudioRecord不断读取音频数据
                byte[] buffer = new byte[minBufferSize];
                //ByteBuffer byteBuffer = ByteBuffer.allocateDirect(minBufferSize);
                int len = audioRecord.read(buffer, 0, buffer.length);
                if(len > 0){
                    Log.d(TAG, "audioRecord.read "+buffer.length);
                }
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(cameraHelper!=null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        isAudioRecord = false;
        if(audioRecord!=null) {
            audioRecord.release();
            audioRecord = null;
        }
    }
}
