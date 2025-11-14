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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.framework.ca.CAService;

public class NioServer extends NioConnection {

    protected InetSocketAddress localAddress;
    private ServerSocketChannel serverSocket;

    protected ConcurrentHashMap<InetSocketAddress, Link> links;

    public NioServer(final String name, final int port, final int workers, final HandlerFactory factory,
             final CAService caService, final Integer sslHandShakeTimeout) {
        super(name, port, workers, factory);
        setCAService(caService);
        setSslHandshakeTimeout(sslHandShakeTimeout);
        localAddress = null;
        links = new ConcurrentHashMap<>(1024);
    }

    public int getPort() {
        return serverSocket.socket().getLocalPort();
    }

    @Override
    protected void init() throws IOException {
        _selector = SelectorProvider.provider().openSelector();

        serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);

        localAddress = new InetSocketAddress(_port);
        serverSocket.socket().bind(localAddress);

        serverSocket.register(_selector, SelectionKey.OP_ACCEPT, null);

        logger.info("NioServer started and listening on {}", serverSocket.socket().getLocalSocketAddress());
    }

    @Override
    public void cleanUp() throws IOException {
        super.cleanUp();
        if (serverSocket != null && serverSocket.isOpen()) {
            serverSocket.close();
        }
        logger.info("NioConnection stopped on {}",  localAddress.toString());
    }

    @Override
    protected void registerLink(final InetSocketAddress addr, final Link link) {
        links.put(addr, link);
    }

    @Override
    protected void unregisterLink(final InetSocketAddress saddr) {
        links.remove(saddr);
    }

    /**
     * Sends the data to the address specified.  If address is not already
     * connected, this does nothing and returns null.  If address is already
     * connected, then it returns the attached object so the caller can
     * prepare for any responses.
     * @param saddr
     * @param data
     * @return null if not sent.  attach object in link if sent.
     */
    public Object send(final InetSocketAddress saddr, final byte[] data) throws ClosedChannelException {
        final Link link = links.get(saddr);
        if (link == null) {
            return null;
        }
        link.send(data);
        return link.attachment();
    }
}
