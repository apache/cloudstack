//
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
//

package com.cloud.utils.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cloudstack.framework.ca.CAService;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 */
public class Link {
    protected static Logger LOGGER = LogManager.getLogger(Link.class);

    private final InetSocketAddress _addr;
    private final NioConnection _connection;
    private SelectionKey _key;
    private final ConcurrentLinkedQueue<ByteBuffer[]> _writeQueue;
    private final ByteBuffer headerBuffer = ByteBuffer.allocate(4); // accumulates length header inside TLS
    private ByteBuffer netBuffer;
    private ByteBuffer appBuffer;
    private ByteBuffer plainTextBuffer;
    private int frameRemaining = -1; // remaining bytes for current frame (inside TLS)
    private Object _attach;

    private SSLEngine _sslEngine;

    public Link(InetSocketAddress addr, NioConnection connection) {
        _addr = addr;
        _connection = connection;
        _attach = null;
        _key = null;
        _writeQueue = new ConcurrentLinkedQueue<ByteBuffer[]>();
        plainTextBuffer = null;
    }

    public Link(Link link) {
        this(link._addr, link._connection);
    }

    public Object attachment() {
        return _attach;
    }

    public void attach(Object attach) {
        _attach = attach;
    }

    public void setKey(SelectionKey key) {
        synchronized (this) {
            _key = key;
        }
    }

    public void setSSLEngine(SSLEngine sslEngine) {
        _sslEngine = sslEngine;
        if (_sslEngine == null) {
            netBuffer = null;
            appBuffer = null;
            headerBuffer.clear();
            frameRemaining = -1;
            plainTextBuffer = null;
            return;
        }
        final SSLSession s = _sslEngine.getSession();
        netBuffer = ByteBuffer.allocate(Math.max(s.getPacketBufferSize(), 16 * 1024));
        appBuffer = ByteBuffer.allocate(Math.max(s.getApplicationBufferSize(), 16 * 1024));
        headerBuffer.clear();
        frameRemaining = -1;
        plainTextBuffer = null;
    }

    private static void doWrite(SocketChannel ch, ByteBuffer[] buffers, SSLEngine sslEngine) throws IOException {
        if (sslEngine == null) {
            throw new IOException("SSLEngine not set");
        }
        final SSLSession session = sslEngine.getSession();
        ByteBuffer netBuf = ByteBuffer.allocate(session.getPacketBufferSize());

        // Build app sequence: 4-byte length header + payload buffers
        int totalLen = 0;
        for (ByteBuffer b : buffers) totalLen += b.remaining();
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(totalLen).flip();

        ByteBuffer[] appSeq = new ByteBuffer[buffers.length + 1];
        appSeq[0] = header;
        for (int i = 0; i < buffers.length; i++) {
            appSeq[i + 1] = buffers[i].duplicate();
        }

        while (true) {
            // Check if all app buffers are fully consumed
            boolean allDone = true;
            for (ByteBuffer b : appSeq) {
                if (b.hasRemaining()) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;

            netBuf.clear();
            SSLEngineResult res;
            try {
                res = sslEngine.wrap(appSeq, netBuf);
            } catch (SSLException e) {
                throw new IOException("SSL wrap failed: " + e.getMessage(), e);
            }
            switch (res.getStatus()) {
                case OK:
                    netBuf.flip();
                    while (netBuf.hasRemaining()) {
                        ch.write(netBuf); // may be partial, loop until drained
                    }
                    break;
                case BUFFER_OVERFLOW:
                    netBuf = enlargeBuffer(netBuf, session.getPacketBufferSize());
                    break;
                case CLOSED:
                    throw new IOException("SSLEngine is CLOSED during write");
                default:
                    throw new IOException("Unexpected SSLEngineResult status on wrap: " + res.getStatus());
            }
            // Drain delegated tasks if any
            if (res.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null) task.run();
            }
            if (res.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) {
                // Unusual during application writes; upper layer should drive handshake
                break;
            }
        }
    }

    /**
     * write method to write to a socket.  This method writes to completion so
     * it doesn't follow the nio standard.  We use this to make sure we write
     * our own protocol.
     *
     * @param ch channel to write to.
     * @param buffers buffers to write.
     * @throws IOException if unable to write to completion.
     */
    public static void write(SocketChannel ch, ByteBuffer[] buffers, SSLEngine sslEngine) throws IOException {
        synchronized (ch) {
            doWrite(ch, buffers, sslEngine);
        }
    }

    public byte[] read(SocketChannel ch) throws IOException {
        if (_sslEngine == null) {
            throw new IOException("SSLEngine not set");
        }
        if (ch.read(netBuffer) == -1) {
            throw new IOException("Connection closed with -1 on read.");
        }
        netBuffer.flip();
        while (netBuffer.hasRemaining()) {
            SSLEngineResult res;
            try {
                res = _sslEngine.unwrap(netBuffer, appBuffer);
            } catch (SSLException e) {
                throw new IOException("SSL unwrap failed: " + e.getMessage(), e);
            }
            switch (res.getStatus()) {
                case OK:
                    appBuffer.flip();
                    while (appBuffer.hasRemaining()) {
                        if (frameRemaining < 0) {
                            int need = 4 - headerBuffer.position();
                            int take = Math.min(need, appBuffer.remaining());
                            int oldLimit = appBuffer.limit();
                            appBuffer.limit(appBuffer.position() + take);
                            headerBuffer.put(appBuffer);
                            appBuffer.limit(oldLimit);
                            if (headerBuffer.position() < 4) break;
                            headerBuffer.flip();
                            frameRemaining = headerBuffer.getInt();
                            headerBuffer.clear();
                            if (frameRemaining < 0) {
                                throw new IOException("Negative frame length");
                            }
                            if (plainTextBuffer == null || plainTextBuffer.capacity() < frameRemaining) {
                                plainTextBuffer = ByteBuffer.allocate(Math.max(frameRemaining, 2048));
                            }
                            plainTextBuffer.clear();
                        } else {
                            int toCopy = Math.min(frameRemaining, appBuffer.remaining());
                            if (plainTextBuffer.remaining() < toCopy) {
                                ByteBuffer newBuffer = ByteBuffer.allocate(plainTextBuffer.capacity() + Math.max(toCopy, 4096));
                                plainTextBuffer.flip();
                                newBuffer.put(plainTextBuffer);
                                plainTextBuffer = newBuffer;
                            }
                            int oldLimit = appBuffer.limit();
                            appBuffer.limit(appBuffer.position() + toCopy);
                            plainTextBuffer.put(appBuffer);
                            appBuffer.limit(oldLimit);
                            frameRemaining -= toCopy;
                            if (frameRemaining == 0) {
                                plainTextBuffer.flip();
                                byte[] result = new byte[plainTextBuffer.remaining()];
                                plainTextBuffer.get(result);
                                appBuffer.compact();
                                netBuffer.compact();
                                frameRemaining = -1;
                                return result;
                            }
                        }
                    }
                    appBuffer.compact();
                    break;
                case BUFFER_OVERFLOW:
                    appBuffer = enlargeBuffer(appBuffer, _sslEngine.getSession().getApplicationBufferSize());
                    break;
                case BUFFER_UNDERFLOW:
                    netBuffer = handleBufferUnderflow(_sslEngine, netBuffer);
                    netBuffer.compact();
                    return null;
                case CLOSED:
                    throw new IOException("SSLEngine closed during read");
                default:
                    throw new IOException("Unexpected SSLEngineResult status on unwrap: " + res.getStatus());
            }
            if (res.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable task;
                while ((task = _sslEngine.getDelegatedTask()) != null) task.run();
            }
        }
        netBuffer.compact();
        return null;
    }

    public void send(byte[] data) throws ClosedChannelException {
        send(data, false);
    }

    public void send(byte[] data, boolean close) throws ClosedChannelException {
        send(new ByteBuffer[] {ByteBuffer.wrap(data)}, close);
    }

    public void send(ByteBuffer[] data, boolean close) throws ClosedChannelException {
        ByteBuffer[] item = new ByteBuffer[data.length];
        int remaining = 0;
        for (int i = 0; i < data.length; i++) {
            remaining += data[i].remaining();
            item[i] = data[i];
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending framed message of length " + remaining);
        }

        _writeQueue.add(item);
        if (close) {
            _writeQueue.add(new ByteBuffer[0]);
        }
        synchronized (this) {
            if (_key == null) {
                throw new ClosedChannelException();
            }
            _connection.change(SelectionKey.OP_WRITE, _key, null);
        }
    }

    public void send(ByteBuffer[] data) throws ClosedChannelException {
        send(data, false);
    }

    public synchronized void close() {
        if (_key != null) {
            _connection.close(_key);
        }
    }

    public boolean write(SocketChannel ch) throws IOException {
        ByteBuffer[] data = null;
        while ((data = _writeQueue.poll()) != null) {
            if (data.length == 0) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Closing connection requested");
                }
                return true;
            }
            doWrite(ch, data, _sslEngine);
        }
        return false;
    }

    public InetSocketAddress getSocketAddress() {
        return _addr;
    }

    public String getIpAddress() {
        return _addr.getAddress().toString();
    }

    public synchronized void terminated() {
        _key = null;
    }

    public synchronized void schedule(Task task) throws ClosedChannelException {
        if (_key == null) {
            throw new ClosedChannelException();
        }
        _connection.scheduleTask(task);
    }

    public static KeyStore loadKeyStore(final InputStream stream, final char[] passphrase) throws GeneralSecurityException, IOException {
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(stream, passphrase);
        return ks;
    }

    public static SSLEngine initServerSSLEngine(final CAService caService, final String clientAddress) throws GeneralSecurityException, IOException {
        final SSLContext sslContext = SSLUtils.getSSLContextWithLatestProtocolVersion();
        if (caService != null) {
            return caService.createSSLEngine(sslContext, clientAddress);
        }
        LOGGER.error("CA service is not configured, by-passing CA manager to create SSL engine");
        char[] passphrase = KeyStoreUtils.DEFAULT_KS_PASSPHRASE;
        final KeyStore ks = loadKeyStore(NioConnection.class.getResourceAsStream("/cloud.keystore"), passphrase);
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        tmf.init(ks);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext.createSSLEngine();
    }

    public static SSLContext initManagementSSLContext(final CAService caService) throws GeneralSecurityException, IOException {
        if (caService == null) {
            throw new CloudRuntimeException("CAService is not available to load/get management server keystore");
        }
        final KeyStore ks = caService.getManagementKeyStore();
        char[] passphrase = caService.getKeyStorePassphrase();

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        final TrustManager[] tms = tmf.getTrustManagers();

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        final SSLContext sslContext = SSLUtils.getSSLContextWithLatestProtocolVersion();
        sslContext.init(kmf.getKeyManagers(), tms, new SecureRandom());
        return sslContext;
    }

    public static SSLContext initClientSSLContext() throws GeneralSecurityException, IOException {
        char[] passphrase = KeyStoreUtils.DEFAULT_KS_PASSPHRASE;
        File confFile = PropertiesUtil.findConfigFile("agent.properties");
        if (confFile != null) {
            LOGGER.info("Conf file found: " + confFile.getAbsolutePath());
            final String pass = PropertiesUtil.loadFromFile(confFile).getProperty(KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
            if (pass != null) {
                passphrase = pass.toCharArray();
            }
        }

        InputStream stream = null;
        if (confFile != null) {
            final String keystorePath = confFile.getParent() + "/" + KeyStoreUtils.KS_FILENAME;
            if (new File(keystorePath).exists()) {
                stream = new FileInputStream(keystorePath);
            }
        }

        final KeyStore ks = loadKeyStore(stream, passphrase);
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        TrustManager[] tms;
        if (stream != null) {
            // This enforces a two-way SSL authentication
            tms = tmf.getTrustManagers();
        } else {
            // This enforces a one-way SSL authentication
            tms = new TrustManager[]{new TrustAllManager()};
            LOGGER.warn("Failed to load keystore, using trust all manager");
        }

        if (stream != null) {
            stream.close();
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        final SSLContext sslContext = SSLUtils.getSSLContextWithLatestProtocolVersion();
        sslContext.init(kmf.getKeyManagers(), tms, new SecureRandom());
        return sslContext;
    }

    public static ByteBuffer enlargeBuffer(ByteBuffer buffer, final int sessionProposedCapacity) {
        if (buffer == null || sessionProposedCapacity < 0) {
            return buffer;
        }
        if (sessionProposedCapacity > buffer.capacity()) {
            buffer = ByteBuffer.allocate(sessionProposedCapacity);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    public static ByteBuffer handleBufferUnderflow(final SSLEngine engine, final ByteBuffer buffer) {
        if (engine == null || buffer == null) {
            return buffer;
        }
        if (buffer.position() < buffer.limit()) {
            return buffer;
        }
        ByteBuffer replaceBuffer = enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
        buffer.flip();
        replaceBuffer.put(buffer);
        return replaceBuffer;
    }

    private static HandshakeHolder doHandshakeUnwrap(final SocketChannel socketChannel, final SSLEngine sslEngine,
                                             ByteBuffer peerAppData, ByteBuffer peerNetData, final int appBufferSize) throws IOException {
        if (socketChannel == null || sslEngine == null || peerAppData == null || peerNetData == null || appBufferSize < 0) {
            return new HandshakeHolder(peerAppData, peerNetData, false);
        }
        if (socketChannel.read(peerNetData) < 0) {
            if (sslEngine.isInboundDone() && sslEngine.isOutboundDone()) {
                return new HandshakeHolder(peerAppData, peerNetData, false);
            }
            try {
                sslEngine.closeInbound();
            } catch (SSLException e) {
                LOGGER.warn("This SSL engine was forced to close inbound due to end of stream.", e);
            }
            sslEngine.closeOutbound();
            // After closeOutbound the engine will be set to WRAP state,
            // in order to try to send a close message to the client.
            return new HandshakeHolder(peerAppData, peerNetData, true);
        }
        peerNetData.flip();
        SSLEngineResult result = null;
        try {
            result = sslEngine.unwrap(peerNetData, peerAppData);
            peerNetData.compact();
        } catch (final SSLException sslException) {
            LOGGER.error(String.format("SSL error caught during unwrap data: %s, for local address=%s, remote address=%s. The client may have invalid ca-certificates.",
                    sslException.getMessage(), socketChannel.getLocalAddress(), socketChannel.getRemoteAddress()));
            sslEngine.closeOutbound();
            return new HandshakeHolder(peerAppData, peerNetData, false);
        }
        if (result == null) {
            return new HandshakeHolder(peerAppData, peerNetData, false);
        }
        switch (result.getStatus()) {
            case OK:
                break;
            case BUFFER_OVERFLOW:
                // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                peerAppData = enlargeBuffer(peerAppData, appBufferSize);
                break;
            case BUFFER_UNDERFLOW:
                // Will occur either when no data was read from the peer or when the peerNetData buffer
                // was too small to hold all peer's data.
                peerNetData = handleBufferUnderflow(sslEngine, peerNetData);
                break;
            case CLOSED:
                if (sslEngine.isOutboundDone()) {
                    return new HandshakeHolder(peerAppData, peerNetData, false);
                } else {
                    sslEngine.closeOutbound();
                }
                break;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
        }
        return new HandshakeHolder(peerAppData, peerNetData, true);
    }

    private static HandshakeHolder doHandshakeWrap(final SocketChannel socketChannel, final SSLEngine sslEngine,
                                           ByteBuffer myAppData, ByteBuffer myNetData, ByteBuffer peerNetData,
                                           final int netBufferSize) throws IOException {
        if (socketChannel == null || sslEngine == null || myNetData == null || peerNetData == null
                || myAppData == null || netBufferSize < 0) {
            return new HandshakeHolder(myAppData, myNetData, false);
        }
        myNetData.clear();
        SSLEngineResult result = null;
        try {
            result = sslEngine.wrap(myAppData, myNetData);
        } catch (final SSLException sslException) {
            LOGGER.error(String.format("SSL error caught during wrap data: %s, for local address=%s, remote address=%s.",
                    sslException.getMessage(), socketChannel.getLocalAddress(), socketChannel.getRemoteAddress()));
            sslEngine.closeOutbound();
            return new HandshakeHolder(myAppData, myNetData, true);
        }
        if (result == null) {
            return new HandshakeHolder(myAppData, myNetData, false);
        }
        switch (result.getStatus()) {
            case OK :
                myNetData.flip();
                while (myNetData.hasRemaining()) {
                    socketChannel.write(myNetData);
                }
                break;
            case BUFFER_OVERFLOW:
                // Will occur if there is not enough space in myNetData buffer to write all the data
                // that would be generated by the method wrap. Since myNetData is set to session's packet
                // size we should not get to this point because SSLEngine is supposed to produce messages
                // smaller or equal to that, but a general handling would be the following:
                myNetData = enlargeBuffer(myNetData, netBufferSize);
                break;
            case BUFFER_UNDERFLOW:
                throw new SSLException("Buffer underflow occurred after a wrap. We should not reach here.");
            case CLOSED:
                try {
                    myNetData.flip();
                    while (myNetData.hasRemaining()) {
                        socketChannel.write(myNetData);
                    }
                    // At this point the handshake status will probably be NEED_UNWRAP
                    // so we make sure that peerNetData is clear to read.
                    peerNetData.clear();
                } catch (Exception e) {
                    LOGGER.error("Failed to send server's CLOSE message due to socket channel's failure.");
                }
                break;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
        }
        return new HandshakeHolder(myAppData, myNetData, true);
    }

    public static boolean doHandshake(final SocketChannel socketChannel, final SSLEngine sslEngine) throws IOException {
        return doHandshake(socketChannel, sslEngine, null);
    }

    public static boolean doHandshake(final SocketChannel socketChannel, final SSLEngine sslEngine, Integer timeout) throws IOException {
        if (socketChannel == null || sslEngine == null) {
            return false;
        }
        final int appBufferSize = sslEngine.getSession().getApplicationBufferSize();
        final int netBufferSize = sslEngine.getSession().getPacketBufferSize();
        ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer myNetData = ByteBuffer.allocate(netBufferSize);
        ByteBuffer peerNetData = ByteBuffer.allocate(netBufferSize);

        final Executor executor = Executors.newSingleThreadExecutor();
        final long startTimeMills = System.currentTimeMillis();

        HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        long timeoutMillis = ObjectUtils.defaultIfNull(timeout, 30) * 1000L;
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
                && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            final long timeTaken = System.currentTimeMillis() - startTimeMills;

            if (timeTaken > timeoutMillis) {
                LOGGER.warn("SSL Handshake has taken more than {} ms to connect to: {}" +
                        " while status: {}. Please investigate this connection.", timeoutMillis, socketChannel.getRemoteAddress(),
                        handshakeStatus);
                return false;
            }
            switch (handshakeStatus) {
                case NEED_UNWRAP:
                    final HandshakeHolder unwrapResult = doHandshakeUnwrap(socketChannel, sslEngine, peerAppData, peerNetData, appBufferSize);
                    peerAppData = unwrapResult.getAppDataBuffer();
                    peerNetData = unwrapResult.getNetDataBuffer();
                    if (!unwrapResult.isSuccess()) {
                        return false;
                    }
                    break;
                case NEED_WRAP:
                    final HandshakeHolder wrapResult = doHandshakeWrap(socketChannel, sslEngine,  myAppData, myNetData, peerNetData, netBufferSize);
                    myNetData = wrapResult.getNetDataBuffer();
                    if (!wrapResult.isSuccess()) {
                        return false;
                    }
                    break;
                case NEED_TASK:
                    Runnable task;
                    while ((task = sslEngine.getDelegatedTask()) != null) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("SSL: Running delegated task!");
                        }
                        executor.execute(task);
                    }
                    break;
                case FINISHED:
                    break;
                case NOT_HANDSHAKING:
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
            handshakeStatus = sslEngine.getHandshakeStatus();
        }
        return true;
    }

    private static class HandshakeHolder {
        private ByteBuffer appData;
        private ByteBuffer netData;
        private boolean success = true;

        HandshakeHolder(ByteBuffer appData, ByteBuffer netData, boolean success) {
            this.appData = appData;
            this.netData = netData;
            this.success = success;
        }

        ByteBuffer getAppDataBuffer() {
            return appData;
        }

        ByteBuffer getNetDataBuffer() {
            return netData;
        }

        boolean isSuccess() {
            return success;
        }
    }

}
