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

/** @file
 * Lexical analysis.
 */

#include "sys_string.h"
#include "lexis.h"
#include <errno.h>

/** Check if a value lies in a (closed) range.
 *
 * @param x value to test
 * @param lo low end of the range
 * @param hi high end of the range
 * @return 1 if x is in the interval [lo, hi], 0 otherwise
 */
inline static int in_range(int x, int lo, int hi){
    return (lo <= x) && (x <= hi);
}

/** Determine if a string is an (unsigned) decimal number.
 * 
 * @param s pointer to characters to test
 * @param n length of string
 * @return 1 if s is a decimal number, 0 otherwise.
 */
int is_decimal_number(const char *s, int n){
    int i;
    if(n <= 0)return 0;
    for(i = 0; i < n; i++){
        if(!in_decimal_digit_class(s[i])) return 0;
    }
    return 1;
}

/** Determine if a string is a hex number.
 * Hex numbers are 0, or start with 0x or 0X followed
 * by a non-zero number of hex digits (0-9,a-f,A-F).
 * 
 * @param s pointer to characters to test
 * @param n length of string
 * @return 1 if s is a hex number, 0 otherwise.
 */
int is_hex_number(const char *s, int n){
    int i;
    if(n <= 0) return 0;
    if(n == 1){
        return s[0]=='0';
    }
    if(n <= 3) return 0;
    if(s[0] != '0' || (s[1] != 'x' && s[1] != 'X')) return 0;
    for(i = 2; i < n; i++){
        if(!in_hex_digit_class(s[i])) return 0;
    }
    return 1;
}

/** Test if a string matches a keyword.
 * The comparison is case-insensitive.
 * The comparison fails if either argument is null.
 *
 * @param s string
 * @param k keyword
 * @return 1 if they match, 0 otherwise
 */
int is_keyword(const char *s, const char *k){
  return s && k && !strcasecmp(s, k);
}

/** Test if a string matches a character.
 *
 * @param s string
 * @param c character (non-null)
 * @return 1 if s contains exactly c, 0 otherwise
 */
int is_keychar(const char *s, char c){
  return c && (s[0] == c) && !s[1];
}
