/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
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
    
    protected WeakHashMap<InetSocketAddress, Link> _links;
    
    public NioServer(String name, int port, int workers, HandlerFactory factory) {
        super(name, port, workers, factory);
        _localAddr = null;
        _links = new WeakHashMap<InetSocketAddress, Link>(1024);
    }
    
    @Override
    protected void init() throws IOException {
        _selector = SelectorProvider.provider().openSelector();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        _localAddr = new InetSocketAddress(_port);
        ssc.socket().bind(_localAddr);

        ssc.register(_selector, SelectionKey.OP_ACCEPT, null);
        
        s_logger.info("NioConnection started and listening on " + _localAddr.toString());
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
