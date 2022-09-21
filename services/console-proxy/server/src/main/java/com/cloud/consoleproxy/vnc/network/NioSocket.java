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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioSocket {

    private SocketChannel socketChannel;
    private Selector writeSelector;
    private Selector readSelector;

    private static final Logger s_logger = Logger.getLogger(NioSocket.class);

    private void initializeSocket() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setSoTimeout(5000);
            writeSelector = Selector.open();
            readSelector = Selector.open();
            socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
            socketChannel.register(readSelector, SelectionKey.OP_READ);
        } catch (IOException e) {
            s_logger.error("Could not initialize NioSocket: " + e.getMessage(), e);
        }
    }

    private void connectSocket(String host, int port) {
        try {
            socketChannel.connect(new InetSocketAddress(host, port));
            Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            boolean connected = false;
            while (selector.select(3000) > 0) {
                while (!connected) {
                    Set keys = selector.selectedKeys();
                    Iterator i = keys.iterator();

                    while (i.hasNext()) {
                        SelectionKey key = (SelectionKey)i.next();

                        // Remove the current key
                        i.remove();

                        // Attempt a connection
                        if (key.isConnectable()) {
                            if (socketChannel.isConnectionPending()) {
                                socketChannel.finishConnect();
                            }
                            connected = true;
                        }
                    }
                }
            }
            socketChannel.socket().setTcpNoDelay(false);
        } catch (IOException e) {
            s_logger.error(String.format("Error creating NioSocket to %s:%s: %s", host, port, e.getMessage()), e);
        }
    }

    public NioSocket(String host, int port) {
        initializeSocket();
        connectSocket(host, port);
    }

    protected int select(boolean read, Integer timeout) {
        try {
            Selector selector = read ? readSelector : writeSelector;
            selector.selectedKeys().clear();
            return timeout == null ? selector.select() : selector.selectNow();
        } catch (IOException e) {
            s_logger.error(String.format("Error obtaining %s select: %s", read ? "read" : "write", e.getMessage()), e);
            return -1;
        }
    }

    protected int readFromSocketChannel(ByteBuffer readBuffer, int len) {
        try {
            int readBytes = socketChannel.read(readBuffer.slice().limit(len));
            int position = readBuffer.position();
            readBuffer.position(position + readBytes);
            return Math.max(readBytes, 0);
        } catch (Exception e) {
            s_logger.error("Error reading from socket channel: " + e.getMessage(), e);
            return 0;
        }
    }

    protected int writeToSocketChannel(ByteBuffer buf, int len) {
        try {
            int writtenBytes = socketChannel.write(buf.slice().limit(len));
            buf.position(buf.position() + writtenBytes);
            return writtenBytes;
        } catch (java.io.IOException e) {
            s_logger.error("Error writing bytes to socket channel: " + e.getMessage(), e);
            return 0;
        }
    }
}
