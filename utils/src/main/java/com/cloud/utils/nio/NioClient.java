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
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.cloud.utils.LogUtils;
import org.apache.cloudstack.utils.security.SSLUtils;

/**
 * NIO Client for the {@code com.cloud.agent.Agent}.
 */
public class NioClient extends NioConnection {

    protected String host;
    protected SocketChannel clientConnection;

    public NioClient(final String name, final String host, final int port, final int workers,
             final Integer sslHandshakeTimeout, final HandlerFactory factory) {
        super(name, port, workers, factory);
        setSslHandshakeTimeout(sslHandshakeTimeout);
        this.host = host;
    }

    @Override
    protected void init() throws IOException {
        Task task;
        String hostLog = LogUtils.getHostLog(host, _port);
        try {
            logger.info("Connecting to {}", hostLog);
            _selector = Selector.open();
            clientConnection = SocketChannel.open();
            clientConnection.socket().setKeepAlive(true);

            final InetSocketAddress serverAddress = new InetSocketAddress(host, _port);
            boolean isConnected = clientConnection.connect(serverAddress);
            logger.info("Connected to {}: {}", hostLog, isConnected);

            clientConnection.configureBlocking(false);

            logger.debug("Initializing client SSL context");
            final SSLContext sslContext = Link.initClientSSLContext();
            logger.debug("Initialized client SSL context");

            logger.debug("Creating SSL Engine for {}", hostLog);
            SSLEngine sslEngine = sslContext.createSSLEngine(host, _port);
            sslEngine.setUseClientMode(true);
            logger.debug("Created SSL Engine for {}", hostLog);
            String[] enabledProtocols = SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols());
            logger.debug("Enabled SSL Engine protocols for {}: {}", hostLog, Arrays.asList(enabledProtocols));
            sslEngine.setEnabledProtocols(enabledProtocols);

            Integer sshHandshakeTimeout = getSslHandshakeTimeout();
            logger.debug("Begin SSL Handshake for {} with timeout {}s (default 30s)", hostLog, sshHandshakeTimeout);
            sslEngine.beginHandshake();
            if (!Link.doHandshake(clientConnection, sslEngine, sshHandshakeTimeout)) {
                throw new IOException(String.format("SSL Handshake failed while connecting to host: %s", hostLog));
            }
            logger.info("SSL Handshake done for {}", hostLog);

            final Link link = new Link(serverAddress, this);
            link.setSSLEngine(sslEngine);
            final SelectionKey key = clientConnection.register(_selector, SelectionKey.OP_READ);
            link.setKey(key);
            key.attach(link);

            logger.info("Creating task {} for {}", Task.Type.CONNECT, hostLog);
            // Notice we've already connected due to the handshake, so let's get the remaining task done
            task = _factory.create(Task.Type.CONNECT, link, null);
            logger.info("Created task {} for {}", Task.Type.CONNECT, hostLog);
        } catch (GeneralSecurityException e) {
            cleanUp();
            throw new IOException(String.format("Exception while connecting to %s", hostLog), e);
        } catch (Exception e) {
            cleanUp();
            throw e;
        }
        if (task != null) {
            logger.info("Submit task {} for {}", task.getType(), hostLog);
            _executor.submit(task);
            logger.info("Submitted task {} for {}", task.getType(), hostLog);
        } else {
            logger.info("Task is null, nothing to submit for {}", hostLog);
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
        Optional<Selector> selectorOptional = Optional.ofNullable(_selector);
        selectorOptional
                .filter(Selector::isOpen)
                .map(selector -> {
                    Set<SelectionKey> keys;
                    try {
                        logger.trace("Getting keys from Selector");
                        keys = selector.keys();
                        logger.trace("Got {} keys from Selector", keys.size());
                    } catch (ClosedSelectorException e) {
                        logger.trace("Failed to get keys from Selector", e);
                        keys = Set.of();
                    }
                    return keys;
                })
                .orElseGet(Set::of)
                .forEach(key -> {
                    try {
                        if (key.isValid()) {
                            logger.trace("Cancelling SelectionKey");
                            key.cancel();
                            logger.trace("Cancelled SelectionKey");
                        } else {
                            logger.trace("SelectionKey already cancelled");
                        }
                    } catch (CancelledKeyException e) {
                        logger.trace("Failed to cancel SelectionKey", e);
                    }
                    Optional.ofNullable(key.channel())
                            .filter(SelectableChannel::isOpen)
                            .ifPresent(channel -> {
                                try {
                                    logger.trace("Closing SelectableChannel");
                                    channel.close();
                                    logger.trace("Closed SelectableChannel");
                                } catch (IOException e) {
                                    logger.trace("Failed to close SelectableChannel", e);
                                }
                            });
                });

        selectorOptional.ifPresent(selector -> {
            try {
                logger.trace("Closing Selector");
                selector.close();
                logger.trace("Closed Selector");
            } catch (IOException e) {
                logger.trace("Failed to close Selector", e);
            }
        });

        // socket channel should be closed here already, but just in case
        Optional.ofNullable(clientConnection)
                .filter(SocketChannel::isOpen)
                .ifPresent(socketChannel -> {
                    SocketAddress address;
                    try {
                        address = socketChannel.getRemoteAddress();
                    } catch (IOException e) {
                        logger.trace("Failed to get SocketAddress from SocketChannel", e);
                        address = null;
                    }
                    try {
                        socketChannel.shutdownOutput();
                    } catch (IOException e) {
                        logger.trace("Failed to shutdown output in SocketChannel", e);
                    }
                    try {
                        socketChannel.shutdownInput();
                    } catch (IOException e) {
                        logger.trace("Failed to shutdown input in SocketChannel", e);
                    }
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        logger.trace("Failed to close SocketChannel", e);
                    }
                    logger.info("NioClient connection to {} closed", address);
                });
    }

    public String getHost() {
        return host;
    }
}
