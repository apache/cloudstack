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

#ifndef _XEN_LIB_SOCKET_STREAM_H_
#define _XEN_LIB_SOCKET_STREAM_H_

#ifndef __KERNEL__
#include "iostream.h"
#include <stdio.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

/** Data associated with a socket stream. */
typedef struct SocketData {
    /** The socket file descriptor. */
    int fd;
    /** Source address from last read (recvfrom). */
    struct sockaddr_in saddr;
    /** Destination address for writes (sendto). */
    struct sockaddr_in daddr;
    /** Write flags (sendto). */
    int flags;
    /** Buffer size. */
    int buf_n;
    /** Buffer for formatted printing. */
    char buf[1024];
} SocketData;

extern IOStream *socket_stream_new(int fd);
extern int socket_stream_data(IOStream *io, SocketData **data);
extern int socket_stream_check(IOStream *io);
extern int socket_stream_set_addr(IOStream *io, struct sockaddr_in *addr);
extern int socket_stream_set_flags(IOStream *io, int flags);

#endif
#endif /* !_XEN_LIB_SOCKET_STREAM_H_ */
