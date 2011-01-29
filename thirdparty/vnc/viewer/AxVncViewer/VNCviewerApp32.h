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

#include "VNCviewerApp.h"
#include "Daemon.h"
#include "Flasher.h"

class VNCviewerApp32 : public VNCviewerApp {
public:
	VNCviewerApp32(HINSTANCE hInstance, PSTR szCmdLine);

	void NewConnection();
	void NewConnection(TCHAR *host, int port, TCHAR* pszUser = NULL, TCHAR* pszPassword = NULL, TCHAR* pszProxy = NULL, HWND rootHwnd = NULL, HWND* pHwndAppFrame = NULL);
	void NewConnection(SOCKET sock);
	void NewConnection(TCHAR *configFile);

	~VNCviewerApp32();
private:
	// Set up registry for program's sounds
	void RegisterSounds();
	Flasher *m_pflasher;
	Daemon  *m_pdaemon;
};

