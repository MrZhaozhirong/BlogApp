package org.zzrblog.blogapp.objects;

import android.opengl.Matrix;

import org.zzrblog.blogapp.data.VertexArray;
import org.zzrblog.blogapp.program.ColorShaderProgram;
import org.zzrblog.blogapp.utils.Geometry;
import org.zzrblog.blogapp.utils.ObjectBuilder;

import java.util.List;

/**
 * Created by zzr on 2018/2/8.
 */

public class Puck {
    private static final int POSITION_COMPONENT_COUNT = 3;

    public float[] modelMatrix = new float[16];

    public final float radius, height;

    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;

    public Geometry.Vector speedVector;
    public Geometry.Point position;

    public Puck(float radius, float height, int numPointsAroundPuck) {
        ObjectBuilder.GeneratedData puck = ObjectBuilder.createPuck(
                new Geometry.Cylinder(new Geometry.Point(0f, 0f, 0f), radius, height),
                numPointsAroundPuck
        );

        vertexArray = new VertexArray(puck.vertexData);
        drawList = puck.drawCommandlist;

        this.radius = radius;
        this.height = height;

        Matrix.setIdentityM(modelMatrix,0);
    }

    public void bindData(ColorShaderProgram shaderProgram) {
        vertexArray.setVertexAttributePointer(
                shaderProgram.aPositionLocation,
                POSITION_COMPONENT_COUNT,
                0, 0
        );
    }

    public void draw() {
        for(ObjectBuilder.DrawCommand command : drawList) {
            command.draw();
        }
    }
}
