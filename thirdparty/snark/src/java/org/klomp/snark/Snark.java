/*
 * Snark - Main snark program startup class. Copyright (C) 2003 Mark J. Wielaard
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.klomp.snark.bencode.BDecoder;

/**
 * Main Snark object used to fetch or serve a given file.
 * 
 * @author Mark Wielaard (mark@klomp.org)
 */
public class Snark
{
    /** The lowest port Snark will listen on for connections */
    public final static int MIN_PORT = 6881;

    /** The highest port Snark will listen on for connections */
    public final static int MAX_PORT = 6889;

    /** The path to the file being torrented */
    public String torrent;

    /** The metadata known about the torrent */
    public MetaInfo meta;

    /** The storage helper assisting us */
    public Storage storage;

    /** The coordinator managing our peers */
    public PeerCoordinator coordinator;

    /** Parcels out incoming requests to the appropriate places */
    public ConnectionAcceptor acceptor;

    /** Obtains information on new peers. */
    public TrackerClient trackerclient;

    /**
     * Constructs a Snark client.
     * @param torrent The address of the torrent to download or file to serve
     * @param ip The IP address to use when serving data
     * @param user_port The port number to use
     * @param slistener A custom {@link StorageListener} to use
     * @param clistener A custom {@link CoordinatorListener} to use
     */
    public Snark (String torrent, String ip, int user_port,
        StorageListener slistener, CoordinatorListener clistener)
    {
        this.slistener = slistener;
        this.clistener = clistener;
        this.torrent = torrent;
        this.user_port = user_port;
        this.ip = ip;

        // Create a new ID and fill it with something random. First nine
        // zeros bytes, then three bytes filled with snark and then
        // sixteen random bytes.
        Random random = new Random();
        int i;
        for (i = 0; i < 9; i++) {
            id[i] = 0;
        }
        id[i++] = snark;
        id[i++] = snark;
        id[i++] = snark;
        while (i < 20) {
            id[i++] = (byte)random.nextInt(256);
        }

        log.log(Level.FINE, "My peer id: " + PeerID.idencode(id));
    }

    /**
     * Sets the global logging level of Snark.
     */
    public static void setLogLevel (Level level)
    {
        log.setLevel(level);
        log.setUseParentHandlers(false);
        Handler handler = new ConsoleHandler();
        handler.setLevel(level);
        log.addHandler(handler);
    }

    /**
     * Returns a human-readable state of Snark.
     */
    public String getStateString ()
    {
        return activities[activity];
    }

    /**
     * Returns the integer code for the human-readable state of Snark.
     */
    public int getState ()
    {
        return activity;
    }

    /**
     * Establishes basic information such as {@link #id}, opens ports,
     * and determines whether to act as a peer or seed.
     */
    public void setupNetwork ()
        throws IOException
    {
        activity = NETWORK_SETUP;

        IOException lastException = null;
        if (user_port != -1) {
            port = user_port;
            try {
                serversocket = new ServerSocket(port);
            } catch (IOException ioe) {
                lastException = ioe;
            }
        } else {
            for (port = MIN_PORT; serversocket == null && port <= MAX_PORT; port++) {
                try {
                    serversocket = new ServerSocket(port);
                } catch (IOException ioe) {
                    lastException = ioe;
                }
            }
        }
        if (serversocket == null) {
            String message = "Cannot accept incoming connections ";
            if (user_port == -1) {
                message = message + "tried ports " + MIN_PORT + " - "
                    + MAX_PORT;
            } else {
                message = message + "on port " + user_port;
            }

            if (ip != null || user_port != -1) {
                abort(message, lastException);
            } else {
                log.log(Level.WARNING, message);
            }
            port = -1;
        } else {
            port = serversocket.getLocalPort();
            log.log(Level.FINE, "Listening on port: " + port);
        }

        // Figure out what the torrent argument represents.
        meta = null;
        File f = null;
        try {
            InputStream in;
            f = new File(torrent);
            if (f.exists()) {
                in = new FileInputStream(f);
            } else {
                activity = GETTING_TORRENT;
                URL u = new URL(torrent);
                URLConnection c = u.openConnection();
                c.connect();
                in = c.getInputStream();

                if (c instanceof HttpURLConnection) {
                    // Check whether the page exists
                    int code = ((HttpURLConnection)c).getResponseCode();
                    if (code / 100 != 2) {
                        // responses
                        abort("Loading page '" + torrent + "' gave error code "
                            + code + ", it probably doesn't exists");
                    }
                }
            }
            meta = new MetaInfo(new BDecoder(in));
        } catch (IOException ioe) {
            // OK, so it wasn't a torrent metainfo file.
            if (f != null && f.exists()) {
                if (ip == null) {
                    abort("'" + torrent + "' exists,"
                        + " but is not a valid torrent metainfo file."
                        + System.getProperty("line.separator")
                        + "  (use --share to create a torrent from it"
                        + " and start sharing)", ioe);
                } else {
                    // Try to create a new metainfo file
                    log.log(Level.INFO,
                        "Trying to create metainfo torrent for '" + torrent
                            + "'");
                    try {
                        activity = CREATING_TORRENT;
                        storage = new Storage(f, "http://" + ip + ":" + port
                            + "/announce", slistener);
                        storage.create();
                        meta = storage.getMetaInfo();
                    } catch (IOException ioe2) {
                        abort("Could not create torrent for '" + torrent + "'",
                            ioe2);
                    }
                }
            } else {
                abort("Cannot open '" + torrent + "'", ioe);
            }
        }

        log.log(Level.INFO, meta.toString());
    }

    /**
     * Start the upload/download process and begins exchanging pieces
     * with other peers.
     */
    public void collectPieces ()
        throws IOException
    {
        // When the metainfo torrent was created from an existing file/dir
        // it already exists.
        if (storage == null) {
            try {
                activity = CHECKING_STORAGE;
                storage = new Storage(meta, slistener);
                storage.check();
            } catch (IOException ioe) {
                abort("Could not create storage", ioe);
            }
        }

        activity = COLLECTING_PIECES;
        coordinator = new PeerCoordinator(id, meta, storage, clistener);
        HttpAcceptor httpacceptor;
        if (ip != null) {
            MetaInfo m = meta.reannounce("http://" + ip + ":" + port
                + "/announce");
            Tracker tracker = new Tracker(m);
            try {
                tracker.addPeer(meta.getHexInfoHash(),
                    new PeerID(id, InetAddress.getByName(ip), port));
            } catch (UnknownHostException oops) {
                abort("Could not start tracker for " + ip, oops);
            }
            httpacceptor = new HttpAcceptor(tracker);
            // Debug code for writing out .torrent to disk
            /*
            byte[] torrentData = tracker.getMetaInfo(
                meta.getHexInfoHash()).getTorrentData();
            try {
                log.log(Level.INFO, "Writing torrent to file " + torrent
                    + ".torrent");
                FileOutputStream fos = new FileOutputStream(torrent
                    + ".torrent");
                fos.write(torrentData);
                fos.close();
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not save torrent file.");
            }
            */
        } else {
            httpacceptor = null;
        }

        PeerAcceptor peeracceptor = new PeerAcceptor(coordinator);
        acceptor = new ConnectionAcceptor(serversocket, httpacceptor,
            peeracceptor);
        acceptor.start();

        if (ip != null) {
            log.log(Level.INFO, "Torrent available on " + "http://" + ip + ":"
                + port + "/" + meta.getHexInfoHash() + ".torrent");
        }

        trackerclient = new TrackerClient(meta, coordinator, port);
        trackerclient.start();
        coordinator.setTracker(trackerclient);
    }

    /**
     * Aborts program abnormally.
     */
    public static void abort (String s)
        throws IOException
    {
        abort(s, null);
    }

    /**
     * Aborts program abnormally.
     */
    public static void abort (String s, IOException ioe)
        throws IOException
    {
        log.log(Level.SEVERE, s, ioe);
        throw new IOException(s);
    }

    /** The listen port requested by the user */
    protected int user_port;

    /** The port number Snark listens on */
    protected int port;

    /** The IP address to listen on, if applicable */
    protected String ip;

    /** The {@link StorageListener} to send updates to */
    protected StorageListener slistener;

    /** The {@link CoordinatorListener} to send updates to */
    protected CoordinatorListener clistener;

    /** Our BitTorrent client id number, randomly assigned */
    protected byte[] id = new byte[20];

    /** The server socket that we are using to listen for connections */
    protected ServerSocket serversocket;

    /**
     * A magic constant used to identify the Snark library in the clientid.
     * 
     * <pre>Taking Three as the subject to reason about--
     * A convenient number to state--
     * We add Seven, and Ten, and then multiply out
     * By One Thousand diminished by Eight.
     *
     * The result we proceed to divide, as you see,
     * By Nine Hundred and Ninety Two:
     * Then subtract Seventeen, and the answer must be
     * Exactly and perfectly true.</pre>
     */
    protected static final byte snark =
        (((3 + 7 + 10) * (1000 - 8)) / 992) - 17;

    /** An integer indicating Snark's current activity. */
    protected int activity = NOT_STARTED;

    /** The list of possible activities */
    protected static final String[] activities =
        {"Not started", "Network setup", "Getting torrent", "Creating torrent",
        "Checking storage", "Collecting pieces", "Seeding"};

    public static final int NOT_STARTED = 0;
    public static final int NETWORK_SETUP = 1;
    public static final int GETTING_TORRENT = 2;
    public static final int CREATING_TORRENT = 3;
    public static final int CHECKING_STORAGE = 4;
    public static final int COLLECTING_PIECES = 5;
    public static final int SEEDING = 6;

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark");
}
