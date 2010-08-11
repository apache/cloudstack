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
#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/skbuff.h>
#include <linux/slab.h>

#include <skb_context.h>

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

SkbContext *SkbContext_create(u32 vnet, u32 addr, int protocol, void *data,
                              void (*free_fn)(SkbContext *)){
    SkbContext *context = NULL;

    context = kmalloc(sizeof(SkbContext), GFP_ATOMIC);
    if(!context) goto exit;
    context->vnet = vnet;
    context->addr = addr;
    context->protocol = protocol;
    context->data = data;
    context->free_fn = free_fn;
    context->next = NULL;
    atomic_set(&context ->refcount, 1);
  exit:
    return context;
}
                                       
void SkbContext_free(SkbContext *context){
    if(!context) return;
    if(context->next) SkbContext_decref(context->next);
    if(context->free_fn) context->free_fn(context);
    context->vnet = 0;
    context->addr = 0;
    context->protocol = 0;
    context->free_fn = NULL;
    context->data = NULL;
    context->next = NULL;
    kfree(context);
}

int SkbContext_push(SkbContext **val, u32 vnet, u32 addr, int protocol,
                    void *data, void (*free_fn)(SkbContext *)){
    int err = 0;
    SkbContext *context = NULL;

    dprintf("> vnet=%u addr=%u.%u.%u.%u protocol=%d\n",
            vnet, NIPQUAD(addr), protocol);
    context = SkbContext_create(vnet, addr, protocol, data, free_fn);
    if(!context){
        err = -ENOMEM;
        goto exit;
    }
    context->next = *val;
    *val = context;
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

int skb_push_context(struct sk_buff *skb, u32 vnet, u32 addr, int protocol,
                     void *data, void (*free_fn)(SkbContext *)){
    int err = 0;
    //SkbContext *ctxt = SKB_CONTEXT(skb);
    dprintf("> skb=%p\n", skb);

    //err = SkbContext_push(&ctxt, vnet, addr, protocol, data, free_fn); //todo fixme
    //SKB_CONTEXT(skb) = ctxt;//todo fixme
    dprintf("< err=%d\n", err);
    return err;
}
                                       

