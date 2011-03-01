/* Copyright (c) 2008, XenSource Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of XenSource Inc. nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
#ifndef _VHD_LIB_H_
#define _VHD_LIB_H_

#include <string.h>
#if defined(__linux__)
#include <endian.h>
#include <byteswap.h>
#elif defined(__NetBSD__)
#include <sys/endian.h>
#include <sys/bswap.h>
#endif

#include "blk_uuid.h"
#include "vhd.h"

#ifndef O_LARGEFILE
#define O_LARGEFILE	0
#endif

#if BYTE_ORDER == LITTLE_ENDIAN
#if defined(__linux__)
  #define BE16_IN(foo)             (*(foo)) = bswap_16(*(foo))
  #define BE32_IN(foo)             (*(foo)) = bswap_32(*(foo))
  #define BE64_IN(foo)             (*(foo)) = bswap_64(*(foo))
  #define BE16_OUT(foo)            (*(foo)) = bswap_16(*(foo))
  #define BE32_OUT(foo)            (*(foo)) = bswap_32(*(foo))
  #define BE64_OUT(foo)            (*(foo)) = bswap_64(*(foo))
#elif defined(__NetBSD__)
  #define BE16_IN(foo)             (*(foo)) = bswap16(*(foo))
  #define BE32_IN(foo)             (*(foo)) = bswap32(*(foo))
  #define BE64_IN(foo)             (*(foo)) = bswap64(*(foo))
  #define BE16_OUT(foo)            (*(foo)) = bswap16(*(foo))
  #define BE32_OUT(foo)            (*(foo)) = bswap32(*(foo))
  #define BE64_OUT(foo)            (*(foo)) = bswap64(*(foo))
#endif
#else
  #define BE16_IN(foo)
  #define BE32_IN(foo)
  #define BE64_IN(foo)
  #define BE32_OUT(foo)
  #define BE32_OUT(foo)
  #define BE64_OUT(foo)
#endif

#define MIN(a, b)                  (((a) < (b)) ? (a) : (b))
#define MAX(a, b)                  (((a) > (b)) ? (a) : (b))

#define VHD_MAX_NAME_LEN           1024

#define VHD_BLOCK_SHIFT            21
#define VHD_BLOCK_SIZE             (1ULL << VHD_BLOCK_SHIFT)

#define UTF_16                     "UTF-16"
#define UTF_16LE                   "UTF-16LE"
#define UTF_16BE                   "UTF-16BE"

#define VHD_OPEN_RDONLY            0x00001
#define VHD_OPEN_RDWR              0x00002
#define VHD_OPEN_FAST              0x00004
#define VHD_OPEN_STRICT            0x00008
#define VHD_OPEN_IGNORE_DISABLED   0x00010

#define VHD_FLAG_CREAT_PARENT_RAW        0x00001

#define vhd_flag_set(word, flag)         ((word) |= (flag))
#define vhd_flag_clear(word, flag)       ((word) &= ~(flag))
#define vhd_flag_test(word, flag)        ((word) & (flag))


#define ENABLE_FAILURE_TESTING
#define FAIL_REPARENT_BEGIN        0
#define FAIL_REPARENT_LOCATOR      1
#define FAIL_REPARENT_END          2
#define FAIL_RESIZE_BEGIN          3
#define FAIL_RESIZE_DATA_MOVED     4
#define FAIL_RESIZE_METADATA_MOVED 5
#define FAIL_RESIZE_END            6
#define NUM_FAIL_TESTS             7

#ifdef ENABLE_FAILURE_TESTING
#define TEST_FAIL_AT(point) \
	if (TEST_FAIL[point]) { \
		printf("Failing at %s\n", ENV_VAR_FAIL[point]); exit(EINVAL); }
#define TEST_FAIL_EXTERN_VARS              \
	extern const char* ENV_VAR_FAIL[]; \
	extern int TEST_FAIL[];
#else
#define TEST_FAIL_AT(point)
#define TEST_FAIL_EXTERN_VARS
#endif // ENABLE_FAILURE_TESTING


static const char                  VHD_POISON_COOKIE[] = "v_poison";

typedef struct hd_ftr              vhd_footer_t;
typedef struct dd_hdr              vhd_header_t;
typedef struct vhd_bat             vhd_bat_t;
typedef struct vhd_batmap          vhd_batmap_t;
typedef struct dd_batmap_hdr       vhd_batmap_header_t;
typedef struct prt_loc             vhd_parent_locator_t;
typedef struct vhd_context         vhd_context_t;
typedef uint32_t                   vhd_flag_creat_t;

struct vhd_bat {
	uint32_t                   spb;
	uint32_t                   entries;
	uint32_t                  *bat;
};

struct vhd_batmap {
	vhd_batmap_header_t        header;
	char                      *map;
};

struct vhd_context {
	int                        fd;
	char                      *file;
	int                        oflags;
	int                        is_block;

	uint32_t                   spb;
	uint32_t                   bm_secs;

	vhd_header_t               header;
	vhd_footer_t               footer;
	vhd_bat_t                  bat;
	vhd_batmap_t               batmap;
};

static inline uint32_t
secs_round_up(uint64_t bytes)
{
	return ((bytes + (VHD_SECTOR_SIZE - 1)) >> VHD_SECTOR_SHIFT);
}

static inline uint32_t
secs_round_up_no_zero(uint64_t bytes)
{
	return (secs_round_up(bytes) ? : 1);
}

static inline uint64_t
vhd_sectors_to_bytes(uint64_t sectors)
{
	return sectors << VHD_SECTOR_SHIFT;
}

static inline uint64_t
vhd_bytes_padded(uint64_t bytes)
{
	return vhd_sectors_to_bytes(secs_round_up_no_zero(bytes));
}

static inline int
vhd_type_dynamic(vhd_context_t *ctx)
{
	return (ctx->footer.type == HD_TYPE_DYNAMIC ||
		ctx->footer.type == HD_TYPE_DIFF);
}

static inline int
vhd_creator_tapdisk(vhd_context_t *ctx)
{
	return !strncmp(ctx->footer.crtr_app, "tap", 3);
}

static inline int
vhd_disabled(vhd_context_t *ctx)
{
	return (!memcmp(ctx->footer.cookie,
			VHD_POISON_COOKIE, sizeof(ctx->footer.cookie)));
}

static inline size_t
vhd_parent_locator_size(vhd_parent_locator_t *loc)
{
	/*
	 * MICROSOFT_COMPAT
	 * data_space *should* be in sectors,
	 * but sometimes we find it in bytes
	 */
	if (loc->data_space < 512)
		return vhd_sectors_to_bytes(loc->data_space);
	else if (loc->data_space % 512 == 0)
		return loc->data_space;
	else
		return 0;
}

static inline int
vhd_parent_raw(vhd_context_t *ctx)
{
	return blk_uuid_is_nil(&ctx->header.prt_uuid);
}

void libvhd_set_log_level(int);

int vhd_test_file_fixed(const char *, int *);

uint32_t vhd_time(time_t time);
size_t vhd_time_to_string(uint32_t timestamp, char *target);
uint32_t vhd_chs(uint64_t size);

uint32_t vhd_checksum_footer(vhd_footer_t *);
uint32_t vhd_checksum_header(vhd_header_t *);
uint32_t vhd_checksum_batmap(vhd_batmap_t *);

void vhd_footer_in(vhd_footer_t *);
void vhd_footer_out(vhd_footer_t *);
void vhd_header_in(vhd_header_t *);
void vhd_header_out(vhd_header_t *);
void vhd_bat_in(vhd_bat_t *);
void vhd_bat_out(vhd_bat_t *);
void vhd_batmap_header_in(vhd_batmap_t *);
void vhd_batmap_header_out(vhd_batmap_t *);

int vhd_validate_footer(vhd_footer_t *footer);
int vhd_validate_header(vhd_header_t *header);
int vhd_validate_batmap_header(vhd_batmap_t *batmap);
int vhd_validate_batmap(vhd_batmap_t *batmap);
int vhd_validate_platform_code(uint32_t code);

int vhd_open(vhd_context_t *, const char *file, int flags);
void vhd_close(vhd_context_t *);
int vhd_create(const char *name, uint64_t bytes, int type, vhd_flag_creat_t);
/* vhd_snapshot: the bytes parameter is optional and can be 0 if the snapshot 
 * is to have the same size as the (first non-empty) parent */
int vhd_snapshot(const char *snapshot, uint64_t bytes, const char *parent,
		vhd_flag_creat_t);

int vhd_hidden(vhd_context_t *, int *);
int vhd_chain_depth(vhd_context_t *, int *);

off_t vhd_position(vhd_context_t *);
int vhd_seek(vhd_context_t *, off_t, int);
int vhd_read(vhd_context_t *, void *, size_t);
int vhd_write(vhd_context_t *, void *, size_t);

int vhd_offset(vhd_context_t *, uint32_t, uint32_t *);

int vhd_end_of_headers(vhd_context_t *ctx, off_t *off);
int vhd_end_of_data(vhd_context_t *ctx, off_t *off);
int vhd_batmap_header_offset(vhd_context_t *ctx, off_t *off);

int vhd_get_header(vhd_context_t *);
int vhd_get_footer(vhd_context_t *);
int vhd_get_bat(vhd_context_t *);
int vhd_get_batmap(vhd_context_t *);

void vhd_put_header(vhd_context_t *);
void vhd_put_footer(vhd_context_t *);
void vhd_put_bat(vhd_context_t *);
void vhd_put_batmap(vhd_context_t *);

int vhd_has_batmap(vhd_context_t *);
int vhd_batmap_test(vhd_context_t *, vhd_batmap_t *, uint32_t);
void vhd_batmap_set(vhd_context_t *, vhd_batmap_t *, uint32_t);
void vhd_batmap_clear(vhd_context_t *, vhd_batmap_t *, uint32_t);

int vhd_get_phys_size(vhd_context_t *, off_t *);
int vhd_set_phys_size(vhd_context_t *, off_t);

int vhd_bitmap_test(vhd_context_t *, char *, uint32_t);
void vhd_bitmap_set(vhd_context_t *, char *, uint32_t);
void vhd_bitmap_clear(vhd_context_t *, char *, uint32_t);

int vhd_parent_locator_count(vhd_context_t *);
int vhd_parent_locator_get(vhd_context_t *, char **);
int vhd_parent_locator_read(vhd_context_t *, vhd_parent_locator_t *, char **);
int vhd_find_parent(vhd_context_t *, const char *, char **);
int vhd_parent_locator_write_at(vhd_context_t *, const char *,
				off_t, uint32_t, size_t,
				vhd_parent_locator_t *);

int vhd_header_decode_parent(vhd_context_t *, vhd_header_t *, char **);
int vhd_change_parent(vhd_context_t *, char *parent_path, int raw);

int vhd_read_footer(vhd_context_t *, vhd_footer_t *);
int vhd_read_footer_at(vhd_context_t *, vhd_footer_t *, off_t);
int vhd_read_footer_strict(vhd_context_t *, vhd_footer_t *);
int vhd_read_header(vhd_context_t *, vhd_header_t *);
int vhd_read_header_at(vhd_context_t *, vhd_header_t *, off_t);
int vhd_read_bat(vhd_context_t *, vhd_bat_t *);
int vhd_read_batmap(vhd_context_t *, vhd_batmap_t *);
int vhd_read_bitmap(vhd_context_t *, uint32_t block, char **bufp);
int vhd_read_block(vhd_context_t *, uint32_t block, char **bufp);

int vhd_write_footer(vhd_context_t *, vhd_footer_t *);
int vhd_write_footer_at(vhd_context_t *, vhd_footer_t *, off_t);
int vhd_write_header(vhd_context_t *, vhd_header_t *);
int vhd_write_header_at(vhd_context_t *, vhd_header_t *, off_t);
int vhd_write_bat(vhd_context_t *, vhd_bat_t *);
int vhd_write_batmap(vhd_context_t *, vhd_batmap_t *);
int vhd_write_bitmap(vhd_context_t *, uint32_t block, char *bitmap);
int vhd_write_block(vhd_context_t *, uint32_t block, char *data);

int vhd_io_read(vhd_context_t *, char *, uint64_t, uint32_t);
int vhd_io_write(vhd_context_t *, char *, uint64_t, uint32_t);

#endif
