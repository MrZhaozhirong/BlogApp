//
// Created by nicky on 2018/11/29.
//
#include <android/log.h>

#ifndef BLOGAPP_ZZR_BLOG_COMMON_H
#define BLOGAPP_ZZR_BLOG_COMMON_H

#define LOGD(FORMAT,...) __android_log_print(ANDROID_LOG_DEBUG,"ZzrBlogApp",FORMAT,##__VA_ARGS__);
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"ZzrBlogApp",FORMAT,##__VA_ARGS__);
#define LOGW(FORMAT,...) __android_log_print(ANDROID_LOG_WARN,"ZzrBlogApp",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"ZzrBlogApp",FORMAT,##__VA_ARGS__);


#endif //BLOGAPP_ZZR_BLOG_COMMON_H
