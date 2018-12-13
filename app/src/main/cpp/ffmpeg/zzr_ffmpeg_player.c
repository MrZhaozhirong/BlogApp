#include <jni.h>
#include <stdio.h>
#include "../log_common.h"
#include "include/libavutil/log.h"
#include "include/libavformat/avformat.h"
#include "include/libswscale/swscale.h"
#include "include/libavutil/imgutils.h"
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <unistd.h>
#include "../libyuv/libyuv.h"

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
    gInputPath = (jstring) (*gJNIEnv)->NewGlobalRef(gJNIEnv, input_jstr); //创建输入的媒体资源的全局引用。
    gSurface = (*gJNIEnv)->NewGlobalRef(gJNIEnv, surface); //创建surface全局引用。

    // 0.FFmpeg's av_log output
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
        (*gJNIEnv)->DeleteGlobalRef(gJNIEnv, gInputPath);
        (*gJNIEnv)->DeleteGlobalRef(gJNIEnv, gSurface);
    }
    gJNIEnv = NULL;
}




JNIEXPORT jint JNICALL
Java_org_zzrblog_mp_ZzrFFPlayer_play(JNIEnv *env, jobject jobj)
{
    const char *input_cstr = (*env)->GetStringUTFChars(env, gInputPath, 0);


    AVFormatContext *pFormatContext = avformat_alloc_context();
    // 打开输入视频文件
    if(avformat_open_input(&pFormatContext, input_cstr, NULL, NULL) != 0){
        LOGE("%s","打开输入视频文件失败");
        return -1;
    }
    // 获取视频信息
    if(avformat_find_stream_info(pFormatContext,NULL) < 0){
        LOGE("%s","获取视频信息失败");
        return -2;
    }

    int video_stream_idx = -1;
    for(int i=0; i<pFormatContext->nb_streams; i++)
    {
        //根据类型判断，是否是视频流
        if(pFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_idx = i;
            break;
        }
    }
    LOGD("VIDEO的索引位置：%d", video_stream_idx);
    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[video_stream_idx]->codecpar->codec_id);
    if(pCodec == NULL) {
        LOGE("%s","解码器创建失败.");
        return -3;
    }

    AVCodecContext * pCodecContext = avcodec_alloc_context3(pCodec);
    if(pCodecContext == NULL) {
        LOGE("%s","创建解码器对应的上下文失败.");
        return -4;
    }
    avcodec_parameters_to_context(pCodecContext, pFormatContext->streams[video_stream_idx]->codecpar);
    if(avcodec_open2(pCodecContext, pCodec, NULL) < 0){
        LOGE("%s","解码器无法打开");
        return -5;
    } else {
        LOGI("设置解码器解码格式pix_fmt：%d", pCodecContext->pix_fmt);
    }



    //编码数据
    AVPacket *packet = av_packet_alloc();
    //像素数据（解码数据）
    AVFrame *yuv_frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();

    //用于 缩放
    struct SwsContext *sws_ctx = sws_getContext(
            pCodecContext->width, pCodecContext->height, pCodecContext->pix_fmt,
            pCodecContext->width, pCodecContext->height, AV_PIX_FMT_YUV420P,
            SWS_BILINEAR, NULL, NULL, NULL);

    // 准备native绘制的窗体
    ANativeWindow* nativeWindow = ANativeWindow_fromSurface(gJNIEnv, gSurface);
    // 设置缓冲区的属性（宽、高、像素格式）
    ANativeWindow_setBuffersGeometry(nativeWindow, pCodecContext->width, pCodecContext->height, WINDOW_FORMAT_RGBA_8888);
    // 绘制时的缓冲区
    ANativeWindow_Buffer nativeWinBuffer;

    int ret;
    while(av_read_frame(pFormatContext, packet) >= 0)
    {
        if(packet->stream_index == video_stream_idx)
        {
            //AVPacket->AVFrame
            ret = avcodec_send_packet(pCodecContext, packet);
            if(ret < 0){
                LOGE("avcodec_send_packet：%d\n", ret);
                continue;
            }
            while(ret >= 0) {
                ret = avcodec_receive_frame(pCodecContext, yuv_frame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF){
                    LOGD("avcodec_receive_frame：%d\n", ret);
                    break;
                }else if (ret < 0) {
                    LOGW("avcodec_receive_frame：%d\n", AVERROR(ret));
                    goto end;  //end处进行资源释放等善后处理
                }

                if (ret >= 0)
                {
                    ANativeWindow_lock(nativeWindow, &nativeWinBuffer, NULL);
                    // 上锁并关联 ANativeWindow + ANativeWindow_Buffer

                    av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, nativeWinBuffer.bits,
                                         AV_PIX_FMT_RGBA, pCodecContext->width, pCodecContext->height, 1 );
                    // rgb.AVFrame对象 关联 ANativeWindow_Buffer的真实内存空间actual bits.

                    I420ToARGB(yuv_frame->data[0], yuv_frame->linesize[0],
                               yuv_frame->data[2], yuv_frame->linesize[2],
                               yuv_frame->data[1], yuv_frame->linesize[1],
                               rgb_frame->data[0], rgb_frame->linesize[0],
                               pCodecContext->width, pCodecContext->height);
                    // yuv.AVFrame 转 rgb.AVFrame

                    ANativeWindow_unlockAndPost(nativeWindow);
                    // 释放锁并 swap交换显示内存到屏幕上。
                    usleep(100 * 16);
                }
            }
        }
        av_packet_unref(packet);
    }

end:
    ANativeWindow_release(nativeWindow);
    av_frame_free(&yuv_frame);
    av_frame_free(&rgb_frame);
    avcodec_close(pCodecContext);
    avcodec_free_context(&pCodecContext);
    avformat_close_input(&pFormatContext);
    avformat_free_context(pFormatContext);
    (*gJNIEnv)->ReleaseStringUTFChars(gJNIEnv, gInputPath, input_cstr);
    return 0;
}

int isEmptyStr(const char * str){
    if(strlen(str)==0) return -1;
    if(*str == '\0') return -1;
    return 0;
}