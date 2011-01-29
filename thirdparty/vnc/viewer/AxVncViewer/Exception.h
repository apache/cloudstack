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


#pragma once

// Exceptions used in VNCviewer

class Exception  
{
public:
	Exception(const char *info = NULL);
	virtual void Report() = 0;
	virtual ~Exception();
	char *m_info;
};

// This indicates something that the catcher should close 
// the connection quietly.
// Report() will display a TRACE message
class QuietException : public Exception {
public:
	QuietException(const char *info = NULL);
	virtual void Report();
	virtual ~QuietException();
};

// This indicates something the user should be told about.
// In situations of ambiguity, the 'close' parameter can be used
// to specify whether or not the connection is closed as a result.
// In general it will be.
// Report() will display a message box
class WarningException : public Exception {
public:
	WarningException(const char *info = NULL, bool close = true);
	virtual void Report();
	virtual ~WarningException();
	bool m_close;
};

// This is serious stuff - similar to an assert - we may not use?
// Report will display an important message box. Connection definitely
// closed.
class ErrorException : public Exception {
public:
	ErrorException(const char *info = NULL);
	virtual void Report();
	virtual ~ErrorException();
};
