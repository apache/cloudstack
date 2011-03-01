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

#include "libvhd.h"

static int
__raw_io_write(int fd, char* buf, uint64_t sec, uint32_t secs)
{
	off_t off;
	size_t ret;

	errno = 0;
	off = lseek(fd, vhd_sectors_to_bytes(sec), SEEK_SET);
	if (off == (off_t)-1) {
		printf("raw parent: seek(0x%08"PRIx64") failed: %d\n",
		       vhd_sectors_to_bytes(sec), -errno);
		return -errno;
	}

	ret = write(fd, buf, vhd_sectors_to_bytes(secs));
	if (ret == vhd_sectors_to_bytes(secs))
		return 0;

	printf("raw parent: write of 0x%"PRIx64" returned %zd, errno: %d\n",
	       vhd_sectors_to_bytes(secs), ret, -errno);
	return (errno ? -errno : -EIO);
}

/*
 * Use 'parent' if the parent is VHD, and 'parent_fd' if the parent is raw
 */
static int
vhd_util_coalesce_block(vhd_context_t *vhd, vhd_context_t *parent,
		int parent_fd, uint64_t block)
{
	int i, err;
	char *buf, *map;
	uint64_t sec, secs;

	buf = NULL;
	map = NULL;
	sec = block * vhd->spb;

	if (vhd->bat.bat[block] == DD_BLK_UNUSED)
		return 0;

	err = posix_memalign((void **)&buf, 4096, vhd->header.block_size);
	if (err)
		return -err;

	err = vhd_io_read(vhd, buf, sec, vhd->spb);
	if (err)
		goto done;

	if (vhd_has_batmap(vhd) && vhd_batmap_test(vhd, &vhd->batmap, block)) {
		if (parent->file)
			err = vhd_io_write(parent, buf, sec, vhd->spb);
		else
			err = __raw_io_write(parent_fd, buf, sec, vhd->spb);
		goto done;
	}

	err = vhd_read_bitmap(vhd, block, &map);
	if (err)
		goto done;

	for (i = 0; i < vhd->spb; i++) {
		if (!vhd_bitmap_test(vhd, map, i))
			continue;

		for (secs = 0; i + secs < vhd->spb; secs++)
			if (!vhd_bitmap_test(vhd, map, i + secs))
				break;

		if (parent->file)
			err = vhd_io_write(parent,
					   buf + vhd_sectors_to_bytes(i),
					   sec + i, secs);
		else
			err = __raw_io_write(parent_fd,
					     buf + vhd_sectors_to_bytes(i),
					     sec + i, secs);
		if (err)
			goto done;

		i += secs;
	}

	err = 0;

done:
	free(buf);
	free(map);
	return err;
}

int
vhd_util_coalesce(int argc, char **argv)
{
	int err, c;
	uint64_t i;
	char *name, *pname;
	vhd_context_t vhd, parent;
	int parent_fd = -1;

	name  = NULL;
	pname = NULL;
	parent.file = NULL;

	if (!argc || !argv)
		goto usage;

	optind = 0;
	while ((c = getopt(argc, argv, "n:h")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 'h':
		default:
			goto usage;
		}
	}

	if (!name || optind != argc)
		goto usage;

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY);
	if (err) {
		printf("error opening %s: %d\n", name, err);
		return err;
	}

	err = vhd_parent_locator_get(&vhd, &pname);
	if (err) {
		printf("error finding %s parent: %d\n", name, err);
		vhd_close(&vhd);
		return err;
	}

	if (vhd_parent_raw(&vhd)) {
		parent_fd = open(pname, O_RDWR | O_DIRECT | O_LARGEFILE, 0644);
		if (parent_fd == -1) {
			err = -errno;
			printf("failed to open parent %s: %d\n", pname, err);
			vhd_close(&vhd);
			return err;
		}
	} else {
		err = vhd_open(&parent, pname, VHD_OPEN_RDWR);
		if (err) {
			printf("error opening %s: %d\n", pname, err);
			free(pname);
			vhd_close(&vhd);
			return err;
		}
	}

	err = vhd_get_bat(&vhd);
	if (err)
		goto done;

	if (vhd_has_batmap(&vhd)) {
		err = vhd_get_batmap(&vhd);
		if (err)
			goto done;
	}

	for (i = 0; i < vhd.bat.entries; i++) {
		err = vhd_util_coalesce_block(&vhd, &parent, parent_fd, i);
		if (err)
			goto done;
	}

	err = 0;

 done:
	free(pname);
	vhd_close(&vhd);
	if (parent.file)
		vhd_close(&parent);
	else
		close(parent_fd);
	return err;

usage:
	printf("options: <-n name> [-h help]\n");
	return -EINVAL;
}
