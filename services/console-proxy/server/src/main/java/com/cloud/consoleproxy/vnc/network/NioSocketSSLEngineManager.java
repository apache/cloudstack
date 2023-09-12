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
package com.cloud.consoleproxy.vnc.network;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NioSocketSSLEngineManager {

    private final SSLEngine engine;

    private final ByteBuffer myNetData;
    private final ByteBuffer peerNetData;

    private final Executor executor;
    private final NioSocketInputStream inputStream;
    private final NioSocketOutputStream outputStream;

    public NioSocketSSLEngineManager(SSLEngine sslEngine, NioSocketHandler socket) {
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        engine = sslEngine;

        executor = Executors.newSingleThreadExecutor();

        int pktBufSize = engine.getSession().getPacketBufferSize();
        myNetData = ByteBuffer.allocate(pktBufSize);
        peerNetData = ByteBuffer.allocate(pktBufSize);
    }

    private void handshakeNeedUnwrap(ByteBuffer peerAppData) throws SSLException {
        peerNetData.flip();
        SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
        peerNetData.compact();

        switch (result.getStatus()) {
            case BUFFER_UNDERFLOW:
                int avail = inputStream.getReadBytesAvailableToFitSize(1, peerNetData.remaining(),
                        false);
                inputStream.readBytes(peerNetData, avail);
                break;
            case OK:
            case BUFFER_OVERFLOW:
                break;
            case CLOSED:
                engine.closeInbound();
                break;
        }
    }

    private void handshakeNeedWrap(ByteBuffer myAppData) throws SSLException {
        SSLEngineResult result = engine.wrap(myAppData, myNetData);

        switch (result.getStatus()) {
            case OK:
                myNetData.flip();
                outputStream.writeBytes(myNetData, myNetData.remaining());
                outputStream.flushWriteBuffer();
                myNetData.compact();
                break;
            case CLOSED:
                engine.closeOutbound();
                break;
            case BUFFER_OVERFLOW:
            case BUFFER_UNDERFLOW:
                break;
        }
    }

    private void handleHandshakeStatus(SSLEngineResult.HandshakeStatus handshakeStatus,
                                       ByteBuffer peerAppData, ByteBuffer myAppData) throws SSLException {
        switch (handshakeStatus) {
            case NEED_UNWRAP:
            case NEED_UNWRAP_AGAIN:
                handshakeNeedUnwrap(peerAppData);
                break;

            case NEED_WRAP:
                handshakeNeedWrap(myAppData);
                break;

            case NEED_TASK:
                executeTasks();
                break;

            case FINISHED:
            case NOT_HANDSHAKING:
                break;
        }
    }

    public void doHandshake() throws SSLException {
        engine.beginHandshake();
        SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();

        int appBufSize = engine.getSession().getApplicationBufferSize();
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufSize);
        ByteBuffer myAppData = ByteBuffer.allocate(appBufSize);

        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
                handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            handleHandshakeStatus(handshakeStatus, peerAppData, myAppData);
            handshakeStatus = engine.getHandshakeStatus();
        }
    }

    private void executeTasks() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            executor.execute(task);
        }
    }

    public int read(ByteBuffer data) throws IOException {
        peerNetData.flip();
        SSLEngineResult result = engine.unwrap(peerNetData, data);
        peerNetData.compact();

        switch (result.getStatus()) {
            case OK :
                return result.bytesProduced();
            case BUFFER_UNDERFLOW:
                // attempt to drain the underlying buffer first
                int need = peerNetData.remaining();
                int available = inputStream.getReadBytesAvailableToFitSize(1, need, false);
                inputStream.readBytes(peerNetData, available);
                break;
            case CLOSED:
                engine.closeInbound();
                break;
            case BUFFER_OVERFLOW:
                break;
        }
        return 0;
    }

    public int write(ByteBuffer data) throws IOException {
        int n = 0;
        while (data.hasRemaining()) {
            SSLEngineResult result = engine.wrap(data, myNetData);
            n += result.bytesConsumed();
            switch (result.getStatus()) {
                case OK:
                    myNetData.flip();
                    outputStream.writeBytes(myNetData, myNetData.remaining());
                    outputStream.flushWriteBuffer();
                    myNetData.compact();
                    break;

                case BUFFER_OVERFLOW:
                    myNetData.flip();
                    outputStream.writeBytes(myNetData, myNetData.remaining());
                    myNetData.compact();
                    break;

                case CLOSED:
                    engine.closeOutbound();
                    break;

                case BUFFER_UNDERFLOW:
                    break;
            }
        }
        return n;
    }

    public SSLSession getSession() {
        return engine.getSession();
    }

}
