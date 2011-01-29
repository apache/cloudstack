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
#include "AccelKeys.h"

//
// Build the Accelerators Table
// 
AccelKeys::AccelKeys()
{
	ACCEL AccelTable[16];
	int i = 0;
	m_hWnd = 0;

	/*
	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F3;
	AccelTable[i++].cmd = ID_CONN_CTLESC;
	*/

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F3;
	AccelTable[i++].cmd = ID_VIEWONLYTOGGLE;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F4;
	AccelTable[i++].cmd = ID_CONN_CTLALTDEL;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F5;
	AccelTable[i++].cmd = ID_CONN_SAVE_AS;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F6;
	AccelTable[i++].cmd = IDC_OPTIONBUTTON;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F7;
	AccelTable[i++].cmd = ID_FILETRANSFER;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F8;
	AccelTable[i++].cmd = ID_TEXTCHAT;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F9;
	AccelTable[i++].cmd = ID_DBUTTON;  // Toolbar Toggle

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F10;
	AccelTable[i++].cmd = ID_AUTOSCALING;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FNOINVERT;
	AccelTable[i].key = VK_F11;
	AccelTable[i++].cmd = ID_NORMALSCREEN;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F11;
	AccelTable[i++].cmd = ID_HALFSCREEN;

	AccelTable[i].fVirt = FVIRTKEY | FCONTROL | FALT | FNOINVERT;
	AccelTable[i].key = VK_F12;
	AccelTable[i++].cmd = ID_FULLSCREEN;

	m_hAccelTable = CreateAcceleratorTable((LPACCEL)AccelTable, i);
}

//
//
//
bool AccelKeys::TranslateAccelKeys(MSG *pMsg)
{
	if (m_hWnd == 0)
		return false;
	else
		return (TranslateAccelerator(m_hWnd, m_hAccelTable, pMsg) != 0);
}


//
// 
//
AccelKeys::~AccelKeys()
{
	DestroyAcceleratorTable(m_hAccelTable);
}
