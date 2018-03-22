package org.zzrblog.blogapp.cube;

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

public class CubeActivity extends Activity {

    private GLSurfaceView glSurfaceView;
    private CubeRenderer cubeRenderer;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        cubeRenderer = new CubeRenderer(CubeActivity.this);

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
            glSurfaceView.setRenderer(cubeRenderer);
            rendererSet = true;
        } else {
            Toast.makeText(this, "this device does not support OpenGL ES 2.0",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        glSurfaceView.setClickable(true);
        glSurfaceView.setOnTouchListener(new GLViewTouchListener());
        setContentView(glSurfaceView);
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

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private class GLViewTouchListener implements View.OnTouchListener {
        private int mode = 0;
        private float oldDist;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            // -------------判断多少个触碰点---------------------------------
            switch (event.getAction() & MotionEvent.ACTION_MASK ){
                case MotionEvent.ACTION_DOWN:
                    mode = 1;
                    break;
                case MotionEvent.ACTION_UP:
                    mode = 0;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    mode -= 1;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    mode += 1;
                    break;
            }

            if(event.getAction() == MotionEvent.ACTION_DOWN){
                //萨达萨达撒
                int i = 1;
            }else if(event.getAction() ==MotionEvent.ACTION_MOVE){
                if (mode == 2) {
                    //双指操作
                    float newDist = spacing(event);
                    if ( (newDist > oldDist + 20) || (newDist < oldDist - 20) ) {
                        final float distance = newDist - oldDist;
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                cubeRenderer.handleMultiTouch(distance);
                            }
                        });
                        oldDist = newDist;
                    }
                }
            }else if(event.getAction() == MotionEvent.ACTION_UP){

            }
            return false;
        }
    }


}
