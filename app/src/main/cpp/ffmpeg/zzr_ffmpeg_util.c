//
// Created by nicky on 2018/11/29.
//

#include <jni.h>
#include "../log_common.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/imgutils.h"
#include "include/libswscale/swscale.h"
#include "include/libswresample/swresample.h"

//Output FFmpeg's av_log()
void custom_log(void *ptr, int level, const char* fmt, va_list vl){
    FILE *fp=fopen("/storage/emulated/0/av_log.txt","w+");
    if(fp){
        vfprintf(fp,fmt,vl);
        fflush(fp);
        fclose(fp);
    }
}

JNIEXPORT jint JNICALL
Java_org_zzrblog_ffmp_ZzrFFmpeg_Mp4TOYuv(JNIEnv *env, jclass clazz,
                                       jstring input_mp4_jstr, jstring output_yuv_jstr) {

    //const char *input_mp4_cstr = env->GetStringUTFChars(input_mp4_jstr, 0);
    const char *input_mp4_cstr = (*env)->GetStringUTFChars(env, input_mp4_jstr, 0);
    const char *output_yuv_cstr = (*env)->GetStringUTFChars(env, output_yuv_jstr, 0);
    LOGD("MP4输入文件：%s", input_mp4_cstr);
    LOGD("YUV输出文件：%s", output_yuv_cstr);

    av_log_set_callback(custom_log);
    // 1.注册组件
    av_register_all();
    avcodec_register_all();
    avformat_network_init();
    // 2.获取格式上下文指针，便于打开媒体容器文件获取媒体信息
    AVFormatContext *pFormatContext = avformat_alloc_context();
    // 打开输入视频文件
    if(avformat_open_input(&pFormatContext, input_mp4_cstr, NULL, NULL) != 0){
        LOGE("%s","打开输入视频文件失败");
        return -1;
    }
    // 获取视频信息
    if(avformat_find_stream_info(pFormatContext,NULL) < 0){
        LOGE("%s","获取视频信息失败");
        return -2;
    }

    // 3.准备视频解码器，根据视频AVStream所在pFormatCtx->streams中位置，找出索引
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
    // 根据codec_parameter的codec索引，提取对应的解码器。
    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[video_stream_idx]->codecpar->codec_id);
    if(pCodec == NULL) {
        LOGE("%s","解码器创建失败.");
        return -3;
    }
    // 4.创建解码器对应的上下文
    AVCodecContext * pCodecContext = avcodec_alloc_context3(pCodec);
    if(pCodecContext == NULL) {
        LOGE("%s","创建解码器对应的上下文失败.");
        return -4;
    }
    // 坑位！！！
    //pCodecContext->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    //pCodecContext->width = pFormatContext->streams[video_stream_idx]->codecpar->width;
    //pCodecContext->height = pFormatContext->streams[video_stream_idx]->codecpar->height;
    //pCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    avcodec_parameters_to_context(pCodecContext, pFormatContext->streams[video_stream_idx]->codecpar);
    // 5.打开解码器
    if(avcodec_open2(pCodecContext, pCodec, NULL) < 0){
        LOGE("%s","解码器无法打开");
        return -5;
    } else {
        LOGI("解码器解码视频格式：%d", pCodecContext->pix_fmt);
    }


    // 解码流程，多看多理解。

    // 解压缩数据对象
    AVPacket *packet = av_packet_alloc();
    // 解码数据对象
    AVFrame *frame = av_frame_alloc();
    AVFrame *yuvFrame = av_frame_alloc();

    // 为yuvFrame缓冲区分配内存，只有指定了AVFrame的像素格式、画面大小才能真正分配内存
    int buffer_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecContext->width, pCodecContext->height, 1);
    uint8_t *out_buffer = (uint8_t *)av_malloc((size_t) buffer_size);
    // 初始化yuvFrame缓冲区
    av_image_fill_arrays(yuvFrame->data, yuvFrame->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodecContext->width, pCodecContext->height, 1 );

    // yuv输出文件
    FILE* fp_yuv = fopen(output_yuv_cstr, "wb");

    //用于尺寸缩放
    struct SwsContext *sws_ctx = sws_getContext(
            pCodecContext->width, pCodecContext->height, pCodecContext->pix_fmt,
            pCodecContext->width, pCodecContext->height, AV_PIX_FMT_YUV420P,
            SWS_BICUBIC, NULL, NULL, NULL); //SWS_BILINEAR

    int ret, frameCount = 0;
    // 5. 循环读取视频数据的分包 AVPacket
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
                ret = avcodec_receive_frame(pCodecContext, frame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF){
                    LOGD("avcodec_receive_frame：%d\n", ret);
                    break;
                }else if (ret < 0) {
                    LOGW("avcodec_receive_frame：%d\n", AVERROR(ret));
                    goto end;  //end处进行资源释放等善后处理
                }
                if (ret >= 0)
                {   //frame->yuvFrame (调整缩放)
                    sws_scale(sws_ctx,
                              (const uint8_t* const*)frame->data, frame->linesize, 0, frame->height,
                              yuvFrame->data, yuvFrame->linesize);
                    //向YUV文件保存解码之后的帧数据
                    //写入文件，一个像素包含一个Y
                    int y_size = frame->width * frame->height;
                    fwrite(yuvFrame->data[0], 1, (size_t) y_size, fp_yuv);
                    fwrite(yuvFrame->data[1], 1, (size_t) y_size/4, fp_yuv);
                    fwrite(yuvFrame->data[2], 1, (size_t) y_size/4, fp_yuv);
                    frameCount++ ;
                }
            }
        }
        av_packet_unref(packet);
    }
    LOGI("总共解码%d帧",frameCount++);


end:
    // 结束回收工作
    fclose(fp_yuv);

    sws_freeContext(sws_ctx);
    av_free(out_buffer);
    av_frame_free(&frame);
    av_frame_free(&yuvFrame);
    avcodec_close(pCodecContext);
    avcodec_free_context(&pCodecContext);
    avformat_close_input(&pFormatContext);
    avformat_free_context(pFormatContext);

    //env->ReleaseStringUTFChars(input_mp4_jstr, input_mp4_cstr);
    (*env)->ReleaseStringUTFChars(env, input_mp4_jstr, input_mp4_cstr);
    (*env)->ReleaseStringUTFChars(env, output_yuv_jstr, output_yuv_cstr);

    return 0;
}






#define MAX_AUDIO_FARME_SIZE 48000 * 2

JNIEXPORT jint JNICALL
Java_org_zzrblog_ffmp_ZzrFFmpeg_Mp34TOPcm(JNIEnv *env, jclass clazz, jstring input_media_jstr, jstring output_pcm_jstr) {
    const char *input_media_cstr = (*env)->GetStringUTFChars(env, input_media_jstr, 0);
    const char *output_pcm_cstr = (*env)->GetStringUTFChars(env, output_pcm_jstr, 0);

    av_log_set_callback(custom_log);
    // 注册组件
    av_register_all();
    avcodec_register_all();
    avformat_network_init();

    AVFormatContext *pFormatContext = avformat_alloc_context();
    if(avformat_open_input(&pFormatContext, input_media_cstr,NULL,NULL) != 0){
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



    //开始解码
    AVPacket *packet = av_packet_alloc();
    AVFrame *frame = av_frame_alloc();
    //frame->16bit双声道 采样率44100 PCM采样格式
    SwrContext *swrCtx = swr_alloc();
    //  设置采样参数-------------start
    //输入的采样格式
    enum AVSampleFormat in_sample_fmt = pCodecContext->sample_fmt;
    //输出采样格式16bit PCM
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    //输入采样率
    int in_sample_rate = pCodecContext->sample_rate;
    //输出采样率
    int out_sample_rate = 44100;
    //输入的声道布局
    uint64_t in_ch_layout = pCodecContext->channel_layout;
    //输出的声道布局（立体声）
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    //  设置采样参数---------------end
    swr_alloc_set_opts(swrCtx,
                       out_ch_layout,out_sample_fmt,out_sample_rate,
                       in_ch_layout,in_sample_fmt,in_sample_rate,
                       0, NULL);
    swr_init(swrCtx);


    //16bit 44100 PCM 数据 内存空间。
    uint8_t *out_buffer = (uint8_t *)av_malloc(MAX_AUDIO_FARME_SIZE);
    //根据声道个数 获取 匹配的声道布局（双声道->立体声stereo）
    //av_get_default_channel_layout(pCodecContext->channels);
    //根据声道布局 获取 输出的声道个数
    int out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);

    FILE *fp_pcm = fopen(output_pcm_cstr,"wb");
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
                    fwrite(out_buffer, 1, (size_t) out_buffer_size, fp_pcm);
                }
            }
        }
        av_packet_unref(packet);
    }
    LOGD("媒体文件转换PCM结束\n");


end:
    fclose(fp_pcm);
    av_frame_free(&frame);
    av_free(out_buffer);
    swr_free(&swrCtx);

    (*env)->ReleaseStringUTFChars(env, input_media_jstr, input_media_cstr);
    (*env)->ReleaseStringUTFChars(env, output_pcm_jstr, output_pcm_cstr);
    return 0;
}




