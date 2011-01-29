/////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2002 Ultr@Vnc Team Members. All Rights Reserved.
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
////////////////////////////////////////////////////////////////////////////
//
// DSMPlugin.h: interface for the CDSMPlugin class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(CDSMPlugin_H)
#define CDSMPlugin_H

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#ifdef UNDER_CE
#include "omnithreadce.h"
#else
#include "omnithread.h"
#endif

#include "windows.h"

// A plugin dll must export the following functions (with same convention)
typedef char* (__cdecl  *DESCRIPTION)(void);
typedef int   (__cdecl  *STARTUP)(void);
typedef int   (__cdecl  *SHUTDOWN)(void);
typedef int   (__cdecl  *SETPARAMS)(HWND, char*);
typedef char* (__cdecl  *GETPARAMS)(void);
typedef BYTE* (__cdecl  *TRANSFORMBUFFER)(BYTE*, int, int*);
typedef BYTE* (__cdecl  *RESTOREBUFFER)(BYTE*, int, int*);
typedef void  (__cdecl  *FREEBUFFER)(BYTE*);
typedef int   (__cdecl  *RESET)(void);


//
//
//
class CDSMPlugin  
{
public:
	void SetLoaded(bool fEnable);
	bool IsLoaded(void) { return m_fLoaded; };
	void SetEnabled(bool fEnable);
	bool IsEnabled(void) { return m_fEnabled; };
	bool InitPlugin(void);
	bool SetPluginParams(HWND hWnd, char* szParams);
	char* GetPluginParams(void);
	char* DescribePlugin(void);
	int  ListPlugins(HWND hComboBox);
	bool LoadPlugin(char* szPlugin, bool fAllowMulti);
	bool UnloadPlugin(void); // Could be private
	BYTE* TransformBuffer(BYTE* pDataBuffer, int nDataLen, int* nTransformedDataLen);
	BYTE* RestoreBufferStep1(BYTE* pDataBuffer, int nDataLen, int* nRestoredDataLen);
	BYTE* RestoreBufferStep2(BYTE* pDataBuffer, int nDataLen, int* nRestoredDataLen);
	void RestoreBufferUnlock();
	char* GetPluginName(void) { return m_szPluginName;} ;
	char* GetPluginVersion(void)  {  return m_szPluginVersion;} ;
	char* GetPluginDate(void) { return m_szPluginDate; } ;
	char* GetPluginAuthor(void) { return m_szPluginAuthor;} ;
	char* GetPluginFileName(void) { return m_szPluginFileName;} ;
	CDSMPlugin();
	virtual ~CDSMPlugin();
	bool ResetPlugin(void);

	long m_lPassLen; 

	omni_mutex m_RestMutex;

private:
	bool m_fLoaded;
	bool m_fEnabled;

	char szPassword[64];

	char m_szPluginName[128]; // Name of the plugin and very short description
	char m_szPluginVersion[16];
	char m_szPluginDate[16];
	char m_szPluginAuthor[64];
	char m_szPluginFileName[128]; // No path, just the filename and possible comment

	HMODULE m_hPDll;
	char m_szDllName[196];

	// Plugin's functions pointers when loaded
	DESCRIPTION     m_PDescription;
	SHUTDOWN		m_PShutdown;
	STARTUP			m_PStartup;
	SETPARAMS		m_PSetParams;
	GETPARAMS		m_PGetParams;
	TRANSFORMBUFFER m_PTransformBuffer;
	RESTOREBUFFER	m_PRestoreBuffer;
	FREEBUFFER		m_PFreeBuffer;
	RESET			m_PReset;

	BYTE* m_pTransBuffer;
	BYTE* m_pRestBuffer;

	omni_mutex m_TransMutex;
	// omni_mutex m_RestMutex;
};

#endif
