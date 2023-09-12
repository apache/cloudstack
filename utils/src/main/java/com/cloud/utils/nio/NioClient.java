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

    protected String _host;
    protected SocketChannel _clientConnection;

    public NioClient(final String name, final String host, final int port, final int workers, final HandlerFactory factory) {
        super(name, port, workers, factory);
        _host = host;
    }

    @Override
    protected void init() throws IOException {
        _selector = Selector.open();
        Task task = null;

        try {
            _clientConnection = SocketChannel.open();

            s_logger.info("Connecting to " + _host + ":" + _port);
            final InetSocketAddress peerAddr = new InetSocketAddress(_host, _port);
            _clientConnection.connect(peerAddr);
            _clientConnection.configureBlocking(false);

            final SSLContext sslContext = Link.initClientSSLContext();
            SSLEngine sslEngine = sslContext.createSSLEngine(_host, _port);
            sslEngine.setUseClientMode(true);
            sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));
            sslEngine.beginHandshake();
            if (!Link.doHandshake(_clientConnection, sslEngine)) {
                s_logger.error("SSL Handshake failed while connecting to host: " + _host + " port: " + _port);
                _selector.close();
                throw new IOException("SSL Handshake failed while connecting to host: " + _host + " port: " + _port);
            }
            s_logger.info("SSL: Handshake done");
            s_logger.info("Connected to " + _host + ":" + _port);

            final Link link = new Link(peerAddr, this);
            link.setSSLEngine(sslEngine);
            final SelectionKey key = _clientConnection.register(_selector, SelectionKey.OP_READ);
            link.setKey(key);
            key.attach(link);
            // Notice we've already connected due to the handshake, so let's get the
            // remaining task done
            task = _factory.create(Task.Type.CONNECT, link, null);
        } catch (final GeneralSecurityException e) {
            _selector.close();
            throw new IOException("Failed to initialise security", e);
        } catch (final IOException e) {
            _selector.close();
            throw e;
        }
        _executor.submit(task);
    }

    @Override
    protected void registerLink(final InetSocketAddress saddr, final Link link) {
        // don't do anything.
    }

    @Override
    protected void unregisterLink(final InetSocketAddress saddr) {
        // don't do anything.
    }

    @Override
    public void cleanUp() throws IOException {
        super.cleanUp();
        if (_clientConnection != null) {
            _clientConnection.close();
        }
        s_logger.info("NioClient connection closed");
    }
}
