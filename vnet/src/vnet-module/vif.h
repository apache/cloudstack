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
#ifndef _VNET_VIF_H_
#define _VNET_VIF_H_

#ifdef __KERNEL__
#include <asm/atomic.h>
#else
#include "spinlock.h"
#endif

#include <if_varp.h>
struct IOStream;

/** Key for entries in the vif table. */
typedef struct VifKey {
    struct VnetId vnet;
    struct Vmac vmac;
} VifKey;

typedef struct Vif {
    struct VnetId vnet;
    struct Vmac vmac;
    atomic_t refcount;
    unsigned long timestamp;
    int flags;
} Vif;

enum {
    VIF_FLAG_PERSISTENT = 1,
};

extern void vif_print(struct IOStream *io);

extern void vif_decref(struct Vif *vif);
extern void vif_incref(struct Vif *vif);

extern int vif_create(struct VnetId *vnet, struct Vmac *vmac, int flags, struct Vif **vif);
extern int vif_lookup(struct VnetId *vnet, struct Vmac *vmac, struct Vif **vif);
extern int vif_update(struct VnetId *vnet, struct Vmac *vmac);
extern int vif_remove(struct VnetId *vnet, struct Vmac *vmac);
extern void vif_purge(void);
extern int vif_remove_vnet(struct VnetId *vnet);

extern int vif_init(void);
extern void vif_exit(void);

#endif
