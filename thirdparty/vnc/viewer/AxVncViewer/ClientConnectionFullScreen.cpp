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
// whence you received this file, check http://www.uk.research.att.com/vnc or 
// contact the authors on vnc@uk.research.att.com for information on obtaining it.
//
// Many thanks to Greg Hewgill <greg@hewgill.com> for providing the basis for 
// the full-screen mode.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"
#include "FullScreenTitleBar.h" //Added by: Lars Werner (http://lars.werner.no)

// Parameters for scrolling in full screen mode
#define BUMPSCROLLBORDER 8
#define BUMPSCROLLAMOUNTX 32
#define BUMPSCROLLAMOUNTY 8
extern char sz_J1[128];
extern char sz_J2[64];

bool ClientConnection::InFullScreenMode() 
{
	return m_opts.m_FullScreen; 
};

// You can explicitly change mode by calling this
void ClientConnection::SetFullScreenMode(bool enable)
{
	if (m_opts.m_FullScreen==true) skipprompt2=true;
	else skipprompt2=false;
	m_opts.m_FullScreen = enable;
	RealiseFullScreenMode();

	// Modif sf@2002 - v1.1.0 - In case of server scaling
	// Clear the Window (in black)
    if (m_opts.m_nServerScale > 1)
	{
		RECT winrect;
		GetWindowRect(m_hwndMain, &winrect);
		int winwidth = winrect.right - winrect.left;
		int winheight = winrect.bottom - winrect.top;
		ObjectSelector b(m_hBitmapDC, m_hBitmap);
		PaletteSelector p(m_hBitmapDC, m_hPalette);
		RECT rect;
		SetRect(&rect, 0,0, winwidth, winheight);
		COLORREF bgcol = RGB(0x0, 0x0, 0x0);
		FillSolidRect(&rect, bgcol);
		// Update the whole screen 
		SendFullFramebufferUpdateRequest();
	}
}

// If the options have been changed other than by calling 
// SetFullScreenMode, you need to call this to make it happen.
void ClientConnection::RealiseFullScreenMode()
{
	LONG style = GetWindowLong(m_hwndMain, GWL_STYLE);
	if (m_opts.m_FullScreen) {

		// A bit crude here - we can skip the prompt on a registry setting.
		// We'll do this properly later.
		HKEY hRegKey;
		DWORD skipprompt = 0;
		if ( RegCreateKey(HKEY_CURRENT_USER, SETTINGS_KEY_NAME, &hRegKey)  != ERROR_SUCCESS ) {
	        hRegKey = NULL;
		} else {
			DWORD skippromptsize = sizeof(skipprompt);
			DWORD valtype;	
			if ( RegQueryValueEx( hRegKey,  "SkipFullScreenPrompt", NULL, &valtype, 
				(LPBYTE) &skipprompt, &skippromptsize) != ERROR_SUCCESS) {
				skipprompt = 0;
			}
			RegCloseKey(hRegKey);
		}
		
		skipprompt = 1; //sf@2004 - This prompt isn't needed any more now that we have
						// the fullscreen title bar :) Thanks Lars !
		if (!skipprompt && !skipprompt2)
			MessageBox(m_hwndMain, 
				sz_J1,
				sz_J2,
				MB_OK | MB_ICONINFORMATION | MB_TOPMOST | MB_SETFOREGROUND);

		/* Does not work yet
		// Used by VNCon - Copyright (C) 2001-2003 - Alastair Burr
		//ShowWindow(m_hwnd, SW_HIDE);
		*/
		ShowWindow(m_hwndMain, SW_MAXIMIZE);

		style = GetWindowLong(m_hwndMain, GWL_STYLE);
		style &= ~(WS_DLGFRAME | WS_THICKFRAME);
		SetWindowLong(m_hwndMain, GWL_STYLE, style);
		/* Does not work yet
		// Used by VNCon - Copyright (C) 2001-2003 - Alastair Burr
		// int cx = this->m_hScrollMax;
		// int cy = this->m_vScrollMax;
		*/
		int cx = GetSystemMetrics(SM_CXSCREEN);
		int cy = GetSystemMetrics(SM_CYSCREEN);
		SetWindowPos(m_hwndMain, HWND_TOPMOST, -1, -1, cx+3, cy+3, SWP_FRAMECHANGED);
		CheckMenuItem(GetSystemMenu(m_hwndMain, FALSE), ID_FULLSCREEN, MF_BYCOMMAND|MF_CHECKED);
		if (m_opts.m_ShowToolbar)
		SetWindowPos(m_hwnd, m_hwndTBwin,0,m_TBr.bottom,m_winwidth, m_winheight, SWP_SHOWWINDOW);
		else 
		{
			SetWindowPos(m_hwnd, m_hwndTBwin,0,0,cx+3, cy+3, SWP_SHOWWINDOW);
			SetWindowPos(m_hwndTBwin, NULL ,0,0,0, 0, SWP_HIDEWINDOW);
		}

		TitleBar.DisplayWindow(TRUE, TRUE); //Added by: Lars Werner (http://lars.werner.no)
 		TitleBar.SetText(m_desktopName); //Added by: Lars Werner (http://lars.werner.no)

	} else {
		ShowWindow(m_hwndMain, SW_NORMAL);
		style = GetWindowLong(m_hwndMain, GWL_STYLE);
#ifndef _ULTRAVNCAX_
		style |= WS_DLGFRAME | WS_THICKFRAME;
#endif
		SetWindowLong(m_hwndMain, GWL_STYLE, style);
		SetWindowPos(m_hwndMain, HWND_NOTOPMOST, 0,0,100,100, SWP_NOMOVE | SWP_NOSIZE | SWP_FRAMECHANGED); //Modified by: Lars Werner (http://lars.werner.no) - Reason: Bugfix, The framework got invisible after moving, so a NCCALCSIZE needed to be called!
		CheckMenuItem(GetSystemMenu(m_hwndMain, FALSE), ID_FULLSCREEN, MF_BYCOMMAND|MF_UNCHECKED);

		TitleBar.DisplayWindow(FALSE, TRUE); //Added by: Lars Werner (http://lars.werner.no)

		if (m_hwndStatus)::RedrawWindow(m_hwndStatus, NULL,NULL,TRUE); //Added by: Lars Werner (http://lars.werner.no) - Reason: The status window is not getting redrawn after a resize.
	}
}

bool ClientConnection::BumpScroll(int x, int y)
{
	int dx = 0;
	int dy = 0;
	int rightborder = GetSystemMetrics(SM_CXSCREEN)-BUMPSCROLLBORDER;
	int bottomborder = GetSystemMetrics(SM_CYSCREEN)-BUMPSCROLLBORDER-(m_TBr.bottom - m_TBr.top);
	if (x < BUMPSCROLLBORDER)
		dx = -BUMPSCROLLAMOUNTX * m_opts.m_scale_num / m_opts.m_scale_den;
	if (x >= rightborder)
		dx = +BUMPSCROLLAMOUNTX * m_opts.m_scale_num / m_opts.m_scale_den;;
	if (y < BUMPSCROLLBORDER)
		dy = -BUMPSCROLLAMOUNTY * m_opts.m_scale_num / m_opts.m_scale_den;;
	if (y >= bottomborder)
		dy = +BUMPSCROLLAMOUNTY * m_opts.m_scale_num / m_opts.m_scale_den;;
	if (dx || dy) {
		if (ScrollScreen(dx,dy)) {
			// If we haven't physically moved the cursor, artificially
			// generate another mouse event so we keep scrolling.
			POINT p;
			GetCursorPos(&p);
			if (p.x == x && p.y == y)
				SetCursorPos(x,y);
			return true;
		} 
	}
	return false;
}
