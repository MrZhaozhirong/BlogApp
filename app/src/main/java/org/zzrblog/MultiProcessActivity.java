package org.zzrblog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.zzrblog.blogapp.IFFmpegAIDLInterface;
import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2018/12/26.
 */

public class MultiProcessActivity extends Activity{

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp_test);

        logView = findViewById(R.id.log_view);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mFFmpegAIDLInterface!=null) {
            unbindService(mServiceConnection);
        }
        unregisterReceiver(logMsgReceiver);
    }
}
