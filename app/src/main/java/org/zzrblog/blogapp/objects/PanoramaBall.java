package org.zzrblog.blogapp.objects;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.data.IndexBuffer;
import org.zzrblog.blogapp.data.VertexBuffer;
import org.zzrblog.blogapp.program.BallShaderProgram;
import org.zzrblog.blogapp.utils.CameraViewport;
import org.zzrblog.blogapp.utils.Constants;
import org.zzrblog.blogapp.utils.MatrixHelper;
import org.zzrblog.blogapp.utils.TextureHelper;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;


/**
 * Created by zzr on 2018/3/23.
 */

public class PanoramaBall {
    private static final int POSITION_COORDIANTE_COMPONENT_COUNT = 3; // 每个顶点的坐标数 x y z
    private static final int TEXTURE_COORDIANTE_COMPONENT_COUNT = 2; // 每个顶点的坐标数 x y z
    private static final int STRIDE = (POSITION_COORDIANTE_COMPONENT_COUNT
            + TEXTURE_COORDIANTE_COMPONENT_COUNT)
            * Constants.BYTES_PER_FLOAT;

    private Context context;
    IndexBuffer indexBuffer;
    VertexBuffer vertexBuffer;
    BallShaderProgram ballShaderProgram;
    private int numElements = 0; // 记录要画多少个三角形
    private int textureId;

    private float[] mProjectionMatrix = new float[16];// 投影矩阵
    private float[] mViewMatrix = new float[16]; // 摄像机位置朝向9参数矩阵
    private float[] mModelMatrix = new float[16];// 模型变换矩阵
    private float[] mMVPMatrix = new float[16];// 获取具体物体的总变换矩阵
    private float[] getFinalMatrix() {
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    private void initVertexData() {
        final int angleSpan = 5;// 将球进行单位切分的角度，此数值越小划分矩形越多，球面越趋近平滑
        final float radius = 1.0f;// 球体半径
        short offset = 0;
        ArrayList<Float> vertexList = new ArrayList<>(); // 使用list存放顶点数据
        ArrayList<Short> indexList = new ArrayList<>();// 顶点索引数组
        for (int vAngle = 0; vAngle < 180; vAngle = vAngle + angleSpan)
        {
            for (int hAngle = 0; hAngle <= 360; hAngle = hAngle + angleSpan)
            {
                // st纹理坐标
                float s0 = hAngle / 360.0f; //左上角 s
                float t0 = vAngle / 180.0f; //左上角 t
                float s1 = (hAngle + angleSpan)/360.0f; //右下角s
                float t1 = (vAngle + angleSpan)/180.0f; //右下角t
                // 左上角 0
                float z0 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.cos(Math
                        .toRadians(hAngle)));
                float x0 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.sin(Math
                        .toRadians(hAngle)));
                float y0 = (float) (radius * Math.cos(Math.toRadians(vAngle)));
                vertexList.add(x0);
                vertexList.add(y0);
                vertexList.add(z0);
                vertexList.add(s0);
                vertexList.add(t0);
                // 右上角 1
                float z1 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.cos(Math
                        .toRadians(hAngle + angleSpan)));
                float x1 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.sin(Math
                        .toRadians(hAngle + angleSpan)));
                float y1 = (float) (radius * Math.cos(Math.toRadians(vAngle)));
                vertexList.add(x1);
                vertexList.add(y1);
                vertexList.add(z1);
                vertexList.add(s1);
                vertexList.add(t0);
                // 右下角 2
                float z2 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .cos(Math.toRadians(hAngle + angleSpan)));
                float x2 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .sin(Math.toRadians(hAngle + angleSpan)));
                float y2 = (float) (radius * Math.cos(Math.toRadians(vAngle + angleSpan)));
                vertexList.add(x2);
                vertexList.add(y2);
                vertexList.add(z2);
                vertexList.add(s1);
                vertexList.add(t1);
                // 左下角 3
                float z3 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .cos(Math.toRadians(hAngle)));
                float x3 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .sin(Math.toRadians(hAngle)));
                float y3 = (float) (radius * Math.cos(Math.toRadians(vAngle + angleSpan)));
                vertexList.add(x3);
                vertexList.add(y3);
                vertexList.add(z3);
                vertexList.add(s0);
                vertexList.add(t1);

                indexList.add((short)(offset + 0));
                indexList.add((short)(offset + 3));
                indexList.add((short)(offset + 2));
                indexList.add((short)(offset + 0));
                indexList.add((short)(offset + 2));
                indexList.add((short)(offset + 1));

                offset += 4; // 4个顶点的偏移
            }
        }

        numElements = indexList.size();// 记录有多少个索引点

        float[] data_vertex = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            data_vertex[i] = vertexList.get(i);
        }
        vertexBuffer = new VertexBuffer(data_vertex);

        short[] data_index = new short[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            data_index[i] = indexList.get(i);
        }
        indexBuffer = new IndexBuffer(data_index);
    }

    private void buildProgram() {
        ballShaderProgram = new BallShaderProgram(context);
        ballShaderProgram.userProgram();
    }

    private void setAttributeStatus() {
        vertexBuffer.setVertexAttributePointer(
                ballShaderProgram.aPositionLocation,
                POSITION_COORDIANTE_COMPONENT_COUNT,
                STRIDE, 0 );
        vertexBuffer.setVertexAttributePointer(
                ballShaderProgram.aTextureCoordinatesLocation,
                TEXTURE_COORDIANTE_COMPONENT_COUNT,
                STRIDE,
                POSITION_COORDIANTE_COMPONENT_COUNT * Constants.BYTES_PER_FLOAT);
    }

    private void initTexture() {
        textureId = TextureHelper.loadTexture(context, R.mipmap.test_normal);
    }


    public PanoramaBall(Context context){
        this.context = context;
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    public void onSurfaceCreated(EGLConfig eglConfig) {
        initVertexData();
        initTexture();
        buildProgram();
        setAttributeStatus();
    }

    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0,0,width,height);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        if(currentViewport==null)
            currentViewport = new CameraViewport();
        if(targetViewport==null)
            targetViewport = new CameraViewport();

        currentViewport.overlook = CameraViewport.CRYSTAL_OVERLOOK;
        currentViewport.setCameraVector(0, 0, 2.8f);
        currentViewport.setTargetViewVector(0f, 0f, 0.0f);
        currentViewport.setCameraUpVector(0f, 1.0f, 0.0f);
        currentViewport.copyTo(targetViewport);

        MatrixHelper.perspectiveM(mProjectionMatrix, currentViewport.overlook,
                (float)width/(float)height, 0.01f, 1000f);
        Matrix.setLookAtM(this.mViewMatrix,0,
                currentViewport.cx,  currentViewport.cy,  currentViewport.cz,
                currentViewport.tx,  currentViewport.ty,  currentViewport.tz,
                currentViewport.upx, currentViewport.upy, currentViewport.upz);
    }

    public void onDrawFrame() {
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        ballShaderProgram.userProgram();
        setAttributeStatus();
        updateBallMatrix();
        ballShaderProgram.setUniforms(getFinalMatrix(), textureId);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getIndexBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private CameraViewport currentViewport;
    private CameraViewport targetViewport;
    private int currentControlMode = Constants.RENDER_MODE_CRYSTAL;
    private int targetControlMode  = currentControlMode;
    //** 视野变换
    public int nextControlMode() {
        if(currentControlMode == Constants.RENDER_MODE_CRYSTAL){
            targetViewport.overlook = CameraViewport.PERSPECTIVE_OVERLOOK;
            targetViewport.setCameraVector(0, 0, 1.0f);
            //targetViewport.setTargetViewVector(0f, 0f, 0.0f);
            //targetViewport.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = Constants.RENDER_MODE_PERSPECTIVE;
        }

        if(currentControlMode == Constants.RENDER_MODE_PERSPECTIVE){
            targetViewport.overlook = CameraViewport.PLANET_OVERLOOK;
            targetViewport.setCameraVector(0, 0, 1.0f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = Constants.RENDER_MODE_PLANET;
        }

        if(currentControlMode == Constants.RENDER_MODE_PLANET){
            targetViewport.overlook = CameraViewport.CRYSTAL_OVERLOOK;
            targetViewport.setCameraVector(0, 0, -1.9f);
            //tartgetEye.setTargetViewVector(0f, 0f, 0.0f);
            //tartgetEye.setCameraUpVector(0f, 1.0f, 0.0f);
            targetControlMode = Constants.RENDER_MODE_CRYSTAL;
        }
        return targetControlMode;
    }













    //** 惯性自滚标志
    private volatile boolean gestureInertia_isStop = true;
    private float mLastX;
    private float mLastY;
    private float rotationX = 0;
    private float rotationY = 0;
    private float[] mMatrixRotationX = new float[16];
    private float[] mMatrixRotationY = new float[16];

    private void updateBallMatrix() {
        Matrix.setIdentityM(this.mModelMatrix, 0);
        Matrix.setIdentityM(mMatrixRotationX, 0);
        Matrix.setIdentityM(mMatrixRotationY, 0);
        if(rotationY > 360 || rotationY < -360){
            rotationY = rotationY % 360;
        }
        Matrix.rotateM(mMatrixRotationY, 0, this.rotationY, 0, 1, 0);
        Matrix.rotateM(mMatrixRotationX, 0, this.rotationX, 1, 0, 0);
        Matrix.multiplyMM(this.mModelMatrix,0, mMatrixRotationX,0, mMatrixRotationY,0 );
    }

    public void handleTouchUp(final float x, final float y,
                              final float xVelocity, final float yVelocity) {
        this.mLastX = 0;
        this.mLastY = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleGestureInertia(x, y, xVelocity, yVelocity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleGestureInertia(float upX, float upY, float xVelocity, float yVelocity)
            throws InterruptedException {
        this.gestureInertia_isStop = false;
        float mXVelocity = xVelocity;
        float mYVelocity = yVelocity;
        while(!this.gestureInertia_isStop){
//--------------------------------------------------------------------------------
            float offsetY = mYVelocity / 2000;
            this.rotationX = this.rotationX + offsetY;

            float offsetX = mXVelocity / 2000;
            this.rotationY = this.rotationY + offsetX;

            if(rotationX%360 > 90 ){
                this.rotationX = 90;
            }
            if(rotationX%360 < -90 ){
                this.rotationX = -90;
            }
//--------------------------------------------------------------------------------
            if(Math.abs(mYVelocity - 0.97f*mYVelocity) < 0.00001f
                    || Math.abs(mXVelocity - 0.97f*mXVelocity) < 0.00001f){
                this.gestureInertia_isStop = true;
            }
            mYVelocity = 0.975f*mYVelocity;
            mXVelocity = 0.975f*mXVelocity;
            Thread.sleep(5);
        }
    }


    public void handleTouchDown(float x, float y) {
        this.mLastX = x;
        this.mLastY = y;
    }

    public void handleTouchMove(float x, float y) {
        float offsetX = this.mLastX - x;
        float offsetY = this.mLastY - y;
        this.rotationY -= offsetX/10 ; // 屏幕横坐标的步伐，球应该是绕着Y轴旋转
        this.rotationX -= offsetY/10 ; // 屏幕纵坐标的步伐，球应该是绕着X轴旋转

        if(rotationX%360 > 90 ){
            this.rotationX = 90;
        }
        if(rotationX%360 < -90 ){
            this.rotationX = -90;
        }
        this.mLastX = x;
        this.mLastY = y;
    }

}
