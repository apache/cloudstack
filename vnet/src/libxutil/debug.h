/*
 * Copyright (C) 2004 Mike Wray <mike.wray@hp.com>
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
#ifndef _XUTIL_DEBUG_H_
#define _XUTIL_DEBUG_H_

#ifndef MODULE_NAME
#define MODULE_NAME ""
#endif

#ifdef __KERNEL__
#include <linux/config.h>
#include <linux/kernel.h>

#ifdef DEBUG

#define dprintf(fmt, args...) printk(KERN_DEBUG   "[DBG] " MODULE_NAME ">%s" fmt, __FUNCTION__, ##args)
#define wprintf(fmt, args...) printk(KERN_WARNING "[WRN] " MODULE_NAME ">%s" fmt, __FUNCTION__, ##args)
#define iprintf(fmt, args...) printk(KERN_INFO    "[INF] " MODULE_NAME ">%s" fmt, __FUNCTION__, ##args)
#define eprintf(fmt, args...) printk(KERN_ERR     "[ERR] " MODULE_NAME ">%s" fmt, __FUNCTION__, ##args)

#else

#define dprintf(fmt, args...) do {} while(0)
#define wprintf(fmt, args...) printk(KERN_WARNING "[WRN] " MODULE_NAME fmt, ##args)
#define iprintf(fmt, args...) printk(KERN_INFO    "[INF] " MODULE_NAME fmt, ##args)
#define eprintf(fmt, args...) printk(KERN_ERR     "[ERR] " MODULE_NAME fmt, ##args)

#endif

#else

#include <stdio.h>

#ifdef DEBUG

#define dprintf(fmt, args...) fprintf(stdout, "%d [DBG] " MODULE_NAME ">%s" fmt, getpid(), __FUNCTION__, ##args)
#define wprintf(fmt, args...) fprintf(stderr, "%d [WRN] " MODULE_NAME ">%s" fmt, getpid(), __FUNCTION__, ##args)
#define iprintf(fmt, args...) fprintf(stderr, "%d [INF] " MODULE_NAME ">%s" fmt, getpid(), __FUNCTION__, ##args)
#define eprintf(fmt, args...) fprintf(stderr, "%d [ERR] " MODULE_NAME ">%s" fmt, getpid(), __FUNCTION__, ##args)

#else

#define dprintf(fmt, args...) do {} while(0)
#define wprintf(fmt, args...) fprintf(stderr, "%d [WRN] " MODULE_NAME fmt, getpid(), ##args)
#define iprintf(fmt, args...) fprintf(stderr, "%d [INF] " MODULE_NAME fmt, getpid(), ##args)
#define eprintf(fmt, args...) fprintf(stderr, "%d [ERR] " MODULE_NAME fmt, getpid(), ##args)

#endif

#endif

/** Print format for an IP address.
 * See NIPQUAD(), HIPQUAD()
 */
#define IPFMT "%u.%u.%u.%u"

#endif /* ! _XUTIL_DEBUG_H_ */
