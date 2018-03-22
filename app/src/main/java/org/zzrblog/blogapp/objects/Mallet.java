package org.zzrblog.blogapp.objects;


import android.opengl.Matrix;

import org.zzrblog.blogapp.data.VertexArray;
import org.zzrblog.blogapp.program.ColorShaderProgram;
import org.zzrblog.blogapp.utils.Constants;
import org.zzrblog.blogapp.utils.Geometry;
import org.zzrblog.blogapp.utils.ObjectBuilder;

import java.util.List;

/**
 * Created by ZZR on 2017/2/10.
 */

public class Mallet {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT+COLOR_COMPONENT_COUNT)* Constants.BYTES_PER_FLOAT;

    public float[] modelMatrix = new float[16];

    //private static final float[] VERTEX_DATA = {
    //        // 两个木槌的质点位置
    //        //x,    y,    R, G, B
    //        0f,   -0.4f,  1f,1f,1f,
    //        0f,    0.4f,  1f,1f,1f,
    //};

    //public Mallet(){
    //    vertexArray = new VertexArray(VERTEX_DATA);
    //}

    private final VertexArray vertexArray;
    private List<ObjectBuilder.DrawCommand> drawList;

    public final float radius;
    public final float height;

    public Geometry.Point position ;
    public Geometry.Point previousPosition;
    public volatile boolean isPressed;

    public Mallet(float radius, float height, int numPointsAroundMallet){
        ObjectBuilder.GeneratedData mallet = ObjectBuilder.createMallet(
                new Geometry.Point(0f, 0f, 0f),
                radius, height, numPointsAroundMallet);
        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(mallet.vertexData);
        drawList = mallet.drawCommandlist;

        Matrix.setIdentityM(modelMatrix,0);
    }

    public void bindData(ColorShaderProgram shaderProgram){
        vertexArray.setVertexAttributePointer(
                shaderProgram.aPositionLocation,
                POSITION_COMPONENT_COUNT, 0, 0
        );

        //vertexArray.setVertexAttributePointer(
        //        shaderProgram.aPositionLocation,
        //        POSITION_COMPONENT_COUNT,
        //        STRIDE,
        //        0);

        //vertexArray.setVertexAttributePointer(
        //        shaderProgram.aColorLocation,
        //        COLOR_COMPONENT_COUNT,
        //        STRIDE,
        //        POSITION_COMPONENT_COUNT
        //);
    }

    public void draw(){
        //GLES20.glDrawArrays(GLES20.GL_POINTS, 0,2);
        for (ObjectBuilder.DrawCommand command : drawList) {
            command.draw();
        }
    }
}
