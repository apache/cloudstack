/*
 * Copyright (C) 2004, 2005 Mike Wray <mike.wray@hp.com>
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
#ifndef _VNET_SKB_UTIL_H_
#define _VNET_SKB_UTIL_H_

#ifdef __KERNEL__
#include <net/route.h>
#include <linux/skbuff.h>

#else

#include "skbuff.h"

#endif

struct sk_buff;

extern int skb_make_room(struct sk_buff **pskb, struct sk_buff *skb, int head_n, int tail_n);

extern int skb_put_bits(const struct sk_buff *skb, int offset, void *src, int len);

extern void skb_print_bits(const char *msg, struct sk_buff *skb, int offset, int n);

extern void buf_print(char *buf, int n);

extern void *skb_trim_tail(struct sk_buff *skb, int n);

extern void print_skb_data(const char *msg, int count, struct sk_buff *skb, u8 *data, int len);
extern void print_skb(const char *msg, int count, struct sk_buff *skb);

extern void print_ethhdr(const char *msg, struct sk_buff *skb);
extern void print_iphdr(const char *msg, struct sk_buff *skb);
extern void print_udphdr(const char *msg, struct sk_buff *skb);

/* The mac.ethernet field went away in 2.6 in favour of eth_hdr().
 */
#ifdef __KERNEL__
#  if LINUX_VERSION_CODE < KERNEL_VERSION(2,6,0)
#    define NEED_ETH_HDR
#  endif
#else
#  define NEED_ETH_HDR
#endif

#ifdef NEED_ETH_HDR

static inline struct ethhdr *eth_hdr(const struct sk_buff *skb)
{
	return (struct ethhdr *)skb->mac.raw;
}

#endif

/*
 * It's a copy from {kernel}/include/linux/skbuff.h func '__skb_pull' and 'skb_pull'
 * to aviodthe BUG_ON when pulling into the data (getting forwarded ip-frames)
 */
static inline unsigned char *__skb_pull_vn(struct sk_buff *skb, unsigned int len)
{
        skb->len -= len;
        //BUG_ON(skb->len < skb->data_len);
        return skb->data += len;
}
static inline unsigned char *skb_pull_vn(struct sk_buff *skb, unsigned int len)
{
        return unlikely(len > skb->len) ? NULL : __skb_pull_vn(skb, len);
}


#ifdef __KERNEL__

struct scatterlist;

extern int skb_scatterlist(struct sk_buff *skb, struct scatterlist *sg,
                           int *sg_n, int offset, int len);

#if LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,0)

static inline int skb_route(struct sk_buff *skb, struct rtable **prt){
    int err = 0;
    struct flowi fl = {
        .nl_u = {
            .ip4_u = {
                .daddr = skb->nh.iph->daddr,
                .saddr = skb->nh.iph->saddr,
                .tos   = skb->nh.iph->tos,
            }
        }
    };
    
    if(skb->dev){
        fl.oif = skb->dev->ifindex;
    }
    err = ip_route_output_key(prt, &fl);
    return err;
}

#else

static inline int skb_route(struct sk_buff *skb, struct rtable **prt){
    int err = 0;
    struct rt_key key = { };
    key.dst = skb->nh.iph->daddr;
    key.src = skb->nh.iph->saddr;
    key.tos = skb->nh.iph->tos;
    if(skb->dev){
        key.oif = skb->dev->ifindex;
    }
    err = ip_route_output_key(prt, &key);
    return err;
}

#endif

#endif /* __KERNEL__ */

/** Arp header struct with all the fields so we can access them. */
struct arpheader
{
	unsigned short	ar_hrd;		/* format of hardware address	*/
	unsigned short	ar_pro;		/* format of protocol address	*/
	unsigned char	ar_hln;		/* length of hardware address	*/
	unsigned char	ar_pln;		/* length of protocol address	*/
	unsigned short	ar_op;		/* ARP opcode (command)		*/

#if 1
	 /*
	  *	 Ethernet looks like this : This bit is variable sized however...
	  */
	unsigned char		ar_sha[ETH_ALEN];	/* sender hardware address	*/
	unsigned char		ar_sip[4];		/* sender IP address		*/
	unsigned char		ar_tha[ETH_ALEN];	/* target hardware address	*/
	unsigned char		ar_tip[4];		/* target IP address		*/
#endif

};

#endif /* ! _VNET_SKB_UTIL_H_ */
