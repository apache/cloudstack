/*
 * PeerListener - Interface for listening to peer events. Copyright (C) 2003
 * Mark J. Wielaard
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

/**
 * Listener for Peer events.
 */
public interface PeerListener
{
    /**
     * Called when the connection to the peer has started and the handshake was
     * successfull.
     * 
     * @param peer
     *            the Peer that just got connected.
     */
    void connected (Peer peer);

    /**
     * Called when the connection to the peer was terminated or the connection
     * handshake failed.
     * 
     * @param peer
     *            the Peer that just got disconnected.
     */
    void disconnected (Peer peer);

    /**
     * Called when a choke message is received.
     * 
     * @param peer
     *            the Peer that got the message.
     * @param choke
     *            true when the peer got a choke message, false when the peer
     *            got an unchoke message.
     */
    void gotChoke (Peer peer, boolean choke);

    /**
     * Called when an interested message is received.
     * 
     * @param peer
     *            the Peer that got the message.
     * @param interest
     *            true when the peer got a interested message, false when the
     *            peer got an uninterested message.
     */
    void gotInterest (Peer peer, boolean interest);

    /**
     * Called when a have piece message is received. If the method returns true
     * and the peer has not yet received a interested message or we indicated
     * earlier to be not interested then an interested message will be send.
     * 
     * @param peer
     *            the Peer that got the message.
     * @param piece
     *            the piece number that the per just got.
     * 
     * @return true when it is a piece that we want, false if the piece is
     *         already known.
     */
    boolean gotHave (Peer peer, int piece);

    /**
     * Called when a bitmap message is received. If this method returns true a
     * interested message will be send back to the peer.
     * 
     * @param peer
     *            the Peer that got the message.
     * @param bitfield
     *            a BitField containing the pieces that the other side has.
     * 
     * @return true when the BitField contains pieces we want, false if the
     *         piece is already known.
     */
    boolean gotBitField (Peer peer, BitField bitfield);

    /**
     * Called when a piece is received from the peer. The piece must be
     * requested by Peer.request() first. If this method returns false that
     * means the Peer provided a corrupted piece and the connection will be
     * closed.
     * 
     * @param peer
     *            the Peer that got the piece.
     * @param piece
     *            the piece number received.
     * @param bs
     *            the byte array containing the piece.
     * 
     * @return true when the bytes represent the piece, false otherwise.
     * @throws IOException 
     */
    boolean gotPiece (Peer peer, int piece, byte[] bs) throws IOException;

    /**
     * Called when the peer wants (part of) a piece from us. Only called when
     * the peer is not choked by us (<code>peer.choke(false)</code> was
     * called).
     * 
     * @param peer
     *            the Peer that wants the piece.
     * @param piece
     *            the piece number requested.
     * 
     * @return a byte array containing the piece or null when the piece is not
     *         available (which is a protocol error).
     */
    byte[] gotRequest (Peer peer, int piece) throws IOException;

    /**
     * Called when a (partial) piece has been downloaded from the peer.
     * 
     * @param peer
     *            the Peer from which size bytes where downloaded.
     * @param size
     *            the number of bytes that where downloaded.
     */
    void downloaded (Peer peer, int size);

    /**
     * Called when a (partial) piece has been uploaded to the peer.
     * 
     * @param peer
     *            the Peer to which size bytes where uploaded.
     * @param size
     *            the number of bytes that where uploaded.
     */
    void uploaded (Peer peer, int size);

    /**
     * Called when we are downloading from the peer and need to ask for a new
     * piece. Might be called multiple times before <code>gotPiece()</code> is
     * called.
     * 
     * @param peer
     *            the Peer that will be asked to provide the piece.
     * @param bitfield
     *            a BitField containing the pieces that the other side has.
     * 
     * @return one of the pieces from the bitfield that we want or -1 if we are
     *         no longer interested in the peer.
     */
    int wantPiece (Peer peer, BitField bitfield);
}
