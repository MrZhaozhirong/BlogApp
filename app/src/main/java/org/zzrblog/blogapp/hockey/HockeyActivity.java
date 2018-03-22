package org.zzrblog.blogapp.hockey;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Created by zzr on 2018/1/12.
 */

public class HockeyActivity extends Activity implements View.OnTouchListener {

    private GLSurfaceView glSurfaceView;
    private HockeyRenderer3 hockeyRenderer;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        hockeyRenderer = new HockeyRenderer3(HockeyActivity.this);

        int glVersion = getGLVersion();
        if(glVersion > 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")))
                ){
            glSurfaceView.setEGLContextClientVersion(2);
            glSurfaceView.setRenderer(hockeyRenderer);
            rendererSet = true;
        } else {
            Toast.makeText(this, "this device does not support OpenGL ES 2.0",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        glSurfaceView.setOnTouchListener(this);
        setContentView(glSurfaceView);
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if(event == null) return false;
        final float normalizedX = (event.getX() / view.getWidth()) * 2 - 1;
        final float normalizedY = -((event.getY() / view.getHeight()) * 2 - 1);

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    hockeyRenderer.handleTouchDown(normalizedX,normalizedY);
                }
            });
        }else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    hockeyRenderer.handleTouchMove(normalizedX,normalizedY);
                }
            });
        }else {
            return false;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if( rendererSet ){
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( rendererSet ){
            glSurfaceView.onPause();
        }
    }


    private int getGLVersion(){
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo deviceConfigurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return deviceConfigurationInfo.reqGlEsVersion;
    }

}
