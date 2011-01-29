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



#include "stdhdrs.h"
#include "vncviewer.h"

HBITMAP
    DoGetBkGndBitmap(IN CONST UINT uBmpResId )
    {
        static HBITMAP hbmBkGnd = NULL;
        if (NULL == hbmBkGnd)
        {
			/*char szFileName[MAX_PATH];
			if (GetModuleFileName(NULL, szFileName, MAX_PATH))
				{
				char* p = strrchr(szFileName, '\\');
					if (p == NULL) return false;
					*p = '\0';
				strcat (szFileName,"\\background2.bmp");
			}
			hbmBkGnd = (HBITMAP)LoadImage( NULL, szFileName, IMAGE_BITMAP, 0, 0,
               LR_CREATEDIBSECTION | LR_DEFAULTSIZE | LR_LOADFROMFILE );*/
			hbmBkGnd = (HBITMAP)LoadImage(
                GetModuleHandle(NULL), MAKEINTRESOURCE(uBmpResId),
                    IMAGE_BITMAP, 0, 0, LR_CREATEDIBSECTION);


            if (NULL == hbmBkGnd)
                hbmBkGnd = (HBITMAP)-1;
        }
        return (hbmBkGnd == (HBITMAP)-1)
            ? NULL : hbmBkGnd;
    }
BOOL
    DoSDKEraseBkGnd(
        IN CONST HDC hDC,
        IN CONST COLORREF crBkGndFill
      )
    {
        HBITMAP hbmBkGnd = DoGetBkGndBitmap(IDB_BITMAP10);
        if (hDC && hbmBkGnd)
        {
            RECT rc;
            if ((ERROR != GetClipBox(hDC, &rc)) && !IsRectEmpty(&rc))
            {
                HDC hdcMem = CreateCompatibleDC(hDC);
                if (hdcMem)
                {
                    HBRUSH hbrBkGnd = CreateSolidBrush(crBkGndFill);
                    if (hbrBkGnd)
                    {
                        HGDIOBJ hbrOld = SelectObject(hDC, hbrBkGnd);
                        if (hbrOld)
                        {
                            SIZE size = {
                                (rc.right-rc.left), (rc.bottom-rc.top)
                            };

                            if (PatBlt(hDC, rc.left, rc.top, size.cx, size.cy, PATCOPY))
                            {
                                HGDIOBJ hbmOld = SelectObject(hdcMem, hbmBkGnd);
                                if (hbmOld)
                                {
                                    BitBlt(hDC, rc.left, rc.top, size.cx, size.cy,
                                        hdcMem, rc.left, rc.top, SRCCOPY);
                                    SelectObject(hdcMem, hbmOld);
                                }
                            }
                            SelectObject(hDC, hbrOld);
                        }
                        DeleteObject(hbrBkGnd);
                    }
                    DeleteDC(hdcMem);
                }
            }
        }
        return TRUE;
    }

// Process the About dialog.
static LRESULT CALLBACK AboutDlgProc(HWND hwnd, UINT iMsg, 
										   WPARAM wParam, LPARAM lParam) {
	switch (iMsg) {
	case WM_INITDIALOG:
		{
			CentreWindow(hwnd);
			SetForegroundWindow(hwnd);
            extern char buildtime[];
            SetDlgItemText(hwnd, IDC_BUILDTIME, buildtime);
			return TRUE;
		}
	case WM_CLOSE:
		EndDialog(hwnd, TRUE);
		return TRUE;
	case WM_COMMAND:
		if (LOWORD(wParam) == IDOK) {
			EndDialog(hwnd, TRUE);
		}
	/*case WM_ERASEBKGND:
            {
               DoSDKEraseBkGnd((HDC)wParam, RGB(255,0,0));
				return true;
            }
	case WM_CTLCOLORSTATIC:
			{
				SetBkMode((HDC) wParam, TRANSPARENT);
				return (DWORD) GetStockObject(NULL_BRUSH);
			}*/
	}
	return FALSE;
}

#ifndef _ULTRAVNCAX_
void ShowAboutBox()
#else
void ShowAboutBox ( HWND parent /*= NULL*/ )
#endif
{
#ifndef _ULTRAVNCAX_
	int res = DialogBox(pApp->m_instance, 
 		DIALOG_MAKEINTRESOURCE(IDD_APP_ABOUT),
		NULL, (DLGPROC) AboutDlgProc);
#else
	int res = DialogBox(pApp->m_instance, 
 		DIALOG_MAKEINTRESOURCE(IDD_APP_ABOUT),
		parent, (DLGPROC) AboutDlgProc);
#endif
}
	
