// VMOps Instance Manager.cpp : Implementation of WinMain

#include "stdafx.h"
#include "resource.h"
#include "VMOpsInstanceManager_i.h"

#include <stdio.h>
#include <tchar.h>

#include "VMOpsServiceImpl.h"
using namespace VMOps;

class CVMOpsInstanceManagerModule : public CAtlServiceModuleT< CVMOpsInstanceManagerModule, IDS_SERVICENAME >
{
public :
	DECLARE_LIBID(LIBID_VMOpsInstanceManagerLib)
	DECLARE_REGISTRY_APPID_RESOURCEID(IDR_VMOPSINSTANCEMANAGER, "{7C7E823B-66C7-4D1C-8DE3-9616605B1768}")
	HRESULT InitializeSecurity() throw()
	{
		// TODO : Call CoInitializeSecurity and provide the appropriate security settings for 
		// your service
		// Suggested - PKT Level Authentication, 
		// Impersonation Level of RPC_C_IMP_LEVEL_IDENTIFY 
		// and an appropiate Non NULL Security Descriptor.
		return S_OK;
	}

	HRESULT PreMessageLoop(int nShowCmd = SW_HIDE) throw()
	{
		CAtlServiceModuleT< CVMOpsInstanceManagerModule, IDS_SERVICENAME >::PreMessageLoop(nShowCmd);

		CLogger::GetInstance()->Initialize();
		m_serviceProvider.Start();

		SetServiceStatus(SERVICE_RUNNING);
		return S_OK;
	}

	HRESULT PostMessageLoop() throw()
	{
		CAtlServiceModuleT< CVMOpsInstanceManagerModule, IDS_SERVICENAME >::PostMessageLoop();
		m_serviceProvider.Stop();
		CLogger::GetInstance()->Cleanup();
		return S_OK;
	}

	// override to install the service as auto-start service
	inline HRESULT RegisterAppId(bool bService = false) throw()
	{
		if (!Uninstall())
			return E_FAIL;

		HRESULT hr = UpdateRegistryAppId(TRUE);
		if (FAILED(hr))
			return hr;

		CRegKey keyAppID;
		LONG lRes = keyAppID.Open(HKEY_CLASSES_ROOT, _T("AppID"), KEY_WRITE);
		if (lRes != ERROR_SUCCESS)
			return AtlHresultFromWin32(lRes);

		CRegKey key;

		lRes = key.Create(keyAppID, GetAppIdT());
		if (lRes != ERROR_SUCCESS)
			return AtlHresultFromWin32(lRes);

		key.DeleteValue(_T("LocalService"));

		if (!bService)
			return S_OK;

		key.SetStringValue(_T("LocalService"), m_szServiceName);

		// Create service
		if (!InstallAsAutoService())
			return E_FAIL;
		return S_OK;
	}

	BOOL InstallAsAutoService() throw()
	{
		if (IsInstalled())
			return TRUE;

		// Get the executable file path
		TCHAR szFilePath[MAX_PATH + _ATL_QUOTES_SPACE];
		DWORD dwFLen = ::GetModuleFileName(NULL, szFilePath + 1, MAX_PATH);
		if( dwFLen == 0 || dwFLen == MAX_PATH )
			return FALSE;

		// Quote the FilePath before calling CreateService
		szFilePath[0] = _T('\"');
		szFilePath[dwFLen + 1] = _T('\"');
		szFilePath[dwFLen + 2] = 0;

		SC_HANDLE hSCM = ::OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
		if (hSCM == NULL)
		{
			TCHAR szBuf[1024];
			if (AtlLoadString(ATL_SERVICE_MANAGER_OPEN_ERROR, szBuf, 1024) == 0)
#ifdef UNICODE
				Checked::wcscpy_s(szBuf, _countof(szBuf), _T("Could not open Service Manager"));
#else
				Checked::strcpy_s(szBuf, _countof(szBuf), _T("Could not open Service Manager"));
#endif
			MessageBox(NULL, szBuf, m_szServiceName, MB_OK);
			return FALSE;
		}

		SC_HANDLE hService = ::CreateService(
			hSCM, m_szServiceName, _T("Cloud.com VM Instance Manager"), 
			SERVICE_ALL_ACCESS, SERVICE_WIN32_OWN_PROCESS,
			/*SERVICE_DEMAND_START*/ SERVICE_AUTO_START, SERVICE_ERROR_NORMAL,
			szFilePath, NULL, NULL, _T("RPCSS\0"), NULL, NULL);

		if (hService == NULL)
		{
			::CloseServiceHandle(hSCM);
			TCHAR szBuf[1024];
			if (AtlLoadString(ATL_SERVICE_START_ERROR, szBuf, 1024) == 0)
#ifdef UNICODE
				Checked::wcscpy_s(szBuf, _countof(szBuf), _T("Could not create service"));
#else
				Checked::strcpy_s(szBuf, _countof(szBuf), _T("Could not create service"));
#endif
			MessageBox(NULL, szBuf, m_szServiceName, MB_OK);
			return FALSE;
		}

		::CloseServiceHandle(hService);
		::CloseServiceHandle(hSCM);
		return TRUE;
	}

private :
	CVMOpsServiceProvider m_serviceProvider;
};

CVMOpsInstanceManagerModule _AtlModule;

//
extern "C" int WINAPI _tWinMain(HINSTANCE /*hInstance*/, HINSTANCE /*hPrevInstance*/, 
                                LPTSTR /*lpCmdLine*/, int nShowCmd)
{
    return _AtlModule.WinMain(nShowCmd);
}

