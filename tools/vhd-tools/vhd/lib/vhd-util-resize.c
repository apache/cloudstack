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
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <syslog.h>
#include <inttypes.h>
#include <sys/mman.h>

#include "libvhd-journal.h"

#if 1
#define DFPRINTF(_f, _a...) fprintf(stdout, _f, ##_a)
#else
#define DFPRINTF(_f, _a...) ((void)0)
#endif

#define EPRINTF(_f, _a...)					\
	do {							\
		syslog(LOG_INFO, "%s: " _f, __func__, ##_a);	\
		DFPRINTF(_f, _a);				\
	} while (0)

typedef struct vhd_block {
	uint32_t block;
	uint32_t offset;
} vhd_block_t;

TEST_FAIL_EXTERN_VARS;

static inline uint32_t
secs_to_blocks_down(vhd_context_t *vhd, uint64_t secs)
{
	return secs / vhd->spb;
}

static uint32_t
secs_to_blocks_up(vhd_context_t *vhd, uint64_t secs)
{
	uint32_t blocks;

	blocks = secs / vhd->spb;
	if (secs % vhd->spb)
		blocks++;

	return blocks;
}

static int
vhd_fixed_shrink(vhd_journal_t *journal, uint64_t secs)
{
	int err;
	uint64_t new_eof;
	vhd_context_t *vhd;

	vhd = &journal->vhd;

	new_eof = vhd->footer.curr_size - vhd_sectors_to_bytes(secs);
	if (new_eof <= sizeof(vhd_footer_t))
		return -EINVAL;

	err = ftruncate(vhd->fd, new_eof);
	if (err)
		return errno;

	vhd->footer.curr_size = new_eof;
	return vhd_write_footer(vhd, &vhd->footer);
}

static int
vhd_write_zeros(vhd_journal_t *journal, off_t off, uint64_t size)
{
	int err;
	char *buf;
	vhd_context_t *vhd;
	uint64_t bytes, map;

	vhd = &journal->vhd;
	map = MIN(size, VHD_BLOCK_SIZE);

	err = vhd_seek(vhd, off, SEEK_SET);
	if (err)
		return err;

	buf = mmap(0, map, PROT_READ, MAP_SHARED | MAP_ANON, -1, 0);
	if (buf == MAP_FAILED)
		return -errno;

	do {
		bytes = MIN(size, map);

		err = vhd_write(vhd, buf, bytes);
		if (err)
			break;

		size -= bytes;
	} while (size);

	munmap(buf, map);

	return err;
}

static int
vhd_fixed_grow(vhd_journal_t *journal, uint64_t secs)
{
	int err;
	vhd_context_t *vhd;
	uint64_t size, eof, new_eof;

	size = vhd_sectors_to_bytes(secs);
	vhd  = &journal->vhd;

	err = vhd_seek(vhd, 0, SEEK_END);
	if (err)
		goto out;

	eof = vhd_position(vhd);
	if (eof == (off_t)-1) {
		err = -errno;
		goto out;
	}

	err = vhd_write_zeros(journal, eof - sizeof(vhd_footer_t), size);
	if (err)
		goto out;

	new_eof = eof + size;
	err = vhd_seek(vhd, new_eof, SEEK_SET);
	if (err)
		goto out;

	vhd->footer.curr_size += size;
	err = vhd_write_footer(vhd, &vhd->footer);
	if (err)
		goto out;

	err = 0;

out:
	return err;
}

static int
vhd_fixed_resize(vhd_journal_t *journal, uint64_t size)
{
	int err;
	vhd_context_t *vhd;
	uint64_t cur_secs, new_secs;

	vhd      = &journal->vhd;
	cur_secs = vhd->footer.curr_size >> VHD_SECTOR_SHIFT;
	new_secs = size << (20 - VHD_SECTOR_SHIFT);

	if (cur_secs == new_secs)
		return 0;
	else if (cur_secs > new_secs)
		err = vhd_fixed_shrink(journal, cur_secs - new_secs);
	else
		err = vhd_fixed_grow(journal, new_secs - cur_secs);

	return err;
}

static inline void
swap(vhd_block_t *list, int a, int b)
{
	vhd_block_t tmp;

	tmp     = list[a];
	list[a] = list[b];
	list[b] = tmp;
}

static int
partition(vhd_block_t *list, int left, int right, int pidx)
{
	int i, sidx;
	long long pval;

	sidx = left;
	pval = list[pidx].offset;
	swap(list, pidx, right);

	for (i = left; i < right; i++)
		if (list[i].offset >= pval) {
			swap(list, sidx, i);
			++sidx;
		}

	swap(list, right, sidx);
	return sidx;
}

static void
quicksort(vhd_block_t *list, int left, int right)
{
	int pidx, new_pidx;

	if (right < left)
		return;

	pidx     = left;
	new_pidx = partition(list, left, right, pidx);
	quicksort(list, left, new_pidx - 1);
	quicksort(list, new_pidx + 1, right);
}

static int
vhd_move_block(vhd_journal_t *journal, uint32_t src, off_t offset)
{
	int err;
	char *buf;
	size_t size;
	vhd_context_t *vhd;
	off_t off, src_off;

	buf     = NULL;
	vhd     = &journal->vhd;
	off     = offset;
	size    = vhd_sectors_to_bytes(vhd->bm_secs);
	src_off = vhd->bat.bat[src];

	if (src_off == DD_BLK_UNUSED)
		return -EINVAL;
	src_off = vhd_sectors_to_bytes(src_off);

	err  = vhd_journal_add_block(journal, src,
				     VHD_JOURNAL_DATA | VHD_JOURNAL_METADATA);
	if (err)
		goto out;

	err  = vhd_read_bitmap(vhd, src, &buf);
	if (err)
		goto out;

	err  = vhd_seek(vhd, off, SEEK_SET);
	if (err)
		goto out;

	err  = vhd_write(vhd, buf, size);
	if (err)
		goto out;

	free(buf);
	buf   = NULL;
	off  += size;
	size  = vhd_sectors_to_bytes(vhd->spb);

	err  = vhd_read_block(vhd, src, &buf);
	if (err)
		goto out;

	err  = vhd_seek(vhd, off, SEEK_SET);
	if (err)
		goto out;

	err  = vhd_write(vhd, buf, size);
	if (err)
		goto out;

	vhd->bat.bat[src] = offset >> VHD_SECTOR_SHIFT;

	err = vhd_write_zeros(journal, src_off,
			      vhd_sectors_to_bytes(vhd->bm_secs + vhd->spb));

out:
	free(buf);
	return err;
}

static int
vhd_clobber_block(vhd_journal_t *journal, uint32_t src, uint32_t dest)
{
	int err;
	off_t off;
	vhd_context_t *vhd;

	vhd = &journal->vhd;
	off = vhd_sectors_to_bytes(vhd->bat.bat[dest]);

	err = vhd_journal_add_block(journal, dest,
				    VHD_JOURNAL_DATA | VHD_JOURNAL_METADATA);
	if (err)
		return err;

	err = vhd_move_block(journal, src, off);
	if (err)
		return err;

	vhd->bat.bat[dest] = DD_BLK_UNUSED;

	return 0;
}

/*
 * remove a list of blocks from the vhd file
 * if a block to be removed:
 *   - resides at the end of the file: simply clear its bat entry
 *   - resides elsewhere: move the last block in the file into its position
 *                        and update the bat to reflect this
 */
static int
vhd_defrag_shrink(vhd_journal_t *journal,
		  vhd_block_t *original_free_list, int free_cnt)
{
	vhd_context_t *vhd;
	int i, j, free_idx, err;
	vhd_block_t *blocks, *free_list;

	err       = 0;
	blocks    = NULL;
	free_list = NULL;
	vhd       = &journal->vhd;

	blocks = malloc(vhd->bat.entries * sizeof(vhd_block_t));
	if (!blocks) {
		err = -ENOMEM;
		goto out;
	}

	free_list = malloc(free_cnt * sizeof(vhd_block_t));
	if (!free_list) {
		err = -ENOMEM;
		goto out;
	}

	for (i = 0; i < vhd->bat.entries; i++) {
		blocks[i].block  = i;
		blocks[i].offset = vhd->bat.bat[i];
	}

	memcpy(free_list, original_free_list,
	       free_cnt * sizeof(vhd_block_t));

	/* sort both the to-free list and the bat list
	 * in order of descending file offset */
	quicksort(free_list, 0, free_cnt - 1);
	quicksort(blocks, 0, vhd->bat.entries - 1);

	for (i = 0, free_idx = 0;
	     i < vhd->bat.entries && free_idx < free_cnt; i++) {
		vhd_block_t *b = blocks + i;

		if (b->offset == DD_BLK_UNUSED)
			continue;

		for (j = free_idx; j < free_cnt; j++)
			if (b->block == free_list[j].block) {
				/* the last block in the file is in the list of
				 * blocks to remove; no need to shuffle the
				 * data -- just clear the bat entry */
				vhd->bat.bat[free_list[j].block] = DD_BLK_UNUSED;
				free_idx++;
				continue;
			}

		err = vhd_clobber_block(journal, b->block,
					free_list[free_idx++].block);
		if (err)
			goto out;
	}

	/* clear any bat entries for blocks we did not shuffle */
	for (i = free_idx; i < free_cnt; i++)
		vhd->bat.bat[free_list[i].block] = DD_BLK_UNUSED;

out:
	free(blocks);
	free(free_list);

	return err;
}

static int
vhd_clear_bat_entries(vhd_journal_t *journal, uint32_t entries)
{
	int i, err;
	vhd_context_t *vhd;
	off_t orig_map_off, new_map_off;
	uint32_t orig_entries, new_entries;

	vhd          = &journal->vhd;
	orig_entries = vhd->header.max_bat_size;
	new_entries  = orig_entries - entries;

	if (vhd_has_batmap(vhd)) {
		err = vhd_batmap_header_offset(vhd, &orig_map_off);
		if (err)
			return err;
	}

	/* update header */
	vhd->header.max_bat_size = new_entries;
	err = vhd_write_header(vhd, &vhd->header);
	if (err)
		return err;

	/* update footer */
	vhd->footer.curr_size =	(uint64_t)new_entries * vhd->header.block_size;
	vhd->footer.geometry  = vhd_chs(vhd->footer.curr_size);
	err = vhd_write_footer(vhd, &vhd->footer);
	if (err)
		return err;

	/* update bat -- we don't reclaim space, just clear entries */
	for (i = new_entries; i < orig_entries; i++)
		vhd->bat.bat[i] = 0;

	err = vhd_write_bat(vhd, &vhd->bat);
	if (err)
		return err;

	/* update this after write_bat so the end of the bat is zeored */
	vhd->bat.entries = new_entries;

	if (!vhd_has_batmap(vhd))
		return 0;

	/* zero out old batmap header if new header has moved */
	err = vhd_batmap_header_offset(vhd, &new_map_off);
	if (err)
		return err;

	if (orig_map_off != new_map_off) {
		size_t size;

		size = vhd_bytes_padded(sizeof(struct dd_batmap_hdr));

		err = vhd_write_zeros(journal, orig_map_off, size);
		if (err)
			return err;
	}

	/* update batmap -- clear entries for freed blocks */
	for (i = new_entries; i < orig_entries; i++)
		vhd_batmap_clear(vhd, &vhd->batmap, i);

	err = vhd_write_batmap(vhd, &vhd->batmap);
	if (err)
		return err;

	return 0;
}

static int
vhd_dynamic_shrink(vhd_journal_t *journal, uint64_t secs)
{
	off_t eof;
	uint32_t blocks;
	vhd_context_t *vhd;
	int i, j, err, free_cnt;
	struct vhd_block *free_list;

	printf("dynamic shrink not fully implemented\n");
	return -ENOSYS;

	eof       = 0;
	free_cnt  = 0;
	free_list = NULL;
	vhd       = &journal->vhd;

	blocks    = secs_to_blocks_down(vhd, secs);
	if (blocks == 0)
		return 0;

	if (vhd_has_batmap(vhd)) {
		err = vhd_get_batmap(vhd);
		if (err)
			return err;
	}

	free_list = malloc(blocks * sizeof(struct vhd_block));
	if (!free_list)
		return -ENOMEM;

	for (i = vhd->bat.entries - 1, j = 0; i >= 0 && j < blocks; i--, j++) {
		uint32_t blk = vhd->bat.bat[i];

		if (blk != DD_BLK_UNUSED) {
			free_list[free_cnt].block  = i;
			free_list[free_cnt].offset = blk;
			free_cnt++;
		}
	}

	if (free_cnt) {
		err = vhd_defrag_shrink(journal, free_list, free_cnt);
		if (err)
			goto out;
	}

	err = vhd_clear_bat_entries(journal, blocks);
	if (err)
		goto out;

	/* remove data beyond footer */
	err = vhd_end_of_data(vhd, &eof);
	if (err)
		goto out;

	err = ftruncate(vhd->fd, eof + sizeof(vhd_footer_t));
	if (err) {
		err = -errno;
		goto out;
	}

	err = 0;

out:
	free(free_list);
	return err;
}

static inline void
vhd_first_data_block(vhd_context_t *vhd, vhd_block_t *block)
{
	int i;
	uint32_t blk;

	memset(block, 0, sizeof(vhd_block_t));

	for (i = 0; i < vhd->bat.entries; i++) {
		blk = vhd->bat.bat[i];

		if (blk != DD_BLK_UNUSED) {
			if (!block->offset || blk < block->offset) {
				block->block  = i;
				block->offset = blk;
			}
		}
	}
}

static inline uint32_t
vhd_next_block_offset(vhd_context_t *vhd)
{
	int i;
	uint32_t blk, end, spp, next;

	next = 0;
	spp  = getpagesize() >> VHD_SECTOR_SHIFT;

	for (i = 0; i < vhd->bat.entries; i++) {
		blk = vhd->bat.bat[i];

		if (blk != DD_BLK_UNUSED) {
			end  = blk + vhd->spb + vhd->bm_secs;
			next = MAX(next, end);
		}
	}

	return next;
}

static inline int
in_range(off_t off, off_t start, off_t size)
{
	return (start < off && start + size > off);
}

#define SKIP_HEADER 0x01
#define SKIP_BAT    0x02
#define SKIP_BATMAP 0x04
#define SKIP_PLOC   0x08
#define SKIP_DATA   0x10

static inline int
skip_check(int mode, int type)
{
	return mode & type;
}

static int
vhd_check_for_clobber(vhd_context_t *vhd, off_t off, int mode)
{
	int i, n;
	char *msg;
	size_t size;
	vhd_block_t fb;
	vhd_parent_locator_t *loc;

	msg = NULL;

	if (!vhd_type_dynamic(vhd))
		return 0;

	if (off < VHD_SECTOR_SIZE) {
		msg = "backup footer";
		goto fail;
	}

	if (!skip_check(mode, SKIP_HEADER))
		if (in_range(off,
			     vhd->footer.data_offset, sizeof(vhd_header_t))) {
			msg = "header";
			goto fail;
		}

	if (!skip_check(mode, SKIP_BAT))
		if (in_range(off, vhd->header.table_offset,
			     vhd_bytes_padded(vhd->header.max_bat_size *
					      sizeof(uint32_t)))) {
			msg = "bat";
			goto fail;
		}

	if (!skip_check(mode, SKIP_BATMAP))
		if (vhd_has_batmap(vhd) &&
		    in_range(off, vhd->batmap.header.batmap_offset,
			     vhd_bytes_padded(vhd->batmap.header.batmap_size))) {
			msg = "batmap";
			goto fail;
		}

	if (!skip_check(mode, SKIP_PLOC)) {
		n = sizeof(vhd->header.loc) / sizeof(vhd_parent_locator_t);
		for (i = 0; i < n; i++) {
			loc = vhd->header.loc + i;
			if (loc->code == PLAT_CODE_NONE)
				continue;

			size = vhd_parent_locator_size(loc);
			if (in_range(off, loc->data_offset, size)) {
				msg = "parent locator";
				goto fail;
			}
		}
	}

	if (!skip_check(mode, SKIP_DATA)) {
		vhd_first_data_block(vhd, &fb);
		if (fb.offset && in_range(off,
					  vhd_sectors_to_bytes(fb.offset),
					  VHD_BLOCK_SIZE)) {
			msg = "data block";
			goto fail;
		}
	}

	return 0;

fail:
	EPRINTF("write to 0x%08"PRIx64" would clobber %s\n", off, msg);
	return -EINVAL;
}

/*
 * take any metadata after the bat (@eob) and shift it
 */
static int
vhd_shift_metadata(vhd_journal_t *journal, off_t eob,
		   size_t bat_needed, size_t map_needed)
{
	int i, n, err;
	vhd_context_t *vhd;
	size_t size_needed;
	char *buf, **locators;
	vhd_parent_locator_t *loc;

	vhd         = &journal->vhd;
	size_needed = bat_needed + map_needed;

	n = sizeof(vhd->header.loc) / sizeof(vhd_parent_locator_t);

	locators = calloc(n, sizeof(char *));
	if (!locators)
		return -ENOMEM;

	for (i = 0; i < n; i++) {
		size_t size;

		loc = vhd->header.loc + i;
		if (loc->code == PLAT_CODE_NONE)
			continue;

		if (loc->data_offset < eob)
			continue;

		size = vhd_parent_locator_size(loc);
		err  = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
		if (err) {
			err = -err;
			buf = NULL;
			goto out;
		}

		err  = vhd_seek(vhd, loc->data_offset, SEEK_SET);
		if (err)
			goto out;

		err  = vhd_read(vhd, buf, size);
		if (err)
			goto out;

		locators[i] = buf;
	}

	for (i = 0; i < n; i++) {
		off_t off;
		size_t size;

		if (!locators[i])
			continue;

		loc  = vhd->header.loc + i;
		off  = loc->data_offset + size_needed;
		size = vhd_parent_locator_size(loc);

		if (vhd_check_for_clobber(vhd, off + size, SKIP_PLOC)) {
			EPRINTF("%s: shifting locator %d would clobber data\n",
				vhd->file, i);
			return -EINVAL;
		}

		err  = vhd_seek(vhd, off, SEEK_SET);
		if (err)
			goto out;

		err  = vhd_write(vhd, locators[i], size);
		if (err)
			goto out;

		free(locators[i]);
		locators[i]      = NULL;
		loc->data_offset = off;

		/* write the new header after writing the new bat */
	}

	if (vhd_has_batmap(vhd) && vhd->batmap.header.batmap_offset > eob) {
		vhd->batmap.header.batmap_offset += bat_needed;

		/* write the new batmap after writing the new bat */
	}

	err = 0;

out:
	for (i = 0; i < n; i++)
		free(locators[i]);
	free(locators);

	return err;
}

static int
vhd_add_bat_entries(vhd_journal_t *journal, int entries)
{
	int i, err;
	off_t off;
	vhd_bat_t new_bat;
	vhd_context_t *vhd;
	uint32_t new_entries;
	vhd_batmap_t new_batmap;
	uint64_t bat_size, new_bat_size, map_size, new_map_size;

	vhd          = &journal->vhd;
	new_entries  = vhd->header.max_bat_size + entries;

	bat_size     = vhd_bytes_padded(vhd->header.max_bat_size *
					sizeof(uint32_t));
	new_bat_size = vhd_bytes_padded(new_entries * sizeof(uint32_t));

	map_size     = vhd_bytes_padded((vhd->header.max_bat_size + 7) >> 3);
	new_map_size = vhd_bytes_padded((new_entries + 7) >> 3);

	off = vhd->header.table_offset + new_bat_size;
	if (vhd_check_for_clobber(vhd, off, SKIP_BAT | SKIP_BATMAP)) {
		EPRINTF("%s: writing new bat of 0x%"PRIx64" bytes "
			"at 0x%08"PRIx64" would clobber data\n", 
			vhd->file, new_bat_size, vhd->header.table_offset);
		return -EINVAL;
	}

	if (vhd_has_batmap(vhd)) {
		off = vhd->batmap.header.batmap_offset + new_map_size;
		if (vhd_check_for_clobber(vhd, off, 0)) {
			EPRINTF("%s: writing new batmap of 0x%"PRIx64" bytes"
				" at 0x%08"PRIx64" would clobber data\n", vhd->file,
				new_map_size, vhd->batmap.header.batmap_offset);
			return -EINVAL;
		}
	}

	/* update header */
	vhd->header.max_bat_size = new_entries;
	err = vhd_write_header(vhd, &vhd->header);
	if (err)
		return err;

	/* update footer */
	vhd->footer.curr_size = (uint64_t)new_entries * vhd->header.block_size;
	vhd->footer.geometry  = vhd_chs(vhd->footer.curr_size);
	vhd->footer.checksum  = vhd_checksum_footer(&vhd->footer);
	err = vhd_write_footer(vhd, &vhd->footer);
	if (err)
		return err;

	/* allocate new bat */
	err = posix_memalign((void **)&new_bat.bat, VHD_SECTOR_SIZE, new_bat_size);
	if (err)
		return -err;

	new_bat.spb     = vhd->bat.spb;
	new_bat.entries = new_entries;
	memcpy(new_bat.bat, vhd->bat.bat, bat_size);
	for (i = vhd->bat.entries; i < new_entries; i++)
		new_bat.bat[i] = DD_BLK_UNUSED;

	/* write new bat */
	err = vhd_write_bat(vhd, &new_bat);
	if (err) {
		free(new_bat.bat);
		return err;
	}

	/* update in-memory bat */
	free(vhd->bat.bat);
	vhd->bat = new_bat;

	if (!vhd_has_batmap(vhd))
		return 0;

	/* allocate new batmap */
	err = posix_memalign((void **)&new_batmap.map,
			     VHD_SECTOR_SIZE, new_map_size);
	if (err)
		return err;

	new_batmap.header = vhd->batmap.header;
	new_batmap.header.batmap_size = secs_round_up_no_zero(new_map_size);
	memcpy(new_batmap.map, vhd->batmap.map, map_size);
	memset(new_batmap.map + map_size, 0, new_map_size - map_size);

	/* write new batmap */
	err = vhd_write_batmap(vhd, &new_batmap);
	if (err) {
		free(new_batmap.map);
		return err;
	}

	/* update in-memory batmap */
	free(vhd->batmap.map);
	vhd->batmap = new_batmap;

	return 0;
}

static int
vhd_dynamic_grow(vhd_journal_t *journal, uint64_t secs)
{
	int i, err;
	off_t eob, eom;
	vhd_context_t *vhd;
	vhd_block_t first_block;
	uint64_t blocks, size_needed;
	uint64_t bat_needed, bat_size, bat_avail, bat_bytes, bat_secs;
	uint64_t map_needed, map_size, map_avail, map_bytes, map_secs;

	vhd         = &journal->vhd;

	size_needed = 0;
	bat_needed  = 0;
	map_needed  = 0;

	/* number of vhd blocks to add */
	blocks      = secs_to_blocks_up(vhd, secs);

	/* size in bytes needed for new bat entries */
	bat_needed  = blocks * sizeof(uint32_t);
	map_needed  = (blocks >> 3) + 1;

	/* available bytes in current bat */
	bat_bytes   = vhd->header.max_bat_size * sizeof(uint32_t);
	bat_secs    = secs_round_up_no_zero(bat_bytes);
	bat_size    = vhd_sectors_to_bytes(bat_secs);
	bat_avail   = bat_size - bat_bytes;

	if (vhd_has_batmap(vhd)) {
		/* avaliable bytes in current batmap */
		map_bytes   = (vhd->header.max_bat_size + 7) >> 3;
		map_secs    = vhd->batmap.header.batmap_size;
		map_size    = vhd_sectors_to_bytes(map_secs);
		map_avail   = map_size - map_bytes;
	} else {
		map_needed  = 0;
		map_avail   = 0;
	}

	/* we have enough space already; just extend the bat */
	if (bat_needed <= bat_avail && map_needed <= map_avail)
		goto add_entries;

	/* we need to add new sectors to the bat */
	if (bat_needed > bat_avail) {
		bat_needed -= bat_avail;
		bat_needed  = vhd_bytes_padded(bat_needed);
	} else
		bat_needed  = 0;

	/* we need to add new sectors to the batmap */
	if (map_needed > map_avail) {
		map_needed -= map_avail;
		map_needed  = vhd_bytes_padded(map_needed);
	} else
		map_needed  = 0;

	/* how many additional bytes do we need? */
	size_needed = bat_needed + map_needed;

	/* calculate space between end of headers and beginning of data */
	err = vhd_end_of_headers(vhd, &eom);
	if (err)
		return err;

	eob = vhd->header.table_offset + vhd_sectors_to_bytes(bat_secs);
	vhd_first_data_block(vhd, &first_block);

	/* no blocks allocated; just shift post-bat metadata */
	if (!first_block.offset)
		goto shift_metadata;

	/* 
	 * not enough space -- 
	 * move vhd data blocks to the end of the file to make room 
	 */
	do {
		off_t new_off, bm_size, gap_size;

		new_off = vhd_sectors_to_bytes(vhd_next_block_offset(vhd));

		/* data region of segment should begin on page boundary */
		bm_size = vhd_sectors_to_bytes(vhd->bm_secs);
		if ((new_off + bm_size) % 4096) {
			gap_size = 4096 - ((new_off + bm_size) % 4096);

			err = vhd_write_zeros(journal, new_off, gap_size);
			if (err)
				return err;

			new_off += gap_size;
		}

		err = vhd_move_block(journal, first_block.block, new_off);
		if (err)
			return err;

		vhd_first_data_block(vhd, &first_block);

	} while (eom + size_needed >= vhd_sectors_to_bytes(first_block.offset));

	TEST_FAIL_AT(FAIL_RESIZE_DATA_MOVED);

shift_metadata:
	/* shift any metadata after the bat to make room for new bat sectors */
	err = vhd_shift_metadata(journal, eob, bat_needed, map_needed);
	if (err)
		return err;

	TEST_FAIL_AT(FAIL_RESIZE_METADATA_MOVED);

add_entries:
	return vhd_add_bat_entries(journal, blocks);
}

static int
vhd_dynamic_resize(vhd_journal_t *journal, uint64_t size)
{
	int err;
	vhd_context_t *vhd;
	uint64_t cur_secs, new_secs;

	vhd      = &journal->vhd;
	cur_secs = vhd->footer.curr_size >> VHD_SECTOR_SHIFT;
	new_secs = size << (20 - VHD_SECTOR_SHIFT);

	if (cur_secs == new_secs)
		return 0;

	err = vhd_get_header(vhd);
	if (err)
		return err;

	err = vhd_get_bat(vhd);
	if (err)
		return err;

	if (vhd_has_batmap(vhd)) {
		err = vhd_get_batmap(vhd);
		if (err)
			return err;
	}

	if (cur_secs > new_secs)
		err = vhd_dynamic_shrink(journal, cur_secs - new_secs);
	else
		err = vhd_dynamic_grow(journal, new_secs - cur_secs);

	return err;
}

static int
vhd_util_resize_check_creator(const char *name)
{
	int err;
	vhd_context_t vhd;

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY | VHD_OPEN_STRICT);
	if (err) {
		printf("error opening %s: %d\n", name, err);
		return err;
	}

	if (!vhd_creator_tapdisk(&vhd)) {
		printf("%s not created by xen; resize not supported\n", name);
		err = -EINVAL;
	}

	vhd_close(&vhd);
	return err;
}

int
vhd_util_resize(int argc, char **argv)
{
	char *name, *jname;
	uint64_t size;
	int c, err, jerr;
	vhd_journal_t journal;
	vhd_context_t *vhd;

	err   = -EINVAL;
	size  = 0;
	name  = NULL;
	jname = NULL;

	optind = 0;
	while ((c = getopt(argc, argv, "n:j:s:h")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 'j':
			jname = optarg;
			break;
		case 's':
			err  = 0;
			size = strtoull(optarg, NULL, 10);
			break;
		case 'h':
		default:
			goto usage;
		}
	}

	if (err || !name || !jname || argc != optind)
		goto usage;

	err = vhd_util_resize_check_creator(name);
	if (err)
		return err;

	libvhd_set_log_level(1);
	err = vhd_journal_create(&journal, name, jname);
	if (err) {
		printf("creating journal failed: %d\n", err);
		return err;
	}

	vhd = &journal.vhd;

	err = vhd_get_footer(vhd);
	if (err)
		goto out;

	TEST_FAIL_AT(FAIL_RESIZE_BEGIN);

	if (vhd_type_dynamic(vhd))
		err = vhd_dynamic_resize(&journal, size);
	else
		err = vhd_fixed_resize(&journal, size);

	TEST_FAIL_AT(FAIL_RESIZE_END);

out:
	if (err) {
		printf("resize failed: %d\n", err);
		jerr = vhd_journal_revert(&journal);
	} else
		jerr = vhd_journal_commit(&journal);

	if (jerr) {
		printf("closing journal failed: %d\n", jerr);
		vhd_journal_close(&journal);
	} else
		vhd_journal_remove(&journal);

	return (err ? : jerr);

usage:
	printf("options: <-n name> <-j journal> <-s size (in MB)> [-h help]\n");
	return -EINVAL;
}
