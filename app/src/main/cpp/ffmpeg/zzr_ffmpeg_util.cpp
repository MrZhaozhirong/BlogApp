//
// Created by nicky on 2018/11/29.
//

#include <jni.h>
#include "../zzr_blog_common.h"
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/avutil.h"
#include "include/libavutil/frame.h"
#include "include/libavutil/imgutils.h"
#include "include/libswscale/swscale.h"


JNIEXPORT jint JNICALL
Java_org_zzrblog_mp_ZzrFFmpeg_Mp4TOYuv(JNIEnv *env, jclass clazz, jstring input_path_jstr, jstring output_path_jstr) {

    const char *input_path_cstr = env->GetStringUTFChars(input_path_jstr, 0);
    const char *output_path_cstr = env->GetStringUTFChars(output_path_jstr, 0);
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
    // 提取对应的解码器。
    AVCodec *pCodec = avcodec_find_decoder(pFormatContext->streams[video_stream_idx]->codecpar->codec_id);
    // 创建解码器对应的上下文
    AVCodecContext * pCodeContext = avcodec_alloc_context3(pCodec);
    // 打开解码器
    if(avcodec_open2(pCodeContext, pCodec, NULL) < 0){
        LOGE("%s","解码器无法打开");
        return -3;
    } else {
        LOGI("当前解码器pix_fmt：%d", pCodeContext->pix_fmt);
    }


    // 4.解码准备，多看多理解。

    // 编码数据对象
    AVPacket *packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    // 解码数据对象
    AVFrame *frame = av_frame_alloc();
    AVFrame *yuvFrame = av_frame_alloc();

    // 为yuvFrame缓冲区分配内存，只有指定了AVFrame的像素格式、画面大小才能真正分配内存
    int buffer_size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodeContext->width, pCodeContext->height, 1);
    uint8_t *out_buffer = (uint8_t *)av_malloc((size_t) buffer_size);
    // 初始化yuvFrame缓冲区
    av_image_fill_arrays(yuvFrame->data, yuvFrame->linesize, out_buffer,
                         AV_PIX_FMT_YUV420P, pCodeContext->width, pCodeContext->height, 1 );

    // 打开输出文件
    FILE* fp_yuv = fopen(output_path_cstr,"wb");

    //用于像素格式转换或者缩放
    struct SwsContext *sws_ctx = sws_getContext(
            pCodeContext->width, pCodeContext->height, pCodeContext->pix_fmt,
            pCodeContext->width, pCodeContext->height, AV_PIX_FMT_YUV420P,
            SWS_BILINEAR, NULL, NULL, NULL);

    int len, isGot, frameCount = 0;
    // 5. 循环读取压缩的视频数据 AVPacket
    while(av_read_frame(pFormatContext, packet) >= 0){
        //解码AVPacket->AVFrame
        len = avcodec_decode_video2(pCodeContext, frame, &isGot, packet);
        //Zero if no frame could be decompressed
        if(isGot){
            //frame->yuvFrame (YUV420P)
            //RGB转为指定的YUV420P像素帧
            sws_scale(sws_ctx,
                      frame->data, frame->linesize, 0, frame->height,
                      yuvFrame->data, yuvFrame->linesize);
            //向YUV文件保存解码之后的帧数据
            //AVFrame->YUV
            //一个像素包含一个Y
            int y_size = pCodeContext->width * pCodeContext->height;
            fwrite(yuvFrame->data[0], 1, (size_t) y_size, fp_yuv);
            fwrite(yuvFrame->data[1], 1, (size_t) y_size/4, fp_yuv);
            fwrite(yuvFrame->data[2], 1, (size_t) y_size/4, fp_yuv);

            frameCount++ ;
        }

        av_free_packet(packet);
    }
    LOGI("总共解码%d帧",frameCount++);
    fclose(fp_yuv);


    // 结束回收工作
    av_free(out_buffer);
    av_frame_free(&frame);
    av_frame_free(&yuvFrame);
    avcodec_close(pCodeContext);
    avformat_free_context(pFormatContext);

    env->ReleaseStringUTFChars(input_path_jstr, input_path_cstr);
    env->ReleaseStringUTFChars(output_path_jstr, output_path_cstr);
    return 0;
}


