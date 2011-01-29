// UltraVncAx.cpp : Implementation of DLL Exports.


// Note: Proxy/Stub Information
//      To build a separate proxy/stub DLL, 
//      run nmake -f UltraVncAxps.mk in the project directory.

#include "stdafx.h"
#include "resource.h"
#include <initguid.h>
#include "UltraVncAx.h"

#include "UltraVncAx_i.c"
#include "UltraVncAxObj.h"


CComModule _Module;

BEGIN_OBJECT_MAP(ObjectMap)
OBJECT_ENTRY(CLSID_UltraVncAxObj, CUltraVncAxObj)
END_OBJECT_MAP()

// for safety scripting
HRESULT CreateComponentCategory(CATID catid, WCHAR* catDescription);
HRESULT RegisterCLSIDInCategory(REFCLSID clsid, CATID catid);
HRESULT UnRegisterCLSIDInCategory(REFCLSID clsid, CATID catid);

HRESULT CreateComponentCategory(CATID catid, WCHAR* catDescription)
{
    ICatRegister* pcr = NULL ;
    HRESULT hr = S_OK ;

    hr = CoCreateInstance(CLSID_StdComponentCategoriesMgr, 
			NULL, CLSCTX_INPROC_SERVER, IID_ICatRegister, (void**)&pcr);
	if (FAILED(hr))
		return hr;

    // Make sure the HKCR\Component Categories\{..catid...}
    // key is registered
    CATEGORYINFO catinfo;
    catinfo.catid = catid;
    catinfo.lcid = 0x0409 ; // english

	// Make sure the provided description is not too long.
	// Only copy the first 127 characters if it is
	int len = lstrlenW(catDescription);
	if (len>127)
		len = 127;
    lstrcpynW(catinfo.szDescription, catDescription, len);
	// Make sure the description is null terminated
	catinfo.szDescription[len] = '\0';

    hr = pcr->RegisterCategories(1, &catinfo);
	pcr->Release();

	return hr;
}

HRESULT RegisterCLSIDInCategory(REFCLSID clsid, CATID catid)
{
// Register your component categories information.
    ICatRegister* pcr = NULL ;
    HRESULT hr = S_OK ;
    hr = CoCreateInstance(CLSID_StdComponentCategoriesMgr, 
			NULL, CLSCTX_INPROC_SERVER, IID_ICatRegister, (void**)&pcr);
    if (SUCCEEDED(hr))
    {
       // Register this category as being "implemented" by
       // the class.
       CATID rgcatid[1] ;
       rgcatid[0] = catid;
       hr = pcr->RegisterClassImplCategories(clsid, 1, rgcatid);
    }

    if (pcr != NULL)
        pcr->Release();
  
	return hr;
}

HRESULT UnRegisterCLSIDInCategory(REFCLSID clsid, CATID catid)
{
    ICatRegister* pcr = NULL ;
    HRESULT hr = S_OK ;
    hr = CoCreateInstance(CLSID_StdComponentCategoriesMgr, 
			NULL, CLSCTX_INPROC_SERVER, IID_ICatRegister, (void**)&pcr);
    if (SUCCEEDED(hr))
    {
       // Unregister this category as being "implemented" by
       // the class.
       CATID rgcatid[1] ;
       rgcatid[0] = catid;
       hr = pcr->UnRegisterClassImplCategories(clsid, 1, rgcatid);
    }

    if (pcr != NULL)
        pcr->Release();
  
	return hr;
}


/////////////////////////////////////////////////////////////////////////////
// DLL Entry Point

#include "Log.h"
extern Log vnclog;

extern "C"
BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID /*lpReserved*/)
{
    if (dwReason == DLL_PROCESS_ATTACH)
    {
        _Module.Init(ObjectMap, hInstance, &LIBID_ULTRAVNCAXLib);
        DisableThreadLibraryCalls(hInstance);
    }
    else if (dwReason == DLL_PROCESS_DETACH)
	{
        _Module.Term();
		
		vnclog.Print(1, _T("DllMain detach process done\n"));
	}
    return TRUE;    // ok
}

/////////////////////////////////////////////////////////////////////////////
// Used to determine whether the DLL can be unloaded by OLE

STDAPI DllCanUnloadNow(void)
{
    return (_Module.GetLockCount()==0) ? S_OK : S_FALSE;
}

/////////////////////////////////////////////////////////////////////////////
// Returns a class factory to create an object of the requested type

STDAPI DllGetClassObject(REFCLSID rclsid, REFIID riid, LPVOID* ppv)
{
    return _Module.GetClassObject(rclsid, riid, ppv);
}

/////////////////////////////////////////////////////////////////////////////
// DllRegisterServer - Adds entries to the system registry

STDAPI DllRegisterServer(void)
{
    // registers object, typelib and all interfaces in typelib
    HRESULT hr = _Module.RegisterServer(TRUE);

	CreateComponentCategory(CATID_SafeForScripting, L"Controls that are safely scriptable");
	RegisterCLSIDInCategory(CLSID_UltraVncAxObj, CATID_SafeForScripting);
	CreateComponentCategory(CATID_SafeForInitializing, L"Controls safely initializable from persistent data");
	RegisterCLSIDInCategory(CLSID_UltraVncAxObj, CATID_SafeForInitializing);
	return hr;
}

/////////////////////////////////////////////////////////////////////////////
// DllUnregisterServer - Removes entries from the system registry

STDAPI DllUnregisterServer(void)
{
	UnRegisterCLSIDInCategory(CLSID_UltraVncAxObj, CATID_SafeForScripting);
	UnRegisterCLSIDInCategory(CLSID_UltraVncAxObj, CATID_SafeForInitializing);

    return _Module.UnregisterServer(TRUE);
}


