//
// Created by zzr on 2019/1/4.
//
#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <unistd.h>
#include <pthread.h>
#include "include/libavformat/avformat.h"
#include "include/libswresample/swresample.h"
#include "include/libswscale/swscale.h"
#include "include/libavutil/imgutils.h"
#include "../libyuv/libyuv/convert_from.h"

#include "../common/zzr_common.h"
#include "AVPacket_buffer.h"

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// 声明全局变量，在common.c的JNI_OnLoad定义
extern JavaVM * gJavaVM;

#define MAX_AUDIO_FRAME_SIZE 44100*2

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

    pthread_t thread_avpacket_distributor;
    int stop_thread_avpacket_distributor;   //1为真（stop）0为假（continue）

    pthread_t thread_video_decoder;
    int stop_thread_video_decoder;
    AV_PACKET_BUFFER* video_avpacket_buffer;

    pthread_t thread_audio_decoder;
    int stop_thread_audio_decoder;
    AV_PACKET_BUFFER* audio_avpacket_buffer;

} SyncPlayer;




// avpacket_distributor：负责不断的读取视频文件中AVPacket，分别放入对应的解码器
void* avpacket_distributor(void* arg)
{
    SyncPlayer *player = (SyncPlayer *) arg;
    AVFormatContext *pFormatContext = player->input_format_ctx;
    //AVPacket* packet = av_packet_alloc();
    // 不用堆内存空间，因为线程创建的堆内存通过memcopy复制自定义的AVPacket_buffer当中，不高效。
    AVPacket packet; // 栈内存空间
    AVPacket *pkt = &packet; // 指向栈内存空间的指针
    int video_frame_count = 0;
    int audio_frame_count = 0;
    while (av_read_frame(pFormatContext, pkt) >= 0)
    {
        if(player->stop_thread_avpacket_distributor != 0)
            break;
        if (pkt->stream_index == player->video_stream_index)
        {
            AV_PACKET_BUFFER *video_buffer = player->video_avpacket_buffer;
            pthread_mutex_lock(&video_buffer->mutex);
            AVPacket *video_avpacket_buffer_data = get_write_packet(video_buffer);
            //buffer内部堆空间 = 当前栈空间数据，间接赋值。
            *video_avpacket_buffer_data = packet;
            pthread_mutex_unlock(&video_buffer->mutex);
            video_frame_count++;
        }
        if (pkt->stream_index == player->audio_stream_index)
        {
            AV_PACKET_BUFFER *audio_buffer = player->audio_avpacket_buffer;
            pthread_mutex_lock(&audio_buffer->mutex);
            AVPacket *audio_avpacket_buffer_data = get_write_packet(audio_buffer);
            *audio_avpacket_buffer_data = packet;
            pthread_mutex_unlock(&audio_buffer->mutex);
            audio_frame_count++;
        }
    }
    //av_packet_unref(packet);
    // 不需要在此解引用，应当在解码线程使用之后。
    LOGI("video_frame_count：%d", video_frame_count);
    LOGI("audio_frame_count：%d", audio_frame_count);
    // 分发线程结束，也就是说AVPacket写操作也结束了。
    // 我们把AVPacketBuffer->write_current_position+1取反 表示写入已经结束。
    // 解码线程读取操作进行判断，以便能正确退出线程。
    player->video_avpacket_buffer->write_current_position = -(player->video_avpacket_buffer->write_current_position+1);
    player->audio_avpacket_buffer->write_current_position = -(player->audio_avpacket_buffer->write_current_position+1);
    LOGI("thread_avpacket_distributor exit ...\n");
    return 0;
}

void* audio_avframe_decoder(void* arg)
{
    JNIEnv *env = NULL;
    if ( (*gJavaVM)->AttachCurrentThread(gJavaVM, &env,NULL) != JNI_OK) {
        LOGE("gJavaVM->Env Error!\n");
        pthread_exit((void *) -1);
    }

    SyncPlayer* player = (SyncPlayer*)arg;
    AVCodecContext* audioCodecCtx = player->input_codec_ctx[player->audio_stream_index];
    AV_PACKET_BUFFER* audioAVPacketButter = player->audio_avpacket_buffer;

    AVFrame *frame = av_frame_alloc();
    //16bit 44100 PCM 数据的实际内存空间。
    uint8_t *out_buffer = (uint8_t *)av_malloc(MAX_AUDIO_FRAME_SIZE);
    // AudioTrack.play
    (*env)->CallVoidMethod(env, player->audio_track, player->audio_track_play_mid);

    int ret;
    while(player->stop_thread_audio_decoder == 0)
    {
        pthread_mutex_lock(&audioAVPacketButter->mutex);
        AVPacket* packet = get_read_packet(audioAVPacketButter);
        pthread_mutex_unlock(&audioAVPacketButter->mutex);
        //AVPacket->AVFrame
        ret = avcodec_send_packet(audioCodecCtx, packet);
        if (ret == AVERROR_EOF){
            av_packet_unref(packet);
            LOGW("audio_decoder avcodec_send_packet：%d\n", ret);
            break; // 跳出 while(player->stop_thread_audio_decoder==0)
        }else if(ret < 0){
            av_packet_unref(packet);
            LOGE("audio_decoder avcodec_send_packet：%d\n", ret);
            continue;
        }

        while(ret >= 0)
        {
            ret = avcodec_receive_frame(audioCodecCtx, frame);
            if (ret == AVERROR(EAGAIN) ) {
                //LOGD("audio_decoder avcodec_receive_frame：%d\n", ret);
                break; // 跳出 while(ret>=0)
            } else if (ret < 0 || ret == AVERROR_EOF) {
                LOGW("audio_decoder avcodec_receive_frame：%d\n", AVERROR(ret));
                av_packet_unref(packet);
                goto end;  //end处进行资源释放等善后处理
            }
            if (ret >= 0)
            {
                swr_convert(player->swr_ctx, &out_buffer, MAX_AUDIO_FRAME_SIZE, (const uint8_t **) frame->data, frame->nb_samples);
                //获取sample的size
                int out_buffer_size = av_samples_get_buffer_size(NULL, player->out_channel_nb,
                                                                 frame->nb_samples, player->out_sample_fmt, 1);
                //AudioTrack.write(byte[] int int) 需要byte数组,对应jni的jbyteArray
                //需要把out_buffer缓冲区数据转成byte数组
                jbyteArray audio_data_byteArray = (*env)->NewByteArray(env, out_buffer_size);
                jbyte* fp_AudioDataArray = (*env)->GetByteArrayElements(env, audio_data_byteArray, NULL);
                memcpy(fp_AudioDataArray, out_buffer, (size_t) out_buffer_size);
                (*env)->ReleaseByteArrayElements(env, audio_data_byteArray, fp_AudioDataArray,0);
                // AudioTrack.write PCM数据
                (*env)->CallIntMethod(env,player->audio_track,player->audio_track_write_mid,
                                      audio_data_byteArray, 0, out_buffer_size);
                //！！！释放局部引用，要不然会局部引用溢出
                (*env)->DeleteLocalRef(env,audio_data_byteArray);
            }
        }
        av_packet_unref(packet);
    }

end:
    av_free(out_buffer);
    av_frame_free(&frame);
    LOGI("thread_audio_avframe_decoder exit ...\n");
    (*gJavaVM)->DetachCurrentThread(gJavaVM);
    return 0;
}

void* video_avframe_decoder(void* arg)
{
    SyncPlayer* player = (SyncPlayer*)arg;
    AVCodecContext* videoCodecCtx = player->input_codec_ctx[player->video_stream_index];
    AV_PACKET_BUFFER* videoAVPacketButter = player->video_avpacket_buffer;

    AVFrame *yuv_frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();

    //用于 缩放
    //AVFrame *sws_yuv_frame = av_frame_alloc();
    //struct SwsContext *sws_ctx = sws_getContext(
    //        videoCodecCtx->width, videoCodecCtx->height, videoCodecCtx->pix_fmt,
    //        videoCodecCtx->width/2, videoCodecCtx->height/2, AV_PIX_FMT_YUV420P,
    //        SWS_BILINEAR, NULL, NULL, NULL);
    //int buffer_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, videoCodecCtx->width/2, videoCodecCtx->height/2, 1);
    //uint8_t *out_buffer = (uint8_t *)av_malloc((size_t) buffer_size);
    //// 初始化 sws_yuv_frame 缓冲区
    //av_image_fill_arrays(sws_yuv_frame->data, sws_yuv_frame->linesize, out_buffer,
    //                     AV_PIX_FMT_YUV420P, videoCodecCtx->width/2, videoCodecCtx->height/2, 1 );

    // 准备native绘制的窗体
    ANativeWindow* nativeWindow = player->native_window;
    // 设置缓冲区的属性（宽、高、像素格式）
    ANativeWindow_setBuffersGeometry(nativeWindow, videoCodecCtx->width, videoCodecCtx->height, WINDOW_FORMAT_RGBA_8888);
    // 绘制时的缓冲区
    ANativeWindow_Buffer nativeWinBuffer;
    int ret;
    while(player->stop_thread_video_decoder == 0)
    {
        pthread_mutex_lock(&videoAVPacketButter->mutex);
        AVPacket* packet = get_read_packet(videoAVPacketButter);
        pthread_mutex_unlock(&videoAVPacketButter->mutex);
        //AVPacket->AVFrame
        ret = avcodec_send_packet(videoCodecCtx, packet);
        if (ret == AVERROR_EOF){
            av_packet_unref(packet);
            LOGW("video_decoder avcodec_send_packet：%d\n", ret);
            break; //跳出 while(player->stop_thread_video_decoder == 0)
        }else if(ret < 0){
            av_packet_unref(packet);
            LOGE("video_decoder avcodec_send_packet：%d\n", ret);
            continue;
        }

        while(ret >= 0)
        {
            ret = avcodec_receive_frame(videoCodecCtx, yuv_frame);
            if (ret == AVERROR(EAGAIN) ){
                //LOGD("video_decoder avcodec_receive_frame：%d\n", ret);
                break; //跳出 while(ret >= 0)
            }else if (ret < 0 || ret == AVERROR_EOF) {
                LOGW("video_decoder avcodec_receive_frame：%d\n", AVERROR(ret));
                av_packet_unref(packet);
                goto end;
            }

            if (ret >= 0)
            {
                //sws_scale(sws_ctx,
                //          (const uint8_t* const*)yuv_frame->data, yuv_frame->linesize, 0, yuv_frame->height,
                //          sws_yuv_frame->data, sws_yuv_frame->linesize);

                ANativeWindow_lock(nativeWindow, &nativeWinBuffer, NULL);
                // 上锁并关联 ANativeWindow + ANativeWindow_Buffer
                av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, nativeWinBuffer.bits,
                                     AV_PIX_FMT_RGBA, videoCodecCtx->width, videoCodecCtx->height, 1 );
                // rgb.AVFrame对象 关联 ANativeWindow_Buffer的真实内存空间actual bits.
                I420ToARGB(yuv_frame->data[0], yuv_frame->linesize[0],
                           yuv_frame->data[2], yuv_frame->linesize[2],
                           yuv_frame->data[1], yuv_frame->linesize[1],
                           rgb_frame->data[0], rgb_frame->linesize[0],
                           videoCodecCtx->width, videoCodecCtx->height);
                // yuv.AVFrame 转 rgb.AVFrame
                ANativeWindow_unlockAndPost(nativeWindow);
                // 释放锁并 swap交换显示内存到屏幕上。
            }
        }
        av_packet_unref(packet);
    }

end:
    av_frame_free(&yuv_frame);
    av_frame_free(&rgb_frame);
    LOGI("thread_video_avframe_decoder exit ...\n");
    return 0;
}



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
    jobject jthiz = player->jinstance;
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

void initPlayerAVPacketBuffer(SyncPlayer* player)
{
    // 视频的AVPacket的缓冲区
    if(player->video_avpacket_buffer != NULL) {
        free_avpacket_buffer(player->video_avpacket_buffer);
        player->video_avpacket_buffer = NULL; // 防止野指针
    }
    player->video_avpacket_buffer = alloc_avpacket_buffer(BUFFER_SIZE*2);
    // 音频的AVPacket的缓冲区
    if(player->audio_avpacket_buffer != NULL) {
        free_avpacket_buffer(player->audio_avpacket_buffer);
        player->audio_avpacket_buffer = NULL; // 防止野指针
    }
    player->audio_avpacket_buffer = alloc_avpacket_buffer(BUFFER_SIZE);
}










//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// native method implementation ////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

SyncPlayer* mSyncPlayer;

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativeInit(JNIEnv *env, jobject instance)
{
    av_log_set_callback(ffmpeg_custom_log);
    av_register_all();
    avcodec_register_all();
    avformat_network_init();
    // malloc与calloc区别
    // 1.malloc是以字节为单位，calloc是以item为单位。
    // 2.malloc需要memset初始化为0，calloc默认初始化为0
    mSyncPlayer = (SyncPlayer*)calloc(1, sizeof(SyncPlayer));
    // SyncPlayer的java对象，需要建立引用 NewGlobalRef，记得DeleteGlobalRef
    mSyncPlayer->jinstance = (*env)->NewGlobalRef(env, instance);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_SyncPlayer_nativeRelease(JNIEnv *env, jobject instance)
{
    if(mSyncPlayer == NULL)
        return;
    if(mSyncPlayer->input_format_ctx == NULL){
        return;
    }
    // 暂停工作线程
    mSyncPlayer->stop_thread_avpacket_distributor = 1;
    pthread_join(mSyncPlayer->thread_avpacket_distributor, NULL);
    mSyncPlayer->stop_thread_video_decoder = 1;
    pthread_join(mSyncPlayer->thread_video_decoder, NULL);
    mSyncPlayer->stop_thread_audio_decoder = 1;
    pthread_join(mSyncPlayer->thread_audio_decoder, NULL);
    // 释放音频相关
    (*env)->DeleteGlobalRef(env, mSyncPlayer->audio_track);
    swr_free(&(mSyncPlayer->swr_ctx));
    // 释放解码器
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
    // 释放AVPacket缓冲区
    LOGD("free audio_avpacket_buffer ");
    free_avpacket_buffer(mSyncPlayer->audio_avpacket_buffer);
    LOGD("free video_avpacket_buffer ");
    free_avpacket_buffer(mSyncPlayer->video_avpacket_buffer);
    // 释放输入文件上下文
    avformat_close_input(&(mSyncPlayer->input_format_ctx));
    avformat_free_context(mSyncPlayer->input_format_ctx);
    mSyncPlayer->input_format_ctx = NULL;
    // 释放 SyncPlayer
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
        enum AVMediaType meida_type = pFormatContext->streams[i]->codecpar->codec_type;
        switch(meida_type)
        {
            case AVMEDIA_TYPE_VIDEO:
                video_stream_idx = i;
                break;
            case AVMEDIA_TYPE_AUDIO:
                audio_stream_idx = i;
                break;
            default:
                continue;
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
    if(mSyncPlayer->input_format_ctx == NULL) {
        LOGW("%s","请调用函数：nativePrepare");
        return;
    }

    initPlayerAVPacketBuffer(mSyncPlayer);

    mSyncPlayer->stop_thread_avpacket_distributor = 0;
    pthread_create(&(mSyncPlayer->thread_avpacket_distributor), NULL, avpacket_distributor, mSyncPlayer);
    usleep(1000); // 1000us = 1ms
    // 意义在于让avpacket_distributor比video_avframe_decoder早点运行。
    // 不清楚的同学可以学习java.util.concurrent.CountDownLatch的运用，触类旁通。

    mSyncPlayer->stop_thread_video_decoder = 0;
    pthread_create(&(mSyncPlayer->thread_video_decoder), NULL, video_avframe_decoder, mSyncPlayer);

    mSyncPlayer->stop_thread_audio_decoder = 0;
    pthread_create(&(mSyncPlayer->thread_audio_decoder), NULL, audio_avframe_decoder, mSyncPlayer);

    usleep(50000); // 50ms
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////END： native method implementation ////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////


