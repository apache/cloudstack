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

#include "TestPlugin.h"

HINSTANCE hInstance;
PLUGINSTRUCT* pPlugin = NULL;  // struct (or class instance) that handles all Plugin params.
							   // Given as an example.
 
// Internal Plugin vars, depending on what it does
char  szExternalKey[255];	// To store the password/key transmitted via SetParams() by UltraVNC apps
char  szLoaderType[32];     // To store the type of application that has loaded the plugin 
BYTE* pLocalTransBuffer = NULL; // Local Transformation buffer (freed on VNC demand)
BYTE* pLocalRestBuffer = NULL;  // Local Restoration buffer (freed on VNC demand)
int   nLocalTransBufferSize = 0;
int   nLocalRestBufferSize = 0;
//


// Plugin Description
// Please use the following format (with ',' (comma) as separator)
// Name-Description,Author,Date,Version,FileName-Comment
// For the version, we recommend the following format: x.y.z
// The other fields (Name-Description, Author, Date, FileName-Comment) are format free (don't use ',' in them, of course)
#define PLUGIN_DESCRIPTION  "TestPlugin,Sam,Nov 21 2002,1.0.0,TestPlugin.dsm"



// ----------------------------------------------------------------------
//
// A VNC DSM Plugin MUST export (extern "C" (__cdecl)) all the following
// functions (same names, same signatures)
//
// For return values, the rule is:
//    < 0, 0, and NULL pointer mean Error
//    > 0 and pointer != NULL mean Ok
//
// ----------------------------------------------------------------------
//

// Returns the ID string of the Plugin
//
TESTPLUGIN_API char* Description(void)
{
	return PLUGIN_DESCRIPTION;
}


//
// Initialize the plugin and all its internals
// Return -1 if error
//
TESTPLUGIN_API int Startup(void)
{
	// Init everything
	memset(szExternalKey, 0, sizeof(szExternalKey));
	// Create threads if any
	return 1;
}


//
// Stop and Clean up the plugin 
// Return -1 if error
// 
TESTPLUGIN_API int Shutdown(void)
{
	// Terminate Threads if any
	// Cleanup everything
	return 1;
}


//
// Stop and Clean up the plugin 
// Return -1 if error
// 
TESTPLUGIN_API int Reset(void)
{
	// Reset the plugin (buffers, keys, whatever that 
	// requires to be reset between 2 connections

	return 1;
}


//
// Set the plugin params (Key or password )
// If several params are needed, they can be transmitted separated with ',' (comma)
// then translated if necessary. They also can be taken from the internal Plugin config
// 
// WARNING: The plugin is responsible for implementing necessary GUI or File/Registry reading
// to acquire additionnal parameters and to ensure their persistence if necessary.
// Same thing for events/errors logging.
// 
// This function can be called 2 times, both from vncviewer and WinVNC:
// 
// 1.If the user clicks on the Plugin's "config" button in vncviewer and WinVNC dialog boxes
//   In this case this function is called with hVNC != 0 (CASE 1)
//
//   -> szParams is a string formatted as follow: "Part1,Part2"
//   Part1 = "NoPassword"
//   Part2 = type of application that has loaded the plugin
//     "viewer"     : for vncviewer
//     "server-svc" : for WinVNC run as a service
//     "server-app" : for WINVNC run as an application
//
//   -> The Plugin Config dialog box is displayed if any.
// 
// 2.When then plugin is Inited from VNC viewer or Server, right after Startup() call (CASE 2);
//   In this case, this function is called with hVNC = 0 and
//   szParams is a string formatted as follows: "part1,Part2"
//   Part1 = The VNC password, if required by the GetParams() function return value
//   Part2 = type of application that has loaded the plugin
//      "viewer"     : for vncviewer
//      "server-svc" : for WinVNC run as a service
//      "server-app" : for WINVNC run as an application
//   (this info can be used for application/environnement dependent
//    operations (config saving...))
//   
TESTPLUGIN_API int SetParams(HWND hVNC, char* szParams)
{
	// CASE 1
	// Get the environnement (szLoaderType) value that is always sent from 
	// VNC viewer or server
	MyStrToken(szLoaderType, szParams, 2, ',');

	// If hVNC != 0, display for instance the Plugin Config Dialog box 
	if (hVNC)
	{
		// Display the Plugin Config dialog box
		DoDialog();
	}

	// CASE 2: 
	// Use szParams to setup the Plugin.
	// In this example Plugin, the externalkey is not used but we store it anyway.for demo.
	// (it corresponds to the VNC password as we require it in the GetParams() function below)
	MyStrToken(szExternalKey, szParams, 1, ',');

	return 1;
}


//
// Return the current plugin params
// As the plugin is basically a blackbox, VNC doesn't need to know 
// the Plugin parameters.
// But we use this method to know if the plugin needs the VNC password
// as a parameter to do its job correctly (for login step).
// Thus this function is called once before the SetParams() function is called
//  - Return "VNCPasswordNeeded" if VNC password must be transmitted by the UltraVNC app
//  - Return any other Plugin parameters value otherwise (not used in WinVNC & vncviewer for now)
TESTPLUGIN_API char* GetParams(void)
{
	if (strlen(szExternalKey) > 0)
		return szExternalKey; // Return the already stored externalkey params
	else
		return "VNCPasswordNeeded";
	    // return "IveGotAllINeedThanks";
}

// 
// TransformBuffer function
//
// Transform the data given in pDataBuffer then return the pointer on the allocated 
// buffer containing the resulting data.
// The length of the resulting data is given by pnTransformedDataLen
//
TESTPLUGIN_API BYTE* TransformBuffer(BYTE* pDataBuffer, int nDataLen, int* pnTransformedDataLen)
{
	BYTE* pTransBuffer = CheckLocalTransBufferSize(GiveTransDataLen(nDataLen));
	if (pTransBuffer == NULL)
	{
		*pnTransformedDataLen = -1;
		return NULL;
	}

	// Do the actual data padding/transformation/utilization or whatever here
	// In this Sample plugin we do nothing.
	// We just copy the incoming data into the destination buffer, without modifiying it.
	int i;
	for (i = 0; i < nDataLen; i++)
	{
		pTransBuffer[i] = pDataBuffer[i];
	}

	// return the transformed data length
	*pnTransformedDataLen = GiveTransDataLen(nDataLen);

	return pTransBuffer; // Actually, pTransBuffer = pLocalTransBuffer
}


//
// RestoreBuffer function
//
// This function has a 2 mandatory behaviors:
//
// 1. If pRestoredDataBuffer is NULL, the function must return the pointer to current
//    LocalRestBuffer that is going to receive the Transformed data to restore
//    from VNC viewer/server's socket.
//    This buffer must be of the size of transformed data, calculated from nDataLen
//    and this size must be given back in pnRestoredDataLen.
//
// 2. If pRestoredDataBuffer != NULL, it is the destination buffer that is going to receive
//    the restored data. So the function must restore the data that is currently in the
//    local pLocalRestBuffer (nDataLen long) and put the result in pRestoredDataBuffer.
//    The length of the resulting data is given back in pnTransformedDataLen
//
// Explanation: When VNC viewer/server wants to restore some data, it does the following:
// - Calls RestoreBuffer with NULL to get the buffer (and its length) to store incoming transformed data
// - Reads incoming transformed data from socket directly into the buffer given (and of given length)
// - Calls RestoreBuffer again to actually restore data into the given destination buffer.
// This way the copies of data buffers are reduced to the minimum.
// 
TESTPLUGIN_API BYTE* RestoreBuffer(BYTE* pRestoredDataBuffer, int nDataLen, int* pnRestoredDataLen)
{
	// If given buffer is NULL, allocate necessary space here and return the pointer.
	// Additionaly, calculate the resulting length based on nDataLen and return it at the same time.
	if (pRestoredDataBuffer == NULL)
	{
		// Give the size of the transformed data buffer, based on the original data length
		*pnRestoredDataLen = GiveTransDataLen(nDataLen);
		// Ensure the pLocalRestBuffer that receive transformed data is big enought
		BYTE* pBuffer = CheckLocalRestBufferSize(*pnRestoredDataLen);
		return pBuffer; // Actually pBuffer = pLocalRestBuffer
	}

	// If we reach this point, pLocalTransBuffer must contain the transformed data to restore
	// Do the actual data padding/transformation/utilization or whatever here
	// In this Sample plugin we do nothing.
	// We just copy data into the destination buffer, without modifiying it.
	// WARNING: don't use the *pnRestoredDataLen value here, as its value can be
	// modified between the 2 calls to this function.
	// Instead use the GiveTransDataLen() and GiveRestDataLen() functions.
	int i;
	for (i = 0; i < nDataLen; i++)
	{
		pRestoredDataBuffer[i] = pLocalRestBuffer[i];
	}

	// return the resulting data length
	*pnRestoredDataLen = GiveRestDataLen(nDataLen);

	return pLocalRestBuffer;
}


//
// Free the DataBuffer and TransBuffer than have been allocated
// in TransformBuffer and RestoreBuffer, using the method adapted
// to the used allocation method.
//
TESTPLUGIN_API void FreeBuffer(BYTE* pBuffer)
{
	if (pBuffer != NULL) 
		free(pBuffer);

	return;
}


// -----------------------------------------------------------------
// End of functions that must be exported
// -----------------------------------------------------------------
 


// -----------------------------------------------------------------
//  Plugin internal Config Dialog Box Sample
// -----------------------------------------------------------------


// Move the given window to the centre of the screen 
// and bring it to the top.
void CentreWindow(HWND hwnd)
{
	RECT winrect, workrect;
	
	// Find how large the desktop work area is
	SystemParametersInfo(SPI_GETWORKAREA, 0, &workrect, 0);
	int workwidth = workrect.right -  workrect.left;
	int workheight = workrect.bottom - workrect.top;
	
	// And how big the window is
	GetWindowRect(hwnd, &winrect);
	int winwidth = winrect.right - winrect.left;
	int winheight = winrect.bottom - winrect.top;
	// Make sure it's not bigger than the work area
	winwidth = min(winwidth, workwidth);
	winheight = min(winheight, workheight);

	// Now centre it
	SetWindowPos(hwnd, 
		HWND_TOP,
		workrect.left + (workwidth-winwidth) / 2,
		workrect.top + (workheight-winheight) / 2,
		winwidth, winheight, 
		SWP_SHOWWINDOW);
	SetForegroundWindow(hwnd);
}


//
// Display the Plugin Config Dialog box
//
int DoDialog(void)
{
 	return DialogBoxParam(hInstance, MAKEINTRESOURCE(IDD_CONFIG_DIALOG), 
		NULL, (DLGPROC) ConfigDlgProc, (LONG) pPlugin);
}


//
// Config Dialog box callback
//
BOOL CALLBACK ConfigDlgProc(HWND hwnd,  UINT uMsg,  WPARAM wParam, LPARAM lParam )
{
	PLUGINSTRUCT* _this = (PLUGINSTRUCT*) GetWindowLong(hwnd, GWL_USERDATA);

	switch (uMsg)
	{
	case WM_INITDIALOG:
		{
			// Init the various fields with the saved values if they exist.
			CentreWindow(hwnd);
            return TRUE;
		}

	case WM_COMMAND:
		switch (LOWORD(wParam))
		{
		case IDOK:
			// Save the parameters in an ini file or registry for instance/
			// szLoaderType (environnement value) param can be used if necessary.
			return TRUE;

		case IDCANCEL:
			EndDialog(hwnd, FALSE);
			return TRUE;
		}
		break;

	case WM_DESTROY:
		EndDialog(hwnd, FALSE);
		return TRUE;
	}
	return 0;
}





// -----------------------------------------------------------------
// Others internal functions, some depending on what the Plugin does
// -----------------------------------------------------------------


//
//
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
// Calculate the len of the data after Transformation and return it. 
// 
// MANDATORY: The calculation must be possible by
// ONLY knowing the source data length ! (=> forget compression algos...)
//
int GiveTransDataLen(int nDataLen)
{
	int nTransDataLen = nDataLen; // In this example, the datalen remains unchanged

	return nTransDataLen;
}

//
// Calculate the len of the data after Restauration and return it. 
// 
// MANDATORY: The calculation must be possible by
// ONLY knowing the source data length ! (=> forget compression algos...)
//
int GiveRestDataLen(int nDataLen)
{
	int nRestDataLen = nDataLen; // In this example, the datalen remains unchanged

	return nRestDataLen;
}

//
// Allocate more space for the local transformation buffer if necessary
// and returns the pointer to this buffer
//
BYTE* CheckLocalTransBufferSize(int nBufferSize)
{
	if (nLocalTransBufferSize >= nBufferSize) return pLocalTransBuffer;

	BYTE *pNewBuffer = (BYTE *) malloc (nBufferSize + 256);
	if (pNewBuffer == NULL) 
	{
		return NULL;
	}
	if (pLocalTransBuffer != NULL)
		free(pLocalTransBuffer);

	pLocalTransBuffer = pNewBuffer;
	nLocalTransBufferSize = nBufferSize + 256;

	memset(pLocalTransBuffer, 0, nLocalTransBufferSize);

	return pLocalTransBuffer;
}


//
// Allocate more space for the local restoration buffer if necessary
// and returns the pointer to this buffer
//
BYTE* CheckLocalRestBufferSize(int nBufferSize)
{
	if (nLocalRestBufferSize >= nBufferSize) return pLocalRestBuffer;

	BYTE *pNewBuffer = (BYTE *) malloc (nBufferSize + 256);
	if (pNewBuffer == NULL) 
	{
		return NULL;
	}
	if (pLocalRestBuffer != NULL)
		free(pLocalRestBuffer);

	pLocalRestBuffer = pNewBuffer;
	nLocalRestBufferSize = nBufferSize + 256;

	memset(pLocalRestBuffer, 0, nLocalRestBufferSize);

	return pLocalRestBuffer;
}



//
// DLL Main Entry point  
// 
BOOL WINAPI DllMain( HINSTANCE hInst, 
                       DWORD  dwReason, 
                       LPVOID lpReserved
					 )
{
    switch (dwReason)
	{
		case DLL_PROCESS_ATTACH:
			hInstance = hInst;
		case DLL_THREAD_ATTACH:
		case DLL_THREAD_DETACH:
		case DLL_PROCESS_DETACH:
			break;
    }
    return TRUE;
}

