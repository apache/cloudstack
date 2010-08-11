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
#ifndef __VNET_ESP_H__
#define __VNET_ESP_H__

#ifdef __KERNEL__
#include <linux/config.h>
#include <linux/types.h>
#include <linux/crypto.h>

#else

#include "sys_kernel.h"

struct crypto_tfm;

#endif

/** Header used by IPSEC ESP (Encapsulated Security Payload). */
typedef struct ESPHdr {
    /** The spi (security parameters index). */
    u32 spi;
    /** Sequence number. */
    u32 seq;
    /* Variable length data (depends on crypto suite).
       Mind the 64 bit alignment! */
    u8  data[0];
} ESPHdr;

/** Padding trailer used by IPSEC ESP.
 * Follows the padding itself with the padding length and the
 * protocol being encapsulated.
 */
typedef struct ESPPadding {
    u8 pad_n;
    u8 protocol;
} ESPPadding;

/** Size of the esp header (spi and seq). */
static const int ESP_HDR_N = sizeof(ESPHdr);

/** Size of the esp pad and next protocol field. */
static const int ESP_PAD_N = sizeof(ESPPadding);

enum {
    SASTATE_VOID,
    SASTATE_ACQUIRE,
    SASTATE_VALID,
    SASTATE_ERROR,
    SASTATE_EXPIRED,
    SASTATE_DEAD,
};

struct ESPState;

/** A cipher instance. */
typedef struct ESPCipher {
    /** Cipher key. */
    u8 *key;
    /** Key size (bytes). */
    int key_n;
    /** Initialization vector (IV). */
    u8 *iv;
    /** IV size (bytes). */
    int iv_n;
    /** Block size for padding (bytes). */
    int pad_n;
    /** Cipher block size (bytes). */
    int block_n;
    /** Cipher crypto transform. */
    struct crypto_tfm *tfm;
} ESPCipher;

/** A digest instance. */
typedef struct ESPDigest {
    /** Digest key. */
    u8 *key;
    /** Key size (bytes) */
    int key_n;
    /** ICV size used (bytes). */
    u8 icv_n;
    /** Full ICV size when computed (bytes). */
    u8 icv_full_n;
    /** Working storage for computing ICV. */
    u8 *icv_tmp;
    /** Function used to compute ICV (e.g. HMAC). */
    void (*icv)(struct ESPState *esp,
                struct sk_buff *skb,
                int offset,
                int len,
                u8 *icv);
    /** Digest crypto transform (e.g. SHA). */
    struct crypto_tfm *tfm;
} ESPDigest;

typedef struct ESPState {
    struct ESPCipher cipher;
    struct ESPDigest digest;
} ESPState;

extern int esp_module_init(void);
extern void esp_module_exit(void);

#endif /* !__VNET_ESP_H__ */
