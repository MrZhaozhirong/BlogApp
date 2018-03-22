package org.zzrblog.blogapp.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.zzrblog.blogapp.data.VertexArray;
import org.zzrblog.blogapp.program.TextureShaderProgram;
import org.zzrblog.blogapp.utils.Constants;


/**
 * Created by ZZR on 2017/2/10.
 */

public class Table {

    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int  STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT)
                                        * Constants.BYTES_PER_FLOAT;

    public static final float leftBound = -0.5f;
    public static final float rightBound = 0.5f;
    public static final float farBound = -0.8f;
    public static final float nearBound = 0.8f;



    private static final float[] VERTEX_DATA = {
            //x,    y,      s,      t
            0f,     0f,     0.5f,   0.5f,
            -0.5f,  -0.8f,  0f,     0.9f,
            0.5f,   -0.8f,  1f,     0.9f,
            0.5f,   0.8f,   1f,     0.1f,
            -0.5f,  0.8f,   0f,     0.1f,
            -0.5f,  -0.8f,  0f,     0.9f,
    };

    private final VertexArray vertexArray;
    public float[] modelMatrix = new float[16];

    public Table(){
        vertexArray = new VertexArray(VERTEX_DATA);
        Matrix.setIdentityM(modelMatrix,0);
    }

    public void bindData(TextureShaderProgram shaderProgram){
        vertexArray.setVertexAttributePointer(
                shaderProgram.aPositionLocation,
                POSITION_COMPONENT_COUNT,
                STRIDE,
                0
        );

        vertexArray.setVertexAttributePointer(
                shaderProgram.aTextureCoordinatesLocation,
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE,
                POSITION_COMPONENT_COUNT
        );
    }

    public void draw(){
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);
    }


}
