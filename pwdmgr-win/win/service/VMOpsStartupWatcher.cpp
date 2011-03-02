//
// VMOpsStartupWatcher.cpp
// VMOps instance manager implementation
//
// Copyright (C) VMOps Inc.
// All rights reserved.
//

#include "VMOpsServiceImpl.h"
#include <atlbase.h>

using namespace VMOps;

#define RETRY_INTERVAL 1000
#define MAX_WAIT_TIME  1800000			// 30 minutes

/////////////////////////////////////////////////////////////////////////////
// Helpers
//
BOOL IsMiniSetupInProgess()
{
	CRegKey key;

	if(key.Open(HKEY_LOCAL_MACHINE, _T("SYSTEM\\Setup"), KEY_READ) == ERROR_SUCCESS)
	{
		DWORD dwValue = 0;
		key.QueryDWORDValue(_T("SystemSetupInProgress"), dwValue);
		if(dwValue != 0)
		{
			CLogger::GetInstance()->Log("INFO", "Mini-setup is in pregress");
		}

		return dwValue != 0;
	}
	else
	{
		CLogger::GetInstance()->Log("INFO", "Mini-setup information is not present");
	}

	return FALSE;
}

/////////////////////////////////////////////////////////////////////////////
// CVMOpsStartupWatcher
//
CVMOpsStartupWatcher::CVMOpsStartupWatcher(CVMOpsServiceProvider* pProvider)
{
	_ASSERTE(pProvider);
	m_pProvider = pProvider;
}

CVMOpsStartupWatcher::~CVMOpsStartupWatcher()
{
}

DWORD CVMOpsStartupWatcher::ThreadRun()
{
	DWORD dwStartTick = GetTickCount();
	while(TRUE)
	{
		if(WaitForSingleObject(GetStopEventHandle(), RETRY_INTERVAL) == WAIT_OBJECT_0)
			break;

		if(DoStartupConfig())
			break;

		if(GetTickCount() - dwStartTick > MAX_WAIT_TIME)
		{
			CLogger::GetInstance()->Log("WARN", "Unable to contact default gateway, give up trying after 30 minutes");
			break;
		}
	}

	return 0;
}

BOOL CVMOpsStartupWatcher::GetPasswordProviderUrl(LPTSTR lpszUrl)
{
	// asumming we have enough space in lpszUrl
	char achBuf[256];
	achBuf[0] = 0;
	DWORD dwLength = sizeof(achBuf);
	if(m_pProvider->GetNextPasswordProvider(achBuf, &dwLength) == HERROR_SUCCESS && achBuf[0] != 0)
	{
		USES_CONVERSION;

		char achUrl[256];
		sprintf(achUrl, "http://%s:8080/", achBuf);

		lstrcpy(lpszUrl, A2T(achUrl));
		return TRUE;
	}
	return FALSE;
}


BOOL CVMOpsStartupWatcher::DoStartupConfig()
{
	USES_CONVERSION;

	if(IsMiniSetupInProgess())
	{
		CLogger::GetInstance()->Log("INFO", "Mini-setup is detected, skip VMOps startup configuratin process");
		return TRUE;
	}

	TCHAR achUrl[256];
	char achResult[256];

	memset(achUrl, 0, sizeof(achUrl));
	GetPasswordProviderUrl(achUrl);

	if(achUrl[0] != 0)
	{
		CLogger::GetInstance()->Log("INFO", "Contact password provider at : %ws", achUrl);

		memset(achResult, 0, sizeof(achResult));
		DWORD dwBytesToRead = sizeof(achResult) - 1;
		DWORD dwBytesRead = 0;
		if(m_pProvider->SimpleHttpGet(achUrl, _T("DomU_Request: send_my_password"), 
			achResult, dwBytesToRead, &dwBytesRead) == HERROR_SUCCESS)
		{
			achResult[dwBytesRead] = 0;
			// Trim whitespace at tail
			int nPos = strlen(achResult) - 1;
			while(nPos > 0)
			{
				if(strchr(" \t\r\n", achResult[nPos]) != NULL)
					achResult[nPos] = 0;
				else
					break;

				nPos--;
			}

			if(strcmp(achResult, "saved_password") != 0)
			{
				CLogger::GetInstance()->Log("INFO", "Need to set new password for this VM. First letter in password : %c", achResult[0]);

				if(m_pProvider->SetPassword(_T("Administrator"), A2T(achResult)) == HERROR_SUCCESS)
				{
					CLogger::GetInstance()->Log("INFO", "New password has been set for this VM");

					memset(achResult, 0, sizeof(achResult));
					m_pProvider->SimpleHttpGet(achUrl, _T("DomU_Request: saved_password"), 
						achResult, dwBytesToRead, &dwBytesRead);
				}
				else
				{
					CLogger::GetInstance()->Log("ERROR", "Error to set new password");
					return FALSE;
				}
			}
			else
			{
				CLogger::GetInstance()->Log("INFO", "No need to set password");
			}
		}
		else 
		{
			CLogger::GetInstance()->Log("ERROR", "Unable to contact password provider at : %ws", achUrl);
			return FALSE;
		}

		return TRUE;
	}

	return FALSE;
}

/////////////////////////////////////////////////////////////////////////////
