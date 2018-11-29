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

    int Mp4_TO_YUV(String input_path_str, String output_path_str);
}
