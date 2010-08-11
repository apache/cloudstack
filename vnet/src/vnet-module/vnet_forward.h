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
#ifndef _VNET_FORWARD_H_
#define _VNET_FORWARD_H_

#include <if_varp.h>

struct sk_buff;
struct IOStream;

extern int vnet_peer_add(struct VarpAddr *addr, uint16_t port);
extern int vnet_peer_del(struct VarpAddr *addr);
extern void vnet_peer_print(struct IOStream *io);

extern int vnet_forward_send(struct sk_buff *skb);
extern int vnet_forward_recv(struct sk_buff *skb);
extern int vnet_forward_init(void);
extern void vnet_forward_exit(void);

#endif /* _VNET_FORWARD_H_ */
