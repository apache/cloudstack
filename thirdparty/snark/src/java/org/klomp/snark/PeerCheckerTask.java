/*
 * PeerCheckTasks - TimerTask that checks for good/bad up/downloaders. Copyright
 * (C) 2003 Mark J. Wielaard
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TimerTask that checks for good/bad up/downloader. Works together with the
 * PeerCoordinator to select which Peers get (un)choked.
 */
class PeerCheckerTask extends TimerTask
{
    private static final long KILOPERSECOND = 1024 * (PeerCoordinator.CHECK_PERIOD / 1000);

    private final PeerCoordinator coordinator;

    PeerCheckerTask (PeerCoordinator coordinator)
    {
        this.coordinator = coordinator;
    }

    @Override
    public void run ()
    {
        synchronized (coordinator.peers) {
            // Calculate total uploading and worst downloader.
            long worstdownload = Long.MAX_VALUE;
            Peer worstDownloader = null;

            int peers = 0;
            int uploaders = 0;
            int downloaders = 0;
            int interested = 0;
            int interesting = 0;
            int choking = 0;
            int choked = 0;

            long uploaded = 0;
            long downloaded = 0;

            // Keep track of peers we remove now,
            // we will add them back to the end of the list.
            List<Peer> removed = new ArrayList<Peer>();

            Iterator it = coordinator.peers.iterator();
            while (it.hasNext()) {
                Peer peer = (Peer)it.next();

                // Remove dying peers
                if (!peer.isConnected()) {
                    it.remove();
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

                // XXX - We should calculate the up/download rate a bit
                // more intelligently
                long upload = peer.getUploaded();
                uploaded += upload;
                long download = peer.getDownloaded();
                downloaded += download;
                peer.resetCounters();

                log.log(Level.FINEST, peer + ":" + " ul: " + upload
                    / KILOPERSECOND + " dl: " + download / KILOPERSECOND
                    + " i: " + peer.isInterested() + " I: "
                    + peer.isInteresting() + " c: " + peer.isChoking() + " C: "
                    + peer.isChoked());

                // If we are at our max uploaders and we have lots of other
                // interested peers try to make some room.
                // (Note use of coordinator.uploaders)
                if (coordinator.uploaders >= PeerCoordinator.MAX_UPLOADERS
                    && interested > PeerCoordinator.MAX_UPLOADERS
                    && !peer.isChoking()) {
                    // Check if it still wants pieces from us.
                    if (!peer.isInterested()) {
                        log.log(Level.FINER, "Choke uninterested peer: " + peer);
                        peer.setChoking(true);
                        uploaders--;
                        coordinator.uploaders--;

                        // Put it at the back of the list
                        it.remove();
                        removed.add(peer);
                    } else if (peer.isChoked()) {
                        // If they are choking us make someone else a downloader
                        log.log(Level.FINEST, "Choke choking peer: " + peer);
                        peer.setChoking(true);
                        uploaders--;
                        coordinator.uploaders--;

                        // Put it at the back of the list
                        it.remove();
                        removed.add(peer);
                    } else if (peer.isInteresting() && !peer.isChoked()
                        && download == 0) {
                        // We are downloading but didn't receive anything...
                        log.log(Level.FINEST,
                            "Choke downloader that doesn't deliver:" + peer);
                        peer.setChoking(true);
                        uploaders--;
                        coordinator.uploaders--;

                        // Put it at the back of the list
                        it.remove();
                        removed.add(peer);
                    } else if (!peer.isChoking() && download < worstdownload) {
                        // Make sure download is good if we are uploading
                        worstdownload = download;
                        worstDownloader = peer;
                    }
                }
            }

            // Resync actual uploaders value
            // (can shift a bit by disconnecting peers)
            coordinator.uploaders = uploaders;

            // Remove the worst downloader if needed.
            if (uploaders >= PeerCoordinator.MAX_UPLOADERS
                && interested > PeerCoordinator.MAX_UPLOADERS
                && worstDownloader != null) {
                log.log(Level.FINEST, "Choke worst downloader: "
                    + worstDownloader);

                worstDownloader.setChoking(true);
                coordinator.uploaders--;

                // Put it at the back of the list
                coordinator.peers.remove(worstDownloader);
                removed.add(worstDownloader);
            }

            // Optimistically unchoke a peer
            coordinator.unchokePeer();

            // Put peers back at the end of the list that we removed earlier.
            coordinator.peers.addAll(removed);
        }
    }

    protected static final Logger log = Logger.getLogger("org.klomp.snark.peer");
}
