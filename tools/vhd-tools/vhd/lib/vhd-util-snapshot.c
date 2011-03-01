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
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

#include "libvhd.h"

static int
vhd_util_find_snapshot_target(const char *name, char **result, int *parent_raw)
{
	int i, err;
	char *target;
	vhd_context_t vhd;

	*parent_raw = 0;
	*result     = NULL;

	target = strdup(name);
	if (!target)
		return -ENOMEM;

	for (;;) {
		err = vhd_open(&vhd, target, VHD_OPEN_RDONLY);
		if (err)
			return err;

		if (vhd.footer.type != HD_TYPE_DIFF)
			goto out;

		err = vhd_get_bat(&vhd);
		if (err)
			goto out;

		for (i = 0; i < vhd.bat.entries; i++)
			if (vhd.bat.bat[i] != DD_BLK_UNUSED)
				goto out;

		free(target);
		err = vhd_parent_locator_get(&vhd, &target);
		if (err)
			goto out;

		if (vhd_parent_raw(&vhd)) {
			*parent_raw = 1;
			goto out;
		}

		vhd_close(&vhd);
	}

out:
	vhd_close(&vhd);
	if (err)
		free(target);
	else
		*result = target;

	return err;
}

static int
vhd_util_check_depth(const char *name, int *depth)
{
	int err;
	vhd_context_t vhd;

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY);
	if (err)
		return err;

	err = vhd_chain_depth(&vhd, depth);
	vhd_close(&vhd);

	return err;
}

int
vhd_util_snapshot(int argc, char **argv)
{
	vhd_flag_creat_t flags;
	int c, err, prt_raw, limit;
	char *name, *pname, *ppath, *backing;
	uint64_t size;
	vhd_context_t vhd;

	name    = NULL;
	pname   = NULL;
	ppath   = NULL;
	backing = NULL;
	size    = 0;
	flags   = 0;
	limit   = 0;

	if (!argc || !argv) {
		err = -EINVAL;
		goto usage;
	}

	optind = 0;
	while ((c = getopt(argc, argv, "n:p:l:mh")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 'p':
			pname = optarg;
			break;
		case 'l':
			limit = strtol(optarg, NULL, 10);
			break;
		case 'm':
			vhd_flag_set(flags, VHD_FLAG_CREAT_PARENT_RAW);
			break;
		case 'h':
			err = 0;
			goto usage;
		default:
			err = -EINVAL;
			goto usage;
		}
	}

	if (!name || !pname || optind != argc) {
		err = -EINVAL;
		goto usage;
	}

	ppath = realpath(pname, NULL);
	if (!ppath)
		return -errno;

	if (vhd_flag_test(flags, VHD_FLAG_CREAT_PARENT_RAW)) {
		backing = strdup(ppath);
		if (!backing) {
			err = -ENOMEM;
			goto out;
		}
	} else {
		err = vhd_util_find_snapshot_target(ppath, &backing, &prt_raw);
		if (err) {
			backing = NULL;
			goto out;
		}

		/* 
		 * if the sizes of the parent chain are non-uniform, we need to 
		 * pick the right size: that of the supplied parent
		 */
		if (strcmp(ppath, backing)) {
			err = vhd_open(&vhd, ppath, VHD_OPEN_RDONLY);
			if (err)
				goto out;
			size = vhd.footer.curr_size;
			vhd_close(&vhd);
		}

		if (prt_raw)
			vhd_flag_set(flags, VHD_FLAG_CREAT_PARENT_RAW);
	}

	if (limit && !vhd_flag_test(flags, VHD_FLAG_CREAT_PARENT_RAW)) {
		int depth;

		err = vhd_util_check_depth(backing, &depth);
		if (err)
			printf("error checking snapshot depth: %d\n", err);
		else if (depth + 1 > limit) {
			err = -ENOSPC;
			printf("snapshot depth exceeded: "
			       "current depth: %d, limit: %d\n", depth, limit);
		}

		if (err)
			goto out;
	}

	err = vhd_snapshot(name, size, backing, flags);

out:
	free(ppath);
	free(backing);

	return err;

usage:
	printf("options: <-n name> <-p parent name> [-l snapshot depth limit]"
	       " [-m parent_is_raw] [-h help]\n");
	return err;
}
