#include <winsock2.h>
#include <windows.h>
#include "resource\resource.h"
#include <commctrl.h>
#include <stdio.h>

//==================Header===========================
#define ICMP_ECHOREPLY	0
#define ICMP_ECHOREQ	8

typedef char rfbProtocolVersionMsg[13];	/* allow extra byte for null */
#define sz_rfbProtocolVersionMsg 12
#define rfbProtocolVersionFormat "RFB %03d.%03d\n"

// IP Header -- RFC 791
typedef struct tagIPHDR
{
	u_char  VIHL;			// Version and IHL
	u_char	TOS;			// Type Of Service
	short	TotLen;			// Total Length
	short	ID;				// Identification
	short	FlagOff;		// Flags and Fragment Offset
	u_char	TTL;			// Time To Live
	u_char	Protocol;		// Protocol
	u_short	Checksum;		// Checksum
	struct	in_addr iaSrc;	// Internet Address - Source
	struct	in_addr iaDst;	// Internet Address - Destination
}IPHDR, *PIPHDR;
// ICMP Header - RFC 792
typedef struct tagICMPHDR
{
	u_char	Type;			// Type
	u_char	Code;			// Code
	u_short	Checksum;		// Checksum
	u_short	ID;				// Identification
	u_short	Seq;			// Sequence
	char	Data;			// Data
}ICMPHDR, *PICMPHDR;
#define REQ_DATASIZE 32		// Echo Request Data size
#define	DEFDATALEN_SMALL	44	/* default small data len */
#define	DEFDATALEN_BIG		108	/* default big data len */
// ICMP Echo Request
typedef struct tagECHOREQUEST
{
	ICMPHDR icmpHdr;
	DWORD	dwTime;
	char	cData[REQ_DATASIZE];
}ECHOREQUEST, *PECHOREQUEST;
// ICMP Echo Reply
typedef struct tagECHOREPLY
{
	IPHDR	ipHdr;
	ECHOREQUEST	echoRequest;
	char    cFiller[256];
}ECHOREPLY, *PECHOREPLY;

//=================functions=======================
BOOL Ping(BYTE b1,BYTE b2,BYTE b3,BYTE b4);
void ReportError(LPCSTR pstrFrom);
int  WaitForEchoReply(SOCKET s);
u_short in_cksum(u_short *addr, int len);

// ICMP Echo Request/Reply functions
int		SendEchoRequest(SOCKET, LPSOCKADDR_IN);
DWORD	RecvEchoReply(SOCKET, LPSOCKADDR_IN, u_char *);
int vncscan(BYTE b1,BYTE b2,BYTE b3,BYTE b4,int m_port,int mask,BYTE digit);
void startvnc(char Temp1[255]);

//==============Global Vatriabls===================
static HWND hList=NULL;  // List View identifier
LVCOLUMN LvCol; // Make Coluom struct for ListView
LVITEM LvItem;  // ListView Item struct
int iSelect=0;
int flag=0;
HWND hEdit;
BYTE b1,b2,b3,b4;
SOCKET m_sock;
DWORD	  dwElapsed;
UINT m_timer;
BOOL scanner=false;
BYTE digit;
int mask;
int timeout,mytimer;
int timeoutsec;
int     m_port;
//===================================================

//======================Handles================================================//
HINSTANCE hInst; // main function handler
#define WIN32_LEAN_AND_MEAN // this will assume smaller exe

//================================About dialog window=============================//

void GetIp()
{
	char     Hostname[100];
	HOSTENT *pHostEnt;
	int  **ppaddr;
	SOCKADDR_IN sockAddr;
	// Initialise winsock
	WORD wVersionRequested = MAKEWORD(2, 0);
	WSADATA wsaData;
	if (WSAStartup(wVersionRequested, &wsaData) != 0) 
		{
			return;
		}

	////Find Local Adress
	gethostname( Hostname, sizeof( Hostname ));
	pHostEnt = gethostbyname( Hostname );
	ppaddr = (int**)pHostEnt->h_addr_list;
	sockAddr.sin_addr.s_addr = **ppaddr;
	b1=sockAddr.sin_addr.S_un.S_un_b.s_b1;
	b2=sockAddr.sin_addr.S_un.S_un_b.s_b2;
	b3=sockAddr.sin_addr.S_un.S_un_b.s_b3;
	b4=sockAddr.sin_addr.S_un.S_un_b.s_b4;
	WSACleanup ();
}
BOOL CALLBACK DialogProc(HWND hWnd, UINT Message, WPARAM wParam, LPARAM lParam)
{
//	if ((Message!=WM_TIMER || Message!=WM_CLOSE) && scanner) return false;
  switch(Message)
  {
        
         // This Window Message will close the dialog  //
		//============================================//
		case WM_CLOSE:
			{
				KillTimer(hWnd, m_timer);
			     EndDialog(hWnd,0); // kill dialog
			}
			break;

		case WM_NOTIFY:
		{
			switch(LOWORD(wParam))
			{
			    case IDC_LIST: 
                if(((LPNMHDR)lParam)->code == NM_DBLCLK)
				{
				  char Text[255]={0};  
				  char Temp[255]={0};
				  char Temp1[255]={0};
				  int iSlected=0;
				  int j=0;

				  iSlected=SendMessage(hList,LVM_GETNEXTITEM,-1,LVNI_FOCUSED);
				  
				  if(iSlected==-1)
				  {
                    MessageBox(hWnd,"No Items in ListView","Error",MB_OK|MB_ICONINFORMATION);
					break;
				  }

				  memset(&LvItem,0,sizeof(LvItem));
                  LvItem.mask=LVIF_TEXT;
				  LvItem.iSubItem=0;
				  LvItem.pszText=Text;
				  LvItem.cchTextMax=256;
				  LvItem.iItem=iSlected;
                  
				  SendMessage(hList,LVM_GETITEMTEXT, iSlected, (LPARAM)&LvItem);
				  
				  sprintf(Temp1,Text);
				  
				  /*for(j=1;j<=4;j++)
				  {
					LvItem.iSubItem=j;
				    SendMessage(hList,LVM_GETITEMTEXT, iSlected, (LPARAM)&LvItem);
				    sprintf(Temp," %s",Text);
					lstrcat(Temp1,Temp);
				  }*/

				  startvnc(Temp1);

				}
				if(((LPNMHDR)lParam)->code == NM_CLICK)
				{
					iSelect=SendMessage(hList,LVM_GETNEXTITEM,-1,LVNI_FOCUSED);
				    
					if(iSelect==-1)
					{
                      MessageBox(hWnd,"No Vnc server selected","Error",MB_OK|MB_ICONINFORMATION);
					  break;
					}
					flag=1;
				}

				if(((LPNMHDR)lParam)->code == LVN_BEGINLABELEDIT)
				{
                  hEdit=ListView_GetEditControl(hList);
				}
				
				if(((LPNMHDR)lParam)->code == LVN_ENDLABELEDIT)
				{
					int iIndex;
					char text[255]="";
					iIndex=SendMessage(hList,LVM_GETNEXTITEM,-1,LVNI_FOCUSED);
				    LvItem.iSubItem=0;
                    LvItem.pszText=text;
                    GetWindowText(hEdit, text, sizeof(text));
					SendMessage(hList,LVM_SETITEMTEXT,(WPARAM)iIndex,(LPARAM)&LvItem);
				}
				break;
			}
		}

		case WM_PAINT:
			{
				return 0;
			}
			break;
		case WM_TIMER:
			{
				if (scanner==true)
				{
					digit++;
					if (digit>=254)
					{
						scanner=false;
						SetDlgItemText(hWnd, IDC_STATUS2, "Done");
						digit=1;
					}
					SetDlgItemInt(hWnd, IDC_STATUS, digit, false);
					vncscan(b1,b2,b3,b4,m_port,mask,digit);
				}
			}
			break;

		// This Window Message is the heart of the dialog  //
		//================================================//
		case WM_INITDIALOG:
			{
				GetIp();

                SetFocus(hWnd);
				hList=GetDlgItem(hWnd,IDC_LIST); // get the ID of the ListView
				SendMessage(hList,LVM_SETEXTENDEDLISTVIEWSTYLE,0,LVS_EX_FULLROWSELECT); // Set style
				SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS1), IPM_SETADDRESS, 0, MAKEIPADDRESS(b1,b2,b3,b4));//(LPARAM)&dwAddr);
				SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS2), IPM_SETADDRESS, 0, MAKEIPADDRESS(255,255,255,0));
				SetDlgItemInt( hWnd, IDC_PORT,5900, FALSE);

				// Here we put the info on the Coulom headers
				// this is not data, only name of each header we like
                memset(&LvCol,0,sizeof(LvCol)); // Reset Coluom
				LvCol.mask=LVCF_TEXT|LVCF_WIDTH|LVCF_SUBITEM; // Type of mask
				LvCol.cx=0x40;                                // width between each coloum
				LvCol.pszText="Server";                     // First Header
 				LvCol.cx=0x60;

				// Inserting Couloms as much as we want
				SendMessage(hList,LVM_INSERTCOLUMN,0,(LPARAM)&LvCol); // Insert/Show the coloum
				LvCol.pszText="MS logon";                          // Next coloum
                SendMessage(hList,LVM_INSERTCOLUMN,1,(LPARAM)&LvCol); // ...
				LvCol.pszText="FileTrans";                       //
                SendMessage(hList,LVM_INSERTCOLUMN,2,(LPARAM)&LvCol); //
				LvCol.pszText="Type";                              //
				SendMessage(hList,LVM_INSERTCOLUMN,3,(LPARAM)&LvCol); //
				

                memset(&LvItem,0,sizeof(LvItem)); // Reset Item Struct
				
				//  Setting properties Of Items:

				LvItem.mask=LVIF_TEXT;   // Text Style
				LvItem.cchTextMax = 256; // Max size of test
                
				LvItem.iItem=0;          // choose item  
				LvItem.iSubItem=0;       // Put in first coluom
				LvItem.pszText="Item 0"; // Text to display (can be from a char variable) (Items)
				m_timer=SetTimer( hWnd, 1,  200, NULL);
				
				return TRUE; // Always True			
			}
			break;

     // This Window Message will control the dialog  //
	//==============================================//
        case WM_COMMAND:
		{
                 switch(LOWORD(wParam)) // what we press on?
				 {
						case IDC_SCAN:
							{
								DWORD dwAddr;
								DWORD dwMask;
									SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS1), IPM_GETADDRESS, 0, (LPARAM)&dwAddr);
									SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS2), IPM_GETADDRESS, 0, (LPARAM)&dwMask);
									SendMessage(hList,LVM_DELETEALLITEMS,0,0);
									b1=FIRST_IPADDRESS(dwMask);
									b2=SECOND_IPADDRESS(dwMask);
									b3=THIRD_IPADDRESS(dwMask);
									b4=FOURTH_IPADDRESS(dwMask);
									mask=0;
									if (b4==0) mask=1;
									if (b3==0) mask=2;
									b1=FIRST_IPADDRESS(dwAddr);
									b2=SECOND_IPADDRESS(dwAddr);
									b3=THIRD_IPADDRESS(dwAddr);
									b4=FOURTH_IPADDRESS(dwAddr);
									m_port=GetDlgItemInt( hWnd, IDC_PORT, NULL, TRUE);
									digit=1;
								timeoutsec=0;
								timeout=10000;
								scanner=true;
								SetDlgItemText(hWnd, IDC_STATUS2, "Please wait..scanning");
								break;

							}

						case IDC_SCAN2:
							{
								DWORD dwAddr;
								DWORD dwMask;
								SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS1), IPM_GETADDRESS, 0, (LPARAM)&dwAddr);
								SendMessage(GetDlgItem(hWnd, IDC_IPADDRESS2), IPM_GETADDRESS, 0, (LPARAM)&dwMask);
								SendMessage(hList,LVM_DELETEALLITEMS,0,0);
								b1=FIRST_IPADDRESS(dwMask);
								b2=SECOND_IPADDRESS(dwMask);
								b3=THIRD_IPADDRESS(dwMask);
								b4=FOURTH_IPADDRESS(dwMask);
								mask=0;
								if (b4==0) mask=1;
								if (b3==0) mask=2;
								b1=FIRST_IPADDRESS(dwAddr);
								b2=SECOND_IPADDRESS(dwAddr);
								b3=THIRD_IPADDRESS(dwAddr);
								b4=FOURTH_IPADDRESS(dwAddr);
								digit=1;
								timeoutsec=2;
								timeout=0;
								m_port=GetDlgItemInt( hWnd, IDC_PORT, NULL, TRUE);
								scanner=true;
								SetDlgItemText(hWnd, IDC_STATUS2, "Please wait..scanning");
								break;

							}
				 }
		}
        break;
    
	    default:
		{
             return FALSE;
		}
    }

	return TRUE;
}

//===========================MAIN FUNCTION-WIN32 STARTING POINT========================================//
int WINAPI WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, PSTR szCmdLine, int iCmdShow)
{
	// add this code if win2000/nt/xp doesn't
	// load ur winmain (experimental only)
	// also add comctl32.lib

	INITCOMMONCONTROLSEX InitCtrls;
    InitCtrls.dwICC = ICC_LISTVIEW_CLASSES|ICC_INTERNET_CLASSES;
    InitCtrls.dwSize = sizeof(INITCOMMONCONTROLSEX);
    BOOL bRet = InitCommonControlsEx(&InitCtrls);

	hInst=hInstance;
	DialogBoxParam(hInstance, MAKEINTRESOURCE(IDC_DIALOG), NULL, (DLGPROC)DialogProc,0);
	return 0;
}
//======================================================================================================//
bool ReadExact(char *inbuf, int wanted)
{
	

	int offset = 0;
	while (wanted > 0) {

		int bytes = recv(m_sock, inbuf+offset, wanted, 0);
		if (bytes == 0) return false;
		if (bytes == SOCKET_ERROR) {
			return false;
		}
		wanted -= bytes;
		offset += bytes;

	}
	return true;
}


int vncscan(BYTE b1,BYTE b2,BYTE b3,BYTE b4,int m_port,int mask,BYTE digit)
{
	int res;
	SOCKADDR_IN thataddr;
	unsigned long yes;
	m_sock = INVALID_SOCKET;
	yes = 1;
	setbuf(stderr, 0);

	// Initialise winsock
	WORD wVersionRequested = MAKEWORD(2, 0);
	WSADATA wsaData;
	if (WSAStartup(wVersionRequested, &wsaData) != 0) 
		{
			return 0;
		}
	//init
	char     Hostname[100];
	HOSTENT *pHostEnt;
	int  **ppaddr;
	SOCKADDR_IN sockAddr;
	gethostname( Hostname, sizeof( Hostname ));
	pHostEnt = gethostbyname( Hostname );
	ppaddr = (int**)pHostEnt->h_addr_list;
	sockAddr.sin_addr.s_addr = **ppaddr;
	thataddr.sin_addr.s_addr = sockAddr.sin_addr.s_addr;
	thataddr.sin_addr.S_un.S_un_b.s_b1=b1;
	thataddr.sin_addr.S_un.S_un_b.s_b2=b2;
	thataddr.sin_addr.S_un.S_un_b.s_b3=b3;
	thataddr.sin_addr.S_un.S_un_b.s_b4=b4;
	thataddr.sin_addr.S_un.S_un_b.s_b4=digit;

		if (Ping(thataddr.sin_addr.S_un.S_un_b.s_b1,thataddr.sin_addr.S_un.S_un_b.s_b2,thataddr.sin_addr.S_un.S_un_b.s_b3,thataddr.sin_addr.S_un.S_un_b.s_b4))
			{
				m_sock = socket(AF_INET, SOCK_STREAM, 0);
				// Set the socket options:
				if (m_sock == INVALID_SOCKET) return 0;
				BOOL nodelayval = TRUE;
				if (setsockopt(m_sock, IPPROTO_TCP, TCP_NODELAY, (const char *) &nodelayval, sizeof(BOOL))) return 0;
				setsockopt(m_sock, SOL_SOCKET, SO_REUSEADDR, (const char *) &nodelayval, sizeof(BOOL));
		
				thataddr.sin_family = AF_INET;
				thataddr.sin_port = htons(m_port);
				res = connect(m_sock, (LPSOCKADDR) &thataddr, sizeof(thataddr));
				if (res == SOCKET_ERROR)
					{	
						printf("Socket error %d\n",digit);
					}
				else
					{
					rfbProtocolVersionMsg pv;
					ReadExact(pv, sz_rfbProtocolVersionMsg);
					pv[sz_rfbProtocolVersionMsg] = 0;
					int m_majorVersion,m_minorVersion;
					sscanf(pv,rfbProtocolVersionFormat,&m_majorVersion,&m_minorVersion);
					printf("%s %i %i %d\r\n" ,inet_ntoa(thataddr.sin_addr),m_majorVersion,m_minorVersion,dwElapsed);

					// Minor = 4 means that server supports FileTransfer and requires ms logon
					// Minor = 6 means that server support FileTransfer and requires normal VNC logon
					int iItem;
					char ItemText[100];
					iItem=SendMessage(hList,LVM_GETITEMCOUNT,0,0);
					LvItem.iItem=iItem;            // choose item  
				    LvItem.iSubItem=0;         // Put in first coluom
				    LvItem.pszText=inet_ntoa(thataddr.sin_addr);   // Text to display (can be from a char variable) (Items)
                    SendMessage(hList,LVM_INSERTITEM,0,(LPARAM)&LvItem); // Send to the Listview
					if (m_minorVersion==6) strcpy(ItemText,"Yes");
					else strcpy(ItemText,"No");
				    LvItem.pszText=ItemText;   // Text to display (can be from a char variable) (Items)
                    LvItem.iSubItem=1;         // Put in first coluom
					SendMessage(hList,LVM_SETITEM,0,(LPARAM)&LvItem);
					if (m_minorVersion==6 || m_minorVersion==4) strcpy(ItemText,"Yes");
					else strcpy(ItemText,"No");
				    LvItem.pszText=ItemText;   // Text to display (can be from a char variable) (Items)
                    LvItem.iSubItem=2;         // Put in first coluom
					SendMessage(hList,LVM_SETITEM,0,(LPARAM)&LvItem);
					if (m_minorVersion==3) strcpy(ItemText,"Real/Tight");
					else strcpy(ItemText,"Ultra");
				    LvItem.pszText=ItemText;   // Text to display (can be from a char variable) (Items)
                    LvItem.iSubItem=3;         // Put in first coluom
					SendMessage(hList,LVM_SETITEM,0,(LPARAM)&LvItem);
					}
				closesocket(m_sock);
		}
	WSACleanup ();
	return 0;
}

BOOL Ping(BYTE b1,BYTE b2,BYTE b3,BYTE b4)
{
	SOCKET	  rawSocket;
	SOCKADDR_IN saDest;
	SOCKADDR_IN saSrc;
	DWORD	  dwTimeSent;
	u_char    cTTL;
	int       nRet;

	// Create a Raw socket
	rawSocket = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
	if (rawSocket == SOCKET_ERROR) 
	{
		return FALSE;
	}

	// Setup destination socket address
	saDest.sin_addr.S_un.S_un_b.s_b1 = b1;
	saDest.sin_addr.S_un.S_un_b.s_b2 = b2;
	saDest.sin_addr.S_un.S_un_b.s_b3 = b3;
	saDest.sin_addr.S_un.S_un_b.s_b4 = b4;
	saDest.sin_family = AF_INET;
	saDest.sin_port = 0;
	SendEchoRequest(rawSocket, &saDest);
	nRet = WaitForEchoReply(rawSocket);
	if (nRet == SOCKET_ERROR)
		{
			return FALSE;
		}
	if (!nRet)
		{
			printf("\nTimeOut");
			return FALSE;
		}
	dwTimeSent = RecvEchoReply(rawSocket, &saSrc, &cTTL);

		// Calculate elapsed time
	dwElapsed = GetTickCount() - dwTimeSent;
	/*printf("\nReply from: %s: bytes=%d time=%ldms TTL=%d", 
               inet_ntoa(saSrc.sin_addr), 
			   REQ_DATASIZE,
               dwElapsed,
               cTTL);*/
	nRet = closesocket(rawSocket);
	if (nRet == SOCKET_ERROR)
		ReportError("closesocket()");
	return TRUE;
}


int SendEchoRequest(SOCKET s,LPSOCKADDR_IN lpstToAddr) 
{
	static ECHOREQUEST echoReq;
	static nId = 1;
	static nSeq = 1;
	int nRet;

	// Fill in echo request
	echoReq.icmpHdr.Type		= ICMP_ECHOREQ;
	echoReq.icmpHdr.Code		= 0;
	echoReq.icmpHdr.Checksum	= 0;
	echoReq.icmpHdr.ID			= nId++;
	echoReq.icmpHdr.Seq			= nSeq++;

	// Fill in some data to send
	for (nRet = 0; nRet < REQ_DATASIZE; nRet++)
		echoReq.cData[nRet] = ' '+nRet;

	// Save tick count when sent
	echoReq.dwTime				= GetTickCount();

	// Put data in packet and compute checksum
	echoReq.icmpHdr.Checksum = in_cksum((u_short *)&echoReq, sizeof(ECHOREQUEST));

	// Send the echo request  								  
	nRet = sendto(s,						/* socket */
				 (LPSTR)&echoReq,			/* buffer */
				 sizeof(ECHOREQUEST),
				 0,							/* flags */
				 (LPSOCKADDR)lpstToAddr, /* destination */
				 sizeof(SOCKADDR_IN));   /* address length */

	if (nRet == SOCKET_ERROR) 
		ReportError("sendto()");
	return (nRet);
}


// RecvEchoReply()
// Receive incoming data
// and parse out fields
DWORD RecvEchoReply(SOCKET s, LPSOCKADDR_IN lpsaFrom, u_char *pTTL) 
{
	ECHOREPLY echoReply;
	int nRet;
	int nAddrLen = sizeof(struct sockaddr_in);

	// Receive the echo reply	
	nRet = recvfrom(s,					// socket
					(LPSTR)&echoReply,	// buffer
					sizeof(ECHOREPLY),	// size of buffer
					0,					// flags
					(LPSOCKADDR)lpsaFrom,	// From address
					&nAddrLen);			// pointer to address len

	// Check return value
	if (nRet == SOCKET_ERROR) 
		ReportError("recvfrom()");

	// return time sent and IP TTL
	*pTTL = echoReply.ipHdr.TTL;
	return(echoReply.echoRequest.dwTime);   		
}

// What happened?
void ReportError(LPCSTR pWhere)
{
	fprintf(stderr,"\n%s error: %d\n",
		WSAGetLastError());
}


// WaitForEchoReply()
// Use select() to determine when
// data is waiting to be read
int WaitForEchoReply(SOCKET s)
{
	struct timeval Timeout;
	fd_set readfds;

	readfds.fd_count = 1;
	readfds.fd_array[0] = s;
	Timeout.tv_sec = timeoutsec;
    Timeout.tv_usec = timeout;

	return(select(1, &readfds, NULL, NULL, &Timeout));
}

u_short in_cksum(u_short *addr, int len)
{
	register int nleft = len;
	register u_short *w = addr;
	register u_short answer;
	register int sum = 0;

	/*
	 *  Our algorithm is simple, using a 32 bit accumulator (sum),
	 *  we add sequential 16 bit words to it, and at the end, fold
	 *  back all the carry bits from the top 16 bits into the lower
	 *  16 bits.
	 */
	while( nleft > 1 )  {
		sum += *w++;
		nleft -= 2;
	}

	/* mop up an odd byte, if necessary */
	if( nleft == 1 ) {
		u_short	u = 0;

		*(u_char *)(&u) = *(u_char *)w ;
		sum += u;
	}

	/*
	 * add back carry outs from top 16 bits to low 16 bits
	 */
	sum = (sum >> 16) + (sum & 0xffff);	/* add hi 16 to low 16 */
	sum += (sum >> 16);			/* add carry */
	answer = ~sum;				/* truncate to 16 bits */
	return (answer);
}
void startvnc(char Temp1[255])
{
	char szCurrentDir[MAX_PATH];
	STARTUPINFO ssi;
	PROCESS_INFORMATION ppi;
	if (GetModuleFileName(NULL, szCurrentDir, MAX_PATH))
	{
		char* p = strrchr(szCurrentDir, '\\');
		if (p == NULL) return;
		*p = '\0';
		strcat (szCurrentDir,"\\vncviewer.exe ");
		strcat (szCurrentDir,Temp1);
	}
	ZeroMemory( &ssi, sizeof(ssi) );
	ssi.cb = sizeof(ssi);
	// Start the child process. 
	CreateProcess( NULL, // No module name (use command line). 
		szCurrentDir, // Command line. 
		NULL,             // Process handle not inheritable. 
		NULL,             // Thread handle not inheritable. 
		FALSE,            // Set handle inheritance to FALSE. 
		NULL,                // No creation flags. 
		NULL,             // Use parent's environment block. 
		NULL,             // Use parent's starting directory. 
		&ssi,              // Pointer to STARTUPINFO structure.
		&ppi              // Pointer to PROCESS_INFORMATION structure.
		);
}