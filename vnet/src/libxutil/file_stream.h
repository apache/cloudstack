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

#ifndef _XUTIL_FILE_STREAM_H_
#define _XUTIL_FILE_STREAM_H_

#ifndef __KERNEL__
#include "iostream.h"
#include <stdio.h>

extern IOStream *file_stream_new(FILE *f);
extern IOStream *file_stream_fopen(const char *file, const char *flags);
extern IOStream *file_stream_fdopen(int fd, const char *flags);
extern IOStream get_stream_stdout(void);
extern IOStream get_stream_stderr(void);
extern IOStream get_stream_stdin(void);

extern int file_stream_setvbuf(IOStream *io, char *buf, int mode, size_t size);
#endif
#endif /* !_XUTIL_FILE_STREAM_H_ */
