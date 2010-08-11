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

#ifdef __KERNEL__
#include <linux/errno.h>
#else
#include <errno.h>
#endif

#include "sys_string.h"
#include "enum.h"

/** Map an enum name to its value using a table.
 *
 * @param name enum name
 * @param defs enum definitions
 * @return enum value or -1 if not known
 */
int enum_name_to_val(char *name, EnumDef *defs){
    int val = -1;
    for(; defs->name; defs++){
	if(!strcmp(defs->name, name)){
	    val = defs->val;
	    break;
	}
    }
    return val;
}

/** Map an enum value to its name using a table.
 *
 * @param val enum value
 * @param defs enum definitions
 * @param defs_n number of definitions
 * @return enum name or NULL if not known
 */
char *enum_val_to_name(int val, EnumDef *defs){
    char *name = NULL;
    for(; defs->name; defs++){
	if(val == defs->val){
	    name = defs->name;
	    break;
	}
    }
    return name;
}

