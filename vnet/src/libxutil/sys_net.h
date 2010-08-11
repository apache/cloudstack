/*
 * Copyright (C) 2001 - 2004 Mike Wray <mike.wray@hp.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifndef _XUTIL_SYS_NET_H_
#define _XUTIL_SYS_NET_H_
/** @file
 *
 * Replacement for standard network includes.
 * Works in user or kernel code.
 */

extern int get_inet_addr(const char *s, unsigned long *address);
extern unsigned long bits_to_mask(int n);
extern int mask_to_bits(unsigned long mask);
extern int get_host_address(const char *name, unsigned long *address);
extern int get_service_port(const char *name, unsigned long *port);
extern char *get_port_service(unsigned long port);
extern int convert_service_to_port(const char *s, unsigned long *port);

#ifdef __KERNEL__
#include <linux/kernel.h>
#include <linux/types.h>
#include <linux/errno.h>
#include <linux/slab.h>
#include <asm/byteorder.h> 

#ifndef htonl
#define htonl(x) __constant_htonl(x)
#endif

#ifndef ntohl
#define ntohl(x) __constant_ntohl(x)
#endif

#ifndef htons
#define htons(x) __constant_htons(x)
#endif

#ifndef ntohs
#define ntohs(x) __constant_ntohs(x)
#endif

#include <linux/in.h>
extern char *inet_ntoa(struct in_addr inaddr);
extern int inet_aton(const char *address, struct in_addr *inp);

#else

#include <limits.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>

#endif

extern char *mac_ntoa(const unsigned char *macaddr);
extern int mac_aton(const char *addr, unsigned char *macaddr);

#endif /* !_XUTIL_SYS_NET_H_ */



