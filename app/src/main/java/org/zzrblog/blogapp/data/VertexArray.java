package org.zzrblog.blogapp.data;

import android.opengl.GLES20;

import org.zzrblog.blogapp.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by ZZR on 2017/2/22.
 */

public class VertexArray {


    private final FloatBuffer floatBuffer;

    public VertexArray(float[] vertexData) {
        floatBuffer = ByteBuffer.allocateDirect(vertexData.length * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
    }

    public void setVertexAttributePointer(int attributeLocation,
                                       int componentCount, int stride, int dataOffset){
        floatBuffer.position(dataOffset);
        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT,
                false, stride, floatBuffer);
        GLES20.glEnableVertexAttribArray(attributeLocation);
        floatBuffer.position(0);
    }
}
