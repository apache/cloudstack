/*
 * Copyright (C) 2005, 2006 Mike Wray <mike.wray@hp.com>.
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
#include <stdbool.h>
#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <getopt.h>
#include <errno.h>
#include <time.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>

#include <signal.h>
#include <sys/wait.h>
#include <sys/select.h>

#include <asm/types.h> // For __u32 etc.

#include <linux/ip.h>  // For struct iphdr.
#include <linux/udp.h> // For struct udphdr.

#include <linux/if.h>
#include <linux/if_ether.h>
#include <linux/if_tun.h> 

#include "sys_kernel.h"
#include "skbuff.h"
#include "spinlock.h"

#include "allocate.h"

#include "file_stream.h"
#include "string_stream.h"
#include "socket_stream.h"
#include "sys_net.h"

#include "enum.h"
#include "sxpr.h"
#include "sxpr_parser.h"

#include "connection.h"
#include "select.h"
#include "timer.h"

#include "if_etherip.h"
#include "if_varp.h"
#include "varp.h"
#include "vnet.h"
#include "vnet_dev.h"
#include "vnet_eval.h"
#include "vnet_forward.h"
#include "tunnel.h"
#include "etherip.h"
#include "sxpr_util.h"

#define MODULE_NAME "VNETD"
#define DEBUG 1
#undef DEBUG 
#include "debug.h"

#define PROGRAM "vnetd"
#define VERSION "1.0"

typedef struct Vnetd {
    unsigned long port;
    int ttl;
    int verbose;
    int etherip;

    int udp_sock;
    struct sockaddr_in udp_sock_addr;
    int mcast_sock;
    struct sockaddr_in mcast_sock_addr;
    int etherip_sock;
    struct sockaddr_in etherip_sock_addr;
    int unix_sock;
    char *unix_path;

    int raw_sock;

    struct sockaddr_in ucast_addr;
    struct sockaddr_in mcast_addr;

    HashTable *vnet_table;

    ConnList *conns;

} Vnetd;

Vnetd _vnetd = {}, *vnetd = &_vnetd;

uint32_t vnetd_intf_addr(Vnetd *vnetd){
    return vnetd->ucast_addr.sin_addr.s_addr;
}

uint32_t vnetd_mcast_addr(Vnetd *vnetd){
    return vnetd->mcast_addr.sin_addr.s_addr;
}

void vnetd_set_mcast_addr(Vnetd *vnetd, uint32_t addr){
    varp_mcast_addr = addr;
    vnetd->mcast_addr.sin_addr.s_addr = addr;
}

uint16_t vnetd_mcast_port(Vnetd *vnetd){
    return vnetd->mcast_addr.sin_port;
}

uint32_t vnetd_addr(void){
    return vnetd_intf_addr(vnetd);
}

/** Open tap device.
 */
int tap_open(struct net_device *dev){
    int err;
    /* IFF_TAP      : Ethernet tap device.
     * IFF_NO_PI    : Don't add packet info struct when reading.
     * IFF_ONE_QUEUE: Drop packets when the dev queue is full. The driver uses
     *                the queue size from the device, which defaults to 1000 for etherdev.
     *                If not set the driver stops the device queue when it goes over
     *                TUN_READQ_SIZE, which is 10. Broken - makes the device stall
     *                under load.
     */
    struct ifreq ifr = { };
    ifr.ifr_flags = (IFF_TAP | IFF_NO_PI | IFF_ONE_QUEUE);

    dprintf(">\n");
    dev->tapfd = open("/dev/net/tun", O_RDWR);
    if(dev->tapfd < 0){
        err = -errno;
        perror("open");
        goto exit;
    }
    strcpy(ifr.ifr_name, dev->name);
    err = ioctl(dev->tapfd, TUNSETIFF, (void *)&ifr);
    if(err < 0){
        err = -errno;
        perror("ioctl");
        goto exit;
    }
    strcpy(dev->name, ifr.ifr_name);
    dprintf("> dev=%s\n", dev->name);
    // Make it non-blocking.
    fcntl(dev->tapfd, F_SETFL, O_NONBLOCK);

  exit:
    if(err && (dev->tapfd >= 0)){
        close(dev->tapfd);
        dev->tapfd = -1;
    }
    dprintf("< err=%d\n", err);
    return err;
}

/** Close tap device.
 */
int tap_close(struct net_device *dev){
    int err = 0;

    if(dev->tapfd >= 0){
        err = close(dev->tapfd);
        dev->tapfd = -1;
    }
    return err;
}

/** Open vnif tap device for a vnet.
 */
int vnet_dev_add(struct Vnet *vnet){
    int err = 0;
    struct net_device *dev = ALLOCATE(struct net_device);
    strcpy(dev->name, vnet->device);
    err = tap_open(dev);
    if(err){
        wprintf("> Unable to open tap device.\n"
                "The tun module must be loaded and\n"
                "the vnet kernel module must not be loaded.\n");
        deallocate(dev);
        goto exit;
    }
    vnet->dev = dev;
  exit:
    return err;
}

/** Close vnif tap device for a vnet.
 */
void vnet_dev_remove(struct Vnet *vnet){
    if(vnet->dev){
        tap_close(vnet->dev);
        deallocate(vnet->dev);
        vnet->dev = NULL;
    }
}

/** Receive decapsulated ethernet packet on skb->dev.
 * Always succeeds. The skb must not be referred to after
 * this is called.
 */
int netif_rx(struct sk_buff *skb){
    int err = 0, n, k;
    struct net_device *dev = skb->dev;
    if(!dev){
        err = -ENODEV;
        goto exit;
    }
    n = skb->tail - skb->mac.raw;
    k = write(dev->tapfd, skb->mac.raw, n);
    if(k < 0){
        err = -errno;
        perror("write");
    } else if(k < n){
        //todo: What?
    }
  exit:
    kfree_skb(skb);
    return err;
}

static const int SKB_SIZE = 1700;

struct sk_buff *skb_new(void){
    return alloc_skb(SKB_SIZE, GFP_ATOMIC);
}

/** Receive a packet and fill-in source and destination addresses.
 * Just like recvfrom() but adds the destination address.
 * The socket must have the IP_PKTINFO option set so that the
 * destination address information is available.
 *
 * @param sock socket
 * @param buf receive buffer
 * @param len size of buffer
 * @param flags receive flags
 * @param from source address
 * @param fromlen size of source address
 * @param dest destination address
 * @param destlen size of destination address
 * @return number of bytes read on success, negative otherwise
 */
int recvfromdest(int sock, void *buf, size_t len, int flags,
                 struct sockaddr *from, socklen_t *fromlen,
                 struct sockaddr *dest, socklen_t *destlen){
    int ret = 0;
    struct iovec iov;
    struct msghdr msg;
    struct cmsghdr *cmsg;
    char cbuf[1024];
    struct in_pktinfo *info;
    struct sockaddr_in *dest_in = (struct sockaddr_in *)dest;

    //dest_in->sin_family = AF_INET;
    //dest_in->sin_port   = 0;
    getsockname(sock, dest, destlen);

    iov.iov_base       = buf;
    iov.iov_len        = len;
    msg.msg_name       = from;
    msg.msg_namelen    = *fromlen;
    msg.msg_iov        = &iov;
    msg.msg_iovlen     = 1;
    msg.msg_control    = cbuf;
    msg.msg_controllen = sizeof(cbuf);
    
    ret = recvmsg(sock, &msg, flags);
    if(ret < 0) goto exit;
    *fromlen = msg.msg_namelen;
    
    for(cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)){
        if((cmsg->cmsg_level == SOL_IP) && (cmsg->cmsg_type == IP_PKTINFO)){
            info = (void*)CMSG_DATA(cmsg);
            dest_in->sin_addr = info->ipi_addr;
            break;
        }
    }

  exit:
    return ret;
}

/** Read an skb from a udp socket and fill in its headers.
 */
int skb_recv_udp(int sock, int flags,
                 struct sockaddr_in *peer, socklen_t *peer_n,
                 struct sockaddr_in *dest, socklen_t *dest_n,
                 struct sk_buff **pskb){
    int err = 0, n;
    struct sk_buff *skb = skb_new();

    skb->mac.raw = skb->data;
    skb_reserve(skb, ETH_HLEN);
    skb->nh.raw = skb->data;
    skb_reserve(skb, sizeof(struct iphdr));
    // Rcvr wants skb->data pointing at the udphdr.
    skb->h.raw = skb_put(skb, sizeof(struct udphdr));
    n = recvfromdest(sock, skb->tail, skb_tailroom(skb), flags,
                     (struct sockaddr *)peer, peer_n,
                     (struct sockaddr *)dest, dest_n);
    if(n < 0){
        err = -errno;
        //perror("recvfrom");
        goto exit;
    }
    dprintf("> peer=%s:%d\n", inet_ntoa(peer->sin_addr), ntohs(peer->sin_port));
    dprintf("> dest=%s:%d\n", inet_ntoa(dest->sin_addr), ntohs(dest->sin_port));
    skb_put(skb, n);
    skb->protocol = skb->nh.iph->protocol = IPPROTO_UDP;
    skb->nh.iph->saddr = peer->sin_addr.s_addr;
    skb->h.uh->source  = peer->sin_port;
    skb->nh.iph->daddr = dest->sin_addr.s_addr;
    skb->h.uh->dest    = dest->sin_port;
  exit:
    if(err < 0){
        kfree_skb(skb);
        *pskb = NULL;
    } else {
        *pskb = skb;
    }
    return (err < 0 ? err : n);
}

/** Read an skb fom a raw socket and fill in its headers.
 */
int skb_recv_raw(int sock, int flags,
                 struct sockaddr_in *peer, socklen_t *peer_n,
                 struct sockaddr_in *dest, socklen_t *dest_n,
                 struct sk_buff **pskb){
    int err = 0, n;
    struct sk_buff *skb = skb_new();

    skb->mac.raw = skb->data;
    skb_reserve(skb, ETH_HLEN);
    skb->nh.raw = skb->data;
    skb_reserve(skb, sizeof(struct iphdr));
    // Rcvr wants skb->data pointing after ip hdr, at raw protocol hdr.
    n = recvfromdest(sock, skb->tail, skb_tailroom(skb), flags,
                     (struct sockaddr *)peer, peer_n,
                     (struct sockaddr *)dest, dest_n);
    if(n < 0){
        err = -errno;
        //perror("recvfrom");
        goto exit;
    }
    skb_put(skb, n);
    // On a raw socket the port in the address is the protocol.
    skb->protocol = skb->nh.iph->protocol = peer->sin_port;
    skb->nh.iph->saddr = peer->sin_addr.s_addr;
    skb->nh.iph->daddr = dest->sin_addr.s_addr;
  exit:
    if(err < 0){
        kfree_skb(skb);
        *pskb = NULL;
    } else {
        *pskb = skb;
    }
    return (err < 0 ? err : n);
}

/** Read an skb from a file descriptor.
 * Used for skbs coming to us from the tap device.
 * The skb content is an ethernet frame.
 */
int skb_read(int fd, struct sk_buff **pskb){
    int err = 0, n;
    struct sk_buff *skb = skb_new();

    // Reserve space for the headers we will add.
    skb_reserve(skb, 100);
    // Rcvr will want ethhdr on the skb.
    skb->mac.raw = skb->tail;
    n = read(fd, skb->tail, skb_tailroom(skb));
    if(n < 0){
        err = -errno;
        //perror("read");
        goto exit;
    }
    skb_put(skb, n);
  exit:
    if(err < 0){
        kfree_skb(skb);
        *pskb = NULL;
    } else {
        *pskb = skb;
    }
    return (err < 0 ? err : n);
}

/** Read an skb from the tap device for a vnet and send it.
 */
int vnet_read(Vnet *vnet){
    int err;
    struct sk_buff *skb = NULL;

    err = skb_read(vnet->dev->tapfd, &skb);
    if(err < 0) goto exit;
    err = vnet_skb_send(skb, &vnet->vnet);
  exit:
    if(skb) kfree_skb(skb);
    return (err < 0 ? err : 0);
}

/** Transmit an skb to the network.
 */
int _skb_xmit(struct sk_buff *skb, uint32_t saddr){
    int err = 0;
    int sock;
    unsigned char *data;
    struct sockaddr_in addr = { .sin_family = AF_INET };
    int flags = 0;

    if(saddr){
        dprintf("> Raw IP send\n");
        sock = vnetd->raw_sock;
        skb->nh.iph->saddr = saddr;
        addr.sin_addr.s_addr = skb->nh.iph->daddr;
        // Should be the protocol, but is ignored. See raw(7) man page.
        addr.sin_port        = 0;
        // Data includes the ip header.
        data = (void*)(skb->nh.iph);
    } else {        
        switch(skb->nh.iph->protocol){
        case IPPROTO_UDP:
            dprintf("> protocol=UDP\n");
            sock = vnetd->udp_sock;
            // Data comes after the udp header.
            data = (void*)(skb->h.uh + 1);
            addr.sin_addr.s_addr = skb->nh.iph->daddr;
            addr.sin_port        = skb->h.uh->dest;
            break;
        case IPPROTO_ETHERIP:
            dprintf("> protocol=ETHERIP\n");
            if(vnetd->etherip_sock < 0){
                err = -ENOSYS;
                goto exit;
            }
            sock = vnetd->etherip_sock;
            // Data comes after the ip header.
            data = (void*)(skb->nh.iph + 1);
            addr.sin_addr.s_addr = skb->nh.iph->daddr;
            // Should be the protocol, but is ignored. See raw(7) man page.
            addr.sin_port        = 0;
            break;
        default:
            err = -ENOSYS;
            wprintf("> protocol=%d, %d\n", skb->nh.iph->protocol, skb->protocol);
            goto exit;
        }
    }

    dprintf("> sending %d bytes to %s:%d protocol=%d\n",
            skb->tail - data,
            inet_ntoa(addr.sin_addr),
            ntohs(addr.sin_port),
            skb->nh.iph->protocol);

    err = sendto(sock, data, skb->tail - data, flags,
                 (struct sockaddr *)&addr, sizeof(addr));
    if(err < 0){
        err = -errno;
        perror("sendto");
    }
  exit:    
    if(err >= 0){
        // Caller will assume skb freed if no error.
        kfree_skb(skb);
        err = 0;
    }
    dprintf("< err=%d\n", err);
    return err;
}

int varp_open(uint32_t mcaddr, uint16_t port){
    return 0;
}

void varp_close(void){
}

/** Create a raw socket.
 *
 * @param protocol protocol
 * @param flags flags (VSOCK_*)
 * @param mcaddr multicast addr used with flag VSOCK_MULTICAST
 * @param sock return value for the socket
 */
int vnetd_raw_socket(Vnetd *vnetd, int protocol, int flags,
                     uint32_t mcaddr, int *sock){
    int err;
    int bcast = (flags & VSOCK_BROADCAST);

    err = *sock = socket(AF_INET, SOCK_RAW, protocol);
    if(err < 0){
        err = -errno;
        perror("socket");
        goto exit;
    }
    if(bcast){
        err = setsock_broadcast(*sock, bcast);
        if(err < 0) goto exit;
    }
    if(flags & VSOCK_MULTICAST){
        err = setsock_multicast(*sock, INADDR_ANY, mcaddr);
        if(err < 0) goto exit;
    }
    //todo ?? fcntl(*sock, F_SETFL, O_NONBLOCK);
  exit:
    return err;
}

int get_dev_address(char *dev, unsigned long *addr){
    int err = 0;
    int sock = -1;
    struct ifreq ifreq = {};
    struct sockaddr_in *in_addr;

    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if(sock < 0){
        err = -errno;
        goto exit;
    }
    strncpy(ifreq.ifr_name, dev, IFNAMSIZ);
    err = ioctl(sock, SIOCGIFADDR, &ifreq);
    if(err){
        err = -errno;
        goto exit;
    }
    in_addr = (struct sockaddr_in *) &ifreq.ifr_addr;
    *addr = in_addr->sin_addr.s_addr;
    //iprintf("> dev=%s addr=%s\n", dev, inet_ntoa(in_addr->sin_addr));
  exit:
    if(sock >= 0) close(sock);
    return err;
}

int get_intf_address(unsigned long *addr){
    int err = 0;
    char *devs[] = {"cloudbr0", "eth0", "eth1", "eth2", "wlan0", "wlan1", NULL};
    char **dev;

    for(dev = devs; *dev; dev++){
        err = get_dev_address(*dev, addr);
        if(err == 0) goto exit;
    }
    err = -ENOSYS;
  exit:
    return err;
}

/** Get our own address. So we can ignore broadcast traffic
 * we sent ourselves.
 *
 * @param addr
 * @return 0 on success, error code otherwise
 */
int get_self_addr(struct sockaddr_in *addr){
    int err = 0;
    unsigned long saddr;
 
    err = get_intf_address(&saddr);
    if(err) goto exit;
    addr->sin_addr.s_addr = saddr;
    err = 0;
  exit:
    return err;
}

static int eval_vnetd_mcaddr(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Vnetd *vnetd = data;
    Sxpr oaddr = intern("addr");
    Sxpr ottl = intern("ttl");
    uint32_t addr;
    int ttl;

    err = child_addr(exp, oaddr, &addr);
    if(err < 0) goto exit;
    vnetd_set_mcast_addr(vnetd, addr);
    if(child_int(exp, ottl, &ttl) == 0){
        vnetd->ttl = ttl;
    }
  exit:
    return err;
}

static int vnetd_eval_io(Vnetd *vnetd, Parser *parser, SxprEval *defs,
                         IOStream *in, IOStream *out){
    int err = 0;
    char buf[1024];
    int k, n = sizeof(buf) - 1;

    for( ; ; ){
        k = IOStream_read(in, buf, n);
        if(k < 0){
            err = k;
            goto exit;
        }
        err = Parser_input(parser, buf, k);
        if(err < 0) goto exit;
        while(Parser_ready(parser)){
            Sxpr exp = Parser_get_val(parser);
            if(NONEP(exp)) break;
            err = vnet_eval_defs(defs, exp, out, vnetd);
            if(err) goto exit;
        }
        if(Parser_at_eof(parser)) break;
    }
  exit:
    return err;
}

static int vnetd_configure(Vnetd *vnetd, char *file){
    int err = 0;
    Parser *parser = NULL;    
    IOStream *io = NULL;
    SxprEval defs[] = {
        { .name = intern("peer.add"),     .fn = eval_peer_add     },
        { .name = intern("varp.mcaddr"),  .fn = eval_vnetd_mcaddr },
        { .name = intern("vnet.add"),     .fn = eval_vnet_add     },
        { .name = ONONE, .fn = NULL } };

    parser = Parser_new(); 
    io = file_stream_fopen(file, "rb");
    if(!io){
        err = -errno;
        goto exit;
    }
    vnetd_eval_io(vnetd, parser, defs, io, iostdout);
  exit:
    if(io) IOStream_close(io);
    Parser_free(parser);
    return err;
}

#define OPT_MCADDR   'a'
#define KEY_MCADDR   "varp_mcaddr"
#define DOC_MCADDR   "<addr>\n\t VARP multicast address"

#define OPT_FILE     'f'
#define KEY_FILE     "file"
#define DOC_FILE     "<file>\n\t Configuration file to load"

#define OPT_HELP     'h'
#define KEY_HELP     "help"
#define DOC_HELP     "\n\tprint help"

#define OPT_VERSION  'v'
#define KEY_VERSION  "version"
#define DOC_VERSION  "\n\tprint version"

#define OPT_VERBOSE  'V'
#define KEY_VERBOSE  "verbose"
#define DOC_VERBOSE  "\n\tverbose flag"

/** Print a usage message.
 * Prints to stdout if err is zero, and exits with 0.
 * Prints to stderr if err is non-zero, and exits with 1.
 *
 * @param err error code
 */
static void usage(int err){
    FILE *out = (err ? stderr : stdout);

    fprintf(out, "Usage: %s [options]\n", PROGRAM);
    fprintf(out, "-%c, --%s %s\n", OPT_MCADDR,   KEY_MCADDR,   DOC_MCADDR);
    fprintf(out, "-%c, --%s %s\n", OPT_FILE,     KEY_FILE,     DOC_FILE);
    fprintf(out, "-%c, --%s %s\n", OPT_VERBOSE,  KEY_VERBOSE,  DOC_VERBOSE);
    fprintf(out, "-%c, --%s %s\n", OPT_VERSION,  KEY_VERSION,  DOC_VERSION);
    fprintf(out, "-%c, --%s %s\n", OPT_HELP,     KEY_HELP,     DOC_HELP);
    exit(err ? 1 : 0);
}

/** Short options. Options followed by ':' take an argument. */
static char *short_opts = (char[]){
    OPT_MCADDR,   ':',
    OPT_FILE,     ':',
    OPT_HELP,
    OPT_VERSION,
    OPT_VERBOSE,
    0 };

/** Long options. */
static struct option const long_opts[] = {
    { KEY_MCADDR,   required_argument, NULL, OPT_MCADDR   },
    { KEY_FILE,     required_argument, NULL, OPT_FILE     },
    { KEY_HELP,     no_argument,       NULL, OPT_HELP     },
    { KEY_VERSION,  no_argument,       NULL, OPT_VERSION  },
    { KEY_VERBOSE,  no_argument,       NULL, OPT_VERBOSE  },
    { NULL,         0,                 NULL, 0            }
};

static int vnetd_getopts(Vnetd *vnetd, int argc, char *argv[]){
    int err = 0;
    int key = 0;
    int long_index = 0;

    while(1){
	key = getopt_long(argc, argv, short_opts, long_opts, &long_index);
	if(key == -1) break;
	switch(key){
        case OPT_MCADDR: {
            unsigned long addr;
            err = get_inet_addr(optarg, &addr);
            if(err) goto exit;
            vnetd_set_mcast_addr(vnetd, addr);
            break; }
        case OPT_FILE:
            err = vnetd_configure(vnetd, optarg);
            if(err) goto exit;
            break;
	case OPT_HELP:
	    usage(0);
	    break;
	case OPT_VERBOSE:
	    vnetd->verbose = true;
	    break;
	case OPT_VERSION:
            iprintf("> %s %s\n", PROGRAM, VERSION);
            exit(0);
	    break;
	default:
	    usage(EINVAL);
	    break;
	}
    }
  exit:
    return err;
}

/** Initialise vnetd params.
 *
 * @param vnetd vnetd
 */
static int vnetd_init(Vnetd *vnetd, int argc, char *argv[]){
    int err = 0;

    // Use etherip-in-udp encapsulation.
    etherip_in_udp = true;

    *vnetd = (Vnetd){};
    vnetd->port = htons(VARP_PORT);
    vnetd->verbose = false;
    vnetd->ttl = 1; // Default multicast ttl.
    vnetd->etherip = true;
    vnetd->udp_sock = -1;
    vnetd->mcast_sock = -1;
    vnetd->etherip_sock = -1;
    vnetd_set_mcast_addr(vnetd, htonl(VARP_MCAST_ADDR));
    vnetd->mcast_addr.sin_port = vnetd->port;
    vnetd->unix_path = "/tmp/vnetd";

    vnetd_getopts(vnetd, argc, argv);
    
    err = get_self_addr(&vnetd->ucast_addr);
    vnetd->ucast_addr.sin_port = vnetd->port;
    dprintf("> mcaddr=%s\n", inet_ntoa(vnetd->mcast_addr.sin_addr));
    dprintf("> addr  =%s\n", inet_ntoa(vnetd->ucast_addr.sin_addr));
    return err;
}

static void vnet_select(Vnetd *vnetd, SelectSet *set){
    HashTable_for_decl(entry);

    HashTable_for_each(entry, vnetd->vnet_table){
        Vnet *vnet = entry->value;
        struct net_device *dev = vnet->dev;
        if(!dev) continue;
        if(dev->tapfd < 0) continue;
        SelectSet_add(set, dev->tapfd, SELECT_READ);
    }
}

static void vnet_handle(Vnetd *vnetd, SelectSet *set){
    HashTable_for_decl(entry);

    HashTable_for_each(entry, vnetd->vnet_table){
        Vnet *vnet = entry->value;
        struct net_device *dev = vnet->dev;
        if(!dev) continue;
        if(dev->tapfd < 0) continue;
        if(SelectSet_in_read(set, dev->tapfd)){
            int n;
            for(n = 64; n > 0; --n){
                if(vnet_read(vnet) < 0) break;
            }
        }
    }
}

static int vnetd_handle_udp(Vnetd *vnetd, struct sockaddr_in *addr, int sock){
    int err = 0, n = 0;
    struct sockaddr_in peer, dest;
    socklen_t peer_n = sizeof(peer), dest_n = sizeof(dest);
    int flags = MSG_DONTWAIT;
    struct sk_buff *skb = NULL;

    dest = *addr;
    n = skb_recv_udp(sock, flags, &peer, &peer_n, &dest, &dest_n, &skb);
    if(n < 0){
        err = n;
        goto exit;
    }
    dprintf("> Received %d bytes from=%s:%d dest=%s:%d\n",
            n,
            inet_ntoa(peer.sin_addr), htons(peer.sin_port),
            inet_ntoa(dest.sin_addr), htons(dest.sin_port));
    if(peer.sin_addr.s_addr == vnetd_intf_addr(vnetd)){
        dprintf("> Ignoring message from self.\n");
        goto exit;
    }
    if(dest.sin_addr.s_addr == vnetd_mcast_addr(vnetd)){
        vnet_forward_send(skb);
    }
    err = varp_handle_message(skb);

  exit:
    if(skb) kfree_skb(skb);
    return err;
}

static int vnetd_handle_etherip(Vnetd *vnetd, struct sockaddr_in *addr, int sock){
    int err = 0, n = 0;
    struct sockaddr_in peer, dest;
    socklen_t peer_n = sizeof(peer), dest_n = sizeof(dest);
    int flags = 0;
    struct sk_buff *skb = NULL;

    dest = *addr;
    n = skb_recv_raw(sock, flags, &peer, &peer_n, &dest, &dest_n, &skb);
    if(n < 0){
        err = n;
        goto exit;
    }
    dprintf("> Received %d bytes from=%s:%d dest=%s:%d\n",
            n,
            inet_ntoa(peer.sin_addr), htons(peer.sin_port),
            inet_ntoa(dest.sin_addr), htons(dest.sin_port));
    if(peer.sin_addr.s_addr == vnetd_intf_addr(vnetd)){
        dprintf("> Ignoring message from self.\n");
        goto exit;
    }
    err = etherip_protocol_recv(skb);
  exit:
    if(skb) kfree_skb(skb);
    return err;
}

typedef struct ConnClient {
    Vnetd *vnetd;
    Parser *parser;
} ConnClient;

static int conn_handle_fn(Conn *conn, int mode){
    int err;
    ConnClient *client = conn->data;
    char data[1024] = {};
    int k;
    int done = false;

    k = IOStream_read(conn->in, data, sizeof(data));
    if(k < 0){
        err = k;
        goto exit;
    }
    if(!client->parser){
        err = -ENOSYS;
        goto exit;
    }
    if((k == 0) && Parser_at_eof(client->parser)){
        err = -EINVAL;
        goto exit;
    }
    err = Parser_input(client->parser, data, k);
    if(err < 0) goto exit;
    while(Parser_ready(client->parser)){
        Sxpr sxpr = Parser_get_val(client->parser);
        err = vnet_eval(sxpr, conn->out, NULL);
        if(err) goto exit;
        done = true;
    }
    if(done || Parser_at_eof(client->parser)){
        // Close at EOF.
        err = -EIO;
    }
  exit:
    if(err < 0){
        Parser_free(client->parser);
        client->parser = NULL;
    }
    return (err < 0 ? err : 0);
}

static int vnetd_handle_unix(Vnetd *vnetd, int sock){
    int err;
    ConnClient *client = NULL;
    Conn *conn = NULL;
    struct sockaddr_un peer = {};
    socklen_t peer_n = sizeof(peer);
    int peersock;

    peersock = accept(sock, (struct sockaddr *)&peer, &peer_n);
    if(peersock < 0){
        perror("accept");
        err = -errno;
        goto exit;
    }
    // We want non-blocking i/o.
    fcntl(peersock, F_SETFL, O_NONBLOCK);
    client = ALLOCATE(ConnClient);
    client->vnetd = vnetd;
    client->parser = Parser_new();
    conn = Conn_new(conn_handle_fn, client);
    err = Conn_init(conn, peersock, SOCK_STREAM, SELECT_READ,
                    (struct sockaddr_in){});
    if(err) goto exit;
    vnetd->conns = ConnList_add(vnetd->conns, conn);
  exit:
    if(err){
        Conn_close(conn);
        close(peersock);
    }
    if(err < 0) wprintf("< err=%d\n", err);
    return err;
}

static void vnetd_select(Vnetd *vnetd, SelectSet *set){
    SelectSet_add(set, vnetd->unix_sock, SELECT_READ);
    SelectSet_add(set, vnetd->udp_sock, SELECT_READ);
    SelectSet_add(set, vnetd->mcast_sock, SELECT_READ);
    if(vnetd->etherip_sock >= 0){
        SelectSet_add(set, vnetd->etherip_sock, SELECT_READ);
    }
    vnet_select(vnetd, set);
    ConnList_select(vnetd->conns, set);
}

static void vnetd_handle(Vnetd *vnetd, SelectSet *set){
    if(SelectSet_in_read(set, vnetd->unix_sock)){
        vnetd_handle_unix(vnetd, vnetd->unix_sock);
    }
    if(SelectSet_in_read(set, vnetd->udp_sock)){
        int n;

        for(n = 256; n > 0; --n){
            if(vnetd_handle_udp(vnetd, &vnetd->udp_sock_addr, vnetd->udp_sock) < 0){
                break;
            }
        }
    }
    if(SelectSet_in_read(set, vnetd->mcast_sock)){
        vnetd_handle_udp(vnetd, &vnetd->mcast_sock_addr, vnetd->mcast_sock);
    }
    if((vnetd->etherip_sock >= 0) &&
       SelectSet_in_read(set, vnetd->etherip_sock)){
        vnetd_handle_etherip(vnetd, &vnetd->etherip_sock_addr, vnetd->etherip_sock);
    }
    vnet_handle(vnetd, set);
    vnetd->conns = ConnList_handle(vnetd->conns, set);
}

/** Counter for timer alarms.
 */
static unsigned timer_alarms = 0;

static int vnetd_main(Vnetd *vnetd){
    int err = 0;
    SelectSet _set = {}, *set = &_set;
    struct timeval _timeout = {}, *timeout = &_timeout;

    vnetd->vnet_table = vnet_table;

    for( ; ; ){
        timeout->tv_sec = 0;
        timeout->tv_usec = 500000;
        SelectSet_zero(set);
        vnetd_select(vnetd, set);
        err = SelectSet_select(set, timeout);
        if(err == 0) continue;
        if(err < 0){
            switch(errno){
            case EINTR:
                if(timer_alarms){
                    timer_alarms = 0;
                    process_timers();
                }
                continue;
            case EBADF:
                continue;
            default:
                perror("select");
                goto exit;
            }
        }
        vnetd_handle(vnetd, set);
    }
  exit:
    return err;
}

static int getsockaddr(int sock, struct sockaddr_in *addr){
    socklen_t addr_n = sizeof(struct sockaddr_in);
    return getsockname(sock, (struct sockaddr*)addr, &addr_n);
}

static int vnetd_etherip_sock(Vnetd *vnetd){
    int err = 0;

    if(!vnetd->etherip) goto exit;
    err = vnetd_raw_socket(vnetd, IPPROTO_ETHERIP,
                           (VSOCK_BROADCAST | VSOCK_MULTICAST),
                           vnetd_mcast_addr(vnetd),
                           &vnetd->etherip_sock);
    if(err < 0) goto exit;
    err = setsock_pktinfo(vnetd->etherip_sock, true);
    if(err < 0) goto exit;
    getsockaddr(vnetd->etherip_sock, &vnetd->etherip_sock_addr);
  exit:
    return err;
}

static int vnetd_udp_sock(Vnetd *vnetd){
    int err;
    uint32_t mcaddr = vnetd_mcast_addr(vnetd);

    err = create_socket(SOCK_DGRAM, INADDR_ANY, vnetd->port,
                        (VSOCK_BIND | VSOCK_REUSE),
                        &vnetd->udp_sock);
    if(err < 0) goto exit;
    err = setsock_pktinfo(vnetd->udp_sock, true);
    if(err < 0) goto exit;
    getsockaddr(vnetd->udp_sock, &vnetd->udp_sock_addr);
    vnetd->mcast_sock_addr.sin_addr.s_addr = vnetd_intf_addr(vnetd);

    err = create_socket(SOCK_DGRAM, mcaddr, vnetd_mcast_port(vnetd),
                        (VSOCK_REUSE | VSOCK_BROADCAST | VSOCK_MULTICAST),
                        &vnetd->mcast_sock);
    if(err < 0) goto exit;
    err = setsock_pktinfo(vnetd->udp_sock, true);
    if(err < 0) goto exit;
    err = setsock_multicast(vnetd->mcast_sock, INADDR_ANY, mcaddr);
    if(err < 0) goto exit;
    err = setsock_multicast_ttl(vnetd->mcast_sock, vnetd->ttl);
    if(err < 0) goto exit;
    getsockaddr(vnetd->mcast_sock, &vnetd->mcast_sock_addr);
    vnetd->mcast_sock_addr.sin_addr.s_addr = mcaddr;

  exit:
    if(err < 0){
        close(vnetd->udp_sock);
        close(vnetd->mcast_sock);
        vnetd->udp_sock = -1;
        vnetd->mcast_sock = -1;
    }
    return err;
}

static int vnetd_raw_sock(Vnetd *vnetd){
    int err;

    err = vnetd_raw_socket(vnetd, IPPROTO_RAW,
                           (VSOCK_BROADCAST),
                           vnetd_mcast_addr(vnetd),
                           &vnetd->raw_sock);
    if(err){
        close(vnetd->raw_sock);
        vnetd->raw_sock = -1;
    }
    return err;
}

static int vnetd_unix_sock(Vnetd *vnetd){
    int err = 0;
    struct sockaddr_un addr = { .sun_family = AF_UNIX };
    socklen_t addr_n;
    
    vnetd->unix_sock = socket(addr.sun_family, SOCK_STREAM, 0);
    if(vnetd->unix_sock < 0){
        err = -errno;
        perror("unix socket");
        goto exit;
    }
    unlink(vnetd->unix_path);
    strcpy(addr.sun_path, vnetd->unix_path);
    addr_n = sizeof(addr) - sizeof(addr.sun_path) + strlen(vnetd->unix_path) + 1;
    err = bind(vnetd->unix_sock, (struct sockaddr *)&addr, addr_n);
    if(err < 0){
        err = -errno;
        perror("unix bind");
        goto exit;
    }
    err = listen(vnetd->unix_sock, 5);
    if(err < 0){
        err = -errno;
        perror("unix listen");
    }
  exit:
    return err;
}
   
/** Handle SIGPIPE.
 *
 * @param code signal code
 * @param info signal info
 * @param data
 */
static void sigaction_SIGPIPE(int code, siginfo_t *info, void *data){
    dprintf("> SIGPIPE\n");
}

/** Handle SIGALRM.
 *
 * @param code signal code
 * @param info signal info
 * @param data
 */
static void sigaction_SIGALRM(int code, siginfo_t *info, void *data){
    timer_alarms++;
}

/** Type for signal handling functions. */
typedef void SignalAction(int code, siginfo_t *info, void *data);

/** Install a handler for a signal.
 *
 * @param signum signal
 * @param action handler
 * @return 0 on success, error code otherwise
 */
static int catch_signal(int signum, SignalAction *action){
    int err = 0;
    struct sigaction sig = {};
    dprintf(">\n");
    sig.sa_sigaction = action;
    sig.sa_flags = SA_SIGINFO;
    err = sigaction(signum, &sig, NULL);
    if(err){
        err = -errno;
        perror("sigaction");
    }
    return err;
}    

int main(int argc, char *argv[]){
    int err = 0;

    err = tunnel_module_init();
    if(err < 0) goto exit;
    err = vnet_init();
    if(err < 0) goto exit;
    err = vnetd_init(vnetd, argc, argv);
    if(err < 0) goto exit;
    err = catch_signal(SIGPIPE, sigaction_SIGPIPE);
    if(err < 0) goto exit;
    err = catch_signal(SIGALRM, sigaction_SIGALRM); 
    if(err < 0) goto exit;
    err = vnetd_etherip_sock(vnetd);
    if(err < 0) goto exit;
    err = vnetd_udp_sock(vnetd);
    if(err < 0) goto exit;
    err = vnetd_raw_sock(vnetd);
    if(err < 0) goto exit;
    err = vnetd_unix_sock(vnetd);
    if(err < 0) goto exit;
    err = vnetd_main(vnetd);
exit:
    return (err ? 1 : 0);
}
