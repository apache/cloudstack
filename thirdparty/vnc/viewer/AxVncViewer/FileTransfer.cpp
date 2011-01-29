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


// FileTransfer.cpp: implementation of the FileTransfer class.

// sf@2002 - sf@2003 - sf@2004 - FileTransfer
// This class handles all the FileTransfer messages, events and procs, as well as the
// DialogBox which allows the user to browse Client ans Server disks-directories,
// and select some files to transfer between Client and Server.
//
// The GUI is very basic because I don't want to include MFC Classes in VNC...
// I use only Windows SDK.
//
//
// The GUI is now quite bearable, but following modifs could be done one day or another
// - Add more columns to FileLists (File type, Attributes...)
// - Add the possibility to sort files by colomns (File Size Order, File Ext order...)
// - Total progress should be based on total files' size instead of total number of files
// - Make the History persistent (file) so it's not lost each time the FileTransfer Win is closed
// - Clean-up the code (duplicated parts, arrays and strings dimensions checks...)
// - Display the total files size in the currently displayed directory
// - Remember the current directories - Partially done: the FT window can be minimized...


#include "stdhdrs.h"
#include "vncviewer.h"
#include "FileTransfer.h"
#include "Exception.h"
#include "commctrl.h"
#include "shlobj.h"
#include "zlib/zlib.h"

// These strings contain all the translated FT messages 
extern char sz_H1[64];
extern char sz_H2[64];
extern char sz_H3[128];
extern char sz_H4[64];
extern char sz_H5[64];
extern char sz_H6[64];
extern char sz_H7[64];
extern char sz_H8[64];
extern char sz_H9[64];
extern char sz_H10[64];
extern char sz_H11[64];
extern char sz_H12[64];
extern char sz_H13[64];
extern char sz_H14[64];
extern char sz_H15[64];
extern char sz_H16[64];
extern char sz_H17[64];
extern char sz_H18[64];
extern char sz_H19[64];
extern char sz_H20[64];
extern char sz_H21[64];
extern char sz_H22[64];
extern char sz_H23[64];
extern char sz_H24[64];
extern char sz_H25[64];
extern char sz_H26[64];
extern char sz_H27[64];
extern char sz_H28[64];
extern char sz_H29[64];
extern char sz_H30[64];
extern char sz_H31[64];
extern char sz_H32[64];
extern char sz_H33[64];
extern char sz_H34[64];
extern char sz_H35[64];
extern char sz_H36[64];
extern char sz_H37[64];
extern char sz_H38[128];
extern char sz_H39[64];
extern char sz_H40[64];
extern char sz_H41[64];
extern char sz_H42[64];
extern char sz_H43[128];
extern char sz_H44[64];
extern char sz_H45[64];
extern char sz_H46[128];
extern char sz_H47[64];
extern char sz_H48[64];
extern char sz_H49[64];
extern char sz_H50[64];
extern char sz_H51[64];
extern char sz_H52[64];
extern char sz_H53[64];
extern char sz_H54[64];
extern char sz_H55[64];
extern char sz_H56[64];
extern char sz_H57[64];

// Folder Transfer messages
extern char sz_H58[64];
extern char sz_H59[64];
extern char sz_H60[64];
extern char sz_H61[64];
extern char sz_H62[128];
extern char sz_H63[64];
extern char sz_H64[64];
extern char sz_H65[64];
extern char sz_H66[64];
extern char sz_H67[64];
extern char sz_H68[128];
extern char sz_H69[64];
extern char sz_H70[64];
extern char sz_H71[64];
extern char sz_H72[128];
extern char sz_H73[64];

// File/dir Rename messages
extern char sz_M1[64];
extern char sz_M2[64];
extern char sz_M3[64];
extern char sz_M4[64];
extern char sz_M5[64];
extern char sz_M6[64];
extern char sz_M7[64];
extern char sz_M8[64];

typedef BOOL (WINAPI *PGETDISKFREESPACEEX)(LPCSTR,PULARGE_INTEGER, PULARGE_INTEGER, PULARGE_INTEGER);

//
//
//
FileTransfer::FileTransfer(VNCviewerApp *pApp, ClientConnection *pCC)
{
//	vnclog.Print(0, _T("FileTransfer\n"));
	m_pApp	= pApp;
	m_pCC	= pCC;
	m_fAbort = false;
	m_fAborted = false;
	m_FilesList.clear();
	m_nFilesToTransfer = 0;
	m_nFilesTransfered = 0;
	m_fFileCommandPending = false;
	m_fFileTransferRunning = false;
	m_fFileDownloadRunning = false;
	m_fDirectoryReceptionRunning = false;
	m_fVisible = true;
	m_fFTAllowed = false;
	m_timer = 0;
	m_pZipUnZip = new CZipUnZip32();
	m_lpCSBuffer = NULL;
	m_nCSOffset = 0;
	m_nCSBufferSize = 0;
	m_nDeleteCount = 0;
	memset(m_szDeleteButtonLabel, 0, sizeof(m_szDeleteButtonLabel));
	memset(m_szNewFolderButtonLabel, 0, sizeof(m_szNewFolderButtonLabel));
	memset(m_szRenameButtonLabel, 0, sizeof(m_szRenameButtonLabel));
	m_fOldFTProtocole = false;
	m_nBlockSize = sz_rfbBlockSize;
	m_dwCurrentValue = 0;
	m_dwCurrentPercent = 0;
	m_fSendFileChunk = false;
	m_mmRes = -1; 
}

//
//
//
FileTransfer::~FileTransfer()
{
//	vnclog.Print(0, _T("nFileTransfer\n"));
	m_fFileCommandPending = false;
	m_fFileTransferRunning = false;
	m_FilesList.clear();
	if (m_pZipUnZip) delete m_pZipUnZip;
}


void FileTransfer::InitFTTimer()
{
	if (m_mmRes != -1) return;

	m_fSendFileChunk = false;
	m_mmRes = timeSetEvent( 1, 0, fpTimer, (DWORD)this, TIME_PERIODIC );
}


void FileTransfer::KillFTTimer()
{
	timeKillEvent(m_mmRes);
	m_mmRes = -1;
}


void CALLBACK FileTransfer::fpTimer(UINT uID, UINT uMsg, DWORD dwUser, DWORD dw1, DWORD dw2)
{
	FileTransfer* ft = (FileTransfer *) dwUser;

	if (!ft->m_fFileUploadRunning) return;

	if (!ft->m_fSendFileChunk)
	{
		ft->m_fSendFileChunk = true;

		ft->m_dwLastChunkTime = timeGetTime();

		SendMessage(ft->m_pCC->m_hwndMain, FileTransferSendPacketMessage, (WPARAM) 0, (LPARAM) 0);

		// sf@2005 - FileTransfer Temporization
		// - Prevents the windows message stack to be blocked too much when transfering over slow conenction
		// - Gives more priority to screen updates during asynchronous filetransfer
		long lDelta = timeGetTime() - ft->m_dwLastChunkTime;
		if (lDelta > 200)
		{
			//if (lDelta < 3000)
			//	Sleep(lDelta);
			//else
				Sleep(150);
		}
		else if (!ft->m_fVisible && !ft->m_fOldFTProtocole && !ft->m_pCC->IsDormant())
			Sleep(50);

		ft->m_fSendFileChunk = false;
	}
}

//
//
//
void FileTransfer::ShowFileTransferWindow(bool fVisible)
{
//	vnclog.Print(0, _T("ShowFileTransferWindow\n"));
	ShowWindow(hWnd, fVisible ? SW_RESTORE : SW_MINIMIZE);
	SetForegroundWindow(hWnd);
	// Put the FT Windows always on Top if fullscreen
	if (fVisible && m_pCC->InFullScreenMode())
	{
		RECT Rect;
		GetWindowRect(hWnd, &Rect);
		SetWindowPos(hWnd, 
					HWND_TOPMOST,
					Rect.left,
					Rect.top,
					Rect.right - Rect.left,
					Rect.bottom - Rect.top,
					SWP_SHOWWINDOW);
	}

	m_fVisible = fVisible; // This enables screen updates to be processed in ClientConnection
	// Refresh screen view if FileTransfer window has been hidden
	if (!fVisible)
		m_pCC->SendAppropriateFramebufferUpdateRequest();
}


//
// Simply the classic Windows message processing
//
bool PseudoYield(HWND hWnd)
{
//	vnclog.Print(0, _T("PseudoYield\n"));
	MSG msg;
	while (PeekMessage(&msg, hWnd, 0, 0, PM_REMOVE))
	{
		TranslateMessage(&msg);
		DispatchMessage(&msg);
		if (msg.message == WM_CLOSE) return FALSE;
	}
	return TRUE;
}



//
//	ProcessFileTransferMsg
//
//  Here we process all incoming FileTransferMsg stuff
//  coming from the server.
//  The server only sends FileTransfer data when requested
//  by the client. Possible request are:
//
//  - Send the list of your drives
//  - Send the content of a directory
//  - Send a file
//  - Accept a file
//  - ...
// 
//  We use the main ClientConnection thread and its
//  rfb message reception loop.
//  This function is called by the rfb message processing thread.
//  Thus it's safe to call the ReadExact and ReadString 
//  functions in the functions that are called from here:
//  PopulateRemoteListBox, ReceiveFile
// 
void FileTransfer::ProcessFileTransferMsg(void)
{
//	vnclog.Print(0, _T("ProcessFileTransferMsg\n"));
	rfbFileTransferMsg ft;
	m_pCC->ReadExact(((char *) &ft) + m_pCC->m_nTO, sz_rfbFileTransferMsg - m_pCC->m_nTO);

	switch (ft.contentType)
	{
	// Response to a rfbDirContentRequest request:
	// some directory data is received from the server
	case rfbDirPacket:
		switch (ft.contentParam)
		{
		// Response to a rfbRDrivesList request
		case rfbADrivesList:
			ListRemoteDrives(hWnd, Swap32IfLE(ft.length));
			m_fFileCommandPending = false;
			break;

		// Response to a rfbRDirContent request 
		case rfbADirectory:
		case rfbAFile:
			if (!m_fDirectoryReceptionRunning)
				PopulateRemoteListBox(hWnd, Swap32IfLE(ft.length));
			else
				ReceiveDirectoryItem(hWnd, Swap32IfLE(ft.length));
			break;
		default: // This is bad. Add rfbADirectoryEnd instead...
			if (m_fDirectoryReceptionRunning)
			{
				FinishDirectoryReception();
				m_fFileCommandPending = false;
			}
			break;

		}
		break;

	// In response to a rfbFileTransferRequest request
	// A file is received from the server.
	case rfbFileHeader:
		ReceiveFiles(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
		break;

	// In response to a rfbFileTransferOffer request
	// The server can send the checksums of the destination file before sending a ack through
	// rfbFileAcceptHeader (only if the destination file already exists and is accessible)
	case rfbFileChecksums:
		ReceiveDestinationFileChecksums(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
		break;

	// In response to a rfbFileTransferOffer request
	// A ack or nack is received from the server.
	case rfbFileAcceptHeader:
		SendFiles(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
		break;

	// Response to a command
	case rfbCommandReturn:
		switch (ft.contentParam)
		{
		case rfbADirCreate:
			CreateRemoteDirectoryFeedback(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
			m_fFileCommandPending = false;
			break;

		case rfbAFileDelete:
			DeleteRemoteFileFeedback(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
			m_fFileCommandPending = false;
			break;

		case rfbAFileRename:
			RenameRemoteFileOrDirectoryFeedback(Swap32IfLE(ft.size), Swap32IfLE(ft.length));
			m_fFileCommandPending = false;
			break;
		}
		break;
		
	// Should never be handled here but in the File Transfer Loop
	case rfbFilePacket:
		ReceiveFileChunk(Swap32IfLE(ft.length), Swap32IfLE(ft.size));
		break;

	// Should never be handled here but in the File Transfer Loop
	case rfbEndOfFile:
		FinishFileReception();
		break;

	// Abort current file transfer
	// For versions <=RC18 we also use it to test if we're allowed to use FileTransfer on the server
	case rfbAbortFileTransfer:
		// AbortFileDownload();
		if (m_fFileDownloadRunning)
		{
			m_fFileDownloadError = true;
			FinishFileReception();
		}
		else
		{
			// We want the viewer to be backward compatible with UltraWinVNC running the old FT protocole
			m_fOldFTProtocole = true;  // Old permission method -> it's a <=RC18 server
			m_nBlockSize = 4096; // Old packet size value...
			ShowWindow(GetDlgItem(hWnd, IDC_RENAME_B), SW_HIDE);

			TestPermission(Swap32IfLE(ft.size), 0);
		}
		break;
	
	// New FT handshaking/permission method (from RC19)
	case rfbFileTransferAccess:
		TestPermission(Swap32IfLE(ft.size), ft.contentParam);
		break;

	default:
		return;
		break;
	}
}


//
// request file transfer permission 
//
void FileTransfer::RequestPermission()
{
//	vnclog.Print(0, _T("RequestPermission\n"));

    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	// Versions <= RC18 method
	ft.contentType = rfbAbortFileTransfer; 
	// ft.contentParam = 0; 
	ft.contentParam = rfbFileTransferVersion; // Old viewer will send 0
	// New method can't be used yet as we want backward compatibility (new viewer FT must 
	// work with old UltraWinVNC FT
	// ft.contentType = rfbFileTransferAccess; 
	// ft.contentParam = rfbFileTransferVersion;
	ft.length = 0;
	ft.size = 0;
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	return;
}


//
// Test if we are allowed to access filetransfer
//
bool FileTransfer::TestPermission(long lSize, int nVersion)
{
//	vnclog.Print(0, _T("TestPermission\n"));
	if (lSize == -1)
	{
		m_fFTAllowed = false;
		HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
		SendDlgItemMessage(hWnd, IDC_REMOTE_DRIVECB, LB_RESETCONTENT, 0, 0L);
		ListView_DeleteAllItems(hWndRemoteList);
		SetDlgItemText(hWnd, IDC_CURR_REMOTE, sz_H1);
		SetDlgItemText(hWnd, IDC_REMOTE_STATUS, sz_H2);
		SetStatus(sz_H3);
		DisableButtons(hWnd);
		ShowWindow(GetDlgItem(hWnd, IDCANCEL), SW_SHOW);
	}
	else
	{
		m_fFTAllowed = true;
		RequestRemoteDrives();
		SetStatus(sz_H4);
	}

	return true;
}


//
// Receive all the files that are referenced in m_FilesList
//
bool FileTransfer::ReceiveFiles(unsigned long lSize, int nLen)
{
//	vnclog.Print(0, _T("ReceiveFiles\n"));
	// Receive the incoming file
	m_nFilesTransfered++;
	SetGlobalCount();
	if (!ReceiveFile(lSize, nLen))
		RequestNextFile();

	return true;
}

//
// A file has just been received
// Request the following one if any
//
bool FileTransfer::RequestNextFile()
{
//	vnclog.Print(0, _T("RequestNextFile\n"));

	SetGlobalCount();

	m_iFile++; // go to next file in the list of files to receive

	HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	// If one more file has to be received, ask for it !
	if (m_iFile != m_FilesList.end() && !m_fAbort)
	{
		TCHAR szSelectedFile[128];
		TCHAR szDstFile[MAX_PATH];
		memset(szSelectedFile, 0, 128);
		memset(szDstFile, 0, MAX_PATH);

		LVITEM Item;
		Item.mask = LVIF_TEXT;
		Item.iSubItem = 0;
		Item.pszText = szSelectedFile;
		Item.cchTextMax = 128;

		// Get the name file to receive in the remote list
		Item.iItem = *m_iFile;
		ListView_GetItem(hWndRemoteList, &Item);

		GetDlgItemText(hWnd, IDC_CURR_REMOTE, szDstFile, sizeof(szDstFile));
		if (!strlen(szDstFile)) return false; // no destination dir selected - msgbox ?
		strcat(szDstFile, szSelectedFile);

		RequestRemoteFile(szDstFile);
	}
	else // All the files have been processed and hopefully received
	{
		// Refresh the local list so new files are displayed and highlighted
		ListView_DeleteAllItems(hWndLocalList);
		PopulateLocalListBox(hWnd, "");

		if (m_fAbort)
			SetStatus(sz_H5);
		else
			SetStatus(sz_H6);

		EnableButtons(hWnd);

		ShowFileTransferWindow(true);
		Sleep(1000);
		//MessageBeep(-1);

		// Unlock 
		m_fFileCommandPending = false;
	}

	return true;
}


//
// Send all the files that are referenced in m_FilesList
//
bool FileTransfer::SendFiles(long lSize, int nLen)
{
	InitFTTimer(); // sf@2005

//	vnclog.Print(0, _T("SendFiles\n"));
	// Receive the incoming file
	m_nFilesTransfered++;
	SetGlobalCount();
	if (!SendFile(lSize, nLen))
		OfferNextFile();

	return true;
}


bool FileTransfer::OfferNextFile()
{
//	vnclog.Print(0, _T("OfferNextFile\n"));
	SetGlobalCount();

	m_iFile++; // go to next file in the list of files to send

	HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	// If one more file has to be sent, offer it !
	if (m_iFile != m_FilesList.end() && !m_fAbort)
	{
		TCHAR szSelectedFile[128];
		TCHAR szSrcFile[MAX_PATH];
		memset(szSelectedFile, 0, 128);
		memset(szSrcFile, 0, MAX_PATH);

		LVITEM Item;
		Item.mask = LVIF_TEXT;
		Item.iSubItem = 0;
		Item.pszText = szSelectedFile;
		Item.cchTextMax = 128;

		// Get the name of file to send in the local list
		Item.iItem = *m_iFile;
		ListView_GetItem(hWndLocalList, &Item);

		GetDlgItemText(hWnd, IDC_CURR_LOCAL, szSrcFile, sizeof(szSrcFile));
		if (!strlen(szSrcFile)) return false; // no destination dir selected - msgbox ?
		strcat(szSrcFile, szSelectedFile);

		if (!OfferLocalFile(szSrcFile))
		   SendFiles(-1, 0);
	}
	else // All the files have been processed and hopefully received
	{
		// Refresh the remote list so new files are displayed and highlighted
		ListView_DeleteAllItems(hWndRemoteList);
		RequestRemoteDirectoryContent(hWnd, "");

		if (m_fAbort)
			SetStatus(sz_H7);
		else
			SetStatus(sz_H6);

		EnableButtons(hWnd);

		ShowFileTransferWindow(true);
		Sleep(1000);
		//MessageBeep(-1);

		// Unlock 
		m_fFileCommandPending = false;

		KillFTTimer(); // sf@2005
	}

	return true;
}


//
// Format file size so it is user friendly to read
// 
void FileTransfer::GetFriendlyFileSizeString(__int64 Size, char* szText)
{
	szText[0] = '\0';
	if( Size > (1024*1024*1024) )
	{
		__int64 lRest = (Size % (1024*1024*1024));
		Size /= (1024*1024*1024);
		wsprintf(szText,"%u.%2.2lu Gb", (unsigned long)Size, (unsigned long)(lRest * 100 / 1024 / 1024 / 1024));
	}
	else if( Size > (1024*1024) )
	{
		unsigned long lRest = (Size % (1024*1024));
		Size /= (1024*1024);
		wsprintf(szText,"%u.%2.2lu Mb", (unsigned long)Size, lRest * 100 / 1024 / 1024);
	}
	else if ( Size > 1024 )
	{
		unsigned long lRest = Size % (1024);
		Size /= 1024;
		wsprintf(szText,"%u.%2.2lu Kb", (unsigned long)Size, lRest * 100 / 1024);
	}
	else
	{
		wsprintf(szText,"%u bytes", (unsigned long)Size);
	}
}


//
// GetFileSize() doesn't handle files > 4GBytes...
// GetFileSizeEx() doesn't exist under Win9x...
// So let's write our own function.
// 
bool FileTransfer::MyGetFileSize(char* szFilePath, ULARGE_INTEGER *n2FileSize)
{
	WIN32_FIND_DATA fd;
	HANDLE ff;

	SetErrorMode(SEM_FAILCRITICALERRORS); // No popup please !
	ff = FindFirstFile(szFilePath, &fd);
	SetErrorMode( 0 );

	if (ff == INVALID_HANDLE_VALUE)
	{
		return false;
	}

	FindClose(ff);

	(*n2FileSize).LowPart = fd.nFileSizeLow;
	(*n2FileSize).HighPart = fd.nFileSizeHigh;
	(*n2FileSize).QuadPart = (((__int64)fd.nFileSizeHigh) << 32 ) + fd.nFileSizeLow;
	
	return true;
}


//
// Add a file line to a ListView
// 
void FileTransfer::AddFileToFileList(HWND hWnd, int nListId, WIN32_FIND_DATA& fd, bool fLocalSide)
{
//	vnclog.Print(0, _T("AddFileToFileList\n"));
	char szFileName[MAX_PATH + 2];
	HWND hWndList = GetDlgItem(hWnd, nListId);

	// If we need to keep more info on the file, we can use LVITEM lParam 
	// (it will be usefull if we want to make comparison between local and remote files
	// for a resume function for instance, or for sorting purposes)
	// 
	// We could also display Files attributes and other Files times...

	if (((fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) == FILE_ATTRIBUTE_DIRECTORY && strcmp(fd.cFileName, "."))
		||
		(!strcmp(fd.cFileName, ".."))
	   )
	{
		sprintf(szFileName, "%s%s%s", rfbDirPrefix, fd.cFileName, rfbDirSuffix);
		char szUpDirMask[16];
		sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);

		if (!strcmp(szFileName, szUpDirMask) && nListId == IDC_LOCAL_FILELIST)
		{
			nListId = nListId;
		}
		// Name
		LVITEM Item;
		Item.mask = LVIF_TEXT | LVIF_IMAGE;
		Item.iItem = 0;
		Item.iSubItem = 0;
		Item.iImage = 0;
		Item.pszText = szFileName;
		int nItem = ListView_InsertItem(hWndList, &Item);
		
		// Type
		Item.mask = LVIF_TEXT;
		Item.iItem = nItem;
		Item.iSubItem = 1;
		Item.pszText = "Folder";
		ListView_SetItem(hWndList, &Item);

	}
	else if (strcmp(fd.cFileName, ".")) // Test actually Not necessary for remote list
	{
		// Name
		LVITEM Item;
		Item.mask = LVIF_TEXT | LVIF_IMAGE;
		Item.iItem = 0;
		Item.iSubItem = 0;
		Item.iImage = 1;
		Item.pszText = fd.cFileName;
		int nItem = ListView_InsertItem(hWndList,&Item);
		
		// Size
		__int64 Size = ( ((__int64)fd.nFileSizeHigh) << 32 ) + fd.nFileSizeLow;
		char szText[256];
		GetFriendlyFileSizeString(Size, szText);

		Item.mask = LVIF_TEXT;
		Item.iItem = nItem;
		Item.iSubItem = 1;
		Item.pszText = szText;
		ListView_SetItem(hWndList, &Item);
		
		// Last Modif Time
		// sf@2003
		// For now, we've made the choice off displaying all the files 
		// off client AND server sides converted in clients local
		// time only. So we ALSO convert server's files times in client local time
		FILETIME LocalFileTime;
		FileTimeToLocalFileTime(&fd.ftLastWriteTime, &LocalFileTime);
		SYSTEMTIME FileTime;
		FileTimeToSystemTime(fLocalSide ? &LocalFileTime : &LocalFileTime /*&fd.ftLastWriteTime*/, &FileTime);
		wsprintf(szText,"%2.2d/%2.2d/%4.4d %2.2d:%2.2d",
				FileTime.wMonth,
				FileTime.wDay,
				FileTime.wYear,
				FileTime.wHour,
				FileTime.wMinute
				);

		Item.mask = LVIF_TEXT;
		Item.iItem = nItem;
		Item.iSubItem = 2;
		Item.pszText = szText;
		ListView_SetItem(hWndList,&Item);
	}

}


//
// Select the new transfered files in the dest FileList so the user find them easely
//
void FileTransfer::HighlightTransferedFiles(HWND hSrcList, HWND hDstList)
{
//	vnclog.Print(0, _T("HighlightTransferedFiles\n"));
	if (m_FilesList.size() > 0)
	{
		TCHAR szSelectedFile[128];

		LVITEM Item;
		Item.mask = LVIF_TEXT;
		Item.iSubItem = 0;
		Item.pszText = szSelectedFile;
		Item.cchTextMax = 128;

		LVFINDINFO Info;
		Info.flags = LVFI_STRING;
		Info.psz = (LPSTR)szSelectedFile;

		for (m_iFile = m_FilesList.begin();
			m_iFile != m_FilesList.end();
			m_iFile++)
		{
			// Get the name of the file sent
			Item.iItem = *m_iFile;
			ListView_GetItem(hSrcList, &Item);

			// Find this file in the list and highlight it
			int nTheIndex = ListView_FindItem(hDstList, -1, &Info);
			if (nTheIndex > -1)
			{
				ListView_SetItemState(hDstList, nTheIndex, LVIS_SELECTED, LVIS_SELECTED);
				ListView_EnsureVisible(hDstList, nTheIndex, FALSE);
			}
		}	
		m_FilesList.clear();
	}

}


//
//
//
bool FileTransfer::IsShortcutFolder(LPSTR szPath)
{
//	vnclog.Print(0, _T("IsShortcutFolder\n"));
	// Todo: Cultures Translation
	char szGUIDir[64];

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "My Documents", rfbDirSuffix);
	if (!strnicmp(szPath, szGUIDir, strlen(szGUIDir)))
		return true;

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Desktop", rfbDirSuffix);
	if (!strnicmp(szPath, szGUIDir, strlen(szGUIDir)))
		return true;

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Network Favorites", rfbDirSuffix);
	if (!strnicmp(szPath, szGUIDir, strlen(szGUIDir)))
		return true;

	return false;
}


//
//
//
bool FileTransfer::ResolvePossibleShortcutFolder(HWND hWnd, LPSTR szFolder)
{
//	vnclog.Print(0, _T("ResolvePossibleShortcutFolder\n"));
	TCHAR szP[MAX_PATH];
	int nFolder = -1;

	char szGUIDir[64];

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "My Documents", rfbDirSuffix);
	if (!strnicmp(szFolder, szGUIDir, strlen(szGUIDir)))
		nFolder = CSIDL_PERSONAL;

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Desktop", rfbDirSuffix);
	if (!strnicmp(szFolder, szGUIDir, strlen(szGUIDir)))
		nFolder = CSIDL_DESKTOP;

	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Network Favorites", rfbDirSuffix);
	if (!strnicmp(szFolder, szGUIDir, strlen(szGUIDir)))
		nFolder = CSIDL_NETHOOD;

	/*
	if (!strnicmp(szFolder, "(Net. Shares)", 9))
		nFolder = CSIDL_NETWORK;
	*/

	if (nFolder != -1)
	{
		// if (SHGetSpecialFolderPath(NULL, szP, nFolder, FALSE))
		if (GetSpecialFolderPath(nFolder, szP))
		{
			strcat(szP,"\\");
			SetDlgItemText(hWnd, IDC_CURR_LOCAL, szP);
		}
		return true;
	}
	return false;
}


bool FileTransfer::GetSpecialFolderPath(int nId, char* szPath)
{
	LPITEMIDLIST pidl;

	if (SHGetSpecialFolderLocation(0, nId, &pidl) != NOERROR)
		return false;

	if (!SHGetPathFromIDList(pidl, szPath) )
		return false;

	return true;
}
//
// Populate the local machine listbox with files located in szPath
//
void FileTransfer::PopulateLocalListBox(HWND hWnd, LPSTR szPath)
{
//	vnclog.Print(0, _T("PopulateLocalListBox\n"));
	char ofDir[MAX_PATH];
	char ofDirT[MAX_PATH];
	int nSelected = -1;
	int nCount = 0;
	int nFileCount = 0;
	char szLocalStatus[128];

	HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	ofDir[0] = '\0';
	ofDirT[0] = '\0';

	if (lstrlen(szPath) == 0)
	{
		nCount = ListView_GetItemCount(hWndLocalList);
		for (nSelected = 0; nSelected < nCount; nSelected++)
		{
			if(ListView_GetItemState(hWndLocalList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
			{
				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iItem = nSelected;
				Item.iSubItem = 0;
				Item.pszText = ofDirT;
				Item.cchTextMax = MAX_PATH;
				ListView_GetItem(hWndLocalList, &Item);
				break;
			}
		}
		if (ResolvePossibleShortcutFolder(hWnd, ofDirT))
			ofDirT[0] = '\0';
	}
	else
	{
		// Usual shortcuts case
		if (ResolvePossibleShortcutFolder(hWnd, szPath))
		{
		}
		else
		{
			// General case
			szPath[6] = '\0';
			// szPath always contains a drive letter (X:) or (..)
			strcpy(ofDirT, szPath);
			// In the case of (..) we keep the current path intact
			char szUpDirMask[16];
			sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
			if (strcmp(ofDirT, szUpDirMask))
				SetDlgItemText(hWnd, IDC_CURR_LOCAL, "");
		}
	}

	if (nSelected == nCount || lstrlen(ofDirT) == 0)
	{
		GetDlgItemText(hWnd, IDC_CURR_LOCAL, ofDirT, sizeof(ofDirT));
		if (strlen(ofDirT) == 0) return; 
	}
	else
	{
		if (ofDirT[0] == rfbDirPrefix[0] && ofDirT[1] == rfbDirPrefix[1])
		{
			strncpy(ofDir, ofDirT + 2, strlen(ofDirT) - 3); 
			ofDir[strlen(ofDirT) - 4] = '\0';
		}
		else
			return;

		GetDlgItemText(hWnd, IDC_CURR_LOCAL, ofDirT, sizeof(ofDirT));
		if (!stricmp(ofDir, ".."))
		{	
			char* p;
			ofDirT[strlen(ofDirT) - 1] = '\0';
			p = strrchr(ofDirT, '\\');
			if (p == NULL) return;
			*p = '\0';
		}
		else
			strcat(ofDirT, ofDir);
		strcat(ofDirT, "\\");
		SetDlgItemText(hWnd, IDC_CURR_LOCAL, ofDirT);
	}
	strcpy(ofDir, ofDirT);
	strcat(ofDir, "*");

	// Select the good drive in the drives combo box (the first time only)
	int nIndex = SendDlgItemMessage(hWnd, IDC_LOCAL_DRIVECB, CB_GETCURSEL, 0, 0L);
	if (nIndex == LB_ERR)
	{
	    char szDrive[5];
		strcpy(szDrive, rfbDirPrefix);
	    strncat(szDrive, ofDir, 2);
	    nIndex = SendDlgItemMessage(hWnd, IDC_LOCAL_DRIVECB, CB_FINDSTRING, -1, (LPARAM)(LPSTR)szDrive); 
	    SendDlgItemMessage(hWnd, IDC_LOCAL_DRIVECB, CB_SETCURSEL, nIndex, 0L);
	}

	sprintf(szLocalStatus, sz_H8); 
	SetDlgItemText(hWnd, IDC_LOCAL_STATUS, szLocalStatus);

	ListView_DeleteAllItems(hWndLocalList);

	WIN32_FIND_DATA fd;
	HANDLE ff;
	int bRet = 1;

	SetErrorMode(SEM_FAILCRITICALERRORS); // No popup please !
	ff = FindFirstFile(ofDir, &fd);
	SetErrorMode( 0 );

	if (ff == INVALID_HANDLE_VALUE)
	{
		sprintf(szLocalStatus, sz_H9); 
		SetDlgItemText(hWnd, IDC_LOCAL_STATUS, szLocalStatus);
		return;
	}

	while (bRet != 0)
	{
		AddFileToFileList(hWnd, IDC_LOCAL_FILELIST, fd, true);
		nFileCount++;
		if (!PseudoYield(GetParent(hWnd))) return;
		bRet = FindNextFile(ff, &fd);
	}

	FindClose(ff);

	sprintf(szLocalStatus, " > %d %s", nFileCount, sz_H58); 
	SetDlgItemText(hWnd, IDC_LOCAL_STATUS, szLocalStatus);

	// If some files have been received 
	HighlightTransferedFiles( hWndRemoteList, hWndLocalList);

}



//
// Request the contents of a remote directory
//
void FileTransfer::RequestRemoteDirectoryContent(HWND hWnd, LPSTR szPath)
{
//	vnclog.Print(0, _T("RequestRemoteDirectoryContent\n"));
	if (!m_fFTAllowed)
	{
		m_fFileCommandPending = false;
		return;
	}

	char ofDir[MAX_PATH];
	char ofDirT[MAX_PATH];
	int nSelected = -1;
	int nCount = 0;
	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	ofDir[0] = '\0';
	ofDirT[0] = '\0';

	if (lstrlen(szPath) == 0)
	{
		nCount = ListView_GetItemCount(hWndRemoteList);
		for (nSelected = 0; nSelected < nCount; nSelected++)
		{
			if(ListView_GetItemState(hWndRemoteList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
			{
				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iItem = nSelected;
				Item.iSubItem = 0;
				Item.pszText = ofDirT;
				Item.cchTextMax = MAX_PATH;
				ListView_GetItem(hWndRemoteList, &Item);
				break;
			}
		}
	}
	else
	{
		if (!IsShortcutFolder(szPath))
			szPath[6] = '\0';
		// szPath always contains a drive letter (X:) or (..)
		strcpy(ofDirT, szPath);
		// In the case of (..) we keep the current path intact
		char szUpDirMask[16];
		sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
		if (strcmp(ofDirT, szUpDirMask))
			SetDlgItemText(hWnd, IDC_CURR_REMOTE, "");
	}

	if (nSelected == nCount || lstrlen(ofDirT) == 0)
	{
		GetDlgItemText(hWnd, IDC_CURR_REMOTE, ofDirT, sizeof(ofDirT));
		if (strlen(ofDirT) == 0) 
		{
			m_fFileCommandPending = false;
			return; 
		}
	}
	else
	{
		if (ofDirT[0] == rfbDirPrefix[0] && ofDirT[1] == rfbDirPrefix[1])
		{
			strncpy(ofDir, ofDirT + 2, strlen(ofDirT) - 3); 
			ofDir[strlen(ofDirT) - 4] = '\0';
		}
		else
		{
			m_fFileCommandPending = false;
			return;
		}

		GetDlgItemText(hWnd, IDC_CURR_REMOTE, ofDirT, sizeof(ofDirT));
		if (!stricmp(ofDir, ".."))
		{	
			char* p;
			ofDirT[strlen(ofDirT) - 1] = '\0';
			p = strrchr(ofDirT, '\\');
			if (p == NULL)
			{
				m_fFileCommandPending = false;
				return;
			}
			*p = '\0';
		}
		else
			strcat(ofDirT, ofDir);
		strcat(ofDirT, "\\");
		SetDlgItemText(hWnd, IDC_CURR_REMOTE, ofDirT);
	}
	strcpy(ofDir, ofDirT);

	// Todo: In case of shortcuts dir, do a translation here !

	// Select the good drive in the drives combo box (the first time only)
	int nIndex = SendDlgItemMessage(hWnd, IDC_REMOTE_DRIVECB, CB_GETCURSEL, 0, 0L);
	if (nIndex == LB_ERR)
	{
	    char szDrive[5];
		strcpy(szDrive, rfbDirPrefix);
	    strncat(szDrive, ofDir, 2);
	    nIndex = SendDlgItemMessage(hWnd, IDC_REMOTE_DRIVECB, CB_FINDSTRING, -1, (LPARAM)(LPSTR)szDrive); 
	    SendDlgItemMessage(hWnd, IDC_REMOTE_DRIVECB, CB_SETCURSEL, nIndex, 0L);
	}

	ListView_DeleteAllItems(hWndRemoteList);

    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbDirContentRequest;
    ft.contentParam = rfbRDirContent; // Directory content please
	ft.length = Swap32IfLE(strlen(ofDir));
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	m_pCC->WriteExact((char *)ofDir, strlen(ofDir));

	return;
}


//
// Populate the remote machine listbox with files received from server
//
void FileTransfer::PopulateRemoteListBox(HWND hWnd, int nLen)
{
//	vnclog.Print(0, _T("PopulateRemoteListBox\n"));
	char szRemoteStatus[128];

	// If the distant media is not browsable for some reason
	if (nLen == 0)
	{
		sprintf(szRemoteStatus, sz_H10); 
		SetDlgItemText(hWnd, IDC_REMOTE_STATUS, szRemoteStatus);
		return;
	}

	// sf@2004 - Read the returned Directory full path
	if (nLen > 1 && !m_fOldFTProtocole)
	{
		TCHAR szPath[MAX_PATH];
		if (nLen > sizeof(szPath)) return;
		m_pCC->ReadString((char *)szPath, nLen);
		SetDlgItemText(hWnd, IDC_CURR_REMOTE, szPath);
	}

	sprintf(szRemoteStatus, sz_H11); 
	SetDlgItemText(hWnd, IDC_REMOTE_STATUS, szRemoteStatus);

	// The dir in the current packet
	memset(&m_fd, '\0', sizeof(WIN32_FIND_DATA));

	m_nFileCount = 0;
	m_fDirectoryReceptionRunning = true;

	// FT Backward compatibility DIRTY hack for DSMPlugin mode...
	if (m_fOldFTProtocole && m_pCC->m_fUsePlugin)
	{
		m_pCC->m_nTO = 0;
		ProcessFileTransferMsg();
	}

	//ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_REMOTE_FILELIST));

}


//
//
//
void FileTransfer::ReceiveDirectoryItem(HWND hWnd, int nLen)
{
//	vnclog.Print(0, _T("ReceiveDirectoryItem\n"));
	if (!m_fDirectoryReceptionRunning) return;

	if (nLen > sizeof(m_szFileSpec)) return;

	// Read the File/Directory full info
	m_pCC->ReadString((char *)m_szFileSpec, nLen);
	memset(&m_fd, '\0', sizeof(WIN32_FIND_DATA));
	memcpy(&m_fd, m_szFileSpec, nLen);
	AddFileToFileList(hWnd, IDC_REMOTE_FILELIST, m_fd, false);
	m_nFileCount++;

	// PseudoYield(pFileTransfer->hWnd);
	if (!PseudoYield(GetParent(hWnd))) return;

	// FT Backward compatibility DIRTY hack for DSMPlugin mode...
	if (m_fOldFTProtocole && m_pCC->m_fUsePlugin)
	{
		m_pCC->m_nTO = 0;
		ProcessFileTransferMsg();
	}

}


//
//
//
void FileTransfer::FinishDirectoryReception()
{
//	vnclog.Print(0, _T("FinishDirectoryReception\n"));
	if (!m_fDirectoryReceptionRunning) return;

	HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	char szRemoteStatus[128];
	sprintf(szRemoteStatus, " > %d %s", m_nFileCount, sz_H58); 
	SetDlgItemText(hWnd, IDC_REMOTE_STATUS, szRemoteStatus);

	// If some files have been sent to remote side highlight them 
	HighlightTransferedFiles(hWndLocalList, hWndRemoteList);
	// UpdateWindow(hWnd);

	m_fDirectoryReceptionRunning = false;

}

//
// request the list of remote drives 
//
void FileTransfer::RequestRemoteDrives()
{
//	vnclog.Print(0, _T("RequestRemoteDrives\n"));
	if (!m_fFTAllowed) return;

	// TODO : hook error !
    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbDirContentRequest;
    ft.contentParam = rfbRDrivesList; // List of Remote Drives please
	ft.length = 0;
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);

	return;
}


//
// Fill the Remote FilesList and remote drives combo box 
//
void FileTransfer::ListRemoteDrives(HWND hWnd, int nLen)
{
//	vnclog.Print(0, _T("ListRemoteDrives\n"));
	TCHAR szDrivesList[256]; // Format when filled : "C:t<NULL>D:t<NULL>....Z:t<NULL><NULL>"
	TCHAR szDrive[4];
	TCHAR szTheDrive[128];
	TCHAR szType[32];
	int nIndex = 0;

	if (nLen > sizeof(szDrivesList)) return;

	m_pCC->ReadString((char *)szDrivesList, nLen);

	HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

	SendDlgItemMessage(hWnd, IDC_REMOTE_DRIVECB, LB_RESETCONTENT, 0, 0L);
	ListView_DeleteAllItems(hWndRemoteList);
	SetDlgItemText(hWnd, IDC_CURR_REMOTE, "");

	// Fill the tree with the remote drives
	while (nIndex < nLen - 3)
	{
		strcpy(szDrive, szDrivesList + nIndex);
		nIndex += 4;

		// Get the type of drive
		switch (szDrive[2])
		{
		case 'l':
			sprintf(szType, "%s", "Local Disk");
			break;
		case 'f':
			sprintf(szType, "%s", "Removable");
			break;
		case 'c':
			sprintf(szType, "%s", "CD-ROM");
			break;
		case 'n':
			sprintf(szType, "%s", "Network");
			break;
		default:
			sprintf(szType, "%s", "Unknown");
			break;
		}

		szDrive[2] = '\0'; // remove the type char
		sprintf(szTheDrive, "%s%s%s", rfbDirPrefix, szDrive, rfbDirSuffix);

		LVITEM Item;
		Item.mask = LVIF_TEXT | LVIF_IMAGE;
		Item.iItem = 0;
		Item.iSubItem = 0;
		Item.iImage = 2;
		Item.pszText = szTheDrive;
		int nItem = ListView_InsertItem(hWndRemoteList, &Item);
		
		Item.mask = LVIF_TEXT;
		Item.iItem = nItem;
		Item.iSubItem = 1;
		Item.pszText = szType;
		ListView_SetItem(hWndRemoteList, &Item);

		// Prepare it for Combo Box and add it
		strcat(szTheDrive, " - ");
		strcat(szTheDrive, szType);

		SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szTheDrive); 

	}

	// List the usual shorcuts

	if (!m_fOldFTProtocole)
	{
	char szGUIDir[64];

	// MyDocuments
	LVITEM Item;
	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "My Documents", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndRemoteList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 

	// Desktop
	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Desktop", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndRemoteList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 

	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Network Favorites", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndRemoteList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 
	}

	SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_SETCURSEL, -1, 0);
}


//
// List local drives
//
void FileTransfer::ListDrives(HWND hWnd)
{
//	vnclog.Print(0, _T("ListDrives\n"));
	TCHAR szDrivesList[256]; // Format when filled : "C:\<NULL>D:\<NULL>....Z:\<NULL><NULL>"
	TCHAR szDrive[4];
	TCHAR szTheDrive[32];
	TCHAR szType[32];
	UINT nType = 0;
	DWORD dwLen;
	int nIndex = 0;
	dwLen = GetLogicalDriveStrings(256, szDrivesList);

	HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	SendDlgItemMessage(hWnd, IDC_LOCAL_DRIVECB, LB_RESETCONTENT, 0, 0L);

	ListView_DeleteAllItems(hWndLocalList);
	SetDlgItemText(hWnd, IDC_CURR_LOCAL, "");

	// Parse the list of drives
	while (nIndex < dwLen - 3)
	{
		strcpy(szDrive, szDrivesList + nIndex);
		nIndex += 4;
		szDrive[2] = '\0'; // remove the '\'
		sprintf(szTheDrive, "%s%s%s", rfbDirPrefix, szDrive, rfbDirSuffix);

		// szName[0] = '\0';
		szType[0] = '\0';

		strcat(szDrive, "\\");

		// GetVolumeInformation(szDrive, szName, sizeof(szName), NULL, NULL, NULL, NULL, NULL);

		// Get infos on the Drive (type and Name)
		nType = GetDriveType(szDrive);
		switch (nType)
		{
        case DRIVE_FIXED:
			sprintf(szType, "%s", "Local Disk");
			break;
		case DRIVE_REMOVABLE:
			sprintf(szType, "%s", "Removable");
			break;
        case DRIVE_CDROM:
			sprintf(szType, "%s", "CD-ROM");
			break;
        case DRIVE_REMOTE:
			sprintf(szType, "%s", "Network");
			break;
		default:
			sprintf(szType, "%s", "Unknown");
			break;
		}

		// Add it to the ListView
		LVITEM Item;
		Item.mask = LVIF_TEXT | LVIF_IMAGE;
		Item.iItem = 0;
		Item.iSubItem = 0;
		Item.iImage = 2;
		Item.pszText = szTheDrive;
		int nItem = ListView_InsertItem(hWndLocalList, &Item);
		
		Item.mask = LVIF_TEXT;
		Item.iItem = nItem;
		Item.iSubItem = 1;
		Item.pszText = szType;
		ListView_SetItem(hWndLocalList, &Item);

		// Prepare it for Combo Box and add it
		strcat(szTheDrive, " - ");
		strcat(szTheDrive, szType);

		SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szTheDrive); 
	}

	// List the usual shorcuts

	char szGUIDir[64];

	// MyDocuments
	LVITEM Item;
	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "My Documents", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndLocalList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 

	// Desktop
	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Desktop", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndLocalList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 

	// MyDocuments
	Item.mask = LVIF_TEXT;
	Item.iItem = 0;
	Item.iSubItem = 0;
	sprintf(szGUIDir, "%s%s%s", rfbDirPrefix, "Network Favorites", rfbDirSuffix);
	Item.pszText = szGUIDir; // Todo: Fr/De
	ListView_InsertItem(hWndLocalList, &Item);
	SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_ADDSTRING, 0, (LPARAM)szGUIDir); 

	SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_SETCURSEL, -1, 0);
}


//
// Set gauge max value
//
void FileTransfer::SetTotalSize(HWND hWnd, DWORD dwTotalSize)
{
//	vnclog.Print(0, _T("SetTotalSize\n"));
	SendDlgItemMessage(hWnd, IDC_PROGRESS, PBM_SETPOS, (WPARAM)0, (LPARAM)0L);
	SendDlgItemMessage(hWnd, IDC_PROGRESS, PBM_SETRANGE, (WPARAM)0, MAKELPARAM(0, m_nBlockSize));
}

//
// Set gauge value
//
void FileTransfer::SetGauge(HWND hWnd, __int64 dwCount)
{
//	vnclog.Print(0, _T("SetGauge\n"));
	DWORD dwSmallerCount = (DWORD)(dwCount / m_nBlockSize);
	DWORD dwSmallerFileSize = (DWORD)(m_nnFileSize / m_nBlockSize);
	if (dwSmallerFileSize == 0) dwSmallerFileSize = 1;
	DWORD dwValue = (DWORD)( (((__int64)(dwSmallerCount) * m_nBlockSize / dwSmallerFileSize)));
	if (dwValue != m_dwCurrentValue)
	{
		SendDlgItemMessage(hWnd, IDC_PROGRESS, PBM_SETPOS, dwValue, 0);
		m_dwCurrentValue = dwValue;
	}

	DWORD dwPercent = (DWORD)(((__int64)(dwSmallerCount) * 100 / dwSmallerFileSize));
	if (dwPercent != m_dwCurrentPercent)
	{
		char szPercent[5];
		sprintf(szPercent, "%d%%", dwPercent);
		SetDlgItemText(hWnd, IDC_PERCENT, szPercent);
		m_dwCurrentPercent = dwPercent;
	}
}


//
// Display global progress (ratio files transfered/Total Files To transfer)
//
void FileTransfer::SetGlobalCount()
{
//	vnclog.Print(0, _T("SetGlobalCount\n"));
	char szGlobal[64];
	
	wsprintf(szGlobal,
			"File %d/%d",
			m_nFilesTransfered,
			m_nFilesToTransfer
			);
	SetDlgItemText(hWnd, IDC_GLOBAL_STATUS, szGlobal);
}


//
// Display current status and add it to the history combo box
//
void FileTransfer::SetStatus(LPSTR szStatus)
{
//	vnclog.Print(0, _T("SetStatus\n"));
	// time_t lTime;
	char dbuffer [9];
	char tbuffer [9];

	char szHist[255 + 64];

	SetDlgItemText(hWnd, IDC_STATUS, szStatus);
	_tzset();
	// time(&lTime);
	_strdate(dbuffer);
	_strtime(tbuffer);
	sprintf(szHist, " > %s %s - %s", dbuffer, tbuffer/*ctime(&lTime)*/, szStatus);
	LRESULT Index = SendMessage(GetDlgItem(hWnd, IDC_HISTORY_CB), CB_ADDSTRING, 0, (LPARAM)szHist); 
	SendMessage(GetDlgItem(hWnd, IDC_HISTORY_CB), CB_SETCURSEL, (WPARAM)Index, (LPARAM)0);		
}


//
// Request a file
//
void FileTransfer::RequestRemoteFile(LPSTR szRemoteFileName)
{

//	vnclog.Print(0, _T("RequestRemoteFile\n"));
	if (!m_fFTAllowed) return;

	// Ensure Backward FT compatibility (Directory reception)....
	if (m_fOldFTProtocole)
	{
		char* p1 = strrchr(szRemoteFileName, '\\') + 1;
		char* p2 = strrchr(szRemoteFileName, rfbDirSuffix[0]);
		if (
			p1[0] == rfbDirPrefix[0] && p1[1] == rfbDirPrefix[1]  // Check dir prefix
			&& p2[0] == rfbDirSuffix[0] && p2[1] == rfbDirSuffix[1] && p2 != NULL && p1 < p2  // Check dir suffix
			) //
		{
			// p1 = strrchr(szRemoteFileName, '\\') + 1;
			char szDirectoryName[MAX_PATH];
			strcpy(szDirectoryName, p1 + 2); // Skip dir prefix (2 chars)
			szDirectoryName[strlen(szDirectoryName) - 2] = '\0'; // Remove dir suffix (2 chars)
			*p1 = '\0';
			strcat(szRemoteFileName, "("),
			strcat(szRemoteFileName, szDirectoryName);
			strcat(szRemoteFileName, ")");
		}
	}

	// TODO : hook error !
    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbFileTransferRequest;
    ft.contentParam = 0;
	ft.length = Swap32IfLE(strlen(szRemoteFileName));
	ft.size = (m_pCC->kbitsPerSecond > 2048) ? Swap32IfLE(0) : Swap32IfLE(1); // 1 means "Enable compression" 
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
    m_pCC->WriteExact((char *)szRemoteFileName, strlen(szRemoteFileName));

	return;
}


//
// Receive a file
//
bool FileTransfer::ReceiveFile(unsigned long lSize, int nLen)
{
//	vnclog.Print(0, _T("ReceiveFile\n"));
	if (!m_fFTAllowed) return false;

	int fError = false;

	rfbFileTransferMsg ft;
	ft.type = rfbFileTransfer;
	ft.contentType = rfbFileHeader;
	ft.size = Swap32IfLE(0);
	ft.length = Swap32IfLE(0);

	char *szRemoteFileName = new char [nLen+1];
	if (szRemoteFileName == NULL) return false;
	memset(szRemoteFileName, 0, nLen+1);

	// Read in the Name of the file to copy (remote full name !)
	m_pCC->ReadExact(szRemoteFileName, nLen);

	char szStatus[512];

	// If lSize = -1 (0xFFFFFFFF) that means that the Src file on the remote machine
	// could not be opened for some reason (locked, doesn't exits any more...)
	if (lSize == -1)
	{
		sprintf(szStatus, " %s < %s > %s", sz_H12, szRemoteFileName, sz_H13); 
		// SetDlgItemText(pFileTransfer->hWnd, IDC_STATUS, szStatus);
		SetStatus(szStatus);
		delete [] szRemoteFileName;
		return false;
	}

	// sf@2004 - The file size can be wrong for huge files (>4Gb)
	// idealy we should pass another param (sizeH) in the rfbFileTransfer msg (same thing
	// for the Date/Time) but we want to maintain backward compatibility between all Ultra V1 RC version..
	// So instead we pass the additionnal High size param after the received string...of the current msg
	// Parse the FileTime and isolate filename
	CARD32 sizeH = 0;
	if (!m_fOldFTProtocole)
	{
		CARD32 sizeHtmp;
		m_pCC->ReadExact((char*)&sizeHtmp, sizeof(CARD32));
		sizeH = Swap32IfLE(sizeHtmp);
	}

	// Get the current path (destination path)
	GetDlgItemText(hWnd, IDC_CURR_LOCAL, m_szDestFileName, sizeof(m_szDestFileName));

	char *p = strrchr(szRemoteFileName, ',');
	if (p == NULL)
		m_szIncomingFileTime[0] = '\0';
	else 
	{
		strcpy(m_szIncomingFileTime, p+1);
		*p = '\0';
	}

	// Check the free space on local destination drive
	bool fErr = false;
	ULARGE_INTEGER lpFreeBytesAvailable;
	ULARGE_INTEGER lpTotalBytes;		
	ULARGE_INTEGER lpTotalFreeBytes;
	unsigned long dwFreeKBytes;
	char *szDestPath = new char [strlen(m_szDestFileName) + 1];
	memset(szDestPath, 0, strlen(m_szDestFileName) + 1);
	strcpy(szDestPath, m_szDestFileName);
	*strrchr(szDestPath, '\\') = '\0'; // We don't handle UNCs for now

	PGETDISKFREESPACEEX pGetDiskFreeSpaceEx;
	pGetDiskFreeSpaceEx = (PGETDISKFREESPACEEX)GetProcAddress( GetModuleHandle("kernel32.dll"),"GetDiskFreeSpaceExA");
	if (pGetDiskFreeSpaceEx)
						{

						if (!pGetDiskFreeSpaceEx((LPCTSTR)szDestPath,
							&lpFreeBytesAvailable,
							&lpTotalBytes,
							&lpTotalFreeBytes)) fErr = true;
						}
	delete [] szDestPath;
	dwFreeKBytes  = (unsigned long) (Int64ShraMod32(lpFreeBytesAvailable.QuadPart, 10));
	if (dwFreeKBytes < (unsigned long)(lSize / 1000)) fErr = true;

	if (fErr)
	{
		sprintf(szStatus, " %s < %s >",sz_H14, strrchr(szRemoteFileName, '\\') + 1); 
		SetStatus(szStatus);
		delete [] szRemoteFileName;
		// Tell the server to cancel the transfer
		ft.size = Swap32IfLE(-1);
		if (m_fOldFTProtocole)
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
		else
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
		return false;
	}

	strcat(m_szDestFileName, strrchr(szRemoteFileName, '\\') + 1);

	m_nnFileSize = (((__int64)(sizeH)) << 32) + lSize;
	char szFFS[96];
	GetFriendlyFileSizeString(m_nnFileSize, szFFS);
	sprintf(szStatus, " %s < %s > (%s) <<<",
			sz_H15, m_szDestFileName , szFFS/*, szRemoteFileName*/); 
	SetStatus(szStatus);

	SetTotalSize(hWnd, lSize); // In bytes
	SetGauge(hWnd, 0); // In bytes
	UpdateWindow(hWnd);

	// Create the local Destination file
	m_hDestFile = CreateFile(m_szDestFileName, 
							GENERIC_WRITE | GENERIC_READ,
							FILE_SHARE_READ | FILE_SHARE_WRITE, 
							NULL,
							OPEN_ALWAYS,
							FILE_FLAG_SEQUENTIAL_SCAN,
							NULL);

	// sf@2004 - Delta Transfer
	// DWORD dwErr = GetLastError();
	bool fAlreadyExists = (GetLastError() == ERROR_ALREADY_EXISTS);

	if (m_hDestFile == INVALID_HANDLE_VALUE)
	{
		sprintf(szStatus, " %s < %s > %s", sz_H12, m_szDestFileName, sz_H16); 
		SetStatus(szStatus);
		CloseHandle(m_hDestFile);
		delete [] szRemoteFileName;

		// Tell the server to cancel the transfer
		ft.size = Swap32IfLE(-1);
		if (m_fOldFTProtocole)
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
		else
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
		return false;
	}

	delete [] szRemoteFileName;

	m_pCC->CheckFileChunkBufferSize(m_nBlockSize + 1024);

	// sf@2004 - Delta Transfer
	if (fAlreadyExists && !m_fOldFTProtocole)
	{
		// DWORD dwFileSize = GetFileSize(m_hDestFile, NULL); 
		ULARGE_INTEGER n2FileSize;
		bool bSizeOk = MyGetFileSize(m_szDestFileName, &n2FileSize); 
		// if (dwFileSize != 0xFFFFFFFF)
		if (bSizeOk)
		{
			unsigned long nCSBufferSize = (4 * (unsigned long)(n2FileSize.QuadPart / m_nBlockSize)) + 1024;
			char* lpCSBuff = new char [nCSBufferSize];
			if (lpCSBuff != NULL)
			{
				int nCSBufferLen = GenerateFileChecksums(	m_hDestFile,
															lpCSBuff,
															nCSBufferSize
														);
				if (nCSBufferLen != -1)
				{
					//sprintf(szStatus, " Sending %d bytes of file checksums to remote machine. Please Wait...", nCSBufferSize); 
					//SetStatus(szStatus);

					rfbFileTransferMsg ftm;
					ftm.type = rfbFileTransfer;
					ftm.contentType = rfbFileChecksums;
					ftm.size = Swap32IfLE(nCSBufferSize);
					ftm.length = Swap32IfLE(nCSBufferLen);
					m_pCC->WriteExact((char *)&ftm, sz_rfbFileTransferMsg, rfbFileTransfer);
					m_pCC->WriteExact((char *)lpCSBuff, nCSBufferLen);
				}
			}
		}
	}

	// Tell the server that the transfer can start
	ft.size = Swap32IfLE(lSize); 
	if (m_fOldFTProtocole)
		m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
	else
		m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);

	// DWORD dwNbPackets = (DWORD)(lSize / m_nBlockSize);
	m_dwNbReceivedPackets = 0;
	m_dwNbBytesWritten = 0;
	m_dwTotalNbBytesWritten = 0;
	m_dwTotalNbBytesNotReallyWritten = 0;
	m_nPacketCount = 0;
	m_fPacketCompressed = true;

	m_fFileDownloadError = false;
	m_fFileDownloadRunning = true;

	// FT Backward compatibility DIRTY hack for DSMPlugin mode...
	if (m_fOldFTProtocole && m_pCC->m_fUsePlugin)
	{
		m_pCC->m_nTO = 0;
		ProcessFileTransferMsg();
	}

	return true;
}


//
// Receive incoming file chunk
//
bool FileTransfer::ReceiveFileChunk(int nLen, int nSize)
{
//	vnclog.Print(0, _T("ReceiveFileChunk\n"));
	if (!m_fFileDownloadRunning) return false;

	if (m_fFileDownloadError)
		FinishFileReception();

	BOOL fRes = true;
	bool fAlreadyHere = (nSize == 2);
	m_fPacketCompressed = true; // sf@2005 - This missing line was causing RC19 file reception bug...

	if (nLen > m_pCC->m_filechunkbufsize) return false;

	// sf@2004 - Delta Transfer - Empty packet
	if (fAlreadyHere) 
	{
		DWORD dwPtr = SetFilePointer(m_hDestFile, nLen, NULL, FILE_CURRENT); 
		if (dwPtr == 0xFFFFFFFF)
			fRes = false;
	}
	else
	{
		m_pCC->ReadExact((char *)m_pCC->m_filechunkbuf, nLen);

		if (nSize == 0) 
			m_fPacketCompressed = false;
		
		unsigned int nRawBytes = m_nBlockSize + 1024;

		if (m_fPacketCompressed)
		{
			// Decompress incoming data
			m_pCC->CheckFileZipBufferSize(nRawBytes);
			int nRetU = uncompress(	(unsigned char*)m_pCC->m_filezipbuf,// Dest 
									(unsigned long *)&nRawBytes,// Dest len
									(unsigned char*)m_pCC->m_filechunkbuf,		// Src
									nLen		// Src len
								 );							    

			if (nRetU != 0)
			{
				// vnclog.Print(0, _T("uncompress error in ReceiveFile: %d\n"), nRet);
				m_fFileDownloadError = true;
				// break;
			}
			Sleep(5);
		}

		fRes = WriteFile(m_hDestFile,
							m_fPacketCompressed ? m_pCC->m_filezipbuf : m_pCC->m_filechunkbuf,
							m_fPacketCompressed ? nRawBytes : nLen,
							&m_dwNbBytesWritten,
							NULL);
	}

	if (!fRes)
	{
		// TODO: Ask the server to stop the transfer
		m_fFileDownloadError = true;
	}

	m_dwTotalNbBytesWritten += (fAlreadyHere ? nLen : m_dwNbBytesWritten);
	m_dwTotalNbBytesNotReallyWritten += (fAlreadyHere ? nLen : 0);
	m_dwNbReceivedPackets++;

	// Refresh of the progress bar
	SetGauge(hWnd, m_dwTotalNbBytesWritten);
	PseudoYield(GetParent(hWnd));

	// We still support the *dirty* old "Abort" method (for backward compatibility wirh UltraVNC Servers <=RC18)
	if (m_fOldFTProtocole)
	{
		// Every 10 packets, test if the transfer must be stopped
		m_nPacketCount++;
		if (m_nPacketCount > 10)
		{
			m_fAborted = true;
			rfbFileTransferMsg ft;
			ft.type = rfbFileTransfer;
			ft.contentType = rfbFilePacket;
			ft.contentParam = 0;
			ft.length = 0;

			if (m_fAbort || m_fFileDownloadError)
			{
				// Ask the server to stop the transfer
				ft.size = Swap32IfLE(-1);
				m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg/*, rfbFileTransfer*/);
			}
			else
			{
				// Tell the server to continue the transfer
				ft.size = Swap32IfLE(0);
				m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg/*, rfbFileTransfer*/);
			}
			m_nPacketCount = 0;
		}
	}
	else // New V2 FT Protocole
	{
		// Now abort the file transfer if required by the user
		if (m_fAbort && !m_fAborted)
		{
			m_fAborted = true;
			rfbFileTransferMsg ft;
			ft.type = rfbFileTransfer;
			ft.contentType = rfbAbortFileTransfer;
			ft.contentParam = 0;
			ft.length = 0;
			ft.size = 0;
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
		}
	}

	// FT Backward compatibility DIRTY hack for DSMPlugin mode...
	if (m_fOldFTProtocole && m_pCC->m_fUsePlugin)
	{
		m_pCC->m_nTO = 0;
		ProcessFileTransferMsg();
	}
	return true;

}


//
// Finish File Download
//
bool FileTransfer::FinishFileReception()
{
//	vnclog.Print(0, _T("FinishFileReception\n"));
	if (!m_fFileDownloadRunning) return false;

	m_fFileDownloadRunning = false;

	// sf@2004 - Delta transfer
	SetEndOfFile(m_hDestFile);

	// TODO : check dwNbReceivedPackets and dwTotalNbBytesWritten or test a checksum
	FlushFileBuffers(m_hDestFile);
	char szStatus[512];
	if (m_fFileDownloadError)
		sprintf(szStatus, " %s < %s > %s", sz_H19,m_szDestFileName,sz_H20); 
	else
		// sprintf(szStatus, " %s < %s > %s - %u bytes", sz_H17, m_szDestFileName,sz_H18, (m_dwTotalNbBytesWritten - m_dwTotalNbBytesNotReallyWritten));  // Testing
		sprintf(szStatus, " %s < %s > %s", sz_H17, m_szDestFileName,sz_H18); 

	SetStatus(szStatus);

	// Set the DestFile Time Stamp
	if (strlen(m_szIncomingFileTime))
	{
		FILETIME DestFileTime;
		SYSTEMTIME FileTime;
		FileTime.wMonth  = atoi(m_szIncomingFileTime);
		FileTime.wDay    = atoi(m_szIncomingFileTime + 3);
		FileTime.wYear   = atoi(m_szIncomingFileTime + 6);
		FileTime.wHour   = atoi(m_szIncomingFileTime + 11);
		FileTime.wMinute = atoi(m_szIncomingFileTime + 14);
		FileTime.wMilliseconds = 0;
		FileTime.wSecond = 0;
		SystemTimeToFileTime(&FileTime, &DestFileTime);
		// ToDo: hook error
		SetFileTime(m_hDestFile, &DestFileTime, &DestFileTime, &DestFileTime);
	}

	CloseHandle(m_hDestFile);

	// sf@2004 - Delta Transfer - Now we can keep the existing file data :)
	if (m_fFileDownloadError && m_fOldFTProtocole) DeleteFile(m_szDestFileName);

	// sf@2003 - Directory Transfer trick
	// If the file is an Ultra Directory Zip we unzip it here and we delete the
	// received file
	// Todo: make a better free space check (above) in this particular case. The free space must be at least
	// 3 times the size of the directory zip file (this zip file is ~50% of the real directory size) 
	UnzipPossibleDirectory(m_szDestFileName);

	// SetStatus(szStatus);
	UpdateWindow(hWnd);

	// Sound notif
	// MessageBeep(-1);

	// Request the next file in the list
	RequestNextFile();
	return true;
}


//
// Unzip possible directory
// Todo: handle unzip error correctly...
//
int FileTransfer::UnzipPossibleDirectory(LPSTR szFileName)
{
//	vnclog.Print(0, _T("UnzipPossibleDirectory\n"));
	if (!m_fFileDownloadError 
		&& 
		!strncmp(strrchr(szFileName, '\\') + 1, rfbZipDirectoryPrefix, strlen(rfbZipDirectoryPrefix))
	   )
	{
		char szStatus[512];
		char szPath[MAX_PATH + MAX_PATH];
		char szDirName[MAX_PATH]; // Todo: improve this (size) 
		strcpy(szPath, szFileName);
		// Todo: improve all this (p, p2, p3 NULL test or use a standard substring extraction function)
		char *p = strrchr(szPath, '\\') + 1; 
		char *p2 = strchr(p, '-') + 1; // rfbZipDirectoryPrefix MUST have a "-" at the end...
		strcpy(szDirName, p2);
		char *p3 = strrchr(szDirName, '.');
		*p3 = '\0';
		if (p != NULL) *p = '\0';
		strcat(szPath, szDirName);

		// Create the Directory
		sprintf(szStatus, " %s < %s > %s", sz_H59 , szDirName, sz_H60); 
		SetStatus(szStatus);

		bool fUnzip = m_pZipUnZip->UnZipDirectory(szPath, szFileName);
		if (fUnzip)
			sprintf(szStatus, " %s < %s > %s", sz_H61, szDirName, sz_H18);
		else
			sprintf(szStatus, " %s < %s >. %s", sz_H62, szDirName, sz_H63);
		SetStatus(szStatus);
		DeleteFile(szFileName);
	}						
	return 0;
}

//
// Finish File Download
//
bool FileTransfer::AbortFileReception()
{
//	vnclog.Print(0, _T("AbortFileReception\n"));
	if (!m_fFileDownloadRunning) return false;

	m_fFileDownloadError = true;
	FlushFileBuffers(m_hDestFile);
	char szStatus[512];
	sprintf(szStatus, " %s < %s > %s", sz_H19, m_szDestFileName,sz_H20); 

	m_fFileDownloadRunning = false;

	return true;
}

//
// Offer a file
//
bool FileTransfer::OfferLocalFile(LPSTR szSrcFileName)
{
//	vnclog.Print(0, _T("OfferLocalFile\n"));
	if (!m_fFTAllowed) return false;

	char szStatus[512];

	strcpy(m_szSrcFileName, szSrcFileName);

	// sf@2003 - Directory Transfer trick
	// The File to transfer is actually a directory, so we must Zip it recursively and send
	// the resulting zip file (it will be recursively unzipped on server side once
	// the transfer is done)
	int nDirZipRet = ZipPossibleDirectory(m_szSrcFileName);
	if (nDirZipRet == -1)
		return false;

	// Open local src file
	m_hSrcFile = CreateFile(
							m_szSrcFileName,		
							GENERIC_READ,		
							FILE_SHARE_READ,	
							NULL,				
							OPEN_EXISTING,		
							FILE_FLAG_SEQUENTIAL_SCAN,	
							NULL
							);				

	if (m_hSrcFile == INVALID_HANDLE_VALUE)
	{
		sprintf(szStatus, " %s < %s >", sz_H21, m_szSrcFileName); 
		SetStatus(szStatus);
		DWORD TheError = GetLastError();
		return false;
	}

	// Size of src file
	ULARGE_INTEGER n2SrcSize;
	bool bSize = MyGetFileSize(m_szSrcFileName, &n2SrcSize); 
	// if (dwSrcSize == -1)
	if (!bSize)
	{
		sprintf(szStatus, " %s < %s >", sz_H21, m_szSrcFileName);
		SetStatus(szStatus);
		CloseHandle(m_hSrcFile);
		return false;
	}

	char szFFS[96];
	GetFriendlyFileSizeString(n2SrcSize.QuadPart, szFFS);
	sprintf(szStatus, " %s < %s > (%s) >>>", sz_H22, m_szSrcFileName , szFFS); 

	SetStatus(szStatus);
	m_nnFileSize = n2SrcSize.QuadPart;
	SetTotalSize(hWnd, m_nnFileSize); // In bytes
	SetGauge(hWnd, 0); // In bytes
	UpdateWindow(hWnd);

	// Add the File Time Stamp to the filename
	FILETIME SrcFileModifTime; 
	BOOL fRes = GetFileTime(m_hSrcFile, NULL, NULL, &SrcFileModifTime);
	if (!fRes)
	{
		sprintf(szStatus, " %s < %s >", sz_H23, m_szSrcFileName); 
		SetStatus(szStatus);
		CloseHandle(m_hSrcFile);
		return false;
	}

	CloseHandle(m_hSrcFile);

	TCHAR szDstFileName[MAX_PATH + 32];
	memset(szDstFileName, 0, MAX_PATH + 32);

	GetDlgItemText(hWnd, IDC_CURR_REMOTE, szDstFileName, sizeof(szDstFileName));
	if (!strlen(szDstFileName)) return false; // no destination dir selected - msgbox ?
	strcat(szDstFileName, strrchr(m_szSrcFileName, '\\') + 1);

	char szSrcFileTime[18];
	// sf@2003
	// For now, we've made the choice off displaying all the files 
	// off client AND server sides converted in clients local
	// time only. We keep file time as it is before transfering the file (absolute time)
	/*
	FILETIME LocalFileTime;
	FileTimeToLocalFileTime(&SrcFileModifTime, &LocalFileTime);
	*/
	SYSTEMTIME FileTime;
	FileTimeToSystemTime(/*&LocalFileTime*/&SrcFileModifTime, &FileTime);
	wsprintf(szSrcFileTime,"%2.2d/%2.2d/%4.4d %2.2d:%2.2d",
			FileTime.wMonth,
			FileTime.wDay,
			FileTime.wYear,
			FileTime.wHour,
			FileTime.wMinute
			);
	strcat(szDstFileName, ",");
	strcat(szDstFileName, szSrcFileTime);

	// sf@2004 - Delta Transfer
	if (m_lpCSBuffer != NULL) 
	{
		delete [] m_lpCSBuffer;
		m_lpCSBuffer = NULL;
	}
	m_nCSOffset = 0;
	m_nCSBufferSize = 0;

	// Send the FileTransferMsg with rfbFileTransferOffer
	// So the server creates the appropriate new file on the other side
    rfbFileTransferMsg ft;

    ft.type = rfbFileTransfer;
	ft.contentType = rfbFileTransferOffer;
    ft.contentParam = 0;
    ft.size = Swap32IfLE(n2SrcSize.LowPart); // File Size in bytes
	ft.length = Swap32IfLE(strlen(szDstFileName));
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	m_pCC->WriteExact((char *)szDstFileName, strlen(szDstFileName));

	if (!m_fOldFTProtocole)
	{
		CARD32 sizeH = Swap32IfLE(n2SrcSize.HighPart);
		m_pCC->WriteExact((char *)&sizeH, sizeof(CARD32));
	}

	return true;
}


//
// Zip a possible directory
//
int FileTransfer::ZipPossibleDirectory(LPSTR szSrcFileName)
{
//	vnclog.Print(0, _T("ZipPossibleDirectory\n"));
	char* p1 = strrchr(szSrcFileName, '\\') + 1;
	char* p2 = strrchr(szSrcFileName, rfbDirSuffix[0]);
	if (
		p1[0] == rfbDirPrefix[0] && p1[1] == rfbDirPrefix[1]  // Check dir prefix
		&& p2[1] == rfbDirSuffix[1] && p2 != NULL && p1 < p2  // Check dir suffix
		) //
	{
		// sf@2004 - Improving Directory Transfer: Avoids ReadOnly media problem
		char szDirZipPath[MAX_PATH];
		char szWorkingDir[MAX_PATH];
		if (GetModuleFileName(NULL, szWorkingDir, MAX_PATH))
		{
			char* p = strrchr(szWorkingDir, '\\');
			if (p == NULL)
				return -1;
			*(p+1) = '\0';
		}
		else
			return -1;

		char szStatus[512];
		char szPath[MAX_PATH];
		char szDirectoryName[MAX_PATH];
		strcpy(szPath, szSrcFileName);
		p1 = strrchr(szPath, '\\') + 1;
		strcpy(szDirectoryName, p1 + 2); // Skip dir prefix (2 chars)
		szDirectoryName[strlen(szDirectoryName) - 2] = '\0'; // Remove dir suffix (2 chars)
		*p1 = '\0';
		if ((strlen(szPath) + strlen(rfbZipDirectoryPrefix) + strlen(szDirectoryName) + 4) > (MAX_PATH - 1)) return false;
		// sprintf(szSrcFileName, "%s%s%s%s", szPath, rfbZipDirectoryPrefix, szDirectoryName, ".zip"); 
		sprintf(szDirZipPath, "%s%s%s%s", szWorkingDir, rfbZipDirectoryPrefix, szDirectoryName, ".zip"); 
		strcat(szPath, szDirectoryName);
		strcpy(szDirectoryName, szPath);
		if (strlen(szDirectoryName) > (MAX_PATH - 4)) return -1;
		strcat(szDirectoryName, "\\*.*");
		sprintf(szStatus, " %s < %s > %s", sz_H64, szPath, sz_H65);
		SetStatus(szStatus);
		bool fZip = m_pZipUnZip->ZipDirectory(szPath, szDirectoryName, szDirZipPath/*szSrcFileName*/, true);
		if (fZip)
			sprintf(szStatus, " %s < %s > %s", sz_H66, szPath, sz_H67);
		else
			sprintf(szStatus, " %s < %s >. %s", sz_H68, szPath, sz_H69);
		SetStatus(szStatus);
		if (!fZip) return -1;
		strcpy(szSrcFileName, szDirZipPath);
		return 1;
	}
	else
		return 0;
}

//
// sf@2004 - Delta Transfer
// Destination file already exists
// The server sends the checksums of this file in one shot.
// 
bool FileTransfer::ReceiveDestinationFileChecksums(int nSize, int nLen)
{
//	vnclog.Print(0, _T("ReceiveDestinationFileChecksums\n"));
	m_lpCSBuffer = new char [nLen+1]; //nSize
	if (m_lpCSBuffer == NULL) 
	{
		return false;
	}

	// char szStatus[255];
	// sprintf(szStatus, " Receiving %d bytes of file checksums from remote machine. Please wait...", nLen); 
	// SetStatus(szStatus);

	memset(m_lpCSBuffer, '\0', nLen+1); // nSize

	m_pCC->ReadExact((char *)m_lpCSBuffer, nLen);
	m_nCSBufferSize = nLen;

	return true;
}


//
//  SendFile 
// 
bool FileTransfer::SendFile(long lSize, int nLen)
{
//	vnclog.Print(0, _T("SendFile\n"));
	if (!m_fFTAllowed) return false;
	if (nLen == 0) return false; // Used when the local file could no be open in OfferLocalFile

	char *szRemoteFileName = new char [nLen+1];
	if (szRemoteFileName == NULL) return false;
	memset(szRemoteFileName, 0, nLen+1);

	// Read in the Name of the file to copy (remote full name !)
	m_pCC->ReadExact(szRemoteFileName, nLen);

	char szStatus[512];

	// If lSize = -1 (0xFFFFFFFF) that means that the Dst file on the remote machine
	// could not be created for some reason (locked..)
	if (lSize == -1)
	{
		sprintf(szStatus, " %s < %s > %s",sz_H12, szRemoteFileName,sz_H24); 
		SetStatus(szStatus);
		sprintf(szStatus, " %s < %s >%s", sz_H25,szRemoteFileName,sz_H26);
		SetStatus(szStatus);

		delete [] szRemoteFileName;
		return false;
	}

	// Build the local file path
	// sf@2004 - Directory Transfer improvement : filename already known
	/*
	if (m_fOldFTProtocole)
	{
	GetDlgItemText(hWnd, IDC_CURR_LOCAL, m_szSrcFileName, sizeof(m_szSrcFileName));
	strcat(m_szSrcFileName, strrchr(szRemoteFileName, '\\') + 1);
	}
	*/

	delete [] szRemoteFileName;

	// Open src file
	m_hSrcFile = CreateFile(
							m_szSrcFileName,		
							GENERIC_READ,		
							FILE_SHARE_READ,	
							NULL,				
							OPEN_EXISTING,		
							FILE_FLAG_SEQUENTIAL_SCAN,	
							NULL
							);				

	if (m_hSrcFile == INVALID_HANDLE_VALUE)
	{
		sprintf(szStatus, " %s < %s >", sz_H21, m_szSrcFileName); 
		SetStatus(szStatus);
		DWORD TheError = GetLastError();
		return false;
	}

	m_fFileUploadError = false;
	m_dwNbBytesRead = 0;
	m_dwTotalNbBytesRead = 0;
	m_fEof = false;

	// If the connection speed is > 2048 Kbit/s, no need to compress.
	m_fCompress = (m_pCC->kbitsPerSecond <= 2048);

	m_fFileUploadRunning = true;
	m_dwLastChunkTime = timeGetTime();
	// m_nNotSent = 0;
	// SendFileChunk();
	return true;

}


//
// Send the next file packet (upload)
// This function is called asynchronously from the
// main ClientConnection message loop
// 
bool FileTransfer::SendFileChunk()
{
//	vnclog.Print(0, _T("SendFilechunk\n"));
	if (! m_fFileUploadRunning) return false;

	if ( m_fEof || m_fFileUploadError)
	{
		FinishFileSending();
		return true;
	}

	m_pCC->CheckFileChunkBufferSize(m_nBlockSize + 1024);

	int nRes = ReadFile(m_hSrcFile, m_pCC->m_filechunkbuf, m_nBlockSize, &m_dwNbBytesRead, NULL);
	if (!nRes && m_dwNbBytesRead != 0)
	{
		m_fFileUploadError = true;
	}

	if (nRes && m_dwNbBytesRead == 0)
	{
		m_fEof = true;
	}
	else
	{
		// sf@2004 - Delta Transfer
		bool fAlreadyThere = false;
		unsigned long nCS = 0;
		// if Checksums are available for this file
		if (m_lpCSBuffer != NULL)
		{
			if (m_nCSOffset < m_nCSBufferSize)
			{
				memcpy(&nCS, &m_lpCSBuffer[m_nCSOffset], 4);
				if (nCS != 0)
				{
					m_nCSOffset += 4;
					unsigned long cs = adler32(0L, Z_NULL, 0);
					cs = adler32(cs, m_pCC->m_filechunkbuf, (int)m_dwNbBytesRead);
					if (cs == nCS)
						fAlreadyThere = true;
				}
			}
		}

		if (fAlreadyThere)
		{
			// Send the FileTransferMsg with empty rfbFilePacket
			rfbFileTransferMsg ft;
			ft.type = rfbFileTransfer;
			ft.contentType = rfbFilePacket;
			ft.size = Swap32IfLE(2); // Means "Empty packet"// Swap32IfLE(nCS); 
			ft.length = Swap32IfLE(m_dwNbBytesRead);
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
			// m_nNotSent += m_dwNbBytesRead;
		}
		else
		{
			// Compress the data
			// (Compressed data can be longer if it was already compressed)
			unsigned int nMaxCompSize = m_nBlockSize + 1024; // TODO: Improve this...
			bool fCompressed = false;
			if (m_fCompress && !m_fOldFTProtocole)
			{
				m_pCC->CheckFileZipBufferSize(nMaxCompSize);
				int nRetC = compress((unsigned char*)(m_pCC->m_filezipbuf),
											(unsigned long*)&nMaxCompSize,	
											(unsigned char*)m_pCC->m_filechunkbuf,
											m_dwNbBytesRead
											);
				if (nRetC != 0)
				{
					// Todo: send data uncompressed instead
					m_fFileUploadError = true;
					return false;
				}
				Sleep(5);
				fCompressed = true;
			}

			// If data compressed is larger, we're presumably dealing with already compressed data.
			if (nMaxCompSize > m_dwNbBytesRead)
				fCompressed = false;
				// m_fCompress = false;



			// Send the FileTransferMsg with rfbFilePacket
			rfbFileTransferMsg ft;
			ft.type = rfbFileTransfer;
			ft.contentType = rfbFilePacket;
			ft.size = fCompressed ? Swap32IfLE(1) : Swap32IfLE(0); 
			ft.length = fCompressed ? Swap32IfLE(nMaxCompSize) : Swap32IfLE(m_dwNbBytesRead);
			if(m_fOldFTProtocole)
				m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
			else
				m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);

			if (fCompressed)
				m_pCC->WriteExact((char *)m_pCC->m_filezipbuf, nMaxCompSize);
			else
				m_pCC->WriteExact((char *)m_pCC->m_filechunkbuf, m_dwNbBytesRead);
		}

		m_dwTotalNbBytesRead += m_dwNbBytesRead;

		// Refresh progress bar
		SetGauge(hWnd, m_dwTotalNbBytesRead);
		PseudoYield(GetParent(hWnd));
	
		if (m_fAbort)
		{
			m_fFileUploadError = true;
			FinishFileSending();
			return false;
		}
	}

	// Order next asynchronous packet sending
	// If Gui is visible or old blocking FT protocole : no screen updates, more speed
	/*
	if (m_fVisible || m_fOldFTProtocole)
	{
		// SendFileChunk(); // Fixme: Beware call stack...
		// if (m_pCC->kbitsPerSecond <= 2048) 
		PostMessage(m_pCC->m_hwndMain, FileTransferSendPacketMessage, (WPARAM) 0, (LPARAM) 0);
	}
	else
	{
		//if (m_pCC->kbitsPerSecond <= 2048) 
		//else
		//	Sleep(50);

		PostMessage(m_pCC->m_hwndMain, FileTransferSendPacketMessage, (WPARAM) 0, (LPARAM) 0);
	}
	// PostMessage(hWnd, FileTransferSendPacketMessage, (WPARAM) 0, (LPARAM) 0);
	*/

	return true;
}


bool FileTransfer::FinishFileSending()
{
//	vnclog.Print(0, _T("FinishSendFile\n"));
	if (!m_fFileUploadRunning) return false;

	m_fFileUploadRunning = false;

	char szStatus[512];

	CloseHandle(m_hSrcFile);
	
	if ( !m_fFileUploadError || m_fEof)
	{
		rfbFileTransferMsg ft;

		ft.type = rfbFileTransfer;
		ft.contentType = rfbEndOfFile;
		if (m_fOldFTProtocole)
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
		else
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
		sprintf(szStatus, " %s < %s > %s", sz_H17, m_szSrcFileName, sz_H27/*, (int)((lTotalComp * 100) / dwTotalNbBytesWritten), fCompress ? "C" : "N"*//*, szDstFileName*/); 
	}
	else // Error during file transfer loop
	{
		rfbFileTransferMsg ft;
		ft.type = rfbFileTransfer;
		ft.contentType = rfbAbortFileTransfer;
		if (m_fOldFTProtocole)
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg);
		else
			m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
		sprintf(szStatus, " %s < %s > %s", sz_H19, m_szSrcFileName, sz_H28); 
	}

	// If the transfered file is a Directory zip, we delete it locally, whatever the result of the transfer
	if (!strncmp(strrchr(m_szSrcFileName, '\\') + 1, rfbZipDirectoryPrefix, strlen(rfbZipDirectoryPrefix)))
	{
		DeleteFile(m_szSrcFileName);
		if (!m_fFileUploadError)
		{
			char szDirectoryName[MAX_PATH];
			char *p = strrchr(m_szSrcFileName, '\\');
			char *p1 = strchr(p, '-');
			strcpy(szDirectoryName, p1 + 1);
			szDirectoryName[strlen(szDirectoryName) - 4] = '\0'; // Remove '.zip'
			// sprintf(szStatus, " %s < %s > %s - Not really sent: %ld", sz_H66, szDirectoryName, sz_H70, m_nNotSent);
			sprintf(szStatus, " %s < %s > %s", sz_H66, szDirectoryName, sz_H70);
		}
	}

	SetStatus(szStatus);
	UpdateWindow(hWnd);

	// Sound notif
	//MessageBeep(-1);

	// Send next file in the list, if any
	OfferNextFile();

	// if (nRet) return true; else return false;
	// return !m_fFileUploadError;
	return true;
}



//
// Request the creation of a directory on the remote machine
//
void FileTransfer::CreateRemoteDirectory(LPSTR szDir)
{
    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbCommand;
    ft.contentParam = rfbCDirCreate;
	ft.size = 0;
	ft.length = Swap32IfLE(strlen(szDir));
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	m_pCC->WriteExact((char *)szDir, strlen(szDir));
	return;
}


//
// Request the deletion of a file on the remote machine
//
void FileTransfer::DeleteRemoteFile(LPSTR szFile)
{
    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbCommand;
    ft.contentParam = rfbCFileDelete;
	ft.size = 0;
	ft.length = Swap32IfLE(strlen(szFile));
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	m_pCC->WriteExact((char *)szFile, strlen(szFile));
	return;
}

//
// Request the renaming of a file or a directory
// 
void FileTransfer::RenameRemoteFileOrDirectory(LPSTR szCurrentName, LPSTR szNewName)
{
	char szMsgContent[(2 * MAX_PATH) + 1];
	sprintf(szMsgContent, "%s*%s", szCurrentName, szNewName);
    rfbFileTransferMsg ft;
    ft.type = rfbFileTransfer;
	ft.contentType = rfbCommand;
    ft.contentParam = rfbCFileRename; // or rfbCDirRename ...
	ft.size = 0;
	ft.length = Swap32IfLE(strlen(szMsgContent));
    m_pCC->WriteExact((char *)&ft, sz_rfbFileTransferMsg, rfbFileTransfer);
	m_pCC->WriteExact((char *)szMsgContent, strlen(szMsgContent));
	return;
}

//
// Server's response to a directory creation command
//
bool FileTransfer::CreateRemoteDirectoryFeedback(long lSize, int nLen)
{
	char *szRemoteName = new char [nLen+1];
	if (szRemoteName == NULL) return false;
	memset(szRemoteName, 0, nLen+1);
	m_pCC->ReadExact(szRemoteName, nLen);
	char szStatus[512];

	if (lSize == -1)
	{
		sprintf(szStatus, "%s < %s > %s", sz_H29,szRemoteName,sz_H30); 
		SetStatus(szStatus);
		delete [] szRemoteName;
		return false;
	}
	sprintf(szStatus, "%s < %s > %s",sz_H31, szRemoteName,sz_H32); 
	SetStatus(szStatus);
	// Refresh the remote list
	ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_REMOTE_FILELIST));
	RequestRemoteDirectoryContent(hWnd, "");

	delete [] szRemoteName;
	return true;
}


//
// Server's response to a File Deletion command
//
bool FileTransfer::DeleteRemoteFileFeedback(long lSize, int nLen)
{
	char *szRemoteName = new char [nLen+1];
	if (szRemoteName == NULL) return false;
	memset(szRemoteName, 0, nLen+1);
	m_pCC->ReadExact(szRemoteName, nLen);
	char szStatus[512];

	if (lSize == -1)
	{
		sprintf(szStatus, "%s < %s > %s", sz_H33,szRemoteName,sz_H30); 
		SetStatus(szStatus);
		delete [] szRemoteName;
		return false;
	}
	sprintf(szStatus, "%s < %s > %s", sz_H17,szRemoteName,sz_H34); 
	SetStatus(szStatus);
	// Refresh the remote list
	if (--m_nDeleteCount == 0)
		ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_REMOTE_FILELIST));
	RequestRemoteDirectoryContent(hWnd, "");

	delete [] szRemoteName;
	return true;
}


//
// Server's response to a File Deletion command
//
bool FileTransfer::RenameRemoteFileOrDirectoryFeedback(long lSize, int nLen)
{
	char *szContent = new char [nLen+1];
	if (szContent == NULL) return false;
	memset(szContent, 0, nLen+1);
	m_pCC->ReadExact(szContent, nLen);
	char szStatus[512];

	char *p = strrchr(szContent, '*');
	char szOldName[MAX_PATH];
	char szCurrentName[MAX_PATH];

	strcpy(szCurrentName, p + 1); 
	*p = '\0';
	strcpy(szOldName, szContent);

	if (lSize == -1 || p == NULL)
	{
		sprintf(szStatus, " %s < %s > %s", sz_M5, szOldName, sz_H30); 
		SetStatus(szStatus);
		delete [] szContent;
		return false;
	}

	sprintf(szStatus, " %s < %s > %s < %s >", sz_M8 , szOldName, sz_M7, szCurrentName); 
	SetStatus(szStatus);
	// Refresh the remote list
	ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_REMOTE_FILELIST));
	RequestRemoteDirectoryContent(hWnd, "");

	delete [] szContent;
	return true;
}


//
// sf@2004 - Delta Transfer
// Create the checksums buffer of an open file
//
int FileTransfer::GenerateFileChecksums(HANDLE hFile, char* lpCSBuffer, int nCSBufferSize)
{
	bool fEof = false;
	bool fError = false;
	DWORD dwNbBytesRead = 0;
	int nCSBufferOffset = 0;

	char* lpBuffer = new char [m_nBlockSize];
	if (lpBuffer == NULL)
		return -1;

	while ( !fEof )
	{
		int nRes = ReadFile(hFile, lpBuffer, m_nBlockSize, &dwNbBytesRead, NULL);
		if (!nRes && dwNbBytesRead != 0)
			fError = true;

		if (nRes && dwNbBytesRead == 0)
			fEof = true;
		else
		{
			unsigned long cs = adler32(0L, Z_NULL, 0);
			cs = adler32(cs, (unsigned char*)lpBuffer, (int)dwNbBytesRead);
			memcpy(lpCSBuffer + nCSBufferOffset, &cs, 4);
			nCSBufferOffset += 4; 
		}
	}

	SetFilePointer(hFile, 0L, NULL, FILE_BEGIN); 
	delete [] lpBuffer;

	if (fError) 
		return -1;

	return nCSBufferOffset;

}


bool FileTransfer::IsDirectoryGetIt(char* szName)
{
	char szWork[MAX_PATH];
	strcpy(szWork, szName);
	if (szWork[0] == rfbDirPrefix[0] && szWork[1] == rfbDirPrefix[1])
	{
		strncpy(szName, szWork + 2, strlen(szWork) - 3); 
		szName[strlen(szWork) - 4] = '\0';
		return true;
	}	
	return false;
}

#ifndef _ULTRAVNCAX_
int FileTransfer::DoDialog()
#else
int FileTransfer::DoDialog ( HWND parent /*= NULL*/ )
#endif
{
#ifndef _ULTRAVNCAX_
 	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FILETRANSFER_DLG), 
		NULL, (DLGPROC) FileTransferDlgProc, (LONG) this);
#else
 	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FILETRANSFER_DLG), 
		parent, (DLGPROC) FileTransferDlgProc, (LONG) this);
#endif
}


//
// 2006 - Resizable FT Window mod - By Roytam1 & and KP774
//
void FTAdjustLeft(LPRECT lprc)
{
	int cx = lprc->right - lprc->left - GetSystemMetrics(SM_CXSIZEFRAME) * 2;
	if(cx < 611)
	{
		lprc->left = lprc->right - 611 - GetSystemMetrics(SM_CXSIZEFRAME) * 2;
	}
}


void FTAdjustTop(LPRECT lprc)
{
	int cy = lprc->bottom - lprc->top - GetSystemMetrics(SM_CYSIZEFRAME) * 2;
	if(cy < 429)
	{
		lprc->top = lprc->bottom - 429 - GetSystemMetrics(SM_CYSIZEFRAME) * 2;
	}
}



void FTAdjustRight(LPRECT lprc)
{
	int cx = lprc->right - lprc->left - GetSystemMetrics(SM_CXSIZEFRAME) * 2;
	if(cx < 611)
	{
		lprc->right = lprc->left + 611 + GetSystemMetrics(SM_CXSIZEFRAME) * 2;
	}
}



void FTAdjustBottom(LPRECT lprc)
{
	int cy = lprc->bottom - lprc->top - GetSystemMetrics(SM_CYSIZEFRAME) * 2;
	if(cy < 429)
	{
		lprc->bottom = lprc->top + 429 + GetSystemMetrics(SM_CYSIZEFRAME) * 2;
	}
}


// sf@2006
void FTAdjustFileNameColumns(HWND hWnd)
{

	RECT rc;
	GetWindowRect(GetDlgItem(hWnd, IDC_LOCAL_FILELIST), &rc);
	int w = rc.right - rc.left;
	int cw = w - (70 + 100 + 25);
	if (cw < 120) cw = 120;

	LVCOLUMN Column;
	Column.mask = LVCF_FMT|LVCF_WIDTH|LVCF_TEXT|LVCF_ORDER|LVCF_SUBITEM;
	Column.fmt = LVCFMT_LEFT;
	Column.cx = cw;
	Column.pszText = "Name";
	Column.iSubItem = 0;
	Column.iOrder = 0;

	HWND hWndList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
	ListView_SetColumn(hWndList, 0, &Column);
	hWndList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
	ListView_SetColumn(hWndList, 0, &Column);
}

//
//
//
BOOL CALLBACK FileTransfer::FileTransferDlgProc(  HWND hWnd,  UINT uMsg,  WPARAM wParam, LPARAM lParam ) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. But we can get a pseudo-this from the parameter to 
	// WM_INITDIALOG, which we therafter store with the window and retrieve
	// as follows:
	FileTransfer *_this = (FileTransfer *) GetWindowLong(hWnd, GWL_USERDATA);

	switch (uMsg)
	{
	case WM_TIMER:
		{
			// We have to wait for NetBuf flush
			// !!!: Not necessary anymore - Pb solved on server side.
			DWORD lTime = timeGetTime();
			// DWORD lLastTime = _this->m_pCC->m_lLastRfbRead;
			// DWORD lDelta = abs(timeGetTime() - _this->m_pCC->m_lLastRfbRead);
			if (true/*meGetTime() - _this->m_pCC->m_lLastRfbRead) > 1000 */)
			{
				_this->m_fFileCommandPending = true;
				_this->RequestPermission();
				if (KillTimer(hWnd, _this->m_timer))
					_this->m_timer = 0;
			}
		break;
		}

	case WM_INITDIALOG:
		{
            SetWindowLong(hWnd, GWL_USERDATA, lParam);
            FileTransfer *_this = (FileTransfer *) lParam;
            // CentreWindow(hWnd);
			_this->hWnd = hWnd;

			// Window always on top if Fullscreen On
			if (_this->m_pCC->InFullScreenMode())
			{
				RECT Rect;
				GetWindowRect(hWnd, &Rect);
				SetWindowPos(hWnd, 
							HWND_TOPMOST,
							Rect.left,
							Rect.top,
							Rect.right - Rect.left,
							Rect.bottom - Rect.top,
							SWP_SHOWWINDOW);
			}

			SetForegroundWindow(hWnd);

			// Set the title 
			const long lTitleBufSize=256;			
			char szRemoteName[lTitleBufSize];
			char szTitle[lTitleBufSize];
			if (_snprintf(szRemoteName, 127 ,"%s", _this->m_pCC->m_desktopName) < 0 )
			{
				szRemoteName[128-4]='.';
				szRemoteName[128-3]='.';
				szRemoteName[128-2]='.';
				szRemoteName[128-1]=0x00;
			}	
			_snprintf(szTitle, lTitleBufSize-1," %s < %s>  -  VMOpsVNC", sz_H35,szRemoteName);
			SetWindowText(hWnd, szTitle);

			// Create all the columns of the Files ListViews
			LVCOLUMN Column;
			Column.mask = LVCF_FMT|LVCF_WIDTH|LVCF_TEXT|LVCF_ORDER|LVCF_SUBITEM;
			Column.fmt = LVCFMT_LEFT;
			Column.cx = 166;
			Column.pszText = "Name";
			Column.iSubItem = 0;
			Column.iOrder = 0;
			HWND hWndList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			ListView_InsertColumn(hWndList, 0, &Column);
			hWndList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
			ListView_InsertColumn(hWndList, 0, &Column);

			Column.mask = LVCF_FMT|LVCF_WIDTH|LVCF_TEXT|LVCF_ORDER|LVCF_SUBITEM;
			Column.fmt = LVCFMT_LEFT;
			Column.cx = 70;
			Column.pszText = "Size";
			Column.iSubItem = 1;
			Column.iOrder = 1;
			hWndList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			ListView_InsertColumn(hWndList, 1, &Column);
			hWndList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
			ListView_InsertColumn(hWndList, 1, &Column);

			Column.mask = LVCF_FMT|LVCF_WIDTH|LVCF_TEXT|LVCF_ORDER|LVCF_SUBITEM;
			Column.fmt = LVCFMT_LEFT;
			Column.cx = 100;
			Column.pszText = "Modified";
			Column.iSubItem = 2;
			Column.iOrder = 2;
			hWndList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			ListView_InsertColumn(hWndList, 2, &Column);
			hWndList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
			ListView_InsertColumn(hWndList, 2, &Column);
			
			// Make the selection bar full width in the ListViews
			hWndList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			ListView_SetExtendedListViewStyleEx(hWndList, LVS_EX_FULLROWSELECT, LVS_EX_FULLROWSELECT);
			hWndList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
			ListView_SetExtendedListViewStyleEx(hWndList, LVS_EX_FULLROWSELECT, LVS_EX_FULLROWSELECT); 

			// Create Icons List of ListViews
			_this->InitListViewImagesList(GetDlgItem(hWnd, IDC_LOCAL_FILELIST));
			_this->InitListViewImagesList(GetDlgItem(hWnd, IDC_REMOTE_FILELIST));
			
			// Create the status bar
			HWND hStatusBar = CreateStatusWindow(WS_VISIBLE|WS_CHILD, sz_H36 , hWnd, IDC_STATUS);

			// Populate the Local listboxes with local drives
			_this->ListDrives(hWnd);

			// Populate the remote listboxes with remote drives if allowed by the server
			if (false/*!_this->m_pCC->m_pEncrypt->IsEncryptionEnabled()*/)
			{
				/*
				if (!_this->m_fFileCommandPending)
				{
					_this->m_fFileCommandPending = true;
					// RequestRemoteDrives(_this);
					RequestPermission(_this);
					SetStatus(_this, " Connecting. Please Wait...");
				}
				*/
			}
			else
			{
				_this->m_timer = SetTimer( hWnd, 3333,  1000, NULL);
				_this->SetStatus(sz_H37);
			}

			// Save original (translated) buttons labels 
			if (strlen(_this->m_szDeleteButtonLabel) == 0)
			{
//				char szLbl[64];
				HWND hB = GetDlgItem(hWnd, IDC_DELETE_B);
				GetWindowText(hB, (LPSTR)_this->m_szDeleteButtonLabel, 64);
			}

			if (strlen(_this->m_szNewFolderButtonLabel) == 0)
			{
//				char szLbl[64];
				HWND hB = GetDlgItem(hWnd, IDC_NEWFOLDER_B);
				GetWindowText(hB, (LPSTR)_this->m_szNewFolderButtonLabel, 64);
			}

			if (strlen(_this->m_szRenameButtonLabel) == 0)
			{
//				char szLbl[64];
				HWND hB = GetDlgItem(hWnd, IDC_RENAME_B);
				GetWindowText(hB, (LPSTR)_this->m_szRenameButtonLabel, 64);
			}

			// Disable Close Window in in title bar
			HMENU hMenu = GetSystemMenu(_this->hWnd, 0);
			int nCount = GetMenuItemCount(hMenu);
			RemoveMenu(hMenu, nCount-1, MF_DISABLED | MF_BYPOSITION);
			RemoveMenu(hMenu, nCount-2, MF_DISABLED | MF_BYPOSITION);
			DrawMenuBar(_this->hWnd);
            return TRUE;
		}
		break;

	case WM_COMMAND:
		switch (LOWORD(wParam))
		{
		case IDOK:
			return TRUE;

		case IDCANCEL:
			EndDialog(hWnd, FALSE);
			return TRUE;

		// Send selected files
		case IDC_UPLOAD_B:
			{
			if (_this->m_fFileCommandPending) break;

			TCHAR szSelectedFile[128];
			TCHAR szCurrLocal[MAX_PATH];
			TCHAR szDstFile[MAX_PATH + 32];
			memset(szSelectedFile, 0, 128);
			memset(szCurrLocal, 0, MAX_PATH);
			memset(szDstFile, 0, MAX_PATH + 32);
 
			int nSelected = -1;
			int nCount = 0;
			HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

			LVITEM Item;
			Item.mask = LVIF_TEXT;
			Item.iSubItem = 0;
			Item.pszText = szSelectedFile;
			Item.cchTextMax = 128;

			// If no destination is set,nothing to do.
			GetDlgItemText(hWnd, IDC_CURR_REMOTE, szDstFile, sizeof(szDstFile));
			if (!strlen(szDstFile)) break; // no dest dir selected

			// Get all the selected files on check if they already exist on remote side
			// If they already exist, the user is prompted for overwrite
			// Store the indexes of selected files in a list
			_this->m_FilesList.clear();
			nCount = ListView_GetItemCount(hWndLocalList);
			_this->m_nConfirmAnswer = CONFIRM_YES;
			for (nSelected = 0; nSelected < nCount; nSelected++)
			{
				if(ListView_GetItemState(hWndLocalList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
				{
					Item.iItem = nSelected;
					ListView_GetItem(hWndLocalList, &Item);
					// if (szSelectedFile[0] != '(') // Only a file can be transfered
					char szUpDirMask[16];
					sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
					if (stricmp(szSelectedFile, szUpDirMask))
					{ 
						LVFINDINFO Info;
						Info.flags = LVFI_STRING;
						Info.psz = (LPSTR)szSelectedFile;
						int nTheIndex = ListView_FindItem(hWndRemoteList, -1, &Info);
						bool fDirectory = (szSelectedFile[0] == rfbDirPrefix[0] && szSelectedFile[1] == rfbDirPrefix[1]);
						if (nTheIndex > -1) 
						{
							if (_this->m_nConfirmAnswer == CONFIRM_YES || _this->m_nConfirmAnswer == CONFIRM_NO)
							{
								char szMes[MAX_PATH + 96];
								if (fDirectory)
									wsprintf(szMes, "%s < %s >\n%s", sz_H71, szSelectedFile, sz_H72);
								else
									wsprintf(szMes, "%s < %s >\n%s", sz_H17, szSelectedFile, sz_H38);
								_this->DoFTConfirmDialog(sz_H39, _T(szMes));
								if (_this->m_nConfirmAnswer == CONFIRM_NO)
									continue;
								if (_this->m_nConfirmAnswer == CONFIRM_NOALL)
									break;
							}
						}
						// Add the file to the list
						_this->m_FilesList.push_back(nSelected);
					}
				}
			}
			if (_this->m_FilesList.size() == 0) break;

			// Display Status
			char szLocalStatus[128];
			sprintf(szLocalStatus, " > %d %s", _this->m_FilesList.size(),sz_H40); 
			SetDlgItemText(hWnd, IDC_LOCAL_STATUS, szLocalStatus);
			sprintf(szLocalStatus, "%s %d %s", sz_H41,_this->m_FilesList.size(),sz_H42); 
			_this->SetStatus(szLocalStatus);

			_this->m_nFilesTransfered = 0;
			_this->m_nFilesToTransfer = _this->m_FilesList.size();

			// Disable buttons
			_this->DisableButtons(_this->hWnd);
			ShowWindow(GetDlgItem(hWnd, IDC_ABORT_B), SW_SHOW);

			// Get the fisrt selected file name
			_this->m_iFile = _this->m_FilesList.begin();
			Item.iItem = *_this->m_iFile;
			ListView_GetItem(hWndLocalList, &Item);

			GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
			if (!strlen(szCurrLocal)) break; // no src dir selected
			strcat(szCurrLocal, szSelectedFile);

			// Request the first file of the list (-> triggers the transfer of the whole list)
			_this->m_fFileCommandPending = true;
			_this->m_fAbort = false;
			_this->m_fAborted = false;
			if (!_this->OfferLocalFile(szCurrLocal))
				_this->SendFiles(-1, 0); // If the first file could not be opened try next file

			}
			break;

		// Receive selected files
		case IDC_DOWNLOAD_B:
			{
			if (_this->m_fFileCommandPending) break;
			TCHAR szSelectedFile[128];
			TCHAR szCurrLocal[MAX_PATH];
			TCHAR szDstFile[MAX_PATH + 32];

			memset(szSelectedFile, 0, 128);
			memset(szCurrLocal, 0, MAX_PATH);
			memset(szDstFile, 0, MAX_PATH + 32);

			int nSelected = -1;
			int nCount = 0;
			HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
			HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);

			LVITEM Item;
			Item.mask = LVIF_TEXT;
			Item.iSubItem = 0;
			Item.pszText = szSelectedFile;
			Item.cchTextMax = 128;

			// If no dst dir is selected, nothing to do
			GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
			if (!strlen(szCurrLocal)) break; // no dst dir selected

			// Get all the selected files on check if they already exist on local side
			// If they already exist, the user is prompted for overwrite
			// Store the indexes of selected files in a list
			_this->m_FilesList.clear();
			nCount = ListView_GetItemCount(hWndRemoteList);
			_this->m_nConfirmAnswer = CONFIRM_YES;
			for (nSelected = 0; nSelected < nCount; nSelected++)
			{
				if(ListView_GetItemState(hWndRemoteList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
				{
					Item.iItem = nSelected;
					ListView_GetItem(hWndRemoteList, &Item);
					// if (szSelectedFile[0] != '(') // Only a file can be transfered
					char szUpDirMask[16];
					sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
					if (stricmp(szSelectedFile, szUpDirMask))
					{ 
						LVFINDINFO Info;
						Info.flags = LVFI_STRING;
						Info.psz = (LPSTR)szSelectedFile;
						int nTheIndex = ListView_FindItem(hWndLocalList, -1, &Info);
						bool fDirectory = (szSelectedFile[0] == rfbDirPrefix[0] && szSelectedFile[1] == rfbDirPrefix[1]);
						if (nTheIndex > -1) 
						{

							if (_this->m_nConfirmAnswer == CONFIRM_YES || _this->m_nConfirmAnswer == CONFIRM_NO)
							{
								char szMes[MAX_PATH + 96];
								if (fDirectory)
									wsprintf(szMes, "%s < %s >\n\n%s", sz_H73, szSelectedFile, sz_H72);
								else
									wsprintf(szMes, "%s < %s >\n\n%s", sz_H17,szSelectedFile,sz_H43);
								_this->DoFTConfirmDialog(sz_H47, _T(szMes));
								if (_this->m_nConfirmAnswer == CONFIRM_NO)
									continue;
								if (_this->m_nConfirmAnswer == CONFIRM_NOALL)
									break;
							}
						}
						// Add the file to the list
						_this->m_FilesList.push_back(nSelected);
					}
				}
			}
			if (_this->m_FilesList.size() == 0) break;

			// Display Status
			char szRemoteStatus[128];
			sprintf(szRemoteStatus, " > %d %s", _this->m_FilesList.size(),sz_H40); 
			SetDlgItemText(hWnd, IDC_REMOTE_STATUS, szRemoteStatus);
			sprintf(szRemoteStatus, "%s %d %s ",sz_H44, _this->m_FilesList.size(),sz_H45); 
			_this->SetStatus(szRemoteStatus);

			_this->m_nFilesTransfered = 0;
			_this->m_nFilesToTransfer = _this->m_FilesList.size();

			// Disable buttons
			_this->DisableButtons(_this->hWnd);
			ShowWindow(GetDlgItem(_this->hWnd, IDC_ABORT_B), SW_SHOW);

			// Get the fisrt selected file name
			_this->m_iFile = _this->m_FilesList.begin();
			Item.iItem = *_this->m_iFile;
			ListView_GetItem(hWndRemoteList, &Item);

			GetDlgItemText(hWnd, IDC_CURR_REMOTE, szDstFile, sizeof(szDstFile));
			if (!strlen(szDstFile)) break; // no src dir selected
			strcat(szDstFile, szSelectedFile);

			// Request the first file of the list (-> triggers the transfer of the whole list)
			_this->m_fFileCommandPending = true;
			_this->m_fAbort = false;
			_this->m_fAborted = false;
			_this->RequestRemoteFile(szDstFile);

			}
			break;

		case IDC_LOCAL_ROOTB:
			{
			char ofDir[MAX_PATH];
			int nSelected = SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_GETCURSEL, 0, 0); 
			if (nSelected == -1) break;
			SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_GETLBTEXT, (WPARAM)nSelected, (LPARAM)ofDir); 
			//ofDir[4] = '\0'; // Hum...
			_this->PopulateLocalListBox(hWnd, ofDir);
			}
			break;

		case IDC_REMOTE_ROOTB:
			if (!_this->m_fFileCommandPending)
			{
				_this->m_fFileCommandPending = true;
				char ofDir[MAX_PATH];
				int nSelected = SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_GETCURSEL, 0, 0); 
				if (nSelected == -1) break;
				SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_GETLBTEXT, (WPARAM)nSelected, (LPARAM)ofDir); 
				//ofDir[4] = '\0'; // Hum...
				_this->RequestRemoteDirectoryContent(hWnd, ofDir);					
			}
			break;

		case IDC_LOCAL_UPB:
			{
			char szUpDirMask[16];
			sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
			_this->PopulateLocalListBox(hWnd, szUpDirMask);
			}
			break;

		case IDC_REMOTE_UPB:
			if (!_this->m_fFileCommandPending)
			{
				_this->m_fFileCommandPending = true;
				char szUpDirMask[16];
				sprintf(szUpDirMask, "%s..%s", rfbDirPrefix, rfbDirSuffix);
				_this->RequestRemoteDirectoryContent(hWnd, szUpDirMask);					
			}
			break;

		case IDC_ABORT_B:
			_this->m_fAbort = true;
			break;

		case IDC_DELETE_B:
			// Delete Local File
			if (_this->m_fFocusLocal)
			{
				HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
				int nCount = ListView_GetSelectedCount(hWndLocalList);

				char szMes[MAX_PATH + 96];
				TCHAR szSelectedFile[128];
				memset(szSelectedFile, 0, 128);
 				int nSelected = -1;

				TCHAR szCurrLocal[MAX_PATH];
				memset(szCurrLocal, 0, MAX_PATH);
				GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
				if (!strlen(szCurrLocal)) break; // no dst dir selected

				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iSubItem = 0;
				Item.pszText = szSelectedFile;
				Item.cchTextMax = 128;
				nCount = ListView_GetItemCount(hWndLocalList);
				_this->m_nConfirmAnswer = CONFIRM_YES;
				for (nSelected = 0; nSelected < nCount; nSelected++)
				{
					if(ListView_GetItemState(hWndLocalList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
					{
						Item.iItem = nSelected;
						ListView_GetItem(hWndLocalList, &Item);
						if (szSelectedFile[0] == rfbDirPrefix[0] && szSelectedFile[1] == rfbDirPrefix[1]) continue;// Only a file can be deleted
						GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
						if (strlen(szCurrLocal) + strlen(szSelectedFile) > MAX_PATH)
						{
							// TODO: Display Error
							continue;
						}
						if (_this->m_nConfirmAnswer == CONFIRM_YES || _this->m_nConfirmAnswer == CONFIRM_NO)
						{
							wsprintf(szMes, "%s\n\n< %s > ?\n", sz_H48,szSelectedFile);
							_this->DoFTConfirmDialog(sz_H47, _T(szMes));
							if (_this->m_nConfirmAnswer == CONFIRM_NO)
								continue;
							if (_this->m_nConfirmAnswer == CONFIRM_NOALL)
								break;
						}
						strcat(szCurrLocal, szSelectedFile);
						if (!DeleteFile(szCurrLocal))
						{
							wsprintf(szMes, "%s < %s >", sz_H49,szCurrLocal);
							_this->SetStatus(szMes);
							break;
						}
						wsprintf(szMes, "%s < %s > %s", sz_H17,szCurrLocal,sz_H50);
						_this->SetStatus(szMes);
					}
				}
				// Refresh the Local List view
				ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_LOCAL_FILELIST));
				_this->PopulateLocalListBox(hWnd, "");
			}
			else // Delete remote file
			{
				if (_this->m_fFileCommandPending) break;
				HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
				int nCount = ListView_GetSelectedCount(hWndRemoteList);

				char szMes[MAX_PATH + 96];
				TCHAR szSelectedFile[128];
				memset(szSelectedFile, 0, 128);
 				int nSelected = -1;

				TCHAR szCurrRemote[MAX_PATH];
				memset(szCurrRemote, 0, MAX_PATH);
				GetDlgItemText(hWnd, IDC_CURR_REMOTE, szCurrRemote, sizeof(szCurrRemote));
				if (!strlen(szCurrRemote)) break; // no dst dir selected

				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iSubItem = 0;
				Item.pszText = szSelectedFile;
				Item.cchTextMax = 128;
				nCount = ListView_GetItemCount(hWndRemoteList);
				_this->m_nConfirmAnswer = CONFIRM_YES;
				_this->m_nDeleteCount = 0;
				for (nSelected = 0; nSelected < nCount; nSelected++)
				{
					if(ListView_GetItemState(hWndRemoteList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
					{
						Item.iItem = nSelected;
						ListView_GetItem(hWndRemoteList, &Item);
						if (szSelectedFile[0] == rfbDirPrefix[0] && szSelectedFile[1] == rfbDirPrefix[1]) continue;// Only a file can be deleted
						GetDlgItemText(hWnd, IDC_CURR_REMOTE, szCurrRemote, sizeof(szCurrRemote));
						if (strlen(szCurrRemote) + strlen(szSelectedFile) > MAX_PATH) continue;
						if (_this->m_nConfirmAnswer == CONFIRM_YES || _this->m_nConfirmAnswer == CONFIRM_NO)
						{
							wsprintf(szMes, "%s\n\n< %s > ?\n", sz_H51, szSelectedFile);
							_this->DoFTConfirmDialog(sz_H47, _T(szMes));
							if (_this->m_nConfirmAnswer == CONFIRM_NO)
								continue;
							if (_this->m_nConfirmAnswer == CONFIRM_NOALL)
								break;
						}
						_this->m_fFileCommandPending = true;
						strcat(szCurrRemote, szSelectedFile);
						_this->m_nDeleteCount++;
						_this->DeleteRemoteFile(szCurrRemote);
					}
				}
			}
			break;

		case IDC_NEWFOLDER_B:
			// Create Local Folder
			if (_this->m_fFocusLocal)
			{
				char szMes[MAX_PATH + 96];
				TCHAR szCurrLocal[MAX_PATH];
				GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
				if (!strlen(szCurrLocal)) break; // no dst dir selected
				memset(_this->m_szFTParam, '\0', sizeof(_this->m_szFTParam));
				_this->DoFTParamDialog(sz_H57,sz_H52);
				if (strlen(_this->m_szFTParam) == 0 || (strlen(szCurrLocal) + strlen(_this->m_szFTParam)) > 248) 
				{
					// TODO: Error Message
					break;
				}
				strcat(szCurrLocal, _this->m_szFTParam);
				if (!CreateDirectory(szCurrLocal, NULL))
				{
					// TODO: Error Message
					wsprintf(szMes, "%s < %s >", sz_H53,szCurrLocal);
					_this->SetStatus(szMes);
					break;
				}

				wsprintf(szMes, "%s < %s > %s", sz_H54,szCurrLocal,sz_H55);
				_this->SetStatus(szMes);

				// Refresh the Local List view
				ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_LOCAL_FILELIST));
				_this->PopulateLocalListBox(hWnd, "");

			}
			else // Create Remote Folder
			{
				if (_this->m_fFileCommandPending) break;
				TCHAR szCurrRemote[MAX_PATH];
				GetDlgItemText(hWnd, IDC_CURR_REMOTE, szCurrRemote, sizeof(szCurrRemote));
				if (!strlen(szCurrRemote)) break; // no dst dir selected
				memset(_this->m_szFTParam, '\0', sizeof(_this->m_szFTParam));
				_this->DoFTParamDialog(sz_H57,sz_H56);
				if (strlen(_this->m_szFTParam) == 0 || (strlen(szCurrRemote) + strlen(szCurrRemote)) > 248) 
				{
					// TODO: Error Message
					break;
				}

				_this->m_fFileCommandPending = true;
				strcat(szCurrRemote, _this->m_szFTParam);
				_this->CreateRemoteDirectory(szCurrRemote);
			}
			break;

		case IDC_RENAME_B:
			{
			// Create Local Folder
			if (_this->m_fFocusLocal)
			{
				HWND hWndLocalList = GetDlgItem(hWnd, IDC_LOCAL_FILELIST);
				int nCount = ListView_GetSelectedCount(hWndLocalList);
				if (nCount == 0 || nCount > 1)
				{
					MessageBox(	NULL,
								sz_M1, 
								sz_M2, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
					break; 
				}

				char szMes[MAX_PATH + 96];
				TCHAR szSelectedFile[128];
				memset(szSelectedFile, 0, 128);
 				int nSelected = -1;

				TCHAR szCurrLocal[MAX_PATH];
				memset(szCurrLocal, 0, MAX_PATH);
				TCHAR szNewLocal[MAX_PATH];
				memset(szNewLocal, 0, MAX_PATH);
				GetDlgItemText(hWnd, IDC_CURR_LOCAL, szCurrLocal, sizeof(szCurrLocal));
				if (!strlen(szCurrLocal)) break; // no dst dir selected

				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iSubItem = 0;
				Item.pszText = szSelectedFile;
				Item.cchTextMax = 128;
				nCount = ListView_GetItemCount(hWndLocalList);
				for (nSelected = 0; nSelected < nCount; nSelected++)
				{
					if(ListView_GetItemState(hWndLocalList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
					{
						Item.iItem = nSelected;
						ListView_GetItem(hWndLocalList, &Item);
						_this->IsDirectoryGetIt(szSelectedFile);
						strcpy(_this->m_szFTParam, szSelectedFile);
						_this->DoFTParamDialog(sz_M3, sz_M4);
						if (strlen(_this->m_szFTParam) == 0 || (strlen(szCurrLocal) + strlen(_this->m_szFTParam)) > 248) 
						{
							// TODO: Error Message
							break;
						}
						strcpy(szNewLocal, szCurrLocal);
						strcat(szCurrLocal, szSelectedFile); // Old full name
						strcat(szNewLocal, _this->m_szFTParam); // New full name
						if (!MoveFile(szCurrLocal, szNewLocal))
						{
							wsprintf(szMes, "%s < %s >", sz_M5, szCurrLocal);
							_this->SetStatus(szMes);
							break;
						}

						wsprintf(szMes, "%s < %s > %s < %s >", sz_M6, szCurrLocal, sz_M7, szNewLocal);
						_this->SetStatus(szMes);
					}
				}
				// Refresh the Local List view
				ListView_DeleteAllItems(GetDlgItem(hWnd, IDC_LOCAL_FILELIST));
				_this->PopulateLocalListBox(hWnd, "");
			}
			else // Rename Remote
			{
				if (_this->m_fFileCommandPending) break;
				HWND hWndRemoteList = GetDlgItem(hWnd, IDC_REMOTE_FILELIST);
				int nCount = ListView_GetSelectedCount(hWndRemoteList);
				if (nCount == 0 || nCount > 1)
				{
					MessageBox(	NULL,
								sz_M1, 
								sz_M2, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
					break; 
				}

//				char szMes[MAX_PATH + 96];
				TCHAR szSelectedFile[128];
				memset(szSelectedFile, 0, 128);
 				int nSelected = -1;

				TCHAR szCurrRemote[MAX_PATH];
				memset(szCurrRemote, 0, MAX_PATH);
				TCHAR szNewRemote[MAX_PATH];
				memset(szNewRemote, 0, MAX_PATH);
				GetDlgItemText(hWnd, IDC_CURR_REMOTE, szCurrRemote, sizeof(szCurrRemote));
				if (!strlen(szCurrRemote)) break; // no dst dir selected

				LVITEM Item;
				Item.mask = LVIF_TEXT;
				Item.iSubItem = 0;
				Item.pszText = szSelectedFile;
				Item.cchTextMax = 128;
				nCount = ListView_GetItemCount(hWndRemoteList);
				for (nSelected = 0; nSelected < nCount; nSelected++)
				{
					if(ListView_GetItemState(hWndRemoteList, nSelected, LVIS_SELECTED) & LVIS_SELECTED)
					{
						Item.iItem = nSelected;
						ListView_GetItem(hWndRemoteList, &Item);
						_this->IsDirectoryGetIt(szSelectedFile);
						strcpy(_this->m_szFTParam, szSelectedFile);
						_this->DoFTParamDialog(sz_M3, sz_M4);
						if (strrchr(_this->m_szFTParam, '*') != NULL 
							|| 
							strlen(_this->m_szFTParam) == 0 ||
							(strlen(szCurrRemote) + strlen(_this->m_szFTParam)) > 248) 
						{
							// TODO: Error Message
							break;
						}
						strcpy(szNewRemote, szCurrRemote);
						strcat(szCurrRemote, szSelectedFile); // Old full name
						strcat(szNewRemote, _this->m_szFTParam); // New full name

						_this->m_fFileCommandPending = true;
						_this->RenameRemoteFileOrDirectory(szCurrRemote, szNewRemote);
					}
				}
			}
			}
			break;

		case IDC_LOCAL_DRIVECB: 
            switch (HIWORD(wParam)) 
            { 
		        case CBN_SELCHANGE:
					char ofDir[MAX_PATH];
					int nSelected = SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_GETCURSEL, 0, 0); 
					if (nSelected == -1) break;
					SendMessage(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), CB_GETLBTEXT, (WPARAM)nSelected, (LPARAM)ofDir); 
					// ofDir[4] = '\0'; // Hum...
					_this->PopulateLocalListBox(hWnd, ofDir);
					// UpdateWindow(hWnd);
					break;
			}
			break;

		case IDC_REMOTE_DRIVECB:
            switch (HIWORD(wParam)) 
            { 
		        case CBN_SELCHANGE: 
					char ofDir[MAX_PATH];
					int nSelected = SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_GETCURSEL, 0, 0); 
					if (nSelected == -1) break;
					SendMessage(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), CB_GETLBTEXT, (WPARAM)nSelected, (LPARAM)ofDir); 
					//ofDir[4] = '\0'; // Hum...
					_this->RequestRemoteDirectoryContent(hWnd, ofDir);					
					// UpdateWindow(hWnd);
					break;
			}
			break;

		case IDC_HIDE_B:
			_this->ShowFileTransferWindow(false);
			return TRUE;

		}
		break;


	case WM_SYSCOMMAND:
		switch (LOWORD(wParam))
		{
		case SC_RESTORE:
			_this->ShowFileTransferWindow(true);
			return TRUE;

		case SC_MINIMIZE:
			SendMessage(hWnd,WM_COMMAND,MAKEWPARAM(IDC_HIDE_B,0),0);
			return FALSE;

		}
		break;

	case WM_SIZING:

		LPRECT lprc;
		lprc = (LPRECT)lParam;
		switch(wParam)
        {
		case WMSZ_TOPLEFT:
			FTAdjustTop(lprc);
			FTAdjustLeft(lprc);

		case WMSZ_TOP:
			FTAdjustTop(lprc);

		case WMSZ_TOPRIGHT:
			FTAdjustTop(lprc);
			FTAdjustRight(lprc);

		case WMSZ_LEFT:
			FTAdjustLeft(lprc);

		case WMSZ_RIGHT:
			FTAdjustRight(lprc);

		case WMSZ_BOTTOMLEFT:
			FTAdjustBottom(lprc);
			FTAdjustLeft(lprc);

		case WMSZ_BOTTOM:
			FTAdjustBottom(lprc);

		case WMSZ_BOTTOMRIGHT:
			FTAdjustBottom(lprc);
			FTAdjustRight(lprc);
		}
		return TRUE;


	case WM_SIZE:

		int cx;
		int cy;
		int icx;
		int icy;
		int lf_an;
		RECT rc;

		if(wParam == SIZE_MINIMIZED)
		{
			break;
		}

		cx = LOWORD(lParam);	//Client Width
		cy = HIWORD(lParam);	//Client Height
		icy = cy-85-50;
		icx = cx/2 - (21+4) * 2 - 94 - 95 - 7 * 4;
		lf_an=(cx - 112)/2;

		//Left
		GetWindowRect(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), &rc);
		MoveWindow(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB),              4,       4,   icx,  rc.bottom - rc.top, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_LM_STATIC),            4+icx+7,       4,   141,                  19, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_LOCAL_ROOTB),    4+icx+7+141+7,       4,    25,                  18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_LOCAL_UPB), 4+icx+7+141+7+25+4,       4,    25,                  18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_CURR_LOCAL),                 4,      25, lf_an,                  18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_LOCAL_FILELIST),             4,      46, lf_an,                 icy, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_LOCAL_STATUS),               4, cy-85+4, lf_an,                  15, TRUE);

		//Right
		GetWindowRect(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), &rc);
		MoveWindow(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB),               lf_an+109,       4,   icx,   rc.bottom - rc.top, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_RM_STATIC),              lf_an+109+icx+7,       4,   141,                   19, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_REMOTE_ROOTB),     lf_an+109+icx+7+141+7,       4,    25,                   18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_REMOTE_UPB),  lf_an+109+icx+7+141+7+25+4,       4,    25,                   18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_CURR_REMOTE),                  lf_an+109,      25, lf_an,                   18, TRUE); 
		MoveWindow(GetDlgItem(hWnd, IDC_REMOTE_FILELIST),              lf_an+109,      46, lf_an,                  icy, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_REMOTE_STATUS),                lf_an+109, cy-85+4, lf_an,                   15, TRUE);

		//Bottom
		icx = cx-135-69;
		MoveWindow(GetDlgItem(hWnd, IDC_HS_STATIC),                  8,          cy-85+4+18+4,     39,                15, TRUE);
		GetWindowRect(GetDlgItem(hWnd, IDC_HISTORY_CB), &rc);
		MoveWindow(GetDlgItem(hWnd, IDC_HISTORY_CB),                65,            cy-85+4+18,  cx-69,  rc.bottom-rc.top, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_PR_STATIC),                  8,   cy-85+4+15+4+4+18+3,     56,                15, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_PROGRESS),                  65,   cy-85+4+15+4+4+18+2,    icx,                15, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_PERCENT),             65+icx+6, cy-85+4+10+4+4+18+4+2,     25,                12, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_GLOBAL_STATUS),  65+icx+6+25+3, cy-85+4+10+4+4+18+4+2,     97,                12, TRUE);
		GetWindowRect(GetDlgItem(hWnd, IDC_STATUS), &rc);
		MoveWindow(GetDlgItem(hWnd, IDC_STATUS),                     0, cy-(rc.bottom-rc.top),     cx,  rc.bottom-rc.top, TRUE);

		//Center
		icy = 46+icy/2;
		MoveWindow(GetDlgItem(hWnd, IDC_UPLOAD_B),     lf_an+10+2,  icy-15-20-6-20-5-20, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_DOWNLOAD_B),   lf_an+10+2,       icy-15-20-6-20, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_ABORT_B),      lf_an+10+2,            icy-15-20, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_DELETE_B),     lf_an+10+2,               icy+15, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_NEWFOLDER_B),  lf_an+10+2,          icy+15+20+6, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_RENAME_B),     lf_an+10+2,     icy+15+20+6+20+6, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDC_HIDE_B),       lf_an+10+2,        cy-103-20-4-6, 90, 20, TRUE);
		MoveWindow(GetDlgItem(hWnd, IDCANCEL),         lf_an+10+2,             cy-103-4, 90, 20, TRUE);
		InvalidateRect(hWnd, NULL, FALSE);

		FTAdjustFileNameColumns(hWnd); // sf@2006

		return TRUE;


	// Messages from ListViews
	case WM_NOTIFY:
		{
			// int nId = (int) wParam;
			LPNMHDR lpNmh = (LPNMHDR) lParam;

			switch(lpNmh->code)
			{
			case HDN_ITEMCLICK:
				return TRUE;

			case NM_SETFOCUS:
				if (lpNmh->hwndFrom == GetDlgItem(hWnd, IDC_LOCAL_FILELIST))
				{
					_this->m_fFocusLocal = true;
					char szTxt[64];
					HWND hB = GetDlgItem(hWnd, IDC_DELETE_B);
					sprintf(szTxt, "<- %s", _this->m_szDeleteButtonLabel);
					SetWindowText(hB, szTxt);
					hB = GetDlgItem(hWnd, IDC_NEWFOLDER_B);
					sprintf(szTxt, "<- %s", _this->m_szNewFolderButtonLabel);
					SetWindowText(hB, szTxt);
					hB = GetDlgItem(hWnd, IDC_RENAME_B);
					sprintf(szTxt, "<- %s", _this->m_szRenameButtonLabel);
					SetWindowText(hB, szTxt);
				}

				if (lpNmh->hwndFrom == GetDlgItem(hWnd, IDC_REMOTE_FILELIST))
				{
					_this->m_fFocusLocal = false;
					char szTxt[64];
					HWND hB = GetDlgItem(hWnd, IDC_DELETE_B);
					sprintf(szTxt, "%s ->", _this->m_szDeleteButtonLabel);
					SetWindowText(hB, szTxt);
					hB = GetDlgItem(hWnd, IDC_NEWFOLDER_B);
					sprintf(szTxt, "%s ->", _this->m_szNewFolderButtonLabel);
					SetWindowText(hB, szTxt);
					hB = GetDlgItem(hWnd, IDC_RENAME_B);
					sprintf(szTxt, "%s ->", _this->m_szRenameButtonLabel);
					SetWindowText(hB, szTxt);
				}

				return TRUE;

			case NM_DBLCLK:
				if (lpNmh->hwndFrom == GetDlgItem(hWnd, IDC_LOCAL_FILELIST))
				{
					_this->PopulateLocalListBox(hWnd, "");
				}

				if (lpNmh->hwndFrom == GetDlgItem(hWnd, IDC_REMOTE_FILELIST))
				{
					if (!_this->m_fFileCommandPending)
					{
						_this->m_fFileCommandPending = true;
						_this->RequestRemoteDirectoryContent(hWnd, "");
					}
				}
				return TRUE;
			}

		}
		break;

	case WM_DESTROY:
		if (_this->m_timer != 0) KillTimer(hWnd, _this->m_timer);
		EndDialog(hWnd, FALSE);
		return TRUE;

	}
	/*
	// Process FileTransfer asynchronous Send Packet Message
	if (uMsg == FileTransferSendPacketMessage)
	{
		_this->SendFileChunk();
		return 0;
	}
	*/

	return 0;
}


//
// Params acquisition Dialog Box
// 
int FileTransfer::DoFTParamDialog(LPSTR szTitle, LPSTR szComment)
{
	strcpy(m_szFTParamTitle, szTitle);
	strcpy(m_szFTParamComment, szComment);
#ifndef _ULTRAVNCAX_
	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FTPARAM_DLG), NULL, (DLGPROC) FTParamDlgProc, (LONG) this);
#else
	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FTPARAM_DLG), hWnd, (DLGPROC) FTParamDlgProc, (LONG) this);
#endif
}


BOOL CALLBACK FileTransfer::FTParamDlgProc(  HWND hwnd,  UINT uMsg, WPARAM wParam, LPARAM lParam )
{
	FileTransfer *_this = (FileTransfer *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (uMsg)
	{
	case WM_INITDIALOG:
		{
			SetWindowLong(hwnd, GWL_USERDATA, lParam);
			_this = (FileTransfer *) lParam;
			CentreWindow(hwnd);

			// Set Title
			SetWindowText(hwnd, _this->m_szFTParamTitle);
			// Set Comment
			SetDlgItemText(hwnd, IDC_FTPARAMCOMMENT, _this->m_szFTParamComment);
			//S et param initial value
			SetDlgItemText(hwnd, IDC_FTPARAM_EDIT, _this->m_szFTParam);

			return TRUE;
		}

	case WM_COMMAND:
		switch (LOWORD(wParam))
		{
		case IDOK:
			{
				UINT res = GetDlgItemText( hwnd,  IDC_FTPARAM_EDIT, _this->m_szFTParam, 256);
				EndDialog(hwnd, TRUE);
				return TRUE;
			}
		case IDCANCEL:
			strcpy(_this->m_szFTParam, "");
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


//
// Params acquisition Dialog Box
// 
int FileTransfer::DoFTConfirmDialog(LPSTR szTitle, LPSTR szComment)
{
	strcpy(m_szFTConfirmTitle, szTitle);
	strcpy(m_szFTConfirmComment, szComment);
#ifndef _ULTRAVNCAX_
	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FTCONFIRM_DLG), NULL, (DLGPROC) FTConfirmDlgProc, (LONG) this);
#else
	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_FTCONFIRM_DLG), hWnd, (DLGPROC) FTConfirmDlgProc, (LONG) this);
#endif
}


BOOL CALLBACK FileTransfer::FTConfirmDlgProc(  HWND hwnd,  UINT uMsg, WPARAM wParam, LPARAM lParam )
{
	FileTransfer *_this = (FileTransfer *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (uMsg)
	{
	case WM_INITDIALOG:
		{
			SetWindowLong(hwnd, GWL_USERDATA, lParam);
			_this = (FileTransfer *) lParam;
			CentreWindow(hwnd);

			// Set Title
			SetWindowText(hwnd, _this->m_szFTConfirmTitle);
			// Set Comment
			SetDlgItemText(hwnd, IDC_FTPCONFIRMCOMMENT, _this->m_szFTConfirmComment);

			// Todo: Init buttons labels with corrsponding culture (En, Fr, De...)
			return TRUE;
		}

	case WM_COMMAND:
		switch (LOWORD(wParam))
		{
		case IDC_YES_B:
				_this->m_nConfirmAnswer = CONFIRM_YES;
				EndDialog(hwnd, TRUE);
				return TRUE;

		case IDC_YESALL_B:
				_this->m_nConfirmAnswer = CONFIRM_YESALL;
				EndDialog(hwnd, TRUE);
				return TRUE;

		case IDC_NO_B:
				_this->m_nConfirmAnswer = CONFIRM_NO;
				EndDialog(hwnd, TRUE);
				return TRUE;

		case IDC_NOALL_B:
				_this->m_nConfirmAnswer = CONFIRM_NOALL;
				EndDialog(hwnd, TRUE);
				return TRUE;

		case IDOK:
				EndDialog(hwnd, TRUE);
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

//
//
//
void FileTransfer::DisableButtons(HWND hWnd)
{
	ShowWindow(GetDlgItem(hWnd, IDC_UPLOAD_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_DOWNLOAD_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_DELETE_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_NEWFOLDER_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_RENAME_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDCANCEL), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_HIDE_B), SW_SHOW);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_FILELIST), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_ROOTB), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_UPB), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_FILELIST), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_ROOTB), FALSE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_UPB), FALSE);
}

//
//
//
void FileTransfer::EnableButtons(HWND hWnd)
{
	ShowWindow(GetDlgItem(hWnd, IDC_ABORT_B), SW_HIDE);
	ShowWindow(GetDlgItem(hWnd, IDC_UPLOAD_B), SW_SHOW);
	ShowWindow(GetDlgItem(hWnd, IDC_DOWNLOAD_B), SW_SHOW);
	ShowWindow(GetDlgItem(hWnd, IDCANCEL), SW_SHOW);
	ShowWindow(GetDlgItem(hWnd, IDC_DELETE_B), SW_SHOW);
	ShowWindow(GetDlgItem(hWnd, IDC_NEWFOLDER_B), SW_SHOW);
	if (!m_fOldFTProtocole)
		ShowWindow(GetDlgItem(hWnd, IDC_RENAME_B), SW_SHOW);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_FILELIST), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_DRIVECB), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_ROOTB), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_LOCAL_UPB), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_FILELIST), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_DRIVECB), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_ROOTB), TRUE);
	EnableWindow(GetDlgItem(hWnd, IDC_REMOTE_UPB), TRUE);
}



void FileTransfer::InitListViewImagesList(HWND hListView)
{
    HIMAGELIST hImageList = ImageList_Create(16, 16, ILC_MASK, 2, 2); 

    HICON hIcon = LoadIcon(pApp->m_instance, MAKEINTRESOURCE(IDI_DIR)); 
    ImageList_AddIcon(hImageList, hIcon);  
    DestroyIcon(hIcon); 

    hIcon = LoadIcon(pApp->m_instance, MAKEINTRESOURCE(IDI_FILE)); 
    ImageList_AddIcon(hImageList, hIcon); 
    DestroyIcon(hIcon); 

    hIcon = LoadIcon(pApp->m_instance, MAKEINTRESOURCE(IDI_DRIVE)); 
    ImageList_AddIcon(hImageList, hIcon); 
    DestroyIcon(hIcon); 

    ListView_SetImageList(hListView, hImageList, LVSIL_SMALL); 
}