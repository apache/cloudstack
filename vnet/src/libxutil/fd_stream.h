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

#ifndef _XMC_FD_STREAM_H_
#define _XMC_FD_STREAM_H_

#ifndef __KERNEL__
#include "iostream.h"

/** Data associated with a fd stream. */
typedef struct FDData {
    /** The socket file descriptor. */
    int fd;
} FDData;

extern IOStream *fd_stream_new(int fd);
extern int fd_stream_data(IOStream *io, FDData **data);
extern int fd_stream_check(IOStream *io);

#endif
#endif /* !_XMC_FD_STREAM_H_ */
