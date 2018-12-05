package org.zzrblog.mp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.zzrblog.ZzrApplication;
import org.zzrblog.blogapp.IFFmpegAIDLInterface;
import org.zzrblog.blogapp.R;

import java.io.File;

/**
 * Created by zzr on 2018/11/29.
 */

public class MultiProcessActivity extends Activity {

    /**
     * 接收调试信息
     * */
    public class StatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if( ZzrApplication.DEBUG_ACTION.equalsIgnoreCase(intent.getAction()) ){
                String msg = intent.getStringExtra("DEBUG_MSG");
                showLogView( msg+"\n");
            }
        }
    }
    StatusReceiver logMsgReceiver = new StatusReceiver();

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp_enter);

        logView = findViewById(R.id.logView);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());

        IntentFilter filter1 = new IntentFilter(ZzrApplication.DEBUG_ACTION);
        registerReceiver(logMsgReceiver, filter1);
        // LocalBroadcastManager.getInstance(this).registerReceiver(); 多进程失效
    }

    public void showLogView(String msg){
        if(logView == null)
            return;
        logView.append(msg+"\n");
        int offset=logView.getLineCount()*logView.getLineHeight();
        if(offset>logView.getHeight()){
            logView.scrollTo(0,offset-logView.getHeight());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unregisterReceiver(logMsgReceiver);
    }


    private IFFmpegAIDLInterface mFFmpegAIDLInterface;

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mFFmpegAIDLInterface = IFFmpegAIDLInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mFFmpegAIDLInterface = null;
        }
    };




    public void clickOnMultiProcess(@SuppressLint("USELESS") View view) {
        if(mFFmpegAIDLInterface == null) {
            bindService(new Intent(MultiProcessActivity.this, FFmpegAIDLService.class), mServiceConnection, BIND_AUTO_CREATE);
        } else {
            try {
                showLogView(mFFmpegAIDLInterface.getName()+" 进程已经启动.");
                Toast.makeText(MultiProcessActivity.this, "FFmpeg进程已经启动.", Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void clickOnMultiProcess_MP42YUV(View view) {
        if(mFFmpegAIDLInterface==null) {
            showLogView("先启动多进程-AIDL,谢谢！");
            Toast.makeText(MultiProcessActivity.this, "先启动多进程-AIDL,谢谢！", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = Environment.getExternalStorageDirectory().getPath();
        String input_str = path + "/10s_test.mp4";
        String output_str = path + "/10s_test.yuv";
        if(!new File(input_str).exists()){
            showLogView(input_str+" 文件不存在！");
            Toast.makeText(MultiProcessActivity.this, "找不到测试用例文件！", Toast.LENGTH_SHORT).show();
            return;
        }
        if(new File(output_str).exists()){
            showLogView(output_str+" 文件已存在，自动删除。");
            boolean delete = new File(output_str).delete();
            if(!delete) return;
        }
        try {
            int i = mFFmpegAIDLInterface.Mp4_TO_YUV(input_str, output_str);
            showLogView("Mp4_TO_YUV return:"+i);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //ZzrFFmpeg.Mp4TOYuv(input_str, output_str);
    }


}
