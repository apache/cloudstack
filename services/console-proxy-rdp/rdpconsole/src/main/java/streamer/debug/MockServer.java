// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package streamer.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

public class MockServer implements Runnable {
    private static final Logger s_logger = Logger.getLogger(MockServer.class);

    private boolean shutdown = false;
    private ServerSocket serverSocket;
    private final Packet[] packets;
    private Throwable exception;
    private boolean shutdowned;

    /**
     * Set to true to enable debugging messages.
     */
    protected boolean verbose = System.getProperty("rdpclient.MockServer.debug", "false").equals("true");

    public MockServer(Packet packets[]) {
        this.packets = packets;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(0);

        shutdown = false;
        exception = null;
        shutdowned = false;

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {

        try (Socket socket = serverSocket.accept();) {

            if (verbose)
                System.out.println("[" + this + "] INFO: Client connected: " + socket.getRemoteSocketAddress() + ".");

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            try {
                for (int i = 0; i < packets.length && !shutdown; i++) {

                    Packet packet = packets[i];
                    switch (packet.type) {
                    case CLIENT: {
                        // Read client data and compare it with mock data
                        // (unless "ignore" option is set)
                        byte actualData[] = new byte[packet.data.length];
                        int actualDataLength = is.read(actualData);

                        if (verbose)
                            System.out.println("[" + this + "] INFO: Data is read: {" + Arrays.toString(Arrays.copyOf(actualData, actualDataLength)) + "}.");

                        if (!packet.ignore) {
                            // Compare actual data with expected data
                            if (actualDataLength != packet.data.length) {
                                throw new AssertionError("Actual length of client request for packet #" + (i + 1) + " (\"" + packet.id + "\")"
                                        + " does not match length of expected client request. Actual length: " + actualDataLength + ", expected legnth: " + packet.data.length
                                        + ".");
                            }

                            for (int j = 0; j < packet.data.length; j++) {

                                if (packet.data[j] != actualData[j]) {
                                    throw new AssertionError("Actual byte #" + (j + 1) + " of client request for packet #" + (i + 1) + " (\"" + packet.id + "\")"
                                            + " does not match corresponding byte of expected client request. Actual byte: " + actualData[j] + ", expected byte: " + packet.data[j]
                                                    + ".");
                                }
                            }
                        }
                        break;
                    }
                    case SERVER: {
                        // Send mock data to client
                        os.write(packet.data);

                        if (verbose)
                            System.out.println("[" + this + "] INFO: Data is written: {" + Arrays.toString(packet.data) + "}.");

                        break;
                    }
                    case UPGRADE_TO_SSL: {
                        // Attach SSL context to socket

                        final SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
                        SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket(socket, null, serverSocket.getLocalPort(), true);
                        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
                        sslSocket.setUseClientMode(false);
                        sslSocket.startHandshake();
                        is = sslSocket.getInputStream();
                        os = sslSocket.getOutputStream();

                        break;
                    }
                    default:
                        throw new RuntimeException("Unknown packet type: " + packet.type);
                    }

                }
            } finally {
                try {
                    is.close();
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "in stream close failed: " + e.getLocalizedMessage());
                }
                try {
                    os.close();
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "out stream close failed: " + e.getLocalizedMessage());
                }
                try {
                    serverSocket.close();
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "server socket close failed: " + e.getLocalizedMessage());
                }
            }
        } catch (Throwable e) {
            System.err.println("Error in mock server: " + e.getMessage());
            e.printStackTrace(System.err);
            exception = e;
        }
        shutdowned = true;
        if (verbose)
            System.out.println("[" + this + "] INFO: Mock server shutdowned.");

    }

    public void shutdown() {
        shutdown = true;
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress)serverSocket.getLocalSocketAddress();
    }

    public Throwable getException() {
        return exception;
    }

    public static class Packet {
        public static enum PacketType {
            SERVER, CLIENT, UPGRADE_TO_SSL;
        }

        public String id = "";

        public Packet() {
        }

        public Packet(String id) {
            this.id = id;
        }

        public PacketType type;

        public boolean ignore = false;

        public byte data[];
    }

    public boolean isShutdowned() {
        return shutdowned;
    }

    public void waitUntilShutdowned(long timeToWaitMiliseconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeToWaitMiliseconds;
        while (!shutdowned && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }
}
