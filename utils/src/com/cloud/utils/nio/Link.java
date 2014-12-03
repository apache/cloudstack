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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.DbProperties;

/**
 */
public class Link {
    private static final Logger s_logger = Logger.getLogger(Link.class);

    private final InetSocketAddress _addr;
    private final NioConnection _connection;
    private SelectionKey _key;
    private final ConcurrentLinkedQueue<ByteBuffer[]> _writeQueue;
    private ByteBuffer _readBuffer;
    private ByteBuffer _plaintextBuffer;
    private Object _attach;
    private boolean _readHeader;
    private boolean _gotFollowingPacket;

    private SSLEngine _sslEngine;
    public static String keystoreFile = "/cloud.keystore";

    public Link(InetSocketAddress addr, NioConnection connection) {
        _addr = addr;
        _connection = connection;
        _readBuffer = ByteBuffer.allocate(2048);
        _attach = null;
        _key = null;
        _writeQueue = new ConcurrentLinkedQueue<ByteBuffer[]>();
        _readHeader = true;
        _gotFollowingPacket = false;
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
    }

    /**
     * No user, so comment it out.
     *
     * Static methods for reading from a channel in case
     * you need to add a client that doesn't require nio.
     * @param ch channel to read from.
     * @param bytebuffer to use.
     * @return bytes read
     * @throws IOException if not read to completion.
    public static byte[] read(SocketChannel ch, ByteBuffer buff) throws IOException {
        synchronized(buff) {
            buff.clear();
            buff.limit(4);

            while (buff.hasRemaining()) {
                if (ch.read(buff) == -1) {
                    throw new IOException("Connection closed with -1 on reading size.");
                }
            }

            buff.flip();

            int length = buff.getInt();
            ByteArrayOutputStream output = new ByteArrayOutputStream(length);
            WritableByteChannel outCh = Channels.newChannel(output);

            int count = 0;
            while (count < length) {
                buff.clear();
                int read = ch.read(buff);
                if (read < 0) {
                    throw new IOException("Connection closed with -1 on reading data.");
                }
                count += read;
                buff.flip();
                outCh.write(buff);
            }

            return output.toByteArray();
        }
    }
     */

    private static void doWrite(SocketChannel ch, ByteBuffer[] buffers, SSLEngine sslEngine) throws IOException {
        SSLSession sslSession = sslEngine.getSession();
        ByteBuffer pkgBuf = ByteBuffer.allocate(sslSession.getPacketBufferSize() + 40);
        SSLEngineResult engResult;

        ByteBuffer headBuf = ByteBuffer.allocate(4);

        int totalLen = 0;
        for (ByteBuffer buffer : buffers) {
            totalLen += buffer.limit();
        }

        int processedLen = 0;
        while (processedLen < totalLen) {
            headBuf.clear();
            pkgBuf.clear();
            engResult = sslEngine.wrap(buffers, pkgBuf);
            if (engResult.getHandshakeStatus() != HandshakeStatus.FINISHED && engResult.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING &&
                    engResult.getStatus() != SSLEngineResult.Status.OK) {
                throw new IOException("SSL: SSLEngine return bad result! " + engResult);
            }

            processedLen = 0;
            for (ByteBuffer buffer : buffers) {
                processedLen += buffer.position();
            }

            int dataRemaining = pkgBuf.position();
            int header = dataRemaining;
            int headRemaining = 4;
            pkgBuf.flip();
            if (processedLen < totalLen) {
                header = header | HEADER_FLAG_FOLLOWING;
            }
            headBuf.putInt(header);
            headBuf.flip();

            while (headRemaining > 0) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Writing Header " + headRemaining);
                }
                long count = ch.write(headBuf);
                headRemaining -= count;
            }
            while (dataRemaining > 0) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Writing Data " + dataRemaining);
                }
                long count = ch.write(pkgBuf);
                dataRemaining -= count;
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

    /* SSL has limitation of 16k, we may need to split packets. 18000 is 16k + some extra SSL informations */
    protected static final int MAX_SIZE_PER_PACKET = 18000;
    protected static final int HEADER_FLAG_FOLLOWING = 0x10000;

    public byte[] read(SocketChannel ch) throws IOException {
        if (_readHeader) {   // Start of a packet
            if (_readBuffer.position() == 0) {
                _readBuffer.limit(4);
            }

            if (ch.read(_readBuffer) == -1) {
                throw new IOException("Connection closed with -1 on reading size.");
            }

            if (_readBuffer.hasRemaining()) {
                s_logger.trace("Need to read the rest of the packet length");
                return null;
            }
            _readBuffer.flip();
            int header = _readBuffer.getInt();
            int readSize = (short)header;
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Packet length is " + readSize);
            }

            if (readSize > MAX_SIZE_PER_PACKET) {
                throw new IOException("Wrong packet size: " + readSize);
            }

            if (!_gotFollowingPacket) {
                _plaintextBuffer = ByteBuffer.allocate(2000);
            }

            if ((header & HEADER_FLAG_FOLLOWING) != 0) {
                _gotFollowingPacket = true;
            } else {
                _gotFollowingPacket = false;
            }

            _readBuffer.clear();
            _readHeader = false;

            if (_readBuffer.capacity() < readSize) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Resizing the byte buffer from " + _readBuffer.capacity());
                }
                _readBuffer = ByteBuffer.allocate(readSize);
            }
            _readBuffer.limit(readSize);
        }

        if (ch.read(_readBuffer) == -1) {
            throw new IOException("Connection closed with -1 on read.");
        }

        if (_readBuffer.hasRemaining()) {   // We're not done yet.
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Still has " + _readBuffer.remaining());
            }
            return null;
        }

        _readBuffer.flip();

        ByteBuffer appBuf;

        SSLSession sslSession = _sslEngine.getSession();
        SSLEngineResult engResult;
        int remaining = 0;

        while (_readBuffer.hasRemaining()) {
            remaining = _readBuffer.remaining();
            appBuf = ByteBuffer.allocate(sslSession.getApplicationBufferSize() + 40);
            engResult = _sslEngine.unwrap(_readBuffer, appBuf);
            if (engResult.getHandshakeStatus() != HandshakeStatus.FINISHED && engResult.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING &&
                    engResult.getStatus() != SSLEngineResult.Status.OK) {
                throw new IOException("SSL: SSLEngine return bad result! " + engResult);
            }
            if (remaining == _readBuffer.remaining()) {
                throw new IOException("SSL: Unable to unwrap received data! still remaining " + remaining + "bytes!");
            }

            appBuf.flip();
            if (_plaintextBuffer.remaining() < appBuf.limit()) {
                // We need to expand _plaintextBuffer for more data
                ByteBuffer newBuffer = ByteBuffer.allocate(_plaintextBuffer.capacity() + appBuf.limit() * 5);
                _plaintextBuffer.flip();
                newBuffer.put(_plaintextBuffer);
                _plaintextBuffer = newBuffer;
            }
            _plaintextBuffer.put(appBuf);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Done with packet: " + appBuf.limit());
            }
        }

        _readBuffer.clear();
        _readHeader = true;

        if (!_gotFollowingPacket) {
            _plaintextBuffer.flip();
            byte[] result = new byte[_plaintextBuffer.limit()];
            _plaintextBuffer.get(result);
            return result;
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Waiting for more packets");
            }
            return null;
        }
    }

    public void send(byte[] data) throws ClosedChannelException {
        send(data, false);
    }

    public void send(byte[] data, boolean close) throws ClosedChannelException {
        send(new ByteBuffer[] {ByteBuffer.wrap(data)}, close);
    }

    public void send(ByteBuffer[] data, boolean close) throws ClosedChannelException {
        ByteBuffer[] item = new ByteBuffer[data.length + 1];
        int remaining = 0;
        for (int i = 0; i < data.length; i++) {
            remaining += data[i].remaining();
            item[i + 1] = data[i];
        }

        item[0] = ByteBuffer.allocate(4);
        item[0].putInt(remaining);
        item[0].flip();

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Sending packet of length " + remaining);
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
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Closing connection requested");
                }
                return true;
            }

            ByteBuffer[] raw_data = new ByteBuffer[data.length - 1];
            System.arraycopy(data, 1, raw_data, 0, data.length - 1);

            doWrite(ch, raw_data, _sslEngine);
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

    public static SSLContext initSSLContext(boolean isClient) throws GeneralSecurityException, IOException {
        InputStream stream;
        SSLContext sslContext = null;
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        TrustManager[] tms;

        File confFile = PropertiesUtil.findConfigFile("db.properties");
        if (null != confFile && !isClient) {
            final String pass = DbProperties.getDbProperties().getProperty("db.cloud.keyStorePassphrase");
            char[] passphrase = "vmops.com".toCharArray();
            if (pass != null) {
                passphrase = pass.toCharArray();
            }
            String confPath = confFile.getParent();
            String keystorePath = confPath + keystoreFile;
            if (new File(keystorePath).exists()) {
                stream = new FileInputStream(keystorePath);
            } else {
                s_logger.warn("SSL: Fail to find the generated keystore. Loading fail-safe one to continue.");
                stream = NioConnection.class.getResourceAsStream("/cloud.keystore");
                passphrase = "vmops.com".toCharArray();
            }
            ks.load(stream, passphrase);
            stream.close();
            kmf.init(ks, passphrase);
            tmf.init(ks);
            tms = tmf.getTrustManagers();
        } else {
            ks.load(null, null);
            kmf.init(ks, null);
            tms = new TrustManager[1];
            tms[0] = new TrustAllManager();
        }

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tms, null);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("SSL: SSLcontext has been initialized");
        }

        return sslContext;
    }

    public static void doHandshake(SocketChannel ch, SSLEngine sslEngine, boolean isClient) throws IOException {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("SSL: begin Handshake, isClient: " + isClient);
        }

        SSLEngineResult engResult;
        SSLSession sslSession = sslEngine.getSession();
        HandshakeStatus hsStatus;
        ByteBuffer in_pkgBuf = ByteBuffer.allocate(sslSession.getPacketBufferSize() + 40);
        ByteBuffer in_appBuf = ByteBuffer.allocate(sslSession.getApplicationBufferSize() + 40);
        ByteBuffer out_pkgBuf = ByteBuffer.allocate(sslSession.getPacketBufferSize() + 40);
        ByteBuffer out_appBuf = ByteBuffer.allocate(sslSession.getApplicationBufferSize() + 40);
        int count;
        ch.socket().setSoTimeout(10 * 1000);
        InputStream inStream = ch.socket().getInputStream();
        // Use readCh to make sure the timeout on reading is working
        ReadableByteChannel readCh = Channels.newChannel(inStream);

        if (isClient) {
            hsStatus = SSLEngineResult.HandshakeStatus.NEED_WRAP;
        } else {
            hsStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
        }

        while (hsStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("SSL: Handshake status " + hsStatus);
            }
            engResult = null;
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                out_pkgBuf.clear();
                out_appBuf.clear();
                out_appBuf.put("Hello".getBytes());
                engResult = sslEngine.wrap(out_appBuf, out_pkgBuf);
                out_pkgBuf.flip();
                int remain = out_pkgBuf.limit();
                while (remain != 0) {
                    remain -= ch.write(out_pkgBuf);
                    if (remain < 0) {
                        throw new IOException("Too much bytes sent?");
                    }
                }
            } else if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                in_appBuf.clear();
                // One packet may contained multiply operation
                if (in_pkgBuf.position() == 0 || !in_pkgBuf.hasRemaining()) {
                    in_pkgBuf.clear();
                    count = 0;
                    try {
                        count = readCh.read(in_pkgBuf);
                    } catch (SocketTimeoutException ex) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Handshake reading time out! Cut the connection");
                        }
                        count = -1;
                    }
                    if (count == -1) {
                        throw new IOException("Connection closed with -1 on reading size.");
                    }
                    in_pkgBuf.flip();
                }
                engResult = sslEngine.unwrap(in_pkgBuf, in_appBuf);
                ByteBuffer tmp_pkgBuf = ByteBuffer.allocate(sslSession.getPacketBufferSize() + 40);
                int loop_count = 0;
                while (engResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    // The client is too slow? Cut it and let it reconnect
                    if (loop_count > 10) {
                        throw new IOException("Too many times in SSL BUFFER_UNDERFLOW, disconnect guest.");
                    }
                    // We need more packets to complete this operation
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("SSL: Buffer underflowed, getting more packets");
                    }
                    tmp_pkgBuf.clear();
                    count = ch.read(tmp_pkgBuf);
                    if (count == -1) {
                        throw new IOException("Connection closed with -1 on reading size.");
                    }
                    tmp_pkgBuf.flip();

                    in_pkgBuf.mark();
                    in_pkgBuf.position(in_pkgBuf.limit());
                    in_pkgBuf.limit(in_pkgBuf.limit() + tmp_pkgBuf.limit());
                    in_pkgBuf.put(tmp_pkgBuf);
                    in_pkgBuf.reset();

                    in_appBuf.clear();
                    engResult = sslEngine.unwrap(in_pkgBuf, in_appBuf);
                    loop_count++;
                }
            } else if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                Runnable run;
                while ((run = sslEngine.getDelegatedTask()) != null) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("SSL: Running delegated task!");
                    }
                    run.run();
                }
            } else if (hsStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                throw new IOException("NOT a handshaking!");
            }
            if (engResult != null && engResult.getStatus() != SSLEngineResult.Status.OK) {
                throw new IOException("Fail to handshake! " + engResult.getStatus());
            }
            if (engResult != null)
                hsStatus = engResult.getHandshakeStatus();
            else
                hsStatus = sslEngine.getHandshakeStatus();
        }
    }

}
