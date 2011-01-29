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


// KeyMap.h
// mapping of windows virtual key codes to X keysyms.

#ifndef KEYMAP_H__
#define KEYMAP_H__

#pragma once

#include "keysymdef.h"

#include "rfb.h"

// A single key press on the client may result in more than one 
// going to the server.

const unsigned int MaxKeysPerKey = 4;
const CARD32 VoidKeyCode = XK_VoidSymbol;

// keycodes contains the keysyms terminated by an VoidKeyCode.
// The releaseModifiers is a set of ORed flags indicating whether 
// particular modifier-up messages should be sent before the keys 
// and modifier-down after.

const CARD32 KEYMAP_LCONTROL = 0x0001;
const CARD32 KEYMAP_RCONTROL = 0x0002;
const CARD32 KEYMAP_LALT     = 0x0004;
const CARD32 KEYMAP_RALT     = 0x0008;

typedef struct {
    CARD32 keycodes[MaxKeysPerKey];
    CARD32 releaseModifiers;
} KeyActionSpec;



class KeyMap {
public:
	KeyMap();
	KeyActionSpec PCtoX(UINT virtkey, DWORD keyData);
private:
	// CARD32 keymap[256];
	unsigned char buf[4]; // lots of space for now
	BYTE keystate[256];

	DWORD   dwKeybId;
    HKL     dwhkl;
    unsigned char   storedDeadChar;
    UINT    baseCharVK;
    bool    sendDeadKey;
};

#endif