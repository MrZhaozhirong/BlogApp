//
// Created by nicky on 2019/1/9.
//
#include "AVPacket_buffer.h"

// 创建AVPacket缓冲区
AV_PACKET_BUFFER* alloc_avpacket_buffer(int size)
{
    AV_PACKET_BUFFER* pAVPacketBuffer = (AV_PACKET_BUFFER*)malloc(sizeof(AV_PACKET_BUFFER));
    pAVPacketBuffer->size = size;
    pAVPacketBuffer->write_current_position = 0;
    pAVPacketBuffer->read_current_position = 0;
    //数组开辟空间
    pAVPacketBuffer->avpacket_ptr_array = malloc(sizeof(AVPacket*) * size);
    int i;
    for(i=0; i<size; i++){
        pAVPacketBuffer->avpacket_ptr_array[i] = av_packet_alloc();
        // must be freed using av_packet_free().
    }
    return pAVPacketBuffer;
}

// 回收AVPacket缓冲区
void free_avpacket_buffer(AV_PACKET_BUFFER* pAVPacketBuffer)
{
    for(int i=0; i<pAVPacketBuffer->size; i++){
        // void av_packet_free(AVPacket **pkt);
        av_packet_free(& (pAVPacketBuffer->avpacket_ptr_array[i]) );
    }
    free(pAVPacketBuffer->avpacket_ptr_array);
    free(pAVPacketBuffer);
}


// 获取下一个索引位置
int get_next(AV_PACKET_BUFFER * pAVPacketBuffer, int current){
    return (current + 1) % pAVPacketBuffer->size;
}

// 获取一个写入AVPacket的单元
AVPacket* get_write_packet(AV_PACKET_BUFFER * pAVPacketBuffer)
{
    int current = pAVPacketBuffer->write_current_position;
    pAVPacketBuffer->write_current_position = get_next(pAVPacketBuffer, current);
    return pAVPacketBuffer->avpacket_ptr_array[current];
}

// 获取一个读取AVPacket的单元
AVPacket* get_read_packet(AV_PACKET_BUFFER * pAVPacketBuffer)
{
    int current = pAVPacketBuffer->read_current_position;
    pAVPacketBuffer->read_current_position = get_next(pAVPacketBuffer, current);
    return pAVPacketBuffer->avpacket_ptr_array[current];
}