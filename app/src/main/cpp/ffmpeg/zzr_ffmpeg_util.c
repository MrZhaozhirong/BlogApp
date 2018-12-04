//
// Created by nicky on 2018/11/29.
//

#include <jni.h>
#include "../zzr_blog_common.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/imgutils.h"
#include "include/libswscale/swscale.h"


JNIEXPORT jint JNICALL
Java_org_zzrblog_mp_ZzrFFmpeg_Mp4TOYuv(JNIEnv *env, jclass clazz, jstring input_path_jstr, jstring output_path_jstr) {

    //const char *input_path_cstr = env->GetStringUTFChars(input_path_jstr, 0);
    //const char *output_path_cstr = env->GetStringUTFChars(output_path_jstr, 0);
    const char *input_path_cstr = (*env)->GetStringUTFChars(env, input_path_jstr, 0);
    const char *output_path_cstr = (*env)->GetStringUTFChars(env, output_path_jstr, 0);
    LOGD("输入文件：%s", input_path_cstr);
    LOGD("输出文件：%s", output_path_cstr);

    // 1.注册组件
    av_register_all();
    // 2.获取格式上下文指针，便于打开媒体容器文件获取媒体信息
    AVFormatContext *pFormatContext = avformat_alloc_context();
    // 打开输入视频文件
    if(avformat_open_input(&pFormatContext, input_path_cstr, NULL, NULL) != 0){
        LOGE("%s","打开输入视频文件失败");
        return -1;
    }
    // 获取视频信息
    if(avformat_find_stream_info(pFormatContext,NULL) < 0){
        LOGE("%s","获取视频信息失败");
        return -2;
    }

    // 3.准备视频解码器，需要找到视频对应的AVStream所在pFormatCtx->streams中，是VIDEO的索引位置
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
    // 提取对应的解码器。
    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[video_stream_idx]->codecpar->codec_id);
    if(pCodec == NULL) {
        LOGE("%s","解码器创建失败.");
        return -3;
    }
    // 创建解码器对应的上下文
    AVCodecContext * pCodecContext = avcodec_alloc_context3(pCodec);
    if(pCodecContext == NULL) {
        LOGE("%s","创建解码器对应的上下文失败.");
        return -4;
    }
    // 打开解码器
    if(avcodec_open2(pCodecContext, pCodec, NULL) < 0){
        LOGE("%s","解码器无法打开");
        return -5;
    } else {
        LOGI("当前解码器pix_fmt：%d", pCodecContext->pix_fmt);
    }


    // 4.解码准备，多看多理解。

    // 编码数据对象
    AVPacket *packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    // 解码数据对象
    AVFrame *frame = av_frame_alloc();
    AVFrame *yuvFrame = av_frame_alloc();

    // 为yuvFrame缓冲区分配内存，只有指定了AVFrame的像素格式、画面大小才能真正分配内存
    int buffer_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecContext->width, pCodecContext->height, 1);
    uint8_t *out_buffer = (uint8_t *)av_malloc((size_t) buffer_size);
    // 初始化yuvFrame缓冲区
    av_image_fill_arrays(yuvFrame->data, yuvFrame->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodecContext->width, pCodecContext->height, 1 );

    // 打开输出文件
    FILE* fp_yuv = fopen(output_path_cstr,"wb");

    //用于像素格式转换或者缩放
    struct SwsContext *sws_ctx = sws_getContext(
            pCodecContext->width, pCodecContext->height, pCodecContext->pix_fmt,
            pCodecContext->width, pCodecContext->height, AV_PIX_FMT_YUV420P,
            SWS_BICUBIC, NULL, NULL, NULL); //SWS_BILINEAR

    int ret, isGot, frameCount = 0;
    // 5. 循环读取压缩的视频数据 AVPacket
    while(av_read_frame(pFormatContext, packet) >= 0){
        if(packet->stream_index == video_stream_idx) {
            //解码AVPacket->AVFrame
            ret = avcodec_decode_video2(pCodecContext, frame, &isGot, packet);
            if(ret < 0){
                LOGE("解码失败.\n");
            }
            //Zero if no frame could be decompressed
            if(isGot){
                //frame->yuvFrame (YUV420P)
                //RGB转为指定的YUV420P像素帧
                sws_scale(sws_ctx,
                          (const uint8_t* const*)frame->data, frame->linesize, 0, frame->height,
                          yuvFrame->data, yuvFrame->linesize);
                //向YUV文件保存解码之后的帧数据
                //AVFrame->YUV
                //一个像素包含一个Y
                int y_size = pCodecContext->width * pCodecContext->height;
                fwrite(yuvFrame->data[0], 1, (size_t) y_size, fp_yuv);
                fwrite(yuvFrame->data[1], 1, (size_t) y_size/4, fp_yuv);
                fwrite(yuvFrame->data[2], 1, (size_t) y_size/4, fp_yuv);

                frameCount++ ;
            }
        }
        av_free_packet(packet);
    }
    //flush decoder
    //FIX: Flush Frames remained in Codec
    while (1) {
        ret = avcodec_decode_video2(pCodecContext, frame, &isGot, packet);
        if (ret < 0)
            break;
        if (!isGot)
            break;
        sws_scale(sws_ctx, (const uint8_t* const*)frame->data, frame->linesize, 0, frame->height,
                  yuvFrame->data, yuvFrame->linesize);
        int y_size = pCodecContext->width * pCodecContext->height;
        fwrite(yuvFrame->data[0], 1, (size_t) y_size, fp_yuv);
        fwrite(yuvFrame->data[1], 1, (size_t) (y_size / 4), fp_yuv);
        fwrite(yuvFrame->data[2], 1, (size_t) (y_size / 4), fp_yuv);
        frameCount++;
    }
    LOGI("总共解码%d帧",frameCount++);
    fclose(fp_yuv);


    // 结束回收工作
    sws_freeContext(sws_ctx);
    av_free(out_buffer);
    av_frame_free(&frame);
    av_frame_free(&yuvFrame);
    avcodec_close(pCodecContext);
    avformat_free_context(pFormatContext);

    //env->ReleaseStringUTFChars(input_path_jstr, input_path_cstr);
    //env->ReleaseStringUTFChars(output_path_jstr, output_path_cstr);
    (*env)->ReleaseStringUTFChars(env, input_path_jstr, input_path_cstr);
    (*env)->ReleaseStringUTFChars(env, output_path_jstr, output_path_cstr);
    return 0;
}


