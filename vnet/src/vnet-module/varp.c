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
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/string.h>
#include <linux/version.h>

#include <linux/net.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netdevice.h>
#include <linux/inetdevice.h>
#include <linux/udp.h>

#include <net/ip.h>
#include <net/protocol.h>
#include <net/route.h>
#include <linux/skbuff.h>
#include <linux/spinlock.h>
#include <asm/semaphore.h>

#else

#include "sys_kernel.h"
#include <netinet/in.h>
#include <arpa/inet.h>
#include <linux/ip.h>
#include <linux/udp.h>
#include "spinlock.h"
#include "skbuff.h"

#endif

#include <tunnel.h>
#include <vnet.h>
#include <vif.h>
#include <if_varp.h>
#include <varp.h>
#include <varp_util.h>
#include <vnet.h>
#include <etherip.h>
#include <vnet_forward.h>

#include "allocate.h"
#include "iostream.h"
#include "hash_table.h"
#include "sys_net.h"
#include "sys_string.h"
#include "skb_util.h"
#include "timer_util.h"

#define MODULE_NAME "VARP"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file VARP: Virtual ARP.
 *
 * Handles virtual ARP requests for vnet/vmac.
 */

/*

Varp uses UDP on port 1798.

on domain up: ?
  send varp.announce { id, vmac, vnet, coa } for each vif
  that haven't announced before, or has changed.
  install vif entries in local table.

on varp.announce{ id, vmac, vnet, coa }:
  update VARP entry for vmac x vnet if have one, reset ttl.

on varp.request { id, vmac, vnet }:
  if have a vif for the requested vmac/vnet,
  reply with varp.announce{ id, vmac, vnet, coa }

on timer:
  traverse VARP table, flush old entries.

on probe timer:
  probe again if not out of tries.
  if out of tries invalidate entry.

*/

/** Time-to-live of varp entries (in jiffies).*/
#define VARP_ENTRY_TTL      (60*HZ)

/** Maximum number of varp probes to make. */
#define VARP_PROBE_MAX      5

/** Interval between varp probes (in jiffies). */
#define VARP_PROBE_INTERVAL (3*HZ)

/** Maximum number of queued skbs for a varp entry. */
#define VARP_QUEUE_MAX      16

/** Number of buckets in the varp table (must be prime). */
#define VARP_TABLE_BUCKETS  3001

/** Varp entry states. */
enum {
    VARP_STATE_INCOMPLETE = 1,
    VARP_STATE_REACHABLE = 2,
    VARP_STATE_FAILED = 3,
};

/** Varp entry flags. */
enum {
    VARP_FLAG_PROBING = 1,
    VARP_FLAG_PERMANENT = 2,
};

/** Key for varp entries. */
typedef struct VarpKey {
    /** Vnet id (network order). */
    VnetId vnet;
    /** Virtual MAC address. */
    Vmac vmac;
} VarpKey;

/** An entry in the varp cache. */
typedef struct VarpEntry {
    /** Key for the entry. */
    VarpKey key;
    /** Care-of address for the key. */
    VarpAddr addr;
    /** Last-updated timestamp. */
    unsigned long timestamp;
    /** State. */
    short state;
    /** Flags. */
    short flags;
    /** Reference count. */
    atomic_t refcount;
    /** Lock. */
    rwlock_t lock;
    unsigned long lflags;

    /** How many probes have been made. */
    atomic_t probes;
    /** Probe timer. */
    struct timer_list timer;
    void (*error)(struct VarpEntry *ventry, struct sk_buff *skb);
    /** Outbound skb queue. */
    struct sk_buff_head queue;
    /** Maximum size of the queue. */
    int queue_max;
    atomic_t deleted;
} VarpEntry;

/** The varp cache. Varp entries indexed by VarpKey. */
typedef struct VarpTable {

    HashTable *table;

    /** Sweep timer. */
    struct timer_list timer;

    rwlock_t lock;
    struct semaphore mutex;

    int entry_ttl;
    int probe_max;
    int probe_interval;
    int queue_max;

} VarpTable;

/** The varp cache. */
static VarpTable *varp_table = NULL;

/** Module parameter for the multicast address. */
static char *varp_mcaddr = NULL;

/** Multicast address (network order). */
u32 varp_mcast_addr = 0;

/** UDP port (network order). */
u16 varp_port = 0;

char *varp_device = "xen-br0";

#define VarpTable_read_lock(vtable, flags)    \
  do{ read_lock_irqsave(&(vtable)->lock, (flags)); } while(0)

#define VarpTable_read_unlock(vtable, flags)  \
  do{ read_unlock_irqrestore(&(vtable)->lock, (flags)); } while(0)

#define VarpTable_write_lock(vtable, flags)    \
  do{ write_lock_irqsave(&(vtable)->lock, (flags)); } while(0)

#define VarpTable_write_unlock(vtable, flags)  \
  do{ write_unlock_irqrestore(&(vtable)->lock, (flags)); } while(0)

#define VarpEntry_lock(ventry, flags)    \
  do{ write_lock_irqsave(&(ventry)->lock, (flags)); (ventry)->lflags = (flags); } while(0)

#define VarpEntry_unlock(ventry, flags)  \
  do{ (flags) = (ventry)->lflags; write_unlock_irqrestore(&(ventry)->lock, (flags)); } while(0)

void VarpTable_sweep(VarpTable *vtable);
void VarpTable_flush(VarpTable *vtable);
void VarpTable_print(VarpTable *vtable, IOStream *io);
int VarpEntry_output(VarpEntry *ventry, struct sk_buff *skb);

#include "./varp_util.c"

/** Print the varp cache (if debug on).
 */
void varp_dprint(void){
#ifdef DEBUG
    VarpTable_print(varp_table, iostdout);
#endif
} 

/** Flush the varp cache.
 */
void varp_flush(void){
    VarpTable_flush(varp_table);
}

#ifdef __KERNEL__
static int device_ucast_addr(const char *device, uint32_t *addr)
{
    int err;
    struct net_device *dev = NULL;

    err = vnet_get_device(device, &dev);
    if(err) goto exit;
    err = vnet_get_device_address(dev, addr);
  exit:
    if(err){
        *addr = 0;
    }
    return err;
}

/** Get the unicast address of the varp device.
 */
int varp_ucast_addr(uint32_t *addr)
{
    int err = -ENODEV;
    const char *devices[] = { varp_device, "eth0", "eth1", "eth2", NULL };
    const char **p;
    for(p = devices; err && *p; p++){
        err = device_ucast_addr(*p, addr);
    }
    return err;
}

/** Lookup a network device by name.
 *
 * @param name device name
 * @param dev return parameter for the device
 * @return 0 on success, error code otherwise
 */
int vnet_get_device(const char *name, struct net_device **dev){
    int err = 0;
    *dev = dev_get_by_name(name);
    if(!*dev){
        err = -ENETDOWN;
    }
    return err;
}

/** Get the source address from a device.
 *
 * @param dev device
 * @param addr return parameter for address
 * @return 0 on success, error code otherwise
 */
int vnet_get_device_address(struct net_device *dev, u32 *addr){
    int err = 0;
    struct in_device *in_dev;

    in_dev = in_dev_get(dev);
    if(!in_dev){
        err = -ENODEV;
        goto exit;
    }
    *addr = in_dev->ifa_list->ifa_address;
    in_dev_put(in_dev);
  exit:
    return err;
}

#else

int varp_ucast_addr(uint32_t *addr)
{
    return 0;
}

#endif

/** Print varp info and the varp cache.
 */
void varp_print(IOStream *io){
    uint32_t addr = 0;
    varp_ucast_addr(&addr);

    IOStream_print(io, "(varp \n");
    IOStream_print(io, " (device %s)\n", varp_device);
    IOStream_print(io, " (mcast_addr " IPFMT ")\n", NIPQUAD(varp_mcast_addr));
    IOStream_print(io, " (ucast_addr " IPFMT ")\n", NIPQUAD(addr));
    IOStream_print(io, " (port %d)\n", ntohs(varp_port));
    IOStream_print(io, " (encapsulation %s)\n",
                   (etherip_in_udp ? "etherip_in_udp" : "etherip"));
    IOStream_print(io, " (entry_ttl %lu)\n", varp_table->entry_ttl);
    IOStream_print(io, ")\n");
    VarpTable_print(varp_table, io);
}

#ifdef __KERNEL__

#if LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,0)

static inline int addr_route(u32 daddr, struct rtable **prt){
    int err = 0;
    struct flowi fl = {
        .nl_u = {
            .ip4_u = {
                .daddr = daddr,
            }
        }
    };
    
    err = ip_route_output_key(prt, &fl);
    return err;
}

#else

static inline int addr_route(u32 daddr, struct rtable **prt){
    int err = 0;
    struct rt_key key = { .dst = daddr };
    err = ip_route_output_key(prt, &key);
    return err;
}

#endif // LINUX_VERSION_CODE

#ifndef LL_RESERVED_SPACE
#define HH_DATA_MOD	16
#define LL_RESERVED_SPACE(dev) \
        ((dev->hard_header_len & ~(HH_DATA_MOD - 1)) + HH_DATA_MOD)

#endif // LL_RESERVED_SPACE

#else // __KERNEL__

#define ip_eth_mc_map(daddr, dmac) do{ }while(0)

#endif // __KERNEL__

/** Send a varp protocol message.
 *
 * @param opcode varp opcode (host order)
 * @param dev device (may be null)
 * @param skb skb being replied to (may be null)
 * @param vnet vnet id (in network order)
 * @param vmac vmac (in network order)
 * @return 0 on success, error code otherwise
 */
int varp_send(u16 opcode, struct net_device *dev, struct sk_buff *skbin,
              VnetId *vnet, Vmac *vmac){
    int err = 0;
    int link_n = 0;
    int ip_n = sizeof(struct iphdr);
    int udp_n = sizeof(struct udphdr);
    int varp_n = sizeof(VarpHdr);
    struct sk_buff *skbout = NULL;
    VarpHdr *varph = NULL;
    u8 smacbuf[6] = {}, dmacbuf[6] = {};
    u8 *smac = smacbuf, *dmac = dmacbuf;
    u32 saddr = 0, daddr = 0;
    u16 sport = 0, dport = 0;
#if defined(DEBUG)
    char vnetbuf[VNET_ID_BUF];
#endif

    dprintf("> opcode=%d vnet= %s vmac=" MACFMT "\n",
            opcode, VnetId_ntoa(vnet, vnetbuf), MAC6TUPLE(vmac->mac));

    dport = varp_port;
    if(skbin){
        daddr = skbin->nh.iph->saddr;
        dmac = eth_hdr(skbin)->h_source;
        sport = skbin->h.uh->dest;
    } else {
        if(MULTICAST(varp_mcast_addr)){
            daddr = varp_mcast_addr;
            ip_eth_mc_map(daddr, dmac);
        } else {
            daddr = INADDR_BROADCAST;
        }
        sport = varp_port;
    }

#ifdef __KERNEL__
    {
        struct in_device *in_dev = NULL;
        if(!dev){
            struct rtable *rt = NULL;
            err = addr_route(daddr, &rt);
            if(err) goto exit;
            dev = rt->u.dst.dev;
        }
        
        in_dev = in_dev_get(dev);
        if(!in_dev){
            err = -ENODEV;
            goto exit;
        }
        link_n = LL_RESERVED_SPACE(dev);
        saddr = in_dev->ifa_list->ifa_address;
        smac = dev->dev_addr;
        if(daddr == INADDR_BROADCAST){
            daddr = in_dev->ifa_list->ifa_broadcast;
            dmac = dev->broadcast;
        }
        in_dev_put(in_dev);
    }
#else
    {
        extern uint32_t vnetd_addr(void); 
        saddr = vnetd_addr();
    }
#endif // __KERNEL__

    dprintf("> dev=%s\n", (dev ? dev->name : "<none>"));
    dprintf("> smac=" MACFMT " dmac=" MACFMT "\n", MAC6TUPLE(smac), MAC6TUPLE(dmac));
    dprintf("> saddr=" IPFMT " daddr=" IPFMT "\n", NIPQUAD(saddr), NIPQUAD(daddr));
    dprintf("> sport=%u dport=%u\n", ntohs(sport), ntohs(dport));

    skbout = alloc_skb(link_n + ip_n + udp_n + varp_n, GFP_ATOMIC);
    if (!skbout){
        err = -ENOMEM;
        goto exit;
    }
    skbout->dev = dev;
    skb_reserve(skbout, link_n);
    skbout->protocol = htons(ETH_P_IP);

#ifdef __KERNEL__
    // Device header. Pushes device header on front of skb.
    if (dev->hard_header){
        err = dev->hard_header(skbout, dev, ETH_P_IP, dmac, smac, skbout->len);
        if(err < 0) goto exit;
        //skbout->mac.raw = skbout->data;
        skb_reset_mac_header(skbout);
    }
#else
    smac = smac; // Defeat unused variable warning.
#endif // __KERNEL__

    // IP header.
    skbout->nh.raw = skb_put(skbout, ip_n);
    skbout->nh.iph->version  = 4;
    skbout->nh.iph->ihl      = ip_n / 4;
    skbout->nh.iph->tos      = 0;
    skbout->nh.iph->tot_len  = htons(ip_n + udp_n + varp_n);
    skbout->nh.iph->id       = 0;
    skbout->nh.iph->frag_off = 0;
    skbout->nh.iph->ttl      = 64;
    skbout->nh.iph->protocol = IPPROTO_UDP;
    skbout->nh.iph->saddr    = saddr;
    skbout->nh.iph->daddr    = daddr;  
    skbout->nh.iph->check    = 0;

    // UDP header.
    skbout->h.raw = skb_put(skbout, udp_n);
    skbout->h.uh->source     = sport;
    skbout->h.uh->dest       = dport;
    skbout->h.uh->len        = htons(udp_n + varp_n);
    skbout->h.uh->check      = 0;

    // Varp header.
    varph = (void*)skb_put(skbout, varp_n);
    *varph = (VarpHdr){};
    varph->hdr.id            = htons(VARP_ID);
    varph->hdr.opcode        = htons(opcode);
    varph->vnet              = *vnet;
    varph->vmac              = *vmac;
    varph->addr.family       = AF_INET;
    varph->addr.u.ip4.s_addr = saddr;

    err = skb_xmit(skbout);

  exit:
    if(err && skbout) kfree_skb(skbout);
    dprintf("< err=%d\n", err);
    return err;
}


/** Send a varp request for the vnet and destination mac of a packet.
 * Assumes the ventry is locked.
 *
 * @param skb packet
 * @param vnet vnet (in network order)
 * @return 0 on success, error code otherwise
 */
int varp_solicit(VnetId *vnet, Vmac *vmac){
    return varp_send(VARP_OP_REQUEST, NULL, NULL, vnet, vmac);
}

/* Test some flags.
 *
 * @param ventry varp entry
 * @param flags to test
 * @return nonzero if flags set
 */
int VarpEntry_get_flags(VarpEntry *ventry, int flags){
    return ventry->flags & flags;
}

/** Set some flags.
 *
 * @param ventry varp entry
 * @param flags to set
 * @param set set flags on if nonzero, off if zero
 * @return new flags value
 */
int VarpEntry_set_flags(VarpEntry *ventry, int flags, int set){
    if(set){
        ventry->flags |= flags;
    } else {
        ventry->flags &= ~flags;
    }
    return ventry->flags;
}

/** Print a varp entry.
 *
 * @param ventry varp entry
 */
void VarpEntry_print(VarpEntry *ventry, IOStream *io){
    IOStream_print(io, "(ventry \n");
    if(ventry){
        unsigned long now = jiffies;
        char *state, *flags;
        char vnetbuf[VNET_ID_BUF];
        char addrbuf[VARP_ADDR_BUF];

        switch(ventry->state){
        case VARP_STATE_INCOMPLETE: state = "incomplete"; break;
        case VARP_STATE_REACHABLE:  state = "reachable"; break;
        case VARP_STATE_FAILED:     state = "failed"; break;
        default:                    state = "unknown"; break;
        }
        flags = (VarpEntry_get_flags(ventry, VARP_FLAG_PROBING) ? "P" : "-");

        IOStream_print(io, " (ref %d)\n", atomic_read(&ventry->refcount));
        IOStream_print(io, " (state %s)\n", state);
        IOStream_print(io, " (flags %s)\n", flags);
        IOStream_print(io, " (addr %s)\n", VarpAddr_ntoa(&ventry->addr, addrbuf));
        IOStream_print(io, " (queue %d)\n", skb_queue_len(&ventry->queue));
        IOStream_print(io, " (age %lu)\n", now - ventry->timestamp);
        IOStream_print(io, " (vmac " MACFMT ")\n", MAC6TUPLE(ventry->key.vmac.mac));
        IOStream_print(io, " (vnet %s)\n", VnetId_ntoa(&ventry->key.vnet, vnetbuf));
    }
    IOStream_print(io, ")\n");
}

/** Free a varp entry.
 *
 * @param ventry varp entry
 */
static void VarpEntry_free(VarpEntry *ventry){
    if(!ventry) return;
    deallocate(ventry);
}

/** Increment reference count.
 *
 * @param ventry varp entry (may be null)
 */
void VarpEntry_incref(VarpEntry *ventry){
    if(!ventry) return;
    atomic_inc(&ventry->refcount);
}

/** Decrement reference count, freeing if zero.
 *
 * @param ventry varp entry (may be null)
 */
void VarpEntry_decref(VarpEntry *ventry){
    if(!ventry) return;
    if(atomic_dec_and_test(&ventry->refcount)){
        VarpEntry_free(ventry);
    }
}

/** Call the error handler.
 *
 * @param ventry varp entry
 */
void VarpEntry_error(VarpEntry *ventry){
    struct sk_buff *skb;
    skb = skb_peek(&ventry->queue);
    if(!skb) return;
    if(ventry->error) ventry->error(ventry, skb);
    skb_queue_purge(&ventry->queue);
}

/** Schedule the varp entry timer.
 * Must increment the reference count before doing
 * this the first time, so the ventry won't be freed
 * before the timer goes off.
 *
 * @param ventry varp entry
 */
void VarpEntry_schedule(VarpEntry *ventry){
    timer_set(&ventry->timer, VARP_PROBE_INTERVAL);
}

/** Function called when a varp entry timer goes off.
 * If the entry is still incomplete, carries on probing.
 * Otherwise stops probing.
 *
 * @param arg ventry
 */
static void varp_timer_fn(unsigned long arg){
    unsigned long flags;
    VarpEntry *ventry = (VarpEntry *)arg;
    struct sk_buff *skb = NULL;
    int probing = 0;

    dprintf(">\n");
    VarpEntry_lock(ventry, flags);
    if(!atomic_read(&ventry->deleted)){
        switch(ventry->state){
        case VARP_STATE_REACHABLE:
        case VARP_STATE_FAILED:
            break;
        case VARP_STATE_INCOMPLETE:
            // Probe if haven't run out of tries, otherwise fail.
            if(atomic_read(&ventry->probes) < VARP_PROBE_MAX){
                unsigned long qflags;
                VnetId vnet;
                Vmac vmac;

                probing = 1;
                spin_lock_irqsave(&ventry->queue.lock, qflags);
                skb = skb_peek(&ventry->queue);
                if(skb){
                    vmac = *(Vmac*)eth_hdr(skb)->h_dest;
                }
                spin_unlock_irqrestore(&ventry->queue.lock, qflags);
                if(skb){
                    dprintf("> skbs in queue - solicit\n");
                    vnet = ventry->key.vnet;
                    atomic_inc(&ventry->probes);
                    VarpEntry_unlock(ventry, flags);
                    varp_solicit(&vnet, &vmac);
                    VarpEntry_lock(ventry, flags);        
                } else {
                    dprintf("> empty queue.\n");
                }
                VarpEntry_schedule(ventry);
            } else {
                VarpEntry_error(ventry);
                ventry->state = VARP_STATE_FAILED;
            }
            break;
        }
    }
    VarpEntry_set_flags(ventry, VARP_FLAG_PROBING, probing);
    VarpEntry_unlock(ventry, flags);
    if(!probing) VarpEntry_decref(ventry);
    dprintf("<\n");
}

/** Default error function for varp entries.
 *
 * @param ventry varp entry
 * @param skb packet dropped because of error
 */
static void varp_error_fn(VarpEntry *ventry, struct sk_buff *skb){
}

/** Create a varp entry. Initializes the internal state.
 *
 * @param vnet vnet id
 * @param vmac virtual MAC address (copied)
 * @return ventry or null
 */
VarpEntry * VarpEntry_new(VnetId *vnet, Vmac *vmac){
    VarpEntry *ventry = ALLOCATE(VarpEntry);
    if(ventry){
        unsigned long now = jiffies;

        atomic_set(&ventry->refcount, 1);
        atomic_set(&ventry->probes, 0);
        atomic_set(&ventry->deleted, 0);
        ventry->lock = RW_LOCK_UNLOCKED;
        ventry->state = VARP_STATE_INCOMPLETE;
        ventry->queue_max = VARP_QUEUE_MAX;
        skb_queue_head_init(&ventry->queue);
        timer_init(&ventry->timer, varp_timer_fn, ventry);
        ventry->timestamp = now;
        ventry->error = varp_error_fn;

        ventry->key.vnet = *vnet;
        ventry->key.vmac = *vmac;
    }
    return ventry;
}

/** Hash function for keys in the varp cache.
 * Hashes the vnet id and mac.
 *
 * @param k key (VarpKey)
 * @return hashcode
 */
static Hashcode varp_key_hash_fn(void *k){
    return hash_hvoid(0, k, sizeof(VarpKey));
}

/** Test equality for keys in the varp cache.
 * Compares vnet and mac.
 *
 * @param k1 key to compare (VarpKey)
 * @param k2 key to compare (VarpKey)
 * @return 1 if equal, 0 otherwise
 */
static int varp_key_equal_fn(void *k1, void *k2){
    return memcmp(k1, k2, sizeof(VarpKey)) == 0;
}

/** Free an entry in the varp cache.
 *
 * @param table containing table
 * @param entry entry to free
 */
static void varp_entry_free_fn(HashTable *table, HTEntry *entry){
    VarpEntry *ventry;
    if(!entry) return;
    ventry = entry->value;
    if(ventry) VarpEntry_decref(ventry);
    HTEntry_free(entry);
}

/** Free the whole varp cache.
 * Dangerous.
 *
 * @param vtable varp cache
 */
void VarpTable_free(VarpTable *vtable){
    unsigned long vtflags;
    dprintf(">\n");
    if(!vtable) return;
    VarpTable_write_lock(vtable, vtflags);
    timer_cancel(&vtable->timer);
    vtable->timer.data = 0;
    if(vtable->table){
        HashTable *table = vtable->table;
        HashTable_for_decl(entry);

        vtable->table = NULL;
        HashTable_for_each(entry, table){
            VarpEntry *ventry = entry->value;
            unsigned long flags;
            VarpEntry_lock(ventry, flags);
            atomic_set(&ventry->deleted, 1);
            if(VarpEntry_get_flags(ventry, VARP_FLAG_PROBING)){
                timer_cancel(&ventry->timer);
                ventry->timer.data = 0;
                VarpEntry_decref(ventry);
            }
            VarpEntry_unlock(ventry, flags);
        }
        HashTable_free(table); 
    }
    VarpTable_write_unlock(vtable, vtflags);
    deallocate(vtable);
}

/** Schedule the varp table timer.
 *
 * @param vtable varp table
 */
void VarpTable_schedule(VarpTable *vtable){
    timer_set(&vtable->timer, vtable->entry_ttl);
}

/** Function called when the varp table timer goes off.
 * Sweeps old varp cache entries and reschedules itself.
 *
 * @param arg varp table
 */
static void varp_table_timer_fn(unsigned long arg){
    VarpTable *vtable = (VarpTable *)arg;
    if(vtable){
        VarpTable_sweep(vtable);
        VarpTable_schedule(vtable);
    }
}

/** Print a varp table.
 *
 * @param vtable table
 */
void VarpTable_print(VarpTable *vtable, IOStream *io){
    HashTable_for_decl(entry);
    VarpEntry *ventry;
    unsigned long vtflags, flags;

    VarpTable_read_lock(vtable, vtflags);
    HashTable_for_each(entry, vtable->table){
        ventry = entry->value;
        VarpEntry_lock(ventry, flags);
        VarpEntry_print(ventry, io);
        VarpEntry_unlock(ventry, flags);
    }
    VarpTable_read_unlock(vtable, vtflags);
}

/** Create a varp table.
 *
 * @return new table or null
 */
VarpTable * VarpTable_new(void){
    int err = -ENOMEM;
    VarpTable *vtable = NULL;

    vtable = ALLOCATE(VarpTable);
    if(!vtable) goto exit;
    vtable->table = HashTable_new(VARP_TABLE_BUCKETS);
    if(!vtable->table) goto exit;
    vtable->table->key_size = sizeof(VarpKey);
    vtable->table->key_equal_fn = varp_key_equal_fn;
    vtable->table->key_hash_fn = varp_key_hash_fn;
    vtable->table->entry_free_fn = varp_entry_free_fn;

    vtable->entry_ttl = VARP_ENTRY_TTL;
    vtable->probe_max = VARP_PROBE_MAX;
    vtable->probe_interval = VARP_PROBE_INTERVAL;
    vtable->queue_max = VARP_QUEUE_MAX;

    init_MUTEX(&vtable->mutex);
    vtable->lock = RW_LOCK_UNLOCKED;
    timer_init(&vtable->timer, varp_table_timer_fn, vtable);
    err = 0;
  exit:
    if(err){
        VarpTable_free(vtable);
        vtable = NULL;
    }
    return vtable;
}

/** Add a new entry to the varp table.
 *
 * @param vtable table
 * @param vnet vnet id
 * @param vmac virtual MAC address (copied)
 * @return new entry or null
 */
VarpEntry * VarpTable_add(VarpTable *vtable, VnetId *vnet, Vmac *vmac){
    int err = 0;
    VarpKey key = { .vnet = *vnet, .vmac = *vmac};
    VarpEntry *ventry = NULL;
    HTEntry *entry = NULL;
    unsigned long vtflags;

    VarpTable_write_lock(vtable, vtflags);
    ventry = HashTable_get(vtable->table, &key);
    if(ventry){
        VarpEntry_incref(ventry);
        goto exit;
    }
    err = -ENOMEM;
    ventry = VarpEntry_new(vnet, vmac);
    if(!ventry) goto exit;
    entry = HashTable_add(vtable->table, ventry, ventry);
    if(!entry){
        VarpEntry_decref(ventry);
        ventry = NULL;
        goto exit;
    }
    err = 0;
    VarpEntry_incref(ventry);
  exit:
    VarpTable_write_unlock(vtable, vtflags);
    return ventry;
}

/** Remove an entry from the varp table.
 *
 * @param vtable table
 * @param ventry entry to remove
 * @return removed count
 */
int VarpTable_remove(VarpTable *vtable, VarpEntry *ventry){
    //TODO: Could send a varp announce with null addr for the entry
    // vnet and vmac to notify others, so they will resolve the addr
    // instead of sending traffic to us.
    atomic_set(&ventry->deleted, 1);
    skb_queue_purge(&ventry->queue);
    return HashTable_remove(vtable->table, ventry);
}

/** Remove all entries using a vnet.
 * Caller must hold the table lock.
 *
 * @param vtable table
 * @param vnet vnet
 * @return removed count
 */
int VarpTable_remove_vnet(VarpTable *vtable, VnetId *vnet){
    int count = 0;
    HashTable_for_decl(entry);

    HashTable_for_each(entry, vtable->table){
        VarpEntry *ventry = entry->value;
        if(VnetId_eq(&ventry->key.vnet, vnet)){
            count += VarpTable_remove(vtable, ventry);
        }
    }
    return count;
}

/** Remove all entries using a vnet from the varp table.
 *
 * @param vnet vnet
 * @return removed count
 */
int varp_remove_vnet(VnetId *vnet){
    int count = 0;
    unsigned long vtflags;

    VarpTable_write_lock(varp_table, vtflags);
    count = VarpTable_remove_vnet(varp_table, vnet);
    VarpTable_write_unlock(varp_table, vtflags);
    return count;
}

/** Lookup an entry in the varp table.
 *
 * @param vtable table
 * @param vnet vnet id
 * @param vmac virtual MAC address
 * @param create create a new entry if needed if true
 * @return entry found or null
 */
VarpEntry * VarpTable_lookup(VarpTable *vtable, VnetId *vnet, Vmac *vmac, int create){
    VarpKey key = { .vnet = *vnet, .vmac = *vmac };
    VarpEntry *ventry = NULL;
    unsigned long vtflags;

    VarpTable_read_lock(vtable, vtflags);
    ventry = HashTable_get(vtable->table, &key);
    if(ventry) VarpEntry_incref(ventry);
    VarpTable_read_unlock(vtable, vtflags);

    if(!ventry && create){
        ventry = VarpTable_add(vtable, vnet, vmac);
    }
    return ventry;
}

/** Handle output for a reachable ventry.
 * Send the skb using the tunnel to the care-of address.
 * Assumes the ventry lock is held.
 *
 * @param ventry varp entry
 * @param skb skb to send
 * @return 0 on success, error code otherwise
 */
int VarpEntry_send(VarpEntry *ventry, struct sk_buff *skb){
    int err = 0;
    unsigned long flags = 0;
    VarpAddr addr;
    VnetId vnet;

    dprintf("> skb=%p\n", skb);
    vnet = ventry->key.vnet;
    addr = ventry->addr;
    VarpEntry_unlock(ventry, flags);
    err = vnet_tunnel_send(&vnet, &addr, skb);
    VarpEntry_lock(ventry, flags);
    dprintf("< err=%d\n", err);
    return err;
}

/** Handle output for a non-reachable ventry. Send messages to complete it.
 * If the entry is still incomplete, queue the skb, otherwise
 * send it. If the queue is full, dequeue and free an old skb to
 * make room for the new one.
 * Assumes the ventry lock is held.
 *
 * @param ventry varp entry
 * @param skb skb to send
 * @return 0 on success, error code otherwise
 */
int VarpEntry_resolve(VarpEntry *ventry, struct sk_buff *skb){
    int err = 0;
    unsigned long flags = 0;
    VnetId vnet;
    Vmac vmac;

    dprintf("> skb=%p\n", skb);
    ventry->state = VARP_STATE_INCOMPLETE;
    atomic_set(&ventry->probes, 1);
    if(!VarpEntry_get_flags(ventry, VARP_FLAG_PROBING)){
        VarpEntry_set_flags(ventry, VARP_FLAG_PROBING, 1);
        VarpEntry_incref(ventry);
        VarpEntry_schedule(ventry);
    }
    vnet = ventry->key.vnet;
    vmac = *(Vmac*)eth_hdr(skb)->h_dest;
    VarpEntry_unlock(ventry, flags);
    varp_solicit(&vnet, &vmac);
    VarpEntry_lock(ventry, flags);

    if(ventry->state == VARP_STATE_INCOMPLETE){
        while(skb_queue_len(&ventry->queue) >= ventry->queue_max){
            struct sk_buff *oldskb;
            oldskb = skb_dequeue(&ventry->queue);
            //oldskb = ventry->queue.next;
            //__skb_unlink(oldskb, &ventry->queue);
            if(!oldskb) break;
            dprintf("> dropping skb=%p\n", oldskb);
            kfree_skb(oldskb);
        }
        skb_queue_tail(&ventry->queue, skb);
    } else {
        err = VarpEntry_send(ventry, skb);
    }
    dprintf("< err=%d\n", err);
    return err;
}

/** Process the output queue for a ventry.  Sends the queued skbs if
 * the ventry is reachable, otherwise drops them.
 *
 * @param ventry varp entry
 */
void VarpEntry_process_queue(VarpEntry *ventry){
    struct sk_buff *skb;
    for( ; ; ){
        if(ventry->state != VARP_STATE_REACHABLE) break;
        skb = skb_dequeue(&ventry->queue);
        if(!skb) break;
        VarpEntry_send(ventry, skb);
    }
    skb_queue_purge(&ventry->queue);
}

/** Multicast an skb on a vnet.
 *
 * @param vnet vnet id
 * @param skb skb to send
 * @return 0 on success, error code otherwise
 */
static int varp_multicast(VnetId *vnet, struct sk_buff *skb){
    VarpAddr addr = { .family = AF_INET };
    addr.u.ip4.s_addr = varp_mcast_addr;
    return vnet_tunnel_send(vnet, &addr, skb);
}

/** Handle output for a ventry. Resolves the ventry
 * if necessary.
 *
 * @param ventry varp entry
 * @param skb skb to send
 * @return 0 on success, error code otherwise
 */
int VarpEntry_output(VarpEntry *ventry, struct sk_buff *skb){
    int err = 0;
    unsigned long flags;

    VarpEntry_lock(ventry, flags);
    switch(ventry->state){
    case VARP_STATE_REACHABLE:
        if(skb_queue_len(&ventry->queue) > 0){
            VarpEntry_process_queue(ventry);
        }
        err = VarpEntry_send(ventry, skb);
        break;
    default: 
        if(0){
            err = VarpEntry_resolve(ventry, skb);
        } else {     
            // Multicast the skb if the entry is not reachable.
            VnetId vnet = ventry->key.vnet;
            VarpEntry_unlock(ventry, flags);
            err = varp_multicast(&vnet, skb);
            VarpEntry_lock(ventry, flags);
        }
        break;
    }
    VarpEntry_unlock(ventry, flags);
    return err;
}

/** Update a ventry. Sets the address and state to those given
 * and sets the timestamp to 'now'.
 *
 * @param ventry varp entry
 * @param addr care-of address
 * @param state state
 * @return 0 on success, error code otherwise
 */
int VarpEntry_update(VarpEntry *ventry, VarpAddr *addr, int state, int vflags){
    int err = 0;
    unsigned long now = jiffies;
    unsigned long flags;

    VarpEntry_lock(ventry, flags);
    //if(atomic_read(&ventry->deleted)) goto exit;
    if(VarpEntry_get_flags(ventry, VARP_FLAG_PERMANENT)) goto exit;
    ventry->addr = *addr;
    ventry->timestamp = now;
    ventry->state = state;
    // Can't process the queue while atomic as it calls schedule(),
    // and that's bad.
    //if(0 && (vflags & VARP_UPDATE_QUEUE) && !in_atomic()){
    //    VarpEntry_process_queue(ventry);
    //}
  exit:
    VarpEntry_unlock(ventry, flags);
    dprintf("< err=%d\n", err);
    return err;
}
    
/** Update the entry for a vnet.
 *
 * @param vtable varp table
 * @param vnet vnet id
 * @param vmac mac address
 * @param addr care-of-address
 * @param state state
 * @param flags update flags
 * @return 0 on success, error code otherwise
 */
int VarpTable_update(VarpTable *vtable, VnetId *vnet, Vmac *vmac, VarpAddr *addr,
                     int state, int flags){
    int err = 0;
    VarpEntry *ventry;
#ifdef DEBUG
    char vnetbuf[VNET_ID_BUF];
    char addrbuf[VARP_ADDR_BUF];
    
    dprintf("> vnet=%s mac=" MACFMT " addr=%s state=%d flags=%x\n",
            VnetId_ntoa(vnet, vnetbuf),
            MAC6TUPLE(vmac->mac),
            VarpAddr_ntoa(addr, addrbuf),
            state,
            flags);
#endif
    ventry = VarpTable_lookup(vtable, vnet, vmac, (flags & VARP_UPDATE_CREATE));
    if(!ventry){
        err = -ENOENT;
        goto exit;
    }
    err = VarpEntry_update(ventry, addr, state, flags);
    VarpEntry_decref(ventry);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Update the entry for a vnet: make it reachable and create an entry
 * if needed.
 *
 * @param vnet vnet id
 * @param vmac mac address
 * @param addr care-of-address
 * @return 0 on success, error code otherwise
 */
int varp_update(VnetId *vnet, unsigned char *vmac, VarpAddr *addr){
    int err = 0;
    if(!varp_table){
        err = -ENOSYS;
    } else {
        err = VarpTable_update(varp_table, vnet, (Vmac*)vmac, addr,
                               VARP_STATE_REACHABLE, VARP_UPDATE_CREATE);
    }
    return err;
}

static inline int VarpEntry_sweepable(VarpEntry *ventry){
    return !VarpEntry_get_flags(ventry, (VARP_FLAG_PERMANENT | VARP_FLAG_PROBING));
}

static inline int VarpTable_old(VarpTable *vtable, VarpEntry *ventry, unsigned long now){
    return now - ventry->timestamp > vtable->entry_ttl;
}

/** Sweep old varp entries.
 * Doesn't affect entries that are probing or permanent.
 *
 * @param vtable table
 */
void VarpTable_sweep(VarpTable *vtable){
    HashTable_for_decl(entry);
    VarpEntry *ventry;
    unsigned long now = jiffies;
    unsigned long vtflags, flags;
    int sweep, swept = 0;

    if(!vtable) return;
    VarpTable_write_lock(vtable, vtflags);
    HashTable_for_each(entry, vtable->table){
        ventry = entry->value;
        VarpEntry_lock(ventry, flags);
        sweep = VarpEntry_sweepable(ventry) && VarpTable_old(vtable, ventry, now);
        if(sweep){
            swept++;
            //iprintf("> Sweeping:\n");
            //VarpEntry_print(ventry, iostdout);
            //VarpEntry_process_queue(ventry);
            ventry->state = VARP_STATE_INCOMPLETE;
        }
        VarpEntry_unlock(ventry, flags);
        if(sweep){
            VarpTable_remove(vtable, ventry);
        }
    }
    VarpTable_write_unlock(vtable, vtflags);
    if(swept){
        iprintf(">\n");
        varp_print(iostdout);
    }
}

/** Flush the varp table.
 *
 * @param vtable table
 */
void VarpTable_flush(VarpTable *vtable){
    HashTable_for_decl(entry);
    VarpEntry *ventry;
    unsigned long vtflags, flags;
    int flush;

    VarpTable_write_lock(vtable, vtflags);
    HashTable_for_each(entry, vtable->table){
        ventry = entry->value;
        VarpEntry_lock(ventry, flags);
        flush = (!VarpEntry_get_flags(ventry, VARP_FLAG_PERMANENT) &&
                 !VarpEntry_get_flags(ventry, VARP_FLAG_PROBING));                
        if(flush){
            iprintf("> Flushing:\n");
            VarpEntry_print(ventry, iostdout);
        }
        VarpEntry_unlock(ventry, flags);
        if(flush){
            VarpTable_remove(vtable, ventry);
        }
    }
    VarpTable_write_unlock(vtable, vtflags);
}

/** Handle a varp request. Look for a vif with the requested 
 * vnet and vmac. If find one, reply with the vnet, vmac and our
 * address. Otherwise do nothing.
 *
 * @param skb incoming message
 * @param varph varp message
 * @return 0 if ok, -ENOENT if no matching vif, or error code
 */
int varp_handle_request(struct sk_buff *skb, VarpHdr *varph){
    int err = -ENOENT;
    VnetId *vnet;
    Vmac *vmac;
    Vif *vif = NULL;

    dprintf(">\n");
    vnet = &varph->vnet;
    vmac = &varph->vmac;
    if(vif_lookup(vnet, vmac, &vif)) goto exit;
    varp_send(VARP_OP_ANNOUNCE, skb->dev, skb, vnet, vmac);
    vif_decref(vif);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Announce the vnet and vmac of a vif (gratuitous varp).
 *
 * @param dev device to send on (may be null)
 * @param vif vif
 * @return 0 on success, error code otherwise
 */
int varp_announce_vif(struct net_device *dev, Vif *vif){
    int err = 0;
    dprintf(">\n");
    if(!varp_table){
        err = -ENOSYS;
        goto exit;
    }
    err = varp_send(VARP_OP_ANNOUNCE, dev, NULL, &vif->vnet, &vif->vmac);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Handle a varp announce message.
 * Update the matching ventry if we have one.
 *
 * @param skb incoming message
 * @param varp message
 * @return 0 if OK, -ENOENT if no matching entry
 */
int varp_handle_announce(struct sk_buff *skb, VarpHdr *varph){
    int err = 0;

    dprintf(">\n");
    err = VarpTable_update(varp_table,
                           &varph->vnet, &varph->vmac, &varph->addr,
                           VARP_STATE_REACHABLE, 
                           (VARP_UPDATE_CREATE | VARP_UPDATE_QUEUE));
    dprintf("< err=%d\n", err);
    return err;
}

/** Handle an incoming varp message.
 *
 * @param skb incoming message
 * @return 0 if OK, error code otherwise
 */
int varp_handle_message(struct sk_buff *skb){
    // Assume nh, h set, skb->data points at udp hdr (h).
    int err = -EINVAL;
    VarpHdr *varph; // = (void*)(skb->h.uh + 1);

    dprintf("> skb=%p saddr=" IPFMT " daddr=" IPFMT " head=%p tail=%p data=%p len=%d data_len=%d users=%d\n",
            skb,
            NIPQUAD(skb->nh.iph->saddr),
            NIPQUAD(skb->nh.iph->daddr), 
            skb->head, skb->tail, skb->data,skb->len, skb->data_len, 
            atomic_read(&skb->users));

    if(!varp_table){
        err = -ENOSYS;
        return err;
    }
#ifdef __KERNEL__
    if (skb_is_nonlinear(skb) && skb_linearize(skb) != 0) {
        err = -ENOMEM;
        goto exit;
    }
#endif
    if(MULTICAST(skb->nh.iph->daddr)){
        if(skb->nh.iph->daddr != varp_mcast_addr){
            // Ignore multicast packets not addressed to us.
            err = 0;
            dprintf("> Ignoring daddr=" IPFMT " mcaddr=" IPFMT "\n",
                    NIPQUAD(skb->nh.iph->daddr), NIPQUAD(varp_mcast_addr));
            goto exit;
        }
    }
#ifdef __KERNEL__
    varph = (void*)  __pskb_pull(skb, sizeof(struct udphdr));
#else
    varph = (void*)skb_pull_vn(skb, sizeof(struct udphdr));
#endif


    if(skb->len < sizeof(struct VnetMsgHdr)){
        wprintf("> Varp msg too short: %d < %ld\n", skb->len, sizeof(struct VnetMsgHdr));
        goto exit;
    }

    if (! varph || ! skb->data) {
      err = -EINVAL;
      goto exit;
    }

    switch(ntohs(varph->hdr.id)){
    case VARP_ID: // Varp message. Handled below.
        if(skb->len < sizeof(*varph)){
            wprintf("> Varp msg too short: %d < %ld\n", skb->len, sizeof(*varph));
            goto exit;
        }
        break;
    case VUDP_ID: // Etherip-in-udp packet.
        skb_pull_vn(skb, sizeof(struct VnetMsgHdr));
        err = etherip_protocol_recv(skb);
        goto exit;
    case VFWD_ID: // Forwarded.
        skb_pull_vn(skb, sizeof(struct VnetMsgHdr));
        err = vnet_forward_recv(skb);
        goto exit;
    default:
        // It's not varp at all - ignore it.
        wprintf("> Invalid varp id: %d\n", ntohs(varph->hdr.id));
        print_skb("INVALID", 0, skb);
        goto exit;
    }
#ifdef DEBUG
    {
        char vnetbuf[VNET_ID_BUF];
        char addrbuf[VARP_ADDR_BUF];
        dprintf("> saddr=" IPFMT " daddr=" IPFMT "\n",
                NIPQUAD(skb->nh.iph->saddr), NIPQUAD(skb->nh.iph->daddr));
        dprintf("> sport=%u dport=%u\n", ntohs(skb->h.uh->source), ntohs(skb->h.uh->dest));
        dprintf("> opcode=%d vnet=%s vmac=" MACFMT " addr=%s\n",
                ntohs(varph->hdr.opcode),
                VnetId_ntoa(&varph->vnet, vnetbuf),
                MAC6TUPLE(varph->vmac.mac),
                VarpAddr_ntoa(&varph->addr, addrbuf));
        varp_dprint();
    }
#endif
    switch(ntohs(varph->hdr.opcode)){
    case VARP_OP_REQUEST:
        err = varp_handle_request(skb, varph);
        break;
    case VARP_OP_ANNOUNCE:
        err = varp_handle_announce(skb, varph);
        break;
    default:
        wprintf("> Unknown opcode: %d \n", ntohs(varph->hdr.opcode));
        break;
    }
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Send an outgoing packet on the appropriate vnet tunnel.
 *
 * @param skb outgoing message
 * @param vnet vnet (network order)
 * @return 0 on success, error code otherwise
 */
int varp_output(struct sk_buff *skb, VnetId *vnet){
    int err = 0;
    unsigned char *mac = NULL;
    Vmac *vmac = NULL;
    VarpEntry *ventry = NULL;
#if defined(DEBUG)
    char vnetbuf[VNET_ID_BUF];
#endif

    dprintf("> vnet=%s\n", VnetId_ntoa(vnet, vnetbuf));
    if(!varp_table){
        err = -ENOSYS;
        goto exit;
    }
    if(!skb->mac.raw){
        wprintf("> No ethhdr in skb!\n");
        err = -EINVAL;
        goto exit;
    }
    mac = eth_hdr(skb)->h_dest;
    vmac = (Vmac*)mac;
    if(mac_is_multicast(mac)){
        err = varp_multicast(vnet, skb);
    } else {
        ventry = VarpTable_lookup(varp_table, vnet, vmac, 1);
        if(ventry){
            err = VarpEntry_output(ventry, skb);
            VarpEntry_decref(ventry);
        } else {
            err = -ENOMEM;
        }
    }
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Set the varp multicast address (after initialization).
 *
 * @param addr address (network order)
 * @return 0 on success, error code otherwise
 */
int varp_set_mcast_addr(uint32_t addr){
    int err = 0;
    varp_close();
    varp_mcast_addr = addr;
    err = varp_open(varp_mcast_addr, varp_port);
    return err;
}

/** Initialize the varp multicast address from a module parameter.
 *
 * @param s address in IPv4 notation
 * @return 0 on success, error code otherwise
 */
static void varp_init_mcast_addr(char *s){
    unsigned long v = 0;

    dprintf("> %s\n", s);
    if(s && (get_inet_addr(s, &v) >= 0)){
        varp_mcast_addr = (u32)v;
    } else {
        varp_mcast_addr = htonl(VARP_MCAST_ADDR);
    }
}

/** Initialize the varp cache.
 *
 * @return 0 on success, error code otherwise
 */
int varp_init(void){
    int err = 0;
    
    dprintf(">\n");
    varp_table = VarpTable_new();
    if(!varp_table){
        err = -ENOMEM;
        goto exit;
    }
    VarpTable_schedule(varp_table);
    varp_init_mcast_addr(varp_mcaddr);
    varp_port = htons(VARP_PORT);

    err = varp_open(varp_mcast_addr, varp_port);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Close the varp cache.
 */
void varp_exit(void){
    dprintf(">\n");
    varp_close();
    if(varp_table){
        VarpTable *vtable = varp_table;
        varp_table = NULL;
        VarpTable_free(vtable);
    }
    dprintf("<\n");
}

module_param(varp_mcaddr, charp, 0644);
module_param(varp_device, charp, 0644);
MODULE_PARM_DESC(varp_mcaddr, "VARP multicast address");
MODULE_PARM_DESC(varp_device, "VARP network device");
