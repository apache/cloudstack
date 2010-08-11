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
#ifndef _VNET_ETHERIP_H_
#define _VNET_ETHERIP_H_

#include "if_etherip.h"

#ifdef __KERNEL__
extern int etherip_module_init(void);
extern void etherip_module_exit(void);
#endif

extern int etherip_protocol_recv(struct sk_buff *skb);
extern int etherip_in_udp;

struct VnetId;
struct VarpAddr;
struct Tunnel;

extern int etherip_tunnel_create(struct VnetId *vnet, struct VarpAddr *addr,
                                 struct Tunnel *base, struct Tunnel **tunnel);
#endif
