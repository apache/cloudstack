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

//
// MRU maintains a list of 'Most Recently Used' strings in the registry
// 

#ifndef MRU_H__
#define MRU_H__

#pragma once
#include <windows.h>
#include <tchar.h>

class MRU  
{
public:

    // Create an MRU and initialise it from the key.
    // Key created if not existant.

    MRU(LPTSTR keyname, unsigned int maxnum = 8);

    // Add the item specified at the front of the list
    // Move it there if not already.  If this makes the
    // list longer than the maximum, older ones are deleted.
    void AddItem(LPTSTR txt);

    // How many items are on the list?
    int NumItems();

    // Return them in order. 0 is the newest.
    // NumItems()-1 is the oldest.
    int GetItem(int index, LPTSTR buf, int buflen);

    // Remove the specified item if it exists.
    void RemoveItem(LPTSTR txt);

    // Remove the item with the given index.
    // If this is greater than NumItems()-1 it will be ignored.
    void RemoveItem(int index);

	virtual ~MRU();

private:
	void ReadIndex();

    TCHAR m_index[28];  // allow a bit of workspace beyond maximum of 26
    HKEY m_hRegKey;
    unsigned int m_maxnum;

    void Tidy();
protected:
	void WriteIndex();
};

#endif
