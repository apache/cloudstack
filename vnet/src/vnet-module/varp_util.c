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

static int hex16(char *s, uint16_t *val)
{
    int err = -EINVAL;
    uint16_t v = 0;
    
    for( ; *s; s++){
        v <<= 4;
        if('0' <= *s && *s <= '9'){
            v |= *s - '0';
        } else if('A' <= *s && *s <= 'F'){
            v |= *s - 'A' + 10;
        } else if('a' <= *s && *s <= 'f'){
            v |= *s - 'a' + 10;
        } else {
            goto exit;
        }
    }
    err = 0;
  exit:
    *val = (err ? 0 : v);
    return err;
}

int VnetId_aton(const char *s, VnetId *vnet){
    int err = -EINVAL;
    const char *p, *q;
    uint16_t v;
    char buf[5];
    int buf_n = sizeof(buf) - 1;
    int i, n;
    const int elts_n = VNETID_SIZE16;

    q = s;
    p = strchr(q, ':');
    i = (p ? 0 : elts_n - 1);
    do {
        if(!p){
            if(i < elts_n - 1) goto exit;
            p = s + strlen(s);
        }
        n = p - q;
        if(n > buf_n) goto exit;
        memcpy(buf, q, n);
        buf[n] = '\0';
        err = hex16(buf, &v);
        if(err) goto exit;
        vnet->u.vnet16[i] = htons(v);
        q = p+1;
        p = strchr(q, ':');
        i++;
    } while(i < elts_n);
    err = 0;
  exit:
    if(err){
        *vnet = (VnetId){};
    }
    return err;
}
