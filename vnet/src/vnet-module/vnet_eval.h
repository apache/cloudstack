/*
 * Copyright (C) 2004, 2005 Mike Wray <mike.wray@hp.com>
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
#ifndef _VNET_EVAL_H_
#define _VNET_EVAL_H_

#include "sxpr.h"
struct IOStream;

typedef struct SxprEval {
    Sxpr name;
    int (*fn)(Sxpr, struct IOStream *, void *data);
} SxprEval;

extern int eval_peer_add(Sxpr exp, struct IOStream *out, void *data);
extern int eval_vnet_add(Sxpr exp, struct IOStream *out, void *data);
extern int vnet_eval_defs(SxprEval *defs, Sxpr exp, struct IOStream *out, void *data);
extern int vnet_eval(Sxpr exp, struct IOStream *out, void *data);

#endif /* ! _VNET_EVAL_H_ */
