package org.zzrblog.blogapp.cube;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import org.zzrblog.blogapp.objects.CubeIndex;
import org.zzrblog.blogapp.program.CubeShaderProgram;
import org.zzrblog.blogapp.utils.Constants;
import org.zzrblog.blogapp.utils.MatrixHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by zzr on 2018/3/19.
 */

public class CubeRenderer implements GLSurfaceView.Renderer {


    private final Context context;
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    CubeIndex cube;
    CubeShaderProgram cubeShaderProgram;

    public CubeRenderer(Context context) {
        this.context = context;
        Matrix.setIdentityM(projectionMatrix,0);
        Matrix.setIdentityM(viewMatrix,0);
        Matrix.setIdentityM(viewProjectionMatrix,0);
        Matrix.setIdentityM(modelViewProjectionMatrix,0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        cube = new CubeIndex();

        cubeShaderProgram = new CubeShaderProgram(context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float)width/(float)height, 1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0,
                4f, 4f, 4f,
                0f, 0f, 0f,
                0f, 1f, 0f);
        Matrix.multiplyMM(viewProjectionMatrix,0,  projectionMatrix,0, viewMatrix,0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(modelViewProjectionMatrix,0, viewProjectionMatrix,0, cube.modelMatrix,0);
        cubeShaderProgram.userProgram();
        cubeShaderProgram.setUniforms(modelViewProjectionMatrix);
        cube.bindData(cubeShaderProgram);
        cube.draw();
    }


    public void handleMultiTouch(float distance) {
        Log.d(Constants.TAG, "distance : "+distance);
        float OBJECT_SCALE_FLOAT = 2.0f;
        if(distance > 0) {
            Matrix.scaleM(cube.modelMatrix,0, OBJECT_SCALE_FLOAT,OBJECT_SCALE_FLOAT,OBJECT_SCALE_FLOAT);
        } else {
            Matrix.scaleM(cube.modelMatrix,0, 1/OBJECT_SCALE_FLOAT,1/OBJECT_SCALE_FLOAT,1/OBJECT_SCALE_FLOAT);
        }
    }
}
