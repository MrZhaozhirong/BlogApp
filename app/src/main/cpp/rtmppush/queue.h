//
// Created by zzr on 2019/2/21.
//

#ifndef BLOGAPP_QUEUE_H
#define BLOGAPP_QUEUE_H

typedef struct queue_node {
    struct queue_node* prev;
    struct queue_node* next;
    void *p; //节点的值
} QNode;

// 新建“双向链表”。成功，返回表头；否则，返回NULL
extern int create_queue();
// 撤销“双向链表”。成功，返回0；否则，返回-1
extern int destroy_queue();
// “双向链表是否为空”。为空的话返回1；否则，返回0。
extern int queue_is_empty();
// 返回“双向链表的大小”
extern int queue_size();
// 获取“双向链表中第1个元素”。成功，返回节点指针；否则，返回NULL。
extern void* queue_get_first();
// 获取“双向链表中最后1个元素”。成功，返回节点指针；否则，返回NULL。
extern void* queue_get_last();

// 将“value”插入到表头位置。成功，返回0；否则，返回-1。
extern int queue_insert_first(void *pval);
// 将“value”插入到末尾位置。成功，返回0；否则，返回-1。
extern int queue_append_last(void *pval);

// 删除组后一个节点。成功，返回0；否则，返回-1
extern int queue_delete_last();
// 删除第一个节点。成功，返回0；否则，返回-1
extern int queue_delete_first();

// 获取“双向链表中第index位置的元素”。成功，返回节点指针；否则，返回NULL。
extern void* queue_get(int index);
// 将“value”插入到index位置。成功，返回0；否则，返回-1。
extern int queue_insert(int index, void *pval);
// 删除“双向链表中index位置的节点”。成功，返回0；否则，返回-1
extern int queue_delete(int index);

#endif //BLOGAPP_QUEUE_H
