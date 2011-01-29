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
// RRE (Rising Rectangle Encoding)
//
// The bits of the ClientConnection object to do with RRE.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"

void ClientConnection::ReadRRERect(rfbFramebufferUpdateRectHeader *pfburh)
{
	// An RRE rect is always followed by a background color
	// For speed's sake we read them together into a buffer.
	char tmpbuf[sz_rfbRREHeader+4];			// biggest pixel is 4 bytes long
    rfbRREHeader *prreh = (rfbRREHeader *) tmpbuf;
	CARD8 *pcolor = (CARD8 *) tmpbuf + sz_rfbRREHeader;
	ReadExact(tmpbuf, sz_rfbRREHeader + m_minPixelBytes);

	prreh->nSubrects = Swap32IfLE(prreh->nSubrects);
	
	SETUP_COLOR_SHORTCUTS;
    COLORREF color;
    switch (m_myFormat.bitsPerPixel) {
        case 8:
            color = COLOR_FROM_PIXEL8_ADDRESS(pcolor); break;
        case 16:
			color = COLOR_FROM_PIXEL16_ADDRESS(pcolor); break;
        case 24:
        case 32:
            color = COLOR_FROM_PIXEL32_ADDRESS(pcolor); break;
    }

	// No other threads can use bitmap DC
	omni_mutex_lock l(m_bitmapdcMutex);
	ObjectSelector b(m_hBitmapDC, m_hBitmap);
	PaletteSelector ps(m_hBitmapDC, m_hPalette);
		
	// Draw the background of the rectangle
	FillSolidRect(pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h, color);
	
    if (prreh->nSubrects == 0) return;
	
	// Draw the sub-rectangles
    rfbRectangle rect, *pRect;
	// The size of an RRE subrect including color info
	int subRectSize = m_minPixelBytes + sz_rfbRectangle;
    
	// Read subrects into the buffer 
	CheckBufferSize(subRectSize * prreh->nSubrects);
    ReadExact(m_netbuf, subRectSize * prreh->nSubrects);
	BYTE *p = (BYTE *) m_netbuf;
	for (CARD32 i = 0; i < prreh->nSubrects; i++) {
		pRect = (rfbRectangle *) (p + m_minPixelBytes);
		
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			color = COLOR_FROM_PIXEL8_ADDRESS(p); break;
		case 16:
			color = COLOR_FROM_PIXEL16_ADDRESS(p); break;
		case 32:
			color = COLOR_FROM_PIXEL32_ADDRESS(p); break;
		};
		
		rect.x = (CARD16) (Swap16IfLE(pRect->x) + pfburh->r.x);
		rect.y = (CARD16) (Swap16IfLE(pRect->y) + pfburh->r.y);
		rect.w = Swap16IfLE(pRect->w);
		rect.h = Swap16IfLE(pRect->h);
		
		FillSolidRect(rect.x, rect.y, rect.w, rect.h, color);
		p+=subRectSize;
	}
}
