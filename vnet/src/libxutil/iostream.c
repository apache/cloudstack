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

#include "iostream.h"
#include "sys_string.h"

/** Print on a stream, like vfprintf().
 *
 * @param stream to print to
 * @param format for the print (as fprintf())
 * @param args arguments to print
 * @return result code from the print
 */
int IOStream_vprint(IOStream *stream, const char *format, va_list args){
  char buffer[1024];
  int k = sizeof(buffer), n;

  n = vsnprintf(buffer, k, (char*)format, args);
  if(n < 0 || n > k ){
      n = k;
  }
  n = IOStream_write(stream, buffer, n);
  return n;
}

/** Print on a stream, like fprintf().
 *
 * @param stream to print to
 * @param format for the print (as fprintf())
 * @return result code from the print
 */
int IOStream_print(IOStream *stream, const char *format, ...){
  va_list args;
  int result = -1;

  va_start(args, format);
  result = IOStream_vprint(stream, format, args);
  va_end(args);
  return result;
}
