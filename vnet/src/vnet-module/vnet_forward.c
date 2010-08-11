/*
 * Copyright (C) 2005, 2006 Mike Wray <mike.wray@hp.com>
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
#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/init.h>

#include <linux/version.h>
#include <linux/spinlock.h>

#include <linux/skbuff.h>
#include <linux/net.h>
#include <linux/netdevice.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netfilter_bridge.h>
#include <linux/netfilter_ipv4.h>
#include <linux/udp.h>

#include <net/ip.h>
#include <net/protocol.h>
#include <net/route.h>
#include <net/checksum.h>

#else

#include <netinet/in.h>
#include <arpa/inet.h>

#include "sys_kernel.h"
#include "spinlock.h"
#include "skbuff.h"
#include <linux/ip.h>
#include <linux/udp.h>

#endif

#include <varp.h>
#include <if_varp.h>
#include <varp.h>
#include <skb_util.h>
#include <skb_context.h>

#include "allocate.h"
#include "iostream.h"
#include "hash_table.h"
#include "vnet_forward.h"

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

extern int _skb_xmit(struct sk_buff *skb, uint32_t saddr);

typedef struct VnetPeer {
    struct VarpAddr addr;
    uint16_t port;
    atomic_t refcount;
    int tx_packets;
    int rx_packets;
} VnetPeer;

static HashTable *vnet_peer_table = NULL;
static rwlock_t vnet_peer_table_lock = RW_LOCK_UNLOCKED;

#define vnet_peer_read_lock(flags)    read_lock_irqsave(&vnet_peer_table_lock, (flags))
#define vnet_peer_read_unlock(flags)  read_unlock_irqrestore(&vnet_peer_table_lock, (flags))
#define vnet_peer_write_lock(flags)   write_lock_irqsave(&vnet_peer_table_lock, (flags))
#define vnet_peer_write_unlock(flags) write_unlock_irqrestore(&vnet_peer_table_lock, (flags))

static void VnetPeer_decref(VnetPeer *peer){
    if(!peer) return;
    if(atomic_dec_and_test(&peer->refcount)){
        kfree(peer);
    }
}

static void VnetPeer_incref(VnetPeer *peer){
    if(!peer) return;
    atomic_inc(&peer->refcount);
}

static void VnetPeer_print(VnetPeer *peer, IOStream *io){
    char addrbuf[VARP_ADDR_BUF];
    
    IOStream_print(io, "(vnet_peer\n");
    IOStream_print(io, "  (addr %s)\n", VarpAddr_ntoa(&peer->addr, addrbuf));
    IOStream_print(io, "  (port %d)\n", htons(peer->port));
    IOStream_print(io, "  (tx_packets %d)\n", peer->tx_packets);
    IOStream_print(io, "  (rx_packets %d)\n", peer->tx_packets);
    IOStream_print(io, ")\n");
}

static int VnetPeer_forward(VnetPeer *peer, struct sk_buff *fwdskb){
    int err = 0;
    const int ip_n = sizeof(struct iphdr);
    const int udp_n = sizeof(struct udphdr);
    const int vnet_n = sizeof(struct VnetMsgHdr);
    int head_n = 16 + ip_n + udp_n + vnet_n;
    int push_n = 0;
    struct sk_buff *skb = NULL;
    struct VnetMsgHdr *vhdr;
    uint32_t saddr = 0;
    uint16_t sport = varp_port;
    uint32_t daddr = peer->addr.u.ip4.s_addr;
    uint16_t dport = varp_port;

    if(!fwdskb) goto exit;
    if(daddr == fwdskb->nh.iph->saddr){
        // Don't forward if the skb src addr is the peer addr.
        dprintf("> Forward loop on " IPFMT "\n", NIPQUAD(daddr));
        goto exit;
    }
    // On entry fwdskb->data should be at fwdskb->nh.raw (adjust if not).
    // Also fwdskb->h.raw and fwdskb->nh.raw are set.
    if(fwdskb->data > fwdskb->nh.raw){
        push_n = fwdskb->data - fwdskb->nh.raw;
        head_n += push_n;
    }
    // If has headroom, copies header (which incs ref on dst),
    // otherwise only clones header, which does not inc ref on dst.
    skb = skb_realloc_headroom(fwdskb, head_n);
    //skb = skb_copy_expand(fwdskb, head_n, 0, GFP_ATOMIC);
    if(!skb){
        err = -ENOMEM;
        goto exit;
    }

    if(push_n){
        skb_push(skb, push_n);
    }

#ifdef DEBUG
    printk("\nOriginal packet:\n");
    print_iphdr(__FUNCTION__, skb);
    skb_print_bits(__FUNCTION__, skb, 0, skb->len);
#endif

    skb->mac.raw = NULL;
    vhdr = (void*)skb_push(skb, vnet_n);
    vhdr->id       = htons(VFWD_ID);
    vhdr->opcode   = 0;

    // Setup the UDP header.
    skb->h.raw = skb_push(skb, udp_n);
    skb->h.uh->source = sport;		        // Source port.
    skb->h.uh->dest   = dport;		        // Destination port.
    skb->h.uh->len    = htons(skb->len);	// Total packet length (bytes).
    skb->h.uh->check  = 0;

    // Setup the IP header.
    skb->nh.raw = skb_push(skb, ip_n); 
    skb->nh.iph->version  = 4;			// Standard version.
    skb->nh.iph->ihl      = ip_n / 4;		// IP header length (32-bit words).
    skb->nh.iph->tos      = 0;			// No special type-of-service.
    skb->nh.iph->tot_len  = htons(skb->len);    // Total packet length (bytes).
    skb->nh.iph->id       = 0;			// No flow id.
    skb->nh.iph->protocol = IPPROTO_UDP;        // IP protocol number.
    skb->nh.iph->frag_off = 0;
    skb->nh.iph->ttl      = 64;			// Linux default time-to-live.
    skb->nh.iph->saddr    = saddr;		// Source address.
    skb->nh.iph->daddr    = daddr;              // Destination address.
    skb->nh.iph->check    = 0;

#ifdef DEBUG
    printk("\nWrapped packet:\n");
    print_iphdr(__FUNCTION__, skb);
    print_udphdr(__FUNCTION__, skb);
    skb_print_bits(__FUNCTION__, skb, 0, skb->len);
#endif

    err = _skb_xmit(skb, saddr);
    peer->tx_packets++;

  exit:
    if(err < 0) kfree_skb(skb);
    return err;
}

int vnet_peer_get(VarpAddr *addr, VnetPeer **peer){
    unsigned long flags;

    vnet_peer_read_lock(flags);
    *peer = HashTable_get(vnet_peer_table, addr);
    VnetPeer_incref(*peer);
    vnet_peer_read_unlock(flags);
    return (*peer ? 0 : -ENOENT);
}

int vnet_peer_add(VarpAddr *addr, uint16_t port){
    int err = 0;
    unsigned long flags;
    VnetPeer *peer;
    
    vnet_peer_write_lock(flags);
    peer = HashTable_get(vnet_peer_table, addr);
    if(peer){
        VnetPeer_incref(peer);
        goto exit;
    }
    peer = ALLOCATE(VnetPeer);
    if(!peer){
        err = -ENOMEM;
        goto exit;
    }
    peer->addr = *addr;
    peer->port = port;
    VnetPeer_incref(peer);
    if(!HashTable_add(vnet_peer_table, &peer->addr, peer)){
        VnetPeer_decref(peer);
        err = -ENOMEM;
    }
  exit:
    vnet_peer_write_unlock(flags);
    return err;
}

int vnet_peer_del(VarpAddr *addr){
    int ret = 0;
    unsigned long flags;

    vnet_peer_write_lock(flags);
    ret = HashTable_remove(vnet_peer_table, addr);
    vnet_peer_write_unlock(flags);
    return ret;
}

void vnet_peer_print(IOStream *io){
    HashTable_for_decl(entry);
    unsigned long flags;

    if(!vnet_peer_table) return;
    vnet_peer_read_lock(flags);
    HashTable_for_each(entry, vnet_peer_table){
        VnetPeer *peer = entry->value;
        VnetPeer_print(peer, io);
    }
    vnet_peer_read_unlock(flags);
}

int vnet_forward_send(struct sk_buff *skb){
    int err = 0;
    unsigned long flags;
    HashTable_for_decl(entry);
    int count = 0;

    if(!vnet_peer_table){
        goto exit;
    }
    vnet_peer_read_lock(flags);
    HashTable_for_each(entry, vnet_peer_table){
        VnetPeer *peer = entry->value;
        VnetPeer_forward(peer, skb);
        count++;
    }
    vnet_peer_read_unlock(flags);
  exit:
    return err;
}

int vnet_forward_recv(struct sk_buff *skb){
    int err = 0;
    VarpAddr addr = { .family = AF_INET };
    VnetPeer *peer = NULL;
    unsigned char eth[ETH_HLEN] = {};
    struct sk_buff *recvskb;

    if(!vnet_peer_table){
        dprintf("> no table\n");
        return -ENOSYS;
    }
    // On entry mac.raw, h.raw, nh.raw are set.
    // skb->data points after the fwd vnet header, at the complete
    // forwarded packet (which has IP hdr, no eth hdr).

    // Save the eth hdr and source addr (peer).
    memcpy(eth, skb->mac.raw, ETH_HLEN);
    addr.u.ip4.s_addr = skb->nh.iph->saddr;
    err = vnet_peer_get(&addr, &peer);
    if(err){
        wprintf("> no peer for " IPFMT "\n", NIPQUAD(skb->nh.iph->saddr));
        goto exit;
    }
    peer->rx_packets++;
    skb->mac.raw = NULL;
    skb->nh.raw = skb->data;
    skb->h.raw = skb->data + sizeof(struct iphdr);
    if(!skb->nh.iph->saddr){
        skb->nh.iph->saddr = addr.u.ip4.s_addr;
    }
#ifdef __KERNEL__
    // Fix IP options, checksum, skb dst, netfilter state.
    memset(&(IPCB(skb)->opt), 0, sizeof(struct ip_options));
    skb->dev = NULL;
    dst_release(skb->dst);
    skb->dst = NULL;
    nf_reset(skb);
#endif // __KERNEL__

    skb->mac.raw = skb->nh.raw - ETH_HLEN;
    memcpy(skb->mac.raw, eth, ETH_HLEN);

    // Map destination mcast addresses to our mcast address.
    if(MULTICAST(skb->nh.iph->daddr)){
        skb->nh.iph->daddr = varp_mcast_addr;
        //xmit does this: ip_eth_mc_map(varp_mcast_addr, eth_hdr(skb)->h_dest);
    }

    // Handle (a copy of) it ourselves, because
    // if it is looped-back by xmit it will be ignored.
    recvskb = alloc_skb(skb->len, GFP_ATOMIC);
    if(recvskb){
        recvskb->protocol = htons(ETH_P_IP);

        recvskb->nh.raw = skb_put(recvskb, skb->len);
        recvskb->h.raw = recvskb->data + sizeof(struct iphdr); 
        skb_copy_bits(skb, 0, recvskb->data, skb->len);
        
        // Data points at the unwrapped iphdr, but varp_handle_message()
        // expects it to point at the udphdr, so pull.
        skb_pull_vn(recvskb, sizeof(struct iphdr));
        if(varp_handle_message(recvskb) <= 0){
            kfree_skb(recvskb);
        }
    }
    err = _skb_xmit(skb, skb->nh.iph->saddr);
    if(err >= 0) err = 1;
  exit:
    return err;
}

/** Hash function for keys in the peer table.
 */
static Hashcode peer_key_hash_fn(void *k){
    return hash_hvoid(0, k, sizeof(struct VarpAddr));
}

/** Equality function for keys in the peer table.
 */
static int peer_key_equal_fn(void *k1, void *k2){
    return memcmp(k1, k2, sizeof(struct VarpAddr)) == 0;
}

static void peer_entry_free_fn(HashTable *table, HTEntry *entry){
    if(!entry) return;
    VnetPeer_decref((VnetPeer*)entry->value);
    HTEntry_free(entry);
}

int vnet_forward_init(void){
    int err = 0;
    if(vnet_peer_table) goto exit;
    vnet_peer_table = HashTable_new(0);
    if(!vnet_peer_table){
        err = -ENOMEM;
        goto exit;
    }
    vnet_peer_table->key_size = sizeof(struct VarpAddr);
    vnet_peer_table->key_equal_fn = peer_key_equal_fn;
    vnet_peer_table->key_hash_fn = peer_key_hash_fn;
    vnet_peer_table->entry_free_fn = peer_entry_free_fn;
  exit:
    return err;
}

void vnet_forward_exit(void){
    HashTable_free(vnet_peer_table);
    vnet_peer_table = NULL;
}
