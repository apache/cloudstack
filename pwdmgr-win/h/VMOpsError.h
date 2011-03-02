//
// VMOpsError.h
// Common error codes
//
// Copyright (C) Cloud.com Inc.
// All rights reserved.
//
#ifndef __VMOpsError_H__
#define __VMOpsError_H__

#if defined(WIN32) || defined(_WIN32)
#include <windows.h>
#endif

#ifndef HERROR_DEFINED
#define HERROR_DEFINED

typedef LONG HERROR;
#define MAKE_ERROR(module, code)	\
	(1L << 31 | (module << 16) | code)

#define HERROR_FAILED(error) \
	((error & (1 << 31L)) != 0)

#endif

// common error codes
#define HERROR_SUCCESS						0
#define HERROR_FAIL							MAKE_ERROR(0, 1)
#define HERROR_NOT_SUPPORTED				MAKE_ERROR(0, 2)
#define HERROR_INVALID_PARAMETER			MAKE_ERROR(0, 3)
#define HERROR_INSUFFICIENT_BUFFER			MAKE_ERROR(0, 4)
#define HERROR_NOT_FOUND					MAKE_ERROR(0, 5)

#endif // !__VMOpsError_H__

/////////////////////////////////////////////////////////////////////////////
