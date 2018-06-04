package org.zzrblog.blogapp.data;

import android.opengl.GLES20;


/**
 * Created by zzr on 2017/8/12.
 */

public class FrameBuffer {

    private int mWidth;
    private int mHeight;
    private int frameBufferId;
    private int renderBufferId;
    private int textureId;

    public FrameBuffer() {
        mWidth=0;
        mHeight=0;
        frameBufferId=0;
        renderBufferId=0;
        textureId=0;
    }
    public int getTextureId() {
        return textureId;
    }
    public boolean isInstantiation() {
        return mWidth!=0||mHeight!=0;
    }




    public boolean setup(int width, int height){
        this.mWidth = width;
        this.mHeight = height;

        final int frameBuffers[] = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffers, 0);
        if (frameBuffers[0] == 0) {
            int i = GLES20.glGetError();
            throw new RuntimeException("Could not create a new frame buffer object, glErrorString : "+ GLES20.glGetString(i));
        }
        frameBufferId = frameBuffers[0];
        //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);

        //final int renderBuffers[] = new int[1];
        //GLES20.glGenRenderbuffers(1, renderBuffers, 0);
        //if (renderBuffers[0] == 0) {
        //    int i = GLES20.glGetError();
        //    throw new RuntimeException("Could not create a new render buffer object, glErrorString : "+GLES20.glGetString(i));
        //}
        //renderBufferId = renderBuffers[0];
        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferId);
        //GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);
        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);

        //GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
        //                                GLES20.GL_RENDERBUFFER, renderBufferId);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return  true;
    }


    public void reSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;

        end();
        release();
        setup(width, height);
    }


    public boolean begin(){
        if(textureId==0 ){
            textureId = createFBOTexture(mWidth, mHeight, GLES20.GL_RGBA);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                                    GLES20.GL_TEXTURE_2D, textureId, 0);
        //GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
        //                            GLES20.GL_RENDERBUFFER, renderBufferId);

        return true;
    }

    private int createFBOTexture(int width, int height, int format) {
        final int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        if(textureIds[0] == 0){
            int i = GLES20.glGetError();
            throw new RuntimeException("Could not create a new texture buffer object, glErrorString : "+ GLES20.glGetString(i));
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height,
                0, format, GLES20.GL_UNSIGNED_BYTE, null);
        return textureIds[0];
    }

    public void end(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    public void release(){
        mWidth = 0;
        mHeight = 0;

        GLES20.glDeleteFramebuffers(1, new int[]{frameBufferId}, 0);
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        //GLES20.glDeleteRenderbuffers(1, new int[]{renderBufferId}, 0);
        frameBufferId = 0;
        textureId = 0;
        //renderBufferId = 0;
    }
}
