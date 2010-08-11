/*
 * Copyright (C) 2003 - 2004 Mike Wray <mike.wray@hp.com>.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or  (at your option) any later version. This library is 
 * distributed in the  hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */
#ifndef _VNET_CONNECTION_H_
#define _VNET_CONNECTION_H_

#include <netinet/in.h>

#include "iostream.h"
#include "select.h"

/** A connection.
 * The underlying transport is a socket. 
 * Contains in and out streams using the socket.
 */
typedef struct Conn {
    struct sockaddr_in addr;
    int sock;
    int type;
    int mode; // select mode
    IOStream *in;
    IOStream *out;
    int (*fn)(struct Conn *conn, int mode);
    void *data;
} Conn;

typedef struct ConnList {
    Conn *conn;
    struct ConnList *next;
} ConnList;

extern ConnList * ConnList_add(ConnList *l, Conn *conn);
extern ConnList * ConnList_del(ConnList *l, Conn *conn);
extern void ConnList_close(ConnList *l);
extern void ConnList_select(ConnList *l, SelectSet *set);
extern ConnList * ConnList_handle(ConnList *l, SelectSet *set);
    
extern Conn * Conn_new(int (*fn)(struct Conn *conn, int mode), void *data);
extern int Conn_init(Conn *conn, int sock, int type, int mode, struct sockaddr_in addr);
extern int Conn_connect(Conn *conn, int type, struct in_addr ipaddr, uint16_t port);
extern void Conn_select(Conn *conn, SelectSet *set);
extern int Conn_handle(Conn *conn, SelectSet *set);
extern void Conn_close(Conn *conn);
extern int Conn_socket(int socktype, uint32_t saddr, uint32_t port, int flags, Conn **val);

/** Socket flags. */
enum {
    VSOCK_REUSE     =  1,
    VSOCK_BIND      =  2,
    VSOCK_CONNECT   =  4,
    VSOCK_BROADCAST =  8,
    VSOCK_MULTICAST = 16,
 };

extern int create_socket(int socktype, uint32_t saddr, uint32_t port, int flags, int *sock);
extern int setsock_reuse(int sock, int val);
extern int setsock_broadcast(int sock, int val);
extern int setsock_multicast(int sock, uint32_t iaddr, uint32_t maddr);
extern int setsock_multicast_ttl(int sock, uint8_t ttl);
extern int setsock_pktinfo(int sock, int val);
extern char * socket_flags(int flags);

#endif /* ! _VNET_CONNECTION_H_ */
