//
// Created by zzr on 2019/1/4.
//
#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <unistd.h>
#include <pthread.h>
#include "../common/log_common.h"
#include "include/libavformat/avformat.h"
#include "include/libswresample/swresample.h"

//声明全局变量，在common.c的JNI_OnLoad定义
extern JavaVM * gJavaVM;

typedef struct _SyncPlayer {
    // 数据源 格式上下文
    AVFormatContext *input_format_ctx;
    // 流的总个数
    int num_streams;
    // 频视频流索引位置
    int video_stream_index;
    // 音视频流索引位置
    int audio_stream_index;
    // AVCodecContext 二级指针 动态数组
    // 长度为streams_num
    AVCodecContext * * input_codec_ctx;


    // SyncPlayer的java对象，需要建立引用 NewGlobalRef，记得DeleteGlobalRef
    jobject jinstance;
    // 视频渲染相关
    ANativeWindow* native_window;
    SwrContext *swr_ctx;
    // 音频播放相关
    enum AVSampleFormat in_sample_fmt;  //输入的采样格式
    enum AVSampleFormat out_sample_fmt; //输出采样格式16bit PCM
    int in_sample_rate;                 //输入采样率
    int out_sample_rate;                //输出采样率
    int out_channel_nb;                 //输出的声道个数
    // 音频播放对象 java对象，需要 NewGlobalRef，记得DeleteGlobalRef
    jobject* audio_track;
    jmethodID audio_track_play_mid;
    jmethodID audio_track_write_mid;


    pthread_t thread_AVPacket_distributor;
    int stop_thread_avpacket_distributor; //1为真（stop）0为假（continue）
} SyncPlayer;

SyncPlayer* mSyncPlayer;



// 根据 stream_idx流索引，初始化对应的AVCodecContext，并保存到SyncPlayer
int alloc_codec_context(SyncPlayer *player,int stream_idx)
{
    AVFormatContext *pFormatContext = player->input_format_ctx;

    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[stream_idx]->codecpar->codec_id);
    if(pCodec == NULL){
        LOGE("无法获取 %d 的解码器",stream_idx);
        return -1;
    }
    AVCodecContext *pCodecContext = avcodec_alloc_context3(pCodec);
    if(pCodecContext == NULL) {
        LOGE("创建 %d 解码器对应的上下文失败.", stream_idx);
        return -2;
    }
    int ret = avcodec_parameters_to_context(pCodecContext, pFormatContext->streams[stream_idx]->codecpar);
    if(ret < 0) {
        LOGE("avcodec_parameters_to_context：%d\n", AVERROR(ret));
        return -3;
    }
    if(avcodec_open2(pCodecContext, pCodec, NULL) < 0){
        LOGE("%s","解码器无法打开");
        return -4;
    }
    player->input_codec_ctx[stream_idx] = pCodecContext;
    return 0;
}

// 初始化音频相关的变量
int initAudioTrack(SyncPlayer* player, JNIEnv* env)
{
    AVCodecContext *audio_codec_ctx = player->input_codec_ctx[player->audio_stream_index];
    //重采样设置参数-------------start
    enum AVSampleFormat in_sample_fmt = audio_codec_ctx->sample_fmt;
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    int in_sample_rate = audio_codec_ctx->sample_rate;
    int out_sample_rate = in_sample_rate;
    uint64_t in_ch_layout = audio_codec_ctx->channel_layout;
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    //16bit 44100 PCM 统一音频采样格式与采样率
    SwrContext *swr_ctx = swr_alloc();
    swr_alloc_set_opts(swr_ctx,
                       out_ch_layout,out_sample_fmt,out_sample_rate,
                       in_ch_layout,in_sample_fmt,in_sample_rate,
                       0, NULL);
    int ret = swr_init(swr_ctx);
    if(ret < 0) {
        LOGE("swr_init：%d\n", AVERROR(ret));
        return -1;
    }
    int out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);
    //重采样设置参数-------------end
    // 保存设置
    player->in_sample_fmt = in_sample_fmt;
    player->out_sample_fmt = out_sample_fmt;
    player->in_sample_rate = in_sample_rate;
    player->out_sample_rate = out_sample_rate;
    player->out_channel_nb = out_channel_nb;
    player->swr_ctx = swr_ctx;



    //JNI AudioTrack-------------start
    jobject jthiz = mSyncPlayer->jinstance;
    jclass player_class = (*env)->GetObjectClass(env, jthiz);
    //AudioTrack对象
    jmethodID create_audio_track_mid = (*env)->GetMethodID(env,player_class,"createAudioTrack","(II)Landroid/media/AudioTrack;");
    jobject audio_track = (*env)->CallObjectMethod(env,jthiz,create_audio_track_mid,player->out_sample_rate,player->out_channel_nb);
    //调用AudioTrack.play方法
    jclass audio_track_class = (*env)->GetObjectClass(env,audio_track);
    jmethodID audio_track_play_mid = (*env)->GetMethodID(env,audio_track_class,"play","()V");
    player->audio_track_play_mid = audio_track_play_mid;
    //AudioTrack.write
    jmethodID audio_track_write_mid = (*env)->GetMethodID(env,audio_track_class,"write","([BII)I");
    player->audio_track_write_mid = audio_track_write_mid;
    //JNI AudioTrack-------------end
    player->audio_track = (*env)->NewGlobalRef(env,audio_track);
    return 0;
}


/**
 * 生产者线程：负责不断的读取视频文件中AVPacket，分别放入两个队列中
 */
void* AVPacket_distributor(void* arg)
{
    SyncPlayer* player = (SyncPlayer*)arg;
    AVPacket *packet = av_packet_alloc();
    AVFormatContext* pFormatContext = player->input_format_ctx;
    AVCodecContext* audioCodecCtx = player->input_codec_ctx[player->audio_stream_index];
    AVCodecContext* videoCodecCtx = player->input_codec_ctx[player->video_stream_index];
    int ret;

    while(av_read_frame(pFormatContext, packet) >= 0)
    {
        if(player->stop_thread_avpacket_distributor > 0) {
            av_packet_unref(packet);
            pthread_exit((void*) 1);
        }
        if(packet->stream_index == player->audio_stream_index)
        {
            ret = avcodec_send_packet(audioCodecCtx, packet);
            if(ret < 0) {
                LOGE("audioCodecCtx avcodec_send_packet：%d\n", ret);
                continue;
            }
        }
        if(packet->stream_index == player->video_stream_index)
        {
            ret = avcodec_send_packet(videoCodecCtx, packet);
            if(ret < 0) {
                LOGE("videoCodecCtx avcodec_send_packet：%d\n", ret);
                continue;
            }
        }
        av_packet_unref(packet);
    }
    return 0;
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// native method implementation //////// JNIEXPORT JNICALL
///////////////////////////////////////////////////////////////////////////////////////////////////////////
JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativeInit(JNIEnv *env, jobject instance)
{
    // 0.FFmpeg's av_log output
    av_log_set_callback(ffmpeg_custom_log);
    // 1.注册组件
    av_register_all();
    avcodec_register_all();
    avformat_network_init();

    mSyncPlayer = (SyncPlayer*)malloc(sizeof(SyncPlayer));
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativeRelease(JNIEnv *env, jobject instance)
{
    if(mSyncPlayer == NULL)
        return;
    if(mSyncPlayer->input_format_ctx == NULL){
        return;
    }
    (*env)->DeleteGlobalRef(env, mSyncPlayer->audio_track);
    for(int i=0; i<mSyncPlayer->num_streams; i++) {
        // 有可能出现为空，因为只保存了音视频的AVCodecContext，没有处理字幕流的
        // 但是空间还是按照num_streams的个数创建了
        AVCodecContext * pCodecContext = mSyncPlayer->input_codec_ctx[i];
        if(pCodecContext != NULL)
        {
            avcodec_close(pCodecContext);
            avcodec_free_context(&pCodecContext);
            pCodecContext = NULL; //防止野指针
        }
    }
    free(mSyncPlayer->input_codec_ctx);

    mSyncPlayer->stop_thread_avpacket_distributor = 1;
    pthread_join(mSyncPlayer->thread_AVPacket_distributor, NULL);

    avformat_close_input(&(mSyncPlayer->input_format_ctx));
    avformat_free_context(mSyncPlayer->input_format_ctx);
    mSyncPlayer->input_format_ctx = NULL;
    (*env)->DeleteGlobalRef(env, mSyncPlayer->jinstance);
    free(mSyncPlayer);
    mSyncPlayer = NULL;
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativePrepare(JNIEnv *env, jobject instance,
                                                jstring media_input_jstr, jobject jSurface)
{
    if(mSyncPlayer == NULL) {
        LOGE("%s","请调用函数：nativeInit");
        return;
    }
    const char *media_input_cstr = (*env)->GetStringUTFChars(env, media_input_jstr, 0);

    AVFormatContext *pFormatContext = avformat_alloc_context();
    // 打开输入视频文件
    if(avformat_open_input(&pFormatContext, media_input_cstr, NULL, NULL) != 0){
        LOGE("%s","打开输入视频文件失败");
        return;
    }
    // 获取视频信息
    if(avformat_find_stream_info(pFormatContext,NULL) < 0){
        LOGE("%s","获取视频信息失败");
        return;
    }

    int video_stream_idx = -1;
    int audio_stream_idx = -1;
    for(int i=0; i<pFormatContext->nb_streams; i++)
    {
        if(pFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_idx = i;
            break;
        }
        if(pFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            break;
        }
    }
    LOGD("VIDEO的索引位置：%d", video_stream_idx);
    LOGD("AUDIO的索引位置：%d", audio_stream_idx);

    mSyncPlayer->input_format_ctx = pFormatContext;
    mSyncPlayer->num_streams = pFormatContext->nb_streams;
    mSyncPlayer->audio_stream_index = audio_stream_idx;
    mSyncPlayer->video_stream_index = video_stream_idx;
    // 开辟nb_streams个空间，每个都是指针 (AVCodecContext* )
    mSyncPlayer->input_codec_ctx = calloc(pFormatContext->nb_streams, sizeof(AVCodecContext* ) );
    // 根据索引初始化对应的AVCodecContext，并放入mSyncPlayer.input_codec_ctx数组 对应的位置
    int ret ;
    ret = alloc_codec_context(mSyncPlayer, video_stream_idx);
    if(ret < 0) return;
    ret = alloc_codec_context(mSyncPlayer, audio_stream_idx);
    if(ret < 0) return;

    // SyncPlayer的java对象，需要建立引用 NewGlobalRef，记得DeleteGlobalRef
    mSyncPlayer->jinstance = (*env)->NewGlobalRef(env, instance);
    // 初始化视频渲染相关 ANativeWindow是NDK对象，不需要NewGlobalRef
    mSyncPlayer->native_window = ANativeWindow_fromSurface(env, jSurface);
    // 初始化音频播放相关 audio_track是java对象，需要NewGlobalRef，记得DeleteGlobalRef
    ret = initAudioTrack(mSyncPlayer, env);
    if(ret < 0) return;



    (*env)->ReleaseStringUTFChars(env, media_input_jstr, media_input_cstr);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativePlay(JNIEnv *env, jobject instance)
{
    if(mSyncPlayer == NULL) {
        LOGE("%s","请调用函数：nativeInit");
        return;
    }
    if(mSyncPlayer->input_format_ctx == NULL){
        LOGW("%s","请调用函数：nativePrepare");
        return;
    }

    mSyncPlayer->stop_thread_avpacket_distributor = 0;
    pthread_create(&(mSyncPlayer->thread_AVPacket_distributor),NULL, AVPacket_distributor, mSyncPlayer);
    usleep(1000); // 1000us = 1ms

}