//
// VMOpsServiceProvider.cpp
// VMOps instance manager implementation
//
// Copyright (C) VMOps Inc.
// All rights reserved.
//
#include "VMOpsServiceImpl.h"

#include <atlbase.h>
#include <atlconv.h>

#include <lm.h>
#include <iptypes.h>
#include <iphlpapi.h>
#include <wininet.h>
#include <list>

using namespace VMOps;

/////////////////////////////////////////////////////////////////////////////
// Helpers
//
bool IsAddressAlreadyInList(const IP_ADDRESS_STRING& addr, std::list<IP_ADDRESS_STRING>& listDhCPServers)
{
	for(std::list<IP_ADDRESS_STRING>::iterator it = listDhCPServers.begin(); it != listDhCPServers.end(); it++)
	{
		if(strcmpi(addr.String, (*it).String) == 0)
			return true;
	}

	return false;
}

void GetDHCPServers(std::list<IP_ADDRESS_STRING>& listDhCPServers)
{
	ULONG ulSize = 0;
	GetAdaptersInfo(NULL, &ulSize);

	PIP_ADAPTER_INFO pHead = (PIP_ADAPTER_INFO)new BYTE[ulSize];
	PIP_ADAPTER_INFO pAdapterInfo = pHead;
	GetAdaptersInfo(pAdapterInfo, &ulSize);

	while(pAdapterInfo->Next != NULL)
	{
		if(pAdapterInfo->DhcpEnabled && !IsAddressAlreadyInList(pAdapterInfo->DhcpServer.IpAddress, listDhCPServers))
			listDhCPServers.push_back(pAdapterInfo->DhcpServer.IpAddress);

		pAdapterInfo = pAdapterInfo->Next;
	}

	delete [](LPBYTE)pHead;
}

/////////////////////////////////////////////////////////////////////////////
// CVMOpsServiceProvider
//
CVMOpsServiceProvider::CVMOpsServiceProvider()
{
	m_pWatcher = NULL;
}

CVMOpsServiceProvider::~CVMOpsServiceProvider()
{
	if(m_pWatcher != NULL)
		delete m_pWatcher;
}

HERROR CVMOpsServiceProvider::SetPassword(LPCTSTR lpszUserName, LPCTSTR lpszPassword)
{
	_ASSERTE(lpszUserName != NULL);

	USES_CONVERSION;
	USER_INFO_1003 ui;
	ui.usri1003_password = T2W((LPTSTR)lpszPassword);

	NET_API_STATUS status = NetUserSetInfo(NULL, T2W((LPTSTR)lpszUserName), 1003, (LPBYTE)&ui, NULL);
	if(status != NERR_Success)
	{
		switch(status)
		{
		case ERROR_ACCESS_DENIED :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : ERROR_ACCESS_DENIED");
			break;

		case ERROR_INVALID_PARAMETER :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : ERROR_INVALID_PARAMETER");
			break;

		case NERR_InvalidComputer :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_InvalidComputer");
			break;

		case NERR_NotPrimary :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_NotPrimary");
			break;

		case NERR_SpeGroupOp :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_SpeGroupOp");
			break;

		case NERR_LastAdmin :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_LastAdmin");
			break;

		case NERR_BadPassword :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_BadPassword");
			break;

		case NERR_PasswordTooShort :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_PasswordTooShort");
			break;

		case NERR_UserNotFound :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : NERR_UserNotFound");
			break;

		default :
			CLogger::GetInstance()->Log("ERROR", "SetPassword failed with error : 0x%lx", (DWORD)status);
			break;
		}
		return HERROR_FAIL;
	}
	return HERROR_SUCCESS;
}

HERROR CVMOpsServiceProvider::GetNextPasswordProvider(LPSTR lpszBuf, LPDWORD pdwLength)
{
	if(m_lstProviders.size() == 0)
	{
		IP_ADDRESS_STRING addr;
		memset(&addr, 0, sizeof(addr));
		DWORD dwLength = sizeof(addr.String);
		GetDefaultGateway(addr.String, &dwLength);

		if(addr.String[0] != 0)
			m_lstProviders.push_back(addr);

		GetDHCPServers(m_lstProviders);
	}

	if(m_lstProviders.size() > 0)
	{
		strcpy(lpszBuf, (*(m_lstProviders.begin())).String);
		m_lstProviders.pop_front();
	}
	else
	{
		lpszBuf[0] = 0;
	}

	return HERROR_SUCCESS;
}


HERROR CVMOpsServiceProvider::GetDefaultGateway(LPSTR lpszBuf, LPDWORD pdwLength)
{
	_ASSERTE(pdwLength);

	PIP_ADAPTER_INFO pAdapter = NULL;
	DWORD dwBufLength = 0;

	GetAdaptersInfo(NULL, &dwBufLength);
	if(dwBufLength == 0)
		return HERROR_FAIL;

	pAdapter = (PIP_ADAPTER_INFO)new BYTE[dwBufLength];
	if ((GetAdaptersInfo( pAdapter, &dwBufLength)) != NO_ERROR)
	{
		delete [] (LPBYTE)pAdapter;
		return HERROR_FAIL;
	}

	HERROR hReturn = HERROR_NOT_FOUND;
	while (pAdapter) 
	{
		if(pAdapter->GatewayList.IpAddress.String != NULL && pAdapter->GatewayList.IpAddress.String[0] != 0)
		{
			if(*pdwLength < strlen(pAdapter->GatewayList.IpAddress.String) + 1)
			{
				*pdwLength = strlen(pAdapter->GatewayList.IpAddress.String) + 1;

				hReturn = HERROR_INSUFFICIENT_BUFFER;
			} else {
				_ASSERTE(lpszBuf);

				strcpy(lpszBuf, pAdapter->GatewayList.IpAddress.String);
				hReturn = HERROR_SUCCESS;
			}

			break;
		}
		pAdapter = pAdapter->Next;
	}

	delete [] (LPBYTE)pAdapter;
	return hReturn;
}

HERROR CVMOpsServiceProvider::SimpleHttpGet(LPCTSTR lpszUrl, LPCTSTR lpszHeaders, 
	LPVOID pOutputBuffer, DWORD dwBytesToRead, DWORD* pdwBytesRead)
{
	HINTERNET hInternet = InternetOpen(_T("VMOps Instance Manager"), INTERNET_OPEN_TYPE_DIRECT, NULL, NULL, 0);
	if(hInternet == NULL)
		return HERROR_FAIL;

	HINTERNET hConnection = InternetOpenUrl(hInternet, lpszUrl, lpszHeaders, -1, 
		INTERNET_FLAG_PRAGMA_NOCACHE | INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_UI, NULL);
	if(hConnection == NULL)
	{
		InternetCloseHandle(hInternet);
		return HERROR_FAIL;
	}

	if(!InternetReadFile(hConnection, pOutputBuffer, dwBytesToRead, pdwBytesRead))
	{
		InternetCloseHandle(hConnection);
		InternetCloseHandle(hInternet);
		return HERROR_FAIL;
	}

	InternetCloseHandle(hConnection);
	InternetCloseHandle(hInternet);
	return HERROR_SUCCESS;
}

HERROR CVMOpsServiceProvider::Start()
{
	CLogger::GetInstance()->Log("INFO", "VMOps instance Management Service started");

	m_pWatcher = new CVMOpsStartupWatcher(this);
	m_pWatcher->Create(0);

	CLogger::GetInstance()->Log("INFO", "VMOps instance Management startup watcher started");
	return HERROR_SUCCESS;
}

HERROR CVMOpsServiceProvider::Stop()
{
	if(m_pWatcher != NULL)
	{
		m_pWatcher->Stop();
		CLogger::GetInstance()->Log("INFO", "VMOps instance Management startup watcher stopped");
	}

	CLogger::GetInstance()->Log("INFO", "VMOps instance Management Service stopped");
	return HERROR_SUCCESS;
}

/////////////////////////////////////////////////////////////////////////////
