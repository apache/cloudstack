/*
 * ConnectionAcceptor - Accepts connections and routes them to sub-acceptors.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Accepts connections on a TCP port and routes them to sub-acceptors.
 */
public class ConnectionAcceptor implements Runnable
{
    private final ServerSocket serverSocket;

    private final HttpAcceptor httpacceptor;

    private final PeerAcceptor peeracceptor;

    private Thread thread;

    private boolean stop;

    public ConnectionAcceptor (ServerSocket serverSocket,
        HttpAcceptor httpacceptor, PeerAcceptor peeracceptor)
    {
        this.serverSocket = serverSocket;
        this.httpacceptor = httpacceptor;
        this.peeracceptor = peeracceptor;

        stop = false;
    }

    public void start ()
    {
        thread = new Thread(this);
        thread.start();
    }

    public void halt ()
    {
        stop = true;

        ServerSocket ss = serverSocket;
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException ioe) {
            }
        }

        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
    }

    public int getPort ()
    {
        return serverSocket.getLocalPort();
    }

    public void run ()
    {
        while (!stop) {
            try {
                final Socket socket = serverSocket.accept();
                Thread t = new Thread("Connection-" + socket) {
                    @Override
                    public void run ()
                    {
                        try {
                            InputStream in = socket.getInputStream();
                            OutputStream out = socket.getOutputStream();
                            BufferedInputStream bis = new BufferedInputStream(
                                in);
                            BufferedOutputStream bos = new BufferedOutputStream(
                                out);

                            // See what kind of connection it is.
                            if (httpacceptor != null) {
                                byte[] scratch = new byte[4];
                                bis.mark(4);
                                for (int len = 0; len < 4; len++) {
                                	scratch[len++] = (byte)bis.read();
                                }
                                bis.reset();
                                if (scratch[0] == 19 && scratch[1] == 'B'
                                    && scratch[2] == 'i' && scratch[3] == 't') {
                                    peeracceptor.connection(socket, bis, bos);
                                } else if (scratch[0] == 'G'
                                    && scratch[1] == 'E' && scratch[2] == 'T'
                                    && scratch[3] == ' ') {
                                    httpacceptor.connection(socket, bis, bos);
                                }
                            } else {
                                peeracceptor.connection(socket, bis, bos);
                            }
                        } catch (IOException ioe) {
                            try {
                                socket.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                };
                t.start();
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Error while accepting", ioe);
                stop = true;
            }
        }

        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark.server");
}
