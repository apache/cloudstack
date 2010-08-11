/*
 * Copyright (C) 2002 - 2004 Mike Wray <mike.wray@hp.com>.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or  (at your option) any later version. This library is 
 * distributed in the  hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

#ifndef _XEN_LIB_UTIL_H_
#define _XEN_LIB_UTIL_H_

#include "iostream.h"

extern int print_address(IOStream *io, unsigned long address);
extern int get_protocol_number(char *name, unsigned long *protocol);
extern char *get_protocol_name(unsigned long protocol);
extern char *get_host_name(unsigned long addr);

#endif /* ! _XEN_LIB_UTIL_H_ */
