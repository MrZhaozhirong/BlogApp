package org.zzrblog.blogapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Created by zzr on 2018/1/26.
 */

public class TextureHelper {
    private static final String TAG = "TextureHelper";


    public static int loadTexture(Context context,int resourceId) {
        final int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1, textureObjectIds, 0);

        if(textureObjectIds[0] == 0){
            Log.e(TAG,"Could not generate a new OpenGL texture object!");
            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   //指定需要的是原始数据，非压缩数据
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if(bitmap == null){
            Log.e(TAG, "Resource ID "+resourceId + "could not be decode");
            GLES20.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }

        //告诉OpenGL后面纹理调用应该是应用于哪个纹理对象
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);

        //设置缩小的时候（GL_TEXTURE_MIN_FILTER）使用mipmap三线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        //设置放大的时候（GL_TEXTURE_MAG_FILTER）使用双线程过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //Android设备y坐标是反向的，正常图显示到设备上是水平颠倒的，解决方案就是设置纹理包装，纹理T坐标（y）设置镜面重复
        //ball读取纹理的时候  t范围坐标取正常值+1
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        //快速生成mipmap贴图
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        //解除纹理操作的绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureObjectIds[0];
    }
}
