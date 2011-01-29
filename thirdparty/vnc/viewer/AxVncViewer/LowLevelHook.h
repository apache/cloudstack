//  TODO: add Copyright?
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

// This is the source for the low-level keyboard hook, which allows intercepting and sending
// special keys (such as ALT,CTRL, ALT+TAB, etc) to the VNCServer side.
// written by Assaf Gordon (Assaf@mazleg.com), 10/9/2003


#pragma once

#define WINVER 0x0400
#define _WIN32_WINNT 0x0400
#include <windows.h>

class LowLevelHook
{
public:
        static BOOL Initialize(HWND hwndMain);
        static BOOL Release();

private:
        static BOOL GetScrollLockState() ;

        static LRESULT CALLBACK VncLowLevelKbHookProc(INT nCode, WPARAM wParam, LPARAM lParam);

        static HWND g_hwndVNCViewer;
        static DWORD g_VncProcessID;
        static BOOL  g_fHookActive;
        static BOOL  g_fGlobalScrollLock;
        static HHOOK g_HookID;
};