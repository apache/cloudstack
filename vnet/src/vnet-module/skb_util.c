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
#ifdef __KERNEL__
#include <linux/config.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/version.h>

#include <asm/scatterlist.h>
#include <linux/crypto.h>
#include <linux/pfkeyv2.h>
#include <linux/random.h>

#include <linux/net.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netdevice.h>
#include <linux/tcp.h>
#include <linux/udp.h>

#include <net/ip.h>
#include <net/protocol.h>
#include <net/route.h>
#include <linux/skbuff.h>

#if (LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,21) )
#include <linux/highmem.h>
static inline void *kmap_skb_frag(const skb_frag_t *frag)
{
#ifdef CONFIG_HIGHMEM
        BUG_ON(in_irq());

        local_bh_disable();
#endif
        return kmap_atomic(frag->page, KM_SKB_DATA_SOFTIRQ);
}

static inline void kunmap_skb_frag(void *vaddr)
{
        kunmap_atomic(vaddr, KM_SKB_DATA_SOFTIRQ);
#ifdef CONFIG_HIGHMEM
        local_bh_enable();
#endif
}

#endif


#else

#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>

#include <netinet/in.h>
#include <arpa/inet.h>

#include <sys/types.h>
#include <sys/socket.h>

#include <linux/if_ether.h>
#include <linux/if_arp.h>
#include <linux/ip.h>
#include <linux/tcp.h>
#include <linux/udp.h>

#include "sys_kernel.h"
#include "skbuff.h"

#if defined(__LITTLE_ENDIAN)
#define HIPQUAD(addr) \
	((unsigned char *)&addr)[3], \
	((unsigned char *)&addr)[2], \
	((unsigned char *)&addr)[1], \
	((unsigned char *)&addr)[0]
#elif defined(__BIG_ENDIAN)
#define HIPQUAD	NIPQUAD
#else
#error "Please fix asm/byteorder.h"
#endif /* __LITTLE_ENDIAN */

#endif

#include <varp.h>
#include <skb_util.h>

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

//============================================================================
/** Make enough room in an skb for extra header and trailer.
 *
 * @param pskb return parameter for expanded skb
 * @param skb skb
 * @param head_n required headroom
 * @param tail_n required tailroom
 * @return 0 on success, error code otherwise
 */
int skb_make_room(struct sk_buff **pskb, struct sk_buff *skb, int head_n, int tail_n){
    int err = 0;
    int has_headroom = (head_n <= skb_headroom(skb));
    int has_tailroom = (tail_n <= skb_tailroom(skb));
    int writeable = !skb_cloned(skb) && !skb_shared(skb);

    dprintf("> skb=%p headroom=%d head_n=%d tailroom=%d tail_n=%d\n",
            skb,
            skb_headroom(skb), head_n,
            skb_tailroom(skb), tail_n);
    if(writeable && has_headroom && has_tailroom){
        // There's room! Reuse it.
        *pskb = skb;
    } else if(writeable && has_tailroom){
        // Tailroom, no headroom. Expand header the way GRE does.
        struct sk_buff *new_skb = skb_realloc_headroom(skb, head_n + 16);
        if(!new_skb){
            err = -ENOMEM;
            goto exit;
        }
        kfree_skb(skb);
        *pskb = new_skb;
    } else {
        // No room. Expand. There may be more efficient ways to do
        // this, but this is simple and correct.
        struct sk_buff *new_skb = skb_copy_expand(skb, head_n + 16, tail_n, GFP_ATOMIC);
        if(!new_skb){
            err = -ENOMEM;
            goto exit;
        }
        kfree_skb(skb);
        *pskb = new_skb;
    }
    dprintf("> skb=%p headroom=%d head_n=%d tailroom=%d tail_n=%d\n",
            *pskb,
            skb_headroom(*pskb), head_n,
            skb_tailroom(*pskb), tail_n);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Copy some data bits from a kernel buffer to an skb.
 * Derived in the obvious way from skb_copy_bits().
 */
int skb_put_bits(const struct sk_buff *skb, int offset, void *src, int len)
{
    int i, copy;
    int start = skb->len - skb->data_len;

    if (offset > (int)skb->len-len)
        goto fault;

    /* Copy header. */
    if ((copy = start-offset) > 0) {
        if (copy > len)
            copy = len;
        memcpy(skb->data + offset, src, copy);
        if ((len -= copy) == 0)
            return 0;
        offset += copy;
        src += copy;
    }

#ifdef __KERNEL__
    for (i = 0; i < skb_shinfo(skb)->nr_frags; i++) {
        int end;

        BUG_TRAP(start <= offset+len);

        end = start + skb_shinfo(skb)->frags[i].size;
        if ((copy = end-offset) > 0) {
            u8 *vaddr;

            if (copy > len)
                copy = len;

            vaddr = kmap_skb_frag(&skb_shinfo(skb)->frags[i]);
            memcpy(vaddr + skb_shinfo(skb)->frags[i].page_offset + offset - start,
                   src,
                   copy);
            kunmap_skb_frag(vaddr);

            if ((len -= copy) == 0)
                return 0;
            offset += copy;
            src += copy;
        }
        start = end;
    }

    if (skb_shinfo(skb)->frag_list) {
        struct sk_buff *list;
        
        for (list = skb_shinfo(skb)->frag_list; list; list=list->next) {
            int end;
            
            BUG_TRAP(start <= offset+len);
            
            end = start + list->len;
            if ((copy = end-offset) > 0) {
                if (copy > len)
                    copy = len;
                if (skb_put_bits(list, offset-start, src, copy))
                    goto fault;
                if ((len -= copy) == 0)
                    return 0;
                offset += copy;
                src += copy;
            }
            start = end;
        }
    }
#else
    i=0;
#endif

    if (len == 0)
        return 0;

 fault:
    return -EFAULT;
}

int skboffset(struct sk_buff *skb, unsigned char *ptr){
    if(!ptr || ptr < skb->head || ptr > skb->tail){
        return -1;
    }
    return (ptr - skb->head);
}

/** Print some bits of an skb.
 *
 * @param skb to print
 * @param offset byte offset to start printing at
 * @param n number of bytes to print
 */
void skb_print_bits(const char *msg, struct sk_buff *skb, int offset, int n){
    int chunk = 16;
    int i, k;
    u8 buff[chunk];
    if(!skb) return;
    printk("%s> tot=%d len=%d data=%d mac=%d nh=%d h=%d\n",
           msg,
           skb->tail - skb->head,
           skb->len,
           skboffset(skb, skb->data),
           skboffset(skb, skb->mac.raw),
           skboffset(skb, skb->nh.raw),
           skboffset(skb, skb->h.raw));
    printk("%s> head=%p data=%p mac=%p nh=%p h=%p tail=%p\n",
           msg, skb->head, skb->data,
           skb->mac.raw, skb->nh.raw, skb->h.raw,
           skb->tail);
    while(n){
        k = (n > chunk ? chunk : n);
        skb_copy_bits(skb, offset, buff, k);
        printk("%03d ", offset);
        for(i=0; i<k; i++){
            if(i == 8)printk(" "); 
            printk(":%02x", buff[i] & 0xff);
        }
        printk(" \n");
        n -= k;
        offset += k;
    }
}

/** Print a buffer.
 *
 * @param buf to print
 * @param n number of bytes to print
 */
void buf_print(char *buf, int n){
    int i;
    for(i=0; i<n; i++){
        if( i % 16 == 0) printk("\n%04d ", i);
        else if(i % 8 == 0) printk(" ");
        printk(":%02x", buf[i] & 0xff);
    }
    printk(" %04d\n", n);
}

/** Remove some space from the tail of an skb.
 *
 * @todo fixme: Do we need to handle frags?
 */
void *skb_trim_tail(struct sk_buff *skb, int n){
    skb->tail -= n;
    skb->len -= n;
    return skb->tail;
}

#ifdef __KERNEL__

static const int DEBUG_SCATTERLIST = 0;

#if LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,0)
#define SET_SCATTER_ADDR(sg, addr) do{} while(0)
#else
#define SET_SCATTER_ADDR(sg, addr) (sg).address = (addr)
#endif

/** Convert a (possibly fragmented) skb into a scatter list.
 *
 * @param skb skb to convert
 * @param sg scatterlist to set up
 * @param sg_n size of sg on input, number of elements set on output
 * @param offset offset into data to start at
 * @param len number of bytes
 * @return 0 on success, error code otherwise
 */
int skb_scatterlist(struct sk_buff *skb, struct scatterlist *sg, int *sg_n,
                    int offset, int len){
    int err = 0;
    int start;		// No. of bytes copied so far (where next copy starts).
    int size;		// Size of the next chunk.
    int end;		// Where the next chunk ends (start + size).
    int copy;		// Number of bytes to copy in one operation.
    int sg_i = 0;	// Index into sg.
    int i;
    
    if(DEBUG_SCATTERLIST){
        dprintf("> offset=%d len=%d (end=%d), skb len=%d,\n",
                offset, len, offset+len, skb->len);
    }
    start = 0;
    size = skb_headlen(skb);
    end = start + size;
    copy = end - offset;
    if(copy > 0){
        char *p;
        if(copy > len) copy = len;
        if(sg_i >= *sg_n){
            err = -EINVAL;
            goto exit;
        }
        p = skb->data + offset;
        SET_SCATTER_ADDR(sg[sg_i], NULL);
        sg[sg_i].page = virt_to_page(p);
        sg[sg_i].offset = ((unsigned long)p & ~PAGE_MASK);
        sg[sg_i].length = copy;
        if(DEBUG_SCATTERLIST){
            dprintf("> sg_i=%d .page=%p .offset=%u .length=%d\n",
                    sg_i, sg[sg_i].page, sg[sg_i].offset, sg[sg_i].length);
        }
        sg_i++;
        if((len -= copy) == 0) goto exit;
        offset += copy;
    }
    start = end;
    for (i = 0; i < skb_shinfo(skb)->nr_frags; i++){
        BUG_TRAP(start <= offset + len);
        size = skb_shinfo(skb)->frags[i].size;
        end = start + size;
        copy = end - offset;
        if(copy > 0){
            skb_frag_t *frag = &skb_shinfo(skb)->frags[i];
            if(copy > len) copy = len;
            if(sg_i >= *sg_n){
                err = -EINVAL;
                goto exit;
            }
            SET_SCATTER_ADDR(sg[sg_i], NULL);
            sg[sg_i].page = frag->page;
            sg[sg_i].offset = frag->page_offset + offset - start;
            sg[sg_i].length = copy;
            if(DEBUG_SCATTERLIST){
                dprintf("> sg_i=%d .page=%p .offset=%u .length=%d\n",
                        sg_i, sg[sg_i].page, sg[sg_i].offset, sg[sg_i].length);
            }
            sg_i++;
            if((len -= copy) == 0) goto exit;
            offset += copy;
        }
        start = end;
    }
  exit:
    if(!err) *sg_n = sg_i;
    if(len) wprintf("> len=%d\n", len);
    if(len) BUG();
    if(err) dprintf("< err=%d sg_n=%d\n", err, *sg_n);
    return err;
}

#endif

void print_skb_data(const char *msg, int count, struct sk_buff *skb, u8 *data, int len)
{
    static int skb_count = 1000000;
    u8 *ptr, *end;
    u32 src_addr, dst_addr;
    // Transport layer header.
    union {
        struct tcphdr  *th;
        struct udphdr  *uh;
        struct icmphdr *icmph;
        struct igmphdr *igmph;
        struct iphdr   *ipiph;
        unsigned char  *raw;
    } h;
    // Network layer header.
    union {
        struct iphdr   *iph;
        struct ipv6hdr *ipv6h;
        struct arpheader  *arph;
        struct ipxhdr  *ipxh;
        unsigned char  *raw;
    } nh;
    // Link layer header.
    union {
        struct ethhdr  *ethernet;
        unsigned char  *raw;
    } mac;
    int protocol;
    if(!count) count = ++skb_count;
    if(!msg) msg = (char *)__FUNCTION__;
    if(!data){
        printk("%s.%d> null data\n", msg, count);
        return;
    }
    ptr = data;
    end = data + len;
    mac.raw = ptr;
    ptr += sizeof(struct ethhdr);
    if(ptr > end){ printk("***MAC:");  goto exit; }
    protocol = ntohs(mac.ethernet->h_proto);
    nh.raw = ptr;

    printk("%s.%d> type=%d protocol=0x%x\n",
           msg, count, skb->pkt_type, htons(skb->protocol));
    if(1){
        printk("%s.%d> %p mac src=" MACFMT " dst=" MACFMT "\n",
               msg, count, data,
               MAC6TUPLE(mac.ethernet->h_source),
               MAC6TUPLE(mac.ethernet->h_dest));
    }

    switch(protocol){
    case ETH_P_ARP:
        ptr += sizeof(struct arpheader);
        if(ptr > end){ printk("***ARP:");  goto exit; }
        if(0){
            printk("%s.%d> ARP hrd=%d, pro=%d, hln=%d, pln=%d, op=%d\n",
                   msg, count,
                   nh.arph->ar_hrd, nh.arph->ar_pro, nh.arph->ar_hln,
                   nh.arph->ar_pln, nh.arph->ar_op);
        }
        memcpy(&src_addr, nh.arph->ar_sip, 4);
        src_addr = ntohl(src_addr);
        memcpy(&dst_addr, nh.arph->ar_tip, 4);
        dst_addr = ntohl(dst_addr);
        printk("%s.%d> ARP HW src=" MACFMT " dst=" MACFMT "\n",
               msg, count, MAC6TUPLE(nh.arph->ar_sha), MAC6TUPLE(nh.arph->ar_tha));
        printk("%s.%d> ARP IP src=" IPFMT " dst=" IPFMT "\n",
               msg, count, HIPQUAD(src_addr), HIPQUAD(dst_addr));
        break;
    case ETH_P_IP: {
        u16 src_port, dst_port;
        if(ptr + sizeof(struct iphdr) > end){ printk("***IP:");  goto exit; }
        src_addr = ntohl(nh.iph->saddr);
        dst_addr = ntohl(nh.iph->daddr);
        if(1){
            printk("%s.%d> IP proto=%d src=" IPFMT " dst=" IPFMT "\n",
                   msg, count, nh.iph->protocol,
                   HIPQUAD(src_addr), HIPQUAD(dst_addr));
            printk("%s.%d> IP tot_len=%u len=%d\n",
                   msg, count, ntohs(nh.iph->tot_len), len - ETH_HLEN);
        }
        ptr += (nh.iph->ihl * 4);
        if(ptr > end){ printk ("***IP: len"); goto exit; }
        h.raw = ptr;
        switch(nh.iph->protocol){
        case IPPROTO_TCP:
            ptr += sizeof(struct tcphdr);
            if(ptr > end){ printk("***TCP:"); goto exit; }
            src_port = ntohs(h.th->source);
            dst_port = ntohs(h.th->dest);
            printk("%s.%d> TCP src=" IPFMT ":%u dst=" IPFMT ":%u\n",
                   msg, count,
                   HIPQUAD(src_addr), src_port,
                   HIPQUAD(dst_addr), dst_port);
            break;
        case IPPROTO_UDP:
            ptr += sizeof(struct udphdr);
            if(ptr > end){ printk("***UDP:"); goto exit; }
            src_port = ntohs(h.uh->source);
            dst_port = ntohs(h.uh->dest);
            printk("%s.%d> UDP src=" IPFMT ":%u dst=" IPFMT ":%u\n",
                   msg, count,
                   HIPQUAD(src_addr), src_port,
                   HIPQUAD(dst_addr), dst_port);
            break;
        default:
            printk("%s.%d> IP %d src=" IPFMT " dst=" IPFMT "\n",
                   msg, count,
                   nh.iph->protocol, HIPQUAD(src_addr), HIPQUAD(dst_addr));
            break;
        }
        break; }
    case ETH_P_IPV6:
        printk("%s.%d> IPv6\n", msg, count);
        break;
    case ETH_P_IPX:
        printk("%s.%d> IPX\n", msg, count);
        break;
    default:
        printk("%s.%d> protocol=%d\n", msg, count, protocol);
        break;
    }
    return;
  exit:
    printk("%s.%d> %s: skb problem\n", msg, count, __FUNCTION__);
    printk("%s.%d> %s: data=%p end=%p(%d) ptr=%p(%d) eth=%d ip=%d\n",
           msg, count, __FUNCTION__,
           data, end, end - data, ptr, ptr - data,
           sizeof(struct ethhdr),
           sizeof(struct iphdr));
    return;
}

void print_skb(const char *msg, int count, struct sk_buff *skb){
    print_skb_data(msg, count, skb, skb->mac.raw, skb->tail - skb->mac.raw);
}

void print_ethhdr(const char *msg, struct sk_buff *skb){
    struct ethhdr *eth;

    if(!skb || skboffset(skb, skb->mac.raw) < 0) return;
    eth = eth_hdr(skb);
    printk("%s> ETH proto=%d src=" MACFMT " dst=" MACFMT "\n",
           msg,
           ntohs(eth->h_proto),
           MAC6TUPLE(eth->h_source),
           MAC6TUPLE(eth->h_dest));
}

void print_iphdr(const char *msg, struct sk_buff *skb){
    u32 src_addr, dst_addr;
    
    if(!skb || skboffset(skb, skb->nh.raw) < 0) return;
    src_addr = ntohl(skb->nh.iph->saddr);
    dst_addr = ntohl(skb->nh.iph->daddr);
    printk("%s> IP proto=%d src=" IPFMT " dst=" IPFMT " tot_len=%u\n",
           msg,
           skb->nh.iph->protocol,
           HIPQUAD(src_addr),
           HIPQUAD(dst_addr),
           ntohs(skb->nh.iph->tot_len));
}

void print_udphdr(const char *msg, struct sk_buff *skb){
    if(!skb || skboffset(skb, skb->h.raw) < 0) return;
    printk("%s> UDP src=%u dst=%u len=%u\n",
           msg,
           ntohs(skb->h.uh->source),
           ntohs(skb->h.uh->dest),
           ntohs(skb->h.uh->len));
}
