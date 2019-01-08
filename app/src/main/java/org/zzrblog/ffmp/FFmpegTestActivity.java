package org.zzrblog.ffmp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.zzrblog.blogapp.R;

import java.io.File;


/**
 * Created by zzr on 2018/11/29.
 */

public class FFmpegTestActivity extends Activity {


    private static final String TAG = "FFmpegTest";
    private SurfaceView surfaceView;
    private ZzrFFPlayer ffPlayer;
    private SyncPlayer  syncPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_test);

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(syncPlayer!=null) {
            syncPlayer.release();
            syncPlayer = null;
        }
        if(ffPlayer!=null) {
            ffPlayer.release();
            ffPlayer = null;
        }
    }

    public void clickOnPlay(@SuppressLint("USELESS") View view) {
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
        if(syncPlayer==null) {
            syncPlayer = new SyncPlayer(FFmpegTestActivity.this);
            syncPlayer.setMediaSource(input_mp4);
            syncPlayer.setRender(surfaceView.getHolder().getSurface());
            syncPlayer.prepare();
        }
        syncPlayer.play();
    }










    public void clickOnMP42YUV(View view) {
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

    public void clickOnMP42PCM(View view) {
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
}
