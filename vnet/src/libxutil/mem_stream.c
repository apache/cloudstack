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

/** @file
 * IOStream subtype for input and output to memory.
 * Usable from user or kernel code (with __KERNEL__ defined).
 */

#include "sys_string.h"
#include "mem_stream.h"
#include "allocate.h"

/** Internal state for a memory stream.
 *
 * The memory stream buffer is treated as a circular buffer.
 * The lo and hi markers indicate positions in the buffer, but
 * are not reduced modulo the buffer size. This avoids the ambiguity
 * between a full and empty buffer when using reduced values.
 *
 * If x is a marker, then buf + (x % buf_n) is the corresponding
 * pointer into the buffer. When the buffer is empty, lo == hi,
 * and the corresponding pointers are equal. When the buffer is
 * full, hi == lo + buf_n, and the corresponding pointers
 * are also equal.
 *
 * Data is written after the high pointer and read from the lo pointer.
 * The value hi - lo is the number of bytes in the buffer.
 */
typedef struct MemData {
    /** Data buffer. */
    char *buf;
    /** Low marker - start of readable area. */
    unsigned long lo;
    /** High marker - end of readable area, start of writeable area. */
    unsigned long hi;
    /** Size of the buffer. */
    unsigned int buf_n;
    /** Maximum size the buffer can grow to. */
    unsigned int buf_max;
    /** Error code. */
    int err;
} MemData;

/** Get number of bytes available to read.
 *
 * @param data mem stream
 * @return bytes
 */
static inline int mem_len(struct MemData *data){
    return data->hi - data->lo;
}

/** Get available space left in the buffer.
 *
 * @param data mem stream
 * @return bytes
 */
static inline int mem_room(struct MemData *data){
    return data->buf_n - mem_len(data);
}

/** Get a pointer to the start of the data in the buffer.
 *
 * @param data mem stream
 * @return lo pointer
 */
static inline char * mem_lo(struct MemData *data){
    return data->buf + (data->lo % data->buf_n);
}

/** Get a pointer to the end of the data in the buffer.
 *
 * @param data mem stream
 * @return hi pointer
 */
static inline char * mem_hi(struct MemData *data){
    return data->buf + (data->hi % data->buf_n);
}

/** Get a pointer to the end of the buffer.
 *
 * @param data mem stream
 * @return end pointer
 */
static inline char * mem_end(struct MemData *data){
    return data->buf + data->buf_n;
}

static int mem_error(IOStream *io);
static int mem_close(IOStream *io);
static void mem_free(IOStream *io);
static int mem_write(IOStream *io, const void *msg, size_t n);
static int mem_read(IOStream *io, void *buf, size_t n);

/** Minimum delta used to increment the buffer. */
static int delta_min = 256;

/** Methods for a memory stream. */
static IOMethods mem_methods = {
    read:  mem_read,
    write: mem_write,
    error: mem_error,
    close: mem_close,
    free:  mem_free,
};

/** Get the memory stream state.
 *
 * @param io memory stream
 * @return state
 */
static inline MemData *get_mem_data(IOStream *io){
    return (MemData*)io->data;
}

/** Get the number of bytes available to read.
 *
 * @param io memory stream
 * @return number of bytes
 */
int mem_stream_avail(IOStream *io){
    MemData *data = get_mem_data(io);
    return (data->err ? -data->err : mem_len(data));
}

/** Copy bytes from a memory stream into a buffer.
 *
 * @param data mem stream
 * @param buf buffer
 * @param n number of bytes to copy
 */
static void mem_get(MemData *data, char *buf, size_t n){
    char *start = mem_lo(data);
    char *end = mem_end(data);
    if (start + n < end) {
        memcpy(buf, start, n);
    } else {
        int k = end - start;
        memcpy(buf, start, k);
        memcpy(buf + k, data->buf, n - k);
    }
}

/** Copy bytes from a buffer into a memory stream.
 *
 * @param data mem stream
 * @param buf buffer
 * @param n number of bytes to copy
 */
static void mem_put(MemData *data, const char *buf, size_t n){
    char *start = mem_hi(data);
    char *end = mem_end(data);
    if(start + n < end){
        memcpy(start, buf, n);
    } else {
        int k = end - start;
        memcpy(start, buf, k);
        memcpy(data->buf, buf + k, n - k);
    }
}

/** Expand the buffer used by a memory stream.
 *
 * @param data mem stream
 * @param extra number of bytes to expand by
 * @return 0 on success, negative error otherwise
 */
static int mem_expand(MemData *data, size_t extra){
    int err = -ENOMEM;
    int delta = (extra < delta_min ? delta_min : extra);
    int buf_n;
    char *buf;
    if(data->buf_max > 0){
        int delta_max = data->buf_max - data->buf_n;
        if(delta > delta_max){
            delta = extra;
            if(delta > delta_max) goto exit;
        }
    }
    buf_n = data->buf_n + delta;
    buf = allocate(buf_n);
    if(!buf) goto exit;
    mem_get(data, buf, mem_len(data));
    data->hi = mem_len(data);
    data->lo = 0;
    deallocate(data->buf);
    data->buf = buf;
    data->buf_n = buf_n;
    err = 0;
  exit:
    if(err){
        data->err = -err;
    }
    return err;
}

/** Write bytes from a buffer into a memory stream.
 * The internal buffer is expanded as needed to hold the data,
 * up to the stream maximum (if specified). If the buffer cannot
 * be expanded -ENOMEM is returned.
 *
 * @param io mem stream
 * @param buf buffer
 * @param n number of bytes to write
 * @return number of bytes written on success, negative error code otherwise
 */
static int mem_write(IOStream *io, const void *msg, size_t n){
    int room;
    MemData *data = get_mem_data(io);
    if(data->err) return -data->err;
    room = mem_room(data);
    if(n > room){
        int err = mem_expand(data, n - room);
        if(err) return err;
    }
    mem_put(data, msg, n);
    data->hi += n;
    return n;
}

/** Read bytes from a memory stream into a buffer.
 *
 * @param io mem stream
 * @param buf buffer
 * @param n maximum number of bytes to read
 * @return number of bytes read on success, negative error code otherwise
 */
static int mem_read(IOStream *io, void *buf, size_t n){
    int k;
    MemData *data = get_mem_data(io);
    if(data->err) return -data->err;
    k = mem_len(data);
    if(n > k){
        n = k;
    }
    mem_get(data, buf, n);
    data->lo += n;
    return n;
}

/** Test if a memory stream has an error.
 *
 * @param io mem stream
 * @return 0 if ok, error code otherwise
 */
static int mem_error(IOStream *io){
    MemData *data = get_mem_data(io);
    return data->err;
}

/** Close a memory stream.
 *
 * @param io mem stream
 * @return 0
 */
static int mem_close(IOStream *io){
    MemData *data = get_mem_data(io);
    if(!data->err){
        data->err = ENOTCONN;
    }
    return 0;
}

/** Free a memory stream.
 *
 * @param io mem stream
 */
static void mem_free(IOStream *io){
    MemData *data = get_mem_data(io);
    deallocate(data->buf);
    memzero(data, sizeof(*data));
    deallocate(data);
}

/** Allocate and initialise a memory stream.
 *
 * @param buf_n initial buffer size (0 means default)
 * @param buf_max maximum buffer size (0 means no max)
 * @return new stream (free using IOStream_close)
 */
IOStream *mem_stream_new_size(size_t buf_n, size_t buf_max){
    int err = -ENOMEM;
    MemData *data = ALLOCATE(MemData);
    IOStream *io = NULL;
    if(!data) goto exit;
    io = ALLOCATE(IOStream);
    if(!io) goto exit;
    if(buf_n <= delta_min){
        buf_n = delta_min;
    }
    if(buf_max > 0 && buf_max < buf_n){
        buf_max = buf_n;
    }
    data->buf = allocate(buf_n);
    if(!data->buf) goto exit;
    data->buf_n = buf_n;
    data->buf_max = buf_max;
    io->methods = &mem_methods;
    io->data = data;
    io->nofree = 0;
    err = 0;
  exit:
    if(err){
        deallocate(data);
        deallocate(io);
        io = NULL;
    }
    return io;
}
