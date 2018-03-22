package org.zzrblog.blogapp.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.zzrblog.blogapp.data.VertexArray;
import org.zzrblog.blogapp.program.CubeShaderProgram;
import org.zzrblog.blogapp.utils.Constants;

import java.nio.ByteBuffer;

/**
 * Created by zzr on 2018/3/20.
 */

public class CubeIndex {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT+COLOR_COMPONENT_COUNT)* Constants.BYTES_PER_FLOAT;


    private final VertexArray vertexArray;
    public float[] modelMatrix = new float[16];

    public CubeIndex() {
        vertexArray = new VertexArray(CUBE_DATA);
        Matrix.setIdentityM(modelMatrix,0);
        indexArray.position(0);
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
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6*2*3, GLES20.GL_UNSIGNED_BYTE, indexArray);
    }

    private static final float[] CUBE_DATA = {
            //x,   y,    z     R,  G,  B
            -1f,   1f,   1f,   1f, 0f, 0f, // 0 left top near
             1f,   1f,   1f,   1f, 0f, 1f, // 1 right top near
            -1f,  -1f,   1f,   0f, 0f, 1f, // 2 left bottom near
             1f,  -1f,   1f,   0f, 1f, 0f, // 3 right bottom near
            -1f,   1f,  -1f,   0f, 1f, 0f, // 4 left top far
             1f,   1f,  -1f,   0f, 0f, 1f, // 5 right top far
            -1f,  -1f,  -1f,   1f, 0f, 1f, // 6 left bottom far
             1f,  -1f,  -1f,   1f, 0f, 0f, // 7 right bottom far
    };

    private ByteBuffer indexArray = ByteBuffer.allocateDirect(6 * 2 * 3)
            .put(new byte[]{
                    //front
                    1, 0, 2,
                    1, 2, 3,
                    //back
                    5, 4, 6,
                    5, 6, 7,
                    //left
                    4, 0, 2,
                    4, 2, 6,
                    //right
                    5, 1, 3,
                    5, 3, 7,
                    //top
                    5, 4, 0,
                    5, 0, 1,
                    //bottom
                    7, 6, 2,
                    7, 2, 3
            });
}
