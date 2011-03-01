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
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>

#include "lvm-util.h"

#define _NAME "%255s"
static char line[1024];

static inline int
lvm_read_line(FILE *scan)
{
	memset(line, 0, sizeof(line));
	return (fscanf(scan, "%1023[^\n]", line) != 1);
}

static inline int
lvm_next_line(FILE *scan)
{
	return (fscanf(scan, "%1023[\n]", line) != 1);
}

static int
lvm_copy_name(char *dst, const char *src, size_t size)
{
	if (strnlen(src, size) == size)
		return -ENAMETOOLONG;

	strcpy(dst, src);
	return 0;
}

static int
lvm_parse_pv(struct vg *vg, const char *name, int pvs, uint64_t start)
{
	int i, err;
	struct pv *pv;

	pv = NULL;

	if (!vg->pvs) {
		vg->pvs = calloc(pvs, sizeof(struct pv));
		if (!vg->pvs)
			return -ENOMEM;
	}

	for (i = 0; i < pvs; i++) {
		pv = vg->pvs + i;

		if (!pv->name[0])
			break;

		if (!strcmp(pv->name, name))
			return -EEXIST;
	}

	if (!pv)
		return -ENOENT;

	if (i == pvs)
		return -ENOMEM;

	err = lvm_copy_name(pv->name, name, sizeof(pv->name) - 1);
	if (err)
		return err;

	pv->start = start;
	return 0;
}

static int
lvm_open_vg(const char *vgname, struct vg *vg)
{
	FILE *scan;
	int i, err, pvs, lvs;
	char *cmd, pvname[256];
	uint64_t size, pv_start;

	memset(vg, 0, sizeof(*vg));

	err = asprintf(&cmd, "/usr/sbin/vgs %s --noheadings --nosuffix --units=b "
		       "--options=vg_name,vg_extent_size,lv_count,pv_count,"
		       "pv_name,pe_start --unbuffered 2> /dev/null", vgname);
	if (err == -1)
		return -ENOMEM;

	errno = 0;
	scan  = popen(cmd, "r");
	if (!scan) {
		err = (errno ? -errno : ENOMEM);
		goto out;
	}

	for (;;) {
		if (lvm_read_line(scan))
			break;

		err = -EINVAL;
                if (sscanf(line, _NAME" %"SCNu64" %d %d "_NAME" %"SCNu64,
			   vg->name, &size, &lvs, &pvs, pvname, &pv_start) != 6)
			goto out;

		if (strcmp(vg->name, vgname))
			goto out;

		err = lvm_parse_pv(vg, pvname, pvs, pv_start);
		if (err)
			goto out;

		if (lvm_next_line(scan))
			break;
	}

	err = -EINVAL;
	if (strcmp(vg->name, vgname))
		goto out;

	for (i = 0; i < pvs; i++)
		if (!vg->pvs[i].name[0])
			goto out;

	err = -ENOMEM;
	vg->lvs = calloc(lvs, sizeof(struct lv));
	if (!vg->lvs)
		goto out;

	err             = 0;
	vg->lv_cnt      = lvs;
	vg->pv_cnt      = pvs;
	vg->extent_size = size;

out:
	if (scan)
		pclose(scan);
	if (err)
		lvm_free_vg(vg);
	free(cmd);
	return err;
}

static int
lvm_parse_lv_devices(struct vg *vg, struct lv_segment *seg, char *devices)
{
	int i;
	uint64_t start, pe_start;

	for (i = 0; i < strlen(devices); i++)
		if (strchr(",()", devices[i]))
			devices[i] = ' ';

        if (sscanf(devices, _NAME" %"SCNu64, seg->device, &start) != 2)
		return -EINVAL;

	pe_start = -1;
	for (i = 0; i < vg->pv_cnt; i++)
		if (!strcmp(vg->pvs[i].name, seg->device)) {
			pe_start = vg->pvs[i].start;
			break;
		}

	if (pe_start == -1)
		return -EINVAL;

	seg->pe_start = (start * vg->extent_size) + pe_start;
	return 0;
}

static int
lvm_scan_lvs(struct vg *vg)
{
	char *cmd;
	FILE *scan;
	int i, err;

	err = asprintf(&cmd, "/usr/sbin/lvs %s --noheadings --nosuffix --units=b "
		       "--options=lv_name,lv_size,segtype,seg_count,seg_start,"
		       "seg_size,devices --unbuffered 2> /dev/null", vg->name);
	if (err == -1)
		return -ENOMEM;

	errno = 0;
	scan  = popen(cmd, "r");
	if (!scan) {
		err = (errno ? -errno : -ENOMEM);
		goto out;
	}

	for (i = 0;;) {
		int segs;
		struct lv *lv;
		struct lv_segment seg;
		uint64_t size, seg_start;
		char type[32], name[256], dev[256], devices[1024];

		if (i >= vg->lv_cnt)
			break;

		if (lvm_read_line(scan)) {
			vg->lv_cnt = i;
			break;
		}

		err = -EINVAL;
		lv  = vg->lvs + i;

                if (sscanf(line, _NAME" %"SCNu64" %31s %u %"SCNu64" %"SCNu64" %1023s",
			   name, &size, type, &segs, &seg_start,
			   &seg.pe_size, devices) != 7)
			goto out;

		if (seg_start)
			goto next;

		if (!strcmp(type, "linear"))
			seg.type = LVM_SEG_TYPE_LINEAR;
		else
			seg.type = LVM_SEG_TYPE_UNKNOWN;

		if (lvm_parse_lv_devices(vg, &seg, devices))
			goto out;

		i++;
		lv->size          = size;
		lv->segments      = segs;
		lv->first_segment = seg;

		err = lvm_copy_name(lv->name, name, sizeof(lv->name) - 1);
		if (err)
			goto out;
		err = -EINVAL;

	next:
		if (lvm_next_line(scan))
			goto out;
	}

	err = 0;

out:
	if (scan)
		pclose(scan);
	free(cmd);
	return err;
}

void
lvm_free_vg(struct vg *vg)
{
	free(vg->lvs);
	free(vg->pvs);
	memset(vg, 0, sizeof(*vg));
}

int
lvm_scan_vg(const char *vg_name, struct vg *vg)
{
	int err;

	memset(vg, 0, sizeof(*vg));

	err = lvm_open_vg(vg_name, vg);
	if (err)
		return err;

	err = lvm_scan_lvs(vg);
	if (err) {
		lvm_free_vg(vg);
		return err;
	}

	return 0;
}

#ifdef LVM_UTIL
static int
usage(void)
{
	printf("usage: lvm-util <vgname>\n");
	exit(EINVAL);
}

int
main(int argc, char **argv)
{
	int i, err;
	struct vg vg;
	struct pv *pv;
	struct lv *lv;
	struct lv_segment *seg;

	if (argc != 2)
		usage();

	err = lvm_scan_vg(argv[1], &vg);
	if (err) {
		printf("scan failed: %d\n", err);
		return (err >= 0 ? err : -err);
	}

	
        printf("vg %s: extent_size: %"PRIu64", pvs: %d, lvs: %d\n",
	       vg.name, vg.extent_size, vg.pv_cnt, vg.lv_cnt);

	for (i = 0; i < vg.pv_cnt; i++) {
		pv = vg.pvs + i;
                printf("pv %s: start %"PRIu64"\n", pv->name, pv->start);
	}

	for (i = 0; i < vg.lv_cnt; i++) {
		lv  = vg.lvs + i;
		seg = &lv->first_segment;                
                printf("lv %s: size: %"PRIu64", segments: %u, type: %u, "
                       "dev: %s, pe_start: %"PRIu64", pe_size: %"PRIu64"\n",
		       lv->name, lv->size, lv->segments, seg->type,
		       seg->device, seg->pe_start, seg->pe_size);
	}

	lvm_free_vg(&vg);
	return 0;
}
#endif
