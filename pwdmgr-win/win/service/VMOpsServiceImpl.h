//
// VMOpsServiceImpl.h
// VMOps instance manager implementation
//
// Copyright (C) VMOps Inc.
// All rights reserved.
//
#ifndef __VMOpsServiceImpl_H__
#define __VMOpsServiceImpl_H__

#include "VMOpsError.h"
#include "ThreadUtil.h"

#include <stdio.h>
#include <IPHlpApi.h>
#include <list>

namespace VMOps {

/////////////////////////////////////////////////////////////////////////////
// class diagram
//
class CVMOpsServiceProvider;
class CThread;
		class CVMOpsStartupWatcher;

class CLogger;

/////////////////////////////////////////////////////////////////////////////
// CVMOpsServiceProvider
//
class CVMOpsServiceProvider
{
public :
	CVMOpsServiceProvider();
	~CVMOpsServiceProvider();

public :
	HERROR SetPassword(LPCTSTR lpszUserName, LPCTSTR lpszPassword);
	HERROR GetNextPasswordProvider(LPSTR lpszBuf, LPDWORD pdwLength);
	HERROR GetDefaultGateway(LPSTR lpszBuf, LPDWORD pdwLength);
	HERROR SimpleHttpGet(LPCTSTR lpszUrl, LPCTSTR lpszHeaders, 
		LPVOID pOutputBuffer, DWORD dwBytesToRead, DWORD* pdwBytesRead);

	HERROR Start();
	HERROR Stop();

protected :
	CVMOpsStartupWatcher* m_pWatcher;

	std::list<IP_ADDRESS_STRING> m_lstProviders;
};

/////////////////////////////////////////////////////////////////////////////
// CVMOpsStartupWatcher
//
class CVMOpsStartupWatcher : public CThread
{
public :
	CVMOpsStartupWatcher(CVMOpsServiceProvider* pProvider);
	virtual ~CVMOpsStartupWatcher();

public :
	CVMOpsServiceProvider* GetProvider() { return m_pProvider; }

protected :
	virtual DWORD ThreadRun();

	BOOL DoStartupConfig();
	BOOL GetPasswordProviderUrl(LPTSTR lpszUrl);

protected :
	CVMOpsServiceProvider* m_pProvider;
};

/////////////////////////////////////////////////////////////////////////////
// CLogger
// A simple logger for internal use
//
class CLogger
{
public :
	CLogger();
	~CLogger();

public :
	static CLogger* GetInstance();
	BOOL Initialize();
	void RotateLog();
	void Cleanup();

	void Log(LPCSTR lpszCategory, LPCSTR lpszFormat, ...);

private :
	CCriticalSection m_lock;
	FILE* m_pFile;

private :
	static CLogger* s_pInstance;
};

}
#endif	// __VMOpsServiceProvider_H__

/////////////////////////////////////////////////////////////////////////////
