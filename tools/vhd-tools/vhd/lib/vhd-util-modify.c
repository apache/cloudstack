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
 * Altering operations:
 *
 * 1. Change the parent pointer to another file.
 * 2. Change the size of the file containing the VHD image. This does NOT 
 * affect the VHD disk capacity, only the physical size of the file containing 
 * the VHD. Naturally, it is not possible to set the file size to be less than  
 * the what VHD utilizes.
 * The operation doesn't actually change the file size, but it writes the 
 * footer in the right location such that resizing the file (manually, as a 
 * separate step) will produce the correct results. If the new file size is 
 * greater than the current file size, the file must first be expanded and then 
 * altered with this operation. If the new size is smaller than the current 
 * size, the VHD must first be altered with this operation and then the file 
 * must be shrunk. Failing to resize the file will result in a corrupted VHD.
 */

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include "libvhd.h"

TEST_FAIL_EXTERN_VARS;

int
vhd_util_modify(int argc, char **argv)
{
	char *name;
	vhd_context_t vhd;
	int err, c, size, parent, parent_raw;
	off_t newsize = 0;
	char *newparent = NULL;

	name       = NULL;
	size       = 0;
	parent     = 0;
	parent_raw = 0;

	optind = 0;
	while ((c = getopt(argc, argv, "n:s:p:mh")) != -1) {
		switch (c) {
		case 'n':
			name = optarg;
			break;
		case 's':
			size = 1;
			errno = 0;
			newsize = strtoll(optarg, NULL, 10);
			if (errno) {
				fprintf(stderr, "Invalid size '%s'\n", optarg);
				goto usage;
			}
			break;
		case 'p':
			parent = 1;
			newparent = optarg;
			break;
		case 'm':
			parent_raw = 1;
			break;

		case 'h':
		default:
			goto usage;
		}
	}

	if (!name || optind != argc)
		goto usage;

	err = vhd_open(&vhd, name, VHD_OPEN_RDWR);
	if (err) {
		printf("error opening %s: %d\n", name, err);
		return err;
	}

	if (size) {
		err = vhd_set_phys_size(&vhd, newsize);
		if (err)
			printf("failed to set physical size to %"PRIu64":"
			       " %d\n", newsize, err);
	}

	if (parent) {
		TEST_FAIL_AT(FAIL_REPARENT_BEGIN);
		err = vhd_change_parent(&vhd, newparent, parent_raw);
		if (err) {
			printf("failed to set parent to '%s': %d\n",
					newparent, err);
			goto done;
		}
		TEST_FAIL_AT(FAIL_REPARENT_END);
	}

done:
	vhd_close(&vhd);
	return err;

usage:
	printf("*** Dangerous operations, use with care ***\n");
	printf("options: <-n name> [-p NEW_PARENT set parent [-m raw]] "
			"[-s NEW_SIZE set size] [-h help]\n");
	return -EINVAL;
}
