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
#include <string.h>

#include "atomicio.h"
#include "libvhd-journal.h"

#define VHD_JOURNAL_ENTRY_TYPE_FOOTER_P  1
#define VHD_JOURNAL_ENTRY_TYPE_FOOTER_C  2
#define VHD_JOURNAL_ENTRY_TYPE_HEADER    3
#define VHD_JOURNAL_ENTRY_TYPE_LOCATOR   4
#define VHD_JOURNAL_ENTRY_TYPE_BAT       5
#define VHD_JOURNAL_ENTRY_TYPE_BATMAP_H  6
#define VHD_JOURNAL_ENTRY_TYPE_BATMAP_M  7
#define VHD_JOURNAL_ENTRY_TYPE_DATA      8

typedef struct vhd_journal_entry {
	uint64_t                         cookie;
	uint32_t                         type;
	uint32_t                         size;
	uint64_t                         offset;
	uint32_t                         checksum;
} vhd_journal_entry_t;

static inline int
vhd_journal_seek(vhd_journal_t *j, off_t offset, int whence)
{
	off_t off;

	off = lseek(j->jfd, offset, whence);
	if (off == (off_t)-1)
		return -errno;

	return 0;
}

static inline off_t
vhd_journal_position(vhd_journal_t *j)
{
	return lseek(j->jfd, 0, SEEK_CUR);
}

static inline int
vhd_journal_read(vhd_journal_t *j, void *buf, size_t size)
{
	ssize_t ret;

	errno = 0;

	ret = atomicio(read, j->jfd, buf, size);
	if (ret != size)
		return (errno ? -errno : -EIO);

	return 0;
}

static inline int
vhd_journal_write(vhd_journal_t *j, void *buf, size_t size)
{
	ssize_t ret;

	errno = 0;

	ret = atomicio(vwrite, j->jfd, buf, size);
	if (ret != size)
		return (errno ? -errno : -EIO);

	return 0;
}

static inline int
vhd_journal_truncate(vhd_journal_t *j, off_t length)
{
	int err;

	err = ftruncate(j->jfd, length);
	if (err == -1)
		return -errno;

	return 0;
}

static inline int
vhd_journal_sync(vhd_journal_t *j)
{
	int err;

	err = fdatasync(j->jfd);
	if (err)
		return -errno;

	return 0;
}

static inline void
vhd_journal_header_in(vhd_journal_header_t *header)
{
	BE64_IN(&header->vhd_footer_offset);
	BE32_IN(&header->journal_data_entries);
	BE32_IN(&header->journal_metadata_entries);
	BE64_IN(&header->journal_data_offset);
	BE64_IN(&header->journal_metadata_offset);
}

static inline void
vhd_journal_header_out(vhd_journal_header_t *header)
{
	BE64_OUT(&header->vhd_footer_offset);
	BE32_OUT(&header->journal_data_entries);
	BE32_OUT(&header->journal_metadata_entries);
	BE64_OUT(&header->journal_data_offset);
	BE64_OUT(&header->journal_metadata_offset);
}

static int
vhd_journal_validate_header(vhd_journal_t *j, vhd_journal_header_t *header)
{
	int err;
	off_t eof;

	if (memcmp(header->cookie,
		   VHD_JOURNAL_HEADER_COOKIE, sizeof(header->cookie)))
		return -EINVAL;

	err = vhd_journal_seek(j, j->header.journal_eof, SEEK_SET);
	if (err)
		return err;

	eof = vhd_journal_position(j);
	if (eof == (off_t)-1)
		return -errno;

	if (j->header.journal_data_offset > j->header.journal_eof)
		return -EINVAL;

	if (j->header.journal_metadata_offset > j->header.journal_eof)
		return -EINVAL;

	return 0;
}

static int
vhd_journal_read_journal_header(vhd_journal_t *j, vhd_journal_header_t *header)
{
	int err;
	size_t size;

	size = sizeof(vhd_journal_header_t);
	err  = vhd_journal_seek(j, 0, SEEK_SET);
	if (err)
		return err;

	err  = vhd_journal_read(j, header, size);
	if (err)
		return err;

	vhd_journal_header_in(header);

	return vhd_journal_validate_header(j, header);
}

static int
vhd_journal_write_header(vhd_journal_t *j, vhd_journal_header_t *header)
{
	int err;
	size_t size;
	vhd_journal_header_t h;

	memcpy(&h, header, sizeof(vhd_journal_header_t));

	err = vhd_journal_validate_header(j, &h);
	if (err)
		return err;

	vhd_journal_header_out(&h);
	size = sizeof(vhd_journal_header_t);

	err  = vhd_journal_seek(j, 0, SEEK_SET);
	if (err)
		return err;

	err = vhd_journal_write(j, &h, size);
	if (err)
		return err;

	return 0;
}

static int
vhd_journal_add_journal_header(vhd_journal_t *j)
{
	int err;
	off_t off;
	vhd_context_t *vhd;

	vhd = &j->vhd;
	memset(&j->header, 0, sizeof(vhd_journal_header_t));

	err = vhd_seek(vhd, 0, SEEK_END);
	if (err)
		return err;

	off = vhd_position(vhd);
	if (off == (off_t)-1)
		return -errno;

	err = vhd_get_footer(vhd);
	if (err)
		return err;

	blk_uuid_copy(&j->header.uuid, &vhd->footer.uuid);
	memcpy(j->header.cookie,
	       VHD_JOURNAL_HEADER_COOKIE, sizeof(j->header.cookie));
	j->header.vhd_footer_offset = off - sizeof(vhd_footer_t);
	j->header.journal_eof = sizeof(vhd_journal_header_t);

	return vhd_journal_write_header(j, &j->header);
}

static void
vhd_journal_entry_in(vhd_journal_entry_t *entry)
{
	BE32_IN(&entry->type);
	BE32_IN(&entry->size);
	BE64_IN(&entry->offset);
	BE64_IN(&entry->cookie);
	BE32_IN(&entry->checksum);
}

static void
vhd_journal_entry_out(vhd_journal_entry_t *entry)
{
	BE32_OUT(&entry->type);
	BE32_OUT(&entry->size);
	BE64_OUT(&entry->offset);
	BE64_OUT(&entry->cookie);
	BE32_OUT(&entry->checksum);
}

static uint32_t
vhd_journal_checksum_entry(vhd_journal_entry_t *entry, char *buf, size_t size)
{
	int i;
	unsigned char *blob;
	uint32_t checksum, tmp;

	checksum        = 0;
	tmp             = entry->checksum;
	entry->checksum = 0;

	blob = (unsigned char *)entry;
	for (i = 0; i < sizeof(vhd_journal_entry_t); i++)
		checksum += blob[i];

	blob = (unsigned char *)buf;
	for (i = 0; i < size; i++)
		checksum += blob[i];

	entry->checksum = tmp;
	return ~checksum;
}

static int
vhd_journal_validate_entry(vhd_journal_entry_t *entry)
{
	if (entry->size == 0)
		return -EINVAL;

	if (entry->size & (VHD_SECTOR_SIZE - 1))
		return -EINVAL;

	if (entry->cookie != VHD_JOURNAL_ENTRY_COOKIE)
		return -EINVAL;

	return 0;
}

static int
vhd_journal_read_entry(vhd_journal_t *j, vhd_journal_entry_t *entry)
{
	int err;

	err = vhd_journal_read(j, entry, sizeof(vhd_journal_entry_t));
	if (err)
		return err;

	vhd_journal_entry_in(entry);
	return vhd_journal_validate_entry(entry);
}

static int
vhd_journal_write_entry(vhd_journal_t *j, vhd_journal_entry_t *entry)
{
	int err;
	vhd_journal_entry_t e;

	err = vhd_journal_validate_entry(entry);
	if (err)
		return err;

	memcpy(&e, entry, sizeof(vhd_journal_entry_t));
	vhd_journal_entry_out(&e);

	err = vhd_journal_write(j, &e, sizeof(vhd_journal_entry_t));
	if (err)
		err;

	return 0;
}

static int
vhd_journal_validate_entry_data(vhd_journal_entry_t *entry, char *buf)
{
	int err;
	uint32_t checksum;

	err      = 0;
	checksum = vhd_journal_checksum_entry(entry, buf, entry->size);

	if (checksum != entry->checksum)
		return -EINVAL;

	return err;
}

static int
vhd_journal_update(vhd_journal_t *j, off_t offset,
		   char *buf, size_t size, uint32_t type)
{
	int err;
	off_t eof;
	uint64_t *off, off_bak;
	uint32_t *entries;
	vhd_journal_entry_t entry;

	entry.type     = type;
	entry.size     = size;
	entry.offset   = offset;
	entry.cookie   = VHD_JOURNAL_ENTRY_COOKIE;
	entry.checksum = vhd_journal_checksum_entry(&entry, buf, size);

	err = vhd_journal_seek(j, j->header.journal_eof, SEEK_SET);
	if (err)
		return err;

	err = vhd_journal_write_entry(j, &entry);
	if (err)
		goto fail;

	err = vhd_journal_write(j, buf, size);
	if (err)
		goto fail;

	if (type == VHD_JOURNAL_ENTRY_TYPE_DATA) {
		off     = &j->header.journal_data_offset;
		entries = &j->header.journal_data_entries;
	} else {
		off     = &j->header.journal_metadata_offset;
		entries = &j->header.journal_metadata_entries;
	}

	off_bak = *off;
	if (!(*entries)++)
		*off = j->header.journal_eof;
	j->header.journal_eof += (size + sizeof(vhd_journal_entry_t));

	err = vhd_journal_write_header(j, &j->header);
	if (err) {
		if (!--(*entries))
			*off = off_bak;
		j->header.journal_eof -= (size + sizeof(vhd_journal_entry_t));
		goto fail;
	}

	return 0;

fail:
	if (!j->is_block)
		vhd_journal_truncate(j, j->header.journal_eof);
	return err;
}

static int
vhd_journal_add_footer(vhd_journal_t *j)
{
	int err;
	off_t off;
	vhd_context_t *vhd;
	vhd_footer_t footer;

	vhd = &j->vhd;

	err = vhd_seek(vhd, 0, SEEK_END);
	if (err)
		return err;

	off = vhd_position(vhd);
	if (off == (off_t)-1)
		return -errno;

	err = vhd_read_footer_at(vhd, &footer, off - sizeof(vhd_footer_t));
	if (err)
		return err;

	vhd_footer_out(&footer);
	err = vhd_journal_update(j, off - sizeof(vhd_footer_t),
				 (char *)&footer,
				 sizeof(vhd_footer_t),
				 VHD_JOURNAL_ENTRY_TYPE_FOOTER_P);
	if (err)
		return err;

	if (!vhd_type_dynamic(vhd))
		return 0;

	err = vhd_read_footer_at(vhd, &footer, 0);
	if (err)
		return err;

	vhd_footer_out(&footer);
	err = vhd_journal_update(j, 0,
				 (char *)&footer,
				 sizeof(vhd_footer_t),
				 VHD_JOURNAL_ENTRY_TYPE_FOOTER_C);

	return err;
}

static int
vhd_journal_add_header(vhd_journal_t *j)
{
	int err;
	off_t off;
	vhd_context_t *vhd;
	vhd_header_t header;

	vhd = &j->vhd;

	err = vhd_read_header(vhd, &header);
	if (err)
		return err;

	off = vhd->footer.data_offset;

	vhd_header_out(&header);
	err = vhd_journal_update(j, off,
				 (char *)&header,
				 sizeof(vhd_header_t),
				 VHD_JOURNAL_ENTRY_TYPE_HEADER);

	return err;
}

static int
vhd_journal_add_locators(vhd_journal_t *j)
{
	int i, n, err;
	vhd_context_t *vhd;

	vhd = &j->vhd;

	err = vhd_get_header(vhd);
	if (err)
		return err;

	n = sizeof(vhd->header.loc) / sizeof(vhd_parent_locator_t);
	for (i = 0; i < n; i++) {
		char *buf;
		off_t off;
		size_t size;
		vhd_parent_locator_t *loc;

		loc  = vhd->header.loc + i;
		err  = vhd_validate_platform_code(loc->code);
		if (err)
			return err;

		if (loc->code == PLAT_CODE_NONE)
			continue;

		off  = loc->data_offset;
		size = vhd_parent_locator_size(loc);

		err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
		if (err)
			return -err;

		err  = vhd_seek(vhd, off, SEEK_SET);
		if (err)
			goto end;

		err  = vhd_read(vhd, buf, size);
		if (err)
			goto end;

		err  = vhd_journal_update(j, off, buf, size,
					  VHD_JOURNAL_ENTRY_TYPE_LOCATOR);
		if (err)
			goto end;

		err = 0;

	end:
		free(buf);
		if (err)
			break;
	}

	return err;
}

static int
vhd_journal_add_bat(vhd_journal_t *j)
{
	int err;
	off_t off;
	size_t size;
	vhd_bat_t bat;
	vhd_context_t *vhd;

	vhd  = &j->vhd;

	err  = vhd_get_header(vhd);
	if (err)
		return err;

	err  = vhd_read_bat(vhd, &bat);
	if (err)
		return err;

	off  = vhd->header.table_offset;
	size = vhd_bytes_padded(bat.entries * sizeof(uint32_t));

	vhd_bat_out(&bat);
	err  = vhd_journal_update(j, off, (char *)bat.bat, size,
				  VHD_JOURNAL_ENTRY_TYPE_BAT);

	free(bat.bat);
	return err;
}

static int
vhd_journal_add_batmap(vhd_journal_t *j)
{
	int err;
	off_t off;
	size_t size;
	vhd_context_t *vhd;
	vhd_batmap_t batmap;

	vhd  = &j->vhd;

	err  = vhd_batmap_header_offset(vhd, &off);
	if (err)
		return err;

	err  = vhd_read_batmap(vhd, &batmap);
	if (err)
		return err;

	size = vhd_bytes_padded(sizeof(struct dd_batmap_hdr));

	vhd_batmap_header_out(&batmap);
	err  = vhd_journal_update(j, off, (char *)&batmap.header, size,
				  VHD_JOURNAL_ENTRY_TYPE_BATMAP_H);
	if (err)
		goto out;

	vhd_batmap_header_in(&batmap);
	off  = batmap.header.batmap_offset;
	size = vhd_sectors_to_bytes(batmap.header.batmap_size);

	err  = vhd_journal_update(j, off, batmap.map, size,
				  VHD_JOURNAL_ENTRY_TYPE_BATMAP_M);

out:
	free(batmap.map);
	return err;
}

static int
vhd_journal_add_metadata(vhd_journal_t *j)
{
	int err;
	off_t eof;
	vhd_context_t *vhd;

	vhd = &j->vhd;

	err = vhd_journal_add_footer(j);
	if (err)
		return err;

	if (!vhd_type_dynamic(vhd))
		return 0;

	err = vhd_journal_add_header(j);
	if (err)
		return err;

	err = vhd_journal_add_locators(j);
	if (err)
		return err;

	err = vhd_journal_add_bat(j);
	if (err)
		return err;

	if (vhd_has_batmap(vhd)) {
		err = vhd_journal_add_batmap(j);
		if (err)
			return err;
	}

	j->header.journal_data_offset = j->header.journal_eof;
	return vhd_journal_write_header(j, &j->header);
}

static int
__vhd_journal_read_footer(vhd_journal_t *j,
			  vhd_footer_t *footer, uint32_t type)
{
	int err;
	vhd_journal_entry_t entry;

	err = vhd_journal_read_entry(j, &entry);
	if (err)
		return err;

	if (entry.type != type)
		return -EINVAL;

	if (entry.size != sizeof(vhd_footer_t))
		return -EINVAL;

	err = vhd_journal_read(j, footer, entry.size);
	if (err)
		return err;

	vhd_footer_in(footer);
	return vhd_validate_footer(footer);
}

static int
vhd_journal_read_footer(vhd_journal_t *j, vhd_footer_t *footer)
{
	return __vhd_journal_read_footer(j, footer,
					 VHD_JOURNAL_ENTRY_TYPE_FOOTER_P);
}

static int
vhd_journal_read_footer_copy(vhd_journal_t *j, vhd_footer_t *footer)
{
	return __vhd_journal_read_footer(j, footer,
					 VHD_JOURNAL_ENTRY_TYPE_FOOTER_C);
}

static int
vhd_journal_read_header(vhd_journal_t *j, vhd_header_t *header)
{
	int err;
	vhd_journal_entry_t entry;

	err = vhd_journal_read_entry(j, &entry);
	if (err)
		return err;

	if (entry.type != VHD_JOURNAL_ENTRY_TYPE_HEADER)
		return -EINVAL;

	if (entry.size != sizeof(vhd_header_t))
		return -EINVAL;

	err = vhd_journal_read(j, header, entry.size);
	if (err)
		return err;

	vhd_header_in(header);
	return vhd_validate_header(header);
}

static int
vhd_journal_read_locators(vhd_journal_t *j, char ***locators, int *locs)
{
	int err, n, _locs;
	char **_locators, *buf;
	off_t pos;
	vhd_journal_entry_t entry;

	_locs     = 0;
	*locs     = 0;
	*locators = NULL;

	n = sizeof(j->vhd.header.loc) / sizeof(vhd_parent_locator_t);
	_locators = calloc(n, sizeof(char *));
	if (!_locators)
		return -ENOMEM;

	for (;;) {
		buf = NULL;

		pos = vhd_journal_position(j);
		err = vhd_journal_read_entry(j, &entry);
		if (err)
			goto fail;

		if (entry.type != VHD_JOURNAL_ENTRY_TYPE_LOCATOR) {
			err = vhd_journal_seek(j, pos, SEEK_SET);
			if (err)
				goto fail;
			break;
		}

		if (_locs >= n) {
			err = -EINVAL;
			goto fail;
		}

		err = posix_memalign((void **)&buf,
				     VHD_SECTOR_SIZE, entry.size);
		if (err) {
			err = -err;
			buf = NULL;
			goto fail;
		}

		err = vhd_journal_read(j, buf, entry.size);
		if (err)
			goto fail;

		_locators[_locs++] = buf;
		err                = 0;
	}


	*locs     = _locs;
	*locators = _locators;

	return 0;

fail:
	if (_locators) {
		for (n = 0; n < _locs; n++)
			free(_locators[n]);
		free(_locators);
	}
	return err;
}

static int
vhd_journal_read_bat(vhd_journal_t *j, vhd_bat_t *bat)
{
	int err;
	size_t size;
	vhd_context_t *vhd;
	vhd_journal_entry_t entry;

	vhd  = &j->vhd;

	size = vhd_bytes_padded(vhd->header.max_bat_size * sizeof(uint32_t));

	err  = vhd_journal_read_entry(j, &entry);
	if (err)
		return err;

	if (entry.type != VHD_JOURNAL_ENTRY_TYPE_BAT)
		return -EINVAL;

	if (entry.size != size)
		return -EINVAL;

	if (entry.offset != vhd->header.table_offset)
		return -EINVAL;

	err = posix_memalign((void **)&bat->bat, VHD_SECTOR_SIZE, size);
	if (err)
		return -err;

	err = vhd_journal_read(j, bat->bat, entry.size);
	if (err)
		goto fail;

	bat->spb     = vhd->header.block_size >> VHD_SECTOR_SHIFT;
	bat->entries = vhd->header.max_bat_size;
	vhd_bat_in(bat);

	return 0;

fail:
	free(bat->bat);
	bat->bat = NULL;
	return err;
}

static int
vhd_journal_read_batmap_header(vhd_journal_t *j, vhd_batmap_t *batmap)
{
	int err;
	char *buf;
	size_t size;
	vhd_journal_entry_t entry;

	size = vhd_bytes_padded(sizeof(struct dd_batmap_hdr));

	err  = vhd_journal_read_entry(j, &entry);
	if (err)
		return err;

	if (entry.type != VHD_JOURNAL_ENTRY_TYPE_BATMAP_H)
		return -EINVAL;

	if (entry.size != size)
		return -EINVAL;

	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err)
		return err;

	err = vhd_journal_read(j, buf, entry.size);
	if (err) {
		free(buf);
		return err;
	}

	memcpy(&batmap->header, buf, sizeof(batmap->header));

	vhd_batmap_header_in(batmap);
	return vhd_validate_batmap_header(batmap);
}

static int
vhd_journal_read_batmap_map(vhd_journal_t *j, vhd_batmap_t *batmap)
{
	int err;
	vhd_journal_entry_t entry;

	err  = vhd_journal_read_entry(j, &entry);
	if (err)
		return err;

	if (entry.type != VHD_JOURNAL_ENTRY_TYPE_BATMAP_M)
		return -EINVAL;

	if (entry.size != vhd_sectors_to_bytes(batmap->header.batmap_size))
		return -EINVAL;

	if (entry.offset != batmap->header.batmap_offset)
		return -EINVAL;

	err = posix_memalign((void **)&batmap->map,
			     VHD_SECTOR_SIZE, entry.size);
	if (err)
		return -err;

	err = vhd_journal_read(j, batmap->map, entry.size);
	if (err) {
		free(batmap->map);
		batmap->map = NULL;
		return err;
	}

	return 0;
}

static int
vhd_journal_read_batmap(vhd_journal_t *j, vhd_batmap_t *batmap)
{
	int err;

	err = vhd_journal_read_batmap_header(j, batmap);
	if (err)
		return err;

	err = vhd_journal_read_batmap_map(j, batmap);
	if (err)
		return err;

	err = vhd_validate_batmap(batmap);
	if (err) {
		free(batmap->map);
		batmap->map = NULL;
		return err;
	}

	return 0;
}

static int
vhd_journal_restore_footer(vhd_journal_t *j, vhd_footer_t *footer)
{
	return vhd_write_footer_at(&j->vhd, footer,
				   j->header.vhd_footer_offset);
}

static int
vhd_journal_restore_footer_copy(vhd_journal_t *j, vhd_footer_t *footer)
{
	return vhd_write_footer_at(&j->vhd, footer, 0);
}

static int
vhd_journal_restore_header(vhd_journal_t *j, vhd_header_t *header)
{
	off_t off;
	vhd_context_t *vhd;

	vhd = &j->vhd;
	off = vhd->footer.data_offset;

	return vhd_write_header_at(&j->vhd, header, off);
}

static int
vhd_journal_restore_locators(vhd_journal_t *j, char **locators, int locs)
{
	size_t size;
	vhd_context_t *vhd;
	int i, n, lidx, err;
	vhd_parent_locator_t *loc;

	lidx = 0;
	vhd  = &j->vhd;

	n = sizeof(vhd->header.loc) / sizeof(vhd_parent_locator_t);

	for (i = 0; i < n && lidx < locs; i++) {
		loc  = vhd->header.loc + i;
		if (loc->code == PLAT_CODE_NONE)
			continue;

		err  = vhd_seek(vhd, loc->data_offset, SEEK_SET);
		if (err)
			return err;

		size = vhd_parent_locator_size(loc);
		err  = vhd_write(vhd, locators[lidx++], size);
		if (err)
			return err;
	}

	return 0;
}

static int
vhd_journal_restore_bat(vhd_journal_t *j, vhd_bat_t *bat)
{
	return vhd_write_bat(&j->vhd, bat);
}

static int
vhd_journal_restore_batmap(vhd_journal_t *j, vhd_batmap_t *batmap)
{
	return vhd_write_batmap(&j->vhd, batmap);
}

static int
vhd_journal_restore_metadata(vhd_journal_t *j)
{
	off_t off;
	char **locators;
	vhd_footer_t copy;
	vhd_context_t *vhd;
	int i, locs, hlocs, err;

	vhd      = &j->vhd;
	locs     = 0;
	hlocs    = 0;
	locators = NULL;

	err = vhd_journal_seek(j, sizeof(vhd_journal_header_t), SEEK_SET);
	if (err)
		return err;

	err  = vhd_journal_read_footer(j, &vhd->footer);
	if (err)
		return err;

	if (!vhd_type_dynamic(vhd))
		goto restore;

	err  = vhd_journal_read_footer_copy(j, &copy);
	if (err)
		return err;

	err  = vhd_journal_read_header(j, &vhd->header);
	if (err)
		return err;

	for (hlocs = 0, i = 0; i < vhd_parent_locator_count(vhd); i++) {
		if (vhd_validate_platform_code(vhd->header.loc[i].code))
			return err;

		if (vhd->header.loc[i].code != PLAT_CODE_NONE)
			hlocs++;
	}

	if (hlocs) {
		err  = vhd_journal_read_locators(j, &locators, &locs);
		if (err)
			return err;

		if (hlocs != locs) {
			err = -EINVAL;
			goto out;
		}
	}

	err  = vhd_journal_read_bat(j, &vhd->bat);
	if (err)
		goto out;

	if (vhd_has_batmap(vhd)) {
		err  = vhd_journal_read_batmap(j, &vhd->batmap);
		if (err)
			goto out;
	}

restore:
	off  = vhd_journal_position(j);
	if (off == (off_t)-1)
		return -errno;

	if (j->header.journal_data_offset != off)
		return -EINVAL;

	err  = vhd_journal_restore_footer(j, &vhd->footer);
	if (err)
		goto out;

	if (!vhd_type_dynamic(vhd))
		goto out;

	err  = vhd_journal_restore_footer_copy(j, &copy);
	if (err)
		goto out;

	err  = vhd_journal_restore_header(j, &vhd->header);
	if (err)
		goto out;

	if (locs) {
		err = vhd_journal_restore_locators(j, locators, locs);
		if (err)
			goto out;
	}

	err  = vhd_journal_restore_bat(j, &vhd->bat);
	if (err)
		goto out;

	if (vhd_has_batmap(vhd)) {
		err  = vhd_journal_restore_batmap(j, &vhd->batmap);
		if (err)
			goto out;
	}

	err = 0;

out:
	if (locators) {
		for (i = 0; i < locs; i++)
			free(locators[i]);
		free(locators);
	}

	if (!err && !vhd->is_block)
		err = ftruncate(vhd->fd,
			  j->header.vhd_footer_offset +
			  sizeof(vhd_footer_t));

	return err;
}

static int
vhd_journal_disable_vhd(vhd_journal_t *j)
{
	int err;
	vhd_context_t *vhd;

	vhd = &j->vhd;

	err = vhd_get_footer(vhd);
	if (err)
		return err;

	memcpy(&vhd->footer.cookie,
	       VHD_POISON_COOKIE, sizeof(vhd->footer.cookie));
	vhd->footer.checksum = vhd_checksum_footer(&vhd->footer);

	err = vhd_write_footer(vhd, &vhd->footer);
	if (err)
		return err;

	return 0;
}

static int
vhd_journal_enable_vhd(vhd_journal_t *j)
{
	int err;
	vhd_context_t *vhd;

	vhd = &j->vhd;

	err = vhd_get_footer(vhd);
	if (err)
		return err;

	if (!vhd_disabled(vhd))
		return 0;

	memcpy(&vhd->footer.cookie, HD_COOKIE, sizeof(vhd->footer.cookie));
	vhd->footer.checksum = vhd_checksum_footer(&vhd->footer);

	err = vhd_write_footer(vhd, &vhd->footer);
	if (err)
		return err;

	return 0;
}

int
vhd_journal_close(vhd_journal_t *j)
{
	if (j->jfd)
		close(j->jfd);

	vhd_close(&j->vhd);
	free(j->jname);

	return 0;
}

int
vhd_journal_remove(vhd_journal_t *j)
{
	int err;

	err = vhd_journal_enable_vhd(j);
	if (err)
		return err;

	if (j->jfd) {
		close(j->jfd);
		if (!j->is_block)
			unlink(j->jname);
	}

	vhd_close(&j->vhd);
	free(j->jname);

	return 0;
}

int
vhd_journal_open(vhd_journal_t *j, const char *file, const char *jfile)
{
	int err;
	vhd_context_t *vhd;

	memset(j, 0, sizeof(vhd_journal_t));

	j->jfd = -1;
	vhd    = &j->vhd;

	j->jname = strdup(jfile);
	if (j->jname == NULL)
		return -ENOMEM;

	j->jfd = open(j->jname, O_LARGEFILE | O_RDWR);
	if (j->jfd == -1) {
		err = -errno;
		goto fail;
	}

	err = vhd_test_file_fixed(j->jname, &j->is_block);
	if (err)
		goto fail;

	vhd->fd = open(file, O_LARGEFILE | O_RDWR | O_DIRECT);
	if (vhd->fd == -1) {
		err = -errno;
		goto fail;
	}

	err = vhd_test_file_fixed(file, &vhd->is_block);
	if (err)
		goto fail;

	err = vhd_journal_read_journal_header(j, &j->header);
	if (err)
		goto fail;

	err = vhd_journal_restore_metadata(j);
	if (err)
		goto fail;

	close(vhd->fd);
	free(vhd->bat.bat);
	free(vhd->batmap.map);

	err = vhd_open(vhd, file, VHD_OPEN_RDWR);
	if (err)
		goto fail;

	err = vhd_get_bat(vhd);
	if (err)
		goto fail;

	if (vhd_has_batmap(vhd)) {
		err = vhd_get_batmap(vhd);
		if (err)
			goto fail;
	}

	err = vhd_journal_disable_vhd(j);
	if (err)
		goto fail;

	return 0;

fail:
	vhd_journal_close(j);
	return err;
}

int
vhd_journal_create(vhd_journal_t *j, const char *file, const char *jfile)
{
	char *buf;
	int i, err;
	size_t size;
	off_t off;
	struct stat stats;

	memset(j, 0, sizeof(vhd_journal_t));
	j->jfd = -1;

	j->jname = strdup(jfile);
	if (j->jname == NULL) {
		err = -ENOMEM;
		goto fail1;
	}

	if (access(j->jname, F_OK) == 0) {
		err = vhd_test_file_fixed(j->jname, &j->is_block);
		if (err)
			goto fail1;

		if (!j->is_block) {
			err = -EEXIST;
			goto fail1;
		}
	}

	if (j->is_block)
		j->jfd = open(j->jname, O_LARGEFILE | O_RDWR, 0644);
	else
		j->jfd = open(j->jname,
			      O_CREAT | O_TRUNC | O_LARGEFILE | O_RDWR, 0644);
	if (j->jfd == -1) {
		err = -errno;
		goto fail1;
	}

	err = vhd_open(&j->vhd, file, VHD_OPEN_RDWR | VHD_OPEN_STRICT);
	if (err)
		goto fail1;

	err = vhd_get_bat(&j->vhd);
	if (err)
		goto fail2;

	if (vhd_has_batmap(&j->vhd)) {
		err = vhd_get_batmap(&j->vhd);
		if (err)
			goto fail2;
	}

	err = vhd_journal_add_journal_header(j);
	if (err)
		goto fail2;

	err = vhd_journal_add_metadata(j);
	if (err)
		goto fail2;

	err = vhd_journal_disable_vhd(j);
	if (err)
		goto fail2;

	err = vhd_journal_sync(j);
	if (err)
		goto fail2;

	return 0;

fail1:
	if (j->jfd != -1) {
		close(j->jfd);
		if (!j->is_block)
			unlink(j->jname);
	}
	free(j->jname);
	memset(j, 0, sizeof(vhd_journal_t));

	return err;

fail2:
	vhd_journal_remove(j);
	return err;
}

int
vhd_journal_add_block(vhd_journal_t *j, uint32_t block, char mode)
{
	int err;
	char *buf;
	off_t off;
	size_t size;
	uint64_t blk;
	vhd_context_t *vhd;

	buf = NULL;
	vhd = &j->vhd;

	if (!vhd_type_dynamic(vhd))
		return -EINVAL;

	err = vhd_get_bat(vhd);
	if (err)
		return err;

	if (block >= vhd->bat.entries)
		return -ERANGE;

	blk = vhd->bat.bat[block];
	if (blk == DD_BLK_UNUSED)
		return 0;

	off = vhd_sectors_to_bytes(blk);

	if (mode & VHD_JOURNAL_METADATA) {
		size = vhd_sectors_to_bytes(vhd->bm_secs);

		err  = vhd_read_bitmap(vhd, block, &buf);
		if (err)
			return err;

		err  = vhd_journal_update(j, off, buf, size,
					  VHD_JOURNAL_ENTRY_TYPE_DATA);

		free(buf);

		if (err)
			return err;
	}

	if (mode & VHD_JOURNAL_DATA) {
		off += vhd_sectors_to_bytes(vhd->bm_secs);
		size = vhd_sectors_to_bytes(vhd->spb);

		err  = vhd_read_block(vhd, block, &buf);
		if (err)
			return err;

		err  = vhd_journal_update(j, off, buf, size,
					  VHD_JOURNAL_ENTRY_TYPE_DATA);
		free(buf);

		if (err)
			return err;
	}

	return vhd_journal_sync(j);
}

/*
 * commit indicates the transaction completed 
 * successfully and we can remove the undo log
 */
int
vhd_journal_commit(vhd_journal_t *j)
{
	int err;

	j->header.journal_data_entries     = 0;
	j->header.journal_metadata_entries = 0;
	j->header.journal_data_offset      = 0;
	j->header.journal_metadata_offset  = 0;

	err = vhd_journal_write_header(j, &j->header);
	if (err)
		return err;

	if (!j->is_block)
		err = vhd_journal_truncate(j, sizeof(vhd_journal_header_t));
	if (err)
		return -errno;

	return 0;
}

/*
 * revert indicates the transaction failed
 * and we should revert any changes via the undo log
 */
int
vhd_journal_revert(vhd_journal_t *j)
{
	int i, err;
	char *buf, *file;
	vhd_context_t *vhd;
	vhd_journal_entry_t entry;

	err  = 0;
	vhd  = &j->vhd;
	buf  = NULL;

	file = strdup(vhd->file);
	if (!file)
		return -ENOMEM;

	vhd_close(&j->vhd);
	j->vhd.fd = open(file, O_RDWR | O_DIRECT | O_LARGEFILE);
	if (j->vhd.fd == -1) {
		free(file);
		return -errno;
	}

	err = vhd_test_file_fixed(file, &vhd->is_block);
	if (err) {
		free(file);
		return err;
	}

	err  = vhd_journal_restore_metadata(j);
	if (err) {
		free(file);
		return err;
	}

	close(vhd->fd);
	free(vhd->bat.bat);
	free(vhd->batmap.map);

	err = vhd_open(vhd, file, VHD_OPEN_RDWR);
	free(file);
	if (err)
		return err;

	err = vhd_journal_seek(j, j->header.journal_data_offset, SEEK_SET);
	if (err)
		return err;

	for (i = 0; i < j->header.journal_data_entries; i++) {
		err = vhd_journal_read_entry(j, &entry);
		if (err)
			goto end;

		err = posix_memalign((void **)&buf,
				     VHD_SECTOR_SIZE, entry.size);
		if (err) {
			err = -err;
			buf = NULL;
			goto end;
		}

		err = vhd_journal_read(j, buf, entry.size);
		if (err)
			goto end;

		err = vhd_journal_validate_entry_data(&entry, buf);
		if (err)
			goto end;

		err = vhd_seek(vhd, entry.offset, SEEK_SET);
		if (err)
			goto end;

		err = vhd_write(vhd, buf, entry.size);
		if (err)
			goto end;

		err = 0;

	end:
		free(buf);
		buf = NULL;
		if (err)
			break;
	}

	if (err)
		return err;

	if (!vhd->is_block) {
		err = ftruncate(vhd->fd, j->header.vhd_footer_offset +
				sizeof(vhd_footer_t));
		if (err)
			return -errno;
	}

	return vhd_journal_sync(j);
}
