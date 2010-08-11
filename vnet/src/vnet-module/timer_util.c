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

#ifdef __KERNEL__
#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/string.h>
#include <linux/version.h>
#include <linux/jiffies.h>
#include <linux/posix-timers.h>

#include <linux/spinlock.h>
#include <asm/semaphore.h>

#else

#include "sys_kernel.h"
#include "spinlock.h"

#endif

#include "timer_util.h"

#define MODULE_NAME "TIMER"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

#ifdef __KERNEL__

void timer_init(struct timer_list *timer, void (*fn)(unsigned long), void *data){
    init_timer(timer);
    timer->data = (unsigned long)data;
    timer->function = fn;
}

void timer_set(struct timer_list *timer, unsigned long ttl){
    unsigned long now = jiffies;
    timer->expires = now + ttl;
    add_timer(timer);
}

#else

void timer_init(struct Timer *timer, void (*fn)(unsigned long), void *data){
    *timer = (struct Timer){};
    timer->data = (unsigned long)data;
    timer->fn = fn;
}

void timer_set(struct Timer *timer, unsigned long ttl){
    double now = time_now();
    timer->expiry = now + (double)ttl/(double)HZ;
    Timer_cancel(timer);
    Timer_add(timer);
}

#endif
