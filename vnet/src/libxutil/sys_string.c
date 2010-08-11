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

#ifdef __KERNEL__
#  include <linux/config.h>
#  include <linux/module.h>
#  include <linux/kernel.h>
#  include <linux/errno.h>
#else
#  include <errno.h>
#endif

#include "allocate.h"
#include "sys_string.h"

#ifdef __KERNEL__

#define deferr(_err) case _err: return #_err

extern char *strerror(int err)
{
    switch(err){
        deferr(EPERM);
        deferr(ENOENT);
        deferr(ESRCH);
        deferr(EINTR);
        deferr(EIO);
        deferr(EINVAL);
        deferr(ENOMEM);
        deferr(EACCES);
        deferr(EFAULT);
        deferr(EBUSY);
        
    default:
        return "ERROR";
    }
}

#endif

/** Set the base to use for converting a string to a number.  Base is
 * hex if starts with 0x, otherwise decimal.
 *
 * @param s input string
 * @param base where to put the base
 * @return rest of s to parse as a number
 */
inline static const char * convert_set_base(const char *s, int *base){
    *base = 10;
    if(s){
        if(*s=='0'){
            s++;
            if(*s=='x' || *s=='X'){
                *base = 16;
                s++;
            }
        }
    }
    return s;
}

/** Set the sign to use for converting a string to a number.
 * Value is 1 for positive, -1 for negative.
 *
 * @param s input string
 * @param sign where to put the sign
 * @return rest of s to parse as a number
 */
inline static const char * convert_set_sign(const char *s, int *sign){
    *sign = 1;
    if(s){
        if(*s == '+'){
            *sign = 1;
            s++;
        } else if (*s == '-'){
            *sign = -1;
            s++;
        }
    }
    return s;
}

/** Get the numerical value of a digit in the given base.
 *
 * @param c digit character
 * @param base to use
 * @return numerical value of digit in range 0..base-1 or
 * -1 if not in range for the base
 */
inline static int convert_get_digit(char c, int base){
    int d;

    if('0'<=c  && c<='9'){
        d = c - '0';
    } else if('a'<=c && c<='f'){
        d = c - 'a' + 10;
    } else if('A'<=c && c<='F'){
        d = c - 'A' + 10;
    } else {
        d = -1;
    }
    return (d < base ? d : -1);
}

/** Convert a string to an unsigned long by parsing it as a number.
 * Will accept hex or decimal in usual C syntax.
 *
 * @param str input string
 * @param val where to put the result
 * @return 0 if converted OK, negative otherwise
 */
int convert_atoul(const char *str, unsigned long *val){
    int err = 0;
    unsigned long v = 0;
    int base;
    const char *s = str;

    if(!s) {
        err = -EINVAL;
        goto exit;
    }
    s = convert_set_base(s, &base);
    for( ; !err && *s; s++){
        int digit = convert_get_digit(*s, base);
        if(digit<0){
            err = -EINVAL;
            goto exit;
        }
        v *= base;
        v += digit;
    } 
  exit:
    *val = (err ? 0 : v);
    return err;
}

/** Convert a string to a long by parsing it as a number.
 * Will accept hex or decimal in usual C syntax.
 *
 * @param str input string
 * @param val where to put the result
 * @return 0 if converted OK, negative otherwise
 */
int convert_atol(const char *str, long *val){
    int err = 0;
    unsigned long v = 0;
    int base, sign = 1;
    const char *s = str;

    if(!s) {
        err = -EINVAL;
        goto exit;
    }
    s = convert_set_sign(s, &sign);
    s = convert_set_base(s, &base);
    for( ; !err && *s; s++){
        int digit = convert_get_digit(*s, base);
        if(digit<0){
            err = -EINVAL;
            goto exit;
        }
        v *= base;
        v += digit;
    } 
    if(sign < 0) v = -v;
  exit:
    *val = (err ? 0 : v);
    return err;
}

/** Combine a directory path with a relative path to produce
 * a new path.
 *
 * @param s directory path
 * @param t relative path
 * @return new combined path s/t
 */
int path_concat(char *s, char *t, char **val){
    int err = 0;
    int sn, tn, vn;
    char *v;
    sn = strlen(s);
    if(sn > 0 && s[sn-1] == '/'){
        sn--;
    }
    tn = strlen(t);
    if(tn > 0 && t[0] == '/'){
        tn--;
    }
    vn = sn+tn+1;
    v = (char*)allocate(vn+1);
    if(!v){
        err = -ENOMEM;
        goto exit;
    }
    strncpy(v, s, sn);
    v[sn] = '/';
    strncpy(v+sn+1, t, tn);
    v[vn] = '\0';
  exit:
    *val = (err ? NULL : v);
    return err;    
}
