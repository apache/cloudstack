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
#include "sys_net.h"
#include "if_varp.h"
#include "varp_util.h"
#include "sxpr_util.h"

int stringof(Sxpr exp, char **s){
    int err = 0;
    if(ATOMP(exp)){
        *s = atom_name(exp);
    } else if(STRINGP(exp)){
        *s = string_string(exp);
    } else {
        err = -EINVAL;
        *s = NULL;
    }
    return err;
}

int child_string(Sxpr exp, Sxpr key, char **s){
    int err = 0;
    Sxpr val = sxpr_child_value(exp, key, ONONE);
    err = stringof(val, s);
    return err;
}

int intof(Sxpr exp, int *v){
    int err = 0;
    char *s;
    unsigned long l;
    if(INTP(exp)){
        *v = OBJ_INT(exp);
    } else {
        err = stringof(exp, &s);
        if(err) goto exit;
        err = convert_atoul(s, &l);
        *v = (int)l;
    }
 exit:
    return err;
}

int child_int(Sxpr exp, Sxpr key, int *v){
    int err = 0;
    Sxpr val = sxpr_child_value(exp, key, ONONE);
    err = intof(val, v);
    return err;
}

int vnetof(Sxpr exp, VnetId *v){
    int err = 0;
    char *s;
    err = stringof(exp, &s);
    if(err) goto exit;
    err = VnetId_aton(s, v);
  exit:
    return err;
}

int child_vnet(Sxpr exp, Sxpr key, VnetId *v){
    int err = 0;
    Sxpr val = sxpr_child_value(exp, key, ONONE);
    err = vnetof(val, v);
    return err;
}

int macof(Sxpr exp, unsigned char *v){
    int err = 0;
    char *s;
    err = stringof(exp, &s);
    if(err) goto exit;
    err = mac_aton(s, v);
  exit:
    return err;
}

int child_mac(Sxpr exp, Sxpr key, unsigned char *v){
    int err = 0;
    Sxpr val = sxpr_child_value(exp, key, ONONE);
    err = macof(val, v);
    return err;
}

int addrof(Sxpr exp, uint32_t *v){
    int err = 0;
    char *s;
    unsigned long w;
    err = stringof(exp, &s);
    if(err) goto exit;
    err = get_inet_addr(s, &w);
    if(err) goto exit;
    *v = (uint32_t)w;
  exit:
    return err;
}

int child_addr(Sxpr exp, Sxpr key, uint32_t *v){
    int err = 0;
    Sxpr val = sxpr_child_value(exp, key, ONONE);
    err = addrof(val, v);
    return err;
}
