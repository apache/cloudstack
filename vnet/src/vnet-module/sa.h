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
#ifndef __VNET_SA_H__
#define __VNET_SA_H__

#ifdef __KERNEL__
#include <linux/types.h>
#include <linux/crypto.h>

#else

#include "sys_kernel.h"

#endif

struct Vnet;
struct VarpAddr;
struct Tunnel;

#ifndef CRYPTO_MAX_KEY_BYTES
#define CRYPTO_MAX_KEY_BYTES            64
#define CRYPTO_MAX_KEY_BITS             (CRYPTO_MAX_KEY_BYTES * 8)
#endif

#ifndef CRYPTO_MAX_ALG_NAME
#define CRYPTO_MAX_ALG_NAME		64
#endif

typedef struct SALimits {
    u64 bytes_soft;
    u64 bytes_hard;
    u64 packets_soft;
    u64 packets_hard;
} SALimits;

typedef struct SACounts {
    u64 bytes;
    u64 packets;
    u32 integrity_failures;
} SACounts;

typedef struct SAReplay {
    int replay;
    u32 send_seq;
    u32 recv_seq;
    u32 bitmap;
    u32 replay_window;
} SAReplay;

typedef struct SAKey {
    char name[CRYPTO_MAX_ALG_NAME];
    int bits;
    char key[CRYPTO_MAX_KEY_BYTES];
} SAKey;

typedef struct SAKeying {
    u8 state;
    u8 dying;
} SAKeying;

typedef struct SAIdent {
    u32 id;
    u32 spi;
    u32 addr;
    u32 protocol;
} SAIdent;

struct SAType;

/** Security assocation (SA). */
typedef struct SAState {
    atomic_t refcount;
    spinlock_t lock;
    /** Identifier. */
    struct SAIdent ident;
    /** Security flags. */
    int security;
    /** Keying state. */
    struct SAKeying keying;
    /** Byte counts etc. */
    struct SACounts counts;
    /** Byte limits etc. */
    struct SALimits limits;
    /** Replay protection. */
    struct SAReplay replay;
    /** Digest algorithm. */
    struct SAKey digest;
    /** Cipher algorithm. */
    struct SAKey cipher;
    /** Compress algorith. */
    struct SAKey compress;
    /** SA type (ESP, AH). */
    struct SAType *type;
    /** Data for the SA type to use. */
    void *data;
} SAState;
    
typedef struct SAType {
    char *name;
    int protocol;
    int (*init)(SAState *state, void *args);
    void (*fini)(SAState *state);
    int (*recv)(SAState *state, struct sk_buff *skb);
    int (*send)(SAState *state, struct sk_buff *skb, struct Tunnel *tunnel);
    u32 (*size)(SAState *state, int size);
} SAType;

/** Information needed to create an SA.
 * Unused algorithms have zero key size.
 */
typedef struct SAInfo {
    /** Identifier. */
    SAIdent ident;
    /** Security flags. */
    int security;
    /** Digest algorithm and key. */
    SAKey digest;
    /** Cipher algorithm and key. */
    SAKey cipher;
    /** Compress algorithm and key. */
    SAKey compress;
    /** SA lifetime limits. */
    SALimits limits;
    /** Replay protection window. */
    int replay_window;
} SAInfo;

enum sa_alg_type {
    SA_ALG_DIGEST = 1,
    SA_ALG_CIPHER = 2,
    SA_ALG_COMPRESS = 3,
};

extern int SAType_add(SAType *type);
extern int SAType_del(SAType *type);
extern int SAType_get(int protocol, SAType **type);

extern int sa_table_init(void);
extern void sa_table_exit(void);
extern int sa_table_delete(SAState *state);
extern int sa_table_add(SAState *state);
extern SAState * sa_table_lookup_spi(u32 spi, u32 protocol, u32 addr);
extern SAState * sa_table_lookup_id(u32 id);

/** Increment reference count.
 *
 * @param sa security association (may be null)
 */
static inline void SAState_incref(SAState *sa){
    if(!sa) return;
    atomic_inc(&sa->refcount);
}

/** Decrement reference count, freeing if zero.
 *
 * @param sa security association (may be null)
 */
static inline void SAState_decref(SAState *sa){
    if(!sa) return;
    if(atomic_dec_and_test(&sa->refcount)){
        sa->type->fini(sa);
        kfree(sa);
    }
}

extern SAState *SAState_alloc(void);
extern int SAState_init(SAIdent *id, SAState **statep);
extern int SAState_create(SAInfo *info, SAState **statep);

static inline int SAState_send(SAState *sa, struct sk_buff *skb, struct Tunnel *tunnel){
    return sa->type->send(sa, skb, tunnel);
}

static inline int SAState_recv(SAState *sa, struct sk_buff *skb){
    return sa->type->recv(sa, skb);
}

static inline int SAState_size(SAState *sa, int n){
    return sa->type->size(sa, n);
}

extern int sa_create(int security, u32 spi, u32 protocol, u32 addr, SAState **sa);
extern int sa_set(SAInfo *info, int update, SAState **val);
extern int sa_delete(int id);

enum {
    SA_AUTH = 1,
    SA_CONF = 2
};

enum {
    SA_STATE_ACQUIRE = 1,
    SA_STATE_VALID   = 2,
};

extern int sa_tunnel_create(struct Vnet *info, struct VarpAddr *addr,
                            struct Tunnel *base, struct Tunnel **tunnel);

#endif /* !__VNET_SA_H__ */
