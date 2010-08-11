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
 * An IOStream implementation using sockets.
 */
#ifndef __KERNEL__

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include "allocate.h"
#include "socket_stream.h"

#define MODULE_NAME "sock"
#define DEBUG 0
//#undef DEBUG
#include "debug.h"

static int socket_read(IOStream *s, void *buf, size_t n);
static int socket_write(IOStream *s, const void *buf, size_t n);
static int socket_error(IOStream *s);
static int socket_close(IOStream *s);
static void socket_free(IOStream *s);
static int socket_flush(IOStream *s);

/** Methods used by a socket IOStream. */
static const IOMethods socket_methods = {
    read:  socket_read,
    write: socket_write,
    error: socket_error,
    close: socket_close,
    free:  socket_free,
    flush: socket_flush,
};

/** Get the socket data.
 * 
 * @param io socket stream
 * @return data
 */
static inline SocketData * socket_data(IOStream *io){
    return (SocketData *)io->data;
}

/** Test if a stream is a socket stream.
 *
 * @param io stream
 * @return 0 if a socket stream, -EINVAL if not
 */
int socket_stream_check(IOStream *io){
    return (io && io->methods == &socket_methods ? 0 : -EINVAL);
}

/** Get the data for a socket stream.
 *
 * @param io stream
 * @param data return value for the data
 * @return 0 if a socket stream, -EINVAL if not
 */
int socket_stream_data(IOStream *io, SocketData **data){
    int err = socket_stream_check(io);
    if(err){
        *data = NULL;
    } else {
        *data = socket_data(io);
    }
    return err;
}

/** Set the destination address for a socket stream.
 *
 * @param io stream
 * @param addr address
 * @return 0 if a socket stream, -EINVAL if not
 */
int socket_stream_set_addr(IOStream *io, struct sockaddr_in *addr){
    int err = 0;
    SocketData *data = NULL;
    err = socket_stream_data(io, &data);
    if(!err){
        data->daddr = *addr;
    }
    return err;
}

/** Set the send flags for a socket stream.
 *
 * @param io stream
 * @param flags flags
 * @return 0 if a socket stream, -EINVAL if not
 */
int socket_stream_set_flags(IOStream *io, int flags){
    int err = 0;
    SocketData *data = NULL;
    err = socket_stream_data(io, &data);
    if(!err){
        data->flags = flags;
    }
    return err;
}

/** Write to the underlying socket using sendto.
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to write
 * @return number of bytes written
 */
static int socket_write(IOStream *s, const void *buf, size_t n){
    SocketData *data = socket_data(s);
    struct sockaddr *daddr = (struct sockaddr *)&data->daddr;
    socklen_t daddr_n = sizeof(data->daddr);
    int k;
    dprintf("> sock=%d addr=%s:%d n=%d\n",
            data->fd, inet_ntoa(data->daddr.sin_addr), ntohs(data->daddr.sin_port), n);
    if(0){
        struct sockaddr_in self = {};
        socklen_t self_n;
        getsockname(data->fd, (struct sockaddr *)&self, &self_n);
        dprintf("> sockname sock=%d %s:%d\n",
                data->fd, inet_ntoa(self.sin_addr), ntohs(self.sin_port));
    }
    k = sendto(data->fd, buf, n, data->flags, daddr, daddr_n);
    dprintf("> sendto=%d\n", k);
    return k;
}

/** Read from the underlying stream using recv();
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to read
 * @return number of bytes read
 */
static int socket_read(IOStream *s, void *buf, size_t n){
    SocketData *data = socket_data(s);
    int k;
    struct sockaddr *saddr = (struct sockaddr *)&data->saddr;
    socklen_t saddr_n = sizeof(data->saddr);
    k = recvfrom(data->fd, buf, n, data->flags, saddr, &saddr_n);
    return k;
}

/** Flush the socket (no-op).
 *
 * @param s socket stream
 * @return 0 on success, error code otherwise
 */
static int socket_flush(IOStream *s){
    return 0;
}

/** Check if a socket stream has an error (no-op).
 *
 * @param s socket stream
 * @return 1 if has an error, 0 otherwise
 */
static int socket_error(IOStream *s){
    // Read SOL_SOCKET/SO_ERROR ?
    return 0;
}

/** Close a socket stream.
 *
 * @param s socket stream to close
 * @return result of the close
 */
static int socket_close(IOStream *s){
    SocketData *data = socket_data(s);
    return close(data->fd);
}

/** Free a socket stream.
 *
 * @param s socket stream
 */
static void socket_free(IOStream *s){
    SocketData *data = socket_data(s);
    deallocate(data);
}

/** Create an IOStream for a socket.
 *
 * @param fd socket to wtap
 * @return new IOStream using fd for i/o
 */
IOStream *socket_stream_new(int fd){
    int err = -ENOMEM;
    IOStream *io = NULL;
    SocketData *data = NULL;

    io = ALLOCATE(IOStream);
    if(!io) goto exit;
    io->methods = &socket_methods;
    data = ALLOCATE(SocketData);
    if(!data) goto exit;
    io->data = data;
    data->fd = fd;
    data->buf_n = sizeof(data->buf);
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
