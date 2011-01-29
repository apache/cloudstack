//  Copyright (C) 2000 Tridia Corporation. All Rights Reserved.
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
// TightVNC distribution homepage on the Web: http://www.tightvnc.com/
//
// If the source code for the VNC system is not available from the place 
// whence you received this file, check http://www.uk.research.att.com/vnc or contact
// the authors on vnc@uk.research.att.com for information on obtaining it.

// ZLIB Encoding
//
// The bits of the ClientConnection object to do with zlib.
#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"
#include "lzo/minilzo.h"

void ClientConnection::ReadUltraRect(rfbFramebufferUpdateRectHeader *pfburh) {

	UINT numpixels = pfburh->r.w * pfburh->r.h;
    // this assumes at least one byte per pixel. Naughty.
	UINT numRawBytes = numpixels * m_minPixelBytes;
	UINT numCompBytes;
	UINT new_len;

	rfbZlibHeader hdr;

	// Read in the rfbZlibHeader
	omni_mutex_lock l(m_bitmapdcMutex);
	ReadExact((char *)&hdr, sz_rfbZlibHeader);

	numCompBytes = Swap32IfLE(hdr.nBytes);

	// Read in the compressed data
    CheckBufferSize(numCompBytes);
	ReadExact(m_netbuf, numCompBytes);
	CheckZlibBufferSize(numRawBytes);
	lzo1x_decompress((BYTE*)m_netbuf,numCompBytes,(BYTE*)m_zlibbuf,&new_len,NULL);
	SoftCursorLockArea(pfburh->r.x, pfburh->r.y,pfburh->r.w,pfburh->r.h);
	if (!Check_Rectangle_borders(pfburh->r.x, pfburh->r.y,pfburh->r.w,pfburh->r.h)) return;
	if (m_DIBbits) ConvertAll(pfburh->r.w,pfburh->r.h,pfburh->r.x, pfburh->r.y,m_myFormat.bitsPerPixel/8,(BYTE *)m_zlibbuf,(BYTE *)m_DIBbits,m_si.framebufferWidth);

}

void ClientConnection::ReadUltraZip(rfbFramebufferUpdateRectHeader *pfburh,HRGN *prgn)
{
	UINT nNbCacheRects = pfburh->r.x;
	UINT numRawBytes = pfburh->r.y+pfburh->r.w*65535;
	UINT numCompBytes;
	UINT new_len;
	rfbZlibHeader hdr;
	// Read in the rfbZlibHeader
	omni_mutex_lock l(m_bitmapdcMutex);
	ReadExact((char *)&hdr, sz_rfbZlibHeader);
	numCompBytes = Swap32IfLE(hdr.nBytes);
	// Check the net buffer
	CheckBufferSize(numCompBytes);
	// Read the compressed data
	ReadExact((char *)m_netbuf, numCompBytes);

	// Verify buffer space for cache rects list
	CheckZlibBufferSize(numRawBytes+500);

	lzo1x_decompress((BYTE*)m_netbuf,numCompBytes,(BYTE*)m_zlibbuf,&new_len,NULL);
	BYTE* pzipbuf = m_zlibbuf;
	for (int i = 0 ; i < nNbCacheRects; i++)
	{
		rfbFramebufferUpdateRectHeader surh;
		memcpy((char *) &surh,pzipbuf, sz_rfbFramebufferUpdateRectHeader);
		surh.r.x = Swap16IfLE(surh.r.x);
		surh.r.y = Swap16IfLE(surh.r.y);
		surh.r.w = Swap16IfLE(surh.r.w);
		surh.r.h = Swap16IfLE(surh.r.h);
		surh.encoding = Swap32IfLE(surh.encoding);
		pzipbuf += sz_rfbFramebufferUpdateRectHeader;

		RECT rect;
		rect.left = surh.r.x;
		rect.right = surh.r.x + surh.r.w;
		rect.top = surh.r.y;
		rect.bottom = surh.r.y + surh.r.h;
		//border check
		if (!Check_Rectangle_borders(rect.left,rect.top,surh.r.w,surh.r.h)) return;

		SoftCursorLockArea(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
		
		 if ( surh.encoding==rfbEncodingRaw)
			{
				UINT numpixels = surh.r.w * surh.r.h;							  
				if (m_DIBbits) ConvertAll(surh.r.w,surh.r.h,surh.r.x, surh.r.y,m_myFormat.bitsPerPixel/8,(BYTE *)pzipbuf,(BYTE *)m_DIBbits,m_si.framebufferWidth);
				pzipbuf +=numpixels*m_myFormat.bitsPerPixel/8;
				InvalidateRegion(&rect,prgn);
			}
	}

}