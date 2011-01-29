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


// Daemon.cpp: implementation of the Daemon class.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "Daemon.h"
#include "Exception.h"
//#include "ClientConnection.h"
#include "AboutBox.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////
#define DAEMON_CLASS_NAME "VNCviewer Daemon"
extern char sz_I1[64];
extern char sz_I2[64];
extern char sz_I3[64];

Daemon::Daemon(int port)
{

	// Create a dummy window
	WNDCLASSEX wndclass;

	wndclass.cbSize			= sizeof(wndclass);
	wndclass.style			= CS_HREDRAW | CS_VREDRAW | CS_DBLCLKS;
	wndclass.lpfnWndProc	= Daemon::WndProc;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= pApp->m_instance;
	wndclass.hIcon			= LoadIcon(NULL, IDI_APPLICATION);
	wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wndclass.hbrBackground	= (HBRUSH) GetStockObject(WHITE_BRUSH);
	wndclass.lpszMenuName	= (const char *) NULL;
	wndclass.lpszClassName	= DAEMON_CLASS_NAME;
	wndclass.hIconSm		= LoadIcon(NULL, IDI_APPLICATION);

	RegisterClassEx(&wndclass);

	m_hwnd = CreateWindow(DAEMON_CLASS_NAME,
				DAEMON_CLASS_NAME,
				WS_OVERLAPPEDWINDOW,
				CW_USEDEFAULT,
				CW_USEDEFAULT,
				200, 200,
				NULL,
				NULL,
				pApp->m_instance,
				NULL);
	
	// record which client created this window
	SetWindowLong(m_hwnd, GWL_USERDATA, (LONG) this);

	// Load a popup menu
	m_hmenu = LoadMenu(pApp->m_instance, MAKEINTRESOURCE(IDR_TRAYMENU));

	// sf@2003 - Store Port number for systray display
	m_nPort = port;

	// Create a listening socket
    struct sockaddr_in addr;

    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = INADDR_ANY;

    m_sock = socket(AF_INET, SOCK_STREAM, 0);
	if (!m_sock) throw WarningException(sz_I1);
    
	try {
		int one = 1, res = 0;
		//res = setsockopt(m_sock, SOL_SOCKET, SO_REUSEADDR, (const char *) &one, sizeof(one));
		//if (res == SOCKET_ERROR) 
		//  throw WarningException("Error setting Daemon socket options");
		
		res = bind(m_sock, (struct sockaddr *)&addr, sizeof(addr));
		if (res == SOCKET_ERROR)
			throw WarningException(sz_I2);
		
		res = listen(m_sock, 5);
		if (res == SOCKET_ERROR)
			throw WarningException(sz_I3);
	} catch (...) {
		closesocket(m_sock);
		m_sock = 0;
		throw;
	}
	
	// Send a message to specified window on an incoming connection
	WSAAsyncSelect (m_sock,  m_hwnd,  WM_SOCKEVENT, FD_ACCEPT);

	// Create the tray icon
	AddTrayIcon();
	
	// A timer checks that the tray icon is intact
	m_timer = SetTimer( m_hwnd, IDT_TRAYTIMER,  15000, NULL);
}

void Daemon::AddTrayIcon() {
	vnclog.Print(4, _T("Adding tray icon\n"));
	SendTrayMsg(NIM_ADD);
}

void Daemon::CheckTrayIcon() {
	vnclog.Print(8, _T("Checking tray icon\n"));
	if (!SendTrayMsg(NIM_MODIFY)) {
		vnclog.Print(4, _T("Tray icon not there - reinstalling\n"));
		AddTrayIcon();
	};
}

void Daemon::RemoveTrayIcon() {
	vnclog.Print(4, _T("Deleting tray icon\n"));
	SendTrayMsg(NIM_DELETE);
}

bool Daemon::SendTrayMsg(DWORD msg)
{
	m_nid.hWnd = m_hwnd;
	m_nid.cbSize = sizeof(m_nid);
	m_nid.uID = IDR_TRAY;	// never changes after construction

	// Phil Money @ Advantig, LLC 7-9-2005
if (GetListenMode()){ 
        m_nid.hIcon = LoadIcon(pApp->m_instance, MAKEINTRESOURCE(IDR_TRAY));
	}else{ 
        m_nid.hIcon = LoadIcon(pApp->m_instance, MAKEINTRESOURCE(IDR_TRAY_DISABLED)); // Phil Money @ Advantig, LLC 7-9-2005
	} 
  	

	m_nid.uFlags = NIF_ICON | NIF_MESSAGE;
	m_nid.uCallbackMessage = WM_TRAYNOTIFY;
	m_nid.szTip[0] = '\0';
	// Use resource string as tip if there is one
	if (LoadString(pApp->m_instance, IDR_TRAY, m_nid.szTip, sizeof(m_nid.szTip))) {
		m_nid.uFlags |= NIF_TIP;
	}

	// sf@2003 - Add the port number to the tip
	char szTmp[16];
	sprintf(szTmp, " - Port:%ld", m_nPort);
	strcat(m_nid.szTip, szTmp);

	return (bool) (Shell_NotifyIcon(msg, &m_nid) != 0);
}

// Process window messages
LRESULT CALLBACK Daemon::WndProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. We have stored a pseudo-this in the window user data, 
	// though.
	Daemon *_this = (Daemon *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (iMsg) {

	case WM_CREATE:
		{
			return 0;
		}

	case WM_SOCKEVENT:
		{
			assert(HIWORD(lParam) == 0);
			// A new socket created by accept might send messages to
			// this procedure. We can ignore them.
			if(wParam != _this->m_sock) {
				return 0;
			}

			switch(lParam) {
			case FD_ACCEPT:
				{
					SOCKET hNewSock;
					hNewSock = accept(_this->m_sock, NULL, NULL);
					WSAAsyncSelect(hNewSock, hwnd, 0, 0);
					unsigned long nbarg = 0;
					ioctlsocket(hNewSock, FIONBIO, &nbarg);
					// Phil Money @ Advantig, LLC 7-9-2005
					if (ListenMode){ 

						pApp->NewConnection(hNewSock);

					}else{ 
						closesocket(hNewSock); 
					} 
					
					break;
				}
			case FD_READ:
				{
					unsigned long numbytes;
					ioctlsocket(_this->m_sock, FIONREAD, &numbytes);
					recv(_this->m_sock, _this->netbuf, numbytes, 0);
					break;
				}
			case FD_CLOSE:
				vnclog.Print(5, _T("Daemon connection closed\n"));
				DestroyWindow(hwnd);
				break;
			}
			
			return 0;
		}
	case WM_COMMAND:
		switch (LOWORD(wParam)) {
		case ID_NEWCONN:
			pApp->NewConnection();
			break;
		case IDC_OPTIONBUTTON:
			pApp->m_options.DoDialog();
			break;
		// Phil Money @ Advantig, LLC 7-9-2005
		case ID_LISTEN_MODE:
			if (GetListenMode()){ 
				SetListenMode(false); 
			}else{ 
				SetListenMode(true); 
			} 
			_this->CheckTrayIcon(); 
			break;
		case ID_CLOSEAPP:
			vnclog.Print(1, _T("PostQuitMessage in handling ID_CLOSEAPP\n"));

			PostQuitMessage(0);
			//DestroyWindow(hwnd);
			break;
		case ID_CLOSEDAEMON:
			//if (_this->m_sock!=NULL) shutdown(_this->m_sock, SD_BOTH);
			//if (_this->m_sock!=NULL) closesocket(_this->m_sock);
			//_this->m_sock=NULL;
			break;
		case IDD_APP_ABOUT:
			ShowAboutBox();
			break;
		}
		return 0;
	case WM_TRAYNOTIFY:
		{
			HMENU hSubMenu = GetSubMenu(_this->m_hmenu, 0);
			if (lParam==WM_LBUTTONDBLCLK) {
				// double click: execute first menu item
				::SendMessage(_this->m_nid.hWnd, WM_COMMAND, 
					GetMenuItemID(hSubMenu, 0), 0);
			} else if (lParam==WM_RBUTTONUP) {
				if (hSubMenu == NULL) { 
					vnclog.Print(2, _T("No systray submenu\n"));
					return 0;
				}
				// Make first menu item the default (bold font)
				::SetMenuDefaultItem(hSubMenu, 0, TRUE);
				/*if (_this->m_sock==NULL){
					MENUITEMINFO pItem;
					ZeroMemory( &pItem, sizeof(pItem) );
					pItem.cbSize		= sizeof(pItem);
					pItem.fMask			= MIIM_TYPE;
					pItem.fType			= MFT_STRING;
					pItem.dwTypeData	= (LPTSTR)"Deamon closed !!!";

					SetMenuItemInfo( hSubMenu, 5, TRUE, &pItem );
					::SetMenuDefaultItem(hSubMenu, 5, TRUE);
				}*/
				
				// Display the menu at the current mouse location. There's a "bug"
				// (Microsoft calls it a feature) in Windows 95 that requires calling
				// SetForegroundWindow. To find out more, search for Q135788 in MSDN.
				//
				POINT mouse;
				GetCursorPos(&mouse);
				::SetForegroundWindow(_this->m_nid.hWnd);
				::TrackPopupMenu(hSubMenu, 0, mouse.x, mouse.y, 0,
					_this->m_nid.hWnd, NULL);
				
			} 
			return 0;
		}
	case WM_TIMER:
		_this->CheckTrayIcon();
		return 0;
	case WM_DESTROY:
		PostQuitMessage(0);
		return 0;
	}
	
	return DefWindowProc(hwnd, iMsg, wParam, lParam);
}

Daemon::~Daemon()
{
	KillTimer(m_hwnd, m_timer);
	RemoveTrayIcon();
	DestroyMenu(m_hmenu);
	if (m_sock!=NULL) shutdown(m_sock, SD_BOTH);
	if (m_sock!=NULL) closesocket(m_sock);
}


// Phil Money @ Advantig, LLC 7-9-2005
void 
SetListenMode(bool listenmode) 
{ 
	ListenMode = listenmode; 
	return; 
} 

bool 
GetListenMode() 
{ 
	return(ListenMode); 
} 
