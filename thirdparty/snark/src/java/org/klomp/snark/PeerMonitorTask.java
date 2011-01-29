/*
 * PeerMonitorTasks - TimerTask that monitors the peers and total up/down speed
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

import java.util.Iterator;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TimerTask that monitors the peers and total up/download speeds. Works
 * together with the main Snark class to report periodical statistics.
 */
public class PeerMonitorTask extends TimerTask
{
    public static final long MONITOR_PERIOD = 10 * 1000; // Ten seconds.

    private static final long KILOPERSECOND = 1024 * (MONITOR_PERIOD / 1000);

    private final PeerCoordinator coordinator;

    private long lastDownloaded = 0;

    private long lastUploaded = 0;

    public PeerMonitorTask (PeerCoordinator coordinator)
    {
        this.coordinator = coordinator;
    }

    @Override
    public void run ()
    {
        // Get some statistics
        int peers = 0;
        int uploaders = 0;
        int downloaders = 0;
        int interested = 0;
        int interesting = 0;
        int choking = 0;
        int choked = 0;

        synchronized (coordinator.peers) {
            Iterator it = coordinator.peers.iterator();
            while (it.hasNext()) {
                Peer peer = (Peer)it.next();

                // Don't list dying peers
                if (!peer.isConnected()) {
                    continue;
                }

                peers++;

                if (!peer.isChoking()) {
                    uploaders++;
                }
                if (!peer.isChoked() && peer.isInteresting()) {
                    downloaders++;
                }
                if (peer.isInterested()) {
                    interested++;
                }
                if (peer.isInteresting()) {
                    interesting++;
                }
                if (peer.isChoking()) {
                    choking++;
                }
                if (peer.isChoked()) {
                    choked++;
                }
            }
        }

        // Print some statistics
        long downloaded = coordinator.getDownloaded();
        String totalDown;
        if (downloaded >= 10 * 1024 * 1024) {
            totalDown = (downloaded / (1024 * 1024)) + "MB";
        } else {
            totalDown = (downloaded / 1024) + "KB";
        }
        long uploaded = coordinator.getUploaded();
        String totalUp;
        if (uploaded >= 10 * 1024 * 1024) {
            totalUp = (uploaded / (1024 * 1024)) + "MB";
        } else {
            totalUp = (uploaded / 1024) + "KB";
        }

        int needP = coordinator.storage.needed();
        long needMB = needP * coordinator.metainfo.getPieceLength(0)
            / (1024 * 1024);
        int totalP = coordinator.metainfo.getPieces();
        long totalMB = coordinator.metainfo.getTotalLength() / (1024 * 1024);

        log.log(Level.INFO, "Down: " + (downloaded - lastDownloaded)
            / KILOPERSECOND + "KB/s" + " (" + totalDown + ")" + " Up: "
            + (uploaded - lastUploaded) / KILOPERSECOND + "KB/s" + " ("
            + totalUp + ")" + " Need " + needP + " (" + needMB + "MB)" + " of "
            + totalP + " (" + totalMB + "MB)" + " pieces");
        log.log(Level.INFO, peers + ": Download #" + downloaders + " Upload #"
            + uploaders + " Interested #" + interested + " Interesting #"
            + interesting + " Choking #" + choking + " Choked #" + choked);
        lastDownloaded = downloaded;
        lastUploaded = uploaded;
    }

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark.status");
}
