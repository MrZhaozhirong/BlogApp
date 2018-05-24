package org.zzrblog.camera.program;


import android.opengl.GLES20;

import org.zzrblog.camera.gles.GlUtil;

/**
 * Created by zzr on 2018/5/21.
 */

public class WaterSignSProgram extends ShaderProgram {

    private static final String VERTEX_SHADER =
                    "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public WaterSignSProgram() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);

        uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix");
        GlUtil.checkLocation(uMVPMatrixLoc, "uMVPMatrix");
        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition");
        GlUtil.checkLocation(aPositionLoc, "aPosition");
        aTextureCoordLoc = GLES20.glGetAttribLocation(programId, "aTextureCoord");
        GlUtil.checkLocation(aTextureCoordLoc, "aTextureCoord");
        sTextureLoc = GLES20.glGetUniformLocation(programId, "sTexture");
        GlUtil.checkLocation(sTextureLoc, "sTexture");
    }

    public int uMVPMatrixLoc;
    public int aPositionLoc;
    public int aTextureCoordLoc;
    public int sTextureLoc;
}
