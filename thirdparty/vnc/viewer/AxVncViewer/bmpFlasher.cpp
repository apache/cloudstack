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


// BmpFlasher.cpp: implementation of the BmpFlasher class.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "BmpFlasher.h"
#include "Exception.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

#define BmpFlasher_CLASS_NAME "VNCviewer BmpFlasher"
#define FLASHFONTHEIGHT 80
int m_nTimerID;
HBITMAP hbmBkGnd = NULL;

HBITMAP
    DoGetBkGndBitmap2(IN CONST UINT uBmpResId  )
    {
        if (NULL == hbmBkGnd)
        {
			/*char szFileName[MAX_PATH];
			if (GetModuleFileName(NULL, szFileName, MAX_PATH))
				{
				char* p = strrchr(szFileName, '\\');
					if (p == NULL) return false;
					*p = '\0';
				strcat (szFileName,"\\flash.bmp");
			}
			hbmBkGnd = (HBITMAP)LoadImage( NULL, szFileName, IMAGE_BITMAP, 0, 0,
               LR_CREATEDIBSECTION | LR_DEFAULTSIZE | LR_LOADFROMFILE );*/
				hbmBkGnd = (HBITMAP)LoadImage(
                GetModuleHandle(NULL), MAKEINTRESOURCE(uBmpResId),
                    IMAGE_BITMAP, 0, 0, LR_CREATEDIBSECTION);

            if (NULL == hbmBkGnd)
                hbmBkGnd = (HBITMAP)-1;
        }
        return (hbmBkGnd == (HBITMAP)-1)
            ? NULL : hbmBkGnd;
    }
BOOL
    DoSDKEraseBkGnd2(
        IN CONST HDC hDC,
        IN CONST COLORREF crBkGndFill
      )
    {
        DoGetBkGndBitmap2(IDB_BITMAP11);
        if (hDC && hbmBkGnd)
        {
            RECT rc;
            if ((ERROR != GetClipBox(hDC, &rc)) && !IsRectEmpty(&rc))
            {
                HDC hdcMem = CreateCompatibleDC(hDC);
                if (hdcMem)
                {
                    HBRUSH hbrBkGnd = CreateSolidBrush(crBkGndFill);
                    if (hbrBkGnd)
                    {
                        HGDIOBJ hbrOld = SelectObject(hDC, hbrBkGnd);
                        if (hbrOld)
                        {
                            SIZE size = {
                                (rc.right-rc.left), (rc.bottom-rc.top)
                            };

                            if (PatBlt(hDC, rc.left, rc.top, size.cx, size.cy, PATCOPY))
                            {
                                HGDIOBJ hbmOld = SelectObject(hdcMem, hbmBkGnd);
                                if (hbmOld)
                                {
                                    BitBlt(hDC, rc.left, rc.top, size.cx, size.cy,
                                        hdcMem, rc.left, rc.top, SRCCOPY);
                                    SelectObject(hdcMem, hbmOld);
                                }
                            }
                            SelectObject(hDC, hbrOld);
                        }
                        DeleteObject(hbrBkGnd);
                    }
                    DeleteDC(hdcMem);
                }
            }
        }
        return TRUE;
    }

BmpFlasher::BmpFlasher()
{
	// Create a dummy window.  We don't use it for anything except
	// receiving socket events, so a seperate listening thread would
	// probably be easier!

	WNDCLASSEX wndclass;

	wndclass.cbSize			= sizeof(wndclass);
	wndclass.style			= CS_HREDRAW | CS_VREDRAW;
	wndclass.lpfnWndProc	= BmpFlasher::WndProc;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= pApp->m_instance;
	wndclass.hIcon			= LoadIcon(NULL, IDI_APPLICATION);
	wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wndclass.hbrBackground	= (HBRUSH) GetStockObject(WHITE_BRUSH);
	wndclass.lpszMenuName	= (const char *) NULL;
	wndclass.lpszClassName	= BmpFlasher_CLASS_NAME;
	wndclass.hIconSm		= LoadIcon(NULL, IDI_APPLICATION);

	RegisterClassEx(&wndclass);

	m_hwnd = CreateWindow(BmpFlasher_CLASS_NAME,
				BmpFlasher_CLASS_NAME,
				WS_OVERLAPPED | WS_THICKFRAME,
				CW_USEDEFAULT,
				CW_USEDEFAULT,
				300, 200,
				NULL,
				NULL,
				pApp->m_instance,
				NULL);
	SetWindowLong(m_hwnd, GWL_USERDATA, (LONG) this);
	LONG style = GetWindowLong(m_hwnd, GWL_STYLE);
	style = GetWindowLong(m_hwnd, GWL_STYLE);
	style &= ~(WS_DLGFRAME | WS_THICKFRAME);
	SetWindowLong(m_hwnd, GWL_STYLE, style);
	SetWindowPos(m_hwnd, HWND_TOPMOST, -1, -1,
		300, 200, SWP_FRAMECHANGED);
	CentreWindow(m_hwnd);

}

// Process window messages
LRESULT CALLBACK BmpFlasher::WndProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. We use Allen Hadden's (ahadden@taratec.com) suggestion 
	// from a newsgroup to get the pseudo-this.
	BmpFlasher *_this = (BmpFlasher *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (iMsg) {
	
	case WM_ERASEBKGND:
            {
                DoSDKEraseBkGnd2((HDC)wParam, RGB(255,0,0));
				return true;
            }
	case WM_CTLCOLORSTATIC:
			{
				SetBkMode((HDC) wParam, TRANSPARENT);
				return (DWORD) GetStockObject(NULL_BRUSH);
			}
	case WM_TIMER:
			DestroyWindow(hwnd);
			break;
	case WM_CREATE:
		CentreWindow(hwnd);
		SetForegroundWindow(hwnd);
		m_nTimerID = SetTimer(hwnd,1, 200, 0);
		return 0;
	case WM_CLOSE:
		KillTimer(hwnd,m_nTimerID);
		break;
	case WM_USER:
		switch (lParam)
		{
		case WM_LBUTTONDBLCLK:
		case WM_LBUTTONDOWN:
		case WM_RBUTTONDOWN:
		case WM_KEYDOWN:
		case WM_MBUTTONDOWN:
		DestroyWindow(hwnd);
		KillTimer(hwnd,m_nTimerID);
		break;
		}
	}
	
	return DefWindowProc(hwnd, iMsg, wParam, lParam);
}

void
BmpFlasher::Killflash()
{
	if (m_hwnd) DestroyWindow(m_hwnd);
	m_hwnd=NULL;
}

BmpFlasher::~BmpFlasher()
{
	if (m_hwnd) DestroyWindow(m_hwnd);
	m_hwnd=NULL;
	if (hbmBkGnd) DeleteObject(hbmBkGnd);
	hbmBkGnd=NULL;

}

