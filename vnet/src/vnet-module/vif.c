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
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/spinlock.h>
#include <linux/jiffies.h>
#include <linux/timer.h>

#else

#include "sys_kernel.h"
#include "spinlock.h"
#include "skbuff.h"

#endif

#include <vif.h>
#include <varp.h>
#include <varp_util.h>

#include "allocate.h"
#include "iostream.h"
#include "hash_table.h"
#include "timer_util.h"

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** Vif table ttl - interval between sweeps of old vifs. */
#define VIF_TABLE_TTL (60*HZ)

/** Vif entry ttl - a vif entry older than this is removed. */
#define VIF_ENTRY_TTL (60*HZ)

/** Table of vifs indexed by VifKey. */
HashTable *vif_table = NULL;
rwlock_t vif_table_lock = RW_LOCK_UNLOCKED;
struct timer_list vif_table_timer = {};
int vif_table_sweeps = 0;

#define vif_read_lock(flags)    read_lock_irqsave(&vif_table_lock, (flags))
#define vif_read_unlock(flags)  read_unlock_irqrestore(&vif_table_lock, (flags))
#define vif_write_lock(flags)   write_lock_irqsave(&vif_table_lock, (flags))
#define vif_write_unlock(flags) write_unlock_irqrestore(&vif_table_lock, (flags))

void vif_entry_print(Vif *vif, IOStream *io){
    char vnetbuf[VNET_ID_BUF];
    unsigned long now = jiffies;

    IOStream_print(io, "(vif\n");
    IOStream_print(io, " (vnet %s)\n", VnetId_ntoa(&vif->vnet, vnetbuf));
    IOStream_print(io, " (vmac " MACFMT ")\n", MAC6TUPLE(vif->vmac.mac));
    IOStream_print(io, " (age %u)\n", now - vif->timestamp);
    IOStream_print(io, ")\n");
}

void vif_print(IOStream *io){
    HashTable_for_decl(entry);
    Vif *vif;
    unsigned long flags;

    vif_read_lock(flags);
    IOStream_print(io, "(viftable\n");
    IOStream_print(io, " (table_ttl %u)\n", VIF_TABLE_TTL);
    IOStream_print(io, " (entry_ttl %u)\n", VIF_ENTRY_TTL);
    IOStream_print(io, " (sweeps %d)\n", vif_table_sweeps);
    IOStream_print(io, ")\n");
    
    HashTable_for_each(entry, vif_table){
        vif = entry->value;
        vif_entry_print(vif, io);
    }
    vif_read_unlock(flags);
}

void vif_decref(Vif *vif){
    if(!vif) return;
    if(atomic_dec_and_test(&vif->refcount)){
        kfree(vif);
    }
}

void vif_incref(Vif *vif){
    if(!vif) return;
    atomic_inc(&vif->refcount);
}

/** Hash function for keys in the vif table.
 * Hashes the vnet id and mac.
 *
 * @param k key (VifKey)
 * @return hashcode
 */
static Hashcode vif_key_hash_fn(void *k){
    return hash_hvoid(0, k, sizeof(VifKey));
}

/** Test equality for keys in the vif table.
 * Compares vnet and mac.
 *
 * @param k1 key to compare (VifKey)
 * @param k2 key to compare (VifKey)
 * @return 1 if equal, 0 otherwise
 */
static int vif_key_equal_fn(void *k1, void *k2){
    return memcmp(k1, k2, sizeof(VifKey)) == 0;
}

/** Free an entry in the vif table.
 *
 * @param table containing table
 * @param entry entry to free
 */
static void vif_entry_free_fn(HashTable *table, HTEntry *entry){
    Vif *vif;
    if(!entry) return;
    vif = entry->value;
    if(vif){
        vif_decref(vif);
    }
    HTEntry_free(entry);
}

/** Lookup a vif.
 * Caller must hold vif lock.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, -ENOENT otherwise
 */
static int _vif_lookup(VnetId *vnet, Vmac *vmac, Vif **vif){
    int err = 0;
    VifKey key = { .vnet = *vnet, .vmac = *vmac };
    HTEntry *entry = NULL;
    
    entry = HashTable_get_entry(vif_table, &key);
    if(entry){
        *vif = entry->value;
        vif_incref(*vif);
    } else {
        *vif = NULL;
        err = -ENOENT;
    }
    return err;
}

/** Lookup a vif.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, -ENOENT otherwise
 */
int vif_lookup(VnetId *vnet, Vmac *vmac, Vif **vif){
    unsigned long flags;    
    int err;

    vif_read_lock(flags);
    err = _vif_lookup(vnet, vmac, vif);
    vif_read_unlock(flags);
    return err;
}

/** Create a new vif.
 * Entry must not exist.
 * Caller must hold vif lock.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, negative error code otherwise
 */
static int _vif_add(VnetId *vnet, Vmac *vmac, Vif **val){
    int err = 0;
    Vif *vif = NULL;
    HTEntry *entry;
    unsigned long now = jiffies;

    vif = ALLOCATE(Vif);
    if(!vif){
        err = -ENOMEM;
        goto exit;
    }
    atomic_set(&vif->refcount, 1);
    vif->vnet = *vnet;
    vif->vmac = *vmac;
    vif->timestamp = now;
    entry = HashTable_add(vif_table, vif, vif);
    if(!entry){
        err = -ENOMEM;
        deallocate(vif);
        vif = NULL;
        goto exit;
    }
    vif_incref(vif);
  exit:
    *val = (err ? NULL : vif);
    return err;
}

/** Delete a vif entry.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return number of entries deleted, or negative error code
 */
int vif_remove(VnetId *vnet, Vmac *vmac){
    int err = 0;
    VifKey key = { .vnet = *vnet, .vmac = *vmac };
    unsigned long flags;

    vif_write_lock(flags);
    err = HashTable_remove(vif_table, &key);
    vif_write_unlock(flags);
    return err;
}

/** Delete all vifs on a vnet.
 *
 * @param vnet vnet id
 * @return number of entries deleted
 */
int vif_remove_vnet(VnetId *vnet){
    int count = 0;
    unsigned long flags;
    HashTable_for_decl(entry);

    
    vif_write_lock(flags);
    HashTable_for_each(entry, vif_table){
        Vif *vif = entry->value;
        if(VnetId_eq(&vif->vnet, vnet)){
            count += HashTable_remove(vif_table, vif);
        }
    }
    vif_write_unlock(flags);
    return count;
}

/** Purge the vif table.
 */
void vif_purge(void){
    unsigned long flags;
    vif_write_lock(flags);
    HashTable_clear(vif_table);
    vif_write_unlock(flags);
}

/** Sweep old vif entries from the vif table.
 */
void vif_sweep(void){
    HashTable_for_decl(entry);
    Vif *vif;
    int vif_count = 0;
    unsigned long now = jiffies;
    unsigned long old = VIF_ENTRY_TTL;
    unsigned long flags;

    vif_write_lock(flags);
    vif_table_sweeps++;
    HashTable_for_each(entry, vif_table){
        vif = entry->value;
        vif_count++;
        if(!(vif->flags & VIF_FLAG_PERSISTENT)
           && (now - vif->timestamp > old)){
            iprintf("> Sweeping:\n");
            vif_entry_print(vif, iostdout);
            HashTable_remove(vif_table, entry->key);
        }
    }
    vif_write_unlock(flags);
}

/** Create a new vif if it does not exist.
 * Caller must hold vif lock.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, negative error code otherwise
 */
int _vif_create(VnetId *vnet, Vmac *vmac, Vif **vif){
    int err = 0;

    if(_vif_lookup(vnet, vmac, vif) == 0){
        goto exit;
    }
    err = _vif_add(vnet, vmac, vif);
  exit:
    return err;
}

/** Create a new vif if it does not exist.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, negative error code otherwise
 */
int vif_create(VnetId *vnet, Vmac *vmac, int vflags, Vif **vif){
    int err = 0;
    unsigned long flags;

    vif_write_lock(flags);
    err = _vif_create(vnet, vmac, vif);
    if(!err && *vif){
        (*vif)->flags = vflags;
    }
    vif_write_unlock(flags);
    return err;
}

/** Update the timestamp for a vif.
 *
 * @param vnet vnet id
 * @param mac MAC address
 * @return 0 on success, negative error code otherwise
 */
int vif_update(VnetId *vnet, Vmac *vmac){
    Vif *vif = NULL;
    int err = 0;
    unsigned long now = jiffies;
    unsigned long flags;

    vif_write_lock(flags);
    err = _vif_create(vnet, vmac, &vif);
    if(err) goto exit;
    vif->timestamp = now;
    vif_decref(vif);
  exit:
    vif_write_unlock(flags);
    return err;
}

static void vif_table_timer_fn(unsigned long arg){
    if(!vif_table) return;
    vif_sweep();
    timer_set(&vif_table_timer, VIF_TABLE_TTL);
}
    
/** Initialize the vif table.
 *
 * @return 0 on success, error code otherwise
 */
int vif_init(void){
    int err = 0;
    vif_table = HashTable_new(0);
    if(!vif_table){
        err = -ENOMEM;
        goto exit;
    }
    vif_table->entry_free_fn = vif_entry_free_fn;
    vif_table->key_size = sizeof(VifKey);
    vif_table->key_hash_fn   = vif_key_hash_fn;
    vif_table->key_equal_fn  = vif_key_equal_fn;

    timer_init(&vif_table_timer, vif_table_timer_fn, 0);
    timer_set(&vif_table_timer, VIF_TABLE_TTL);

  exit:
    if(err < 0){
        eprintf("> vif_init err=%d\n", err);
    }
    return err;
}

void vif_exit(void){
    timer_cancel(&vif_table_timer);
    HashTable_free(vif_table);
    vif_table = NULL;
}
