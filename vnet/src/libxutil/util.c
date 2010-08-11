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

#include "sys_net.h"
#include "sys_string.h"

#ifndef __KERNEL__
#  include <grp.h>   
#  include <pwd.h>  
#endif

#include "util.h"


/** @file Various utility functions.
 */

/** Print an address (in network order) as an IPv4 address string
 * in dot notation.
 *
 * @param io where to print address
 * @param address to print (in network order)
 * @return bytes printed
 */
int print_address(IOStream *io, unsigned long address){
#ifdef __KERNEL__
    address = ntohl(address);
    return IOStream_print(io, "%u.%u.%u.%u", 
                          (unsigned)((address >> 24) & 0xff),
                          (unsigned)((address >> 16) & 0xff),
                          (unsigned)((address >>  8) & 0xff),
                          (unsigned)((address      ) & 0xff));
#else
    struct in_addr inaddr = { s_addr: address };
    return IOStream_print(io, inet_ntoa(inaddr));
#endif
}

/** Get the protocol number for a protocol.
 *
 * @param name protocol name
 * @param protocol where to put the protocol number
 * @return 0 if OK, error otherwise
 */  
int get_protocol_number(char *name, unsigned long *protocol){
#ifdef __KERNEL__
    return -1;
#else
    struct protoent *proto = getprotobyname(name);
    if(!proto){
	return -1;
    }
    *protocol = proto->p_proto;
    return 0;
#endif
}

/** Get the protocol name for a protocol number.
 *
 * @param protocol number
 * @return name or null
 */
char *get_protocol_name(unsigned long protocol){
#ifdef __KERNEL__
    return 0;
#else
    struct protoent *proto = getprotobynumber(protocol);
    if(!proto){
	return 0;
    }
    return proto->p_name;
#endif
}

/** Get the host name for an address.
 *
 * @param addr address
 * @return host name or null
 */
char *get_host_name(unsigned long addr){
#ifdef __KERNEL__
    return 0;
#else
    struct in_addr inaddr;
    struct hostent *host = 0;

    inaddr.s_addr = addr;
    host = gethostbyaddr((char*)&inaddr, sizeof(inaddr), AF_INET);
    if(!host) return NULL;
    return host->h_name;
#endif
}
