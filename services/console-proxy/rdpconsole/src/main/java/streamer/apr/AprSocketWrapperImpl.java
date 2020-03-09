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
package streamer.apr;

import static streamer.debug.MockServer.Packet.PacketType.CLIENT;
import static streamer.debug.MockServer.Packet.PacketType.SERVER;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.tomcat.jni.Address;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.Pool;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;
import org.apache.tomcat.jni.SSLSocket;
import org.apache.tomcat.jni.Socket;

import streamer.BaseElement;
import streamer.Direction;
import streamer.Element;
import streamer.Event;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.Queue;
import streamer.SocketWrapper;
import streamer.debug.MockServer;
import streamer.debug.MockServer.Packet;
import streamer.ssl.SSLState;
import sun.security.x509.X509CertImpl;

public class AprSocketWrapperImpl extends PipelineImpl implements SocketWrapper {
    private static final Logger s_logger = Logger.getLogger(AprSocketWrapperImpl.class);

    static {
        try {
            Library.initialize(null);
            SSL.initialize(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load Tomcat Native Library (Apache Portable Runtime).", e);
        }
    }

    private final SSLState sslState;

    final long pool = Pool.create(0);
    long inetAddress;
    long socket;
    private AprSocketSource source;
    private AprSocketSink sink;

    boolean shutdown = false;

    private final boolean shutdowned = false;

    public AprSocketWrapperImpl(String id, SSLState sslState) {
        super(id);
        this.sslState = sslState;
    }

    @Override
    protected HashMap<String, Element> initElementMap(String id) {
        HashMap<String, Element> map = new HashMap<String, Element>();

        source = new AprSocketSource(id + "." + OUT, this);
        sink = new AprSocketSink(id + "." + IN, this);

        // Pass requests to read data to socket input stream
        map.put(OUT, source);

        // All incoming data, which is sent to this socket wrapper, will be sent
        // to socket remote
        map.put(IN, sink);

        return map;
    }

    /**
     * Connect this socket wrapper to remote server and start main loop on
     * AprSocketSource stdout link, to watch for incoming data, and
     * AprSocketSink stdin link, to pull for outgoing data.
     */
    @Override
    public void connect(InetSocketAddress address) throws IOException {
        try {
            inetAddress = Address.info(address.getHostName(), Socket.APR_UNSPEC, address.getPort(), 0, pool);
            socket = Socket.create(Address.getInfo(inetAddress).family, Socket.SOCK_STREAM, Socket.APR_PROTO_TCP, pool);
        } catch (Exception e) {
            throw new IOException("[" + this + "] ERROR: Cannot create socket for \"" + address + "\".", e);
        }

        int ret = Socket.connect(socket, inetAddress);
        if (ret != 0)
            throw new IOException("[" + this + "] ERROR: Cannot connect to remote host \"" + address + "\": " + Error.strerror(ret));

        source.setSocket(socket);
        sink.setSocket(socket);

        // Start polling for data to send to remote sever
        runMainLoop(IN, STDIN, true, true);

        // Push incoming data from server to handlers
        runMainLoop(OUT, STDOUT, false, false);

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

    @Override
    public void validate() {

        if (getPads(Direction.IN).size() == 0)
            throw new RuntimeException("[ " + this + "] BUG: Input of socket is not connected.");

        if (getPads(Direction.OUT).size() == 0)
            throw new RuntimeException("[ " + this + "] BUG: Output of socket is not connected.");

    }

    @Override
    public void upgradeToSsl() {

        try {
            long sslContext;
            try {
                sslContext = SSLContext.make(pool, SSL.SSL_PROTOCOL_TLSV1, SSL.SSL_MODE_CLIENT);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create SSL context using Tomcat native library.", e);
            }

            SSLContext.setOptions(sslContext, SSL.SSL_OP_DONT_INSERT_EMPTY_FRAGMENTS | SSL.SSL_OP_TLS_BLOCK_PADDING_BUG | SSL.SSL_OP_MICROSOFT_BIG_SSLV3_BUFFER
                    | SSL.SSL_OP_MSIE_SSLV2_RSA_PADDING);
            // FIXME: verify certificate by default
            SSLContext.setVerify(sslContext, SSL.SSL_CVERIFY_NONE, 0);
            int ret;
            try {
                ret = SSLSocket.attach(sslContext, socket);
            } catch (Exception e) {
                throw new RuntimeException("[" + this + "] ERROR: Cannot attach SSL context to socket: ", e);
            }
            if (ret != 0)
                throw new RuntimeException("[" + this + "] ERROR: Cannot attach SSL context to socket(" + ret + "): " + SSL.getLastError());

            try {
                ret = SSLSocket.handshake(socket);
            } catch (Exception e) {
                throw new RuntimeException("[" + this + "] ERROR: Cannot make SSL handshake with server: ", e);
            }
            if (ret != 0 && ret != 20014) // 20014: bad certificate signature FIXME: show prompt for self signed certificate
                throw new RuntimeException("[" + this + "] ERROR: Cannot make SSL handshake with server(" + ret + "): " + SSL.getLastError());

            try {
                byte[] key = SSLSocket.getInfoB(socket, SSL.SSL_INFO_CLIENT_CERT);
                //*DEBUG*/System.out.println("DEBUG: Server cert:\n"+new ByteBuffer(key).dump());
                sslState.serverCertificateSubjectPublicKeyInfo = new X509CertImpl(key).getPublicKey().getEncoded();
            } catch (Exception e) {
                throw new RuntimeException("[" + this + "] ERROR: Cannot get server public key: ", e);
            }

        } catch (RuntimeException e) {
            shutdown();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        if (shutdown)
            return;

        shutdown = true;

        try {
            handleEvent(Event.STREAM_CLOSE, Direction.IN);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "handling stream close event failed on input: " + e.getLocalizedMessage());
        }
        try {
            handleEvent(Event.STREAM_CLOSE, Direction.OUT);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "handling event close event failed on output: " + e.getLocalizedMessage());
        }
    }

    void destroyPull() {
        if (shutdowned)
            return;

        // Causes segfault in AprSocketSource.poll() method, so this function must be called from it
        try {
            Socket.close(socket);
            // or
            // Socket.shutdown(socket, Socket.APR_SHUTDOWN_READWRITE);
            Pool.destroy(pool);
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "failure during network cleanup: " + e.getLocalizedMessage());
        }

    }

    @Override
    public String toString() {
        return "AprSocketWrapper(" + id + ")";
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

            AprSocketWrapperImpl socketWrapper = new AprSocketWrapperImpl("socket", null);

            pipeline.add(socketWrapper);
            pipeline.add(new BaseElement("echo"));
            pipeline.add(new Queue("queue")); // To decouple input and output

            pipeline.link("socket", "echo", "queue", "socket");

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

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

}
