/*
 * Copyright (C) 2004 Mike Wray <mike.wray@hp.com>
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
 * An IOStream implementation using fds.
 */
#ifndef __KERNEL__

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include "allocate.h"
#include "fd_stream.h"

#define MODULE_NAME "fd_stream"
#define DEBUG 1
//#undef DEBUG
#include "debug.h"

static int fd_read(IOStream *s, void *buf, size_t n);
static int fd_write(IOStream *s, const void *buf, size_t n);
static int fd_error(IOStream *s);
static int fd_close(IOStream *s);
static void fd_free(IOStream *s);
static int fd_flush(IOStream *s);

/** Methods used by a fd IOStream. */
static const IOMethods fd_methods = {
    read:  fd_read,
    write: fd_write,
    error: fd_error,
    close: fd_close,
    free:  fd_free,
    flush: fd_flush,
};

/** Get the fd data.
 * 
 * @param io fd stream
 * @return data
 */
static inline FDData * fd_data(IOStream *io){
    return (FDData *)io->data;
}

/** Test if a stream is a fd stream.
 *
 * @param io stream
 * @return 0 if a fd stream, -EINVAL if not
 */
int fd_stream_check(IOStream *io){
    return (io && io->methods == &fd_methods ? 0 : -EINVAL);
}

/** Get the data for a fd stream.
 *
 * @param io stream
 * @param data return value for the data
 * @return 0 if a fd stream, -EINVAL if not
 */
int fd_stream_data(IOStream *io, FDData **data){
    int err = fd_stream_check(io);
    if(err){
        *data = NULL;
    } else {
        *data = fd_data(io);
    }
    return err;
}


/** Write to the underlying fd.
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to write
 * @return number of bytes written
 */
static int fd_write(IOStream *s, const void *buf, size_t n){
    FDData *data = fd_data(s);
    int k;
    k = write(data->fd, buf, n);
    return k;
}

/** Read from the underlying stream;
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to read
 * @return number of bytes read
 */
static int fd_read(IOStream *s, void *buf, size_t n){
    FDData *data = fd_data(s);
    int k;
    k = read(data->fd, buf, n);
    //printf("> fd_read> buf=%p n=%d --> k=%d\n", buf, n, k);
    return k;
}

/** Flush the fd (no-op).
 *
 * @param s fd stream
 * @return 0 on success, error code otherwise
 */
static int fd_flush(IOStream *s){
    return 0;
}

/** Check if a fd stream has an error (no-op).
 *
 * @param s fd stream
 * @return 1 if has an error, 0 otherwise
 */
static int fd_error(IOStream *s){
    return 0;
}

/** Close a fd stream.
 *
 * @param s fd stream to close
 * @return result of the close
 */
static int fd_close(IOStream *s){
    FDData *data = fd_data(s);
    return close(data->fd);
}

/** Free a fd stream.
 *
 * @param s fd stream
 */
static void fd_free(IOStream *s){
    FDData *data = fd_data(s);
    deallocate(data);
}

/** Create an IOStream for a fd.
 *
 * @param fd fd to wtap
 * @return new IOStream using fd for i/o
 */
IOStream *fd_stream_new(int fd){
    int err = -ENOMEM;
    IOStream *io = NULL;
    FDData *data = NULL;

    io = ALLOCATE(IOStream);
    if(!io) goto exit;
    io->methods = &fd_methods;
    data = ALLOCATE(FDData);
    if(!data) goto exit;
    io->data = data;
    data->fd = fd;
    err = 0;
  exit:
    if(err){
        if(io){
            if(data) deallocate(data);
            deallocate(io);
            io = NULL;
        }
    }
    return io;
}

#endif
