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

#ifndef _XUTIL_KERNEL_STREAM_H_
#define _XUTIL_KERNEL_STREAM_H_

#ifdef __KERNEL__
#include "iostream.h"

extern IOStream get_stream_kernel(void);
#define get_stream_stdout get_stream_kernel

#endif /* __KERNEL__ */
#endif /* !_XUTIL_KERNEL_STREAM_H_ */
