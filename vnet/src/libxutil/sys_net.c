/*
 * Copyright (C) 2001 - 2004 Mike Wray <mike.wray@hp.com>
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

#ifdef __KERNEL__
#  include <linux/errno.h>
#else
#  include <errno.h>
#endif

/** @file
 * All network data are kept in network order and only converted to
 * host order for display. Network data includes IP addresses, port numbers and
 * network masks.
 */

/** Maximum value for a port. */
#define PORT_MAX 0xffff

/** Convert a number of bits to a network mask
 * for IP addresses. The number of bits must
 * be in the range 1-31.
 *
 * @param n number of bits to set in the mask
 * @return value with n high bits set (in network order)
 */
unsigned long bits_to_mask(int n){
    unsigned long mask = (n ? (1 << 31) : 0);
    int i;
    for(i=1; i<n; i++){
        mask |= (mask >> 1);
    }
    return htonl(mask);
}

/** Convert a network mask to a number of bits.
 *
 * @param mask network mask in network order
 * @return number of bits in mask
 */
int mask_to_bits(unsigned long mask){
    // Start with n set to the number of bits in the mask. Then reduce n by
    // the number of low zero bits in the mask.
    int n = 32;
    for(mask = ntohl(mask);
        (mask & 1)==0 && n>0;
        mask >>= 1){
        n--;
    }
    return n;
}

/** Get the index of the first occurrence of a character in a string.
 * Stops at end of string or after n characters.
 *
 * @param s input string
 * @param n maximum number of charactes to search
 * @param c character to look for
 * @return index of first occurrence, -1 if not found
 */
inline static int indexof(const char *s, int n, char c){
    int i;
    for(i=0; i<n && *s; i++, s++){
        if(*s == c) return i;
    }
    return -1;
}

/** Convert an IPv4 address in dot notation into an unsigned long (in network order).
 *
 * @param s input string
 * @param address where to put the address
 * @return 0 on success, negative on error
 */
int get_inet_addr(const char *s, unsigned long *address){
    // Number of bits in a byte.
    const int BYTE_BITS = 8;
    // Number of bytes in a word.
    const int WORD_BYTES = 4;
    // Max value for a component of an address.
    const int ADDR_MAX  = 255;
    // Separator for components of an address.
    const char dot = '.';

    int n;
    unsigned long addr = 0;
    unsigned long v;
    int i;
    int err = -EINVAL;
    // Bit shift for the current byte.
    int shift = BYTE_BITS * (WORD_BYTES - 1);
    char buf[64];

    n = strlen(s);
    if(n >= sizeof(buf)){
        goto exit;
    }
    for(i=0; i < WORD_BYTES; i++){
        int idx = indexof(s, n, dot);
        idx = (idx < 0 ? strlen(s) : idx);
        strncpy(buf, s, idx); buf[idx]='\0';
        if(convert_atoul(buf, &v)){
            goto exit;
        }
        if(v < 0 || v > ADDR_MAX){
            goto exit;
        }
        addr |= (v << shift);
        if(idx == n) break;
        shift -= BYTE_BITS;
        s += idx+1;
    }
    err = 0;
  exit:
    addr = htonl(addr);
    *address = (err ? 0 : addr);
    return err;
}

#ifdef __KERNEL__
/** Convert an address in network order to IPv4 dot notation.
 * The return value is a static buffer which is overwritten on each call.
 *
 * @param inaddr address (in network order)
 * @return address in dot notation
 */
char *inet_ntoa(struct in_addr inaddr){
    static char address[16] = {};
    uint32_t addr = ntohl(inaddr.s_addr);
    snprintf(address, sizeof(address), "%d.%d.%d.%d",
            (unsigned)((addr >> 24) & 0xff),
            (unsigned)((addr >> 16) & 0xff),
            (unsigned)((addr >>  8) & 0xff),
            (unsigned)((addr      ) & 0xff));
    return address;
}


/** Convert a string in IPv4 dot notation to an int in network order.
 *
 * @param address address in dot notation
 * @param inp result of conversion (in network order)
 * @return 0 on success, error code on error
 */
int inet_aton(const char *address, struct in_addr *inp){
    int err = 0; 
    unsigned long addr;
    
    err = get_inet_addr(address, &addr);
    if(err) goto exit;
    inp->s_addr = addr;
  exit:
    return err;
}
#endif

/** Convert a hostname or IPv4 address string to an address in network order.
 *
 * @param name input hostname or address string
 * @param address where to put the address
 * @return 0 if address found OK, nonzero otherwise
 */
int get_host_address(const char *name, unsigned long *address){
#ifdef __KERNEL__
    return get_inet_addr(name, address);
#else
    struct hostent *host = gethostbyname(name);
    if(!host){
        return -ENOENT;
    }
    *address = ((struct in_addr *)(host->h_addr))->s_addr;
    return 0;
#endif
}

/** Convert a service name to a port (in network order).
 *
 * @param name service name
 * @param port where to put the port
 * @return 0 if service port found OK, negative otherwise
 */
int get_service_port(const char *name, unsigned long *port){
#ifdef __KERNEL__
    return -ENOSYS;
#else
    struct servent *service;
    service = getservbyname(name, 0);
    if(!service){
        return -EINVAL;
    }
    *port = service->s_port;
    return 0;
#endif
}

/** Convert a port number (in network order) to a service name.
 *
 * @param port the port number
 * @return service name if found OK, NULL otherwise
 */
char *get_port_service(unsigned long port){
#ifdef __KERNEL__
    return NULL;
#else
    struct servent *service = getservbyport(port, 0);
    return (service ? service->s_name : NULL);
#endif
}

/** Convert a decimal integer or service name to a port (in network order).
 *
 * @param s input to convert
 * @param port where to put the port
 * @return 0 if port found OK, -1 otherwise
 */
int convert_service_to_port(const char *s, unsigned long *port){
    int err = 0;
    unsigned long value;
    if(convert_atoul(s, &value) == 0){
        int ok = (0 <= value) && (value <= PORT_MAX);
        if(ok){
            value = htons((unsigned short)value);
        } else {
            err = -EINVAL;
        }
    } else {
        err = get_service_port(s, &value);
    }
    *port = (err ? 0: value);
    return err;
}

#define MAC_ELEMENT_N  6 // Number of elements in a MAC address.
#define MAC_DIGIT_N    2 // Number of digits in an element in a MAC address.
#define MAC_LENGTH    17 //((MAC_ELEMENT_N * MAC_DIGIT_N) + MAC_ELEMENT_N - 1)

/** Convert a mac address from a string of the form
 * XX:XX:XX:XX:XX:XX to numerical form (an array of 6 unsigned chars).
 * Each X denotes a hex digit: 0..9, a..f, A..F.
 * Also supports using '-' as the separator instead of ':'.
 *
 * @param mac_in string to convert
 * @param mac destination for the value
 * @return 0 on success, -1 on error
 */
int mac_aton(const char *mac_in, unsigned char *mac){
    int err = 0;
    int i, j;
    const char *p;
    char sep = 0;
    unsigned char d;
    if(!mac_in || strlen(mac_in) != MAC_LENGTH){
        err = -1;
        goto exit;
    }
    for(i = 0, p = mac_in; i < MAC_ELEMENT_N; i++){
        d = 0;
        if(i){
            if(!sep){
                if(*p == ':' || *p == '-') sep = *p;
            }
            if(sep && *p == sep){
                p++;
            } else {
                err = -1;
                goto exit;
            }
        }
        for(j = 0; j < MAC_DIGIT_N; j++, p++){
            if(j) d <<= 4;
            if(*p >= '0' && *p <= '9'){
                d += (*p - '0');
            } else if(*p >= 'A' && *p <= 'F'){
                d += (*p - 'A') + 10;
            } else if(*p >= 'a' && *p <= 'f'){
                d += (*p - 'a') + 10;
            } else {
                err = -1;
                goto exit;
            }
        }
        mac[i] = d;
    }
  exit:
    return err;
}

/** Convert a MAC address from numerical form to a string.
 *
 * @param mac address to convert
 * @return static string value
 */
char *mac_ntoa(const unsigned char *mac){
    static char buf[MAC_LENGTH + 1];
    int buf_n = sizeof(buf);

    memset(buf, 0, buf_n);
    snprintf(buf, buf_n, "%02x:%02x:%02x:%02x:%02x:%02x",
             mac[0], mac[1], mac[2],
             mac[3], mac[4], mac[5]);
    buf[buf_n - 1] = '\0';
    return buf;
}
