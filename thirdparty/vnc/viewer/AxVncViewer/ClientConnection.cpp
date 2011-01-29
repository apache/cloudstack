//  Copyright (C) 2002 Ultr@VNC Team Members. All Rights Reserved.
//
//  Copyright (C) 2000-2002 Const Kaplinsky. All Rights Reserved.
//
//  Copyright (C) 2002 RealVNC Ltd. All Rights Reserved.
//
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
// whence you received this file, check http://www.uk.research.att.com/vnc or contact
// the authors on vnc@uk.research.att.com for information on obtaining it.


// Many thanks to Randy Brown <rgb@inven.com> for providing the 3-button
// emulation code.

// This is the main source for a ClientConnection object.
// It handles almost everything to do with a connection to a server.
// The decoding of specific rectangle encodings is done in separate files.


#define _WIN32_WINDOWS 0x0410
#define WINVER 0x0400

#include "stdhdrs.h"

#include "vncviewer.h"

#ifdef UNDER_CE
#include "omnithreadce.h"
#define SD_BOTH 0x02
#else
#include "omnithread.h"
#endif

#include "ClientConnection.h"
#include "SessionDialog.h"
#include "AuthDialog.h"
#include "AboutBox.h"
#include "LowLevelHook.h"

#include "Exception.h"
extern "C" {
	#include "vncauth.h"
}

#include "rdr/FdInStream.h"
#include "rdr/ZlibInStream.h"
#include "rdr/Exception.h"

#include "rfb/dh.h"

#include "DSMPlugin/DSMPlugin.h" // sf@200

#include "Log.h"
extern Log vnclog;

#define INITIALNETBUFSIZE 4096
#define MAX_ENCODINGS (LASTENCODING+10)
#define VWR_WND_CLASS_NAME _T("VNCviewer")
#define VWR_WND_CLASS_NAME_VIEWER _T("VNCviewerwindow")
#define SESSION_MRU_KEY_NAME _T("Software\\ORL\\VNCviewer\\MRU")

const UINT FileTransferSendPacketMessage = RegisterWindowMessage("UltraVNC.Viewer.FileTransferSendPacketMessage");
extern bool g_passwordfailed;


#ifdef _ULTRAVNCAX_
static HWND GetTopMostWnd( HWND h )
{
	if ( h == NULL )
		return NULL;

	while( 1 )
	{
		HWND prev = h;
		h = GetParent( h );
		if ( h == NULL )
			return prev;
	}
}
#endif

/*
 * Macro to compare pixel formats.
 */

#define PF_EQ(x,y)							\
	((x.bitsPerPixel == y.bitsPerPixel) &&				\
	 (x.depth == y.depth) &&					\
	 ((x.bigEndian == y.bigEndian) || (x.bitsPerPixel == 8)) &&	\
	 (x.trueColour == y.trueColour) &&				\
	 (!x.trueColour || ((x.redMax == y.redMax) &&			\
			    (x.greenMax == y.greenMax) &&		\
			    (x.blueMax == y.blueMax) &&			\
			    (x.redShift == y.redShift) &&		\
			    (x.greenShift == y.greenShift) &&		\
			    (x.blueShift == y.blueShift))))

const rfbPixelFormat vnc8bitFormat			= {8,8,0,1,7,7,3,0,3,6, 0, 0}; // 256 colors
const rfbPixelFormat vnc8bitFormat_64		= {8,6,0,1,3,3,3,4,2,0, 0, 0} ;	// 64 colors
const rfbPixelFormat vnc8bitFormat_8		= {8,3,0,1,1,1,1,2,1,0, 0, 0} ;	// 8 colors
const rfbPixelFormat vnc8bitFormat_8Grey	= {8,8,0,1,7,7,3,0,3,6, 1, 0} ;	// 8 colors-Dark Scale
const rfbPixelFormat vnc8bitFormat_4Grey	= {8,6,0,1,3,3,3,4,2,0, 1, 0} ;	// 4 colors-Grey Scale
const rfbPixelFormat vnc8bitFormat_2Grey	= {8,3,0,1,1,1,1,2,1,0, 1, 0} ;	// 2 colors-Grey Scale

const rfbPixelFormat vnc16bitFormat			= {16,16,0,1,63,31,31,0,6,11, 0, 0};

//static LRESULT CALLBACK ClientConnection::WndProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam);
extern HWND currentHWND;
extern char sz_L1[64];
extern char sz_L2[64];
extern char sz_L3[64];
extern char sz_L4[64];
extern char sz_L5[64];
extern char sz_L6[64];
extern char sz_L7[64];
extern char sz_L8[64];
extern char sz_L9[64];
extern char sz_L10[64];
extern char sz_L11[64];
extern char sz_L12[64];
extern char sz_L13[64];
extern char sz_L14[64];
extern char sz_L15[64];
extern char sz_L16[64];
extern char sz_L17[64];
extern char sz_L18[64];
extern char sz_L19[64];
extern char sz_L20[64];
extern char sz_L21[64];
extern char sz_L22[64];
extern char sz_L23[64];
extern char sz_L24[64];
extern char sz_L25[64];
extern char sz_L26[64];
extern char sz_L27[64];
extern char sz_L28[64];
extern char sz_L29[64];
extern char sz_L30[64];
extern char sz_L31[64];
extern char sz_L32[64];
extern char sz_L33[64];
extern char sz_L34[64];
extern char sz_L35[64];
extern char sz_L36[64];
extern char sz_L37[64];
extern char sz_L38[64];
extern char sz_L39[64];
extern char sz_L40[64];
extern char sz_L41[64];
extern char sz_L42[64];
extern char sz_L43[64];
extern char sz_L44[64];
extern char sz_L45[64];
extern char sz_L46[64];
extern char sz_L47[64];
extern char sz_L48[64];
extern char sz_L49[64];
extern char sz_L50[64];
extern char sz_L51[64];
extern char sz_L52[64];
extern char sz_L53[64];
extern char sz_L54[64];
extern char sz_L55[64];
extern char sz_L56[64];
extern char sz_L57[64];
extern char sz_L58[64];
extern char sz_L59[64];
extern char sz_L60[64];
extern char sz_L61[64];
extern char sz_L62[64];
extern char sz_L63[64];
extern char sz_L64[64];
extern char sz_L65[64];
extern char sz_L66[64];
extern char sz_L67[64];
extern char sz_L68[64];
extern char sz_L69[64];
extern char sz_L70[64];
extern char sz_L71[64];
extern char sz_L72[64];
extern char sz_L73[64];
extern char sz_L74[64];
extern char sz_L75[64];
extern char sz_L76[64];
extern char sz_L77[64];
extern char sz_L78[64];
extern char sz_L79[64];
extern char sz_L80[64];
extern char sz_L81[64];
extern char sz_L82[64];
extern char sz_L83[64];
extern char sz_L84[64];
extern char sz_L85[64];
extern char sz_L86[64];
extern char sz_L87[64];
extern char sz_L88[64];
extern char sz_L89[64];
extern char sz_L90[64];
extern char sz_L91[64];
extern char sz_L92[64];

extern char sz_F1[64];
extern char sz_F5[128];
extern char sz_F6[64];
extern bool command_line;



// *************************************************************************
//  A Client connection involves two threads - the main one which sets up
//  connections and processes window messages and inputs, and a 
//  client-specific one which receives, decodes and draws output data 
//  from the remote server.
//  This first section contains bits which are generally called by the main
//  program thread.
// *************************************************************************

ClientConnection::ClientConnection(VNCviewerApp *pApp) 
  : fis(0), zis(0)
{
	Init(pApp);
	memset(m_strUserName, 0, sizeof(m_strUserName));
	memset(m_strPassword, 0, sizeof(m_strPassword));
}

ClientConnection::ClientConnection(VNCviewerApp *pApp, SOCKET sock) 
  : fis(0), zis(0)
{
	Init(pApp);
	memset(m_strUserName, 0, sizeof(m_strUserName));
	memset(m_strPassword, 0, sizeof(m_strPassword));

    if (m_opts.autoDetect)
	{
      m_opts.m_Use8Bit = rfbPF256Colors; //true;
	  m_opts.m_fEnableCache = true; // sf@2002
	}
	m_sock = sock;
	m_serverInitiated = true;
	struct sockaddr_in svraddr;
	int sasize = sizeof(svraddr);
	if (getpeername(sock, (struct sockaddr *) &svraddr, 
		&sasize) != SOCKET_ERROR) {
		_stprintf(m_host, _T("%d.%d.%d.%d"), 
			svraddr.sin_addr.S_un.S_un_b.s_b1, 
			svraddr.sin_addr.S_un.S_un_b.s_b2, 
			svraddr.sin_addr.S_un.S_un_b.s_b3, 
			svraddr.sin_addr.S_un.S_un_b.s_b4);
		m_port = svraddr.sin_port;
	} else {
		_tcscpy(m_host,sz_L1);
		m_port = 0;
	};
}

ClientConnection::ClientConnection(VNCviewerApp *pApp, LPTSTR host, int port, LPTSTR pszUser, LPTSTR pszPassword, LPTSTR pszProxy,
								   HWND rootHwnd, HWND* pHwndAppFrame)
  : fis(0), zis(0)
{
	Init(pApp);
	memset(m_strUserName, 0, sizeof(m_strUserName));
	memset(m_strPassword, 0, sizeof(m_strPassword));

#ifdef _ULTRAVNCAX_
	m_hwndAx = rootHwnd;
	m_pHwndAppFrame = pHwndAppFrame;
#endif

    if (m_opts.autoDetect)
	{
		m_opts.m_Use8Bit = rfbPF256Colors; //true;
		m_opts.m_fEnableCache = true; // sf@2002
	}
	_tcsncpy(m_host, host, MAX_HOST_NAME_LEN);
	m_port = port;


	if (pszProxy && strlen(pszProxy) > 0) {
		::ParseDisplay((CHAR*)pszProxy, m_proxyhost, MAX_HOST_NAME_LEN-1, &m_proxyport);
		m_fUseProxy = true;
	} else {
		m_fUseProxy = false;
	}

	if(pszUser != NULL)
		strncpy(m_strUserName, pszUser, sizeof(m_strUserName) - 1);;

	if(pszPassword != NULL)
		strncpy(m_strPassword, pszPassword, sizeof(m_strPassword));
}

void ClientConnection::Init(VNCviewerApp *pApp)
{
	Pressed_Cancel=false;
	saved_set=false;
	m_hwnd = 0;
	m_desktopName = NULL;
	m_port = -1;
	m_proxyport = -1;
//	m_proxy = 0;
	m_serverInitiated = false;
	m_netbuf = NULL;
	m_netbufsize = 0;
	m_zlibbuf = NULL;
	m_zlibbufsize = 0;
	m_decompStreamInited = false;
	m_hwndNextViewer = NULL;	
	m_pApp = pApp;
	m_dormant = false;
	m_hBitmapDC = NULL;
	m_hBitmap = NULL;
	m_hCacheBitmapDC = NULL;
	m_hCacheBitmap = NULL;
	m_hPalette = NULL;
	m_encPasswd[0] = '\0';
	m_clearPasswd[0] = '\0'; // Modif sf@2002
	// static window
	m_BytesSend=0;
	m_BytesRead=0;

	// We take the initial conn options from the application defaults
	m_opts = m_pApp->m_options;

	m_sock = INVALID_SOCKET;
	m_bKillThread = false;
#ifdef _ULTRAVNCAX_
	m_threadStarted = false; // BUG ?
#else
	m_threadStarted = true;
#endif
	m_running = false;
	m_pendingFormatChange = false;

	// sf@2002 - v1.1.2 - Data Stream Modification Plugin handling
	m_nTO = 1;
	m_pDSMPlugin = new CDSMPlugin();
	m_fUsePlugin = false;
	m_fUseProxy = false;
	m_pNetRectBuf = NULL;
	m_fReadFromNetRectBuf = false;  // 
	m_nNetRectBufOffset = 0;
	m_nReadSize = 0;
	m_nNetRectBufSize = 0;
	m_pZRLENetRectBuf = NULL;
	m_fReadFromZRLENetRectBuf = false;  // 
	m_nZRLENetRectBufOffset = 0;
	m_nZRLEReadSize = 0;
	m_nZRLENetRectBufSize = 0;

	// ZlibHex
	m_decompStreamInited = false;
	m_decompStreamRaw.total_in = ZLIBHEX_DECOMP_UNINITED;
	m_decompStreamEncoded.total_in = ZLIBHEX_DECOMP_UNINITED;

	// Initialise a few fields that will be properly set when the
	// connection has been negotiated
	m_fullwinwidth = m_fullwinheight = 0;
	m_si.framebufferWidth = m_si.framebufferHeight = 0;

	m_hScrollPos = 0; m_vScrollPos = 0;

	m_waitingOnEmulateTimer = false;
	m_emulatingMiddleButton = false;

    oldPointerX = oldPointerY = oldButtonMask = 0;

	// Create a buffer for various network operations
	CheckBufferSize(INITIALNETBUFSIZE);

	m_pApp->RegisterConnection(this);

    kbitsPerSecond = 0;
	m_lLastChangeTime = 0; // 0 because we want the first encoding switching to occur quickly
	                     // (in Auto mode, ZRLE is used: pointless over a LAN)

	m_fScalingDone = false;

    zis = new rdr::ZlibInStream;

	// tight cusorhandling
	prevCursorSet = false;
	rcCursorX = 0;
	rcCursorY = 0;

	// Modif sf@2002 - FileTransfer
	m_pFileTransfer = new FileTransfer(m_pApp, this); 	
	m_filezipbuf = NULL;
	m_filezipbufsize = 0;
	m_filechunkbuf = NULL;
	m_filechunkbufsize = 0;

	// Modif sf@2002 - Text Chat
	m_pTextChat = new TextChat(m_pApp, this); 	

	// Modif sf@2002 - Scaling
	m_pendingScaleChange = false;
	m_pendingCacheInit = false;
	m_nServerScale = 1;

	//ms logon
	m_ms_logon=false;

	// sf@2002 - FileTransfer on server
	m_fServerKnowsFileTransfer = false;

	// Auto Mode
	m_nConfig = 0;

	// sf@2002 - Options window flag
	m_fOptionsOpen = false;

	// Tight encoding
	for (int i = 0; i < 4; i++)
		m_tightZlibStreamActive[i] = false;

	m_hwnd=NULL;
	m_hbands=NULL;
	m_hwndTB=NULL;
	m_hwndTBwin=NULL;
	m_hwndMain=NULL;
	m_hwndStatus=NULL;
	m_TrafficMonitor=NULL;
	m_logo_wnd=NULL;
	m_button_wnd=NULL;
	// m_ToolbarEnable=true;
	m_remote_mouse_disable=false;
	m_SWselect=false;

	EncodingStatusWindow = -1;
	OldEncodingStatusWindow = -2;

	m_nStatusTimer = 0;
//	m_FTtimer = 0;
	skipprompt2=true;
//	flash=NULL;
	// UltraFast
	m_hmemdc=NULL;
	m_DIBbits=NULL;
	m_membitmap=NULL;
	m_BigToolbar=false;
	strcpy(m_proxyhost,"");
	KillEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
	newtick=0;
	oldtick=0;

	m_zipbuf=NULL;
	m_filezipbuf=NULL;
	m_filechunkbuf=NULL;
	m_zlibbuf=NULL; 
	rcSource=NULL;
	rcMask=NULL;
}

// 
// Run() creates the connection if necessary, does the initial negotiations
// and then starts the thread running which does the output (update) processing.
// If Run throws an Exception, the caller must delete the ClientConnection object.
//

void ClientConnection::Run()
{
	// Get the host name and port if we haven't got it
	if (m_port == -1) 
	{
		GetConnectDetails();
		// sf@2002 - DSM Plugin loading if required
		LoadDSMPlugin();
	}
	else
	{
		LoadDSMPlugin();
		// sf@2003 - Take command line quickoption into account
		HandleQuickOption();
	}

	// Modif sf@2003 - In case Auto Mode is On with DSMPlugin, we disable ZRLE right now !
	/*
	if (m_opts.autoDetect)
	{
		if (m_pDSMPlugin->IsEnabled())
		{
			m_opts.m_PreferredEncoding = rfbEncodingTight;
			m_opts.m_fEnableCache = false; // Cache does not work perfectly with Tight
			m_opts.m_Use8Bit = true; // full colors as Tight is going to last at least 30s...
			m_lLastChangeTime = timeGetTime(); // defer the first possible Auto encoding switching in 30s
		}
	}
	*/

	// Modif sf@2002 - bit of a hack...and unsafe
	if (strlen(	m_pApp->m_options.m_clearPassword) > 0) 
		strcpy(m_clearPasswd, m_pApp->m_options.m_clearPassword);

	if (saved_set)
	{
		saved_set=FALSE;
		Save_Latest_Connection();
	}
	// Connect if we're not already connected
	if (m_sock == INVALID_SOCKET) 
		if (strcmp(m_proxyhost,"")!=NULL && m_fUseProxy)ConnectProxy();
		else Connect();

	SetSocketOptions();

	SetDSMPluginStuff(); // The Plugin is now activated BEFORE the protocol negociation 
						 // so ALL the communication data travel through the DSMPlugin
	if (strcmp(m_proxyhost,"")!=NULL && m_fUseProxy)
		NegotiateProxy();
	NegotiateProtocolVersion();
	Authenticate();
	

	GTGBS_CreateDisplay();

	GTGBS_CreateToolbar();

	// Set up windows etc 
	CreateDisplay();

//	if (flash) {flash->Killflash();}
	SendClientInit();
	
	ReadServerInit();
	
	CreateLocalFramebuffer();

	SetupPixelFormat();

	Createdib();

    SetFormatAndEncodings();

	SizeWindow();
       
	// This starts the worker thread.
	// The rest of the processing continues in run_undetached.
#ifndef _ULTRAVNCAX_
	LowLevelHook::Initialize(m_hwndMain);
#endif
	start_undetached();
	
	EndDialog(m_hwndStatus,0);
}

HWND ClientConnection::GTGBS_ShowConnectWindow()
{
	DWORD				  threadID;
	m_statusThread = CreateThread(NULL,0,(LPTHREAD_START_ROUTINE )ClientConnection::GTGBS_ShowStatusWindow,(LPVOID)this,0,&threadID);
	ResumeThread(m_statusThread);
	return (HWND)0;
}

////////////////////////////////////////////////////////
#include <commctrl.h>
#include <shellapi.h>
#include <lmaccess.h>
#include <lmat.h>
#include <lmalert.h>

void ClientConnection::CreateButtons(BOOL mini,BOOL ultra)
{
	if (ultra)	  
	{
		int nr_buttons = 14;
		TBADDBITMAP tbab; 
		TBBUTTON tbButtons []=
		{
			{0,ID_BUTTON_CAD,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,0},
			{1,ID_BUTTON_FULLSCREEN,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,1},
			{2,ID_BUTTON_PROPERTIES,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,2},
			{3,ID_BUTTON_REFRESH,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,3},
			{4,ID_BUTTON_STRG_ESC,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,4},
			{5,ID_BUTTON_SEP,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,5},
			{6,ID_BUTTON_INFO,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,6},
			{7,ID_BUTTON_END,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,7},
			{8,ID_BUTTON_DBUTTON,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,8},
			{9,ID_BUTTON_DINPUT,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,9},
			{10,ID_BUTTON_FTRANS,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,10},
			{11,ID_BUTTON_SW,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,11},
			{12,ID_BUTTON_DESKTOP,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,12},
			{13,ID_BUTTON_TEXTCHAT,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,13},
			
		};
		static char *szTips[14] = 
		{
				sz_L2,
				sz_L3,
				sz_L4,
				sz_L5,
				sz_L6,
				sz_L7,
				sz_L8,
				sz_L9,
				sz_L10,
				sz_L11,
				sz_L12,
				sz_L13,
				sz_L14,
				sz_L15,
		};
		int stdidx;
		HWND m_hwndTT;
		UINT buttonmap,minibuttonmap;
		int row,col;
		TOOLINFO ti;
		int id=0;
		RECT clr;
		InitCommonControls();
		GetClientRect(m_hwndMain,&clr);
		m_TBr.left=0;
		m_TBr.right=clr.right;
		m_TBr.top=0;
		m_TBr.bottom=28;
		buttonmap=IDB_BITMAP1;
		minibuttonmap=IDB_BITMAP7;
		if (m_remote_mouse_disable)
					{
						buttonmap=IDB_BITMAP8;
						minibuttonmap=IDB_BITMAP9;
					}
		if (mini)
		{
			m_hwndTB = CreateToolbarEx(
				m_hwndTBwin
				//,WS_CHILD|WS_BORDER|WS_VISIBLE|TBSTYLE_TOOLTIPS|TBSTYLE_WRAPABLE|TB_AUTOSIZE 
				,WS_CHILD | TBSTYLE_WRAPABLE | WS_VISIBLE |TBSTYLE_TOOLTIPS |CCS_NORESIZE| TBSTYLE_FLAT | TBSTYLE_TRANSPARENT 
				,IDR_TOOLBAR
				,nr_buttons
				,(HINSTANCE)m_pApp->m_instance
				,minibuttonmap
				,(LPCTBBUTTON)&tbButtons
				,nr_buttons
				,10
				,10
				,10
				,10
				,sizeof(TBBUTTON));
		}
		else
		{
			m_hwndTB = CreateToolbarEx(
				m_hwndTBwin
				//,WS_CHILD|WS_BORDER|WS_VISIBLE|TBSTYLE_TOOLTIPS|TBSTYLE_WRAPABLE|TB_AUTOSIZE 
				,WS_CHILD | TBSTYLE_WRAPABLE | WS_VISIBLE |TBSTYLE_TOOLTIPS |CCS_NORESIZE | TBSTYLE_FLAT | TBSTYLE_TRANSPARENT 
				,IDR_TOOLBAR
				,nr_buttons
				,(HINSTANCE)m_pApp->m_instance
				,buttonmap
				,(LPCTBBUTTON)&tbButtons
				,nr_buttons
				,20
				,20
				,20
				,20
				,sizeof(TBBUTTON));
		}
		
		tbab.hInst = m_pApp->m_instance;
		tbab.nID = IDB_BITMAP1;
		stdidx = SendMessage(m_hwndTB,TB_ADDBITMAP,6,(LPARAM)&tbab);
		RECT tbrect;
		RECT wrect;
		RECT trect;
		SendMessage(m_hwndTB,TB_SETROWS,(WPARAM) MAKEWPARAM (2, true),(LPARAM) (LPRECT) (&trect));
		
		GetClientRect(m_hwndTB,&tbrect);
		GetClientRect(m_hwndTBwin,&wrect);
		
		
		m_hwndTT = CreateWindow(
			TOOLTIPS_CLASS,
			(LPSTR)NULL,
			TTS_ALWAYSTIP,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			NULL,
			(HMENU)NULL,
			(HINSTANCE)m_pApp->m_instance,
			NULL);
		
		DWORD buttonWidth = LOWORD(SendMessage(m_hwndTB,TB_GETBUTTONSIZE,(WPARAM)0,(LPARAM)0));
		DWORD buttonHeight = HIWORD(SendMessage(m_hwndTB,TB_GETBUTTONSIZE,(WPARAM)0,(LPARAM)0));
		
		for (row = 0; row < 1 ; row++ ) 
			for (col = 0; col < nr_buttons; col++) { 
				ti.cbSize = sizeof(TOOLINFO); 
				ti.uFlags = 0 ; 
				ti.hwnd = m_hwndTB; 
				ti.hinst = m_pApp->m_instance; 
				ti.uId = (UINT) id; 
				ti.lpszText = (LPSTR) szTips[id++]; 
				ti.rect.left = col * buttonWidth; 
				ti.rect.top = row * buttonHeight; 
				ti.rect.right = ti.rect.left + buttonWidth; 
				ti.rect.bottom = ti.rect.top + buttonHeight; 
				
				SendMessage(m_hwndTT, TTM_ADDTOOL, 0, 
                    (LPARAM) (LPTOOLINFO) &ti); 
				
			}
			SendMessage(m_hwndTB,TB_SETTOOLTIPS,(WPARAM)(HWND)m_hwndTT,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETTIPBKCOLOR,(WPARAM)(COLORREF)0x00404040,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETTIPTEXTCOLOR,(WPARAM)(COLORREF)0x00F5B28D,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETDELAYTIME,(WPARAM)(DWORD)TTDT_INITIAL,(LPARAM)(INT) MAKELONG(200,0));
			
			SetWindowLong(m_hwndTBwin, GWL_USERDATA, (LONG) this);
			ShowWindow(m_hwndTB, SW_SHOW);
			ShowWindow(m_hwndTBwin, SW_SHOW);
	}
	else
	{
		int nr_buttons=9;
		TBADDBITMAP tbab; 
		TBBUTTON tbButtons []=
		{
			{0,ID_BUTTON_CAD,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,0},
			{1,ID_BUTTON_FULLSCREEN,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,1},
			{2,ID_BUTTON_PROPERTIES,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,2},
			{3,ID_BUTTON_REFRESH,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,3},
			{4,ID_BUTTON_STRG_ESC,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,4},
			{5,ID_BUTTON_SEP,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,5},
			{6,ID_BUTTON_INFO,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,6},
			{7,ID_BUTTON_END,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,7},
			{8,ID_BUTTON_DBUTTON,TBSTATE_ENABLED,TBSTYLE_BUTTON,0L,8},
			
		};
		static char *szTips[9] = 
		{
			sz_L2,
				sz_L3,
				sz_L4,
				sz_L5,
				sz_L6,
				sz_L7,
				sz_L8,
				sz_L9,
				sz_L10,
		};
		int stdidx;
		HWND m_hwndTT;
		int row,col;
		TOOLINFO ti; 
		int id=0;
		RECT clr;
		InitCommonControls();
		GetClientRect(m_hwndMain,&clr);
		m_TBr.left=0;
		m_TBr.right=clr.right;
		m_TBr.top=0;
		m_TBr.bottom=28;
		if (mini)
		{
			m_hwndTB = CreateToolbarEx(
				m_hwndTBwin
				//,WS_CHILD|WS_BORDER|WS_VISIBLE|TBSTYLE_TOOLTIPS|TBSTYLE_WRAPABLE|TB_AUTOSIZE 
				,WS_CHILD | TBSTYLE_WRAPABLE | WS_VISIBLE |TBSTYLE_TOOLTIPS |CCS_NORESIZE | TBSTYLE_FLAT | TBSTYLE_TRANSPARENT 
				,IDR_TOOLBAR
				,nr_buttons
				,(HINSTANCE)m_pApp->m_instance
				,IDB_BITMAP7
				,(LPCTBBUTTON)&tbButtons
				,nr_buttons
				,12
				,8
				,12
				,8
				,sizeof(TBBUTTON));
		}
		else
		{
			m_hwndTB = CreateToolbarEx(
				m_hwndTBwin
				//,WS_CHILD|WS_BORDER|WS_VISIBLE|TBSTYLE_TOOLTIPS|TBSTYLE_WRAPABLE|TB_AUTOSIZE 
				,WS_CHILD | TBSTYLE_WRAPABLE | WS_VISIBLE |TBSTYLE_TOOLTIPS |CCS_NORESIZE | TBSTYLE_FLAT | TBSTYLE_TRANSPARENT 
				,IDR_TOOLBAR
				,nr_buttons
				,(HINSTANCE)m_pApp->m_instance
				,IDB_BITMAP1
				,(LPCTBBUTTON)&tbButtons
				,nr_buttons
				,20
				,20
				,20
				,20
				,sizeof(TBBUTTON));
		}
		
		tbab.hInst = m_pApp->m_instance;
		tbab.nID = IDB_BITMAP1;
		stdidx = SendMessage(m_hwndTB,TB_ADDBITMAP,6,(LPARAM)&tbab);
		RECT tbrect;
		RECT wrect;
		RECT trect;
		SendMessage(m_hwndTB,TB_SETROWS,(WPARAM) MAKEWPARAM (2, true),(LPARAM) (LPRECT) (&trect));
		
		GetClientRect(m_hwndTB,&tbrect);
		GetClientRect(m_hwndTBwin,&wrect);
		
		
		m_hwndTT = CreateWindow(
			TOOLTIPS_CLASS,
			(LPSTR)NULL,
			TTS_ALWAYSTIP,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			CW_USEDEFAULT,
			NULL,
			(HMENU)NULL,
			(HINSTANCE)m_pApp->m_instance,
			NULL);
		
		
		
		DWORD buttonWidth = LOWORD(SendMessage(m_hwndTB,TB_GETBUTTONSIZE,(WPARAM)0,(LPARAM)0));
		DWORD buttonHeight = HIWORD(SendMessage(m_hwndTB,TB_GETBUTTONSIZE,(WPARAM)0,(LPARAM)0));
		
		for (row = 0; row < 1 ; row++ ) 
			for (col = 0; col < nr_buttons; col++) { 
				ti.cbSize = sizeof(TOOLINFO); 
				ti.uFlags = 0 ; 
				ti.hwnd = m_hwndTB; 
				ti.hinst = m_pApp->m_instance; 
				ti.uId = (UINT) id; 
				ti.lpszText = (LPSTR) szTips[id++]; 
				ti.rect.left = col * buttonWidth; 
				ti.rect.top = row * buttonHeight; 
				ti.rect.right = ti.rect.left + buttonWidth; 
				ti.rect.bottom = ti.rect.top + buttonHeight; 
				
				SendMessage(m_hwndTT, TTM_ADDTOOL, 0, 
                    (LPARAM) (LPTOOLINFO) &ti); 
				
			}
			SendMessage(m_hwndTB,TB_SETTOOLTIPS,(WPARAM)(HWND)m_hwndTT,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETTIPBKCOLOR,(WPARAM)(COLORREF)0x0000ff00,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETTIPTEXTCOLOR,(WPARAM)(COLORREF)0x00000000,(LPARAM)0);
			SendMessage(m_hwndTT,TTM_SETDELAYTIME,(WPARAM)(DWORD)TTDT_INITIAL,(LPARAM)(INT) MAKELONG(200,0));

			SetWindowLong(m_hwndTBwin, GWL_USERDATA, (LONG) this);
			ShowWindow(m_hwndTB, SW_SHOW);
			ShowWindow(m_hwndTBwin, SW_SHOW);
	}
}




void ClientConnection::GTGBS_CreateToolbar()
{

	RECT clr;
	WNDCLASS wndclass;

	wndclass.style			= 0;
	wndclass.lpfnWndProc	= ClientConnection::WndProcTBwin;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= m_pApp->m_instance;
	wndclass.hIcon			= LoadIcon(m_pApp->m_instance, MAKEINTRESOURCE(IDI_MAINICON));
	switch (m_opts.m_localCursor) {
	case NOCURSOR:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_NOCURSOR));
		break;
	case NORMALCURSOR:
		wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
		break;
	case DOTCURSOR:
	default:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_DOTCURSOR));
	}
	//wndclass.hbrBackground	= (HBRUSH) GetStockObject(BLACK_BRUSH);
	wndclass.hbrBackground	=   (HBRUSH)(COLOR_BTNFACE+1);
    wndclass.lpszMenuName	= (const TCHAR *) NULL;
	wndclass.lpszClassName	= VWR_WND_CLASS_NAME;

	RegisterClass(&wndclass);

	const DWORD winstyle = WS_CHILD ;
		
	GetClientRect(m_hwndMain,&clr);
	m_hwndTBwin = CreateWindowEx(
					//WS_EX_TOPMOST  ,
					0,
					VWR_WND_CLASS_NAME,
					_T("VNC ToolBar"),
					winstyle,
					0,
					0,
					clr.right - clr.left,	
					28,
					//m_hwnd,                // Parent handle
					m_hwndMain,
					NULL,                // Menu handle
					m_pApp->m_instance,
					NULL);
	
	ShowWindow(m_hwndTBwin, SW_HIDE);
	//////////////////////////////////////////////////
	if ((clr.right-clr.left)>140+85+14*24)
		CreateButtons(false,m_fServerKnowsFileTransfer);
	else 
		CreateButtons(true,m_fServerKnowsFileTransfer);
	//////////////////////////////////////////////////
	RECT r;
	
	GetClientRect(m_hwndTBwin,&r);
	m_TrafficMonitor = CreateWindowEx(WS_EX_NOPARENTNOTIFY | WS_EX_CLIENTEDGE,
											"Static",
											NULL,
											WS_CHILD | WS_VISIBLE ,
											clr.right - clr.left-45,
											((r.bottom-r.top) / 2) - 8,
											35,
											22,
											m_hwndTBwin,
											NULL,
											m_pApp->m_instance,
											NULL);

	m_bitmapNONE = LoadImage(m_pApp->m_instance,MAKEINTRESOURCE(IDB_STAT_NONE),IMAGE_BITMAP,22,20,LR_SHARED);
	m_bitmapFRONT = LoadImage(m_pApp->m_instance,MAKEINTRESOURCE(IDB_STAT_FRONT),IMAGE_BITMAP,22,20,LR_SHARED);
	m_bitmapBACK= LoadImage(m_pApp->m_instance,MAKEINTRESOURCE(IDB_STAT_BACK),IMAGE_BITMAP,22,20,LR_SHARED);
	HDC hdc = GetDC(m_TrafficMonitor);
	HDC hdcBits;
	hdcBits = CreateCompatibleDC(hdc);
	SelectObject(hdcBits,m_bitmapNONE);
	BitBlt(hdc,0,0,22,22,hdcBits,0,0,SRCCOPY);
	DeleteDC(hdcBits);
	ReleaseDC(m_TrafficMonitor,hdc);

	///////////////////////////////////////////////////
	m_logo_wnd = CreateWindow(
									"combobox",
									"",
									WS_CHILD | WS_VISIBLE | WS_TABSTOP|CBS_SIMPLE | CBS_AUTOHSCROLL | WS_VSCROLL,
									clr.right - clr.left-45-70,
									4,
									70,
									28,
									m_hwndTBwin,
									(HMENU)9999,
									m_pApp->m_instance,
									NULL);
	m_button_wnd = CreateWindow(
									"button",
									"",
									WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON | WS_TABSTOP,
									clr.right - clr.left-45-70-200,
									4,
									20,
									20,
									m_hwndTBwin,
									(HMENU)9998,
									m_pApp->m_instance,
									NULL);
	TCHAR valname[256];
	MRU *m_pMRU;
	m_pMRU = new MRU(SESSION_MRU_KEY_NAME,26);
    for (int i = 0; i < m_pMRU->NumItems(); i++) {
                m_pMRU->GetItem(i, valname, 255);
                int pos = SendMessage(m_logo_wnd, CB_ADDSTRING, 0, (LPARAM) valname);

            }
            SendMessage(m_logo_wnd, CB_SETCURSEL, 0, 0);
	if (m_pMRU) delete m_pMRU;		

}
//////////////////////////////////////////////////////////

void ClientConnection::CreateDisplay() 
{
#ifdef _WIN32_WCE
	//const DWORD winstyle = WS_VSCROLL | WS_HSCROLL | WS_CAPTION | WS_SYSMENU;
	const DWORD winstyle =  WS_CHILD;
#else
	//const DWORD winstyle = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX | WS_THICKFRAME | WS_VSCROLL | WS_HSCROLL;
	const DWORD winstyle = WS_CHILD;
#endif
	RECT Rmain;
	RECT Rtb;
	GetClientRect(m_hwndMain,&Rmain);
	GetClientRect(m_hwndTBwin,&Rtb);

	WNDCLASS wndclass;

	wndclass.style			= 0;
	wndclass.lpfnWndProc	= ClientConnection::WndProchwnd;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= m_pApp->m_instance;
	wndclass.hIcon			= LoadIcon(m_pApp->m_instance, MAKEINTRESOURCE(IDI_MAINICON));
	switch (m_opts.m_localCursor) {
	case NOCURSOR:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_NOCURSOR));
		break;
	case NORMALCURSOR:
		wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
		break;
	case DOTCURSOR:
	default:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_DOTCURSOR));
	}
	//wndclass.hbrBackground	= (HBRUSH) GetStockObject(BLACK_BRUSH);
	wndclass.hbrBackground	=   NULL;//(HBRUSH)(COLOR_WINDOW+1);
    wndclass.lpszMenuName	= (const TCHAR *) NULL;
	wndclass.lpszClassName	= VWR_WND_CLASS_NAME_VIEWER;

	RegisterClass(&wndclass);

	m_hwnd = CreateWindow(VWR_WND_CLASS_NAME_VIEWER,
	//m_hwnd = CreateWindow(_T("VNCMDI_Window"),
			      _T("VNCviewer"),
			      winstyle ,
			      0,
			      Rtb.top + Rtb.bottom,
			      CW_USEDEFAULT,       // x-size
			      CW_USEDEFAULT,       // y-size
			      //NULL,                // Parent handle
				  m_hwndMain,
			      NULL,                // Menu handle
			      m_pApp->m_instance,
			      NULL);


	//ShowWindow(m_hwnd, SW_HIDE);
	ShowWindow(m_hwnd, SW_SHOW);

	// record which client created this window
	SetWindowLong(m_hwnd, GWL_USERDATA, (LONG) this);
//	SendMessage(m_hwnd,WM_CREATE,0,0);


	// Create a memory DC which we'll use for drawing to
	// the local framebuffer
	m_hBitmapDC = CreateCompatibleDC(NULL);
	m_hCacheBitmapDC = CreateCompatibleDC(NULL);

	// Set a suitable palette up
	if (GetDeviceCaps(m_hBitmapDC, RASTERCAPS) & RC_PALETTE) {
		vnclog.Print(3, _T("Palette-based display - %d entries, %d reserved\n"), 
			GetDeviceCaps(m_hBitmapDC, SIZEPALETTE), GetDeviceCaps(m_hBitmapDC, NUMRESERVED));
		BYTE buf[sizeof(LOGPALETTE)+216*sizeof(PALETTEENTRY)];
		LOGPALETTE *plp = (LOGPALETTE *) buf;
		int pepos = 0;
		for (int r = 5; r >= 0; r--) {
			for (int g = 5; g >= 0; g--) {
				for (int b = 5; b >= 0; b--) {
					plp->palPalEntry[pepos].peRed   = r * 255 / 5; 	
					plp->palPalEntry[pepos].peGreen = g * 255 / 5;
					plp->palPalEntry[pepos].peBlue  = b * 255 / 5;
					plp->palPalEntry[pepos].peFlags  = NULL;
					pepos++;
				}
			}
		}
		plp->palVersion = 0x300;
		plp->palNumEntries = 216;
		m_hPalette = CreatePalette(plp);
	}

	// Add stuff to System menu
	HMENU hsysmenu = GetSystemMenu(m_hwndMain, FALSE);
	if (!m_opts.m_restricted) {
		// Modif sf@2002
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_FILETRANSFER,	sz_L16);
		AppendMenu(hsysmenu, MF_STRING, ID_TEXTCHAT,	sz_L17);
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_DBUTTON,	sz_L18);
		// AppendMenu(hsysmenu, MF_STRING, ID_BUTTON,	_T("Show Toolbar Buttons"));
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_DINPUT,	sz_L19);
		AppendMenu(hsysmenu, MF_STRING, ID_INPUT,	sz_L20);
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, IDC_OPTIONBUTTON,	sz_L21);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_ABOUT,		sz_L22);
		AppendMenu(hsysmenu, MF_STRING, ID_REQUEST_REFRESH,	sz_L23);
		AppendMenu(hsysmenu, MF_STRING, ID_VIEWONLYTOGGLE,	"View Only"); // Todo: translate

		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_FULLSCREEN,		sz_L24);
		AppendMenu(hsysmenu, MF_STRING, ID_AUTOSCALING,		sz_L25);
		// Modif sf@2002
		AppendMenu(hsysmenu, MF_STRING, ID_HALFSCREEN,		sz_L26);
		AppendMenu(hsysmenu, MF_STRING, ID_FUZZYSCREEN,		sz_L27);
		AppendMenu(hsysmenu, MF_STRING, ID_NORMALSCREEN,	sz_L28);
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_MAXCOLORS,		sz_L29);
		AppendMenu(hsysmenu, MF_STRING, ID_256COLORS,		sz_L30);
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
 		AppendMenu(hsysmenu, MF_STRING, ID_CONN_CTLALTDEL,	sz_L31);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_CTLESC,		sz_L32);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_CTLDOWN,	sz_L33);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_CTLUP,		sz_L34);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_ALTDOWN,	sz_L35);
		AppendMenu(hsysmenu, MF_STRING, ID_CONN_ALTUP,		sz_L36);
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_NEWCONN,			sz_L37);
		AppendMenu(hsysmenu, MF_STRING | (m_serverInitiated ? MF_GRAYED : 0), 
			ID_CONN_SAVE_AS,	sz_L38);
	}
    AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
	AppendMenu(hsysmenu, MF_STRING, IDD_APP_ABOUT,		sz_L39);
	if (m_opts.m_listening) {
		AppendMenu(hsysmenu, MF_SEPARATOR, NULL, NULL);
		AppendMenu(hsysmenu, MF_STRING, ID_CLOSEDAEMON, sz_L40);
	}
	DrawMenuBar(m_hwndMain);
	TheAccelKeys.SetWindowHandle(m_opts.m_NoHotKeys ? 0 : m_hwndMain);

	CheckMenuItem(GetSystemMenu(m_hwndMain, FALSE),
				  ID_DBUTTON,
				  MF_BYCOMMAND | (m_opts.m_ShowToolbar ? MF_CHECKED :MF_UNCHECKED));

	CheckMenuItem(GetSystemMenu(m_hwndMain, FALSE),
				  ID_AUTOSCALING,
				  MF_BYCOMMAND | (m_opts.m_fAutoScaling ? MF_CHECKED :MF_UNCHECKED));

	// Set up clipboard watching
#ifndef _WIN32_WCE
	// We want to know when the clipboard changes, so
	// insert ourselves in the viewer chain. But doing
	// this will cause us to be notified immediately of
	// the current state.
	// We don't want to send that.
	m_initialClipboardSeen = false;
	m_hwndNextViewer = SetClipboardViewer(m_hwnd); 	
#endif

#ifndef _ULTRAVNCAX_
	//Added by: Lars Werner (http://lars.werner.no)
	if(TitleBar.GetSafeHwnd()==NULL) 
		TitleBar.Create(m_pApp->m_instance, m_hwndMain);
#endif
}


//
// sf@2002 - DSMPlugin loading and initialization if required
//
void ClientConnection::LoadDSMPlugin()
{
	if (m_opts.m_fUseDSMPlugin)
	{
		if (!m_pDSMPlugin->IsLoaded())
		{
			m_pDSMPlugin->LoadPlugin(m_opts.m_szDSMPluginFilename, m_opts.m_listening);
			if (m_pDSMPlugin->IsLoaded())
			{
				if (m_pDSMPlugin->InitPlugin())
				{
					m_pDSMPlugin->SetEnabled(true);
					m_pDSMPlugin->DescribePlugin();
					/*
					MessageBox(NULL, 
					_T(_this->m_pDSMPlugin->DescribePlugin()),
					_T("Plugin Description"), MB_OK | MB_ICONEXCLAMATION );
					*/
				}
				else
				{
					m_pDSMPlugin->SetEnabled(false);
					MessageBox(NULL, 
						sz_F1, 
						sz_F6, MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND | MB_TOPMOST);
					return;
				}
			}
			else
			{
				m_pDSMPlugin->SetEnabled(false);
				MessageBox(NULL, 
					sz_F5, 
					sz_F6, MB_OK | MB_ICONEXCLAMATION | MB_SETFOREGROUND | MB_TOPMOST);
				return;
			}
		}
	}
	return;
}


//
// Get & Set the VNC password for the DSMPlugin if necessary
// 
void ClientConnection::SetDSMPluginStuff()
{
	if (m_pDSMPlugin->IsEnabled())
	{
		char szParams[256+16];

		// Does the plugin need the VNC password to do its job ?
		if (!stricmp(m_pDSMPlugin->GetPluginParams(), "VNCPasswordNeeded"))
		{
			// Yes. The user must enter the VNC password
			// He won't be prompted again for password if ms_logon is not used.
			if (strlen(m_clearPasswd) == 0) // Possibly set using -password command line
			{
				AuthDialog ad;
#ifdef _ULTRAVNCAX_
				ad.parent = GetTopMostWnd( m_hwndMain ? m_hwndMain : m_hwndAx );
#endif
				if (ad.DoDialog(false))
				{	
					strncpy(m_clearPasswd, ad.m_passwd,254);
				}
			}
			strcpy(szParams, m_clearPasswd);
		}
		else
			strcpy(szParams, "NoPassword");

		// The second parameter tells the plugin the kind of program is using it
		// (in vncviewer : "viewer")
		strcat(szParams, ",");
		strcat(szParams, "viewer");

		// Initialize the DSM Plugin with params
		if (!m_pDSMPlugin->SetPluginParams(NULL, szParams))
		{
			m_pDSMPlugin->SetEnabled(false);
			m_fUsePlugin = false;
			vnclog.Print(0, _T("DSMPlugin cannot be configured\n"));
			throw WarningException(sz_L41);
		}
		// If all went well
		m_fUsePlugin = true;
	}
}


//
//
//
void ClientConnection::HandleQuickOption()
{
	switch (m_opts.m_quickoption)
	{
	case 1:
		m_opts.m_PreferredEncoding = rfbEncodingZRLE;
		m_opts.m_Use8Bit = rfbPFFullColors; //false; 
		m_opts.m_fEnableCache = true;
		m_opts.autoDetect = true;
		break;

	case 2:
		m_opts.m_PreferredEncoding = rfbEncodingHextile;
		m_opts.m_Use8Bit = rfbPFFullColors; // false; // Max colors
		m_opts.autoDetect = false;
		m_opts.m_fEnableCache = false;
//		m_opts.m_localCursor = NOCURSOR;
		// m_opts.m_requestShapeUpdates = true;
		// m_opts.m_ignoreShapeUpdates = false;
		break;

	case 3:
		m_opts.m_PreferredEncoding = rfbEncodingZRLE; // rfbEncodingZlibHex;
		m_opts.m_Use8Bit = rfbPF256Colors; //false; 
		m_opts.autoDetect = false;
		m_opts.m_fEnableCache = false;
//		m_opts.m_localCursor = NOCURSOR;
		break;

	case 4:
		m_opts.m_PreferredEncoding = rfbEncodingZRLE;
		m_opts.m_Use8Bit = rfbPF64Colors; //true; 
		m_opts.autoDetect = false;
		m_opts.m_fEnableCache = true;
		break;

	case 5:
		m_opts.m_PreferredEncoding = rfbEncodingZRLE;
		m_opts.m_Use8Bit = rfbPF8Colors; //true;
		// m_opts.m_scaling = true; 
		// m_opts.m_scale_num = 200; 
		// m_opts.m_scale_den = 100;
		// m_opts.m_nServerScale = 2;
		m_opts.m_enableJpegCompression = false;
		m_opts.autoDetect = false;
		m_opts.m_fEnableCache = true;
		break;

	case 7:
		m_opts.m_PreferredEncoding = rfbEncodingUltra;
		m_opts.m_Use8Bit = rfbPFFullColors; //false; // Max colors
		m_opts.autoDetect = false;
		m_opts.m_fEnableCache = false;
		m_opts.m_requestShapeUpdates = false;
		m_opts.m_ignoreShapeUpdates = true;
//		m_opts.m_localCursor = NOCURSOR;
		break;

	default: // 0 can be set by noauto command line option. Do not chnage any setting in this case
		/* sf@2005
		m_opts.m_PreferredEncoding = rfbEncodingZRLE;
		m_opts.m_Use8Bit = rfbPF256Colors; //false; 
		m_opts.m_fEnableCache = true;
		m_opts.autoDetect = false;
		*/
		break;
	}

}

void ClientConnection::GetConnectDetails()
{
	if (m_opts.m_configSpecified) {
		LoadConnection(m_opts.m_configFilename, false);
	}
	else
	{
		char optionfile[MAX_PATH];
		char *tempvar=NULL;
		tempvar = getenv( "TEMP" );
		if (tempvar) strcpy(optionfile,tempvar);
		else strcpy(optionfile,"");
		strcat(optionfile,"\\options.vnc");
		if (!command_line)
		{
			if (LoadConnection(optionfile, false)==-1)
				{
					SessionDialog sessdlg(&m_opts, this, m_pDSMPlugin); //sf@2002
					if (!sessdlg.DoDialog())
					{
						throw QuietException(sz_L42);
					}
					_tcsncpy(m_host, sessdlg.m_host_dialog, MAX_HOST_NAME_LEN);
					m_port = sessdlg.m_port;
					_tcsncpy(m_proxyhost, sessdlg.m_proxyhost, MAX_HOST_NAME_LEN);
			//		_tcsncpy(m_remotehost, sessdlg.m_remotehost, MAX_HOST_NAME_LEN);
					m_proxyport = sessdlg.m_proxyport;
					m_fUseProxy = sessdlg.m_fUseProxy;
					if (m_opts.autoDetect)
					{
						m_opts.m_Use8Bit = rfbPF256Colors;
						m_opts.m_fEnableCache = true; // sf@2002
					}
				}
		}
		else
		{
			SessionDialog sessdlg(&m_opts, this, m_pDSMPlugin); //sf@2002
			if (!sessdlg.DoDialog())
					{
						throw QuietException(sz_L42);
					}
			_tcsncpy(m_host, sessdlg.m_host_dialog, MAX_HOST_NAME_LEN);
			m_port = sessdlg.m_port;
			_tcsncpy(m_proxyhost, sessdlg.m_proxyhost, MAX_HOST_NAME_LEN);
	//		_tcsncpy(m_remotehost, sessdlg.m_remotehost, MAX_HOST_NAME_LEN);
			m_proxyport = sessdlg.m_proxyport;
			m_fUseProxy = sessdlg.m_fUseProxy;
			if (m_opts.autoDetect)
			{
				m_opts.m_Use8Bit = rfbPF256Colors;
				m_opts.m_fEnableCache = true; // sf@2002
			}
		}
		
	}
	// This is a bit of a hack: 
	// The config file may set various things in the app-level defaults which 
	// we don't want to be used except for the first connection. So we clear them
	// in the app defaults here.
	m_pApp->m_options.m_host_options[0] = '\0';
	m_pApp->m_options.m_port = -1;
	m_pApp->m_options.m_proxyhost[0] = '\0';
	m_pApp->m_options.m_proxyport = -1;
	m_pApp->m_options.m_connectionSpecified = false;
	m_pApp->m_options.m_configSpecified = false;

}

void ClientConnection::Connect()
{
	struct sockaddr_in thataddr;
	int res;
	if (!m_opts.m_NoStatus) GTGBS_ShowConnectWindow();
	
	m_sock = socket(PF_INET, SOCK_STREAM, 0);
	if (m_hwndStatus) SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L43);
	if (m_sock == INVALID_SOCKET) {if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L44);throw WarningException(sz_L44);}
	int one = 1;

	if (m_hwndStatus) SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L45);
	if (m_hwndStatus) UpdateWindow(m_hwndStatus);

	// The host may be specified as a dotted address "a.b.c.d"
	// Try that first
	thataddr.sin_addr.s_addr = inet_addr(m_host);
	
	// If it wasn't one of those, do gethostbyname
	if (thataddr.sin_addr.s_addr == INADDR_NONE) {
		LPHOSTENT lphost;
		lphost = gethostbyname(m_host);
		
		if (lphost == NULL) { 
			//if(myDialog!=0)DestroyWindow(myDialog);
			if (m_hwndStatus) SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L46);
			throw WarningException(sz_L46); 
		};
		thataddr.sin_addr.s_addr = ((LPIN_ADDR) lphost->h_addr)->s_addr;
	};
	
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L47);
	if (m_hwndStatus)ShowWindow(m_hwndStatus,SW_SHOW);
	if (m_hwndStatus)UpdateWindow(m_hwndStatus);
	if (m_hwndStatus)SetDlgItemInt(m_hwndStatus,IDC_PORT,m_port,FALSE);
	thataddr.sin_family = AF_INET;
	thataddr.sin_port = htons(m_port);
	
	res = connect(m_sock, (LPSOCKADDR) &thataddr, sizeof(thataddr));
	if (res == SOCKET_ERROR) 
		{
			if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L48);
			if (!Pressed_Cancel) throw WarningException(sz_L48);
			else throw QuietException(sz_L48);
		}
	vnclog.Print(0, _T("Connected to %s port %d\n"), m_host, m_port);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L49);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_VNCSERVER,m_host);
	if (m_hwndStatus)ShowWindow(m_hwndStatus,SW_SHOW);
	if (m_hwndStatus)UpdateWindow(m_hwndStatus);
}

void ClientConnection::ConnectProxy()
{
	struct sockaddr_in thataddr;
	int res;
	if (!m_opts.m_NoStatus) GTGBS_ShowConnectWindow();
	
	m_sock = socket(PF_INET, SOCK_STREAM, 0);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L43);
	if (m_sock == INVALID_SOCKET) {if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L44);throw WarningException(sz_L44);}
	int one = 1;

	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L45);
	if (m_hwndStatus)UpdateWindow(m_hwndStatus);

	// The host may be specified as a dotted address "a.b.c.d"
	// Try that first
	thataddr.sin_addr.s_addr = inet_addr(m_proxyhost);
	
	// If it wasn't one of those, do gethostbyname
	if (thataddr.sin_addr.s_addr == INADDR_NONE) {
		LPHOSTENT lphost;
		lphost = gethostbyname(m_proxyhost);
		
		if (lphost == NULL) { 
			//if(myDialog!=0)DestroyWindow(myDialog);
			if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L46);
			throw WarningException(sz_L46); 
		};
		thataddr.sin_addr.s_addr = ((LPIN_ADDR) lphost->h_addr)->s_addr;
	};
	
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L47);
	if (m_hwndStatus)ShowWindow(m_hwndStatus,SW_SHOW);
	if (m_hwndStatus)UpdateWindow(m_hwndStatus);
	if (m_hwndStatus)SetDlgItemInt(m_hwndStatus,IDC_PORT,m_proxyport,FALSE);
	thataddr.sin_family = AF_INET;
	thataddr.sin_port = htons(m_proxyport);
	
	res = connect(m_sock, (LPSOCKADDR) &thataddr, sizeof(thataddr));
	if (res == SOCKET_ERROR) {if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L48);throw WarningException(sz_L48);}
	vnclog.Print(0, _T("Connected to %s port %d\n"), m_proxyhost, m_proxyport);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L49);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_VNCSERVER,m_proxyhost);
	if (m_hwndStatus)ShowWindow(m_hwndStatus,SW_SHOW);
	if (m_hwndStatus)UpdateWindow(m_hwndStatus);
}

void ClientConnection::SetSocketOptions() 
{
	// Disable Nagle's algorithm
	BOOL nodelayval = TRUE;
	if (setsockopt(m_sock, IPPROTO_TCP, TCP_NODELAY, (const char *) &nodelayval, sizeof(BOOL)))
		throw WarningException(sz_L50);

        fis = new rdr::FdInStream(m_sock);
		fis->SetDSMMode(m_pDSMPlugin->IsEnabled()); // sf@2003 - Special DSM mode for ZRLE encoding
}


void ClientConnection::NegotiateProtocolVersion()
{
	rfbProtocolVersionMsg pv;

   /* if the connection is immediately closed, don't report anything, so
       that pmw's monitor can make test connections */

	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L89);
    try
	{
		ReadExact(pv, sz_rfbProtocolVersionMsg);
	}
	catch (Exception &c)
	{
		vnclog.Print(0, _T("Error reading protocol version: %s\n"),
                          c.m_info);
		if (m_fUsePlugin)
			throw WarningException("Connection failed - Error reading Protocol Version\r\n\n\r"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- The selected DSMPlugin is not compatible with the one running on the Server\r\n"
									"- The selected DSMPlugin is not correctly configured (also possibly on the Server)\r\n"
									"- The password you've possibly entered is incorrect\r\n"
									);
		else
			throw WarningException("Connection failed - Error reading Protocol Version\r\n\n\r"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- Viewer and Server are not compatible (they use different RFB protocoles)\r\n"
									"- Bad connection\r\n"
									);

		throw QuietException(c.m_info);
	}

    pv[sz_rfbProtocolVersionMsg] = 0;

	/*
	// sf@2005 - Cleanup scrambled chars before parsing -> Restore original RFB protocol header first chars
	if (m_fUsePlugin)
	{
		pv[0] = rfbProtocolVersionFormat[0];
		pv[1] = rfbProtocolVersionFormat[1];
		pv[2] = rfbProtocolVersionFormat[2];
		pv[3] = rfbProtocolVersionFormat[3];
	}
	*/

	// XXX This is a hack.  Under CE we just return to the server the
	// version number it gives us without parsing it.  
	// Too much hassle replacing sscanf for now. Fix this!
#ifdef UNDER_CE
	m_majorVersion = rfbProtocolMajorVersion;
	m_minorVersion = rfbProtocolMinorVersion;
#else
    if (sscanf(pv,rfbProtocolVersionFormat,&m_majorVersion,&m_minorVersion) != 2)
	{
		if (m_fUsePlugin)
			throw WarningException("Connection failed - Invalid protocol !\r\n\r\n"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- The selected DSMPlugin is not compatible with the one running on the Server\r\n"
									"- The selected DSMPlugin is not correctly configured (also possibly on the Server)\r\n"
									"- The password you've possibly entered is incorrect\r\n"
									);
		else
			throw WarningException("Connection failed - Invalid protocol !\r\n\r\n"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- Viewer and Server are not compatible (they use different RFB protocoles)\r\n"
									);
    }

    vnclog.Print(0, _T("RFB server supports protocol version %d.%d\n"),
	    m_majorVersion,m_minorVersion);

	// UltraVNC specific functionnalities
	// - ms logon
	// - FileTransfer (TODO: change Minor version in next eSVNC release so it's compatible with Ultra)
	// Minor = 4 means that server supports FileTransfer and requires ms logon
	// Minor = 6 means that server support FileTransfer and requires normal VNC logon
	if (m_minorVersion == 4)
	{
		m_ms_logon = true;
		m_fServerKnowsFileTransfer = true;
	}
	if (m_minorVersion == 6) // 6 because 5 already used in TightVNC viewer for some reason
	{
		m_ms_logon = false;
		m_fServerKnowsFileTransfer = true;
	}
	// Added for SC so we can do something before actual data transfer start
	if (m_minorVersion == 14 || m_minorVersion == 16)
	{
		m_fServerKnowsFileTransfer = true;
	}

    else if ((m_majorVersion == 3) && (m_minorVersion < 3)) {
		
        /* if server is 3.2 we can't use the new authentication */
		vnclog.Print(0, _T("Can't use IDEA authentication\n"));
        /* This will be reported later if authentication is requested*/

    } else {
		
        /* any other server version, just tell the server what we want */
		m_majorVersion = rfbProtocolMajorVersion;
		m_minorVersion = rfbProtocolMinorVersion; // always 4 for Ultra Viewer

    }

    sprintf(pv,rfbProtocolVersionFormat, m_majorVersion, m_minorVersion);
#endif

    WriteExact(pv, sz_rfbProtocolVersionMsg);
	if (m_minorVersion == 14 || m_minorVersion == 16)
	{
		int size;
		ReadExact((char *)&size,sizeof(int));
		char mytext[1024]; //10k
		ReadExact(mytext,size);
		mytext[size]=0;

		int returnvalue=MessageBox(NULL,   mytext,"Accept Incoming SC connection", MB_YESNO |  MB_TOPMOST);
		if (returnvalue==IDNO) 
		{
			int nummer=0;
			WriteExact((char *)&nummer,sizeof(int));
			throw WarningException("You refused connection.....");
		}
		else
		{
			int nummer=1;
			WriteExact((char *)&nummer,sizeof(int));

		}
		
	}


	vnclog.Print(0, _T("Connected to RFB server, using protocol version %d.%d\n"),
		rfbProtocolMajorVersion, rfbProtocolMinorVersion);


}

void ClientConnection::NegotiateProxy()
{
	rfbProtocolVersionMsg pv;

   /* if the connection is immediately closed, don't report anything, so
       that pmw's monitor can make test connections */

	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L89);
    try
	{
		ReadExactProxy(pv, sz_rfbProtocolVersionMsg);
	}
	catch (Exception &c)
	{
		vnclog.Print(0, _T("Error reading protocol version: %s\n"),
                          c.m_info);
		if (m_fUsePlugin)
			throw WarningException("Proxy Connection failed - Error reading Protocol Version\r\n\n\r"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- The selected DSMPlugin is not compatible with the one running on the Server\r\n"
									"- The selected DSMPlugin is not correctly configured (also possibly on the Server)\r\n"
									"- The password you've possibly entered is incorrect\r\n"
									);
		else
			throw WarningException("Proxy Connection failed - Error reading Protocol Version\r\n\n\r"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- Viewer and Server are not compatible (they use different RFB protocoles)\r\n"
									"- Bad connection\r\n"
									);

		throw QuietException(c.m_info);
	}

    pv[sz_rfbProtocolVersionMsg] = 0;

	/*
	// sf@2005 - Cleanup scrambled chars before parsing -> Restore original RFB protocol header first chars
	if (m_fUsePlugin)
	{
		pv[0] = rfbProtocolVersionFormat[0];
		pv[1] = rfbProtocolVersionFormat[1];
		pv[2] = rfbProtocolVersionFormat[2];
		pv[3] = rfbProtocolVersionFormat[3];
	}
	*/

    if (sscanf(pv,rfbProtocolVersionFormat,&m_majorVersion,&m_minorVersion) != 2)
	{
		if (m_fUsePlugin)
			throw WarningException("Proxy Connection failed - Invalid protocol !\r\n\r\n"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- The selected DSMPlugin is not compatible with the one running on the Server\r\n"
									"- The selected DSMPlugin is not correctly configured (also possibly on the Server)\r\n"
									"- The password you've possibly entered is incorrect\r\n"
									);
		else
			throw WarningException("Proxy Connection failed - Invalid protocol !\r\n\r\n"
									"Possible causes:\r\r"
									"- You've forgotten to select a DSMPlugin and the Server uses a DSMPlugin\r\n"
									"- Viewer and Server are not compatible (they use different RFB protocoles)\r\n"
									);
    }

    vnclog.Print(0, _T("Connected to proxy \n"),
	    m_majorVersion,m_minorVersion);

	if (m_majorVersion==0 && m_minorVersion==0)
	{
	TCHAR tmphost[MAX_HOST_NAME_LEN];
	TCHAR tmphost2[256];
	_tcscpy(tmphost,m_host);
	if (strcmp(tmphost,"")!=NULL)
	{
	_tcscat(tmphost,":");
	_tcscat(tmphost,itoa(m_port,tmphost2,10));
	}
    WriteExactProxy(tmphost,MAX_HOST_NAME_LEN);

	vnclog.Print(0, _T("Connected to RFB server, using protocol version %d.%d\n"),
		rfbProtocolMajorVersion, rfbProtocolMinorVersion);
	}


}

void ClientConnection::Authenticate()
{
	CARD32 authScheme, reasonLen, authResult;
    CARD8 challenge[CHALLENGESIZE];
	CARD8 challengems[CHALLENGESIZEMS];
	
	ReadExact((char *)&authScheme, 4);
    authScheme = Swap32IfLE(authScheme);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L90);
    switch (authScheme) {
		
    case rfbConnFailed:
		ReadExact((char *)&reasonLen, 4);
		reasonLen = Swap32IfLE(reasonLen);
		
		CheckBufferSize(reasonLen+1);
		ReadString(m_netbuf, reasonLen);
		
		vnclog.Print(0, _T("RFB connection failed, reason: %s\n"), m_netbuf);
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L91);
		throw WarningException(m_netbuf);
        break;
		
    case rfbNoAuth:
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L92);
		vnclog.Print(0, _T("No authentication needed\n"));
		break;
		
    case rfbVncAuth:
		{
            if ((m_majorVersion == 3) && (m_minorVersion < 3)) 
			{
                /* if server is 3.2 we can't use the new authentication */
                vnclog.Print(0, _T("Can't use IDEA authentication\n"));

                MessageBox(NULL, 
                    sz_L51, 
                    sz_L52, 
                    MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);

                throw WarningException("Can't use IDEA authentication any more!");
            }
			// rdv@2002 - v1.1.x
			char passwd[256];
			char domain[256];
			char user[256];

			memset(passwd, 0, sizeof(char)*256);
			memset(domain, 0, sizeof(char)*256);
			memset(user, 0, sizeof(char)*256);

			// We ignore the clear password in case of ms_logon !
			// Todo: Add ms_user & ms_password command line params
			if (m_ms_logon) memset(m_clearPasswd, 0, sizeof(m_clearPasswd));

			if(strlen(m_strPassword) > 0)
			{
				strncpy(m_clearPasswd, m_strPassword, sizeof(m_clearPasswd) - 1);
			}

			// Was the password already specified in a config file or entered for DSMPlugin ?
			// Modif sf@2002 - A clear password can be transmitted via the vncviewer command line

			if (strlen(m_clearPasswd)>0)
			{
				strcpy(passwd, m_clearPasswd);
			} 
			else if (strlen((const char *) m_encPasswd)>0)
			{
				char *pw = vncDecryptPasswd(m_encPasswd);
				strcpy(passwd, pw);
				free(pw);
			}
			else 
			{
				AuthDialog ad;
#ifdef _ULTRAVNCAX_
				ad.parent = GetTopMostWnd( m_hwndMain ? m_hwndMain : m_hwndAx );
#endif
				///////////////ppppppppppppppppppppppppppppppppppppppppp
				if (ad.DoDialog(m_ms_logon))
				{
//					flash = new BmpFlasher;
					#ifndef UNDER_CE
					strncpy(passwd, ad.m_passwd,254);
					strncpy(user, ad.m_user,254);
					strncpy(domain, ad.m_domain,254);
					#else
					int origlen = _tcslen(ad.m_passwd);
					int newlen = WideCharToMultiByte(
						CP_ACP,    // code page
						0,         // performance and mapping flags
						ad.m_passwd, // address of wide-character string
						origlen,   // number of characters in string
						passwd,    // address of buffer for new string
						255,       // size of buffer
						NULL, NULL );
					
					passwd[newlen]= '\0';
					//user
					origlen = _tcslen(ad.m_user);
					newlen = WideCharToMultiByte(
						CP_ACP,    // code page
						0,         // performance and mapping flags
						ad.m_user, // address of wide-character string
						origlen,   // number of characters in string
						user,    // address of buffer for new string
						255,       // size of buffer
						NULL, NULL );
					
					user[newlen]= '\0';
					//domain
					origlen = _tcslen(ad.m_domain);
					newlen = WideCharToMultiByte(
						CP_ACP,    // code page
						0,         // performance and mapping flags
						ad.m_domain, // address of wide-character string
						origlen,   // number of characters in string
						domain,    // address of buffer for new string
						255,       // size of buffer
						NULL, NULL );
					
					domain[newlen]= '\0';
#endif
					if (strlen(user)==0 ||!m_ms_logon)//need longer passwd for ms
						{
							if (strlen(passwd) == 0) {
//								if (flash) {flash->Killflash();}
								vnclog.Print(0, _T("Password had zero length\n"));
								throw WarningException(sz_L53);
							}
							if (strlen(passwd) > 8) {
								passwd[8] = '\0';
							}
						}
					if (m_ms_logon) vncEncryptPasswdMs(m_encPasswdMs, passwd);
					vncEncryptPasswd(m_encPasswd, passwd);
				} 
				else 
				{
//					if (flash) {flash->Killflash();}
					throw QuietException(sz_L54);
				}
			}
			/*
			// sf@2002 - DSM Plugin
			if (m_pDSMPlugin->IsEnabled())
			{
				// Initialize the DSL Plugin with the entered password
				if (!m_pDSMPlugin->SetPluginParams(NULL, passwd))
				{
					m_pDSMPlugin->SetEnabled(false);
					m_fUsePlugin = false;
					vnclog.Print(0, _T("DSMPlugin cannot be configured\n"));
					throw WarningException("DSMPlugin cannot be configured");
				}

				m_fUsePlugin = true;

				// TODO: Make a special challenge with time stamp 
				// to prevent recording the logon session for later replay

			}
			*/

			// sf@2002 
			// m_ms_logon = false;
			if (m_ms_logon) ReadExact((char *)challengems, CHALLENGESIZEMS);
			ReadExact((char *)challenge, CHALLENGESIZE);

			// MS logon
			if (m_ms_logon) 
			{
				int i=0;
				for (i=0;i<32;i++)
				{
					challengems[i]=m_encPasswdMs[i]^challengems[i];
				}
				WriteExact((char *) user, sizeof(char)*256);
				WriteExact((char *) domain, sizeof(char)*256);
				WriteExact((char *) challengems, CHALLENGESIZEMS);
				vncEncryptBytes(challenge, passwd);

				/* Lose the plain-text password from memory */
				int nLen = (int)strlen(passwd);
				for ( i=0; i< nLen; i++) {
					passwd[i] = '\0';
				}
			
				WriteExact((char *) challenge, CHALLENGESIZE);
			}
			else // Regular VNC logon
			{
				vncEncryptBytes(challenge, passwd);

				/* Lose the plain-text password from memory */
				int nLen = (int)strlen(passwd);
				for (int i=0; i< nLen; i++) {
					passwd[i] = '\0';
				}
			
				WriteExact((char *) challenge, CHALLENGESIZE);
			}
			ReadExact((char *) &authResult, 4);
			
			authResult = Swap32IfLE(authResult);
			
			switch (authResult) 
			{
			case rfbVncAuthOK:
				if (m_hwndStatus)vnclog.Print(0, _T("VNC authentication succeeded\n"));
				SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L55);
				g_passwordfailed=false;
				break;
			case rfbVncAuthFailed:
				vnclog.Print(0, _T("VNC authentication failed!"));
				g_passwordfailed=true;
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L56);
//				if (flash) {flash->Killflash();}
				throw WarningException(sz_L57);
			case rfbVncAuthTooMany:
				throw WarningException(
					sz_L58);
			default:
				vnclog.Print(0, _T("Unknown VNC authentication result: %d\n"),
					(int)authResult);
//				if (flash) {flash->Killflash();}
				throw ErrorException(sz_L59);
			}
			break;
		}
    case rfbMsLogon:
		AuthMsLogon();
		break;
	default:
		vnclog.Print(0, _T("Unknown authentication scheme from RFB server: %d\n"),
			(int)authScheme);
//		if (flash) {flash->Killflash();}
		throw ErrorException(sz_L60);
    }
}

// marscha@2006: Try to better hide the windows password.
// I know that this is no breakthrough in modern cryptography.
// It's just a patch/kludge/workaround.
void ClientConnection::AuthMsLogon() {
	char gen[8], mod[8], pub[8], resp[8];
	char user[256], passwd[64];
	unsigned char key[8];

	memset(m_clearPasswd, 0, sizeof(m_clearPasswd)); // ??
	
	ReadExact(gen, sizeof(gen));
	ReadExact(mod, sizeof(mod));
	ReadExact(resp, sizeof(resp));
		
	DH dh(bytesToInt64(gen), bytesToInt64(mod));
	int64ToBytes(dh.createInterKey(), pub);

	WriteExact(pub, sizeof(pub));

	int64ToBytes(dh.createEncryptionKey(bytesToInt64(resp)), (char*) key);
	vnclog.Print(100, _T("After DH: g=%I64u, m=%I64u, i=%I64u, key=%I64u\n"),
	  bytesToInt64(gen), bytesToInt64(mod), bytesToInt64(pub), bytesToInt64((char*) key));
	// get username and passwd
	AuthDialog ad;
#ifdef _ULTRAVNCAX_
				ad.parent = GetTopMostWnd( m_hwndMain ? m_hwndMain : m_hwndAx );
#endif
	if (ad.DoDialog(m_ms_logon, true)) {
#ifndef UNDER_CE
		strncpy(passwd, ad.m_passwd, 64);
		strncpy(user, ad.m_user, 254);
		//strncpy(domain, ad.m_domain, 254);
#else
		vncWc2Mb(passwd, ad.m_passwd, 64);
		vncWc2Mb(user, ad.m_user, 256);
		//vncWc2Mb(domain, ad.m_domain, 256);
#endif
		//vncEncryptPasswdMs(m_encPasswdMs, passwd);
	} else {
		throw QuietException(sz_L54);
	}
	//user = domain + "\\" + user;

	vncEncryptBytes2((unsigned char*) user, sizeof(user), key);
	vncEncryptBytes2((unsigned char*) passwd, sizeof(passwd), key);
	
	WriteExact(user, sizeof(user));
	WriteExact(passwd, sizeof(passwd));


	CARD32 authResult;
	ReadExact((char *) &authResult, 4);
	
	authResult = Swap32IfLE(authResult);
	
	switch (authResult) {
	case rfbVncAuthOK:
		vnclog.Print(0, _T("MS-Logon (DH) authentication succeeded.\n"));
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L55);
		g_passwordfailed=false;
		break;
	case rfbVncAuthFailed:
		vnclog.Print(0, _T("MS-Logon (DH) authentication failed!\n"));
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L56);
		g_passwordfailed=true;
		throw WarningException(sz_L57);
	case rfbVncAuthTooMany:
		throw WarningException(sz_L58);
	default:
		vnclog.Print(0, _T("Unknown MS-Logon (DH) authentication result: %d\n"),
			(int)authResult);
		throw ErrorException(sz_L59);
	}
}

void ClientConnection::SendClientInit()
{
    rfbClientInitMsg ci;
	ci.shared = m_opts.m_Shared;

    WriteExact((char *)&ci, sz_rfbClientInitMsg); // sf@2002 - RSM Plugin
}

void ClientConnection::ReadServerInit()
{
    ReadExact((char *)&m_si, sz_rfbServerInitMsg);
	

    m_si.framebufferWidth = Swap16IfLE(m_si.framebufferWidth);
    m_si.framebufferHeight = Swap16IfLE(m_si.framebufferHeight);
    m_si.format.redMax = Swap16IfLE(m_si.format.redMax);
    m_si.format.greenMax = Swap16IfLE(m_si.format.greenMax);
    m_si.format.blueMax = Swap16IfLE(m_si.format.blueMax);
    m_si.nameLength = Swap32IfLE(m_si.nameLength);
	
    m_desktopName = new TCHAR[m_si.nameLength + 4 + 256];

#ifdef UNDER_CE
    char *deskNameBuf = new char[m_si.nameLength + 4];

	ReadString(deskNameBuf, m_si.nameLength);
    
	MultiByteToWideChar( CP_ACP,   MB_PRECOMPOSED, 
			     deskNameBuf, m_si.nameLength,
			     m_desktopName, m_si.nameLength+1);
    delete deskNameBuf;
#else
    ReadString(m_desktopName, m_si.nameLength);
#endif
    // TCHAR tcDummy [MAX_PATH * 3];
	
	// sprintf(tcDummy,"%s ",m_desktopName);
	strcat(m_desktopName, " ");
	SetWindowText(m_hwndMain, m_desktopName);	

	vnclog.Print(0, _T("Desktop name \"%s\"\n"),m_desktopName);
	vnclog.Print(1, _T("Geometry %d x %d depth %d\n"),
		m_si.framebufferWidth, m_si.framebufferHeight, m_si.format.depth );

	//SetWindowText(m_hwndMain, m_desktopName);	
	if (m_pDSMPlugin->IsEnabled())
	{
			char szMess[255];
			memset(szMess, 0, 255);
			sprintf(szMess, "--- VMOpsVNC Viewer + %s-v%s",
					m_pDSMPlugin->GetPluginName(),
					m_pDSMPlugin->GetPluginVersion()
					);
			strcat(m_desktopName, szMess);
	}
	SetWindowText(m_hwndMain, m_desktopName);	

	SizeWindow();
}

void ClientConnection::SizeWindow()
{
	// Find how large the desktop work area is
	RECT workrect;

#ifdef _ULTRAVNCAX_
	::GetClientRect( ::GetParent( m_hwndMain ), & workrect );
#else
	SystemParametersInfo(SPI_GETWORKAREA, 0, &workrect, 0);
#endif

	int workwidth = workrect.right -  workrect.left;
	int workheight = workrect.bottom - workrect.top;
	vnclog.Print(2, _T("Screen work area is %d x %d\n"), workwidth, workheight);

	// sf@2003 - AutoScaling 
	if (m_opts.m_fAutoScaling && !m_fScalingDone)
	{
		// We save the scales values coming from options
		m_opts.m_saved_scale_num = m_opts.m_scale_num;
		m_opts.m_saved_scale_den = m_opts.m_scale_den;
		m_opts.m_saved_scaling = m_opts.m_scaling;

		NONCLIENTMETRICS ncm = {0};
		ncm.cbSize = sizeof(ncm);
		SystemParametersInfo(SPI_GETNONCLIENTMETRICS, 0, &ncm, 0); 
		int TitleBarHeight = ncm.iCaptionHeight + 10;
		
		int nLocalHeight = workheight; 
		nLocalHeight -= TitleBarHeight;
		//if (m_opts.m_ShowToolbar)
		nLocalHeight -= (m_TBr.bottom); // Always take toolbar into account in calculation

		//
		// we need to consider the ratio both from width and height
		// - Kelven Yang 12/14/2008
		//
		m_opts.m_scale_num = min((int)((workheight * 100) / m_si.framebufferHeight),
			(int)((workwidth * 100) / m_si.framebufferWidth));

		// m_opts.m_scale_num = (int)((nLocalHeight * 100) / m_si.framebufferHeight);

		if (m_opts.m_scale_num >= 100) {
			// Sheng: Do not scale larger than the actual screen size
			m_opts.m_scale_num = 100;
			m_opts.m_scaling = false;
		} else {
			m_opts.m_scaling = true; 
		}
		m_opts.m_scale_den = 100;
		m_fScalingDone = true;
	}
	
	if (!m_opts.m_fAutoScaling && m_fScalingDone)
	{
		// Restore scale values to the original options values
		m_opts.m_scale_num = m_opts.m_saved_scale_num;
		m_opts.m_scale_den = m_opts.m_saved_scale_den;
		m_opts.m_scaling = m_opts.m_saved_scaling;
		m_fScalingDone = false;
	}

	// Size the window.
	// Let's find out how big a window would be needed to display the
	// whole desktop (assuming no scrollbars).

	RECT fullwinrect;

	if (m_opts.m_scaling)
		SetRect(&fullwinrect, 0, 0,
				m_si.framebufferWidth * m_opts.m_scale_num / m_opts.m_scale_den,
				m_si.framebufferHeight * m_opts.m_scale_num / m_opts.m_scale_den);
	else 
		SetRect(&fullwinrect, 0, 0, m_si.framebufferWidth, m_si.framebufferHeight);

	AdjustWindowRectEx(&fullwinrect, 
			   GetWindowLong(m_hwnd, GWL_STYLE) & ~WS_VSCROLL & ~WS_HSCROLL, 
			   FALSE, GetWindowLong(m_hwnd, GWL_EXSTYLE));
	/*
	AdjustWindowRectEx(&fullwinrect, 
			   GetWindowLong(m_hwndMain, GWL_STYLE), 
			   FALSE, GetWindowLong(m_hwndMain, GWL_EXSTYLE));
	*/

	m_fullwinwidth = fullwinrect.right - fullwinrect.left;
	m_fullwinheight = (fullwinrect.bottom - fullwinrect.top);

	m_winwidth  = min(m_fullwinwidth,  workwidth);
	m_winheight = min(m_fullwinheight, workheight);

	//SetWindowPos(m_hwnd, HWND_TOP,
	if (m_opts.m_ShowToolbar)
		SetWindowPos(m_hwnd, m_hwndTBwin, 0, m_TBr.bottom, m_winwidth, m_winheight, SWP_SHOWWINDOW);
	else 
	{
		SetWindowPos(m_hwnd, m_hwndTBwin, 0, 0, m_winwidth, m_winheight, SWP_SHOWWINDOW);
		SetWindowPos(m_hwndTBwin, NULL ,0, 0, 0, 0, SWP_HIDEWINDOW);
	}

   // Hauptfenster positionieren
	/*
	SetRect(&fullwinrect, 0, 0, m_si.framebufferWidth * m_opts.m_scale_num / m_opts.m_scale_den, 
								m_si.framebufferHeight* m_opts.m_scale_num / m_opts.m_scale_den );
	*/
	AdjustWindowRectEx(&fullwinrect, 
					   GetWindowLong(m_hwndMain, GWL_STYLE) & ~WS_VSCROLL & ~WS_HSCROLL, 
					   FALSE, GetWindowLong(m_hwndMain, GWL_EXSTYLE));

	m_fullwinwidth = fullwinrect.right - fullwinrect.left;
	m_fullwinheight = (fullwinrect.bottom - fullwinrect.top);

	//m_winwidth  = min(m_fullwinwidth+16,  workwidth);
	m_winwidth  = min(m_fullwinwidth,  workwidth);
	//m_winheight = min(m_fullwinheight+m_TBr.bottom + m_TBr.top+16 , workheight);
	if (m_opts.m_ShowToolbar)
		m_winheight = min(m_fullwinheight + m_TBr.bottom + m_TBr.top , workheight);
	else
		m_winheight = min(m_fullwinheight, workheight);

	HWND hwndIa = HWND_TOP;
	UINT flags = SWP_SHOWWINDOW;
	INT x = workrect.left + (workwidth-m_winwidth) / 2;
	INT y = workrect.top + (workheight-m_winheight) / 2;
	INT cx = m_winwidth;
	INT cy = m_winheight;

#ifdef _ULTRAVNCAX_
	// no z-order manipulation.
	hwndIa = NULL;
	flags |= SWP_NOZORDER;
#endif

	SetWindowPos(m_hwndMain, hwndIa,
				x,
				y,
				cx, cy, flags);

#ifndef _ULTRAVNCAX_
	SetForegroundWindow(m_hwndMain);
#else
	SetFocus( ::GetParent( m_hwndMain ) );
#endif

	if (m_opts.m_ShowToolbar)
		MoveWindow(m_hwndTBwin, 0, 0, workwidth, m_TBr.bottom - m_TBr.top, TRUE);

	if (m_opts.m_ShowToolbar)
		MoveWindow(m_hwndTB, 0, 0, m_winwidth-200, m_TBr.bottom - m_TBr.top, TRUE);

	if (m_opts.m_ShowToolbar)
		ShowWindow(m_hwndTB, SW_SHOW);
	else
		ShowWindow(m_hwndTB, SW_HIDE);

	if (m_opts.m_ShowToolbar)
		ShowWindow(m_hwndTBwin, SW_SHOW);
	else
		ShowWindow(m_hwndTB, SW_HIDE);

}

// We keep a local copy of the whole screen.  This is not strictly necessary
// for VNC, but makes scrolling & deiconifying much smoother.

void ClientConnection::CreateLocalFramebuffer()
{
	omni_mutex_lock l(m_bitmapdcMutex);
	
	// We create a bitmap which has the same pixel characteristics as
	// the local display, in the hope that blitting will be faster.

	TempDC hdc(m_hwnd);

	if (m_hBitmap != NULL)
		DeleteObject(m_hBitmap);

	m_hBitmap = CreateCompatibleBitmap(hdc, m_si.framebufferWidth, m_si.framebufferHeight);
	if (m_hBitmap == NULL)
		throw WarningException(sz_L61);
	// Select this bitmap into the DC with an appropriate palette
	ObjectSelector b(m_hBitmapDC, m_hBitmap);
	PaletteSelector p(m_hBitmapDC, m_hPalette);
	// Modif RDV@2002 - Cache Encoding
	// Modif sf@2002
	if (m_opts.m_fEnableCache)
	{
		if (m_hCacheBitmap != NULL) DeleteObject(m_hCacheBitmap);
		m_hCacheBitmap = CreateCompatibleBitmap(m_hBitmapDC, m_si.framebufferWidth, m_si.framebufferHeight);
		vnclog.Print(0, _T("Cache: Cache buffer bitmap creation\n"));
	}
	
	RECT rect;

	SetRect(&rect, 0,0, m_si.framebufferWidth, m_si.framebufferHeight);
	COLORREF bgcol = RGB(0, 0, 50);
	FillSolidRect(&rect, bgcol);
	
	COLORREF oldbgcol  = SetBkColor(  m_hBitmapDC, bgcol);
	COLORREF oldtxtcol = SetTextColor(m_hBitmapDC, RGB(255,255,255));
	rect.right = m_si.framebufferWidth / 2;
	rect.bottom = m_si.framebufferHeight / 2;
	
	DrawText (m_hBitmapDC, sz_L62, -1, &rect,
		DT_SINGLELINE | DT_CENTER | DT_VCENTER);

	SetBkColor(  m_hBitmapDC, oldbgcol);
	SetTextColor(m_hBitmapDC, oldtxtcol);
	InvalidateRect(m_hwnd, NULL, FALSE);

}

void ClientConnection::SetupPixelFormat() {
	// Have we requested a reduction to 8-bit?
    if (m_opts.m_Use8Bit)
	{		
		switch (m_opts.m_Use8Bit)
		{
		case rfbPF256Colors:
			m_myFormat = vnc8bitFormat;
			break;
		case rfbPF64Colors:
			m_myFormat = vnc8bitFormat_64;
			break;
		case rfbPF8Colors:
			m_myFormat = vnc8bitFormat_8;
			break;
		case rfbPF8GreyColors:
			m_myFormat = vnc8bitFormat_8Grey;
			break;
		case rfbPF4GreyColors:
			m_myFormat = vnc8bitFormat_4Grey;
			break;
		case rfbPF2GreyColors:
			m_myFormat = vnc8bitFormat_2Grey;
			break;
		}
		vnclog.Print(2, _T("Requesting 8-bit truecolour\n"));  
		// We don't support colormaps so we'll ask the server to convert
    }
	else if (!m_si.format.trueColour)
	{
        
        // We'll just request a standard 16-bit truecolor
        vnclog.Print(2, _T("Requesting 16-bit truecolour\n"));
        m_myFormat = vnc16bitFormat;
    }
	else
	{

		// Normally we just use the sever's format suggestion
		m_myFormat = m_si.format;
        m_myFormat.bigEndian = 0; // except always little endian

		// It's silly requesting more bits than our current display has, but
		// in fact it doesn't usually amount to much on the network.
		// Windows doesn't support 8-bit truecolour.
		// If our display is palette-based, we want more than 8 bit anyway,
		// unless we're going to start doing palette stuff at the server.
		// So the main use would be a 24-bit true-colour desktop being viewed
		// on a 16-bit true-colour display, and unless you have lots of images
		// and hence lots of raw-encoded stuff, the size of the pixel is not
		// going to make much difference.
		//   We therefore don't bother with any restrictions, but here's the
		// start of the code if we wanted to do it.

		if (false) {
		
			// Get a DC for the root window
			TempDC hrootdc(NULL);
			int localBitsPerPixel = GetDeviceCaps(hrootdc, BITSPIXEL);
			int localRasterCaps	  = GetDeviceCaps(hrootdc, RASTERCAPS);
			vnclog.Print(2, _T("Memory DC has depth of %d and %s pallete-based.\n"), 
				localBitsPerPixel, (localRasterCaps & RC_PALETTE) ? "is" : "is not");
			
			// If we're using truecolor, and the server has more bits than we do
			if ( (localBitsPerPixel > m_myFormat.depth) && 
				! (localRasterCaps & RC_PALETTE)) {
				m_myFormat.depth = localBitsPerPixel;

				// create a bitmap compatible with the current display
				// call GetDIBits twice to get the colour info.
				// set colour masks and shifts
				
			}
		}
	}

	// The endian will be set before sending
}


void ClientConnection::SetFormatAndEncodings()
{
	// Set pixel format to myFormat
    
	rfbSetPixelFormatMsg spf;

    spf.type = rfbSetPixelFormat;
    spf.format = m_myFormat;
    spf.format.redMax = Swap16IfLE(spf.format.redMax);
    spf.format.greenMax = Swap16IfLE(spf.format.greenMax);
    spf.format.blueMax = Swap16IfLE(spf.format.blueMax);

    WriteExact((char *)&spf, sz_rfbSetPixelFormatMsg, rfbSetPixelFormat);

    // The number of bytes required to hold at least one pixel.
	m_minPixelBytes = (m_myFormat.bitsPerPixel + 7) >> 3;

	// Set encodings
    char buf[sz_rfbSetEncodingsMsg + MAX_ENCODINGS * 4];
    rfbSetEncodingsMsg *se = (rfbSetEncodingsMsg *)buf;
    CARD32 *encs = (CARD32 *)(&buf[sz_rfbSetEncodingsMsg]);
    int len = 0;
	
    se->type = rfbSetEncodings;
    se->nEncodings = 0;

	bool useCompressLevel = false;
	int i = 0;
	// Put the preferred encoding first, and change it if the
	// preferred encoding is not actually usable.
	for (i = LASTENCODING; i >= rfbEncodingRaw; i--)
	{
		if (m_opts.m_PreferredEncoding == i) {
			if (m_opts.m_UseEnc[i])
			{
				encs[se->nEncodings++] = Swap32IfLE(i);
	  			if ( i == rfbEncodingZlib ||
					 i == rfbEncodingTight ||
					 i == rfbEncodingZlibHex
			   )
				{
					useCompressLevel = true;
				}
			}
			else 
			{
				m_opts.m_PreferredEncoding--;
			}
		}
	}

	// Now we go through and put in all the other encodings in order.
	// We do rather assume that the most recent encoding is the most
	// desirable!
	for (i = LASTENCODING; i >= rfbEncodingRaw; i--)
	{
		if ( (m_opts.m_PreferredEncoding != i) &&
			 (m_opts.m_UseEnc[i]))
		{
			encs[se->nEncodings++] = Swap32IfLE(i);
			if ( i == rfbEncodingZlib ||
				 i == rfbEncodingTight ||
				 i == rfbEncodingZlibHex
				)
			{
				useCompressLevel = true;
			}
		}
	}

	// Tight - Request desired compression level if applicable
	if ( useCompressLevel && m_opts.m_useCompressLevel &&
		 m_opts.m_compressLevel >= 0 &&
		 m_opts.m_compressLevel <= 9) {
		encs[se->nEncodings++] = Swap32IfLE( rfbEncodingCompressLevel0 +
											 m_opts.m_compressLevel );
	}

	// Tight - Request cursor shape updates if enabled by user
	if (m_opts.m_requestShapeUpdates) {
		encs[se->nEncodings++] = Swap32IfLE(rfbEncodingXCursor);
		encs[se->nEncodings++] = Swap32IfLE(rfbEncodingRichCursor);
		if (!m_opts.m_ignoreShapeUpdates)
			encs[se->nEncodings++] = Swap32IfLE(rfbEncodingPointerPos); // marscha PointerPos
	}

	// Tight - Request JPEG quality level if JPEG compression was enabled by user
	if ( m_opts.m_enableJpegCompression &&
		 m_opts.m_jpegQualityLevel >= 0 &&
		 m_opts.m_jpegQualityLevel <= 9) {
		encs[se->nEncodings++] = Swap32IfLE( rfbEncodingQualityLevel0 +
											 m_opts.m_jpegQualityLevel );
	}

    // Modif rdv@2002
	//Tell the server that we support the special Zlibencoding
	encs[se->nEncodings++] = Swap32IfLE(rfbEncodingXOREnable);

	// Tight - LastRect - SINGLE WINDOW
	encs[se->nEncodings++] = Swap32IfLE(rfbEncodingLastRect);
	encs[se->nEncodings++] = Swap32IfLE(rfbEncodingNewFBSize);

	// Modif sf@2002
	if (m_opts.m_fEnableCache)
	{
		encs[se->nEncodings++] = Swap32IfLE(rfbEncodingCacheEnable);
		// vnclog.Print(0, _T("Cache: Enable Cache sent to Server\n"));
	}

    // len = sz_rfbSetEncodingsMsg + se->nEncodings * 4;
	
    // sf@2002 - DSM Plugin
	int nEncodings = se->nEncodings;
	se->nEncodings = Swap16IfLE(se->nEncodings);
	// WriteExact((char *)buf, len);
	WriteExact((char *)buf, sz_rfbSetEncodingsMsg, rfbSetEncodings);
	for (int x = 0; x < nEncodings; x++)
	{ 
		WriteExact((char *)&encs[x], sizeof(CARD32));
	}
}
void ClientConnection::Createdib()
{
	omni_mutex_lock l(m_bitmapdcMutex);
	TempDC hdc(m_hwnd);
	BitmapInfo bi;
	UINT iUsage;
    memset(&bi, 0, sizeof(bi));
	
	iUsage = m_myFormat.trueColour ? DIB_RGB_COLORS : DIB_PAL_COLORS;
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biBitCount = m_myFormat.bitsPerPixel;
    bi.bmiHeader.biSizeImage = (m_myFormat.bitsPerPixel / 8) * m_si.framebufferWidth * m_si.framebufferHeight;
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biWidth = m_si.framebufferWidth;
    bi.bmiHeader.biHeight = -m_si.framebufferHeight;
    bi.bmiHeader.biCompression = (m_myFormat.bitsPerPixel > 8) ? BI_BITFIELDS : BI_RGB;
    bi.mask.red = m_myFormat.redMax << m_myFormat.redShift;
    bi.mask.green = m_myFormat.greenMax << m_myFormat.greenShift;
    bi.mask.blue = m_myFormat.blueMax << m_myFormat.blueShift;

	if (m_hmemdc != NULL) {DeleteDC(m_hmemdc);m_hmemdc = NULL;m_DIBbits=NULL;}
	if (m_membitmap != NULL) {DeleteObject(m_membitmap);m_membitmap= NULL;}
//	m_hmemdc = CreateCompatibleDC(hdc);
	m_hmemdc = CreateCompatibleDC(m_hBitmapDC);
	m_membitmap = CreateDIBSection(m_hmemdc, (BITMAPINFO*)&bi.bmiHeader, iUsage, &m_DIBbits, NULL, 0);

	ObjectSelector bb(m_hmemdc, m_membitmap);

	if (m_myFormat.bitsPerPixel==8 && m_myFormat.trueColour)
	{
		struct Colour {
		int r, g, b;
		};
		Colour rgbQ[256];
        /*UINT num_entries;
		num_entries =GetPaletteEntries(m_hPalette, 0, 0, NULL);              
        size_t pal_size = sizeof(LOGPALETTE) +(num_entries - 1) * sizeof(PALETTEENTRY);
        LOGPALETTE* pLogPal =(LOGPALETTE*) new unsigned char[pal_size];
        UINT num_got = GetPaletteEntries( m_hPalette, 0, num_entries, pLogPal->palPalEntry);
          for (UINT i=0; i<num_got; ++i)
          {
            rgbQ[i].rgbRed = pLogPal->palPalEntry[i].peRed;
            rgbQ[i].rgbGreen = pLogPal->palPalEntry[i].peGreen;
            rgbQ[i].rgbBlue = pLogPal-> palPalEntry[i].peBlue;
          }

         delete [] pLogPal;*/

		 for (int i=0; i < (1<<(m_myFormat.depth)); i++) {
			rgbQ[i].b = ((((i >> m_myFormat.blueShift) & m_myFormat.blueMax) * 65535) + m_myFormat.blueMax/2) / m_myFormat.blueMax;
			rgbQ[i].g = ((((i >> m_myFormat.greenShift) & m_myFormat.greenMax) * 65535) + m_myFormat.greenMax/2) / m_myFormat.greenMax;
			rgbQ[i].r = ((((i >> m_myFormat.redShift) & m_myFormat.redMax) * 65535) + m_myFormat.redMax/2) / m_myFormat.redMax;
		 }

	for (i=0; i<256; i++)
	{
		bi.color[i].rgbRed      = rgbQ[i].r >> 8;
		bi.color[i].rgbGreen    = rgbQ[i].g >> 8;
		bi.color[i].rgbBlue     = rgbQ[i].b >> 8;
		bi.color[i].rgbReserved = 0;
	}
	SetDIBColorTable(m_hmemdc, 0, 256, bi.color);
	}
}
// Closing down the connection.
// Close the socket, kill the thread.
void ClientConnection::KillThread()
{
#ifdef _ULTRAVNCAX_
	if ( m_bKillThread )
		return;
#endif

	m_bKillThread = true;
	m_running = false;

	if (m_sock != INVALID_SOCKET) {
		shutdown(m_sock, SD_BOTH);
		closesocket(m_sock);
		m_sock = INVALID_SOCKET;
	}
#ifdef _ULTRAVNCAX_
	if ( m_threadStarted )
#endif
	WaitForSingleObject(KillEvent, 100000);
}


ClientConnection::~ClientConnection()
{
	if (m_hwndStatus)
		EndDialog(m_hwndStatus,0);

	if (m_pNetRectBuf != NULL)
		delete [] m_pNetRectBuf;
#ifndef _ULTRAVNCAX_
	LowLevelHook::Release();
#endif

	// Modif sf@2002 - FileTransfer
	if (m_pFileTransfer) 
		delete(m_pFileTransfer);

	// Modif sf@2002 - Text Chat
	if (m_pTextChat)
		delete(m_pTextChat);

	// Modif sf@2002 - DSMPlugin handling
	if (m_pDSMPlugin != NULL)
		delete(m_pDSMPlugin);

    if (zis)
      delete zis;

    if (fis)
      delete fis;

	if (m_pZRLENetRectBuf != NULL)
		delete [] m_pZRLENetRectBuf;

	if (m_sock != INVALID_SOCKET) {
		shutdown(m_sock, SD_BOTH);
		closesocket(m_sock);
		m_sock = INVALID_SOCKET;
	}

	if (m_desktopName != NULL) delete [] m_desktopName;
	delete [] m_netbuf;

	if (m_hCacheBitmapDC != NULL)
			DeleteDC(m_hCacheBitmapDC);
	if (m_hCacheBitmapDC != NULL)
			DeleteObject(m_hCacheBitmapDC);
	if (m_hCacheBitmap != NULL)
		DeleteObject(m_hCacheBitmap);
	
	if (m_hBitmapDC != NULL)
		DeleteDC(m_hBitmapDC);
	if (m_hBitmapDC != NULL)
		DeleteObject(m_hBitmapDC);
	if (m_hBitmap != NULL)
		DeleteObject(m_hBitmap);

	if (m_hPalette != NULL)
		DeleteObject(m_hPalette);
	//UltraFast
	if (m_hmemdc != NULL) {DeleteDC(m_hmemdc);m_hmemdc = NULL;m_DIBbits=NULL;}
	if (m_membitmap != NULL) {DeleteObject(m_membitmap);m_membitmap = NULL;}
//	if (flash) delete flash;
	m_pApp->DeregisterConnection(this);
	if (m_zipbuf!=NULL)
		delete [] m_zipbuf;
	if (m_filezipbuf!=NULL)
		delete [] m_filezipbuf;
	if (m_filechunkbuf!=NULL)
		delete [] m_filechunkbuf;
	if (m_zlibbuf!=NULL)
		delete [] m_zlibbuf;
	if (m_hwndTBwin!= 0)
		DestroyWindow(m_hwndTBwin);
	if (rcSource!=NULL)
		delete[] rcSource;
	if (rcMask!=NULL)
		delete[] rcMask;
	CloseHandle(KillEvent);
}

// You can specify a dx & dy outside the limits; the return value will
// tell you whether it actually scrolled.
bool ClientConnection::ScrollScreen(int dx, int dy) 
{
	dx = max(dx, -m_hScrollPos);
	dx = min(dx, m_hScrollMax-(m_cliwidth)-m_hScrollPos);
	dy = max(dy, -m_vScrollPos);
	dy = min(dy, m_vScrollMax-(m_cliheight)-m_vScrollPos);
	if (dx || dy) {
		m_hScrollPos += dx;
		m_vScrollPos += dy;
		RECT clirect;
		RECT Rtb;
		GetClientRect(m_hwndMain, &clirect);
		if (m_opts.m_ShowToolbar)
			GetClientRect(m_hwndTBwin, &Rtb);
		else 
			{
				Rtb.top=0;
				Rtb.bottom=0;
			}
		
		clirect.top += Rtb.top;
		clirect.bottom += Rtb.bottom;
		ScrollWindowEx(m_hwnd, -dx, -dy, NULL, &clirect, NULL, NULL,  SW_INVALIDATE);
		UpdateScrollbars();
		UpdateWindow(m_hwnd);
		
		return true;
	}
	return false;
}



// ProcessPointerEvent handles the delicate case of emulating 3 buttons
// on a two button mouse, then passes events off to SubProcessPointerEvent.
inline void ClientConnection::ProcessPointerEvent(int x, int y, DWORD keyflags, UINT msg) 
{
	if (m_opts.m_Emul3Buttons) {
		// XXX To be done:
		// If this is a left or right press, the user may be 
		// about to press the other button to emulate a middle press.
		// We need to start a timer, and if it expires without any
		// further presses, then we send the button press. 
		// If a press of the other button, or any release, comes in
		// before timer has expired, we cancel timer & take different action.
	  if (m_waitingOnEmulateTimer)
	    {
	      if (msg == WM_LBUTTONUP || msg == WM_RBUTTONUP ||
		  abs(x - m_emulateButtonPressedX) > m_opts.m_Emul3Fuzz ||
		  abs(y - m_emulateButtonPressedY) > m_opts.m_Emul3Fuzz)
		{
		  // if button released or we moved too far then cancel.
		  // First let the remote know where the button was down
		  SubProcessPointerEvent(
					 m_emulateButtonPressedX, 
					 m_emulateButtonPressedY, 
					 m_emulateKeyFlags);
		  // Then tell it where we are now
		  SubProcessPointerEvent(x, y, keyflags);
		}
	      else if (
		       (msg == WM_LBUTTONDOWN && (m_emulateKeyFlags & MK_RBUTTON))
		       || (msg == WM_RBUTTONDOWN && (m_emulateKeyFlags & MK_LBUTTON)))
		{
		  // Triggered an emulate; remove left and right buttons, put
		  // in middle one.
		  DWORD emulatekeys = keyflags & ~(MK_LBUTTON|MK_RBUTTON);
		  emulatekeys |= MK_MBUTTON;
		  SubProcessPointerEvent(x, y, emulatekeys);
		  
		  m_emulatingMiddleButton = true;
		}
	      else
		{
		  // handle movement normally & don't kill timer.
		  // just remove the pressed button from the mask.
		  DWORD keymask = m_emulateKeyFlags & (MK_LBUTTON|MK_RBUTTON);
		  DWORD emulatekeys = keyflags & ~keymask;
		  SubProcessPointerEvent(x, y, emulatekeys);
		  return;
		}
	      
	      // if we reached here, we don't need the timer anymore.
	      KillTimer(m_hwnd, m_emulate3ButtonsTimer);
	      m_waitingOnEmulateTimer = false;
	    }
	  else if (m_emulatingMiddleButton)
	    {
	      if ((keyflags & MK_LBUTTON) == 0 && (keyflags & MK_RBUTTON) == 0)
		{
		  // We finish emulation only when both buttons come back up.
		  m_emulatingMiddleButton = false;
		  SubProcessPointerEvent(x, y, keyflags);
		}
	      else
		{
		  // keep emulating.
		  DWORD emulatekeys = keyflags & ~(MK_LBUTTON|MK_RBUTTON);
		  emulatekeys |= MK_MBUTTON;
		  SubProcessPointerEvent(x, y, emulatekeys);
		}
	    }
	  else
	    {
	      // Start considering emulation if we've pressed a button
	      // and the other isn't pressed.
	      if ( (msg == WM_LBUTTONDOWN && !(keyflags & MK_RBUTTON))
		   || (msg == WM_RBUTTONDOWN && !(keyflags & MK_LBUTTON)))
		{
		  // Start timer for emulation.
		  m_emulate3ButtonsTimer = 
		    SetTimer(
			     m_hwnd, 
			     IDT_EMULATE3BUTTONSTIMER, 
			     m_opts.m_Emul3Timeout, 
			     NULL);
		  
		  if (!m_emulate3ButtonsTimer)
		    {
		      vnclog.Print(0, _T("Failed to create timer for emulating 3 buttons"));
		      PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
		      return;
		    }
		  
		  m_waitingOnEmulateTimer = true;
		  
		  // Note that we don't send the event here; we're batching it for
		  // later.
		  m_emulateKeyFlags = keyflags;
		  m_emulateButtonPressedX = x;
		  m_emulateButtonPressedY = y;
		}
	      else
		{
		  // just send event noramlly
		  SubProcessPointerEvent(x, y, keyflags);
		}
	    }
 	}
	else
	  {
	    SubProcessPointerEvent(x, y, keyflags);
	  }
}

// SubProcessPointerEvent takes windows positions and flags and converts 
// them into VNC ones.

inline void ClientConnection::SubProcessPointerEvent(int x, int y, DWORD keyflags)
{
	int mask;
  
	if (m_opts.m_SwapMouse) {
		mask = ( ((keyflags & MK_LBUTTON) ? rfbButton1Mask : 0) |
				 ((keyflags & MK_MBUTTON) ? rfbButton3Mask : 0) |
				 ((keyflags & MK_RBUTTON) ? rfbButton2Mask : 0) );
	} else {
		mask = ( ((keyflags & MK_LBUTTON) ? rfbButton1Mask : 0) |
				 ((keyflags & MK_MBUTTON) ? rfbButton2Mask : 0) |
				 ((keyflags & MK_RBUTTON) ? rfbButton3Mask : 0) );
	}

	if ((short)HIWORD(keyflags) > 0) {
		mask |= rfbButton4Mask;
	} else if ((short)HIWORD(keyflags) < 0) {
		mask |= rfbButton5Mask;
	}

	try {
		int x_scaled =
			(x + m_hScrollPos) * m_opts.m_scale_den / m_opts.m_scale_num;
		int y_scaled =
			(y + m_vScrollPos) * m_opts.m_scale_den / m_opts.m_scale_num;

		SendPointerEvent(x_scaled, y_scaled, mask);

		if ((short)HIWORD(keyflags) != 0) {
			// Immediately send a "button-up" after mouse wheel event.
			mask &= !(rfbButton4Mask | rfbButton5Mask);
			SendPointerEvent(x_scaled, y_scaled, mask);
		}
	} catch (Exception &e) {
		e.Report();
		PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
	}
}


//
// RealVNC 335 method
// 
inline void ClientConnection::ProcessMouseWheel(int delta)
{
  int wheelMask = rfbWheelUpMask;
  if (delta < 0) {
    wheelMask = rfbWheelDownMask;
    delta = -delta;
  }
  while (delta > 0) {
    SendPointerEvent(oldPointerX, oldPointerY, oldButtonMask | wheelMask);
    SendPointerEvent(oldPointerX, oldPointerY, oldButtonMask & ~wheelMask);
    delta -= 120;
  }
}


//
// SendPointerEvent.
//

inline void
ClientConnection::SendPointerEvent(int x, int y, int buttonMask)
{
	if (m_pFileTransfer->m_fFileTransferRunning && ( m_pFileTransfer->m_fVisible || m_pFileTransfer->m_fOldFTProtocole)) return;
	if (m_pTextChat->m_fTextChatRunning && m_pTextChat->m_fVisible) return;

	//omni_mutex_lock l(m_UpdateMutex);

	/*
	newtick=GetTickCount();
	if ((newtick-oldtick)<100) return;
	oldtick=newtick;
	*/
    
	rfbPointerEventMsg pe;

    oldPointerX = x;
    oldPointerY = y;
    oldButtonMask = buttonMask;
    pe.type = rfbPointerEvent;
    pe.buttonMask = buttonMask;
    if (x < 0) x = 0;
    if (y < 0) y = 0;
	// tight cursor handling
	SoftCursorMove(x, y);
    pe.x = Swap16IfLE(x);
    pe.y = Swap16IfLE(y);
	WriteExact((char *)&pe, sz_rfbPointerEventMsg, rfbPointerEvent); // sf@2002 - For DSM Plugin
}

//
// ProcessKeyEvent
//
// Normally a single Windows key event will map onto a single RFB
// key message, but this is not always the case.  Much of the stuff
// here is to handle AltGr (=Ctrl-Alt) on international keyboards.
// Example cases:
//
//    We want Ctrl-F to be sent as:
//      Ctrl-Down, F-Down, F-Up, Ctrl-Up.
//    because there is no keysym for ctrl-f, and because the ctrl
//    will already have been sent by the time we get the F.
//
//    On German keyboards, @ is produced using AltGr-Q, which is
//    Ctrl-Alt-Q.  But @ is a valid keysym in its own right, and when
//    a German user types this combination, he doesn't mean Ctrl-@.
//    So for this we will send, in total:
//
//      Ctrl-Down, Alt-Down,   
//                 (when we get the AltGr pressed)
//
//      Alt-Up, Ctrl-Up, @-Down, Ctrl-Down, Alt-Down 
//                 (when we discover that this is @ being pressed)
//
//      Alt-Up, Ctrl-Up, @-Up, Ctrl-Down, Alt-Down
//                 (when we discover that this is @ being released)
//
//      Alt-Up, Ctrl-Up
//                 (when the AltGr is released)

inline void ClientConnection::ProcessKeyEvent(int virtkey, DWORD keyData)
{
    bool down = ((keyData & 0x80000000l) == 0);

    // if virtkey found in mapping table, send X equivalent
    // else
    //   try to convert directly to ascii
    //   if result is in range supported by X keysyms,
    //      raise any modifiers, send it, then restore mods
    //   else
    //      calculate what the ascii would be without mods
    //      send that

#ifdef _DEBUG
#ifdef UNDER_CE
	char *keyname="";
#else
    char keyname[32];
    if (GetKeyNameText(  keyData,keyname, 31)) {
//        vnclog.Print(4, _T("Process key: %s (keyData %04x): virtkey %04x "), keyname, keyData,virtkey);
//		if (virtkey==0x00dd) 
//			vnclog.Print(4, _T("Process key: %s (keyData %04x): virtkey %04x "), keyname, keyData,virtkey);
    };
#endif
#endif

	try {
		KeyActionSpec kas = m_keymap.PCtoX(virtkey, keyData);    
		
		if (kas.releaseModifiers & KEYMAP_LCONTROL) {
			SendKeyEvent(XK_Control_L, false );
			vnclog.Print(5, _T("fake L Ctrl raised\n"));
		}
		if (kas.releaseModifiers & KEYMAP_LALT) {
			SendKeyEvent(XK_Alt_L, false );
			vnclog.Print(5, _T("fake L Alt raised\n"));
		}
		if (kas.releaseModifiers & KEYMAP_RCONTROL) {
			SendKeyEvent(XK_Control_R, false );
			vnclog.Print(5, _T("fake R Ctrl raised\n"));
		}
		if (kas.releaseModifiers & KEYMAP_RALT) {
			SendKeyEvent(XK_Alt_R, false );
			vnclog.Print(5, _T("fake R Alt raised\n"));
		}
		
		for (int i = 0; kas.keycodes[i] != XK_VoidSymbol && i < MaxKeysPerKey; i++) {
			SendKeyEvent(kas.keycodes[i], down );
			//vnclog.Print(4, _T("Sent keysym %04x (%s)\n"), 
			//	kas.keycodes[i], down ? _T("press") : _T("release"));
		}
		
		if (kas.releaseModifiers & KEYMAP_RALT) {
			SendKeyEvent(XK_Alt_R, true );
			vnclog.Print(5, _T("fake R Alt pressed\n"));
		}
		if (kas.releaseModifiers & KEYMAP_RCONTROL) {
			SendKeyEvent(XK_Control_R, true );
			vnclog.Print(5, _T("fake R Ctrl pressed\n"));
		}
		if (kas.releaseModifiers & KEYMAP_LALT) {
			SendKeyEvent(XK_Alt_L, false );
			vnclog.Print(5, _T("fake L Alt pressed\n"));
		}
		if (kas.releaseModifiers & KEYMAP_LCONTROL) {
			SendKeyEvent(XK_Control_L, false );
			vnclog.Print(5, _T("fake L Ctrl pressed\n"));
		}
	} catch (Exception &e) {
		e.Report();
		PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
	}

}

//
// SendKeyEvent
//

inline void
ClientConnection::SendKeyEvent(CARD32 key, bool down)
{
	if (m_pFileTransfer->m_fFileTransferRunning && ( m_pFileTransfer->m_fVisible || m_pFileTransfer->m_fOldFTProtocole)) return;
	if (m_pTextChat->m_fTextChatRunning && m_pTextChat->m_fVisible) return;

    rfbKeyEventMsg ke;

    ke.type = rfbKeyEvent;
    ke.down = down ? 1 : 0;
    ke.key = Swap32IfLE(key);
    WriteExact((char *)&ke, sz_rfbKeyEventMsg, rfbKeyEvent);
    //vnclog.Print(0, _T("SendKeyEvent: key = x%04x status = %s ke.key=%d\n"), key, 
      //  down ? _T("down") : _T("up"),ke.key);
}

#ifndef UNDER_CE
//
// SendClientCutText
//

void ClientConnection::SendClientCutText(char *str, int len)
{
	if (m_pFileTransfer->m_fFileTransferRunning && ( m_pFileTransfer->m_fVisible || m_pFileTransfer->m_fOldFTProtocole)) return;
	if (m_pTextChat->m_fTextChatRunning && m_pTextChat->m_fVisible) return;

	rfbClientCutTextMsg cct;

    cct.type = rfbClientCutText;
    cct.length = Swap32IfLE(len);
    WriteExact((char *)&cct, sz_rfbClientCutTextMsg, rfbClientCutText);
	WriteExact(str, len);
	vnclog.Print(6, _T("Sent %d bytes of clipboard\n"), len);
}
#endif

// Copy any updated areas from the bitmap onto the screen.

inline void ClientConnection::DoBlit() 
{

	if (m_hBitmap == NULL) return;
	if (!m_running) return;
				
	// No other threads can use bitmap DC
	omni_mutex_lock l(m_bitmapdcMutex);

	PAINTSTRUCT ps;
	HDC hdc = BeginPaint(m_hwnd, &ps);

	// Select and realize hPalette
	PaletteSelector p(hdc, m_hPalette);
	ObjectSelector b(m_hBitmapDC, m_hBitmap);
	
	if (m_opts.m_delay) {
		// Display the area to be updated for debugging purposes
		COLORREF oldbgcol = SetBkColor(hdc, RGB(0,0,0));
		::ExtTextOut(hdc, 0, 0, ETO_OPAQUE, &ps.rcPaint, NULL, 0, NULL);
		SetBkColor(hdc,oldbgcol);
		::Sleep(m_pApp->m_options.m_delay);
	}
	
	if (m_opts.m_scaling)
	{
		int n = m_opts.m_scale_num;
		int d = m_opts.m_scale_den;
		
		SetStretchBltMode(hdc, HALFTONE);
		SetBrushOrgEx(hdc, 0,0, NULL);
		{
			if(UltraFast && m_hmemdc)
			{
				ObjectSelector bb(m_hmemdc, m_membitmap);
				StretchBlt(
				hdc, 
				ps.rcPaint.left, 
				ps.rcPaint.top, 
				ps.rcPaint.right-ps.rcPaint.left, 
				ps.rcPaint.bottom-ps.rcPaint.top, 
				m_hmemdc, 
				(ps.rcPaint.left+m_hScrollPos)     * d / n, 
				(ps.rcPaint.top+m_vScrollPos)      * d / n,
				(ps.rcPaint.right-ps.rcPaint.left) * d / n, 
				(ps.rcPaint.bottom-ps.rcPaint.top) * d / n, 
				SRCCOPY);
			}
			else
			{
				if (!StretchBlt(
					hdc, 
					ps.rcPaint.left, 
					ps.rcPaint.top, 
					ps.rcPaint.right-ps.rcPaint.left, 
					ps.rcPaint.bottom-ps.rcPaint.top, 
					m_hBitmapDC, 
					(ps.rcPaint.left+m_hScrollPos)     * d / n, 
					(ps.rcPaint.top+m_vScrollPos)      * d / n,
					(ps.rcPaint.right-ps.rcPaint.left) * d / n, 
					(ps.rcPaint.bottom-ps.rcPaint.top) * d / n, 
					SRCCOPY)) 
				{
					vnclog.Print(0, _T("Blit error %d\n"), GetLastError());
					// throw ErrorException("Error in blit!\n");
				};
			}
		}
	}
	else
	{
		if (UltraFast && m_hmemdc)
		{
			ObjectSelector bb(m_hmemdc, m_membitmap);
			BitBlt(hdc, ps.rcPaint.left, ps.rcPaint.top, 
			ps.rcPaint.right-ps.rcPaint.left, ps.rcPaint.bottom-ps.rcPaint.top, 
			m_hmemdc, ps.rcPaint.left+m_hScrollPos, ps.rcPaint.top+m_vScrollPos, SRCCOPY);
		}
		else
		{
			if (!BitBlt(
					hdc,
					ps.rcPaint.left,
					ps.rcPaint.top , 
					ps.rcPaint.right-ps.rcPaint.left,
					ps.rcPaint.bottom-ps.rcPaint.top , 
					m_hBitmapDC,
					ps.rcPaint.left+m_hScrollPos,
					//ps.rcPaint.top +m_vScrollPos- (m_TBr.bottom - m_TBr.top)  , SRCCOPY)) 
					ps.rcPaint.top +m_vScrollPos,
					SRCCOPY)) 
			{
				vnclog.Print(0, _T("Blit error %d\n"), GetLastError());
				// throw ErrorException("Error in blit!\n");
			}
		}
	}
	EndPaint(m_hwnd, &ps);

}

inline void ClientConnection::UpdateScrollbars() 
{
	// We don't update the actual scrollbar info in full-screen mode
	// because it causes them to flicker.
	bool setInfo = !InFullScreenMode();

	SCROLLINFO scri;
	scri.cbSize = sizeof(scri);
	scri.fMask = SIF_ALL;
	scri.nMin = 0;
	scri.nMax = m_hScrollMax; 
	scri.nPage= m_cliwidth;
	scri.nPos = m_hScrollPos; 
	
	if (setInfo) 
		SetScrollInfo(m_hwndMain, SB_HORZ, &scri, TRUE);
	
	scri.cbSize = sizeof(scri);
	scri.fMask = SIF_ALL;
	scri.nMin = 0;

	scri.nMax = m_vScrollMax ;
	scri.nPage= m_cliheight;
	scri.nPos = m_vScrollPos; 
	
	if (setInfo) 
		SetScrollInfo(m_hwndMain, SB_VERT, &scri, TRUE);
}


void ClientConnection::ShowConnInfo()
{
	TCHAR buf[2048];
#ifndef UNDER_CE
	char kbdname[9];
	GetKeyboardLayoutName(kbdname);
#else
	TCHAR *kbdname = _T("(n/a)");
#endif
	TCHAR num[16];
	_stprintf(
		buf,
		_T("Connected to: %s\n\r\n\r")
		_T("Host: %s  Port: %d\n\r")
		_T("%s %s  %s\n\r\n\r")
		_T("Desktop geometry: %d x %d x %d\n\r")
		_T("Using depth: %d\n\r")
		_T("Line speed estimate: %d kbit/s\n")
		_T("Current protocol version: %d.%d\n\r\n\r")
		_T("Current keyboard name: %s\n\r\n\r")
		_T("Using Plugin : %s - %s\n\r\n\r"), // sf@2002 - v1.1.2
		m_desktopName, m_host, m_port,
		strcmp(m_proxyhost,"") ? m_proxyhost : "", 
		strcmp(m_proxyhost,"") ? "Port" : "", 
		strcmp(m_proxyhost,"") ? itoa(m_proxyport, num, 10) : "", 
		m_si.framebufferWidth, m_si.framebufferHeight,
                m_si.format.depth,
		m_myFormat.depth, kbitsPerSecond,
		m_majorVersion, m_minorVersion,
		kbdname,
		m_pDSMPlugin->IsEnabled() ? m_pDSMPlugin->GetPluginName() : "",
		m_pDSMPlugin->IsEnabled() ? m_pDSMPlugin->GetPluginVersion() : "");
	MessageBox(NULL, buf, _T("Connection info"), MB_ICONINFORMATION | MB_OK | MB_SETFOREGROUND | MB_TOPMOST);
}

// ********************************************************************
//  Methods after this point are generally called by the worker thread.
//  They finish the initialisation, then chiefly read data from the server.
// ********************************************************************


void* ClientConnection::run_undetached(void* arg) {

	vnclog.Print(9,_T("Update-processing thread started\n"));

	m_threadStarted = true;
	try
	{
		// Modif sf@2002 - Server Scaling
		m_nServerScale = m_opts.m_nServerScale;
		if (m_nServerScale > 1) SendServerScale(m_nServerScale);

		SendFullFramebufferUpdateRequest();

		SizeWindow();
		RealiseFullScreenMode();
		if (!InFullScreenMode()) SizeWindow();

		m_running = true;
		UpdateWindow(m_hwnd);

		// sf@2002 - Attempt to speed up the thing
		// omni_thread::set_priority(omni_thread::PRIORITY_LOW);

        rdr::U8 msgType;
		while (!m_bKillThread) 
		{
			// sf@2002 - DSM Plugin
			if (!m_fUsePlugin)			
			{
				msgType = fis->readU8();
				m_nTO = 1; // Read the rest of the rfb message (normal case)
			}
			else if (m_pDSMPlugin->IsEnabled())
			{
				// Read the additional type char sent by the DSM Plugin (server)
				// We need it to know the type of rfb message that follows
				// because we can't see the type inside the transformed rfb message.
				ReadExact((char *)&msgType, sizeof(msgType));
				m_nTO = 0; // we'll need to read the whole transformed rfb message that follows
			}
				
            switch (msgType)
			{
			case rfbFramebufferUpdate:
				ReadScreenUpdate();
				break;

			case rfbSetColourMapEntries:
		        vnclog.Print(3, _T("rfbSetColourMapEntries read but not supported\n") );
				throw WarningException(sz_L63);
				break;

			case rfbBell:
				ReadBell();
				break;

			case rfbServerCutText:
				ReadServerCutText();
				break;

			// Modif sf@2002 - FileTransfer
			// File Transfer Message
			case rfbFileTransfer:
				{
				// vnclog.Print(0, _T("rfbFileTransfer\n") );
				// m_pFileTransfer->ProcessFileTransferMsg();
				// sf@2005 - FileTransfer rfbMessage and screen updates must be sent/received 
				// by the same thread
				SendMessage(m_hwndMain, FileTransferSendPacketMessage, 1, 0); 
				}
				break;

			// Modif sf@2002 - Text Chat
			case rfbTextChat:
				m_pTextChat->ProcessTextChatMsg();
				break;

			// Modif sf@2002 - Server Scaling
			// Server Scaled screen buffer size has changed, so we resize
			// the viewer window
			case rfbResizeFrameBuffer:
			{
				rfbResizeFrameBufferMsg rsmsg;
				ReadExact(((char*)&rsmsg) + m_nTO, sz_rfbResizeFrameBufferMsg - m_nTO);

				m_si.framebufferWidth = Swap16IfLE(rsmsg.framebufferWidth);
				m_si.framebufferHeight = Swap16IfLE(rsmsg.framebufferHeigth);

				ClearCache();
				CreateLocalFramebuffer();
				// SendFullFramebufferUpdateRequest();
				Createdib();
				m_pendingScaleChange = true;
				m_pendingFormatChange = true;
				SendAppropriateFramebufferUpdateRequest();
				
				SizeWindow();
				InvalidateRect(m_hwnd, NULL, TRUE);
				RealiseFullScreenMode();	
				break;
			}

			default:
                      vnclog.Print(3, _T("Unknown message type x%02x\n"), msgType );
                      throw WarningException(sz_L64);
                      break;

			/*
			default:
                log.Print(3, _T("Unknown message type x%02x\n"), msgType );
				throw WarningException("Unhandled message type received!\n");
			*/
			}
			// yield();
		}
        
	}
	catch (WarningException)
	{
		// sf@2002
		// m_pFileTransfer->m_fFileTransferRunning = false;
		// m_pTextChat->m_fTextChatRunning = false;
		PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
	}
	catch (QuietException &e)
	{
		// sf@2002
		// m_pFileTransfer->m_fFileTransferRunning = false;
		// m_pTextChat->m_fTextChatRunning = false;
		PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
	}
	catch (rdr::Exception& e)
	{
		vnclog.Print(0,"rdr::Exception (1): %s\n",e.str());
		// m_pFileTransfer->m_fFileTransferRunning = false;
		// m_pTextChat->m_fTextChatRunning = false;
		// throw QuietException(e.str());
		PostMessage(m_hwndMain, WM_CLOSE, 0, 0);
	}
	SetEvent(KillEvent);
		// sf@2002
	m_pFileTransfer->m_fFileTransferRunning = false;
	m_pTextChat->m_fTextChatRunning = false;

	vnclog.Print(4, _T("Update-processing thread finishing\n") );
	return this;
}


//
// Requesting screen updates from the server
//

inline void
ClientConnection::SendFramebufferUpdateRequest(int x, int y, int w, int h, bool incremental)
{
	if (m_pFileTransfer->m_fFileTransferRunning && ( m_pFileTransfer->m_fVisible || m_pFileTransfer->m_fOldFTProtocole)) return;
	if (m_pTextChat->m_fTextChatRunning && m_pTextChat->m_fVisible) return;

	//omni_mutex_lock l(m_UpdateMutex);

    rfbFramebufferUpdateRequestMsg fur;

	// vnclog.Print(0, _T("Request %s update x=%d,y=%d,w=%d,h=%d\n"), incremental ? _T("incremental") : _T("full"),x,y,w,h);

    fur.type = rfbFramebufferUpdateRequest;
    fur.incremental = incremental ? 1 : 0;
    fur.x = Swap16IfLE(x);
    fur.y = Swap16IfLE(y);
    fur.w = Swap16IfLE(w);
    fur.h = Swap16IfLE(h);

	//vnclog.Print(10, _T("Request %s update\n"), incremental ? _T("incremental") : _T("full"));	
    WriteExact((char *)&fur, sz_rfbFramebufferUpdateRequestMsg, rfbFramebufferUpdateRequest);
}

inline void ClientConnection::SendIncrementalFramebufferUpdateRequest()
{
    SendFramebufferUpdateRequest(0, 0, m_si.framebufferWidth,
					m_si.framebufferHeight, true);
}

void ClientConnection::SendFullFramebufferUpdateRequest()
{
    SendFramebufferUpdateRequest(0, 0, m_si.framebufferWidth,
					m_si.framebufferHeight, false);
}


void ClientConnection::SendAppropriateFramebufferUpdateRequest()
{
	if (m_pendingFormatChange) 
	{
		vnclog.Print(3, _T("Requesting new pixel format\n") );

		// Cache init/reinit - A SetFormatAndEncoding() implies a cache reinit on server side
		// Cache enabled, so it's going to be reallocated/reinited on server side
		if (m_opts.m_fEnableCache)
		{
			// create viewer cache buffer if necessary
			if (m_hCacheBitmap == NULL)
			{
				m_hCacheBitmapDC = CreateCompatibleDC(m_hBitmapDC);
				m_hCacheBitmap = CreateCompatibleBitmap(m_hBitmapDC, m_si.framebufferWidth, m_si.framebufferHeight);
			}
			ClearCache(); // Clear the cache
			m_pendingCacheInit = true; // Order full update to synchronize both sides caches
		}
		else // No cache requested - The cache on the other side is to be cleared/deleted
			 // Todo: fix the cache switching pb when viewer has been started without cache
		{
			/* causes balck rects after cache off/on
			// Delete local cache
			DeleteDC(m_hCacheBitmapDC);
			if (m_hCacheBitmap != NULL) DeleteObject(m_hCacheBitmap);
			if (m_hCacheBitmapDC != NULL) DeleteObject(m_hCacheBitmapDC);			
			m_hCacheBitmap = NULL;
			m_pendingCacheInit = false;
			*/
		}
		
		rfbPixelFormat oldFormat = m_myFormat;
		SetupPixelFormat();
		// tight cursor handling
		SoftCursorFree();
		Createdib();
		SetFormatAndEncodings();
		m_pendingFormatChange = false;

		// If the pixel format has changed, or cache, or scale request whole screen
		if (!PF_EQ(m_myFormat, oldFormat) || m_pendingCacheInit || m_pendingScaleChange)
		{
			SendFullFramebufferUpdateRequest();	
		}
		else
		{
			SendIncrementalFramebufferUpdateRequest();
		}
		m_pendingScaleChange = false;
		m_pendingCacheInit = false;
	}
	else 
	{
		if (!m_dormant)
			SendIncrementalFramebufferUpdateRequest();
	}
}

//
// Modif sf@2002 - Server Scaling
//
bool ClientConnection::SendServerScale(int nScale)
{
    rfbSetScaleMsg ssc;
    int len = 0;

    ssc.type = rfbSetScale;
    ssc.scale = /*(unsigned short)*/ nScale;

    WriteExact((char*)&ssc, sz_rfbSetScaleMsg, rfbSetScale);

    return true;
}

//
// Modif rdv@2002 - Set Server input 
//
bool ClientConnection::SendServerInput(BOOL enabled)
{
    rfbSetServerInputMsg sim;
    int len = 0;

    sim.type = rfbSetServerInput;
    sim.status = enabled;

    WriteExact((char*)&sim, sz_rfbSetServerInputMsg, rfbSetServerInput);

    return true;
}

//
// Modif rdv@2002 - Single window
//
bool ClientConnection::SendSW(int x, int y)
{
    rfbSetSWMsg sw;
    int len = 0;
	if (x==9999 && y==9999)
	{
		sw.type = rfbSetSW;
		sw.x = Swap16IfLE(1);
		sw.y = Swap16IfLE(1);
	}
	else
	{
		int x_scaled =
			(x + m_hScrollPos) * m_opts.m_scale_den / m_opts.m_scale_num;
		int y_scaled =
			(y + m_vScrollPos) * m_opts.m_scale_den / m_opts.m_scale_num;
		
		sw.type = rfbSetSW;
		sw.x = Swap16IfLE(x_scaled);
		sw.y = Swap16IfLE(y_scaled);
	}
	
    WriteExact((char*)&sw, sz_rfbSetSWMsg, rfbSetSW);
	m_SWselect=false;
    return true;
}


// A ScreenUpdate message has been received
inline void ClientConnection::ReadScreenUpdate()
{
	HDC hdcX,hdcBits;

	bool fTimingAlreadyStopped = false;
	fis->startTiming();
	
	rfbFramebufferUpdateMsg sut;
	ReadExact(((char *) &sut)+m_nTO, sz_rfbFramebufferUpdateMsg-m_nTO);
    sut.nRects = Swap16IfLE(sut.nRects);
	HRGN UpdateRegion=CreateRectRgn(0,0,0,0);
	bool Recover_from_sync=false;
	
    //if (sut.nRects == 0) return;  XXX tjr removed this - is this OK?
	
	for (UINT i=0; i < sut.nRects; i++)
	{
		rfbFramebufferUpdateRectHeader surh;
		ReadExact((char *) &surh, sz_rfbFramebufferUpdateRectHeader);
		surh.r.x = Swap16IfLE(surh.r.x);
		surh.r.y = Swap16IfLE(surh.r.y);
		surh.r.w = Swap16IfLE(surh.r.w);
		surh.r.h = Swap16IfLE(surh.r.h);
		surh.encoding = Swap32IfLE(surh.encoding);
		// vnclog.Print(0, _T("%d %d\n"), i,sut.nRects);
		//vnclog.Print(0, _T("encoding %d\n"), surh.encoding);
		
		// Tight - If lastrect we must quit this loop (nRects = 0xFFFF)
		if (surh.encoding == rfbEncodingLastRect)
			break;
		
		if (surh.encoding == rfbEncodingNewFBSize)
		{
			ReadNewFBSize(&surh);
			break;
		}
		
		// Tight cursor handling
		if ( surh.encoding == rfbEncodingXCursor ||
			surh.encoding == rfbEncodingRichCursor )
		{
			ReadCursorShape(&surh);
			continue;
		}
		
        // marscha PointerPos	
		if (surh.encoding == rfbEncodingPointerPos) {
			//vnclog.Print(0, _T("reading cursorPos (%d,%d)\n"), surh.r.x, surh.r.y);
			ReadCursorPos(&surh);
			continue;
		}

		if (surh.encoding !=rfbEncodingNewFBSize && surh.encoding != rfbEncodingCacheZip && surh.encoding != rfbEncodingSolMonoZip && surh.encoding !=rfbEncodingUltraZip)
			SoftCursorLockArea(surh.r.x, surh.r.y, surh.r.w, surh.r.h);
		
		// Modif sf@2002 - DSM Plugin
		// With DSM, all rects contents (excepted caches) are buffered into memory in one shot
		// then they will be read in this buffer by the "regular" Read*Type*Rect() functions
		if (m_fUsePlugin && m_pDSMPlugin->IsEnabled())
		{
			if (!m_fReadFromNetRectBuf)
			{
				switch (surh.encoding)
				{
				case rfbEncodingRaw:
				case rfbEncodingRRE: 
				case rfbEncodingCoRRE:
				case rfbEncodingHextile:
				case rfbEncodingUltra:
				case rfbEncodingZlib:
				case rfbEncodingXOR_Zlib:
				case rfbEncodingXORMultiColor_Zlib:
				case rfbEncodingXORMonoColor_Zlib:
				case rfbEncodingSolidColor:
				case rfbEncodingTight:
				case rfbEncodingZlibHex:
					{
						// Get the size of the rectangle data buffer
						ReadExact((char*)&(m_nReadSize), sizeof(CARD32));
						m_nReadSize = Swap32IfLE(m_nReadSize);
						// Read the whole  rect buffer and put the result in m_netbuf
						CheckNetRectBufferSize((int)(m_nReadSize));
						CheckBufferSize((int)(m_nReadSize)); // sf@2003
						ReadExact((char*)(m_pNetRectBuf), (int)(m_nReadSize));
						// Tell the following ReadExact() function calls to Read Data from memory
						m_nNetRectBufOffset = 0;
						m_fReadFromNetRectBuf = true;
					}
					break;
				}
			}
			
			// ZRLE special case
			if (!fis->GetReadFromMemoryBuffer())
			{
				if (surh.encoding == rfbEncodingZRLE)
				{
					// Get the size of the rectangle data buffer
					ReadExact((char*)&(m_nZRLEReadSize), sizeof(CARD32));
					m_nZRLEReadSize = Swap32IfLE(m_nZRLEReadSize);
					// Read the whole  rect buffer and put the result in m_netbuf
					CheckZRLENetRectBufferSize((int)(m_nZRLEReadSize));
					CheckBufferSize((int)(m_nZRLEReadSize)); // sf@2003
					ReadExact((char*)(m_pZRLENetRectBuf), (int)(m_nZRLEReadSize));
					// Tell the following Read() function calls to Read Data from memory
					fis->SetReadFromMemoryBuffer(m_nZRLEReadSize, (char*)(m_pZRLENetRectBuf));
				}
			}
		}
		
		RECT cacherect;
		if (m_opts.m_fEnableCache)
		{
			cacherect.left=surh.r.x;
			cacherect.right=surh.r.x+surh.r.w;
			cacherect.top=surh.r.y;
			cacherect.bottom=surh.r.y+surh.r.h;
		}

		if (m_TrafficMonitor)
		{
			hdcX = GetDC(m_TrafficMonitor);
			hdcBits = CreateCompatibleDC(hdcX);
			SelectObject(hdcBits,m_bitmapBACK);
			BitBlt(hdcX,4,2,22,20,hdcBits,0,0,SRCCOPY);
			DeleteDC(hdcBits);
			ReleaseDC(m_TrafficMonitor,hdcX);
		}

		//vnclog.Print(0, _T("encoding %d\n"), surh.encoding);
		if (surh.encoding==rfbEncodingUltra || surh.encoding==rfbEncodingUltraZip)
		{
			UltraFast=true;
		}
		else
		{
		// sf@2003 - The only one that is missing in the test below is "raw"... maybe a "else" would be enought ?
		/*
		if (surh.encoding==rfbEncodingHextile || surh.encoding==rfbEncodingTight || surh.encoding==rfbEncodingZRLE
			|| surh.encoding==rfbEncodingSolidColor || surh.encoding==rfbEncodingSolMonoZip
			|| surh.encoding==rfbEncodingRRE || surh.encoding==rfbEncodingCoRRE|| surh.encoding==rfbEncodingZlib
			|| surh.encoding==rfbEncodingZlibHex || surh.encoding==rfbEncodingXOR_Zlib|| surh.encoding==rfbEncodingXORMultiColor_Zlib
		
			)*/
				UltraFast=false;
		}

		// sf@2004
		if (m_fUsePlugin && m_pDSMPlugin->IsEnabled() && (m_fReadFromNetRectBuf || fis->GetReadFromMemoryBuffer()))
		{
			fis->stopTiming();
			kbitsPerSecond = fis->kbitsPerSecond();
			fTimingAlreadyStopped = true;
		}

			// vnclog.Print(0, _T("known encoding %d - not supported!\n"), surh.encoding);
		switch (surh.encoding)
		{
		case rfbEncodingHextile:
			SaveArea(cacherect);
			ReadHextileRect(&surh);
			EncodingStatusWindow=rfbEncodingHextile;
			break;
		case rfbEncodingUltra:
			ReadUltraRect(&surh);
			EncodingStatusWindow=rfbEncodingUltra;
			break;
		case rfbEncodingUltraZip:
			ReadUltraZip(&surh,&UpdateRegion);
			break;
		case rfbEncodingRaw:
			SaveArea(cacherect);
			ReadRawRect(&surh);
			EncodingStatusWindow=rfbEncodingRaw;
			break;
		case rfbEncodingCopyRect:
			ReadCopyRect(&surh);
			break;
		case rfbEncodingCache:
			ReadCacheRect(&surh);
			break;
		case rfbEncodingCacheZip:
			ReadCacheZip(&surh,&UpdateRegion);
			break;
		case rfbEncodingSolMonoZip:
			ReadSolMonoZip(&surh,&UpdateRegion);
			break;
		case rfbEncodingRRE:
			SaveArea(cacherect);
			ReadRRERect(&surh);
			EncodingStatusWindow=rfbEncodingRRE;
			break;
		case rfbEncodingCoRRE:
			SaveArea(cacherect);
			ReadCoRRERect(&surh);
			EncodingStatusWindow=rfbEncodingCoRRE;
			break;
		case rfbEncodingZlib:
			SaveArea(cacherect);
			ReadZlibRect(&surh,0);
			EncodingStatusWindow=rfbEncodingZlib;
			break;
		case rfbEncodingZlibHex:
			SaveArea(cacherect);
			ReadZlibHexRect(&surh);
			EncodingStatusWindow=rfbEncodingZlibHex;
			break;
		case rfbEncodingXOR_Zlib:
			SaveArea(cacherect);
			ReadZlibRect(&surh,1);
			break;
		case rfbEncodingXORMultiColor_Zlib:
			SaveArea(cacherect);
			ReadZlibRect(&surh,2);
			break;
		case rfbEncodingXORMonoColor_Zlib:
			SaveArea(cacherect);
			ReadZlibRect(&surh,3);
			break;
		case rfbEncodingSolidColor:
			SaveArea(cacherect);
			ReadSolidRect(&surh);
			break;
		case rfbEncodingZRLE:
			SaveArea(cacherect);
			zrleDecode(surh.r.x, surh.r.y, surh.r.w, surh.r.h);
			EncodingStatusWindow=rfbEncodingZRLE;
			break;
		case rfbEncodingTight:
			SaveArea(cacherect);
			ReadTightRect(&surh);
			EncodingStatusWindow=rfbEncodingTight;
			break;
		default:
			// vnclog.Print(0, _T("Unknown encoding %d - not supported!\n"), surh.encoding);
			// Try to empty buffer...
			// so next update should be back in sync
			BYTE * buffer;
			int i=0;
			while (TRUE)
			{
				int aantal=fis->Check_if_buffer_has_data();
				if (aantal>0) buffer = new BYTE [aantal];
				if (aantal>0)
				{
					i=0;
					ReadExact(((char *) buffer), aantal);
					delete [] buffer;
					Sleep(5);
				}
				else if (aantal==0)
				{
					if (i==5) break;
					Sleep(200);
					i++;
				}
				else break;   
			}
			vnclog.Print(0, _T("Buffer cleared, sync should be back OK..Continue \n"));
			Recover_from_sync=true;
			break;
		}
		if (Recover_from_sync) 
		{
		    Recover_from_sync=false;
			break;
		}
		
		if (surh.encoding !=rfbEncodingNewFBSize && surh.encoding != rfbEncodingCacheZip && surh.encoding != rfbEncodingSolMonoZip && surh.encoding != rfbEncodingUltraZip)
		{
			RECT rect;
			rect.left   = surh.r.x;
			rect.top    = surh.r.y;
			rect.right  = surh.r.x + surh.r.w ;
			rect.bottom = surh.r.y + surh.r.h; 
			InvalidateScreenRect(&rect); 			
		}
		else if (surh.encoding !=rfbEncodingNewFBSize)
		{
			InvalidateRgn(m_hwnd, UpdateRegion, FALSE);
			HRGN tempregion=CreateRectRgn(0,0,0,0);
			CombineRgn(UpdateRegion,UpdateRegion,tempregion,RGN_AND);
			DeleteObject(tempregion);
		}

		if (m_TrafficMonitor)
		{
			hdcX = GetDC(m_TrafficMonitor);
			hdcBits = CreateCompatibleDC(hdcX);
			SelectObject(hdcBits,m_bitmapNONE);
			BitBlt(hdcX,4,2,22,20,hdcBits,0,0,SRCCOPY);
			DeleteDC(hdcBits);
			ReleaseDC(m_TrafficMonitor,hdcX);
		}

		SoftCursorUnlockScreen();
	}

	if (!fTimingAlreadyStopped)
	{
		fis->stopTiming();
		kbitsPerSecond = fis->kbitsPerSecond();
	}

	// sf@2002
	// We only change the preferred encoding if FileTransfer is not running and if
	// the last encoding change occured more than 30s ago
	if (m_opts.autoDetect 
		&&
		!m_pFileTransfer->m_fFileTransferRunning
		&& 
		(timeGetTime() - m_lLastChangeTime) > 30000)
	{
		int nOldServerScale = m_nServerScale;

		// If connection speed > 1Mbits/s - All to the max
		/*if (kbitsPerSecond > 2000 && (m_nConfig != 7))
		{
			m_nConfig = 1;
			m_opts.m_PreferredEncoding = rfbEncodingUltra;
			m_opts.m_Use8Bit = false; // Max colors
			m_opts.m_fEnableCache = false;
			m_pendingFormatChange = true;
			m_lLastChangeTime = timeGetTime();
		}*/

		if (kbitsPerSecond > 1000 && (m_nConfig != 1))
		{
			m_nConfig = 1;
			m_opts.m_PreferredEncoding = rfbEncodingHextile;
			m_opts.m_Use8Bit = rfbPFFullColors; // Max colors
			m_opts.m_fEnableCache = false;
			m_pendingFormatChange = true;
			m_lLastChangeTime = timeGetTime();
		}
		// Medium connection speed 
		else if (kbitsPerSecond < 256 && kbitsPerSecond > 128 && (m_nConfig != 2))
		{
			m_nConfig = 2;
			m_opts.m_PreferredEncoding = rfbEncodingZRLE; //rfbEncodingZlibHex;
			m_opts.m_Use8Bit = rfbPF256Colors; 
			// m_opts.m_compressLevel = 9;
			m_opts.m_fEnableCache = false;
			m_pendingFormatChange = true;
			m_lLastChangeTime = timeGetTime();
		}
		// Modem (including cable modem) connection speed 
		else if (kbitsPerSecond < 128 && kbitsPerSecond > 19 && (m_nConfig != 3))
		{
			m_nConfig = 3;
			m_opts.m_PreferredEncoding = rfbEncodingTight; // rfbEncodingZRLE;
			m_opts.m_Use8Bit = rfbPF64Colors; 
			// m_opts.m_compressLevel = 9;
			m_opts.m_fEnableCache = false;
			m_pendingFormatChange = true;
			m_lLastChangeTime = timeGetTime();
		}
		
		// Slow Modem connection speed 
		// Not sure it's a good thing in Auto mode...because in some cases
		// (CTRL-ALT-DEL, initial screen loading, connection short hangups...)
		// the speed can be momentary VERY slow. The fast fuzzy/normal modes switching
		// can be quite disturbing and useless in these situations.
		else if (kbitsPerSecond < 19 && kbitsPerSecond > 5 && (m_nConfig != 4))
		{
			m_nConfig = 4;
			m_opts.m_PreferredEncoding = rfbEncodingTight; //rfbEncodingZRLE;
			m_opts.m_Use8Bit = rfbPF8Colors; 
			// m_opts.m_compressLevel = 9; 
			// m_opts.m_scaling = true;
			// m_opts.m_scale_num = 2;
			// m_opts.m_scale_den = 1;
			// m_nServerScale = 2;
			// m_opts.m_nServerScale = 2;
			m_opts.m_fEnableCache = false;
			m_pendingFormatChange = true;
		}
		/*
		if (m_nServerScale != nOldServerScale)
		{
			SendServerScale(m_nServerScale);
		}
		*/
    }
		
	// Inform the other thread that an update is needed.
	
	PostMessage(m_hwnd, WM_REGIONUPDATED, NULL, NULL);
	DeleteObject(UpdateRegion);
}	



void ClientConnection::SetDormant(bool newstate)
{
	vnclog.Print(5, _T("%s dormant mode\n"), newstate ? _T("Entering") : _T("Leaving"));
	m_dormant = newstate;
	if (!m_dormant)
		SendIncrementalFramebufferUpdateRequest();
}

// The server has copied some text to the clipboard - put it 
// in the local clipboard too.

void ClientConnection::ReadServerCutText() 
{
	rfbServerCutTextMsg sctm;
	vnclog.Print(6, _T("Read remote clipboard change\n"));
	ReadExact(((char *) &sctm)+m_nTO, sz_rfbServerCutTextMsg-m_nTO);
	int len = Swap32IfLE(sctm.length);
	
	CheckBufferSize(len);
	if (len == 0) {
		m_netbuf[0] = '\0';
	} else {
		ReadString(m_netbuf, len);
	}
	UpdateLocalClipboard(m_netbuf, len);
}


void ClientConnection::ReadBell() 
{
	rfbBellMsg bm;
	ReadExact(((char *) &bm)+m_nTO, sz_rfbBellMsg-m_nTO);

	#ifdef UNDER_CE
	MessageBeep( MB_OK );
	#else

	if (! ::PlaySound("VNCViewerBell", NULL, 
		SND_APPLICATION | SND_ALIAS | SND_NODEFAULT | SND_ASYNC) ) {
		::Beep(440, 125);
	}
	#endif
	if (m_opts.m_DeiconifyOnBell) {
		if (IsIconic(m_hwnd)) {
			SetDormant(false);
			ShowWindow(m_hwnd, SW_SHOWNORMAL);
		}
	}
	vnclog.Print(6, _T("Bell!\n"));
}


// General utilities -------------------------------------------------

// Reads the number of bytes specified into the buffer given

void ClientConnection::ReadExact(char *inbuf, int wanted)
{
	//omni_mutex_lock l(m_readMutex);
	// Status window and connection activity updates
	// We comment this because it just takes too much time to the viewer thread
	/*
	HDC hdcX,hdcBits;
	if (m_TrafficMonitor)
	{
		hdcX = GetDC(m_TrafficMonitor);
		hdcBits = CreateCompatibleDC(hdcX);
		SelectObject(hdcBits,m_bitmapBACK);
		BitBlt(hdcX,1,1,22,20,hdcBits,0,0,SRCCOPY);
		DeleteDC(hdcBits);
		ReleaseDC(m_TrafficMonitor,hdcX);
	}
	*/

	// m_BytesRead += wanted;
	/*
	m_BytesRead = fis->GetBytesRead();
	SetDlgItemInt(m_hwndStatus, IDC_RECEIVED, m_BytesRead, false);
	SetDlgItemInt(m_hwndStatus, IDC_SPEED, kbitsPerSecond, false);
	*/
	try
	{
		// sf@2002 - DSM Plugin
		if (m_fUsePlugin)
		{
			if (m_pDSMPlugin->IsEnabled())
			{
				omni_mutex_lock l(m_pDSMPlugin->m_RestMutex); 

				// If we must read already restored data from memory
				if (m_fReadFromNetRectBuf)
				{
					memcpy(inbuf, m_pNetRectBuf + m_nNetRectBufOffset, wanted);
					m_nNetRectBufOffset += wanted;
					if (m_nNetRectBufOffset == m_nReadSize)
					{
						// Next ReadExact calls should read the socket
						m_fReadFromNetRectBuf = false;
						m_nNetRectBufOffset = 0;
					}
				}
				// Read restored data from ZRLE mem netbuffer
				else if (fis->GetReadFromMemoryBuffer())
				{
					fis->readBytes(inbuf, wanted);
				}
				else // read tansformed data from the socket (normal case)
				{
					// Get the DSMPlugin destination buffer where to put transformed incoming data
					// The number of bytes to read calculated from bufflen is given back in nTransDataLen
					int nTransDataLen = 0;
					BYTE* pTransBuffer = m_pDSMPlugin->RestoreBufferStep1(NULL, wanted, &nTransDataLen);
					if (pTransBuffer == NULL)
					{
						// m_pDSMPlugin->RestoreBufferUnlock();
						throw WarningException(sz_L65);
					}
					
					// Read bytes directly into Plugin Dest rest. buffer
					fis->readBytes(pTransBuffer, nTransDataLen);
					
					// Ask plugin to restore data from its local rest. buffer into inbuf
					int nRestDataLen = 0;
					m_pDSMPlugin->RestoreBufferStep2((BYTE*)inbuf, nTransDataLen, &nRestDataLen);
					
					// Check if we actually get the real original data length
					if (nRestDataLen != wanted)
					{
						throw WarningException(sz_L66);
					}
				}
			}
			else
			{
				fis->readBytes(inbuf, wanted);
			}
		}
		else
		{
			fis->readBytes(inbuf, wanted);
		}
	}
	catch (rdr::Exception& e)
	{
		vnclog.Print(0, "rdr::Exception (2): %s\n",e.str());
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L67);
		throw QuietException(e.str());
	}

	// Too slow !
	/*
	if (m_TrafficMonitor)
	{
		hdcX = GetDC(m_TrafficMonitor);
		hdcBits = CreateCompatibleDC(hdcX);
		SelectObject(hdcBits,m_bitmapNONE);
		BitBlt(hdcX,1,1,22,20,hdcBits,0,0,SRCCOPY);
		DeleteDC(hdcBits);
		ReleaseDC(m_TrafficMonitor,hdcX);
	}
	*/

}

void ClientConnection::ReadExactProxy(char *inbuf, int wanted)
{
	//omni_mutex_lock l(m_readMutex);
	// Status window and connection activity updates
	// We comment this because it just takes too much time to the viewer thread
	
	try
	{
		
		{
			fis->readBytes(inbuf, wanted);
		}
	}
	catch (rdr::Exception& e)
	{
		vnclog.Print(0, "rdr::Exception (2): %s\n",e.str());
		if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_STATUS,sz_L67);
		throw QuietException(e.str());
	}

}

// Read the number of bytes and return them zero terminated in the buffer 
/*inline*/ void ClientConnection::ReadString(char *buf, int length)
{
	if (length > 0)
		ReadExact(buf, length);
	buf[length] = '\0';
    vnclog.Print(10, _T("Read a %d-byte string\n"), length);
}

//
// sf@2002 - DSM Plugin
//
void ClientConnection::WriteExact(char *buf, int bytes, CARD8 msgType)
{
	if (!m_fUsePlugin)
	{
		WriteExact(buf, bytes);
	}
	else if (m_pDSMPlugin->IsEnabled())
	{
		// Send the transformed message type first 
		WriteExact((char*)&msgType, sizeof(msgType));
		// Then send the transformed rfb message content
		WriteExact(buf, bytes);
	}
}

// Sends the number of bytes specified from the buffer
void ClientConnection::WriteExact(char *buf, int bytes)
{

	if (bytes == 0) return;
	// Too slow
	/*
	HDC hdcX,hdcBits;
	if (m_TrafficMonitor)
	{
		hdcX = GetDC(m_TrafficMonitor);
		hdcBits = CreateCompatibleDC(hdcX);
		SelectObject(hdcBits,m_bitmapFRONT);
		BitBlt(hdcX,1,1,22,20,hdcBits,0,0,SRCCOPY);
		DeleteDC(hdcBits);
		ReleaseDC(m_TrafficMonitor,hdcX);
	}
	*/

	omni_mutex_lock l(m_writeMutex);
	//vnclog.Print(10, _T("  writing %d bytes\n"), bytes);

	m_BytesSend += bytes;
/*
	SetDlgItemInt(m_hwndStatus,IDC_SEND,m_BytesSend,false);
*/
	int i = 0;
    int j;

	// sf@2002 - DSM Plugin
	char *pBuffer = buf;
	if (m_fUsePlugin)
	{
		if (m_pDSMPlugin->IsEnabled())
		{
			int nTransDataLen = 0;
			pBuffer = (char*)(m_pDSMPlugin->TransformBuffer((BYTE*)buf, bytes, &nTransDataLen));
			if (pBuffer == NULL || (bytes > 0 && nTransDataLen == 0))
				throw WarningException(sz_L68);
			bytes = nTransDataLen;
		}
	}

    while (i < bytes)
	{
		j = send(m_sock, pBuffer+i, bytes-i, 0);
		if (j == SOCKET_ERROR || j==0)
		{
			LPVOID lpMsgBuf;
			int err = ::GetLastError();
			FormatMessage(     
				FORMAT_MESSAGE_ALLOCATE_BUFFER | 
				FORMAT_MESSAGE_FROM_SYSTEM |     
				FORMAT_MESSAGE_IGNORE_INSERTS, NULL,
				err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
				(LPTSTR) &lpMsgBuf, 0, NULL ); // Process any inserts in lpMsgBuf.
			vnclog.Print(1, _T("Socket error %d: %s\n"), err, lpMsgBuf);
			LocalFree( lpMsgBuf );
			m_running = false;

			throw WarningException(sz_L69);
		}
		i += j;
    }

	// Too slow
	/*
	if (m_TrafficMonitor)
	{
		hdcX = GetDC(m_TrafficMonitor);
		hdcBits = CreateCompatibleDC(hdcX);
		SelectObject(hdcBits,m_bitmapNONE);
		BitBlt(hdcX,1,1,22,20,hdcBits,0,0,SRCCOPY);
		DeleteDC(hdcBits);
		ReleaseDC(m_TrafficMonitor,hdcX);
	}
	*/

}



// Sends the number of bytes specified from the buffer
void ClientConnection::WriteExactProxy(char *buf, int bytes)
{

	if (bytes == 0) return;
	omni_mutex_lock l(m_writeMutex);
	//vnclog.Print(10, _T("  writing %d bytes\n"), bytes);

	m_BytesSend += bytes;
/*
	SetDlgItemInt(m_hwndStatus,IDC_SEND,m_BytesSend,false);
*/
	int i = 0;
    int j;

	// sf@2002 - DSM Plugin
	char *pBuffer = buf;

    while (i < bytes)
	{
		j = send(m_sock, pBuffer+i, bytes-i, 0);
		if (j == SOCKET_ERROR || j==0)
		{
			LPVOID lpMsgBuf;
			int err = ::GetLastError();
			FormatMessage(     
				FORMAT_MESSAGE_ALLOCATE_BUFFER | 
				FORMAT_MESSAGE_FROM_SYSTEM |     
				FORMAT_MESSAGE_IGNORE_INSERTS, NULL,
				err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
				(LPTSTR) &lpMsgBuf, 0, NULL ); // Process any inserts in lpMsgBuf.
			vnclog.Print(1, _T("Socket error %d: %s\n"), err, lpMsgBuf);
			LocalFree( lpMsgBuf );
			m_running = false;

			throw WarningException(sz_L69);
		}
		i += j;
    }

}

// Makes sure netbuf is at least as big as the specified size.
// Note that netbuf itself may change as a result of this call.
// Throws an exception on failure.
void ClientConnection::CheckBufferSize(int bufsize)
{
	if (m_netbufsize > bufsize) return;

	omni_mutex_lock l(m_bufferMutex);

	char *newbuf = new char[bufsize+256];
	if (newbuf == NULL) {
		throw ErrorException(sz_L70);
	}

	// Only if we're successful...

	if (m_netbuf != NULL)
		delete [] m_netbuf;
	m_netbuf = newbuf;
	m_netbufsize=bufsize + 256;
	vnclog.Print(4, _T("bufsize expanded to %d\n"), m_netbufsize);
}


// Makes sure zipbuf is at least as big as the specified size.
// Note that zlibbuf itself may change as a result of this call.
// Throws an exception on failure.
// sf@2002

void ClientConnection::CheckZipBufferSize(int bufsize)
{
	unsigned char *newbuf;

	if (m_zipbufsize > bufsize) return;

	omni_mutex_lock l(m_ZipBufferMutex);

	newbuf = (unsigned char *)new char[bufsize + 256];
	if (newbuf == NULL) {
		throw ErrorException(sz_L71);
	}

	// Only if we're successful...

	if (m_zipbuf != NULL)
		delete [] m_zipbuf;
	m_zipbuf = newbuf;
	m_zipbufsize = bufsize + 256;
	vnclog.Print(4, _T("zipbufsize expanded to %d\n"), m_zipbufsize);


}

void ClientConnection::CheckFileZipBufferSize(int bufsize)
{
	unsigned char *newbuf;

	if (m_filezipbufsize > bufsize) return;

	omni_mutex_lock l(m_FileZipBufferMutex);

	newbuf = (unsigned char *)new char[bufsize + 256];
	if (newbuf == NULL) {
		throw ErrorException(sz_L71);
	}

	// Only if we're successful...

	if (m_filezipbuf != NULL)
		delete [] m_filezipbuf;
	m_filezipbuf = newbuf;
	m_filezipbufsize = bufsize + 256;
	vnclog.Print(4, _T("zipbufsize expanded to %d\n"), m_filezipbufsize);
}

void ClientConnection::CheckFileChunkBufferSize(int bufsize)
{
	unsigned char *newbuf;

	if (m_filechunkbufsize > bufsize) return;

	omni_mutex_lock l(m_FileChunkBufferMutex);

	newbuf = (unsigned char *)new char[bufsize + 256];
	if (newbuf == NULL) {
		throw ErrorException(sz_L71);
	}


	if (m_filechunkbuf != NULL)
		delete [] m_filechunkbuf;
	m_filechunkbuf = newbuf;
	m_filechunkbufsize = bufsize + 256;
	vnclog.Print(4, _T("m_filechunkbufsize expanded to %d\n"), m_filechunkbufsize);


}

// Processing NewFBSize pseudo-rectangle. Create new framebuffer of
// the size specified in pfburh->r.w and pfburh->r.h, and change the
// window size correspondingly.
//
void ClientConnection::ReadNewFBSize(rfbFramebufferUpdateRectHeader *pfburh)
{
	m_si.framebufferWidth = pfburh->r.w;
	m_si.framebufferHeight = pfburh->r.h;
	ClearCache();
	CreateLocalFramebuffer();
    SendFullFramebufferUpdateRequest();
	Createdib();\
	m_pendingScaleChange = true;
	m_pendingFormatChange = true;
	SendAppropriateFramebufferUpdateRequest();
	SizeWindow();
	InvalidateRect(m_hwnd, NULL, TRUE);
	RealiseFullScreenMode();
}

//
// sf@2002 - DSMPlugin 
//

//
// Ensures that the temporary "alignement" buffer in large enough 
//
inline void ClientConnection::CheckNetRectBufferSize(int nBufSize)
{
	if (m_nNetRectBufSize > nBufSize) return;

	omni_mutex_lock l(m_NetRectBufferMutex);

	BYTE *newbuf = new BYTE[nBufSize + 256];
	if (newbuf == NULL) 
	{
		// Error
	}
	if (m_pNetRectBuf != NULL)
		delete [] m_pNetRectBuf;

	m_pNetRectBuf = newbuf;
	m_nNetRectBufSize = nBufSize + 256;
}


//
// Ensures that the temporary "alignement" buffer in large enough 
//
inline void ClientConnection::CheckZRLENetRectBufferSize(int nBufSize)
{
	if (m_nZRLENetRectBufSize > nBufSize) return;

	omni_mutex_lock l(m_ZRLENetRectBufferMutex);

	BYTE *newbuf = new BYTE[nBufSize + 256];
	if (newbuf == NULL) 
	{
		// Error
	}
	if (m_pZRLENetRectBuf != NULL)
		delete [] m_pZRLENetRectBuf;

	m_pZRLENetRectBuf = newbuf;
	m_nZRLENetRectBufSize = nBufSize + 256;
}



//
// Format file size so it is user friendly to read
// 
void ClientConnection::GetFriendlySizeString(__int64 Size, char* szText)
{
	szText[0] = '\0';
	if( Size > (1024*1024*1024) )
	{
		__int64 lRest = (Size % (1024*1024*1024));
		Size /= (1024*1024*1024);
		wsprintf(szText,"%u.%4.4lu Gb", (unsigned long)Size, (unsigned long)((__int64)(lRest) * 10000 / 1024 / 1024 / 1024));
	}
	else if( Size > (1024*1024) )
	{
		unsigned long lRest = (Size % (1024*1024));
		Size /= (1024*1024);
		wsprintf(szText,"%u.%3.3lu Mb", (unsigned long)Size, (unsigned long)((__int64)(lRest) * 1000 / 1024 / 1024));
	}
	else if ( Size > 1024 )
	{
		unsigned long lRest = Size % (1024);
		Size /= 1024;
		wsprintf(szText,"%u.%2.2lu Kb", (unsigned long)Size, lRest * 100 / 1024);
	}
	else
	{
		wsprintf(szText,"%u bytes", (unsigned long)Size);
	}
}

//
// sf@2002
// 
void ClientConnection::UpdateStatusFields()
{
	char szText[256];

	// Bytes Received
	m_BytesRead = fis->GetBytesRead();
	GetFriendlySizeString(m_BytesRead, szText);
	// SetDlgItemInt(m_hwndStatus, IDC_RECEIVED, m_BytesRead, false);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_RECEIVED, szText);

	// Bytes Sent
	GetFriendlySizeString(m_BytesSend, szText);
	if (m_hwndStatus)SetDlgItemText(m_hwndStatus,IDC_SEND, szText);

	// Speed
	if (m_hwndStatus)SetDlgItemInt(m_hwndStatus, IDC_SPEED, kbitsPerSecond, false);

	// Encoder
	if (m_fStatusOpen) // It's called by the status window timer... fixme
	{
		if (EncodingStatusWindow!=OldEncodingStatusWindow)
		{
			OldEncodingStatusWindow = EncodingStatusWindow;
			switch (EncodingStatusWindow)
			{
			case rfbEncodingRaw:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "Raw, Cache" : "Raw");
				break;	
			case rfbEncodingRRE:			
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "RRE, Cache" : "RRE");
				break;
			case rfbEncodingCoRRE:
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "CoRRE, Cache" : "CoRRE");
				break;
			case rfbEncodingHextile:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "Hextile, Cache" : "Hextile");
				break;
			case rfbEncodingUltra:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "Ultra, Cache" : "Ultra");
				break;
			case rfbEncodingZlib:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "XORZlib, Cache" : "XORZlib");
				break;
  			case rfbEncodingZRLE:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "ZRLE, Cache" :"ZRLE");
  				break;
			case rfbEncodingTight:
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "Tight, Cache" : "Tight");
				break; 
			case rfbEncodingZlibHex:		
				if (m_hwndStatus)SetDlgItemText(m_hwndStatus, IDC_ENCODER, m_opts.m_fEnableCache ? "ZlibHex, Cache" : "ZlibHex");
				break;
			}
		}
	}
	else
		OldEncodingStatusWindow = -1;

}


////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////

void ClientConnection::GTGBS_ScrollToolbar(int dx, int dy)
{

/*	dx = max(dx, -m_hScrollPos);
	dx = min(dx, m_hScrollMax-(m_cliwidth)-m_hScrollPos);
	dy = max(dy, -m_vScrollPos);
	dy = min(dy, m_vScrollMax-(m_cliheight)-m_vScrollPos);
	if (dx || dy) {
		RECT clirect;
		GetClientRect(m_hwndTBwin, &clirect);
		ScrollWindowEx(m_hwndTBwin, dx, dy, NULL, &clirect, NULL, NULL, SW_ERASE );
		DoBlit();
	}
*/
}


void ClientConnection::GTGBS_CreateDisplay()
{
	// Das eigendliche HauptFenster erstellen,
	// welches das VNC-Fenster und die Toolbar enthält
	WNDCLASS wndclass;

	wndclass.style			= 0;
	wndclass.lpfnWndProc	= ClientConnection::WndProc;
	wndclass.cbClsExtra		= 0;
	wndclass.cbWndExtra		= 0;
	wndclass.hInstance		= m_pApp->m_instance;
	wndclass.hIcon			= LoadIcon(m_pApp->m_instance, MAKEINTRESOURCE(IDI_MAINICON));
	switch (m_opts.m_localCursor) {
	case NOCURSOR:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_NOCURSOR));
		break;
	case NORMALCURSOR:
		wndclass.hCursor		= LoadCursor(NULL, IDC_ARROW);
		break;
	case DOTCURSOR:
	default:
		wndclass.hCursor		= LoadCursor(m_pApp->m_instance, MAKEINTRESOURCE(IDC_DOTCURSOR));
	}
	wndclass.hbrBackground	= (HBRUSH) GetStockObject(BLACK_BRUSH);
    wndclass.lpszMenuName	= (const TCHAR *) NULL;
	wndclass.lpszClassName	= _T("VNCMDI_Window");

	RegisterClass(&wndclass);


#ifdef _WIN32_WCE
	DWORD winstyle = WS_VSCROLL | WS_HSCROLL | WS_CAPTION | WS_SYSMENU;
#else
	DWORD winstyle = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | 
	  WS_MINIMIZEBOX |WS_MAXIMIZEBOX | WS_THICKFRAME | WS_VSCROLL | WS_HSCROLL;
#endif

	int		x = CW_USEDEFAULT;
	int		y = CW_USEDEFAULT;
	int		w = 320;
	int		h = 200;
	HWND	parent = NULL;

#ifdef _ULTRAVNCAX_

	winstyle = WS_CHILD | WS_VSCROLL | WS_HSCROLL;

	x = 0;
	y = 0;

	RECT		rectClient;
	::GetClientRect( m_hwndAx, & rectClient );
	w = rectClient.right;
	h = rectClient.bottom;

	parent = m_hwndAx;

#endif

	m_hwndMain = CreateWindow(_T("VNCMDI_Window"),
			  _T("VNCviewer"),
			  winstyle,
			  x,
			  y,
			  //CW_USEDEFAULT,
			  //CW_USEDEFAULT,
			  w,h,
			  parent,              // Parent handle
			  NULL,                // Menu handle
			  m_pApp->m_instance,
			  NULL);
	//ShowWindow(m_hwndMain,SW_SHOW);

	SetWindowLong(m_hwndMain, GWL_USERDATA, (LONG) this);

#ifdef _ULTRAVNCAX_
	*m_pHwndAppFrame = m_hwndMain;
#endif
}

//
//
//
LRESULT CALLBACK ClientConnection::GTGBS_ShowStatusWindow(LPVOID lpParameter)
{
	ClientConnection *_this = (ClientConnection*)lpParameter;

	 _this->m_fStatusOpen = true;
	DialogBoxParam(_this->m_pApp->m_instance,MAKEINTRESOURCE(IDD_STATUS),NULL,(DLGPROC)ClientConnection::GTGBS_StatusProc,(LPARAM)_this);
	// _this->m_fStatusOpen = false;
	return 0;
}


//
//
//
LRESULT CALLBACK ClientConnection::GTGBS_StatusProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
	ClientConnection* _this = (ClientConnection *) GetWindowLong(hwnd, GWL_USERDATA);
	
	switch (iMsg)
	{
	case WM_INITDIALOG:
		{
			// sf@2002 - Make the window always on top
			RECT Rect;
			GetWindowRect(hwnd, &Rect);
			SetWindowPos(hwnd, 
				HWND_TOPMOST,
				Rect.left,
				Rect.top,
				Rect.right - Rect.left,
				Rect.bottom - Rect.top,
				SWP_SHOWWINDOW);
			
			char wt[MAX_PATH];
			ClientConnection *_this = (ClientConnection *)lParam;
			SetWindowLong(hwnd, GWL_USERDATA, (LONG) _this);
			SetDlgItemInt(hwnd,IDC_RECEIVED,_this->m_BytesRead,false);
			SetDlgItemInt(hwnd,IDC_SEND,_this->m_BytesSend,false);
			
			if (_this->m_host != NULL) {
				SetDlgItemText(hwnd,IDC_VNCSERVER,_this->m_host);
				sprintf(wt,"%s %s",sz_L72,_this->m_host);
				SetWindowText(hwnd,wt);
			} else {
				SetDlgItemText(hwnd,IDC_VNCSERVER,_T(""));
				SetWindowText(hwnd,sz_L73);
			}
			
			if(_this->m_port != NULL)
				SetDlgItemInt(hwnd,IDC_PORT,_this->m_port,FALSE);
			else
				SetDlgItemText(hwnd,IDC_PORT,_T(""));
			
			if(_this->m_sock != NULL )
			{
				if (_this->m_pDSMPlugin->IsEnabled())
				{
					char szMess[255];
					memset(szMess, 0, 255);
					sprintf(szMess, "%s (%s-v%s)",
							sz_L49,
							_this->m_pDSMPlugin->GetPluginName(),
							_this->m_pDSMPlugin->GetPluginVersion()
							);
					SetDlgItemText(hwnd,IDC_STATUS, szMess);
				}
				else
					SetDlgItemText(hwnd,IDC_STATUS,sz_L49);
				
			}
			else
			{
				SetDlgItemText(hwnd,IDC_STATUS,sz_L74);
			}
			
			//CentreWindow(hwnd);
			ShowWindow(hwnd,SW_SHOW);
			_this->m_hwndStatus = hwnd;
			if (_this->m_running) {
				//Normaler status
				ShowWindow(GetDlgItem(hwnd,IDQUIT),SW_HIDE);
				ShowWindow(GetDlgItem(hwnd,IDCLOSE),SW_SHOW);
				// sf@2002
				if (!_this->m_nStatusTimer)
					_this->m_nStatusTimer = SetTimer( hwnd, 3333, 1000, NULL);
				
			} else {
				//Verbindungsaufbau status
				ShowWindow(GetDlgItem(hwnd,IDQUIT),SW_SHOW);
				ShowWindow(GetDlgItem(hwnd,IDCLOSE),SW_HIDE);
				SetDlgItemText(hwnd,IDC_STATUS,sz_L43);
				HMENU hMenu = GetSystemMenu(hwnd,0);
				EnableMenuItem(hMenu,SC_CLOSE,MF_BYCOMMAND | MF_GRAYED);
			}
			
			return TRUE;
		}
	case WM_CLOSE:
		{
			EndDialog(hwnd, TRUE);
			return TRUE;
		}
	case WM_COMMAND:
		{
			if (LOWORD(wParam) == IDCLOSE) {
				EndDialog(hwnd, TRUE);
			}
			if (LOWORD(wParam) == IDQUIT) {
				_this->Pressed_Cancel=true;
				EndDialog(hwnd, TRUE);
			}

			/*if (LOWORD(wParam) == IDQUIT) {
				PostQuitMessage(0);
				ClientConnection *_this = (ClientConnection *) GetWindowLong(hwnd, GWL_USERDATA);
				_this->KillThread();
				
				//EndDialog(hwnd, TRUE);
			}*/
			return TRUE;
		} 
		
		// sf@2002 - Every timer tic, we update the status values (speed, Sent, received, Encoder)
	case WM_TIMER:
		{
			_this->UpdateStatusFields();
			return TRUE;
		}
		
	case WM_DESTROY:
		{
			// sf@2002 - Destroy the status timer... TODO: improve this
			if (_this->m_nStatusTimer != 0) 
			{
				KillTimer(hwnd, _this->m_nStatusTimer);			
				_this->m_nStatusTimer = 0;
			}
			_this->OldEncodingStatusWindow = -1;
			_this->m_fStatusOpen = false;
			return TRUE;
		}
	}
	return FALSE;
}

//
//
//
LRESULT CALLBACK ClientConnection::GTGBS_SendCustomKey_proc(HWND Dlg, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
	
	switch (iMsg)
	{
	case WM_INITDIALOG:
		{
			SetFocus(GetDlgItem(Dlg,IDC_CUSTOM_KEY));
			ShowWindow(Dlg, SW_SHOW);
			// Window always on top
			RECT Rect;
			GetWindowRect(Dlg, &Rect);
			SetWindowPos(Dlg, 
				HWND_TOPMOST,
				Rect.left,
				Rect.top,
				Rect.right - Rect.left,
				Rect.bottom - Rect.top,
				SWP_SHOWWINDOW);
			return TRUE;
		}
	case WM_CLOSE:
		{
			EndDialog(Dlg, 0);
			return TRUE;
		}
	case WM_COMMAND:
		{
			BOOL Okay;
			UINT Key;
			UINT STRG=0;
			UINT ALT=0;
			UINT ALTGR=0;
			if (LOWORD(wParam) == IDCANCEL) {
				EndDialog(Dlg, 0);
			}
			if (LOWORD(wParam) == IDOK) {
				if (SendMessage(GetDlgItem(Dlg,IDC_STRG), BM_GETCHECK, 0, 0) == BST_CHECKED)
					STRG=1;
				
				if (SendMessage(GetDlgItem(Dlg,IDC_ALT), BM_GETCHECK, 0, 0) == BST_CHECKED)
					ALT=1;
				
				if (SendMessage(GetDlgItem(Dlg,IDC_ALTGR), BM_GETCHECK, 0, 0) == BST_CHECKED)
					ALTGR=1;
				
				
				Key = GetDlgItemInt(Dlg,IDC_CUSTOM_KEY,&Okay,FALSE);
				
				if (ALT!=0)
					Key |=KEYMAP_LALT;
				if (ALTGR != 0)
					Key |= KEYMAP_RALT;
				if (STRG != 0)
					Key |= KEYMAP_RCONTROL;
				
				if (Okay)
					EndDialog(Dlg, Key);
				else
					EndDialog(Dlg, 0);
			}
		}
	}
	return 0;
}

//
// Process windows messages
//
LRESULT CALLBACK ClientConnection::WndProc(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
	
	// This is a static method, so we don't know which instantiation we're 
	// dealing with.  But we've stored a 'pseudo-this' in the window data.
	ClientConnection *_this = (ClientConnection *) GetWindowLong(hwnd, GWL_USERDATA);
	
	if (_this == NULL)
		return DefWindowProc(hwnd, iMsg, wParam, lParam);

	// HWND parent;

	{
		// Main Window 
		// if ( hwnd == _this->m_hwndMain)
		{
			switch (iMsg)
			{
//			case WM_TIMER:
//				KillTimer(hwnd,_this->m_FTtimer);
//				_this->m_FTtimer=0;
//				_this->m_pFileTransfer->SendFileChunk();
//				break;
			case WM_SYSCOMMAND:
				{
					switch (LOWORD(wParam))
					{
					case ID_SW:
						if (!_this->m_SWselect)
						{
							_this->m_SWselect=true;
						}
						break;
						
					case ID_DESKTOP:
						if (!_this->m_SWselect)
						{
							_this->m_SWselect=true;
							_this->SendSW(9999,9999);
						}
						break;
						
					// Toggle toolbar & toolbar menu option
					case ID_DBUTTON:
						_this->m_opts.m_ShowToolbar = !_this->m_opts.m_ShowToolbar;
						CheckMenuItem(GetSystemMenu(_this->m_hwndMain, FALSE),
							          ID_DBUTTON,
									  MF_BYCOMMAND | (_this->m_opts.m_ShowToolbar ? MF_CHECKED :MF_UNCHECKED));
						_this->SizeWindow();
						_this->SetFullScreenMode(_this->InFullScreenMode());
						break;
						
					/*
					case ID_BUTTON:
						_this->m_opts.m_ShowToolbar=true;
						_this->SizeWindow();
						_this->SetFullScreenMode(_this->InFullScreenMode());
						break;
					*/	

					case ID_AUTOSCALING:
						_this->m_opts.m_fAutoScaling = !_this->m_opts.m_fAutoScaling;
						CheckMenuItem(GetSystemMenu(_this->m_hwndMain, FALSE),
							          ID_AUTOSCALING,
									  MF_BYCOMMAND | (_this->m_opts.m_fAutoScaling ? MF_CHECKED :MF_UNCHECKED));
						_this->SizeWindow();
						InvalidateRect(hwnd, NULL, TRUE);
						_this->RealiseFullScreenMode();
						break;

					case ID_DINPUT:
						_this->m_remote_mouse_disable = true;
						if (_this->m_opts.m_ShowToolbar)
						{
							RECT rect;
							GetWindowRect(hwnd, &rect);
							_this->m_winwidth = rect.right - rect.left;
							_this->m_winheight = rect.bottom - rect.top ;
							if ((_this->m_winwidth) > 140+85+14*24)
							{
								DestroyWindow(_this->m_hwndTB);
								_this->m_BigToolbar=true;
								_this->CreateButtons(false,_this->m_fServerKnowsFileTransfer);
							}
							else 
							{
								_this->m_BigToolbar=false;
								DestroyWindow(_this->m_hwndTB);
								_this->CreateButtons(true,_this->m_fServerKnowsFileTransfer);
							}
							SendMessage(hwnd,WM_SIZE,(WPARAM)ID_DINPUT,(LPARAM)0);
						}
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendServerInput(true);
						break;
						
					case ID_INPUT:
						_this->m_remote_mouse_disable = false;
						if (_this->m_opts.m_ShowToolbar)
						{
							RECT rect;
							GetWindowRect(hwnd, &rect);
							_this->m_winwidth = rect.right - rect.left;
							_this->m_winheight = rect.bottom - rect.top ;
							if ((_this->m_winwidth) > 140+85+14*24)
							{
								DestroyWindow(_this->m_hwndTB);
								_this->m_BigToolbar=true;
								_this->CreateButtons(false,_this->m_fServerKnowsFileTransfer);
							}
							else 
							{
								_this->m_BigToolbar=false;
								DestroyWindow(_this->m_hwndTB);
								_this->CreateButtons(true,_this->m_fServerKnowsFileTransfer);
							}
							SendMessage(hwnd,WM_SIZE,(WPARAM)ID_DINPUT,(LPARAM)0);
						}
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendServerInput(false);
						break;
						
					case SC_MINIMIZE:
						_this->SetDormant(true);
						if (_this->m_hwndStatus)ShowWindow(_this->m_hwndStatus,SW_MINIMIZE);
						break;

					case SC_MAXIMIZE: //Added by: Lars Werner (http://lars.werner.no)
						_this->SetFullScreenMode(!_this->InFullScreenMode());
						break;
						
					case SC_RESTORE:
						_this->SetDormant(false);
						if (_this->m_hwndStatus)ShowWindow(_this->m_hwndStatus,SW_NORMAL);
						break;
						
					case ID_NEWCONN:
						_this->m_pApp->NewConnection();
						return 0;
						
					case ID_CONN_SAVE_AS:
						_this->SaveConnection();
						return 0;
						
					case IDC_OPTIONBUTTON: 
						{
							if (_this->m_fOptionsOpen) return 0;
							_this->m_fOptionsOpen = true;
							
							// Modif sf@2002 - Server Scaling
							int nOldServerScale = _this->m_nServerScale;
							int prev_scale_num = _this->m_opts.m_scale_num;
							int prev_scale_den = _this->m_opts.m_scale_den;
							bool fOldToolbarState = _this->m_opts.m_ShowToolbar;
							int nOldAutoMode = _this->m_opts.autoDetect;
							
							if (_this->m_opts.DoDialog(true))
							{
								/*
								// Modif sf@2002 - Cache
								if (_this->m_opts.m_fEnableCache && _this->m_hCacheBitmap == NULL)
								{
									_this->m_hCacheBitmapDC = CreateCompatibleDC(_this->m_hBitmapDC);
									_this->m_hCacheBitmap = CreateCompatibleBitmap(_this->m_hBitmapDC, _this->m_si.framebufferWidth, _this->m_si.framebufferHeight);
									_this->m_pendingCacheInit = true;
								}
								*/
								
								// Modif sf@2002 - Server Scaling
								_this->m_nServerScale = _this->m_opts.m_nServerScale;
								if (_this->m_nServerScale != nOldServerScale)
								{
									_this->SendServerScale(_this->m_nServerScale);
								}
								else
								{
									if (prev_scale_num != _this->m_opts.m_scale_num ||
										prev_scale_den != _this->m_opts.m_scale_den)
									{
										// Resize the window if scaling factors were changed
										_this->SizeWindow(/*false*/);
										InvalidateRect(hwnd, NULL, TRUE);
										// Make the window corresponds to the requested state
										_this->RealiseFullScreenMode();
									}
									if (fOldToolbarState != _this->m_opts.m_ShowToolbar)
										_this->SizeWindow();
									_this->m_pendingFormatChange = true;
								}
							}		
							 if (nOldAutoMode != _this->m_opts.autoDetect)
								 _this->m_nConfig = 0;
							_this->OldEncodingStatusWindow = -2; // force update in status window
							_this->m_fOptionsOpen = false;
							return 0;
						}
						
					case IDD_APP_ABOUT:
#ifndef _ULTRAVNCAX_
						ShowAboutBox();
#else
						ShowAboutBox( GetTopMostWnd( _this->m_hwndMain ? _this->m_hwndMain : _this->m_hwndAx ) );
#endif
						return 0;
						
					case ID_CONN_ABOUT:
						_this->ShowConnInfo();
						return 0;
						
					case ID_FULLSCREEN: 
						// Toggle full screen mode
						_this->SetFullScreenMode(!_this->InFullScreenMode());
						return 0;

					case ID_VIEWONLYTOGGLE: 
						// Toggle view only mode
						_this->m_opts.m_ViewOnly = !_this->m_opts.m_ViewOnly;
						// Todo update menu state
						return 0;
						
					case ID_REQUEST_REFRESH: 
						// Request a full-screen update
						_this->SendFullFramebufferUpdateRequest();
						return 0;
	
					case ID_VK_LWINDOWN:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Super_L, true);
						return 0;
					case ID_VK_LWINUP:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Super_L, false);
						return 0;
					case ID_VK_RWINDOWN:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Super_R, true);
						return 0;
					case ID_VK_RWINUP:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Super_R, false);
						return 0;
					case ID_VK_APPSDOWN:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Menu, true);
						return 0;
					case ID_VK_APPSUP:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Menu, false);
						return 0;

						
					// Send START Button
					case ID_CONN_CTLESC:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Control_L,true);
						_this->SendKeyEvent(XK_Escape,true);
						_this->SendKeyEvent(XK_Control_L,false);
						_this->SendKeyEvent(XK_Escape,false);
						return 0;
						
					// Send Ctrl-Alt-Del
					case ID_CONN_CTLALTDEL:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Control_L, true);
						_this->SendKeyEvent(XK_Alt_L,     true);
						_this->SendKeyEvent(XK_Delete,    true);
						_this->SendKeyEvent(XK_Delete,    false);
						_this->SendKeyEvent(XK_Alt_L,     false);
						_this->SendKeyEvent(XK_Control_L, false);
						return 0;
						
					case ID_CONN_CTLDOWN:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Control_L, true);
						return 0;
						
					case ID_CONN_CTLUP:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Control_L, false);
						return 0;
						
					case ID_CONN_ALTDOWN:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Alt_L, true);
						return 0;
						
					case ID_CONN_ALTUP:
						if (_this->m_opts.m_ViewOnly) return 0;
						_this->SendKeyEvent(XK_Alt_L, false);
						return 0;
						
					case ID_CLOSEDAEMON:
						if (MessageBox(NULL, sz_L75, 
							sz_L76, 
							MB_YESNO | MB_ICONQUESTION | MB_DEFBUTTON2) == IDYES) 
						{
							vnclog.Print(1, _T("PostQuitMessage in handling ID_CLOSEDAEMON\n"));
							PostQuitMessage(0);
						}
						return 0;
						
						// Modif sf@2002 - FileTransfer
					case ID_FILETRANSFER: 
						// Check if the Server knows FileTransfer
						if (!_this->m_fServerKnowsFileTransfer)
						{
							MessageBox(NULL, sz_L77, 
								sz_L78, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
						// Don't call FileTRansfer GUI is already open !
						if (_this->m_pFileTransfer->m_fFileTransferRunning)
						{
							_this->m_pFileTransfer->ShowFileTransferWindow(true);
							return 0;
							/*
							MessageBox(NULL, sz_L79, 
								sz_L80, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
							*/
						}
						if (_this->m_pTextChat->m_fTextChatRunning)
						{
							_this->m_pTextChat->ShowChatWindow(true);
							MessageBox(	NULL,
										sz_L86, 
										sz_L88, 
										MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}

						// Call FileTransfer Dialog
						_this->m_pFileTransfer->m_fFileTransferRunning = true;
						_this->m_pFileTransfer->m_fFileCommandPending = false;
#ifndef _ULTRAVNCAX_
						_this->m_pFileTransfer->DoDialog();
#else
						_this->m_pFileTransfer->DoDialog ( GetTopMostWnd( _this->m_hwndMain ? _this->m_hwndMain : _this->m_hwndAx ) );
#endif
						_this->m_pFileTransfer->m_fFileTransferRunning = false;
						// Refresh Screen
						// _this->SendFullFramebufferUpdateRequest();
						if (_this->m_pFileTransfer->m_fVisible || _this->m_pFileTransfer->m_fOldFTProtocole)
							_this->SendAppropriateFramebufferUpdateRequest();
						return 0;
						
						// sf@2002 - Text Chat
					case ID_TEXTCHAT:
						// We use same flag as FT for now
						// Check if the Server knows FileTransfer
						if (!_this->m_fServerKnowsFileTransfer)
						{
							MessageBox(NULL, sz_L81, 
								sz_L82, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
						if (_this->m_pTextChat->m_fTextChatRunning)
						{
							_this->m_pTextChat->ShowChatWindow(true);
							return 0;
							/*
							MessageBox(NULL, sz_L83, 
								sz_L84, 
								MB_OK | MB_ICONINFORMATION | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
							*/
						}
						if (_this->m_pFileTransfer->m_fFileTransferRunning)
						{
							_this->m_pFileTransfer->ShowFileTransferWindow(true);
							MessageBox(NULL,
										sz_L85, 
										sz_L88, 
										MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
						_this->m_pTextChat->m_fTextChatRunning = true;
						_this->m_pTextChat->DoDialog();
						return 0;
						
						// sf@2002
					case ID_MAXCOLORS: 
						if (_this->m_opts.m_Use8Bit)
						{
							_this->m_opts.m_Use8Bit = rfbPFFullColors; //false;
							_this->m_pendingFormatChange = true;
							InvalidateRect(hwnd, NULL, TRUE);
						}
						return 0;
						
						// sf@2002
					case ID_256COLORS: 
						// if (!_this->m_opts.m_Use8Bit)
						{
							_this->m_opts.m_Use8Bit = rfbPF256Colors; //true;
							_this->m_pendingFormatChange = true;
							InvalidateRect(hwnd, NULL, TRUE);
						}
						return 0;
						
						// Modif sf@2002
					case ID_HALFSCREEN: 
						{
							// Toggle halfSize screen mode (server side)
							int nOldServerScale = _this->m_nServerScale;
							
							// Modif sf@2002 - Server Scaling
							_this->m_opts.m_fAutoScaling = false;
							_this->m_nServerScale = 2;
							_this->m_opts.m_nServerScale = 2;
							_this->m_opts.m_scaling = true;
							_this->m_opts.m_scale_num = 100;
							_this->m_opts.m_scale_den = 100;
							
							if (_this->m_nServerScale != nOldServerScale)
							{
								_this->SendServerScale(_this->m_nServerScale);
								// _this->m_pendingFormatChange = true;
							}
							else
							{
								_this->SizeWindow();
								InvalidateRect(hwnd, NULL, TRUE);
								_this->RealiseFullScreenMode();	
								_this->m_pendingFormatChange = true;
							}
							return 0;
						}
						
						// Modif sf@2002
					case ID_FUZZYSCREEN: 
						{
							// Toggle fuzzy screen mode (server side)
							int nOldServerScale = _this->m_nServerScale;
							
							// We don't forbid AutoScaling if selected
							// so the viewer zoom factor is more accurate
							_this->m_nServerScale = 2;
							_this->m_opts.m_nServerScale = 2;
							_this->m_opts.m_scaling = true;
							_this->m_opts.m_scale_num = 200;
							_this->m_opts.m_scale_den = 100;
							
							if (_this->m_nServerScale != nOldServerScale)
							{
								_this->SendServerScale(_this->m_nServerScale);
								// _this->m_pendingFormatChange = true;
							}
							else
							{
								_this->SizeWindow();
								InvalidateRect(hwnd, NULL, TRUE);
								_this->RealiseFullScreenMode();	
								_this->m_pendingFormatChange = true;
							}
							
							return 0;
						}
						
						// Modif sf@2002
					case ID_NORMALSCREEN: 
						{
							// Toggle normal screen
							int nOldServerScale = _this->m_nServerScale;
							
							_this->m_opts.m_fAutoScaling = false;
							_this->m_nServerScale = 1;
							_this->m_opts.m_nServerScale = 1;
							_this->m_opts.m_scaling = false;
							_this->m_opts.m_scale_num = 100;
							_this->m_opts.m_scale_den = 100;
							
							if (_this->m_nServerScale != nOldServerScale)
							{
								_this->SendServerScale(_this->m_nServerScale);
								// _this->m_pendingFormatChange = true;
							}
							else
							{
								_this->SizeWindow();
								InvalidateRect(hwnd, NULL, TRUE);
								_this->SetFullScreenMode(false);	
								_this->m_pendingFormatChange = true;
							}
							return 0;
						}

					} // end switch lowparam syscommand
					
					break;

				}//end case wm_syscommand
				
#ifndef UNDER_CE
				case WM_SIZING:
					{
						// Don't allow sizing larger than framebuffer
						RECT *lprc = (LPRECT) lParam;
						switch (wParam) {
						case WMSZ_RIGHT: 
						case WMSZ_TOPRIGHT:
						case WMSZ_BOTTOMRIGHT:
							lprc->right = min(lprc->right, lprc->left + _this->m_fullwinwidth+1 );
							break;
						case WMSZ_LEFT:
						case WMSZ_TOPLEFT:
						case WMSZ_BOTTOMLEFT:
							lprc->left = max(lprc->left, lprc->right - _this->m_fullwinwidth);
							break;
						}
						
						switch (wParam) {
						case WMSZ_TOP:
						case WMSZ_TOPLEFT:
						case WMSZ_TOPRIGHT:
							if (_this->m_opts.m_ShowToolbar)
								lprc->top = max(lprc->top, lprc->bottom - _this->m_fullwinheight -_this->m_TBr.bottom);
							else
								lprc->top = max(lprc->top, lprc->bottom - _this->m_fullwinheight);
							break;
						case WMSZ_BOTTOM:
						case WMSZ_BOTTOMLEFT:
						case WMSZ_BOTTOMRIGHT:
							if (_this->m_opts.m_ShowToolbar)
								lprc->bottom = min(lprc->bottom, lprc->top + _this->m_fullwinheight + _this->m_TBr.bottom);
							else
								lprc->bottom = min(lprc->bottom, lprc->top + _this->m_fullwinheight);
							break;
						}
						
						return 0;
					}
#endif
				case WM_QUERYOPEN:
					_this->SetDormant(false);
					return true;

				case WM_SETFOCUS:		
					TheAccelKeys.SetWindowHandle(_this->m_opts.m_NoHotKeys ? 0 : hwnd);
					return 0;			

				case WM_KILLFOCUS:
					if (!_this->m_running) return 0;
					if ( _this->m_opts.m_ViewOnly) return 0;
					_this->SendKeyEvent(XK_Alt_L,     false);
					_this->SendKeyEvent(XK_Control_L, false);
					_this->SendKeyEvent(XK_Shift_L,   false);
					_this->SendKeyEvent(XK_Alt_R,     false);
					_this->SendKeyEvent(XK_Control_R, false);
					_this->SendKeyEvent(XK_Shift_R,   false);
					return 0; 
			
				case WM_CLOSE:
					{
#ifndef _ULTRAVNCAX_
						// sf@2002 - Do not close vncviewer if the File Transfer GUI is open !
						if (_this->m_pFileTransfer->m_fFileTransferRunning)
						{
							_this->m_pFileTransfer->ShowFileTransferWindow(true);
							MessageBox(NULL, sz_L85, 
								sz_L88, 
								MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
												
						// sf@2002 - Do not close vncviewer if the Text Chat GUI is open !
						if (_this->m_pTextChat->m_fTextChatRunning)
						{
							_this->m_pTextChat->ShowChatWindow(true);
							MessageBox(NULL, sz_L86, 
								sz_L88, 
								MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
						
						if (_this->m_fOptionsOpen)
						{
							MessageBox(NULL, sz_L87, 
								sz_L88, 
								MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
							return 0;
						}
#endif

						// Close the worker thread as well
						_this->KillThread();

						DestroyWindow(_this->m_hwndTB);
						_this->m_hwndTB = NULL;

						DestroyWindow(_this->m_TrafficMonitor);
						_this->m_TrafficMonitor = NULL;

						DestroyWindow(_this->m_logo_wnd);
						_this->m_logo_wnd = NULL;

						DestroyWindow(_this->m_button_wnd);
						_this->m_button_wnd = NULL;

//						DestroyWindow(_this->m_hwndTBwin);
						DestroyWindow(_this->m_hwnd);
						_this->m_hwnd = NULL;

						DestroyWindow(hwnd);

						vnclog.Print(1, _T("ClientConnection Handle WM_CLOSE done\n"));
						return 0;
					}
					
				case WM_DESTROY:
					{
#ifndef UNDER_CE
						// Remove us from the clipboard viewer chain
						BOOL res = ChangeClipboardChain( _this->m_hwnd, _this->m_hwndNextViewer);
#endif
						if (_this->m_waitingOnEmulateTimer)
						{
							KillTimer(_this->m_hwnd, _this->m_emulate3ButtonsTimer);
							_this->m_waitingOnEmulateTimer = false;
						}
//						if (_this->m_FTtimer != 0) 
//						{
//							KillTimer(hwnd, _this->m_FTtimer);			
//							_this->m_FTtimer = 0;
//						}
						
						//_this->m_hwnd = 0;
						// We are currently in the main thread.
						// The worker thread should be about to finish if
						// it hasn't already. Wait for it.


						if(_this->m_hwndTB)
							DestroyWindow(_this->m_hwndTB);
						_this->m_hwndTB = NULL;

						if(_this->m_TrafficMonitor)
							DestroyWindow(_this->m_TrafficMonitor);
						_this->m_TrafficMonitor = NULL;

						if(_this->m_logo_wnd)
							DestroyWindow(_this->m_logo_wnd);
						_this->m_logo_wnd = NULL;

						if(_this->m_button_wnd)
							DestroyWindow(_this->m_button_wnd);
						_this->m_button_wnd = NULL;

						if(_this->m_hwnd)
							DestroyWindow(_this->m_hwnd);
						_this->m_hwnd = NULL;

						::SetWindowLong(hwnd, GWL_USERDATA, 0);
						_this->KillThread();
						try {
							void *p;
							_this->join(&p);  // After joining, _this is no longer valid
						} catch (omni_thread_invalid& e) {
							// The thread probably hasn't been started yet,
						}
						
						//PostQuitMessage(0);

						vnclog.Print(1, _T("ClientConnection Handle WM_DESTROY done\n"));
						return 0;
					}
					
				case WM_KEYDOWN:
				case WM_KEYUP:
				case WM_SYSKEYDOWN:
				case WM_SYSKEYUP:
					{
						if (!_this->m_running) return 0;
						if ( _this->m_opts.m_ViewOnly) return 0;
						_this->ProcessKeyEvent((int) wParam, (DWORD) lParam);
						return 0;
					}
/*					
				case WM_CHAR:
				case WM_SYSCHAR:
#ifdef UNDER_CE
					{
						int key = wParam;
						vnclog.Print(4,_T("CHAR msg : %02x\n"), key);
						// Control keys which are in the Keymap table will already
						// have been handled.
						if (key == 0x0D  ||  // return
							key == 0x20 ||   // space
							key == 0x08)     // backspace
							return 0;
						
						if (key < 32) key += 64;  // map ctrl-keys onto alphabet
						if (key > 32 && key < 127) {
							_this->SendKeyEvent(wParam & 0xff, true);
							_this->SendKeyEvent(wParam & 0xff, false);
						}
						return 0;
					}
#endif
*/
				case WM_DEADCHAR:
				case WM_SYSDEADCHAR:
					return 0;
					
				case WM_WINDOWPOSCHANGED:
				case WM_SIZE:
					{
						// Calculate window dimensions
						RECT rect;
						RECT Rtb;
						GetWindowRect(hwnd, &rect);
						_this->m_winwidth = rect.right - rect.left;
						_this->m_winheight = rect.bottom - rect.top ;

						if (_this->m_opts.m_ShowToolbar)
						{
							GetWindowRect(_this->m_hwndTBwin, &Rtb);
							//MoveWindow(_this->m_hwndTB,
							//	0,0,_this->m_winwidth - 106, 32,TRUE);
							//SetWindowPos(_this->m_hwndTBwin, HWND_TOP, 0, 0, _this->m_winwidth, 32,SWP_FRAMECHANGED);
							if ((_this->m_winwidth) > 140+85+14*24)
							{
								if (_this->m_BigToolbar==false)
								{
								DestroyWindow(_this->m_hwndTB);
								_this->m_BigToolbar=true;
								_this->CreateButtons(false,_this->m_fServerKnowsFileTransfer);
								}
							}
							else 
							{
								if (_this->m_BigToolbar==true)
								{
								_this->m_BigToolbar=false;
								DestroyWindow(_this->m_hwndTB);
								_this->CreateButtons(true,_this->m_fServerKnowsFileTransfer);
								}
							}
							SetWindowPos(_this->m_hwndTB,HWND_TOP,0,0,_this->m_winwidth - 200, 32,SWP_FRAMECHANGED);
							if (_this->m_TrafficMonitor)
							{
								MoveWindow(_this->m_TrafficMonitor,
									_this->m_winwidth - 55,2,35,30,TRUE);
								MoveWindow(_this->m_logo_wnd,
									_this->m_winwidth - 185,2,130,28,TRUE);
								MoveWindow(_this->m_button_wnd,
									_this->m_winwidth - 200,10,10,10,TRUE);
							}
							if (_this->m_logo_wnd)
							{
								MoveWindow(_this->m_logo_wnd,
									_this->m_winwidth - 185,2,130,28,TRUE);
								MoveWindow(_this->m_button_wnd,
									_this->m_winwidth - 200,10,10,10,TRUE);
							}
							UpdateWindow(_this->m_hwndTB);
							UpdateWindow(_this->m_logo_wnd);
							UpdateWindow(_this->m_button_wnd);
						}
						else
						{
							Rtb.top=0;Rtb.bottom=0;	
						}
						
						
						// If the current window size would be large enough to hold the
						// whole screen without scrollbars, or if we're full-screen,
						// we turn them off.  Under CE, the scroll bars are unchangeable.
		
#ifndef UNDER_CE
						if (_this->InFullScreenMode() ||
							_this->m_winwidth  >= _this->m_fullwinwidth  &&
							_this->m_winheight >= (_this->m_fullwinheight + ((Rtb.bottom - Rtb.top) )) ) {
							//_this->m_winheight >= _this->m_fullwinheight  ) {
							ShowScrollBar(hwnd, SB_HORZ, FALSE);
							ShowScrollBar(hwnd, SB_VERT, FALSE);
						} else {
							ShowScrollBar(hwnd, SB_HORZ, TRUE);
							ShowScrollBar(hwnd, SB_VERT, TRUE);
						}
#endif
					
						// Update these for the record
						// And consider that in full-screen mode the window
						// is actually bigger than the remote screen.
						GetClientRect(hwnd, &rect);
						
						_this->m_cliwidth = min( (int)(rect.right - rect.left), 
							                     (int)(_this->m_si.framebufferWidth * _this->m_opts.m_scale_num / _this->m_opts.m_scale_den));
						if (_this->m_opts.m_ShowToolbar)
							_this->m_cliheight = min( (int)rect.bottom - rect.top ,
							                          (int)_this->m_si.framebufferHeight * _this->m_opts.m_scale_num / _this->m_opts.m_scale_den + _this->m_TBr.bottom);
						else
							_this->m_cliheight = min( (int)(rect.bottom - rect.top) ,
							                          (int)(_this->m_si.framebufferHeight * _this->m_opts.m_scale_num / _this->m_opts.m_scale_den));
						
						_this->m_hScrollMax = (int)_this->m_si.framebufferWidth * _this->m_opts.m_scale_num / _this->m_opts.m_scale_den;
						if (_this->m_opts.m_ShowToolbar)
							_this->m_vScrollMax = (int)(_this->m_si.framebufferHeight *
							                            _this->m_opts.m_scale_num / _this->m_opts.m_scale_den)
														+ _this->m_TBr.bottom;
						else 
							_this->m_vScrollMax = (int)(_this->m_si.framebufferHeight*
							                           _this->m_opts.m_scale_num / _this->m_opts.m_scale_den);
						
						int newhpos, newvpos;
						newhpos = max(0, 
							          min(_this->m_hScrollPos, 
					                      _this->m_hScrollMax - max(_this->m_cliwidth, 0)
										 )
									 );
						newvpos = max(0,
							          min(_this->m_vScrollPos, 
							              _this->m_vScrollMax - max(_this->m_cliheight, 0)
										 )
								     );
						
						ScrollWindowEx(_this->m_hwnd,
							           _this->m_hScrollPos - newhpos,
									   _this->m_vScrollPos - newvpos,
							           NULL, &rect, NULL, NULL,  SW_INVALIDATE);
						
						_this->m_hScrollPos = newhpos;
						_this->m_vScrollPos = newvpos;
						_this->UpdateScrollbars();
						
						
					//Added by: Lars Werner (http://lars.werner.no)
					if(wParam==SIZE_MAXIMIZED&&_this->InFullScreenMode()==FALSE)
					{
						_this->SetFullScreenMode(!_this->InFullScreenMode());
						//MessageBox(NULL,"Fullscreeen from maximizehora...","KAKE",MB_OK);
						//return 0;
					}
				
					//Modified by: Lars Werner (http://lars.werner.no)
					if(_this->InFullScreenMode()==TRUE)
						return 0;
					else
						break;
					}

				case WM_HSCROLL:
					{
						int dx = 0;
						int pos = HIWORD(wParam);
						switch (LOWORD(wParam)) {
						case SB_LINEUP:
							dx = -2; break;
						case SB_LINEDOWN:
							dx = 2; break;
						case SB_PAGEUP:
							dx = _this->m_cliwidth * -1/4; break;
						case SB_PAGEDOWN:
							dx = _this->m_cliwidth * 1/4; break;
						case SB_THUMBPOSITION:
							dx = pos - _this->m_hScrollPos;
						case SB_THUMBTRACK:
							dx = pos - _this->m_hScrollPos;
						}
						_this->ScrollScreen(dx,0);
						
						return 0;
					}
					
				case WM_VSCROLL:
					{
						int dy = 0;
						int pos = HIWORD(wParam);
						switch (LOWORD(wParam)) {
						case SB_LINEUP:
							dy = -2; break;
						case SB_LINEDOWN:
							dy = 2; break;
						case SB_PAGEUP:
							dy = _this->m_cliheight * -1/4; break;
						case SB_PAGEDOWN:
							dy = _this->m_cliheight * 1/4; break;
						case SB_THUMBPOSITION:
							dy = pos - _this->m_vScrollPos;
						case SB_THUMBTRACK:
							dy = pos - _this->m_vScrollPos;
						}
						_this->ScrollScreen(0,dy);
						
						return 0;
					}
					
					// RealVNC 335 method
				case WM_MOUSEWHEEL:
					if (!_this->m_opts.m_ViewOnly)
						_this->ProcessMouseWheel((SHORT)HIWORD(wParam));
					return 0;

					
				//Added by: Lars Werner (http://lars.werner.no) - These is the custom messages from the TitleBar
				case tbWM_CLOSE:
					SendMessage(_this->m_hwndMain, WM_CLOSE,NULL,NULL);
					return 0;

				case tbWM_MINIMIZE:
					_this->SetDormant(true);
					ShowWindow(_this->m_hwndMain, SW_MINIMIZE);
					return 0;

				case tbWM_MAXIMIZE:
					//_this->SetFullScreenMode(!_this->InFullScreenMode());
					_this->SetFullScreenMode(FALSE);
					return 0;
			} // end of iMsg switch
			
			//return DefWindowProc(hwnd, iMsg, wParam, lParam);

			// Process asynchronous FileTransfer in this thread
			if ((iMsg == FileTransferSendPacketMessage) && (_this->m_pFileTransfer != NULL))
			{
				if (LOWORD(wParam) == 0)
				{
//					if (_this->m_FTtimer != 0)_this->m_FTtimer=SetTimer(hwnd,11, 100, 0);//
					_this->m_pFileTransfer->SendFileChunk();
				}
				else
					_this->m_pFileTransfer->ProcessFileTransferMsg();
				return 0;
			}

		} // End if Main Window
	}
	
	return DefWindowProc(hwnd, iMsg, wParam, lParam);
	
	// We know about an unused variable here.
#pragma warning(disable : 4101)
}


//
//
//
LRESULT CALLBACK ClientConnection::WndProcTBwin(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
	ClientConnection *_this = (ClientConnection *) GetWindowLong(hwnd, GWL_USERDATA);
	if (_this == NULL) return DefWindowProc(hwnd, iMsg, wParam, lParam);

	HWND parent;
	if (_this->m_opts.m_ShowToolbar==true)
		{
			parent = _this->m_hwndMain;
			switch (iMsg) 
			{
			case WM_PAINT:
				{
					if (_this->m_logo_wnd)
						{
						/*HDC hdcX,hdcBits;
						hdcX = GetDC(_this->m_logo_wnd);
						hdcBits = CreateCompatibleDC(hdcX);
						SelectObject(hdcBits,_this->m_logo_min);
						BitBlt(hdcX,0,0,70,28,hdcBits,0,0,SRCCOPY);
						DeleteDC(hdcBits);
						ReleaseDC(_this->m_logo_wnd,hdcX);*/
						UpdateWindow(_this->m_logo_wnd);
						}
					break;
				}
				
			case WM_COMMAND:
				if (LOWORD(wParam) == ID_BUTTON_INFO)
				{
					if (IsWindow(_this->m_hwndStatus)){
						if (_this->m_hwndStatus)SetForegroundWindow(_this->m_hwndStatus);
						if (_this->m_hwndStatus)ShowWindow(_this->m_hwndStatus, SW_NORMAL);
					}else{
						SECURITY_ATTRIBUTES   lpSec;
						DWORD				  threadID;
						_this->m_statusThread = CreateThread(NULL,0,(LPTHREAD_START_ROUTINE )ClientConnection::GTGBS_ShowStatusWindow,(LPVOID)_this,0,&threadID);
					}
					return 0;
				}
				if (LOWORD(wParam) ==9998)
				{
					vnclog.Print(0,_T("CLICKK %d\n"),HIWORD(wParam));
					switch (HIWORD(wParam)) {
						case 0:
								{
								int port;
								TCHAR fulldisplay[256];
								TCHAR display[256];
								GetDlgItemText(hwnd, 9999, display, 256);
								_tcscpy(fulldisplay, display);
								vnclog.Print(0,_T("CLICKK %s\n"),fulldisplay);
								ParseDisplay(fulldisplay, display, 256, &port);
								_this->m_pApp->NewConnection(display,port, NULL, NULL);
								}
						}
					break;
					return TRUE;
				}

				
				if (LOWORD(wParam) == ID_BUTTON_SEP)
				{
					UINT Key;
					//_this->SendKeyEvent(XK_Execute,     true);
					//_this->SendKeyEvent(XK_Execute,     false);
					Key = DialogBox(_this->m_pApp->m_instance,MAKEINTRESOURCE(IDD_CUSTUM_KEY),NULL,(DLGPROC)ClientConnection::GTGBS_SendCustomKey_proc);
					if (Key>0){
						vnclog.Print(0,_T("START Send Custom Key %d\n"),Key);
						if ( (Key & KEYMAP_LALT) == KEYMAP_LALT){
							_this->SendKeyEvent(XK_Alt_L,true);
							_this->SendKeyEvent(Key ^ KEYMAP_LALT,true);
							_this->SendKeyEvent(Key ^ KEYMAP_LALT,false);
							_this->SendKeyEvent(XK_Alt_L,false);
						}else if ( (Key & KEYMAP_RALT) ==KEYMAP_RALT){
							_this->SendKeyEvent(XK_Alt_R,true);
							_this->SendKeyEvent(XK_Control_R,true);
							_this->SendKeyEvent(Key ^ KEYMAP_RALT,true);
							_this->SendKeyEvent(Key ^ KEYMAP_RALT,false);
							_this->SendKeyEvent(XK_Alt_R,false);
							_this->SendKeyEvent(XK_Control_R,false);
							
						}else if ( (Key &  KEYMAP_RCONTROL) == KEYMAP_RCONTROL){
							_this->SendKeyEvent(XK_Control_R,true);
							_this->SendKeyEvent(Key ^ KEYMAP_RCONTROL,true);
							_this->SendKeyEvent(Key ^ KEYMAP_RCONTROL,false);
							_this->SendKeyEvent(XK_Control_R,false);
						}else{
							_this->SendKeyEvent(Key,true);
							_this->SendKeyEvent(Key,false);
						}
						
						
						vnclog.Print(0,_T("END   Send Custom Key %d\n"),Key);
					}
					SetForegroundWindow(_this->m_hwnd);
					
					return 0;
				}

				if (LOWORD(wParam) == ID_BUTTON_END ) 
				{
					SendMessage(parent,WM_CLOSE,(WPARAM)0,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_CAD ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_CONN_CTLALTDEL,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_FULLSCREEN ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_FULLSCREEN,(LPARAM)0);
					return 0;
				}
			
				if (LOWORD(wParam) == ID_BUTTON_FTRANS ) 
				{
					if (_this->m_pFileTransfer->m_fFileTransferRunning)
					{
						_this->m_pFileTransfer->ShowFileTransferWindow(true);
					}
					else
						SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_FILETRANSFER,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_DBUTTON ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_DBUTTON,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_SW ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_SW,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_DESKTOP ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_DESKTOP,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_TEXTCHAT ) 
				{
					if (_this->m_pTextChat->m_fTextChatRunning)
					{
						_this->m_pTextChat->ShowChatWindow(true);
					}
					else
						SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_TEXTCHAT,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_DINPUT ) 
				{
					if (_this->m_remote_mouse_disable)
					{
						_this->m_remote_mouse_disable=false;
						SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_INPUT,(LPARAM)0);
						SendMessage(parent,WM_SIZE,(WPARAM)ID_DINPUT,(LPARAM)0);
					}
					else
					{
						_this->m_remote_mouse_disable=true;
						SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_DINPUT,(LPARAM)0);
						SendMessage(parent,WM_SIZE,(WPARAM)ID_DINPUT,(LPARAM)0);
					}
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_PROPERTIES ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)IDC_OPTIONBUTTON,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_REFRESH ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_REQUEST_REFRESH,(LPARAM)0);
					return 0;
				}
				
				if (LOWORD(wParam) == ID_BUTTON_STRG_ESC ) 
				{
					SendMessage(parent,WM_SYSCOMMAND,(WPARAM)ID_CONN_CTLESC,(LPARAM)0);
					return 0;
				}
			}
		}
return DefWindowProc(hwnd, iMsg, wParam, lParam);
}


//
//
//
LRESULT CALLBACK ClientConnection::WndProchwnd(HWND hwnd, UINT iMsg, WPARAM wParam, LPARAM lParam)
{
	
	//	HWND parent;
	ClientConnection *_this = (ClientConnection *) GetWindowLong(hwnd, GWL_USERDATA);
	if (_this == NULL) return DefWindowProc(hwnd, iMsg, wParam, lParam);
	switch (iMsg) 
			{
				
			case WM_CREATE:
				SetTimer(_this->m_hwnd,3335, 1000, NULL);
				return 0;
				
			case WM_REGIONUPDATED:
				//_this->DoBlit();
				_this->SendAppropriateFramebufferUpdateRequest();
				return 0;
				
			case WM_PAINT:
				_this->DoBlit();
				return 0;
				
			case WM_TIMER:
				if (wParam == _this->m_emulate3ButtonsTimer)
				{
					_this->SubProcessPointerEvent( 
						_this->m_emulateButtonPressedX,
						_this->m_emulateButtonPressedY,
						_this->m_emulateKeyFlags);
					KillTimer(_this->m_hwnd, _this->m_emulate3ButtonsTimer);
					_this->m_waitingOnEmulateTimer = false;
				}
				return 0;
				
			case WM_LBUTTONDOWN:
			case WM_LBUTTONUP:
				if (_this->m_SWselect) 
				{
					_this->m_SWpoint.x=LOWORD(lParam);
					_this->m_SWpoint.y=HIWORD(lParam);
					_this->SendSW(_this->m_SWpoint.x,_this->m_SWpoint.y);
					return 0;
				}
			case WM_MBUTTONDOWN:
			case WM_MBUTTONUP:
			case WM_RBUTTONDOWN:
			case WM_RBUTTONUP:
			case WM_MOUSEMOVE:
				{
					if (_this->m_SWselect) {return 0;}
					if (!_this->m_running) return 0;
//					if (GetFocus() != hwnd) return 0;
//					if (GetFocus() != _this->m_hwnd) return 0;
#ifndef _ULTRAVNCAX_
					if (GetFocus() != _this->m_hwndMain) return 0;
#endif
					int x = LOWORD(lParam);
					int y = HIWORD(lParam);
					wParam = MAKEWPARAM(LOWORD(wParam), 0);
					if (_this->InFullScreenMode()) {
						if (_this->BumpScroll(x,y))
							return 0;
					}
					if ( _this->m_opts.m_ViewOnly) return 0;
					_this->ProcessPointerEvent(x,y, wParam, iMsg);
					return 0;
				}
				
			case WM_KEYDOWN:
			case WM_KEYUP:
			case WM_SYSKEYDOWN:
			case WM_SYSKEYUP:
				{
					if (!_this->m_running) return 0;
					if ( _this->m_opts.m_ViewOnly) return 0;
					_this->ProcessKeyEvent((int) wParam, (DWORD) lParam);
					return 0;
				}
				
			case WM_CHAR:
			case WM_SYSCHAR:
#ifdef UNDER_CE
				{
					int key = wParam;
					vnclog.Print(4,_T("CHAR msg : %02x\n"), key);
					// Control keys which are in the Keymap table will already
					// have been handled.
					if (key == 0x0D  ||  // return
						key == 0x20 ||   // space
						key == 0x08)     // backspace
						return 0;
					
					if (key < 32) key += 64;  // map ctrl-keys onto alphabet
					if (key > 32 && key < 127) {
						_this->SendKeyEvent(wParam & 0xff, true);
						_this->SendKeyEvent(wParam & 0xff, false);
					}
					return 0;
				}
#endif
			case WM_DEADCHAR:
			case WM_SYSDEADCHAR:
				return 0;
				
			case WM_SETFOCUS:
				if (_this->InFullScreenMode())
					SetWindowPos(hwnd, HWND_TOPMOST, 0,0,100,100, SWP_NOMOVE | SWP_NOSIZE);
				return 0;

				// Cacnel modifiers when we lose focus
			case WM_KILLFOCUS:
				{
					
					if (!_this->m_running) return 0;
					if (_this->InFullScreenMode()) {
						// We must top being topmost, but we want to choose our
						// position carefully.
						HWND foreground = GetForegroundWindow();
						HWND hwndafter = NULL;
						if ((foreground == NULL) || 
							(GetWindowLong(foreground, GWL_EXSTYLE) & WS_EX_TOPMOST)) {
							hwndafter = HWND_NOTOPMOST;
						} else {
							hwndafter = GetNextWindow(foreground, GW_HWNDNEXT); 
						}
						
						SetWindowPos(hwnd, hwndafter, 0,0,100,100, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
					}
					/*	
					vnclog.Print(6, _T("Losing focus - cancelling modifiers\n"));
					_this->SendKeyEvent(XK_Alt_L,     false);
					_this->SendKeyEvent(XK_Control_L, false);
					_this->SendKeyEvent(XK_Shift_L,   false);
					_this->SendKeyEvent(XK_Alt_R,     false);
					_this->SendKeyEvent(XK_Control_R, false);
					_this->SendKeyEvent(XK_Shift_R,   false);
					*/
					return 0;
				}

			case WM_CLOSE:
				{
#ifndef _ULTRAVNCAX_
					// sf@2002 - Do not close vncviewer if the File Transfer GUI is open !
					if (_this->m_pFileTransfer->m_fFileTransferRunning)
					{
						_this->m_pFileTransfer->ShowFileTransferWindow(true);
						MessageBox(NULL, sz_L85, 
							sz_L88, 
							MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
						return 0;
					}
					
					// sf@2002 - Do not close vncviewer if the Text Chat GUI is open !
					if (_this->m_pTextChat->m_fTextChatRunning)
					{
						_this->m_pTextChat->ShowChatWindow(true);
						MessageBox(NULL, sz_L86, 
							sz_L88, 
							MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
						return 0;
					}

					if (_this->m_fOptionsOpen)
					{
						MessageBox(NULL, sz_L87, 
							sz_L88, 
							MB_OK | MB_ICONSTOP | MB_SETFOREGROUND | MB_TOPMOST);
						return 0;
					}
#endif
					
					// Close the worker thread as well
					_this->KillThread();
					DestroyWindow(hwnd);
					return 0;
				}
				
			case WM_DESTROY:
				{
				#ifndef UNDER_CE
				// Remove us from the clipboard viewer chain
				BOOL res = ChangeClipboardChain( hwnd, _this->m_hwndNextViewer);
				#endif
				if (_this->m_waitingOnEmulateTimer)
				{
				KillTimer(_this->m_hwnd, _this->m_emulate3ButtonsTimer);
				KillTimer(_this->m_hwnd, 3335);
				_this->m_waitingOnEmulateTimer = false;
				}
				/*
				  _this->m_hwnd = 0;
				  // We are currently in the main thread.
				  // The worker thread should be about to finish if
				  // it hasn't already. Wait for it.
				  try {
				  void *p;
				  _this->join(&p);  // After joining, _this is no longer valid
				  } catch (omni_thread_invalid& e) {
				  // The thread probably hasn't been started yet,
				  }*/
				  
					return 0;
				}
				
				
			case WM_QUERYNEWPALETTE:
				{
					TempDC hDC(hwnd);
					
					// Select and realize hPalette
					PaletteSelector p(hDC, _this->m_hPalette);
					InvalidateRect(hwnd, NULL, FALSE);
					UpdateWindow(hwnd);
					return TRUE;
				}
				
			case WM_PALETTECHANGED:
				// If this application did not change the palette, select
				// and realize this application's palette
				if ((HWND) wParam != hwnd)
				{
					// Need the window's DC for SelectPalette/RealizePalette
					TempDC hDC(hwnd);
					PaletteSelector p(hDC, _this->m_hPalette);
					// When updating the colors for an inactive window,
					// UpdateColors can be called because it is faster than
					// redrawing the client area (even though the results are
					// not as good)
#ifndef UNDER_CE
					UpdateColors(hDC);
#else
					InvalidateRect(hwnd, NULL, FALSE);
					UpdateWindow(hwnd);
#endif
					
				}
				break;
				
#ifndef UNDER_CE
			case WM_SIZING:
				{
					// Don't allow sizing larger than framebuffer
					RECT *lprc = (LPRECT) lParam;
					switch (wParam) {
					case WMSZ_RIGHT: 
					case WMSZ_TOPRIGHT:
					case WMSZ_BOTTOMRIGHT:
						lprc->right = min(lprc->right, lprc->left + _this->m_fullwinwidth+1);
						break;
					case WMSZ_LEFT:
					case WMSZ_TOPLEFT:
					case WMSZ_BOTTOMLEFT:
						lprc->left = max(lprc->left, lprc->right - _this->m_fullwinwidth);
						break;
					}
					
					switch (wParam) {
					case WMSZ_TOP:
					case WMSZ_TOPLEFT:
					case WMSZ_TOPRIGHT:
						lprc->top = max(lprc->top, lprc->bottom - _this->m_fullwinheight);
						break;
					case WMSZ_BOTTOM:
					case WMSZ_BOTTOMLEFT:
					case WMSZ_BOTTOMRIGHT:
						lprc->bottom = min(lprc->bottom, lprc->top + _this->m_fullwinheight);
						break;
					}
					
					return 0;
				}
				
			case WM_SETCURSOR:
				{
					// if we have the focus, let the cursor change as normal
					if (GetFocus() == hwnd) 
						break;
					
					HCURSOR h;
					switch (_this->m_opts.m_localCursor) {
					case NOCURSOR:
						h= LoadCursor(_this->m_pApp->m_instance, MAKEINTRESOURCE(IDC_NOCURSOR));
						break;
					case NORMALCURSOR:
						h= LoadCursor(NULL, IDC_ARROW);
						break;
					case DOTCURSOR:
					default:
						h= LoadCursor(_this->m_pApp->m_instance, MAKEINTRESOURCE(IDC_DOTCURSOR));
					}
					if (_this->m_SWselect) h= LoadCursor(_this->m_pApp->m_instance, MAKEINTRESOURCE(IDC_CURSOR1));
					SetCursor(h);
					
					return 0;
				}
				
				
			case WM_DRAWCLIPBOARD:
				_this->ProcessLocalClipboardChange();
				return 0;
				
			case WM_CHANGECBCHAIN:
				{
					// The clipboard chain is changing
					HWND hWndRemove = (HWND) wParam;     // handle of window being removed 
					HWND hWndNext = (HWND) lParam;       // handle of next window in chain 
					// If next window is closing, update our pointer.
					if (hWndRemove == _this->m_hwndNextViewer)  
						_this->m_hwndNextViewer = hWndNext;  
					// Otherwise, pass the message to the next link.  
					else if (_this->m_hwndNextViewer != NULL) 
						::SendMessage(_this->m_hwndNextViewer, WM_CHANGECBCHAIN, 
						(WPARAM) hWndRemove,  (LPARAM) hWndNext );  
					return 0;
					
				}
#endif

			// Modif VNCon MultiView support
			// Messages used by VNCon - Copyright (C) 2001-2003 - Alastair Burr
			case WM_GETSCALING:
				{
					WPARAM wPar;
					wPar = MAKEWPARAM(_this->m_hScrollMax, _this->m_vScrollMax);
					SendMessage((HWND)wParam, WM_GETSCALING, wPar, lParam);
					return TRUE;
					
				}
				
			case WM_SETSCALING:
				{          
					_this->m_opts.m_scaling = true;
					_this->m_opts.m_scale_num = wParam;
					_this->m_opts.m_scale_den = lParam;
					if (_this->m_opts.m_scale_num == 1 && _this->m_opts.m_scale_den == 1)
						_this->m_opts.m_scaling = false;
					_this->SizeWindow();
					InvalidateRect(hwnd, NULL, TRUE);
					return TRUE;
					
				}
				
			case WM_SETVIEWONLY:
				{
					_this->m_opts.m_ViewOnly = (wParam == 1);
					return TRUE;
				}
			// End Modif for VNCon MultiView support


			}//end switch (iMsg) 
			
			return DefWindowProc(hwnd, iMsg, wParam, lParam);
}
void
ClientConnection:: ConvertAll(int width, int height, int xx, int yy,int bytes_per_pixel,BYTE* source,BYTE* dest,int framebufferWidth)
{
	int bytesPerInputRow = width * bytes_per_pixel;
	int bytesPerOutputRow = framebufferWidth * bytes_per_pixel;
	BYTE *sourcepos,*destpos;
	destpos = (BYTE *)dest + (bytesPerOutputRow * yy)+(xx * bytes_per_pixel);
	sourcepos=(BYTE*)source;

    int y;
    width*=bytes_per_pixel;
    for (y=0; y<height; y++) {
        memcpy(destpos, sourcepos, width);
        sourcepos = (BYTE*)sourcepos + bytesPerInputRow;
        destpos = (BYTE*)destpos + bytesPerOutputRow;
    }
}

bool
ClientConnection:: Check_Rectangle_borders(int x,int y,int w,int h)
{
	if (x<0) return false;
	if (y<0) return false;
	if (x+w>m_si.framebufferWidth) return false;
	if (y+h>m_si.framebufferHeight) return false;
	if (x+w<x) return false;
	if (y+h<y) return false;
	return true;
}

#pragma warning(default :4101)
