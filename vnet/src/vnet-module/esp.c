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
#include <linux/config.h>
#include <linux/module.h>
#include <linux/types.h>
#include <linux/sched.h>
#include <linux/kernel.h>
#include <asm/uaccess.h>

#include <linux/init.h>

#include <linux/version.h>

#include <linux/skbuff.h>
#include <linux/netdevice.h>
#include <linux/net.h>
#include <linux/in.h>
#include <linux/inet.h>

#include <net/ip.h>
#include <net/protocol.h>
#include <net/route.h>

#include <linux/if_ether.h>
#include <linux/icmp.h>

#include <asm/scatterlist.h>
#include <linux/crypto.h>
#include <linux/pfkeyv2.h>
#include <linux/random.h>

#include <esp.h>
#include <sa.h>
#include <sa_algorithm.h>
#include <tunnel.h>
#include <vnet.h>
#include <skb_util.h>
#include <skb_context.h>

static const int DEBUG_ICV = 0;

#define MODULE_NAME "IPSEC"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

#if ((LINUX_VERSION_CODE != KERNEL_VERSION(2,6,18) ) || !defined(CONFIG_CRYPTO_HMAC))
#warning No esp transform - 

int __init esp_module_init(void){
    return 0;
}

void __exit esp_module_exit(void){
}

#else

/* Outgoing packet:                            [ eth | ip | data ]
 * After etherip:        [ eth2 | ip2 |  ethip | eth | ip | data ]
 * After esp   :   [ eth2 | ip2 | esp | {ethip | eth | ip | data} | pad | icv ]
 *                                                        ^     +
 * The curly braces { ... } denote encryption.
 * The esp header includes the fixed esp headers and the iv (variable size).
 * The point marked ^ does not move. To the left is in the header, to the right
 * is in the frag. Remember that all outgoing skbs (from domains) have 1 frag.
 * Data after + is added by esp, using an extra frag.
 *
 * Incoming as above.
 * After decrypt:  [ eth2 | ip2 | esp |  ethip | eth | ip | data  | pad | icv ]
 * Trim tail:      [ eth2 | ip2 | esp |  ethip | eth | ip | data ]
 * Drop hdr:             [ eth2 | ip2 |  ethip | eth | ip | data ]
 *                                    ^
 * The point marked ^ does not move. Incoming skbs are linear (no frags).
 * The tail is trimmed by adjusting skb->tail and len.
 * The esp hdr is dropped by using memmove to move the headers and
 * adjusting the skb pointers.
 *
 * todo: Now this code is in linux we can't assume 1 frag for outbound skbs,
 * or (maybe) that memmove is safe on inbound.
 */

/** Round n up to a multiple of block.
 * If block is less than 2 does nothing.
 * Otherwise assume block is a power of 2.
 *
 * @param n to round up
 * @param block size to round to a multiple of
 * @return rounded value
 */
static inline int roundupto(int n, int block){
    if(block <= 1) return n;
    block--;
    return (n + block) & ~block;
}

/** Check if n is a multiple of block.
 * If block is less than 2 returns 1.
 * Otherwise assumes block is a power of 2.
 *
 * @param n to check
 * @param block block size
 * @return 1 if a multiple, 0 otherwise
 */
static inline int multipleof(int n, int block){
    if(block <= 1) return 1;
    block--;
    return !(n & block);
}

/** Convert from bits to bytes.
 *
 * @param n number of bits
 * @return number of bytes
 */
static inline int bits_to_bytes(int n){
    return n / 8;
}


/** Insert esp padding at the end of an skb.
 * Inserts padding bytes, number of padding bytes, protocol number.
 *
 * @param skb skb
 * @param offset offset from skb end to where padding should end
 * @param extra_n total amount of padding
 * @param protocol protocol number (from original ip hdr)
 * @return 0 on success, error code otherwise
 */
static int esp_sa_pad(struct sk_buff *skb, int offset, int extra_n,
                      unsigned char protocol){
    int err;
    char *data;
    int pad_n = extra_n - ESP_PAD_N;
    int i;
    char buf[extra_n];

    data = buf;
    for(i = 1; i <= pad_n; i++){
        *data++ = i;
    }
    *data++ = pad_n;
    *data++ = protocol;
    err = skb_put_bits(skb, skb->len - offset - extra_n, buf, extra_n);
    return err;
}

/** Encrypt skb. Skips esp header and iv.
 * Assumes skb->data points at esp header.
 *
 * @param esp esp state
 * @parm esph esp header
 * @param skb packet
 * @param head_n size of esp header and iv
 * @param iv_n size of iv
 * @param text_n size of ciphertext
 * @return 0 on success, error code otherwise
 */
static int esp_sa_encrypt(ESPState *esp, ESPHdr *esph, struct sk_buff *skb,
                   int head_n, int iv_n, int text_n){
    int err = 0;
    int sg_n = skb_shinfo(skb)->nr_frags + 1;
    struct scatterlist sg[sg_n];

    err = skb_scatterlist(skb, sg, &sg_n, head_n, text_n);
    if(err) goto exit;
    if(iv_n){
        crypto_cipher_set_iv(esp->cipher.tfm, esp->cipher.iv, iv_n);
    }
    crypto_cipher_encrypt(esp->cipher.tfm, sg, sg, text_n);
    if(iv_n){
        memcpy(esph->data, esp->cipher.iv, iv_n);
        crypto_cipher_get_iv(esp->cipher.tfm, esp->cipher.iv, iv_n);
    }
  exit:
    return err;
}

/** Decrypt skb. Skips esp header and iv.
 * Assumes skb->data points at esp header.
 *
 * @param esp esp state
 * @parm esph esp header
 * @param skb packet
 * @param head_n size of esp header and iv
 * @param iv_n size of iv
 * @param text_n size of ciphertext
 * @return 0 on success, error code otherwise
 */
static int esp_sa_decrypt(ESPState *esp, ESPHdr *esph, struct sk_buff *skb,
                   int head_n, int iv_n, int text_n){
    int err = 0;
    int sg_n = skb_shinfo(skb)->nr_frags + 1;
    struct scatterlist sg[sg_n];

    err = skb_scatterlist(skb, sg, &sg_n, head_n, text_n);
    if(err) goto exit;
    if(iv_n){
        crypto_cipher_set_iv(esp->cipher.tfm, esph->data, iv_n);
    }
    crypto_cipher_decrypt(esp->cipher.tfm, sg, sg, text_n);
  exit:
    return err;
}

/** Compute icv. Includes esp header, iv and ciphertext.
 * Assumes skb->data points at esp header.
 *
 * @param esp esp state
 * @param skb packet
 * @param digest_n number of bytes to digest
 * @param icv_n size of icv
 * @return 0 on success, error code otherwise
 */
static int esp_sa_digest(ESPState *esp, struct sk_buff *skb, int digest_n, int icv_n){
    int err = 0;
    u8 icv[icv_n];
    
    if(DEBUG_ICV){
        dprintf("> skb digest_n=%d icv_n=%d\n", digest_n, icv_n);
        skb_print_bits("esp", skb, 0, digest_n);
    }
    memset(icv, 0, icv_n);
    esp->digest.icv(esp, skb, 0, digest_n, icv);
    skb_put_bits(skb, digest_n, icv, icv_n);
    return err;
}

/** Check the icv and trim it from the skb tail.
 *
 * @param sa sa state
 * @param esp esp state
 * @param esph esp header
 * @param skb packet
 * @return 0 on success, error code otherwise
 */
static int esp_check_icv(SAState *sa, ESPState *esp, ESPHdr *esph, struct sk_buff *skb){
    int err = 0;
    int icv_n = esp->digest.icv_n;
    int digest_n = skb->len - icv_n;
    u8 icv_skb[icv_n];
    u8 icv_new[icv_n];

    dprintf(">\n");
    if(DEBUG_ICV){
        dprintf("> skb len=%d digest_n=%d icv_n=%d\n",
                skb->len, digest_n, icv_n);
        skb_print_bits("esp", skb, 0, skb->len);
    }
    if(skb_copy_bits(skb, digest_n, icv_skb, icv_n)){
        wprintf("> Error getting icv from skb\n");
        goto exit;
    }
    esp->digest.icv(esp, skb, 0, digest_n, icv_new);
    if(DEBUG_ICV){
        dprintf("> len=%d icv_n=%d", digest_n, icv_n);
        printk("\nskb="); buf_print(icv_skb, icv_n);
        printk("new="); buf_print(icv_new, icv_n);
    }
    if(unlikely(memcmp(icv_new, icv_skb, icv_n))){
        wprintf("> ICV check failed!\n");
        err = -EINVAL;
        sa->counts.integrity_failures++;
        goto exit;
    }
    skb_trim_tail(skb, icv_n);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Send a packet via an ESP SA.
 *
 * @param sa SA state
 * @param skb packet to send
 * @param tunnel underlying tunnel
 * @return 0 on success, negative error code otherwise
 */
static int esp_sa_send(SAState *sa, struct sk_buff *skb, Tunnel *tunnel){
    int err = 0;
    int ip_n;           // Size of ip header.
    int plaintext_n;	// Size of plaintext.
    int ciphertext_n;   // Size of ciphertext (including padding).
    int extra_n;        // Extra bytes needed for ciphertext.
    int icv_n = 0;      // Size of integrity check value (icv).
    int iv_n = 0;       // Size of initialization vector (iv).
    int head_n;         // Size of esp header and iv.
    int tail_n;         // Size of esp trailer: padding and icv.
    ESPState  *esp;
    ESPHdr *esph;

    dprintf(">\n");
    esp = sa->data;
    ip_n = (skb->nh.iph->ihl << 2);
    // Assuming skb->data points at ethernet header, exclude ethernet
    // header and IP header.
    plaintext_n = skb->len - ETH_HLEN - ip_n;
    // Add size of padding fields.
    ciphertext_n = roundupto(plaintext_n + ESP_PAD_N, esp->cipher.block_n);
    if(esp->cipher.pad_n > 0){
        ciphertext_n = roundupto(ciphertext_n, esp->cipher.pad_n);
    }
    extra_n = ciphertext_n - plaintext_n;
    iv_n = esp->cipher.iv_n;
    icv_n = esp->digest.icv_n;
    dprintf("> len=%d plaintext=%d ciphertext=%d extra=%d\n",
            skb->len, plaintext_n, ciphertext_n, extra_n);
    dprintf("> iv=%d icv=%d\n", iv_n, icv_n);
    skb_print_bits("iv", skb, 0, skb->len);

    // Add headroom for esp header and iv, tailroom for the ciphertext
    // and icv.
    head_n = ESP_HDR_N + iv_n;
    tail_n = extra_n + icv_n;
    err = skb_make_room(&skb, skb, head_n, tail_n);
    if(err) goto exit;
    dprintf("> skb=%p\n", skb);
    // Move the headers up to make space for the esp header.  We can
    // use memmove() since all this data fits in the skb head.
    // todo: Can't assume this anymore?
    dprintf("> header push...\n");
    __skb_push(skb, head_n);
    if(0 && skb->mac.raw){
        dprintf("> skb->mac=%p\n", skb->mac.raw);
        dprintf("> ETH header pull...\n");
        memmove(skb->data, skb->mac.raw, ETH_HLEN);
        skb->mac.raw = skb->data; 
        skb_pull_vn(skb, ETH_HLEN);
    }
    dprintf("> IP header pull...\n");
    memmove(skb->data, skb->nh.raw, ip_n);
    skb->nh.raw = skb->data;
    skb_pull_vn(skb, ip_n);
    esph = (void*)skb->data;
    // Add spi and sequence number.
    esph->spi = sa->ident.spi;
    esph->seq = htonl(++sa->replay.send_seq);
    // Insert the padding bytes: extra bytes less the pad fields
    // themselves.
    dprintf("> esp_sa_pad ...\n");
    esp_sa_pad(skb, icv_n, extra_n, skb->nh.iph->protocol);
    if(sa->security & SA_CONF){
        dprintf("> esp_sa_encrypt...\n");
        err = esp_sa_encrypt(esp, esph, skb, head_n, iv_n, ciphertext_n);
        if(err) goto exit;
    }
    if(icv_n){
        dprintf("> esp_sa_digest...\n");
        err = esp_sa_digest(esp, skb, head_n + ciphertext_n, icv_n);
        if(err) goto exit;
    }
    dprintf("> IP header push...\n");
    __skb_push(skb, ip_n);
    if(0 && skb->mac.raw){
        dprintf("> ETH header push...\n");
        __skb_push(skb, ETH_HLEN);
    }
    // Fix ip header. Adjust length field, set protocol, zero
    // checksum.
    {
        // Total packet length (bytes).
        int tot_len = ntohs(skb->nh.iph->tot_len);
        tot_len += head_n;
        tot_len += tail_n;
        skb->nh.iph->protocol = IPPROTO_ESP;
        skb->nh.iph->tot_len  = htons(tot_len);
        skb->nh.iph->check    = 0;
    }
    err = Tunnel_send(tunnel, skb);
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Release an skb context.
 * Drops the refcount on the SA.
 *
 * @param context to free
 */
static void esp_context_free_fn(SkbContext *context){
    SAState *sa;
    if(!context) return;
    sa = context->data;
    if(!sa) return;
    context->data = NULL;
    SAState_decref(sa);
}   

/** Receive a packet via an ESP SA.
 * Does ESP receive processing (check icv, decrypt), strips
 * ESP header and re-receives.
 *
 * If return 1 the packet has been freed.
 * If return <= 0 the caller must free.
 *
 * @param sa SA
 * @param skb packet
 * @return >= 0 on success, negative protocol otherwise
 */
static int esp_sa_recv(SAState *sa, struct sk_buff *skb){
    int err = -EINVAL;
    int mine = 0;
    int vnet = 0; //todo: fixme - need to record skb vnet somewhere
    ESPState *esp;
    ESPHdr *esph;
    ESPPadding *pad;
    int block_n;	// Cipher blocksize.
    int icv_n;          // Size of integrity check value (icv).
    int iv_n;           // Size of initialization vector (iv).
    int text_n;         // Size of text (ciphertext or plaintext).
    int head_n;         // Size of esp header and iv.

    dprintf("> skb=%p\n", skb);
    // Assumes skb->data points at esp hdr.
    esph = (void*)skb->data;
    esp = sa->data;
    block_n = crypto_tfm_alg_blocksize(esp->cipher.tfm);
    icv_n = esp->digest.icv_n;
    iv_n = esp->cipher.iv_n;
    head_n = ESP_HDR_N + iv_n;
    text_n = skb->len - head_n - icv_n;
    if(text_n < ESP_PAD_N || !multipleof(text_n, block_n)){
        wprintf("> Invalid size: text_n=%d tfm:block_n=%d esp:block_n=%d\n",
                text_n, block_n, esp->cipher.block_n);
        goto exit;
    }
    if(icv_n){
        err = esp_check_icv(sa, esp, esph, skb);
        if(err) goto exit;
    }
    mine = 1;
    if(sa->security & SA_CONF){
        err = esp_sa_decrypt(esp, esph, skb, head_n, iv_n, text_n);
        if(err) goto exit;
    }
    // Strip esp header by moving the other headers down.
    //todo Maybe not safe to do this anymore.
    memmove(skb->mac.raw + head_n, skb->mac.raw, (skb->data - skb->mac.raw));
    skb->mac.raw += head_n;
    skb->nh.raw  += head_n;
    // Move skb->data back to ethernet header.
    // Do in 2 moves to ensure offsets are +ve,
    // since args to skb_pull/skb_push are unsigned.
    skb_pull_vn(skb, head_n);
    __skb_push(skb, skb->data - skb->mac.raw);
    // After this esph is invalid.
    esph = NULL;
    // Trim padding, restore protocol in IP header.
    pad = skb_trim_tail(skb, ESP_PAD_N);
    text_n -= ESP_PAD_N;
    if((pad->pad_n > 255) | (pad->pad_n > text_n)){
        wprintf("> Invalid padding: pad_n=%d text_n=%d\n", pad->pad_n, text_n);
        goto exit;
    }
    skb_trim_tail(skb, pad->pad_n);
    skb->nh.iph->protocol = pad->protocol;
    err = skb_push_context(skb, vnet, sa->ident.addr, IPPROTO_ESP,
                           sa, esp_context_free_fn);
    if(err) goto exit;
    // Increase sa refcount now the skb context refers to it.
    // Refcount is decreased by esp_context_free_fn.
    SAState_incref(sa);
    // Deliver skb to be received by network code.
    // Not safe to refer to the skb after this.
    // todo: return -skb->nh.iph->protocol instead?
    netif_rx(skb);
  exit:
    if(mine){
        if(err < 0){
            kfree_skb(skb);
        }
        err = 1;
    }
    dprintf("< skb=%p err=%d\n", skb, err);
    return err;
}

/** Estimate the packet size for some data using ESP processing.    
 *
 * @param sa ESP SA
 * @param data_n data size
 * @return size after ESP processing
 */
static u32 esp_sa_size(SAState *sa, int data_n){
    // Even in transport mode have to round up to blocksize.
    // Have to add some padding for alignment even if pad_n is zero.
    ESPState *esp = sa->data;
    
    data_n = roundupto(data_n + ESP_PAD_N, esp->cipher.block_n);
    if(esp->cipher.pad_n > 0){
        data_n = roundupto(data_n, esp->cipher.pad_n);
    }
    data_n += esp->digest.icv_n;
    //data_n += esp->cipher.iv_n;
    data_n += ESP_HDR_N;
    return data_n;
}

/** Compute an icv using HMAC digest.
 *
 * @param esp ESP state
 * @param skb packet to digest
 * @param offset offset to start at
 * @param len number of bytes to digest
 * @param icv return parameter for ICV
 * @return 0 on success, negative error code otherwise
 */
static inline void esp_hmac_digest(ESPState *esp, struct sk_buff *skb,
                                   int offset, int len, u8 *icv){
    int err = 0;
    struct crypto_tfm *digest = esp->digest.tfm;
    char *icv_tmp = esp->digest.icv_tmp;
    int sg_n = skb_shinfo(skb)->nr_frags + 1;
    struct scatterlist sg[sg_n];

    dprintf("> offset=%d len=%d\n", offset, len);
    memset(icv, 0, esp->digest.icv_n);
    if(DEBUG_ICV){
        dprintf("> key len=%d\n", esp->digest.key_n);
        printk("\nkey=");
        buf_print(esp->digest.key,esp->digest.key_n); 
    }
    crypto_hmac_init(digest, esp->digest.key, &esp->digest.key_n);
    err = skb_scatterlist(skb, sg, &sg_n, offset, len);
    crypto_hmac_update(digest, sg, sg_n);
    crypto_hmac_final(digest, esp->digest.key, &esp->digest.key_n, icv_tmp);
    if(DEBUG_ICV){
        dprintf("> digest len=%d ", esp->digest.icv_n);
        printk("\nval=");
        buf_print(icv_tmp, esp->digest.icv_n);
    }
    memcpy(icv, icv_tmp, esp->digest.icv_n);
    dprintf("<\n");
}

/** Finish up an esp state.
 * Releases the digest, cipher, iv and frees the state.
 *
 * @parma esp state
 */
static void esp_fini(ESPState *esp){
    if(!esp) return;
    if(esp->digest.tfm){
        crypto_free_tfm(esp->digest.tfm);
        esp->digest.tfm = NULL; 
    }
    if(esp->digest.icv_tmp){
        kfree(esp->digest.icv_tmp);
        esp->digest.icv_tmp = NULL;
    }
    if(esp->cipher.tfm){
        crypto_free_tfm(esp->cipher.tfm);
        esp->cipher.tfm = NULL;
    }
    if(esp->cipher.iv){
        kfree(esp->cipher.iv);
        esp->cipher.iv = NULL;
    }
    kfree(esp);
}

/** Release an ESP SA.
 *
 * @param sa ESO SA
 */
static void esp_sa_fini(SAState *sa){
    ESPState *esp;
    if(!sa) return;
    esp = sa->data;
    if(!esp) return;
    esp_fini(esp);
    sa->data = NULL;
}

/** Initialize the cipher for an ESP SA.
 *
 * @param sa ESP SA
 * @param esp ESP state
 * @return 0 on success, negative error code otherwise
 */
static int esp_cipher_init(SAState *sa, ESPState *esp){
    int err = 0; 
    SAAlgorithm *algo = NULL;
    int cipher_mode = CRYPTO_TFM_MODE_CBC;

    dprintf("> sa=%p esp=%p\n", sa, esp);
    dprintf("> cipher=%s\n", sa->cipher.name);
    algo = sa_cipher_by_name(sa->cipher.name);
    if(!algo){
        wprintf("> Cipher unavailable: %s\n", sa->cipher.name);
        err = -EINVAL;
        goto exit;
    }
    esp->cipher.key_n = roundupto(sa->cipher.bits, 8);
    // If cipher is null must use ECB because CBC algo does not support blocksize 1.
    if(strcmp(sa->cipher.name, "cipher_null")){
        cipher_mode = CRYPTO_TFM_MODE_ECB;
    }
    esp->cipher.tfm = crypto_alloc_tfm(sa->cipher.name, cipher_mode);
    if(!esp->cipher.tfm){
        err = -ENOMEM;
        goto exit;
    }
    esp->cipher.block_n = roundupto(crypto_tfm_alg_blocksize(esp->cipher.tfm), 4);
    esp->cipher.iv_n = crypto_tfm_alg_ivsize(esp->cipher.tfm);
    esp->cipher.pad_n = 0;
    if(esp->cipher.iv_n){
        esp->cipher.iv = kmalloc(esp->cipher.iv_n, GFP_KERNEL);
        get_random_bytes(esp->cipher.iv, esp->cipher.iv_n);
    }
    crypto_cipher_setkey(esp->cipher.tfm, esp->cipher.key, esp->cipher.key_n);
    err = 0;
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Initialize the digest for an ESP SA.
 *
 * @param sa ESP SA
 * @param esp ESP state
 * @return 0 on success, negative error code otherwise
 */
static int esp_digest_init(SAState *sa, ESPState *esp){
    int err = 0;
    SAAlgorithm *algo = NULL;
    
    dprintf(">\n");
    esp->digest.key = sa->digest.key;
    esp->digest.key_n = bits_to_bytes(roundupto(sa->digest.bits, 8));
    esp->digest.tfm = crypto_alloc_tfm(sa->digest.name, 0);
    if(!esp->digest.tfm){
        err = -ENOMEM;
        goto exit;
    }
    algo = sa_digest_by_name(sa->digest.name);
    if(!algo){
        wprintf("> Digest unavailable: %s\n", sa->digest.name);
        err = -EINVAL;
        goto exit;
    }
    esp->digest.icv = esp_hmac_digest;
    esp->digest.icv_full_n = bits_to_bytes(algo->info.digest.icv_fullbits);
    esp->digest.icv_n = bits_to_bytes(algo->info.digest.icv_truncbits);
    
    if(esp->digest.icv_full_n != crypto_tfm_alg_digestsize(esp->digest.tfm)){
        err = -EINVAL;
        wprintf("> digest %s, size %u != %hu\n",
                sa->digest.name,
                crypto_tfm_alg_digestsize(esp->digest.tfm),
                esp->digest.icv_full_n);
        goto exit;
    }
    
    esp->digest.icv_tmp = kmalloc(esp->digest.icv_full_n, GFP_KERNEL);
    if(!esp->digest.icv_tmp){
        err = -ENOMEM;
        goto exit;
    }
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Initialize an ESP SA.
 *
 * @param sa ESP SA
 * @param args arguments
 * @return 0 on success, negative error code otherwise
 */
static int esp_sa_init(SAState *sa, void *args){
    int err = 0;
    ESPState *esp = NULL;
    
    dprintf("> sa=%p\n", sa);
    esp = kmalloc(sizeof(*esp), GFP_KERNEL);
    if(!esp){
        err = -ENOMEM;
        goto exit;
    }
    *esp = (ESPState){};
    err = esp_cipher_init(sa, esp);
    if(err) goto exit;
    err = esp_digest_init(sa, esp);
    if(err) goto exit;
    sa->data = esp;
  exit:
    if(err){
        if(esp) esp_fini(esp);
    }
    dprintf("< err=%d\n", err);
    return err;
}

/** SA type for ESP.
 */
static SAType esp_sa_type = {
    .name     = "ESP",
    .protocol = IPPROTO_ESP,
    .init     = esp_sa_init,
    .fini     = esp_sa_fini,
    .size     = esp_sa_size,
    .recv     = esp_sa_recv,
    .send     = esp_sa_send
};

/** Get the ESP header from a packet.
 *
 * @param skb packet
 * @param esph return parameter for header
 * @return 0 on success, negative error code otherwise
 */
static int esp_skb_header(struct sk_buff *skb, ESPHdr **esph){
    int err = 0;
    if(skb->len < ESP_HDR_N){
        err = -EINVAL;
        goto exit;
    }
    *esph = (ESPHdr*)skb->data;
  exit:
    return err;
}

/** Handle an incoming skb with ESP protocol.
 *
 * Lookup spi, if state found hand to the state.
 * If no state, check spi, if ok, create state and pass to it.
 * If spi not ok, drop.
 *
 * Return value convention for protocols:
 * >= 0 Protocol took the packet
 * < 0  A -ve protocol id the packet should be re-received as.
 *
 * So always return >=0 if we took the packet, even if we dropped it.
 * 
 * @param skb packet
 * @return 0 on sucess, negative protocol number otherwise
 */
static int esp_protocol_recv(struct sk_buff *skb){
    int err = 0;
    const int eth_n = ETH_HLEN;
    int ip_n;
    ESPHdr *esph = NULL;
    SAState *sa = NULL;
    u32 addr;
    
    dprintf(">\n");
#ifdef DEBUG
    dprintf("> recv skb=\n"); 
    skb_print_bits("", skb, 0, skb->len);
#endif
    ip_n = (skb->nh.iph->ihl << 2);
    if(skb->data == skb->mac.raw){
        // skb->data points at ethernet header.
        if (!pskb_may_pull(skb, eth_n + ip_n)){
            wprintf("> Malformed skb\n");
            err = -EINVAL;
            goto exit;
        }
        skb_pull_vn(skb, eth_n + ip_n);
    }
    addr = skb->nh.iph->daddr;
    err = esp_skb_header(skb, &esph);
    if(err) goto exit;
    dprintf("> spi=%08x protocol=%d addr=" IPFMT "\n",
            esph->spi, IPPROTO_ESP, NIPQUAD(addr));
    sa = sa_table_lookup_spi(esph->spi, IPPROTO_ESP, addr);
    if(!sa){
        err = vnet_sa_create(esph->spi, IPPROTO_ESP, addr, &sa);
        if(err) goto exit;
    }
    //todo: Return a -ve protocol instead? See esp_sa_recv.
    err = SAState_recv(sa, skb);
  exit:
    if(sa) SAState_decref(sa);
    if(err <= 0){
        kfree_skb(skb);
        err = 0;
    }
    dprintf("< err=%d\n", err);
    return err;
}

/** Handle an ICMP error related to ESP.
 *
 * @param skb ICMP error packet
 * @param info
 */
static void esp_protocol_icmp_err(struct sk_buff *skb, u32 info){
    struct iphdr *iph = (struct iphdr*)skb->data;
    ESPHdr *esph;
    SAState *sa;
    
    dprintf("> ICMP error type=%d code=%d\n",
            skb->h.icmph->type, skb->h.icmph->code);
    if(skb->h.icmph->type != ICMP_DEST_UNREACH ||
       skb->h.icmph->code != ICMP_FRAG_NEEDED){
        return;
    }
    
    //todo: need to check skb has enough len to do this.
    esph = (ESPHdr*)(skb->data + (iph->ihl << 2));
    sa = sa_table_lookup_spi(esph->spi, IPPROTO_ESP, iph->daddr);
    if(!sa) return;
    wprintf("> ICMP unreachable on SA ESP spi=%08x addr=" IPFMT "\n",
            ntohl(esph->spi), NIPQUAD(iph->daddr));
    SAState_decref(sa);
}

//============================================================================
#if LINUX_VERSION_CODE >= KERNEL_VERSION(2,6,0)
// Code for 2.6 kernel.

/** Protocol handler for ESP.
 */
static struct net_protocol esp_protocol = {
    .handler     = esp_protocol_recv,
    .err_handler = esp_protocol_icmp_err
};

static int esp_protocol_add(void){
    return inet_add_protocol(&esp_protocol, IPPROTO_ESP);
}

static int esp_protocol_del(void){
    return inet_del_protocol(&esp_protocol, IPPROTO_ESP);
}

//============================================================================
#else
//============================================================================
// Code for 2.4 kernel.

/** Protocol handler for ESP.
 */
static struct inet_protocol esp_protocol = {
    .name        = "ESP",
    .protocol    = IPPROTO_ESP,
    .handler     = esp_protocol_recv,
    .err_handler = esp_protocol_icmp_err
};

static int esp_protocol_add(void){
    inet_add_protocol(&esp_protocol);
    return 0;
}

static int esp_protocol_del(void){
    return inet_del_protocol(&esp_protocol);
}

#endif
//============================================================================


/** Initialize the ESP module.
 * Registers the ESP protocol and SA type.
 *
 * @return 0 on success, negative error code otherwise
 */
int __init esp_module_init(void){
    int err = 0;
    dprintf(">\n");
    err = SAType_add(&esp_sa_type);
    if(err < 0){
        eprintf("> Error adding esp sa type\n");
        goto exit;
    }
    esp_protocol_add();
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

/** Finalize the ESP module.
 * Deregisters the ESP protocol and SA type.
 */
void __exit esp_module_exit(void){
    if(esp_protocol_del() < 0){
        eprintf("> Error removing esp protocol\n");
    }
    if(SAType_del(&esp_sa_type) < 0){
        eprintf("> Error removing esp sa type\n");
    }
}

#endif // CONFIG_CRYPTO_HMAC
