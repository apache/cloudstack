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

#ifdef __KERNEL__

#include <linux/config.h>
#include <linux/module.h>
#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/version.h>
#include <linux/errno.h>

#else 

#include "sys_kernel.h"
#include "spinlock.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#endif

#include "vnet.h"
#include "varp.h"
#include "vif.h"
#include "vnet_forward.h"
#include "sa.h"

#include "iostream.h"

#ifdef __KERNEL__
#include "kernel_stream.h"
#else
#include "file_stream.h"
#endif

#include "sxpr_util.h"
#include "vnet_eval.h"

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** Create a vnet.
 * It is an error if a vnet with the same id exists.
 *
 * @param vnet vnet id
 * @param device vnet device name
 * @param security security level
 * @return 0 on success, error code otherwise
 */
static int ctrl_vnet_add(VnetId *vnet, char *device, int security){
    int err = 0;
    Vnet *vnetinfo = NULL;

    if(strlen(device) >= IFNAMSIZ){
        err = -EINVAL;
        goto exit;
    }
    if(Vnet_lookup(vnet, NULL) == 0){
        err = -EEXIST;
        goto exit;
    }
    err = Vnet_alloc(&vnetinfo);
    if(err) goto exit;
    vnetinfo->vnet = *vnet;
    vnetinfo->security = security;
    strcpy(vnetinfo->device, device);
    err = Vnet_create(vnetinfo);
  exit:
    if(vnetinfo) Vnet_decref(vnetinfo);
    return err;
}

/** Create an entry for a vif with the given vnet and vmac.
 *
 * @param vnet vnet id
 * @param vmac mac address
 * @return 0 on success, error code otherwise
 */
static int ctrl_vif_add(VnetId *vnet, Vmac *vmac){
    int err = 0;
    Vif *vif = NULL;

    err = Vnet_lookup(vnet, NULL);
    if(err) goto exit;
    err = vif_create(vnet, vmac, 0, &vif);
  exit:
    if(vif) vif_decref(vif);
    return err;
}

/** Delete a vif.
 *
 * @param vnet vnet id
 * @param vmac mac address
 * @return 0 on success, error code otherwise
 */
static int ctrl_vif_del(VnetId *vnet, Vmac *vmac){
    int err = 0;
    Vif *vif = NULL;

    err = Vnet_lookup(vnet, NULL);
    if(err) goto exit;
    err = vif_lookup(vnet, vmac, &vif);
    if(err) goto exit;
    vif_remove(vnet, vmac);
  exit:
    if(vif) vif_decref(vif);
    return err;
}

/** (varp.print)
 */
static int eval_varp_print(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    vnet_print(out);
    vif_print(out);
    varp_print(out);
    return err;
}

static int eval_varp_list(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    varp_print(out);
    return err;
}

/** (varp.mcaddr (addr <addr>))
 */
static int eval_varp_mcaddr(Sxpr exp, IOStream *out, void *data){
    int err =0;
    Sxpr oaddr = intern("addr");
    uint32_t addr;

    err = child_addr(exp, oaddr, &addr);
    if(err < 0) goto exit;
    varp_set_mcast_addr(addr);
  exit:
    return err;
}

/** (varp.flush)
 */
static int eval_varp_flush(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    varp_flush();
    return err;
}

/** (vnet.add (id <id>)
 *            [(vnetif <name>)]
 *            [(security { none | auth | conf } )]
 *  )
 */
int eval_vnet_add(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr oid = intern("id");
    Sxpr osecurity = intern("security");
    Sxpr ovnetif = intern("vnetif");
    Sxpr csecurity;
    VnetId vnet = {};
    char *device = NULL;
    char dev[IFNAMSIZ] = {};
    char *security = NULL;
    int sec;

    err = child_vnet(exp, oid, &vnet);
    if(err) goto exit;
    child_string(exp, ovnetif, &device);
    if(!device){
        snprintf(dev, IFNAMSIZ-1, "vnif%04x", ntohs(vnet.u.vnet16[VNETID_SIZE16 - 1]));
        device = dev;
    }
    csecurity = sxpr_child_value(exp, osecurity, intern("none"));
    err = stringof(csecurity, &security);
    if(err) goto exit;
    if(strcmp(security, "none")==0){
        sec = 0;
    } else if(strcmp(security, "auth")==0){
        sec = SA_AUTH;
    } else if(strcmp(security, "conf")==0){
        sec = SA_CONF;
    } else {
        err = -EINVAL;
        goto exit;
    }
    err = ctrl_vnet_add(&vnet, device, sec);
 exit:
    return err;
}

/** Delete a vnet.
 *
 * (vnet.del (id <id>))
 *
 * @param vnet vnet id
 * @return 0 on success, error code otherwise
 */
static int eval_vnet_del(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr oid = intern("id");
    VnetId vnet = {};

    err = child_vnet(exp, oid, &vnet);
    if(err) goto exit;
    err = Vnet_del(&vnet);
  exit:
    return err;
}

static int eval_vnet_list(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    vnet_print(out);
    return err;
}

/** (vif.add (vnet <vnet>) (vmac <macaddr>))
 */
static int eval_vif_add(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr ovnet = intern("vnet");
    Sxpr ovmac = intern("vmac");
    VnetId vnet = {};
    Vmac vmac = {};

    err = child_vnet(exp, ovnet, &vnet);
    if(err) goto exit;
    err = child_mac(exp, ovmac, vmac.mac);
    if(err) goto exit;
    err = ctrl_vif_add(&vnet, &vmac);
  exit:
    return err;
}

/** (vif.del (vnet <vnet>) (vmac <macaddr>))
 */
static int eval_vif_del(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr ovnet = intern("vnet");
    Sxpr ovmac = intern("vmac");
    VnetId vnet = {};
    Vmac vmac = {};

    err = child_vnet(exp, ovnet, &vnet);
    if(err) goto exit;
    err = child_mac(exp, ovmac, vmac.mac);
    if(err) goto exit;
    err = ctrl_vif_del(&vnet, &vmac);
  exit:
    return err;
}

static int eval_vif_list(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    vif_print(out);
    return err;
}

/** Eval a vnet add request.
 *
 * (peer.add (addr <addr>) [(port <port>)])
 *
 * @param exp request
 * @param out output stream
 * @param data data
 * @return 0 on success, error code otherwise
 */
int eval_peer_add(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr oaddr = intern("addr");
    Sxpr oport = intern("port");
    VarpAddr addr = { .family = AF_INET };
    int port;

    err = child_addr(exp, oaddr, &addr.u.ip4.s_addr);
    if(err < 0) goto exit;
    err = child_int(exp, oport, &port);
    if(err < 0){
        err = 0;
        port = varp_port;
    }
    if(err) goto exit;
    err = vnet_peer_add(&addr, port);
  exit:
    return err;
}

/** Eval a peer delete request.
 *
 * (peer.del (addr <addr>))
 *
 * @param vnetd vnetd
 * @param exp request
 * @param out output stream
 * @param data data
 * @return 0 on success, error code otherwise
 */
static int eval_peer_del(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    Sxpr oaddr = intern("addr");
    VarpAddr addr = { .family = AF_INET };

    err = child_addr(exp, oaddr, &addr.u.ip4.s_addr);
    if(err < 0) goto exit;
    err = vnet_peer_del(&addr);
  exit:
    return err;
}

/** Eval a peer list request.
 *
 * (peer.list)
 *
 * @param exp request
 * @param out output stream
 * @param data data
 * @return 0 on success, error code otherwise
 */
static int eval_peer_list(Sxpr exp, IOStream *out, void *data){
    int err = 0;
    vnet_peer_print(out);
    return err;
}

int vnet_eval_defs(SxprEval *defs, Sxpr exp, IOStream *io, void *data){
    int err = 0;
    SxprEval *def;

    iprintf("> "); objprint(iostdout, exp, 0); IOStream_print(iostdout, "\n");
    err = -ENOSYS;
    for(def = defs; !NONEP(def->name); def++){
        if(sxpr_elementp(exp, def->name)){
            err = def->fn(exp, io, data);
            break;
        }
    }
    iprintf("< err=%d\n", err);
    return err;
}

int vnet_eval(Sxpr exp, IOStream *io, void *data){
    SxprEval defs[] = {
        { .name = intern("peer.add"),     .fn = eval_peer_add     },
        { .name = intern("peer.del"),     .fn = eval_peer_del     },
        { .name = intern("peer.list"),    .fn = eval_peer_list    },
        { .name = intern("varp.flush"),   .fn = eval_varp_flush   },
        { .name = intern("varp.list"),    .fn = eval_varp_list    },
        { .name = intern("varp.mcaddr"),  .fn = eval_varp_mcaddr  },
        { .name = intern("varp.print"),   .fn = eval_varp_print   },
        { .name = intern("vif.add"),      .fn = eval_vif_add      },
        { .name = intern("vif.del"),      .fn = eval_vif_del      },
        { .name = intern("vif.list"),     .fn = eval_vif_list     },
        { .name = intern("vnet.add"),     .fn = eval_vnet_add     },
        { .name = intern("vnet.del"),     .fn = eval_vnet_del     },
        { .name = intern("vnet.list"),    .fn = eval_vnet_list    },
        { .name = ONONE, .fn = NULL } };
    return vnet_eval_defs(defs, exp, io, data);
}
