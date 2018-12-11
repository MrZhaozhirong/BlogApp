#include <jni.h>
#include <stdio.h>
#include "../log_common.h"
#include "include/libavutil/log.h"
#include "include/libavformat/avformat.h"

JNIEnv *gJNIEnv;
jstring gInputPath;
jobject gSurface;

void custom_log(void *ptr, int level, const char* fmt, va_list vl){
    FILE *fp=fopen("/storage/emulated/0/av_log.txt","w+");
    if(fp){
        vfprintf(fp,fmt,vl);
        fflush(fp);
        fclose(fp);
    }
}


JNIEXPORT void JNICALL
Java_org_zzrblog_mp_ZzrFFPlayer_init(JNIEnv *env, jobject jobj, jstring input_jstr, jobject surface)
{
    gJNIEnv = env;
    gInputPath = (jstring) (*env)->NewGlobalRef(env, input_jstr); //创建输入的媒体资源的全局引用。
    gSurface = (*env)->NewGlobalRef(env, surface); //创建surface全局引用。

    // 0.FFmpeg 's av_log output
    av_log_set_callback(custom_log);
    // 1.注册组件
    av_register_all();
    avcodec_register_all();
    avformat_network_init();
}

JNIEXPORT void JNICALL
Java_org_zzrblog_mp_ZzrFFPlayer_release(JNIEnv *env, jobject jobj)
{
    if(gJNIEnv!=NULL)
    {
        (*gJNIEnv)->DeleteGlobalRef(env, gInputPath);
        (*gJNIEnv)->DeleteGlobalRef(env, gSurface);
    }
    gJNIEnv = NULL;
}




JNIEXPORT void JNICALL
Java_org_zzrblog_mp_ZzrFFPlayer_play(JNIEnv *env, jobject jobj)
{

}