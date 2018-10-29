#include <jni.h>
#include <android/log.h>
#include "inc/fmod.hpp"

#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"BlogApp",FORMAT,##__VA_ARGS__);
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"BlogApp",FORMAT,##__VA_ARGS__);
#define LOGW(FORMAT,...) __android_log_print(ANDROID_LOG_WARN,"BlogApp",FORMAT,##__VA_ARGS__);


//全局引用，可以跨越多个线程，在手动释放之前，一直有效
JNIEnv *gJNIEnv;
jstring gMediaPath;


extern "C"
{

void Java_org_zzrblog_fmod_VoiceEffectUtils_init(JNIEnv *env, jclass clazz )
{
    gJNIEnv = env;
    jstring obj = gJNIEnv->NewStringUTF("file:///android_asset/dfb.mp3");
    gMediaPath = (jstring) gJNIEnv->NewGlobalRef(obj);
}

void Java_org_zzrblog_fmod_VoiceEffectUtils_play(JNIEnv *env, jclass clazz, jint type )
{
    FMOD::System    *system;
    FMOD::Sound     *sound;

    //初始化 FMOD.System
    System_Create(&system);
    system->init(32, FMOD_INIT_NORMAL, NULL);

    //创建 FMOD.Sound
    const char* path_c_str = env->GetStringUTFChars(gMediaPath, NULL);
    LOGI("%s", path_c_str);
    system->createSound(path_c_str, FMOD_DEFAULT, NULL, &sound);


    //根据type 处理不同特效
    try {

    }catch (...){
        LOGW("%s","VoiceEffectUtils play 发生异常...");
        goto end;
    }


end:
    env->ReleaseStringUTFChars(gMediaPath,path_c_str);
    sound->release();
    system->close();
    system->release();
}

void Java_org_zzrblog_fmod_VoiceEffectUtils_release(JNIEnv *env, jclass clazz )
{
    gJNIEnv->DeleteGlobalRef(gMediaPath);
    gJNIEnv = NULL;
}

} /* extern "C" */