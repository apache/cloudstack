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

/** @file
 * An IOStream implementation using printk() for output.
 * Input is not implemented.
 */
#ifdef __KERNEL__

#include <linux/config.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/types.h>
#include <linux/errno.h>
#include <linux/slab.h>
#include <linux/spinlock.h>

#include "kernel_stream.h"
#include "allocate.h"

/** Number of characters in the output buffer.
 * The kernel uses 1024 for printk, so that should suffice.
 */
#define BUF_N 1024

/** State for a kernel stream. */
typedef struct KernelData {
    /** Stream lock. We need a lock to serialize access to the stream. */
    spinlock_t lock;
    /** Saved flags for locking. */
    unsigned long flags;
    /** Size of the output buffer. */
    int buf_n;
    /** Output buffer. */
    char buf[BUF_N];
} KernelData;

static int kernel_write(IOStream *s, const void *msg, size_t n);
static void kernel_free(IOStream *s);
static void kernel_stream_lock(IOStream *s);
static void kernel_stream_unlock(IOStream *s);

/** Methods for a kernel stream. Output only. */
static const IOMethods kernel_methods = {
    write:  kernel_write,
    free:   kernel_free,
    lock:   kernel_stream_lock,
    unlock: kernel_stream_unlock,
};

/** Shared state for kernel streams.
 * All implementations write using printk, so we can use
 * shared state and avoid allocating it.
 */
static const KernelData kernel_data = {
    lock:  SPIN_LOCK_UNLOCKED,
    flags: 0,
    buf_n: BUF_N,
};

/** Stream for kernel printk. */
static IOStream iokernel = {
    methods: &kernel_methods,
    data:    &kernel_data,
    nofree:  1,
};

/** Stream for kernel printk. */
IOStream *iostdout = &iokernel;

/** Stream for kernel printk. */
IOStream *iostdin = &iokernel;

/** Stream for kernel printk. */
IOStream *iostderr = &iokernel;

/** Get an output-only stream implementation using
 * printk(). The stream uses static storage, and must not be freed.
 *
 * @return kernel stream
 */
IOStream get_stream_kernel(void){
    return iokernel;
}

/** Obtain the lock on the stream state.
 *
 * @param kdata stream state
 */
static inline void KernelData_lock(KernelData *kdata){
    spin_lock_irqsave(&kdata->lock, kdata->flags);
}

/** Release the lock on the stream state.
 *
 * @param kdata stream state
 */
static inline void KernelData_unlock(KernelData *kdata){
    spin_unlock_irqrestore(&kdata->lock, kdata->flags);
}

/** Get the stream state.
 *
 * @param s kernel stream
 * @return stream state
 */
static inline KernelData *get_kernel_data(IOStream *s){
    return (KernelData*)s->data;
}

/** Obtain the lock on the stream state.
 *
 * @param s stream
 */
void kernel_stream_lock(IOStream *s){
    KernelData_lock(get_kernel_data(s));
}

/** Release the lock on the stream state.
 *
 * @param s stream
 */
void kernel_stream_unlock(IOStream *s){
    KernelData_unlock(get_kernel_data(s));
}

/** Write to a kernel stream.
 *
 * @param stream kernel stream
 * @param format print format
 * @param args print arguments
 * @return result of the print
 */
static int kernel_write(IOStream *stream, const void *buf, size_t n){
    KernelData *kdata = get_kernel_data(stream);
    int k;
    k = kdata->buf_n - 1;
    if(n < k) k = n;
    memcpy(kdata->buf, buf, k);
    kdata->buf[k] = '\0';
    printk(kdata->buf);
    return k;
}

/** Free a kernel stream.
 * Frees the internal state of the stream.
 * Do not call this unless the stream was dynamically allocated.
 * Do not call this on a stream returned from get_stream_kernel().
 *
 * @param io stream to free
 */
static void kernel_free(IOStream *io){
    KernelData *kdata;
    if(io == &iokernel) return;
    kdata = get_kernel_data(io);
    memset(kdata, 0, sizeof(*kdata));
    deallocate(kdata);
}
#endif /* __KERNEL__ */




