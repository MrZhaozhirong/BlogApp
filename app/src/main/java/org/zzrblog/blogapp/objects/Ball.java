package org.zzrblog.blogapp.objects;

import android.content.Context;

import org.zzrblog.blogapp.data.VertexBuffer;
import org.zzrblog.blogapp.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * Created by zzr on 2018/3/23.
 */

public class Ball {

    public Ball(Context context){

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
                // 左上角 0
                float x0 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.cos(Math
                        .toRadians(hAngle)));
                float y0 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.sin(Math
                        .toRadians(hAngle)));
                float z0 = (float) (radius * Math.cos(Math.toRadians(vAngle)));
                vertexList.add(x0);
                vertexList.add(y0);
                vertexList.add(z0);
                // 右上角 1
                float x1 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.cos(Math
                        .toRadians(hAngle + angleSpan)));
                float y1 = (float) (radius * Math.sin(Math.toRadians(vAngle)) * Math.sin(Math
                        .toRadians(hAngle + angleSpan)));
                float z1 = (float) (radius * Math.cos(Math.toRadians(vAngle)));
                vertexList.add(x1);
                vertexList.add(y1);
                vertexList.add(z1);
                // 右下角 2
                float x2 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .cos(Math.toRadians(hAngle + angleSpan)));
                float y2 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .sin(Math.toRadians(hAngle + angleSpan)));
                float z2 = (float) (radius * Math.cos(Math.toRadians(vAngle + angleSpan)));
                vertexList.add(x2);
                vertexList.add(y2);
                vertexList.add(z2);
                // 左下角 3
                float x3 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .cos(Math.toRadians(hAngle)));
                float y3 = (float) (radius * Math.sin(Math.toRadians(vAngle + angleSpan)) * Math
                        .sin(Math.toRadians(hAngle)));
                float z3 = (float) (radius * Math.cos(Math.toRadians(vAngle + angleSpan)));
                vertexList.add(x3);
                vertexList.add(y3);
                vertexList.add(z3);

                indexList.add((short)(offset + 0));
                indexList.add((short)(offset + 3));
                indexList.add((short)(offset + 2));
                indexList.add((short)(offset + 0));
                indexList.add((short)(offset + 2));
                indexList.add((short)(offset + 1));

                offset += 4; // 4个顶点的偏移
            }
        }

        float[] data_vertex = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            data_vertex[i] = vertexList.get(i);
        }
        VertexBuffer vertexBuffer = new VertexBuffer(data_vertex);


        short[] data_index = new short[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            data_index[i] = indexList.get(i);
        }
        ShortBuffer indexArray = ByteBuffer.allocateDirect(indexList.size() * Constants.BYTES_PER_SHORT).asShortBuffer();
        indexArray.put(data_index).position(0);
    }


}
