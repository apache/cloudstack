/*
 * Copyright (C) 2004, 2005, 2006 Mike Wray <mike.wray@hp.com>
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
#include <linux/kernel.h>
#include <linux/types.h>
#include <linux/version.h>

#include <asm/uaccess.h>
#include <linux/net.h>
#include <linux/in.h>
#include <linux/ip.h>
#include <linux/sched.h>
#include <linux/file.h>
#include <linux/version.h>
#include <linux/smp_lock.h>
#include <net/sock.h>
#include <linux/kthread.h>

#include <if_varp.h>
#include <varp.h>
#include <vnet_forward.h>

/* Get macros needed to define system calls as functions in the kernel. */
#define __KERNEL_SYSCALLS__
int errno=0;
#define _GNU_SOURCE  

#include <linux/unistd.h>
#include <linux/syscalls.h>

#define MODULE_NAME "VARP"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file
 * Support for the VARP udp sockets.
 */

extern  int syscall(int number, ...);

#if LINUX_VERSION_CODE < KERNEL_VERSION(2,6,0)

/* Compensate for struct sock fields having 'sk_' added to them in 2.6. */
#define sk_receive_queue receive_queue
#define sk_sleep         sleep

/* Here because inline in 'socket.c' (2.4, in net.h for 2.6). */
#define sockfd_put(sock) fput((sock)->file)

#endif

static inline mm_segment_t change_fs(mm_segment_t fs){
    mm_segment_t oldfs = get_fs();
    set_fs(fs);
    return oldfs;
}

/** Define the fcntl() syscall. */
#if (LINUX_VERSION_CODE == KERNEL_VERSION(2,6,18) )
static inline _syscall3(int, fcntl,
                        unsigned int, fd, 
                        unsigned int, cmd,
                        unsigned long, arg)
#endif
/* Replicate the user-space socket API.
 * The parts we need anyway.
 *
 * Some architectures use socketcall() to multiplex the socket-related calls,
 * but others define individual syscalls instead.
 * Architectures using socketcall() define __ARCH_WANT_SYS_SOCKETCALL.
 * NB. x86_64 architecture asserts __ARCH_WANT_SYS_SOCKETCALL in error.
 */

#if defined(__ARCH_WANT_SYS_SOCKETCALL) && !defined(__x86_64__)

/* Define the socketcall() syscall.
 * Multiplexes all the socket-related calls.
 *
 * @param call socket call id
 * @param args arguments (upto 6)
 * @return call-dependent value
 */
static inline _syscall2(int, socketcall,
                        int, call,
                        unsigned long *, args)

int socket(int family, int type, int protocol){
    unsigned long args[6];
    
    args[0] = (unsigned long)family;
    args[1] = (unsigned long)type;
    args[2] = (unsigned long)protocol;
    return socketcall(SYS_SOCKET, args);
}

int bind(int fd, struct sockaddr *umyaddr, int addrlen){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)umyaddr;
    args[2] = (unsigned long)addrlen;
    return socketcall(SYS_BIND, args);
}

int connect(int fd, struct sockaddr *uservaddr, int addrlen){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)uservaddr;
    args[2] = (unsigned long)addrlen;
    return socketcall(SYS_CONNECT, args);
}

int sendto(int fd, void * buff, size_t len,
           unsigned flags, struct sockaddr *addr,
           int addr_len){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)buff;
    args[2] = (unsigned long)len;
    args[3] = (unsigned long)flags;
    args[4] = (unsigned long)addr;
    args[5] = (unsigned long)addr_len;
    return socketcall(SYS_SENDTO, args);
}

int recvfrom(int fd, void * ubuf, size_t size,
             unsigned flags, struct sockaddr *addr,
             int *addr_len){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)ubuf;
    args[2] = (unsigned long)size;
    args[3] = (unsigned long)flags;
    args[4] = (unsigned long)addr;
    args[5] = (unsigned long)addr_len;
    return socketcall(SYS_RECVFROM, args);
}

int setsockopt(int fd, int level, int optname, void *optval, int optlen){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)level;
    args[2] = (unsigned long)optname;
    args[3] = (unsigned long)optval;
    args[4] = (unsigned long)optlen;
    return socketcall(SYS_SETSOCKOPT, args);
}

int getsockopt(int fd, int level, int optname, void *optval, int *optlen){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)level;
    args[2] = (unsigned long)optname;
    args[3] = (unsigned long)optval;
    args[4] = (unsigned long)optlen;
    return socketcall(SYS_GETSOCKOPT, args);
}

int shutdown(int fd, int how){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)how;
    return socketcall(SYS_SHUTDOWN, args);
}

int getsockname(int fd, struct sockaddr *usockaddr, int *usockaddr_len){
    unsigned long args[6];
    
    args[0] = (unsigned long)fd;
    args[1] = (unsigned long)usockaddr;
    args[2] = (unsigned long)usockaddr_len;
    return socketcall(SYS_GETSOCKNAME, args);
}

#elif defined(__x86_64__)


int socket(int family, int type, int protocol){
    struct socket* sock;
    int err;
    int err2;
 
    err = sock_create_kern(family, type, protocol, &sock);
    if (err < 0) goto exit;
    dprintf("sock_create err=%d\n",err); 
    err = sock_map_fd(sock);
    if ((sock = sockfd_lookup(err, &err2 )) == NULL)
      dprintf("sock_create lookup err\n");

exit:
    return err;
}

int bind(int fd, struct sockaddr *address, int addrlen){
    struct socket *sock;
    int err;

   if((sock = sockfd_lookup(fd, &err))!=NULL) 
   {
     err = sock->ops->bind (sock, (struct sockaddr *)address,addrlen);
   }

   return err;
}

int connect(int fd, struct sockaddr *address, int addrlen){
  struct socket *sock;
  int err;

  sock = sockfd_lookup(fd, &err);
  if (!sock)
          goto out;

  err = sock->ops->connect(sock, (struct sockaddr *) address, addrlen,
                           sock->file->f_flags);
out:
  return err;

}


int setsockopt(int fd, int level, int optname, void *optval, int optlen){
  int err;
  struct socket *sock;

  if (optlen < 0)
          return -EINVAL;

  if ((sock = sockfd_lookup(fd, &err)) != NULL)
  {
      if (level == SOL_SOCKET)
          err=sock_setsockopt(sock,level,optname,optval,optlen);
      else
          err=sock->ops->setsockopt(sock, level, optname, optval, optlen);
  }
  return err;

}


int shutdown(int fd, int how){
  int err;
#if 1
  struct socket *sock;
  //for multicast sockets, shutdown returns ENOTCONN
  if ((sock = sockfd_lookup(fd, &err)) !=NULL)
  {
     err = sock->ops->shutdown(sock, how);
     sock_release(sock);
  }
#endif
  err=sys_close(fd);
  return err;

}

#if 0
int sendto(int fd, void * buff, size_t len,
           unsigned flags, struct sockaddr *addr,
           int addr_len){
    return syscall(__NR_sendto, fd, buff, len, flags, addr, addr_len);
}

int recvfrom(int fd, void * ubuf, size_t size,
             unsigned flags, struct sockaddr *addr,
             int *addr_len){
    return syscall(__NR_recvfrom, fd, ubuf, size, flags, addr, addr_len);
}
int getsockopt(int fd, int level, int optname, void *optval, int *optlen){
    return syscall(__NR_getsockopt, fd, level, optname, optval, optlen);
}

int getsockname(int fd, struct sockaddr *usockaddr, int *usockaddr_len){
    return syscall(__NR_getsockname, fd, usockaddr, usockaddr_len);
}
#endif

#else /* !__ARCH_WANT_SYS_SOCKETCALL */

/* No socketcall - define the individual syscalls. */

static inline _syscall3(int, socket,
                        int, family,
                        int, type,
                        int, protocol);

static inline _syscall3(int, bind,
                        int, fd,
                        struct sockaddr *, umyaddr,
                        int, addrlen);

static inline _syscall3(int, connect,
                        int, fd,
                        struct sockaddr *, uservaddr,
                        int, addrlen);

static inline _syscall6(int, sendto,
                        int, fd,
                        void *, buff,
                        size_t, len,
                        unsigned, flags,
                        struct sockaddr *, addr,
                        int, addr_len);

static inline _syscall6(int, recvfrom,
                        int, fd,
                        void *, ubuf,
                        size_t, size,
                        unsigned, flags,
                        struct sockaddr *, addr,
                        int *, addr_len);

static inline _syscall5(int, setsockopt,
                        int, fd,
                        int, level,
                        int, optname,
                        void *, optval,
                        int, optlen);

static inline _syscall5(int, getsockopt,
                        int, fd,
                        int, level,
                        int, optname,
                        void *, optval,
                        int *, optlen);

static inline _syscall2(int, shutdown,
                        int, fd,
                        int, how);

static inline _syscall3(int, getsockname,
                        int, fd,
                        struct sockaddr *, usockaddr,
                        int *, usockaddr_len);

#endif /* __ARCH_WANT_SYS_SOCKETCALL */

/*============================================================================*/
/** Socket flags. */
enum VsockFlag {
    VSOCK_REUSE     =  1,
    VSOCK_BIND      =  2,
    VSOCK_CONNECT   =  4,
    VSOCK_BROADCAST =  8,
    VSOCK_MULTICAST = 16,
    VSOCK_NONBLOCK  = 32,
 };

/** Convert socket flags to a string.
 *
 * @param flags flags
 * @return static string
 */
char * socket_flags(int flags){
    static char s[7];
    int i = 0;
    s[i++] = (flags & VSOCK_CONNECT   ? 'c' : '-');
    s[i++] = (flags & VSOCK_BIND      ? 'b' : '-');
    s[i++] = (flags & VSOCK_REUSE     ? 'r' : '-');
    s[i++] = (flags & VSOCK_BROADCAST ? 'B' : '-');
    s[i++] = (flags & VSOCK_MULTICAST ? 'M' : '-');
    s[i++] = (flags & VSOCK_NONBLOCK  ? 'N' : '-');
    s[i++] = '\0';
    return s;
}

/** Control flag for whether varp should be running.
 * If this is set 0 then the varp thread will notice and
 * (eventually) exit.
 */
atomic_t varp_run = ATOMIC_INIT(0);

enum {
    VARP_STATE_EXITED  = 2,
    VARP_STATE_RUNNING = 1,
    VARP_STATE_NONE    = 0,
    VARP_STATE_ERROR   = -1,
};

/** State indicating whether the varp thread is running. */
atomic_t varp_state = ATOMIC_INIT(VARP_STATE_NONE);

int varp_thread_err = 0;

/** The varp multicast socket. */
int varp_mcast_sock = -1;

/** The varp unicast socket. */
int varp_ucast_sock = -1;

/** Set socket option to reuse address.
 *
 * @param sock socket
 * @param reuse flag
 * @return 0 on success, error code otherwise
 */
int setsock_reuse(int sock, int reuse){
    int err = 0;
    err = setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));
    if(err < 0){
        eprintf("> setsockopt SO_REUSEADDR: %d %d\n", err, errno);
    }
    return err;
}

/** Set socket broadcast option.
 *
 * @param sock socket
 * @param bcast flag
 * @return 0 on success, error code otherwise
 */
int setsock_broadcast(int sock, int bcast){
    int err = 0;
    err = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &bcast, sizeof(bcast));
    if(err < 0){
        eprintf("> setsockopt SO_BROADCAST: %d %d\n", err, errno);
    }
    return err;
}

/** Join a socket to a multicast group.
 *
 * @param sock socket
 * @param saddr multicast address
 * @return 0 on success, error code otherwise
 */
int setsock_multicast(int sock, uint32_t saddr){
    int err = 0;
    struct ip_mreqn mreq = {};
    int mloop = 0;

    // See 'man 7 ip' for these options.
    mreq.imr_multiaddr.s_addr = saddr;       // IP multicast address.
    mreq.imr_address.s_addr   = INADDR_ANY;  // Interface IP address.
    mreq.imr_ifindex = 0;                    // Interface index (0 means any).
    err = setsockopt(sock, SOL_IP, IP_MULTICAST_LOOP, &mloop, sizeof(mloop));
    if(err < 0){
        eprintf("> setsockopt IP_MULTICAST_LOOP: %d %d\n", err, errno);
        goto exit;
    }
    err = setsockopt(sock, SOL_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(mreq));
    if(err < 0){
        eprintf("> setsockopt IP_ADD_MEMBERSHIP: %d %d\n", err, errno);
        goto exit;
    }
  exit:
    return err;
}

/** Set a socket's multicast ttl (default is 1).
 * @param sock socket
 * @param ttl ttl
 * @return 0 on success, error code otherwise
 */
int setsock_multicast_ttl(int sock, uint8_t ttl){
    int err = 0;
    err = setsockopt(sock, SOL_IP, IP_MULTICAST_TTL, &ttl, sizeof(ttl));
    return err;
}

/** Create a socket.
 * The flags can include values from enum VsockFlag.
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
    int sock;
    struct sockaddr_in addr_in;
    struct sockaddr *addr = (struct sockaddr *)&addr_in;
    int addr_n = sizeof(addr_in);
    int sockproto = 0;

    dprintf(">\n");
    addr_in.sin_family      = AF_INET;
    addr_in.sin_addr.s_addr = saddr;
    addr_in.sin_port        = port;
    dprintf("> flags=%s addr=%u.%u.%u.%u port=%d\n",
            socket_flags(flags),
            NIPQUAD(saddr), ntohs(port));

    switch(socktype){
    case SOCK_DGRAM:  sockproto = IPPROTO_UDP; break;
    case SOCK_STREAM: sockproto = IPPROTO_TCP; break;
    }
    sock = socket(AF_INET, socktype, sockproto);
    if(sock < 0) goto exit;
    if(flags & VSOCK_REUSE){
        err = setsock_reuse(sock, 1);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_BROADCAST){
        err = setsock_broadcast(sock, 1);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_MULTICAST){
        err = setsock_multicast(sock, saddr);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_CONNECT){
        err = connect(sock, addr, addr_n);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_BIND){
        err = bind(sock, addr, addr_n);
        if(err < 0) goto exit;
    }
#if (LINUX_VERSION_CODE == KERNEL_VERSION(2,6,18) )
    if(flags & VSOCK_NONBLOCK){
        err = fcntl(sock, F_SETFL, O_NONBLOCK);
        if(err < 0) goto exit;
    }
#endif
  exit:
    *val = (err ? -1 : sock);
    if(err) eprintf("> err=%d errno=%d\n", err, errno);
    return err;
}

/** Open the varp multicast socket.
 *
 * @param mcaddr multicast address 
 * @param port port
 * @param val return parameter for the socket
 * @return 0 on success, error code otherwise
 */
int varp_mcast_open(uint32_t mcaddr, uint16_t port, int *val){
    int err = 0;
    int flags = VSOCK_REUSE;
    int sock = 0;
    
    dprintf(">\n");
    flags |= VSOCK_MULTICAST;
    flags |= VSOCK_BROADCAST;
    
    err = create_socket(SOCK_DGRAM, mcaddr, port, flags, &sock);
    if(err < 0) goto exit;
    if(MULTICAST(mcaddr)){
        err = setsock_multicast_ttl(sock, 2);
        if(err < 0) goto exit;
    }
  exit:
    if(err){
        shutdown(sock, 2);
    }
    *val = (err ? -1 : sock);
    dprintf("< err=%d val=%d\n", err, *val);
    return err;
}

/** Open the varp unicast socket.
 *
 * @param addr address 
 * @param port port
 * @param val return parameter for the socket
 * @return 0 on success, error code otherwise
 */
int varp_ucast_open(uint32_t addr, u16 port, int *val){
    int err = 0;
    int flags = (VSOCK_BIND | VSOCK_REUSE);
    dprintf(">\n");
    err = create_socket(SOCK_DGRAM, addr, port, flags, val);
    dprintf("< err=%d val=%d\n", err, *val);
    return err;
}

/**
 * Return code > 0 means the handler owns the packet.
 * Return code <= 0 means we still own it, with < 0 meaning
 * an error.
 */
static int handle_varp_skb(struct sk_buff *skb){
    int err = 0;
    switch(skb->pkt_type){
    case PACKET_BROADCAST:
    case PACKET_MULTICAST:
        vnet_forward_send(skb);
        /* Fall through. */
    case PACKET_HOST:
        err = varp_handle_message(skb);
        break;
    case PACKET_OTHERHOST:
        dprintf("> PACKET_OTHERHOST\n");
        break;
    case PACKET_OUTGOING:
        dprintf("> PACKET_OUTGOING\n");
        break;
    case PACKET_FASTROUTE:
        dprintf("> PACKET_FASTROUTE\n");
        break;
    case PACKET_LOOPBACK:
        // Outbound mcast/bcast are echoed with this type. Drop.
        dprintf("> LOOP src=" IPFMT " dst=" IPFMT " dev=%s\n",
                NIPQUAD(skb->nh.iph->saddr),
                NIPQUAD(skb->nh.iph->daddr),
                (skb->dev ? skb->dev->name : "??"));
      default:
        // Drop.
        break;
    }
    if(err <= 0){
        kfree_skb(skb);
    }
    return (err < 0 ? err : 0);
}

/** Handle some skbs on a varp socket (if any).
 *
 * @param fd socket file descriptor
 * @param n maximum number of skbs to handle
 * @return number of skbs handled
 */
static int handle_varp_sock(int fd, int n){
    int ret = 0;
    int err = 0;
    struct sk_buff *skb;
    struct socket *sock = NULL;

    sock = sockfd_lookup(fd, &err);
    if (!sock){
        wprintf("> no sock for fd=%d\n", fd);
        goto exit;
    }
    for( ; ret < n; ret++){
        if(!sock->sk) break;
        skb = skb_dequeue(&sock->sk->sk_receive_queue);
        if(!skb) break;
        // Call the skb destructor so it isn't charged to the socket anymore.
        // An skb from a socket receive queue is charged to the socket
        // by skb_set_owner_r() until its destructor is called.
        // If the destructor is not called the socket will run out of
        // receive queue space and be unable to accept incoming skbs.
        // The destructor used is sock_rfree(), see 'include/net/sock.h'.
        // Other destructors: sock_wfree, sk_stream_rfree.
        skb_orphan(skb);
        handle_varp_skb(skb);
    }
    sockfd_put(sock);
  exit:
    dprintf("< ret=%d\n", ret);
    return ret;
}

/** Add a wait queue to a socket.
 *
 * @param fd socket file descriptor
 * @param waitq queue
 * @return 0 on success, error code otherwise
 */
int sock_add_wait_queue(int fd, wait_queue_t *waitq){
    int err = -EINVAL;
    struct socket *sock = NULL;

    if(fd < 0) goto exit;
    sock = sockfd_lookup(fd, &err);
    if (!sock) goto exit;
    add_wait_queue(sock->sk->sk_sleep, waitq);
    sockfd_put(sock);
    err = 0;
  exit:
    return err;
}

/** Remove a wait queue from a socket.
 *
 * @param fd socket file descriptor
 * @param waitq queue
 * @return 0 on success, error code otherwise
 */
int sock_remove_wait_queue(int fd, wait_queue_t *waitq){
    int err = -EINVAL;
    struct socket *sock = NULL;

    if(fd < 0) goto exit;
    sock = sockfd_lookup(fd, &err);
    if (!sock) goto exit;
    remove_wait_queue(sock->sk->sk_sleep, waitq);
    sockfd_put(sock);
    err = 0;
  exit:
    return err;
}

#if 0
// Default data ready function on a socket.
static void sock_def_readable(struct sock *sk, int len)
{
	read_lock(&sk->sk_callback_lock);
	if (sk->sk_sleep && waitqueue_active(sk->sk_sleep))
		wake_up_interruptible(sk->sk_sleep);
	sk_wake_async(sk,1,POLL_IN);
	read_unlock(&sk->sk_callback_lock);
}
#endif

static void sock_data_ready(struct sock *sk, int len){
    struct sk_buff *skb;
    //read_lock(&sk->sk_callback_lock);
    skb = skb_dequeue(&sk->sk_receive_queue);
    if(skb){
        skb_orphan(skb);
    }
    //read_unlock(&sk->sk_callback_lock);
    if(skb){
        handle_varp_skb(skb);
    }
}

/** Set the data ready callback on a socket.
 */
int sock_set_callback(int fd){
    int err = -EINVAL;
    struct socket *sock = NULL;

    if(fd < 0) goto exit;
    sock = sockfd_lookup(fd, &err);
    if (!sock) goto exit;
    sock->sk->sk_data_ready = sock_data_ready;
    sockfd_put(sock);
    err = 0;
  exit:
    return err;
}

/** Open the sockets. */
int varp_sockets_open(u32 mcaddr, u16 port){
    int err = 0;
    mm_segment_t oldfs;

    dprintf("> mcaddr=%u.%u.%u.%u port=%u\n", NIPQUAD(mcaddr), ntohs(port));
    oldfs = change_fs(KERNEL_DS);
    err = varp_mcast_open(mcaddr, port, &varp_mcast_sock);
    if(err < 0 ) goto exit;
    err = varp_ucast_open(INADDR_ANY, port, &varp_ucast_sock);
    if(err < 0 ) goto exit;
    sock_set_callback(varp_ucast_sock);
    sock_set_callback(varp_mcast_sock);
  exit:
    set_fs(oldfs);
    dprintf("< err=%d\n", err);
    return err;
}	

/** Close the sockets. */
void varp_sockets_close(void){
    mm_segment_t oldfs;
    int err;
    struct ip_mreqn mreq = {};

    oldfs = change_fs(KERNEL_DS);
    if(varp_mcast_sock >= 0){
        mreq.imr_multiaddr.s_addr = htonl(VARP_MCAST_ADDR);
        mreq.imr_address.s_addr   = INADDR_ANY;
        mreq.imr_ifindex = 0;                  
        err =setsockopt(varp_mcast_sock, SOL_IP,IP_DROP_MEMBERSHIP, &mreq, sizeof(mreq));
        if(err < 0){
            eprintf("> setsockopt IP_MULTICAST_DROP: %d %d\n", err, errno);
        }
        shutdown(varp_mcast_sock, 2);
        varp_mcast_sock = -1;
    }
    if(varp_ucast_sock >= 0){
        err=shutdown(varp_ucast_sock, 2);
        if(err){
            eprintf("> ucast sock shutdown err=%d\n",  err);
        }
        varp_ucast_sock = -1;
    }
    set_fs(oldfs);
}

/** Loop handling the varp sockets.
 * We use kernel API for this (waitqueue, schedule_timeout) instead
 * of select because the select syscall was returning EFAULT. Oh well.
 *
 * @param arg arguments
 * @return exit code
 */
int varp_main(void *arg){
    int err = 0;
    long timeout = 1 * HZ;
    int count = 0;
    DECLARE_WAITQUEUE(mcast_wait, current);
    DECLARE_WAITQUEUE(ucast_wait, current);

    dprintf("> start\n");
    //snprintf(current->comm, sizeof(TASK_COMM_LEN), "vnet_varp");

    err = varp_sockets_open(htonl(VARP_MCAST_ADDR), htons(VARP_PORT));
    if(err) goto exit;

    err = sock_add_wait_queue(varp_mcast_sock, &mcast_wait);
    if(err) goto exit_mcast_sock;
    err = sock_add_wait_queue(varp_ucast_sock, &ucast_wait);
    if(err) goto exit_ucast_sock;
    atomic_set(&varp_state, VARP_STATE_RUNNING);
    for( ; atomic_read(&varp_run); ){
        count = 0;
        count += handle_varp_sock(varp_mcast_sock, 1);
        count += handle_varp_sock(varp_ucast_sock, 16);
        if(!count){
            if(!atomic_read(&varp_run)) break;
            // No skbs were handled, go to sleep.
            set_current_state(TASK_INTERRUPTIBLE);
            schedule_timeout(timeout);
            __set_current_state(TASK_RUNNING);
        }
    }
  exit_ucast_sock:
    sock_remove_wait_queue(varp_ucast_sock, &ucast_wait);
  exit_mcast_sock:
    sock_remove_wait_queue(varp_mcast_sock, &mcast_wait);
    varp_sockets_close();
  exit:
    if(err){
        eprintf("%s< err=%d\n", __FUNCTION__, err);
    }
    varp_thread_err = err;
    atomic_set(&varp_state, VARP_STATE_EXITED);
    //MOD_DEC_USE_COUNT;
    return err;
}

/** Close the varp sockets and stop the thread handling them.
 */
void varp_close(void){
    int tries = 10;
    dprintf(">\n");
    // Tell the varp thread to stop and wait a while for it.
    atomic_set(&varp_run, 0);
    while(atomic_read(&varp_state) == VARP_STATE_RUNNING && tries-- > 0){
        set_current_state(TASK_INTERRUPTIBLE);
        schedule_timeout(HZ / 2);
        __set_current_state(TASK_RUNNING);
    }
    //MOD_DEC_USE_COUNT;
    dprintf("<\n");
}    

/** Open the varp sockets and start the thread handling them.
 *
 * @param mcaddr multicast address
 * @param port port
 * @return 0 on success, error code otherwise
 */
int varp_open(u32 mcaddr, u16 port){
    int err = 0;
    struct task_struct* task;
    
    //MOD_INC_USE_COUNT;
    dprintf(">\n");
    atomic_set(&varp_run, 1);
    atomic_set(&varp_state, VARP_STATE_NONE);
#if 1
    task = kthread_run(varp_main, (void*)NULL, "vnet_varpd");
    if (IS_ERR(task) < 0) {
	eprintf("> Unable to start varp main thread\n");
        goto exit;
    }
#endif
#if 0
    while(atomic_read(&varp_state) == VARP_STATE_NONE){
        set_current_state(TASK_INTERRUPTIBLE);
        schedule_timeout(1 * HZ);
        __set_current_state(TASK_RUNNING);
    }
    err = varp_thread_err;
#endif
  exit:
    if(err){
        wprintf("> err=%d\n", err);
    }
    return err;
}
