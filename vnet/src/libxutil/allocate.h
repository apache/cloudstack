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

#ifndef _XUTIL_ALLOCATE_H_
#define _XUTIL_ALLOCATE_H_

/** Allocate memory for a given type, and cast. */
#define ALLOCATE(ctype) (ctype *)allocate(sizeof(ctype))

/** Allocate memory for a given type, and cast. */
#define ALLOCATE_TYPE(ctype, type) (ctype *)allocate(sizeof(ctype))

extern void *allocate_type(int size, int type);
extern void *allocate(int size);
extern void deallocate(void *);
extern void memzero(void *p, int size);

typedef void AllocateFailedFn(int size, int type);
extern AllocateFailedFn *allocate_failed_fn;

#endif /* _XUTIL_ALLOCATE_H_ */









