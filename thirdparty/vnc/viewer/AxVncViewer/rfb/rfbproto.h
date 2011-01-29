/*
//  Copyright (C) 2002 Ultr@VNC Team Members. All Rights Reserved.
//  Copyright (C) 2000-2002 Const Kaplinsky. All Rights Reserved.
 *  Copyright (C) 2002 RealVNC Ltd.  All Rights Reserved.
 *  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
 *
 *  This is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 *  USA.
 */

/*
 * rfbproto.h - header file for the RFB protocol version 3.3
 *
 * Uses types CARD<n> for an n-bit unsigned integer, INT<n> for an n-bit signed
 * integer (for n = 8, 16 and 32).
 *
 * All multiple byte integers are in big endian (network) order (most
 * significant byte first).  Unless noted otherwise there is no special
 * alignment of protocol structures.
 *
 *
 * Once the initial handshaking is done, all messages start with a type byte,
 * (usually) followed by message-specific data.  The order of definitions in
 * this file is as follows:
 *
 *  (1) Structures used in several types of message.
 *  (2) Structures used in the initial handshaking.
 *  (3) Message types.
 *  (4) Encoding types.
 *  (5) For each message type, the form of the data following the type byte.
 *      Sometimes this is defined by a single structure but the more complex
 *      messages have to be explained by comments.
 */


/*****************************************************************************
 *
 * Structures used in several messages
 *
 *****************************************************************************/

/*-----------------------------------------------------------------------------
 * Structure used to specify a rectangle.  This structure is a multiple of 4
 * bytes so that it can be interspersed with 32-bit pixel data without
 * affecting alignment.
 */

typedef struct {
    CARD16 x;
    CARD16 y;
    CARD16 w;
    CARD16 h;
} rfbRectangle;

#define sz_rfbRectangle 8


/*-----------------------------------------------------------------------------
 * Structure used to specify pixel format.
 */

typedef struct {

    CARD8 bitsPerPixel;		/* 8,16,32 only */

    CARD8 depth;		/* 8 to 32 */

    CARD8 bigEndian;		/* True if multi-byte pixels are interpreted
				   as big endian, or if single-bit-per-pixel
				   has most significant bit of the byte
				   corresponding to first (leftmost) pixel. Of
				   course this is meaningless for 8 bits/pix */

    CARD8 trueColour;		/* If false then we need a "colour map" to
				   convert pixels to RGB.  If true, xxxMax and
				   xxxShift specify bits used for red, green
				   and blue */

    /* the following fields are only meaningful if trueColour is true */

    CARD16 redMax;		/* maximum red value (= 2^n - 1 where n is the
				   number of bits used for red). Note this
				   value is always in big endian order. */

    CARD16 greenMax;		/* similar for green */

    CARD16 blueMax;		/* and blue */

    CARD8 redShift;		/* number of shifts needed to get the red
				   value in a pixel to the least significant
				   bit. To find the red value from a given
				   pixel, do the following:
				   1) Swap pixel value according to bigEndian
				      (e.g. if bigEndian is false and host byte
				      order is big endian, then swap).
				   2) Shift right by redShift.
				   3) AND with redMax (in host byte order).
				   4) You now have the red value between 0 and
				      redMax. */

    CARD8 greenShift;		/* similar for green */

    CARD8 blueShift;		/* and blue */

    CARD8 pad1;
    CARD16 pad2;

} rfbPixelFormat;

#define sz_rfbPixelFormat 16

// Color settings values
#define rfbPFFullColors		0
#define rfbPF256Colors		1
#define rfbPF64Colors		2
#define rfbPF8Colors		3
#define rfbPF8GreyColors	4
#define rfbPF4GreyColors	5
#define rfbPF2GreyColors	6


/*****************************************************************************
 *
 * Initial handshaking messages
 *
 *****************************************************************************/

/*-----------------------------------------------------------------------------
 * Protocol Version
 *
 * The server always sends 12 bytes to start which identifies the latest RFB
 * protocol version number which it supports.  These bytes are interpreted
 * as a string of 12 ASCII characters in the format "RFB xxx.yyy\n" where
 * xxx and yyy are the major and minor version numbers (for version 3.3
 * this is "RFB 003.003\n").
 *
 * The client then replies with a similar 12-byte message giving the version
 * number of the protocol which should actually be used (which may be different
 * to that quoted by the server).
 *
 * It is intended that both clients and servers may provide some level of
 * backwards compatibility by this mechanism.  Servers in particular should
 * attempt to provide backwards compatibility, and even forwards compatibility
 * to some extent.  For example if a client demands version 3.1 of the
 * protocol, a 3.0 server can probably assume that by ignoring requests for
 * encoding types it doesn't understand, everything will still work OK.  This
 * will probably not be the case for changes in the major version number.
 *
 * The format string below can be used in sprintf or sscanf to generate or
 * decode the version string respectively.
 */

#define rfbProtocolVersionFormat "RFB %03d.%03d\n"
#define rfbProtocolMajorVersion 3
//#define rfbProtocolMinorVersion 4 // Reserved to UltravNC ! (as well as "6")
#define rfbProtocolMinorVersion 3 // Reserved to UltravNC ! (as well as "6")

typedef char rfbProtocolVersionMsg[13];	/* allow extra byte for null */

#define sz_rfbProtocolVersionMsg 12


/*-----------------------------------------------------------------------------
 * Authentication
 *
 * Once the protocol version has been decided, the server then sends a 32-bit
 * word indicating whether any authentication is needed on the connection.
 * The value of this word determines the authentication scheme in use.  For
 * version 3.0 of the protocol this may have one of the following values:
 */

#define rfbConnFailed 0
#define rfbNoAuth 1
#define rfbVncAuth 2
/* rfbMsLogon indicates UltraVNC's MS-Logon with (hopefully) better security */
#define rfbMsLogon 0xfffffffa

/*
 * rfbConnFailed:	For some reason the connection failed (e.g. the server
 *			cannot support the desired protocol version).  This is
 *			followed by a string describing the reason (where a
 *			string is specified as a 32-bit length followed by that
 *			many ASCII characters).
 *
 * rfbNoAuth:		No authentication is needed.
 *
 * rfbVncAuth:		The VNC authentication scheme is to be used.  A 16-byte
 *			challenge follows, which the client encrypts as
 *			appropriate using the password and sends the resulting
 *			16-byte response.  If the response is correct, the
 *			server sends the 32-bit word rfbVncAuthOK.  If a simple
 *			failure happens, the server sends rfbVncAuthFailed and
 *			closes the connection. If the server decides that too
 *			many failures have occurred, it sends rfbVncAuthTooMany
 *			and closes the connection.  In the latter case, the
 *			server should not allow an immediate reconnection by
 *			the client.
 */

#define rfbVncAuthOK 0
#define rfbVncAuthFailed 1
#define rfbVncAuthTooMany 2


/*-----------------------------------------------------------------------------
 * Client Initialisation Message
 *
 * Once the client and server are sure that they're happy to talk to one
 * another, the client sends an initialisation message.  At present this
 * message only consists of a boolean indicating whether the server should try
 * to share the desktop by leaving other clients connected, or give exclusive
 * access to this client by disconnecting all other clients.
 */

typedef struct {
    CARD8 shared;
} rfbClientInitMsg;

#define sz_rfbClientInitMsg 1


/*-----------------------------------------------------------------------------
 * Server Initialisation Message
 *
 * After the client initialisation message, the server sends one of its own.
 * This tells the client the width and height of the server's framebuffer,
 * its pixel format and the name associated with the desktop.
 */

typedef struct {
    CARD16 framebufferWidth;
    CARD16 framebufferHeight;
    rfbPixelFormat format;	/* the server's preferred pixel format */
    CARD32 nameLength;
    /* followed by char name[nameLength] */
} rfbServerInitMsg;

#define sz_rfbServerInitMsg (8 + sz_rfbPixelFormat)


/*
 * Following the server initialisation message it's up to the client to send
 * whichever protocol messages it wants.  Typically it will send a
 * SetPixelFormat message and a SetEncodings message, followed by a
 * FramebufferUpdateRequest.  From then on the server will send
 * FramebufferUpdate messages in response to the client's
 * FramebufferUpdateRequest messages.  The client should send
 * FramebufferUpdateRequest messages with incremental set to true when it has
 * finished processing one FramebufferUpdate and is ready to process another.
 * With a fast client, the rate at which FramebufferUpdateRequests are sent
 * should be regulated to avoid hogging the network.
 */



/*****************************************************************************
 *
 * Message types
 *
 *****************************************************************************/

/* server -> client */

#define rfbFramebufferUpdate 0
#define rfbSetColourMapEntries 1
#define rfbBell 2
#define rfbServerCutText 3
#define rfbResizeFrameBuffer 4 // Modif sf@2002 
//Modif cs@2005
#ifdef DSHOW
#define rfbKeyFrameUpdate 5
#endif
#define rfbPalmVNCReSizeFrameBuffer 0xF


/* client -> server */

#define rfbSetPixelFormat 0
#define rfbFixColourMapEntries 1	/* not currently supported */
#define rfbSetEncodings 2
#define rfbFramebufferUpdateRequest 3
#define rfbKeyEvent 4
#define rfbPointerEvent 5
#define rfbClientCutText 6
#define rfbFileTransfer 7     // Modif sf@2002 - actually bidirectionnal
#define rfbSetScale 8 // Modif sf@2002
#define rfbSetServerInput	9 // Modif rdv@2002
#define rfbSetSW	10// Modif rdv@2002
#define rfbTextChat	11// Modif sf@2002 - TextChat - Bidirectionnal
//Modif cs@2005
#ifdef DSHOW
#define rfbKeyFrameRequest 12
#endif
#define rfbPalmVNCSetScaleFactor 0xF // PalmVNC 1.4 & 2.0 SetScale Factor message



/*****************************************************************************
 *
 * Encoding types
 *
 *****************************************************************************/

#define rfbEncodingRaw 0
#define rfbEncodingCopyRect 1
#define rfbEncodingRRE 2
#define rfbEncodingCoRRE 4
#define rfbEncodingHextile 5
#define rfbEncodingZlib    6
#define rfbEncodingTight   7
#define rfbEncodingZlibHex 8
#define rfbEncodingUltra	9
#define rfbEncodingZRLE 16

// Cache & XOR-Zlib - rdv@2002
#define rfbEncodingCache					0xFFFF0000
#define rfbEncodingCacheEnable				0xFFFF0001
#define rfbEncodingXOR_Zlib					0xFFFF0002
#define rfbEncodingXORMonoColor_Zlib		0xFFFF0003
#define rfbEncodingXORMultiColor_Zlib		0xFFFF0004
#define rfbEncodingSolidColor				0xFFFF0005
#define rfbEncodingXOREnable				0xFFFF0006
#define rfbEncodingCacheZip					0xFFFF0007
#define rfbEncodingSolMonoZip				0xFFFF0008
#define rfbEncodingUltraZip				0xFFFF0009


// Same encoder number as in tight 
/*
#define rfbEncodingXCursor         0xFFFFFF10
#define rfbEncodingRichCursor      0xFFFFFF11
#define rfbEncodingNewFBSize       0xFFFFFF21
*/

/*
 *  Tight Special encoding numbers:
 *   0xFFFFFF00 .. 0xFFFFFF0F -- encoding-specific compression levels;
 *   0xFFFFFF10 .. 0xFFFFFF1F -- mouse cursor shape data;
 *   0xFFFFFF20 .. 0xFFFFFF2F -- various protocol extensions;
 *   0xFFFFFF30 .. 0xFFFFFFDF -- not allocated yet;
 *   0xFFFFFFE0 .. 0xFFFFFFEF -- quality level for JPEG compressor;
 *   0xFFFFFFF0 .. 0xFFFFFFFF -- cross-encoding compression levels.
 */

#define rfbEncodingCompressLevel0  0xFFFFFF00
#define rfbEncodingCompressLevel1  0xFFFFFF01
#define rfbEncodingCompressLevel2  0xFFFFFF02
#define rfbEncodingCompressLevel3  0xFFFFFF03
#define rfbEncodingCompressLevel4  0xFFFFFF04
#define rfbEncodingCompressLevel5  0xFFFFFF05
#define rfbEncodingCompressLevel6  0xFFFFFF06
#define rfbEncodingCompressLevel7  0xFFFFFF07
#define rfbEncodingCompressLevel8  0xFFFFFF08
#define rfbEncodingCompressLevel9  0xFFFFFF09

#define rfbEncodingXCursor         0xFFFFFF10
#define rfbEncodingRichCursor      0xFFFFFF11
#define rfbEncodingPointerPos      0xFFFFFF18
#define rfbEncodingLastRect        0xFFFFFF20
#define rfbEncodingNewFBSize       0xFFFFFF21
 
#define rfbEncodingQualityLevel0   0xFFFFFFE0
#define rfbEncodingQualityLevel1   0xFFFFFFE1
#define rfbEncodingQualityLevel2   0xFFFFFFE2
#define rfbEncodingQualityLevel3   0xFFFFFFE3
#define rfbEncodingQualityLevel4   0xFFFFFFE4
#define rfbEncodingQualityLevel5   0xFFFFFFE5
#define rfbEncodingQualityLevel6   0xFFFFFFE6
#define rfbEncodingQualityLevel7   0xFFFFFFE7
#define rfbEncodingQualityLevel8   0xFFFFFFE8
#define rfbEncodingQualityLevel9   0xFFFFFFE9



/*****************************************************************************
 *
 * Server -> client message definitions
 *
 *****************************************************************************/


/*-----------------------------------------------------------------------------
 * FramebufferUpdate - a block of rectangles to be copied to the framebuffer.
 *
 * This message consists of a header giving the number of rectangles of pixel
 * data followed by the rectangles themselves.  The header is padded so that
 * together with the type byte it is an exact multiple of 4 bytes (to help
 * with alignment of 32-bit pixels):
 */

typedef struct {
    CARD8 type;			/* always rfbFramebufferUpdate */
    CARD8 pad;
    CARD16 nRects;
    /* followed by nRects rectangles */
} rfbFramebufferUpdateMsg;

#define sz_rfbFramebufferUpdateMsg 4

#ifdef DSHOW
/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * KeyFrameUpdate - Acknowledgment of a key frame request, it tells the client
 * that the next update received will be a key frame.
 */

typedef struct {
    CARD8 type;
} rfbKeyFrameUpdateMsg;

#define sz_rfbKeyFrameUpdateMsg 1
#endif

/*
 * Each rectangle of pixel data consists of a header describing the position
 * and size of the rectangle and a type word describing the encoding of the
 * pixel data, followed finally by the pixel data.  Note that if the client has
 * not sent a SetEncodings message then it will only receive raw pixel data.
 * Also note again that this structure is a multiple of 4 bytes.
 */

typedef struct {
    rfbRectangle r;
    CARD32 encoding;	/* one of the encoding types rfbEncoding... */
} rfbFramebufferUpdateRectHeader;

#define sz_rfbFramebufferUpdateRectHeader (sz_rfbRectangle + 4)


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Raw Encoding.  Pixels are sent in top-to-bottom scanline order,
 * left-to-right within a scanline with no padding in between.
 */


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * CopyRect Encoding.  The pixels are specified simply by the x and y position
 * of the source rectangle.
 */

typedef struct {
    CARD16 srcX;
    CARD16 srcY;
} rfbCopyRect;

#define sz_rfbCopyRect 4


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * RRE - Rise-and-Run-length Encoding.  We have an rfbRREHeader structure
 * giving the number of subrectangles following.  Finally the data follows in
 * the form [<bgpixel><subrect><subrect>...] where each <subrect> is
 * [<pixel><rfbRectangle>].
 */

typedef struct {
    CARD32 nSubrects;
} rfbRREHeader;

#define sz_rfbRREHeader 4


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * CoRRE - Compact RRE Encoding.  We have an rfbRREHeader structure giving
 * the number of subrectangles following.  Finally the data follows in the form
 * [<bgpixel><subrect><subrect>...] where each <subrect> is
 * [<pixel><rfbCoRRERectangle>].  This means that
 * the whole rectangle must be at most 255x255 pixels.
 */

typedef struct {
    CARD8 x;
    CARD8 y;
    CARD8 w;
    CARD8 h;
} rfbCoRRERectangle;

#define sz_rfbCoRRERectangle 4


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Hextile Encoding.  The rectangle is divided up into "tiles" of 16x16 pixels,
 * starting at the top left going in left-to-right, top-to-bottom order.  If
 * the width of the rectangle is not an exact multiple of 16 then the width of
 * the last tile in each row will be correspondingly smaller.  Similarly if the
 * height is not an exact multiple of 16 then the height of each tile in the
 * final row will also be smaller.  Each tile begins with a "subencoding" type
 * byte, which is a mask made up of a number of bits.  If the Raw bit is set
 * then the other bits are irrelevant; w*h pixel values follow (where w and h
 * are the width and height of the tile).  Otherwise the tile is encoded in a
 * similar way to RRE, except that the position and size of each subrectangle
 * can be specified in just two bytes.  The other bits in the mask are as
 * follows:
 *
 * BackgroundSpecified - if set, a pixel value follows which specifies
 *    the background colour for this tile.  The first non-raw tile in a
 *    rectangle must have this bit set.  If this bit isn't set then the
 *    background is the same as the last tile.
 *
 * ForegroundSpecified - if set, a pixel value follows which specifies
 *    the foreground colour to be used for all subrectangles in this tile.
 *    If this bit is set then the SubrectsColoured bit must be zero.
 *
 * AnySubrects - if set, a single byte follows giving the number of
 *    subrectangles following.  If not set, there are no subrectangles (i.e.
 *    the whole tile is just solid background colour).
 *
 * SubrectsColoured - if set then each subrectangle is preceded by a pixel
 *    value giving the colour of that subrectangle.  If not set, all
 *    subrectangles are the same colour, the foreground colour;  if the
 *    ForegroundSpecified bit wasn't set then the foreground is the same as
 *    the last tile.
 *
 * The position and size of each subrectangle is specified in two bytes.  The
 * Pack macros below can be used to generate the two bytes from x, y, w, h,
 * and the Extract macros can be used to extract the x, y, w, h values from
 * the two bytes.
 */

#define rfbHextileRaw			(1 << 0)
#define rfbHextileBackgroundSpecified	(1 << 1)
#define rfbHextileForegroundSpecified	(1 << 2)
#define rfbHextileAnySubrects		(1 << 3)
#define rfbHextileSubrectsColoured	(1 << 4)

#define rfbHextilePackXY(x,y) (((x) << 4) | (y))
#define rfbHextilePackWH(w,h) ((((w)-1) << 4) | ((h)-1))
#define rfbHextileExtractX(byte) ((byte) >> 4)
#define rfbHextileExtractY(byte) ((byte) & 0xf)
#define rfbHextileExtractW(byte) (((byte) >> 4) + 1)
#define rfbHextileExtractH(byte) (((byte) & 0xf) + 1)

/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * ZLIB - zlib compression Encoding.  We have an rfbZlibHeader structure
 * giving the number of bytes to follow.  Finally the data follows in
 * zlib compressed format.
 */

typedef struct {
    CARD32 nBytes;
} rfbZlibHeader;

#define sz_rfbZlibHeader 4

/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * ZRLE - encoding combining Zlib compression, tiling, palettisation and
 * run-length encoding.
 */

typedef struct {
    CARD32 length;
} rfbZRLEHeader;

#define sz_rfbZRLEHeader 4

#define rfbZRLETileWidth 64
#define rfbZRLETileHeight 64


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Tight Encoding.  FIXME: Add more documentation.
 */

#define rfbTightExplicitFilter         0x04
#define rfbTightFill                   0x08
#define rfbTightJpeg                   0x09
#define rfbTightMaxSubencoding         0x09

/* Filters to improve compression efficiency */
#define rfbTightFilterCopy             0x00
#define rfbTightFilterPalette          0x01
#define rfbTightFilterGradient         0x02


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * ZLIBHEX - zlib compressed Hextile Encoding.  Essentially, this is the
 * hextile encoding with zlib compression on the tiles that can not be
 * efficiently encoded with one of the other hextile subencodings.  The
 * new zlib subencoding uses two bytes to specify the length of the
 * compressed tile and then the compressed data follows.  As with the
 * raw sub-encoding, the zlib subencoding invalidates the other
 * values, if they are also set.
 */

#define rfbHextileZlibRaw		(1 << 5)
#define rfbHextileZlibHex		(1 << 6)
#define rfbHextileZlibMono		(1 << 7)


/*-----------------------------------------------------------------------------
 * SetColourMapEntries - these messages are only sent if the pixel
 * format uses a "colour map" (i.e. trueColour false) and the client has not
 * fixed the entire colour map using FixColourMapEntries.  In addition they
 * will only start being sent after the client has sent its first
 * FramebufferUpdateRequest.  So if the client always tells the server to use
 * trueColour then it never needs to process this type of message.
 */

typedef struct {
    CARD8 type;			/* always rfbSetColourMapEntries */
    CARD8 pad;
    CARD16 firstColour;
    CARD16 nColours;

    /* Followed by nColours * 3 * CARD16
       r1, g1, b1, r2, g2, b2, r3, g3, b3, ..., rn, bn, gn */

} rfbSetColourMapEntriesMsg;

#define sz_rfbSetColourMapEntriesMsg 6


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * XCursor encoding. This is a special encoding used to transmit X-style
 * cursor shapes from server to clients. Note that for this encoding,
 * coordinates in rfbFramebufferUpdateRectHeader structure hold hotspot
 * position (r.x, r.y) and cursor size (r.w, r.h). If (w * h != 0), two RGB
 * samples are sent after header in the rfbXCursorColors structure. They
 * denote foreground and background colors of the cursor. If a client
 * supports only black-and-white cursors, it should ignore these colors and
 * assume that foreground is black and background is white. Next, two bitmaps
 * (1 bits per pixel) follow: first one with actual data (value 0 denotes
 * background color, value 1 denotes foreground color), second one with
 * transparency data (bits with zero value mean that these pixels are
 * transparent). Both bitmaps represent cursor data in a byte stream, from
 * left to right, from top to bottom, and each row is byte-aligned. Most
 * significant bits correspond to leftmost pixels. The number of bytes in
 * each row can be calculated as ((w + 7) / 8). If (w * h == 0), cursor
 * should be hidden (or default local cursor should be set by the client).
 */

typedef struct {
    CARD8 foreRed;
    CARD8 foreGreen;
    CARD8 foreBlue;
    CARD8 backRed;
    CARD8 backGreen;
    CARD8 backBlue;
} rfbXCursorColors;

#define sz_rfbXCursorColors 6
/*-----------------------------------------------------------------------------
 * Bell - ring a bell on the client if it has one.
 */

typedef struct {
    CARD8 type;			/* always rfbBell */
} rfbBellMsg;

#define sz_rfbBellMsg 1



/*-----------------------------------------------------------------------------
 * ServerCutText - the server has new text in its cut buffer.
 */

typedef struct {
    CARD8 type;			/* always rfbServerCutText */
    CARD8 pad1;
    CARD16 pad2;
    CARD32 length;
    /* followed by char text[length] */
} rfbServerCutTextMsg;

#define sz_rfbServerCutTextMsg 8



/*-----------------------------------------------------------------------------
 * // Modif sf@2002
 * FileTransferMsg - The client sends FileTransfer message.
 * Bidirectional message - Files can be sent from client to server & vice versa
 */

typedef struct _rfbFileTransferMsg {
    CARD8 type;			/* always rfbFileTransfer */
    CARD8 contentType;  // See defines below
    CARD16 contentParam;// Other possible content classification (Dir or File name, etc..)
	CARD32 size;		// FileSize or packet index or error or other 
	// CARD32 sizeH;		// Additional 32Bits params to handle big values. Only for V2 (we want backward compatibility between all V1 versions)
    CARD32 length;
    /* followed by data char text[length] */
} rfbFileTransferMsg;

#define sz_rfbFileTransferMsg	12

#define rfbFileTransferVersion  2 // v1 is the old FT version ( <= 1.0.0 RC18 versions)

// FileTransfer Content types and Params defines
#define rfbDirContentRequest	1 // Client asks for the content of a given Server directory
#define rfbDirPacket			2 // Full directory name or full file name.
								  // Null content means end of Directory
#define rfbFileTransferRequest	3 // Client asks the server for the transfer of a given file
#define rfbFileHeader			4 // First packet of a file transfer, containing file's features
#define rfbFilePacket			5 // One chunk of the file
#define rfbEndOfFile			6 // End of file transfer (the file has been received or error)
#define rfbAbortFileTransfer	7 // The file transfer must be aborted, whatever the state
#define rfbFileTransferOffer	8 // The client offers to send a file to the server
#define rfbFileAcceptHeader		9 // The server accepts or rejects the file
#define rfbCommand				10 // The Client sends a simple command (File Delete, Dir create etc...)
#define rfbCommandReturn		11 // The Client receives the server's answer about a simple command
#define rfbFileChecksums		12 // The zipped checksums of the destination file (Delta Transfer)
#define rfbFileTransferAccess	14 // Request FileTransfer authorization

								// rfbDirContentRequest client Request - content params 
#define rfbRDirContent			1 // Request a Server Directory contents
#define rfbRDrivesList			2 // Request the server's drives list
#define rfbRDirRecursiveList	3 // Request a server directory content recursive sorted list
#define rfbRDirRecursiveSize	4 // Request a server directory content recursive size

								// rfbDirPacket & rfbCommandReturn  server Answer - content params
#define rfbADirectory			1 // Reception of a directory name
#define rfbAFile				2 // Reception of a file name 
#define rfbADrivesList			3 // Reception of a list of drives
#define rfbADirCreate			4 // Response to a create dir command 
#define rfbADirDelete			5 // Response to a delete dir command 
#define rfbAFileCreate			6 // Response to a create file command 
#define rfbAFileDelete			7 // Response to a delete file command 
#define rfbAFileRename			8 // Response to a rename file command 
#define rfbADirRename			9 // Response to a rename dir command 
#define rfbADirRecursiveListItem	10 
#define rfbADirRecursiveSize		11 

								// rfbCommand Command - content params
#define rfbCDirCreate			1 // Request the server to create the given directory
#define rfbCDirDelete			2 // Request the server to delete the given directory
#define rfbCFileCreate			3 // Request the server to create the given file
#define rfbCFileDelete			4 // Request the server to delete the given file
#define rfbCFileRename			5 // Request the server to rename the given file 
#define rfbCDirRename			6 // Request the server to rename the given directory

								// Errors - content params or "size" field
#define rfbRErrorUnknownCmd     1  // Unknown FileTransfer command.
#define rfbRErrorCmd			0xFFFFFFFF// Error when a command fails on remote side (ret in "size" field)

#define sz_rfbBlockSize			8192  // Size of a File Transfer packet (before compression)
#define rfbZipDirectoryPrefix   "!UVNCDIR-\0" // Transfered directory are zipped in a file with this prefix. Must end with "-"
#define sz_rfbZipDirectoryPrefix 9 
#define rfbDirPrefix			"[ "
#define rfbDirSuffix			" ]"		



/*-----------------------------------------------------------------------------
 * Modif sf@2002
 * TextChatMsg - Utilized to order the TextChat mode on server or client
 * Bidirectional message
 */

typedef struct _rfbTextChatMsg {
    CARD8 type;			/* always rfbTextChat */
    CARD8 pad1;         // Could be used later as an additionnal param
    CARD16 pad2;		// Could be used later as text offset, for instance
    CARD32 length;      // Specific values for Open, close, finished (-1, -2, -3)
    /* followed by char text[length] */
} rfbTextChatMsg;

#define sz_rfbTextChatMsg 8

#define rfbTextMaxSize		4096
#define rfbTextChatOpen		0xFFFFFFFF 
#define rfbTextChatClose	0xFFFFFFFE  
#define rfbTextChatFinished 0xFFFFFFFD  



/*-----------------------------------------------------------------------------
 * Modif sf@2002
 * ResizeFrameBuffer - The Client must change the size of its framebuffer  
 */

typedef struct _rfbResizeFrameBufferMsg {
    CARD8 type;			/* always rfbResizeFrameBuffer */
	CARD8 pad1;
	CARD16 framebufferWidth;	// FrameBuffer width
	CARD16 framebufferHeigth;	// FrameBuffer height
} rfbResizeFrameBufferMsg;

#define sz_rfbResizeFrameBufferMsg 6


/*-----------------------------------------------------------------------------
 * Copyright (C) 2001 Harakan Software
 * PalmVNC 1.4 & 2.? ResizeFrameBuffer message
 * ReSizeFrameBuffer - tell the RFB client to alter its framebuffer, either
 * due to a resize of the server desktop or a client-requested scaling factor.
 * The pixel format remains unchanged.
 */

typedef struct {
    CARD8 type;			/* always rfbReSizeFrameBuffer */
	CARD8 pad1;
	CARD16 desktop_w;	/* Desktop width */
	CARD16 desktop_h;	/* Desktop height */
	CARD16 buffer_w;	/* FrameBuffer width */
	CARD16 buffer_h;	/* Framebuffer height */
    CARD16 pad2;

} rfbPalmVNCReSizeFrameBufferMsg;

#define sz_rfbPalmVNCReSizeFrameBufferMsg (12)




/*-----------------------------------------------------------------------------
 * Union of all server->client messages.
 */

typedef union {
    CARD8 type;
    rfbFramebufferUpdateMsg fu;
    rfbSetColourMapEntriesMsg scme;
    rfbBellMsg b;
    rfbServerCutTextMsg sct;
	rfbResizeFrameBufferMsg rsfb;
	rfbPalmVNCReSizeFrameBufferMsg prsfb; 
	rfbFileTransferMsg ft;
	rfbTextChatMsg tc;
} rfbServerToClientMsg;


/*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * RDV Cache Encoding.  
 * special is not used at this point, can be used to reset cache or other specials
 * just put it to make sure we don't have to change the encoding again.  
 */

typedef struct {
    CARD16 special;
} rfbCacheRect;

#define sz_rfbCacheRect 2





/*****************************************************************************
 *
 * Message definitions (client -> server)
 *
 *****************************************************************************/


/*-----------------------------------------------------------------------------
 * SetPixelFormat - tell the RFB server the format in which the client wants
 * pixels sent.
 */

typedef struct {
    CARD8 type;			/* always rfbSetPixelFormat */
    CARD8 pad1;
    CARD16 pad2;
    rfbPixelFormat format;
} rfbSetPixelFormatMsg;

#define sz_rfbSetPixelFormatMsg (sz_rfbPixelFormat + 4)


/*-----------------------------------------------------------------------------
 * FixColourMapEntries - when the pixel format uses a "colour map", fix
 * read-only colour map entries.
 *
 *    ***************** NOT CURRENTLY SUPPORTED *****************
 */

typedef struct {
    CARD8 type;			/* always rfbFixColourMapEntries */
    CARD8 pad;
    CARD16 firstColour;
    CARD16 nColours;

    /* Followed by nColours * 3 * CARD16
       r1, g1, b1, r2, g2, b2, r3, g3, b3, ..., rn, bn, gn */

} rfbFixColourMapEntriesMsg;

#define sz_rfbFixColourMapEntriesMsg 6


/*-----------------------------------------------------------------------------
 * SetEncodings - tell the RFB server which encoding types we accept.  Put them
 * in order of preference, if we have any.  We may always receive raw
 * encoding, even if we don't specify it here.
 */

typedef struct {
    CARD8 type;			/* always rfbSetEncodings */
    CARD8 pad;
    CARD16 nEncodings;
    /* followed by nEncodings * CARD32 encoding types */
} rfbSetEncodingsMsg;

#define sz_rfbSetEncodingsMsg 4


/*-----------------------------------------------------------------------------
 * FramebufferUpdateRequest - request for a framebuffer update.  If incremental
 * is true then the client just wants the changes since the last update.  If
 * false then it wants the whole of the specified rectangle.
 */

typedef struct {
    CARD8 type;			/* always rfbFramebufferUpdateRequest */
    CARD8 incremental;
    CARD16 x;
    CARD16 y;
    CARD16 w;
    CARD16 h;
} rfbFramebufferUpdateRequestMsg;

#define sz_rfbFramebufferUpdateRequestMsg 10


/*-----------------------------------------------------------------------------
 * KeyEvent - key press or release
 *
 * Keys are specified using the "keysym" values defined by the X Window System.
 * For most ordinary keys, the keysym is the same as the corresponding ASCII
 * value.  Other common keys are:
 *
 * BackSpace		0xff08
 * Tab			0xff09
 * Return or Enter	0xff0d
 * Escape		0xff1b
 * Insert		0xff63
 * Delete		0xffff
 * Home			0xff50
 * End			0xff57
 * Page Up		0xff55
 * Page Down		0xff56
 * Left			0xff51
 * Up			0xff52
 * Right		0xff53
 * Down			0xff54
 * F1			0xffbe
 * F2			0xffbf
 * ...			...
 * F12			0xffc9
 * Shift		0xffe1
 * Control		0xffe3
 * Meta			0xffe7
 * Alt			0xffe9
 */

typedef struct {
    CARD8 type;			/* always rfbKeyEvent */
    CARD8 down;			/* true if down (press), false if up */
    CARD16 pad;
    CARD32 key;			/* key is specified as an X keysym */
} rfbKeyEventMsg;

#define sz_rfbKeyEventMsg 8


/*-----------------------------------------------------------------------------
 * PointerEvent - mouse/pen move and/or button press.
 */

typedef struct {
    CARD8 type;			/* always rfbPointerEvent */
    CARD8 buttonMask;		/* bits 0-7 are buttons 1-8, 0=up, 1=down */
    CARD16 x;
    CARD16 y;
} rfbPointerEventMsg;

#define rfbButton1Mask 1
#define rfbButton2Mask 2
#define rfbButton3Mask 4
#define rfbButton4Mask 8
#define rfbButton5Mask 16
#define rfbWheelUpMask rfbButton4Mask    // RealVNC 335 method
#define rfbWheelDownMask rfbButton5Mask

#define sz_rfbPointerEventMsg 6



/*-----------------------------------------------------------------------------
 * ClientCutText - the client has new text in its cut buffer.
 */

typedef struct {
    CARD8 type;			/* always rfbClientCutText */
    CARD8 pad1;
    CARD16 pad2;
    CARD32 length;
    /* followed by char text[length] */
} rfbClientCutTextMsg;

#define sz_rfbClientCutTextMsg 8


/*-----------------------------------------------------------------------------
 * sf@2002 - Set Server Scale
 * SetServerScale - Server must change the scale of the client buffer.
 */

typedef struct _rfbSetScaleMsg {
    CARD8 type;			/* always rfbSetScale */
    CARD8 scale;		/* Scale value 1<sv<n */
    CARD16 pad;
} rfbSetScaleMsg;

#define sz_rfbSetScaleMsg 4


/*-----------------------------------------------------------------------------
 * Copyright (C) 2001 Harakan Software
 * PalmVNC 1.4 & 2.? SetScale Factor message 
 * SetScaleFactor - tell the RFB server to alter the scale factor for the
 * client buffer.
 */
typedef struct {
    CARD8 type;			/* always rfbSetScaleFactor */

    CARD8 scale;		/* Scale factor (positive non-zero integer) */
    CARD16 pad2;
} rfbPalmVNCSetScaleFactorMsg;

#define sz_rfbPalmVNCSetScaleFactorMsg (4)


/*-----------------------------------------------------------------------------
 * rdv@2002 - Set input status
 * SetServerInput - Server input is dis/enabled
 */

typedef struct _rfbSetServerInputMsg {
    CARD8 type;			/* always rfbSetScale */
    CARD8 status;		/* Scale value 1<sv<n */
    CARD16 pad;
} rfbSetServerInputMsg;

#define sz_rfbSetServerInputMsg 4

/*-----------------------------------------------------------------------------
 * rdv@2002 - Set SW
 * SetSW - Server SW/full desktop
 */

typedef struct _rfbSetSWMsg {
    CARD8 type;			/* always rfbSetSW */
    CARD8 status;		
    CARD16 x;
    CARD16 y;
} rfbSetSWMsg;

#define sz_rfbSetSWMsg 6


/*-----------------------------------------------------------------------------
 * Union of all client->server messages.
 */

typedef union {
    CARD8 type;
    rfbSetPixelFormatMsg spf;
    rfbFixColourMapEntriesMsg fcme;
    rfbSetEncodingsMsg se;
    rfbFramebufferUpdateRequestMsg fur;
    rfbKeyEventMsg ke;
    rfbPointerEventMsg pe;
    rfbClientCutTextMsg cct;
	rfbSetScaleMsg ssc;
	rfbPalmVNCSetScaleFactorMsg pssf;
	rfbSetServerInputMsg sim;
	rfbFileTransferMsg ft;
	rfbSetSWMsg sw;
	rfbTextChatMsg tc;
} rfbClientToServerMsg;
