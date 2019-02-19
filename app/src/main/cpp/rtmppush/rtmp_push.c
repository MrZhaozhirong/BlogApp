#include <jni.h>
#include <malloc.h>
#include "../common/zzr_common.h"
#include "x264/include/x264.h"

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// 声明全局变量，在common.c的JNI_OnLoad定义
extern JavaVM * gJavaVM;

typedef struct _rtmp_push {
    int width;
    int height;
    int bitrate;
    int fps;
    int sampleRateInHz;
    int channel;

    x264_picture_t* pic_in;
    x264_t *x264_encoder;
    x264_picture_t* pic_out;
} RtmpPusher;

RtmpPusher* gRtmpPusher;
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// native method implementation ////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////


JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedAudioData
        (JNIEnv *env, jobject jobj, jbyteArray array, jint len)
{

}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_setAudioOptions(JNIEnv *env, jobject jobj,
                                                 jint sampleRateInHz, jint channel)
{
    if(gRtmpPusher == NULL) {
        gRtmpPusher = (RtmpPusher*)calloc(1, sizeof(RtmpPusher));
    }
    gRtmpPusher->sampleRateInHz = sampleRateInHz;
    gRtmpPusher->channel = channel;
}




JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedVideoData
        (JNIEnv *env, jobject jobj, jbyteArray array)
{
    if(gRtmpPusher == NULL )
        return;
    int array_len = (*env)->GetArrayLength(env, array);
    int option_len = gRtmpPusher->width * gRtmpPusher->height * 3/2;
    if(array_len < option_len)
        return;

    int y_len = gRtmpPusher->width * gRtmpPusher->height;
    int u_len = y_len / 4;
    int v_len = y_len / 4;

    //NV21->YUV420P
    jbyte* nv21_buffer = (*env)->GetByteArrayElements(env,array,NULL);
    jbyte* y = gRtmpPusher->pic_in->img.plane[0];
    jbyte* u = gRtmpPusher->pic_in->img.plane[1];
    jbyte* v = gRtmpPusher->pic_in->img.plane[2];
    memcpy(y, nv21_buffer, (size_t) y_len);
    for (int i = 0; i < u_len; i++) {
        *(v + i) = *(nv21_buffer + y_len + i * 2);
        *(u + i) = *(nv21_buffer + y_len + i * 2 + 1);
    }



    x264_nal_t *nal = NULL; //h264编码得到NALU数组
    int n_nal = -1; //NALU的个数
    //进行h264编码
    if(x264_encoder_encode(gRtmpPusher->x264_encoder,&nal, &n_nal,
                           gRtmpPusher->pic_in, gRtmpPusher->pic_out) < 0){
        LOGE("%s","编码失败");
        return;
    }
    //使用rtmp协议将h264编码的视频数据发送给流媒体服务器
    //帧分为关键帧和普通帧，为了提高画面的纠错率，关键帧应都包含SPS和PPS数据
    int sps_len , pps_len;
    unsigned char sps[100];
    unsigned char pps[100];
    memset(sps,0,100);
    memset(pps,0,100);

    //遍历NALU数组，根据NALU的类型判断
    for(int i=0; i < n_nal; i++){
        if(nal[i].i_type == NAL_SPS){
            //复制SPS数据
            sps_len = nal[i].i_payload - 4;
            memcpy(sps,nal[i].p_payload + 4, sps_len); //不复制四字节起始码
        }else if(nal[i].i_type == NAL_PPS){
            //复制PPS数据
            pps_len = nal[i].i_payload - 4;
            memcpy(pps,nal[i].p_payload + 4, pps_len); //不复制四字节起始码
            //发送序列信息
            //h264关键帧会包含SPS和PPS数据
            add_264_sequence_header(pps,sps,pps_len,sps_len);
        }else{
            //发送帧信息
            add_264_body(nal[i].p_payload, nal[i].i_payload);
        }
    }
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_setVideoOptions(JNIEnv *env, jobject jobj,
                                                        jint width, jint height, jint bitrate, jint fps)
{
    if(gRtmpPusher == NULL) {
        gRtmpPusher = (RtmpPusher*)calloc(1, sizeof(RtmpPusher));
    }
    gRtmpPusher->bitrate = bitrate;
    gRtmpPusher->fps = fps;
    gRtmpPusher->width = width;
    gRtmpPusher->height = height;
    // 参照源码example.c 的模板代码
    ////////////////////////////////////////////////////////////////
    x264_param_t param;
    //x264_param_default_preset设置   两个字符串所代表的参数集在源码目录的common/base.c当中
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    //编码输入的像素格式YUV420P
    param.i_csp = X264_CSP_I420; // X264_CSP_NV21
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
    x264_picture_alloc(gRtmpPusher->pic_in, param.i_csp, param.i_width, param.i_height);

    //打开编码器
    gRtmpPusher->x264_encoder = x264_encoder_open(&param);
    if(gRtmpPusher->x264_encoder){
        LOGI("打开x264编码器成功...");
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