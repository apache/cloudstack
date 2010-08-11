/*
 * Copyright (C) 2003 Hewlett-Packard Company.
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

#ifndef _XUTIL_GZIP_STREAM_H_
#define _XUTIL_GZIP_STREAM_H_

#ifndef __KERNEL__
#include "iostream.h"
#include "zlib.h"

extern IOStream *gzip_stream_new(gzFile *f);
extern IOStream *gzip_stream_fopen(const char *file, const char *flags);
extern IOStream *gzip_stream_fdopen(int fd, const char *flags);
#endif
#endif /* !_XUTIL_GZIP_STREAM_H_ */
