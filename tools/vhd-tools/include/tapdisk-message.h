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
#ifndef _TAPDISK_MESSAGE_H_
#define _TAPDISK_MESSAGE_H_

#include <inttypes.h>

#define TAPDISK_MESSAGE_MAX_PATH_LENGTH  256
#define TAPDISK_MESSAGE_STRING_LENGTH    256

#define TAPDISK_MESSAGE_FLAG_SHARED      0x01
#define TAPDISK_MESSAGE_FLAG_RDONLY      0x02
#define TAPDISK_MESSAGE_FLAG_ADD_CACHE   0x04
#define TAPDISK_MESSAGE_FLAG_VHD_INDEX   0x08
#define TAPDISK_MESSAGE_FLAG_LOG_DIRTY   0x10

typedef struct tapdisk_message           tapdisk_message_t;
typedef uint8_t                          tapdisk_message_flag_t;
typedef struct tapdisk_message_image     tapdisk_message_image_t;
typedef struct tapdisk_message_params    tapdisk_message_params_t;
typedef struct tapdisk_message_string    tapdisk_message_string_t;

struct tapdisk_message_params {
	tapdisk_message_flag_t           flags;
	
	uint8_t                          storage;
	uint32_t                         devnum;
	uint32_t                         domid;
	uint16_t                         path_len;
	char                             path[TAPDISK_MESSAGE_MAX_PATH_LENGTH];
};

struct tapdisk_message_image {
	uint64_t                         sectors;
	uint32_t                         sector_size;
	uint32_t                         info;
};

struct tapdisk_message_string {
	char                             text[TAPDISK_MESSAGE_STRING_LENGTH];
};

struct tapdisk_message {
	uint16_t                         type;
	uint16_t                         cookie;
	uint16_t                         drivertype;

	union {
		pid_t                    tapdisk_pid;
		tapdisk_message_image_t  image;
		tapdisk_message_params_t params;
		tapdisk_message_string_t string;
	} u;
};

enum tapdisk_message_id {
	TAPDISK_MESSAGE_ERROR = 1,
	TAPDISK_MESSAGE_RUNTIME_ERROR,
	TAPDISK_MESSAGE_PID,
	TAPDISK_MESSAGE_PID_RSP,
	TAPDISK_MESSAGE_OPEN,
	TAPDISK_MESSAGE_OPEN_RSP,
	TAPDISK_MESSAGE_PAUSE,
	TAPDISK_MESSAGE_PAUSE_RSP,
	TAPDISK_MESSAGE_RESUME,
	TAPDISK_MESSAGE_RESUME_RSP,
	TAPDISK_MESSAGE_CLOSE,
	TAPDISK_MESSAGE_CLOSE_RSP,
	TAPDISK_MESSAGE_EXIT,
};

static inline char *
tapdisk_message_name(enum tapdisk_message_id id)
{
	switch (id) {
	case TAPDISK_MESSAGE_ERROR:
		return "error";

	case TAPDISK_MESSAGE_PID:
		return "pid";

	case TAPDISK_MESSAGE_PID_RSP:
		return "pid response";

	case TAPDISK_MESSAGE_OPEN:
		return "open";

	case TAPDISK_MESSAGE_OPEN_RSP:
		return "open response";

	case TAPDISK_MESSAGE_PAUSE:
		return "pause";

	case TAPDISK_MESSAGE_PAUSE_RSP:
		return "pause response";

	case TAPDISK_MESSAGE_RESUME:
		return "resume";

	case TAPDISK_MESSAGE_RESUME_RSP:
		return "resume response";

	case TAPDISK_MESSAGE_CLOSE:
		return "close";

	case TAPDISK_MESSAGE_CLOSE_RSP:
		return "close response";

	case TAPDISK_MESSAGE_EXIT:
		return "exit";

	default:
		return "unknown";
	}
}

#endif
