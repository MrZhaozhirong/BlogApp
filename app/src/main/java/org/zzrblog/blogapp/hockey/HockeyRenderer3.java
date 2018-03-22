package org.zzrblog.blogapp.hockey;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import org.zzrblog.blogapp.R;
import org.zzrblog.blogapp.objects.Mallet;
import org.zzrblog.blogapp.objects.Puck;
import org.zzrblog.blogapp.objects.Table;
import org.zzrblog.blogapp.program.ColorShaderProgram;
import org.zzrblog.blogapp.program.TextureShaderProgram;
import org.zzrblog.blogapp.utils.Geometry;
import org.zzrblog.blogapp.utils.MatrixHelper;
import org.zzrblog.blogapp.utils.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * http://blog.csdn.net/a360940265a   文章9代码
 */

public class HockeyRenderer3 implements GLSurfaceView.Renderer {

    private static final String TAG = "HockeyRenderer2";
    private final Context context;
    private Table table;
    private Mallet mallet;
    private Puck puck;
    private TextureShaderProgram textureShaderProgram;
    private ColorShaderProgram colorShaderProgram;

    int textureId;

    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private final float[] invertViewProjectionMatrix = new float[16];

    public HockeyRenderer3(Context context) {
        this.context = context;
        Matrix.setIdentityM(projectionMatrix,0);
        Matrix.setIdentityM(viewMatrix,0);
        Matrix.setIdentityM(viewProjectionMatrix,0);
        Matrix.setIdentityM(modelViewProjectionMatrix,0);

        Matrix.setIdentityM(invertViewProjectionMatrix,0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);

        textureShaderProgram = new TextureShaderProgram(context);
        colorShaderProgram = new ColorShaderProgram(context);

        textureId = TextureHelper.loadTexture(context, R.mipmap.air_hockey_surface);
    }


    
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float)width/(float)height, 1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 1.2f, 2.2f,
                0f, 0f, 0f,
                0f, 1f, 0f);

        Matrix.multiplyMM(viewProjectionMatrix,0,  projectionMatrix,0, viewMatrix,0);
        Matrix.invertM(invertViewProjectionMatrix, 0,  viewProjectionMatrix, 0);

        Matrix.rotateM(table.modelMatrix,0, -90f, 1f,0f,0f);
        Matrix.translateM(mallet.modelMatrix,0, 0f, mallet.height/2f, 0.5f);
        Matrix.translateM(puck.modelMatrix,0, 0f, puck.height/2f, 0f );

        mallet.position = new Geometry.Point(0f, mallet.height/2f, 0.5f);
        puck.position = new Geometry.Point(0f, puck.height/2f, 0f);
        puck.speedVector = new Geometry.Vector(0f, 0f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(modelViewProjectionMatrix,0, viewProjectionMatrix,0, table.modelMatrix,0);
        textureShaderProgram.userProgram();
        textureShaderProgram.setUniforms(modelViewProjectionMatrix, textureId);
        table.bindData(textureShaderProgram);
        table.draw();

        Matrix.setIdentityM(mallet.modelMatrix, 0);
        Matrix.translateM(mallet.modelMatrix,0, mallet.position.x, mallet.position.y, mallet.position.z);
        Matrix.multiplyMM(modelViewProjectionMatrix,0, viewProjectionMatrix,0, mallet.modelMatrix,0);
        colorShaderProgram.userProgram();
        colorShaderProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);
        mallet.bindData(colorShaderProgram);
        mallet.draw();

        updatePuckCollisionTest();
        Matrix.setIdentityM(puck.modelMatrix, 0);
        Matrix.translateM(puck.modelMatrix,0, puck.position.x, puck.position.y, puck.position.z);
        Matrix.multiplyMM(modelViewProjectionMatrix,0, viewProjectionMatrix,0, puck.modelMatrix,0);
        colorShaderProgram.userProgram();
        colorShaderProgram.setUniforms(modelViewProjectionMatrix, 0f, 1f, 0f);
        puck.bindData(colorShaderProgram);
        puck.draw();
    }

    private void updatePuckCollisionTest() {
        puck.position = puck.position.translate(puck.speedVector);
        if(puck.position.x < Table.leftBound + puck.radius
                || puck.position.x > Table.rightBound - puck.radius) {
            puck.speedVector = new Geometry.Vector(-puck.speedVector.x,
                    puck.speedVector.y, puck.speedVector.z);
            puck.speedVector = puck.speedVector.scale(0.9f);
        }
        if(puck.position.z < Table.farBound + puck.radius
                || puck.position.z > Table.nearBound - puck.radius) {
            puck.speedVector = new Geometry.Vector(puck.speedVector.x,
                    puck.speedVector.y, -puck.speedVector.z);
            puck.speedVector = puck.speedVector.scale(0.9f);
        }
        puck.position = new Geometry.Point(
                clamp(puck.position.x, Table.leftBound + puck.radius, Table.rightBound - puck.radius),
                puck.position.y,
                clamp(puck.position.z, Table.farBound + puck.radius, Table.nearBound - puck.radius)
        );
        puck.speedVector = puck.speedVector.scale(0.99f);
    }


    public void handleTouchDown(float normalizedX, float normalizedY) {
        Log.i(TAG, "handleTouchDown normalizedX*normalizedY == " +normalizedX+" "+normalizedY);
        // 我们为啥不直接拿malletPosition当Sphere的中心点？
        // 因为按照原计划木槌位置是跟着手指滑动而改变，所以单纯用初始化的malletPosition不够准确。
        Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(
                new Geometry.Point(mallet.position.x, mallet.position.y, mallet.position.z),
                mallet.radius);

        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        mallet.isPressed = Geometry.intersects(ray, malletBoundingSphere);

        Log.d(TAG, "mallet.malletPressed : "+mallet.isPressed);
    }

    public void handleTouchMove(float normalizedX, float normalizedY) {
        if(mallet.isPressed) {
            // 保存前一刻木槌的位置信息
            mallet.previousPosition = mallet.position;
            // 根据屏幕触碰点 和 视图投影矩阵 产生三维射线
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            // 定义的桌子平面，观察平面的点为（0，0，0）
            Geometry.Plane tablePlane = new Geometry.Plane(new Geometry.Point(0,0,0), new Geometry.Vector(0,1,0));
            // 进行射线-平面 相交测试
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, tablePlane);
            // 根据相交点 更新木槌位置
            //malletPosition = new Geometry.Point(touchedPoint.x, mallet.height/2f, touchedPoint.z);
            mallet.position = new Geometry.Point(
                    clamp(touchedPoint.x, Table.leftBound+mallet.radius, Table.rightBound-mallet.radius),
                    mallet.height/2f, //touchedPoint.y,
                    clamp(touchedPoint.z, Table.farBound+mallet.radius, Table.nearBound-mallet.radius)
            );

            // 检查木槌和冰球是否碰撞，更新冰球移动的方向向量
            float distance = Geometry.vectorBetween(mallet.position, puck.position).length();
            if(distance < (mallet.radius + puck.radius)) {
                puck.speedVector = Geometry.vectorBetween(mallet.previousPosition, mallet.position);
            }
        }
    }



    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private Geometry.Ray convertNormalized2DPointToRay(
            float normalizedX, float normalizedY) {

        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        Matrix.multiplyMV(nearPointWorld,0, invertViewProjectionMatrix,0, nearPointNdc,0);
        Matrix.multiplyMV(farPointWorld,0, invertViewProjectionMatrix,0, farPointNdc,0);

        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay = new Geometry.Point(nearPointWorld[0],nearPointWorld[1],nearPointWorld[2]);
        Geometry.Point farPointRay = new Geometry.Point(farPointWorld[0],farPointWorld[1],farPointWorld[2]);

        return new Geometry.Ray(nearPointRay,
                Geometry.vectorBetween(nearPointRay,farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }
}
