//
// Created by nicky on 2018/11/29.
//

#include <jni.h>
#include "../log_common.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/imgutils.h"
#include "include/libswscale/swscale.h"

//Output FFmpeg's av_log()
void custom_log(void *ptr, int level, const char* fmt, va_list vl){
    FILE *fp=fopen("/storage/emulated/0/av_log.txt","a+");
    if(fp){
        vfprintf(fp,fmt,vl);
        fflush(fp);
        fclose(fp);
    }
}

JNIEXPORT jint JNICALL
Java_org_zzrblog_mp_ZzrFFmpeg_Mp4TOYuv(JNIEnv *env, jclass clazz, jstring input_mp4_jstr, jstring output_yuv_jstr, jstring output_h264_jstr) {

    //const char *input_mp4_cstr = env->GetStringUTFChars(input_mp4_jstr, 0);
    const char *input_mp4_cstr = (*env)->GetStringUTFChars(env, input_mp4_jstr, 0);
    const char *output_yuv_cstr = (*env)->GetStringUTFChars(env, output_yuv_jstr, 0);
    const char *output_h264_cstr = (*env)->GetStringUTFChars(env, output_h264_jstr, 0);
    LOGD("MP4输入文件：%s", input_mp4_cstr);
    LOGD("YUV输出文件：%s", output_yuv_cstr);
    LOGD("h264输出文件：%s", output_h264_cstr);

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
        LOGI("设置解码器解码格式pix_fmt：%d", pCodecContext->pix_fmt);
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
    // 264输出文件
    FILE* fp_264 = fopen(output_h264_cstr,"wb");

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
            // test：h264数据写入本地文件
            fwrite(packet->data, 1, (size_t) packet->size, fp_264);
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
    fclose(fp_264);

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
    (*env)->ReleaseStringUTFChars(env, output_h264_jstr, output_h264_cstr);

    return 0;
}


