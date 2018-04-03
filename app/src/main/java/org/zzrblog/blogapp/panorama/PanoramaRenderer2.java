package org.zzrblog.blogapp.panorama;

import android.content.Context;
import android.opengl.GLSurfaceView;

import org.zzrblog.blogapp.objects.PanoramaBall;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zzr on 2018/3/23.
 */

public class PanoramaRenderer2 implements GLSurfaceView.Renderer{

    private final Context context;
    private PanoramaBall ball;

    public PanoramaRenderer2(Context context) {
        this.context = context;
        ball = new PanoramaBall(context);
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        if(ball!=null) ball.onSurfaceCreated(eglConfig);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        if(ball!=null) ball.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if(ball!=null) ball.onDrawFrame();
    }

    public void handleTouchUp(float x, float y,
                              final float xVelocity, final float yVelocity) {
        if(ball!=null){
            ball.handleTouchUp( x, y, xVelocity, yVelocity);
        }
    }

    public void handleTouchDrag(float x, float y) {
        if(ball!=null){
            ball.handleTouchMove( x, y);
        }
    }

    public void handleTouchDown(float x, float y) {
        if(ball!=null){
            ball.handleTouchDown( x, y);
        }
    }
}
