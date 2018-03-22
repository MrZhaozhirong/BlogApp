package org.zzrblog.blogapp.program;

import android.content.Context;
import android.opengl.GLES20;

import org.zzrblog.blogapp.R;

/**
 * Created by zzr on 2018/2/1.
 */

public class ColorShaderProgram extends ShaderProgram {

    protected static final String U_MATRIX = "u_Matrix";
    public final int uMatrixLocation;

    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    public final int aPositionLocation;
    public final int aColorLocation;

    protected static final String U_COLOR = "u_Color";
    public final int uColorLocation;


    public ColorShaderProgram(Context context) {
        super(context, R.raw.simple_vertex_shader, R.raw.simple_fragment_shader);

        uMatrixLocation = GLES20.glGetUniformLocation(programId, U_MATRIX);

        aColorLocation = GLES20.glGetAttribLocation(programId, A_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(programId, A_POSITION);

        uColorLocation = GLES20.glGetUniformLocation(programId, U_COLOR);
    }

    public void setUniforms(float[] matrix){
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
    }

    public void setUniforms(float[] matrix, float r, float g, float b){
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        GLES20.glUniform4f(uColorLocation, r, g, b, 1f);
    }

}
