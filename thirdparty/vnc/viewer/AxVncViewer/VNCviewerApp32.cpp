//  Copyright (C) 1999 AT&T Laboratories Cambridge. All Rights Reserved.
//
//  This file is part of the VNC system.
//
//  The VNC system is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//
// If the source code for the VNC system is not available from the place 
// whence you received this file, check http://www.uk.research.att.com/vnc or contact
// the authors on vnc@uk.research.att.com for information on obtaining it.

#include "VNCviewerApp32.h"
#include "vncviewer.h"
#include "Exception.h"
extern char sz_A1[64];
extern char sz_A2[64];
extern char sz_A3[64];
extern char sz_A4[64];
extern char sz_A5[64];

// --------------------------------------------------------------------------
VNCviewerApp32::VNCviewerApp32(HINSTANCE hInstance, PSTR szCmdLine) :
	VNCviewerApp(hInstance, szCmdLine)
{

	m_pdaemon = NULL;
	m_pflasher = NULL;
//HKL hkl = LoadKeyboardLayout(  "00000813", 
//			KLF_ACTIVATE | KLF_REPLACELANG | KLF_REORDER  );
	// Load a requested keyboard layout
	if (m_options.m_kbdSpecified) {
		HKL hkl = LoadKeyboardLayout(  m_options.m_kbdname, 
			KLF_ACTIVATE | KLF_REPLACELANG | KLF_REORDER  );
		if (hkl == NULL) {
			MessageBox(NULL, sz_A1, 
				sz_A2, MB_OK | MB_ICONSTOP);
			exit(1);
		}
	}

	// Start listening daemons if requested
	
	if (m_options.m_listening) {
		vnclog.Print(3, _T("In listening mode - staring daemons\n"));
		
		try {
			// m_pflasher = new Flasher(FLASH_PORT_OFFSET);
			m_pflasher = new Flasher(m_options.m_listenPort + FLASH_PORT_OFFSET - INCOMING_PORT_OFFSET);
			m_pdaemon = new Daemon(m_options.m_listenPort);
		} catch (WarningException &e) {
			char msg[1024];
			sprintf(msg,"%s (%s)\n\r%s",sz_A3,
				e.m_info, sz_A4);
			MessageBox(NULL, msg, sz_A5, MB_OK | MB_ICONSTOP);
			exit(1);
		}
		
	} 

	RegisterSounds();
	
}

	
	// These should maintain a list of connections.

void VNCviewerApp32::NewConnection() {
	ClientConnection *pcc = new ClientConnection(this);
	try {
		pcc->Run();
	} catch (Exception &e) {
		e.Report();	
		delete pcc;
	} 
}

#ifdef _ULTRAVNCAX_

typedef struct _STimeoutThreadState
{
	_STimeoutThreadState( ClientConnection* _connection, BOOL* _pbStateStructValid )
	{
		connected = FALSE;
		connection = _connection;
		pbStateStructValid = _pbStateStructValid;
	}

	BOOL				connected;
	ClientConnection*	connection;
	BOOL*				pbStateStructValid;

} STimeoutThreadState;

static DWORD WINAPI TimeoutThread( LPVOID lpParameter )
{
	STimeoutThreadState*		pState = (STimeoutThreadState*) lpParameter;

	// wait.
	::Sleep( 10 * 1000 );

	// we have to close the connection ?
	if ( pState->connected == FALSE )
		pState->connection->KillThread ();

	// return.
	if ( pState->pbStateStructValid )
		* pState->pbStateStructValid = FALSE;
	delete pState;
	return 0;
}

#endif

void VNCviewerApp32::NewConnection(TCHAR *host, int port, TCHAR* pszUser, TCHAR* pszPassword, TCHAR* pszProxy,
								   HWND rootHwnd, HWND* pHwndAppFrame) 
{
	ClientConnection *pcc = new ClientConnection(this, host, port, pszUser, pszPassword, pszProxy, rootHwnd, pHwndAppFrame);

#ifdef _ULTRAVNCAX_

	//
	// This is a very unstable way to do time-out check, it caused the IE crash if
	// we close the window too quickly!
	//
	//	- Kelven Yang 12/11/08
	//
/*
	BOOL						bStateStructValid = TRUE;

	STimeoutThreadState*		pState = new STimeoutThreadState( pcc, & bStateStructValid );
	DWORD						dwID;

	HANDLE		h = ::CreateThread( NULL, 0, & TimeoutThread, pState, 0, & dwID );

	if ( h )
		::CloseHandle( h );
*/

#endif

	try {
		pcc->Run();

#ifdef _ULTRAVNCAX_
/*
		if ( bStateStructValid )
			pState->connected = TRUE;
*/
#endif

	} catch (Exception &e) {

#ifdef _ULTRAVNCAX_
/*
		if ( bStateStructValid )
			pState->connected = TRUE;
*/
#endif

		e.Report();	
		delete pcc;
	} 

#ifdef _ULTRAVNCAX_
/*
	if ( bStateStructValid )
		pState->pbStateStructValid = NULL;
*/
#endif
}

void VNCviewerApp32::NewConnection(SOCKET sock) {
	ClientConnection *pcc = new ClientConnection(this, sock);
	try {
		pcc->Run();
	} catch (Exception &e) {
		e.Report();	
		delete pcc;
	} 
}

// Register the Bell sound event

const char* BELL_APPL_KEY_NAME  = "AppEvents\\Schemes\\Apps\\VNCviewer";
const char* BELL_LABEL = "VNCviewerBell";

void VNCviewerApp32::RegisterSounds() {
	
	HKEY hBellKey;
	char keybuf[256];
	
	sprintf(keybuf, "AppEvents\\EventLabels\\%s", BELL_LABEL);
	// First create a label for it
	if ( RegCreateKey(HKEY_CURRENT_USER, keybuf, &hBellKey)  == ERROR_SUCCESS ) {
		RegSetValue(hBellKey, NULL, REG_SZ, "Bell", 0);
		RegCloseKey(hBellKey);
		
		// Then put the detail in the app-specific area
		
		if ( RegCreateKey(HKEY_CURRENT_USER, BELL_APPL_KEY_NAME, &hBellKey)  == ERROR_SUCCESS ) {
			
			sprintf(keybuf, "%s\\%s", BELL_APPL_KEY_NAME, BELL_LABEL);
			RegCreateKey(HKEY_CURRENT_USER, keybuf, &hBellKey);
			RegSetValue(hBellKey, NULL, REG_SZ, "Bell", 0);
			RegCloseKey(hBellKey);
			
			sprintf(keybuf, "%s\\%s\\.current", BELL_APPL_KEY_NAME, BELL_LABEL);
			if (RegOpenKey(HKEY_CURRENT_USER, keybuf, &hBellKey) != ERROR_SUCCESS) {
				RegCreateKey(HKEY_CURRENT_USER, keybuf, &hBellKey);
				RegSetValue(hBellKey, NULL, REG_SZ, "ding.wav", 0);
			}
			RegCloseKey(hBellKey);
			
			sprintf(keybuf, "%s\\%s\\.default", BELL_APPL_KEY_NAME, BELL_LABEL);
			if (RegOpenKey(HKEY_CURRENT_USER, keybuf, &hBellKey) != ERROR_SUCCESS) {
				RegCreateKey(HKEY_CURRENT_USER, keybuf, &hBellKey);
				RegSetValue(hBellKey, NULL, REG_SZ, "ding.wav", 0);
			}
			RegCloseKey(hBellKey);
		}
		
	} 
	
}


VNCviewerApp32::~VNCviewerApp32() {
	// We don't need to clean up pcc if the thread has been joined.
	if (m_pdaemon != NULL) delete m_pdaemon;
	if (m_pflasher != NULL) delete m_pflasher;
}
	
