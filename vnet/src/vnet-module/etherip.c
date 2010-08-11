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
#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/init.h>

#include <linux/version.h>

#include <linux/skbuff.h>
#include <linux/net.h>
#include <linux/netdevice.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netfilter_bridge.h>
#include <linux/netfilter_ipv4.h>
#include <linux/icmp.h>
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

#define IP_DF		0x4000		/* Flag: "Don't Fragment"	*/

#endif

#include <etherip.h>
#include <tunnel.h>
#include <vnet.h>
#include <varp.h>
#include <if_varp.h>
#include <varp.h>
#include <skb_util.h>
#include <skb_context.h>

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file Etherip implementation.
 * The etherip protocol is used to transport Ethernet frames in IP packets.
 */

/** Flag controlling whether to use etherip-in-udp encapsulation.
 * If false we send etherip protocol in IP packets.
 * If true we send etherip protocol in UDP packets with a vnet header.
 */
int etherip_in_udp = 1;

/** Get the vnet label from an etherip header.
 *
 * @param hdr header
 * @@param vnet (in net order)
 */
void etheriphdr_get_vnet(struct etheriphdr *hdr, VnetId *vnet){
#ifdef CONFIG_ETHERIP_EXT
    *vnet = *(VnetId*)hdr->vnet;
#else
    *vnet = (VnetId){};
    vnet->u.vnet16[VNET_SIZE16 - 1] = (unsigned short)hdr->reserved;
    
#endif
}

/** Set the vnet label in an etherip header.
 * Also sets the etherip version.
 *
 * @param hdr header
 * @param vnet vnet label (in net order)
 */
void etheriphdr_set_vnet(struct etheriphdr *hdr, VnetId *vnet){
#ifdef CONFIG_ETHERIP_EXT
    hdr->version = ETHERIP_VERSION;
    *(VnetId*)hdr->vnet = *vnet;
#else
    hdr->version = ETHERIP_VERSION;
    hdr->reserved = (vnet->u.vnet16[VNET_SIZE16 - 1] & 0x0fff);
#endif
}

/** Open an etherip tunnel.
 *
 * @param tunnel to open
 * @return 0 on success, error code otherwise
 */
static int etherip_tunnel_open(Tunnel *tunnel){
    return 0;
}

/** Close an etherip tunnel.
 *
 * @param tunnel to close
 */
static void etherip_tunnel_close(Tunnel *tunnel){
}


static inline int skb_make_headroom(struct sk_buff **pskb, struct sk_buff *skb, int head_n){
    int err = 0;
    dprintf("> skb=%p headroom=%d head_n=%d\n", skb, skb_headroom(skb), head_n);
    if(head_n > skb_headroom(skb) || skb_cloned(skb) || skb_shared(skb)){
        // Expand header the way GRE does.
        struct sk_buff *new_skb = skb_realloc_headroom(skb, head_n + 16);
        if(!new_skb){
            err = -ENOMEM;
            goto exit;
        }
        kfree_skb(skb);
        *pskb = new_skb;
    } else {
        *pskb = skb;
    }
  exit:
    return err;
}
    
/** Send a packet via an etherip tunnel.
 * Adds etherip header and new ip header around ethernet frame.
 *
 * @param tunnel tunnel
 * @param skb packet
 * @return 0 on success, error code otherwise
 */
static int etherip_tunnel_send(Tunnel *tunnel, struct sk_buff *skb){
    int err = 0;
    const int ip_n = sizeof(struct iphdr);
    const int etherip_n = sizeof(struct etheriphdr);
    const int udp_n = sizeof(struct udphdr);
    const int vnet_n = sizeof(struct VnetMsgHdr);
    int head_n = etherip_n + ip_n /* +  ETH_HLEN */;
    VnetId *vnet = &tunnel->key.vnet;
    struct etheriphdr *etheriph;
    u32 saddr = 0;

    if(etherip_in_udp){
        head_n += vnet_n + udp_n;
    }
    err = skb_make_headroom(&skb, skb, head_n);
    if(err) goto exit;

    // Null the pointer as we are pushing a new IP header.
    skb->mac.raw = NULL;

    // Setup the etherip header.
    etheriph = (void*)skb_push(skb, etherip_n);
    etheriphdr_set_vnet(etheriph, vnet);

    if(etherip_in_udp){
        // Vnet header.
        struct VnetMsgHdr *vhdr = (void*)skb_push(skb, vnet_n);
        vhdr->id     = htons(VUDP_ID);
        vhdr->opcode = 0;

        // Setup the UDP header.
        skb->h.raw = skb_push(skb, udp_n);
        skb->h.uh->source = varp_port;		// Source port.
        skb->h.uh->dest   = varp_port;		// Destination port.
        skb->h.uh->len    = htons(skb->len);	// Total packet length (bytes).
        skb->h.uh->check  = 0;
    }

    // Setup the IP header.
    skb->nh.raw = skb_push(skb, ip_n); 
    skb->nh.iph->version  = 4;			// Standard version.
    skb->nh.iph->ihl      = ip_n / 4;		// IP header length (32-bit words).
    skb->nh.iph->tos      = 0;			// No special type-of-service.
    skb->nh.iph->tot_len  = htons(skb->len);    // Total packet length (bytes).
    skb->nh.iph->id       = 0;			// No flow id (since no frags).
    if(etherip_in_udp){
        skb->nh.iph->protocol = IPPROTO_UDP;    // IP protocol number.
        skb->nh.iph->frag_off = 0;
    } else {
        skb->nh.iph->protocol = IPPROTO_ETHERIP;// IP protocol number.
        skb->nh.iph->frag_off = htons(IP_DF);	// Don't fragment - can't handle frags.
    }
    skb->nh.iph->ttl      = 64;			// Linux default time-to-live.
    skb->nh.iph->saddr    = saddr;		// Source address.
    skb->nh.iph->daddr    = tunnel->key.addr.u.ip4.s_addr; // Destination address.
    skb->nh.iph->check    = 0;			// Zero the checksum.

    // Ethernet header will be filled-in by device.
    err = Tunnel_send(tunnel->base, skb);
    skb = NULL;
  exit:
    if(err && skb){
        wprintf("< err=%d\n", err);
        kfree_skb(skb);
    }
    return err;
}

/** Tunnel type for etherip.
 */
static TunnelType _etherip_tunnel_type = {
    .name	= "ETHERIP",
    .open	= etherip_tunnel_open,
    .close	= etherip_tunnel_close,
    .send 	= etherip_tunnel_send
};

TunnelType *etherip_tunnel_type = &_etherip_tunnel_type;

int etherip_tunnel_create(VnetId *vnet, VarpAddr *addr, Tunnel *base, Tunnel **tunnel){
    return Tunnel_create(etherip_tunnel_type, vnet, addr, base, tunnel);
}

#if defined(__KERNEL__) && defined(CONFIG_BRIDGE_NETFILTER)
/** We need our own copy of this as it is no longer exported from the bridge module.
 */
static inline void _nf_bridge_save_header(struct sk_buff *skb){
    int header_size = 16;
    
    // Were using this modified to use h_proto instead of skb->protocol.
    if(skb->protocol == htons(ETH_P_8021Q)){
        header_size = 18;
    }
    memcpy(skb->nf_bridge->data, skb->data - header_size, header_size);
}
#endif

/** Do etherip receive processing.
 * Strips the etherip header to extract the ethernet frame, sets
 * the vnet from the header and re-receives the frame.
 *
 * Return code 1 means we now own the packet - the caller must not free it.
 * Return code < 0 means an error - caller still owns the packet.
 *
 * @param skb packet
 * @return 1 on success, error code otherwise
 */
int etherip_protocol_recv(struct sk_buff *skb){
    int err = 0;
    const int etherip_n = sizeof(struct etheriphdr);
    struct etheriphdr *etheriph;
    Vnet *vinfo = NULL;
    VnetId vnet = {};
    u32 saddr, daddr;
    char vnetbuf[VNET_ID_BUF];
    struct ethhdr *eth;
    struct sk_buff *newskb;

    dprintf(">\n");
    saddr = skb->nh.iph->saddr;
    daddr = skb->nh.iph->daddr;
    if(MULTICAST(daddr) && (daddr != varp_mcast_addr)){
        // Ignore multicast packets not addressed to us.
        wprintf("> Ignoring mcast skb: src=%u.%u.%u.%u dst=%u.%u.%u.%u"
                " varp_mcast_addr=%u.%u.%u.%u\n",
                NIPQUAD(saddr), NIPQUAD(daddr), NIPQUAD(varp_mcast_addr));
        goto exit;
    }
    if(skb->data == skb->mac.raw){
        // skb->data points at ethernet header.
        //FIXME: Does this ever happen?
        //dprintf("> len=%d\n", skb->len);
        int ip_n = (skb->nh.iph->ihl << 2);
        int pull_n = ETH_HLEN + ip_n;
        if (!pskb_may_pull(skb, pull_n)){
            wprintf("> Malformed skb (eth+ip) src=%u.%u.%u.%u\n",
                    NIPQUAD(saddr));
            err = -EINVAL;
            goto exit;
        }
        skb_pull_vn(skb, pull_n);
    }
    // Assume skb->data points at etherip header.
    etheriph = (void*)skb->data;
    if(etheriph->version != ETHERIP_VERSION){
        wprintf("> Bad etherip version=%d src=%u.%u.%u.%u\n",
                etheriph->version, NIPQUAD(saddr));
        err = -EINVAL;
        goto exit;
    }
    if(!pskb_may_pull(skb, etherip_n)){
        wprintf("> Malformed skb (etherip) src=%u.%u.%u.%u\n",
                NIPQUAD(saddr));
        err = -EINVAL;
        goto exit;
    }
    etheriphdr_get_vnet(etheriph, &vnet);
    // If vnet is secure, context must include IPSEC ESP.
    err = vnet_check_context(&vnet, SKB_CONTEXT(skb), &vinfo);
    if(err){
        dprintf("> Failed security check vnet=%s src=%u.%u.%u.%u\n",
                VnetId_ntoa(&vnet, vnetbuf), NIPQUAD(saddr));
        goto exit;
    }
    // Point at the headers in the contained ethernet frame.
#ifdef __KERNEL__
    __pskb_pull(skb, etherip_n);
    skb_reset_mac_header(skb);
#else
    skb->mac.raw = skb_pull_vn(skb, etherip_n);
#endif
    eth = eth_hdr(skb);

    // Simulate the logic from eth_type_trans()
    // to set skb->pkt_type and skb->protocol.
    if(mac_is_multicast(eth->h_dest)){
        if(mac_is_broadcast(eth->h_dest)){
            skb->pkt_type = PACKET_BROADCAST;
        } else {
            skb->pkt_type = PACKET_MULTICAST;
        }
    } else {
        skb->pkt_type = PACKET_HOST;
    }
    if(ntohs(eth->h_proto) >= 1536){
        skb->protocol = eth->h_proto;
    } else {
        skb->protocol = htons(ETH_P_802_2);
    }
    
    // Assuming a standard Ethernet frame.
    // Should check for protocol? Support ETH_P_8021Q too.
#ifndef __KERNEL__
    skb->nh.raw = skb_pull_vn(skb, ETH_HLEN);
    skb->h.raw = skb->nh.raw + sizeof(struct iphdr);
#else
    __pskb_pull(skb, ETH_HLEN);
    skb_reset_network_header(skb);
    skb_set_transport_header(skb, sizeof(struct iphdr));
#endif

#ifdef __KERNEL__
    // Fix IP options, checksum, skb dst, netfilter state.
    memset(&(IPCB(skb)->opt), 0, sizeof(struct ip_options));
//fixme CHECKSUM_HW no longer defined - check replacing with CHECKSUM_COMPLETE.
/*     if(skb->ip_summed == CHECKSUM_HW){ */
/*         skb->ip_summed = CHECKSUM_NONE; */
/*     } */
    if(skb->ip_summed == CHECKSUM_HW){
        skb->ip_summed = CHECKSUM_NONE;
    }
    dst_release(skb->dst);
    skb->dst = NULL;
    nf_reset(skb);
#ifdef CONFIG_BRIDGE_NETFILTER
    if(skb->nf_bridge){
        // Stop the eth header being clobbered by nf_bridge_maybe_copy_header().
        nf_bridge_save_header(skb);
    }
#endif
#endif // __KERNEL__

    dprintf("> Unpacked srcaddr=" IPFMT " dstaddr=" IPFMT " vnet=%s srcmac=" MACFMT " dstmac=" MACFMT "\n",
            NIPQUAD(skb->nh.iph->saddr),
            NIPQUAD(skb->nh.iph->daddr),
            VnetId_ntoa(&vnet, vnetbuf),
            MAC6TUPLE(eth->h_source),
            MAC6TUPLE(eth->h_dest));
    //print_skb(__FUNCTION__, 0, skb);

    {
        // Know source ip, vnet, vmac, so update the varp cache.
        // For this to work forwarded vnet packets must have the
        // original source address.
        VarpAddr addr = { .family = AF_INET };
        addr.u.ip4.s_addr = saddr;
        varp_update(&vnet, eth->h_source, &addr);
    }

    err = vnet_skb_recv(skb, vinfo);
  exit:
    if(vinfo) Vnet_decref(vinfo);
    dprintf("< skb=%p err=%d\n", skb, err);
    return err;
}


#ifdef __KERNEL__

/** Handle an ICMP error related to etherip.
 *
 * @param skb ICMP error packet
 * @param info
 */
static void etherip_protocol_icmp_err(struct sk_buff *skb, u32 info){
    struct iphdr *iph = (struct iphdr*)skb->data;
    
    wprintf("> ICMP error type=%d code=%d addr=" IPFMT "\n",
            skb->h.icmph->type, skb->h.icmph->code, NIPQUAD(iph->daddr));

    if (skb->h.icmph->type != ICMP_DEST_UNREACH ||
        skb->h.icmph->code != ICMP_FRAG_NEEDED){
        return;
    }
    wprintf("> MTU too big addr= " IPFMT "\n", NIPQUAD(iph->daddr)); 
}

//============================================================================
#if LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,0)
// Code for 2.6 kernel.

/** Etherip protocol. */
static struct net_protocol etherip_protocol = {
    .handler	 = etherip_protocol_recv,
    .err_handler = etherip_protocol_icmp_err,
};

static int etherip_protocol_add(void){
    return inet_add_protocol(&etherip_protocol, IPPROTO_ETHERIP);
}

static int etherip_protocol_del(void){
    return inet_del_protocol(&etherip_protocol, IPPROTO_ETHERIP);
}

//============================================================================
#else
//============================================================================
// Code for 2.4 kernel.

/** Etherip protocol. */
static struct inet_protocol etherip_protocol = {
    .name        = "ETHERIP",
    .protocol    = IPPROTO_ETHERIP,
    .handler	 = etherip_protocol_recv,
    .err_handler = etherip_protocol_icmp_err,
};

static int etherip_protocol_add(void){
    inet_add_protocol(&etherip_protocol);
    return 0;
}

static int etherip_protocol_del(void){
    return inet_del_protocol(&etherip_protocol);
}

#endif
//============================================================================


/** Initialize the etherip module.
 * Registers the etherip protocol.
 *
 * @return 0 on success, error code otherwise
 */
int __init etherip_module_init(void) {
    int err = 0;
    etherip_protocol_add();
    return err;
}

/** Finalize the etherip module.
 * Deregisters the etherip protocol.
 */
void __exit etherip_module_exit(void) {
    if(etherip_protocol_del() < 0){
        printk(KERN_INFO "%s: can't remove etherip protocol\n", __FUNCTION__);
    }
}

#endif // __KERNEL__
