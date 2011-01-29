/*
 * Request - Holds all information needed for a (partial) piece request.
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

/**
 * Holds all information needed for a partial piece request.
 */
class Request
{
    final int piece;

    final byte[] bs;

    final int off;

    final int len;

    /**
     * Creates a new Request.
     * 
     * @param piece
     *            Piece number requested.
     * @param bs
     *            byte array where response should be stored.
     * @param off
     *            the offset in the array.
     * @param len
     *            the number of bytes requested.
     */
    Request (int piece, byte[] bs, int off, int len)
    {
        this.piece = piece;
        this.bs = bs;
        this.off = off;
        this.len = len;

        // Sanity check
        if (piece < 0 || off < 0 || len <= 0 || off + len > bs.length) {
            throw new IndexOutOfBoundsException("Illegal Request " + toString());
        }
    }

    @Override
    public int hashCode ()
    {
        return piece ^ off ^ len;
    }

    @Override
    public boolean equals (Object o)
    {
        if (o instanceof Request) {
            Request req = (Request)o;
            return req.piece == piece && req.off == off && req.len == len;
        }

        return false;
    }

    @Override
    public String toString ()
    {
        return "(" + piece + "," + off + "," + len + ")";
    }
}
