package org.zzrblog.blogapp.utils;

/**
 * Created by zzr on 2018/4/4.
 */

public class CameraViewport {
    public static final float CRYSTAL_OVERLOOK = 70f;
    public static final float PERSPECTIVE_OVERLOOK = 70f;
    public static final float PLANET_OVERLOOK = 150f;

    public float overlook;
    public float cx; // 摄像机位置x
    public float cy; // 摄像机位置y
    public float cz; // 摄像机位置z
    public float tx; // 摄像机目标点x
    public float ty; // 摄像机目标点y
    public float tz; // 摄像机目标点z
    public float upx;// 摄像机UP向量X分量
    public float upy;// 摄像机UP向量Y分量
    public float upz;// 摄像机UP向量Z分量

    public CameraViewport setCameraVector(float cx,float cy,float cz){
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        return this;
    }
    public CameraViewport setTargetViewVector(float tx,float ty,float tz){
        this.tx = tx;
        this.ty = ty;
        this.tz = tz;
        return this;
    }

    public CameraViewport setCameraUpVector(float upx,float upy,float upz){
        this.upx = upx;
        this.upy = upy;
        this.upz = upz;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        CameraViewport targetEye = (CameraViewport) o;
        if(
            beEqualTo(targetEye.cx , this.cx) &&
            beEqualTo(targetEye.cy , this.cy) &&
            beEqualTo(targetEye.cz , this.cz) &&
            beEqualTo(targetEye.tx , this.tx) &&
            beEqualTo(targetEye.ty , this.ty) &&
            beEqualTo(targetEye.tz , this.tz) &&
            beEqualTo(targetEye.upx , this.upx) &&
            beEqualTo(targetEye.upy , this.upy) &&
            beEqualTo(targetEye.upz , this.upz)
        ) {
            return true;
        }else{
            return false;
        }
    }

    private static boolean beEqualTo(float a, float b){
        if(Math.abs(a-b) < 0.001f || Math.abs(a-b)==0f){
            return true;
        }
        return false;
    }

    public void copyTo(CameraViewport targetEye) {
        if(targetEye != null){
            targetEye.cx = this.cx;
            targetEye.cy = this.cy;
            targetEye.cz = this.cz;

            targetEye.tx = this.tx;
            targetEye.ty = this.ty;
            targetEye.tz = this.tz;

            targetEye.upx = this.upx;
            targetEye.upy = this.upy;
            targetEye.upz = this.upz;
        }
    }
}
