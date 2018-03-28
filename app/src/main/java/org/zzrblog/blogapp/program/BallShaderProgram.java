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

    public BallShaderProgram(Context context ) {
        super(context, R.raw.ball_vs, R.raw.ball_fs);

        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);
    }

    public void setUniforms(float[] matrix){
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
    }

}
