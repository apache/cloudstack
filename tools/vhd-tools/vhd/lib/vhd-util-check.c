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
#include <time.h>
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <libgen.h>
#include <inttypes.h>
#include <sys/stat.h>

#include "libvhd.h"
#include "vhd-util.h"

// allow the VHD timestamp to be at most this many seconds into the future to 
// account for time skew with NFS servers
#define TIMESTAMP_MAX_SLACK 1800

static int
vhd_util_check_zeros(void *buf, size_t size)
{
	int i;
	char *p;

	p = buf;
	for (i = 0; i < size; i++)
		if (p[i])
			return i;

	return 0;
}

static int
vhd_util_check_footer_opened(vhd_footer_t *footer)
{
	int i, n;
	uint32_t *buf;

	buf = (uint32_t *)footer;
	n = sizeof(*footer) / sizeof(uint32_t);

	for (i = 0; i < n; i++)
		if (buf[i] != 0xc7c7c7c7)
			return 0;

	return 1;
}

static char *
vhd_util_check_validate_footer(vhd_footer_t *footer)
{
	int size;
	uint32_t checksum, now;

	size = sizeof(footer->cookie);
	if (memcmp(footer->cookie, HD_COOKIE, size))
		return "invalid cookie";

	checksum = vhd_checksum_footer(footer);
	if (checksum != footer->checksum) {
		if (footer->hidden &&
		    !strncmp(footer->crtr_app, "tap", 3) &&
		    (footer->crtr_ver == VHD_VERSION(0, 1) ||
		     footer->crtr_ver == VHD_VERSION(1, 1))) {
			char tmp = footer->hidden;
			footer->hidden = 0;
			checksum = vhd_checksum_footer(footer);
			footer->hidden = tmp;

			if (checksum == footer->checksum)
				goto ok;
		}

		return "invalid checksum";
	}

ok:
	if (!(footer->features & HD_RESERVED))
		return "invalid 'reserved' feature";

	if (footer->features & ~(HD_TEMPORARY | HD_RESERVED))
		return "invalid extra features";

	if (footer->ff_version != HD_FF_VERSION)
		return "invalid file format version";

	if (footer->type != HD_TYPE_DYNAMIC &&
	    footer->type != HD_TYPE_DIFF    &&
	    footer->data_offset != ~(0ULL))
		return "invalid data offset";

	now = vhd_time(time(NULL));
	if (footer->timestamp > now + TIMESTAMP_MAX_SLACK)
		return "creation time in future";

	if (!strncmp(footer->crtr_app, "tap", 3) &&
	    footer->crtr_ver > VHD_CURRENT_VERSION)
		return "unsupported tap creator version";

	if (vhd_chs(footer->curr_size) < footer->geometry)
		return "geometry too large";

	if (footer->type != HD_TYPE_FIXED   &&
	    footer->type != HD_TYPE_DYNAMIC &&
	    footer->type != HD_TYPE_DIFF)
		return "invalid type";

	if (footer->saved && footer->saved != 1)
		return "invalid 'saved' state";

	if (footer->hidden && footer->hidden != 1)
		return "invalid 'hidden' state";

	if (vhd_util_check_zeros(footer->reserved,
				 sizeof(footer->reserved)))
		return "invalid 'reserved' bits";

	return NULL;
}

static char *
vhd_util_check_validate_header(int fd, vhd_header_t *header)
{
	off_t eof;
	int i, cnt, size;
	uint32_t checksum;

	size = sizeof(header->cookie);
	if (memcmp(header->cookie, DD_COOKIE, size))
		return "invalid cookie";

	checksum = vhd_checksum_header(header);
	if (checksum != header->checksum)
		return "invalid checksum";

	if (header->hdr_ver != 0x00010000)
		return "invalid header version";

	if (header->data_offset != ~(0ULL))
		return "invalid data offset";

	eof = lseek(fd, 0, SEEK_END);
	if (eof == (off_t)-1)
		return "error finding eof";

	if (header->table_offset <= 0  ||
	    header->table_offset % 512 ||
	    (header->table_offset +
	     (header->max_bat_size * sizeof(uint32_t)) >
	     eof - sizeof(vhd_footer_t)))
		return "invalid table offset";

	for (cnt = 0, i = 0; i < sizeof(header->block_size) * 8; i++)
		if ((header->block_size >> i) & 1)
			cnt++;

	if (cnt != 1)
		return "invalid block size";

	if (header->res1)
		return "invalid reserved bits";

	if (vhd_util_check_zeros(header->res2, sizeof(header->res2)))
		return "invalid reserved bits";

	return NULL;
}

static char *
vhd_util_check_validate_differencing_header(vhd_context_t *vhd)
{
	vhd_header_t *header;

	header = &vhd->header;

	if (vhd->footer.type == HD_TYPE_DIFF) {
		char *parent;
		uint32_t now;

		now = vhd_time(time(NULL));
		if (header->prt_ts > now + TIMESTAMP_MAX_SLACK)
			return "parent creation time in future";

		if (vhd_header_decode_parent(vhd, header, &parent))
			return "invalid parent name";

		free(parent);
	} else {
		if (vhd_util_check_zeros(header->prt_name,
					 sizeof(header->prt_name)))
			return "invalid non-null parent name";

		if (vhd_util_check_zeros(header->loc, sizeof(header->loc)))
			return "invalid non-null parent locators";

		if (!blk_uuid_is_nil(&header->prt_uuid))
			return "invalid non-null parent uuid";

		if (header->prt_ts)
			return "invalid non-zero parent timestamp";
	}

	return NULL;
}

static char *
vhd_util_check_validate_batmap(vhd_context_t *vhd, vhd_batmap_t *batmap)
{
	int size;
	off_t eof;
	uint32_t checksum;

	size = sizeof(batmap->header.cookie);
	if (memcmp(batmap->header.cookie, VHD_BATMAP_COOKIE, size))
		return "invalid cookie";

	if (batmap->header.batmap_version > VHD_BATMAP_CURRENT_VERSION)
		return "unsupported batmap version";

	checksum = vhd_checksum_batmap(batmap);
	if (checksum != batmap->header.checksum)
		return "invalid checksum";

	if (!batmap->header.batmap_size)
		return "invalid size zero";

	eof = lseek(vhd->fd, 0, SEEK_END);
	if (eof == (off_t)-1)
		return "error finding eof";

	if (!batmap->header.batmap_offset ||
	    batmap->header.batmap_offset % 512)
		return "invalid batmap offset";

	if ((batmap->header.batmap_offset +
	     vhd_sectors_to_bytes(batmap->header.batmap_size)) >
	    eof - sizeof(vhd_footer_t))
		return "invalid batmap size";

	return NULL;
}

static char *
vhd_util_check_validate_parent_locator(vhd_context_t *vhd,
				       vhd_parent_locator_t *loc)
{
	off_t eof;

	if (vhd_validate_platform_code(loc->code))
		return "invalid platform code";

	if (loc->code == PLAT_CODE_NONE) {
		if (vhd_util_check_zeros(loc, sizeof(*loc)))
			return "non-zero locator";

		return NULL;
	}

	if (!loc->data_offset)
		return "invalid data offset";

	if (!loc->data_space)
		return "invalid data space";

	if (!loc->data_len)
		return "invalid data length";

	eof = lseek(vhd->fd, 0, SEEK_END);
	if (eof == (off_t)-1)
		return "error finding eof";

	if (loc->data_offset + vhd_parent_locator_size(loc) >
	    eof - sizeof(vhd_footer_t))
		return "invalid size";

	if (loc->res)
		return "invalid reserved bits";

	return NULL;
}

static const char *
vhd_util_check_validate_parent(vhd_context_t *vhd, const char *ppath)
{
	const char *msg;
	vhd_context_t parent;
	uint32_t status;

	msg = NULL;

	if (vhd_parent_raw(vhd))
		return msg;

	if (vhd_open(&parent, ppath,
				VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED))
		return "error opening parent";

	if (blk_uuid_compare(&vhd->header.prt_uuid, &parent.footer.uuid)) {
		msg = "invalid parent uuid";
		goto out;
	}

out:
	vhd_close(&parent);
	return msg;
}

static int
vhd_util_check_footer(int fd, vhd_footer_t *footer, int ignore)
{
	size_t size;
	int err, opened;
	char *msg, *buf;
	off_t eof, off;
	vhd_footer_t primary, backup;

	memset(&primary, 0, sizeof(primary));
	memset(&backup, 0, sizeof(backup));

	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, sizeof(primary));
	if (err) {
		printf("error allocating buffer: %d\n", err);
		return -err;
	}

	memset(buf, 0, sizeof(primary));

	eof = lseek(fd, 0, SEEK_END);
	if (eof == (off_t)-1) {
		err = -errno;
		printf("error calculating end of file: %d\n", err);
		goto out;
	}

	size = ((eof % 512) ? 511 : 512);
	eof  = lseek(fd, eof - size, SEEK_SET);
	if (eof == (off_t)-1) {
		err = -errno;
		printf("error calculating end of file: %d\n", err);
		goto out;
	}

	err = read(fd, buf, 512);
	if (err != size) {
		err = (errno ? -errno : -EIO);
		printf("error reading primary footer: %d\n", err);
		goto out;
	}

	memcpy(&primary, buf, sizeof(primary));
	opened = vhd_util_check_footer_opened(&primary);
	vhd_footer_in(&primary);

	msg = vhd_util_check_validate_footer(&primary);
	if (msg) {
		if (opened && ignore)
			goto check_backup;

		err = -EINVAL;
		printf("primary footer invalid: %s\n", msg);
		goto out;
	}

	if (primary.type == HD_TYPE_FIXED) {
		err = 0;
		goto out;
	}

check_backup:
	off = lseek(fd, 0, SEEK_SET);
	if (off == (off_t)-1) {
		err = -errno;
		printf("error seeking to backup footer: %d\n", err);
		goto out;
	}

	size = 512;
	memset(buf, 0, sizeof(primary));

	err = read(fd, buf, size);
	if (err != size) {
		err = (errno ? -errno : -EIO);
		printf("error reading backup footer: %d\n", err);
		goto out;
	}

	memcpy(&backup, buf, sizeof(backup));
	vhd_footer_in(&backup);

	msg = vhd_util_check_validate_footer(&backup);
	if (msg) {
		err = -EINVAL;
		printf("backup footer invalid: %s\n", msg);
		goto out;
	}

	if (memcmp(&primary, &backup, sizeof(primary))) {
		if (opened && ignore) {
			memcpy(&primary, &backup, sizeof(primary));
			goto ok;
		}

		if (backup.hidden &&
		    !strncmp(backup.crtr_app, "tap", 3) &&
		    (backup.crtr_ver == VHD_VERSION(0, 1) ||
		     backup.crtr_ver == VHD_VERSION(1, 1))) {
			char cmp, tmp = backup.hidden;
			backup.hidden = 0;
			cmp = memcmp(&primary, &backup, sizeof(primary));
			backup.hidden = tmp;
			if (!cmp)
				goto ok;
		}

		err = -EINVAL;
		printf("primary and backup footers do not match\n");
		goto out;
	}

ok:
	err = 0;
	memcpy(footer, &primary, sizeof(primary));

out:
	free(buf);
	return err;
}

static int
vhd_util_check_header(int fd, vhd_footer_t *footer)
{
	int err;
	off_t off;
	char *msg, *buf;
	vhd_header_t header;

	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, sizeof(header));
	if (err) {
		printf("error allocating header: %d\n", err);
		return err;
	}

	off = footer->data_offset;
	off = lseek(fd, off, SEEK_SET);
	if (off == (off_t)-1) {
		err = -errno;
		printf("error seeking to header: %d\n", err);
		goto out;
	}

	err = read(fd, buf, sizeof(header));
	if (err != sizeof(header)) {
		err = (errno ? -errno : -EIO);
		printf("error reading header: %d\n", err);
		goto out;
	}

	memcpy(&header, buf, sizeof(header));
	vhd_header_in(&header);

	msg = vhd_util_check_validate_header(fd, &header);
	if (msg) {
		err = -EINVAL;
		printf("header is invalid: %s\n", msg);
		goto out;
	}

	err = 0;

out:
	free(buf);
	return err;
}

static int
vhd_util_check_differencing_header(vhd_context_t *vhd)
{
	char *msg;

	msg = vhd_util_check_validate_differencing_header(vhd);
	if (msg) {
		printf("differencing header is invalid: %s\n", msg);
		return -EINVAL;
	}

	return 0;
}

static int
vhd_util_check_bat(vhd_context_t *vhd)
{
	off_t eof, eoh;
	int i, j, err, block_size;

	err = vhd_seek(vhd, 0, SEEK_END);
	if (err) {
		printf("error calculating eof: %d\n", err);
		return err;
	}

	eof = vhd_position(vhd);
	if (eof == (off_t)-1) {
		printf("error calculating eof: %d\n", -errno);
		return -errno;
	}

	/* adjust eof for vhds with short footers */
	if (eof % 512) {
		if (eof % 512 != 511) {
			printf("invalid file size: 0x%"PRIx64"\n", eof);
			return -EINVAL;
		}

		eof++;
	}

	err = vhd_get_bat(vhd);
	if (err) {
		printf("error reading bat: %d\n", err);
		return err;
	}

	err = vhd_end_of_headers(vhd, &eoh);
	if (err) {
		printf("error calculating end of metadata: %d\n", err);
		return err;
	}

	eof  -= sizeof(vhd_footer_t);
	eof >>= VHD_SECTOR_SHIFT;
	eoh >>= VHD_SECTOR_SHIFT;
	block_size = vhd->spb + vhd->bm_secs;

	for (i = 0; i < vhd->header.max_bat_size; i++) {
		uint32_t off = vhd->bat.bat[i];
		if (off == DD_BLK_UNUSED)
			continue;

		if (off < eoh) {
			printf("block %d (offset 0x%x) clobbers headers\n",
			       i, off);
			return -EINVAL;
		}

		if (off + block_size > eof) {
			printf("block %d (offset 0x%x) clobbers footer\n",
			       i, off);
			return -EINVAL;
		}

		for (j = 0; j < vhd->header.max_bat_size; j++) {
			uint32_t joff = vhd->bat.bat[j];

			if (i == j)
				continue;

			if (joff == DD_BLK_UNUSED)
				continue;

			if (off == joff)
				err = -EINVAL;

			if (off > joff && off < joff + block_size)
				err = -EINVAL;

			if (off + block_size > joff &&
			    off + block_size < joff + block_size)
				err = -EINVAL;

			if (err) {
				printf("block %d (offset 0x%x) clobbers "
				       "block %d (offset 0x%x)\n",
				       i, off, j, joff);
				return err;
			}
		}
	}

	return 0;
}

static int
vhd_util_check_batmap(vhd_context_t *vhd)
{
	char *msg;
	int i, err;

	err = vhd_get_bat(vhd);
	if (err) {
		printf("error reading bat: %d\n", err);
		return err;
	}

	err = vhd_get_batmap(vhd);
	if (err) {
		printf("error reading batmap: %d\n", err);
		return err;
	}

	msg = vhd_util_check_validate_batmap(vhd, &vhd->batmap);
	if (msg) {
		printf("batmap is invalid: %s\n", msg);
		return -EINVAL;
	}

	for (i = 0; i < vhd->header.max_bat_size; i++) {
		if (!vhd_batmap_test(vhd, &vhd->batmap, i))
			continue;

		if (vhd->bat.bat[i] == DD_BLK_UNUSED) {
			printf("batmap shows unallocated block %d full\n", i);
			return -EINVAL;
		}
	}

	return 0;
}

static int
vhd_util_check_parent_locators(vhd_context_t *vhd)
{
	int i, n, err;
	vhd_parent_locator_t *loc;
	char *file, *ppath, *location, *pname;
	const char *msg;
	int mac, macx, w2ku, w2ru, wi2r, wi2k, found;

	mac      = 0;
	macx     = 0;
	w2ku     = 0;
	w2ru     = 0;
	wi2r     = 0;
	wi2k     = 0;
	found    = 0;
	pname    = NULL;
	ppath    = NULL;
	location = NULL;

	err = vhd_header_decode_parent(vhd, &vhd->header, &pname);
	if (err) {
		printf("error decoding parent name: %d\n", err);
		return err;
	}

	n = sizeof(vhd->header.loc) / sizeof(vhd->header.loc[0]);
	for (i = 0; i < n; i++) {
		ppath    = NULL;
		location = NULL;
		loc = vhd->header.loc + i;

		msg = vhd_util_check_validate_parent_locator(vhd, loc);
		if (msg) {
			err = -EINVAL;
			printf("invalid parent locator %d: %s\n", i, msg);
			goto out;
		}

		if (loc->code == PLAT_CODE_NONE)
			continue;

		switch (loc->code) {
		case PLAT_CODE_MACX:
			if (macx++)
				goto dup;
			break;

		case PLAT_CODE_MAC:
			if (mac++)
				goto dup;
			break;

		case PLAT_CODE_W2KU:
			if (w2ku++)
				goto dup;
			break;

		case PLAT_CODE_W2RU:
			if (w2ru++)
				goto dup;
			break;

		case PLAT_CODE_WI2R:
			if (wi2r++)
				goto dup;
			break;

		case PLAT_CODE_WI2K:
			if (wi2k++)
				goto dup;
			break;

		default:
			err = -EINVAL;
			printf("invalid  platform code for locator %d\n", i);
			goto out;
		}

		if (loc->code != PLAT_CODE_MACX &&
		    loc->code != PLAT_CODE_W2RU &&
		    loc->code != PLAT_CODE_W2KU)
			continue;

		err = vhd_parent_locator_read(vhd, loc, &ppath);
		if (err) {
			printf("error reading parent locator %d: %d\n", i, err);
			goto out;
		}

		file = basename(ppath);
		if (strcmp(pname, file)) {
			err = -EINVAL;
			printf("parent locator %d name (%s) does not match "
			       "header name (%s)\n", i, file, pname);
			goto out;
		}

		err = vhd_find_parent(vhd, ppath, &location);
		if (err) {
			printf("error resolving %s: %d\n", ppath, err);
			goto out;
		}

		err = access(location, R_OK);
		if (err && loc->code == PLAT_CODE_MACX) {
			err = -errno;
			printf("parent locator %d points to missing file %s "
				"(resolved to %s)\n", i, ppath, location);
			goto out;
		}

		msg = vhd_util_check_validate_parent(vhd, location);
		if (msg) {
			err = -EINVAL;
			printf("invalid parent %s: %s\n", location, msg);
			goto out;
		}

		found++;
		free(ppath);
		free(location);
		ppath = NULL;
		location = NULL;

		continue;

	dup:
		printf("duplicate platform code in locator %d: 0x%x\n",
		       i, loc->code);
		err = -EINVAL;
		goto out;
	}

	if (!found) {
		err = -EINVAL;
		printf("could not find parent %s\n", pname);
		goto out;
	}

	err = 0;

out:
	free(pname);
	free(ppath);
	free(location);
	return err;
}

static void
vhd_util_dump_headers(const char *name)
{
	char *argv[] = { "read", "-p", "-n", (char *)name };
	int argc = sizeof(argv) / sizeof(argv[0]);

	printf("%s appears invalid; dumping metadata\n", name);
	vhd_util_read(argc, argv);
}

static int
vhd_util_check_vhd(const char *name, int ignore)
{
	int fd, err;
	vhd_context_t vhd;
	struct stat stats;
	vhd_footer_t footer;

	fd = -1;
	memset(&vhd, 0, sizeof(vhd));
        memset(&footer, 0, sizeof(footer));

	err = stat(name, &stats);
	if (err == -1) {
		printf("cannot stat %s: %d\n", name, errno);
		return -errno;
	}

	if (!S_ISREG(stats.st_mode) && !S_ISBLK(stats.st_mode)) {
		printf("%s is not a regular file or block device\n", name);
		return -EINVAL;
	}

	fd = open(name, O_RDONLY | O_DIRECT | O_LARGEFILE);
	if (fd == -1) {
		printf("error opening %s\n", name);
		return -errno;
	}

	err = vhd_util_check_footer(fd, &footer, ignore);
	if (err)
		goto out;

	if (footer.type != HD_TYPE_DYNAMIC && footer.type != HD_TYPE_DIFF)
		goto out;

	err = vhd_util_check_header(fd, &footer);
	if (err)
		goto out;

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED);
	if (err)
		goto out;

	err = vhd_util_check_differencing_header(&vhd);
	if (err)
		goto out;

	err = vhd_util_check_bat(&vhd);
	if (err)
		goto out;

	if (vhd_has_batmap(&vhd)) {
		err = vhd_util_check_batmap(&vhd);
		if (err)
			goto out;
	}

	if (vhd.footer.type == HD_TYPE_DIFF) {
		err = vhd_util_check_parent_locators(&vhd);
		if (err)
			goto out;
	}

	err = 0;
	printf("%s is valid\n", name);

out:
	if (err)
		vhd_util_dump_headers(name);
	if (fd != -1)
		close(fd);
	vhd_close(&vhd);
	return err;
}

static int
vhd_util_check_parents(const char *name, int ignore)
{
	int err;
	vhd_context_t vhd;
	char *cur, *parent;

	cur = (char *)name;

	for (;;) {
		err = vhd_open(&vhd, cur, 
				VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED);
		if (err)
			goto out;

		if (vhd.footer.type != HD_TYPE_DIFF || vhd_parent_raw(&vhd)) {
			vhd_close(&vhd);
			goto out;
		}

		err = vhd_parent_locator_get(&vhd, &parent);
		vhd_close(&vhd);

		if (err) {
			printf("error getting parent: %d\n", err);
			goto out;
		}

		if (cur != name)
			free(cur);
		cur = parent;

		err = vhd_util_check_vhd(cur, ignore);
		if (err)
			goto out;
	}

out:
	if (err)
		printf("error checking parents: %d\n", err);
	if (cur != name)
		free(cur);
	return err;
}

int
vhd_util_check(int argc, char **argv)
{
	char *name;
	vhd_context_t vhd;
	int c, err, ignore, parents;

	if (!argc || !argv) {
		err = -EINVAL;
		goto usage;
	}

	ignore  = 0;
	parents = 0;
	name    = NULL;

	optind = 0;
	while ((c = getopt(argc, argv, "n:iph")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 'i':
			ignore = 1;
			break;
		case 'p':
			parents = 1;
			break;
		case 'h':
			err = 0;
			goto usage;
		default:
			err = -EINVAL;
			goto usage;
		}
	}

	if (!name || optind != argc) {
		err = -EINVAL;
		goto usage;
	}

	err = vhd_util_check_vhd(name, ignore);
	if (err)
		goto out;

	if (parents)
		err = vhd_util_check_parents(name, ignore);

out:
	return err;

usage:
	printf("options: -n <file> [-i ignore missing primary footers] "
	       "[-p check parents] [-h help]\n");
	return err;
}
