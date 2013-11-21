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
package streamer;

import static rdpclient.MockServer.Packet.PacketType.CLIENT;
import static rdpclient.MockServer.Packet.PacketType.SERVER;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import rdpclient.MockServer;
import rdpclient.MockServer.Packet;
import rdpclient.TrustAllX509TrustManager;

public class SocketWrapper extends PipelineImpl {

    protected InputStreamSource source;
    protected OutputStreamSink sink;
    protected Socket socket;
    protected InetSocketAddress address;

    protected SSLSocket sslSocket;

    //protected String SSL_VERSION_TO_USE = "TLSv1.2";
    /*DEBUG*/protected String SSL_VERSION_TO_USE = "TLSv1";

    public SocketWrapper(String id) {
        super(id);
    }

    @Override
    protected HashMap<String, Element> initElementMap(String id) {
        HashMap<String, Element> map = new HashMap<String, Element>();

        source = new InputStreamSource(id + "." + OUT, this);
        sink = new OutputStreamSink(id + "." + IN, this);

        // Pass requests to read data to socket input stream
        map.put(OUT, source);

        // All incoming data, which is sent to this socket wrapper, will be sent
        // to socket remote
        map.put(IN, sink);

        return map;
    }

    /**
     * Connect this socket wrapper to remote server and start main loop on
     * IputStreamSource stdout link, to watch for incoming data, and
     * OutputStreamSink stdin link, to pull for outgoing data.
     *
     * @param address
     * @throws IOException
     */
    public void connect(InetSocketAddress address) throws IOException {
        this.address = address;

        // Connect socket to server
        socket = SocketFactory.getDefault().createSocket();
        try {
            socket.connect(address);

            InputStream is = socket.getInputStream();
            source.setInputStream(is);

            OutputStream os = socket.getOutputStream();
            sink.setOutputStream(os);

            // Start polling for data to send to remote sever
            runMainLoop(IN, STDIN, true, true);

            // Push incoming data from server to handlers
            runMainLoop(OUT, STDOUT, false, false);

        } finally {
            socket.close();
        }
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        switch (event) {
            case SOCKET_UPGRADE_TO_SSL:
                upgradeToSsl();
                break;
            default:
                super.handleEvent(event, direction);
                break;
        }
    }

    public void upgradeToSsl() {

        if (sslSocket != null)
            // Already upgraded
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Upgrading socket to SSL.");

        try {
            // Use most secure implementation of SSL available now.
            // JVM will try to negotiate TLS1.2, then will fallback to TLS1.0, if
            // TLS1.2 is not supported.
            SSLContext sslContext = SSLContext.getInstance(SSL_VERSION_TO_USE);

            // Trust all certificates (FIXME: insecure)
            sslContext.init(null, new TrustManager[] {new TrustAllX509TrustManager()}, null);

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            sslSocket = (SSLSocket)sslSocketFactory.createSocket(socket, address.getHostName(), address.getPort(), true);
            sslSocket.startHandshake();

            InputStream sis = sslSocket.getInputStream();
            source.setInputStream(sis);

            OutputStream sos = sslSocket.getOutputStream();
            sink.setOutputStream(sos);

        } catch (Exception e) {
            throw new RuntimeException("Cannot upgrade socket to SSL: " + e.getMessage(), e);
        }

    }

    @Override
    public void validate() {
        for (Element element : elements.values())
            element.validate();

        if (get(IN).getPads(Direction.IN).size() == 0)
            throw new RuntimeException("[ " + this + "] Input of socket is not connected.");

        if (get(OUT).getPads(Direction.OUT).size() == 0)
            throw new RuntimeException("[ " + this + "] Output of socket is not connected.");

    }

    public void shutdown() {
        try {
            handleEvent(Event.STREAM_CLOSE, Direction.IN);
        } catch (Exception e) {
        }
        try {
            handleEvent(Event.STREAM_CLOSE, Direction.OUT);
        } catch (Exception e) {
        }
        try {
            if (sslSocket != null)
                sslSocket.close();
        } catch (Exception e) {
        }
        try {
            socket.close();
        } catch (Exception e) {
        }
    }

    @Override
    public String toString() {
        return "SocketWrapper(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        try {
            System.setProperty("streamer.Link.debug", "true");
            System.setProperty("streamer.Element.debug", "true");
            System.setProperty("rdpclient.MockServer.debug", "true");

            Pipeline pipeline = new PipelineImpl("echo client");

            SocketWrapper socketWrapper = new SocketWrapper("socket");

            pipeline.add(socketWrapper);
            pipeline.add(new BaseElement("echo"));

            pipeline.link("socket", "echo", "socket");

            final byte[] mockData = new byte[] {0x01, 0x02, 0x03};
            MockServer server = new MockServer(new Packet[] {new Packet("Server hello") {
                {
                    type = SERVER;
                    data = mockData;
                }
            }, new Packet("Client hello") {
                {
                    type = CLIENT;
                    data = mockData;
                }
            }, new Packet("Server hello") {
                {
                    type = SERVER;
                    data = mockData;
                }
            }, new Packet("Client hello") {
                {
                    type = CLIENT;
                    data = mockData;
                }
            }});
            server.start();
            InetSocketAddress address = server.getAddress();

            socketWrapper.connect(address);

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

    }
}
