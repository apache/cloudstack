/*
 * PeerConnectionIn - Handles incomming messages and hands them to PeerState.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class PeerConnectionIn implements Runnable
{
    private final Peer peer;

    private final DataInputStream din;

    private Thread thread;

    private boolean quit;

    public PeerConnectionIn (Peer peer, DataInputStream din)
    {
        this.peer = peer;
        this.din = din;
        quit = false;
    }

    void disconnect ()
    {
        if (quit == true) {
            return;
        }

        quit = true;
        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
    }

    public void run ()
    {
        thread = Thread.currentThread();
        try {
            PeerState ps = peer.state;
            while (!quit && ps != null) {
                // Common variables used for some messages.
                int piece;
                int begin;
                int len;

                // Wait till we hear something...
                // The length of a complete message in bytes.
                int i = din.readInt();
                if (i < 0) {
                    throw new IOException("Unexpected length prefix: " + i);
                }

                if (i == 0) {
                    ps.keepAliveMessage();
                    continue;
                }

                byte b = din.readByte();
                Message m = new Message();
                m.type = b;
                switch (b) {
                case 0:
                    ps.chokeMessage(true);
                    break;
                case 1:
                    ps.chokeMessage(false);
                    break;
                case 2:
                    ps.interestedMessage(true);
                    break;
                case 3:
                    ps.interestedMessage(false);
                    break;
                case 4:
                    piece = din.readInt();
                    ps.haveMessage(piece);
                    break;
                case 5:
                    byte[] bitmap = new byte[i - 1];
                    din.readFully(bitmap);
                    ps.bitfieldMessage(bitmap);
                    break;
                case 6:
                    piece = din.readInt();
                    begin = din.readInt();
                    len = din.readInt();
                    ps.requestMessage(piece, begin, len);
                    break;
                case 7:
                    piece = din.readInt();
                    begin = din.readInt();
                    len = i - 9;
                    Request req = ps.getOutstandingRequest(piece, begin, len);
                    byte[] piece_bytes;
                    if (req != null) {
                        piece_bytes = req.bs;
                        din.readFully(piece_bytes, begin, len);
                        ps.pieceMessage(req);
                    } else {
                        // XXX - Consume but throw away afterwards.
                        piece_bytes = new byte[len];
                        din.readFully(piece_bytes);
                    }
                    break;
                case 8:
                    piece = din.readInt();
                    begin = din.readInt();
                    len = din.readInt();
                    ps.cancelMessage(piece, begin, len);
                    break;
                default:
                    byte[] bs = new byte[i - 1];
                    din.readFully(bs);
                    ps.unknownMessage(b, bs);
                }
            }
        } catch (IOException ioe) {
            // Ignore, probably the other side closed connection.
        } catch (Throwable t) {
            log.log(Level.SEVERE, peer + " failed", t);
        } finally {
            peer.disconnect();
        }
    }

    protected static final Logger log = Logger.getLogger("org.klomp.snark.peer");
}
