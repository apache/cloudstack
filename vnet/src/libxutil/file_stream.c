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

/** @file
 * An IOStream implementation using FILE*.
 */
#ifndef __KERNEL__
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include "allocate.h"
#include "file_stream.h"

static int file_read(IOStream *s, void *buf, size_t n);
static int file_write(IOStream *s, const void *buf, size_t n);
static int file_error(IOStream *s);
static int file_close(IOStream *s);
static void file_free(IOStream *s);
static int file_flush(IOStream *s);

/** Methods used by a FILE* IOStream. */
static const IOMethods file_methods = {
    read:  file_read,
    write: file_write,
    error: file_error,
    close: file_close,
    free:  file_free,
    flush: file_flush,
};

/** IOStream for stdin. */
static IOStream _iostdin = {
    methods: &file_methods,
    data: (void*)1,
    nofree: 1,
};

/** IOStream for stdout. */
static IOStream _iostdout = {
    methods: &file_methods,
    data: (void*)2,
    nofree: 1,
};

/** IOStream for stderr. */
static IOStream _iostderr = {
    methods: &file_methods,
    data: (void*)3,
    nofree: 1,
};

/** IOStream for stdin. */
IOStream *iostdin = &_iostdin;

/** IOStream for stdout. */
IOStream *iostdout = &_iostdout;

/** IOStream for stderr. */
IOStream *iostderr = &_iostderr;

/* Get the underlying FILE*.
 *
 * @param s file stream
 * @return the stream s wraps
 */
static inline FILE *get_file(IOStream *s){
     FILE *data = NULL;
     switch((long)s->data){
     case 1:
         data = stdin;
         break;
     case 2:
         data = stdout;
         break;
     case 3:
         data = stderr;
         break;
     default:
         data = (FILE*)s->data;
         break;
     }
     return data;
}

/** Control buffering on the underlying stream, like setvbuf().
 *
 * @param io file stream
 * @param buf buffer
 * @param mode buffering mode (see man setvbuf())
 * @param size buffer size
 * @return 0 on success, non-zero otherwise
 */
int file_stream_setvbuf(IOStream *io, char *buf, int mode, size_t size){
    return setvbuf(get_file(io), buf, mode, size);
}

/** Write to the underlying stream using fwrite();
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to write
 * @return number of bytes written
 */
static int file_write(IOStream *s, const void *buf, size_t n){
    int cnt = 0;
    unsigned char *ptr = (unsigned char*)buf;
    while (n) {
	if ((cnt = fwrite((const void *)(ptr), sizeof(unsigned char), n, get_file(s))) < 0) {
		if (errno == EINTR)
			continue;
		else
			return -1;
	}
	n -= cnt;	
	ptr += cnt;
    }

    return n;
}

/** Read from the underlying stream using fread();
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to read
 * @return number of bytes read
 */
static int file_read(IOStream *s, void *buf, size_t n){
    return fread(buf, 1, n, get_file(s));
}

/** Fush the underlying stream using fflush().
 *
 * @param s file stream
 * @return 0 on success, error code otherwise
 */
static int file_flush(IOStream *s){
    return fflush(get_file(s));
}

/** Check if a stream has an error.
 *
 * @param s file stream
 * @return 1 if has an error, 0 otherwise
 */
static int file_error(IOStream *s){
    return ferror(get_file(s));
}

/** Close a file stream.
 *
 * @param s file stream to close
 * @return result of the close
 */
static int file_close(IOStream *s){
    int result = 0;
    result = fclose(get_file(s));
    return result;
}

/** Free a file stream.
 *
 * @param s file stream
 */
static void file_free(IOStream *s){
    // Nothing extra to do - close did it all.
}

/** Create an IOStream for a stream.
 *
 * @param f stream to wrap
 * @return new IOStream using f for i/o
 */
IOStream *file_stream_new(FILE *f){
    IOStream *io = ALLOCATE(IOStream);
    if(io){
        io->methods = &file_methods;
        io->data = (void*)f;
    }
    return io;
}

/** IOStream version of fopen().
 *
 * @param file name of the file to open
 * @param flags giving the mode to open in (as for fopen())
 * @return new stream for the open file, or 0 if failed
 */
IOStream *file_stream_fopen(const char *file, const char *flags){
    IOStream *io = 0;
    FILE *fin = fopen(file, flags);
    if(fin){
        io = file_stream_new(fin);
        if(!io){
            fclose(fin);
        }
    }
    return io;
}

/** IOStream version of fdopen().
 *
 * @param fd file descriptor
 * @param flags giving the mode to open in (as for fdopen())
 * @return new stream for the open file, or 0 if failed.  Always takes
 *         ownership of fd.
 */
IOStream *file_stream_fdopen(int fd, const char *flags){
    IOStream *io = 0;
    FILE *fin = fdopen(fd, flags);
    if(fin){
        io = file_stream_new(fin);
        if(!io){
            fclose(fin);
        }
    }
    return io;
}
#endif
