/*
 * Copyright (C) 2001 - 2004 Mike Wray <mike.wray@hp.com>
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

#include "allocate.h"

/** @file
 * Support for allocating memory.
 * Usable from user code or kernel code (with __KERNEL__ defined).
 * In user code will use GC if USE_GC is defined.
 */

#ifdef __KERNEL__
/*----------------------------------------------------------------------------*/
#  include <linux/config.h>
#  include <linux/slab.h>
#  include <linux/string.h>
#  include <linux/types.h>

#  define DEFAULT_TYPE    0
#  define MALLOC(n, type) kmalloc(n, type)
#  define FREE(ptr)       kfree(ptr)

/*----------------------------------------------------------------------------*/
#else /* ! __KERNEL__ */

#  include <stdlib.h>
#  include <string.h>

#  define DEFAULT_TYPE    0

#ifdef USE_GC
#  include "gc.h"
#  define MALLOC(n, typ)  GC_malloc(n)
#  define FREE(ptr)       (ptr=NULL)
//typedef void *GC_PTR;
//GC_PTR (*GC_oom_fn)(size_t n);
#else
#  define MALLOC(n, type) malloc(n)
#  define FREE(ptr)       free(ptr)
#endif

/*----------------------------------------------------------------------------*/
#endif

/** Function to call when memory cannot be allocated. */
AllocateFailedFn *allocate_failed_fn = NULL;

/** Allocate memory and zero it.
 * The type is only relevant when calling from kernel code,
 * from user code it is ignored.
 * In kernel code the values accepted by kmalloc can be used:
 * GFP_USER, GFP_ATOMIC, GFP_KERNEL.
 *
 * @param size number of bytes to allocate
 * @param type memory type to allocate (kernel only)
 * @return pointer to the allocated memory or zero
 * if malloc failed
 */
void *allocate_type(int size, int type){
    void *p = MALLOC(size, type);
    if(p){
        memzero(p, size);
    } else if(allocate_failed_fn){
        allocate_failed_fn(size, type);
    }
    return p;
}

/** Allocate memory and zero it.
 *
 * @param size number of bytes to allocate
 * @return pointer to the allocated memory or zero
 * if malloc failed
 */
void *allocate(int size){
    return allocate_type(size, DEFAULT_TYPE);
}

/** Free memory allocated by allocate().
 * No-op if 'p' is null.
 *
 * @param p memory to free
 */
void deallocate(void *p){
    if(p){
        FREE(p);
    }
}

/** Set bytes to zero.
 * No-op if 'p' is null.
 *
 * @param p memory to zero
 * @param size number of bytes to zero
 */
void memzero(void *p, int size){
    if(p){
        memset(p, 0, (size_t)size);
    }
}

