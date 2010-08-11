/*
 * Copyright (C) 2005 Mike Wray <mike.wray@hp.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
#ifndef _VNET_SYS_KERNEL_H_
#define _VNET_SYS_KERNEL_H_

/** @file Compatibility replacements for some kernel defs.
 */

#include <assert.h>
#include <asm/types.h>
//#include <sys/types.h>
#include <unistd.h>

#define printk              printf

#define likely(x)           x
#define unlikely(x)         x
#define current_text_addr() NULL

#define BUG_ON(x)           assert(x)    
#define BUG()               BUG_ON(1)
#define kmalloc(n, m)       allocate_type(n, m)
#define kfree(p)            deallocate(p)
#define in_atomic()         0

#define __init
#define __exit

#define module_init(x)
#define module_exit(x)
#define MODULE_LICENSE(x)
#define MODULE_PARM(v, t)
#define module_param(v, t, s)
#define MODULE_PARM_DESC(v, s)

enum {
    GFP_USER,
    GFP_ATOMIC,
    GFP_KERNEL,
};

typedef signed char s8;
typedef unsigned char u8;

typedef signed short s16;
typedef unsigned short u16;

typedef signed int s32;
typedef unsigned int u32;

typedef signed long long s64;
typedef unsigned long long u64;

#include "allocate.h"

#endif /* ! _VNET_SYS_KERNEL_H_ */
