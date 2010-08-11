/*
 * Copyright (C) 2005 Mike Wray <mike.wray@hp.com>
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

#ifndef _VNET_TIMER_UTIL_H_
#define _VNET_TIMER_UTIL_H_

#ifdef __KERNEL__

struct timer_list;
#define timer_cancel del_timer

#else /* __KERNEL__ */

#include "timer.h"
#define timer_list   Timer
#define HZ           1000
#define jiffies      (unsigned long)(time_now() * HZ)
#define timer_cancel Timer_cancel

#endif /* __KERNEL__ */

void timer_init(struct timer_list *timer, void (*fn)(unsigned long), void *data);
void timer_set(struct timer_list *timer, unsigned long ttl);

#endif /*! _VNET_TIMER_UTIL_H_ */
