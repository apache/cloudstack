//
// VMOpsLogger.cpp
// VMOps instance manager implementation
//
// Copyright (C) VMOps Inc.
// All rights reserved.
//
#include "VMOpsServiceImpl.h"

#include <atlbase.h>

#define	MAX_LOGFILE_SIZE	1000000			// 1M

using namespace VMOps;

/////////////////////////////////////////////////////////////////////////////
// CLogger
//
CLogger* CLogger::s_pInstance = NULL;

CLogger::CLogger()
{
	_ASSERTE(s_pInstance == NULL);

	s_pInstance = this;
	m_pFile = NULL;
}

CLogger::~CLogger()
{
	if(m_pFile != NULL)
		fclose(m_pFile);
}

BOOL CLogger::Initialize()
{
	TCHAR achPath[_MAX_PATH];
	TCHAR achDrive[_MAX_DRIVE];
	TCHAR achDir[_MAX_DIR];

	GetModuleFileName(NULL, achPath, _MAX_PATH);
	_tsplitpath(achPath, achDrive, achDir, NULL, NULL);
	_tmakepath(achPath, achDrive, achDir, _T("vmops"), _T(".log"));
	m_pFile = _tfopen(achPath, _T("a+"));

	return m_pFile != NULL;
}

void CLogger::RotateLog()
{
	TCHAR achPath[_MAX_PATH];
	TCHAR achDrive[_MAX_DRIVE];
	TCHAR achDir[_MAX_DIR];

	GetModuleFileName(NULL, achPath, _MAX_PATH);
	_tsplitpath(achPath, achDrive, achDir, NULL, NULL);
	_tmakepath(achPath, achDrive, achDir, _T("vmops"), _T(".log"));

	TCHAR achPath2[_MAX_PATH];
	GetModuleFileName(NULL, achPath2, _MAX_PATH);
	_tsplitpath(achPath2, achDrive, achDir, NULL, NULL);
	_tmakepath(achPath2, achDrive, achDir, _T("vmops"), _T(".log.bak"));

	MoveFileEx(achPath, achPath2, MOVEFILE_REPLACE_EXISTING);
}

void CLogger::Cleanup()
{
	if(m_pFile != NULL)
		fclose(m_pFile);
	m_pFile = NULL;
}

void CLogger::Log(LPCSTR lpszCategory, LPCSTR lpszFormat, ...)
{
	CLock lock(m_lock);

	if(m_pFile != NULL)
	{
        SYSTEMTIME tm;
        DWORD dwProcessId = GetCurrentProcessId();
        DWORD dwThreadId = GetCurrentThreadId();
        GetSystemTime(&tm);

        fprintf(m_pFile, "[%02u/%02u/%04u %02u:%02u:%02u.%03u][pid:%lu][tid:%lu][%s] ",
            tm.wMonth, tm.wDay, tm.wYear, tm.wHour, tm.wMinute, tm.wSecond, tm.wMilliseconds,
            dwProcessId, dwThreadId, lpszCategory);

        va_list argMark;
        va_start(argMark, lpszFormat);
        vfprintf(m_pFile, lpszFormat, argMark);
        va_end(argMark);
        fprintf(m_pFile, "\n");
        fflush(m_pFile);

		if(ftell(m_pFile) > MAX_LOGFILE_SIZE)
		{
			fclose(m_pFile);
			m_pFile = NULL;

			RotateLog();

			// reopen it
			Initialize();
		}
	}
}

CLogger* CLogger::GetInstance()
{
	return s_pInstance;
}

CLogger g_logger;

/////////////////////////////////////////////////////////////////////////////
