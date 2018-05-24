package org.zzrblog.camera.objects;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import org.zzrblog.camera.gles.GlUtil;
import org.zzrblog.camera.program.FrameRectSProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by zzr on 2018/5/21.
 */

public class FrameRect {

    public static final int SIZE_OF_FLOAT = 4;
    /**
     * 简单的等边三角形（每边1个）。以（0，0）为中心。
     */
    private static final float TRIANGLE_COORDS[] = {
            0.0f,  0.577350269f,   // 0 top
            -0.5f, -0.288675135f,   // 1 bottom left
            0.5f, -0.288675135f    // 2 bottom right
    };
    private static final float TRIANGLE_TEX_COORDS[] = {
            0.5f, 0.0f,     // 0 top center
            0.0f, 1.0f,     // 1 bottom left
            1.0f, 1.0f,     // 2 bottom right
    };

    /**
     * 简单的正方形，指定为三角形条带。
     * 正方形以（0，0）为中心，其大小为1x1。
     *
     * 两个三角形带 0-1-2 1-2-3 (逆时针绕组).
     */
    private static final float RECTANGLE_COORDS[] = {
            -0.5f, -0.5f,   // 0 bottom left
            0.5f, -0.5f,   // 1 bottom right
            -0.5f,  0.5f,   // 2 top left
            0.5f,  0.5f,   // 3 top right
    };
    private static final float RECTANGLE_TEX_COORDS[] = {
            0.0f, 1.0f,     // 0 bottom left
            1.0f, 1.0f,     // 1 bottom right
            0.0f, 0.0f,     // 2 top left
            1.0f, 0.0f      // 3 top right
    };

    /**
     * 一个“完整”的正方形，从两维延伸到-1到1。
     * 当 模型/视图/投影矩阵是都为单位矩阵的时候，这将完全覆盖视口。
     * 纹理坐标相对于矩形是y反的。
     * (This seems to work out right with external textures from SurfaceTexture.)
     */
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
    };
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mCoordsPerVertex;
    private int mCoordsPerTexture;
    private int mVertexCount;
    private int mVertexStride;
    private int mTexCoordStride;


    public FrameRect() {
        mVertexArray = createFloatBuffer(FULL_RECTANGLE_COORDS);
        mTexCoordArray = createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
        mCoordsPerVertex = 2;
        mCoordsPerTexture = 2;
        mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex; // 4
        mTexCoordStride = 2 * SIZE_OF_FLOAT;
        mVertexStride = 2 * SIZE_OF_FLOAT;

        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    private FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZE_OF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    private FrameRectSProgram mProgram;

    public void setShaderProgram(FrameRectSProgram mProgram) {
        this.mProgram = mProgram;
    }

    public float[] mProjectionMatrix = new float[16];// 投影矩阵
    public float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    public float[] mModelMatrix = new float[16];// 模型变换矩阵
    public float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵
    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    public void drawFrame(int mTextureId, float[] texMatrix) {
        GLES20.glUseProgram(mProgram.getShaderProgramId());
        // 设置纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        GLES20.glUniform1i(mProgram.sTextureOESLoc, 0);
        GlUtil.checkGlError("TEXTURE_EXTERNAL_OES sTextureOES");
        // 设置 model / view / projection 矩阵
        GLES20.glUniformMatrix4fv(mProgram.uMVPMatrixLoc, 1, false, getFinalMatrix(), 0);
        GlUtil.checkGlError("glUniformMatrix4fv uMVPMatrixLoc");
        // 设置 纹理变换矩阵
        GLES20.glUniformMatrix4fv(mProgram.uTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv uTexMatrixLoc");
        // 使用简单的VAO 设置顶点坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glVertexAttribPointer(mProgram.aPositionLoc, mCoordsPerVertex,
                GLES20.GL_FLOAT, false, mVertexStride, mVertexArray);
        GlUtil.checkGlError("VAO aPositionLoc");
        // 使用简单的VAO 设置纹理坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glVertexAttribPointer(mProgram.aTextureCoordLoc, mCoordsPerTexture,
                GLES20.GL_FLOAT, false, mTexCoordStride, mTexCoordArray);
        GlUtil.checkGlError("VAO aTextureCoordLoc");
        // GL_TRIANGLE_STRIP三角形带，这就为啥只需要指出4个坐标点，就能画出两个三角形了。
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);
        // Done -- 解绑~
        GLES20.glDisableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glDisableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

    public void drawFrame(int mTextureId, float[] texMatrix, int mTexture2d) {
        GLES20.glUseProgram(mProgram.getShaderProgramId());
        // 设置纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        GLES20.glUniform1i(mProgram.sTextureOESLoc, 0);
        GlUtil.checkGlError("TEXTURE_EXTERNAL_OES sTextureOES");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture2d);
        GLES20.glUniform1i(mProgram.sTextureLoc, 1);
        GlUtil.checkGlError("GL_TEXTURE_2D sTexture");
        // 设置 model / view / projection 矩阵
        GLES20.glUniformMatrix4fv(mProgram.uMVPMatrixLoc, 1, false, getFinalMatrix(), 0);
        GlUtil.checkGlError("glUniformMatrix4fv uMVPMatrixLoc");
        // 设置 纹理变换矩阵
        GLES20.glUniformMatrix4fv(mProgram.uTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv uTexMatrixLoc");
        // 使用简单的VAO 设置顶点坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glVertexAttribPointer(mProgram.aPositionLoc, mCoordsPerVertex,
                GLES20.GL_FLOAT, false, mVertexStride, mVertexArray);
        GlUtil.checkGlError("VAO aPositionLoc");
        // 使用简单的VAO 设置纹理坐标数据
        GLES20.glEnableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glVertexAttribPointer(mProgram.aTextureCoordLoc, mCoordsPerTexture,
                GLES20.GL_FLOAT, false, mTexCoordStride, mTexCoordArray);
        GlUtil.checkGlError("VAO aTextureCoordLoc");
        // GL_TRIANGLE_STRIP三角形带，这就为啥只需要指出4个坐标点，就能画出两个三角形了。
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);
        // Done -- 解绑~
        GLES20.glDisableVertexAttribArray(mProgram.aPositionLoc);
        GLES20.glDisableVertexAttribArray(mProgram.aTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

}
