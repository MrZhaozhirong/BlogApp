package org.zzrblog.ffmp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import org.zzrblog.blogapp.R;
import org.zzrblog.util.CameraHelper;
import org.zzrblog.util.CameraListener;

import java.io.File;


/**
 * Created by zzr on 2018/11/29.
 */

public class FFmpegTestActivity extends Activity implements ViewTreeObserver.OnGlobalLayoutListener {

    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
    };

    private static final String TAG = "FFmpegTest";
    // ffmpeg解码+音视频同步
    private SurfaceView surfaceView;
    private ZzrFFPlayer ffPlayer;
    private SyncPlayer  syncPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_test);

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);

        cameraView = findViewById(R.id.camera_view);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        //在布局结束后才做初始化操作
        cameraView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "FFmpegTestActivity onDestroy ! ");
        if(syncPlayer!=null) {
            syncPlayer.release();
            syncPlayer = null;
        }
        if(ffPlayer!=null) {
            ffPlayer.release();
            ffPlayer = null;
        }
    }

    public void clickOnPlayVideo(@SuppressLint("USELESS") View view) {
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_mp4 = path + "/10s_test.mp4";
        if(ffPlayer==null) {
            ffPlayer = new ZzrFFPlayer();
            ffPlayer.init(input_mp4,surfaceView.getHolder().getSurface());
        }
        ffPlayer.play();
    }

    public void clickOnPlayMusic(@SuppressLint("USELESS") View view) {
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_mp4 = path + "/10s_test.mp4";
        if(ffPlayer==null) {
            ffPlayer = new ZzrFFPlayer();
        }
        ffPlayer.playMusic(input_mp4);
    }

    public void clickOnSyncPlay(@SuppressLint("USELESS") View view) {
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_mp4 = path + "/10s_test.mp4";
        if(syncPlayer!=null) {
            syncPlayer.release();
            syncPlayer = null;
        }
        syncPlayer = new SyncPlayer(FFmpegTestActivity.this);
        syncPlayer.setMediaSource(input_mp4);
        syncPlayer.setRender(surfaceView.getHolder().getSurface());
        syncPlayer.prepare();
        syncPlayer.play();
    }

    public void clickOnMP42YUV(@SuppressLint("USELESS") View view) {
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_mp4 = path + "/10s_test.mp4";
        String output_yuv = path + "/10s_test.yuv";
        if(!new File(input_mp4).exists()){
            Log.d(TAG, input_mp4 + " 文件不存在！");
            Toast.makeText(FFmpegTestActivity.this, "找不到测试用例文件！", Toast.LENGTH_SHORT).show();
            return;
        }
        if(new File(output_yuv).exists()){
            Log.d(TAG, output_yuv+" 文件已存在，自动删除。");
            boolean delete = new File(output_yuv).delete();
            if(!delete) return;
        }
        try {
            //int i = mFFmpegAIDLInterface.Mp4_TO_YUV(input_mp4, output_yuv, output_h264);
            final int i = ZzrFFmpeg.Mp4TOYuv(input_mp4, output_yuv);
            Log.d(TAG, "Mp4_TO_YUV return:"+i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clickOnMP42PCM(@SuppressLint("USELESS") View view) {
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_mp4 = path + "/10s_test.mp4";
        String output_pcm = path + "/10s_test.pcm";
        if(!new File(input_mp4).exists()){
            Log.d(TAG, input_mp4+" 文件不存在！");
            Toast.makeText(FFmpegTestActivity.this, "找不到测试用例文件！", Toast.LENGTH_SHORT).show();
            return;
        }
        if(new File(output_pcm).exists()){
            Log.d(TAG, output_pcm+" 文件已存在，自动删除。");
            boolean delete = new File(output_pcm).delete();
            if(!delete) return;
        }
        try {
            ZzrFFmpeg.Mp34TOPcm(input_mp4, output_pcm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }












    // ffmpeg编码+原生摄像头视频音频采集+推流
    private SurfaceView cameraView;
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
