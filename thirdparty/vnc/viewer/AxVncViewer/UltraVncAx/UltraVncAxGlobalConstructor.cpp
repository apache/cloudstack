//===============================================================
// UltraVncAxGlobalConstructor.cpp
//
// -- Implementation file of class CUltraVncAxGlobalConstructor.
//===============================================================

/////////////////////////////////////////////////////////////////////////////

// -- App obj definition file:
#include "..\VNCviewerApp32.h"

// -- Resource symbols:
#include "..\res\resource.h"

// -- Win32 VncViewer app entry point function:
extern int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, PSTR szCmdLine, int iCmdShow);

// -- Global application object pointer (mantained by the VNCviewerApp class):
extern VNCviewerApp *pApp;

// -- other externs:
bool ParseDisplay(LPTSTR display, LPTSTR phost, int hostlen, int *port);

/////////////////////////////////////////////////////////////////////////////

#include "UltraVncAxGlobalConstructor.h"

#include "Log.h"
extern Log vnclog;


//
// Implementation:
//

CUltraVncAxGlobalConstructor:: ~ CUltraVncAxGlobalConstructor ()
{
	// free.
	if ( pApp )
		delete pApp;
	pApp = NULL;

	vnclog.Print(1, _T("Delete CUltraVncAxGlobalConstructor object 0x%lx"), (DWORD)this);
}

VOID CUltraVncAxGlobalConstructor::Construct ( HINSTANCE hInst )
{
	CCriticalSection::scope			__access__( m_csConstructAccess );

	// construct app object.
	if ( pApp == NULL )
	{
		CHAR*			pszCmdLine = "/nostatus /notoolbar";

		// create the app obj...
		WinMain( hInst, NULL, pszCmdLine, 0 );
		pApp = new VNCviewerApp32( hInst, pszCmdLine );
	}
}

BOOL CUltraVncAxGlobalConstructor::NewConnection ( CONST CHAR* pszHost,  
	CONST CHAR* pszUser, CONST CHAR* pszPassword, CONST CHAR* pszProxy,
	HWND rootHwnd, HWND* pHwndAppFrame)
{
	INT				iPort = 0;

	// parse host name.
	CHAR			host[ 256 ] = "";
	if ( ! ::ParseDisplay( const_cast<CHAR*>( pszHost ), host, 255, & iPort ) )
		return FALSE;


	// start the new connection.
	pApp->NewConnection ( host, iPort, (CHAR*)pszUser, (CHAR*)pszPassword, (CHAR*)pszProxy, rootHwnd, pHwndAppFrame);

	// return.
	return TRUE;
}

VOID CUltraVncAxGlobalConstructor::ProcessKeyEvent( HWND hwnd, WPARAM wParam, LPARAM lParam )
{
	// call the implementation.
	ClientConnection*		pConn = (ClientConnection*) ::GetWindowLong( hwnd, GWL_USERDATA );
	if ( pConn->m_running )
		pConn->ProcessKeyEvent( (int) wParam, (DWORD) lParam );
}

charstring CUltraVncAxGlobalConstructor::ExecuteCommand( HWND hwnd, charstring& csCmdText, BOOL& bIsErr )
{
	bIsErr = FALSE;
	charstring			csRetVal = "Command ok.";

	::TrimAtBothSides( csCmdText );

	// which command ?

	ClientConnection*		pConn = (ClientConnection*) ::GetWindowLong( hwnd, GWL_USERDATA );
	if ( pConn->m_running )
	{
		if ( ::stricmp( csCmdText.c_str (), "filetransfer" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_FILETRANSFER, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "disableremoteim" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_DINPUT, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "enableremoteim" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_INPUT, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "requestrefresh" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_REQUEST_REFRESH, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "fullcolors" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_MAXCOLORS, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "256colors" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_256COLORS, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendctrlaltdel" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_CTLALTDEL, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendstart" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_CTLESC, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendctrldown" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_CTLDOWN, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendctrlup" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_CTLUP, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendaltdown" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_ALTDOWN, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "sendaltup" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, ID_CONN_ALTUP, 0 );
		else if ( ::stricmp( csCmdText.c_str (), "about" ) == 0 )
			::SendMessage( hwnd, WM_SYSCOMMAND, IDD_APP_ABOUT, 0 );
		else
		{
			// return an error.
			csRetVal = "Invalid command.";
			bIsErr = TRUE;
		}
	}
	else
	{
		// return an error.
		csRetVal = "Client not connected.";
		bIsErr = TRUE;
	}

	// return.
	return csRetVal;
}

/////////////////////////////////////////////////////////////////////////////
