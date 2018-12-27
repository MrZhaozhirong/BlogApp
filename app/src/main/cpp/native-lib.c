#include <jni.h>
#include "log_common.h"

#include <unistd.h>
#include <pthread.h>
#include <assert.h>


jobject jMainActivity;
JavaVM *javaVM;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGW("%s\n", "JNI_OnLoad startup ...");
    javaVM = vm;
    JNIEnv *env = NULL;
    jint result;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) == JNI_OK) {
        LOGI("Catch JNI_VERSION_1_6\n");
        result = JNI_VERSION_1_6;
    }
    else if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) == JNI_OK) {
        LOGI("Catch JNI_VERSION_1_4\n");
        result = JNI_VERSION_1_4;
    }
    else {
        LOGI("Default JNI_VERSION_1_2\n");
        result = JNI_VERSION_1_2;
    }

    assert(env != NULL);
    // 动态注册native函数 ...
    return result;
}

void *pthread_run(void *arg) {
    JNIEnv *env = NULL;
    // (*javaVM)->AttachCurrentThread(javaVM,&env,NULL)
    // (*javaVM)->GetEnv(javaVM, (void **)&env, JNI_VERSION_1_6)
    if ( (*javaVM)->AttachCurrentThread(javaVM,&env,NULL) != JNI_OK) {
        LOGE("javaVM->Env Error!\n");
        pthread_exit((void *) -1);
    }

    assert(env != NULL);
    // 此env是新分配的，线程独立的，并不带自定义的包类。
    // 所以FindClass对系统的类是能作用，但是对于自定义的类(即我们自己开发的类)就没有包含在内。

    // System.UUID jclass 线程env只包含系统的默认包类型。
    jclass sys_uuid_cls = (*env)->FindClass(env, "java/util/UUID");
    jmethodID randomUUID_mid = (*env)->GetStaticMethodID(env, sys_uuid_cls, "randomUUID","()Ljava/util/UUID;");
    jmethodID toString_mid = (*env)->GetMethodID(env, sys_uuid_cls, "toString","()Ljava/lang/String;");
    jobject sys_uuid_jobj = (*env)->CallStaticObjectMethod(env, sys_uuid_cls, randomUUID_mid);
    jobject sys_uuid_jstr = (*env)->CallObjectMethod(env, sys_uuid_jobj, toString_mid);
    char* sys_uuid_cstr = (char *) (*env)->GetStringUTFChars(env, sys_uuid_jstr, NULL);
    LOGI("System.UUID : %s", sys_uuid_cstr);

    // 自定义的类型 可以通过引用线程共享的thiz对象，再GetObjectClass获取jclass
    jclass clazz = (*env)->GetObjectClass(env, jMainActivity);
    // 线程env不包含自定义的java类类型。
    //jclass clazz = (*env)->FindClass(env, "org/zzrblog/MainActivity");
    jmethodID getUuid_mid = (*env)->GetStaticMethodID(env, clazz, "getUuid","()Ljava/lang/String;");

    char *name = (char *) arg;
    for (int i = 0; i < 5; ++i) {
        jobject uuid_jstr = (*env)->CallStaticObjectMethod(env, clazz, getUuid_mid);
        char* uuid_cstr = (char *) (*env)->GetStringUTFChars(env, uuid_jstr, NULL);
        LOGI("%s, No:%d, uuid:%s", name, i, uuid_cstr);
        sleep(1);
    }

    (*env)->ReleaseStringUTFChars(env, sys_uuid_jstr, sys_uuid_cstr);
    (*env)->DeleteLocalRef(env, sys_uuid_jobj);
    (*javaVM)->DetachCurrentThread(javaVM);
    pthread_exit((void *) 0);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_MainActivity_nativeThreadEnvTest(JNIEnv *env, jobject thiz) {

    if(jMainActivity == NULL) {
        //获取全局引用
        jMainActivity = (*env)->NewGlobalRef(env, thiz);
    }

    pthread_t tid;
    pthread_create(&tid, NULL, pthread_run, (void *) "pthread1");

    //void* reval;
    //pthread_join(tid, &reval);
}
