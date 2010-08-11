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

/** @file
 * An IOStream implementation using zlib gzFile to provide
 * compression and decompression.
 */
#ifndef __KERNEL__

#include <stdio.h>
#include <stdlib.h>

#include "zlib.h"

#include "allocate.h"
#include "gzip_stream.h"

static int gzip_read(IOStream *s, void *buf, size_t n);
static int gzip_write(IOStream *s, const void *buf, size_t n);
static int gzip_error(IOStream *s);
static int gzip_close(IOStream *s);
static void gzip_free(IOStream *s);
static int gzip_flush(IOStream *s);

/** Methods used by a gzFile* IOStream. */
static const IOMethods gzip_methods = {
    read:  gzip_read,
    write: gzip_write,
    error: gzip_error,
    close: gzip_close,
    free:  gzip_free,
    flush: gzip_flush,
};

/** Get the underlying gzFile*.
 * 
 * @param s gzip stream
 * @return the stream s wraps
 */
static inline gzFile get_gzfile(IOStream *s){
    return (gzFile)s->data;
}

/** Write to the underlying stream.
 *
 * @param stream destination
 * @param buf data
 * @param n number of bytes to write
 * @return number of bytes written
 */
static int gzip_write(IOStream *s, const void *buf, size_t n){
    return gzwrite(get_gzfile(s), (void*)buf, n);
}

/** Read from the underlying stream.
 *
 * @param stream input
 * @param buf where to put input
 * @param n number of bytes to read
 * @return number of bytes read
 */
static int gzip_read(IOStream *s, void *buf, size_t n){
    return gzread(get_gzfile(s), buf, n);
}

/** Flush the underlying stream.
 *
 * @param s gzip stream
 * @return 0 on success, error code otherwise
 */
static int gzip_flush(IOStream *s){
    //return gzflush(get_gzfile(s), Z_NO_FLUSH);
    return gzflush(get_gzfile(s), Z_SYNC_FLUSH);
    //return gzflush(get_gzfile(s), Z_FULL_FLUSH);
}

/** Check if a stream has an error.
 *
 * @param s gzip stream
 * @return 1 if has an error, 0 otherwise
 */
static int gzip_error(IOStream *s){
    int err;
    gzFile *gz = get_gzfile(s);
    gzerror(gz, &err);
    return (err == Z_ERRNO ? 1 /* ferror(gzfile(gz)) */ : err);
}

/** Close a gzip stream.
 *
 * @param s gzip stream to close
 * @return result of the close
 */
static int gzip_close(IOStream *s){
    int result = 0;
    result = gzclose(get_gzfile(s));
    return result;
}

/** Free a gzip stream.
 *
 * @param s gzip stream
 */
static void gzip_free(IOStream *s){
    // Nothing to do - close did it all.
}

/** Create an IOStream for a gzip stream.
 *
 * @param f stream to wrap
 * @return new IOStream using f for i/o
 */
IOStream *gzip_stream_new(gzFile *f){
    IOStream *io = ALLOCATE(IOStream);
    if(io){
        io->methods = &gzip_methods;
        io->data = (void*)f;
    }
    return io;
}

/** IOStream version of fopen().
 *
 * @param file name of the file to open
 * @param flags giving the mode to open in (as for fopen())
 * @return new stream for the open file, or NULL if failed
 */
IOStream *gzip_stream_fopen(const char *file, const char *flags){
    IOStream *io = NULL;
    gzFile *fgz;
    fgz = gzopen(file, flags);
    if(fgz){
        io = gzip_stream_new(fgz);
        if(!io){
            gzclose(fgz);
        }
    }
    return io;
}

/** IOStream version of fdopen().
 *
 * @param fd file descriptor
 * @param flags giving the mode to open in (as for fdopen())
 * @return new stream for the open file, or NULL if failed.  Always takes
 *         ownership of fd.
 */
IOStream *gzip_stream_fdopen(int fd, const char *flags){
    IOStream *io = NULL;
    gzFile *fgz;
    fgz = gzdopen(fd, flags);
    if(fgz){
        io = gzip_stream_new(fgz);
        if(!io)
            gzclose(fgz);
    }
    return io;
}
#endif
