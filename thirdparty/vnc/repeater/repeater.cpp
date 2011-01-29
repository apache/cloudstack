/////////////////////////////////////////////////////////////////////////////
//  Copyright (C) 2002 Ultr@VNC Team Members. All Rights Reserved.
//
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
//
/////////////////////////////////////////////////////////////////////////////


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <memory.h>
#include <errno.h>
#include <assert.h>
#include <sys/types.h>
#include <stdarg.h>
#include <fcntl.h>
#include <signal.h>

#include <windows.h>
#include <winsock.h>
#include <sys/stat.h>
#include <io.h>
#include <conio.h>
#include <tchar.h>

#define LOCAL_SOCKET	1
#define METHOD_DIRECT    1
#define socket_errno() WSAGetLastError()
#ifndef FD_ALLOC
#define FD_ALLOC(nfds) ((fd_set*)malloc((nfds+7)/8))
#endif
#define ECONNRESET WSAECONNRESET

#define rfbProtocolVersionFormat "RFB %03d.%03d\n"
#define rfbProtocolMajorVersion 0
#define rfbProtocolMinorVersion 0
#define sz_rfbProtocolVersionMsg 12
#define MAX_HOST_NAME_LEN 250
#define RFB_PORT_OFFSET 5900
typedef char rfbProtocolVersionMsg[13];	/* allow extra byte for null */
#define true TRUE
#define false FALSE

static int visible;
extern BOOL saved_enabled;
extern BOOL saved_enabled2;
extern BOOL saved_enabled3;
extern BOOL saved_enabled4;
extern int saved_portA;
extern int saved_portB;
extern char temp1[50][16];
extern char temp2[50][16];
extern char temp3[50][16];
extern int rule1;
extern int rule2;
extern int rule3;
extern int saved_portS;
extern BOOL saved_allow;
extern BOOL saved_refuse;
extern BOOL saved_refuse2;

#define MAX_LIST 20
#define MAX_IP 1000


typedef struct _mystruct
{
	SOCKET local_in;
	SOCKET local_out;
	SOCKET remote;
	ULONG	code;
	DWORD timestamp;
	BOOL used;
	BOOL waitingThread;
	BOOL server;
	int nummer;
	int size_buffer;
	char preloadbuffer[1028];
}mystruct,*pmystruct;

typedef struct _myipstruct
{
	DWORD timestamp;
	char	code[32];
	char ipaddress[32];

}myipstruct, *pmyipstrict;


char *dest_host = NULL;
u_short dest_port;

int ReadExact(int sock, char *buf, int len);
ULONG  Find_viewer_list(mystruct *Viewerstruct);

void win_log(char *line);

myipstruct ipadresses[MAX_IP];
mystruct Servers[MAX_LIST];
mystruct Viewers[MAX_LIST];

int   relay_method;
int f_debug=1;
BOOL notstopped;

u_short local_port;
u_short server_port;


void
LogStats(int code,long recv,long send)
{
char szFileName[MAX_PATH];
char tempchar[128];
HANDLE hFile=NULL;
FILE *f;
	if (GetModuleFileName(NULL, szFileName, MAX_PATH))
					{
						char* p = strrchr(szFileName, '\\');
						*p = '\0';
						strncat (szFileName,"\\",MAX_PATH);
						strncat (szFileName,_itoa(code,tempchar,10),MAX_PATH);
						strncat (szFileName,".txt",MAX_PATH);
					}

	if ((f = fopen((LPCSTR)szFileName, "a")) != NULL)
		{
			char	msg[100];
			char	buf[5];
			SYSTEMTIME	st; 
			GetLocalTime(&st);
			_itoa(st.wYear,buf,10);
			strcpy(msg,buf);
			strcat(msg,"/");
			_itoa(st.wMonth,buf,10);
			strcat(msg,buf);
			strcat(msg,"/");
			_itoa(st.wDay,buf,10);
			strcat(msg,buf);
			strcat(msg," ");
			_itoa(st.wHour,buf,10);
			strcat(msg,buf);
			strcat(msg,":");
			_itoa(st.wMinute,buf,10);
			strcat(msg,buf);
			strcat(msg,":");
			_itoa(st.wSecond,buf,10);
			strcat(msg,buf);
			strcat(msg," ");
			strcat(msg,"Transmitted: ");
			strcat(msg,ltoa((send+recv)/512,tempchar,10));
			strcat(msg,"k \n");
	
			fprintf(f,msg);
			fclose(f);
	}
}
void
LogStats2(int code)
{
char szFileName[MAX_PATH];
char tempchar[128];
    HANDLE hFile=NULL;
	FILE *f;
	if (GetModuleFileName(NULL, szFileName, MAX_PATH))
					{
						char* p = strrchr(szFileName, '\\');
						*p = '\0';
						strcat (szFileName,"\\");
						strcat (szFileName,itoa(code,tempchar,10));
						strcat (szFileName,".txt");
					}

	if ((f = fopen((LPCSTR)szFileName, "a")) != NULL)
		{
			char	msg[100];
			char	buf[5];
			SYSTEMTIME	st; 
			GetLocalTime(&st);
			_itoa(st.wYear,buf,10);
			strcpy(msg,buf);
			strcat(msg,"/");
			_itoa(st.wMonth,buf,10);
			strcat(msg,buf);
			strcat(msg,"/");
			_itoa(st.wDay,buf,10);
			strcat(msg,buf);
			strcat(msg," ");
			_itoa(st.wHour,buf,10);
			strcat(msg,buf);
			strcat(msg,":");
			_itoa(st.wMinute,buf,10);
			strcat(msg,buf);
			strcat(msg,":");
			_itoa(st.wSecond,buf,10);
			strcat(msg,buf);
			strcat(msg," ");
	
			fprintf(f,msg);
			fclose(f);
	}
}
void
debug( const char *fmt, ... )
{
	char myoutput[256];
	char myoutput2[256];
    va_list args;
	memset(myoutput2,0,256);
    if ( f_debug ) {
	va_start( args, fmt );
	sprintf(myoutput, "UltraVnc> ");
	vsprintf( myoutput, fmt, args );
	va_end( args );
    }
	strncpy(myoutput2,myoutput,strlen(myoutput)-1);
	win_log(myoutput2);
}

void
Clean_ipaddres_List()
{
	int i;
	for (i=0;i<MAX_IP;i++)
	{
		Servers[i].code=0;
		Servers[i].timestamp=false;
	}
}

void
Add_ipaddress_list(myipstruct *Ipstruct)
{
	int i;
	DWORD timestamp=0;
	int nummer=0;
	for (i=0;i<MAX_IP;i++)
	{
		if (strcmp(ipadresses[i].code,Ipstruct->code)==NULL)
		{
			debug("Ip adress added to list %i\n",Ipstruct->code);
			strncpy(ipadresses[i].code,Ipstruct->code,32);
			ipadresses[i].timestamp=GetTickCount();
			strncpy(ipadresses[i].ipaddress,Ipstruct->ipaddress,32);
			return;
		}
	}
	for (i=0;i<MAX_IP;i++)
	{
		if (ipadresses[i].timestamp<timestamp)
		{
			timestamp=ipadresses[i].timestamp;
			nummer=i;
		}
		if (strcmp(ipadresses[i].code,"")==NULL) 
		{
			debug("Ip adress added to list %s\n",Ipstruct->code);
			strncpy(ipadresses[i].code,Ipstruct->code,32);
			ipadresses[i].timestamp=GetTickCount();
			strncpy(ipadresses[i].ipaddress,Ipstruct->ipaddress,32);
			return;
		}
	}

	strncpy(ipadresses[nummer].code,Ipstruct->code,32);
	ipadresses[nummer].timestamp=GetTickCount();
	strncpy(ipadresses[nummer].ipaddress,Ipstruct->ipaddress,32);
}

void
Get_ipaddress_list(myipstruct *Ipstruct)
{
	int i;
	for (i=0;i<MAX_IP;i++)
	{
		if (strcmp(ipadresses[i].code,Ipstruct->code)==NULL)
		{
			debug("Ip adress found in list %i\n",Ipstruct->code);
			strncpy(ipadresses[i].code,Ipstruct->code,32);
			strncpy(Ipstruct->ipaddress,ipadresses[i].ipaddress, 32);
			return;
		}
	}
	strcpy(Ipstruct->ipaddress,"offline");

}


void
Clean_server_List()
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		Servers[i].code=0;
		Servers[i].used=false;
		Servers[i].waitingThread=false;
	}
}

void
Add_server_list(mystruct *Serverstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Servers[i].code==Serverstruct->code) return;
	}
	for (i=0;i<MAX_LIST;i++)
	{
		if (Servers[i].code==0) 
		{
			debug("Server added to list %i\n",Serverstruct->code);
			Servers[i].code=Serverstruct->code;
			Servers[i].local_in=Serverstruct->local_in;
			Servers[i].local_out=Serverstruct->local_out;
			Servers[i].remote=Serverstruct->remote;
			Servers[i].timestamp=GetTickCount();
			Servers[i].used=false;
			Servers[i].waitingThread=false;
			Servers[i].nummer=i;
			return;
		}
	}

}

void 
Remove_server_list(ULONG code)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Servers[i].code==code)
		{
			debug("Server Removed from list %i\n",code);
			Servers[i].code=0;
			Servers[i].used=false;
			Servers[i].waitingThread=false;
			return;
		}
	}
}

ULONG 
Find_server_list(mystruct *Serverstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Servers[i].code==Serverstruct->code)
		{
			Servers[i].used=true;
			Serverstruct->nummer=Servers[i].nummer;
			if (Servers[i].waitingThread==1) Sleep(1000);
			return i;
		}
	}
	return MAX_LIST+1;
}
ULONG 
Find_server_list_id(mystruct *Serverstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Servers[i].code==Serverstruct->code)
		{
			Serverstruct->waitingThread=Servers[i].waitingThread;
			Serverstruct->nummer=Servers[i].nummer;
			return i;
		}
	}
	return MAX_LIST+1;
}

void
Clean_viewer_List()
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		Viewers[i].code=0;
		Viewers[i].used=false;
		Viewers[i].waitingThread=false;
	}
}

void
Add_viewer_list(mystruct *Viewerstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Viewers[i].code==Viewerstruct->code) return;
	}
	for (i=0;i<MAX_LIST;i++)
	{
		if (Viewers[i].code==0) 
		{
			debug("Viewer added to list %i\n",Viewerstruct->code);
			Viewers[i].code=Viewerstruct->code;
			Viewers[i].local_in=Viewerstruct->local_in;
			Viewers[i].local_out=Viewerstruct->local_out;
			Viewers[i].remote=Viewerstruct->remote;
			Viewers[i].timestamp=GetTickCount();
			Viewers[i].used=false;
			Viewers[i].waitingThread=false;
			Viewers[i].nummer=i;
			return;
		}
	}

}

void 
Remove_viewer_list(ULONG code)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Viewers[i].code==code)
		{
			debug("Viewer removed from list %i\n",code);
			Viewers[i].code=0;
			Viewers[i].used=false;
			Viewers[i].waitingThread=false;
			return;
		}
	}
}

ULONG 
Find_viewer_list(mystruct *Viewerstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Viewers[i].code==Viewerstruct->code)
		{
			Viewers[i].used=true;
			Viewerstruct->nummer=Viewers[i].nummer;
			if (Viewers[i].waitingThread==1) Sleep(1000);
			return i;
		}
	}
	return MAX_LIST+1;
}

ULONG 
Find_viewer_list_id(mystruct *Viewerstruct)
{
	int i;
	for (i=0;i<MAX_LIST;i++)
	{
		if (Viewers[i].code==Viewerstruct->code)
		{
			Viewerstruct->waitingThread=Viewers[i].waitingThread;
			Viewerstruct->nummer=Viewers[i].nummer;
			return i;
		}
	}
	return MAX_LIST+1;
}


void
error( const char *fmt, ... )
{
    va_list args;
    va_start( args, fmt );
    fprintf(stderr, "ERROR: ");
    vfprintf( stderr, fmt, args );
    va_end( args );
}

int
local_resolve (const char *host, struct sockaddr_in *addr,
	       struct sockaddr_in *ns)
{
    struct hostent *ent;
    if ( strspn(host, "0123456789.") == strlen(host) ) {
	/* given by IPv4 address */
	addr->sin_family = AF_INET;
	addr->sin_addr.s_addr = inet_addr(host);
    } else {
	debug("resolving host by name: %s\n", host);
	ent = gethostbyname (host);
	if ( ent ) {
	    memcpy (&addr->sin_addr, ent->h_addr, ent->h_length);
	    addr->sin_family = ent->h_addrtype;
	    debug("resolved: %s (%s)\n",
		  host, inet_ntoa(addr->sin_addr));
	} else {
	    debug("failed to resolve locally.\n");
	    return -1;				/* failed */
	}
    }
    return 0;					/* good */
}

SOCKET
open_connection( const char *host, u_short port )
{
	char remote_host[1024];
	BOOL found;
	int i;
    SOCKET s;
    struct sockaddr_in saddr;

    if ( relay_method == METHOD_DIRECT ) {
	host = dest_host;
	port = dest_port;
    }
	if (saved_portS != 0)
	{
		if (saved_portS != port) return SOCKET_ERROR;
	}
    if (local_resolve (host, &saddr, NULL) < 0) {
	error("can't resolve hostname: %s\n", host);
	return SOCKET_ERROR;
    }
    saddr.sin_port = htons(port);

    debug("connecting to %s:%u\n", inet_ntoa(saddr.sin_addr), port);
	strcpy(remote_host,inet_ntoa(saddr.sin_addr));
	found=FALSE;
	if (!saved_allow) found=TRUE;
	else
	{
		for (i=0;i<rule1;i++)
		{
		if (strstr(temp1[i],remote_host) != NULL)
		found=TRUE;
		}
	}
	if (saved_refuse)
	{
		for (i=0;i<rule2;i++)
		{
		if (strstr(temp2[i],inet_ntoa(saddr.sin_addr)) != NULL)
		found=FALSE;
		}
	}
	if (!found) return SOCKET_ERROR;
    s = socket( AF_INET, SOCK_STREAM, 0 );
    if ( connect( s, (struct sockaddr *)&saddr, sizeof(saddr))
	 == SOCKET_ERROR) {
	debug( "connect() failed.\n");
	return SOCKET_ERROR;
    }
    return s;
}

void
fatal( const char *fmt, ... )
{
    va_list args;
    va_start( args, fmt );
    fprintf(stderr, "FATAL: ");
    vfprintf( stderr, fmt, args );
    va_end( args );
    //exit (EXIT_FAILURE);
}

void
report_bytes( char *prefix, char *buf, int len )
{
    if ( ! f_debug )
	return;
    debug( "%s", prefix );
    while ( 0 < len ) {
	fprintf( stderr, " %02x", *(unsigned char *)buf);
	buf++;
	len--;
    }
    fprintf(stderr, "\n");
    return;
}

int
fddatalen( SOCKET fd )
{
    DWORD len = 0;
    struct stat st;
    fstat( 0, &st );
    if ( st.st_mode & _S_IFIFO ) { 
	/* in case of PIPE */
	if ( !PeekNamedPipe( GetStdHandle(STD_INPUT_HANDLE),
			     NULL, 0, NULL, &len, NULL) ) {
	    if ( GetLastError() == ERROR_BROKEN_PIPE ) {
		/* PIPE source is closed */
		/* read() will detects EOF */
		len = 1;
	    } else {
		fatal("PeekNamedPipe() failed, errno=%d\n",
		      GetLastError());
	    }
	}
    } else if ( st.st_mode & _S_IFREG ) {
	/* in case of regular file (redirected) */
	len = 1;			/* always data ready */
    } else if ( _kbhit() ) {
	/* in case of console */
	len = 1;
    }
    return len;
}

DWORD WINAPI do_repeater_wait(LPVOID lpParam)
{
    /** vars for remote input data **/
	int a;
    //char rbuf[1025];				/* remote input buffer */
	char *rbuf;
    int rbuf_len;				/* available data in rbuf */
    int f_remote;				/* read remote input more? */
    /** other variables **/
    int len;
	
	SOCKET local_in=0;
	SOCKET local_out=0;
	SOCKET remote=0;
	ULONG code=0;
	long recvbytes=0;
	long sendbytes=0;
	BOOL server;
	int nummer;
	pmystruct inout=(pmystruct)lpParam;
	remote=inout->remote;
	code=inout->code;
	server=inout->server;
	nummer=inout->nummer;
	LogStats2(code);
	if (server) 
	{
		rbuf=Servers[nummer].preloadbuffer;
		Servers[nummer].waitingThread=1;
	}
	else
	{
		rbuf=Viewers[nummer].preloadbuffer;
		Viewers[nummer].waitingThread=1;
	}
    
    
    f_remote = 1;				/* yes, read from remote */
    rbuf_len = 0;

    while ( f_remote ) {
	FD_SET ifds;
	struct timeval tmo;
	FD_ZERO( &ifds );
	tmo.tv_sec=1;
	tmo.tv_usec=0;

	if (server)
	{
		if (Servers[nummer].used)
		{
			Servers[nummer].waitingThread=0;
			return 0;
		}
	}
	else
	{
		if (Viewers[nummer].used)
		{
			Viewers[nummer].waitingThread=0;
			return 0;
		}
	}
	
	

	FD_SET( remote, &ifds );
	a=select( 0, &ifds, NULL, NULL, &tmo );
	if ( a == 0 ) {
	    /* some error */
	    //debug( "select() 0 \n");
	}
	if ( a == -1 ) {
	    /* some error */
	    debug( "select() failed, %d\n", socket_errno());
		goto error;
	}
	if ( a == 1)
	{
		if (rbuf_len >1024) goto error;
		if ( FD_ISSET(remote, &ifds) && (rbuf_len < 1025) ) 
		{
			len = recv( remote, rbuf + rbuf_len, 1024-rbuf_len, 0);
			debug( "recv %d \n",len);
			if ( len == 0 ) {
			debug("connection closed by peer\n");
			goto error;

			} else if ( len == -1 ) {
			if (socket_errno() != ECONNRESET) {
				
				fatal("recv() faield, %d\n", socket_errno());
				goto error;
			} else {
				debug("ECONNRESET detected\n");
				goto error;
			}
			} else {
			recvbytes +=len;
			if ( 1 < f_debug )		
				report_bytes( "<<<", rbuf, rbuf_len);
			rbuf_len += len;
			if (server) 
				{
					Servers[nummer].size_buffer=rbuf_len;
				}
				else
				{
					Viewers[nummer].size_buffer=rbuf_len;
				}
			}
		}
	}
	
	
	}
error:
	LogStats(code,recvbytes,sendbytes);
	f_remote = 0;			/* no more read from socket */
	closesocket(remote);
	shutdown(remote, 1);
	local_in=0;
	remote=0;
    if (server)
	{
		Servers[nummer].waitingThread=0;
		Remove_server_list(code);
	}
	else
	{
		Viewers[nummer].waitingThread=0;
		Remove_viewer_list(code);
	}
    return 0;
}

DWORD WINAPI do_repeater(LPVOID lpParam)
{
    /** vars for local input data **/
    char lbuf[1024];				/* local input buffer */
    int lbuf_len;				/* available data in lbuf */
    int f_local;				/* read local input more? */
    /** vars for remote input data **/
    char rbuf[1024];				/* remote input buffer */
    int rbuf_len;				/* available data in rbuf */
    int f_remote;				/* read remote input more? */
    /** other variables **/
    int nfds, len;
    fd_set *ifds, *ofds;
    struct timeval *tmo;
//    struct timeval win32_tmo;
	
	SOCKET local_in=0;
	SOCKET local_out=0;
	SOCKET remote=0;
	ULONG code=0;
	long recvbytes=0;
	long sendbytes=0;
	int nummer;
	pmystruct inout=(pmystruct)lpParam;
    local_in=inout->local_in;
	local_out=inout->local_out;
	remote=inout->remote;
	code=inout->code;
	nummer=inout->nummer;
	LogStats2(code);
	lbuf_len = 0;
    rbuf_len = 0;
	if (inout->server) 
	{
		memcpy(lbuf,Viewers[nummer].preloadbuffer,Viewers[nummer].size_buffer);
		lbuf_len=Viewers[nummer].size_buffer;
		Viewers[nummer].size_buffer=0;
	}
	else
	{
		memcpy(lbuf,Servers[nummer].preloadbuffer,Servers[nummer].size_buffer);
		lbuf_len=Servers[nummer].size_buffer;
		Servers[nummer].size_buffer=0;
	}
	if ( 0 < lbuf_len ) {
	    if (inout->server) len = send(remote, lbuf, lbuf_len, 0);
		else len = send(local_in, lbuf, lbuf_len, 0);
	    if ( 1 < f_debug )		/* more verbose */
		report_bytes( ">>>", lbuf, lbuf_len);
	    if ( len == -1 ) {
		debug("send() failed, %d\n", socket_errno());
		goto error;
	    } else if ( 0 < len ) {
		/* move data on to top of buffer */
		sendbytes+=len;
		lbuf_len -= len;
		if ( 0 < lbuf_len )
		    memcpy( lbuf, lbuf+len, lbuf_len );
		assert( 0 <= lbuf_len );
	    }
	}
	

	LogStats2(code);
    /* repeater between stdin/out and socket  */
    nfds = ((local_in<remote)? remote: local_in) +1;
    ifds = FD_ALLOC(nfds);
    ofds = FD_ALLOC(nfds);
    f_local = 1;				/* yes, read from local */
    f_remote = 1;				/* yes, read from remote */

    while ( f_local || f_remote ) {
	FD_ZERO( ifds );
	FD_ZERO( ofds );
	tmo = NULL;

	/** prepare for reading local input **/
	if ( f_local && (lbuf_len < sizeof(lbuf)) ) {
	    FD_SET( local_in, ifds );
	}
	
	/** prepare for reading remote input **/
	if ( f_remote && (rbuf_len < sizeof(rbuf)) ) {
	    FD_SET( remote, ifds );
	}
	
	/* FD_SET( local_out, ofds ); */
	/* FD_SET( remote, ofds ); */
	
	if ( select( nfds, ifds, ofds, NULL, tmo ) == -1 ) {
	    /* some error */
	    error( "select() failed, %d\n", socket_errno());
		goto error;
	}
	/* fake ifds if local is stdio handle because
           select() of Winsock does not accept stdio
           handle. */

	/* remote => local */
	if ( FD_ISSET(remote, ifds) && (rbuf_len < sizeof(rbuf)) ) {
	    len = recv( remote, rbuf + rbuf_len, sizeof(rbuf)-rbuf_len, 0);
	    if ( len == 0 ) {
		debug("connection closed by peer\n");
		goto error;

	    } else if ( len == -1 ) {
		if (socket_errno() != ECONNRESET) {
		    /* error */
		    fatal("recv() faield, %d\n", socket_errno());
			goto error;
		} else {
		    debug("ECONNRESET detected\n");
			goto error;
		}
	    } else {
		recvbytes +=len;
		if ( 1 < f_debug )		/* more verbose */
		    report_bytes( "<<<", rbuf, rbuf_len);
		rbuf_len += len;
	    }
	}
	
	/* local => remote */
	if ( FD_ISSET(local_in, ifds) && (lbuf_len < sizeof(lbuf)) ) {

		len = recv(local_in, lbuf + lbuf_len,sizeof(lbuf)-lbuf_len, 0);

	    if ( len == 0 ) {
		/* stdin is EOF */
		debug("local input is EOF\n");
		goto error;
	    } else if ( len == -1 ) {
		/* error on reading from stdin */
			goto error;
	    } else {
		/* repeat */
		lbuf_len += len;
	    }
	}
	
	/* flush data in buffer to socket */
	if ( 0 < lbuf_len ) {
	    len = send(remote, lbuf, lbuf_len, 0);
	    if ( 1 < f_debug )		/* more verbose */
		report_bytes( ">>>", lbuf, lbuf_len);
	    if ( len == -1 ) {
		debug("send() failed, %d\n", socket_errno());
		goto error;
	    } else if ( 0 < len ) {
		/* move data on to top of buffer */
		sendbytes+=len;
		lbuf_len -= len;
		if ( 0 < lbuf_len )
		    memcpy( lbuf, lbuf+len, lbuf_len );
		assert( 0 <= lbuf_len );
	    }
	}
	
	/* flush data in buffer to local output */
	if ( 0 < rbuf_len ) {

		len = send( local_out, rbuf, rbuf_len, 0);
	    if ( len == -1 ) {
		debug("output (local) failed, errno=%d\n", errno);
		goto error;
	    } 
		else
		{
	    rbuf_len -= len;
	    if ( len < rbuf_len )
		memcpy( rbuf, rbuf+len, rbuf_len );
	    assert( 0 <= rbuf_len );
		}
	}

    }
error:
	LogStats(code,recvbytes,sendbytes);
	f_remote = 0;			/* no more read from socket */
	f_local = 0;
	closesocket(local_in);
	closesocket(remote);
	shutdown(local_in, 1);
	shutdown(remote, 1);
	local_in=0;
	remote=0;
    Remove_server_list(code);
	Remove_viewer_list(code);
    return 0;
}

BOOL ParseDisplay(LPTSTR display, LPTSTR phost, int hostlen, int *pport) 
{
	int tmp_port;
	TCHAR *colonpos = _tcschr(display, L':');
    if (hostlen < (int)_tcslen(display))
        return FALSE;

    /*if (colonpos == NULL)
	{
		return FALSE;
	}
	else
	{
		_tcsncpy(phost, display, colonpos - display);
		phost[colonpos - display] = L'\0';

		{
			// One colon -- interpret as a display number or port number
			if (_stscanf(colonpos + 1, TEXT("%d"), &tmp_port) != 1) 
				return FALSE;
		}
	}*/
	if (colonpos == NULL)
	{
		// No colon -- use default port number
        tmp_port = RFB_PORT_OFFSET;
		_tcsncpy(phost, display, MAX_HOST_NAME_LEN);
	}
	else
	{
		_tcsncpy(phost, display, colonpos - display);
		phost[colonpos - display] = L'\0';
		if (colonpos[1] == L':') {
			// Two colons -- interpret as a port number
			if (_stscanf(colonpos + 2, TEXT("%d"), &tmp_port) != 1) 
				return FALSE;
		}
		else
		{
			// One colon -- interpret as a display number or port number
			if (_stscanf(colonpos + 1, TEXT("%d"), &tmp_port) != 1) 
				return FALSE;

			// RealVNC method - If port < 100 interpret as display number else as Port number
			if (tmp_port < 100)
				tmp_port += RFB_PORT_OFFSET;
		}
	}

    *pport = tmp_port;
    return TRUE;
}

BOOL ParseDisplay2(LPTSTR display, LPTSTR phost, int hostlen, char pport[32]) 
{
	//char tmp_port[512];
	int tmp_port;
	TCHAR *colonpos = _tcschr(display, L':');
    if (hostlen < (int)_tcslen(display))
        return FALSE;

    /*if (colonpos == NULL)
	{
		return FALSE;
	}
	else
	{
		_tcsncpy(phost, display, colonpos - display);
		phost[colonpos - display] = L'\0';

		{
			// One colon -- interpret as a display number or port number
			if (_stscanf(colonpos + 1, TEXT("%s"), tmp_port) != 1) 
				return FALSE;
		}
	}*/
	if (colonpos == NULL)
	{
		// No colon -- use default port number
        tmp_port = RFB_PORT_OFFSET;
		_tcsncpy(phost, display, MAX_HOST_NAME_LEN);
	}
	else
	{
		_tcsncpy(phost, display, colonpos - display);
		phost[colonpos - display] = L'\0';
		if (colonpos[1] == L':') {
			// Two colons -- interpret as a port number
			if (_stscanf(colonpos + 2, TEXT("%d"), &tmp_port) != 1) 
				return FALSE;
		}
		else
		{
			// One colon -- interpret as a display number or port number
			if (_stscanf(colonpos + 1, TEXT("%d"), &tmp_port) != 1) 
				return FALSE;

			// RealVNC method - If port < 100 interpret as display number else as Port number
			if (tmp_port < 100)
				tmp_port += RFB_PORT_OFFSET;
		}
	}
    strncpy(pport,itoa(tmp_port,pport,10),32);
    return TRUE;
}

int
WriteExact(int sock, char *buf, int len)
{
    int n;
	
    while (len > 0) {
	n = send(sock, buf, len,0);
		
	if (n > 0) {
	    buf += n;
	    len -= n;
	} else if (n == 0) {
      	    fprintf(stderr,"WriteExact: write returned 0?\n");
	    exit(1);
        } else {
            return n;
	}
    }
    return 1;
}

int ReadExact(int sock, char *buf, int len)
{
    int n;
	Sleep(500);
    while (len > 0) {
	n = recv(sock, buf, len, 0);
	if (n > 0) {
	    buf += n;
	    len -= n;
		if (len!=0) return -1;
        } else {
            return n;
	}
    }
    return 1;
}


DWORD WINAPI server_listen_ip(LPVOID lpParam)
{   
    int sock;
    int connection;
    struct sockaddr_in name;
    struct sockaddr_in client;
    int socklen;
	int i;
	BOOL found2;
	DWORD dwThreadId;
	TCHAR proxyadress[256];
	TCHAR remotehost[256];
	char remoteport[32];
	mystruct teststruct;



	////////////
    sock = socket (PF_INET, SOCK_STREAM, 0);
    if (sock < 0) fatal("socket() failed, errno=%d\n", socket_errno());
	else debug("socket() initialized\n");
    name.sin_family = AF_INET;
    name.sin_port = htons (5912);
    name.sin_addr.s_addr = htonl (INADDR_ANY);
    if (bind (sock, (struct sockaddr *) &name, sizeof (name)) < 0)
			fatal ("bind() failed, errno=%d\n", socket_errno());
	else debug ("bind() succeded to port %i\n",5912);
    if (listen( sock, 1) < 0)
		fatal ("listen() failed, errno=%d\n", socket_errno());
	else debug ("listen() succeded\n");
    socklen = sizeof(client);
	while(notstopped)
	{
//		debug ("Waiting for connection ...\n");
		connection = accept( sock, (struct sockaddr *)&client, &socklen);
		if ( connection < 0 )
		{
			debug ("accept() failed, errno=%d\n", socket_errno());
			debug ("Check if port is not already in use port=%i ",5912);
			Sleep(5000);
		}
		else
		{
			if (ReadExact(connection, proxyadress, MAX_HOST_NAME_LEN)<0){
				debug("dynamic ip adress failed");
				closesocket(connection);
				goto end;
				}

			if (!ParseDisplay2(proxyadress, remotehost, 255, remoteport)) 
			{
				shutdown(sock, 2);
				closesocket(connection);
				continue;
			}
			strcpy(dest_host,remotehost);
			shutdown(sock, 2);


			if (strcmp(dest_host,"IPS")==0)
			{
				myipstruct Ipstruct;
				strncpy(Ipstruct.code,remoteport,32);
				sprintf(Ipstruct.ipaddress,"%d.%d.%d.%d",client.sin_addr.S_un.S_un_b.s_b1,client.sin_addr.S_un.S_un_b.s_b2, client.sin_addr.S_un.S_un_b.s_b3,client.sin_addr.S_un.S_un_b.s_b4);
				Add_ipaddress_list(&Ipstruct);
				closesocket(connection);
				
			}
			if (strcmp(dest_host,"IPG")==0)
			{
				myipstruct Ipstruct;
				strncpy(Ipstruct.code,remoteport,32);
				Get_ipaddress_list(&Ipstruct);
				send(connection, Ipstruct.ipaddress, 32,0);
				closesocket(connection);
			}
			



		}
end:;
   
    }
	notstopped=false;
    debug ("relaying done.\n");
    WSACleanup();
    return 0;
}




DWORD WINAPI server_listen(LPVOID lpParam)
{
//    int ret;
//    SOCKET  remote;				/* socket */
    SOCKET  local_in;				/* Local input */
    SOCKET  local_out;				/* Local output */
    
    int sock;
    int connection;
    struct sockaddr_in name;
    struct sockaddr client;
    int socklen;
	int i;
	BOOL found2;
	DWORD dwThreadId;
	TCHAR proxyadress[256];
	TCHAR remotehost[256];
	int remoteport;
	mystruct teststruct;



	////////////
    sock = socket (PF_INET, SOCK_STREAM, 0);
    if (sock < 0) fatal("socket() failed, errno=%d\n", socket_errno());
	else debug("socket() initialized\n");
    name.sin_family = AF_INET;
    name.sin_port = htons (server_port);
    name.sin_addr.s_addr = htonl (INADDR_ANY);
    if (bind (sock, (struct sockaddr *) &name, sizeof (name)) < 0)
			fatal ("bind() failed, errno=%d\n", socket_errno());
	else debug ("bind() succeded to port %i\n",server_port);
    if (listen( sock, 1) < 0)
		fatal ("listen() failed, errno=%d\n", socket_errno());
	else debug ("listen() succeded\n");
    socklen = sizeof(client);
	while(notstopped)
	{
//		debug ("Waiting for connection ...\n");
		connection = accept( sock, &client, &socklen);
		if ( connection < 0 )
		{
			debug ("accept() failed, errno=%d\n", socket_errno());
			debug ("Check if port is not already in use port=%i ",server_port);
			Sleep(5000);
		}
		else
		{
			if (ReadExact(connection, proxyadress, MAX_HOST_NAME_LEN)<0){
				debug("Reading Proxy settings error");
				closesocket(connection);
				goto end;
				}
				int i = 0;  // disable
				setsockopt (connection, SOL_SOCKET, SO_RCVTIMEO, (char*) &i, sizeof(i));
				setsockopt (connection, SOL_SOCKET, SO_SNDTIMEO, (char*) &i, sizeof(i));
			if (!ParseDisplay(proxyadress, remotehost, 255, &remoteport))
			{
				shutdown(sock, 2);
				closesocket(connection);
				continue;
			}

			strcpy(dest_host,remotehost);
			dest_port=remoteport;
			shutdown(sock, 2);
			local_in = local_out=connection;
			if (strcmp(dest_host,"ID")==0)
			{
				found2=TRUE;
				if (saved_refuse2)
				{
					found2=FALSE;
					for (i=0;i<rule3;i++)
						{
							if (atoi(temp3[i])==remoteport) found2=true;

						}
				}
				if (!found2) 
					{
					debug("ID refused %i \n",remoteport);
				    closesocket(connection);
					}
				if (found2)
				{
					teststruct.remote=connection;
					teststruct.code=remoteport;
					if (Find_viewer_list(&teststruct)!=MAX_LIST+1)
					{
						debug("ID viewer found %i \n",remoteport);
						//found
						Add_server_list(&teststruct);
						//initiate connection
						Find_server_list(&teststruct);
						teststruct.local_in=Viewers[Find_viewer_list(&teststruct)].local_in;
						teststruct.local_out=Viewers[Find_viewer_list(&teststruct)].local_out;
						teststruct.remote=connection;
						teststruct.nummer=Find_viewer_list(&teststruct);
						teststruct.server=1;
						CreateThread(NULL,0,do_repeater,(LPVOID)&teststruct,0,&dwThreadId);
					}
					else
						{
							//debug("ID viewer not connected %i \n",remoteport);
							//closesocket(connection);
							
							Add_server_list(&teststruct);
							if (Find_server_list_id(&teststruct)!=MAX_LIST+1)
							{
								teststruct.server=1;
								if (teststruct.waitingThread==0)CreateThread(NULL,0,do_repeater_wait,(LPVOID)&teststruct,0,&dwThreadId);
							}
						}
				}
			}
		}
end:;
   
    }
	notstopped=false;
    debug ("relaying done.\n");
    closesocket(local_out);

	closesocket(local_in);
    WSACleanup();
    return 0;
}

DWORD WINAPI server_listen2(LPVOID lpParam)
{
//    int ret;
//    SOCKET  remote;				/* socket */
    SOCKET  local_in;				/* Local input */
    SOCKET  local_out;				/* Local output */
    
    int sock;
    int connection;
    struct sockaddr_in name;
    struct sockaddr client;
    int socklen;
	int i;
	BOOL found2;
	DWORD dwThreadId;
	TCHAR proxyadress[256];
	TCHAR remotehost[256];
	int remoteport;
	mystruct teststruct;



	////////////
    sock = socket (PF_INET, SOCK_STREAM, 0);
    if (sock < 0) fatal("socket() failed, errno=%d\n", socket_errno());
	else debug("socket() initialized\n");
    name.sin_family = AF_INET;
    name.sin_port = htons (443);
    name.sin_addr.s_addr = htonl (INADDR_ANY);
    if (bind (sock, (struct sockaddr *) &name, sizeof (name)) < 0)
			fatal ("bind() failed, errno=%d\n", socket_errno());
	else debug ("bind() succeded to port %i\n",443);
    if (listen( sock, 1) < 0)
		fatal ("listen() failed, errno=%d\n", socket_errno());
	else debug ("listen() succeded\n");
    socklen = sizeof(client);
	while(notstopped)
	{
//		debug ("Waiting for connection ...\n");
		connection = accept( sock, &client, &socklen);
		if ( connection < 0 )
		{
			debug ("accept() failed, errno=%d\n", socket_errno());
			debug ("Check if port is not already in use port=%i ",443);
			Sleep(5000);
		}
		else
		{
			if (ReadExact(connection, proxyadress, MAX_HOST_NAME_LEN)<0){
				debug("Reading Proxy settings error, 443 is also https, webbrowser access try");
				closesocket(connection);
				goto end;
				}
			if (!ParseDisplay(proxyadress, remotehost, 255, &remoteport))
				{
				shutdown(sock, 2);
				closesocket(connection);
				continue;
				}
			strcpy(dest_host,remotehost);
			dest_port=remoteport;
			shutdown(sock, 2);
			local_in = local_out=connection;
			if (strcmp(dest_host,"IDS")==0)
			{
				found2=TRUE;
				if (saved_refuse2)
				{
					found2=FALSE;
					for (i=0;i<rule3;i++)
						{
							if (atoi(temp3[i])==remoteport) found2=true;

						}
				}
				if (!found2) 
					{
					debug("ID refused %i \n",remoteport);
				    closesocket(connection);
					}
				if (found2)
				{
					teststruct.remote=connection;
					teststruct.code=remoteport;
					Add_server_list(&teststruct);
					if (Find_viewer_list(&teststruct)!=MAX_LIST+1)
					{
						//found
						//initiate connection
						Find_server_list(&teststruct);
						teststruct.local_in=Viewers[Find_viewer_list(&teststruct)].local_in;
						teststruct.local_out=Viewers[Find_viewer_list(&teststruct)].local_out;
						teststruct.remote=connection;
						teststruct.nummer=Find_viewer_list(&teststruct);
						teststruct.server=1;
						CreateThread(NULL,0,do_repeater,(LPVOID)&teststruct,0,&dwThreadId);
					}
					else
						{
							if (Find_server_list_id(&teststruct)!=MAX_LIST+1)
							{
								teststruct.server=1;
								if (teststruct.waitingThread==0)CreateThread(NULL,0,do_repeater_wait,(LPVOID)&teststruct,0,&dwThreadId);
							}
						}
				}
			}
			if (strcmp(dest_host,"IDV")==0)
				{
					found2=TRUE;
					if (saved_refuse2)
					{
						found2=FALSE;
						for (i=0;i<rule3;i++)
							{
								if (atoi(temp3[i])==remoteport) found2=true;

							}
					}
					if (!found2)
					{
						closesocket(connection);
						debug("ID refused %i \n",remoteport);
					}
					if (found2)
					{
						teststruct.local_in=local_in;
						teststruct.local_out=local_out;
						teststruct.code=remoteport;
						Add_viewer_list(&teststruct);
					

						if (Find_server_list(&teststruct)!=MAX_LIST+1)
						{
							//found
							//initiate connection
							Find_viewer_list(&teststruct);
							teststruct.local_in=local_in;
							teststruct.local_out=local_out;
							teststruct.remote=Servers[Find_server_list(&teststruct)].remote;
							teststruct.server=0;
							teststruct.nummer=Find_server_list(&teststruct);
							CreateThread(NULL,0,do_repeater,(LPVOID)&teststruct,0,&dwThreadId);
						}
						else
						{
							if (Find_viewer_list_id(&teststruct)!=MAX_LIST+1)
							{
								teststruct.server=0;
								teststruct.remote=local_in;
								if (teststruct.waitingThread==0)CreateThread(NULL,0,do_repeater_wait,(LPVOID)&teststruct,0,&dwThreadId);
							}
						}
					}
				}
		}
end:;
   
    }
	notstopped=false;
    debug ("relaying done.\n");
    closesocket(local_out);

	closesocket(local_in);
    WSACleanup();
    return 0;
}

DWORD WINAPI timer(LPVOID lpParam)
{
	while(notstopped)
	{
		int i;
		DWORD tick=GetTickCount();
		for (i=0;i<MAX_LIST;i++)
			{
				if (tick-Viewers[i].timestamp>300000 && Viewers[i].used==false && Viewers[i].code!=0)
				{
					//
					closesocket(Viewers[i].local_in);
					debug("Remove viewer %i %i \n",Viewers[i].code,i);
					Remove_viewer_list(Viewers[i].code);
				}
				if (tick-Servers[i].timestamp>300000 && Servers[i].used==false && Servers[i].code!=0)
				{
					//
					closesocket(Servers[i].remote);
					debug("Remove server %i\n",Servers[i].code);
					Remove_server_list(Servers[i].code);
				}
			}
//		debug("Searching old connections\n");
		Sleep(60000);

	}
	return 0;
}

int
main_test()// int argc, char **argv )
{
//    int ret;
    SOCKET  remote;				/* socket */
    SOCKET  local_in;				/* Local input */
    SOCKET  local_out;				/* Local output */
    WSADATA wsadata;
	TCHAR proxyadress[256];
	TCHAR remotehost[256];
	int remoteport;
    int sock;
    int connection;
    struct sockaddr_in name;
    struct sockaddr client;
    int socklen;
	int i;
	BOOL found2;
	DWORD dwThreadId;
	mystruct teststruct;
	rfbProtocolVersionMsg pv;
	notstopped=true;

    WSAStartup( 0x101, &wsadata);
	relay_method = METHOD_DIRECT;
	Clean_server_List();
	Clean_viewer_List();
	////////////
	local_port=saved_portA;
	server_port=saved_portB;
/*	if (argc==2)
		local_port=atoi(argv[1]);
	if (argc==3)
		server_port=atoi(argv[2]);*/
	dest_host =(char *)malloc(256);
	////////////
	debug("Copyright (C) 2005 Ultr@VNC Team Members. All Rights Reserved.\n");
	debug("\n");
	debug("\n");
	debug("The Repeater is free software; you can redistribute it and/or modify\n");
	debug("it under the terms of the GNU General Public License as published by\n");
	debug("the Free Software Foundation; either version 2 of the License, or\n");
	debug("(at your option) any later version.\n");
	debug(" \n");
	debug("This program is distributed in the hope that it will be useful,\n");
	debug("but WITHOUT ANY WARRANTY; without even the implied warranty of\n");
	debug("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n");
	debug("GNU General Public License for more details.\n");
	debug(" \n");
	debug("You should have received a copy of the GNU General Public License\n");
	debug("along with this program; if not, write to the Free Software\n");
	debug("Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,\n");
	debug("USA.\n");
	debug(" \n");
	debug(" Repeater for Rel1.0.0 of PcHelpware\n");

    sock = socket (PF_INET, SOCK_STREAM, 0);
    if (sock < 0) fatal("socket() failed, errno=%d\n", socket_errno());
	else debug("socket() initialized\n");
	

    name.sin_family = AF_INET;
    name.sin_port = htons (local_port);
    name.sin_addr.s_addr = htonl (INADDR_ANY);
    if (bind (sock, (struct sockaddr *) &name, sizeof (name)) < 0)
			fatal ("bind() failed, errno=%d\n", socket_errno());
	else debug ("bind() succeded to port %i\n",local_port);
    if (listen( sock, 1) < 0)
		fatal ("listen() failed, errno=%d\n", socket_errno());
	else debug ("listen() succeded\n");
    socklen = sizeof(client);
	if (saved_enabled) CreateThread(NULL,0,server_listen,(LPVOID)&teststruct,0,&dwThreadId);
	if (saved_enabled3)CreateThread(NULL,0,server_listen2,(LPVOID)&teststruct,0,&dwThreadId);
	if (saved_enabled4)CreateThread(NULL,0,server_listen_ip,(LPVOID)&teststruct,0,&dwThreadId);
	CreateThread(NULL,0,timer,(LPVOID)&teststruct,0,&dwThreadId);
	while(notstopped)
	{
//		debug ("Waiting for Viewer connection ...\n");
		connection = accept( sock, &client, &socklen);
		if ( connection < 0 )
		{
			debug ("accept() failed, errno=%d\n", socket_errno());
			debug ("Check if port is not already in use port=%i ",local_port);
			Sleep(5000);
		}
		else
		{
			debug ("accept() connection \n");
			sprintf(pv,rfbProtocolVersionFormat,rfbProtocolMajorVersion,rfbProtocolMinorVersion);
			if (WriteExact(connection, pv, sz_rfbProtocolVersionMsg) < 0) {
				debug("Writing protocol version error");
				closesocket(connection);
				goto end;
				}
			if (ReadExact(connection, proxyadress, MAX_HOST_NAME_LEN)<0){
				debug("Reading Proxy settings error");
				closesocket(connection);
				goto end;
				}
			if (!ParseDisplay(proxyadress, remotehost, 255, &remoteport))
				{
				debug("ParseDisplay failed");
				shutdown(sock, 2);
				closesocket(connection);
				continue;
				}
			strcpy(dest_host,remotehost);
			dest_port=remoteport;
			//debug ("Server %s port %d \n", dest_host,remoteport);
			shutdown(sock, 2);
			local_in = local_out=connection;
			if (strcmp(dest_host,"ID")==0)
			{
				if (strcmp(dest_host,"ID")==0)
				{
					found2=TRUE;
					if (saved_refuse2)
					{
						found2=FALSE;
						for (i=0;i<rule3;i++)
							{
								if (atoi(temp3[i])==remoteport) found2=true;

							}
					}
					if (!found2)
					{
						closesocket(connection);
						debug("ID refused %i \n",remoteport);
					}
					if (found2)
					{
						teststruct.local_in=local_in;
						teststruct.local_out=local_out;
						teststruct.code=remoteport;
						Add_viewer_list(&teststruct);
						debug("ID added %i \n",remoteport);
					

						if (Find_server_list(&teststruct)!=MAX_LIST+1)
						{
							//found
							//initiate connection
							Find_viewer_list(&teststruct);
							teststruct.local_in=local_in;
							teststruct.local_out=local_out;
							teststruct.remote=Servers[Find_server_list(&teststruct)].remote;
							teststruct.server=0;
							teststruct.nummer=Find_server_list(&teststruct);
							CreateThread(NULL,0,do_repeater,(LPVOID)&teststruct,0,&dwThreadId);
						}
						else
						{
							if (Find_viewer_list_id(&teststruct)!=MAX_LIST+1)
							{
								teststruct.server=0;
								teststruct.remote=local_in;
								if (teststruct.waitingThread==0)CreateThread(NULL,0,do_repeater_wait,(LPVOID)&teststruct,0,&dwThreadId);
							}
						}
					}
				}

				//CreateThread(NULL,0,do_repeater_listen,(LPVOID)&teststruct,0,&dwThreadId);
			}
			else if (saved_enabled2)
			{
			remote = open_connection (dest_host, dest_port);
				if ( remote == SOCKET_ERROR )
				{
					debug( "Unable to connect to destination host, errno=%d\n",socket_errno());
					closesocket(local_in);
					goto end;
				}
			debug("connected\n");
			debug ("start relaying.\n");
			teststruct.local_in=local_in;
			teststruct.local_out=local_out;
			teststruct.remote=remote;
			CreateThread(NULL,0,do_repeater,(LPVOID)&teststruct,0,&dwThreadId);
			}
				//do_repeater(local_in, local_out, remote);
		}
end:;
   
    }
	notstopped=false;
    debug ("relaying done.\n");
    closesocket(remote);

	closesocket(local_in);
    WSACleanup();
    return 0;
}