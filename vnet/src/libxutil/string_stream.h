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

#ifndef _XUTIL_STRING_STREAM_H_
#define _XUTIL_STRING_STREAM_H_

#include "iostream.h"

/** Internal state for a string stream.
 * Exposed here so that string streams can be statically created, using
 * string_stream_init().
 */
typedef struct {
    /** The string used for input and ouput. */
    char *string;
    /** Output pointer. */
    char *out;
    /** Input pointer. */
    char *in;
    /** Length of string. */
    int size;
    /** End marker. */
    char *end;
} StringData;

extern IOMethods *string_stream_get_methods(void);
extern IOStream *string_stream_new(char *s, int n);
extern void string_stream_init(IOStream *stream, StringData *data, char *s, int n);

#endif /* !_XUTIL_STRING_STREAM_H_ */
