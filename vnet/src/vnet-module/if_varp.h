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

#ifndef _VNET_IF_VARP_H
#define _VNET_IF_VARP_H

/* Need struct in_addr, struct in6_addr. */
#ifdef __KERNEL__

#include <linux/in.h>
#include <linux/in6.h>

#else

#include <sys/socket.h>
#include <netinet/in.h>

#endif

#include <linux/if_ether.h>

typedef struct Vmac {
    unsigned char mac[ETH_ALEN];
} Vmac;

enum {
    /* Varp protocol messages.
     * Format is defined by struct VarpHdr.
     */
    VARP_ID          = 1,

    /* Vnet ethernet in udp messages.
     * Format is uint16_t id (VUDP_ID), then
     * struct etheriphdr.
     */
    VUDP_ID          = 2,

    /* Forwarded messages.
     */
    VFWD_ID          = 3,

    /* Varp request. */
    VARP_OP_REQUEST  = 1,
    /* Varp announce. */
    VARP_OP_ANNOUNCE = 2,
};

#define VNETID_SIZE8  16
#define VNETID_SIZE16 (VNETID_SIZE8 >> 1)
#define VNETID_SIZE32 (VNETID_SIZE8 >> 2)

typedef struct VnetId {
    union {
        uint8_t  vnet8[VNETID_SIZE8];
        uint16_t vnet16[VNETID_SIZE16];
        uint32_t vnet32[VNETID_SIZE32];
    } u;
} __attribute__((packed)) VnetId;

typedef struct VarpAddr {
    uint8_t family; // AF_INET or AF_INET6.
    union {
        uint8_t raw[16];
        struct in_addr ip4;
        struct in6_addr ip6;
    } u;
    //uint16_t port;
} __attribute__((packed)) VarpAddr;

typedef struct VnetMsgHdr {
    uint16_t id;
    uint16_t opcode;
} __attribute__((packed)) VnetMsgHdr;

typedef struct VarpHdr {
  VnetMsgHdr hdr;
  VnetId vnet;
  Vmac vmac;
  VarpAddr addr;
} __attribute__((packed)) VarpHdr;


/** Default address for varp/vnet broadcasts: 224.10.0.1 */
#define VARP_MCAST_ADDR     0xe00a0001

/** UDP port to use for varp protocol. */
#define VARP_PORT           1798

#endif /* ! _VNET_IF_VARP_H */
