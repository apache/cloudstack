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

// Thanks to Martin C. Mueller <mcm@itwm.uni-kl.de> for assistance with the
// international keyboard mapping stuff.

// KeyMap.cpp
#include "stdhdrs.h"
#include "KeyMap.h"
#include "vncviewer.h"

// Define the keymap structure
typedef struct vncKeymapping_struct {
	UINT wincode;
	UINT Xcode;
} vncKeymapping;

// Make up a VK for Enter - I think anything outside the range 0-255 will do
static const UINT VK_KEYPAD_ENTER = 0x1234;

static const vncKeymapping keymap[] = {
    {VK_BACK,		XK_BackSpace},
    {VK_TAB,		XK_Tab},
    {VK_CLEAR,		XK_Clear},
    {VK_RETURN,		XK_Return},
    {VK_LSHIFT,		XK_Shift_L},
    {VK_RSHIFT,		XK_Shift_R},
    {VK_SHIFT,      XK_Shift_L},
    {VK_LCONTROL,	XK_Control_L},
    {VK_RCONTROL,	XK_Control_R},
    {VK_CONTROL,	XK_Control_L},
    {VK_LMENU,		XK_Alt_L},
    {VK_RMENU,		XK_Alt_R},
    {VK_MENU,		XK_Alt_L},
    {VK_PAUSE,		XK_Pause},
    {VK_CAPITAL,	XK_Caps_Lock},
    {VK_ESCAPE,		XK_Escape},
    {VK_SPACE,		XK_space},
    {VK_PRIOR,		XK_Page_Up},
    {VK_NEXT,		XK_Page_Down},
    {VK_END,		XK_End},
    {VK_HOME,		XK_Home},
    {VK_LEFT,		XK_Left},
    {VK_UP,			XK_Up},
    {VK_RIGHT,		XK_Right},
    {VK_DOWN,		XK_Down},
    {VK_SELECT,		XK_Select},
    {VK_EXECUTE,	XK_Execute},
    {VK_SNAPSHOT,	XK_Print},
    {VK_INSERT,		XK_Insert},
    {VK_DELETE,		XK_Delete},
    {VK_HELP,		XK_Help},
    {VK_NUMPAD0,	XK_KP_0},
    {VK_NUMPAD1,	XK_KP_1},
    {VK_NUMPAD2,	XK_KP_2},
    {VK_NUMPAD3,	XK_KP_3},
    {VK_NUMPAD4,	XK_KP_4},
    {VK_NUMPAD5,	XK_KP_5},
    {VK_NUMPAD6,	XK_KP_6},
    {VK_NUMPAD7,	XK_KP_7},
    {VK_NUMPAD8,	XK_KP_8},
    {VK_NUMPAD9,	XK_KP_9},
    {VK_MULTIPLY,	XK_KP_Multiply},
    {VK_ADD,		XK_KP_Add},
    {VK_SEPARATOR,	XK_KP_Separator},   // often comma
    {VK_SUBTRACT,	XK_KP_Subtract},
    {VK_DECIMAL,	XK_KP_Decimal},
    {VK_DIVIDE,		XK_KP_Divide},
    {VK_F1,			XK_F1},
    {VK_F2,			XK_F2},
    {VK_F3,			XK_F3},
    {VK_F4,			XK_F4},
    {VK_F5,			XK_F5},
    {VK_F6,			XK_F6},
    {VK_F7,			XK_F7},
    {VK_F8,			XK_F8},
    {VK_F9,			XK_F9},
    {VK_F10,		XK_F10},
    {VK_F11,		XK_F11},
    {VK_F12,		XK_F12},
    {VK_F13,		XK_F13},
    {VK_F14,		XK_F14},
    {VK_F15,		XK_F15},
    {VK_F16,		XK_F16},
    {VK_F17,		XK_F17},
    {VK_F18,		XK_F18},
    {VK_F19,		XK_F19},
    {VK_F20,		XK_F20},
    {VK_F21,		XK_F21},
    {VK_F22,		XK_F22},
    {VK_F23,		XK_F23},
    {VK_F24,		XK_F24},
    {VK_NUMLOCK,	XK_Num_Lock},
    {VK_SCROLL,		XK_Scroll_Lock},
    {VK_KEYPAD_ENTER,	XK_KP_Enter},
    {VK_CANCEL,         XK_Break}
};


KeyMap::KeyMap() {
};


KeyActionSpec KeyMap::PCtoX(UINT virtkey, DWORD keyData) { 
	UINT numkeys = 0;
    
    KeyActionSpec kas;
    kas.releaseModifiers = 0;

    bool extended = ((keyData & 0x1000000) != 0);
//    vnclog.Print(8,_T(" keyData %04x \n"), keyData);
    
    if (extended) { 
//        vnclog.Print(8, _T(" (extended) "));
        switch (virtkey) {
        case VK_MENU :
            virtkey = VK_RMENU; break;
        case VK_CONTROL:
            virtkey = VK_RCONTROL; break;
        case VK_RETURN:
            virtkey = VK_KEYPAD_ENTER; break;
        }
    }
    
    // We try looking it up in our table
    UINT mapsize = sizeof(keymap) / sizeof(vncKeymapping);
        
    // Look up the desired code in the table
    for (UINT i = 0; i < mapsize; i++)
    {
        if (keymap[i].wincode == virtkey) {
            kas.keycodes[numkeys++] = keymap[i].Xcode;
            break;
        }
    }
    

    if (numkeys != 0) {
        UINT key = kas.keycodes[numkeys-1];
//        vnclog.Print(8, _T("keymap gives %u (%x) \n"), key, key);

    } else {
        // not found in table
//        vnclog.Print(8, _T("not in special keymap,wincode \n "));
		
		// Under CE, we're not so concerned about this bit because we handle a WM_CHAR message later


#ifndef UNDER_CE

        // we try a simple conversion to Ascii, using the current keyboard mapping
		::GetKeyboardState(keystate);

   		int ret = ::ToAscii(virtkey, 0, keystate, (WORD *) buf, 0);

        // If Left Ctrl & Alt both pressed and ToAscii gives a valid keysym
        // This is for AltGr on international keyboards  (= LCtrl-Alt).
        // e.g. Ctrl-Alt-Q gives @ on German keyboards
        if (  ((keystate[VK_MENU] & 0x80) != 0) &&
            ((keystate[VK_CONTROL] & 0x80) != 0) ) {

           // If the key means anything in this keyboard layout
           if  ( (ret >= 1) && 
                 ( ( (*buf >= 32) && (*buf <= 126) ) ||
                   ( (*buf >= 160) && (*buf <= 255) ) ) 
                ) {

               // Send the modifiers up, then the keystroke, then mods down
               // We don't release the right control; this allows German users
               // to use it for doing Ctl-@ etc. (though not under Win95 --
               // see below)

               if (GetKeyState(VK_LCONTROL) & 0x8000) kas.releaseModifiers |= KEYMAP_LCONTROL;
               if (GetKeyState(VK_LMENU)    & 0x8000) kas.releaseModifiers |= KEYMAP_LALT;
               if (GetKeyState(VK_RMENU)    & 0x8000)
				{
				   kas.releaseModifiers |= KEYMAP_RALT;
				}

               // This is for windows 95, and possibly other systems.  
               // The above GetKeyState calls don't work in 95 - they always return 0.
               // But if we're here at all we know that control and alt are pressed, so let's
               // raise all Control and Alt keys if we haven't registered any yet.
               if (kas.releaseModifiers == 0)
                   kas.releaseModifiers = KEYMAP_LCONTROL | KEYMAP_LALT | KEYMAP_RALT;

//               vnclog.Print(8, _T("Ctrl-Alt pressed: ToAscii (without modifiers) returns %d byte(s): "), ret);

			   if (ret!=2)
			   {
                for (int i = 0; i < ret; i++) {
                   kas.keycodes[numkeys++] = *(buf+i);
//                   vnclog.Print(8, _T("%02x (%c) "), *(buf+i) , *(buf+i));
                }
			   }
//                vnclog.Print(8,_T("\n"));
           } 

        } 
        
        // If not a ctrl-alt key
        if (numkeys == 0 && ret!=2 ) {
//			 vnclog.Print(8,_T("Numkey =0 \n "));

            // There are no keysyms corresponding to control characters 
            // Eg Ctrl F.  The server already knows whether the control 
            // key is pressed. So we are interested in the key that would be 
            // there if the Ctrl were not pressed.
            keystate[VK_CONTROL] = keystate[VK_LCONTROL] = keystate[VK_RCONTROL] = 0;

		    int ret = ::ToAscii(virtkey, 0, keystate, (WORD *) buf, 0);
            if (ret < 0) {
//				vnclog.Print(8,_T("ret -> daedkey, %s %d\n "),buf,buf[0]);
                switch (buf[0]) {
				case '`' :
					kas.keycodes[numkeys++] = XK_dead_grave; break;
				case '\'' :
					kas.keycodes[numkeys++] = XK_dead_acute; break;
				case '~' :
					kas.keycodes[numkeys++] = XK_dead_tilde; break;
				case 94:
					kas.keycodes[numkeys++] = XK_dead_circumflex; break;
				case 168:
					kas.keycodes[numkeys++] = XK_dead_diaeresis; break;
				}
            }
            // if this works, and it's a regular printable character, we just send that
            if (ret >= 1) {
//				vnclog.Print(8,_T("ret >=1 \n "));
//                vnclog.Print(8,_T("ToAscii (without ctrl) returns %d byte(s): "), ret);
				if (ret==2)
				{
					//also dead key strange.....
					if (buf[0]==180 && buf[1]==249) kas.keycodes[numkeys++] = XK_dead_acute;
					if (buf[0]==96 && buf[1]==181) kas.keycodes[numkeys++] = XK_dead_grave;

				}
				else
                for (int i = 0; i < ret; i++) {
                   kas.keycodes[numkeys++] = *(buf+i);
//                  vnclog.Print(8, _T("%02x (%c) %d %d %d"), *(buf+i) , *(buf+i),buf[0],buf[1],buf[2]);
                }
            }
        }
#endif

    }

    kas.keycodes[numkeys] = VoidKeyCode;
	return kas;
};
