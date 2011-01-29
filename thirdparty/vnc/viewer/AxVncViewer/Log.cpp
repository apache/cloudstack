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
// Log.cpp: implementation of the Log class.
//
//////////////////////////////////////////////////////////////////////
#include <stdio.h>
#include <time.h>
#include "stdhdrs.h"
#include "Log.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

const int Log::ToDebug   =  1;
const int Log::ToFile    =  2;
const int Log::ToConsole =  4;

const static int LINE_BUFFER_SIZE = 1024;

Log::Log(int mode, int level, LPTSTR filename, bool append)
{
    hlogfile = NULL;
    m_todebug = false;
    m_toconsole = false;
    m_tofile = false;
    SetMode(mode);
    if (mode & ToFile)  {
        SetFile(filename, append);
    }
}

void Log::SetMode(int mode) {
    
    if (mode & ToDebug)
        m_todebug = true;
    else
        m_todebug = false;

    if (mode & ToFile)  {
        m_tofile = true;
    } else {
        CloseFile();
        m_tofile = false;
    }
    
#ifdef _WIN32_WCE
	m_toconsole = false;
#else
    if (mode & ToConsole) {
        if (!m_toconsole) {
            AllocConsole();
            fclose(stdout);
            fclose(stderr);
#ifdef _MSC_VER
            int fh = _open_osfhandle((long)GetStdHandle(STD_OUTPUT_HANDLE), 0);
            _dup2(fh, 1);
            _dup2(fh, 2);
            _fdopen(1, "wt");
            _fdopen(2, "wt");
            printf("fh is %d\n",fh);
            fflush(stdout);
#endif
        }

        m_toconsole = true;
    } else {
        m_toconsole = false;
    }
#endif
}


void Log::SetLevel(int level) {
    m_level = level;
}

void Log::SetFile(LPTSTR filename, bool append) 
{
    // if a log file is open, close it now.
    CloseFile();

    m_tofile  = true;
    
    // If filename is NULL or invalid we should throw an exception here
    
    hlogfile = CreateFile(
        filename,  GENERIC_WRITE, FILE_SHARE_READ, NULL,
        OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL  );
    
    if (hlogfile == INVALID_HANDLE_VALUE) {
        // We should throw an exception here
        m_todebug = true;
        m_tofile = false;
        Print(0, _T("Error opening log file %s\n"), filename);
    }
    if (append) {
        SetFilePointer( hlogfile, 0, NULL, FILE_END );
    } else {
        SetEndOfFile( hlogfile );
    }
}

// if a log file is open, close it now.
void Log::CloseFile() {
    if (hlogfile != NULL) {
        CloseHandle(hlogfile);
        hlogfile = NULL;
    }
}


#ifndef UNDER_CE

// Non-CE version 

void Log::ReallyPrint(LPTSTR format, va_list ap) 
{
    TCHAR line[LINE_BUFFER_SIZE];
	struct tm *newTime;
	time_t szClock;
	time(&szClock);
	strcpy(line, asctime(localtime(&szClock)));
	int n = strlen(line);
	line[n - 1] = ' ';
	_vsnprintf(line + n, LINE_BUFFER_SIZE-1-n, format, ap); // sf@2006 - Prevents buffer overflow
    if (m_todebug) OutputDebugString(line);

    if (m_toconsole) {
        DWORD byteswritten;
        WriteConsole(GetStdHandle(STD_OUTPUT_HANDLE), line, _tcslen(line)*sizeof(TCHAR), &byteswritten, NULL); 
    };

    if (m_tofile && (hlogfile != NULL)) {
        DWORD byteswritten;
        WriteFile(hlogfile, line, _tcslen(line)*sizeof(TCHAR), &byteswritten, NULL); 

    }	
}

#else

// CE version 

void Log::ReallyPrint(LPTSTR format, va_list ap) 
{
    TCHAR line[LINE_BUFFER_SIZE];

	_vsnprintf(line, LINE_BUFFER_SIZE-1, format, ap); // sf@2006 - Prevents buffer overflow
    if (m_todebug) OutputDebugString(line);

    if (m_tofile && (hlogfile != NULL)) {
        DWORD byteswritten;
		
		// Log file is more readable if non-unicode!
		char ansiline[LINE_BUFFER_SIZE];
		int origlen = _tcslen(line);
		int newlen = WideCharToMultiByte(
			CP_ACP,    // code page
			0,         // performance and mapping flags
			line,      // address of wide-character string
			origlen,   // number of characters in string
			ansiline,  // address of buffer for new string
			255,       // size of buffer
			NULL, NULL );
		WriteFile(hlogfile, ansiline, newlen, &byteswritten, NULL); 
    }	
}

#endif

Log::~Log()
{
    CloseFile();
}

Log theLog();
