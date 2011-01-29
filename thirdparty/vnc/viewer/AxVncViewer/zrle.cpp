//
// Copyright (C) 2002 RealVNC Ltd.  All Rights Reserved.
//

#include "rdr/ZlibInStream.h"
#include "rdr/FdInStream.h"
#include "rdr/Exception.h"
#include "stdhdrs.h"
#include "vncviewer.h"
// #include "ClientConnection.h"

// Instantiate the decoding function for 8, 16 and 32 BPP

#define FAVOUR_FILL_RECT

#define zrleDecode ClientConnection::zrleDecode

#define BPP 8
#define IMAGE_RECT(x,y,w,h,data)                \
    SETUP_COLOR_SHORTCUTS;                      \
    SETPIXELS(m_netbuf,8,x,y,w,h)
#define FILL_RECT(x,y,w,h,pix)                          \
    SETUP_COLOR_SHORTCUTS;                              \
    COLORREF color = COLOR_FROM_PIXEL8_ADDRESS(&pix);   \
    FillSolidRect(x,y,w,h,color)
#include "rfb/zrleDecode.h"
#undef BPP
#undef IMAGE_RECT
#undef FILL_RECT

#define BPP 16
#define IMAGE_RECT(x,y,w,h,data)                \
    SETUP_COLOR_SHORTCUTS;                      \
    SETPIXELS(m_netbuf,16,x,y,w,h)
#define FILL_RECT(x,y,w,h,pix)                          \
    SETUP_COLOR_SHORTCUTS;                              \
    COLORREF color = COLOR_FROM_PIXEL16_ADDRESS(&pix);  \
    FillSolidRect(x,y,w,h,color)
#include "rfb/zrleDecode.h"
#undef BPP
#undef IMAGE_RECT
#undef FILL_RECT

#define IMAGE_RECT(x,y,w,h,data)                \
    SETUP_COLOR_SHORTCUTS;                      \
    SETPIXELS(m_netbuf,32,x,y,w,h)
#define FILL_RECT(x,y,w,h,pix)                          \
    SETUP_COLOR_SHORTCUTS;                              \
    COLORREF color = COLOR_FROM_PIXEL32_ADDRESS(&pix);  \
    FillSolidRect(x,y,w,h,color)

#define BPP 32
#include "rfb/zrleDecode.h"
#define CPIXEL 24A
#include "rfb/zrleDecode.h"
#undef CPIXEL
#define CPIXEL 24B
#include "rfb/zrleDecode.h"
#undef CPIXEL
#undef BPP
#undef IMAGE_RECT
#undef FILL_RECT

#undef zrleDecode

void ClientConnection::zrleDecode(int x, int y, int w, int h)
{
  try {
    CheckBufferSize(rfbZRLETileWidth * rfbZRLETileHeight * 4);
    omni_mutex_lock l(m_bitmapdcMutex);
    ObjectSelector b(m_hBitmapDC, m_hBitmap);
    PaletteSelector p(m_hBitmapDC, m_hPalette);

    switch (m_myFormat.bitsPerPixel) {

    case 8:
      zrleDecode8(x,y,w,h,fis,zis,(rdr::U8*)m_netbuf);
      break;

    case 16:
      zrleDecode16(x,y,w,h,fis,zis,(rdr::U16*)m_netbuf);
      break;

    case 32:
      bool fitsInLS3Bytes
        = ((m_myFormat.redMax   << m_myFormat.redShift)   < (1<<24) &&
           (m_myFormat.greenMax << m_myFormat.greenShift) < (1<<24) &&
           (m_myFormat.blueMax  << m_myFormat.blueShift)  < (1<<24));

      bool fitsInMS3Bytes = (m_myFormat.redShift   > 7  &&
                             m_myFormat.greenShift > 7  &&
                             m_myFormat.blueShift  > 7);

      if ((fitsInLS3Bytes && !m_myFormat.bigEndian) ||
          (fitsInMS3Bytes && m_myFormat.bigEndian))
      {
        zrleDecode24A(x,y,w,h,fis,zis,(rdr::U32*)m_netbuf);
      }
      else if ((fitsInLS3Bytes && m_myFormat.bigEndian) ||
               (fitsInMS3Bytes && !m_myFormat.bigEndian))
      {
        zrleDecode24B(x,y,w,h,fis,zis,(rdr::U32*)m_netbuf);
      }
      else
      {
        zrleDecode32(x,y,w,h,fis,zis,(rdr::U32*)m_netbuf);
      }
      break;
    }

  } catch (rdr::Exception& e) {
    fprintf(stderr,"ZRLE decoder exception: %s\n",e.str());
    throw;
  }
}
