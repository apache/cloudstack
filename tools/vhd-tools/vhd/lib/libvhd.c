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
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <libgen.h>
#include <iconv.h>
#include <sys/mman.h>
#include <sys/stat.h>

#include "libvhd.h"
#include "relative-path.h"

static int libvhd_dbg = 0;

void
libvhd_set_log_level(int level)
{
	if (level)
		libvhd_dbg = 1;
}

#define VHDLOG(_f, _a...)						\
	do {								\
		if (libvhd_dbg)						\
			syslog(LOG_INFO, "libvhd::%s: "_f,		\
			       __func__, ##_a);				\
	} while (0)

#define BIT_MASK 0x80

#ifdef ENABLE_FAILURE_TESTING
const char* ENV_VAR_FAIL[NUM_FAIL_TESTS] = {
	"VHD_UTIL_TEST_FAIL_REPARENT_BEGIN",
	"VHD_UTIL_TEST_FAIL_REPARENT_LOCATOR",
	"VHD_UTIL_TEST_FAIL_REPARENT_END",
	"VHD_UTIL_TEST_FAIL_RESIZE_BEGIN",
	"VHD_UTIL_TEST_FAIL_RESIZE_DATA_MOVED",
	"VHD_UTIL_TEST_FAIL_RESIZE_METADATA_MOVED",
	"VHD_UTIL_TEST_FAIL_RESIZE_END"
};
int TEST_FAIL[NUM_FAIL_TESTS];
#endif // ENABLE_FAILURE_TESTING

static inline int
test_bit (volatile char *addr, int nr)
{
	return ((addr[nr >> 3] << (nr & 7)) & BIT_MASK) != 0;
}

static inline void
set_bit (volatile char *addr, int nr)
{
	addr[nr >> 3] |= (BIT_MASK >> (nr & 7));
}

static inline void
clear_bit (volatile char *addr, int nr)
{
	addr[nr >> 3] &= ~(BIT_MASK >> (nr & 7));
}

static inline int
old_test_bit(volatile char *addr, int nr)
{
	return (((uint32_t *)addr)[nr >> 5] >> (nr & 31)) & 1;
}

static inline void
old_set_bit(volatile char *addr, int nr)
{
	((uint32_t *)addr)[nr >> 5] |= (1 << (nr & 31));
}

static inline void
old_clear_bit(volatile char *addr, int nr)
{
	((uint32_t *)addr)[nr >> 5] &= ~(1 << (nr & 31));
}

void
vhd_footer_in(vhd_footer_t *footer)
{
	BE32_IN(&footer->features);
	BE32_IN(&footer->ff_version);
	BE64_IN(&footer->data_offset);
	BE32_IN(&footer->timestamp);
	BE32_IN(&footer->crtr_ver);
	BE32_IN(&footer->crtr_os);
	BE64_IN(&footer->orig_size);
	BE64_IN(&footer->curr_size);
	BE32_IN(&footer->geometry);
	BE32_IN(&footer->type);
	BE32_IN(&footer->checksum);
}

void
vhd_footer_out(vhd_footer_t *footer)
{
	BE32_OUT(&footer->features);
	BE32_OUT(&footer->ff_version);
	BE64_OUT(&footer->data_offset);
	BE32_OUT(&footer->timestamp);
	BE32_OUT(&footer->crtr_ver);
	BE32_OUT(&footer->crtr_os);
	BE64_OUT(&footer->orig_size);
	BE64_OUT(&footer->curr_size);
	BE32_OUT(&footer->geometry);
	BE32_OUT(&footer->type);
	BE32_OUT(&footer->checksum);
}

void
vhd_header_in(vhd_header_t *header)
{
	int i, n;

	BE64_IN(&header->data_offset);
	BE64_IN(&header->table_offset);
	BE32_IN(&header->hdr_ver);
	BE32_IN(&header->max_bat_size);
	BE32_IN(&header->block_size);
	BE32_IN(&header->checksum);
	BE32_IN(&header->prt_ts);

	n = sizeof(header->loc) / sizeof(vhd_parent_locator_t);

	for (i = 0; i < n; i++) {
		BE32_IN(&header->loc[i].code);
		BE32_IN(&header->loc[i].data_space);
		BE32_IN(&header->loc[i].data_len);
		BE64_IN(&header->loc[i].data_offset);
	}
}

void
vhd_header_out(vhd_header_t *header)
{
	int i, n;

	BE64_OUT(&header->data_offset);
	BE64_OUT(&header->table_offset);
	BE32_OUT(&header->hdr_ver);
	BE32_OUT(&header->max_bat_size);
	BE32_OUT(&header->block_size);
	BE32_OUT(&header->checksum);
	BE32_OUT(&header->prt_ts);

	n = sizeof(header->loc) / sizeof(vhd_parent_locator_t);

	for (i = 0; i < n; i++) {
		BE32_OUT(&header->loc[i].code);
		BE32_OUT(&header->loc[i].data_space);
		BE32_OUT(&header->loc[i].data_len);
		BE64_OUT(&header->loc[i].data_offset);
	}
}

void
vhd_batmap_header_in(vhd_batmap_t *batmap)
{
	BE64_IN(&batmap->header.batmap_offset);
	BE32_IN(&batmap->header.batmap_size);
	BE32_IN(&batmap->header.batmap_version);
	BE32_IN(&batmap->header.checksum);
}

void
vhd_batmap_header_out(vhd_batmap_t *batmap)
{
	BE64_OUT(&batmap->header.batmap_offset);
	BE32_OUT(&batmap->header.batmap_size);
	BE32_OUT(&batmap->header.batmap_version);
	BE32_OUT(&batmap->header.checksum);
}

void
vhd_bat_in(vhd_bat_t *bat)
{
	int i;

	for (i = 0; i < bat->entries; i++)
		BE32_IN(&bat->bat[i]);
}

void
vhd_bat_out(vhd_bat_t *bat)
{
	int i;

	for (i = 0; i < bat->entries; i++)
		BE32_OUT(&bat->bat[i]);
}

uint32_t
vhd_checksum_footer(vhd_footer_t *footer)
{
	int i;
	unsigned char *blob;
	uint32_t checksum, tmp;

	checksum         = 0;
	tmp              = footer->checksum;
	footer->checksum = 0;

	blob = (unsigned char *)footer;
	for (i = 0; i < sizeof(vhd_footer_t); i++)
		checksum += (uint32_t)blob[i];

	footer->checksum = tmp;
	return ~checksum;
}

int
vhd_validate_footer(vhd_footer_t *footer)
{
	int csize;
	uint32_t checksum;

	csize = sizeof(footer->cookie);
	if (memcmp(footer->cookie, HD_COOKIE, csize) != 0 &&
	    memcmp(footer->cookie, VHD_POISON_COOKIE, csize) != 0) {
		char buf[9];
		strncpy(buf, footer->cookie, sizeof(buf));
		buf[sizeof(buf)-1]= '\0';
		VHDLOG("invalid footer cookie: %s\n", buf);
		return -EINVAL;
	}

	checksum = vhd_checksum_footer(footer);
	if (checksum != footer->checksum) {
		/*
		 * early td-util did not re-calculate
		 * checksum when marking vhds 'hidden'
		 */
		if (footer->hidden &&
		    !strncmp(footer->crtr_app, "tap", 3) &&
		    (footer->crtr_ver == VHD_VERSION(0, 1) ||
		     footer->crtr_ver == VHD_VERSION(1, 1))) {
			char tmp = footer->hidden;
			footer->hidden = 0;
			checksum = vhd_checksum_footer(footer);
			footer->hidden = tmp;

			if (checksum == footer->checksum)
				return 0;
		}

		VHDLOG("invalid footer checksum: "
		       "footer = 0x%08x, calculated = 0x%08x\n",
		       footer->checksum, checksum);
		return -EINVAL;
	}

	return 0;
}

uint32_t
vhd_checksum_header(vhd_header_t *header)
{
	int i;
	unsigned char *blob;
	uint32_t checksum, tmp;

	checksum         = 0;
	tmp              = header->checksum;
	header->checksum = 0;

	blob = (unsigned char *)header;
	for (i = 0; i < sizeof(vhd_header_t); i++)
		checksum += (uint32_t)blob[i];

	header->checksum = tmp;
	return ~checksum;
}

int
vhd_validate_header(vhd_header_t *header)
{
	int i, n;
	uint32_t checksum;

	if (memcmp(header->cookie, DD_COOKIE, 8) != 0) {
		char buf[9];
		strncpy(buf, header->cookie, sizeof(buf));
		buf[sizeof(buf)-1]= '\0';
		VHDLOG("invalid header cookie: %s\n", buf);
		return -EINVAL;
	}

	if (header->hdr_ver != 0x00010000) {
		VHDLOG("invalid header version 0x%08x\n", header->hdr_ver);
		return -EINVAL;
	}

	if (header->data_offset != 0xFFFFFFFFFFFFFFFF) {
		VHDLOG("invalid header data_offset 0x%016"PRIx64"\n",
		       header->data_offset);
		return -EINVAL;
	}

	n = sizeof(header->loc) / sizeof(vhd_parent_locator_t);
	for (i = 0; i < n; i++)
		if (vhd_validate_platform_code(header->loc[i].code))
			return -EINVAL;

	checksum = vhd_checksum_header(header);
	if (checksum != header->checksum) {
		VHDLOG("invalid header checksum: "
		       "header = 0x%08x, calculated = 0x%08x\n",
		       header->checksum, checksum);
		return -EINVAL;
	}

	return 0;
}

static inline int
vhd_validate_bat(vhd_bat_t *bat)
{
	if (!bat->bat)
		return -EINVAL;

	return 0;
}

uint32_t
vhd_checksum_batmap(vhd_batmap_t *batmap)
{
	int i, n;
	char *blob;
	uint32_t checksum;

	blob     = batmap->map;
	checksum = 0;

	n = vhd_sectors_to_bytes(batmap->header.batmap_size);

	for (i = 0; i < n; i++) {
		if (batmap->header.batmap_version == VHD_BATMAP_VERSION(1, 1))
			checksum += (uint32_t)blob[i];
		else
			checksum += (uint32_t)(unsigned char)blob[i];
	}

	return ~checksum;
}

int
vhd_validate_batmap_header(vhd_batmap_t *batmap)
{
	if (memcmp(batmap->header.cookie, VHD_BATMAP_COOKIE, 8))
		return -EINVAL;

	if (batmap->header.batmap_version > VHD_BATMAP_CURRENT_VERSION)
		return -EINVAL;

	return 0;
}

int
vhd_validate_batmap(vhd_batmap_t *batmap)
{
	uint32_t checksum;

	if (!batmap->map)
		return -EINVAL;

	checksum = vhd_checksum_batmap(batmap);
	if (checksum != batmap->header.checksum)
		return -EINVAL;

	return 0;
}

int
vhd_batmap_header_offset(vhd_context_t *ctx, off_t *_off)
{
	off_t off;
	size_t  bat;

	*_off = 0;

	off  = ctx->header.table_offset;
	bat  = ctx->header.max_bat_size * sizeof(uint32_t);
	off += vhd_bytes_padded(bat);

	*_off = off;
	return 0;
}

int
vhd_validate_platform_code(uint32_t code)
{
	switch (code) {
	case PLAT_CODE_NONE:
	case PLAT_CODE_WI2R:
	case PLAT_CODE_WI2K:
	case PLAT_CODE_W2RU:
	case PLAT_CODE_W2KU:
	case PLAT_CODE_MAC:
	case PLAT_CODE_MACX:
		return 0;
	default:
		VHDLOG("invalid parent locator code %u\n", code);
		return -EINVAL;
	}
}

int
vhd_parent_locator_count(vhd_context_t *ctx)
{
	return (sizeof(ctx->header.loc) / sizeof(vhd_parent_locator_t));
}

int
vhd_hidden(vhd_context_t *ctx, int *hidden)
{
	int err;

	*hidden = 0;

	if (vhd_type_dynamic(ctx) && vhd_creator_tapdisk(ctx) &&
	    (ctx->footer.crtr_ver == VHD_VERSION(0, 1) ||
	     ctx->footer.crtr_ver == VHD_VERSION(1, 1))) {
		vhd_footer_t copy;

		err = vhd_read_footer_at(ctx, &copy, 0);
		if (err) {
			VHDLOG("error reading backup footer of %s: %d\n",
			       ctx->file, err);
			return err;
		}
		*hidden = copy.hidden;
	} else
		*hidden = ctx->footer.hidden;

	return 0;
}

int
vhd_chain_depth(vhd_context_t *ctx, int *depth)
{
	char *file;
	int err, cnt;
	vhd_context_t vhd, *cur;

	err    = 0;
	cnt    = 0;
	*depth = 0;
	file   = NULL;
	cur    = ctx;

	for (;;) {
		cnt++;

		if (cur->footer.type != HD_TYPE_DIFF)
			break;

		if (vhd_parent_raw(cur)) {
			cnt++;
			break;
		}

		err = vhd_parent_locator_get(cur, &file);
		if (err) {
			file = NULL;
			break;
		}

		if (cur != ctx) {
			vhd_close(cur);
			cur = NULL;
		}

		err = vhd_open(&vhd, file, VHD_OPEN_RDONLY);
		if (err)
			break;

		cur = &vhd;
		free(file);
		file = NULL;
	}

	free(file);
	if (cur && cur != ctx)
		vhd_close(cur);

	if (!err)
		*depth = cnt;

	return err;
}

int
vhd_batmap_test(vhd_context_t *ctx, vhd_batmap_t *batmap, uint32_t block)
{
	if (!vhd_has_batmap(ctx) || !batmap->map)
		return 0;

	if (block >= (batmap->header.batmap_size << (VHD_SECTOR_SHIFT + 3)))
		return 0;

	return test_bit(batmap->map, block);
}

void
vhd_batmap_set(vhd_context_t *ctx, vhd_batmap_t *batmap, uint32_t block)
{
	if (!vhd_has_batmap(ctx) || !batmap->map)
		return;

	if (block >= (batmap->header.batmap_size << (VHD_SECTOR_SHIFT + 3)))
		return;

	set_bit(batmap->map, block);
}

void
vhd_batmap_clear(vhd_context_t *ctx, vhd_batmap_t *batmap, uint32_t block)
{
	if (!vhd_has_batmap(ctx) || !batmap->map)
		return;

	if (block >= (batmap->header.batmap_size << (VHD_SECTOR_SHIFT + 3)))
		return;

	clear_bit(batmap->map, block);
}

int
vhd_bitmap_test(vhd_context_t *ctx, char *map, uint32_t block)
{
	if (vhd_creator_tapdisk(ctx) &&
	    ctx->footer.crtr_ver == 0x00000001)
		return old_test_bit(map, block);

	return test_bit(map, block);
}

void
vhd_bitmap_set(vhd_context_t *ctx, char *map, uint32_t block)
{
	if (vhd_creator_tapdisk(ctx) &&
	    ctx->footer.crtr_ver == 0x00000001)
		return old_set_bit(map, block);

	return set_bit(map, block);
}

void
vhd_bitmap_clear(vhd_context_t *ctx, char *map, uint32_t block)
{
	if (vhd_creator_tapdisk(ctx) &&
	    ctx->footer.crtr_ver == 0x00000001)
		return old_clear_bit(map, block);

	return clear_bit(map, block);
}

/*
 * returns absolute offset of the first 
 * byte of the file which is not vhd metadata
 */
int
vhd_end_of_headers(vhd_context_t *ctx, off_t *end)
{
	int err, i, n;
	uint32_t bat_bytes;
	off_t eom, bat_end;
	vhd_parent_locator_t *loc;

	*end = 0;

	if (!vhd_type_dynamic(ctx))
		return 0;

	eom       = ctx->footer.data_offset + sizeof(vhd_header_t);

	bat_bytes = vhd_bytes_padded(ctx->header.max_bat_size * sizeof(uint32_t));
	bat_end   = ctx->header.table_offset + bat_bytes;

	eom       = MAX(eom, bat_end);

	if (vhd_has_batmap(ctx)) {
		off_t hdr_end, hdr_secs, map_end, map_secs;

		err = vhd_get_batmap(ctx);
		if (err)
			return err;

		hdr_secs = secs_round_up_no_zero(sizeof(vhd_batmap_header_t));
		err      = vhd_batmap_header_offset(ctx, &hdr_end);
		if (err)
			return err;

		hdr_end += vhd_sectors_to_bytes(hdr_secs);
		eom      = MAX(eom, hdr_end);

		map_secs = ctx->batmap.header.batmap_size;
		map_end  = (ctx->batmap.header.batmap_offset +
			    vhd_sectors_to_bytes(map_secs));
		eom      = MAX(eom, map_end);
	}

	/* parent locators */
	n = sizeof(ctx->header.loc) / sizeof(vhd_parent_locator_t);

	for (i = 0; i < n; i++) {
		off_t loc_end;

		loc = &ctx->header.loc[i];
		if (loc->code == PLAT_CODE_NONE)
			continue;

		loc_end = loc->data_offset + vhd_parent_locator_size(loc);
		eom     = MAX(eom, loc_end);
	}

	*end = eom;
	return 0;
}

int
vhd_end_of_data(vhd_context_t *ctx, off_t *end)
{
	int i, err;
	off_t max;
	uint64_t blk;

	if (!vhd_type_dynamic(ctx)) {
		err = vhd_seek(ctx, 0, SEEK_END);
		if (err)
			return err;

		max = vhd_position(ctx);
		if (max == (off_t)-1)
			return -errno;

		*end = max - sizeof(vhd_footer_t);
		return 0;
	}

	err = vhd_end_of_headers(ctx, &max);
	if (err)
		return err;

	err = vhd_get_bat(ctx);
	if (err)
		return err;

	max >>= VHD_SECTOR_SHIFT;

	for (i = 0; i < ctx->bat.entries; i++) {
		blk = ctx->bat.bat[i];

		if (blk != DD_BLK_UNUSED) {
			blk += ctx->spb + ctx->bm_secs;
			max  = MAX(blk, max);
		}
	}

	*end = vhd_sectors_to_bytes(max);
	return 0;
}

uint32_t
vhd_time(time_t time)
{
	struct tm tm;
	time_t micro_epoch;

	memset(&tm, 0, sizeof(struct tm));
	tm.tm_year   = 100;
	tm.tm_mon    = 0;
	tm.tm_mday   = 1;
	micro_epoch  = mktime(&tm);

	return (uint32_t)(time - micro_epoch);
}

/* 
 * Stringify the VHD timestamp for printing.
 * As with ctime_r, target must be >=26 bytes.
 */
size_t 
vhd_time_to_string(uint32_t timestamp, char *target)
{
	char *cr;
	struct tm tm;
	time_t t1, t2;

	memset(&tm, 0, sizeof(struct tm));

	/* VHD uses an epoch of 12:00AM, Jan 1, 2000.         */
	/* Need to adjust this to the expected epoch of 1970. */
	tm.tm_year  = 100;
	tm.tm_mon   = 0;
	tm.tm_mday  = 1;

	t1 = mktime(&tm);
	t2 = t1 + (time_t)timestamp;
	ctime_r(&t2, target);

	/* handle mad ctime_r newline appending. */
	if ((cr = strchr(target, '\n')) != NULL)
		*cr = '\0';

	return (strlen(target));
}

/*
 * nabbed from vhd specs.
 */
uint32_t
vhd_chs(uint64_t size)
{
	uint32_t secs, cylinders, heads, spt, cth;

	secs = secs_round_up_no_zero(size);

	if (secs > 65535 * 16 * 255)
		secs = 65535 * 16 * 255;

	if (secs >= 65535 * 16 * 63) {
		spt   = 255;
		cth   = secs / spt;
		heads = 16;
	} else {
		spt   = 17;
		cth   = secs / spt;
		heads = (cth + 1023) / 1024;

		if (heads < 4)
			heads = 4;

		if (cth >= (heads * 1024) || heads > 16) {
			spt   = 31;
			cth   = secs / spt;
			heads = 16;
		}

		if (cth >= heads * 1024) {
			spt   = 63;
			cth   = secs / spt;
			heads = 16;
		}
	}

	cylinders = cth / heads;

	return GEOM_ENCODE(cylinders, heads, spt);
}

int
vhd_get_footer(vhd_context_t *ctx)
{
	if (!vhd_validate_footer(&ctx->footer))
		return 0;

	return vhd_read_footer(ctx, &ctx->footer);
}

int
vhd_get_header(vhd_context_t *ctx)
{
	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	if (!vhd_validate_header(&ctx->header))
		return 0;

	return vhd_read_header(ctx, &ctx->header);
}

int
vhd_get_bat(vhd_context_t *ctx)
{
	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	if (!vhd_validate_bat(&ctx->bat))
		return 0;

	vhd_put_bat(ctx);
	return vhd_read_bat(ctx, &ctx->bat);
}

int
vhd_get_batmap(vhd_context_t *ctx)
{
	if (!vhd_has_batmap(ctx))
		return -EINVAL;

	if (!vhd_validate_batmap(&ctx->batmap))
		return 0;

	vhd_put_batmap(ctx);
	return vhd_read_batmap(ctx, &ctx->batmap);
}

void
vhd_put_footer(vhd_context_t *ctx)
{
	memset(&ctx->footer, 0, sizeof(vhd_footer_t));
}

void
vhd_put_header(vhd_context_t *ctx)
{
	memset(&ctx->header, 0, sizeof(vhd_header_t));
}

void
vhd_put_bat(vhd_context_t *ctx)
{
	if (!vhd_type_dynamic(ctx))
		return;

	free(ctx->bat.bat);
	memset(&ctx->bat, 0, sizeof(vhd_bat_t));
}

void
vhd_put_batmap(vhd_context_t *ctx)
{
	if (!vhd_type_dynamic(ctx))
		return;

	if (!vhd_has_batmap(ctx))
		return;

	free(ctx->batmap.map);
	memset(&ctx->batmap, 0, sizeof(vhd_batmap_t));
}

/*
 * look for 511 byte footer at end of file
 */
int
vhd_read_short_footer(vhd_context_t *ctx, vhd_footer_t *footer)
{
	int err;
	char *buf;
	off_t eof;

	buf = NULL;

	err = vhd_seek(ctx, 0, SEEK_END);
	if (err)
		goto out;

	eof = vhd_position(ctx);
	if (eof == (off_t)-1) {
		err = -errno;
		goto out;
	}

	err = vhd_seek(ctx, eof - 511, SEEK_SET);
	if (err)
		goto out;

	err = posix_memalign((void **)&buf,
			     VHD_SECTOR_SIZE, sizeof(vhd_footer_t));
	if (err) {
		buf = NULL;
		err = -err;
		goto out;
	}

	memset(buf, 0, sizeof(vhd_footer_t));

	/*
	 * expecting short read here
	 */
	vhd_read(ctx, buf, sizeof(vhd_footer_t));

	memcpy(footer, buf, sizeof(vhd_footer_t));

	vhd_footer_in(footer);
	err = vhd_validate_footer(footer);

out:
	if (err)
		VHDLOG("%s: failed reading short footer: %d\n",
		       ctx->file, err);
	free(buf);
	return err;
}

int
vhd_read_footer_at(vhd_context_t *ctx, vhd_footer_t *footer, off_t off)
{
	int err;
	char *buf;

	buf = NULL;

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	err = posix_memalign((void **)&buf,
			     VHD_SECTOR_SIZE, sizeof(vhd_footer_t));
	if (err) {
		buf = NULL;
		err = -err;
		goto out;
	}

	err = vhd_read(ctx, buf, sizeof(vhd_footer_t));
	if (err)
		goto out;

	memcpy(footer, buf, sizeof(vhd_footer_t));

	vhd_footer_in(footer);
	err = vhd_validate_footer(footer);

out:
	if (err)
		VHDLOG("%s: reading footer at 0x%08"PRIx64" failed: %d\n",
		       ctx->file, off, err);
	free(buf);
	return err;
}

int
vhd_read_footer(vhd_context_t *ctx, vhd_footer_t *footer)
{
	int err;
	off_t off;

	err = vhd_seek(ctx, 0, SEEK_END);
	if (err)
		return err;

	off = vhd_position(ctx);
	if (off == (off_t)-1)
		return -errno;

	err = vhd_read_footer_at(ctx, footer, off - 512);
	if (err != -EINVAL)
		return err;

	err = vhd_read_short_footer(ctx, footer);
	if (err != -EINVAL)
		return err;

	if (ctx->oflags & VHD_OPEN_STRICT)
		return -EINVAL;

	return vhd_read_footer_at(ctx, footer, 0);
}

int
vhd_read_header_at(vhd_context_t *ctx, vhd_header_t *header, off_t off)
{
	int err;
	char *buf;

	buf = NULL;

	if (!vhd_type_dynamic(ctx)) {
		err = -EINVAL;
		goto out;
	}

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	err = posix_memalign((void **)&buf,
			     VHD_SECTOR_SIZE, sizeof(vhd_header_t));
	if (err) {
		buf = NULL;
		err = -err;
		goto out;
	}

	err = vhd_read(ctx, buf, sizeof(vhd_header_t));
	if (err)
		goto out;

	memcpy(header, buf, sizeof(vhd_header_t));

	vhd_header_in(header);
	err = vhd_validate_header(header);

out:
	if (err)
		VHDLOG("%s: reading header at 0x%08"PRIx64" failed: %d\n",
		       ctx->file, off, err);
	free(buf);
	return err;
}

int
vhd_read_header(vhd_context_t *ctx, vhd_header_t *header)
{
	int err;
	off_t off;

	if (!vhd_type_dynamic(ctx)) {
		VHDLOG("%s is not dynamic!\n", ctx->file);
		return -EINVAL;
	}

	off = ctx->footer.data_offset;
	return vhd_read_header_at(ctx, header, off);
}

int
vhd_read_bat(vhd_context_t *ctx, vhd_bat_t *bat)
{
	int err;
	char *buf;
	off_t off;
	size_t size;

	buf  = NULL;

	if (!vhd_type_dynamic(ctx)) {
		err = -EINVAL;
		goto fail;
	}

	off  = ctx->header.table_offset;
	size = vhd_bytes_padded(ctx->header.max_bat_size * sizeof(uint32_t));

	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		buf = NULL;
		err = -err;
		goto fail;
	}

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto fail;

	err = vhd_read(ctx, buf, size);
	if (err)
		goto fail;

	bat->spb     = ctx->header.block_size >> VHD_SECTOR_SHIFT;
	bat->entries = ctx->header.max_bat_size;
	bat->bat     = (uint32_t *)buf;

	vhd_bat_in(bat);

	return 0;

fail:
	free(buf);
	memset(bat, 0, sizeof(vhd_bat_t));
	VHDLOG("%s: failed to read bat: %d\n", ctx->file, err);
	return err;
}

static int
vhd_read_batmap_header(vhd_context_t *ctx, vhd_batmap_t *batmap)
{
	int err;
	char *buf;
	off_t off;
	size_t size;

	buf = NULL;

	err = vhd_batmap_header_offset(ctx, &off);
	if (err)
		goto fail;

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto fail;

	size = vhd_bytes_padded(sizeof(vhd_batmap_header_t));
	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		buf = NULL;
		err = -err;
		goto fail;
	}

	err = vhd_read(ctx, buf, size);
	if (err)
		goto fail;

	memcpy(&batmap->header, buf, sizeof(vhd_batmap_header_t));
	free(buf);
	buf = NULL;

	vhd_batmap_header_in(batmap);

	return 0;

fail:
	free(buf);
	memset(&batmap->header, 0, sizeof(vhd_batmap_header_t));
	VHDLOG("%s: failed to read batmap header: %d\n", ctx->file, err);
	return err;
}

static int
vhd_read_batmap_map(vhd_context_t *ctx, vhd_batmap_t *batmap)
{
	int err;
	char *buf;
	off_t off;
	size_t map_size;

	map_size = vhd_sectors_to_bytes(batmap->header.batmap_size);

	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, map_size);
	if (err) {
		buf = NULL;
		err = -err;
		goto fail;
	}

	off  = batmap->header.batmap_offset;
	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto fail;

	err  = vhd_read(ctx, buf, map_size);
	if (err)
		goto fail;

	batmap->map = buf;
	return 0;

fail:
	free(buf);
	batmap->map = NULL;
	VHDLOG("%s: failed to read batmap: %d\n", ctx->file, err);
	return err;
}

int
vhd_read_batmap(vhd_context_t *ctx, vhd_batmap_t *batmap)
{
	int err;

	if (!vhd_has_batmap(ctx))
		return -EINVAL;

	memset(batmap, 0, sizeof(vhd_batmap_t));

	err = vhd_read_batmap_header(ctx, batmap);
	if (err)
		return err;

	err = vhd_validate_batmap_header(batmap);
	if (err)
		return err;

	err = vhd_read_batmap_map(ctx, batmap);
	if (err)
		return err;

	err = vhd_validate_batmap(batmap);
	if (err)
		goto fail;

	return 0;

fail:
	free(batmap->map);
	memset(batmap, 0, sizeof(vhd_batmap_t));
	return err;
}

int
vhd_has_batmap(vhd_context_t *ctx)
{
	if (!vhd_type_dynamic(ctx))
		return 0;

	if (!vhd_creator_tapdisk(ctx))
		return 0;

	if (ctx->footer.crtr_ver <= VHD_VERSION(0, 1))
		return 0;

	if (ctx->footer.crtr_ver >= VHD_VERSION(1, 2))
		return 1;

	/*
	 * VHDs of version 1.1 probably have a batmap, but may not 
	 * if they were updated from version 0.1 via vhd-update.
	 */
	if (!vhd_validate_batmap_header(&ctx->batmap))
		return 1;

	if (vhd_read_batmap_header(ctx, &ctx->batmap))
		return 0;

	return (!vhd_validate_batmap_header(&ctx->batmap));
}

/* 
 * Is this a block device (with a fixed size)? This affects whether the file 
 * can be truncated and where the footer is written for VHDs.
 */
int
vhd_test_file_fixed(const char *file, int *is_block)
{
	int err;
	struct stat stats;

	err = stat(file, &stats);
	if (err == -1)
		return -errno;

	*is_block = !!(S_ISBLK(stats.st_mode));
	return err;
}

int
vhd_find_parent(vhd_context_t *ctx, const char *parent, char **_location)
{
	int err;
	char *location, *cpath, *cdir, *path;

	err        = 0;
	path       = NULL;
	cpath      = NULL;
	location   = NULL;
	*_location = NULL;

	if (!parent)
		return -EINVAL;

	if (parent[0] == '/') {
		if (!access(parent, R_OK)) {
			path = strdup(parent);
			if (!path)
				return -ENOMEM;
			*_location = path;
			return 0;
		}
	}

	/* check parent path relative to child's directory */
	cpath = realpath(ctx->file, NULL);
	if (!cpath) {
		err = -errno;
		goto out;
	}

	cdir = dirname(cpath);
	if (asprintf(&location, "%s/%s", cdir, parent) == -1) {
		err = -errno;
		location = NULL;
		goto out;
	}

	if (!access(location, R_OK)) {
		path = realpath(location, NULL);
		if (path) {
			*_location = path;
			return 0;
		}
	}
	err = -errno;

out:
	free(location);
	free(cpath);
	return err;
}

static int 
vhd_macx_encode_location(char *name, char **out, int *outlen)
{
	iconv_t cd;
	int len, err;
	size_t ibl, obl;
	char *uri, *uri_utf8, *uri_utf8p, *ret;
	const char *urip;

	err     = 0;
	ret     = NULL;
	*out    = NULL;
	*outlen = 0;
	len     = strlen(name) + strlen("file://");

	ibl     = len;
	obl     = len;

	urip = uri = malloc(ibl + 1);
	uri_utf8 = uri_utf8p = malloc(obl);

	if (!uri || !uri_utf8)
		return -ENOMEM;

	cd = iconv_open("UTF-8", "ASCII");
	if (cd == (iconv_t)-1) {
		err = -errno;
		goto out;
	}

	snprintf(uri, ibl+1, "file://%s", name);

	if (iconv(cd,
#ifdef __linux__
	    (char **)
#endif
	    &urip, &ibl, &uri_utf8p, &obl) == (size_t)-1 ||
	    ibl || obl) {
		err = (errno ? -errno : -EIO);
		goto out;
	}

	ret = malloc(len);
	if (!ret) {
		err = -ENOMEM;
		goto out;
	}

	memcpy(ret, uri_utf8, len);
	*outlen = len;
	*out    = ret;

 out:
	free(uri);
	free(uri_utf8);
	if (cd != (iconv_t)-1)
		iconv_close(cd);

	return err;
}

static int
vhd_w2u_encode_location(char *name, char **out, int *outlen)
{
	iconv_t cd;
	int len, err;
	size_t ibl, obl;
	char *uri, *uri_utf16, *uri_utf16p, *tmp, *ret;
	const char *urip;

	err     = 0;
	ret     = NULL;
	*out    = NULL;
	*outlen = 0;
	cd      = (iconv_t) -1;

	/* 
	 * MICROSOFT_COMPAT
	 * relative paths must start with ".\" 
	 */
	if (name[0] != '/') {
		tmp = strstr(name, "./");
		if (tmp == name)
			tmp += strlen("./");
		else
			tmp = name;

		err = asprintf(&uri, ".\\%s", tmp);
	} else
		err = asprintf(&uri, "%s", name);

	if (err == -1)
		return -ENOMEM;

	tmp = uri;
	while (*tmp != '\0') {
		if (*tmp == '/')
			*tmp = '\\';
		tmp++;
	}

	len  = strlen(uri);
	ibl  = len;
	obl  = len * 2;
	urip = uri;

	uri_utf16 = uri_utf16p = malloc(obl);
	if (!uri_utf16) {
		err = -ENOMEM;
		goto out;
	}

	/* 
	 * MICROSOFT_COMPAT
	 * little endian unicode here 
	 */
	cd = iconv_open("UTF-16LE", "ASCII");
	if (cd == (iconv_t)-1) {
		err = -errno;
		goto out;
	}

	if (iconv(cd,
#ifdef __linux__
	    (char **)
#endif
	    &urip, &ibl, &uri_utf16p, &obl) == (size_t)-1 ||
	    ibl || obl) {
		err = (errno ? -errno : -EIO);
		goto out;
	}

	len = len * 2;
	ret = malloc(len);
	if (!ret) {
		err = -ENOMEM;
		goto out;
	}

	memcpy(ret, uri_utf16, len);
	*outlen = len;
	*out    = ret;
	err     = 0;

 out:
	free(uri);
	free(uri_utf16);
	if (cd != (iconv_t)-1)
		iconv_close(cd);

	return err;
}

static char *
vhd_macx_decode_location(const char *in, char *out, int len)
{
	iconv_t cd;
	char *name;
	size_t ibl, obl;

	name = out;
	ibl  = obl = len;

	cd = iconv_open("ASCII", "UTF-8");
	if (cd == (iconv_t)-1) 
		return NULL;

	if (iconv(cd,
#ifdef __linux__
		(char **)
#endif
		&in, &ibl, &out, &obl) == (size_t)-1 || ibl)
		return NULL;

	iconv_close(cd);
	*out = '\0';

	if (strstr(name, "file://") != name)
		return NULL;

	name += strlen("file://");

	return strdup(name);
}

static char *
vhd_w2u_decode_location(const char *in, char *out, int len, char *utf_type)
{
	iconv_t cd;
	char *name, *tmp;
	size_t ibl, obl;

	tmp = name = out;
	ibl = obl  = len;

	cd = iconv_open("ASCII", utf_type);
	if (cd == (iconv_t)-1) 
		return NULL;

	if (iconv(cd,
#ifdef __linux__
		(char **)
#endif
		&in, &ibl, &out, &obl) == (size_t)-1 || ibl)
		return NULL;

	iconv_close(cd);
	*out = '\0';

	/* TODO: spaces */
	while (tmp != out) {
		if (*tmp == '\\')
			*tmp = '/';
		tmp++;
	}

	if (strstr(name, "C:") == name || strstr(name, "c:") == name)
		name += strlen("c:");

	return strdup(name);
}

int
vhd_header_decode_parent(vhd_context_t *ctx, vhd_header_t *header, char **buf)
{
	char *code, out[512];

	if (vhd_creator_tapdisk(ctx) &&
	    ctx->footer.crtr_ver == VHD_VERSION(0, 1))
		code = UTF_16;
	else
		code = UTF_16BE;

	*buf = vhd_w2u_decode_location(header->prt_name, out, 512, code);
	return (*buf == NULL ? -EINVAL : 0);
}

int
vhd_parent_locator_read(vhd_context_t *ctx,
			vhd_parent_locator_t *loc, char **parent)
{
	int err, size;
	char *raw, *out, *name;

	raw     = NULL;
	out     = NULL;
	name    = NULL;
	*parent = NULL;

	if (ctx->footer.type != HD_TYPE_DIFF) {
		err = -EINVAL;
		goto out;
	}

	switch (loc->code) {
	case PLAT_CODE_MACX:
	case PLAT_CODE_W2KU:
	case PLAT_CODE_W2RU:
		break;
	default:
		err = -EINVAL;
		goto out;
	}

	err = vhd_seek(ctx, loc->data_offset, SEEK_SET);
	if (err)
		goto out;

	size = vhd_parent_locator_size(loc);
	if (size <= 0) {
		err = -EINVAL;
		goto out;
	}

	err = posix_memalign((void **)&raw, VHD_SECTOR_SIZE, size);
	if (err) {
		raw = NULL;
		err = -err;
		goto out;
	}

	err = vhd_read(ctx, raw, size);
	if (err)
		goto out;

	out = malloc(loc->data_len + 1);
	if (!out) {
		err = -ENOMEM;
		goto out;
	}

	switch (loc->code) {
	case PLAT_CODE_MACX:
		name = vhd_macx_decode_location(raw, out, loc->data_len);
		break;
	case PLAT_CODE_W2KU:
	case PLAT_CODE_W2RU:
		name = vhd_w2u_decode_location(raw, out,
					       loc->data_len, UTF_16LE);
		break;
	}

	if (!name) {
		err = -EINVAL;
		goto out;
	}

	err     = 0;
	*parent = name;

out:
	free(raw);
	free(out);

	if (err) {
		VHDLOG("%s: error reading parent locator: %d\n",
		       ctx->file, err);
		VHDLOG("%s: locator: code %u, space 0x%x, len 0x%x, "
		       "off 0x%"PRIx64"\n", ctx->file, loc->code, loc->data_space,
		       loc->data_len, loc->data_offset);
	}

	return err;
}

int
vhd_parent_locator_get(vhd_context_t *ctx, char **parent)
{
	int i, n, err;
	char *name, *location;
	vhd_parent_locator_t *loc;

	err     = 0;
	*parent = NULL;

	if (ctx->footer.type != HD_TYPE_DIFF)
		return -EINVAL;

	n = vhd_parent_locator_count(ctx);
	for (i = 0; i < n; i++) {
		loc = ctx->header.loc + i;
		err = vhd_parent_locator_read(ctx, loc, &name);
		if (err)
			continue;

		err = vhd_find_parent(ctx, name, &location);
		if (err)
			VHDLOG("%s: couldn't find parent %s (%d)\n",
			       ctx->file, name, err);
		free(name);

		if (!err) {
			*parent = location;
			return 0;
		}
	}

	return err;
}

int
vhd_parent_locator_write_at(vhd_context_t *ctx,
			    const char *parent, off_t off, uint32_t code,
			    size_t max_bytes, vhd_parent_locator_t *loc)
{
	struct stat stats;
	int err, len, size;
	char *absolute_path, *relative_path, *encoded, *block;

	memset(loc, 0, sizeof(vhd_parent_locator_t));

	if (ctx->footer.type != HD_TYPE_DIFF)
		return -EINVAL;

	absolute_path = NULL;
	relative_path = NULL;
	encoded       = NULL;
	block         = NULL;
	size          = 0;
	len           = 0;

	switch (code) {
	case PLAT_CODE_MACX:
	case PLAT_CODE_W2KU:
	case PLAT_CODE_W2RU:
		break;
	default:
		return -EINVAL;
	}

	absolute_path = realpath(parent, NULL);
	if (!absolute_path) {
		err = -errno;
		goto out;
	}

	err = stat(absolute_path, &stats);
	if (err) {
		err = -errno;
		goto out;
	}

	if (!S_ISREG(stats.st_mode) && !S_ISBLK(stats.st_mode)) {
		err = -EINVAL;
		goto out;
	}

	relative_path = relative_path_to(ctx->file, absolute_path, &err);
	if (!relative_path || err) {
		err = (err ? err : -EINVAL);
		goto out;
	}

	switch (code) {
	case PLAT_CODE_MACX:
		err = vhd_macx_encode_location(relative_path, &encoded, &len);
		break;
	case PLAT_CODE_W2KU:
	case PLAT_CODE_W2RU:
		err = vhd_w2u_encode_location(relative_path, &encoded, &len);
		break;
	default:
		err = -EINVAL;
	}

	if (err)
		goto out;

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	size = vhd_bytes_padded(len);

	if (max_bytes && size > max_bytes) {
		err = -ENAMETOOLONG;
		goto out;
	}

	err  = posix_memalign((void **)&block, VHD_SECTOR_SIZE, size);
	if (err) {
		block = NULL;
		err   = -err;
		goto out;
	}

	memset(block, 0, size);
	memcpy(block, encoded, len);

	err = vhd_write(ctx, block, size);
	if (err)
		goto out;

	err = 0;

out:
	free(absolute_path);
	free(relative_path);
	free(encoded);
	free(block);

	if (!err) {
		loc->res         = 0;
		loc->code        = code;
		loc->data_len    = len;
		/*
		 * write number of bytes ('size') instead of number of sectors
		 * into loc->data_space to be compatible with MSFT, even though
		 * this goes against the specs
		 */
		loc->data_space  = size; 
		loc->data_offset = off;
	}

	return err;
}

static int
vhd_footer_offset_at_eof(vhd_context_t *ctx, off_t *off)
{
	int err;
	if ((err = vhd_seek(ctx, 0, SEEK_END)))
		return errno;
	*off = vhd_position(ctx) - sizeof(vhd_footer_t);
	return 0;
}

int
vhd_read_bitmap(vhd_context_t *ctx, uint32_t block, char **bufp)
{
	int err;
	char *buf;
	size_t size;
	off_t off;
	uint64_t blk;

	buf   = NULL;
	*bufp = NULL;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	err = vhd_get_bat(ctx);
	if (err)
		return err;

	if (block >= ctx->bat.entries)
		return -ERANGE;

	blk  = ctx->bat.bat[block];
	if (blk == DD_BLK_UNUSED)
		return -EINVAL;

	off  = vhd_sectors_to_bytes(blk);
	size = vhd_bytes_padded(ctx->spb >> 3);

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		return err;

	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err)
		return -err;

	err  = vhd_read(ctx, buf, size);
	if (err)
		goto fail;

	*bufp = buf;
	return 0;

fail:
	free(buf);
	return err;
}

int
vhd_read_block(vhd_context_t *ctx, uint32_t block, char **bufp)
{
	int err;
	char *buf;
	size_t size;
	uint64_t blk;
	off_t end, off;

	buf   = NULL;
	*bufp = NULL;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	err = vhd_get_bat(ctx);
	if (err)
		return err;

	if (block >= ctx->bat.entries)
		return -ERANGE;

	blk  = ctx->bat.bat[block];
	if (blk == DD_BLK_UNUSED)
		return -EINVAL;

	off  = vhd_sectors_to_bytes(blk + ctx->bm_secs);
	size = vhd_sectors_to_bytes(ctx->spb);

	err  = vhd_footer_offset_at_eof(ctx, &end);
	if (err)
		return err;

	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		err = -err;
		goto fail;
	}

	if (end < off + ctx->header.block_size) {
		size = end - off;
		memset(buf + size, 0, ctx->header.block_size - size);
	}

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto fail;

	err  = vhd_read(ctx, buf, size);
	if (err)
		goto fail;

	*bufp = buf;
	return 0;

fail:
	free(buf);
	return err;
}

int
vhd_write_footer_at(vhd_context_t *ctx, vhd_footer_t *footer, off_t off)
{
	int err;
	vhd_footer_t *f;

	f = NULL;

	err = posix_memalign((void **)&f,
			     VHD_SECTOR_SIZE, sizeof(vhd_footer_t));
	if (err) {
		f   = NULL;
		err = -err;
		goto out;
	}

	memcpy(f, footer, sizeof(vhd_footer_t));
	f->checksum = vhd_checksum_footer(f);

	err = vhd_validate_footer(f);
	if (err)
		goto out;

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	vhd_footer_out(f);

	err = vhd_write(ctx, f, sizeof(vhd_footer_t));

out:
	if (err)
		VHDLOG("%s: failed writing footer at 0x%08"PRIx64": %d\n",
		       ctx->file, off, err);
	free(f);
	return err;
}

int
vhd_write_footer(vhd_context_t *ctx, vhd_footer_t *footer)
{
	int err;
	off_t off;

	if (ctx->is_block)
		err = vhd_footer_offset_at_eof(ctx, &off);
	else
		err = vhd_end_of_data(ctx, &off);
	if (err)
		return err;

	err = vhd_write_footer_at(ctx, footer, off);
	if (err)
		return err;

	if (!vhd_type_dynamic(ctx))
		return 0;

	return vhd_write_footer_at(ctx, footer, 0);
}

int
vhd_write_header_at(vhd_context_t *ctx, vhd_header_t *header, off_t off)
{
	int err;
	vhd_header_t *h;

	h = NULL;

	if (!vhd_type_dynamic(ctx)) {
		err = -EINVAL;
		goto out;
	}

	err = posix_memalign((void **)&h,
			     VHD_SECTOR_SIZE, sizeof(vhd_header_t));
	if (err) {
		h   = NULL;
		err = -err;
		goto out;
	}

	memcpy(h, header, sizeof(vhd_header_t));

	h->checksum = vhd_checksum_header(h);
	err = vhd_validate_header(h);
	if (err)
		goto out;

	vhd_header_out(h);

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	err = vhd_write(ctx, h, sizeof(vhd_header_t));

out:
	if (err)
		VHDLOG("%s: failed writing header at 0x%08"PRIx64": %d\n",
		       ctx->file, off, err);
	free(h);
	return err;
}

int
vhd_write_header(vhd_context_t *ctx, vhd_header_t *header)
{
	int err;
	off_t off;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	off = ctx->footer.data_offset;
	return vhd_write_header_at(ctx, header, off);
}

int
vhd_write_bat(vhd_context_t *ctx, vhd_bat_t *bat)
{
	int err;
	off_t off;
	vhd_bat_t b;
	size_t size;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	err = vhd_validate_bat(&ctx->bat);
	if (err)
		return err;

	err = vhd_validate_bat(bat);
	if (err)
		return err;

	memset(&b, 0, sizeof(vhd_bat_t));

	off  = ctx->header.table_offset;
	size = vhd_bytes_padded(bat->entries * sizeof(uint32_t));

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		return err;

	err  = posix_memalign((void **)&b.bat, VHD_SECTOR_SIZE, size);
	if (err)
		return -err;

	memcpy(b.bat, bat->bat, size);
	b.spb     = bat->spb;
	b.entries = bat->entries;
	vhd_bat_out(&b);

	err = vhd_write(ctx, b.bat, size);
	free(b.bat);

	return err;
}

int
vhd_write_batmap(vhd_context_t *ctx, vhd_batmap_t *batmap)
{
	int err;
	off_t off;
	vhd_batmap_t b;
	char *buf, *map;
	size_t size, map_size;

	buf      = NULL;
	map      = NULL;

	if (!vhd_has_batmap(ctx)) {
		err = -EINVAL;
		goto out;
	}

	b.header = batmap->header;
	b.map    = batmap->map;

	b.header.checksum = vhd_checksum_batmap(&b);
	err = vhd_validate_batmap(&b);
	if (err)
		goto out;

	off      = b.header.batmap_offset;
	map_size = vhd_sectors_to_bytes(b.header.batmap_size);

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	err  = posix_memalign((void **)&map, VHD_SECTOR_SIZE, map_size);
	if (err) {
		map = NULL;
		err = -err;
		goto out;
	}

	memcpy(map, b.map, map_size);

	err  = vhd_write(ctx, map, map_size);
	if (err)
		goto out;

	err  = vhd_batmap_header_offset(ctx, &off);
	if (err)
		goto out;

	size = vhd_bytes_padded(sizeof(vhd_batmap_header_t));

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		goto out;

	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		err = -err;
		buf = NULL;
		goto out;
	}

	vhd_batmap_header_out(&b);
	memset(buf, 0, size);
	memcpy(buf, &b.header, sizeof(vhd_batmap_header_t));

	err  = vhd_write(ctx, buf, size);

out:
	if (err)
		VHDLOG("%s: failed writing batmap: %d\n", ctx->file, err);
	free(buf);
	free(map);
	return 0;
}

int
vhd_write_bitmap(vhd_context_t *ctx, uint32_t block, char *bitmap)
{
	int err;
	off_t off;
	uint64_t blk;
	size_t secs, size;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	err = vhd_validate_bat(&ctx->bat);
	if (err)
		return err;

	if (block >= ctx->bat.entries)
		return -ERANGE;

	if ((unsigned long)bitmap & (VHD_SECTOR_SIZE - 1))
		return -EINVAL;

	blk  = ctx->bat.bat[block];
	if (blk == DD_BLK_UNUSED)
		return -EINVAL;

	off  = vhd_sectors_to_bytes(blk);
	size = vhd_sectors_to_bytes(ctx->bm_secs);

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		return err;

	err  = vhd_write(ctx, bitmap, size);
	if (err)
		return err;

	return 0;
}

int
vhd_write_block(vhd_context_t *ctx, uint32_t block, char *data)
{
	int err;
	off_t off;
	size_t size;
	uint64_t blk;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	err = vhd_validate_bat(&ctx->bat);
	if (err)
		return err;

	if (block >= ctx->bat.entries)
		return -ERANGE;

	if ((unsigned long)data & ~(VHD_SECTOR_SIZE -1))
		return -EINVAL;

	blk  = ctx->bat.bat[block];
	if (blk == DD_BLK_UNUSED)
		return -EINVAL;

	off  = vhd_sectors_to_bytes(blk + ctx->bm_secs);
	size = vhd_sectors_to_bytes(ctx->spb);

	err  = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		return err;

	err  = vhd_write(ctx, data, size);
	if (err)
		return err;

	return 0;
}

static inline int
namedup(char **dup, const char *name)
{
	*dup = NULL;

	if (strnlen(name, MAX_NAME_LEN) >= MAX_NAME_LEN)
		return -ENAMETOOLONG;
	
	*dup = strdup(name);
	if (*dup == NULL)
		return -ENOMEM;

	return 0;
}

int
vhd_seek(vhd_context_t *ctx, off_t offset, int whence)
{
	off_t off;

	off = lseek(ctx->fd, offset, whence);
	if (off == (off_t)-1) {
		VHDLOG("%s: seek(0x%08"PRIx64", %d) failed: %d\n",
		       ctx->file, offset, whence, -errno);
		return -errno;
	}

	return 0;
}

off_t
vhd_position(vhd_context_t *ctx)
{
	return lseek(ctx->fd, 0, SEEK_CUR);
}

int
vhd_read(vhd_context_t *ctx, void *buf, size_t size)
{
	size_t ret;

	errno = 0;

	ret = read(ctx->fd, buf, size);
	if (ret == size)
		return 0;

	VHDLOG("%s: read of %zu returned %zd, errno: %d\n",
	       ctx->file, size, ret, -errno);

	return (errno ? -errno : -EIO);
}

int
vhd_write(vhd_context_t *ctx, void *buf, size_t size)
{
	size_t ret;

	errno = 0;

	ret = write(ctx->fd, buf, size);
	if (ret == size)
		return 0;

	VHDLOG("%s: write of %zu returned %zd, errno: %d\n",
	       ctx->file, size, ret, -errno);

	return (errno ? -errno : -EIO);
}

int
vhd_offset(vhd_context_t *ctx, uint32_t sector, uint32_t *offset)
{
	int err;
	uint32_t block;

	if (!vhd_type_dynamic(ctx))
		return sector;

	err = vhd_get_bat(ctx);
	if (err)
		return err;

	block = sector / ctx->spb;
	if (ctx->bat.bat[block] == DD_BLK_UNUSED)
		*offset = DD_BLK_UNUSED;
	else
		*offset = ctx->bat.bat[block] +
			ctx->bm_secs + (sector % ctx->spb);

	return 0;
}

int
vhd_open_fast(vhd_context_t *ctx)
{
	int err;
	char *buf;
	size_t size;

	size = sizeof(vhd_footer_t) + sizeof(vhd_header_t);
	err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		VHDLOG("failed allocating %s: %d\n", ctx->file, -err);
		return -err;
	}

	err = vhd_read(ctx, buf, size);
	if (err) {
		VHDLOG("failed reading %s: %d\n", ctx->file, err);
		goto out;
	}

	memcpy(&ctx->footer, buf, sizeof(vhd_footer_t));
	vhd_footer_in(&ctx->footer);
	err = vhd_validate_footer(&ctx->footer);
	if (err)
		goto out;

	if (vhd_type_dynamic(ctx)) {
		if (ctx->footer.data_offset != sizeof(vhd_footer_t))
			err = vhd_read_header(ctx, &ctx->header);
		else {
			memcpy(&ctx->header,
			       buf + sizeof(vhd_footer_t),
			       sizeof(vhd_header_t));
			vhd_header_in(&ctx->header);
			err = vhd_validate_header(&ctx->header);
		}

		if (err)
			goto out;

		ctx->spb     = ctx->header.block_size >> VHD_SECTOR_SHIFT;
		ctx->bm_secs = secs_round_up_no_zero(ctx->spb >> 3);
	}

out:
	free(buf);
	return err;
}

int
vhd_open(vhd_context_t *ctx, const char *file, int flags)
{
	int err, oflags;

	if (flags & VHD_OPEN_STRICT)
		vhd_flag_clear(flags, VHD_OPEN_FAST);

	memset(ctx, 0, sizeof(vhd_context_t));
	ctx->fd     = -1;
	ctx->oflags = flags;

	err = namedup(&ctx->file, file);
	if (err)
		return err;

	oflags = O_DIRECT | O_LARGEFILE;
	if (flags & VHD_OPEN_RDONLY)
		oflags |= O_RDONLY;
	if (flags & VHD_OPEN_RDWR)
		oflags |= O_RDWR;

	ctx->fd = open(ctx->file, oflags, 0644);
	if (ctx->fd == -1) {
		err = -errno;
		VHDLOG("failed to open %s: %d\n", ctx->file, err);
		goto fail;
	}

	err = vhd_test_file_fixed(ctx->file, &ctx->is_block);
	if (err)
		goto fail;

	if (flags & VHD_OPEN_FAST) {
		err = vhd_open_fast(ctx);
		if (err)
			goto fail;

		return 0;
	}

	err = vhd_read_footer(ctx, &ctx->footer);
	if (err)
		goto fail;

	if (!(flags & VHD_OPEN_IGNORE_DISABLED) && vhd_disabled(ctx)) {
		err = -EINVAL;
		goto fail;
	}

	if (vhd_type_dynamic(ctx)) {
		err = vhd_read_header(ctx, &ctx->header);
		if (err)
			goto fail;

		ctx->spb     = ctx->header.block_size >> VHD_SECTOR_SHIFT;
		ctx->bm_secs = secs_round_up_no_zero(ctx->spb >> 3);
	}

	return 0;

fail:
	if (ctx->fd != -1)
		close(ctx->fd);
	free(ctx->file);
	memset(ctx, 0, sizeof(vhd_context_t));
	return err;
}

void
vhd_close(vhd_context_t *ctx)
{
	if (ctx->file)
		close(ctx->fd);
	free(ctx->file);
	free(ctx->bat.bat);
	free(ctx->batmap.map);
	memset(ctx, 0, sizeof(vhd_context_t));
}

static inline void
vhd_initialize_footer(vhd_context_t *ctx, int type, uint64_t size)
{
	memset(&ctx->footer, 0, sizeof(vhd_footer_t));
	memcpy(ctx->footer.cookie, HD_COOKIE, sizeof(ctx->footer.cookie));
	ctx->footer.features     = HD_RESERVED;
	ctx->footer.ff_version   = HD_FF_VERSION;
	ctx->footer.timestamp    = vhd_time(time(NULL));
	ctx->footer.crtr_ver     = VHD_CURRENT_VERSION;
	ctx->footer.crtr_os      = 0x00000000;
	ctx->footer.orig_size    = size;
	ctx->footer.curr_size    = size;
	ctx->footer.geometry     = vhd_chs(size);
	ctx->footer.type         = type;
	ctx->footer.saved        = 0;
	ctx->footer.data_offset  = 0xFFFFFFFFFFFFFFFF;
	strcpy(ctx->footer.crtr_app, "tap");
	blk_uuid_generate(&ctx->footer.uuid);
}

static int
vhd_initialize_header_parent_name(vhd_context_t *ctx, const char *parent_path)
{
	int err;
	iconv_t cd;
	size_t ibl, obl;
	char *ppath, *dst;
	const char *pname;

	err   = 0;
	pname = NULL;
	ppath = NULL;

	/*
	 * MICROSOFT_COMPAT
	 * big endian unicode here 
	 */
	cd = iconv_open(UTF_16BE, "ASCII");
	if (cd == (iconv_t)-1) {
		err = -errno;
		goto out;
	}

	ppath = strdup(parent_path);
	if (!ppath) {
		err = -ENOMEM;
		goto out;
	}

	pname = basename(ppath);
	if (!strcmp(pname, "")) {
		err = -EINVAL;
		goto out;
	}

	ibl = strlen(pname);
	obl = sizeof(ctx->header.prt_name);
	dst = ctx->header.prt_name;

	memset(dst, 0, obl);

	if (iconv(cd,
#ifdef __linux__
		(char **)
#endif
		&pname, &ibl, &dst, &obl) == (size_t)-1 || ibl)
		err = (errno ? -errno : -EINVAL);

out:
	iconv_close(cd);
	free(ppath);
	return err;
}

static off_t
get_file_size(const char *name)
{
	int fd;
	off_t end;

	fd = open(name, O_LARGEFILE | O_RDONLY);
	if (fd == -1) {
		VHDLOG("unable to open '%s': %d\n", name, errno);
		return -errno;
	}
	end = lseek(fd, 0, SEEK_END);
	close(fd); 
	return end;
}

static int
vhd_initialize_header(vhd_context_t *ctx, const char *parent_path, 
		uint64_t size, int raw)
{
	int err;
	struct stat stats;
	vhd_context_t parent;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	memset(&ctx->header, 0, sizeof(vhd_header_t));
	memcpy(ctx->header.cookie, DD_COOKIE, sizeof(ctx->header.cookie));
	ctx->header.data_offset  = (uint64_t)-1;
	ctx->header.table_offset = VHD_SECTOR_SIZE * 3; /* 1 ftr + 2 hdr */
	ctx->header.hdr_ver      = DD_VERSION;
	ctx->header.block_size   = VHD_BLOCK_SIZE;
	ctx->header.prt_ts       = 0;
	ctx->header.res1         = 0;
	ctx->header.max_bat_size = (ctx->footer.curr_size +
				    VHD_BLOCK_SIZE - 1) >> VHD_BLOCK_SHIFT;

	ctx->footer.data_offset  = VHD_SECTOR_SIZE;

	if (ctx->footer.type == HD_TYPE_DYNAMIC)
		return 0;

	err = stat(parent_path, &stats);
	if (err == -1)
		return -errno;

	if (raw) {
		ctx->header.prt_ts = vhd_time(stats.st_mtime);
		if (!size)
			size = get_file_size(parent_path);
	}
	else {
		err = vhd_open(&parent, parent_path, VHD_OPEN_RDONLY);
		if (err)
			return err;

		ctx->header.prt_ts = vhd_time(stats.st_mtime);
		blk_uuid_copy(&ctx->header.prt_uuid, &parent.footer.uuid);
		if (!size)
			size = parent.footer.curr_size;
		vhd_close(&parent);
	}
	ctx->footer.orig_size    = size;
	ctx->footer.curr_size    = size;
	ctx->footer.geometry     = vhd_chs(size);
	ctx->header.max_bat_size = 
		(size + VHD_BLOCK_SIZE - 1) >> VHD_BLOCK_SHIFT;

	return vhd_initialize_header_parent_name(ctx, parent_path);
}

static int
vhd_write_parent_locators(vhd_context_t *ctx, const char *parent)
{
	int i, err;
	off_t off;
	uint32_t code;

	code = PLAT_CODE_NONE;

	if (ctx->footer.type != HD_TYPE_DIFF)
		return -EINVAL;

	off = ctx->batmap.header.batmap_offset + 
		vhd_sectors_to_bytes(ctx->batmap.header.batmap_size);
	if (off & (VHD_SECTOR_SIZE - 1))
		off = vhd_bytes_padded(off);

	for (i = 0; i < 3; i++) {
		switch (i) {
		case 0:
			code = PLAT_CODE_MACX;
			break;
		case 1:
			code = PLAT_CODE_W2KU;
			break;
		case 2:
			code = PLAT_CODE_W2RU;
			break;
		}

		err = vhd_parent_locator_write_at(ctx, parent, off, code,
						  0, ctx->header.loc + i);
		if (err)
			return err;

		off += vhd_parent_locator_size(ctx->header.loc + i);
	}

	return 0;
}

int
vhd_change_parent(vhd_context_t *child, char *parent_path, int raw)
{
	int i, err;
	char *ppath;
	struct stat stats;
	vhd_context_t parent;

	ppath = realpath(parent_path, NULL);
	if (!ppath) {
		VHDLOG("error resolving parent path %s for %s: %d\n",
		       parent_path, child->file, errno);
		return -errno;
	}

	err = stat(ppath, &stats);
	if (err == -1) {
		err = -errno;
		goto out;
	}

	if (!S_ISREG(stats.st_mode) && !S_ISBLK(stats.st_mode)) {
		err = -EINVAL;
		goto out;
	}

	if (raw) {
		blk_uuid_clear(&child->header.prt_uuid);
	} else {
		err = vhd_open(&parent, ppath, VHD_OPEN_RDONLY);
		if (err) {
			VHDLOG("error opening parent %s for %s: %d\n",
			       ppath, child->file, err);
			goto out;
		}
		blk_uuid_copy(&child->header.prt_uuid, &parent.footer.uuid);
		vhd_close(&parent);
	}

	vhd_initialize_header_parent_name(child, ppath);
	child->header.prt_ts = vhd_time(stats.st_mtime);

	for (i = 0; i < vhd_parent_locator_count(child); i++) {
		vhd_parent_locator_t *loc = child->header.loc + i;
		size_t max = vhd_parent_locator_size(loc);

		switch (loc->code) {
		case PLAT_CODE_MACX:
		case PLAT_CODE_W2KU:
		case PLAT_CODE_W2RU:
			break;
		default:
			continue;
		}

		err = vhd_parent_locator_write_at(child, ppath,
						  loc->data_offset,
						  loc->code, max, loc);
		if (err) {
			VHDLOG("error writing parent locator %d for %s: %d\n",
			       i, child->file, err);
			goto out;
		}
	}

	TEST_FAIL_AT(FAIL_REPARENT_LOCATOR);

	err = vhd_write_header(child, &child->header);
	if (err) {
		VHDLOG("error writing header for %s: %d\n", child->file, err);
		goto out;
	}

	err = 0;

out:
	free(ppath);
	return err;
}

static int
vhd_create_batmap(vhd_context_t *ctx)
{
	off_t off;
	int err, map_bytes;
	vhd_batmap_header_t *header;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	map_bytes = (ctx->header.max_bat_size + 7) >> 3;
	header    = &ctx->batmap.header;

	memset(header, 0, sizeof(vhd_batmap_header_t));
	memcpy(header->cookie, VHD_BATMAP_COOKIE, sizeof(header->cookie));

	err = vhd_batmap_header_offset(ctx, &off);
	if (err)
		return err;

	header->batmap_offset  = off +
		vhd_bytes_padded(sizeof(vhd_batmap_header_t));
	header->batmap_size    = secs_round_up_no_zero(map_bytes);
	header->batmap_version = VHD_BATMAP_CURRENT_VERSION;

	map_bytes = vhd_sectors_to_bytes(header->batmap_size);

	err = posix_memalign((void **)&ctx->batmap.map,
			     VHD_SECTOR_SIZE, map_bytes);
	if (err) {
		ctx->batmap.map = NULL;
		return -err;
	}

	memset(ctx->batmap.map, 0, map_bytes);

	return vhd_write_batmap(ctx, &ctx->batmap);
}

static int
vhd_create_bat(vhd_context_t *ctx)
{
	int i, err;
	size_t size;

	if (!vhd_type_dynamic(ctx))
		return -EINVAL;

	size = vhd_bytes_padded(ctx->header.max_bat_size * sizeof(uint32_t));
	err  = posix_memalign((void **)&ctx->bat.bat, VHD_SECTOR_SIZE, size);
	if (err) {
		ctx->bat.bat = NULL;
		return err;
	}

	memset(ctx->bat.bat, 0, size);
	for (i = 0; i < ctx->header.max_bat_size; i++)
		ctx->bat.bat[i] = DD_BLK_UNUSED;

	err = vhd_seek(ctx, ctx->header.table_offset, SEEK_SET);
	if (err)
		return err;

	ctx->bat.entries = ctx->header.max_bat_size;
	ctx->bat.spb     = ctx->header.block_size >> VHD_SECTOR_SHIFT;

	return vhd_write_bat(ctx, &ctx->bat);
}

static int
vhd_initialize_fixed_disk(vhd_context_t *ctx)
{
	char *buf;
	int i, err;

	if (ctx->footer.type != HD_TYPE_FIXED)
		return -EINVAL;

	err = vhd_seek(ctx, 0, SEEK_SET);
	if (err)
		return err;

	buf = mmap(0, VHD_BLOCK_SIZE, PROT_READ,
		   MAP_SHARED | MAP_ANON, -1, 0);
	if (buf == MAP_FAILED)
		return -errno;

	for (i = 0; i < ctx->footer.curr_size >> VHD_BLOCK_SHIFT; i++) {
		err = vhd_write(ctx, buf, VHD_BLOCK_SIZE);
		if (err)
			goto out;
	}

	err = 0;

out:
	munmap(buf, VHD_BLOCK_SIZE);
	return err;
}

int 
vhd_get_phys_size(vhd_context_t *ctx, off_t *size)
{
	int err;

	if ((err = vhd_end_of_data(ctx, size)))
		return err;
	*size += sizeof(vhd_footer_t);
	return 0;
}

int 
vhd_set_phys_size(vhd_context_t *ctx, off_t size)
{
	off_t phys_size;
	int err;

	err = vhd_get_phys_size(ctx, &phys_size);
	if (err)
		return err;
	if (size < phys_size) {
		// would result in data loss
		VHDLOG("ERROR: new size (%"PRIu64") < phys size (%"PRIu64")\n",
				size, phys_size);
		return -EINVAL;
	}
	return vhd_write_footer_at(ctx, &ctx->footer, 
			size - sizeof(vhd_footer_t));
}

static int
__vhd_create(const char *name, const char *parent, uint64_t bytes, int type,
		vhd_flag_creat_t flags)
{
	int err;
	off_t off;
	vhd_context_t ctx;
	vhd_footer_t *footer;
	vhd_header_t *header;
	uint64_t size, blks;

	switch (type) {
	case HD_TYPE_DIFF:
		if (!parent)
			return -EINVAL;
	case HD_TYPE_FIXED:
	case HD_TYPE_DYNAMIC:
		break;
	default:
		return -EINVAL;
	}

	if (strnlen(name, VHD_MAX_NAME_LEN - 1) == VHD_MAX_NAME_LEN - 1)
		return -ENAMETOOLONG;

	memset(&ctx, 0, sizeof(vhd_context_t));
	footer = &ctx.footer;
	header = &ctx.header;
	blks   = (bytes + VHD_BLOCK_SIZE - 1) >> VHD_BLOCK_SHIFT;
	size   = blks << VHD_BLOCK_SHIFT;

	ctx.fd = open(name, O_WRONLY | O_CREAT |
		      O_TRUNC | O_LARGEFILE | O_DIRECT, 0644);
	if (ctx.fd == -1)
		return -errno;

	ctx.file = strdup(name);
	if (!ctx.file) {
		err = -ENOMEM;
		goto out;
	}

	err = vhd_test_file_fixed(ctx.file, &ctx.is_block);
	if (err)
		goto out;

	vhd_initialize_footer(&ctx, type, size);

	if (type == HD_TYPE_FIXED) {
		err = vhd_initialize_fixed_disk(&ctx);
		if (err)
			goto out;
	} else {
		int raw = vhd_flag_test(flags, VHD_FLAG_CREAT_PARENT_RAW);
		err = vhd_initialize_header(&ctx, parent, size, raw);
		if (err)
			goto out;

		err = vhd_write_footer_at(&ctx, &ctx.footer, 0);
		if (err)
			goto out;

		err = vhd_write_header_at(&ctx, &ctx.header, VHD_SECTOR_SIZE);
		if (err)
			goto out;

		err = vhd_create_batmap(&ctx);
		if (err)
			goto out;

		err = vhd_create_bat(&ctx);
		if (err)
			goto out;

		if (type == HD_TYPE_DIFF) {
			err = vhd_write_parent_locators(&ctx, parent);
			if (err)
				goto out;
		}

		/* write header again since it may have changed */
		err = vhd_write_header_at(&ctx, &ctx.header, VHD_SECTOR_SIZE);
		if (err)
			goto out;
	}

	err = vhd_seek(&ctx, 0, SEEK_END);
	if (err)
		goto out;

	off = vhd_position(&ctx);
	if (off == (off_t)-1) {
		err = -errno;
		goto out;
	}

	if (ctx.is_block)
		off -= sizeof(vhd_footer_t);

	err = vhd_write_footer_at(&ctx, &ctx.footer, off);
	if (err)
		goto out;

	err = 0;

out:
	vhd_close(&ctx);
	if (err && !ctx.is_block)
		unlink(name);
	return err;
}

int
vhd_create(const char *name, uint64_t bytes, int type, vhd_flag_creat_t flags)
{
	return __vhd_create(name, NULL, bytes, type, flags);
}

int
vhd_snapshot(const char *name, uint64_t bytes, const char *parent,
		vhd_flag_creat_t flags)
{
	return __vhd_create(name, parent, bytes, HD_TYPE_DIFF, flags);
}

static int
__vhd_io_fixed_read(vhd_context_t *ctx,
		    char *buf, uint64_t sec, uint32_t secs)
{
	int err;

	err = vhd_seek(ctx, vhd_sectors_to_bytes(sec), SEEK_SET);
	if (err)
		return err;

	return vhd_read(ctx, buf, vhd_sectors_to_bytes(secs));
}

static void
__vhd_io_dynamic_copy_data(vhd_context_t *ctx,
			   char *map, int map_off,
			   char *bitmap, int bitmap_off,
			   char *dst, char *src, int secs)
{
	int i;

	for (i = 0; i < secs; i++) {
		if (test_bit(map, map_off + i))
			goto next;

		if (ctx && !vhd_bitmap_test(ctx, bitmap, bitmap_off + i))
			goto next;

		memcpy(dst, src, VHD_SECTOR_SIZE);
		set_bit(map, map_off + i);

	next:
		src += VHD_SECTOR_SIZE;
		dst += VHD_SECTOR_SIZE;
	}
}

static int
__vhd_io_dynamic_read_link(vhd_context_t *ctx, char *map,
			   char *buf, uint64_t sector, uint32_t secs)
{
	off_t off;
	uint32_t blk, sec;
	int err, cnt, map_off;
	char *bitmap, *data, *src;

	map_off = 0;

	do {
		blk    = sector / ctx->spb;
		sec    = sector % ctx->spb;
		off    = ctx->bat.bat[blk];
		data   = NULL;
		bitmap = NULL;

		if (off == DD_BLK_UNUSED) {
			cnt = MIN(secs, ctx->spb);
			goto next;
		}

		err = vhd_read_bitmap(ctx, blk, &bitmap);
		if (err)
			return err;

		err = vhd_read_block(ctx, blk, &data);
		if (err) {
			free(bitmap);
			return err;
		}

		cnt = MIN(secs, ctx->spb - sec);
		src = data + vhd_sectors_to_bytes(sec);

		__vhd_io_dynamic_copy_data(ctx,
					   map, map_off,
					   bitmap, sec,
					   buf, src, cnt);

	next:
		free(data);
		free(bitmap);

		secs    -= cnt;
		sector  += cnt;
		map_off += cnt;
		buf     += vhd_sectors_to_bytes(cnt);

	} while (secs);

	return 0;
}

static int
__raw_read_link(char *filename,
		char *map, char *buf, uint64_t sec, uint32_t secs)
{
	int fd, err;
	off_t off;
	uint64_t size;
	char *data;

	err = 0;
	errno = 0;
	fd = open(filename, O_RDONLY | O_DIRECT | O_LARGEFILE);
	if (fd == -1) {
		VHDLOG("%s: failed to open: %d\n", filename, -errno);
		return -errno;
	}

	off = lseek(fd, vhd_sectors_to_bytes(sec), SEEK_SET);
	if (off == (off_t)-1) {
		VHDLOG("%s: seek(0x%08"PRIx64") failed: %d\n",
		       filename, vhd_sectors_to_bytes(sec), -errno);
		err = -errno;
		goto close;
	}

	size = vhd_sectors_to_bytes(secs);
	err = posix_memalign((void **)&data, VHD_SECTOR_SIZE, size);
	if (err)
		goto close;

	err = read(fd, data, size);
	if (err != size) {
		VHDLOG("%s: reading of %"PRIu64" returned %d, errno: %d\n",
				filename, size, err, -errno);
		free(data);
		err = errno ? -errno : -EIO;
		goto close;
	}
	__vhd_io_dynamic_copy_data(NULL, map, 0, NULL, 0, buf, data, secs);
	free(data);
	err = 0;

close:
	close(fd);
	return err;
}

static int
__vhd_io_dynamic_read(vhd_context_t *ctx,
		      char *buf, uint64_t sec, uint32_t secs)
{
	int err;
	uint32_t i, done;
	char *map, *next;
	vhd_context_t parent, *vhd;

	err  = vhd_get_bat(ctx);
	if (err)
		return err;

	vhd  = ctx;
	next = NULL;
	map  = calloc(1, secs << (VHD_SECTOR_SHIFT - 3));
	if (!map)
		return -ENOMEM;

	memset(buf, 0, vhd_sectors_to_bytes(secs));

	for (;;) {
		err = __vhd_io_dynamic_read_link(vhd, map, buf, sec, secs);
		if (err)
			goto close;

		for (done = 0, i = 0; i < secs; i++)
			if (test_bit(map, i))
				done++;

		if (done == secs) {
			err = 0;
			goto close;
		}

		if (vhd->footer.type == HD_TYPE_DIFF) {
			err = vhd_parent_locator_get(vhd, &next);
			if (err)
				goto close;
			if (vhd_parent_raw(vhd)) {
				err = __raw_read_link(next, map, buf, sec,
						secs);
				goto close;
			}
		} else {
			err = 0;
			goto close;
		}

		if (vhd != ctx)
			vhd_close(vhd);
		vhd = &parent;

		err = vhd_open(vhd, next, VHD_OPEN_RDONLY);
		if (err)
			goto out;

		err = vhd_get_bat(vhd);
		if (err)
			goto close;

		free(next);
		next = NULL;
	}

close:
	if (vhd != ctx)
		vhd_close(vhd);
out:
	free(map);
	free(next);
	return err;
}

int
vhd_io_read(vhd_context_t *ctx, char *buf, uint64_t sec, uint32_t secs)
{
	if (vhd_sectors_to_bytes(sec + secs) > ctx->footer.curr_size)
		return -ERANGE;

	if (!vhd_type_dynamic(ctx))
		return __vhd_io_fixed_read(ctx, buf, sec, secs);

	return __vhd_io_dynamic_read(ctx, buf, sec, secs);
}

static int
__vhd_io_fixed_write(vhd_context_t *ctx,
		     char *buf, uint64_t sec, uint32_t secs)
{
	int err;

	err = vhd_seek(ctx, vhd_sectors_to_bytes(sec), SEEK_SET);
	if (err)
		return err;

	return vhd_write(ctx, buf, vhd_sectors_to_bytes(secs));
}

static int
__vhd_io_allocate_block(vhd_context_t *ctx, uint32_t block)
{
	char *buf;
	size_t size;
	off_t off, max;
	int i, err, gap, spp;

	spp = getpagesize() >> VHD_SECTOR_SHIFT;

	err = vhd_end_of_data(ctx, &max);
	if (err)
		return err;

	gap   = 0;
	off   = max;
	max >>= VHD_SECTOR_SHIFT;

	/* data region of segment should begin on page boundary */
	if ((max + ctx->bm_secs) % spp) {
		gap  = (spp - ((max + ctx->bm_secs) % spp));
		max += gap;
	}

	err = vhd_seek(ctx, off, SEEK_SET);
	if (err)
		return err;

	size = vhd_sectors_to_bytes(ctx->spb + ctx->bm_secs + gap);
	buf  = mmap(0, size, PROT_READ, MAP_SHARED | MAP_ANON, -1, 0);
	if (buf == MAP_FAILED)
		return -errno;

	err = vhd_write(ctx, buf, size);
	if (err)
		goto out;

	ctx->bat.bat[block] = max;
	err = vhd_write_bat(ctx, &ctx->bat);
	if (err)
		goto out;

	err = 0;

out:
	munmap(buf, size);
	return err;
}

static int
__vhd_io_dynamic_write(vhd_context_t *ctx,
		       char *buf, uint64_t sector, uint32_t secs)
{
	char *map;
	off_t off;
	uint32_t blk, sec;
	int i, err, cnt, ret;

	if (vhd_sectors_to_bytes(sector + secs) > ctx->footer.curr_size)
		return -ERANGE;

	err = vhd_get_bat(ctx);
	if (err)
		return err;

	if (vhd_has_batmap(ctx)) {
		err = vhd_get_batmap(ctx);
		if (err)
			return err;
	}

	do {
		blk = sector / ctx->spb;
		sec = sector % ctx->spb;

		off = ctx->bat.bat[blk];
		if (off == DD_BLK_UNUSED) {
			err = __vhd_io_allocate_block(ctx, blk);
			if (err)
				return err;

			off = ctx->bat.bat[blk];
		}

		off += ctx->bm_secs + sec;
		err  = vhd_seek(ctx, vhd_sectors_to_bytes(off), SEEK_SET);
		if (err)
			return err;

		cnt = MIN(secs, ctx->spb - sec);
		err = vhd_write(ctx, buf, vhd_sectors_to_bytes(cnt));
		if (err)
			return err;

		if (vhd_has_batmap(ctx) &&
		    vhd_batmap_test(ctx, &ctx->batmap, blk))
			goto next;

		err = vhd_read_bitmap(ctx, blk, &map);
		if (err)
			return err;

		for (i = 0; i < cnt; i++)
			vhd_bitmap_set(ctx, map, sec + i);

		err = vhd_write_bitmap(ctx, blk, map);
		if (err)
			goto fail;

		if (vhd_has_batmap(ctx)) {
			for (i = 0; i < ctx->spb; i++)
				if (!vhd_bitmap_test(ctx, map, i)) {
					free(map);
					goto next;
				}

			vhd_batmap_set(ctx, &ctx->batmap, blk);
			err = vhd_write_batmap(ctx, &ctx->batmap);
			if (err)
				goto fail;
		}

		free(map);
		map = NULL;

	next:
		secs   -= cnt;
		sector += cnt;
		buf    += vhd_sectors_to_bytes(cnt);
	} while (secs);

	err = 0;

out:
	ret = vhd_write_footer(ctx, &ctx->footer);
	return (err ? err : ret);

fail:
	free(map);
	goto out;
}

int
vhd_io_write(vhd_context_t *ctx, char *buf, uint64_t sec, uint32_t secs)
{
	if (vhd_sectors_to_bytes(sec + secs) > ctx->footer.curr_size)
		return -ERANGE;

	if (!vhd_type_dynamic(ctx))
		return __vhd_io_fixed_write(ctx, buf, sec, secs);

	return __vhd_io_dynamic_write(ctx, buf, sec, secs);
}
