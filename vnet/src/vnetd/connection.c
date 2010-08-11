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

#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "allocate.h"
#include "connection.h"
#include "file_stream.h"
#include "socket_stream.h"

#define MODULE_NAME "conn"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** Initialize a file stream from a file desciptor.
 *
 * @param fd file descriptor
 * @param mode file mode
 * @param buffered make the stream buffered if 1, unbuffered if 0
 * @param io return parameter for the stream
 * @return 0 on success, error code otherwise
 */
int stream_init(int fd, const char *mode, int buffered, IOStream **io){
    int err = 0;
    *io = file_stream_fdopen(fd, mode);
    if(!*io){
        err = -errno;
        perror("fdopen");
        goto exit;
    }
    if(!buffered){
        // Make unbuffered.
        err = file_stream_setvbuf(*io, NULL, _IONBF, 0);
        if(err){
            err = -errno;
            perror("setvbuf");
            goto exit;
        }
    }
  exit:
    if(err && *io){
        IOStream_close(*io);
        *io = NULL;
    }
    return err;
}

ConnList * ConnList_add(ConnList *l, Conn *conn){
    ConnList *v;
    v = ALLOCATE(ConnList);
    v->conn = conn;
    v->next =l;
    return v;
}

ConnList * ConnList_del(ConnList *l, Conn *conn){
    ConnList *prev, *curr, *next;
    for(prev = NULL, curr = l; curr; prev = curr, curr = next){
        next = curr->next;
        if(curr->conn == conn){
            if(prev){
                prev->next = curr->next;
            } else {
                l = curr->next;
            }
        }
    }
    return l;
}

void ConnList_close(ConnList *l){
    for( ; l; l = l->next){
        Conn_close(l->conn);
    }
}
    
void ConnList_select(ConnList *l, SelectSet *set){
    for( ; l; l = l->next){
        Conn_select(l->conn, set);
    }
}

/** Handle connections according to a select set.
 *
 * @param set indicates ready connections
 */
ConnList * ConnList_handle(ConnList *l, SelectSet *set){
    ConnList *prev, *curr, *next;
    Conn *conn;
    int err;

    for(prev = NULL, curr = l; curr; prev = curr, curr = next){
        next = curr->next;
        conn = curr->conn;
        err = Conn_handle(conn, set);
        if(err){
            if(prev){
                prev->next = curr->next;
            } else {
                l = curr->next;
            }
        }
    }
    return l;
}

Conn *Conn_new(int (*fn)(Conn *conn, int mode), void *data){
    Conn *conn;
    conn = ALLOCATE(Conn);
    conn->fn = fn;
    conn->data = data;
    return conn;
}

int Conn_handler(Conn *conn, int mode){
    int err = 0;
    dprintf(">\n");
    if(conn->fn){
        err = conn->fn(conn, mode);
    } else {
        dprintf("> no handler\n");
        err = -ENOSYS;
    }
    if(err < 0){
        dprintf("> err=%d, closing %d\n", err, conn->sock);
        Conn_close(conn);
    }
    dprintf("< err=%d\n", err);
    return err;
}

int Conn_handle(Conn *conn, SelectSet *set){
    int err = 0;
    int mode = SelectSet_in(set, conn->sock);

    dprintf("> sock=%d mode=%d\n", conn->sock, mode);
    if(mode){
        err = Conn_handler(conn, mode);

    }
    return err;
}

void Conn_select(Conn *conn, SelectSet *set){
    dprintf("> sock=%d\n", conn->sock);
    SelectSet_add(set, conn->sock, conn->mode);
}

/** Initialize a connection.
 *
 * @param conn connection
 * @param sock socket
 * @param ipaddr ip address
 * @return 0 on success, error code otherwise
 */
int Conn_init(Conn *conn, int sock, int type, int mode, struct sockaddr_in addr){
    int err = 0;
    conn->addr = addr;
    conn->type = type;
    conn->mode = mode;
    conn->sock = sock;
    if(type == SOCK_STREAM){
        err = stream_init(sock, "r", 0, &conn->in);
        if(err) goto exit;
        err = stream_init(sock, "w", 0, &conn->out);
        if(err) goto exit;
    } else {
        conn->in = socket_stream_new(sock);
        conn->out = socket_stream_new(sock);
        socket_stream_set_addr(conn->out, &addr);
    }
  exit:
    if(err) eprintf("< err=%d\n", err);
    return err;
}

/** Open a connection.
 *
 * @param conn connection
 * @param socktype socket type
 * @param ipaddr ip address to connect to
 * @param port port
 * @return 0 on success, error code otherwise
 */
int Conn_connect(Conn *conn, int socktype, struct in_addr ipaddr, uint16_t port){
    int err = 0;
    int sock;
    struct sockaddr_in addr_in;
    struct sockaddr *addr = (struct sockaddr *)&addr_in;
    socklen_t addr_n = sizeof(addr_in);
    dprintf("> addr=%s:%d\n", inet_ntoa(ipaddr), ntohs(port));
    sock = socket(AF_INET, socktype, 0);
    if(sock < 0){
        err = -errno;
        goto exit;
    }
    addr_in.sin_family = AF_INET;
    addr_in.sin_addr = ipaddr;
    addr_in.sin_port = port;
    err = connect(sock, addr, addr_n);
    if(err) goto exit;
    err = Conn_init(conn, sock, socktype, 0, addr_in);
  exit:
    if(err){
        perror("Conn_connect");
        eprintf("< err=%d\n", err);
    }
    return err;
}

/** Close a connection.
 *
 * @param conn connection
 */
void Conn_close(Conn *conn){
    if(!conn) return;
    if(conn->in) IOStream_close(conn->in);
    if(conn->out) IOStream_close(conn->out);
    shutdown(conn->sock, 2);
}

/** Set socket option to reuse address.
 */
int setsock_reuse(int sock, int val){
    int err = 0;
    err = setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &val, sizeof(val));
    if(err < 0){
        err = -errno;
        perror("setsockopt SO_REUSEADDR");
    }
    return err;
}

/** Set socket broadcast option.
 */
int setsock_broadcast(int sock, int val){
    int err = 0;
    err = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &val, sizeof(val));
    if(err < 0){
        err = -errno;
        perror("setsockopt SO_BROADCAST");
    }
    return err;
}

/** Join a socket to a multicast group.
 */
int setsock_multicast(int sock, uint32_t iaddr, uint32_t maddr){
    int err = 0;
    struct ip_mreqn mreq = {};
    int mloop = 0;
    // See 'man 7 ip' for these options.
    mreq.imr_multiaddr.s_addr = maddr;       // IP multicast address.
    mreq.imr_address.s_addr   = iaddr;       // Interface IP address.
    mreq.imr_ifindex = 0;                    // Interface index (0 means any).
    err = setsockopt(sock, SOL_IP, IP_MULTICAST_LOOP, &mloop, sizeof(mloop));
    if(err < 0){
        err = -errno;
        perror("setsockopt IP_MULTICAST_LOOP");
        goto exit;
    }
    err = setsockopt(sock, SOL_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if(err < 0){
        err = -errno;
        perror("setsockopt IP_ADD_MEMBERSHIP");
        goto exit;
    }
  exit:
    return err;
}

/** Set a socket's multicast ttl (default is 1).
 */
int setsock_multicast_ttl(int sock, uint8_t ttl){
    int err = 0;
    err = setsockopt(sock, SOL_IP, IP_MULTICAST_TTL, &ttl, sizeof(ttl));
    if(err < 0){
        err = -errno;
        perror("setsockopt IP_MULTICAST_TTL");
    }
    return err;
}

int setsock_pktinfo(int sock, int val){
    int err = 0;
    err = setsockopt(sock, SOL_IP, IP_PKTINFO, &val, sizeof(val));
    if(err < 0){
        err = -errno;
        perror("setsockopt IP_PKTINFO");
    }
    return err;
}

char * socket_flags(int flags){
    static char s[6];
    int i = 0;
    s[i++] = (flags & VSOCK_CONNECT   ? 'c' : '-');
    s[i++] = (flags & VSOCK_BIND      ? 'b' : '-');
    s[i++] = (flags & VSOCK_REUSE     ? 'r' : '-');
    s[i++] = (flags & VSOCK_BROADCAST ? 'B' : '-');
    s[i++] = (flags & VSOCK_MULTICAST ? 'M' : '-');
    s[i++] = '\0';
    return s;
}

/** Create a socket.
 * The flags can include VSOCK_REUSE, VSOCK_BROADCAST, VSOCK_CONNECT.
 *
 * @param socktype socket type
 * @param saddr address
 * @param port port
 * @param flags flags
 * @param val return value for the socket connection
 * @return 0 on success, error code otherwise
 */
int create_socket(int socktype, uint32_t saddr, uint32_t port, int flags, int *val){
    int err = 0;
    int sock = 0;
    struct sockaddr_in addr_in;
    struct sockaddr *addr = (struct sockaddr *)&addr_in;
    socklen_t addr_n = sizeof(addr_in);
    int reuse, bcast;

    //dprintf(">\n");
    reuse = (flags & VSOCK_REUSE);
    bcast = (flags & VSOCK_BROADCAST);
    addr_in.sin_family      = AF_INET;
    addr_in.sin_addr.s_addr = saddr;
    addr_in.sin_port        = port;
    dprintf("> flags=%s addr=%s port=%d\n", socket_flags(flags),
            inet_ntoa(addr_in.sin_addr), ntohs(addr_in.sin_port));

    sock = socket(AF_INET, socktype, 0);
    if(sock < 0){
        err = -errno;
        goto exit;
    }
    if(reuse){
        err = setsock_reuse(sock, reuse);
        if(err < 0) goto exit;
    }
    if(bcast){
        err = setsock_broadcast(sock, bcast);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_CONNECT){
        err = connect(sock, addr, addr_n);
        if(err < 0){
            err = -errno;
            perror("connect");
            goto exit;
        }
    }
    if(flags & VSOCK_BIND){
        err = bind(sock, addr, addr_n);
        if(err < 0){
            err = -errno;
            perror("bind");
            goto exit;
        }
    }
    {
        struct sockaddr_in self = {};
        socklen_t self_n = sizeof(self);
        getsockname(sock, (struct sockaddr *)&self, &self_n);
        dprintf("> sockname sock=%d addr=%s port=%d reuse=%d bcast=%d\n",
                sock, inet_ntoa(self.sin_addr), ntohs(self.sin_port),
                reuse, bcast);
    }
  exit:
    *val = (err ? -1 : sock);
    //dprintf("< err=%d\n", err);
    return err;
}

int Conn_socket(int socktype, uint32_t saddr, uint32_t port, int flags, Conn **val){
    int err;
    int sock;
    struct sockaddr_in addr_in;
    Conn *conn;

    err = create_socket(socktype, saddr, port, flags, &sock);
    if(err) goto exit;
    conn = Conn_new(NULL, NULL);
    addr_in.sin_family      = AF_INET;
    addr_in.sin_addr.s_addr = saddr;
    addr_in.sin_port        = port;
    Conn_init(conn, sock, socktype, 0, addr_in);
  exit:
    *val = (err ? NULL : conn);
    return err;
}
