package org.zzrblog.blogapp.hockey;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.utils.MatrixHelper;
import org.zzrblog.blogapp.utils.ShaderHelper;
import org.zzrblog.blogapp.utils.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zzr on 2018/1/12.
 * http://blog.csdn.net/a360940265a   文章1~5代码
 */

public class HockeyRenderer implements GLSurfaceView.Renderer {

    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    float[] tableVerticesWithTriangles = {
            //// 第一个三角形
            //-0.5f, -0.5f,
            //0.5f, 0.5f,
            //-0.5f, 0.5f,
            //// 第二个三角形
            //-0.5f, -0.5f,
            //0.5f, -0.5f,
            //0.5f, 0.5f,

            // X, Y, R, G, B
            // 三角扇
             0,     0,     1f, 1f, 1f,
            -0.5f, -0.8f,  0.7f,0.7f,0.7f,
             0.5f, -0.8f,  0.7f,0.7f,0.7f,
             0.5f,  0.8f,  0.7f,0.7f,0.7f,
            -0.5f,  0.8f,  0.7f,0.7f,0.7f,
            -0.5f, -0.8f,  0.7f,0.7f,0.7f,
            // 中间的分界线
            -0.5f,  0f,    1f,0f,0f,
             0.5f,  0f,    1f,0f,0f,
            // 两个木槌的质点位置
             0f,   -0.4f,  0f,0f,0f,
             0f,    0.4f,  0f,0f,0f,
    };

    private final FloatBuffer vertexData;
    private String vertexShaderSource;
    private String fragmentShaderSource;

    public HockeyRenderer(Context context) {

        vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_vertex_shader);
        fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_fragment_shader);

        vertexData = ByteBuffer
                .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(tableVerticesWithTriangles);
    }

    //private static final String U_COLOR = "u_Color";
    //private int uColorLocation;
    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;
    private static final String A_COLOR = "a_Color";
    private int aColorLocation;
    private static final String U_MATRIX = "u_Matrix";
    private int uMatrixLocation;


    private final float[] uMatrix = new float[16];

    private final float[] projectionMatrix = new float[16];

    private final float[] modelMatrix = new float[16];


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        int programId = ShaderHelper.buildProgram(vertexShaderSource, fragmentShaderSource);
        GLES20.glUseProgram(programId);
        //uColorLocation = GLES20.glGetUniformLocation(programId, U_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
        aColorLocation = GLES20.glGetAttribLocation(programId, A_COLOR);
        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);

        vertexData.position(0);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, vertexData);
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        vertexData.position(2);
        GLES20.glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, vertexData);
        GLES20.glEnableVertexAttribArray(aColorLocation);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);

        //final float aspectRatio = width > height ?
        //        (float)width / (float)height   :
        //        (float)height / (float)width ;
        //if(width > height){
        //    Matrix.orthoM(projectionMatrix,0, -aspectRatio, aspectRatio,   -1f,1f,    -1f,1f);
        //} else {
        //    Matrix.orthoM(projectionMatrix,0, -1f,1f,    -aspectRatio, aspectRatio,   -1f,1f);
        //}

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float)width/(float)height, 1f, 100f);

        Matrix.setIdentityM(modelMatrix,0);
        Matrix.translateM(modelMatrix, 0, 0f,0f,-3f);
        Matrix.rotateM(modelMatrix,0,  -60f, 1f,0f,0f);

        Matrix.multiplyMM(uMatrix,0,  projectionMatrix,0,   modelMatrix,0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //GLES20.glUniform4f(uColorLocation, 1.0f,1.0f,1.0f, 1.0f);
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glUniformMatrix4fv(uMatrixLocation,1, false,  uMatrix,0);
        //GLES20.glUniform4f(uColorLocation, 1.0f,1.0f,1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);

        //GLES20.glUniform4f(uColorLocation, 1.0f,0.0f,0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 6, 2);

        //GLES20.glUniform4f(uColorLocation, 0.0f,0.0f,1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 8, 1);
        //GLES20.glUniform4f(uColorLocation, 0.0f,1.0f,0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 9, 1);
    }



}
