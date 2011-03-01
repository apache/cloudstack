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
 *
 * Before updating a VHD file, we create a journal consisting of:
 *   - all data at the beginning of the file, up to and including the BAT
 *   - each allocated bitmap (existing at the same offset in the journal as
 *                            its corresponding bitmap in the original file)
 * Updates are performed in place by writing appropriately 
 * transformed versions of journaled bitmaps to the original file.
 */
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

#include "atomicio.h"
#include "libvhd.h"
#include "libvhd-journal.h"

static void
usage(void)
{
	printf("usage: vhd-update <-n name> [-j existing journal] [-h]\n");
	exit(EINVAL);
}

/*
 * update vhd creator version to reflect its new bitmap ordering
 */
static inline int
update_creator_version(vhd_journal_t *journal)
{
	journal->vhd.footer.crtr_ver = VHD_VERSION(1, 1);
	return vhd_write_footer(&journal->vhd, &journal->vhd.footer);
}

static int
journal_bitmaps(vhd_journal_t *journal)
{
	int i, err;

	for (i = 0; i < journal->vhd.bat.entries; i++) {
		err = vhd_journal_add_block(journal, i, VHD_JOURNAL_METADATA);
		if (err)
			return err;
	}

	return 0;
}

/*
 * older VHD bitmaps were little endian
 * and bits within a word were set from right to left
 */
static inline int
old_test_bit(int nr, volatile void * addr)
{
        return (((unsigned long*)addr)[nr/(sizeof(unsigned long)*8)] >>
                (nr % (sizeof(unsigned long)*8))) & 1;
}

/*
 * new VHD bitmaps are big endian
 * and bits within a word are set from left to right
 */
#define BIT_MASK 0x80
static inline void
new_set_bit (int nr, volatile char *addr)
{
        addr[nr >> 3] |= (BIT_MASK >> (nr & 7));
}

static void
convert_bitmap(char *in, char *out, int bytes)
{
	int i;

	memset(out, 0, bytes);

	for (i = 0; i < bytes << 3; i++)
		if (old_test_bit(i, (void *)in))
			new_set_bit(i, out);
}

static int
update_vhd(vhd_journal_t *journal, int rollback)
{
	int i, err;
	size_t size;
	char *buf, *converted;

	buf       = NULL;
	converted = NULL;

	size = vhd_bytes_padded(journal->vhd.spb / 8);
	err  = posix_memalign((void **)&converted, 512, size);
	if (err) {
		converted = NULL;
		goto out;
	}

	for (i = 0; i < journal->vhd.bat.entries; i++) {
		if (journal->vhd.bat.bat[i] == DD_BLK_UNUSED)
			continue;

		err = vhd_read_bitmap(&journal->vhd, i, &buf);
		if (err)
			goto out;

		if (rollback)
			memcpy(converted, buf, size);
		else
			convert_bitmap(buf, converted, size);

		free(buf);

		err = vhd_write_bitmap(&journal->vhd, i, converted);
		if (err)
			goto out;
	}

	err = 0;
 out:
	free(converted);
	return err;
}

static int
open_journal(vhd_journal_t *journal, const char *file, const char *jfile)
{
	int err;

	err = vhd_journal_create(journal, file, jfile);
	if (err) {
		printf("error creating journal for %s: %d\n", file, err);
		return err;
	}

	return 0;
}

static int
close_journal(vhd_journal_t *journal, int err)
{
	if (err)
		err = vhd_journal_revert(journal);
	else
		err = vhd_journal_commit(journal);

	if (err)
		return vhd_journal_close(journal);
	else
		return vhd_journal_remove(journal);
}

int
main(int argc, char **argv)
{
	char *file, *jfile;
	int c, err, rollback;
	vhd_journal_t journal;

	file     = NULL;
	jfile    = NULL;
	rollback = 0;

	while ((c = getopt(argc, argv, "n:j:rh")) != -1) {
		switch(c) {
		case 'n':
			file = optarg;
			break;
		case 'j':
			jfile = optarg;
			err = access(jfile, R_OK);
			if (err == -1) {
				printf("invalid journal arg %s\n", jfile);
				return -errno;
			}
			break;
		case 'r':
			/* add a rollback option for debugging which
			 * pushes journalled bitmaps to original file
			 * without transforming them */
			rollback = 1;
			break;
		default:
			usage();
		}
	}

	if (!file)
		usage();

	if (rollback && !jfile) {
		printf("rollback requires a journal argument\n");
		usage();
	}

	err = open_journal(&journal, file, jfile);
	if (err)
		return err;

	if (!vhd_creator_tapdisk(&journal.vhd) ||
	    journal.vhd.footer.crtr_ver != VHD_VERSION(0, 1) ||
	    journal.vhd.footer.type == HD_TYPE_FIXED) {
		err = 0;
		goto out;
	}

	err = journal_bitmaps(&journal);
	if (err) {
		/* no changes to vhd file yet,
		 * so close the journal and bail */
		vhd_journal_close(&journal);
		return err;
	}

	err = update_vhd(&journal, rollback);
	if (err) {
		printf("update failed: %d; saving journal\n", err);
		goto out;
	}

	err = update_creator_version(&journal);
	if (err) {
		printf("failed to udpate creator version: %d\n", err);
		goto out;
	}

	err = 0;

out:
	err = close_journal(&journal, err);
	return err;
}
