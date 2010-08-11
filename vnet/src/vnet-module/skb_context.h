/*
 * Copyright (C) 2004 Mike Wray <mike.wray@hp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free software Foundation, Inc.,
 * 59 Temple Place, suite 330, Boston, MA 02111-1307 USA
 *
 */

#ifndef __VNET_SKB_CONTEXT_H__
#define __VNET_SKB_CONTEXT_H__

#ifdef __KERNEL__
#include <linux/config.h>
#include <linux/kernel.h>
#include <asm/atomic.h>
#include <linux/types.h>

//todo: fixme
#define SKB_CONTEXT(_skb) ((SkbContext *)(&(_skb)->cb[0]))

#else

#include "sys_kernel.h"
#include "spinlock.h"

//todo: fixme
#define SKB_CONTEXT(_skb) ((SkbContext *)NULL)

#endif

/** Structure used to record inbound processing path for skbs.
 * For example, the ETHERIP protocol handler can use this to
 * tell whether an inbound packet came through IPSEC ESP or not.
 */
typedef struct SkbContext {
    u32 vnet;
    u32 addr;
    int protocol;
    void *data;
    void (*free_fn)(struct SkbContext *);
    atomic_t refcount;
    struct SkbContext *next;
} SkbContext;

/** Decrement the reference count, freeing if zero.
 *
 * @param context context (may be null)
 */
static inline void SkbContext_decref(SkbContext *context){
    extern void SkbContext_free(SkbContext *context);
    if(!context) return;
    if(atomic_dec_and_test(&context->refcount)){
        SkbContext_free(context);
    }
}

/** Increment the reference count.
 *
 * @param context context (may be null)
 */
static inline void SkbContext_incref(SkbContext *context){
    if(!context) return;
    atomic_inc(&context->refcount);
}

extern SkbContext *SkbContext_create(u32 vnet, u32 addr, int protocol, void *data,
                                     void (*free_fn)(SkbContext *));

extern int SkbContext_push(SkbContext **val, u32 vnet, u32 addr, int protocol,
                           void *data, void (*free_fn)(SkbContext *));

struct sk_buff;
extern int skb_push_context(struct sk_buff *skb, u32 vnet, u32 addr, int protocol,
                            void *data, void (*free_fn)(SkbContext *));

#endif /* !__VNET_SKB_CONTEXT_H__ */ 
