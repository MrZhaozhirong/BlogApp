package org.zzrblog.blogapp.program;

import android.content.Context;
import android.opengl.GLES20;

import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2018/3/20.
 */

public class CubeShaderProgram extends ShaderProgram {

    protected static final String U_MATRIX = "u_Matrix";
    public final int uMatrixLocation;

    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    public final int aPositionLocation;
    public final int aColorLocation;


    public CubeShaderProgram(Context context) {
        super(context, R.raw.cube_one_vs, R.raw.cube_one_fs);

        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);
        aColorLocation = GLES20.glGetAttribLocation(programId, A_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
    }

    public void setUniforms(float[] matrix){
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
    }

}
