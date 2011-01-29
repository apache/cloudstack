/*
 * PeerCoordinator - Coordinates which peers do what (up and downloading).
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates what peer does what.
 */
public class PeerCoordinator implements PeerListener
{
    final MetaInfo metainfo;

    final Storage storage;

    // package local for access by CheckDownLoadersTask
    final static long CHECK_PERIOD = 20 * 1000; // 20 seconds

    final static int MAX_CONNECTIONS = 24;

    final static int MAX_UPLOADERS = 4;

    // Approximation of the number of current uploaders.
    // Resynced by PeerChecker once in a while.
    int uploaders = 0;

    // final static int MAX_DOWNLOADERS = MAX_CONNECTIONS;
    // int downloaders = 0;

    private long uploaded;

    private long downloaded;

    // synchronize on this when changing peers or downloaders
    public final List<Peer> peers = new ArrayList<Peer>();

    /** Timer to handle all periodical tasks. */
    private final Timer timer = new Timer(true);

    private final byte[] id;

    // Some random wanted pieces
    private final List<Integer> wantedPieces;

    private boolean halted = false;

    private final CoordinatorListener listener;

    private TrackerClient client;

    public PeerCoordinator (byte[] id, MetaInfo metainfo, Storage storage,
        CoordinatorListener listener)
    {
        this.id = id;
        this.metainfo = metainfo;
        this.storage = storage;
        this.listener = listener;

        // Make a random list of piece numbers
        wantedPieces = new ArrayList<Integer>();
        BitField bitfield = storage.getBitField();
        for (int i = 0; i < metainfo.getPieces(); i++) {
            if (!bitfield.get(i)) {
                wantedPieces.add(i);
            }
        }
        Collections.shuffle(wantedPieces);

        // Install a timer to check the uploaders.
        timer.schedule(new PeerCheckerTask(this), CHECK_PERIOD, CHECK_PERIOD);
    }

    public void setTracker (TrackerClient client)
    {
        this.client = client;
    }

    public byte[] getID ()
    {
        return id;
    }

    public boolean completed ()
    {
        return storage.complete();
    }

    public int getPeers ()
    {
        synchronized (peers) {
            return peers.size();
        }
    }

    /**
     * Returns how many bytes are still needed to get the complete file.
     */
    public long getLeft ()
    {
        // XXX - Only an approximation.
        return storage.needed() * metainfo.getPieceLength(0);
    }

    /**
     * Returns the total number of uploaded bytes of all peers.
     */
    public long getUploaded ()
    {
        return uploaded;
    }

    /**
     * Returns the total number of downloaded bytes of all peers.
     */
    public long getDownloaded ()
    {
        return downloaded;
    }

    public MetaInfo getMetaInfo ()
    {
        return metainfo;
    }

    public boolean needPeers ()
    {
        synchronized (peers) {
            return !halted && peers.size() < MAX_CONNECTIONS;
        }
    }

    public void halt ()
    {
        halted = true;
        synchronized (peers) {
            // Stop peer checker task.
            timer.cancel();

            // Stop peers.
            Iterator it = peers.iterator();
            while (it.hasNext()) {
                Peer peer = (Peer)it.next();
                peer.disconnect();
                it.remove();
            }
        }
    }

    public void connected (Peer peer)
    {
        if (halted) {
            peer.disconnect(false);
            return;
        }

        synchronized (peers) {
            if (peerIDInList(peer.getPeerID(), peers)) {
                log.log(Level.FINER, "Already connected to: " + peer);
                peer.disconnect(false); // Don't deregister this
                // connection/peer.
            } else {
                log.log(Level.FINER, "New connection to peer: " + peer);

                // Add it to the beginning of the list.
                // And try to optimistically make it a uploader.
                peers.add(0, peer);
                unchokePeer();

                if (listener != null) {
                    listener.peerChange(this, peer);
                }
            }
        }
    }

    private static boolean peerIDInList (PeerID pid, List peers)
    {
        Iterator it = peers.iterator();
        while (it.hasNext()) {
            if (pid.sameID(((Peer)it.next()).getPeerID())) {
                return true;
            }
        }
        return false;
    }

    public void addPeer (final Peer peer)
    {
        if (halted) {
            peer.disconnect(false);
            return;
        }

        boolean need_more;
        synchronized (peers) {
            need_more = !peer.isConnected() && peers.size() < MAX_CONNECTIONS;
        }

        if (need_more) {
            // Run the peer with us as listener and the current bitfield.
            final PeerListener listener = this;
            final BitField bitfield = storage.getBitField();
            Runnable r = new Runnable() {
                public void run ()
                {
                    peer.runConnection(listener, bitfield);
                }
            };
            String threadName = peer.toString();
            new Thread(r, threadName).start();
        } else if (log.getLevel().intValue() <= Level.FINER.intValue()) {
            if (peer.isConnected()) {
                log.log(Level.FINER, "Add peer already connected: " + peer);
            } else {
                log.log(Level.FINER, "MAX_CONNECTIONS = " + MAX_CONNECTIONS
                    + " not accepting extra peer: " + peer);
            }
        }
    }

    // (Optimistically) unchoke. Should be called with peers synchronized
    void unchokePeer ()
    {
        // linked list will contain all interested peers that we choke.
        // At the start are the peers that have us unchoked at the end the
        // other peer that are interested, but are choking us.
        List<Peer> interested = new LinkedList<Peer>();
        Iterator it = peers.iterator();
        while (it.hasNext()) {
            Peer peer = (Peer)it.next();
            if (uploaders < MAX_UPLOADERS && peer.isChoking()
                && peer.isInterested()) {
                if (!peer.isChoked()) {
                    interested.add(0, peer);
                } else {
                    interested.add(peer);
                }
            }
        }

        while (uploaders < MAX_UPLOADERS && interested.size() > 0) {
            Peer peer = interested.remove(0);
            log.log(Level.FINER, "Unchoke: " + peer);
            peer.setChoking(false);
            uploaders++;
            // Put peer back at the end of the list.
            peers.remove(peer);
            peers.add(peer);
        }
    }

    public byte[] getBitMap ()
    {
        return storage.getBitField().getFieldBytes();
    }

    /**
     * Returns true if we don't have the given piece yet.
     */
    public boolean gotHave (Peer peer, int piece)
    {
        if (listener != null) {
            listener.peerChange(this, peer);
        }

        synchronized (wantedPieces) {
            return wantedPieces.contains(new Integer(piece));
        }
    }

    /**
     * Returns true if the given bitfield contains at least one piece we are
     * interested in.
     */
    public boolean gotBitField (Peer peer, BitField bitfield)
    {
        if (listener != null) {
            listener.peerChange(this, peer);
        }

        synchronized (wantedPieces) {
            Iterator it = wantedPieces.iterator();
            while (it.hasNext()) {
                int i = ((Integer)it.next()).intValue();
                if (bitfield.get(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns one of pieces in the given BitField that is still wanted or -1 if
     * none of the given pieces are wanted.
     */
    public int wantPiece (Peer peer, BitField havePieces)
    {
        if (halted) {
            return -1;
        }

        synchronized (wantedPieces) {
            Integer piece = null;
            Iterator it = wantedPieces.iterator();
            while (piece == null && it.hasNext()) {
                Integer i = (Integer)it.next();
                if (havePieces.get(i.intValue())) {
                    it.remove();
                    piece = i;
                }
            }

            if (piece == null) {
                return -1;
            }

            // We add it back at the back of the list. It will be removed
            // if gotPiece is called later. This means that the last
            // couple of pieces might very well be asked from multiple
            // peers but that is OK.
            wantedPieces.add(piece);

            return piece.intValue();
        }
    }

    /**
     * Returns a byte array containing the requested piece or null of the piece
     * is unknown.
     */
    public byte[] gotRequest (Peer peer, int piece)
        throws IOException
    {
        if (halted) {
            return null;
        }

        try {
            return storage.getPiece(piece);
        } catch (IOException ioe) {
            Snark.abort("Error reading storage", ioe);
            return null; // Never reached.
        }
    }

    /**
     * Called when a peer has uploaded some bytes of a piece.
     */
    public void uploaded (Peer peer, int size)
    {
        uploaded += size;

        if (listener != null) {
            listener.peerChange(this, peer);
        }
    }

    /**
     * Called when a peer has downloaded some bytes of a piece.
     */
    public void downloaded (Peer peer, int size)
    {
        downloaded += size;

        if (listener != null) {
            listener.peerChange(this, peer);
        }
    }

    /**
     * Returns false if the piece is no good (according to the hash). In that
     * case the peer that supplied the piece should probably be blacklisted.
     */
    public boolean gotPiece (Peer peer, int piece, byte[] bs)
        throws IOException
    {
        if (halted) {
            return true; // We don't actually care anymore.
        }

        synchronized (wantedPieces) {
            Integer p = new Integer(piece);
            if (!wantedPieces.contains(p)) {
                log.log(Level.FINER, peer + " piece " + piece
                    + " no longer needed");

                // No need to announce have piece to peers.
                // Assume we got a good piece, we don't really care anymore.
                return true;
            }

            try {
                if (storage.putPiece(piece, bs)) {
                    log.log(Level.FINER, "Recv p" + piece + " " + peer);
                } else {
                    // Oops. We didn't actually download this then... :(
                    downloaded -= metainfo.getPieceLength(piece);
                    log.log(Level.INFO, "Got BAD piece " + piece + " from "
                        + peer);
                    return false; // No need to announce BAD piece to peers.
                }
            } catch (IOException ioe) {
                Snark.abort("Error writing storage", ioe);
            }
            wantedPieces.remove(p);
        }

        // Announce to the world we have it!
        synchronized (peers) {
            Iterator it = peers.iterator();
            while (it.hasNext()) {
                Peer p = (Peer)it.next();
                if (p.isConnected()) {
                    p.have(piece);
                }
            }
        }

        if (completed()) {
            client.interrupt();
        }
        return true;
    }

    public void gotChoke (Peer peer, boolean choke)
    {
        log.log(Level.FINER, "Got choke(" + choke + "): " + peer);

        if (listener != null) {
            listener.peerChange(this, peer);
        }
    }

    public void gotInterest (Peer peer, boolean interest)
    {
        if (interest) {
            synchronized (peers) {
                if (uploaders < MAX_UPLOADERS) {
                    if (peer.isChoking()) {
                        uploaders++;
                        peer.setChoking(false);
                        log.log(Level.FINER, "Unchoke: " + peer);
                    }
                }
            }
        }

        if (listener != null) {
            listener.peerChange(this, peer);
        }
    }

    public void disconnected (Peer peer)
    {
        log.log(Level.FINER, "Disconnected " + peer);

        synchronized (peers) {
            // Make sure it is no longer in our lists
            if (peers.remove(peer)) {
                // Unchoke some random other peer
                unchokePeer();
            }
        }

        if (listener != null) {
            listener.peerChange(this, peer);
        }
    }

    protected static final Logger log = Logger.getLogger("org.klomp.snark.peer");
}
