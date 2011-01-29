/*
 * Message - A protocol message which can be send through a DataOutputStream.
 * Copyright (C) 2003 Mark J. Wielaard
 * 
 * This file is part of Snark.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.klomp.snark;

import java.io.DataOutputStream;
import java.io.IOException;

// Used to queue outgoing connections
// sendMessage() should be used to translate them to wire format.
class Message
{
    final static byte KEEP_ALIVE = -1;

    final static byte CHOKE = 0;

    final static byte UNCHOKE = 1;

    final static byte INTERESTED = 2;

    final static byte UNINTERESTED = 3;

    final static byte HAVE = 4;

    final static byte BITFIELD = 5;

    final static byte REQUEST = 6;

    final static byte PIECE = 7;

    final static byte CANCEL = 8;

    // Not all fields are used for every message.
    // KEEP_ALIVE doesn't have a real wire representation
    byte type;

    // Used for HAVE, REQUEST, PIECE and CANCEL messages.
    int piece;

    // Used for REQUEST, PIECE and CANCEL messages.
    int begin;

    int length;

    // Used for PIECE and BITFIELD messages
    byte[] data;

    int off;

    int len;

    /** Utility method for sending a message through a DataStream. */
    void sendMessage (DataOutputStream dos) throws IOException
    {
        // KEEP_ALIVE is special.
        if (type == KEEP_ALIVE) {
            dos.writeInt(0);
            return;
        }

        // Calculate the total length in bytes

        // Type is one byte.
        int datalen = 1;

        // piece is 4 bytes.
        if (type == HAVE || type == REQUEST || type == PIECE || type == CANCEL) {
            datalen += 4;
        }

        // begin/offset is 4 bytes
        if (type == REQUEST || type == PIECE || type == CANCEL) {
            datalen += 4;
        }

        // length is 4 bytes
        if (type == REQUEST || type == CANCEL) {
            datalen += 4;
        }

        // add length of data for piece or bitfield array.
        if (type == BITFIELD || type == PIECE) {
            datalen += len;
        }

        // Send length
        dos.writeInt(datalen);
        dos.writeByte(type & 0xFF);

        // Send additional info (piece number)
        if (type == HAVE || type == REQUEST || type == PIECE || type == CANCEL) {
            dos.writeInt(piece);
        }

        // Send additional info (begin/offset)
        if (type == REQUEST || type == PIECE || type == CANCEL) {
            dos.writeInt(begin);
        }

        // Send additional info (length); for PIECE this is implicit.
        if (type == REQUEST || type == CANCEL) {
            dos.writeInt(length);
        }

        // Send actual data
        if (type == BITFIELD || type == PIECE) {
            dos.write(data, off, len);
        }
    }

    @Override
    public String toString ()
    {
        switch (type) {
        case KEEP_ALIVE:
            return "KEEP_ALIVE";
        case CHOKE:
            return "CHOKE";
        case UNCHOKE:
            return "UNCHOKE";
        case INTERESTED:
            return "INTERESTED";
        case UNINTERESTED:
            return "UNINTERESTED";
        case HAVE:
            return "HAVE(" + piece + ")";
        case BITFIELD:
            return "BITFIELD";
        case REQUEST:
            return "REQUEST(" + piece + "," + begin + "," + length + ")";
        case PIECE:
            return "PIECE(" + piece + "," + begin + "," + length + ")";
        case CANCEL:
            return "CANCEL(" + piece + "," + begin + "," + length + ")";
        default:
            return "<UNKNOWN>";
        }
    }
}
