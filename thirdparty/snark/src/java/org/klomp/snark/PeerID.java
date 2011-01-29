/*
 * PeerID - All public information concerning a peer. Copyright (C) 2003 Mark J.
 * Wielaard
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.klomp.snark.bencode.BDecoder;
import org.klomp.snark.bencode.BEValue;
import org.klomp.snark.bencode.InvalidBEncodingException;

public class PeerID implements Comparable
{
    private final byte[] id;

    private final InetAddress address;

    private final int port;

    private final int hash;

    public PeerID (byte[] id, InetAddress address, int port)
    {
        this.id = id;
        this.address = address;
        this.port = port;

        hash = calculateHash();
    }

    /**
     * Creates a PeerID from a BDecoder.
     */
    public PeerID (BDecoder be) throws IOException
    {
        this(be.bdecodeMap().getMap());
    }

    /**
     * Creates a PeerID from a Map containing BEncoded peer id, ip and port.
     */
    public PeerID (Map m)
        throws InvalidBEncodingException, UnknownHostException
    {
        BEValue bevalue = (BEValue)m.get("peer id");
        if (bevalue == null) {
            throw new InvalidBEncodingException("peer id missing");
        }
        id = bevalue.getBytes();

        bevalue = (BEValue)m.get("ip");
        if (bevalue == null) {
            throw new InvalidBEncodingException("ip missing");
        }
        address = InetAddress.getByName(bevalue.getString());

        bevalue = (BEValue)m.get("port");
        if (bevalue == null) {
            throw new InvalidBEncodingException("port missing");
        }
        port = bevalue.getInt();

        hash = calculateHash();
    }

    public byte[] getID ()
    {
        return id;
    }

    public InetAddress getAddress ()
    {
        return address;
    }

    public int getPort ()
    {
        return port;
    }

    private int calculateHash ()
    {
        int b = 0;
        for (byte element : id) {
            b ^= element;
        }
        return (b ^ address.hashCode()) ^ port;
    }

    /**
     * The hash code of a PeerID is the exclusive or of all id bytes.
     */
    @Override
    public int hashCode ()
    {
        return hash;
    }

    /**
     * Returns true if and only if this peerID and the given peerID have the
     * same 20 bytes as ID.
     */
    public boolean sameID (PeerID pid)
    {
        boolean equal = true;
        for (int i = 0; equal && i < id.length; i++) {
            equal = id[i] == pid.id[i];
        }
        return equal;
    }

    /**
     * Two PeerIDs are equal when they have the same id, address and port.
     */
    @Override
    public boolean equals (Object o)
    {
        if (o instanceof PeerID) {
            PeerID pid = (PeerID)o;

            return port == pid.port && address.equals(pid.address)
                && sameID(pid);
        } else {
            return false;
        }
    }

    /**
     * Compares port, address and id.
     */
    public int compareTo (Object o)
    {
        PeerID pid = (PeerID)o;

        int result = port - pid.port;
        if (result != 0) {
            return result;
        }

        result = address.hashCode() - pid.address.hashCode();
        if (result != 0) {
            return result;
        }

        for (byte element : id) {
            result = element - element;
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    /**
     * Returns the String "id@address:port" where id is the hex encoded id with
     * leading zeros removed.
     */
    @Override
    public String toString ()
    {
        return idencode(id) + "@" + address + ":" + port;
    }

    /**
     * Encode an id as a hex encoded string and remove leading zeros.
     */
    public static String idencode (byte[] bs)
    {
        boolean leading_zeros = true;

        StringBuffer sb = new StringBuffer(bs.length * 2);
        for (byte element : bs) {
            int c = element & 0xFF;
            if (leading_zeros && c == 0) {
                continue;
            } else {
                leading_zeros = false;
            }

            if (c < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(c));
        }

        return sb.toString();
    }

}
