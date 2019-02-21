//
// Created by zzr on 2019/2/21.
//

#include <stdint.h>
#include <malloc.h>
#include "queue.h"

// 静态全局变量 http://c.biancheng.net/view/301.html
//如果我们希望全局变量仅限于在本源文件中使用，在其他源文件中不能引用，
//也就是说限制其作用域只在定义该变量的源文件内有效，而在同一源程序的其他源文件中不能使用。
//这时，就可以通过在全局变量之前加上关键字 static 来实现，使全局变量被定义成为一个静态全局变量。
//这样就可以避免在其他源文件中引起的错误。也就起到了对其他源文件进行隐藏与隔离错误的作用，有利于模块化程序设计。
static QNode *phead = NULL; // 注意，表头一般不存放元素值
static int count = 0;

//当程序是由多个C文件组成时，二者的作用域，即可使用的范围不同。
//其中，静态函数(带static的)，只能在本文件中使用，无法跨文件。
//而非静态函数(不带static的)，可以在任何一个文件中使用。当在其它文件中使用时，需要做函数声明后再使用。
static QNode* create_node(void *pval) {
    QNode *pnode = NULL;
    pnode = (QNode *) malloc(sizeof(QNode));
    if (pnode) {
        // 默认的，pnode的前一节点和后一节点都指向它自身
        pnode->prev = pnode->next = pnode;
        // 节点的值为pval
        pnode->p = pval;
    }
    return pnode;
}


// 获取“双向链表中第index位置的节点”
static QNode* get_node(int index) {
    if (index < 0 || index >= count) {
        return NULL;
    }
    if (index <= (count / 2)) {
        int i = 0;
        QNode *pnode = phead->next;
        while ((i++) < index)
            pnode = pnode->next;
        return pnode;
    }
    int j = 0;
    int rindex = count - index - 1;
    QNode *rnode = phead->prev;
    while ((j++) < rindex)
        rnode = rnode->prev;
    return rnode;
}












// 新建“双向链表”の表头节点。成功，返回0；否则，返回-1。
int create_queue() {
    phead = create_node(NULL);
    if (!phead) {
        return -1;
    }
    // 设置“节点个数”为0
    count = 0;
    return 0;
}

// 撤销“双向链表”。成功，返回0；否则，返回-1。
int destroy_queue() {
    if (!phead) {
        return -1;
    }
    QNode *pnode = phead->next;
    QNode *ptmp = NULL;
    while (pnode != phead) {
        ptmp = pnode;
        pnode = pnode->next;
        free(ptmp);
    }
    free(phead);
    phead = NULL;
    count = 0;
    return 0;
}

// “双向链表是否为空”
int queue_is_empty() {
    return count == 0;
}

// 返回“双向链表的大小”
int queue_size() {
    return count;
}

// 获取“双向链表中第1个元素的值”
void* queue_get_first() {
    return queue_get(0);
}
// 获取“双向链表中最后1个元素”
void* queue_get_last() {
    return queue_get(count - 1);
}
// 获取“双向链表中第index位置的元素”。成功，返回节点值；否则，返回-1。
void* queue_get(int index) {
    QNode *pindex = get_node(index);
    if (!pindex) {
        return NULL;
    }
    return pindex->p;
}


// 将“pval”插入到表头位置
int queue_insert_first(void *pval) {
    QNode *pnode = create_node(pval);
    if (!pnode)
        return -1;
    pnode->prev = phead;
    pnode->next = phead->next;
    phead->next->prev = pnode;
    phead->next = pnode;
    count++;
    return 0;
}
// 将“pval”插入到末尾位置
int queue_append_last(void *pval) {
    QNode *pnode = create_node(pval);
    if (!pnode)
        return -1;
    pnode->next = phead;
    pnode->prev = phead->prev;
    phead->prev->next = pnode;
    phead->prev = pnode;
    count++;
    return 0;
}

// 将“pval”插入到index位置。成功，返回0；否则，返回-1。
int queue_insert(int index, void* pval) {
    // 插入表头
    if (index == 0)
        return queue_insert_first(pval);
    // 获取要插入的位置对应的节点
    QNode *pindex = get_node(index);
    if (!pindex)
        return -1;
    // 创建“节点”
    QNode *pnode = create_node(pval);
    if (!pnode)
        return -1;
    pnode->prev = pindex->prev;
    pnode->next = pindex;
    pindex->prev->next = pnode;
    pindex->prev = pnode;
    // 节点个数+1
    count++;
    return 0;
}


// 删除第一个节点
int queue_delete_first() {
    return queue_delete(0);
}
// 删除组后一个节点
int queue_delete_last() {
    return queue_delete(count - 1);
}
// 删除“双向链表中index位置的节点”。成功，返回0；否则，返回-1。
int queue_delete(int index) {
    QNode *pindex = get_node(index);
    if (!pindex) {
        return -1;
    }
    pindex->next->prev = pindex->prev;
    pindex->prev->next = pindex->next;
    free(pindex);
    count--;
    return 0;
}
