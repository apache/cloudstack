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
#include <linux/config.h>
#include <linux/module.h>
#include <linux/types.h>
#include <linux/sched.h>
#include <linux/kernel.h>

#include <linux/skbuff.h>
#include <linux/netdevice.h>
#include <linux/in.h>
#include <linux/tcp.h>
#include <linux/udp.h>

#include <net/ip.h>
#include <net/protocol.h>

#include <linux/if_arp.h>
#include <linux/in6.h>
#include <linux/inetdevice.h>
#include <linux/arcdevice.h>
#include <linux/if_bridge.h>

#include <etherip.h>
#include <vnet.h>
#include <varp.h>
#include <vif.h>
#include <vnet_dev.h>
#include <random.h>

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

#if !defined(CONFIG_BRIDGE) && !defined(CONFIG_BRIDGE_MODULE)
#warning Should configure Ethernet Bridging in kernel Network Options
#endif

#ifndef CONFIG_BRIDGE_NETFILTER
#warning Should configure CONFIG_BRIDGE_NETFILTER in kernel
#endif

static void vnet_dev_destructor(struct net_device *dev){
    Vnet *vnet = dev->priv;
    if(vnet){
        if(vnet->dev == dev){
            vnet->dev = NULL;
        }
        dev->priv = NULL;
        Vnet_decref(vnet);
    }
    free_netdev(dev);
}

static struct net_device_stats *vnet_dev_get_stats(struct net_device *dev){
    static struct net_device_stats stats = {};
    Vnet *vnet = dev->priv;
    return (vnet ? &vnet->stats : &stats);
}

static int vnet_dev_change_mtu(struct net_device *dev, int mtu){
    int err = 0;
    Vnet *vnet = dev->priv;
    if (mtu < 68 || mtu > (vnet ? vnet->mtu : 1500)){
        err = -EINVAL;
        goto exit;
    }
    dev->mtu = mtu;
  exit:
    return err;
}

/** Remove the net device for a vnet.
 * Safe to call if the vnet or its dev are null.
 *
 * @param vnet vnet
 */
void vnet_dev_remove(Vnet *vnet){
    if(vnet && vnet->dev){
        iprintf("> Removing vnet device %s\n", vnet->dev->name);
        unregister_netdev(vnet->dev);
    }
}

static int vnet_dev_open(struct net_device *dev){
    int err = 0;

    netif_start_queue(dev);
    return err;
}

static int vnet_dev_stop(struct net_device *dev){
    int err = 0;

    netif_stop_queue(dev);
    return err;
}

static int vnet_dev_hard_start_xmit(struct sk_buff *skb, struct net_device *dev){
    int err = 0;
    Vnet *vnet = dev->priv;
    int len = 0;

    if(!skb){
        wprintf("> skb NULL!\n");
        return -EINVAL;
    }
    if(!vnet){
        return -ENOTCONN;
    }
    if(vnet->recursion++) {
        extern void print_skb(const char *msg, int count, struct sk_buff *skb);
        char vnetbuf[VNET_ID_BUF];
        
        vnet->stats.collisions++;
	vnet->stats.tx_errors++;
        wprintf("> recursion! vnet=%s\n", VnetId_ntoa(&vnet->vnet, vnetbuf));
        print_skb("RECURSION", 0, skb);
        varp_print(iostdout);
	kfree_skb(skb);
        goto exit;
    }
    if(!skb->mac.raw){
        skb->mac.raw = skb->data;
    }        
    len = skb->len;
    // Must not use skb pointer after vnet_skb_send().
    err = vnet_skb_send(skb, &vnet->vnet);
    if(err < 0){
        vnet->stats.tx_errors++;
    } else {
        vnet->stats.tx_packets++;
        vnet->stats.tx_bytes += len;
    }
  exit:
    vnet->recursion--;
    return 0;
}

void vnet_dev_set_multicast_list(struct net_device *dev){
}

#if 0
static int vnet_dev_do_ioctl(struct net_device *dev, struct ifreq *ifr, int cmd){
    int err = 0;
    
    return err;
}

void vnet_dev_tx_timeout(struct net_device *dev){
    //dev->trans_start = jiffies;
    //netif_wake_queue(dev);
}

static int (*eth_hard_header)(struct sk_buff *skb,
                              struct net_device *dev, unsigned short type,
                              void *daddr, void *saddr, unsigned len) = NULL;

static int vnet_dev_hard_header(struct sk_buff *skb,
                                struct net_device *dev, unsigned short type,
                                void *daddr, void *saddr, unsigned len){
    int err = 0;

    err = eth_hard_header(skb, dev, type, daddr, saddr, len);
    if(err) goto exit;
    skb->mac.raw = skb->data;
  exit:
    return err;
}
#endif

int vnet_device_mac(const char *device, unsigned char *mac){
    int err;
    struct net_device *dev;

    err = vnet_get_device(device, &dev);
    if(err) goto exit;
    memcpy(mac, dev->dev_addr, ETH_ALEN);
    dev_put(dev);
  exit:
    return err;
}

void vnet_dev_mac(unsigned char *mac){
    mac[0] = 0xAA;
    mac[1] = 0xFF;
    get_random_bytes(mac + 2, 4);
}

/** Initial setup of the device for a vnet.
 */
static void vnet_dev_init(struct net_device *dev){
    ether_setup(dev);

#if 0
    if(!eth_hard_header){
        eth_hard_header = dev->hard_header;
    }
    dev->hard_header          = vnet_dev_hard_header;
    //dev->do_ioctl             = vnet_dev_do_ioctl;
    //dev->tx_timeout           = vnet_dev_tx_timeout;
    //dev->watchdog_timeo       = TX_TIMEOUT;
    
#endif

    dev->open                 = vnet_dev_open;
    dev->stop                 = vnet_dev_stop;
    dev->destructor           = vnet_dev_destructor;
    dev->hard_start_xmit      = vnet_dev_hard_start_xmit;
    dev->get_stats            = vnet_dev_get_stats;
    dev->change_mtu           = vnet_dev_change_mtu;
    dev->set_multicast_list   = vnet_dev_set_multicast_list;

    dev->flags |= IFF_DEBUG;
    dev->flags |= IFF_PROMISC;
    dev->flags |= IFF_ALLMULTI;

    vnet_dev_mac(dev->dev_addr);
}

/** Complete the setup of the device for a vnet.
 * Associate the device and the vnet and set mtu etc.
 */
static int vnet_dev_setup(Vnet *vnet, struct net_device *dev){
    int err;

    Vnet_incref(vnet);
    dev->priv = vnet;
    vnet->dev = dev;
    dev->hard_header_len += vnet->header_n;
    if(!etherip_in_udp){
        dev->mtu -= vnet->header_n;
    }
    vnet->mtu = dev->mtu;
    iprintf("> Adding vnet device %s\n", dev->name);
    err = register_netdev(dev);
    if(err){
        eprintf("> register_netdev(%s) = %d\n", dev->name, err);
        vnet_dev_destructor(dev);
    }
    return err;
}

static inline int roundupto(int n, int k){
    return k * ((n + k - 1) / k);
}

/** Add the interface (net device) for a vnet.
 * Sets the dev field of the vnet on success.
 * Does nothing if the vnet already has an interface.
 *
 * @param vnet vnet
 * @return 0 on success, error code otherwise
 */
int vnet_dev_add(Vnet *vnet){
    int err = 0;
    struct net_device *dev = NULL;

    if(vnet->dev) goto exit;
    vnet->header_n = ETH_HLEN + sizeof(struct iphdr) + sizeof(struct etheriphdr);
    if(etherip_in_udp){
        vnet->header_n += sizeof(struct VnetMsgHdr);
        vnet->header_n += sizeof(struct udphdr);
    }
    vnet->header_n = roundupto(vnet->header_n, 4);
    dev = alloc_netdev(0, vnet->device, vnet_dev_init);
    if(!dev){
        err = -ENOMEM;
        goto exit;
    }
    err = vnet_dev_setup(vnet, dev);
    if(err) goto exit;
    rtnl_lock();
    dev_open(dev);
    rtnl_unlock();

  exit:
    return err;
}
