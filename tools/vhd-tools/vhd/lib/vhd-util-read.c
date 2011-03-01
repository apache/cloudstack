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
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>

#include "libvhd.h"
#include "vhd-util.h"

#define nsize     15
static char nbuf[nsize];

static inline char *
__xconv(uint64_t num)
{
	snprintf(nbuf, nsize, "%#" PRIx64 , num);
	return nbuf;
}

static inline char *
__dconv(uint64_t num)
{
	snprintf(nbuf, nsize, "%" PRIu64, num);
	return nbuf;
}

#define conv(hex, num) \
	(hex ? __xconv((uint64_t)num) : __dconv((uint64_t)num))

static void
vhd_print_header(vhd_context_t *vhd, vhd_header_t *h, int hex)
{
	int err;
	uint32_t  cksm;
	char      uuid[39], time_str[26], cookie[9], out[512], *name;

	printf("VHD Header Summary:\n-------------------\n");

	snprintf(cookie, sizeof(cookie), "%s", h->cookie);
	printf("Cookie              : %s\n", cookie);

	printf("Data offset (unusd) : %s\n", conv(hex, h->data_offset));
	printf("Table offset        : %s\n", conv(hex, h->table_offset));
	printf("Header version      : 0x%08x\n", h->hdr_ver);
	printf("Max BAT size        : %s\n", conv(hex, h->max_bat_size));
	printf("Block size          : %s ", conv(hex, h->block_size));
	printf("(%s MB)\n", conv(hex, h->block_size >> 20));

	err = vhd_header_decode_parent(vhd, h, &name);
	printf("Parent name         : %s\n",
	       (err ? "failed to read name" : name));
	free(name);

	blk_uuid_to_string(&h->prt_uuid, uuid, sizeof(uuid));
	printf("Parent UUID         : %s\n", uuid);
    
	vhd_time_to_string(h->prt_ts, time_str);
	printf("Parent timestamp    : %s\n", time_str);

	cksm = vhd_checksum_header(h);
	printf("Checksum            : 0x%x|0x%x (%s)\n", h->checksum, cksm,
		h->checksum == cksm ? "Good!" : "Bad!");
	printf("\n");
}

static void
vhd_print_footer(vhd_footer_t *f, int hex)
{
	uint64_t  c, h, s;
	uint32_t  ff_maj, ff_min, cr_maj, cr_min, cksm, cksm_save;
	char      time_str[26], creator[5], uuid[39], cookie[9];

	printf("VHD Footer Summary:\n-------------------\n");

	snprintf(cookie, sizeof(cookie), "%s", f->cookie);
	printf("Cookie              : %s\n", cookie);

	printf("Features            : (0x%08x) %s%s\n", f->features,
		(f->features & HD_TEMPORARY) ? "<TEMP>" : "",
		(f->features & HD_RESERVED)  ? "<RESV>" : "");

	ff_maj = f->ff_version >> 16;
	ff_min = f->ff_version & 0xffff;
	printf("File format version : Major: %d, Minor: %d\n", 
		ff_maj, ff_min);

	printf("Data offset         : %s\n", conv(hex, f->data_offset));

	vhd_time_to_string(f->timestamp, time_str);
	printf("Timestamp           : %s\n", time_str);

	memcpy(creator, f->crtr_app, 4);
	creator[4] = '\0';
	printf("Creator Application : '%s'\n", creator);

	cr_maj = f->crtr_ver >> 16;
	cr_min = f->crtr_ver & 0xffff;
	printf("Creator version     : Major: %d, Minor: %d\n",
		cr_maj, cr_min);

	printf("Creator OS          : %s\n",
		((f->crtr_os == HD_CR_OS_WINDOWS) ? "Windows" :
		 ((f->crtr_os == HD_CR_OS_MACINTOSH) ? "Macintosh" : 
		  "Unknown!")));

	printf("Original disk size  : %s MB ", conv(hex, f->orig_size >> 20));
	printf("(%s Bytes)\n", conv(hex, f->orig_size));

	printf("Current disk size   : %s MB ", conv(hex, f->curr_size >> 20));
	printf("(%s Bytes)\n", conv(hex, f->curr_size));

	c = f->geometry >> 16;
	h = (f->geometry & 0x0000FF00) >> 8;
	s = f->geometry & 0x000000FF;
	printf("Geometry            : Cyl: %s, ", conv(hex, c));
	printf("Hds: %s, ", conv(hex, h));
	printf("Sctrs: %s\n", conv(hex, s));
	printf("                    : = %s MB ", conv(hex, (c * h * s) >> 11));
	printf("(%s Bytes)\n", conv(hex, c * h * s << 9));

	printf("Disk type           : %s\n", 
		f->type <= HD_TYPE_MAX ? 
		HD_TYPE_STR[f->type] : "Unknown type!\n");

	cksm = vhd_checksum_footer(f);
	printf("Checksum            : 0x%x|0x%x (%s)\n", f->checksum, cksm,
		f->checksum == cksm ? "Good!" : "Bad!");

	blk_uuid_to_string(&f->uuid, uuid, sizeof(uuid));
	printf("UUID                : %s\n", uuid);

	printf("Saved state         : %s\n", f->saved == 0 ? "No" : "Yes");
	printf("Hidden              : %d\n", f->hidden);
	printf("\n");
}

static inline char *
code_name(uint32_t code)
{
	switch(code) {
	case PLAT_CODE_NONE:
		return "PLAT_CODE_NONE";
	case PLAT_CODE_WI2R:
		return "PLAT_CODE_WI2R";
	case PLAT_CODE_WI2K:
		return "PLAT_CODE_WI2K";
	case PLAT_CODE_W2RU:
		return "PLAT_CODE_W2RU";
	case PLAT_CODE_W2KU:
		return "PLAT_CODE_W2KU";
	case PLAT_CODE_MAC:
		return "PLAT_CODE_MAC";
	case PLAT_CODE_MACX:
		return "PLAT_CODE_MACX";
	default:
		return "UNKOWN";
	}
}

static void
vhd_print_parent(vhd_context_t *vhd, vhd_parent_locator_t *loc)
{
	int err;
	char *buf;

	err = vhd_parent_locator_read(vhd, loc, &buf);
	if (err) {
		printf("failed to read parent name\n");
		return;
	}

	printf("       decoded name : %s\n", buf);
}

static void
vhd_print_parent_locators(vhd_context_t *vhd, int hex)
{
	int i, n;
	vhd_parent_locator_t *loc;

	printf("VHD Parent Locators:\n--------------------\n");

	n = sizeof(vhd->header.loc) / sizeof(struct prt_loc);
	for (i = 0; i < n; i++) {
		loc = &vhd->header.loc[i];

		if (loc->code == PLAT_CODE_NONE)
			continue;

		printf("locator:            : %d\n", i);
		printf("       code         : %s\n",
		       code_name(loc->code));
		printf("       data_space   : %s\n",
		       conv(hex, loc->data_space));
		printf("       data_length  : %s\n",
		       conv(hex, loc->data_len));
		printf("       data_offset  : %s\n",
		       conv(hex, loc->data_offset));
		vhd_print_parent(vhd, loc);
		printf("\n");
	}
}

static void
vhd_print_batmap_header(vhd_batmap_t *batmap, int hex)
{
	uint32_t cksm;

	printf("VHD Batmap Summary:\n-------------------\n");
	printf("Batmap offset       : %s\n",
	       conv(hex, batmap->header.batmap_offset));
	printf("Batmap size (secs)  : %s\n",
	       conv(hex, batmap->header.batmap_size));
	printf("Batmap version      : 0x%08x\n",
	       batmap->header.batmap_version);

	cksm = vhd_checksum_batmap(batmap);
	printf("Checksum            : 0x%x|0x%x (%s)\n",
	       batmap->header.checksum, cksm,
	       (batmap->header.checksum == cksm ? "Good!" : "Bad!"));
	printf("\n");
}

static inline int
check_block_range(vhd_context_t *vhd, uint64_t block, int hex)
{
	if (block > vhd->header.max_bat_size) {
		fprintf(stderr, "block %s past end of file\n",
			conv(hex, block));
		return -ERANGE;
	}

	return 0;
}

static int
vhd_print_headers(vhd_context_t *vhd, int hex)
{
	int err;

	vhd_print_footer(&vhd->footer, hex);

	if (vhd_type_dynamic(vhd)) {
		vhd_print_header(vhd, &vhd->header, hex);

		if (vhd->footer.type == HD_TYPE_DIFF)
			vhd_print_parent_locators(vhd, hex);

		if (vhd_has_batmap(vhd)) {
			err = vhd_get_batmap(vhd);
			if (err) {
				printf("failed to get batmap header\n");
				return err;
			}

			vhd_print_batmap_header(&vhd->batmap, hex);
		}
	}

	return 0;
}

static int
vhd_dump_headers(const char *name, int hex)
{
	vhd_context_t vhd;

	libvhd_set_log_level(1);
	memset(&vhd, 0, sizeof(vhd));

	printf("\n%s appears invalid; dumping headers\n\n", name);

	vhd.fd = open(name, O_DIRECT | O_LARGEFILE | O_RDONLY);
	if (vhd.fd == -1)
		return -errno;

	vhd.file = strdup(name);

	vhd_read_footer(&vhd, &vhd.footer);
	vhd_read_header(&vhd, &vhd.header);

	vhd_print_footer(&vhd.footer, hex);
	vhd_print_header(&vhd, &vhd.header, hex);

	close(vhd.fd);
	free(vhd.file);

	return 0;
}

static int
vhd_print_logical_to_physical(vhd_context_t *vhd,
			      uint64_t sector, int count, int hex)
{
	int i;
	uint32_t blk, lsec;
	uint64_t cur, offset;

	if (vhd_sectors_to_bytes(sector + count) > vhd->footer.curr_size) {
		fprintf(stderr, "sector %s past end of file\n",
			conv(hex, sector + count));
			return -ERANGE;
	}

	for (i = 0; i < count; i++) {
		cur    = sector + i;
		blk    = cur / vhd->spb;
		lsec   = cur % vhd->spb;
		offset = vhd->bat.bat[blk];

		if (offset != DD_BLK_UNUSED) {
			offset += lsec + 1;
			offset  = vhd_sectors_to_bytes(offset);
		}

		printf("logical sector %s: ", conv(hex, cur));
		printf("block number: %s, ", conv(hex, blk));
		printf("sector offset: %s, ", conv(hex, lsec));
		printf("file offset: %s\n", (offset == DD_BLK_UNUSED ?
			"not allocated" : conv(hex, offset)));
	}

	return 0;
}

static int
vhd_print_bat(vhd_context_t *vhd, uint64_t block, int count, int hex)
{
	int i;
	uint64_t cur, offset;

	if (check_block_range(vhd, block + count, hex))
		return -ERANGE;

	for (i = 0; i < count; i++) {
		cur    = block + i;
		offset = vhd->bat.bat[cur];

		printf("block: %s: ", conv(hex, cur));
		printf("offset: %s\n",
		       (offset == DD_BLK_UNUSED ? "not allocated" :
			conv(hex, vhd_sectors_to_bytes(offset))));
	}

	return 0;
}

static inline void
write_full(int fd, void* buf, size_t count)
{
	ssize_t num_written = 0;
	if (!buf) return;
	
	
	while(count > 0) {
		
		num_written = write(fd, buf, count);
		if (num_written == -1) {
			if (errno == EINTR) 
				continue;
			else
				return;
		}
		
		count -= num_written;
		buf   += num_written;
	}
}

static int
vhd_print_bitmap(vhd_context_t *vhd, uint64_t block, int count, int hex)
{
	char *buf;
	int i, err;
	uint64_t cur;

	if (check_block_range(vhd, block + count, hex))
		return -ERANGE;

	for (i = 0; i < count; i++) {
		cur = block + i;

		if (vhd->bat.bat[cur] == DD_BLK_UNUSED) {
			printf("block %s not allocated\n", conv(hex, cur));
			continue;
		}

		err = vhd_read_bitmap(vhd, cur, &buf);
		if (err)
			goto out;

		write_full(STDOUT_FILENO, buf, 
			   vhd_sectors_to_bytes(vhd->bm_secs));
		free(buf);
	}

	err = 0;
out:
	return err;
}

static int
vhd_test_bitmap(vhd_context_t *vhd, uint64_t sector, int count, int hex)
{
	char *buf;
	uint64_t cur;
	int i, err, bit;
	uint32_t blk, bm_blk, sec;

	if (vhd_sectors_to_bytes(sector + count) > vhd->footer.curr_size) {
		printf("sector %s past end of file\n", conv(hex, sector));
		return -ERANGE;
	}

	bm_blk = -1;
	buf    = NULL;

	for (i = 0; i < count; i++) {
		cur = sector + i;
		blk = cur / vhd->spb;
		sec = cur % vhd->spb;

		if (blk != bm_blk) {
			bm_blk = blk;
			free(buf);
			buf = NULL;

			if (vhd->bat.bat[blk] != DD_BLK_UNUSED) {
				err = vhd_read_bitmap(vhd, blk, &buf);
				if (err)
					goto out;
			}
		}

		if (vhd->bat.bat[blk] == DD_BLK_UNUSED)
			bit = 0;
		else
			bit = vhd_bitmap_test(vhd, buf, blk);

	print:
		printf("block %s: ", conv(hex, blk));
		printf("sec: %s: %d\n", conv(hex, sec), bit);
	}

	err = 0;
 out:
	free(buf);
	return err;
}

static int
vhd_print_batmap(vhd_context_t *vhd)
{
	int err;
	size_t size;

	err = vhd_get_batmap(vhd);
	if (err) {
		printf("failed to read batmap: %d\n", err);
		return err;
	}

	size = vhd_sectors_to_bytes(vhd->batmap.header.batmap_size);
	write_full(STDOUT_FILENO, vhd->batmap.map, size);

	return 0;
}

static int
vhd_test_batmap(vhd_context_t *vhd, uint64_t block, int count, int hex)
{
	int i, err;
	uint64_t cur;

	if (check_block_range(vhd, block + count, hex))
		return -ERANGE;

	err = vhd_get_batmap(vhd);
	if (err) {
		fprintf(stderr, "failed to get batmap\n");
		return err;
	}

	for (i = 0; i < count; i++) {
		cur = block + i;
		fprintf(stderr, "batmap for block %s: %d\n", conv(hex, cur),
			vhd_batmap_test(vhd, &vhd->batmap, cur));
	}

	return 0;
}

static int
vhd_print_data(vhd_context_t *vhd, uint64_t block, int count, int hex)
{
	char *buf;
	int i, err;
	uint64_t cur;

	err = 0;

	if (check_block_range(vhd, block + count, hex))
		return -ERANGE;

	for (i = 0; i < count; i++) {
		cur = block + i;

		if (vhd->bat.bat[cur] == DD_BLK_UNUSED) {
			printf("block %s not allocated\n", conv(hex, cur));
			continue;
		}

		err = vhd_read_block(vhd, cur, &buf);
		if (err)
			break;

		write_full(STDOUT_FILENO, buf, vhd->header.block_size);
		free(buf);
	}

	return err;
}

static int
vhd_read_data(vhd_context_t *vhd, uint64_t sec, int count, int hex)
{
	char *buf;
	uint64_t cur;
	int err, max, secs;

	if (vhd_sectors_to_bytes(sec + count) > vhd->footer.curr_size)
		return -ERANGE;

	max = MIN(vhd_sectors_to_bytes(count), VHD_BLOCK_SIZE);
	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, max);
	if (err)
		return -err;

	cur = sec;
	while (count) {
		secs = MIN((max >> VHD_SECTOR_SHIFT), count);
		err  = vhd_io_read(vhd, buf, cur, secs);
		if (err)
			break;

		write_full(STDOUT_FILENO, buf, vhd_sectors_to_bytes(secs));

		cur   += secs;
		count -= secs;
	}

	free(buf);
	return err;
}

int
vhd_util_read(int argc, char **argv)
{
	char *name;
	vhd_context_t vhd;
	int c, err, headers, hex;
	uint64_t bat, bitmap, tbitmap, batmap, tbatmap, data, lsec, count, read;

	err     = 0;
	hex     = 0;
	headers = 0;
	count   = 1;
	bat     = -1;
	bitmap  = -1;
	tbitmap = -1;
	batmap  = -1;
	tbatmap = -1;
	data    = -1;
	lsec    = -1;
	read    = -1;
	name    = NULL;

	if (!argc || !argv)
		goto usage;

	optind = 0;
	while ((c = getopt(argc, argv, "n:pt:b:m:i:aj:d:c:r:xh")) != -1) {
		switch(c) {
		case 'n':
			name = optarg;
			break;
		case 'p':
			headers = 1;
			break;
		case 't':
			lsec = strtoul(optarg, NULL, 10);
			break;
		case 'b':
			bat = strtoull(optarg, NULL, 10);
			break;
		case 'm':
			bitmap = strtoull(optarg, NULL, 10);
			break;
		case 'i':
			tbitmap = strtoul(optarg, NULL, 10);
			break;
		case 'a':
			batmap = 1;
			break;
		case 'j':
			tbatmap = strtoull(optarg, NULL, 10);
			break;
		case 'd':
			data = strtoull(optarg, NULL, 10);
			break;
		case 'r':
			read = strtoull(optarg, NULL, 10);
			break;
		case 'c':
			count = strtoul(optarg, NULL, 10);
			break;
		case 'x':
			hex = 1;
			break;
		case 'h':
		default:
			goto usage;
		}
	}

	if (!name || optind != argc)
		goto usage;

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED);
	if (err) {
		printf("Failed to open %s: %d\n", name, err);
		vhd_dump_headers(name, hex);
		return err;
	}

	err = vhd_get_bat(&vhd);
	if (err) {
		printf("Failed to get bat for %s: %d\n", name, err);
		goto out;
	}

	if (headers)
		vhd_print_headers(&vhd, hex);

	if (lsec != -1) {
		err = vhd_print_logical_to_physical(&vhd, lsec, count, hex);
		if (err)
			goto out;
	}

	if (bat != -1) {
		err = vhd_print_bat(&vhd, bat, count, hex);
		if (err)
			goto out;
	}

	if (bitmap != -1) {
		err = vhd_print_bitmap(&vhd, bitmap, count, hex);
		if (err)
			goto out;
	}

	if (tbitmap != -1) {
		err = vhd_test_bitmap(&vhd, tbitmap, count, hex);
		if (err)
			goto out;
	}

	if (batmap != -1) {
		err = vhd_print_batmap(&vhd);
		if (err)
			goto out;
	}

	if (tbatmap != -1) {
		err = vhd_test_batmap(&vhd, tbatmap, count, hex);
		if (err)
			goto out;
	}

	if (data != -1) {
		err = vhd_print_data(&vhd, data, count, hex);
		if (err)
			goto out;
	}

	if (read != -1) {
		err = vhd_read_data(&vhd, read, count, hex);
		if (err)
			goto out;
	}

	err = 0;

 out:
	vhd_close(&vhd);
	return err;

 usage:
	printf("options:\n"
	       "-h          help\n"
	       "-n          name\n"
	       "-p          print VHD headers\n"
	       "-t sec      translate logical sector to VHD location\n"
	       "-b blk      print bat entry\n"
	       "-m blk      print bitmap\n"
	       "-i sec      test bitmap for logical sector\n"
	       "-a          print batmap\n"
	       "-j blk      test batmap for block\n"
	       "-d blk      print data\n"
	       "-c num      num units\n"
	       "-r sec      read num sectors at sec\n"
	       "-x          print in hex\n");
	return EINVAL;
}
