/*
 * TrackerInfo - Holds information returned by a tracker, mainly the peer list.
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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.klomp.snark.bencode.BDecoder;
import org.klomp.snark.bencode.BEValue;
import org.klomp.snark.bencode.InvalidBEncodingException;

public class TrackerInfo
{
    private final String failure_reason;

    private final int interval;

    private final Set peers;

    public TrackerInfo (InputStream in, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        this(new BDecoder(in), my_id, metainfo);
    }

    public TrackerInfo (BDecoder be, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        this(be.bdecodeMap().getMap(), my_id, metainfo);
    }

    public TrackerInfo (Map m, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        BEValue reason = (BEValue)m.get("failure reason");
        if (reason != null) {
            failure_reason = reason.getString();
            interval = -1;
            peers = null;
        } else {
            failure_reason = null;
            BEValue beInterval = (BEValue)m.get("interval");
            if (beInterval == null) {
                throw new InvalidBEncodingException("No interval given");
            } else {
                interval = beInterval.getInt();
            }
            BEValue bePeers = (BEValue)m.get("peers");
            if (bePeers == null) {
                throw new InvalidBEncodingException("No peer list");
            } else {
                peers = getPeers(bePeers.getList(), my_id, metainfo);
            }
        }
    }

    public static Set getPeers (InputStream in, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        return getPeers(new BDecoder(in), my_id, metainfo);
    }

    public static Set getPeers (BDecoder be, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        return getPeers(be.bdecodeList().getList(), my_id, metainfo);
    }

    public static Set<Peer> getPeers (List l, byte[] my_id, MetaInfo metainfo)
        throws IOException
    {
        Set<Peer> peers = new HashSet<Peer>(l.size());

        Iterator it = l.iterator();
        while (it.hasNext()) {
            PeerID peerID = new PeerID(((BEValue)it.next()).getMap());
            peers.add(new Peer(peerID, my_id, metainfo));
        }

        return peers;
    }

    public Set getPeers ()
    {
        return peers;
    }

    public String getFailureReason ()
    {
        return failure_reason;
    }

    public int getInterval ()
    {
        return interval;
    }

    @Override
    public String toString ()
    {
        if (failure_reason != null) {
            return "TrackerInfo[FAILED: " + failure_reason + "]";
        } else {
            return "TrackerInfo[interval=" + interval + ", peers=" + peers
                + "]";
        }
    }
}
