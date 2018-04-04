package org.zzrblog.blogapp.panorama;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Toast;

import org.zzrblog.blogapp.utils.Constants;

/**
 * Created by zzr on 2018/3/23.
 */

public class PanoramaActivity extends Activity{


    private GLSurfaceView glSurfaceView;
    private PanoramaRenderer2 renderer;
    private boolean rendererSet = false;
    private VelocityTracker mVelocityTracker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        renderer = new PanoramaRenderer2(PanoramaActivity.this);

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
            glSurfaceView.setRenderer(renderer);
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
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( rendererSet ){
            glSurfaceView.onPause();
        }
        if(null != mVelocityTracker) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
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
        private long lastClickTime;
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            // -------------判断多少个触碰点---------------------------------
            //switch (event.getAction() & MotionEvent.ACTION_MASK ){
            //    case MotionEvent.ACTION_DOWN:
            //        mode = 1;
            //        break;
            //    case MotionEvent.ACTION_UP:
            //        mode = 0;
            //        break;
            //    case MotionEvent.ACTION_POINTER_UP:
            //        mode -= 1;
            //        break;
            //    case MotionEvent.ACTION_POINTER_DOWN:
            //        oldDist = spacing(event);
            //        mode += 1;
            //        break;
            //}

            if(event.getAction() == MotionEvent.ACTION_DOWN){
                final float x = event.getX();
                final float y = event.getY();
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.handleTouchDown(x, y);
                    }
                });
                // 增加速度
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(event);

                if (System.currentTimeMillis() - lastClickTime < 500) {
                    Log.w(Constants.TAG, "SurfaceView-GL double click in thread."+Thread.currentThread().getName());
                    lastClickTime = 0;
                    glSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            renderer.handleDoubleClick();
                        }
                    });
                } else {
                    lastClickTime = System.currentTimeMillis();
                }
            }else if(event.getAction() ==MotionEvent.ACTION_MOVE){
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                // 在获取速度之前总要进行以上两步
                final float x = event.getX();
                final float y = event.getY();
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.handleTouchDrag(x,y);
                    }
                });

            }else if(event.getAction() == MotionEvent.ACTION_UP){
                final float x = event.getX();
                final float y = event.getY();
                final float xVelocity = mVelocityTracker.getXVelocity();
                final float yVelocity = mVelocityTracker.getYVelocity();
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.handleTouchUp(x, y, xVelocity, yVelocity);
                    }
                });
            }else {
                return false;
            }
            return true;
        }
    }
}
