#include <jni.h>
#include <malloc.h>
#include <pthread.h>
#include "../common/zzr_common.h"
#include "x264/include/x264.h"
#include "rtmp/include/rtmp.h"
#include "faac/include/faac.h"
#include "queue.h"

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// 声明全局变量，在common.c的JNI_OnLoad定义
extern JavaVM * gJavaVM;

typedef struct _rtmp_push {
    int width;
    int height;
    int bitrate;
    int fps;
    int sampleRateInHz; //音频采样频率
    int channelNum; // 音频声道数
    unsigned long nInputSamples; //输入的采样个数
    unsigned long nMaxOutputBytes; //编码输出之后的最大字节数
    //rtmp流媒体地址
    char *rtmp_path;

    // x264视频编码
    x264_picture_t pic_in;
    x264_t *x264_encoder;
    x264_picture_t pic_out;

    // faac音频编码
    faacEncHandle faac_encoder;
    //线程处理
    unsigned int start_time;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    pthread_t rtmp_push_thread_id;
    int stop_rtmp_push_thread;

} RtmpPusher;

RtmpPusher* gRtmpPusher;

/**
 * Send RTMPPacket 工作线程
 * 从双向队列中不断提取RTMPPacket发送到指定的流媒体服务器
 */
void* rtmp_push_thread(void * arg) {
    //创建RTMP对象
    RTMP *rtmp = RTMP_Alloc();
    if(!rtmp){
        LOGE("rtmp初始化失败");
        goto end;
    }
    //初始化rtmp
    RTMP_Init(rtmp);
    //设置连接超时时间
    rtmp->Link.timeout = 5;
    //设置流媒体地址
    RTMP_SetupURL(rtmp, gRtmpPusher->rtmp_path);
    //发布rtmp数据流
    RTMP_EnableWrite(rtmp);
    //建立连接
    if(!RTMP_Connect(rtmp,NULL)){
        LOGE("%s","RTMP 连接失败");
        goto end;
    }
    // 初始化启动计时
    gRtmpPusher->start_time = RTMP_GetTime();
    if(!RTMP_ConnectStream(rtmp,0)) { //连接流
        goto end;
    }

    while(gRtmpPusher->stop_rtmp_push_thread == 0)
    {
        pthread_mutex_lock(&(gRtmpPusher->mutex));
        // 等待RTMPPacket加入到双向队列，然后 pthread_cond_signal
        pthread_cond_wait(&(gRtmpPusher->cond), &(gRtmpPusher->mutex));
        //取出队列中的RTMPPacket
        RTMPPacket *packet = queue_get_first();
        if(packet) {
            queue_delete_first(); //从队列中移除当前节点
            //注意：Note
            // 我们在打包RTMPPacket的时候，没有填 m_nInfoField2，
            // 是因为打包的时候不知道对应的stream_id是多少
            // 这里装填RTMPPacket的m_nInfoField2字段值 = RTMP的推送流ID stream_id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            int i = RTMP_SendPacket(rtmp,packet,TRUE); //TRUE放入librtmp队列中，并不是立即发送
            if(!i){
                LOGE("RTMP 断开");
                RTMPPacket_Free(packet);
                pthread_mutex_unlock(&(gRtmpPusher->mutex));
                goto end;
            }
            RTMPPacket_Free(packet);
        }
        pthread_mutex_unlock(&(gRtmpPusher->mutex));
    }
end:
    LOGI("%s","释放资源");
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
    return 0;
}

/**
 * 加入RTMPPacket双向队列，等待发送线程发送
 */
void add_rtmp_packet(RTMPPacket *packet) {
    if(gRtmpPusher == NULL)
        return;

    pthread_mutex_lock(&(gRtmpPusher->mutex));
    if(gRtmpPusher->stop_rtmp_push_thread == 0) {
        queue_append_last(packet);
    }
    pthread_cond_signal(&(gRtmpPusher->cond));
    pthread_mutex_unlock(&(gRtmpPusher->mutex));
}


/**
 * 添加AAC编码的sequence header
 */
void add_aac_sequence_header()
{
    if(gRtmpPusher==NULL || gRtmpPusher->faac_encoder==NULL)
        return;
    //从faacEncoder获取aac头信息
    unsigned char *spec_buf;
    unsigned long len; //长度
    faacEncGetDecoderSpecificInfo(gRtmpPusher->faac_encoder, &spec_buf, &len);
    uint32_t body_size = 2 + len;
    RTMPPacket *packet = malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet,body_size);
    RTMPPacket_Reset(packet);
    char * body = packet->m_body;
    //AUDIODATA的标志位，各位标志如下
    body[0] = 0xAF;
    // SoundFormat(4bits):10=AAC；
    // SoundRate(2bits):3=44kHz；
    // SoundSize(1bit):1=16-bit samples；
    // SoundType(1bit):1=Stereo sound；

    //AACAUDIODATA的AACPacketType
    body[1] = 0x00;
    // 1表示AAC raw，
    // 0表示AAC sequence header
    //AACAUDIODATA的RawData
    memcpy(&body[2], spec_buf, len); /*spec_buf是AAC sequence header数据*/

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    add_rtmp_packet(packet);
    free(spec_buf);
}

/**
 * 添加AAC头->RTMPPacket队列
 */
void add_aac_body(unsigned char *buf, int len)
{
    if(gRtmpPusher==NULL)
        return;
    int body_size = 2 + len;
    RTMPPacket *packet = malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet,body_size);
    RTMPPacket_Reset(packet);
    char * body = packet->m_body;
    //AUDIODATA的标志位，各位标志如下
    body[0] = 0xAF;
    // SoundFormat(4bits):10=AAC；
    // SoundRate(2bits):3=44kHz；
    // SoundSize(1bit):1=16-bit samples；
    // SoundType(1bit):1=Stereo sound；

    //AACAUDIODATA的AACPacketType
    body[1] = 0x01;
    // 1表示AAC raw，
    // 0表示AAC sequence header
    //AACAUDIODATA的RawData
    memcpy(&body[2], buf, (size_t) len); /*spec_buf是AAC raw数据*/

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = (uint32_t) body_size;
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nTimeStamp = RTMP_GetTime() - gRtmpPusher->start_time;
    add_rtmp_packet(packet);
}

/**
 * 打包h264的SPS与PPS->NALU->RTMPPacket队列
 */
void add_param_sequence(unsigned char* pps, unsigned char* sps, int pps_len, int sps_len)
{
    int body_size = 16 + sps_len + pps_len; //按照H264标准配置SPS和PPS，共使用了16字节
    RTMPPacket *packet = malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet, body_size);
    RTMPPacket_Reset(packet);
    // 获取packet对象当中的m_body指针
    char * body = packet->m_body;
    int i = 0;
    //二进制表示：00010111
    body[i++] = 0x17;//VideoHeaderTag:FrameType(1=key frame)+CodecID(7=AVC)
    body[i++] = 0x00;//AVCPacketType=0（AVC sequence header）表示设置AVCDecoderConfigurationRecord
    //composition time 0x000000 24bit ?
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    /*AVCDecoderConfigurationRecord*/
    body[i++] = 0x01;//configurationVersion，版本为1
    body[i++] = sps[1];//AVCProfileIndication
    body[i++] = sps[2];//profile_compatibility
    body[i++] = sps[3];//AVCLevelIndication
    body[i++] = 0xFF;//lengthSizeMinusOne,H264视频中NALU的长度，计算方法是 1 + (lengthSizeMinusOne & 3),实际测试时发现总为FF，计算结果为4.
    /*sps*/
    body[i++] = 0xE1;//numOfSequenceParameterSets:SPS的个数，计算方法是 numOfSequenceParameterSets & 0x1F,实际测试时发现总为E1，计算结果为1.
    body[i++] = (unsigned char) ((sps_len >> 8) & 0xff);//sequenceParameterSetLength:SPS的长度
    body[i++] = (unsigned char) (sps_len & 0xff);//sequenceParameterSetNALUnits
    memcpy(&body[i], sps, (size_t) sps_len);
    i += sps_len;
    /*pps*/
    body[i++] = 0x01;//numOfPictureParameterSets:PPS 的个数,计算方法是 numOfPictureParameterSets & 0x1F,实际测试时发现总为E1，计算结果为1.
    body[i++] = (unsigned char) ((pps_len >> 8) & 0xff);//pictureParameterSetLength:PPS的长度
    body[i++] = (unsigned char) ((pps_len) & 0xff);//PPS
    memcpy(&body[i], pps, (size_t) pps_len);
    i += pps_len;


    //块消息头的类型（4种）
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    //消息类型ID（1-7协议控制；8，9音视频；10以后为AMF编码消息）
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    //时间戳是绝对值还是相对值
    packet->m_hasAbsTimestamp = 0;
    //块流ID，Audio 和 Video通道
    packet->m_nChannel = 0x04;
    //记录了每一个tag相对于第一个tag（File Header）的相对时间。
    //以毫秒为单位。而File Header的time stamp永远为0。
    packet->m_nTimeStamp = 0;
    //消息流ID, last 4 bytes in a long header, 不在这里配置
    //packet->m_nInfoField2 = -1;
    //Payload Length
    packet->m_nBodySize = (uint32_t) body_size;
    //加入到RTMPPacket发送队列
    add_rtmp_packet(packet);
}

/**
 * 打包h264的图像(IPB)帧数据->NALU->RTMPPacket队列
 */
void add_common_frame(unsigned char *buf ,int len)
{
    // 每个NALU之间通过startcode（起始码）进行分隔，起始码分成两种：0x000001（3Byte）或者0x00000001（4Byte）。
    // 如果NALU对应的Slice为一帧的开始就用0x00000001，否则就用0x000001。
    //去掉起始码(界定符)
    if(buf[2] == 0x00){  //00 00 00 01
        buf += 4;
        len -= 4;
    }else if(buf[2] == 0x01){ // 00 00 01
        buf += 3;
        len -= 3;
    }
    int body_size = len + 9;
    RTMPPacket *packet = malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    // 获取packet对象当中的m_body指针
    char * body = packet->m_body;
    //buf[0] NAL Header与运算，获取type，根据type判断关键帧和普通帧
    //当NAL头信息中，type（第一个字节的前5位）等于5，说明这是关键帧NAL单元
    int type = buf[0] & 0x1f;
    //Inter Frame 帧间压缩 普通帧
    body[0] = 0x27;//VideoHeaderTag:FrameType(2=Inter Frame)+CodecID(7=AVC)
    //IDR I帧图像
    if (type == NAL_SLICE_IDR) {
        body[0] = 0x17;//VideoHeaderTag:FrameType(1=key frame)+CodecID(7=AVC)
    }
    //AVCPacketType = 1
    body[1] = 0x01; /*nal unit,NALUs（AVCPacketType == 1)*/
    body[2] = 0x00; //composition time 0x000000 24bit
    body[3] = 0x00;
    body[4] = 0x00;
    //写入NALU信息，右移8位，一个字节的读取？
    body[5] = (len >> 24) & 0xff;
    body[6] = (len >> 16) & 0xff;
    body[7] = (len >> 8) & 0xff;
    body[8] = (len) & 0xff;
    /*copy data*/
    memcpy(&body[9], buf, (size_t) len);

    //块消息头的类型（4种）
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    //消息类型ID（1-7协议控制；8，9音视频；10以后为AMF编码消息）
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    //时间戳是绝对值还是相对值
    packet->m_hasAbsTimestamp = 0;
    //块流ID，Audio和Vidio通道
    packet->m_nChannel = 0x04;
    //记录了每一个tag相对于第一个tag（File Header）的相对时间。
    packet->m_nTimeStamp = RTMP_GetTime() - gRtmpPusher->start_time;
    //消息流ID, last 4 bytes in a long header, 不在这里配置
    //packet->m_nInfoField2 = -1;
    //Payload Length
    packet->m_nBodySize = (uint32_t) body_size;
    //加入到RTMPPacket发送队列
    add_rtmp_packet(packet);
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////// native method implementation ////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedVideoData
        (JNIEnv *env, jobject jobj, jbyteArray array)
{
    if(gRtmpPusher == NULL )
        return;
    //int array_len = (*env)->GetArrayLength(env, array);
    //int option_len = gRtmpPusher->width * gRtmpPusher->height * 3/2;
    //if(array_len < option_len)
    //    return;

    int y_len = gRtmpPusher->width * gRtmpPusher->height;
    int u_len = y_len / 4;
    int v_len = y_len / 4;

    //NV21->YUV420P
    jbyte* nv21_buffer = (*env)->GetByteArrayElements(env,array,NULL);
    uint8_t* y = gRtmpPusher->pic_in.img.plane[0];
    uint8_t* u = gRtmpPusher->pic_in.img.plane[1];
    uint8_t* v = gRtmpPusher->pic_in.img.plane[2];
    memcpy(y, nv21_buffer, (size_t) y_len);
    for (int i = 0; i < u_len; i++) {
        //NV21 先v后u
        *(v + i) = (uint8_t) *(nv21_buffer + y_len + i * 2);
        *(u + i) = (uint8_t) *(nv21_buffer + y_len + i * 2 + 1);
    }

    x264_nal_t *nal = NULL; //h264编码得到NALU数组
    int n_nal = -1; //NALU的个数
    //进行h264编码
    if(x264_encoder_encode(gRtmpPusher->x264_encoder,&nal, &n_nal,
                           &(gRtmpPusher->pic_in), &(gRtmpPusher->pic_out)) < 0){
        LOGE("%s","编码失败");
        return;
    } else {
        // 初始化编码器的时候，pic_in->i_pts = 0
        // 每编码一帧 i_pts累加1
        gRtmpPusher->pic_in.i_pts += 1;
    }
    //使用rtmp协议将h264编码的视频数据发送给流媒体服务器
    //帧分为关键帧和普通帧，为了提高画面的纠错率，关键帧应都包含SPS和PPS数据
    int sps_len=0, pps_len=0;
    unsigned char sps[256];
    unsigned char pps[256];
    memset(sps,0,256);
    memset(pps,0,256);

    //遍历NALU数组，根据NALU的类型判断
    for(int i=0; i < n_nal; i++){
        if(nal[i].i_type == NAL_SPS) {
            //复制SPS数据
            sps_len = nal[i].i_payload - 4;
            memcpy(sps,nal[i].p_payload + 4, (size_t) sps_len); //不复制四字节起始码
            if(sps_len>0 && pps_len>0) {
                //发送序列信息
                //h264关键帧会包含SPS和PPS数据
                add_param_sequence(pps,sps,pps_len,sps_len);
            }
        }else if(nal[i].i_type == NAL_PPS){
            //复制PPS数据
            pps_len = nal[i].i_payload - 4;
            memcpy(pps,nal[i].p_payload + 4, (size_t) pps_len); //不复制四字节起始码
            if(sps_len>0 && pps_len>0) {
                //发送序列信息
                //h264关键帧会包含SPS和PPS数据
                add_param_sequence(pps,sps,pps_len,sps_len);
            }
        }else{
            //发送普通帧信息
            add_common_frame(nal[i].p_payload, nal[i].i_payload);
        }
    }
    (*env)->ReleaseByteArrayElements(env, array, nv21_buffer, 0);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_prepareVideoEncoder(JNIEnv *env, jobject jobj,
                                                        jint width, jint height, jint bitrate, jint fps)
{
    if(gRtmpPusher == NULL) {
        gRtmpPusher = (RtmpPusher*)calloc(1, sizeof(RtmpPusher));
    } else if(gRtmpPusher->x264_encoder != NULL){
        return;
    }
    gRtmpPusher->bitrate = bitrate;
    gRtmpPusher->fps = fps;
    gRtmpPusher->width = width;
    gRtmpPusher->height = height;
    // 参照源码example.c 的模板代码
    ////////////////////////////////////////////////////////////////
    x264_param_t param;
    //x264_param_default_preset设置   两个字符串所代表的参数集在源码目录的common/base.c当中
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    //编码输入的像素格式YUV420P
    param.i_csp = X264_CSP_I420; // X264_CSP_NV21
    param.i_width  = width;
    param.i_height = height;
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    //恒定码率，会尽量控制在固定码率
    param.rc.i_rc_method = X264_RC_CRF;
    param.rc.i_bitrate = bitrate / 1000; //码率(比特率) 单位Kbps
    param.rc.i_vbv_max_bitrate = (int) (bitrate / 1000 * 1.2); //瞬时最大码率
    //码率控制不通过timebase和timestamp，而是fps
    param.b_vfr_input = 0;
    param.i_fps_num = (uint32_t) fps; //* 帧率分子
    param.i_fps_den = 1; //* 帧率分母
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
    param.i_threads = 1;//并行编码线程数量，0默认为多线程
    //是否把SPS和PPS放入每一个关键帧
    //置为1表示每个I帧都重复带SPS/PPS头，为了提高图像的纠错能力
    param.b_repeat_headers = 1;
    //设置Level级别
    param.i_level_idc = 51;
    //设置Profile档次
    //baseline级别，没有B帧   字符串所代表的参数集在源码目录的common/base.c当中
    x264_param_apply_profile(&param, "baseline");

    //x264_picture_t（输入图像）初始化
    x264_picture_alloc(&(gRtmpPusher->pic_in), param.i_csp, param.i_width, param.i_height);

    gRtmpPusher->pic_in.i_pts = 0;
    //打开编码器
    gRtmpPusher->x264_encoder = x264_encoder_open(&param);
    if(gRtmpPusher->x264_encoder){
        LOGI("打开x264编码器成功...");
    }
}




JNIEXPORT void JNICALL Java_org_zzrblog_ffmp_RtmpPusher_feedAudioData
        (JNIEnv *env, jobject jObj, jbyteArray j_pcm_array, jint len)
{
    if(gRtmpPusher==NULL || gRtmpPusher->faac_encoder==NULL)
        return;
    // 传入的pcm_array 编码是 ENCODING_PCM_16BIT

    // 16位 两字节 相当于 short
    int16_t * pcm_input = (int16_t *)malloc(gRtmpPusher->nInputSamples * sizeof(int16_t));
    // short * pcm_input = (short*) malloc(gRtmpPusher->nInputSamples * sizeof(short));
    // 8位 1字节 相当于 char / byte
    uint8_t * aac_output = (uint8_t *)malloc(gRtmpPusher->nMaxOutputBytes * sizeof(uint8_t));
    //unsigned char * aac_output = (unsigned char*) malloc(gRtmpPusher->nMaxOutputBytes * sizeof(unsigned char));

    jbyte* pPcmArray = (*env)->GetByteArrayElements(env, j_pcm_array, 0);
    int nByteCount = 0;  //缓存计算位
    unsigned int pcm_short_buf_size = (unsigned int) len / 2; //传入的byte数组包含多少个16位的pcm编码采样数据
    int16_t* pcm_short_buf = (int16_t*) pPcmArray;

    while (nByteCount < pcm_short_buf_size) {
        // 针对每nInputSamples个16位pcm数据操作
        unsigned int audioLength = gRtmpPusher->nInputSamples; //aac编码输入的默认采样个数
        if ((nByteCount + gRtmpPusher->nInputSamples) >= pcm_short_buf_size) {
            audioLength = pcm_short_buf_size - nByteCount;
        }
        for (int i = 0; i < audioLength; i++) {
            //每次从传入的pcm音频队列中读出量化位数为8的pcm数据。
            int16_t s = (pcm_short_buf + nByteCount)[i];
            pcm_input[i] = s << 8;//用8个二进制位来表示一个采样量化点（模数转换）
        }
        nByteCount += gRtmpPusher->nInputSamples;
        //利用FAAC进行编码
        int byteslen = faacEncEncode(gRtmpPusher->faac_encoder,
                                     pcm_input, audioLength,
                                     aac_output, gRtmpPusher->nMaxOutputBytes);
        if (byteslen < 1) {
            continue;
        }
        // 从aac_output中得到编码后的aac数据流，放到数据队列
        add_aac_body(aac_output, byteslen);
    }
    //处理完当前批pcm数据了，释放资源
    (*env)->ReleaseByteArrayElements(env, j_pcm_array, pPcmArray, NULL);
    if (aac_output)
        free(aac_output);
    if (pcm_input)
        free(pcm_input);
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_prepareAudioEncoder(JNIEnv *env, jobject jobj,
                                                     jint sampleRateInHz, jint channelNum)
{
    if(gRtmpPusher == NULL) {
        gRtmpPusher = (RtmpPusher*)calloc(1, sizeof(RtmpPusher));
    }else if(gRtmpPusher->faac_encoder != NULL){
        return;
    }
    gRtmpPusher->sampleRateInHz = sampleRateInHz;
    gRtmpPusher->channelNum = channelNum;
    // 初始化faac音频编码器
    gRtmpPusher->faac_encoder = faacEncOpen((unsigned long) sampleRateInHz,
                                            (unsigned int) channelNum,
                                            &(gRtmpPusher->nInputSamples),
                                            &(gRtmpPusher->nMaxOutputBytes));
    if(!gRtmpPusher->faac_encoder){
        LOGE("音频编码器打开失败");
        return;
    }
    //设置音频编码参数
    faacEncConfigurationPtr pFaacConfigure = faacEncGetCurrentConfiguration(gRtmpPusher->faac_encoder);
    pFaacConfigure->mpegVersion = MPEG4;
    pFaacConfigure->allowMidside = 1;//是否允许MidSide Coding(详情百度)
    pFaacConfigure->aacObjectType = LOW;//设置AAC类型
    pFaacConfigure->outputFormat = 0; //输出是否包含ADTS头
    // RAW_STREAM = 0, ADTS_STREAM=1  (ADTS可以实现单帧单独解码，raw由于缺少头无法单帧解码，因此无法做实时传输)
    pFaacConfigure->useTns = 1; //是否使用瞬时噪声定形滤波器(具体作用不是很清楚)
    pFaacConfigure->useLfe = 0; //是否允许一个声道为低频通道
    pFaacConfigure->bitRate = 48000;  //设置比特率
	pFaacConfigure->inputFormat = FAAC_INPUT_16BIT; //设置输入PCM格式
    pFaacConfigure->quantqual = 100;//数字信号的质量
    pFaacConfigure->bandWidth = 0; //频宽
    pFaacConfigure->shortctl = SHORTCTL_NORMAL;
    if(!faacEncSetConfiguration(gRtmpPusher->faac_encoder, pFaacConfigure)) {
        LOGE("%s","音频编码器配置失败..");
        return;
    }
    LOGI("%s","音频编码器配置成功");
}




JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_startPush(JNIEnv *env, jobject jobj, jstring url_jstr)
{
    if(gRtmpPusher == NULL) {
        LOGE("%s","请先调用函数：prepareAudioEncoder&prepareVideoEncoder");
        return;
    }
    if(gRtmpPusher->rtmp_push_thread_id > 0) {
        LOGE("%s","rtmp_push_thread已启动，请stopPush");
        return;
    }
    // 初始化的操作
    const char* url_cstr = (*env)->GetStringUTFChars(env,url_jstr,NULL);
    // 复制url_cstr内容到rtmp_path
    gRtmpPusher->rtmp_path = malloc(strlen(url_cstr) + 1);
    memset(gRtmpPusher->rtmp_path, 0, strlen(url_cstr)+1);
    memcpy(gRtmpPusher->rtmp_path, url_cstr, strlen(url_cstr));
    //初始化互斥锁与条件变量
    pthread_mutex_init(&(gRtmpPusher->mutex), NULL);
    pthread_cond_init(&(gRtmpPusher->cond), NULL);
    //创建RTMPPacket双向队列
    create_queue();
    //启动消费者线程（从队列中不断拉取RTMPPacket发送给流媒体服务器）
    pthread_create(&(gRtmpPusher->rtmp_push_thread_id), NULL, rtmp_push_thread, NULL);
    gRtmpPusher->stop_rtmp_push_thread = 0;
    //RTMPPacket队列已经创建，发送AAC序列信息头
    add_aac_sequence_header();

    (*env)->ReleaseStringUTFChars(env,url_jstr,url_cstr);
    LOGI("%s","RtmpPusher startPush !");
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_stopPush(JNIEnv *env, jobject jobj)
{
    if(gRtmpPusher->rtmp_path!=NULL) {
        free(gRtmpPusher->rtmp_path);
        gRtmpPusher->rtmp_path == NULL; //防止野指针
    }
    gRtmpPusher->stop_rtmp_push_thread = 1;
    pthread_join(gRtmpPusher->rtmp_push_thread_id, NULL);
    // 让startPush重新初始化吧
    destroy_queue();
    pthread_mutex_destroy(&(gRtmpPusher->mutex));
    pthread_cond_destroy(&(gRtmpPusher->cond));
    LOGI("%s","RtmpPusher stopPush !");
}

JNIEXPORT void JNICALL
Java_org_zzrblog_ffmp_RtmpPusher_release(JNIEnv *env, jobject jobj)
{
    x264_encoder_close(gRtmpPusher->x264_encoder);
    faacEncClose(gRtmpPusher->faac_encoder);
    free(gRtmpPusher);
}