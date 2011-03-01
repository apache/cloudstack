/* 
 * Copyright (c) 2008, XenSource Inc.
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
#ifndef _LVM_UTIL_H_
#define _LVM_UTIL_H_

#include <inttypes.h>

#define MAX_NAME_SIZE            256

#define LVM_SEG_TYPE_LINEAR      1
#define LVM_SEG_TYPE_UNKNOWN     2

struct lv_segment {
	uint8_t                  type;
	char                     device[MAX_NAME_SIZE];
	uint64_t                 pe_start;
	uint64_t                 pe_size;
};

struct lv {
	char                     name[MAX_NAME_SIZE];
	uint64_t                 size;
	uint32_t                 segments;
	struct lv_segment        first_segment;
};

struct pv {
	char                     name[MAX_NAME_SIZE];
	uint64_t                 start;
};

struct vg {
	char                     name[MAX_NAME_SIZE];
	uint64_t                 extent_size;

	int                      pv_cnt;
	struct pv               *pvs;

	int                      lv_cnt;
	struct lv               *lvs;
};

int lvm_scan_vg(const char *vg_name, struct vg *vg);
void lvm_free_vg(struct vg *vg);

#endif
