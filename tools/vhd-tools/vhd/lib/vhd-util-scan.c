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

#include <glob.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <fnmatch.h>
#include <libgen.h>	/* for basename() */
#include <sys/stat.h>

#include "list.h"
#include "libvhd.h"
#include "lvm-util.h"

#define VHD_SCAN_FAST        0x01
#define VHD_SCAN_PRETTY      0x02
#define VHD_SCAN_VOLUME      0x04
#define VHD_SCAN_NOFAIL      0x08
#define VHD_SCAN_VERBOSE     0x10
#define VHD_SCAN_PARENTS     0x20

#define VHD_TYPE_RAW_FILE    0x01
#define VHD_TYPE_VHD_FILE    0x02
#define VHD_TYPE_RAW_VOLUME  0x04
#define VHD_TYPE_VHD_VOLUME  0x08

static inline int
target_volume(uint8_t type)
{
	return (type == VHD_TYPE_RAW_VOLUME || type == VHD_TYPE_VHD_VOLUME);
}

static inline int
target_vhd(uint8_t type)
{
	return (type == VHD_TYPE_VHD_FILE || type == VHD_TYPE_VHD_VOLUME);
}

struct target {
	char                 name[VHD_MAX_NAME_LEN];
	char                 device[VHD_MAX_NAME_LEN];
	uint64_t             size;
	uint64_t             start;
	uint64_t             end;
	uint8_t              type;
};

struct iterator {
	int                  cur;
	int                  cur_size;
	int                  max_size;
	struct target       *targets;
};

struct vhd_image {
	char                *name;
	char                *parent;
	uint64_t             capacity;
	off_t                size;
	uint8_t              hidden;
	int                  error;
	char                *message;

	struct target       *target;

	struct list_head     sibling;
	struct list_head     children;
	struct vhd_image    *parent_image;
};

struct vhd_scan {
	int                  cur;
	int                  size;

	int                  lists_cur;
	int                  lists_size;

	struct vhd_image   **images;
	struct vhd_image   **lists;
};

static int flags;
static struct vg vg;
static struct vhd_scan scan;

static int
vhd_util_scan_pretty_allocate_list(int cnt)
{
	int i;
	struct vhd_image *list;

	memset(&scan, 0, sizeof(scan));

	scan.lists_cur  = 1;
	scan.lists_size = 10;

	scan.lists = calloc(scan.lists_size, sizeof(struct vhd_image *));
	if (!scan.lists)
		goto fail;

	scan.lists[0] = calloc(cnt, sizeof(struct vhd_image));
	if (!scan.lists[0])
		goto fail;

	scan.images = calloc(cnt, sizeof(struct vhd_image *));
	if (!scan.images)
		goto fail;

	for (i = 0; i < cnt; i++)
		scan.images[i] = scan.lists[0] + i;

	scan.cur  = 0;
	scan.size = cnt;

	return 0;

fail:
	if (scan.lists) {
		free(scan.lists[0]);
		free(scan.lists);
	}

	free(scan.images);
	memset(&scan, 0, sizeof(scan));
	return -ENOMEM;
}

static void
vhd_util_scan_pretty_free_list(void)
{
	int i;

	if (scan.lists) {
		for (i = 0; i < scan.lists_cur; i++)
			free(scan.lists[i]);
		free(scan.lists);
	}

	free(scan.images);
	memset(&scan, 0, sizeof(scan));
}

static int
vhd_util_scan_pretty_add_image(struct vhd_image *image)
{
	int i;
	struct vhd_image *img;

	for (i = 0; i < scan.cur; i++) {
		img = scan.images[i];
		if (!strcmp(img->name, image->name))
			return 0;
	}

	if (scan.cur >= scan.size) {
		struct vhd_image *new, **list;

		if (scan.lists_cur >= scan.lists_size) {
			list = realloc(scan.lists, scan.lists_size * 2 *
				       sizeof(struct vhd_image *));
			if (!list)
				return -ENOMEM;

			scan.lists_size *= 2;
			scan.lists       = list;
		}

		new = calloc(scan.size, sizeof(struct vhd_image));
		if (!new)
			return -ENOMEM;

		scan.lists[scan.lists_cur++] = new;
		scan.size *= 2;

		list = realloc(scan.images, scan.size *
			       sizeof(struct vhd_image *));
		if (!list)
			return -ENOMEM;

		scan.images = list;
		for (i = 0; i + scan.cur < scan.size; i++)
			scan.images[i + scan.cur] = new + i;
	}

	img = scan.images[scan.cur];
	INIT_LIST_HEAD(&img->sibling);
	INIT_LIST_HEAD(&img->children);

	img->capacity = image->capacity;
	img->size     = image->size;
	img->hidden   = image->hidden;
	img->error    = image->error;
	img->message  = image->message;

	img->name = strdup(image->name);
	if (!img->name)
		goto fail;

	if (image->parent) {
		img->parent = strdup(image->parent);
		if (!img->parent)
			goto fail;
	}

	scan.cur++;
	return 0;

fail:
	free(img->name);
	free(img->parent);
	memset(img, 0, sizeof(*img));
	return -ENOMEM;
}

static int
vhd_util_scan_pretty_image_compare(const void *lhs, const void *rhs)
{
	struct vhd_image *l, *r;

	l = *(struct vhd_image **)lhs;
	r = *(struct vhd_image **)rhs;

	return strcmp(l->name, r->name);
}

static void
vhd_util_scan_print_image_indent(struct vhd_image *image, int tab)
{
	char *pad, *name, *pmsg, *parent;

	pad    = (tab ? " " : "");
	name   = image->name;
	parent = (image->parent ? : "none");

	if ((flags & VHD_SCAN_PRETTY) && image->parent && !image->parent_image)
		pmsg = " (not found in scan)";
	else
		pmsg = "";

	if (!(flags & VHD_SCAN_VERBOSE)) {
		name = basename(image->name);
		if (image->parent)
			parent = basename(image->parent);
	}

	if (image->error)
		printf("%*svhd=%s scan-error=%d error-message='%s'\n",
		       tab, pad, image->name, image->error, image->message);
	else
		printf("%*svhd=%s capacity=%"PRIu64" size=%"PRIu64" hidden=%u "
		       "parent=%s%s\n", tab, pad, name, image->capacity,
		       image->size, image->hidden, parent, pmsg);
}

static void
vhd_util_scan_pretty_print_tree(struct vhd_image *image, int depth)
{
	struct vhd_image *img, *tmp;

	vhd_util_scan_print_image_indent(image, depth * 3);

	list_for_each_entry_safe(img, tmp, &image->children, sibling)
		if (!img->hidden)
			vhd_util_scan_pretty_print_tree(img, depth + 1);

	list_for_each_entry_safe(img, tmp, &image->children, sibling)
		if (img->hidden)
			vhd_util_scan_pretty_print_tree(img, depth + 1);

	free(image->name);
	free(image->parent);

	image->name   = NULL;
	image->parent = NULL;
}

static void
vhd_util_scan_pretty_print_images(void)
{
	int i;
	struct vhd_image *image, **parentp, *parent, *keyp, key;

	qsort(scan.images, scan.cur, sizeof(scan.images[0]),
	      vhd_util_scan_pretty_image_compare);

	for (i = 0; i < scan.cur; i++) {
		image = scan.images[i];

		if (!image->parent) {
			image->parent_image = NULL;
			continue;
		}

		memset(&key, 0, sizeof(key));
		key.name = image->parent;
		keyp     = &key;

		parentp  = bsearch(&keyp, scan.images, scan.cur,
				   sizeof(scan.images[0]),
				   vhd_util_scan_pretty_image_compare);
		if (!parentp) {
			image->parent_image = NULL;
			continue;
		}

		parent = *parentp;
		image->parent_image = parent;
		list_add_tail(&image->sibling, &parent->children);
	}

	for (i = 0; i < scan.cur; i++) {
		image = scan.images[i];

		if (image->parent_image || !image->hidden)
			continue;

		vhd_util_scan_pretty_print_tree(image, 0);
	}

	for (i = 0; i < scan.cur; i++) {
		image = scan.images[i];

		if (!image->name || image->parent_image)
			continue;

		vhd_util_scan_pretty_print_tree(image, 0);
	}

	for (i = 0; i < scan.cur; i++) {
		image = scan.images[i];

		if (!image->name)
			continue;

		vhd_util_scan_pretty_print_tree(image, 0);
	}
}

static void
vhd_util_scan_print_image(struct vhd_image *image)
{
	int err;

	if (!image->error && (flags & VHD_SCAN_PRETTY)) {
		err = vhd_util_scan_pretty_add_image(image);
		if (!err)
			return;

		if (!image->error) {
			image->error   = err;
			image->message = "allocating memory";
		}
	}

	vhd_util_scan_print_image_indent(image, 0);
}

static int
vhd_util_scan_error(const char *file, int err)
{
	struct vhd_image image;

	memset(&image, 0, sizeof(image));
	image.name    = (char *)file;
	image.error   = err;
	image.message = "failure scanning target";

	vhd_util_scan_print_image(&image);

	/*
	if (flags & VHD_SCAN_NOFAIL)
		return 0;
	*/

	return err;
}

static vhd_parent_locator_t *
vhd_util_scan_get_parent_locator(vhd_context_t *vhd)
{
	int i;
	vhd_parent_locator_t *loc;

	loc = NULL;

	for (i = 0; i < 8; i++) {
		if (vhd->header.loc[i].code == PLAT_CODE_MACX) {
			loc = vhd->header.loc + i;
			break;
		}

		if (vhd->header.loc[i].code == PLAT_CODE_W2RU)
			loc = vhd->header.loc + i;

		if (!loc && vhd->header.loc[i].code != PLAT_CODE_NONE)
			loc = vhd->header.loc + i;
	}

	return loc;
}

static inline int
copy_name(char *dst, const char *src)
{
	if (snprintf(dst, VHD_MAX_NAME_LEN, "%s", src) < VHD_MAX_NAME_LEN)
		return 0;

	return -ENAMETOOLONG;
}

/*
 * LVHD stores realpath(parent) in parent locators, so
 * /dev/<vol-group>/<lv-name> becomes /dev/mapper/<vol--group>-<lv--name>
 */
static int
vhd_util_scan_extract_volume_name(char *dst, const char *src)
{
	int err;
	char copy[VHD_MAX_NAME_LEN], *name, *s, *c;

	name = strrchr(src, '/');
	if (!name)
		name = (char *)src;

	/* convert single dashes to slashes, double dashes to single dashes */
	for (c = copy, s = name; *s != '\0'; s++, c++) {
		if (*s == '-') {
			if (s[1] != '-')
				*c = '/';
			else {
				s++;
				*c = '-';
			}
		} else
			*c = *s;
	}

	*c = '\0';
	c = strrchr(copy, '/');
	if (c == name) {
		/* unrecognized format */
		strcpy(dst, src);
		return -EINVAL;
	}

	strcpy(dst, ++c);
	return 0;
}

static int
vhd_util_scan_get_volume_parent(vhd_context_t *vhd, struct vhd_image *image)
{
	int err;
	char name[VHD_MAX_NAME_LEN];
	vhd_parent_locator_t *loc, copy;

	if (flags & VHD_SCAN_FAST) {
		err = vhd_header_decode_parent(vhd,
					       &vhd->header, &image->parent);
		if (!err)
			goto found;
	}

	loc = vhd_util_scan_get_parent_locator(vhd);
	if (!loc)
		return -EINVAL;

	copy = *loc;
	copy.data_offset += image->target->start;
	err = vhd_parent_locator_read(vhd, &copy, &image->parent);
	if (err)
		return err;

found:
	err = vhd_util_scan_extract_volume_name(name, image->parent);
	if (!err)
		return copy_name(image->parent, name);

	return 0;
}

static int
vhd_util_scan_get_parent(vhd_context_t *vhd, struct vhd_image *image)
{
	int i, err;
	vhd_parent_locator_t *loc;

	if (!target_vhd(image->target->type)) {
		image->parent = NULL;
		return 0;
	}

	loc = NULL;

	if (target_volume(image->target->type))
		return vhd_util_scan_get_volume_parent(vhd, image);

	if (flags & VHD_SCAN_FAST) {
		err = vhd_header_decode_parent(vhd,
					       &vhd->header, &image->parent);
		if (!err)
			return 0;
	} else {
		/*
		 * vhd_parent_locator_get checks for the existence of the 
		 * parent file. if this call succeeds, all is well; if not,
		 * we'll try to return whatever string we have before failing
		 * outright.
		 */
		err = vhd_parent_locator_get(vhd, &image->parent);
		if (!err)
			return 0;
	}

	loc = vhd_util_scan_get_parent_locator(vhd);
	if (!loc)
		return -EINVAL;

	return vhd_parent_locator_read(vhd, loc, &image->parent);
}

static int
vhd_util_scan_get_hidden(vhd_context_t *vhd, struct vhd_image *image)
{
	int err, hidden;

	err    = 0;
	hidden = 0;

	if (target_vhd(image->target->type))
		err = vhd_hidden(vhd, &hidden);
	else
		hidden = 1;

	if (err)
		return err;

	image->hidden = hidden;
	return 0;
}

static int
vhd_util_scan_get_size(vhd_context_t *vhd, struct vhd_image *image)
{
	image->size = image->target->size;

	if (target_vhd(image->target->type))
		image->capacity = vhd->footer.curr_size;
	else
		image->capacity = image->size;

	return 0;
}

static int
vhd_util_scan_open_file(vhd_context_t *vhd, struct vhd_image *image)
{
	int err, vhd_flags;

	if (!target_vhd(image->target->type))
		return 0;

	vhd_flags = VHD_OPEN_RDONLY | VHD_OPEN_IGNORE_DISABLED;
	if (flags & VHD_SCAN_FAST)
		vhd_flags |= VHD_OPEN_FAST;

	err = vhd_open(vhd, image->name, vhd_flags);
	if (err) {
		vhd->file      = NULL;
		image->message = "opening file";
		image->error   = err;
		return image->error;
	}

	return 0;
}

static int
vhd_util_scan_read_volume_headers(vhd_context_t *vhd, struct vhd_image *image)
{
	int err;
	char *buf;
	size_t size;
	struct target *target;

	buf    = NULL;
	target = image->target;
	size   = sizeof(vhd_footer_t) + sizeof(vhd_header_t);

	err = posix_memalign((void **)&buf, VHD_SECTOR_SIZE, size);
	if (err) {
		buf            = NULL;
		image->message = "allocating image";
		image->error   = -err;
		goto out;
	}

	err = vhd_seek(vhd, target->start, SEEK_SET);
	if (err) {
		image->message = "seeking to headers";
		image->error   = err;
		goto out;
	}

	err = vhd_read(vhd, buf, size);
	if (err) {
		image->message = "reading headers";
		image->error   = err;
		goto out;
	}

	memcpy(&vhd->footer, buf, sizeof(vhd_footer_t));
	vhd_footer_in(&vhd->footer);
	err = vhd_validate_footer(&vhd->footer);
	if (err) {
		image->message = "invalid footer";
		image->error   = err;
		goto out;
	}

	/* lvhd vhds should always be dynamic */
	if (vhd_type_dynamic(vhd)) {
		if (vhd->footer.data_offset != sizeof(vhd_footer_t))
			err = vhd_read_header_at(vhd, &vhd->header,
						 vhd->footer.data_offset +
						 target->start);
		else {
			memcpy(&vhd->header,
			       buf + sizeof(vhd_footer_t),
			       sizeof(vhd_header_t));
			vhd_header_in(&vhd->header);
			err = vhd_validate_header(&vhd->header);
		}

		if (err) {
			image->message = "reading header";
			image->error   = err;
			goto out;
		}

		vhd->spb = vhd->header.block_size >> VHD_SECTOR_SHIFT;
		vhd->bm_secs = secs_round_up_no_zero(vhd->spb >> 3);
	}

out:
	free(buf);
	return image->error;
}

static int
vhd_util_scan_open_volume(vhd_context_t *vhd, struct vhd_image *image)
{
	int err;
	struct target *target;

	target = image->target;
	memset(vhd, 0, sizeof(*vhd));
	vhd->oflags = VHD_OPEN_RDONLY | VHD_OPEN_FAST;

	if (target->end - target->start < 4096) {
		image->message = "device too small";
		image->error   = -EINVAL;
		return image->error;
	}

	vhd->file = strdup(image->name);
	if (!vhd->file) {
		image->message = "allocating device";
		image->error   = -ENOMEM;
		return image->error;
	}

	vhd->fd = open(target->device, O_RDONLY | O_DIRECT | O_LARGEFILE);
	if (vhd->fd == -1) {
		free(vhd->file);
		vhd->file = NULL;

		image->message = "opening device";
		image->error   = -errno;
		return image->error;
	}

	if (target_vhd(target->type))
		return vhd_util_scan_read_volume_headers(vhd, image);

	return 0;
}

static int
vhd_util_scan_open(vhd_context_t *vhd, struct vhd_image *image)
{
	struct target *target;

	target = image->target;

	if (target_volume(image->target->type) || !(flags & VHD_SCAN_PRETTY))
		image->name = target->name;
	else {
		image->name = realpath(target->name, NULL);
		if (!image->name) {
			image->name    = target->name;
			image->message = "resolving name";
			image->error   = -errno;
			return image->error;
		}
	}

	if (target_volume(target->type))
		return vhd_util_scan_open_volume(vhd, image);
	else
		return vhd_util_scan_open_file(vhd, image);
}

static int
vhd_util_scan_init_file_target(struct target *target,
			       const char *file, uint8_t type)
{
	int err;
	struct stat stats;

	err = stat(file, &stats);
	if (err == -1)
		return -errno;

	err = copy_name(target->name, file);
	if (err)
		return err;

	err = copy_name(target->device, file);
	if (err)
		return err;

	target->type  = type;
	target->start = 0;
	target->size  = stats.st_size;
	target->end   = stats.st_size;

	return 0;
}

static int
vhd_util_scan_init_volume_target(struct target *target,
				 struct lv *lv, uint8_t type)
{
	int err;

	if (lv->first_segment.type != LVM_SEG_TYPE_LINEAR)
		return -ENOSYS;

	err = copy_name(target->name, lv->name);
	if (err)
		return err;

	err = copy_name(target->device, lv->first_segment.device);
	if (err)
		return err;

	target->type  = type;
	target->size  = lv->size;
	target->start = lv->first_segment.pe_start;
	target->end   = target->start + lv->first_segment.pe_size;

	return 0;
}

static int
iterator_init(struct iterator *itr, int cnt, struct target *targets)
{
	memset(itr, 0, sizeof(*itr));

	itr->targets = malloc(sizeof(struct target) * cnt);
	if (!itr->targets)
		return -ENOMEM;

	memcpy(itr->targets, targets, sizeof(struct target) * cnt);

	itr->cur      = 0;
	itr->cur_size = cnt;
	itr->max_size = cnt;

	return 0;
}

static struct target *
iterator_next(struct iterator *itr)
{
	if (itr->cur == itr->cur_size)
		return NULL;

	return itr->targets + itr->cur++;
}

static int
iterator_add_file(struct iterator *itr,
		  struct target *target, const char *parent, uint8_t type)
{
	int i;
	struct target *t;
	char *lname, *rname;

	for (i = 0; i < itr->cur_size; i++) {
		t = itr->targets + i;
		lname = basename((char *)t->name);
		rname = basename((char *)parent);

		if (!strcmp(lname, rname))
			return -EEXIST;
	}

	return vhd_util_scan_init_file_target(target, parent, type);
}

static int
iterator_add_volume(struct iterator *itr,
		    struct target *target, const char *parent, uint8_t type)
{
	int i, err;
	struct lv *lv;

	lv  = NULL;
	err = -ENOENT;

	for (i = 0; i < itr->cur_size; i++)
		if (!strcmp(parent, itr->targets[i].name))
			return -EEXIST;

	for (i = 0; i < vg.lv_cnt; i++) {
		err = fnmatch(parent, vg.lvs[i].name, FNM_PATHNAME);
		if (err != FNM_NOMATCH) {
			lv = vg.lvs + i;
			break;
		}
	}

	if (err && err != FNM_PATHNAME)
		return err;

	if (!lv)
		return -ENOENT;

	return vhd_util_scan_init_volume_target(target, lv, type);
}

static int
iterator_add(struct iterator *itr, const char *parent, uint8_t type)
{
	int err;
	struct target *target;

	if (itr->cur_size == itr->max_size) {
		struct target *new;

		new = realloc(itr->targets,
			      sizeof(struct target) *
			      itr->max_size * 2);
		if (!new)
			return -ENOMEM;

		itr->max_size *= 2;
		itr->targets   = new;
	}

	target = itr->targets + itr->cur_size;

	if (target_volume(type))
		err = iterator_add_volume(itr, target, parent, type);
	else
		err = iterator_add_file(itr, target, parent, type);

	if (err)
		memset(target, 0, sizeof(*target));
	else
		itr->cur_size++;

	return (err == -EEXIST ? 0 : err);
}

static void
iterator_free(struct iterator *itr)
{
	free(itr->targets);
	memset(itr, 0, sizeof(*itr));
}

static void
vhd_util_scan_add_parent(struct iterator *itr,
			 vhd_context_t *vhd, struct vhd_image *image)
{
	int err;
	uint8_t type;

	if (vhd_parent_raw(vhd))
		type = target_volume(image->target->type) ? 
			VHD_TYPE_RAW_VOLUME : VHD_TYPE_RAW_FILE;
	else
		type = target_volume(image->target->type) ? 
			VHD_TYPE_VHD_VOLUME : VHD_TYPE_VHD_FILE;

	err = iterator_add(itr, image->parent, type);
	if (err)
		vhd_util_scan_error(image->parent, err);
}

static int
vhd_util_scan_targets(int cnt, struct target *targets)
{
	int ret, err;
	vhd_context_t vhd;
	struct iterator itr;
	struct target *target;
	struct vhd_image image;

	ret = 0;
	err = 0;

	err = iterator_init(&itr, cnt, targets);
	if (err)
		return err;

	while ((target = iterator_next(&itr))) {
		memset(&vhd, 0, sizeof(vhd));
		memset(&image, 0, sizeof(image));

		image.target = target;

		err = vhd_util_scan_open(&vhd, &image);
		if (err) {
			ret = -EAGAIN;
			goto end;
		}

		err = vhd_util_scan_get_size(&vhd, &image);
		if (err) {
			ret           = -EAGAIN;
			image.message = "getting physical size";
			image.error   = err;
			goto end;
		}

		err = vhd_util_scan_get_hidden(&vhd, &image);
		if (err) {
			ret           = -EAGAIN;
			image.message = "checking 'hidden' field";
			image.error   = err;
			goto end;
		}

		if (vhd.footer.type == HD_TYPE_DIFF) {
			err = vhd_util_scan_get_parent(&vhd, &image);
			if (err) {
				ret           = -EAGAIN;
				image.message = "getting parent";
				image.error   = err;
				goto end;
			}
		}

	end:
		vhd_util_scan_print_image(&image);

		if (flags & VHD_SCAN_PARENTS && image.parent)
			vhd_util_scan_add_parent(&itr, &vhd, &image);

		if (vhd.file)
			vhd_close(&vhd);
		if (image.name != target->name)
			free(image.name);
		free(image.parent);

		if (err && !(flags & VHD_SCAN_NOFAIL))
			break;
	}

	iterator_free(&itr);

	if (flags & VHD_SCAN_NOFAIL)
		return ret;

	return err;
}

static int
vhd_util_scan_targets_pretty(int cnt, struct target *targets)
{
	int err;

	err = vhd_util_scan_pretty_allocate_list(cnt);
	if (err) {
		printf("scan failed: no memory\n");
		return -ENOMEM;
	}

	err = vhd_util_scan_targets(cnt, targets);

	vhd_util_scan_pretty_print_images();
	vhd_util_scan_pretty_free_list();

	return ((flags & VHD_SCAN_NOFAIL) ? 0 : err);
}

static int
vhd_util_scan_find_file_targets(int cnt, char **names,
				const char *filter,
				struct target **_targets, int *_total)
{
	glob_t g;
	struct target *targets;
	int i, globs, err, total;

	total     = cnt;
	globs     = 0;
	*_total   = 0;
	*_targets = NULL;
	
	memset(&g, 0, sizeof(g));

	if (filter) {
		int gflags = ((flags & VHD_SCAN_FAST) ? GLOB_NOSORT : 0);

		errno = 0;
		err   = glob(filter, gflags, vhd_util_scan_error, &g);

		switch (err) {
		case GLOB_NOSPACE:
			err = -ENOMEM;
			break;
		case GLOB_ABORTED:
			err = -EIO;
			break;
		case GLOB_NOMATCH:
			err = -errno;
			break;
		}

		if (err) {
			vhd_util_scan_error(filter, err);
			return err;
		}

		globs  = g.gl_pathc;
		total += globs;
	}

	targets = calloc(total, sizeof(struct target));
	if (!targets) {
		err = -ENOMEM;
		goto out;
	}

	for (i = 0; i < g.gl_pathc; i++) {
		err = vhd_util_scan_init_file_target(targets + i,
						     g.gl_pathv[i],
						     VHD_TYPE_VHD_FILE);
		if (err) {
			vhd_util_scan_error(g.gl_pathv[i], err);
			if (!(flags & VHD_SCAN_NOFAIL))
				goto out;
		}
	}

	for (i = 0; i + globs < total; i++) {
		err = vhd_util_scan_init_file_target(targets + i + globs,
						     names[i],
						     VHD_TYPE_VHD_FILE);
		if (err) {
			vhd_util_scan_error(names[i], err);
			if (!(flags & VHD_SCAN_NOFAIL))
				goto out;
		}
	}

	err       = 0;
	*_total   = total;
	*_targets = targets;

out:
	if (err)
		free(targets);
	if (filter)
		globfree(&g);

	return err;
}

static inline void
swap_volume(struct lv *lvs, int dst, int src)
{
	struct lv copy, *ldst, *lsrc;

	if (dst == src)
		return;

	lsrc = lvs + src;
	ldst = lvs + dst;

	memcpy(&copy, ldst, sizeof(copy));
	memcpy(ldst, lsrc, sizeof(*ldst));
	memcpy(lsrc, &copy, sizeof(copy));
}

static int
vhd_util_scan_sort_volumes(struct lv *lvs, int cnt,
			   const char *filter, int *_matches)
{
	struct lv *lv;
	int i, err, matches;

	matches   = 0;
	*_matches = 0;

	if (!filter)
		return 0;

	for (i = 0; i < cnt; i++) {
		lv  = lvs + i;

		err = fnmatch(filter, lv->name, FNM_PATHNAME);
		if (err) {
			if (err != FNM_NOMATCH) {
				vhd_util_scan_error(lv->name, err);
				if (!(flags & VHD_SCAN_NOFAIL))
					return err;
			}

			continue;
		}

		swap_volume(lvs, matches++, i);
	}

	*_matches = matches;
	return 0;
}

static int
vhd_util_scan_find_volume_targets(int cnt, char **names,
				  const char *volume, const char *filter,
				  struct target **_targets, int *_total)
{
	struct target *targets;
	int i, err, total, matches;

	*_total   = 0;
	*_targets = NULL;
	targets   = NULL;

	err = lvm_scan_vg(volume, &vg);
	if (err)
		return err;

	err = vhd_util_scan_sort_volumes(vg.lvs, vg.lv_cnt,
					 filter, &matches);
	if (err)
		goto out;

	total = matches;
	for (i = 0; i < cnt; i++) {
		err = vhd_util_scan_sort_volumes(vg.lvs + total,
						 vg.lv_cnt - total,
						 names[i], &matches);
		if (err)
			goto out;

		total += matches;
	}

	targets = calloc(total, sizeof(struct target));
	if (!targets) {
		err = -ENOMEM;
		goto out;
	}

	for (i = 0; i < total; i++) {
		err = vhd_util_scan_init_volume_target(targets + i,
						       vg.lvs + i,
						       VHD_TYPE_VHD_VOLUME);
		if (err) {
			vhd_util_scan_error(vg.lvs[i].name, err);
			if (!(flags & VHD_SCAN_NOFAIL))
				goto out;
		}
	}

	err       = 0;
	*_total   = total;
	*_targets = targets;

out:
	if (err)
		free(targets);
	return err;
}

static int
vhd_util_scan_find_targets(int cnt, char **names,
			   const char *volume, const char *filter,
			   struct target **targets, int *total)
{
	if (flags & VHD_SCAN_VOLUME)
		return vhd_util_scan_find_volume_targets(cnt, names,
							 volume, filter,
							 targets, total);
	return vhd_util_scan_find_file_targets(cnt, names,
					       filter, targets, total);
}

int
vhd_util_scan(int argc, char **argv)
{
	int c, ret, err, cnt;
	char *filter, *volume;
	struct target *targets;

	cnt     = 0;
	ret     = 0;
	err     = 0;
	flags   = 0;
	filter  = NULL;
	volume  = NULL;
	targets = NULL;

	optind = 0;
	while ((c = getopt(argc, argv, "m:fcl:pavh")) != -1) {
		switch (c) {
		case 'm':
			filter = optarg;
			break;
		case 'f':
			flags |= VHD_SCAN_FAST;
			break;
		case 'c':
			flags |= VHD_SCAN_NOFAIL;
			break;
		case 'l':
			volume = optarg;
			flags |= VHD_SCAN_VOLUME;
			break;
		case 'p':
			flags |= VHD_SCAN_PRETTY;
			break;
		case 'a':
			flags |= VHD_SCAN_PARENTS;
			break;
		case 'v':
			flags |= VHD_SCAN_VERBOSE;
			break;
		case 'h':
			goto usage;
		default:
			err = -EINVAL;
			goto usage;
		}
	}

	if (!filter && argc - optind == 0) {
		err = -EINVAL;
		goto usage;
	}

	if (flags & VHD_SCAN_PRETTY)
		flags &= ~VHD_SCAN_FAST;

	err = vhd_util_scan_find_targets(argc - optind, argv + optind,
					 volume, filter, &targets, &cnt);
	if (err) {
		printf("scan failed: %d\n", err);
		return err;
	}

	if (!cnt)
		return 0;

	if (flags & VHD_SCAN_PRETTY)
		err = vhd_util_scan_targets_pretty(cnt, targets);
	else
		err = vhd_util_scan_targets(cnt, targets);

	free(targets);
	lvm_free_vg(&vg);

	return ((flags & VHD_SCAN_NOFAIL) ? 0 : err);

usage:
	printf("usage: [OPTIONS] FILES\n"
	       "options: [-m match filter] [-f fast] [-c continue on failure] "
	       "[-l LVM volume] [-p pretty print] [-a scan parents] "
	       "[-v verbose] [-h help]\n");
	return err;
}
