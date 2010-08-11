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

#ifndef _XUTIL_SYS_STRING_H_
#define _XUTIL_SYS_STRING_H_
/** @file
 * Replacement for standard string includes.
 * Works in user or kernel code.
 */
/*============================================================================*/
#define _GNU_SOURCE
#ifdef __KERNEL__

#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/string.h>
#include <linux/types.h>
#include <stdarg.h>
#include "allocate.h"

extern char *strerror(int err);

#if 0
static inline int tolower(int c){
    return (c>='A' && c<='Z' ? (c-'A')+'a' : c);
}
#endif

static inline int isalpha(int c){
    return (c>='A' && c<='Z') || (c>='a' && c<='z');
}

static inline int isdigit(int c){
   return (c>='0' && c<='9');
}

#if 0
static inline int strcasecmp(const char *s1, const char *s2){
	int c1, c2;

	do {
		c1 = tolower(*s1++);
		c2 = tolower(*s2++);
	} while (c1 && c1 == c2);
	return c1 - c2;
}
#endif

static inline char * strdup(const char *s){
    int n = (s ? 1+strlen(s) : 0);
    char *copy = (n ? allocate(n) : NULL);
    if(copy){
        strcpy(copy, s);
    }
    return copy;
}

/*============================================================================*/
#else
#include <string.h>
#include <stdio.h>

#ifndef _GNU_SOURCE
static inline size_t strnlen(const char *s, size_t n){
    int k = 0;
    if(s){
	for(k=0; *s && k<n; s++, k++){}
    }
    return k;
}
#endif

#endif
/*============================================================================*/

extern int convert_atoul(const char *s, unsigned long *v);
extern int convert_atol(const char *s, long *v);
extern int path_concat(char *s, char *t, char **val);

#endif /* !_XUTIL_SYS_STRING_H_ */
