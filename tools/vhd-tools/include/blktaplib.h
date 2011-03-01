/* blktaplib.h
 *
 * Blktap library userspace code.
 *
 * Copyright (c) 2007, XenSource Inc.
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

#ifndef __BLKTAPLIB_H__
#define __BLKTAPLIB_H__

#include <syslog.h>
#include <xenctrl.h>
#include <xen/io/blkif.h>

#if 1
#define DPRINTF(_f, _a...) syslog(LOG_INFO, _f, ##_a)
#else
#define DPRINTF(_f, _a...) ((void)0)
#endif

#define EPRINTF(_f, _a...) syslog(LOG_ERR, "tap-err:%s: " _f, __func__, ##_a)
#define PERROR(_f, _a...)  EPRINTF(_f ": %s", ##_a, strerror(errno))

#define BLK_RING_SIZE __CONST_RING_SIZE(blkif, XC_PAGE_SIZE)

/* size of the extra VMA area to map in attached pages. */
#define BLKTAP_VMA_PAGES BLK_RING_SIZE

/* blktap IOCTLs: These must correspond with the blktap driver ioctls */
#define BLKTAP_IOCTL_KICK_FE         1
#define BLKTAP_IOCTL_KICK_BE         2
#define BLKTAP_IOCTL_SETMODE         3
#define BLKTAP_IOCTL_SENDPID	     4
#define BLKTAP_IOCTL_NEWINTF	     5
#define BLKTAP_IOCTL_MINOR	     6
#define BLKTAP_IOCTL_MAJOR	     7
#define BLKTAP_QUERY_ALLOC_REQS      8
#define BLKTAP_IOCTL_FREEINTF	     9
#define BLKTAP_IOCTL_PRINT_IDXS      100 
#define BLKTAP_IOCTL_BACKDEV_SETUP   200

#define PRIO_SPECIAL_IO             -9999 

/* blktap switching modes: (Set with BLKTAP_IOCTL_SETMODE)             */
#define BLKTAP_MODE_PASSTHROUGH      0x00000000  /* default            */
#define BLKTAP_MODE_INTERCEPT_FE     0x00000001
#define BLKTAP_MODE_INTERCEPT_BE     0x00000002

#define BLKTAP_MODE_INTERPOSE \
           (BLKTAP_MODE_INTERCEPT_FE | BLKTAP_MODE_INTERCEPT_BE)

static inline int BLKTAP_MODE_VALID(unsigned long arg)
{
	return (
		( arg == BLKTAP_MODE_PASSTHROUGH  ) ||
		( arg == BLKTAP_MODE_INTERCEPT_FE ) ||
		( arg == BLKTAP_MODE_INTERPOSE    ) );
}

#define MAX_REQUESTS            BLK_RING_SIZE

#define BLKTAP_IOCTL_KICK       1
#define MAX_PENDING_REQS	BLK_RING_SIZE
#define BLKTAP_DEV_DIR          "/dev/xen"
#define BLKTAP_DEV_NAME         "blktap"
#define BACKDEV_NAME            "backdev"
#define BLKTAP_DEV_MINOR        0
#define BLKTAP_CTRL_DIR         "/var/run/tap"

extern int blktap_major;

#define BLKTAP_RING_PAGES       1 /* Front */
#define BLKTAP_MMAP_REGION_SIZE (BLKTAP_RING_PAGES + MMAP_PAGES)

struct blkif;
struct blkif_info;

typedef struct {
	blkif_request_t  req;
	int              submitting;
	int              secs_pending;
        int16_t          status;
	int              num_retries;
	struct timeval   last_try;
} pending_req_t;

typedef struct blkif {
	domid_t domid;
	long int handle;
	
	long int pdev;
	long int readonly;
	
	enum { DISCONNECTED, DISCONNECTING, CONNECTED } state;
	
	struct blkif_ops *ops;
	struct blkif *hash_next;
	
	void *prv;  /* device-specific data */
	struct blkif_info *info; /*Image parameter passing */
	pending_req_t pending_list[MAX_REQUESTS];
	int devnum;
	int fds[2];
	int be_id;
	char *backend_path;
	int major;
	int minor;
	pid_t tappid;
	int drivertype;
	uint16_t cookie;
	int err;
} blkif_t;

typedef struct blkif_info {
	char *params;
	int   readonly;
	int   storage;
} blkif_info_t;

typedef struct tapdev_info {
	int fd;
	char *mem;
	blkif_sring_t *sring;
	blkif_back_ring_t  fe_ring;
	unsigned long vstart;
	blkif_t *blkif;
} tapdev_info_t;

typedef struct domid_translate {
	unsigned short domid;
	unsigned short busid;
} domid_translate_t ;

typedef struct image {
	unsigned long long size;
	unsigned long secsize;
	unsigned int info;
} image_t;

typedef struct msg_hdr {
	uint16_t   type;
	uint16_t   len;
	uint16_t   drivertype;
	uint16_t   cookie;
} msg_hdr_t;

typedef struct msg_params {
	uint8_t    readonly;
	int        path_off;
	int        path_len;
	int        storage;
} msg_params_t;

typedef struct msg_newdev {
	uint8_t     devnum;
	uint16_t    domid;
} msg_newdev_t;

typedef struct msg_pid {
	pid_t     pid;
} msg_pid_t;

typedef struct msg_cp {
	int       cp_uuid_off;
	int       cp_uuid_len;
	int       cp_drivertype;
} msg_cp_t;

typedef struct msg_lock {
	int       ro;
	int       enforce;
	int       uuid_off;
	int       uuid_len;
} msg_lock_t;

#define READ 0
#define WRITE 1

/*Control Messages between manager and tapdev*/
#define CTLMSG_PARAMS          1
#define CTLMSG_IMG             2
#define CTLMSG_IMG_FAIL        3
#define CTLMSG_NEWDEV          4
#define CTLMSG_NEWDEV_RSP      5
#define CTLMSG_NEWDEV_FAIL     6
#define CTLMSG_CLOSE           7
#define CTLMSG_CLOSE_RSP       8
#define CTLMSG_PID             9
#define CTLMSG_PID_RSP         10
#define CTLMSG_CHECKPOINT      11
#define CTLMSG_CHECKPOINT_RSP  12
#define CTLMSG_LOCK            13
#define CTLMSG_LOCK_RSP        14
#define CTLMSG_PAUSE           15
#define CTLMSG_PAUSE_RSP       16
#define CTLMSG_RESUME          17
#define CTLMSG_RESUME_RSP      18

#define TAPDISK_STORAGE_TYPE_NFS       1
#define TAPDISK_STORAGE_TYPE_EXT       2
#define TAPDISK_STORAGE_TYPE_LVM       3
#define TAPDISK_STORAGE_TYPE_DEFAULT   TAPDISK_STORAGE_TYPE_EXT

/* Abitrary values, must match the underlying driver... */
#define MAX_TAP_DEV 256

/* Accessing attached data page mappings */
#define MMAP_PAGES                                                    \
    (MAX_PENDING_REQS * BLKIF_MAX_SEGMENTS_PER_REQUEST)
#define MMAP_VADDR(_vstart,_req,_seg)                                 \
    ((_vstart) +                                                      \
     ((_req) * BLKIF_MAX_SEGMENTS_PER_REQUEST * getpagesize()) +      \
     ((_seg) * getpagesize()))

/* Defines that are only used by library clients */

#ifndef __COMPILING_BLKTAP_LIB

static char *blkif_op_name[] = {
	[BLKIF_OP_READ]       = "READ",
	[BLKIF_OP_WRITE]      = "WRITE",
};

#endif /* __COMPILING_BLKTAP_LIB */

#endif /* __BLKTAPLIB_H__ */
