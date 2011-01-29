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

#include "stdhdrs.h"
#include "vncviewer.h"
#include "TextChat.h"
#include "Exception.h"
#include "commctrl.h"
#include "richedit.h"

#define TEXTMAXSIZE 16384
#define MAXNAMESIZE	128 // MAX_COMPUTERNAME_LENGTH+1 (32)
#define CHAT_OPEN  -1 // Todo; put these codes in rfbproto.h
#define CHAT_CLOSE -2
#define CHAT_FINISHED -3
extern char sz_E1[64];
extern char sz_E2[64];

//
//
//
TextChat::TextChat(VNCviewerApp *pApp, ClientConnection *pCC)
{
	m_pApp	= pApp;
	m_pCC	= pCC;
	m_fTextChatRunning = false;
	m_fVisible = true;
	
	m_fPersistentTexts = false;
	m_szLocalText = new char [TEXTMAXSIZE];
	memset(m_szLocalText, 0, TEXTMAXSIZE);
	m_szLastLocalText = new char [TEXTMAXSIZE];
	memset(m_szLastLocalText, 0, TEXTMAXSIZE);
	m_szRemoteText = new char [TEXTMAXSIZE];
	memset(m_szRemoteText, 0, TEXTMAXSIZE);
	m_szRemoteName = new char[MAXNAMESIZE];
	memset(m_szRemoteName,0,MAXNAMESIZE);
	m_szLocalName =  new char[MAXNAMESIZE];
	memset(m_szLocalName,0,MAXNAMESIZE);

	m_szTextBoxBuffer = new char[TEXTMAXSIZE]; // History Text (containing all chat messages from everybody)
	memset(m_szTextBoxBuffer,0,TEXTMAXSIZE);

	unsigned long pcSize=MAXNAMESIZE;
	if (GetComputerName(m_szLocalName,&pcSize))
	{
		if( pcSize >= MAXNAMESIZE)
		{
			m_szLocalName[MAXNAMESIZE-4]='.';
			m_szLocalName[MAXNAMESIZE-3]='.';
			m_szLocalName[MAXNAMESIZE-2]='.';
			m_szLocalName[MAXNAMESIZE-1]=0x00;
		}
	}

	//  Load the Rich Edit control DLL
	m_hRichEdit = LoadLibrary( "RICHED32.DLL" );
	if (!m_hRichEdit)
	{  
		MessageBox( NULL, sz_E1,
					sz_E2, MB_OK | MB_ICONEXCLAMATION );
		// Todo: do normal edit instead (no colors)
	}

}


//
//
//
TextChat::~TextChat()
{
	if (m_szLocalText != NULL) delete [] m_szLocalText;
	if (m_szLastLocalText != NULL) delete [] m_szLastLocalText;
	if (m_szRemoteText != NULL) delete [] m_szRemoteText;
	if (m_szRemoteName != NULL) delete [] m_szRemoteName;
	if (m_szLocalName != NULL) delete [] m_szLocalName;
	if (m_szTextBoxBuffer != NULL) delete [] m_szTextBoxBuffer;

	m_fTextChatRunning = false;
	SendMessage(m_hDlg, WM_COMMAND, IDOK, 0L);

	if (m_hRichEdit != NULL) FreeLibrary(m_hRichEdit);

}


//
// Set text format to a selection in the Chat area
//
void TextChat::SetTextFormat(bool bBold /*= false */, bool bItalic /*= false*/
	, long nSize /*= 0x75*/, const char* szFaceName /*= "MS Sans Serif"*/, DWORD dwColor /*= BLACK*/)
{
	if ( GetDlgItem( m_hDlg, IDC_CHATAREA_EDIT ) )  //  Sanity Check
	{		
		CHARFORMAT cf;
		memset( &cf, 0, sizeof(CHARFORMAT) ); //  Initialize structure

		cf.cbSize = sizeof(CHARFORMAT);             
		cf.dwMask = CFM_COLOR | CFM_FACE | CFM_SIZE;
		if (bBold)
		{
			cf.dwMask |= CFM_BOLD;
			cf.dwEffects |= CFE_BOLD;
		}
		if (bItalic)
		{
			cf.dwMask |= CFM_ITALIC;  
			cf.dwEffects |= CFE_ITALIC;
		}
		cf.crTextColor = dwColor;					// set color in AABBGGRR mode (alpha-RGB)
		cf.yHeight = nSize;							// set size in points
		strcpy( cf.szFaceName, szFaceName);
													
		SendDlgItemMessage( m_hDlg, IDC_CHATAREA_EDIT, EM_SETCHARFORMAT, SCF_SELECTION, (LPARAM)&cf );
	}
}


//
//
//
void TextChat::ProcessTextChatMsg() 
{
	rfbTextChatMsg tcm;
	m_pCC->ReadExact(((char *) &tcm) + m_pCC->m_nTO, sz_rfbTextChatMsg - m_pCC->m_nTO);
	int len = Swap32IfLE(tcm.length);
	
	if (len == CHAT_OPEN)
	{
		// Open TextChat Dialog
		PostMessage(m_pCC->m_hwndMain, WM_SYSCOMMAND, ID_TEXTCHAT, (LPARAM)0L);
		return;
	}
	else if (len == CHAT_CLOSE)
	{
		// Close TextChat Dialog
		if (!m_fTextChatRunning) return;
		PostMessage(m_hDlg, WM_COMMAND, IDOK, 0);
		return;
	}
	else if (len == CHAT_FINISHED)
	{
		// On Server notif, request FullScreen update
		if (!m_fTextChatRunning) return;
		m_fTextChatRunning = false;
		m_pCC->SendAppropriateFramebufferUpdateRequest();
		return;
	}
	else
	{
		// Read the incoming text
		if (len > TEXTMAXSIZE) return; // Todo: Improve this...
		if (len == 0)
		{
			SetDlgItemText(m_hDlg, IDC_REMOTETEXT_EDIT, "");
			memset(m_szRemoteText, 0, TEXTMAXSIZE);
		}
		else
		{
			memset(m_szRemoteText, 0, TEXTMAXSIZE);
			m_pCC->ReadExact(m_szRemoteText, len);
			PrintMessage(m_szRemoteText, m_szRemoteName, RED);
			ShowChatWindow(true); // Show the chat window on new message reception
		}
	}
}


//
//
//
void TextChat::SendTextChatRequest(int nMsg)
{
    rfbTextChatMsg tcm;
    tcm.type = rfbTextChat;
	tcm.length = Swap32IfLE(nMsg);
    m_pCC->WriteExact((char *)&tcm, sz_rfbTextChatMsg, rfbTextChat);
	return;
}

//
// Output messages in the chat area 
//
//
void TextChat::PrintMessage(const char* szMessage,const char* szSender,DWORD dwColor /*= BLACK*/)
{
	// char szTextBoxBuffer[TEXTMAXSIZE];			
	// memset(szTextBoxBuffer,0,TEXTMAXSIZE);			
	CHARRANGE cr;	
	
	GETTEXTLENGTHEX lpG;
	lpG.flags = GTL_NUMBYTES;
	lpG.codepage = CP_ACP; // ANSI

	int nLen = SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, EM_GETTEXTLENGTHEX, (WPARAM)&lpG, 0L);
	if (nLen + 32 > TEXTMAXSIZE )
	{
		memset(m_szTextBoxBuffer, 0, TEXTMAXSIZE);
		strcpy(m_szTextBoxBuffer, "------------------------------------------------------------------------------------------------------------------------\n");
		SetDlgItemText(m_hDlg, IDC_CHATAREA_EDIT, m_szTextBoxBuffer);
	}

	// Todo: test if chat text + sender + message > TEXTMAXSIZE -> Modulo display
	if (szSender != NULL) //print the sender's name
	{
		/*
		// Time
		char tbuffer[9];
		_tzset();
		_strtime(tbuffer);

		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,WM_GETTEXT, TEXTMAXSIZE-1,(LONG)m_szTextBoxBuffer);
		cr.cpMax = strlen(m_szTextBoxBuffer);	 // Select the last caracter to make the text insertion
		cr.cpMin  = cr.cpMax;
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,EM_EXSETSEL,0,(LONG) &cr);
		SetTextFormat(false, false, 0x75, "MS Sans Serif", GREY);
		_snprintf(m_szTextBoxBuffer, TEXTMAXSIZE-1, "[%s]", tbuffer);		
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,EM_REPLACESEL,FALSE,(LONG)m_szTextBoxBuffer); // Replace the selection with the message
		*/

		// Sender
		// SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,WM_GETTEXT, TEXTMAXSIZE-1,(LONG)m_szTextBoxBuffer);
		cr.cpMax = nLen; //strlen(m_szTextBoxBuffer);	 // Select the last caracter to make the text insertion
		cr.cpMin  = cr.cpMax;
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, EM_EXSETSEL, 0, (LONG)&cr);
		/***/
		SetTextFormat(false, false, 0x75, "MS Sans Serif", dwColor);
		_snprintf(m_szTextBoxBuffer, MAXNAMESIZE-1 + 4, "<%s>: ", szSender);		
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,EM_REPLACESEL,FALSE,(LONG)m_szTextBoxBuffer); // Replace the selection with the message
	}

	nLen = SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, EM_GETTEXTLENGTHEX, (WPARAM)&lpG, 0L);
	if (szMessage != NULL) //print the sender's message
	{	
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,WM_GETTEXT, TEXTMAXSIZE-1,(LONG)m_szTextBoxBuffer);
		cr.cpMax = nLen; //strlen(m_szTextBoxBuffer);	 // Select the last caracter to make the text insertion
		cr.cpMin  = cr.cpMax;
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT,EM_EXSETSEL,0,(LONG) &cr);
		/***/
		SetTextFormat(false, false, 0x75, "MS Sans Serif", dwColor != GREY ? BLACK : GREY);	
		_snprintf(m_szTextBoxBuffer, TEXTMAXSIZE-1, "%s", szMessage);		
		SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, EM_REPLACESEL, FALSE, (LONG)m_szTextBoxBuffer); 
	}

	// Scroll down the chat window
	// The following seems necessary under W9x & Me if we want the Edit to scroll to bottom...
	SCROLLINFO si;
    ZeroMemory(&si, sizeof(SCROLLINFO));
    si.cbSize = sizeof(SCROLLINFO);
    si.fMask = SIF_RANGE|SIF_PAGE;
    GetScrollInfo(GetDlgItem(m_hDlg, IDC_CHATAREA_EDIT), SB_VERT, &si);
	si.nPos = si.nMax - max(si.nPage - 1, 0);
	SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, WM_VSCROLL, MAKELONG(SB_THUMBPOSITION, si.nPos), 0L);	// Scroll down the ch

	// This line does the bottom scrolling correctly under NT4,W2K, XP...
	// SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, WM_VSCROLL, SB_BOTTOM, 0L);

}

//
// Send local text content
//
void TextChat::SendLocalText(void)
{
	// We keep it because we could use it
	// for future retype functionality. (F3)
	memcpy(m_szLastLocalText, m_szLocalText, strlen(m_szLocalText));

	PrintMessage(m_szLocalText, m_szLocalName, BLUE);

    rfbTextChatMsg tcm;
    tcm.type = rfbTextChat;
	tcm.length = Swap32IfLE(strlen(m_szLocalText));
    m_pCC->WriteExact((char *)&tcm, sz_rfbTextChatMsg, rfbTextChat);
	m_pCC->WriteExact((char *)m_szLocalText, strlen(m_szLocalText));

	//and we clear the input box
	SetDlgItemText(m_hDlg, IDC_INPUTAREA_EDIT, "");
	return;
}


//
//
//
int TextChat::DoDialog()
{
#ifndef _ULTRAVNCAX_
 	return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_TEXTCHAT_DLG), 
							NULL, (DLGPROC) TextChatDlgProc, (LONG) this);
#else
 	HWND h = CreateDialogParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_TEXTCHAT_DLG), 
							NULL, (DLGPROC) TextChatDlgProc, (LONG) this);
	ShowWindow( h, SW_SHOW );
	return 0;
#endif
}


//
//
//
void TextChat::KillDialog()
{
	// DestroyWindow(m_hDlg);
}

//
//
//
void TextChat::ShowChatWindow(bool fVisible)
{
	ShowWindow(m_hDlg, fVisible ? SW_RESTORE : SW_MINIMIZE);
	SetForegroundWindow(m_hDlg);
	// Put the Chat Windows always on Top if fullscreen
	if (fVisible && m_pCC->InFullScreenMode())
	{
		RECT Rect;
		GetWindowRect(m_hDlg, &Rect);
		SetWindowPos(m_hDlg, 
					HWND_TOPMOST,
					Rect.left,
					Rect.top,
					Rect.right - Rect.left,
					Rect.bottom - Rect.top,
					SWP_SHOWWINDOW);
	}

	m_fVisible = fVisible; // This enables screen updates to be processed in ClientConnection
	// Refresh screen view if Chat window has been hidden
	if (!fVisible)
		m_pCC->SendAppropriateFramebufferUpdateRequest();
}

//
//
//
BOOL CALLBACK TextChat::TextChatDlgProc(  HWND hWnd,  UINT uMsg,  WPARAM wParam, LPARAM lParam )
{

	TextChat* _this = (TextChat *) GetWindowLong(hWnd, GWL_USERDATA);

	switch (uMsg)
	{

	case WM_INITDIALOG:
		{
			SetWindowLong(hWnd, GWL_USERDATA, lParam);
            TextChat *_this = (TextChat *) lParam;
			if (_this->m_szLocalText == NULL || _this->m_szRemoteText == NULL)
				EndDialog(hWnd, FALSE);

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

            // CentreWindow(hWnd);
			_this->m_hWnd = hWnd;
			_this->m_hDlg = hWnd;
			
			if (_snprintf(_this->m_szRemoteName,MAXNAMESIZE-1,"%s", _this->m_pCC->m_desktopName) < 0 )
			{
				_this->m_szRemoteName[MAXNAMESIZE-4]='.';
				_this->m_szRemoteName[MAXNAMESIZE-3]='.';
				_this->m_szRemoteName[MAXNAMESIZE-2]='.';
				_this->m_szRemoteName[MAXNAMESIZE-1]=0x00;
			}	
			

			const long lTitleBufSize=256;			
			char szTitle[lTitleBufSize];
			
			_snprintf(szTitle,lTitleBufSize-1," Chat with <%s> - VMOpsVNC",_this->m_szRemoteName);
			SetWindowText(hWnd, szTitle);			

			// Trunc the remote name for display in Chat Area before the first parenthesis, if any.
			char *p = strchr(_this->m_szRemoteName, '('); 
			if (p != NULL) *(p - 1) = '\0';

			//  Chat area			
			_this->SetTextFormat(); //  Set character formatting and background color
			SendDlgItemMessage( hWnd, IDC_CHATAREA_EDIT, EM_SETBKGNDCOLOR, FALSE, 0xFFFFFF ); 

			memset(_this->m_szLocalText, 0, TEXTMAXSIZE);
			// if (!_this->m_fPersistentTexts)
			{
				memset(_this->m_szLastLocalText, 0, TEXTMAXSIZE);
				memset(_this->m_szTextBoxBuffer, 0, TEXTMAXSIZE); // Clear Chat area 
				 //  Load and display diclaimer message
				// sf@2005 - Only if the DSMplugin is used
				if (!_this->m_pCC->m_fUsePlugin)
				{
					if (LoadString(_this->m_pApp->m_instance, IDS_WARNING, _this->m_szRemoteText, TEXTMAXSIZE -1) )
						_this->PrintMessage(_this->m_szRemoteText, NULL ,GREY);
				}
			}

			SetDlgItemText(hWnd, IDC_INPUTAREA_EDIT, _this->m_szLocalText);
			SetDlgItemText(hWnd, IDC_CHATAREA_EDIT, _this->m_szTextBoxBuffer); // Chat area

			// Scroll down the chat window
			// The following seems necessary under W9x & Me if we want the Edit to scroll to bottom...
			SCROLLINFO si;
			ZeroMemory(&si, sizeof(SCROLLINFO));
			si.cbSize = sizeof(SCROLLINFO);
			si.fMask = SIF_RANGE|SIF_PAGE;
			GetScrollInfo(GetDlgItem(hWnd, IDC_CHATAREA_EDIT), SB_VERT, &si);
			si.nPos = si.nMax - max(si.nPage - 1, 0);
			SendDlgItemMessage(hWnd, IDC_CHATAREA_EDIT, WM_VSCROLL, MAKELONG(SB_THUMBPOSITION, si.nPos), 0L);	
			// This line does the bottom scrolling correctly under NT4,W2K, XP...
			// SendDlgItemMessage(m_hDlg, IDC_CHATAREA_EDIT, WM_VSCROLL, SB_BOTTOM, 0L);

			// SendDlgItemMessage(hWnd, IDC_PERSISTENT_CHECK, BM_SETCHECK, _this->m_fPersistentTexts, 0);

			// Tell the other side to open the TextChat Window
			_this->SendTextChatRequest(CHAT_OPEN);

			SetForegroundWindow(hWnd);
			
			return TRUE;
		}
		// break;
	case WM_COMMAND:
		switch (LOWORD(wParam))
		{
		/*
		case IDC_PERSISTENT_CHECK:
		    _this->m_fPersistentTexts = (SendDlgItemMessage(hWnd, IDC_PERSISTENT_CHECK, BM_GETCHECK, 0, 0) == BST_CHECKED);
			return TRUE;
		*/

		case IDOK:
			// Server orders to close TextChat 			
			EndDialog(hWnd, FALSE);
			return TRUE;

		case IDCANCEL:			
			_this->SendTextChatRequest(CHAT_CLOSE); // Server must close TextChat
			EndDialog(hWnd, FALSE);
			return TRUE;

		case IDC_SEND_B:
			{
			memset(_this->m_szLocalText,0,TEXTMAXSIZE);
			UINT nRes = GetDlgItemText( hWnd, IDC_INPUTAREA_EDIT, _this->m_szLocalText, TEXTMAXSIZE-1);
			strcat(_this->m_szLocalText, "\n");
			_this->SendLocalText();		
			SetFocus(GetDlgItem(hWnd, IDC_INPUTAREA_EDIT));
			}
			return TRUE;

		case IDC_HIDE_B:
			_this->ShowChatWindow(false);
			return TRUE;

		case IDC_INPUTAREA_EDIT:
			if(HIWORD(wParam) == EN_UPDATE)			
			{
				memset(_this->m_szLocalText,0,TEXTMAXSIZE);
				UINT nRes = GetDlgItemText( hWnd, IDC_INPUTAREA_EDIT, _this->m_szLocalText, TEXTMAXSIZE);
				if (strstr(_this->m_szLocalText,"\n") >0 ) 
				{
					_this->SendLocalText();			
				}								
			}
			return TRUE;
		}
		break;

	case WM_SYSCOMMAND:
		switch (LOWORD(wParam))
		{
		case SC_RESTORE:
			_this->ShowChatWindow(true);
			//SetFocus(GetDlgItem(hWnd, IDC_INPUTAREA_EDIT));
			return TRUE;
		}
		break;

	case WM_DESTROY:
		// _this->SendTextChatRequest(_this, CHAT_FINISHED);
		EndDialog(hWnd, FALSE);
		return TRUE;
	}
	return 0;
}



