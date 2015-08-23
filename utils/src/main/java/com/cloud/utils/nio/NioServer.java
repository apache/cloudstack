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
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

public class NioServer extends NioConnection {
    private final static Logger s_logger = Logger.getLogger(NioServer.class);

    protected InetSocketAddress _localAddr;
    private ServerSocketChannel _serverSocket;

    protected WeakHashMap<InetSocketAddress, Link> _links;

    public NioServer(String name, int port, int workers, HandlerFactory factory) {
        super(name, port, workers, factory);
        _localAddr = null;
        _links = new WeakHashMap<InetSocketAddress, Link>(1024);
    }

    @Override
    protected void init() throws IOException {
        _selector = SelectorProvider.provider().openSelector();

        _serverSocket = ServerSocketChannel.open();
        _serverSocket.configureBlocking(false);

        _localAddr = new InetSocketAddress(_port);
        _serverSocket.socket().bind(_localAddr);

        _serverSocket.register(_selector, SelectionKey.OP_ACCEPT, null);

        s_logger.info("NioConnection started and listening on " + _localAddr.toString());
    }

    @Override
    public void cleanUp() throws IOException {
        super.cleanUp();
        if (_serverSocket != null) {
            _serverSocket.close();
        }
        s_logger.info("NioConnection stopped on " + _localAddr.toString());
    }

    @Override
    protected void registerLink(InetSocketAddress addr, Link link) {
        _links.put(addr, link);
    }

    @Override
    protected void unregisterLink(InetSocketAddress saddr) {
        _links.remove(saddr);
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
    public Object send(InetSocketAddress saddr, byte[] data) throws ClosedChannelException {
        Link link = _links.get(saddr);
        if (link == null) {
            return null;
        }
        link.send(data);
        return link.attachment();
    }
}
