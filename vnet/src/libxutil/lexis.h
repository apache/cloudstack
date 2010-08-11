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

#ifndef _XUTIL_LEXIS_H_
#define _XUTIL_LEXIS_H_

#include "sys_string.h"

#ifdef __KERNEL__
#  include <linux/ctype.h>
#else
#  include <ctype.h>
#endif

/** @file
 * Lexical analysis.
 */

/** Class of characters treated as space. */
#define space_class ((char []){ '\n', '\r', '\t', ' ', '\f' , 0 })

/** Class of separator characters. */
#define sep_class "{}()<>[]!;\"'"

#define comment_class "#"

/** Determine if a character is in a given class.
 * 
 * @param c character to test
 * @param s null-terminated string of characters in the class
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_class(int c, const char *s){
  return s && (strchr(s, c) != 0);
}

/** Determine if a character is in the space class.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_space_class(int c){
    return in_class(c, space_class);
}

static inline int in_comment_class(int c){
    return in_class(c, comment_class);
}

/** Determine if a character is in the separator class.
 * Separator characters terminate tokens, and do not need space
 * to separate them.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_sep_class(int c){
    return in_class(c, sep_class);
}

/** Determine if a character is in the alpha class.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_alpha_class(int c){
    return isalpha(c);
}

/** Determine if a character is in the octal digit class.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_octal_digit_class(int c){
    return '0' <= c && c <= '7';
}

/** Determine if a character is in the decimal digit class.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_decimal_digit_class(int c){
    return isdigit(c);
}

/** Determine if a character is in the hex digit class.
 * 
 * @param c character to test
 * @return 1 if c is in the class, 0 otherwise.
 */
static inline int in_hex_digit_class(int c){
    return isdigit(c) || in_class(c, "abcdefABCDEF");
}


static inline int in_string_quote_class(int c){
    return in_class(c, "'\"");
}

static inline int in_printable_class(int c){
    return ('A' <= c && c <= 'Z')
        || ('a' <= c && c <= 'z')
        || ('0' <= c && c <= '9')
        || in_class(c, "!$%&*+,-./:;<=>?@^_`{|}~");
}

extern int is_decimal_number(const char *s, int n);
extern int is_hex_number(const char *s, int n);
extern int is_keyword(const char *s, const char *k);
extern int is_keychar(const char *s, char c);

#endif /* !_XUTIL_LEXIS_H_ */
