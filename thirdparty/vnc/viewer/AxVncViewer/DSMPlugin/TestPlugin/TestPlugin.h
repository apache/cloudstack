/////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2002 Ultr@VNC. All Rights Reserved.
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
// IF YOU USE THIS GPL SOURCE CODE TO MAKE YOUR DSM PLUGIN, PLEASE ADD YOUR
// COPYRIGHT TO THE TOP OF THIS FILE AND SHORTLY DESCRIBE/EXPLAIN THE 
// MODIFICATIONS YOU'VE MADE. THANKS.
//
// IF YOU DON'T USE THIS CODE AS A BASE FOR YOUR PLUGIN, THE HEADER ABOVE AND
// ULTR@VNC COPYRIGHT SHOULDN'T BE FOUND IN YOUR PLUGIN SOURCE CODE.
//
////////////////////////////////////////////////////////////////////////////
//
// Testplugin is a sample that shows the rules that a DSM Plugin for UltraVNC
// must follow (Fonctions to export, their signatures, design constraints...)
//
// WARNING: As it is still beta, some rules might change in the near future
//
//
// WARNING : In order to avoid multithreading issues with your DSM Plugin, it is recommended
// (not to say mandatory...) to use 2 distincts contexts (buffers, structs, objects, keys on so on)
// for "Tranformation" and "Restoration" operations. 
// Actually, 2 distinct mutexes are used at DSMPlugin.cpp level (one in the TransformBuffer 
// function, and the other in the RestoreBuffer function) so Transform and Restore events
// may occur at the same time.
// 
//////////////////////////////////////////////////////////////////////////////

#ifdef TESTPLUGIN_EXPORTS
#define TESTPLUGIN_API __declspec(dllexport)
#else
#define TESTPLUGIN_API __declspec(dllimport)
#endif

#include <stdlib.h>
#include <windows.h>
#include <memory.h>
#include "resource.h"

// Plugin struct example - Of course some classes can be used instead...
typedef struct
{
	char szDescription[256];
	int nMode;
	int nInternalParam1;
	int nInternalParam2;  // And so on.. add what you need.

} PLUGINSTRUCT;


//
// Internal local functions
//
BOOL MyStrToken(LPSTR szToken, LPSTR lpString, int nTokenNum, char cSep);
int GiveTransDataLen(int nDataLen);
int GiveRestDataLen(int nDataLen);
BYTE* CheckLocalTransBufferSize(int nBufferSize);
BYTE* CheckLocalRestBufferSize(int nBufferSize);
// Config procs if necessary (the plugin is allowed to have no config dialog at all)
BOOL CALLBACK ConfigDlgProc(HWND hwnd,  UINT uMsg,  WPARAM wParam, LPARAM lParam );
int DoDialog(void);


//
// A DSM Plugin MUST export (extern "C" - __cdecl) all the following functions
// (same names, same signatures)

extern "C"
{
TESTPLUGIN_API char* Description(void);
TESTPLUGIN_API int Startup(void);
TESTPLUGIN_API int Shutdown(void);
TESTPLUGIN_API int Reset(void);
TESTPLUGIN_API int SetParams(HWND hVNC, char* szParams);
TESTPLUGIN_API char* GetParams(void);
TESTPLUGIN_API BYTE* TransformBuffer(BYTE* pDataBuffer, int nDataLen, int* pnTransformedDataLen);
TESTPLUGIN_API BYTE* RestoreBuffer(BYTE* pTransBuffer, int nTransDataLen, int* pnRestoredDataLen);
TESTPLUGIN_API void FreeBuffer(BYTE* pBuffer);
}
