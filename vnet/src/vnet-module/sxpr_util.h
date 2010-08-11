/*
 * Copyright (C) 2005 Mike Wray <mike.wray@hp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free software Foundation, Inc.,
 * 59 Temple Place, suite 330, Boston, MA 02111-1307 USA
 *
 */
#ifndef _SXPR_UTIL_H_
#define _SXPR_UTIL__H_

#include "sxpr.h"
struct VnetId;

int stringof(Sxpr exp, char **s);
int child_string(Sxpr exp, Sxpr key, char **s);
int intof(Sxpr exp, int *v);
int child_int(Sxpr exp, Sxpr key, int *v);
int vnetof(Sxpr exp, struct VnetId *v);
int child_vnet(Sxpr exp, Sxpr key, struct VnetId *v);
int macof(Sxpr exp, unsigned char *v);
int child_mac(Sxpr exp, Sxpr key, unsigned char *v);
int addrof(Sxpr exp, uint32_t *v);
int child_addr(Sxpr exp, Sxpr key, uint32_t *v);

#endif /* ! _SXPR_UTIL_H_ */
