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
// This is an object and macros which provide general logging and debugging functions.
// It can log to a file, to a new console, and/or to debug - others maybe to follow.
// Every log object has a logging level (which can be changed).
// Only log requests with a high enough level attached get logged. So the
// level can be thought of as 'amount of detail'.
// We use Unicode-portable stuff here for compatibility with WinCE.
//
// Typical use:
//
//       Log vnclog;
//       vnclog.SetFile( _T("myapp.log") );
//       ...
//       vnclog.Print(2, _T("x = %d\n"), x);
//

#pragma once
#include <stdarg.h>

class Log  
{
public:
    // Logging mode flags:
    static const int ToDebug;
    static const int ToFile;
    static const int ToConsole;

    // Create a new log object.
    // Parameters as follows:
    //    mode     - specifies where output should go, using combination
    //               of flags above. ToConsole won't do anything on CE.
    //    level    - the default level
    //    filename - if flag Log::ToFile is specified in the type,
    //               a filename must be specified here.
    //    append   - if logging to a file, whether or not to append to any
    //               existing log.
	Log(int mode = ToDebug, int level = 1, LPTSTR filename = NULL, bool append = false);

    inline void Print(int level, LPTSTR format, ...) {
        if (level > m_level) return;
        va_list ap;
        va_start(ap, format);
        ReallyPrint(format, ap);
        va_end(ap);
    }
    
    // Change the log level
    void SetLevel(int level);

    // Change the logging mode
    void SetMode(int mode);

    // Change or set the logging filename.  This enables ToFile mode if
    // not already enabled.
    void SetFile(LPTSTR filename, bool append = false);

	virtual ~Log();

private:
    void ReallyPrint(LPTSTR format, va_list ap);
    void CloseFile();
    bool m_tofile, m_todebug, m_toconsole;
    int m_level;
    HANDLE hlogfile;
};
