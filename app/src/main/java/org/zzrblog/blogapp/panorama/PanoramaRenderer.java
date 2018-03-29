package org.zzrblog.blogapp.panorama;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import org.zzrblog.blogapp.objects.Ball;
import org.zzrblog.blogapp.utils.MatrixHelper;

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

    Ball ball;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        ball = new Ball(context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float)width/(float)height, 1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 0f, 4f,
                0f, 0f, 0f,
                0f, 1f, 0f);
        Matrix.multiplyMM(viewProjectionMatrix,0,  projectionMatrix,0, viewMatrix,0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(modelViewProjectionMatrix,0, viewProjectionMatrix,0, ball.modelMatrix,0);
        ball.draw(modelViewProjectionMatrix);
    }
}
