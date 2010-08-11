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
#include <linux/version.h>
#include <linux/errno.h>

#include <linux/string.h>
#include <linux/spinlock.h>

#include <linux/net.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netdevice.h>

#include <linux/etherdevice.h>
#include <net/ip.h>
#include <net/protocol.h>
#include <net/route.h>
#include <linux/skbuff.h>
#include <net/checksum.h>


#else 

#include <netinet/in.h>
#include <arpa/inet.h>

#include "sys_kernel.h"
#include "spinlock.h"
#include "skbuff.h"

#include <linux/ip.h>  // For struct iphdr.

extern int netif_rx(struct sk_buff *skb);

#endif

#include <tunnel.h>
#include <sa.h>
#include <varp.h>
#include <if_varp.h>
#include <esp.h>
#include <etherip.h>
#include <random.h>

#include <skb_context.h>

#include <skb_util.h>
#include <vnet_dev.h>
#include <vnet.h>
#include <vnet_forward.h>
#include <vif.h>
#include <vnet_ioctl.h>
#include <sa.h>
#ifdef __KERNEL__
#include <sa_algorithm.h>
#endif

#include "allocate.h"
#include "iostream.h"
#include "hash_table.h"
#include "sys_net.h"
#include "sys_string.h"

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** Default vnet security level.
 */
#ifdef __KERNEL__
#if (LINUX_VERSION_CODE == KERNEL_VERSION(2,6,18) )
int vnet_security_default = SA_AUTH ; //| SA_CONF;
#else
int vnet_security_default = 0;
#endif
#else
int vnet_security_default = 0;
#endif

/** The physical vnet. */
Vnet *vnet_physical = NULL;

/** Table of vnets indexed by id. */
HashTable *vnet_table = NULL;

rwlock_t vnet_lock = RW_LOCK_UNLOCKED;

#define vnet_table_read_lock(flags)    read_lock_irqsave(&vnet_lock, flags)
#define vnet_table_read_unlock(flags)  read_unlock_irqrestore(&vnet_lock, flags)
#define vnet_table_write_lock(flags)   write_lock_irqsave(&vnet_lock, flags)
#define vnet_table_write_unlock(flags) write_unlock_irqrestore(&vnet_lock, flags)

/** Decrement reference count, freeing if zero.
 *
 * @param info vnet (OK if null)
 */
void Vnet_decref(Vnet *info){
    if(!info) return;
    if(atomic_dec_and_test(&info->refcount)){
        deallocate(info);
    }
}

/** Increment reference count.
 *
 * @param info vnet (OK if null)
 */
void Vnet_incref(Vnet *info){
    if(!info) return;
    atomic_inc(&info->refcount);
}

void Vnet_print(Vnet *info, IOStream *io)
{
    char vnetbuf[VNET_ID_BUF];
    char *security;

    if(info->security & SA_CONF){
        security = "conf";
    } else if(info->security & SA_AUTH){
        security = "auth";
    } else {
        security = "none";
    }

    IOStream_print(io, "(vnet");
    IOStream_print(io, " (id %s)", VnetId_ntoa(&info->vnet, vnetbuf));
    IOStream_print(io, " (vnetif %s)", info->device);
    IOStream_print(io, " (security %s)", security);
    IOStream_print(io, " (header %d)", info->header_n);
    IOStream_print(io, ")");
}

void vnet_print(IOStream *io)
{
    HashTable_for_decl(entry);
    Vnet *info;
    unsigned long flags;
    
    vnet_table_read_lock(flags);
    HashTable_for_each(entry, vnet_table){
        info = entry->value;
        Vnet_print(info, io);
        IOStream_print(io, "\n");
    }
    vnet_table_read_unlock(flags);
}

/** Allocate a vnet, setting reference count to 1.
 *
 * @param info return parameter for vnet
 * @return 0 on success, error code otherwise
 */
int Vnet_alloc(Vnet **info){
    int err = 0;
    *info = ALLOCATE(Vnet);
    if(*info){
        atomic_set(&(*info)->refcount, 1);
    } else {
        err = -ENOMEM;
    }
    return err;
}

/** Create the virtual interface for a vnet.
 *
 * @param info vnet
 * @return 0 on success, error code otherwise
 */
int Vnet_create(Vnet *info){
    int err = 0;

    err = vnet_dev_add(info);
    if(err) goto exit;
    err = Vnet_add(info);
  exit:
    return err;
}
    
/** Add a vnet to the table under its vnet id.
 *
 * @param info vnet to add
 * @return 0 on success, error code otherwise
 */
int Vnet_add(Vnet *info){
    int err = 0;
    HTEntry *entry = NULL;
    unsigned long flags;

    if(Vnet_lookup(&info->vnet, NULL) == 0){
        //todo: Delete existing vnet info?
        err = -EEXIST;
        goto exit;
    }
    Vnet_incref(info);
    vnet_table_write_lock(flags);
    entry = HashTable_add(vnet_table, &info->vnet, info);
    vnet_table_write_unlock(flags);
    if(!entry){
        err = -ENOMEM;
        vnet_dev_remove(info);
        Vnet_decref(info);
    }
  exit:
    return err;
}

/** Remove a vnet from the table.
 * Also removes all vifs and varp entries for the vnet.
 *
 * @param vnet id of vnet to remove
 * @return number of vnets removed
 */
int Vnet_del(VnetId *vnet){
    int count;
    unsigned long flags;
    Vnet *info;

    vnet_table_write_lock(flags);
    info = HashTable_get(vnet_table, vnet);
    count = HashTable_remove(vnet_table, vnet);
    vnet_table_write_unlock(flags);
    
    varp_remove_vnet(vnet);
    vif_remove_vnet(vnet);

    if(info){
        // Can't do this in the hashtable entry free function because it runs
        // while we hold the vnet table lock, and the vnet tidy up calls
        // vnet_dev_remove(), which calls unregister_netdev(), which schedules.
        vnet_dev_remove(info);
        Vnet_decref(info);
    }
    return count;
}

/** Lookup a vnet by id.
 * References the vnet on success - the caller must decref.
 *
 * @param vnet vnet id
 * @param pinfo return parameter for vnet (or NULL)
 * @return 0 on sucess, -ENOENT if no vnet found
 */
int Vnet_lookup(VnetId *vnet, Vnet **pinfo){
    int err = 0;
    unsigned long flags;
    Vnet *info;

    vnet_table_read_lock(flags);
    info = HashTable_get(vnet_table, vnet);
    if(info){
        if(pinfo){
            Vnet_incref(info);
        }
    } else {
        err = -ENOENT;
    }
    vnet_table_read_unlock(flags);

    if(pinfo){
        *pinfo = (err ? NULL : info);
    }
    return err;
}

static int vnet_key_equal_fn(void *k1, void *k2){
    return memcmp(k1, k2, sizeof(VnetId)) == 0;
}

static Hashcode vnet_key_hash_fn(void *k){
    return hash_hvoid(0, k, sizeof(VnetId));
}

/** Free an entry in the vnet table.
 *
 * @param table containing table
 * @param entry to free
 */
static void vnet_entry_free_fn(HashTable *table, HTEntry *entry){
    if(!entry) return;
    HTEntry_free(entry);
}

void vnet_table_free(void){
    HashTable *vnt;
    HashTable_for_decl(entry);

    vnt = vnet_table;
    if(!vnt) return;
    vnet_table = NULL;
    HashTable_for_each(entry, vnt){
        Vnet *info = entry->value;
        vnet_dev_remove(info);
        Vnet_decref(info);
    }
    HashTable_free(vnt);
}

int vnet_table_init(void){
    int err = 0;
    vnet_table = HashTable_new(0);
    if(!vnet_table){
        err = -ENOMEM;
        goto exit;
    }
    vnet_table->key_size = sizeof(VnetId);
    vnet_table->key_equal_fn = vnet_key_equal_fn;
    vnet_table->key_hash_fn = vnet_key_hash_fn;
    vnet_table->entry_free_fn = vnet_entry_free_fn;

    err = Vnet_alloc(&vnet_physical);
    if(err) goto exit;
    vnet_physical->vnet = toVnetId(VNET_PHYS);
    vnet_physical->security = 0;
    err = Vnet_add(vnet_physical);

  exit:
    if(err){
        vnet_table_free();
    }
    return err;
}

/** Setup some vnet entries (for testing).
 * Vnet 1 is physical, vnets 2 to 10 are insecure, vnets above
 * 10 are secure.
 *
 * @return 0 on success, negative error code otherwise
 */
static int vnet_setup(void){
    int err = 0;
    int i, n = 3;
    int security = vnet_security_default;
    uint32_t vnetid;
    Vnet *vnet;

    for(i=0; i<n; i++){
        err = Vnet_alloc(&vnet);
        if(err) break;
        vnetid = VNET_VIF + i;
        vnet->vnet = toVnetId(vnetid);
        snprintf(vnet->device, 16, "vnif%04x", vnetid);
        vnet->security = (vnetid > 10 ? security : 0);
        err = Vnet_create(vnet);
        Vnet_decref(vnet);
        if(err) break;
    }
    return err;
}

/** Initialize the vnet table and the physical vnet.
 *
 * @return 0 on success, error code otherwise
 */
int vnet_init(void){
    int err = 0;

    err = vnet_forward_init();
    if(err) goto exit;
    err = vnet_table_init();
    if(err) goto exit;
    err = vif_init();
    if(err) goto exit;
    err = varp_init();
    if(err) goto exit;
    err = vnet_setup();
  exit:
    return err;
}

void vnet_exit(void){
    varp_exit();
    vif_exit();
    vnet_table_free();
    vnet_forward_exit();
}

#ifdef __KERNEL__
inline int _skb_xmit(struct sk_buff *skb, uint32_t saddr){
    int err = 0;
    struct rtable *rt = NULL;

    dprintf("> src=%u.%u.%u.%u dst=%u.%u.%u.%u\n",
            NIPQUAD(skb->nh.iph->saddr),
            NIPQUAD(skb->nh.iph->daddr));
    skb->protocol = htons(ETH_P_IP);
    if(saddr){
        skb->nh.iph->saddr = 0;
    }
    err = skb_route(skb, &rt);
    if(err){
        wprintf("> skb_route=%d\n", err);
        wprintf("> dev=%s idx=%d src=%u.%u.%u.%u dst=%u.%u.%u.%u tos=%d\n",
                (skb->dev ? skb->dev->name : "???"),
                (skb->dev ? skb->dev->ifindex : -1),
                NIPQUAD(skb->nh.iph->saddr),
                NIPQUAD(skb->nh.iph->daddr),
                skb->nh.iph->tos);
                
        goto exit;
    }
    dst_release(skb->dst);
    skb->dst = &rt->u.dst;
    if(!skb->dev){
        skb->dev = rt->u.dst.dev;
    }

    ip_select_ident(skb->nh.iph, &rt->u.dst, NULL);

    if(saddr){
        skb->nh.iph->saddr = saddr;
    } else {
        if(!skb->nh.iph->saddr){
            skb->nh.iph->saddr = rt->rt_src;
        }
    }

    ip_send_check(skb->nh.iph);

#if 1
        // Output to skb destination. Will use ip_output(), which fragments.
        // Slightly slower than neigh_compat_output() (marginal - 1%).
        err = dst_output(skb); 
#else
        // Sends direct to device via dev_queue_xmit(). No fragmentation?
        err = neigh_compat_output(skb);
#endif

#if 0
    if(needs_frags){
        err = ip_fragment(skb, ip_finish_output);
    } else {
        err = ip_finish_output(skb);
    }
#endif

  exit:
    dprintf("< err=%d\n", err);
    return err;
}

#else 

extern int _skb_xmit(struct sk_buff *skb, uint32_t saddr);

#endif

int skb_xmit(struct sk_buff *skb){
    if(MULTICAST(skb->nh.iph->daddr)){
        vnet_forward_send(skb);
    }
    return _skb_xmit(skb, 0);
}

/** Called when a vif sends a packet to the network.
 * Encapsulates the packet for its vnet and forwards it.
 *
 * @param skb packet
 * @return 0 on success, error code otherwise
 *
 */
int vnet_skb_send(struct sk_buff *skb, VnetId *vnet){
    VnetId vnet_phys = toVnetId(VNET_PHYS);
    int err = 0;

    //dprintf(">\n");
    skb->dev = NULL;
    if(!vnet || VnetId_eq(vnet, &vnet_phys)){
        // No vnet or physical vnet, send direct to the network. 
        skb_xmit(skb);
    } else {
        // Update the vif table with the source MAC.
        vif_update(vnet, (Vmac*)eth_hdr(skb)->h_source);
        err = varp_output(skb, vnet);
    }
    //dprintf("< err=%d\n", err);
    return err;
}

/** Receive an skb for a vnet.
 * We make the skb come out of the vif for the vnet, and
 * let ethernet bridging forward it to related interfaces.
 *
 * The packet must have skb->mac.raw set and skb->data must point
 * after the device (ethernet) header.
 *
 * Return code 1 means we now own the packet - the caller must not free it.
 * Return code < 0 means an error - caller still owns the packet.
 *
 * @param skb packet
 * @param vnet packet vnet
 */
int vnet_skb_recv(struct sk_buff *skb, Vnet *vnet){
    int err = 1;

    if(!vnet->dev){
        // No device for the vnet.
        err = -ENOTCONN;
        goto exit;
    }
    skb->dev = vnet->dev;
    vnet->stats.rx_packets++;
    vnet->stats.rx_bytes += skb->len;
    netif_rx(skb);
  exit:
    return err;
}


/** Check that a context has the correct properties w.r.t. a vnet.
 * The context must be secure if the vnet requires security.
 *
 * @param vnet vnet id
 * @param context context
 * @return 0 on success, error code otherwise
 *
 * @todo Need to check that the sa provides the correct security level.
 */
int vnet_check_context(VnetId *vnet, SkbContext *context, Vnet **val){
    int err = 0;
    Vnet *info = NULL;
    SAState *sa = NULL;
    
    err = Vnet_lookup(vnet, &info);
    if(err){
        goto exit;
    }
    if(!info->security) goto exit;
    err = -EINVAL;
    if(!context){
        wprintf("> No security context\n");
        goto exit;
    }
    if(context->protocol != IPPROTO_ESP){
        wprintf("> Invalid protocol: wanted %d, got %d\n",
                IPPROTO_ESP, context->protocol);
        goto exit;
    }
    sa = context->data;
    //todo: Check security properties of the SA are correct w.r.t. the vnet.
    //Something like  sa->security == info->security;
    err = 0;
  exit:
    *val = info;
    return err;
}


/** Create a tunnel for a vnet to a given address.
 *
 * @param vnet vnet id
 * @param addr destination address
 * @param tunnel return parameter
 * @return 0 on success, error code otherwise
 */
static int vnet_tunnel_create(VnetId *vnet, VarpAddr *addr, Tunnel **tunnel){
    int err = 0;
    Vnet *info = NULL;
    Tunnel *base = NULL;
    Tunnel *sa_tunnel = NULL;
    Tunnel *eth_tunnel = NULL;

    err = Vnet_lookup(vnet, &info);
    if(err) goto exit;

    if(info->security){
        err = sa_tunnel_create(info, addr, base, &sa_tunnel);
        if(err) goto exit;
        base = sa_tunnel;
    }
    err = etherip_tunnel_create(vnet, addr, base, &eth_tunnel);
  exit:
    Tunnel_decref(sa_tunnel);
    Vnet_decref(info);
    *tunnel = (err ? NULL : eth_tunnel);
    return err;
}

/** Lookup a tunnel for a vnet to a given address.
 * Uses an existing tunnel if there is one.
 *
 * @param vnet vnet id
 * @param addr care-of address
 * @param tunnel return parameter
 * @return 0 on success, error code otherwise
 */
int vnet_tunnel_lookup(VnetId *vnet, VarpAddr *addr, Tunnel **tunnel){
    int err = 0;
    err = Tunnel_lookup(vnet, addr, tunnel);
    if(err){
        err = Tunnel_open(vnet, addr, vnet_tunnel_create, tunnel);
    }
    return err;
}

/** Send a packet on the appropriate tunnel.
 *
 * @param vnet vnet
 * @param addr tunnel endpoint
 * @param skb packet
 * @return 0 on success, error code otherwise
 */
int vnet_tunnel_send(VnetId *vnet, VarpAddr *addr, struct sk_buff *skb){
    int err = 0;
    Tunnel *tunnel = NULL;

    err = vnet_tunnel_lookup(vnet, addr, &tunnel);
    if(err) {
        char vnetbuf[VNET_ID_BUF];
        char addrbuf[VARP_ADDR_BUF];
        wprintf("No tunnel: skb=%p vnet=%s addr=%s\n",
                skb,
                VnetId_ntoa(vnet, vnetbuf),
                VarpAddr_ntoa(addr, addrbuf));
        goto exit;
    }
    err = Tunnel_send(tunnel, skb);
    Tunnel_decref(tunnel);
  exit:
    return err;
}

#ifdef __KERNEL__

/** Module parameter for vnet encapsulation. */
static char *vnet_encaps = NULL;

static void __exit vnet_module_exit(void){
    ProcFS_exit();
    sa_table_exit();
    vnet_exit();
    esp_module_exit();
    etherip_module_exit();
    tunnel_module_exit();
    random_module_exit();
}

/** Initialize the vnet module.
 * Failure is fatal.
 *
 * @return 0 on success, error code otherwise
 */
static int __init vnet_module_init(void){
    int err = 0;

    if(vnet_encaps && !strcmp(vnet_encaps, "udp")){
        etherip_in_udp = 1;
    }
    dprintf(">\n");
    err = random_module_init();
    if(err) wprintf("> random_module_init err=%d\n", err);
    if(err) goto exit;
    err = tunnel_module_init();
    if(err) wprintf("> tunnel_module_init err=%d\n", err);
    if(err) goto exit;
    err = etherip_module_init();
    if(err) wprintf("> etherip_module_init err=%d\n", err);
    if(err) goto exit;
    err = esp_module_init();
    if(err) wprintf("> esp_module_init err=%d\n", err);
    if(err) goto exit;
    err = vnet_init();
    if(err) wprintf("> vnet_init err=%d\n", err);
    if(err) goto exit;
#ifdef __KERNEL__
#if (LINUX_VERSION_CODE == KERNEL_VERSION(2,6,18) )
    sa_algorithm_probe_all();
#endif
#endif
    err = sa_table_init();
    if(err) wprintf("> sa_table_init err=%d\n", err);
    if(err) goto exit;
    ProcFS_init();
  exit:
    if(err < 0){
        vnet_module_exit();
        wprintf("< err=%d\n", err);
    }
    return err;
}

module_init(vnet_module_init);
module_exit(vnet_module_exit);
MODULE_LICENSE("GPL");

module_param(vnet_encaps, charp, 0644);
MODULE_PARM_DESC(vnet_encaps, "Vnet encapsulation: etherip or udp.");

#endif
