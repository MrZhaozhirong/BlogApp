package org.zzrblog.blogapp.panorama;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zzr on 2018/3/23.
 */

public class PanoramaRenderer implements GLSurfaceView.Renderer{

    private final Context context;
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    public PanoramaRenderer(Context context) {
        this.context = context;
        Matrix.setIdentityM(projectionMatrix,0);
        Matrix.setIdentityM(viewMatrix,0);
        Matrix.setIdentityM(viewProjectionMatrix,0);
        Matrix.setIdentityM(modelViewProjectionMatrix,0);
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

    @Override
    public void onDrawFrame(GL10 gl10) {

    }
}
