//  TODO: add Copyright?
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

// This is the source for the low-level keyboard hook, which allows intercepting and sending
// special keys (such as ALT,CTRL, ALT+TAB, etc) to the VNCServer side.
// written by Assaf Gordon (Assaf@mazleg.com), 10/9/2003

#define WINVER 0x0400
#define _WIN32_WINNT 0x0400
#include "LowLevelHook.h"
#include "res/resource.h"

HWND LowLevelHook::g_hwndVNCViewer=NULL;
DWORD LowLevelHook::g_VncProcessID=0;
BOOL  LowLevelHook::g_fHookActive=FALSE;
HHOOK LowLevelHook::g_HookID=0;
BOOL  LowLevelHook::g_fGlobalScrollLock=FALSE;

BOOL LowLevelHook::Initialize(HWND hwndMain)
{
        HINSTANCE hInstance = NULL ;

        g_hwndVNCViewer = NULL ;
        g_fHookActive = GetScrollLockState() ;
        g_VncProcessID = 0 ;
        g_HookID = 0 ;
        g_fGlobalScrollLock = FALSE ;

        //Store our window's handle
        g_hwndVNCViewer = hwndMain;
        if (g_hwndVNCViewer==NULL)
                return FALSE;


        //Get the HInstacne of this window
        //(required because LowLevel-Keyboard-Hook must be global, 
        // and need the HMODULE parameter in SetWindowsHookEx)
        hInstance = (HINSTANCE)GetWindowLong(g_hwndVNCViewer,GWL_HINSTANCE);
        if (hInstance==NULL)
                return FALSE;

        //
        //Store the ProcessID of the VNC window.
        //this will prevent the keyboard hook procedure to interfere
        //with keypressed in other processes' windows
        GetWindowThreadProcessId(g_hwndVNCViewer,&g_VncProcessID);

        //Try to set the hook procedure
        g_HookID = SetWindowsHookEx(WH_KEYBOARD_LL,VncLowLevelKbHookProc,hInstance,0);
        if (g_HookID==0) {

                DWORD dw = GetLastError();

                //TODO:
                //Analyze why the error occured (might be because we're under Win98/95/ME?)
                return FALSE ;
        }
        
        return TRUE;
}

BOOL LowLevelHook::Release()
{
        if (g_HookID!=0) {
                return UnhookWindowsHookEx(g_HookID);
        }
        return FALSE;
}

BOOL LowLevelHook::GetScrollLockState() 
{
  BYTE keyState[256];
  GetKeyboardState((LPBYTE)&keyState);
  return (keyState[VK_SCROLL] & 1);
}


#include "Log.h"
#include <tchar.h>
extern Log vnclog;

LRESULT CALLBACK LowLevelHook::VncLowLevelKbHookProc(INT nCode, WPARAM wParam, LPARAM lParam)
{
	vnclog.Print(1, _T("Begin LowLevelHook::VncLowLevelKbHookProc\n"));
	
        //if set to TRUE, the key-pressed message will NOT be passed on to windows.
    BOOL fHandled = FALSE;

        BOOL fKeyDown = FALSE;

    if (nCode == HC_ACTION) {
        KBDLLHOOKSTRUCT *pkbdllhook = (KBDLLHOOKSTRUCT *)lParam;

                DWORD ProcessID ;

                //Get the process ID of the Active Window
                //(The window with the input focus)
                GetWindowThreadProcessId(GetFocus(),&ProcessID);

                //only if this is "our" process (vncviewer's process)
                //we should intecept the key-presses
                if (ProcessID==g_VncProcessID) {

                        fKeyDown = ( (wParam==WM_KEYDOWN) || (wParam==WM_SYSKEYDOWN) );

                        switch (pkbdllhook->vkCode)
                        {
                                //Print Screen Key 
                                //      Request Full screen Update
                                //      Simulate a "Request Refresh" from the System Menu
                        case VK_SNAPSHOT:
                                if (fKeyDown && g_fHookActive) {
                                        PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_REQUEST_REFRESH,0);
                                        fHandled = TRUE;
                                }
                                break ;

                                //Pause Key 
                                //      Toggle FullScreen On/Off
                                //      Simulate a "FullScreen" from the System Menu
                        case VK_PAUSE:
                                if (fKeyDown && g_fHookActive) {
                                        PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_FULLSCREEN,0);
                                        fHandled = TRUE;
                                }
                                break ;


                                //Left or Right CONTROL keys
                                //      Simulate a "Send CONTROL up/down" from the System Menu
                        case VK_LCONTROL:
                        case VK_RCONTROL:
                                if (g_fHookActive) {
                                        if(fKeyDown)
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_CONN_CTLDOWN,0);
                                        else
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_CONN_CTLUP,0);
                                        fHandled = TRUE;
                                }
                                break;

                                //Either Left or Right ALT keys
                                //      Simulate a "Send ALT up/down" from the System Menu
                        case VK_LMENU:
                        case VK_RMENU:
                                if (g_fHookActive) {
                                        if(fKeyDown)
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_CONN_ALTDOWN,0);
                                        else
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_CONN_ALTUP,0);
                                        fHandled = TRUE;
                                }
                                break;
                        case VK_LWIN:
                                if (g_fHookActive) {
                                        if(fKeyDown)
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_LWINDOWN,0);
                                        else
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_LWINUP,0);
                                        fHandled = TRUE;
                                }
                                break;
						case VK_RWIN:
                                if (g_fHookActive) {
                                        if(fKeyDown)
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_RWINDOWN,0);
                                        else
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_RWINUP,0);
                                        fHandled = TRUE;
                                }
                                break;
						case VK_APPS:
                                if (g_fHookActive) {
                                        if(fKeyDown)
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_APPSDOWN,0);
                                        else
                                                PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,ID_VK_APPSUP,0);
                                        fHandled = TRUE;
                                }
                                break; 




                                //Scroll Lock = Turn the whole thing on or off
                                //This is a little tricky hack:
                                //Windows sets the scroll-lock LED on or off when the user PRESSes the scroll-lock key.
                                //We'll check the LED state when the user RELEASEs the key, so the LED is already set.
                                //If the LED/ScrollLock is ON, we'll activate the special key interception.
                        case VK_SCROLL:
                                if (!fKeyDown) {
                                        g_fHookActive = GetScrollLockState(); 
                                }
                                break;


                                //SPACEBAR = When key interception is Active, no special handling is required for 'spacebar'.
                                //But when key interception is turned off, I want ALT+SPACE to open the VNCViewer's System Menu.
                        case VK_SPACE:
                                if (!g_fHookActive) {
                                        if (pkbdllhook->flags & LLKHF_ALTDOWN) {
                                                if(!fKeyDown)
                                                        PostMessage(g_hwndVNCViewer,WM_SYSCOMMAND,0xF100,0x20); 

                                                fHandled = TRUE;
                                        }
                                }
                                break ;


                                
                                //TAB = If the user presses ALT+TAB, we must block the TAB key (fHandled=TRUE),
                                //Otherwise windows (on the VNCViewer's side) will switch to another application.
                                //But because we block the TAB key, the 'ClientConnection' window won't know to send
                                //a TAB key to the VNCServer. so we simulate a TAB key pressed. 
                                //(The ALT key down was already sent to the VNCServer when the user pressed ALT)
                        case VK_TAB:
                                if (g_fHookActive) {
                                        if (pkbdllhook->flags & LLKHF_ALTDOWN) {
                                                if(fKeyDown)
                                                        PostMessage(g_hwndVNCViewer,WM_KEYDOWN,VK_TAB,0);

                                                /* Implementation Note:
                                                   Don't send the Key-UP event, it confuses Windows on the server side.
                                                   (It makes Windows switch TWO applications at a time). 

                                                   This way it works OK on servers running Win98, Win2K, WinXP and Linux+IceWM.
                                                   Should test it with more servers to make sure it's OK.
                                                */
                                                fHandled = TRUE;
                                        }
                                }
                                break;

                                //ESCAPE = ALT+ESC is also a way to switch application, so we block the ESCAPE key,
                                //Otherwise windows (on the VNCViewer's side) will switch to another application.
                                //Transmitting the ALT+ESCAPE combination to a VNCServer running Windows doesn't work
                                //very well, so for now, we'll just block the ALT+ESCAPE combination.
                                //(CTRL+ESC work OK, BTW)
                case VK_ESCAPE:
                                if (g_fHookActive) {
                                        if (pkbdllhook->flags & LLKHF_ALTDOWN) {
                                                fHandled = TRUE;
                                        }
                                }
                                break;


                        } //switch(pkbdllhook->vkCode)

                } // if (ProcessID == g_VncProcesID)

        } // if (nCode==HT_ACTION)

        //Call the next hook, if we didn't handle this message
	vnclog.Print(1, _T("End LowLevelHook::VncLowLevelKbHookProc\n"));
    return (fHandled ? TRUE : CallNextHookEx(g_HookID, nCode, wParam, lParam));
}