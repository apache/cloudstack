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
//
// CACHERect Encoding
//
// .

#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"
// #include "zlib\zlib.h"


void ClientConnection::ReadCacheRect(rfbFramebufferUpdateRectHeader *pfburh)
{
	rfbCacheRect cr;
	ReadExact((char *) &cr, sz_rfbCacheRect);
	cr.special = Swap16IfLE(cr.special); 
	
	RECT rect;
	rect.left=pfburh->r.x;
	rect.right=pfburh->r.x+pfburh->r.w;
	rect.top=pfburh->r.y;
	rect.bottom=pfburh->r.y+pfburh->r.h;
	RestoreArea(rect);
}

void ClientConnection::SaveArea(RECT &r)
{
	if (!m_opts.m_fEnableCache) return; // sf@2002

	int x = r.left;
	int y = r.top;
	int w = r.right - r.left;
	int h = r.bottom - r.top;

	omni_mutex_lock l(m_bitmapdcMutex);
	ObjectSelector b1(m_hBitmapDC, m_hBitmap);
	PaletteSelector ps1(m_hBitmapDC, m_hPalette);
	ObjectSelector b2(m_hCacheBitmapDC, m_hCacheBitmap);
	PaletteSelector ps2(m_hCacheBitmapDC, m_hPalette);

	if (m_hCacheBitmapDC!=NULL) if (!BitBlt(m_hCacheBitmapDC, x, y, w, h, m_hBitmapDC, x, y, SRCCOPY)) {
		vnclog.Print(0, _T("Error saving screen\n"));
	}
}

void ClientConnection::RestoreArea(RECT &r)
{
	int x = r.left;
	int y = r.top;
	int w = r.right - r.left;
	int h = r.bottom - r.top;
	HBITMAP m_hTempBitmap=NULL;
	HDC		m_hTempBitmapDC=NULL;

	omni_mutex_lock l(m_bitmapdcMutex);

	ObjectSelector b1(m_hBitmapDC, m_hBitmap);
	PaletteSelector ps1(m_hBitmapDC, m_hPalette);
	m_hTempBitmapDC = CreateCompatibleDC(m_hBitmapDC);
	m_hTempBitmap = CreateCompatibleBitmap(m_hBitmapDC, w, h);
	ObjectSelector b3(m_hTempBitmapDC, m_hTempBitmap);
	PaletteSelector ps3(m_hTempBitmapDC, m_hPalette);
	ObjectSelector b2(m_hCacheBitmapDC, m_hCacheBitmap);
	PaletteSelector ps2(m_hCacheBitmapDC, m_hPalette);

	if (!BitBlt(m_hTempBitmapDC, 0, 0, w, h, m_hBitmapDC, x, y, SRCCOPY)) {
		vnclog.Print(0, _T("Error saving temp bitmap\n"));
	}

	if (!BitBlt(m_hBitmapDC, x, y, w, h, m_hCacheBitmapDC, x, y, SRCCOPY)) {
		vnclog.Print(0, _T("Error restoring screen\n"));
	}

	if (!BitBlt(m_hCacheBitmapDC, x, y, w, h, m_hTempBitmapDC, 0, 0, SRCCOPY)) {
		vnclog.Print(0, _T("Error restoring screen under cursor\n"));
	}
	DeleteDC(m_hTempBitmapDC);
	if (m_hTempBitmap != NULL)
		DeleteObject(m_hTempBitmap);
	if (m_hCacheBitmapDC != NULL)
		DeleteObject(m_hTempBitmapDC);
}

//
// sf@2002
// 
void ClientConnection::ClearCache()
{
	if (!m_opts.m_fEnableCache) return;

	if (!BitBlt(m_hCacheBitmapDC, 0, 0, 
				m_si.framebufferWidth, m_si.framebufferHeight, 0, 0, 0, BLACKNESS))
	{
		vnclog.Print(0, _T("Cache: Error Clearing Cache buffer bitmap\n"));
	}
	vnclog.Print(0, _T("Cache: Reset Cache\n"));
}


//
// sf@2002 
// - Read a cache rects zipped block coming from the server
// - Restore all these cache rects on the screen
void ClientConnection::ReadCacheZip(rfbFramebufferUpdateRectHeader *pfburh,HRGN *prgn)
{
	UINT nNbCacheRects = pfburh->r.x;

	UINT numRawBytes = nNbCacheRects * sz_rfbRectangle;
	numRawBytes += (numRawBytes/100) + 8;
	UINT numCompBytes;

	rfbZlibHeader hdr;
	// Read in the rfbZlibHeader
	ReadExact((char *)&hdr, sz_rfbZlibHeader);
	numCompBytes = Swap32IfLE(hdr.nBytes);

	// Check the net buffer
	CheckBufferSize(numCompBytes);

	// Read the compressed data
	ReadExact((char *)m_netbuf, numCompBytes);

	// Verify buffer space for cache rects list
	CheckZipBufferSize(numRawBytes);

	int nRet = uncompress((unsigned char*)m_zipbuf,// Dest  
						  (unsigned long*)&numRawBytes,// Dest len
						  (unsigned char*)m_netbuf,	// Src
						  numCompBytes	// Src len
						 );							    
	if (nRet != 0)
	{
		return;		
	}

	// Read all the cache rects
	rfbRectangle theRect;
	
	BYTE* p = m_zipbuf;
	for (int i = 0 ; i < nNbCacheRects; i++)
	{
		memcpy((BYTE*)&theRect, p, sz_rfbRectangle);
		p += sz_rfbRectangle;

		RECT cacherect;
		cacherect.left = Swap16IfLE(theRect.x);
		cacherect.right = Swap16IfLE(theRect.x) + Swap16IfLE(theRect.w);
		cacherect.top = Swap16IfLE(theRect.y);
		cacherect.bottom = Swap16IfLE(theRect.y) + Swap16IfLE(theRect.h);

		SoftCursorLockArea(cacherect.left, cacherect.top, cacherect.right - cacherect.left, cacherect.bottom - cacherect.top);
		RestoreArea(cacherect);
		InvalidateRegion(&cacherect,prgn);
	}

}

