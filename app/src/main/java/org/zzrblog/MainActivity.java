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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.zzrblog.blogapp.ITestAIDLInterface;
import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.cube.CubeActivity;
import org.zzrblog.blogapp.hockey.HockeyActivity;
import org.zzrblog.blogapp.panorama.PanoramaActivity;
import org.zzrblog.camera.ContinuousRecordActivity;
import org.zzrblog.fmod.EffectActivity;
import org.zzrblog.fmod.FmodActivity;
import org.zzrblog.mp.TestAIDLService;

public class MainActivity extends Activity {

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
        setContentView(R.layout.activity_home);

        PermissionUtils.requestMultiPermissions(this, mPermissionGrant);

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



    private ITestAIDLInterface mITestAIDLInterface;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mITestAIDLInterface = ITestAIDLInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mITestAIDLInterface = null;
        }
    };





    public void clickOnMultiProcess1(@SuppressLint("USELESS") View view) {
        if(mITestAIDLInterface == null) {
            bindService(new Intent(MainActivity.this, TestAIDLService.class), mServiceConnection, BIND_AUTO_CREATE);
            return;
        }
        try {
            showLogView(mITestAIDLInterface.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void clickOnHockey(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, HockeyActivity.class));
    }

    public void clickOnCube(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, CubeActivity.class));
    }

    public void clickOnPanorama(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, PanoramaActivity.class));
    }

    public void clickOnWatermarkRecord(@SuppressLint("USELESS") View view) {
        startActivity(new Intent(MainActivity.this, ContinuousRecordActivity.class));
    }

    public void clickOnFmod(View view) {
        startActivity(new Intent(MainActivity.this, FmodActivity.class));
    }

    public void clickOnFmodEffect(View view){
        startActivity(new Intent(MainActivity.this, EffectActivity.class));
    }

    private PermissionUtils.PermissionGrant mPermissionGrant = new PermissionUtils.PermissionGrant() {

        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_RECORD_AUDIO:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_RECORD_AUDIO", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_GET_ACCOUNTS:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_GET_ACCOUNTS", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_PHONE_STATE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_READ_PHONE_STATE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_CALL_PHONE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_CALL_PHONE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_CAMERA:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_CAMERA", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_ACCESS_FINE_LOCATION:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_ACCESS_FINE_LOCATION", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_ACCESS_COARSE_LOCATION:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_ACCESS_COARSE_LOCATION", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(MainActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };


}
