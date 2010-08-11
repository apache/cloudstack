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
#ifndef _VNET_VARP_UTIL_H
#define _VNET_VARP_UTIL_H

#include "hash_table.h"

/** Size of a string buffer to store a varp address. */
#define VARP_ADDR_BUF 56

/** Size of a string buffer to store a vnet id. */
#define VNET_ID_BUF 56

#ifndef NIPQUAD
#define NIPQUAD(addr) \
	((unsigned char *)&addr)[0], \
	((unsigned char *)&addr)[1], \
	((unsigned char *)&addr)[2], \
	((unsigned char *)&addr)[3]
#endif

#ifndef NIP6
#define NIP6(addr) \
	ntohs((addr).s6_addr16[0]), \
	ntohs((addr).s6_addr16[1]), \
	ntohs((addr).s6_addr16[2]), \
	ntohs((addr).s6_addr16[3]), \
	ntohs((addr).s6_addr16[4]), \
	ntohs((addr).s6_addr16[5]), \
	ntohs((addr).s6_addr16[6]), \
	ntohs((addr).s6_addr16[7])
#endif


static inline const char *VarpAddr_ntoa(VarpAddr *addr, char buf[VARP_ADDR_BUF])
{
    switch(addr->family){
    default:
    case AF_INET:
        snprintf(buf, VARP_ADDR_BUF-1, "%u.%u.%u.%u",
                NIPQUAD(addr->u.ip4));
        break;
    case AF_INET6:
        snprintf(buf, VARP_ADDR_BUF-1,"%04x:%04x:%04x:%04x:%04x:%04x:%04x:%04x",
                NIP6(addr->u.ip6));
        break;
    }
    return buf;
}

static inline const char *VnetId_ntoa(VnetId *vnet, char buf[VNET_ID_BUF])
{
    snprintf(buf, VNET_ID_BUF-1, "%04x:%04x:%04x:%04x:%04x:%04x:%04x:%04x",
            ntohs(vnet->u.vnet16[0]), \
            ntohs(vnet->u.vnet16[1]), \
            ntohs(vnet->u.vnet16[2]), \
            ntohs(vnet->u.vnet16[3]), \
            ntohs(vnet->u.vnet16[4]), \
            ntohs(vnet->u.vnet16[5]), \
            ntohs(vnet->u.vnet16[6]), \
            ntohs(vnet->u.vnet16[7]));
    return buf;
}

extern int VnetId_aton(const char *s, VnetId *vnet);

/** Convert an unsigned in host order to a vnet id.
 */
static inline struct VnetId toVnetId(uint32_t vnetid){
    struct VnetId vnet = {};
    vnet.u.vnet32[VNETID_SIZE32 - 1] = htonl(vnetid);
    return vnet;
}

static inline int VnetId_eq(VnetId *id1, VnetId *id2){
    return memcmp(id1, id2, sizeof(VnetId)) == 0;
}

#endif /* _VNET_VARP_UTIL_H */
