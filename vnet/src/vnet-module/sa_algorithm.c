/* 
 * Copyright (c) 2002 James Morris <jmorris@intercode.com.au>
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
#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/string.h>
#include <linux/crypto.h>
#include <linux/sched.h>
//#include <asm/softirq.h>

#include <sa_algorithm.h>

#define MODULE_NAME "IPSEC"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file Tables of supported IPSEC algorithms.
 * Has tables for digests, ciphers and compression algorithms.
 */

/*
 * Algorithms supported by IPsec.  These entries contain properties which
 * are used in key negotiation and sa processing, and are used to verify
 * that instantiated crypto transforms have correct parameters for IPsec
 * purposes.
 */

/** Digests. */
static SAAlgorithm digest_alg[] = {
    {
        .name = "digest_null",
        .info = {
            .digest = {
                .icv_truncbits = 0,
                .icv_fullbits = 0,
            }
        },
        .alg = {
            .sadb_alg_id = SADB_X_AALG_NULL,
            .sadb_alg_ivlen = 0,
            .sadb_alg_minbits = 0,
            .sadb_alg_maxbits = 0
        }
    },
    {
	.name = "md5",
	.info = { .digest = {
            .icv_truncbits = 96,
            .icv_fullbits = 128,
        } },
        .alg = {
            .sadb_alg_id = SADB_AALG_MD5HMAC,
            .sadb_alg_ivlen = 0,
            .sadb_alg_minbits = 128,
            .sadb_alg_maxbits = 128
	}
    },
    {
	.name = "sha1",
	.info = {
            .digest = {
                .icv_truncbits = 96,
                .icv_fullbits = 160,
            }
	},
	.alg = {
            .sadb_alg_id = SADB_AALG_SHA1HMAC,
            .sadb_alg_ivlen = 0,
            .sadb_alg_minbits = 160,
            .sadb_alg_maxbits = 160
	}
    },
    {
	.name = "sha256",
	.info = {
            .digest = {
                .icv_truncbits = 128,
                .icv_fullbits = 256,
            }
	},
	.alg = {
            .sadb_alg_id = SADB_X_AALG_SHA2_256HMAC,
            .sadb_alg_ivlen = 0,
            .sadb_alg_minbits = 256,
            .sadb_alg_maxbits = 256
	}
    },
/*     { */
/*         .name = "ripemd160", */
/*         .info = { */
/*             .digest = { */
/*                 .icv_truncbits = 96, */
/*                 .icv_fullbits = 160, */
/*             } */
/* 	}, */
/*         .alg = { */
/*             .sadb_alg_id = SADB_X_AALG_RIPEMD160HMAC, */
/*             .sadb_alg_ivlen = 0, */
/*             .sadb_alg_minbits = 160, */
/*             .sadb_alg_maxbits = 160 */
/* 	} */
/*     }, */
    { /* Terminator */ }
};

/** Ciphers. */
static SAAlgorithm cipher_alg[] = {
    {
	.name = "cipher_null",
        .info = {
            .cipher = {
                .blockbits = 8,
                .defkeybits = 0,
            }
	},
        .alg = {
            .sadb_alg_id =	SADB_EALG_NULL,
            .sadb_alg_ivlen = 0,
            .sadb_alg_minbits = 0,
            .sadb_alg_maxbits = 0
	}
    },
    {
        .name = "des",
        .info = {
            .cipher = {
                .blockbits = 64,
                .defkeybits = 64,
            }
	},
        .alg = {
            .sadb_alg_id = SADB_EALG_DESCBC,
            .sadb_alg_ivlen = 8,
            .sadb_alg_minbits = 64,
            .sadb_alg_maxbits = 64
	}
    },
    {
	.name = "des3_ede",
	.info = {
            .cipher = {
                .blockbits = 64,
                .defkeybits = 192,
            }
	},
        .alg = {
            .sadb_alg_id = SADB_EALG_3DESCBC,
            .sadb_alg_ivlen = 8,
            .sadb_alg_minbits = 192,
            .sadb_alg_maxbits = 192
	}
    },
/*     { */
/* 	.name = "cast128", */ //cast5?
/* 	.info = { */
/*             .cipher = { */
/*                 .blockbits = 64, */
/*                 .defkeybits = 128, */
/*             } */
/* 	}, */
/* 	.alg = { */
/*             .sadb_alg_id = SADB_X_EALG_CASTCBC, */
/*             .sadb_alg_ivlen = 8, */
/*             .sadb_alg_minbits = 40, */
/*             .sadb_alg_maxbits = 128 */
/* 	} */
/*     }, */
    {
	.name = "blowfish",
        .info = {
            .cipher = {
                .blockbits = 64,
                .defkeybits = 128,
            }
	},
	.alg = {
            .sadb_alg_id = SADB_X_EALG_BLOWFISHCBC,
            .sadb_alg_ivlen = 8,
            .sadb_alg_minbits = 40,
            .sadb_alg_maxbits = 448
	}
    },
    {
	.name = "aes",
	.info = {
            .cipher = {
                .blockbits = 128,
                .defkeybits = 128,
            }
	},
	.alg = {
            .sadb_alg_id = SADB_X_EALG_AESCBC,
            .sadb_alg_ivlen = 8,
            .sadb_alg_minbits = 128,
            .sadb_alg_maxbits = 256
	}
    },
    { /* Terminator */ }
};

/** Compressors. */
static SAAlgorithm compress_alg[] = {
    {
	.name = "deflate",
	.info = {
            .compress = {
                .threshold = 90,
            }
	},
	.alg = { .sadb_alg_id = SADB_X_CALG_DEFLATE }
    },
/*     { */
/* 	.name = "lzs", */
/* 	.info = { */
/*             .compress = { */
/*                 .threshold = 90, */
/*             } */
/* 	}, */
/* 	.alg = { .sadb_alg_id = SADB_X_CALG_LZS } */
/*     }, */
/*     { */
/* 	.name = "lzjh", */
/* 	.info = { */
/*             .compress = { */
/*                 .threshold = 50, */
/*             } */
/* 	}, */
/* 	.alg = { .sadb_alg_id = SADB_X_CALG_LZJH } */
/*     }, */
    { /* Terminator */ }
};

static SAAlgorithm *sa_algorithm_by_id(SAAlgorithm *algo, int alg_id) {
    for( ; algo && algo->name; algo++){
        if (algo->alg.sadb_alg_id == alg_id) {
            return (algo->available ? algo : NULL);
        }
    }
    return NULL;
}


static SAAlgorithm *sa_algorithm_by_name(SAAlgorithm *algo, char *name) {
	if (!name) return NULL;
	for( ; algo && algo->name; algo++){
		if (strcmp(name, algo->name) == 0) {
                    return (algo->available ? algo : NULL);
                }
	}
	return NULL;
}

SAAlgorithm *sa_digest_by_id(int alg_id) {
    return sa_algorithm_by_id(digest_alg, alg_id);
}

SAAlgorithm *sa_cipher_by_id(int alg_id) {
    return sa_algorithm_by_id(cipher_alg, alg_id);
}

SAAlgorithm *sa_compress_by_id(int alg_id) {
    return sa_algorithm_by_id(compress_alg, alg_id);
}

SAAlgorithm *sa_digest_by_name(char *name) {
    return sa_algorithm_by_name(digest_alg, name);
}

SAAlgorithm *sa_cipher_by_name(char *name) {
    return sa_algorithm_by_name(cipher_alg, name);
}

SAAlgorithm *sa_compress_by_name(char *name) {
    return sa_algorithm_by_name(compress_alg, name);
}

SAAlgorithm *sa_digest_by_index(unsigned int idx) {
    return digest_alg + idx;
}

SAAlgorithm *sa_cipher_by_index(unsigned int idx) {
    return cipher_alg + idx;
}

SAAlgorithm *sa_compress_by_index(unsigned int idx) {
    return compress_alg + idx;
}

static void sa_algorithm_probe(SAAlgorithm *algo){
    int status;
    dprintf("> algo=%p\n", algo); 
    for( ; algo && algo->name; algo++){
        dprintf("> algorithm %s...\n", algo->name);
        status = crypto_alg_available(algo->name, 0);
        dprintf("> algorithm %s status=%d\n",algo->name, status); 
        if (algo->available != status){
            algo->available = status;
        }
    }
    dprintf("<\n"); 
}

/** Crypto api is broken. When an unregistered algorithm is requested it
 * tries to load a module of the same name. But not all algorithms are
 * defined by modules of the same name.
 */
static char *crypto_modules[] = {
    "aes",
    //"arc4",
    "blowfish",
    //"cast5",
    //"cast6",
    "crypto_null",
    "des",
    //"md4",
    "md5",
    //"serpent",
    "sha1",
    "sha256",
    //"sha512",
    //"twofish",
    NULL
};

#include <linux/kmod.h>

static void sa_module_probe(char **modules){
    char **p;
    dprintf(">\n");
    for(p = modules; *p; p++){
        dprintf("> %s\n", *p);
	request_module(*p);
    }
    dprintf("<\n");
}

/**
 * Probe for the availability of crypto algorithms, and set the available
 * flag for any algorithms found on the system.  This is typically called by
 * pfkey during userspace SA add, update or register.
 */
void sa_algorithm_probe_all(void){
    dprintf("> \n"); 
    //BUG_ON(in_softirq());
    sa_module_probe(crypto_modules);
    sa_algorithm_probe(digest_alg);
    sa_algorithm_probe(cipher_alg);
    sa_algorithm_probe(compress_alg);
    dprintf("<\n"); 
}
