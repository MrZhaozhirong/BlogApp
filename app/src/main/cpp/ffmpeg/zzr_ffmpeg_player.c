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
#include "include/libswresample/swresample.h"





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









#define MAX_AUDIO_FARME_SIZE 48000 * 2

typedef struct {
    jobject    audio_track;
    jmethodID   audio_track_play_mid;
    jmethodID   audio_track_write_mid;
} audio_track_fields;

audio_track_fields audioTrackCtx;

int createAudioTrackContext(JNIEnv *env, jobject instance, int out_sample_rate, int out_channel_nb)
{
    jclass player_class = (*env)->GetObjectClass(env, instance);
    //java.AudioTrack对象
    jmethodID create_audio_track_mid = (*env)->GetMethodID(env,player_class,"createAudioTrack","(II)Landroid/media/AudioTrack;");
    jobject audio_track = (*env)->CallObjectMethod(env, instance, create_audio_track_mid, out_sample_rate, out_channel_nb);
    if(audio_track!=NULL) {
        audioTrackCtx.audio_track = audio_track;
    } else {
        return -1;
    }

    //java.AudioTrack.play方法
    jclass audio_track_class = (*env)->GetObjectClass(env,audio_track);
    jmethodID audio_track_play_mid = (*env)->GetMethodID(env,audio_track_class,"play","()V");
    //(*env)->CallVoidMethod(env,audio_track,audio_track_play_mid);
    if(audio_track_play_mid!=NULL) {
        audioTrackCtx.audio_track_play_mid = audio_track_play_mid;
    } else {
        return -2;
    }

    //java.AudioTrack.write方法
    jmethodID audio_track_write_mid = (*env)->GetMethodID(env,audio_track_class,"write","([BII)I");
    //(*env)->CallIntMethod(env,audio_track,audio_track_write_mid, audioData, offsetInBytes, sizeInBytes);
    if(audio_track_write_mid!=NULL) {
        audioTrackCtx.audio_track_write_mid = audio_track_write_mid;
    } else {
        return -3;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_org_zzrblog_mp_ZzrFFPlayer_musicPlay(JNIEnv *env, jobject instance, jstring media_input_jstr) {
    const char *media_input_cstr = (*env)->GetStringUTFChars(env, media_input_jstr, 0);

    av_log_set_callback(custom_log);
    av_register_all();
    avcodec_register_all();
    avformat_network_init();

    AVFormatContext *pFormatContext = avformat_alloc_context();
    if(avformat_open_input(&pFormatContext, media_input_cstr, NULL, NULL) != 0){
        LOGE("%s","打开输入视频文件失败");
        return -1;
    }
    if(avformat_find_stream_info(pFormatContext, NULL) < 0){
        LOGE("%s","获取媒体信息失败");
        return -2;
    }
    int audio_stream_idx = -1;
    for(int i=0; i<pFormatContext->nb_streams; i++)
    {
        if(pFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            break;
        }
    }
    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[audio_stream_idx]->codecpar->codec_id);
    if(pCodec == NULL){
        LOGI("%s","无法获取解码器");
        return -3;
    }
    AVCodecContext * pCodecContext = avcodec_alloc_context3(pCodec);
    if(pCodecContext == NULL) {
        LOGE("%s","创建解码器对应的上下文失败.");
        return -4;
    }
    avcodec_parameters_to_context(pCodecContext, pFormatContext->streams[audio_stream_idx]->codecpar);
    if(avcodec_open2(pCodecContext, pCodec, NULL) < 0) {
        LOGE("%s","解码器无法打开");
        return -5;
    }





    AVPacket *packet = av_packet_alloc();
    AVFrame *frame = av_frame_alloc();
    //frame->16bit双声道 采样率44100 PCM采样格式
    SwrContext *swrCtx = swr_alloc();
    //  设置采样参数-------------start
    enum AVSampleFormat in_sample_fmt = pCodecContext->sample_fmt;
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    int in_sample_rate = pCodecContext->sample_rate;
    int out_sample_rate = 44100;
    uint64_t in_ch_layout = pCodecContext->channel_layout;
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    //  设置采样参数---------------end
    swr_alloc_set_opts(swrCtx,
                       out_ch_layout,out_sample_fmt,out_sample_rate,
                       in_ch_layout,in_sample_fmt,in_sample_rate,
                       0, NULL);
    swr_init(swrCtx);


    //16bit 44100 PCM 数据的实际内存空间。
    uint8_t *out_buffer = (uint8_t *)av_malloc(MAX_AUDIO_FARME_SIZE);
    //根据声道布局 获取 输出的声道个数
    int out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);

    createAudioTrackContext(env, instance, out_sample_rate, out_channel_nb);
    (*env)->CallVoidMethod(env,audioTrackCtx.audio_track,audioTrackCtx.audio_track_play_mid);

    int ret;
    while(av_read_frame(pFormatContext, packet) >= 0)
    {
        if(packet->stream_index == audio_stream_idx)
        {
            ret = avcodec_send_packet(pCodecContext, packet);
            if(ret < 0) {
                LOGE("avcodec_send_packet：%d\n", ret);
                continue;
            }
            while(ret >= 0) {
                ret = avcodec_receive_frame(pCodecContext, frame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                    LOGD("avcodec_receive_frame：%d\n", ret);
                    break;
                } else if (ret < 0) {
                    LOGW("avcodec_receive_frame：%d\n", AVERROR(ret));
                    goto end;  //end处进行资源释放等善后处理
                }

                if (ret >= 0)
                {
                    swr_convert(swrCtx, &out_buffer, MAX_AUDIO_FARME_SIZE, (const uint8_t **) frame->data, frame->nb_samples);
                    //获取sample的size
                    int out_buffer_size = av_samples_get_buffer_size(NULL, out_channel_nb,
                                                                     frame->nb_samples, out_sample_fmt, 1);

                }
            }
        }
        av_packet_unref(packet);
    }
    LOGD("媒体文件转换PCM结束\n");


end:
    av_free(out_buffer);
    swr_free(&swrCtx);
    av_free(out_buffer);
    av_frame_free(&frame);
    avcodec_close(pCodecContext);
    avcodec_free_context(&pCodecContext);
    avformat_close_input(&pFormatContext);
    avformat_free_context(pFormatContext);

    (*env)->ReleaseStringUTFChars(env, media_input_jstr, media_input_cstr);
    return 0;
}

