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
#include "zlib\zlib.h"

void ClientConnection::ReadZlibRect(rfbFramebufferUpdateRectHeader *pfburh,int XOR) {

	UINT numpixels = pfburh->r.w * pfburh->r.h;
    // this assumes at least one byte per pixel. Naughty.
	UINT numRawBytes = numpixels * m_minPixelBytes;
	UINT numCompBytes;
	int inflateResult;

	rfbZlibHeader hdr;

	// Read in the rfbZlibHeader
	ReadExact((char *)&hdr, sz_rfbZlibHeader);

	numCompBytes = Swap32IfLE(hdr.nBytes);

	// Read in the compressed data
    CheckBufferSize(numCompBytes);
	ReadExact(m_netbuf, numCompBytes);

	// Verify enough buffer space for screen update.
	CheckZlibBufferSize(numRawBytes);

	m_decompStream.next_in = (unsigned char *)m_netbuf;
	m_decompStream.avail_in = numCompBytes;
	m_decompStream.next_out = m_zlibbuf;
	m_decompStream.avail_out = numRawBytes;
	m_decompStream.data_type = Z_BINARY;
		
	// Insure the inflator is initialized
	if ( m_decompStreamInited == false ) {
		m_decompStream.total_in = 0;
		m_decompStream.total_out = 0;
		m_decompStream.zalloc = Z_NULL;
		m_decompStream.zfree = Z_NULL;
		m_decompStream.opaque = Z_NULL;

		inflateResult = inflateInit( &m_decompStream );
		if ( inflateResult != Z_OK ) {
			vnclog.Print(0, _T("zlib inflateInit error: %d\n"), inflateResult);
			return;
		}
		m_decompStreamInited = true;
	}

	// Decompress screen data
	inflateResult = inflate( &m_decompStream, Z_SYNC_FLUSH );
	if ( inflateResult < 0 ) {
		vnclog.Print(0, _T("zlib inflate error: %d\n"), inflateResult);
		return;
	}
	SETUP_COLOR_SHORTCUTS;
	if (XOR==3)
	{
		mybool *maskbuffer=(mybool *)m_zlibbuf;
		BYTE *color=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8);
		BYTE *color2=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8)+m_myFormat.bitsPerPixel/8;
		// No other threads can use bitmap DC
		omni_mutex_lock l(m_bitmapdcMutex);
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  

		// This big switch is untidy but fast
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			SETXORMONOPIXELS(maskbuffer,color2, color,8, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 16:
			SETXORMONOPIXELS(maskbuffer,color2, color,16, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 24:
		case 32:
			SETXORMONOPIXELS(maskbuffer,color2, color,32, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)            
				break;
		default:
			vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
			return;
		}
	}

	else if (XOR==2)
	{
		mybool *maskbuffer=(mybool *)m_zlibbuf;
		BYTE *color=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8);
		BYTE *databuffer=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8)+m_myFormat.bitsPerPixel/8;
		// No other threads can use bitmap DC
		omni_mutex_lock l(m_bitmapdcMutex);
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  

		// This big switch is untidy but fast
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			SETXORSOLPIXELS(maskbuffer,databuffer, color,8, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 16:
			SETXORSOLPIXELS(maskbuffer,databuffer, color,16, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 24:
		case 32:
			SETXORSOLPIXELS(maskbuffer,databuffer, color,32, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)            
				break;
		default:
			vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
			return;
		}
	}
	else if (XOR==1)
	{
		mybool *maskbuffer=(mybool *)m_zlibbuf;
		BYTE *databuffer=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8);
		// No other threads can use bitmap DC
		omni_mutex_lock l(m_bitmapdcMutex);
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  
		int aantal=0;
		// This big switch is untidy but fast
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			SETXORPIXELS(maskbuffer,databuffer, 8, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h,aantal)
				break;
		case 16:
			SETXORPIXELS(maskbuffer,databuffer, 16, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h,aantal)
				break;
		case 24:
		case 32:
			SETXORPIXELS(maskbuffer,databuffer, 32, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h,aantal)            
				break;
		default:
			vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
			return;
		}
	}
	else if (XOR==0)
	{
		mybool *maskbuffer=(mybool *)m_zlibbuf;
		BYTE *databuffer=m_zlibbuf+(((pfburh->r.w*pfburh->r.h)+7)/8);
		// No other threads can use bitmap DC
		omni_mutex_lock l(m_bitmapdcMutex);
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  \
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  \

		// This big switch is untidy but fast
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			SETPIXELS(m_zlibbuf, 8, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 16:
			SETPIXELS(m_zlibbuf, 16, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 24:
		case 32:
			SETPIXELS(m_zlibbuf, 32, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)            
				break;
		default:
			vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
			return;
		}
	}
}
// Makes sure zlibbuf is at least as big as the specified size.
// Note that zlibbuf itself may change as a result of this call.
// Throws an exception on failure.
void ClientConnection::CheckZlibBufferSize(int bufsize)
{
	unsigned char *newbuf;

	if (m_zlibbufsize > bufsize) return;

	omni_mutex_lock l(m_zlibBufferMutex);


	newbuf = (unsigned char *)new char[bufsize+256];
//	if (newbuf == NULL) {
//		throw ErrorException("Insufficient memory to allocate zlib buffer.");
//	}

	// Only if we're successful...

	if (m_zlibbuf != NULL)
		delete [] m_zlibbuf;
	m_zlibbuf = newbuf;
	m_zlibbufsize=bufsize + 256;
	vnclog.Print(4, _T("zlibbufsize expanded to %d\n"), m_zlibbufsize);


}

void ClientConnection::ReadSolidRect(rfbFramebufferUpdateRectHeader *pfburh) {

	UINT numpixels = pfburh->r.w * pfburh->r.h;
	ReadExact(m_netbuf, m_myFormat.bitsPerPixel/8);
	SETUP_COLOR_SHORTCUTS;
	
		// No other threads can use bitmap DC
		omni_mutex_lock l(m_bitmapdcMutex);
		ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
		PaletteSelector p(m_hBitmapDC, m_hPalette);							  

		// This big switch is untidy but fast
		switch (m_myFormat.bitsPerPixel) {
		case 8:
			SETSOLPIXELS(m_netbuf,8, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 16:
			SETSOLPIXELS(m_netbuf,16, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)
				break;
		case 24:
		case 32:
			SETSOLPIXELS(m_netbuf,32, pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h)            
				break;
		default:
			vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
			return;
		}
}

void ClientConnection::ReadSolMonoZip(rfbFramebufferUpdateRectHeader *pfburh,HRGN *prgn)
{
	UINT nNbCacheRects = pfburh->r.x;
	UINT numRawBytes = pfburh->r.y+pfburh->r.w*65535;
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
	CheckZipBufferSize(numRawBytes+500);

	int nRet = uncompress((unsigned char*)m_zipbuf,// Dest  
						  (unsigned long*)&numRawBytes,// Dest len
						  (unsigned char*)m_netbuf,	// Src
						  numCompBytes	// Src len
						 );							    
	if (nRet != 0)
	{
		return;		
	}

	BYTE* pzipbuf = m_zipbuf;
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

		SoftCursorLockArea(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
		SaveArea(rect);

		if (surh.encoding==rfbEncodingSolidColor)
			{
				UINT numpixels = surh.r.w * surh.r.h;
				SETUP_COLOR_SHORTCUTS;
	
				// No other threads can use bitmap DC
				omni_mutex_lock l(m_bitmapdcMutex);
				ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
				PaletteSelector p(m_hBitmapDC, m_hPalette);							  

				// This big switch is untidy but fast
				switch (m_myFormat.bitsPerPixel) {
				case 8:
					SETSOLPIXELS(pzipbuf,8, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
					break;
				case 16:
					SETSOLPIXELS(pzipbuf,16, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
					break;
				case 24:
				case 32:
					SETSOLPIXELS(pzipbuf,32, surh.r.x, surh.r.y, surh.r.w, surh.r.h)            
					break;
				default:
					vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
				}
				pzipbuf +=m_myFormat.bitsPerPixel/8;
				InvalidateRegion(&rect,prgn);
			}
		else if ( surh.encoding==rfbEncodingXORMonoColor_Zlib)
			{
				SETUP_COLOR_SHORTCUTS;
				mybool *maskbuffer=(mybool *)pzipbuf;
				BYTE *color=pzipbuf+(((surh.r.w*surh.r.h)+7)/8);
				BYTE *color2=pzipbuf+(((surh.r.w*surh.r.h)+7)/8)+m_myFormat.bitsPerPixel/8;

				// No other threads can use bitmap DC
				omni_mutex_lock l(m_bitmapdcMutex);
				ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
				PaletteSelector p(m_hBitmapDC, m_hPalette);							  

				// This big switch is untidy but fast
				switch (m_myFormat.bitsPerPixel) {
				case 8:
					SETXORMONOPIXELS(maskbuffer,color2, color,8, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
						break;
				case 16:
					SETXORMONOPIXELS(maskbuffer,color2, color,16, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
						break;
				case 24:
				case 32:
					SETXORMONOPIXELS(maskbuffer,color2, color,32, surh.r.x, surh.r.y, surh.r.w, surh.r.h)            
						break;
				default:
					vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
				}
				pzipbuf += (((surh.r.w*surh.r.h)+7)/8)+m_myFormat.bitsPerPixel/8+m_myFormat.bitsPerPixel/8;
				InvalidateRegion(&rect,prgn);
			}
		else if ( surh.encoding==rfbEncodingXOR_Zlib)
			{
				SETUP_COLOR_SHORTCUTS;
				mybool *maskbuffer=(mybool *)pzipbuf;
				BYTE *databuffer=pzipbuf+(((surh.r.w*surh.r.h)+7)/8);
		
				omni_mutex_lock l(m_bitmapdcMutex);
				ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
				PaletteSelector p(m_hBitmapDC, m_hPalette);	
				int aantal=0;

				// This big switch is untidy but fast
				switch (m_myFormat.bitsPerPixel) {
				case 8:
					SETXORPIXELS(maskbuffer,databuffer, 8, surh.r.x, surh.r.y, surh.r.w, surh.r.h,aantal)
						break;
				case 16:
					SETXORPIXELS(maskbuffer,databuffer, 16,  surh.r.x, surh.r.y, surh.r.w, surh.r.h,aantal)
						break;
				case 24:
				case 32:
					SETXORPIXELS(maskbuffer,databuffer, 32,  surh.r.x, surh.r.y, surh.r.w, surh.r.h,aantal)            
						break;
				default:
					vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
				}
			// we need to count the size off the databuffer
				pzipbuf += (((surh.r.w*surh.r.h)+7)/8)+aantal*m_myFormat.bitsPerPixel/8;//mask
				InvalidateRegion(&rect,prgn);
			}
		else if ( surh.encoding==rfbEncodingRaw)
			{
				UINT numpixels = surh.r.w * surh.r.h;
				SETUP_COLOR_SHORTCUTS;
				omni_mutex_lock l(m_bitmapdcMutex);
				ObjectSelector b(m_hBitmapDC, m_hBitmap);							  
				PaletteSelector p(m_hBitmapDC, m_hPalette);							  

				// This big switch is untidy but fast
				switch (m_myFormat.bitsPerPixel) {
					case 8:
						SETPIXELS(pzipbuf, 8, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
							break;
					case 16:
						SETPIXELS(pzipbuf, 16, surh.r.x, surh.r.y, surh.r.w, surh.r.h)
							break;
					case 24:
					case 32:
						SETPIXELS(pzipbuf, 32, surh.r.x, surh.r.y, surh.r.w, surh.r.h)            
							break;
					default:
						vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"), m_myFormat.bitsPerPixel);
				}
				pzipbuf +=numpixels*m_myFormat.bitsPerPixel/8;
				InvalidateRegion(&rect,prgn);
			}
	}

}