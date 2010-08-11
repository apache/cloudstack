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
 * IOStream subtype for input and output to strings.
 * Usable from user or kernel code (with __KERNEL__ defined).
 */

#include "sys_string.h"
#include "string_stream.h"
#include "allocate.h"

static int string_error(IOStream *io);
static int string_close(IOStream *io);
static void string_free(IOStream *io);
static int string_write(IOStream *io, const void *msg, size_t n);
static int string_read(IOStream *io, void *buf, size_t n);

/** Methods for a string stream. */
static IOMethods string_methods = {
    read:  string_read,
    write: string_write,
    error: string_error,
    close: string_close,
    free:  string_free,
};

/** Get the string stream state.
 *
 * @param io string stream
 * @return state
 */
static inline StringData *get_string_data(IOStream *io){
    return (StringData*)io->data;
}

static int string_write(IOStream *io, const void *msg, size_t n){
    StringData *data = get_string_data(io);
    int k;

    k = data->end - data->out;
    if(n > k) n = k;
    memcpy(data->out, msg, n);
    data->out += n;
    return n;
}

static int string_read(IOStream *io, void *buf, size_t n){
    StringData *data = get_string_data(io);
    int k;

    k = data->end - data->in;
    if(n > k) n = k;
    memcpy(buf, data->in, k);
    data->in += n;
    return n;
}

/** Test if a string stream has an error.
 *
 * @param io string stream
 * @return 0 if ok, error code otherwise
 */
static int string_error(IOStream *io){
    StringData *data = get_string_data(io);
    return data->out == NULL;
}

/** Close a string stream.
 *
 * @param io string stream
 * @return 0
 */
static int string_close(IOStream *io){
    StringData *data = get_string_data(io);
    data->in = NULL;
    data->out = NULL;
    return 0;
}

/** Free a string stream.
 * The stream state is freed, but the underlying string is not.
 *
 * @param io string stream
 */
static void string_free(IOStream *io){
    StringData *data = get_string_data(io);
    memzero(data, sizeof(*data));
    deallocate(data);
}

/** Get the methods to use for a string stream.
 *
 * @return methods
 */
IOMethods *string_stream_get_methods(void){
    return &string_methods;
}

/** Initialise a string stream, usually from static data.
 * If the stream and StringData should be freed when
 * the stream is closed, unset io->nofree.
 * The string is not freed on close.
 *
 * @param io address of IOStream to fill in
 * @param data address of StringData to fill in
 * @param s string to use
 * @param n length of the string
 */
void string_stream_init(IOStream *io, StringData *data, char *s, int n){
    if(data && io){
        memzero(data, sizeof(*data));
        data->string = (char*)s;
        data->in = data->string;
        data->out = data->string;
        data->size = n;
        data->end = data->string + n;
        memzero(io, sizeof(*io));
        io->methods = &string_methods;
        io->data = data;
        io->nofree = 1;
    }
}

/** Allocate and initialise a string stream.
 * The stream is freed on close, but the string is not.
 *
 * @param s string to use
 * @param n length of the string
 * @return new stream (free using IOStream_free)
 */
IOStream *string_stream_new(char *s, int n){
    int ok = 0;
    StringData *data = ALLOCATE(StringData);
    IOStream *io = ALLOCATE(IOStream);
    if(data && io){
        ok = 1;
        string_stream_init(io, data, s, n);
        io->nofree = 0;
    }
    if(!ok){
        deallocate(data);
        deallocate(io);
        io = NULL;
    }
    return io;
}
