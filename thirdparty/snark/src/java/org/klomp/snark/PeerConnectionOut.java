/*
 * PeerConnectionOut - Keeps a queue of outgoing messages and delivers them.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class PeerConnectionOut implements Runnable
{
    private final Peer peer;

    private final DataOutputStream dout;

    private Thread thread;

    private boolean quit;

    private List<Message> sendQueue = new ArrayList<Message>();

    public PeerConnectionOut (Peer peer, DataOutputStream dout)
    {
        this.peer = peer;
        this.dout = dout;

        quit = false;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Continuesly monitors for more outgoing messages that have to be send.
     * Stops if quit is true of an IOException occurs.
     */
    public void run ()
    {
        try {
            while (!quit) {
                Message m = null;
                PeerState state = null;
                synchronized (sendQueue) {
                    while (!quit && sendQueue.isEmpty()) {
                        try {
                            // Make sure everything will reach the other side.
                            dout.flush();

                            // Wait till more data arrives.
                            sendQueue.wait();
                        } catch (InterruptedException ie) {
                            /* ignored */
                        }
                    }
                    state = peer.state;
                    if (!quit && state != null) {
                        // Piece messages are big. So if there are other
                        // (control) messages make sure they are send first.
                        // Also remove request messages from the queue if
                        // we are currently being choked to prevent them from
                        // being send even if we get unchoked a little later.
                        // (Since we will resent them anyway in that case.)
                        // And remove piece messages if we are choking.
                        Iterator it = sendQueue.iterator();
                        while (m == null && it.hasNext()) {
                            Message nm = (Message)it.next();
                            if (nm.type == Message.PIECE) {
                                if (state.choking) {
                                    it.remove();
                                }
                                nm = null;
                            } else if (nm.type == Message.REQUEST
                                && state.choked) {
                                it.remove();
                                nm = null;
                            }

                            if (m == null && nm != null) {
                                m = nm;
                                it.remove();
                            }
                        }
                        if (m == null && sendQueue.size() > 0) {
                            m = sendQueue.remove(0);
                        }
                    }
                }
                if (m != null) {
                    log.log(Level.ALL, "Send " + peer + ": " + m);
                    m.sendMessage(dout);

                    // Remove all piece messages after sending a choke message.
                    if (m.type == Message.CHOKE) {
                        removeMessage(Message.PIECE);
                    }

                    // XXX - Should also register overhead...
                    if (m.type == Message.PIECE) {
                        state.uploaded(m.len);
                    }

                    m = null;
                }
            }
        } catch (IOException ioe) {
            // Ignore, probably other side closed connection.
        } catch (Throwable t) {
            log.log(Level.SEVERE, peer + " failed", t);
        } finally {
            quit = true;
            peer.disconnect();
        }
    }

    public void disconnect ()
    {
        synchronized (sendQueue) {
            if (quit == true) {
                return;
            }

            quit = true;
            thread.interrupt();

            sendQueue.clear();
            sendQueue.notify();
        }
    }

    /**
     * Adds a message to the sendQueue and notifies the method waiting on the
     * sendQueue to change.
     */
    private void addMessage (Message m)
    {
        synchronized (sendQueue) {
            sendQueue.add(m);
            sendQueue.notify();
        }
    }

    /**
     * Removes a particular message type from the queue.
     * 
     * @param type
     *            the Message type to remove.
     * @returns true when a message of the given type was removed, false
     *          otherwise.
     */
    private boolean removeMessage (int type)
    {
        boolean removed = false;
        synchronized (sendQueue) {
            Iterator it = sendQueue.iterator();
            while (it.hasNext()) {
                Message m = (Message)it.next();
                if (m.type == type) {
                    it.remove();
                    removed = true;
                }
            }
        }
        return removed;
    }

    void sendAlive ()
    {
        Message m = new Message();
        m.type = Message.KEEP_ALIVE;
        addMessage(m);
    }

    void sendChoke (boolean choke)
    {
        // We cancel the (un)choke but keep PIECE messages.
        // PIECE messages are purged if a choke is actually send.
        synchronized (sendQueue) {
            int inverseType = choke ? Message.UNCHOKE : Message.CHOKE;
            if (!removeMessage(inverseType)) {
                Message m = new Message();
                if (choke) {
                    m.type = Message.CHOKE;
                } else {
                    m.type = Message.UNCHOKE;
                }
                addMessage(m);
            }
        }
    }

    void sendInterest (boolean interest)
    {
        synchronized (sendQueue) {
            int inverseType = interest ? Message.UNINTERESTED
                : Message.INTERESTED;
            if (!removeMessage(inverseType)) {
                Message m = new Message();
                if (interest) {
                    m.type = Message.INTERESTED;
                } else {
                    m.type = Message.UNINTERESTED;
                }
                addMessage(m);
            }
        }
    }

    void sendHave (int piece)
    {
        Message m = new Message();
        m.type = Message.HAVE;
        m.piece = piece;
        addMessage(m);
    }

    void sendBitfield (BitField bitfield)
    {
        Message m = new Message();
        m.type = Message.BITFIELD;
        m.data = bitfield.getFieldBytes();
        m.off = 0;
        m.len = m.data.length;
        addMessage(m);
    }

    void sendRequests (List requests)
    {
        Iterator it = requests.iterator();
        while (it.hasNext()) {
            Request req = (Request)it.next();
            sendRequest(req);
        }
    }

    void sendRequest (Request req)
    {
        Message m = new Message();
        m.type = Message.REQUEST;
        m.piece = req.piece;
        m.begin = req.off;
        m.length = req.len;
        addMessage(m);
    }

    void sendPiece (int piece, int begin, int length, byte[] bytes)
    {
        Message m = new Message();
        m.type = Message.PIECE;
        m.piece = piece;
        m.begin = begin;
        m.length = length;
        m.data = bytes;
        m.off = begin;
        m.len = length;
        addMessage(m);
    }

    void sendCancel (Request req)
    {
        // See if it is still in our send queue
        synchronized (sendQueue) {
            Iterator it = sendQueue.iterator();
            while (it.hasNext()) {
                Message m = (Message)it.next();
                if (m.type == Message.REQUEST && m.piece == req.piece
                    && m.begin == req.off && m.length == req.len) {
                    it.remove();
                }
            }
        }

        // Always send, just to be sure it it is really canceled.
        Message m = new Message();
        m.type = Message.CANCEL;
        m.piece = req.piece;
        m.begin = req.off;
        m.length = req.len;
        addMessage(m);
    }

    // Called by the PeerState when the other side doesn't want this
    // request to be handled anymore. Removes any pending Piece Message
    // from out send queue.
    void cancelRequest (int piece, int begin, int length)
    {
        synchronized (sendQueue) {
            Iterator it = sendQueue.iterator();
            while (it.hasNext()) {
                Message m = (Message)it.next();
                if (m.type == Message.PIECE && m.piece == piece
                    && m.begin == begin && m.length == length) {
                    it.remove();
                }
            }
        }
    }

    protected static final Logger log = Logger.getLogger("org.klomp.snark.peer");
}
