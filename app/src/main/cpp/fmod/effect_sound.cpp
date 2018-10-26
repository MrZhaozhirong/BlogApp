#include <jni.h>
#include "inc/fmod.hpp"
#include "common.h"

extern "C"
{


void Java_org_zzrblog_fmod_VoiceEffectUtils_play(JNIEnv *env, jobject thiz, jstring path_jstr, jint type )
{


    Common_Close();
}

} /* extern "C" */