package org.zzrblog.blogapp.program;

import android.content.Context;
import android.opengl.GLES20;

import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2018/3/27.
 */

public class BallShaderProgram extends ShaderProgram{

    protected static final String U_MATRIX = "u_Matrix";
    public final int uMatrixLocation;

    protected static final String A_POSITION = "a_Position";
    public final int aPositionLocation;

    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    public final int aTextureCoordinatesLocation;

    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
    public final int uTextureUnitLocation;

    public BallShaderProgram(Context context ) {
        super(context, R.raw.ball_vs, R.raw.ball_fs);

        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);

        aTextureCoordinatesLocation=GLES20.glGetAttribLocation(programId, A_TEXTURE_COORDINATES);
        uTextureUnitLocation = GLES20.glGetUniformLocation(programId, U_TEXTURE_UNIT);
    }

    public void setUniforms(float[] matrix,int textureId){
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        // 激活纹理单元0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 绑定纹理对象ID
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // 告诉shaderProgram sampler2D纹理采集器 使用纹理单元0的纹理对象。
        GLES20.glUniform1i(uTextureUnitLocation, 0);
    }

}
