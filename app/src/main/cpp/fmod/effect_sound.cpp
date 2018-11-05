#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include "inc/fmod.hpp"

#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"BlogApp",FORMAT,##__VA_ARGS__);
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"BlogApp",FORMAT,##__VA_ARGS__);
#define LOGW(FORMAT,...) __android_log_print(ANDROID_LOG_WARN,"BlogApp",FORMAT,##__VA_ARGS__);

#define MODE_NORMAL 0
#define MODE_LUOLI 1
#define MODE_DASHU 2
#define MODE_JINGSONG 3
#define MODE_GAOGUAI 4
#define MODE_KONGLING 5
//全局引用，可以跨越多个线程，在手动释放之前，一直有效
JNIEnv *gJNIEnv;
jstring gMediaPath;


extern "C"
{

void Java_org_zzrblog_fmod_VoiceEffectUtils_init(JNIEnv *env, jclass clazz )
{
    gJNIEnv = env;
    jstring obj = gJNIEnv->NewStringUTF("file:///android_asset/dfb.mp3");
    gMediaPath = (jstring) gJNIEnv->NewGlobalRef(obj); //创建cpp内全局引用。
}

void Java_org_zzrblog_fmod_VoiceEffectUtils_release(JNIEnv *env, jclass clazz )
{
    if(gJNIEnv!=NULL)
    {
        gJNIEnv->DeleteGlobalRef(gMediaPath);
    }
    gJNIEnv = NULL;
}

void Java_org_zzrblog_fmod_VoiceEffectUtils_play(JNIEnv *env, jclass clazz, jint type )
{
    FMOD::System    *system;
    void            *extradriverdata = 0;
    FMOD::Sound     *sound;
    FMOD::Channel   *channel;
    bool            playing = true; //判断音频是否还在播放.
    FMOD::DSP       *dsp;


    //初始化 FMOD.System
    System_Create(&system);
    system->init(32, FMOD_INIT_NORMAL, extradriverdata);

    //创建 FMOD.Sound
    const char* path_c_str = env->GetStringUTFChars(gMediaPath, NULL);
    LOGD("%s", path_c_str);
    system->createSound(path_c_str, FMOD_DEFAULT, NULL, &sound);

    //根据type 处理不同特效
    try {
        switch (type)
        {
            case MODE_NORMAL:{
                //触发音频播放
                system->playSound(sound, 0, false, &channel);
            }break;
            case MODE_LUOLI:{
                //DSP digital signal process
                //FMOD_DSP_TYPE_PITCHSHIFT dsp，fmod中预定义好的音效，提升或者降低音调用的一种音效
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                //设置音调的参数
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 2.5);
                //触发音频播放
                system->playSound(sound, 0, false, &channel);
                //添加到channel
                channel->addDSP(0,dsp);
            }break;
            case MODE_JINGSONG:{
                //惊悚
                system->createDSPByType(FMOD_DSP_TYPE_TREMOLO,&dsp);
                dsp->setParameterFloat(FMOD_DSP_TREMOLO_SKEW, 0.5);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0,dsp);
            }break;
            case MODE_DASHU:{
                //大叔
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT,&dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH,0.8);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0,dsp);
            }break;
            case MODE_GAOGUAI:{
                //搞怪 提高说话的速度
                float frequency = 0;
                system->playSound(sound, 0, false, &channel);
                channel->getFrequency(&frequency);
                frequency = frequency * 1.6f;
                channel->setFrequency(frequency);
            }break;
            case MODE_KONGLING:{
                //空灵
                system->createDSPByType(FMOD_DSP_TYPE_ECHO,&dsp);
                dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY,300);
                dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK,20);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0,dsp);
            }break;
            default:
                break;
        }
    } catch (...) {
        LOGW("%s","VoiceEffectUtils play 发生异常...");
        goto end;
    }
    //update一下FMOD的各种状态位。
    system->update();
    //每秒钟判断下是否在播放
    while(playing) {
        channel->isPlaying(&playing);
        usleep(1000 * 1000); //单位是微秒
    }


end:
    LOGI("%s","VoiceEffectUtils play end ...");
    env->ReleaseStringUTFChars(gMediaPath,path_c_str);
    sound->release();
    system->close();
    system->release();
}



} /* extern "C" */