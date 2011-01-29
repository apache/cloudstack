//  Copyright (C) 2002 Ultr@VNC Team Members. All Rights Reserved.
//
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


// AuthDialog.cpp: implementation of the AuthDialog class.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "AuthDialog.h"
#include "Exception.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

AuthDialog::AuthDialog()
#ifdef _ULTRAVNCAX_
: parent( NULL )
#endif
{
	m_passwd[0]=__T('\0');
}

AuthDialog::~AuthDialog()
{
}

int AuthDialog::DoDialog(bool ms_logon, bool isSecure)
{
	HWND			hwndParentWnd = NULL;

#ifdef _ULTRAVNCAX_
	hwndParentWnd = parent;
#endif

	if (isSecure) return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_AUTH_DIALOG), hwndParentWnd, (DLGPROC) DlgProc, (LONG) this);
	else if (ms_logon) return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_AUTH_DIALOG2), hwndParentWnd, (DLGPROC) DlgProc, (LONG) this);
	else return DialogBoxParam(pApp->m_instance, DIALOG_MAKEINTRESOURCE(IDD_AUTH_DIALOG1), hwndParentWnd, (DLGPROC) DlgProc1, (LONG) this);
}

BOOL CALLBACK AuthDialog::DlgProc(  HWND hwnd,  UINT uMsg,  
									   WPARAM wParam, LPARAM lParam ) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. But we can get a pseudo-this from the parameter to 
	// WM_INITDIALOG, which we therafter store with the window and retrieve
	// as follows:
	AuthDialog *_this = (AuthDialog *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (uMsg) {

	case WM_INITDIALOG:
		{
			SetWindowLong(hwnd, GWL_USERDATA, lParam);
			_this = (AuthDialog *) lParam;
			CentreWindow(hwnd);
			SetForegroundWindow(hwnd);
			return TRUE;
		}
	case WM_COMMAND:
		switch (LOWORD(wParam)) {
		case IDOK:
			{
				UINT res= GetDlgItemText( hwnd,  IDC_PASSWD_EDIT,
					_this->m_passwd, 256);
				res= GetDlgItemText( hwnd,  IDD_DOMAIN,
					_this->m_domain, 256);
				res= GetDlgItemText( hwnd,  IDD_USER_NAME,
					_this->m_user, 256);
				
				EndDialog(hwnd, TRUE);

				return TRUE;
			}
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

BOOL CALLBACK AuthDialog::DlgProc1(  HWND hwnd,  UINT uMsg,  
									   WPARAM wParam, LPARAM lParam ) {
	// This is a static method, so we don't know which instantiation we're 
	// dealing with. But we can get a pseudo-this from the parameter to 
	// WM_INITDIALOG, which we therafter store with the window and retrieve
	// as follows:
	AuthDialog *_this = (AuthDialog *) GetWindowLong(hwnd, GWL_USERDATA);

	switch (uMsg) {

	case WM_INITDIALOG:
		{
			SetWindowLong(hwnd, GWL_USERDATA, lParam);
			_this = (AuthDialog *) lParam;
			CentreWindow(hwnd);
			return TRUE;
		}
	case WM_COMMAND:
		switch (LOWORD(wParam)) {
		case IDOK:
			{
				UINT res= GetDlgItemText( hwnd,  IDC_PASSWD_EDIT,
					_this->m_passwd, 256);
				EndDialog(hwnd, TRUE);

				return TRUE;
			}
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

