package org.zzrblog.blogapp.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.zzrblog.blogapp.data.VertexArray;
import org.zzrblog.blogapp.program.CubeShaderProgram;
import org.zzrblog.blogapp.utils.Constants;

/**
 * Created by zzr on 2018/3/20.
 */

public class Cube {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT+COLOR_COMPONENT_COUNT)* Constants.BYTES_PER_FLOAT;


    private final VertexArray vertexArray;
    public float[] modelMatrix = new float[16];

    public Cube() {
        vertexArray = new VertexArray(CUBE_DATA);
        Matrix.setIdentityM(modelMatrix,0);
    }

    public void bindData(CubeShaderProgram shaderProgram){

        vertexArray.setVertexAttributePointer(
                shaderProgram.aPositionLocation,
                POSITION_COMPONENT_COUNT,
                STRIDE,
                0);

        vertexArray.setVertexAttributePointer(
                shaderProgram.aColorLocation,
                COLOR_COMPONENT_COUNT,
                STRIDE,
                POSITION_COMPONENT_COUNT
        );
    }

    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6*2*3);
    }

    private static final float[] CUBE_DATA = {
            //x,    y,    z     R, G, B
             1f,   1f,   1f,    1, 0, 1,   //近平面第一个三角形
            -1f,   1f,   1f,    1, 0, 0,
            -1f,  -1f,   1f,    0, 0, 1,
             1f,   1f,   1f,    1, 0, 1,   //近平面第二个三角形
            -1f,  -1f,   1f,    0, 0, 1,
             1f,  -1f,   1f,    0, 1, 0,

             1f,   1f,  -1f,    0, 0, 1,    //远平面第一个三角形
            -1f,   1f,  -1f,    0, 1, 0,
            -1f,  -1f,  -1f,    1, 0, 1,
             1f,   1f,  -1f,    0, 0, 1,   //远平面第二个三角形
            -1f,  -1f,  -1f,    1, 0, 1,
             1f,  -1f,  -1f,    1, 0, 0,

            -1f,   1f,  -1f,    0, 1, 0,   //左平面第一个三角形
            -1f,   1f,   1f,    1, 0, 0,
            -1f,  -1f,   1f,    0, 0, 1,
            -1f,   1f,  -1f,    0, 1, 0,   //左平面第二个三角形
            -1f,  -1f,   1f,    0, 0, 1,
            -1f,  -1f,  -1f,    1, 0, 1,

             1f,   1f,  -1f,    0, 0, 1,   //右平面第一个三角形
             1f,   1f,   1f,    1, 0, 1,
             1f,  -1f,   1f,    0, 1, 0,
             1f,   1f,  -1f,    0, 0, 1,   //右平面第二个三角形
             1f,  -1f,   1f,    0, 1, 0,
             1f,  -1f,  -1f,    1, 0, 0,

             1f,   1f,  -1f,    0, 0, 1,   //上平面第一个三角形
            -1f,   1f,  -1f,    0, 1, 0,
            -1f,   1f,   1f,    1, 0, 0,
             1f,   1f,  -1f,    0, 0, 1,   //上平面第二个三角形
            -1f,   1f,   1f,    1, 0, 0,
             1f,   1f,   1f,    1, 0, 1,

             1f,  -1f,  -1f,    1, 0, 0,   //下平面第一个三角形
            -1f,  -1f,  -1f,    1, 0, 1,
            -1f,  -1f,   1f,    0, 0, 1,
             1f,  -1f,  -1f,    1, 0, 0,   //下平面第二个三角形
            -1f,  -1f,   1f,    0, 0, 1,
             1f,  -1f,   1f,    0, 1, 0,
    };

}
