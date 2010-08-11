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
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/time.h>
#include <time.h>

#include "allocate.h"
#include "timer.h"

#define MODULE_NAME "TIMER"
#undef DEBUG
#define DEBUG 1
#include "debug.h"

static Timer *timers = NULL;

/** Get the time now as a double (in seconds).
 * Returns zero if could not get the time.
 *
 * @return time now
 */
double time_now(void){
    struct timeval time;
    if(gettimeofday(&time, NULL)) return 0.0;
    return (double)time.tv_sec + (1.0e-6 * (double)time.tv_usec);
}

/** Set the process real-time timer to go off at a given expiry time.
 * The timer will not be set to go off in less than 10 ms
 * (even if the expiry time is sooner, or in the past).
 *
 * @param expiry time (in seconds)
 * @return 0 on success, error code otherwise
 */
static int itimer_set(double expiry){
    struct itimerval val = {};
    struct itimerval old = {};
    double now, delay;
    int err = 0;

    if(expiry == 0.0){
        val.it_value.tv_sec = 0;
        val.it_value.tv_usec = 0;
    } else {
        now = time_now();
        delay = expiry - now;
        if(delay < 0.01) delay = 0.01;
        val.it_value.tv_sec = (long)delay;
        val.it_value.tv_usec = (long)((delay - (double)(long)delay) * 1.0e6);
    }
    err = setitimer(ITIMER_REAL, &val, &old);
    return err;
}

void Timer_free(Timer *z){
#ifndef USE_GC
    if(!z) return;
    deallocate(z);
#endif
}

/** Process any expired timers.
 * Calls the functions of expired timers and removes them
 * from the timer list.
 * Reschedules the interval timer for the earliest expiring timer
 * (if any).
 *
 * Should not be called from within the SIGALRM handler - set
 * a flag there and call it later.
 *
 * @return 0 on success, error code otherwise.
 */
int process_timers(void){
    double now = time_now();
    Timer *curr, *next;
    for(curr = timers; curr; curr = next){
        next = curr->next;
        if(curr->expiry > now) break;
        if(curr->fn) curr->fn(curr->data);
    }
    timers = curr;
    itimer_set((curr ? curr->expiry : 0));
    return 0;
}

void Timer_add(Timer *timer){
    // Insert timer in list ordered by (increasing) expiry time.
    Timer *prev, *curr, *next;
    prev = NULL;
    for(curr = timers; curr; prev = curr, curr = next){
        next = curr->next;
        if(timer->expiry < curr->expiry) break;
    }
    if(prev){
        prev->next = timer;
    } else {
        timers = timer;
    }
    timer->next = curr;

    // Set interval timer to go off for earliest expiry time.
    itimer_set(timer->expiry);
}

Timer * Timer_set(double delay, TimerFn *fn, unsigned long data){
    // Get 'now'.
    double now = time_now();
    Timer *timer = NULL;
    timer = ALLOCATE(Timer);
    if(!timer) goto exit;
    // Add delay to now to get expiry time.
    timer->expiry = now + delay;
    timer->fn = fn;
    timer->data = data;

    Timer_add(timer);
  exit:
    return timer;
}

int Timer_cancel(Timer *timer){
    // Remove timer from list.
    int err = -ENOENT;
    Timer *prev, *curr, *next;
    for(prev = NULL, curr = timers; curr; prev = curr, curr = next){
        next = curr->next;
        if(curr == timer){
            err = 0;
            if(prev){
                prev->next = curr->next;
            } else {
                timers = curr->next;
            }
            curr->next = NULL;
            break;
        }
    }
    return err;
}

