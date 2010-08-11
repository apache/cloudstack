/*
 * Copyright (C) 2004, 2005 Mike Wray <mike.wray@hp.com>
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
#ifndef __VNET_TUNNEL_H__
#define __VNET_TUNNEL_H__

#ifdef __KERNEL__
#include <linux/types.h>
#include <asm/atomic.h>

#else

//#include <linux/types.h>
#include "sys_kernel.h"
#include "spinlock.h"

#endif

#include <if_varp.h>

struct sk_buff;
struct Tunnel;

typedef struct TunnelType {
    const char *name;
    int (*open)(struct Tunnel *tunnel);
    int (*send)(struct Tunnel *tunnel, struct sk_buff *skb);
    void (*close)(struct Tunnel *tunnel);
} TunnelType;

typedef struct TunnelStats {
    int bytes;
    int packets;
    int dropped_bytes;
    int dropped_packets;
} TunnelStats;

typedef struct TunnelKey {
    struct VnetId vnet;
    struct VarpAddr addr;
} TunnelKey;

typedef struct Tunnel {
    /** Key identifying the tunnel. Must be first. */
    struct TunnelKey key;
    /** Reference count. */
    atomic_t refcount;
    /** Tunnel type. */
    struct TunnelType *type;
    /** Statistics. */
    struct TunnelStats send_stats;
    /** Type-dependent state. */
    void *data;
    /** Underlying tunnel (may be null). */
    struct Tunnel *base;
} Tunnel;

extern void Tunnel_free(struct Tunnel *tunnel);

/** Decrement the reference count, freeing if zero.
 *
 * @param tunnel tunnel (may be null)
 */
static inline void Tunnel_decref(struct Tunnel *tunnel){
    if(!tunnel) return;
    if(atomic_dec_and_test(&tunnel->refcount)){
        Tunnel_free(tunnel);
    }
}

/** Increment the reference count.
 *
 * @param tunnel tunnel (may be null)
 */
static inline void Tunnel_incref(struct Tunnel *tunnel){
    if(!tunnel) return;
    atomic_inc(&tunnel->refcount);
}

extern int Tunnel_init(void);
extern int Tunnel_lookup(struct VnetId *vnet, struct VarpAddr *addr, struct Tunnel **tunnel);
extern int Tunnel_open(struct VnetId *vnet, struct VarpAddr *addr,
                       int (*ctor)(struct VnetId *vnet,
                                   struct VarpAddr *addr,
                                   struct Tunnel **ptunnel),
                       struct Tunnel **ptunnel);
extern int Tunnel_add(struct Tunnel *tunnel);
extern int Tunnel_del(struct Tunnel *tunnel);
extern void Tunnel_print(struct Tunnel *tunnel);
extern int Tunnel_send(struct Tunnel *tunnel, struct sk_buff *skb);

extern int Tunnel_create(struct TunnelType *type, struct VnetId *vnet, struct VarpAddr *addr,
                         struct Tunnel *base, struct Tunnel **tunnelp);

extern int tunnel_module_init(void);
extern void tunnel_module_exit(void);

#endif /* !__VNET_TUNNEL_H__ */
