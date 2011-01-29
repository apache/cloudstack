//===============================
// UltraVncAxGlobalConstructor.h
//===============================

#pragma once

/////////////////////////////////////////////////////////////////////////////

#include "CCritSec.h"
#include "Helpers.h"

/////////////////////////////////////////////////////////////////////////////

class CUltraVncAxGlobalConstructor
{
public:

	// destruction.
	virtual ~ CUltraVncAxGlobalConstructor ();

public:

	// methods.
	VOID Construct ( HINSTANCE hInst );
	BOOL NewConnection ( CONST CHAR* pszHost, CONST CHAR* pszUser = NULL, CONST CHAR* pszPassword = NULL,
		CONST CHAR* pszProxy = NULL, HWND rootHwnd = NULL, HWND* pHwndAppFrame = NULL);
	VOID ProcessKeyEvent( HWND hwnd, WPARAM wParam, LPARAM lParam );
	charstring ExecuteCommand( HWND hwnd, charstring& csCmdText, BOOL& bIsErr );

protected:

	// data.
	CCriticalSection				m_csConstructAccess;
};

/////////////////////////////////////////////////////////////////////////////
