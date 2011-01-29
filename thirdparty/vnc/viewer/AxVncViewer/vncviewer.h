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

#ifndef VNCVIEWER_H__
#define VNCVIEWER_H__

#pragma once

#include "res\resource.h"
#include "VNCviewerApp.h"
#include "Log.h"
#include "AccelKeys.h"

#define WM_SOCKEVENT WM_USER+1
#define WM_TRAYNOTIFY WM_SOCKEVENT+1
#define WM_REGIONUPDATED WM_TRAYNOTIFY+1

// The Application
extern VNCviewerApp *pApp;

// Global logger - may be used by anything
extern Log vnclog;
extern AccelKeys TheAccelKeys;

// Display given window in centre of screen
void CentreWindow(HWND hwnd);

// Convert "host:display" into host and port
// Returns true if valid.
bool ParseDisplay(LPTSTR display, LPTSTR phost, int hostlen, int *port);

// Macro DIALOG_MAKEINTRESOURCE is used to allow both normal windows dialogs
// and the selectable aspect ratio dialogs under WinCE (PalmPC vs HPC).
#ifndef UNDER_CE
#define DIALOG_MAKEINTRESOURCE MAKEINTRESOURCE
#else
// Under CE we pick dialog resource according to the 
// screen format selected or determined.
#define DIALOG_MAKEINTRESOURCE(res) SELECT_MAKEINTRESOURCE(res ## _PALM, res)
inline LPTSTR SELECT_MAKEINTRESOURCE(WORD res_palm, WORD res_hpc)
{
	if (pApp->m_options.m_palmpc)
		return MAKEINTRESOURCE(res_palm);
	else
		return MAKEINTRESOURCE(res_hpc);
}
#endif

#endif