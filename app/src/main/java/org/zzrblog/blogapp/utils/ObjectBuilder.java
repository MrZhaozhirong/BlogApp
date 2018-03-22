package org.zzrblog.blogapp.utils;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zzr on 2018/2/7.
 */

public class ObjectBuilder {
    private static final int FLOATS_PER_VERTEX = 3;
    private final float[] vertexData;
    private int offset = 0;
    private final List<DrawCommand> drawList = new ArrayList<DrawCommand>();

    private ObjectBuilder(int sizeInVertices){
        vertexData = new float[sizeInVertices * FLOATS_PER_VERTEX];
    }

    private void createCircle(Geometry.Circle circle, int numPoints){
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCircleInVertices(numPoints);
        // 圆心点
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;

        for(int i = 0; i<= numPoints; i++) {
            float angleInRadians =
                    ((float)i / (float)numPoints)
                    * ((float)Math.PI * 2f);

            vertexData[offset++] = circle.center.x + circle.radius * (float)Math.cos(angleInRadians);
            vertexData[offset++] = circle.center.y;
            vertexData[offset++] = circle.center.z + circle.radius * (float)Math.sin(angleInRadians);
        }
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, startVertex, numVertices);
            }
        });
    }

    private void createCylinder(Geometry.Cylinder cylinder, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCylinderInVertices(numPoints);

        final float yStart = cylinder.center.y - (cylinder.height / 2);
        final float yEnd = cylinder.center.y + (cylinder.height / 2);
        for( int i = 0; i <= numPoints; i++) {
            float angleInRadians =
                    ((float)i / (float)numPoints)
                            * ((float)Math.PI * 2f);

            float xPosition = cylinder.center.x + cylinder.radius * (float)Math.cos(angleInRadians);
            float zPosition = cylinder.center.z + cylinder.radius * (float)Math.sin(angleInRadians);

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yStart;
            vertexData[offset++] = zPosition;

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yEnd;
            vertexData[offset++] = zPosition;
        }
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startVertex, numVertices);
            }
        });
    }

    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }


    public static GeneratedData createPuck(Geometry.Cylinder puck, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) +
                sizeOfCylinderInVertices(numPoints);

        ObjectBuilder builder = new ObjectBuilder(size);

        Geometry.Circle puckTop = new Geometry.Circle(
                puck.center.translateY(puck.height / 2),
                puck.radius );

        builder.createCircle(puckTop, numPoints);
        builder.createCylinder(puck, numPoints);

        return builder.build();
    }

    public static GeneratedData createMallet(Geometry.Point center, float radius, float height, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) * 2
                + sizeOfCylinderInVertices(numPoints) * 2;
        ObjectBuilder builder = new ObjectBuilder(size);

        // 底座部分
        float baseHeight = height * 0.25f;
        Geometry.Circle baseCircle = new Geometry.Circle(
                center.translateY(-baseHeight),
                radius
        );
        Geometry.Cylinder baseCylinder = new Geometry.Cylinder(
                baseCircle.center.translateY(-baseHeight / 2f),
                radius,
                baseHeight
        );
        builder.createCircle(baseCircle, numPoints);
        builder.createCylinder(baseCylinder, numPoints);
        // 上半部分
        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;
        Geometry.Circle handleCircle = new Geometry.Circle(
                center.translateY(height * 0.5f),
                handleRadius
        );
        Geometry.Cylinder handleCylinder = new Geometry.Cylinder(
                handleCircle.center.translateY(-handleHeight / 2f),
                handleRadius,
                handleHeight
        );
        builder.createCircle(handleCircle, numPoints);
        builder.createCylinder(handleCylinder, numPoints);

        return builder.build();
    }

    // 图形绘制指令
    public interface DrawCommand{
        void draw();
    }
    // 构建一个圆形所需的顶点数
    private static int sizeOfCircleInVertices(int numPoints){
        return 1 + (numPoints + 1);
    }
    // 构建一个圆柱侧面所需顶点数
    private static int sizeOfCylinderInVertices(int numPoints){
        return (numPoints + 1) * 2;
    }
    // 指令 数据 包装类
    public static class GeneratedData{
        public final List<DrawCommand> drawCommandlist;
        public final float[] vertexData;
        GeneratedData(float[] data, List<DrawCommand> drawList){
            this.drawCommandlist = drawList;
            this.vertexData = data;
        }
    }
}
