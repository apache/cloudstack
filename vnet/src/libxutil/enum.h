/*
 * Copyright (C) 2002, 2004 Mike Wray <mike.wray@hp.com>
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

#ifndef _XUTIL_ENUM_H_
#define _XUTIL_ENUM_H_

/** Mapping of an enum value to a name. */
typedef struct EnumDef {
    int val;
    char *name;
} EnumDef;

extern int enum_name_to_val(char *name, EnumDef *defs);
extern char *enum_val_to_name(int val, EnumDef *defs);

#endif /* _XUTIL_ENUM_H_ */
