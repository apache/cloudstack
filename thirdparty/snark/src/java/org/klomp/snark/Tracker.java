/*
 * Tracker - Keeps track of clients sharing a particular torrent MetaInfo.
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

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.klomp.snark.bencode.BEncoder;

/**
 * Keeps track of clients sharing a particular torrent MetaInfo.
 */
public class Tracker
{
    private static final int INTERVAL_SEC = 15 * 60; // 15 minutes.

    private final Map<String, MetaInfo> metainfo =
        new HashMap<String, MetaInfo>();

    private final Set<String> info_hashes = new HashSet<String>();

    private Map<String, HashSet<PeerID>> peers =
        new HashMap<String, HashSet<PeerID>>();

    public Tracker (HashSet<String> hashes)
    {
        for (String hash : hashes) {
            info_hashes.add(hash);
            peers.put(hash, new HashSet<PeerID>());
        }
    }

    public Tracker (MetaInfo info)
    {
        String hash = info.getHexInfoHash();
        info_hashes.add(hash);
        metainfo.put(hash, info);
        peers.put(hash, new HashSet<PeerID>());
    }

    public MetaInfo getMetaInfo (String hash)
    {
        return metainfo.get(hash);
    }

    public void addPeer (String info_hash, PeerID peer)
    {
        HashSet<PeerID> peerset = peers.get(info_hash);
        if (peerset != null) {
            synchronized (peerset) {
                peerset.add(peer);
            }
        }
    }

    public byte[] handleRequest (InetAddress address, int port, Map params)
    {
        log.log(Level.FINE, "TrackerReq " + address + ":" + port + " -> "
            + params);

        String info_hash_value = (String)params.get("info_hash");
        if (info_hash_value == null) {
            return failure("No info_hash given");
        }
        info_hash_value = info_hash_value.replace("%", "");

        boolean found = false;
        for (String hash : info_hashes) {
            if (hash.equals(info_hash_value)) {
                found = true;
            }
        }
        if (!found) {
            return failure("Tracker doesn't handle given info_hash");
        }

        byte[] peer_id;
        String peer_id_value = (String)params.get("peer_id");
        if (peer_id_value == null) {
            return failure("No peer_id given");
        }

        peer_id = urldecode(peer_id_value);
        if (peer_id.length != 20) {
            return failure("peer_id must be 20 bytes long");
        }

        int peer_port;
        String peer_port_value = (String)params.get("port");
        if (peer_port_value == null) {
            return failure("No port given");
        }

        try {
            peer_port = Integer.parseInt(peer_port_value);
        } catch (NumberFormatException nfe) {
            return failure("port not a number: " + nfe);
        }

        // This is unsafe although other trackers support it.
        // It is nice for people that use proxies, but opens up
        // a whole can of worms (filling the tracker with fake ips).
        //
        // It could bee allowed for private use and local addresses
        // See RFC1918 and 127.0.0.0/8.
        /*
         * String ip = (String)params.get("ip"); if (ip != null) { try { address =
         * InetAddress.getByName(ip); } catch (UnknownHostException uhe) { } }
         */

        PeerID peer = new PeerID(peer_id, address, peer_port);

        Map<String, Object> response = new HashMap<String, Object>();
        Set<PeerID> peerset = peers.get(info_hash_value);
        synchronized (peerset) {
            String event = (String)params.get("event");
            if ("stopped".equals(event)) {
                peerset.remove(peer);
            } else {
                peerset.add(peer);
            }

            response.put("interval", new Integer(INTERVAL_SEC));
            List<Map<String, Object>> peerList = new ArrayList<Map<String, Object>>();
            Iterator it = peerset.iterator();
            while (it.hasNext()) {
                PeerID peerID = (PeerID)it.next();
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("peer id", peerID.getID());
                m.put("ip", peerID.getAddress().getHostAddress());
                m.put("port", new Integer(peerID.getPort()));
                peerList.add(m);
            }
            response.put("peers", peerList);
        }

        log.log(Level.FINE, "Tracker response: " + response);

        return BEncoder.bencode(response);
    }

    private static byte[] failure (String s)
    {
        Map<String, String> m = new HashMap<String, String>();
        m.put("failure reason", s);
        return BEncoder.bencode(m);
    }

    /**
     * Cheap (but slow) urldecode String to byte array.
     */
    static byte[] urldecode (String s)
    {
        s = s.replace('+', ' ');
        char[] cs = s.toCharArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        while (i < cs.length) {
            if (cs[i] != '%') {
                baos.write((byte)cs[i]);
                i++;
            } else if (i + 2 < cs.length) {
                int val = 16 * Character.digit(cs[i + 1], 16)
                    + Character.digit(cs[i + 2], 16);
                baos.write((byte)val);
                i += 3;
            } else {
                i++;
            }
        }
        return baos.toByteArray();
    }

    protected static final Logger log = Logger.getLogger("org.klomp.snark.Tracker");
}
