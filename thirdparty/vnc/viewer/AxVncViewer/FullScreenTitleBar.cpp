/////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2004 Ultr@VNC Team Members. All Rights Reserved.
//	Copyright (C) 2003-2004 Lars Werner. All Rights reserved
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

//===========================================================================
//	FullScreen Titlebar
//	2004 - All rights reservered
//===========================================================================
//
//	Project/Product :	FullScreenTitlebar
//  FileName		:	FullScreenTitleBar.cpp
//	Author(s)		:	Lars Werner
//  Homepage		:	http://lars.werner.no
//
//	Description		:	Creates a titlebar window used on fullscreen apps.
//                  
//	Classes			:	CTitleBar
//
//	History
//	Vers.  Date      Aut.  Type     Description
//  -----  --------  ----  -------  -----------------------------------------
//	1.00   20 01 04  LW    Create   Original
//	1.01   03 02 80  LW    Updated  Added contextmenus and restore feature
//===========================================================================

#include "stdhdrs.h"
#include "res\\resource.h"
#include "FullScreenTitleBar.h"
#include "log.h"
extern Log vnclog;

//***************************************************************************************

CTitleBar *TitleBarThis=NULL;

//***************************************************************************************

CTitleBar::CTitleBar()
{
	hInstance=NULL;
	Parent=NULL;
	this->Init();
}

CTitleBar::CTitleBar(HINSTANCE hInst, HWND ParentWindow)
{
	hInstance=hInst;
	Parent=ParentWindow;
	this->Init();
}

//***************************************************************************************

CTitleBar::~CTitleBar()
{
	DeleteObject(Font);
	DestroyWindow(m_hWnd);
}

//***************************************************************************************

void CTitleBar::Init()
{
	SlideDown=TRUE; //Slide down at startup
	AutoHide=tbPinNotPushedIn; //sf@2004 - This way, the toolbar is briefly shown then hidden
								// (and doesn't overlap the toobar)
	IntAutoHideCounter=0;
	HideAfterSlide=FALSE;

	//Create font
    HDC hdc;
    long lfHeight;
    
    hdc = GetDC(NULL);
    lfHeight = -MulDiv(10, GetDeviceCaps(hdc, LOGPIXELSY), 72);
    ReleaseDC(NULL, hdc);

	Font=CreateFont(lfHeight, 0, 0, 0, 0, FALSE, 0, 0, 0, 0, 0, 0, 0, "Arial");

	Text=""; //No text at startup...

	m_hWnd=NULL;

	if(Parent!=NULL&&hInstance!=NULL)
	{
		TitleBarThis=this;
		this->CreateDisplay();
	}
}

//***************************************************************************************

void CTitleBar::Create(HINSTANCE hInst, HWND ParentWindow)
{
	hInstance=hInst;
	Parent=ParentWindow;
	this->Init();
}

//***************************************************************************************

void CTitleBar::CreateDisplay()
{
	//Consts are used to select margins
	//GetParent size and size after that!
	RECT lpRect;	
	::GetWindowRect(::GetDesktopWindow(), &lpRect);

	// Create the window
	WNDCLASS wndclass;

	wndclass.style			= CS_DBLCLKS;
	wndclass.lpfnWndProc	= CTitleBar::WndProc;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= hInstance;
	wndclass.hIcon=NULL;
	wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
	wndclass.hbrBackground	= (HBRUSH) GetStockObject(WHITE_BRUSH);
    wndclass.lpszMenuName	= (const TCHAR *) NULL;
	wndclass.lpszClassName	= _T("FSTITLEBAR");

	RegisterClass(&wndclass);

	//Create window without any titlbar
	DWORD winstyle = WS_POPUP | WS_SYSMENU ;

	int CenterX=(lpRect.right-lpRect.left)/2-tbWidth/2;
	int HeightPlacement=-tbHeigth+1;

	if(tbScrollWindow==FALSE)
		HeightPlacement=0;

	m_hWnd = CreateWindow(_T("FSTITLEBAR"),
			      /*_T("Titlebar")*/NULL,
			      winstyle,
			      CenterX,
			      HeightPlacement,
			      tbWidth,       // x-size
			      tbHeigth,       // y-size
			      Parent,                // Parent handle
			      NULL,                // Menu handle
			      hInstance,
			      NULL);

	//Set region to window so it is non rectangular
	HRGN Range;
	POINT Points[4];
	Points[0].x=0;
	Points[0].y=0;
	Points[1].x=tbTriangularPoint;
	Points[1].y=tbHeigth;
	Points[2].x=tbWidth-tbTriangularPoint;
	Points[2].y=tbHeigth;
	Points[3].x=tbWidth;
	Points[3].y=0;
	Range=::CreatePolygonRgn(Points, 4, ALTERNATE );

	::SetWindowRgn(m_hWnd, Range, TRUE);

	//Close button
	HWND Close=CreateWindow("STATIC",
				"Close",
				WS_CHILD | WS_VISIBLE | SS_NOTIFY | SS_OWNERDRAW,
                tbWidth-tbRightSpace-tbcxPicture, tbTopSpace, tbcxPicture, tbcyPicture, m_hWnd,
				(HMENU)tbIDC_CLOSE,
                hInstance,
				NULL);

	//Maximize button
	HWND Maximize=CreateWindow("STATIC",
				"Maximize",
				WS_CHILD | WS_VISIBLE | SS_NOTIFY | SS_OWNERDRAW,
                tbWidth-tbRightSpace-(tbcxPicture*2)-(tbButtonSpace*1), tbTopSpace, tbcxPicture, tbcyPicture, m_hWnd,
				(HMENU)tbIDC_MAXIMIZE,
                hInstance,
				NULL);
	
	//Minimize button
	HWND Minimize=CreateWindow("STATIC",
				"Minimize",
				WS_CHILD | WS_VISIBLE | SS_NOTIFY | SS_OWNERDRAW,
                tbWidth-tbRightSpace-(tbcxPicture*3)-(tbButtonSpace*2), tbTopSpace, tbcxPicture, tbcyPicture, m_hWnd,
				(HMENU)tbIDC_MINIMIZE,
                hInstance,
				NULL);

	//Pin button
	Pin=CreateWindow("STATIC",
				"Pin",
				WS_CHILD | WS_VISIBLE | SS_NOTIFY | SS_OWNERDRAW,
                tbLeftSpace, tbTopSpace, tbcxPicture, tbcyPicture, m_hWnd,
				(HMENU)tbIDC_PIN,
                hInstance,
				NULL);

	//Set the creation of the window
	SetWindowLong(m_hWnd, GWL_USERDATA, (LONG) this);

	//Load pictures
	this->LoadPictures();

	//Show window and start animation
	if(tbHideAtStartup==FALSE)
		ShowWindow(m_hWnd, SW_SHOW);
	if(tbScrollWindow==TRUE)
		SetTimer(m_hWnd, tbScrollTimerID, tbScrollDelay, NULL);
	if(AutoHide==TRUE)
		SetTimer(m_hWnd, tbAutoScrollTimer, tbAutoScrollDelay, NULL);
}

//***************************************************************************************

LRESULT CALLBACK CTitleBar::WndProc(HWND hwnd, UINT iMsg, 
					   WPARAM wParam, LPARAM lParam)
{
	switch (iMsg)
	{

	case WM_CREATE:
		return 0;

	case WM_PAINT:
			TitleBarThis->Draw();
			return 0;

	case WM_CLOSE:
		{
			HWND Window=TitleBarThis->GetSafeHwnd();
			TitleBarThis->FreePictures();
			DestroyWindow(Window);
//			vnclog.Print(0,_T(" Q6 \n"));
//			PostQuitMessage(0);
			return 0;
		}

	case WM_DESTROY:
			TitleBarThis->FreePictures();
//			vnclog.Print(0,_T(" Q7 \n"));
//			PostQuitMessage(0);
			return 0;

	case WM_DRAWITEM:
		{
			HDC hdcMem; 
			LPDRAWITEMSTRUCT lpdis; 

            lpdis = (LPDRAWITEMSTRUCT) lParam; 
            hdcMem = CreateCompatibleDC(lpdis->hDC); 
 
			if(lpdis->CtlID==tbIDC_CLOSE)
					SelectObject(hdcMem, TitleBarThis->hClose); 
			if(lpdis->CtlID==tbIDC_MAXIMIZE)
					SelectObject(hdcMem, TitleBarThis->hMaximize); 
			if(lpdis->CtlID==tbIDC_MINIMIZE)
					SelectObject(hdcMem, TitleBarThis->hMinimize); 
			
			if(lpdis->CtlID==tbIDC_PIN)
			{
				if(TitleBarThis->AutoHide==TRUE)
					SelectObject(hdcMem, TitleBarThis->hPinUp); 
				else
					SelectObject(hdcMem, TitleBarThis->hPinDown); 
			}

			BitBlt(lpdis->hDC,
					lpdis->rcItem.left,
					lpdis->rcItem.top,
	                lpdis->rcItem.right - lpdis->rcItem.left, 
					lpdis->rcItem.bottom - lpdis->rcItem.top, 
	                hdcMem,
					0,
					0,
					SRCCOPY);
 
            DeleteDC(hdcMem); 
            return TRUE; 
		}

	case WM_COMMAND: 
		if (HIWORD(wParam) == BN_CLICKED)
		{
			//Handle the Pin for holding the window
			if(LOWORD(wParam) == tbIDC_PIN)
			{
				if(TitleBarThis->AutoHide==TRUE)
				{
					TitleBarThis->AutoHide=FALSE;
					TitleBarThis->DisplayWindow(TRUE);
				}
				else
				{
					TitleBarThis->AutoHide=TRUE;
					TitleBarThis->DisplayWindow(FALSE);
				}

				//Redraw window to show the new gfx...
				::RedrawWindow(TitleBarThis->Pin, NULL, NULL, TRUE);
			}

			//If default = true we'll send usally showwindow and close messages
			if(tbDefault==TRUE)
			{
				if(LOWORD(wParam) == tbIDC_CLOSE)
					::SendMessage(TitleBarThis->Parent, WM_CLOSE, NULL, NULL);
				if(LOWORD(wParam) == tbIDC_MAXIMIZE)
				{
					//if(::IsZoomed(TitleBarThis->Parent)==TRUE)
						ShowWindow(TitleBarThis->Parent, SW_RESTORE);
					/*else
						ShowWindow(TitleBarThis->Parent, SW_MAXIMIZE);*/
				}
				if(LOWORD(wParam) == tbIDC_MINIMIZE)
					ShowWindow(TitleBarThis->Parent, SW_MINIMIZE);
			}
			else //default = false - send custom message on buttons
			{
				if(LOWORD(wParam) == tbIDC_CLOSE)
					::SendMessage(TitleBarThis->Parent, tbWM_CLOSE, NULL, NULL);
				if(LOWORD(wParam) == tbIDC_MAXIMIZE)
					::SendMessage(TitleBarThis->Parent, tbWM_MAXIMIZE, NULL, NULL);
				if(LOWORD(wParam) == tbIDC_MINIMIZE)
					::SendMessage(TitleBarThis->Parent, tbWM_MINIMIZE, NULL, NULL);
			}
        }

		//Menu part starts here
		{
			UINT IDNum=LOWORD(wParam);
		
			if(IDNum>=tbWMCOMMANDIDStart&&IDNum<tbWMCOMMANDIDEnd) //The ID is in range for a menuclick
			{
				UINT Num=IDNum-tbWMCOMMANDIDStart;

				//When the close,minimize, maximize is not present just send! :)
				if(tbLastIsStandard==FALSE)
					::SendMessage(TitleBarThis->Parent, WM_USER+tbWMUSERID+Num, NULL,NULL);
				else //Handle close, minimize and maximize
				{
					HMENU Menu=LoadMenu(TitleBarThis->hInstance,MAKEINTRESOURCE (tbMENUID));
					HMENU SubMenu=GetSubMenu(Menu,0);;

					UINT Total=0;

					//Get the real number of entries (exluding seperators)
					for(int i=0;i<GetMenuItemCount(SubMenu);i++)
					{
						int res=::GetMenuString(SubMenu, i, NULL, 0, MF_BYPOSITION);
						if(res!=0)
							Total++;
					}

					if(Num==Total-1) //Close button
						::SendMessage(TitleBarThis->m_hWnd,WM_COMMAND,MAKEWPARAM(tbIDC_CLOSE,BN_CLICKED),NULL);
					else if(Num==Total-2) //Minimize button
						::SendMessage(TitleBarThis->m_hWnd,WM_COMMAND,MAKEWPARAM(tbIDC_MINIMIZE,BN_CLICKED),NULL);
					else if(Num==Total-3) //Maximize button
						::SendMessage(TitleBarThis->m_hWnd,WM_COMMAND,MAKEWPARAM(tbIDC_MAXIMIZE,BN_CLICKED),NULL);
					else
						::SendMessage(TitleBarThis->Parent, WM_USER+tbWMUSERID+Num, NULL,NULL);

					DestroyMenu (SubMenu);
					DestroyMenu (Menu);
				}
			}
		}

        break;
	
	case WM_MOUSEMOVE:
			if(TitleBarThis->HideAfterSlide==FALSE)
			{
				TitleBarThis->SlideDown=TRUE;
				::SetTimer(TitleBarThis->m_hWnd, tbScrollTimerID, 20, NULL);
			}
		break;

	case WM_LBUTTONDBLCLK:
			//If the default entries on the context menu is activated then doubleclick is restore :)
			if(tbLastIsStandard==TRUE)
				::SendMessage(TitleBarThis->m_hWnd,WM_COMMAND,MAKEWPARAM(tbIDC_MAXIMIZE,BN_CLICKED),NULL);
		break;

	case WM_RBUTTONDOWN:
		{
			HMENU Menu=LoadMenu(TitleBarThis->hInstance,MAKEINTRESOURCE (tbMENUID));
			HMENU SubMenu=GetSubMenu(Menu,0);;

			POINT  lpPoint;
			::GetCursorPos(&lpPoint);

			int Pos=0;

			//Set ID values to each item
			for(int i=0;i<GetMenuItemCount(SubMenu);i++)
			{
				TCHAR Text[MAX_PATH];
				ZeroMemory(Text,sizeof(LPTSTR));
				int res=::GetMenuString(SubMenu, i, Text, MAX_PATH, MF_BYPOSITION);
				
				if(res!=0)
				{
					::ModifyMenu(SubMenu,i,MF_BYPOSITION, tbWMCOMMANDIDStart+Pos,Text);
					Pos++;
				}
			}

			//Loop through each item from pos to set the default value on restore
			if(tbLastIsStandard==TRUE)
			{
				int RealPos=0;
				for(int i=0;i<GetMenuItemCount(SubMenu);i++)
				{
					TCHAR Text[MAX_PATH];
					ZeroMemory(Text,sizeof(LPTSTR));
					int res=::GetMenuString(SubMenu, i, Text, MAX_PATH, MF_BYPOSITION);
					
					if(res!=0)
					{
						RealPos++;

						if(RealPos==Pos-2)
						::SetMenuDefaultItem(SubMenu, i, TRUE);
					}
				}
			}

			TrackPopupMenu(SubMenu,TPM_LEFTALIGN, lpPoint.x, lpPoint.y, 0, TitleBarThis->m_hWnd, NULL);

			SetForegroundWindow (TitleBarThis->m_hWnd);
			DestroyMenu (SubMenu);
			DestroyMenu (Menu);

			break;
		}

	case WM_TIMER:
		{
			UINT TimerID=(UINT)wParam;
			
			if(TimerID==tbScrollTimerID)
			{
				RECT lpRect;
				::GetWindowRect(TitleBarThis->m_hWnd, &lpRect);

				if( ((lpRect.top==0)&&(TitleBarThis->SlideDown==TRUE))
					||
					((lpRect.top==-tbHeigth+1)&&(TitleBarThis->SlideDown==FALSE)))
				{
					KillTimer(TitleBarThis->m_hWnd, tbScrollTimerID);

					if(TitleBarThis->HideAfterSlide==TRUE)
					{
						TitleBarThis->HideAfterSlide=FALSE;
						ShowWindow(TitleBarThis->GetSafeHwnd(), SW_HIDE);
					}
					return 0;
				}

				if(TitleBarThis->SlideDown==TRUE)
				{
					lpRect.top++; lpRect.bottom++;
				}
				else
				{
					lpRect.top--; lpRect.bottom--;
				}

				::MoveWindow(TitleBarThis->m_hWnd, lpRect.left, lpRect.top, lpRect.right-lpRect.left, lpRect.bottom-lpRect.top, TRUE);
			}

			//Check mouse cordinates and hide if the mouse haven't been in the window for a few seconds
			if(TimerID==tbAutoScrollTimer)
			{
				RECT lpRect;
				POINT pt;
				::GetWindowRect(TitleBarThis->m_hWnd, &lpRect);
				::GetCursorPos(&pt);

				if(PtInRect(&lpRect, pt)==FALSE) 
				{
					TitleBarThis->IntAutoHideCounter++;

					if(TitleBarThis->IntAutoHideCounter==tbAutoScrollTime)
					{
						TitleBarThis->SlideDown=FALSE;
						::SetTimer(TitleBarThis->m_hWnd, tbScrollTimerID, tbScrollDelay, NULL);
					}
				}
				else
				{
					TitleBarThis->IntAutoHideCounter=0;
				}
			}

			break;
		}
	}//Case - end
	
	return DefWindowProc(hwnd, iMsg, wParam, lParam);
}

//***************************************************************************************

void CTitleBar::LoadPictures()
{
	hClose=LoadBitmap(hInstance, MAKEINTRESOURCE(IDB_CLOSE));
	hMaximize=LoadBitmap(hInstance, MAKEINTRESOURCE(IDB_MAXIMIZE));
	hMinimize=LoadBitmap(hInstance, MAKEINTRESOURCE(IDB_MINIMIZE));
	hPinUp=LoadBitmap(hInstance, MAKEINTRESOURCE(IDB_PINUP));
	hPinDown=LoadBitmap(hInstance, MAKEINTRESOURCE(IDB_PINDOWN));
}


void CTitleBar::FreePictures()
{
	DeleteObject(hClose);
	DeleteObject(hMaximize);
	DeleteObject(hMinimize);
	DeleteObject(hPinUp);
	DeleteObject(hPinDown);
}

//***************************************************************************************

void CTitleBar::Draw()
{
	PAINTSTRUCT ps;
	HDC hdc = BeginPaint(m_hWnd, &ps);

	int r1 = GetRValue(tbStartColor);
	int g1 = GetGValue(tbStartColor);
	int b1 = GetBValue(tbStartColor);
	int r2 = GetRValue(tbEndColor);
	int g2 = GetGValue(tbEndColor);
	int b2 = GetBValue(tbEndColor);

	//2 different styles of gradient is available... :)
	if(tbGradientWay==TRUE)
	{
		for ( int x = 0; x<tbWidth; x++)
		{ 
			RECT Rect;
			Rect.left=x;
			Rect.top=0;
			Rect.right=x+1;
			Rect.bottom=tbHeigth;
			HBRUSH Brush=CreateSolidBrush(RGB(r1 * (tbWidth-x)/tbWidth + r2 * x/tbWidth, 
				g1 * (tbWidth-x)/tbWidth + g2 * x/tbWidth, b1 * (tbWidth-x)/tbWidth + b2 * x/tbWidth));

			::FillRect(hdc, &Rect, Brush);
			DeleteObject(Brush);
		}
	}
	else
	{
		for ( int y = 0; y<tbHeigth; y++)
		{ 
			RECT Rect;
			Rect.left=0;
			Rect.top=y;
			Rect.right=tbWidth;
			Rect.bottom=y+1;
			
			HBRUSH Brush=CreateSolidBrush(RGB(r1 * (tbHeigth-y)/tbHeigth + r2 * y/tbHeigth, 
				g1 * (tbHeigth-y)/tbHeigth + g2 * y/tbHeigth, b1 * (tbHeigth-y)/tbHeigth + b2 * y/tbHeigth));

			::FillRect(hdc, &Rect, Brush);
			DeleteObject(Brush);
		}
	}

	//Draw border around window
	HPEN Border=::CreatePen(PS_SOLID, tbBorderWidth, tbBorderPenColor);
	::SelectObject(hdc, Border);

	//Draw border around window
	::MoveToEx(hdc, 0,0, NULL);
	::LineTo(hdc, tbTriangularPoint, tbHeigth);
	::LineTo(hdc, tbWidth-tbTriangularPoint, tbHeigth);
	::LineTo(hdc, tbWidth, 0);
	::LineTo(hdc, 0,0);

	//Draw extra shadow at bottom
	DeleteObject(Border);
	Border=::CreatePen(PS_SOLID, tbBorderWidth, tbBorderPenShadow);
	::SelectObject(hdc, Border);
	::MoveToEx(hdc, tbTriangularPoint+1,tbHeigth-1, NULL);
	::LineTo(hdc, tbWidth-tbTriangularPoint-1, tbHeigth-1);

	//Create rect for drawin the text
	RECT lpRect;
	lpRect.left=tbLeftSpace+tbcxPicture+tbButtonSpace;
	lpRect.top=tbBorderWidth;
	lpRect.right=tbWidth-tbRightSpace-(tbcxPicture*3)-(tbButtonSpace*3);
	lpRect.bottom=tbHeigth-tbBorderWidth;
	
	//Draw text
	::SelectObject(hdc, Font);
	::SetBkMode(hdc, TRANSPARENT);
	::SetTextColor(hdc, tbTextColor);
	::DrawText(hdc, Text,-1,&lpRect, DT_CENTER|DT_SINGLELINE|DT_VCENTER);

	EndPaint(m_hWnd, &ps);
}

//***************************************************************************************

void CTitleBar::SetText(LPTSTR TextOut)
{
	Text=TextOut;
}

//***************************************************************************************

void CTitleBar::DisplayWindow(BOOL Show, BOOL SetHideFlag)
{
	IntAutoHideCounter=0;

	if(Show==TRUE)
	{
		if(tbScrollWindow==TRUE)
		{
			if(SetHideFlag==TRUE)
			{
				HideAfterSlide=FALSE;
				SlideDown=TRUE;
			}
			ShowWindow(m_hWnd, SW_SHOW);
			SetTimer(m_hWnd, tbScrollTimerID, tbScrollDelay, NULL);
		}
		else
			ShowWindow(m_hWnd, SW_SHOW);

		if(AutoHide==TRUE)
			SetTimer(m_hWnd, tbAutoScrollTimer, tbAutoScrollDelay, NULL);
		else
			KillTimer(m_hWnd, tbAutoScrollTimer);
	}
	else
	{
		if(tbScrollWindow==TRUE)
		{
			if(SetHideFlag==TRUE)
			{
				HideAfterSlide=TRUE;
				SlideDown=FALSE;
			}
			SetTimer(m_hWnd, tbScrollTimerID, tbScrollDelay, NULL);
		}
		else
			ShowWindow(m_hWnd, SW_HIDE);

		if(AutoHide==TRUE)
			SetTimer(m_hWnd, tbAutoScrollTimer, tbAutoScrollDelay, NULL);
		else
			KillTimer(m_hWnd, tbAutoScrollTimer);
	}
}

//***************************************************************************************
