#include <jni.h>
#include "../common/zzr_common.h"
#include "x264/include/x264.h"

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// 声明全局变量，在common.c的JNI_OnLoad定义
extern JavaVM * gJavaVM;



//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// native method implementation ////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////


JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedVideoData
        (JNIEnv *env, jobject jobj, jbyteArray array)
{
    //视频数据转为YUV420P
}


JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedAudioData
        (JNIEnv *env, jobject jobj, jbyteArray array, jint len)
{

}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_setAudioOptions(JNIEnv *env, jobject jobj,
                                                 jint sampleRateInHz, jint channel)
{

}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_setVideoOptions(JNIEnv *env, jobject jobj,
                                                        jint width, jint height, jint bitrate, jint fps)
{
    // 参照源码example.c 的模板代码
    ////////////////////////////////////////////
    x264_param_t param;
    //x264_param_default_preset设置   两个字符串所代表的参数集在源码目录的common/base.c当中
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    //编码输入的像素格式YUV420P
    param.i_csp = X264_CSP_I420;
    param.i_width  = width;
    param.i_height = height;
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    //恒定码率，会尽量控制在固定码率
    param.rc.i_rc_method = X264_RC_CRF;
    param.rc.i_bitrate = bitrate / 1000; //码率(比特率) 单位Kbps
    param.rc.i_vbv_max_bitrate = (int) (bitrate / 1000 * 1.2); //瞬时最大码率

    //码率控制不通过timebase和timestamp，而是fps
    param.b_vfr_input = 0;
    param.i_fps_num = (uint32_t) fps; //* 帧率分子
    param.i_fps_den = 1; //* 帧率分母
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
    param.i_threads = 1;//并行编码线程数量，0默认为多线程

    //是否把SPS和PPS放入每一个关键帧
    //置为1表示每个I帧都重复带SPS/PPS头，为了提高图像的纠错能力
    param.b_repeat_headers = 1;
    //设置Level级别
    param.i_level_idc = 51;
    //设置Profile档次
    //baseline级别，没有B帧   字符串所代表的参数集在源码目录的common/base.c当中
    x264_param_apply_profile(&param, "baseline");

    //x264_picture_t（输入图像）初始化
    x264_picture_t pic_in;
    x264_picture_alloc(&pic_in, param.i_csp, param.i_width, param.i_height);

    //打开编码器
    x264_t *x264_encoder = x264_encoder_open(&param);
    if(x264_encoder){
        LOGI("打开编码器成功...");
    }
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_startPush(JNIEnv *env, jobject jobj, jstring url_jstr)
{

}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_stopPush(JNIEnv *env, jobject jobj)
{

}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_release(JNIEnv *env, jobject jobj)
{

}