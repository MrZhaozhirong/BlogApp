#include <jni.h>
#include "log_common.h"

#include <unistd.h>
#include <pthread.h>
#include <assert.h>

JavaVM *javaVM;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
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
        LOGI("Catch JNI_VERSION_1_2\n");
        result = JNI_VERSION_1_2;
    }

    assert(env != NULL);
    // 动态注册native函数 ...
    return result;
}

void *pthread_run(void *arg) {
    JNIEnv *env = NULL;


    char *no = (char *) arg;
    int i;
    for (i = 0; i < 5; ++i) {
        LOGI("thread %s, i:%d", no, i);
        if (i == 4) {
            pthread_exit((void *) 1);
        }
        sleep(1);
    }

    pthread_exit((void *) 0);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_MainActivity_pThreadEnvTest(
        JNIEnv *env,
        jobject thiz) {

    pthread_t tid;
    pthread_create(&tid, NULL, pthread_run, (void *) "NO1");

    //void* reval;
    //pthread_join(tid, &reval);
}
