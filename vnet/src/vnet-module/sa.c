/*
 * Copyright (C) 2004 Mike Wray <mike.wray@hp.com>
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
#include <linux/kernel.h>

#include <tunnel.h>
#include <vnet.h>
#include <sa.h>
#include <sa_algorithm.h>

#include "hash_table.h"
#include "allocate.h"

#define MODULE_NAME "IPSEC"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file IPSEC Security Association (SA).
 */

/** Maximum number of protocols.*/
#define INET_PROTOCOL_MAX 256

/** Table of SA types indexed by protocol. */
static SAType *sa_type[INET_PROTOCOL_MAX] = {};

/** Hash a protocol number.
 *
 * @param protocol protocol number
 * @return hashcode
 */
static inline unsigned char InetProtocol_hash(int protocol){
    return (protocol) & (INET_PROTOCOL_MAX - 1);
}

/** Register an SA type.
 * It is an error if an SA type is already registered for the protocol.
 *
 * @param type SA type
 * @return 0 on success, error code otherwise
 */
int SAType_add(SAType *type){
    int err = -EINVAL;
    int hash;
    if(!type) goto exit;
    hash = InetProtocol_hash(type->protocol);
    if(sa_type[hash]) goto exit;
    err = 0;
    sa_type[hash] = type;
  exit:
    return err;
}

/** Deregister an SA type.
 * It is an error if no SA type is registered for the protocol.
 *
 * @param type SA type
 * @return 0 on success, error code otherwise
 */
int SAType_del(SAType *type){
    int err = -EINVAL;
    int hash;
    if(!type) goto exit;
    hash = InetProtocol_hash(type->protocol);
    if(!sa_type[hash]) goto exit;
    err = 0;
    sa_type[hash] = NULL;
  exit:
    return err;
}

int SAType_get(int protocol, SAType **type){
   int err = -ENOENT;
   int hash;
   hash = InetProtocol_hash(protocol);
   *type = sa_type[hash];
   if(!*type) goto exit;
   err = 0;
  exit:
   return err;
}

/* Defeat compiler warnings about unused functions. */
static int sa_key_check(SAKey *key, enum sa_alg_type type) __attribute__((unused));
static u32 random_spi(void) __attribute__((unused));
static u32 generate_key(u32 key, u32 offset, u32 spi) __attribute__((unused));

/** Check a key has an acceptable length for an algorithm.
 *
 * @param key key
 * @param type algorithm
 * @return 0 on success, error code otherwise
 */
static int sa_key_check(SAKey *key, enum sa_alg_type type){
    return 0;
}

static unsigned long sa_spi_counter = 0;

/** Mangle some input to generate output.
 * This is used to derive spis and keying material from secrets,
 * so it probably ought to be cryptographically strong.
 * Probably ought to use a good hash (sha1) or cipher (aes).
 *
 * @param input input bytes
 * @param n number of bytes
 * @return mangled value
 */
static u32 mangle(void *input, int n){
    return hash_hvoid(0, input, n);
}

/** Generate a random spi.
 * Uses a hashed counter.
 *
 * @return spi
 */
static u32 random_spi(void){
    u32 spi;
    do{
        spi = sa_spi_counter++;
        spi = mangle(&spi, sizeof(spi));
    } while(!spi);
    return spi;
}

 /** Generate a spi for a given protocol and address, using a secret key.
  * The offset is used when it is necessary to generate more than one spi
  * for the same protocol and address.
  *
  * @param key key
  * @param offset offset
  * @param protocol protocol
  * @param addr IP address
  * @return spi
  */
static u32 generate_spi(u32 key, u32 offset, u32 protocol, u32 addr){
    u32 input[] = { key, offset, protocol, addr };
    return mangle(input, sizeof(input));
}

/** Generate keying material for a given spi, based on a
 * secret.
 *
 * @param key secret
 * @param offset offset
 * @param spi spi
 * @return keying material
 */
static u32 generate_key(u32 key, u32 offset, u32 spi){
    u32 input[] = { key, offset, spi };
    return mangle(input, sizeof(input));
}    

/** Allocate a spi.
 * Want to use random ones.
 * So check for ones not in use.
 *
 * When using static keying, both ends need to agree on key.
 * How does that work? Also, will suddenly get traffic using a spi,
 * and will have to create SA then. Or need to create in advance.
 * But can't do that because don't know peers.
 * When get message on a spi that doesn't exist - do what?
 * Use a spi related to the destination addr and a secret.
 * Then receiver can check if spi is ok and create SA on demand.
 * Use hash of key, protocol, addr to generate. Then have to check
 * for in-use because of potential collisions. Receiver can do the
 * same hash and check spi is in usable range. Then derive keys from
 * the spi (using another secret).
 *
 * @param key spi generation key
 * @param protocol protocol
 * @param addr IP address
 * @param spip return parameter for spi
 * @return 0 on success, error code otherwise
 */
int sa_spi_alloc(u32 key, u32 protocol, u32 addr, u32 *spip){
    int err = 0;
    int i = 0, n = 100;
    u32 spi;
    for(i = 0; i < n; i++, spi++){
        spi = generate_spi(key, i, protocol, addr);
        if(!spi) continue;
        if(!sa_table_lookup_spi(spi, protocol, addr)){
            *spip = spi;
            goto exit;
        }
    }
    err = -ENOMEM;
  exit:
    return err;
}

/** Table of SAs. Indexed by unique id and spi/protocol/addr triple.
 */
static HashTable *sa_table = NULL;

static u32 sa_id = 1;

/** Hash an SA id.
 *
 * @param id SA id
 * @return hashcode
 */
static inline Hashcode sa_table_hash_id(u32 id){
    return hash_hvoid(0, &id, sizeof(id));
}

/** Hash SA spi/protocol/addr.
 *
 * @param spi spi
 * @param protocol protocol
 * @param addr IP address
 * @return hashcode
 */
static inline Hashcode sa_table_hash_spi(u32 spi, u32 protocol, u32 addr){
    u32 a[] = { spi, protocol, addr };
    return hash_hvoid(0, a, sizeof(a));
}

/** Test if an SA entry has a given value.
 *
 * @param arg contains SA pointer
 * @param table hashtable
 * @param entry entry containing SA
 * @return 1 if it does, 0 otherwise
 */
static int sa_table_state_fn(TableArg arg, HashTable *table, HTEntry *entry){
    return entry->value == arg.ptr;
}

/** Test if an SA entry has a given id.
 *
 * @param arg contains SA id
 * @param table hashtable
 * @param entry entry containing SA
 * @return 1 if it does, 0 otherwise
 */
static int sa_table_id_fn(TableArg arg, HashTable *table, HTEntry *entry){
    SAState *state = entry->value;
    u32 id = arg.ul;
    return state->ident.id == id;
}

/** Test if an SA entry has a given spi/protocol/addr.
 *
 * @param arg contains SAIdent pointer
 * @param table hashtable
 * @param entry entry containing SA
 * @return 1 if it does, 0 otherwise
 */
static int sa_table_spi_fn(TableArg arg, HashTable *table, HTEntry *entry){
    SAState *state = entry->value;
    SAIdent *ident = arg.ptr;
    return state->ident.spi      == ident->spi
        && state->ident.protocol == ident->protocol
        && state->ident.addr     == ident->addr;
}

/** Free an SA entry. Decrements the SA refcount and frees the entry.
 *
 * @param table containing table
 * @param entry to free
 */
static void sa_table_free_fn(HashTable *table, HTEntry *entry){
    if(!entry) return;
    if(entry->value){
        SAState *state = entry->value;
        SAState_decref(state);
    }
    deallocate(entry);
}

/** Initialize the SA table.
 *
 * @return 0 on success, error code otherwise
 */
int sa_table_init(void){
    int err = 0;
    sa_table = HashTable_new(0);
    if(!sa_table){
        err = -ENOMEM;
        goto exit;
    }
    sa_table->entry_free_fn = sa_table_free_fn;

  exit:
    return err;
}

void sa_table_exit(void){
    HashTable_free(sa_table);
}

/** Remove an SA from the table.
 *
 * @param state SA
 */
int sa_table_delete(SAState *state){
    int count = 0;
    Hashcode h1, h2;
    TableArg arg = { .ptr = state };
    // Remove by id.
    h1 = sa_table_hash_id(state->ident.id);
    count += HashTable_remove_entry(sa_table, h1, sa_table_state_fn, arg);
    // Remove by spi/protocol/addr if spi nonzero.
    if(!state->ident.spi) goto exit;
    h2 = sa_table_hash_spi(state->ident.spi, state->ident.protocol, state->ident.addr);
    if(h1 == h2) goto exit;
    count += HashTable_remove_entry(sa_table, h2, sa_table_state_fn, arg);
  exit:
    return count;
}

/** Add an SA to the table.
 * The SA is indexed by id and spi/protocol/addr (if the spi is non-zero).
 *
 * @param state SA
 * @return 0 on success, error code otherwise
 */
int sa_table_add(SAState *state){
    int err = 0;
    Hashcode h1, h2;
    int entries = 0;

    dprintf(">\n");
    // Index by id.
    h1 = sa_table_hash_id(state->ident.id);
    if(!HashTable_add_entry(sa_table, h1, HKEY(state->ident.id), state)){
        err = -ENOMEM;
        goto exit;
    }
    entries++;
    SAState_incref(state);
    // Index by spi/protocol/addr if spi non-zero.
    if(state->ident.spi){
        h2 = sa_table_hash_spi(state->ident.spi, state->ident.protocol, state->ident.addr);
        if(h1 != h2){
            if(!HashTable_add_entry(sa_table, h2, HKEY(state->ident.id), state)){
                err = -ENOMEM;
                goto exit;
            }
            entries++;
            SAState_incref(state);
        }
    }
  exit:
    if(err && entries){
        sa_table_delete(state);
    }
    dprintf("< err=%d\n", err);
    return err;
}


/** Find an SA by spi/protocol/addr.
 * Increments the SA refcount on success.
 *
 * @param spi spi
 * @param protocol protocol
 * @param addr IP address
 * @return SA or NULL
 */
SAState * sa_table_lookup_spi(u32 spi, u32 protocol, u32 addr){
    SAState *state = NULL;
    Hashcode h;
    SAIdent id = {
        .spi      = spi,
        .protocol = protocol,
        .addr     = addr };
    TableArg arg = { .ptr = &id };
    HTEntry *entry = NULL;

    h = sa_table_hash_spi(spi, protocol, addr);
    entry = HashTable_find_entry(sa_table, h, sa_table_spi_fn, arg);
    if(entry){
        state = entry->value;
        SAState_incref(state);
    }
    return state;
}

/** Find an SA by unique id.
 * Increments the SA refcount on success.
 *
 * @param id id
 * @return SA or NULL
 */
SAState * sa_table_lookup_id(u32 id){
    Hashcode h;
    TableArg arg = { .ul = id };
    HTEntry *entry = NULL;
    SAState *state = NULL;

    dprintf("> id=%u\n", id);
    h = sa_table_hash_id(id);
    entry = HashTable_find_entry(sa_table, h, sa_table_id_fn, arg);
    if(entry){
        state = entry->value;
        SAState_incref(state);
    }
    dprintf("< state=%p\n", state);
    return state;
}

/** Replace an existing SA by another in the table.
 * The existing SA is not removed if the new one cannot be added.
 *
 * @param existing SA to replace
 * @param state new SA
 * @return 0 on success, error code otherwise
 */
static int sa_table_replace(SAState *existing, SAState *state){
    int err = 0;
    // Need check for in-use?
    
    dprintf(">\n");
    if(existing->keying.state != SA_STATE_ACQUIRE){
        err = -EINVAL;
        goto exit;
    }
    // replace it.
    err = sa_table_add(state);
    if(err) goto exit;
    sa_table_delete(existing);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Allocate an SA.
 *
 * @return SA or NULL
 */
SAState *SAState_alloc(void){
    SAState *state;
    
    dprintf(">\n");
    state = kmalloc(sizeof(SAState), GFP_ATOMIC);
    if(!state) goto exit;
    *state = (SAState){};
    atomic_set(&state->refcount, 1);
    state->lock = SPIN_LOCK_UNLOCKED;
  exit:
    dprintf("< state=%p\n", state);
    return state;
}

/** Create an SA in initial state.
 * It has no spi and its keying state is acquire.
 * It must have a unique id, protocol and address.
 * At some point it should get updated with a complete SA.
 *
 * @param ident SA identifier
 * @param statep return parameter for new SA
 * @return 0 on success, error code otherwise
 */
int SAState_init(SAIdent *ident, SAState **statep){
    int err = 0;
    SAState *state = NULL;
   
    if(ident->spi || !ident->id){
        err = -EINVAL;
        goto exit;
    }
    state = SAState_alloc();
    if (!state){
        err = -ENOMEM;
        goto exit;
    }
    state->ident = *ident;
    state->keying.state = SA_STATE_ACQUIRE;
  exit:
    return err;
}

/** Create a complete SA, with spi and cipher suite.
 *
 * @param info SA parameters
 * @param statep return parameter for new SA
 * @return 0 on success, error code otherwise
 */
int SAState_create(SAInfo *info, SAState **statep){
    int err = 0;
    SAState *state = NULL;

    dprintf(">\n");
    state = SAState_alloc();
    if (!state){
        err = -ENOMEM;
        goto exit;
    }
    state->ident = info->ident;
    state->limits = info->limits;
    state->digest = info->digest;
    state->cipher = info->cipher;
    state->compress = info->compress;
    state->security = info->security;
    err = SAType_get(state->ident.protocol, &state->type);
    if (err) goto exit;
    err = state->type->init(state, NULL);
    if (err) goto exit;
    state->keying.state = SA_STATE_VALID;
  exit:
    if(err){
        SAState_decref(state);
        state = NULL;
    }
    *statep = state;
    dprintf("< err=%d\n", err);
    return err;
}

/** Create an SA for the given spi etc.
 * For now we fix the cipher suite and the keys.
 * Digest is SHA1 HMAC with a 128-bit key.
 * Cipher is AES (Rijndael) in CBC mode with a 128-bit key.
 *
 * The cipher suite and keys should really come from policy, with the
 * possibility of negotiating them with the peer (using IKE).
 * Negotiation creates difficulties though - because the SA cannot
 * be created immediately we have to be able to queue packets
 * while the SA is being negotiated.
 *
 * @param spi spi
 * @param protocol protocol
 * @param addr address
 * @param sa return parameter for SA
 * @return 0 on success, error code otherwise
 */
int sa_create(int security, u32 spi, u32 protocol, u32 addr, SAState **sa){
    int err = 0;
    SAInfo info = {};
    char *digest_name = "sha1";
    char *digest_key = "0123456789abcdef";
    int digest_key_n = strlen(digest_key);
    char *cipher_name= "aes";
    char *cipher_key = "0123456789ABCDEF";
    int cipher_key_n = strlen(cipher_key);

    dprintf("> security=%d spi=%u protocol=%u addr=" IPFMT "\n",
            security, spi, protocol, NIPQUAD(addr));
    if(!spi){
        spi = generate_spi(0, 0, protocol, addr);
    }
    dprintf("> info...\n");
    info.ident.id = sa_id++;
    info.ident.spi = spi;
    info.ident.protocol = protocol;
    info.ident.addr = addr;
    info.security = security;

    //sa_algorithm_probe_all();

    dprintf("> digest name=%s key_n=%d\n", digest_name, digest_key_n);
    strcpy(info.digest.name, digest_name);
    info.digest.bits = digest_key_n * 8;
    memcpy(info.digest.key, digest_key, digest_key_n);

    if(security & SA_CONF){
        dprintf("> cipher name=%s key_n=%d\n", cipher_name, cipher_key_n);
        strcpy(info.cipher.name, cipher_name);
        info.cipher.bits = cipher_key_n * 8;
        memcpy(info.cipher.key, cipher_key, cipher_key_n);
    } else {
        dprintf("> cipher name=%s key_n=%d\n", "cipher_null", 0);
        strcpy(info.cipher.name, "cipher_null");
        info.cipher.bits = 0;
        memset(info.cipher.key, 0, sizeof(info.cipher.key));
    }

    err = sa_set(&info, 0, sa);
    dprintf("< err=%d\n", err);
    return err;
}

/** Create or update an SA.
 * The SA is added to the table.
 *
 * @param info SA parameters
 * @param update create if zero, update otherwise
 * @return 0 on success, error code otherwise
 */
int sa_set(SAInfo *info, int update, SAState **val){
    int err = 0;
    SAState *state = NULL;
    SAState *existing = NULL;

    dprintf("> info=%p update=%d val=%p\n", info, update, val);
    existing = sa_table_lookup_id(info->ident.id);
    if(update && !existing){
        err = -ENOENT;
    } else if(!update && existing){
        err = -EINVAL;
    }
    if(err) goto exit;
    err = SAState_create(info, &state);
    if (err) goto exit;
    if(existing){
        err = sa_table_replace(existing, state);
    } else {
        err = sa_table_add(state);
    }
  exit:
    if(existing) SAState_decref(existing);
    if(val && !err){
        *val = state;
    } else {
        SAState_decref(state);
    }
    dprintf("< err=%d\n", err);
    return err;
}

/** Delete an SA. Removes it from the SA table.
 * It is an error if no SA with the given id exists.
 *
 * @param id SA id
 * @return 0 on success, error code otherwise
 */
int sa_delete(int id){
    int err = 0;
    SAState *state;
    state = sa_table_lookup_id(id);
    if (!state){
        err = -ENOENT;
        goto exit;
    }
    sa_table_delete(state);
    SAState_decref(state);
  exit:
    return err;
}
/** Determine ESP security mode for a new SA.
 *
 * @param spi incoming spi
 * @param protocol incoming protocol
 * @param addr source address
 * @return security level or negative error code
 *
 * @todo Need to check spi, and do some lookup for security params.
 */
int vnet_sa_security(u32 spi, int protocol, u32 addr){
    extern int vnet_security_default;
    int security = vnet_security_default;
    dprintf("< security=%x\n", security);
    return security;
}

/** Create a new SA for incoming traffic.
 *
 * @param spi incoming spi
 * @param protocol incoming protocol
 * @param addr source address
 * @param sa return parameter for SA
 * @return 0 on success, error code otherwise
 */
int vnet_sa_create(u32 spi, int protocol, u32 addr, SAState **sa){
    int err = 0;
    int security = vnet_sa_security(spi, protocol, addr);
    if(security < 0){
        err = security;
        goto exit;
    }
    err = sa_create(security, spi, protocol, addr, sa);
  exit:
    return err;
}
/** Open function for SA tunnels.
 *
 * @param tunnel to open
 * @return 0 on success, error code otherwise
 */
static int sa_tunnel_open(Tunnel *tunnel){
    int err = 0;
    //dprintf(">\n");
    //dprintf("< err=%d\n", err);
    return err;
}

/** Close function for SA tunnels.
 *
 * @param tunnel to close (OK if null)
 */
static void sa_tunnel_close(Tunnel *tunnel){
    SAState *sa;
    if(!tunnel) return;
    sa = tunnel->data;
    if(!sa) return;
    SAState_decref(sa);
    tunnel->data = NULL;
}

/** Packet send function for SA tunnels.
 *
 * @param tunnel to send on
 * @param skb packet to send
 * @return 0 on success, negative error code on error
 */
static int sa_tunnel_send(Tunnel *tunnel, struct sk_buff *skb){
    int err = -EINVAL;
    SAState *sa;
    if(!tunnel){
        wprintf("> Null tunnel!\n");
        goto exit;
    }
    sa = tunnel->data;
    if(!sa){
        wprintf("> Null SA!\n");
        goto exit;
    }
    err = SAState_send(sa, skb, tunnel->base);
  exit:
    return err;
}

/** Functions used by SA tunnels. */
static TunnelType _sa_tunnel_type = {
    .name	= "SA",
    .open	= sa_tunnel_open,
    .close	= sa_tunnel_close,
    .send 	= sa_tunnel_send
};

/** Functions used by SA tunnels. */
TunnelType *sa_tunnel_type = &_sa_tunnel_type;

int sa_tunnel_create(Vnet *info, VarpAddr *addr, Tunnel *base, Tunnel **tunnel){
    int err = 0;
    SAState *sa = NULL;
    //FIXME: Assuming IPv4 for now.
    u32 ipaddr = addr->u.ip4.s_addr;
    err = Tunnel_create(sa_tunnel_type, &info->vnet, addr, base, tunnel);
    if(err) goto exit;
    err = sa_create(info->security, 0, IPPROTO_ESP, ipaddr, &sa);
    if(err) goto exit;
    (*tunnel)->data = sa;
  exit:
    return err;
}
