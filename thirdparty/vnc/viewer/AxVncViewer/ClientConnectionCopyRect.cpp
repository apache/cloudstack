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
// whence you received this file, check http://www.uk.research.att.com/vnc or 
// contact the authors on vnc@uk.research.att.com for information on obtaining it.
//
// CopyRect Encoding
//
// The bits of the ClientConnection object to do with CopyRect.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"

void ClientConnection::ReadCopyRect(rfbFramebufferUpdateRectHeader *pfburh) {
	rfbCopyRect cr;
	ReadExact((char *) &cr, sz_rfbCopyRect);
	cr.srcX = Swap16IfLE(cr.srcX); 
	cr.srcY = Swap16IfLE(cr.srcY);
	// By sure the rect is insite the border memcopy does not like it 
	if (!Check_Rectangle_borders(pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)) return;
	if (!Check_Rectangle_borders(cr.srcX, cr.srcY, pfburh->r.w, pfburh->r.h)) return;

	// tight If *Cursor encoding is used, we should extend our "cursor lock area"
	// (previously set to destination rectangle) to the source rect as well.
	SoftCursorLockArea(cr.srcX, cr.srcY, pfburh->r.w, pfburh->r.h);

	if (UltraFast && m_DIBbits)
		{
		omni_mutex_lock l(m_bitmapdcMutex);
		int bytesPerInputRow = m_si.framebufferWidth * m_myFormat.bitsPerPixel/8;
		int bytesPerOutputRow = pfburh->r.w * m_myFormat.bitsPerPixel/8;
		int OutputHeight=pfburh->r.h;
		BYTE *sourcepos,*iptr,*destpos,*optr;
		if (cr.srcY<=pfburh->r.y)
		{
			{
				destpos = (BYTE*)m_DIBbits + (bytesPerInputRow * (pfburh->r.y+pfburh->r.h-1))+(pfburh->r.x * m_myFormat.bitsPerPixel/8);
			sourcepos = (BYTE*)m_DIBbits + (bytesPerInputRow * (cr.srcY+pfburh->r.h-1))+((cr.srcX) * m_myFormat.bitsPerPixel/8);
				iptr=sourcepos;
				optr=destpos;
				while (OutputHeight > 0) {
					memcpy(optr, iptr, bytesPerOutputRow);			
					iptr -= bytesPerInputRow;
					optr -= bytesPerInputRow;
					OutputHeight--;
				}
			}
		}
		else if (cr.srcY>pfburh->r.y)
		{
			{
				destpos = (BYTE*)m_DIBbits + (bytesPerInputRow * pfburh->r.y)+(pfburh->r.x * m_myFormat.bitsPerPixel/8);
				sourcepos = (BYTE*)m_DIBbits + (bytesPerInputRow * (cr.srcY))+((cr.srcX) * m_myFormat.bitsPerPixel/8);
				iptr=sourcepos;
				optr=destpos;
				while (OutputHeight > 0) {
					memcpy(optr, iptr, bytesPerOutputRow);				
					iptr += bytesPerInputRow;
					optr += bytesPerInputRow;
					OutputHeight--;
				}
			}
		}
	}
	else
	{
		omni_mutex_lock l(m_bitmapdcMutex);									  
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  
	
		if (!BitBlt(
			m_hBitmapDC,pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h,
			m_hBitmapDC, cr.srcX, cr.srcY, SRCCOPY)) {
			vnclog.Print(0, _T("Error in blit in ClientConnection::CopyRect\n"));
		}
	}


}
