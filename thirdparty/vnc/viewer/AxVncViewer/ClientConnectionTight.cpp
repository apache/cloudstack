//  Copyright (C) 2000, 2001 Const Kaplinsky. All Rights Reserved.
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

// Tight Encoding
//
// The bits of the ClientConnection object to do with Tight.

#include "stdhdrs.h"
#include "vncviewer.h"
#include "ClientConnection.h"

#define TIGHT_MIN_TO_COMPRESS 12
#define TIGHT_BUFFER_SIZE (2048 * 200)

void ClientConnection::ReadTightRect(rfbFramebufferUpdateRectHeader *pfburh)
{
  if (m_myFormat.bitsPerPixel != 8 &&
      m_myFormat.bitsPerPixel != 16 &&
      m_myFormat.bitsPerPixel != 32) {
      vnclog.Print(0, _T("Invalid number of bits per pixel: %d\n"),
                 m_myFormat.bitsPerPixel);
    return;
  }

  CARD8 comp_ctl;
  ReadExact((char *)&comp_ctl, 1);

  /* Flush zlib streams if we are told by the server to do so. */
  for (int i = 0; i < 4; i++) {
    if ((comp_ctl & 1) && m_tightZlibStreamActive[i]) {
      int err = inflateEnd (&m_tightZlibStream[i]);
      if (err != Z_OK) {
        if (m_tightZlibStream[i].msg != NULL) {
          vnclog.Print(0, _T("zlib inflateEnd() error: %s\n"),
                       m_tightZlibStream[i].msg);
        } else {
          vnclog.Print(0, _T("zlib inflateEnd() error: %d\n"), err);
        }
        return;
      }
      m_tightZlibStreamActive[i] = FALSE;
    }
    comp_ctl >>= 1;
  }

  /* Handle solid rectangles. */
  if (comp_ctl == rfbTightFill) {
    COLORREF fillColour;
    if (m_myFormat.depth == 24 && m_myFormat.redMax == 0xFF &&
        m_myFormat.greenMax == 0xFF && m_myFormat.blueMax == 0xFF) {
      CARD8 fillColourBuf[3];
      ReadExact((char *)&fillColourBuf, 3);
      fillColour = COLOR_FROM_PIXEL24_ADDRESS(fillColourBuf);
    } else {
      CARD32 fillColourBuf;
      ReadExact((char *)&fillColourBuf, m_myFormat.bitsPerPixel / 8);

      SETUP_COLOR_SHORTCUTS;

      switch (m_myFormat.bitsPerPixel) {
      case 8:
        fillColour = COLOR_FROM_PIXEL8_ADDRESS(&fillColourBuf);
        break;
      case 16:
        fillColour = COLOR_FROM_PIXEL16_ADDRESS(&fillColourBuf);
        break;
      default:
        fillColour = COLOR_FROM_PIXEL32_ADDRESS(&fillColourBuf);
      }
    }

    omni_mutex_lock l(m_bitmapdcMutex);
    ObjectSelector b(m_hBitmapDC, m_hBitmap);
    PaletteSelector p(m_hBitmapDC, m_hPalette);

    FillSolidRect(pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h,
                  fillColour);
    return;
  }

  if (comp_ctl == rfbTightJpeg) {
    DecompressJpegRect(pfburh->r.x, pfburh->r.y, pfburh->r.w, pfburh->r.h);
	return;
  }


  /* Quit on unsupported subencoding value. */
  if (comp_ctl >= rfbTightMaxSubencoding) {
    vnclog.Print(0, _T("Tight encoding: bad subencoding value received.\n"));
    return;
  }

  /*
   * Here primary compression mode handling begins.
   * Data was processed with optional filter + zlib compression.
   */

  /* First, we should identify a filter to use. */
  int bitsPixel;
  if ((comp_ctl & rfbTightExplicitFilter) != 0) {
    CARD8 filter_id;
    ReadExact((char *)&filter_id, 1);

    switch (filter_id) {
    case rfbTightFilterCopy:
      bitsPixel = InitFilterCopy(pfburh->r.w, pfburh->r.h);
      break;
    case rfbTightFilterPalette:
      bitsPixel = InitFilterPalette(pfburh->r.w, pfburh->r.h);
      break;
    case rfbTightFilterGradient:
      bitsPixel = InitFilterGradient(pfburh->r.w, pfburh->r.h);
      break;
    default:
      vnclog.Print(0, _T("Tight encoding: unknown filter code received.\n"));
      return;
    }
  } else {
    bitsPixel = InitFilterCopy(pfburh->r.w, pfburh->r.h);
  }
  if (bitsPixel == 0) {
    vnclog.Print(0, _T("Tight encoding: error receiving palette.\n"));
    return;
  }

  /* Determine if the data should be decompressed or just copied. */
  int rowSize = (pfburh->r.w * bitsPixel + 7) / 8;
  if (pfburh->r.h * rowSize < TIGHT_MIN_TO_COMPRESS) {
    CheckBufferSize(pfburh->r.h * rowSize);
    ReadExact(m_netbuf, pfburh->r.h * rowSize);

    CheckZlibBufferSize(pfburh->r.w * pfburh->r.h * 4);
    (this->*m_tightCurrentFilter)(pfburh->r.h);

    omni_mutex_lock l(m_bitmapdcMutex);
    ObjectSelector b(m_hBitmapDC, m_hBitmap);
    PaletteSelector p(m_hBitmapDC, m_hPalette);

    SETPIXELS_NOCONV(m_zlibbuf, pfburh->r.x, pfburh->r.y,
                     pfburh->r.w, pfburh->r.h);

    return;
  }

  /* Read the length (1..3 bytes) of compressed data following. */

  int compressedLen = ReadCompactLen();
  if (compressedLen <= 0) {
    vnclog.Print(0, _T("Tight encoding: bad data received from server.\n"));
    return;
  }

  /* Now let's initialize compression stream if needed. */
  int stream_id = comp_ctl & 0x03;
  z_streamp zs = &m_tightZlibStream[stream_id];
  if (!m_tightZlibStreamActive[stream_id]) {
    zs->zalloc = Z_NULL;
    zs->zfree = Z_NULL;
    zs->opaque = Z_NULL;
    int err = inflateInit(zs);
    if (err != Z_OK) {
      if (zs->msg != NULL) {
        vnclog.Print(0, _T("zlib inflateInit() error: %s.\n"), zs->msg);
      } else {
        vnclog.Print(0, _T("zlib inflateInit() error: %d.\n"), err);
      }
      return;
    }
    m_tightZlibStreamActive[stream_id] = TRUE;
  }

  /* Read, decode and draw actual pixel data in a loop. */

  int beforeBufferSize =
    TIGHT_BUFFER_SIZE * bitsPixel / (bitsPixel + sizeof(COLORREF) * 8)
      & 0xFFFFFFFC;
  CheckBufferSize(beforeBufferSize);

  int afterBufferSize = TIGHT_BUFFER_SIZE - beforeBufferSize;
  CheckZlibBufferSize(afterBufferSize);

  int rowsProcessed = 0, extraBytes = 0;
  int err, numRows, portionLen;

  while (compressedLen > 0) {
    if (compressedLen > TIGHT_ZLIB_BUFFER_SIZE)
      portionLen = TIGHT_ZLIB_BUFFER_SIZE;
    else
      portionLen = compressedLen;

    ReadExact(m_tightbuf, portionLen);

    compressedLen -= portionLen;

    zs->next_in = (Bytef *)m_tightbuf;
    zs->avail_in = portionLen;

    do {
      zs->next_out = (Bytef *)&m_netbuf[extraBytes];
      zs->avail_out = beforeBufferSize - extraBytes;

      err = inflate(zs, Z_SYNC_FLUSH);
      if (err == Z_BUF_ERROR)   // Input exhausted -- no problem
        break;
      if (err != Z_OK && err != Z_STREAM_END) {
        if (zs->msg != NULL) {
          vnclog.Print(0, _T("zlib inflate() error: %s.\n"), zs->msg);
        } else {
          vnclog.Print(0, _T("zlib inflate() error: %d.\n"), err);
        }
        return;
      }

      numRows = (beforeBufferSize - zs->avail_out) / rowSize;

      (this->*m_tightCurrentFilter)(numRows);

      extraBytes = beforeBufferSize - zs->avail_out - numRows * rowSize;
      if (extraBytes > 0)
        memcpy(m_netbuf, &m_netbuf[numRows * rowSize], extraBytes);

      omni_mutex_lock l(m_bitmapdcMutex);
      ObjectSelector b(m_hBitmapDC, m_hBitmap);
      PaletteSelector p(m_hBitmapDC, m_hPalette);

      SETPIXELS_NOCONV(m_zlibbuf, pfburh->r.x, pfburh->r.y + rowsProcessed,
                       pfburh->r.w, numRows);

      rowsProcessed += numRows;
    }
    while (zs->avail_out == 0);
  }

  if (rowsProcessed != pfburh->r.h) {
    vnclog.Print(0, _T("Tight encoding: wrong number of scan lines.\n"));
  }
}

int ClientConnection::ReadCompactLen() {
  CARD8 len_byte;
  ReadExact((char *)&len_byte, 1);
  int compressedLen = (int)len_byte & 0x7F;
  if (len_byte & 0x80) {
    ReadExact((char *)&len_byte, 1);
    compressedLen |= ((int)len_byte & 0x7F) << 7;
    if (len_byte & 0x80) {
      ReadExact((char *)&len_byte, 1);
      compressedLen |= ((int)len_byte & 0xFF) << 14;
    }
  }
  return compressedLen;
}

//----------------------------------------------------------------------------
//
// Filter stuff.
//

// The following variables are defined in the class declaration:
//   tightFilterFunc m_tightCurrentFilter;
//   Bool m_tightCutZeros;
//   int m_tightRectWidth, m_tightRectColors;
//   COLORREF m_tightPalette[256];
//   CARD8 m_tightPrevRow[2048*3*sizeof(CARD16)];

int ClientConnection::InitFilterCopy (int rw, int rh)
{
  tightFilterFunc funcArray[3] = {
    &ClientConnection::FilterCopy8,
    &ClientConnection::FilterCopy16,
    &ClientConnection::FilterCopy32
  };

  m_tightCurrentFilter = funcArray[m_myFormat.bitsPerPixel/16];
  m_tightRectWidth = rw;

  if (m_myFormat.depth == 24 && m_myFormat.redMax == 0xFF &&
      m_myFormat.greenMax == 0xFF && m_myFormat.blueMax == 0xFF) {
    m_tightCutZeros = TRUE;
    m_tightCurrentFilter = &ClientConnection::FilterCopy24;
    return 24;
  }

  m_tightCutZeros = FALSE;
  return m_myFormat.bitsPerPixel;
}

int ClientConnection::InitFilterGradient (int rw, int rh)
{
  int bits = InitFilterCopy(rw, rh);

  tightFilterFunc funcArray[3] = {
    &ClientConnection::FilterGradient8,
    &ClientConnection::FilterGradient16,
    &ClientConnection::FilterGradient32
  };

  m_tightCurrentFilter = funcArray[m_myFormat.bitsPerPixel/16];

  if (m_tightCutZeros) {
    m_tightCurrentFilter = &ClientConnection::FilterGradient24;
    memset(m_tightPrevRow, 0, rw * 3);
  } else
    memset(m_tightPrevRow, 0, rw * 3 * sizeof(CARD16));

  return bits;
}

int ClientConnection::InitFilterPalette (int rw, int rh)
{
  m_tightCurrentFilter = &ClientConnection::FilterPalette;
  m_tightRectWidth = rw;

  CARD8 numColors;
  ReadExact((char *)&numColors, 1);

  m_tightRectColors = (int)numColors;
  if (++m_tightRectColors < 2)
    return 0;

  if (m_myFormat.depth == 24 && m_myFormat.redMax == 0xFF &&
      m_myFormat.greenMax == 0xFF && m_myFormat.blueMax == 0xFF) {

    CheckBufferSize(m_tightRectColors * 3);
    ReadExact(m_netbuf, m_tightRectColors * 3);

    for (int i = 0; i < m_tightRectColors; i++)
      m_tightPalette[i] = COLOR_FROM_PIXEL24_ADDRESS(&m_netbuf[i*3]);

  } else {
    CheckBufferSize(m_tightRectColors * (m_myFormat.bitsPerPixel / 8));
    ReadExact(m_netbuf, m_tightRectColors * (m_myFormat.bitsPerPixel / 8));

    SETUP_COLOR_SHORTCUTS;

    int i;
    switch (m_myFormat.bitsPerPixel) {
    case 8:
      for (i = 0; i < m_tightRectColors; i++)
        m_tightPalette[i] = COLOR_FROM_PIXEL8_ADDRESS(&m_netbuf[i]);
      break;
    case 16:
      for (i = 0; i < m_tightRectColors; i++)
        m_tightPalette[i] = COLOR_FROM_PIXEL16_ADDRESS(&m_netbuf[i*2]);
      break;
    default:
      for (i = 0; i < m_tightRectColors; i++)
        m_tightPalette[i] = COLOR_FROM_PIXEL32_ADDRESS(&m_netbuf[i*4]);
    }
  }

  return (m_tightRectColors == 2) ? 1 : 8;
}

//
// Actual filtering code follows.
//

#define DEFINE_TIGHT_FILTER_COPY(bpp)                                         \
                                                                              \
void ClientConnection::FilterCopy##bpp (int numRows)                          \
{                                                                             \
  COLORREF *dst = (COLORREF *)m_zlibbuf;                                      \
                                                                              \
  SETUP_COLOR_SHORTCUTS;                                                      \
                                                                              \
  int x;                                                                      \
  for (int y = 0; y < numRows; y++) {                                         \
    for (x = 0; x < m_tightRectWidth; x++) {                                  \
      dst[y*m_tightRectWidth+x] =                                             \
        COLOR_FROM_PIXEL##bpp##_ADDRESS(m_netbuf +                            \
                                        (y*m_tightRectWidth+x)*(bpp/8));      \
    }                                                                         \
  }                                                                           \
}

DEFINE_TIGHT_FILTER_COPY(8)
DEFINE_TIGHT_FILTER_COPY(16)
DEFINE_TIGHT_FILTER_COPY(24)
DEFINE_TIGHT_FILTER_COPY(32)

#define DEFINE_TIGHT_FILTER_GRADIENT(bpp)                                     \
                                                                              \
void ClientConnection::FilterGradient##bpp (int numRows)                      \
{                                                                             \
  int x, y, c;                                                                \
  CARD##bpp *src = (CARD##bpp *)m_netbuf;                                     \
  COLORREF *dst = (COLORREF *)m_zlibbuf;                                      \
  CARD16 *thatRow = (CARD16 *)m_tightPrevRow;                                 \
  CARD16 thisRow[2048*3];                                                     \
  CARD16 pix[3];                                                              \
  CARD16 max[3];                                                              \
  int shift[3];                                                               \
  int est[3];                                                                 \
                                                                              \
  max[0] = m_myFormat.redMax;                                                 \
  max[1] = m_myFormat.greenMax;                                               \
  max[2] = m_myFormat.blueMax;                                                \
                                                                              \
  shift[0] = m_myFormat.redShift;                                             \
  shift[1] = m_myFormat.greenShift;                                           \
  shift[2] = m_myFormat.blueShift;                                            \
                                                                              \
  for (y = 0; y < numRows; y++) {                                             \
                                                                              \
    /* First pixel in a row */                                                \
    for (c = 0; c < 3; c++) {                                                 \
      pix[c] = (CARD16)((src[y*m_tightRectWidth] >> shift[c]) +               \
                        thatRow[c] & max[c]);                                 \
      thisRow[c] = pix[c];                                                    \
    }                                                                         \
    dst[y*m_tightRectWidth] = PALETTERGB((CARD32)pix[0] * 255 / max[0],       \
                                         (CARD32)pix[1] * 255 / max[1],       \
                                         (CARD32)pix[2] * 255 / max[2]);      \
                                                                              \
    /* Remaining pixels of a row */                                           \
    for (x = 1; x < m_tightRectWidth; x++) {                                  \
      for (c = 0; c < 3; c++) {                                               \
        est[c] = (int)thatRow[x*3+c] + (int)pix[c]-(int)thatRow[(x-1)*3 + c]; \
        if (est[c] > (int)max[c]) {                                           \
          est[c] = (int)max[c];                                               \
        } else if (est[c] < 0) {                                              \
          est[c] = 0;                                                         \
        }                                                                     \
        pix[c] = (CARD16)((src[y*m_tightRectWidth+x] >> shift[c]) +           \
                          est[c] & max[c]);                                   \
        thisRow[x*3+c] = pix[c];                                              \
      }                                                                       \
      dst[y*m_tightRectWidth+x] = PALETTERGB((CARD32)pix[0] * 255 / max[0],   \
                                             (CARD32)pix[1] * 255 / max[1],   \
                                             (CARD32)pix[2] * 255 / max[2]);  \
    }                                                                         \
    memcpy(thatRow, thisRow, m_tightRectWidth * 3 * sizeof(CARD16));          \
  }                                                                           \
}

DEFINE_TIGHT_FILTER_GRADIENT(8)
DEFINE_TIGHT_FILTER_GRADIENT(16)
DEFINE_TIGHT_FILTER_GRADIENT(32)

void ClientConnection::FilterGradient24 (int numRows)
{
  CARD8 thisRow[2048*3];
  CARD8 pix[3];
  int est[3];

  COLORREF *dst = (COLORREF *)m_zlibbuf;

  for (int y = 0; y < numRows; y++) {

    // First pixel in a row
    for (int c = 0; c < 3; c++) {
      pix[c] = m_tightPrevRow[c] + m_netbuf[y*m_tightRectWidth*3+c];
      thisRow[c] = pix[c];
    }
    dst[y*m_tightRectWidth] = COLOR_FROM_PIXEL24_ADDRESS(pix);

    // Remaining pixels of a row
    for (int x = 1; x < m_tightRectWidth; x++) {
      for (int c = 0; c < 3; c++) {
        est[c] = (int)m_tightPrevRow[x*3+c] + (int)pix[c] -
                 (int)m_tightPrevRow[(x-1)*3+c];
        if (est[c] > 0xFF) {
          est[c] = 0xFF;
        } else if (est[c] < 0x00) {
          est[c] = 0x00;
        }
        pix[c] = (CARD8)est[c] + m_netbuf[(y*m_tightRectWidth+x)*3+c];
        thisRow[x*3+c] = pix[c];
      }
      dst[y*m_tightRectWidth+x] = COLOR_FROM_PIXEL24_ADDRESS(pix);
    }

    memcpy(m_tightPrevRow, thisRow, m_tightRectWidth * 3);
  }
}

void ClientConnection::FilterPalette (int numRows)
{
  int x, y, b, w;
  CARD8 *src = (CARD8 *)m_netbuf;
  COLORREF *dst = (COLORREF *)m_zlibbuf;

  if (m_tightRectColors == 2) {
    w = (m_tightRectWidth + 7) / 8;
    for (y = 0; y < numRows; y++) {
      for (x = 0; x < m_tightRectWidth / 8; x++) {
        for (b = 7; b >= 0; b--)
          dst[y*m_tightRectWidth+x*8+7-b] = m_tightPalette[src[y*w+x] >> b & 1];
      }
      for (b = 7; b >= 8 - m_tightRectWidth % 8; b--) {
        dst[y*m_tightRectWidth+x*8+7-b] = m_tightPalette[src[y*w+x] >> b & 1];
      }
    }
  } else {
    for (y = 0; y < numRows; y++)
      for (x = 0; x < m_tightRectWidth; x++)
        dst[y*m_tightRectWidth+x] = m_tightPalette[(int)src[y*m_tightRectWidth+x]];
  }
}

//
// JPEG decompression code.
//

static bool jpegError;

static void JpegSetSrcManager(j_decompress_ptr cinfo, char *compressedData,
							  int compressedLen);

void ClientConnection::DecompressJpegRect(int x, int y, int w, int h)
{
  struct jpeg_decompress_struct cinfo;
  struct jpeg_error_mgr jerr;

  int compressedLen = (int)ReadCompactLen();
  if (compressedLen <= 0) {
    vnclog.Print(0, _T("Incorrect data received from the server.\n"));
    return;
  }

  CheckBufferSize(compressedLen);
  ReadExact(m_netbuf, compressedLen);

  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_decompress(&cinfo);

  JpegSetSrcManager(&cinfo, m_netbuf, compressedLen);

  jpeg_read_header(&cinfo, TRUE);
  cinfo.out_color_space = JCS_RGB;

  jpeg_start_decompress(&cinfo);
  if ((int)cinfo.output_width != w || (int)cinfo.output_height != h ||
      cinfo.output_components != 3) {
    vnclog.Print(0, _T("Tight Encoding: Wrong JPEG data received.\n"));
    jpeg_destroy_decompress(&cinfo);
    return;
  }

  omni_mutex_lock l(m_bitmapdcMutex);
  ObjectSelector b(m_hBitmapDC, m_hBitmap);
  PaletteSelector p(m_hBitmapDC, m_hPalette);

  // Two scanlines: for 24bit and COLORREF samples
  CheckZlibBufferSize(2*2048*4);

  JSAMPROW rowPointer[1];
  rowPointer[0] = (JSAMPROW)m_zlibbuf;

  COLORREF *pixelPtr;
  for (int dy = 0; cinfo.output_scanline < cinfo.output_height; dy++) {
    jpeg_read_scanlines(&cinfo, rowPointer, 1);
    if (jpegError) {
      break;
    }
    pixelPtr = (COLORREF *)&m_zlibbuf[2048*4];
    for (int dx = 0; dx < w; dx++) {
      *pixelPtr++ = COLOR_FROM_PIXEL24_ADDRESS(&m_zlibbuf[dx*3]);
    }
    SETPIXELS_NOCONV(&m_zlibbuf[2048*4], x, y + dy, w, 1);
  }

  if (!jpegError)
    jpeg_finish_decompress(&cinfo);

  jpeg_destroy_decompress(&cinfo);
}

//
// A "Source manager" for the JPEG library.
//

static struct jpeg_source_mgr jpegSrcManager;
static JOCTET *jpegBufferPtr;
static size_t jpegBufferLen;

static void JpegInitSource(j_decompress_ptr cinfo);
static boolean JpegFillInputBuffer(j_decompress_ptr cinfo);
static void JpegSkipInputData(j_decompress_ptr cinfo, long num_bytes);
static void JpegTermSource(j_decompress_ptr cinfo);

static void
JpegInitSource(j_decompress_ptr cinfo)
{
  jpegError = false;
}

static boolean
JpegFillInputBuffer(j_decompress_ptr cinfo)
{
  jpegError = true;
  jpegSrcManager.bytes_in_buffer = jpegBufferLen;
  jpegSrcManager.next_input_byte = (JOCTET *)jpegBufferPtr;

  return TRUE;
}

static void
JpegSkipInputData(j_decompress_ptr cinfo, long num_bytes)
{
  if (num_bytes < 0 || (size_t)num_bytes > jpegSrcManager.bytes_in_buffer) {
    jpegError = true;
    jpegSrcManager.bytes_in_buffer = jpegBufferLen;
    jpegSrcManager.next_input_byte = (JOCTET *)jpegBufferPtr;
  } else {
    jpegSrcManager.next_input_byte += (size_t) num_bytes;
    jpegSrcManager.bytes_in_buffer -= (size_t) num_bytes;
  }
}

static void
JpegTermSource(j_decompress_ptr cinfo)
{
  /* No work necessary here. */
}

static void
JpegSetSrcManager(j_decompress_ptr cinfo, char *compressedData, int compressedLen)
{
  jpegBufferPtr = (JOCTET *)compressedData;
  jpegBufferLen = (size_t)compressedLen;

  jpegSrcManager.init_source = JpegInitSource;
  jpegSrcManager.fill_input_buffer = JpegFillInputBuffer;
  jpegSrcManager.skip_input_data = JpegSkipInputData;
  jpegSrcManager.resync_to_restart = jpeg_resync_to_restart;
  jpegSrcManager.term_source = JpegTermSource;
  jpegSrcManager.next_input_byte = jpegBufferPtr;
  jpegSrcManager.bytes_in_buffer = jpegBufferLen;

  cinfo->src = &jpegSrcManager;
}

