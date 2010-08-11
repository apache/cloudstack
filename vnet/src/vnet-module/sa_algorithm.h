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
#ifndef __VNET_SA_ALGORITHM_H__
#define __VNET_SA_ALGORITHM_H__

#include <linux/types.h>
#include <linux/pfkeyv2.h>

typedef struct SADigestInfo {
    u16 icv_truncbits;
    u16 icv_fullbits;
} SADigestInfo;

typedef struct SACipherInfo {
    u16 blockbits;
    u16 defkeybits;
} SACipherInfo;

typedef struct SACompressInfo {
    u16 threshold;
} SACompressInfo;

typedef struct SAAlgorithm {
    char *name;
    u8 available;
    union {
        SADigestInfo digest;
        SACipherInfo cipher;
        SACompressInfo compress;
    } info;
    struct sadb_alg alg;
} SAAlgorithm;

extern SAAlgorithm *sa_digest_by_id(int alg_id);
extern SAAlgorithm *sa_cipher_by_id(int alg_id);
extern SAAlgorithm *sa_compress_by_id(int alg_id);
extern SAAlgorithm *sa_digest_by_name(char *name);
extern SAAlgorithm *sa_cipher_by_name(char *name);
extern SAAlgorithm *sa_compress_by_name(char *name);
extern SAAlgorithm *sa_digest_by_index(unsigned int idx);
extern SAAlgorithm *sa_cipher_by_index(unsigned int idx);
extern SAAlgorithm *sa_compress_by_index(unsigned int idx);
extern void sa_algorithm_probe_all(void);

#define MAX_KEY_BITS 512

#endif /* ! __VNET_SA_ALGORITHM_H__ */
