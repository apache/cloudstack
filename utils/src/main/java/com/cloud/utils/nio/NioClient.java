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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.log4j.Logger;

public class NioClient extends NioConnection {
    private static final Logger s_logger = Logger.getLogger(NioClient.class);

    protected String host;
    protected SocketChannel clientConnection;

    public NioClient(final String name, final String host, final int port, final int workers, final Integer sslHandshakeTimeout, final HandlerFactory factory) {
        super(name, port, workers, 1, 2, factory);
        setSslHandshakeTimeout(sslHandshakeTimeout);
        this.host = host;
    }

    protected void closeChannelAndSelector() {
        try {
            if (clientConnection != null && clientConnection.isOpen()) {
                clientConnection.close();
                clientConnection = null;
            }
        } catch (IOException e) {
            s_logger.error("Failed to close SocketChannel", e);
        }
        try {
            if (_selector != null && _selector.isOpen()) {
                _selector.close();
                _selector = null;
            }
        } catch (IOException e) {
            s_logger.error("Failed to close Selector", e);
        }
    }

    @Override
    protected void init() throws IOException {
        Task task;
        String hostLog = host + ":" + _port;
        try {
            s_logger.info(String.format("Connecting to %s", hostLog));
            _selector = Selector.open();
            clientConnection = SocketChannel.open();
            final InetSocketAddress serverAddress = new InetSocketAddress(host, _port);
            clientConnection.connect(serverAddress);
            s_logger.info(String.format("Connected to %s", hostLog));
            clientConnection.configureBlocking(false);

            final SSLContext sslContext = Link.initClientSSLContext();
            SSLEngine sslEngine = sslContext.createSSLEngine(host, _port);
            sslEngine.setUseClientMode(true);
            sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));
            sslEngine.beginHandshake();
            if (!Link.doHandshake(clientConnection, sslEngine, getSslHandshakeTimeout())) {
                throw new IOException(String.format("SSL Handshake failed while connecting to host: %s", hostLog));
            }
            s_logger.info("SSL: Handshake done");

            final Link link = new Link(serverAddress, this);
            link.setSSLEngine(sslEngine);
            final SelectionKey key = clientConnection.register(_selector, SelectionKey.OP_READ);
            link.setKey(key);
            key.attach(link);
            // Notice we've already connected due to the handshake, so let's get the
            // remaining task done
            task = _factory.create(Task.Type.CONNECT, link, null);
        } catch (final GeneralSecurityException e) {
            closeChannelAndSelector();
            throw new IOException("Failed to initialise security", e);
        } catch (final IOException e) {
            closeChannelAndSelector();
            s_logger.error(String.format("IOException while connecting to %s", hostLog), e);
            throw e;
        }
        if (task != null) {
            _executor.submit(task);
        }
    }

    @Override
    protected void registerLink(final InetSocketAddress address, final Link link) {
        // don't do anything.
    }

    @Override
    protected void unregisterLink(final InetSocketAddress address) {
        // don't do anything.
    }

    @Override
    public void cleanUp() throws IOException {
        super.cleanUp();
        if (clientConnection != null && clientConnection.isOpen()) {
            clientConnection.close();
            clientConnection = null;
        }
        s_logger.info("NioClient connection closed");
    }
}
