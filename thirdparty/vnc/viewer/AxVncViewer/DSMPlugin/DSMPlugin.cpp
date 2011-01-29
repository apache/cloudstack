/////////////////////////////////////////////////////////////////////////////
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
//
//
// DSMPlugin.cpp: implementation of the CDSMPlugin class.
//
//////////////////////////////////////////////////////////////////////
//
// This class is the interface between UltraVNC and plugins 
// (third party dlls) that may be developed (written, exported and 
// provided by authorized individuals - according to the law of their 
// country) to alter/modify/process/encrypt rfb data streams between 
// vnc viewer and vnc server.
//
// The goal here is not to design and develop an extensive, versatile
// and powerfull plugin system but to provide people a way
// to easely customize the VNC communication data between client and server.
//
// It handles the following tasks:
// 
// - Listing of all plugins found in the vnc directory (dlls, with ".dsm" extension)
//
// - Loading of a given plugin
//
// - Interface between vnc and the plugin functions: 
//   - Init()               Initialize the plugin
//   - SetParams()          Set Password, or key or other. If more than one param, uses ',' as separator
//   - GetParams()          Give Password, or key or other. If more than one param, uses ',' as separator
//   - DescribePlugin()     Give the Plugin ID string (Name, Author, Date, Version, FileName)
//   - TransformBuffer()    Tell the plugin to do it's transformation against the data in the buffer
//   - RestoreBuffer()      Tell the plugin to restore data to its original state   
//   - Shutdown()           Cleanup and shutdown the plugin
//   - FreeBuffer()         Free a buffer used in the plugin
//	 - Reset()
//
// - Unloading of the current loaded plugin
// 
// WARNING: For the moment, only ONE instance of this class must exist in Vncviewer and WinVNC
// Consequently, WinVNc will impose all its clients to use the same plugin. Maybe we'll 
// improve that soon. It depends on the demand/production of DSM plugins.


#include <memory.h>
#include <stdio.h>
#include <string.h>
#include "DSMPlugin.h"


//
// Utils
//
BOOL MyStrToken(LPSTR szToken, LPSTR lpString, int nTokenNum, char cSep)
{
	int i = 1;
	while (i < nTokenNum)
	{
		while ( *lpString && (*lpString != cSep) &&(*lpString != '\0'))
		{
			lpString++;
		}
		i++;
		lpString++;
	}
	while ((*lpString != cSep) && (*lpString != '\0'))
	{
		*szToken = *lpString;
		szToken++;
		lpString++;
	}
	*szToken = '\0' ;
	if (( ! *lpString ) || (! *szToken)) return NULL;
	return FALSE;
}



//
//
//
CDSMPlugin::CDSMPlugin()
{
	m_fLoaded = false;
	m_fEnabled = false;
	m_lPassLen = 0;

	m_pTransBuffer = NULL;
	m_pRestBuffer = NULL;

	sprintf(m_szPluginName, "Unknown");
	sprintf(m_szPluginVersion, "0.0.0");
	sprintf(m_szPluginDate, "12-12-2002");
	sprintf(m_szPluginAuthor, "Someone");
	sprintf(m_szPluginFileName, "Plugin.dsm"); // No path, just the filename

	m_hPDll = NULL;

	// Plugin's functions pointers init
	STARTUP			m_PStartup = NULL;
	SHUTDOWN		m_PShutdown = NULL;
	SETPARAMS		m_PSetParams = NULL;
	GETPARAMS		m_PGetParams = NULL;
	TRANSFORMBUFFER m_PTransformBuffer = NULL;
	RESTOREBUFFER	m_PRestoreBuffer = NULL;
	TRANSFORMBUFFER	m_PFreeBuffer = NULL;
	RESET			m_PReset = NULL;

}

//
//
//
CDSMPlugin::~CDSMPlugin()
{
	// TODO: Log events
	if (IsLoaded())
		UnloadPlugin();
}


//
//
//
void CDSMPlugin::SetEnabled(bool fEnable)
{
	m_fEnabled =  fEnable;
}


//
//
//
void CDSMPlugin::SetLoaded(bool fEnable)
{
	m_fLoaded =  fEnable;
}


//
//
//
char* CDSMPlugin::DescribePlugin(void)
{
	// TODO: Log events
	char* szDescription = NULL;
	if (m_PDescription)
	{
		 szDescription = (*m_PDescription)();
		 if (szDescription != NULL)
		 {
			MyStrToken(m_szPluginName, szDescription, 1, ',');
			MyStrToken(m_szPluginAuthor, szDescription, 2, ',');
			MyStrToken(m_szPluginDate, szDescription, 3, ',');
			MyStrToken(m_szPluginVersion, szDescription, 4, ',');
			MyStrToken(m_szPluginFileName, szDescription, 5, ',');
		 }
		 return szDescription;
	}

	else 
		return "No Plugin loaded";
}


//
// Init the DSMPlugin system
//
bool CDSMPlugin::InitPlugin(void)
{
	// TODO: Log events
	int nRes = (*m_PStartup)();
	if (nRes < 0) return false;
	else return true;
}

//
// Reset the DSMPlugin
//
bool CDSMPlugin::ResetPlugin(void)
{
	// TODO: Log events
	int nRes = (*m_PReset)();
	if (nRes < 0) return false;
	else return true;
}


//
// szParams is the key (or password) that is transmitted to the loaded DSMplugin
//
bool CDSMPlugin::SetPluginParams(HWND hWnd, char* szParams)
{
	// TODO: Log events
	int nRes = (*m_PSetParams)(hWnd, szParams);
	if (nRes > 0) return true; else return false;

}


//
// Return the loaded DSMplugin current param(s)
//
char* CDSMPlugin::GetPluginParams(void)
{
	// 
	return (*m_PGetParams)();

}


//
// List all the plugins is the current APP directory in the given ComboBox
//
int CDSMPlugin::ListPlugins(HWND hComboBox)
{
	// TODO: Log events
	WIN32_FIND_DATA fd;
	HANDLE ff;
	int fRet = 1;
	int nFiles = 0;
	char szCurrentDir[MAX_PATH];

	if (GetModuleFileName(NULL, szCurrentDir, MAX_PATH))
	{
		char* p = strrchr(szCurrentDir, '\\');
		if (p == NULL)
			return 0;
		*p = '\0';
	}
	else
		return 0;
	// MessageBox(NULL, szCurrentDir, "Current directory", MB_OK);

    if (szCurrentDir[strlen(szCurrentDir) - 1] != '\\') strcat(szCurrentDir, "\\");
	strcat(szCurrentDir, "*.dsm"); // The DSMplugin dlls must have this extension
	
	ff = FindFirstFile(szCurrentDir, &fd);
	if (ff == INVALID_HANDLE_VALUE)
	{
		// Todo: Log error here
		return 0;
	}

	while (fRet != 0)
	{
		SendMessage(hComboBox, CB_ADDSTRING, 0, (LPARAM)(fd.cFileName)); 
		nFiles++;
		fRet = FindNextFile(ff, &fd);
	}

	FindClose(ff);

	return nFiles;
}



//
// Load the given DSMplugin and map its functions
//
bool CDSMPlugin::LoadPlugin(char* szPlugin, bool fAllowMulti)
{
	// sf@2003 - Multi dll trick 
	// I really don't like doing this kind of dirty workaround but I have no time to do
	// better for now. Used only by a listening viewer.
	// Create a numbered temporary copy of the original plugin dll and load it (viewer only, for now)
	if (fAllowMulti)
	{
		bool fDllCopyCreated = false;
		int i = 1;
		char szDllCopyName[196];
		while (!fDllCopyCreated)
		{
			strcpy(szDllCopyName, szPlugin);
			szDllCopyName[strlen(szPlugin) - 4] = '\0'; //remove the ".dsm" extension
			sprintf(szDllCopyName, "%s-tmp.d%d", szDllCopyName, i++);
			fDllCopyCreated = (bool) CopyFile(szPlugin, szDllCopyName, false); 
			// Note: Let's be really dirty; Overwrite if it's possible only (dll not loaded). 
			// This way if for some reason (abnormal process termination) the dll wasn't previously 
			// normally deleted we overwrite/clean it with the new one at the same time.
			if (i > 99) break; // Just in case...
		}
		strcpy(m_szDllName, szDllCopyName);
		m_hPDll = LoadLibrary(m_szDllName);
	}
	else // Use the original plugin dll
	{
		ZeroMemory(m_szDllName, strlen(m_szDllName));
		m_hPDll = LoadLibrary(szPlugin);
	}

	if (m_hPDll == NULL) return false;

	m_PDescription     = (DESCRIPTION)     GetProcAddress(m_hPDll, "Description");
	m_PStartup         = (STARTUP)         GetProcAddress(m_hPDll, "Startup");
	m_PShutdown        = (SHUTDOWN)        GetProcAddress(m_hPDll, "Shutdown");
	m_PSetParams       = (SETPARAMS)       GetProcAddress(m_hPDll, "SetParams");
	m_PGetParams       = (GETPARAMS)       GetProcAddress(m_hPDll, "GetParams");
	m_PTransformBuffer = (TRANSFORMBUFFER) GetProcAddress(m_hPDll, "TransformBuffer");
	m_PRestoreBuffer   = (RESTOREBUFFER)   GetProcAddress(m_hPDll, "RestoreBuffer");
	m_PFreeBuffer      = (FREEBUFFER)      GetProcAddress(m_hPDll, "FreeBuffer");
	m_PReset           = (RESET)           GetProcAddress(m_hPDll, "Reset");

	if (m_PStartup == NULL || m_PShutdown == NULL || m_PSetParams == NULL || m_PGetParams == NULL
		|| m_PTransformBuffer == NULL || m_PRestoreBuffer == NULL || m_PFreeBuffer == NULL)
	{
		FreeLibrary(m_hPDll); 
		if (*m_szDllName) DeleteFile(m_szDllName);
		return false;
	}

	// return ((*m_PStartup)());
	SetLoaded(true);
	return true;
}


//
// Unload the current DSMPlugin from memory
//
bool CDSMPlugin::UnloadPlugin(void)
{
	// TODO: Log events
	// Force the DSMplugin to free the buffers it allocated
	if (m_pTransBuffer != NULL) (*m_PFreeBuffer)(m_pTransBuffer);
	if (m_pRestBuffer != NULL) (*m_PFreeBuffer)(m_pRestBuffer);
	
	m_pTransBuffer = NULL;
	m_pRestBuffer = NULL;

	SetLoaded(false);

	if ((*m_PShutdown)())
	{
		bool fFreed = false;
		fFreed = FreeLibrary(m_hPDll); 
		if (*m_szDllName) DeleteFile(m_szDllName);
		return fFreed;
	}
	else
		return false;
	
}


//
// Tell the plugin to do its transformation on the source data buffer
// Return: pointer on the new transformed buffer (allocated by the plugin)
// nTransformedDataLen is the number of bytes contained in the transformed buffer
//
BYTE* CDSMPlugin::TransformBuffer(BYTE* pDataBuffer, int nDataLen, int* pnTransformedDataLen)
{
	// FixME: possible pb with this mutex in WinVNC
	omni_mutex_lock l(m_TransMutex);

	m_pTransBuffer = (*m_PTransformBuffer)(pDataBuffer, nDataLen, pnTransformedDataLen);

	return m_pTransBuffer;
}


// - If pRestoredDataBuffer = NULL, the plugin check its local buffer and return the pointer
// - Otherwise, restore data contained in its rest. buffer and put the result in pRestoredDataBuffer
//   pnRestoredDataLen is the number bytes put in pRestoredDataBuffers
BYTE* CDSMPlugin::RestoreBufferStep1(BYTE* pRestoredDataBuffer, int nDataLen, int* pnRestoredDataLen)
{
	//m_RestMutex.lock();
	m_pRestBuffer = (*m_PRestoreBuffer)(pRestoredDataBuffer, nDataLen, pnRestoredDataLen);
	return m_pRestBuffer;
}

BYTE* CDSMPlugin::RestoreBufferStep2(BYTE* pRestoredDataBuffer, int nDataLen, int* pnRestoredDataLen)
{
	m_pRestBuffer = (*m_PRestoreBuffer)(pRestoredDataBuffer, nDataLen, pnRestoredDataLen);
	//m_RestMutex.unlock();
	return NULL;
}

void CDSMPlugin::RestoreBufferUnlock()
{
	//m_RestMutex.unlock();
}

