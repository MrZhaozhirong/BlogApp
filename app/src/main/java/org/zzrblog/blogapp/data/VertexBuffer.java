package org.zzrblog.blogapp.data;

import android.opengl.GLES20;

import org.zzrblog.blogapp.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by zzr on 2018/3/23.
 */

public class VertexBuffer {

    private final int bufferId;

    public int getVertexBufferID() {
        return bufferId;
    }

    public VertexBuffer(float[] vertexData) {

        //allocate a buffer
        final int buffers[] = new int[1];
        GLES20.glGenBuffers(buffers.length, buffers, 0);
        if (buffers[0] == 0) {
            int i = GLES20.glGetError();
            throw new RuntimeException("Could not create a new vertex buffer object, glGetError : "+i);
        }
        bufferId = buffers[0];
        //bind to the buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        //Transfer data to native memory.
        FloatBuffer vertexArry = ByteBuffer.allocateDirect(vertexData.length * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);

        vertexArry.position(0);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexArry.capacity() * Constants.BYTES_PER_FLOAT,
                vertexArry, GLES20.GL_STATIC_DRAW);

        //IMPORTANT! unbind the buffer when done with it
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void setVertexAttributePointer(int attributeLocation,
                                          int componentCount, int stride, int dataOffset){
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);

        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT,
                false, stride, dataOffset);
        GLES20.glEnableVertexAttribArray(attributeLocation);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}
