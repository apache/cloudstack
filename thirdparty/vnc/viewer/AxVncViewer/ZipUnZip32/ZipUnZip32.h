
//  Copyright (C) 2002 Ultr@VNC Team Members. All Rights Reserved.
// 
//  This program is free software; you can redistribute it and/or modify
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
// If the source code for the program is not available from the place from
// which you received this file, check 
// http://ultravnc.sourceforge.net/
//
////////////////////////////////////////////////////////////////////////////
/*
  Copyright (c) 1990-2002 Info-ZIP.  All rights reserved.

  See the accompanying file LICENSE, version 2000-Apr-09 or later
  (the contents of which are also included in unzip.h) for terms of use.
  If, for some reason, all these files are missing, the Info-ZIP license
  also may be found at:  ftp://ftp.info-zip.org/pub/infozip/license.html
*/

#ifndef _ZIPUNZIP32_H
#define _ZIPUNZIP32_H

#include "windows.h"


/////////////////////////////////////////////////////////////////////////
// ZIP32 DLL PART
/////////////////////////////////////////////////////////////////////////

#define ZIP_DLL_NAME "zip32.dll"
typedef int (WINAPI DLLPRNT) (LPSTR, unsigned long);
typedef int (WINAPI DLLPASSWORD) (LPSTR, int, LPCSTR, LPCSTR);
typedef int (WINAPI DLLSERVICE) (LPCSTR, unsigned long);
typedef int (WINAPI DLLCOMMENT)(LPSTR);

// Callback functions (called from within the Zip32.dll) structure
typedef struct
{
	DLLPRNT *print;
	DLLCOMMENT *comment;
	DLLPASSWORD *password;
	DLLSERVICE *ServiceApplication;
} ZIPUSERFUNCTIONS, far * LPZIPUSERFUNCTIONS;

// Zip options structure
typedef struct
{       
	LPSTR Date;             /* Date to include after */
	LPSTR szRootDir;        /* Directory to use as base for zipping */
	LPSTR szTempDir;        /* Temporary directory used during zipping */
	BOOL fTemp;             /* Use temporary directory '-b' during zipping */
	BOOL fSuffix;           /* include suffixes (not implemented) */
	BOOL fEncrypt;          /* encrypt files */
	BOOL fSystem;           /* include system and hidden files */
	BOOL fVolume;           /* Include volume label */
	BOOL fExtra;            /* Exclude extra attributes */
	BOOL fNoDirEntries;     /* Do not add directory entries */
	BOOL fExcludeDate;      /* Exclude files newer than specified date */
	BOOL fIncludeDate;      /* Include only files newer than specified date */
	BOOL fVerbose;          /* Mention oddities in zip file structure */
	BOOL fQuiet;            /* Quiet operation */
	BOOL fCRLF_LF;          /* Translate CR/LF to LF */
	BOOL fLF_CRLF;          /* Translate LF to CR/LF */
	BOOL fJunkDir;          /* Junk directory names */
	BOOL fGrow;             /* Allow appending to a zip file */
	BOOL fForce;            /* Make entries using DOS names (k for Katz) */
	BOOL fMove;             /* Delete files added or updated in zip file */
	BOOL fDeleteEntries;    /* Delete files from zip file */
	BOOL fUpdate;           /* Update zip file--overwrite only if newer */
	BOOL fFreshen;          /* Freshen zip file--overwrite only */
	BOOL fJunkSFX;          /* Junk SFX prefix */
	BOOL fLatestTime;       /* Set zip file time to time of latest file in it */
	BOOL fComment;          /* Put comment in zip file */
	BOOL fOffsets;          /* Update archive offsets for SFX files */
	BOOL fPrivilege;        /* Use privileges (WIN32 only) */
	BOOL fEncryption;       /* TRUE if encryption supported, else FALSE.
							   this is a read only flag */
	int  fRecurse;          /* Recurse into subdirectories. 1 => -r, 2 => -R (bugged)*/
	int  fRepair;           /* Repair archive. 1 => -F, 2 => -FF */
	char fLevel;            /* Compression level (0 - 9) */
} ZPOPT, *LPZPOPT;

// Files to Zip structure
typedef struct
{
	int  argc;              /* Count of files to zip */
	LPSTR lpszZipFN;        /* name of archive to create/update */
	char **FNV;             /* array of file names to zip up */
} ZCL, *LPZCL;

typedef int (WINAPI * _DLL_ZIP)(ZCL);
typedef int (WINAPI * _ZIP_USER_FUNCTIONS)(LPZIPUSERFUNCTIONS);
typedef BOOL (WINAPI * ZIPSETOPTIONS)(LPZPOPT);

int WINAPI DummyPassword(LPSTR, int, LPCSTR, LPCSTR);
int WINAPI DummyPrint(char far *, unsigned long);
int WINAPI DummyComment(char far *);


/////////////////////////////////////////////////////////////////////////
// END OF ZIP32 DLL PART
/////////////////////////////////////////////////////////////////////////


/////////////////////////////////////////////////////////////////////////
// UNZIP32 DLL PART
/////////////////////////////////////////////////////////////////////////

#define UNZIP_DLL_NAME "unzip32.dll"

// Dll Callback functions
typedef int (WINAPI DLLPRNT) (LPSTR, unsigned long);
typedef int (WINAPI DLLPASSWORD) (LPSTR, int, LPCSTR, LPCSTR);
typedef int (WINAPI DLLSERVICE) (LPCSTR, unsigned long);
typedef void (WINAPI DLLSND) (void);
typedef int (WINAPI DLLREPLACE)(LPSTR);
typedef void (WINAPI DLLMESSAGE)(	unsigned long, unsigned long, unsigned,
									unsigned, unsigned, unsigned, unsigned, unsigned,
									char, LPSTR, LPSTR, unsigned long, char);

// Unzip callback functions struct
typedef struct
{
	DLLPRNT *print;
	DLLSND *sound;
	DLLREPLACE *replace;
	DLLPASSWORD *password;
	DLLMESSAGE *SendApplicationMessage;
	DLLSERVICE *ServCallBk;
	unsigned long TotalSizeComp;
	unsigned long TotalSize;
	unsigned long CompFactor;       /* "long" applied for proper alignment, only */
	unsigned long NumMembers;
	WORD cchComment;
} USERFUNCTIONS, far * LPUSERFUNCTIONS;

// Unzip options struct
typedef struct
{
	int ExtractOnlyNewer;
	int SpaceToUnderscore;
	int PromptToOverwrite;
	int fQuiet;
	int ncflag;
	int ntflag;
	int nvflag;
	int nfflag;
	int nzflag;
	int ndflag;
	int noflag;
	int naflag;
	int nZIflag;
	int C_flag;
	int fPrivilege;
	LPSTR lpszZipFN;
	LPSTR lpszExtractDir;
} DCL, far * LPDCL;

// Dll exported functions
typedef int (WINAPI * _DLL_UNZIP)(int, char **, int, char **, LPDCL, LPUSERFUNCTIONS);
typedef int (WINAPI * _USER_FUNCTIONS)(LPUSERFUNCTIONS);


int WINAPI password(char *p, int n, const char *m, const char *name);
int WINAPI DisplayBuf(LPSTR, unsigned long);
int WINAPI GetReplaceDlgRetVal(char *);
void WINAPI ReceiveDllMessage(	unsigned long, unsigned long, unsigned,
								unsigned, unsigned, unsigned, unsigned, unsigned,
								char, LPSTR, LPSTR, unsigned long, char);


/////////////////////////////////////////////////////////////////////////
// END OF UNZIP32 DLL PART
/////////////////////////////////////////////////////////////////////////


class CZipUnZip32 
{
public:
	bool ZipDirectory(char* szRootDir, char* szDirectoryName, char* szZipFileName, bool fCompress);
	bool UnZipDirectory(char* szRootDir, char* szZipFileName);
	CZipUnZip32();
	virtual ~CZipUnZip32();

private:
	// Zip part
	_DLL_ZIP				m_PZipArchive; // Zip function
	_ZIP_USER_FUNCTIONS		m_PZipInit;    // Zip init
	ZIPSETOPTIONS			m_PZipSetOptions; // Zip set options

	LPZIPUSERFUNCTIONS m_lpZipUserFunctions;
	HINSTANCE m_hZipDll;
	int m_hFile;

	ZCL m_ZpZCL;
	ZPOPT m_ZpOpt;
	HANDLE m_hZUF;
	HANDLE m_hFileList;

	void FreeUpZipMemory(void);

	// Unzip part
	_DLL_UNZIP m_PWiz_SingleEntryUnzip; // Unzip function pointer
	_USER_FUNCTIONS m_PWiz_Init;        // Init function pointer

	LPUSERFUNCTIONS m_lpUserFunctions;
	LPDCL m_lpDCL;
	HINSTANCE m_hUnzipDll;
	int m_hUnzipFile;

	HANDLE m_hUF;
	HANDLE m_hDCL;
	HANDLE m_hZCL;

	void FreeUpUnzipMemory(void);

};
#endif