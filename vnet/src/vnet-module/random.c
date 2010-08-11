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
#include <linux/config.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/sched.h>
#include <linux/random.h>

#include "hash_table.h"

#define MODULE_NAME "RANDOM"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file
 * Source of randomness.
 * Current implementation is not enough.
 * Needs to be cryptographically strong.
 */

static unsigned long seed = 0;
static unsigned long count = 0;

/** Contribute some random bytes.
 *
 * @param src bytes to contribute
 * @param src_n number of bytes
 */
void add_random_bytes(const void *src, int src_n){
    ++count;
    seed = hash_hvoid(seed, &count, sizeof(count));
    seed = hash_hvoid(seed, src, src_n);
}

/** Get one random byte.
 *
 * @return random byte
 */
int get_random_byte(void){
    int tmp = jiffies;
    add_random_bytes(&tmp, sizeof(tmp));
    return seed;
}

#ifndef __KERNEL__
/* Get some random bytes.
 *
 * @param dst destination for the bytes
 * @param dst_n number of bytes to get
 */
void get_random_bytes(void *dst, int dst_n){
    int i;
    char *p = (char *)dst;
    for(i = 0; i < dst_n; i++){
        *p++ = get_random_byte();
    }
}
#endif

int __init random_module_init(void){
    int dummy;
    int tmp = jiffies;
    seed = (unsigned long)&dummy;
    add_random_bytes(&tmp, sizeof(tmp));
    return 0;
}

void __exit random_module_exit(void){
}

