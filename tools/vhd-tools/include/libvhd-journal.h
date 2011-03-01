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
#ifndef _VHD_JOURNAL_H_
#define _VHD_JOURNAL_H_

#include <inttypes.h>

#include "libvhd.h"

#define VHD_JOURNAL_METADATA       0x01
#define VHD_JOURNAL_DATA           0x02

#define VHD_JOURNAL_HEADER_COOKIE  "vjournal"
#define VHD_JOURNAL_ENTRY_COOKIE   0xaaaa12344321aaaa

typedef struct vhd_journal_header {
	char                       cookie[8];
	blk_uuid_t                 uuid;
	uint64_t                   vhd_footer_offset;
	uint32_t                   journal_data_entries;
	uint32_t                   journal_metadata_entries;
	uint64_t                   journal_data_offset;
	uint64_t                   journal_metadata_offset;
	uint64_t                   journal_eof;
	char                       pad[448];
} vhd_journal_header_t;

typedef struct vhd_journal {
	char                      *jname;
	int                        jfd;
	int                        is_block; /* is jfd a block device */
	vhd_journal_header_t       header;
	vhd_context_t              vhd;
} vhd_journal_t;

int vhd_journal_create(vhd_journal_t *, const char *file, const char *jfile);
int vhd_journal_open(vhd_journal_t *, const char *file, const char *jfile);
int vhd_journal_add_block(vhd_journal_t *, uint32_t block, char mode);
int vhd_journal_commit(vhd_journal_t *);
int vhd_journal_revert(vhd_journal_t *);
int vhd_journal_close(vhd_journal_t *);
int vhd_journal_remove(vhd_journal_t *);

#endif
