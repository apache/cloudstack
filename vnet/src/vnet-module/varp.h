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

#ifndef _VNET_VARP_H
#define _VNET_VARP_H

#ifdef __KERNEL__

#else

#include "sys_kernel.h"

#endif

#include "hash_table.h"
#include "if_varp.h"
#include "varp_util.h"

#define CONFIG_VARP_GRATUITOUS 1

struct net_device;
struct sk_buff;
struct Vif;

enum {
    VARP_UPDATE_CREATE = 1,
    VARP_UPDATE_QUEUE  = 2,
};

extern int vnet_get_device(const char *name, struct net_device **dev);
extern int vnet_get_device_address(struct net_device *dev, u32 *addr);

extern int varp_remove_vnet(struct VnetId *vnet);
extern int varp_handle_message(struct sk_buff *skb);
extern int varp_output(struct sk_buff *skb, struct VnetId *vnet);
extern int varp_update(struct VnetId *vnet, unsigned char *vmac,
                       struct VarpAddr *addr);

extern int varp_init(void);
extern void varp_exit(void);

extern int varp_open(u32 mcaddr, u16 port);
extern void varp_close(void);
extern int varp_set_mcast_addr(u32 addr);

extern void varp_print(struct IOStream *io);
extern void varp_flush(void);

extern int varp_announce_vif(struct net_device *dev, struct Vif *vif);

extern u32 varp_mcast_addr;
extern u16 varp_port;

/* MAC broadcast addr is ff-ff-ff-ff-ff-ff (all 1's).
 * MAC multicast addr has low bit 1, i.e. 01-00-00-00-00-00.
 */

/** Test if a MAC address is a multicast or broadcast address.
 *
 * @param mac address
 * @return 1 if it is, 0 if not
 */
static inline int mac_is_multicast(u8 mac[ETH_ALEN]){
    return mac[0] & 1;
}

/** Test if a MAC address is the broadcast address.
 *
 * @param mac address
 * @return 1 if it is, 0 if not
 */
static inline int mac_is_broadcast(u8 mac[ETH_ALEN]){
    u8 mac_bcast_val[ETH_ALEN] = { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff };
    return memcmp(mac, mac_bcast_val, ETH_ALEN) == 0;
}

/** Test if a MAC address is the all-zero address.
 *
 * @param mac address
 * @return 1 if it is, 0 if not
 */
static inline int mac_is_zero(u8 mac[ETH_ALEN]){
    u8 mac_zero_val[ETH_ALEN] = {};
    return memcmp(mac, mac_zero_val, ETH_ALEN) == 0;
}

/** Print format for a mac address. */
#define MACFMT "%02x:%02x:%02x:%02x:%02x:%02x"

#define MAC6TUPLE(_mac) (_mac)[0], (_mac)[1], (_mac)[2], (_mac)[3], (_mac)[4], (_mac)[5]

/** Get the subnet defined by a netmask and addr.
 *
 * @param netmask subnet netmask
 * @param addr    subnet address
 * @return subnet
 */
static inline u32 subnet_net(u32 netmask, u32 addr){
    return netmask & addr;
}

/** Get the address within a subnet.
 *
 * @param netmask subnet netmask
 * @param addr    address
 * @return address within the subnet
 */
static inline u32 subnet_addr(u32 netmask, u32 addr){
    return ~netmask & addr;
}

/** Get the broadcast address for a subnet.
 *
 * @param netmask subnet netmask
 * @param netaddr subnet address
 * @return subnet broadcast address
 */
static inline u32 subnet_broadcast_addr(u32 netmask, u32 netaddr){
    return subnet_net(netmask, netaddr) | ~netmask;
}

/** Test if an address corresponds to a subnet broadcast.
 * True if the address within the subnet is all 1's (in binary).
 * (even if the address is not in the subnet).
 *
 * @param netmask subnet mask
 * @param add     address
 * @return 1 if it does, 0 otherwise
 */
static inline int subnet_broadcast(u32 netmask, u32 addr){
    return subnet_addr(netmask, INADDR_ANY) == subnet_addr(netmask, addr);
}

/** Test if an address is in a subnet.
 *
 * @param netmask subnet mask
 * @param netaddr subnet address
 * @param addr    address
 * @return 1 if it is, 0 otherwise
 */
static inline int subnet_local(u32 netmask, u32 netaddr, u32 addr){
    return subnet_net(netmask, netaddr) == subnet_net(netmask, addr);
}

#endif /* ! _VNET_VARP_H */
