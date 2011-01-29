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


// Flasher.cpp: implementation of the Flasher class.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "Flasher.h"
#include "Exception.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

#define FLASHER_CLASS_NAME "VNCviewer Flasher"
#define FLASHFONTHEIGHT 80

extern char sz_G1[64];
extern char sz_G2[64];
extern char sz_G3[64];

Flasher::Flasher(int port)
{
	// Create a dummy window.  We don't use it for anything except
	// receiving socket events, so a seperate listening thread would
	// probably be easier!

	WNDCLASSEX wndclass;

	wndclass.cbSize			= sizeof(wndclass);
	wndclass.style			= CS_HREDRAW | CS_VREDRAW;
	wndclass.lpfnWndProc	= Flasher::WndProc;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= pApp->m_instance;
	wndclass.hIcon			= LoadIcon(NULL, IDI_APPLICATION);
	wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wndclass.hbrBackground	= (HBRUSH) GetStockObject(WHITE_BRUSH);
	wndclass.lpszMenuName	= (const char *) NULL;
	wndclass.lpszClassName	= FLASHER_CLASS_NAME;
	wndclass.hIconSm		= LoadIcon(NULL, IDI_APPLICATION);

	RegisterClassEx(&wndclass);

	m_hwnd = CreateWindow(FLASHER_CLASS_NAME,
				FLASHER_CLASS_NAME,
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

	// Select a font for displaying user name
	LOGFONT lf;
	memset(&lf, 0, sizeof(lf));
	lf.lfHeight = FLASHFONTHEIGHT;
	lf.lfWeight = FW_BOLD;
	lf.lfItalic = 1;
	lf.lfPitchAndFamily = VARIABLE_PITCH | FF_SWISS;
	lf.lfFaceName[0] = '\0';
	m_hfont = CreateFontIndirect(&lf);
	if (m_hfont == NULL) {
		vnclog.Print(1, _T("FAILED TO SELECT FLASHER FONT!\n"));
	}

	// Create a listening socket
    struct sockaddr_in addr;

    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = INADDR_ANY;

    m_sock = socket(AF_INET, SOCK_STREAM, 0);
	if (!m_sock) throw WarningException(sz_G1);
    
	try {
		int one = 1, res = 0;

		// If we use the SO_REUSEADDR option then you can run multiple daemons
		// because the bind doesn't return an error.  Only one gets the accept,
		// but when that process dies it hands back to another.  This may or may
		// not be desirable.  We don't use it.

		//int res = setsockopt(m_sock, SOL_SOCKET, SO_REUSEADDR, (const char *) &one, sizeof(one));
		//if (res == SOCKET_ERROR) 
		//	throw WarningException("Error setting Flasher socket options");
		
		res = bind(m_sock, (struct sockaddr *)&addr, sizeof(addr));
		if (res == SOCKET_ERROR)
			throw WarningException(sz_G2);
		
		res = listen(m_sock, 5);
		if (res == SOCKET_ERROR)
			throw WarningException(sz_G3);
	} catch (...) {
		closesocket(m_sock);
		m_sock = 0;
		throw;
	}
	
	// Send a message to specified window on an incoming connection
	WSAAsyncSelect (m_sock,  m_hwnd,  WM_SOCKEVENT, FD_ACCEPT | FD_CLOSE);
}

// convert a lower-case ASCII char to a value 0-255
inline int scalechar(char c) {
	return ( ((c - 'a')+4) & 0x1f ) * 255 / 0x1f;
}

// We use this on each screen saver window running
BOOL CALLBACK KillScreenSaverFunc(HWND hwnd, LPARAM lParam)
{
	char buffer[256];

	// - ONLY try to close Screen-saver windows!!!
	if ((GetClassName(hwnd, buffer, 256) != 0) &&
		(strcmp(buffer, "WindowsScreenSaverClass") == 0))
		PostMessage(hwnd, WM_CLOSE, 0, 0);
	return TRUE;
}

// Process window messages
LRESULT CALLBACK Flasher::WndProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. We use Allen Hadden's (ahadden@taratec.com) suggestion 
	// from a newsgroup to get the pseudo-this.
	Flasher *_this = (Flasher *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (iMsg) {

	case WM_CREATE:
		return 0;

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
					char username[256];
					hNewSock = accept(_this->m_sock, NULL, NULL);
					// make it blocking
					WSAAsyncSelect(hNewSock, hwnd, 0, 0);
					u_long blk = 0;
					int res = ioctlsocket(hNewSock, FIONBIO, &blk);
					assert(res == 0);

					CloseScreenSaver();

					// Se if the server's sending a user name
					int namelen = recv(hNewSock, username, 250, 0);
					if (namelen >= 0) 
						username[namelen] = 0;
					vnclog.Print(2, _T("Flash for '%s'\n"), username);

					closesocket(hNewSock);


					// flash
					// Get a DC for the root window
					HDC hrootdc = ::GetDC(NULL);

					HBRUSH holdbrush = (HBRUSH) SelectObject(hrootdc, 
						(HBRUSH) GetStockObject(BLACK_BRUSH));
					
					// Find the size.
					RECT rect;
					GetClipBox(hrootdc, &rect);
					int barwidth = (rect.right - rect.left) / 10;
					int barheight = max( 
						(rect.bottom - rect.top) / 10,
						FLASHFONTHEIGHT);
					HFONT oldfont = (HFONT) SelectObject(hrootdc, _this->m_hfont);

					// Flash the screen
					::Beep(440,50);
										
					Rectangle(hrootdc, rect.left, rect.top, 
								  rect.right, barheight);
					Rectangle(hrootdc, rect.left,  rect.bottom-barheight, 
								  rect.right, rect.bottom);
					Rectangle(hrootdc, rect.left,  rect.top+barheight, 
								  barwidth, rect.bottom-barheight);
					Rectangle(hrootdc, rect.right-barwidth, rect.top+barheight, 
								  rect.right, rect.bottom-barheight);

					RECT topbar;
					SetRect(&topbar, rect.left, rect.top, 
								  rect.right, barheight);
					int i = 0;
					if (namelen > 0) {
						int oldmode = SetBkMode(hrootdc, TRANSPARENT);
						COLORREF oldcolor = SetTextColor(hrootdc, 
							RGB(scalechar(username[0])/(i+1), scalechar(username[1])/(i+1), scalechar(username[2])/(i+1)));
						DrawText(hrootdc, username, -1, &topbar, DT_CENTER | DT_VCENTER | DT_SINGLELINE);
						SetTextColor(hrootdc, oldcolor);
						SetBkMode(hrootdc, oldmode);
					}
					GdiFlush();
					SelectObject(hrootdc, holdbrush);
					::Sleep(1000);
						
					SelectObject(hrootdc, oldfont);
					InvalidateRect(0, &rect, TRUE);

					::ReleaseDC(NULL, hrootdc);
					break;
				}
			case FD_CLOSE:
				vnclog.Print(2, _T("Flasher connection closed\n"));
				DestroyWindow(hwnd);
				break;
			}
			
			return 0;
		}
	case WM_DESTROY:
		PostQuitMessage(0);
		return 0;
	}
	
	return DefWindowProc(hwnd, iMsg, wParam, lParam);
}

void Flasher::CloseScreenSaver() {
	// Which OS are we on?
	OSVERSIONINFO ovi;
	ovi.dwOSVersionInfoSize = sizeof(ovi);
	if (!GetVersionEx(&ovi)) return;

	switch (ovi.dwPlatformId) {
	case VER_PLATFORM_WIN32_WINDOWS:
		{
			// Windows 95
			HWND hsswnd = FindWindow ("WindowsScreenSaverClass", NULL);
			if (hsswnd != NULL)
				PostMessage(hsswnd, WM_CLOSE, 0, 0); 
			break;
		} 
	case VER_PLATFORM_WIN32_NT:
		{
			// Windows NT
			HDESK hdesk = OpenDesktop(
				TEXT("Screen-saver"),                       
				0,	FALSE,	DESKTOP_READOBJECTS | DESKTOP_WRITEOBJECTS);
			if (hdesk) {
                if (EnumDesktopWindows(hdesk, (WNDENUMPROC) KillScreenSaverFunc, 0)) {
				    CloseDesktop(hdesk);
                }
                Sleep(1000);
			}
		}
	}
}

Flasher::~Flasher()
{
	shutdown(m_sock, SD_BOTH);
	closesocket(m_sock);
	if (m_hfont != NULL) DeleteObject(m_hfont);
}

