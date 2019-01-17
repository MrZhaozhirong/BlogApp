//
// Created by nicky on 2019/1/9.
//
#pragma once
#ifndef BLOGAPP_AVPACKET_BUFFER_H
#define BLOGAPP_AVPACKET_BUFFER_H

#define BUFFER_SIZE 50

#include "include/libavcodec/avcodec.h"

typedef struct _AVPacket_buffer{
    //长度
    int size;

    //AVPacket指针的数组，总共有size个
    AVPacket * * avpacket_ptr_array;

    //push或者pop元素时需要按照先后顺序，依次进行
    int write_current_position;
    int read_current_position;

    // 互斥锁（为了解决size空间较小的时候，前数据比后数据覆盖）
    pthread_mutex_t mutex;
    // 条件变量（为了解决size空间较小的时候，前数据比后数据覆盖）
    pthread_cond_t cond;

    //保留字段
    void * reserve;

} AVPacket_buffer, AV_PACKET_BUFFER;

// 创建AVPacket缓冲区
AV_PACKET_BUFFER* alloc_avpacket_buffer(int size);
// 回收AVPacket缓冲区
void free_avpacket_buffer(AV_PACKET_BUFFER* pAVPacketBuffer);
// 获取一个写入AVPacket的单元
AVPacket* get_write_packet(AV_PACKET_BUFFER * pAVPacketBuffer);
// 获取一个读取AVPacket的单元
AVPacket* get_read_packet(AV_PACKET_BUFFER * pAVPacketBuffer);

#endif //BLOGAPP_AVPACKET_BUFFER_H
