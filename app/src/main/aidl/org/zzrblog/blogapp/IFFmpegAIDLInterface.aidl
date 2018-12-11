// ITestAIDLInterface.aidl
package org.zzrblog.blogapp;

// Declare any non-default types here with import statements

interface IFFmpegAIDLInterface {


    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    String getName();

    int Mp4_TO_YUV(String input_mp4_str, String output_yuv_str, String output_h264_str);
}
