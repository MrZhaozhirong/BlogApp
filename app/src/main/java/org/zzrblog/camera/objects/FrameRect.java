package org.zzrblog.camera.objects;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by zzr on 2018/5/21.
 */

public class FrameRect {

    public static final int SIZE_OF_FLOAT = 4;
    /**
     * 简单的等边三角形（每边1个）。以（0，0）为中心。
     */
    private static final float TRIANGLE_COORDS[] = {
            0.0f,  0.577350269f,   // 0 top
            -0.5f, -0.288675135f,   // 1 bottom left
            0.5f, -0.288675135f    // 2 bottom right
    };
    private static final float TRIANGLE_TEX_COORDS[] = {
            0.5f, 0.0f,     // 0 top center
            0.0f, 1.0f,     // 1 bottom left
            1.0f, 1.0f,     // 2 bottom right
    };

    /**
     * 简单的正方形，指定为三角形条带。
     * 正方形以（0，0）为中心，其大小为1x1。
     *
     * 两个三角形带 0-1-2 1-2-3 (逆时针绕组).
     */
    private static final float RECTANGLE_COORDS[] = {
            -0.5f, -0.5f,   // 0 bottom left
            0.5f, -0.5f,   // 1 bottom right
            -0.5f,  0.5f,   // 2 top left
            0.5f,  0.5f,   // 3 top right
    };
    private static final float RECTANGLE_TEX_COORDS[] = {
            0.0f, 1.0f,     // 0 bottom left
            1.0f, 1.0f,     // 1 bottom right
            0.0f, 0.0f,     // 2 top left
            1.0f, 0.0f      // 3 top right
    };

    /**
     * 一个“完整”的正方形，从两维延伸到-1到1。
     * 当 模型/视图/投影矩阵是都为单位矩阵的时候，这将完全覆盖视口。
     * 纹理坐标相对于矩形是y反的。
     * (This seems to work out right with external textures from SurfaceTexture.)
     */
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
    };
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mCoordsPerVertex;
    private int mVertexCount;
    private int mVertexStride;
    private int mTexCoordStride;

    public FrameRect() {
        mVertexArray = createFloatBuffer(FULL_RECTANGLE_COORDS);
        mTexCoordArray = createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
        mCoordsPerVertex = 2;
        mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex; // 4
        mTexCoordStride = 2 * SIZE_OF_FLOAT;
        mVertexStride = 2 * SIZE_OF_FLOAT;
    }

    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    public int getVertexCount() {
        return mVertexCount;
    }

    public int getVertexStride() {
        return mVertexStride;
    }

    public int getTexCoordStride() {
        return mTexCoordStride;
    }

    public int getCoordsPerVertex() {
        return mCoordsPerVertex;
    }

    private FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZE_OF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }
}
