package org.zzrblog.camera.gles;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * Created by zzr on 2018/5/18.
 */

public class WindowSurface extends EglSurfaceBase {

    private Surface mSurface;
    private boolean bReleaseSurface;

    //将native的surface 与 EGL关联起来
    public WindowSurface(EglCore eglCore, Surface surface, boolean isReleaseSurface) {
        super(eglCore);
        createWindowSurface(surface);
        mSurface = surface;
        bReleaseSurface = isReleaseSurface;
    }

    //将SurfaceTexture 与 EGL关联起来
    protected WindowSurface(EglCore eglCore, SurfaceTexture surfaceTexture) {
        super(eglCore);
        createWindowSurface(surfaceTexture);
    }


    //释放当前EGL上下文 关联 的 surface
    public void release() {
        releaseEglSurface();
        if (mSurface != null
                && bReleaseSurface) {
            mSurface.release();
            mSurface = null;
        }
    }


    /**
     * Recreate the EGLSurface, using the new EglBase.  The caller should have already
     * freed the old EGLSurface with releaseEglSurface().
     * <p>
     * This is useful when we want to update the EGLSurface associated with a Surface.
     * For example, if we want to share with a different EGLContext, which can only
     * be done by tearing down and recreating the context.  (That's handled by the caller;
     * this just creates a new EGLSurface for the Surface we were handed earlier.)
     * <p>
     * If the previous EGLSurface isn't fully destroyed, e.g. it's still current on a
     * context somewhere, the create call will fail with complaints from the Surface
     * about already being connected.
     */
    public void recreate(EglCore newEglCore) {
        if (mSurface == null) {
            throw new RuntimeException("not yet implemented for SurfaceTexture");
        }
        mEglCore = newEglCore;          // switch to new context
        createWindowSurface(mSurface);  // create new surface
    }
}
