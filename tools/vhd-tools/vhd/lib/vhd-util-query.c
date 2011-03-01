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

int
vhd_util_query(int argc, char **argv)
{
	char *name;
	vhd_context_t vhd;
	off_t currsize;
	int ret, err, c, size, physize, parent, fields, depth;

	name    = NULL;
	size    = 0;
	physize = 0;
	parent  = 0;
	fields  = 0;
	depth   = 0;

	if (!argc || !argv) {
		err = -EINVAL;
		goto usage;
	}

	optind = 0;
	while ((c = getopt(argc, argv, "n:vspfdh")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 'v':
			size = 1;
			break;
		case 's':
			physize = 1;
			break;
		case 'p':
			parent = 1;
			break;
		case 'f':
			fields = 1;
			break;
		case 'd':
			depth = 1;
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

	err = vhd_open(&vhd, name, VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED);
	if (err) {
		printf("error opening %s: %d\n", name, err);
		return err;
	}

	if (size)
		printf("%"PRIu64"\n", vhd.footer.curr_size >> 20);

	if (physize) {
		err = vhd_get_phys_size(&vhd, &currsize);
		if (err)
			printf("failed to get physical size: %d\n", err);
		else
			printf("%"PRIu64"\n", currsize);
	}

	if (parent) {
		ret = 0;

		if (vhd.footer.type != HD_TYPE_DIFF)
			printf("%s has no parent\n", name);
		else {
			char *pname;

			ret = vhd_parent_locator_get(&vhd, &pname);
			if (ret)
				printf("query failed\n");
			else {
				printf("%s\n", pname);
				free(pname);
			}
		}

		err = (err ? : ret);
	}

	if (fields) {
		int hidden;

		ret = vhd_hidden(&vhd, &hidden);
		if (ret)
			printf("error checking 'hidden' field: %d\n", ret);
		else
			printf("hidden: %d\n", hidden);

		err = (err ? : ret);
	}

	if (depth) {
		int length;

		ret = vhd_chain_depth(&vhd, &length);
		if (ret)
			printf("error checking chain depth: %d\n", ret);
		else
			printf("chain depth: %d\n", length);

		err = (err ? : ret);
	}
		
	vhd_close(&vhd);
	return err;

usage:
	printf("options: <-n name> [-v print virtual size (in MB)] "
	       "[-s print physical utilization (bytes)] [-p print parent] "
	       "[-f print fields] [-d print chain depth] [-h help]\n");
	return err;
}
