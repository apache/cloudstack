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

#include "stdhdrs.h"
#include "vncviewer.h"
#include "VNCviewerApp.h"
#include "Exception.h"
extern char sz_A2[64];
extern char sz_B1[64];
extern char sz_B2[64];
extern char sz_B3[64];
extern HINSTANCE m_hInstResDLL;

// For WinCE Palm, you might want to use this for debugging, since it
// seems impossible to give the command some arguments.
// #define PALM_LOG 1

VNCviewerApp *pApp;

VNCviewerApp::VNCviewerApp(HINSTANCE hInstance, LPTSTR szCmdLine) {
	pApp = this;
	m_instance = m_hInstResDLL;

	// Read the command line
	m_options.SetFromCommandLine(szCmdLine);
	
	// Logging info
	vnclog.SetLevel(m_options.m_logLevel);
	if (m_options.m_logToConsole) {
		vnclog.SetMode(Log::ToConsole | Log::ToDebug);
	}
	if (m_options.m_logToFile) {
		vnclog.SetFile(m_options.m_logFilename, true);
	}
//vnclog.SetLevel(20);
//vnclog.SetMode(1);
//vnclog.SetFile(_T(".\vncviewer.log"));
#ifdef PALM_LOG
	// Hack override for WinCE Palm developers who can't give
	// options to commands, not even via shortcuts.
	vnclog.SetLevel(20);
 	vnclog.SetFile(_T("\\Temp\\log"));
#endif
 	
	// Clear connection list
	for (int i = 0; i < MAX_CONNECTIONS; i++)
		m_clilist[i] = NULL;

	// Initialise winsock
	WORD wVersionRequested = MAKEWORD(2, 0);
	WSADATA wsaData;
	if (WSAStartup(wVersionRequested, &wsaData) != 0) {
		MessageBox(NULL, sz_B1, sz_A2, MB_OK | MB_ICONSTOP);
		PostQuitMessage(1);
	}
	vnclog.Print(3, _T("Started and Winsock (v %d) initialised\n"), wsaData.wVersion);
}


// The list of clients should fill up from the start and have NULLs
// afterwards.  If the first entry is a NULL, the list is empty.

void VNCviewerApp::RegisterConnection(ClientConnection *pConn) {
	omni_mutex_lock l(m_clilistMutex);
	int i;
	for (i = 0; i < MAX_CONNECTIONS; i++) {
		if (m_clilist[i] == NULL) {
			m_clilist[i] = pConn;
			vnclog.Print(4,_T("Registered connection with app\n"));
			return;
		}
	}
	// If we've got here, something is wrong.
	vnclog.Print(-1, _T("Client list overflow!\n"));
	MessageBox(NULL, sz_B2, sz_B3,
		MB_OK | MB_ICONSTOP);
	PostQuitMessage(1);

}

void VNCviewerApp::DeregisterConnection(ClientConnection *pConn) {
	omni_mutex_lock l(m_clilistMutex);
	int i;
	for (i = 0; i < MAX_CONNECTIONS; i++) {
		if (m_clilist[i] == pConn) {
			// shuffle everything above downwards
			for (int j = i; m_clilist[j] &&	j < MAX_CONNECTIONS-1 ; j++)
				m_clilist[j] = m_clilist[j+1];
			m_clilist[MAX_CONNECTIONS-1] = NULL;
			vnclog.Print(4,_T("Deregistered connection from app\n"));

			// No clients left? then we should finish, unless we're in
			// listening mode.
#ifndef _ULTRAVNCAX_
			if ((m_clilist[0] == NULL) && (!pApp->m_options.m_listening))
				PostQuitMessage(0);
#endif

			return;
		}
	}
	// If we've got here, something is wrong.
	vnclog.Print(-1, _T("Client not found for deregistering!\n"));
	PostQuitMessage(1);
}

// ----------------------------------------------


VNCviewerApp::~VNCviewerApp() {
		
	
	// Clean up winsock
	WSACleanup();
	
	vnclog.Print(2, _T("VNC viewer closing down\n"));

}
