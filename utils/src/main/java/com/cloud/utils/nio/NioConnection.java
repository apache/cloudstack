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

import static com.cloud.utils.AutoCloseableUtil.closeAutoCloseable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import org.apache.cloudstack.utils.security.SSLUtils;

import com.cloud.utils.concurrency.NamedThreadFactory;

/**
 * NioConnection abstracts the NIO socket operations.  The Java implementation
 * provides that.
 */
public abstract class NioConnection implements Runnable {
    private static final Logger s_logger = Logger.getLogger(NioConnection.class);;

    protected Selector _selector;
    protected Thread _thread;
    protected boolean _isRunning;
    protected boolean _isStartup;
    protected int _port;
    protected List<ChangeRequest> _todos;
    protected HandlerFactory _factory;
    protected String _name;
    protected ExecutorService _executor;

    public NioConnection(String name, int port, int workers, HandlerFactory factory) {
        _name = name;
        _isRunning = false;
        _thread = null;
        _selector = null;
        _port = port;
        _factory = factory;
        _executor = new ThreadPoolExecutor(workers, 5 * workers, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(name + "-Handler"));
    }

    public void start() {
        _todos = new ArrayList<ChangeRequest>();

        _thread = new Thread(this, _name + "-Selector");
        _isRunning = true;
        _thread.start();
        // Wait until we got init() done
        synchronized (_thread) {
            try {
                _thread.wait();
            } catch (InterruptedException e) {
                s_logger.warn("Interrupted start thread ", e);
            }
        }
    }

    public void stop() {
        _executor.shutdown();
        _isRunning = false;
        if (_thread != null) {
            _thread.interrupt();
        }
    }

    public boolean isRunning() {
        return _thread.isAlive();
    }

    public boolean isStartup() {
        return _isStartup;
    }

    @Override
    public void run() {
        synchronized (_thread) {
            try {
                init();
            } catch (ConnectException e) {
                s_logger.warn("Unable to connect to remote: is there a server running on port " + _port);
                return;
            } catch (IOException e) {
                s_logger.error("Unable to initialize the threads.", e);
                return;
            } catch (Exception e) {
                s_logger.error("Unable to initialize the threads due to unknown exception.", e);
                return;
            }
            _isStartup = true;
            _thread.notifyAll();
        }

        while (_isRunning) {
            try {
                _selector.select();

                // Someone is ready for I/O, get the ready keys
                Set<SelectionKey> readyKeys = _selector.selectedKeys();
                Iterator<SelectionKey> i = readyKeys.iterator();

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Keys Processing: " + readyKeys.size());
                }
                // Walk through the ready keys collection.
                while (i.hasNext()) {
                    SelectionKey sk = i.next();
                    i.remove();

                    if (!sk.isValid()) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Selection Key is invalid: " + sk.toString());
                        }
                        Link link = (Link)sk.attachment();
                        if (link != null) {
                            link.terminated();
                        } else {
                            closeConnection(sk);
                        }
                    } else if (sk.isReadable()) {
                        read(sk);
                    } else if (sk.isWritable()) {
                        write(sk);
                    } else if (sk.isAcceptable()) {
                        accept(sk);
                    } else if (sk.isConnectable()) {
                        connect(sk);
                    }
                }

                s_logger.trace("Keys Done Processing.");

                processTodos();
            } catch (Throwable e) {
                s_logger.warn("Caught an exception but continuing on.", e);
            }
        }
        synchronized (_thread) {
            _isStartup = false;
        }
    }

    abstract void init() throws IOException;

    abstract void registerLink(InetSocketAddress saddr, Link link);

    abstract void unregisterLink(InetSocketAddress saddr);

    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socket.setKeepAlive(true);

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Connection accepted for " + socket);
        }

        // Begin SSL handshake in BLOCKING mode
        socketChannel.configureBlocking(true);

        SSLEngine sslEngine = null;
        try {
            SSLContext sslContext = Link.initSSLContext(false);
            sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);
            sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));

            Link.doHandshake(socketChannel, sslEngine, false);

        } catch (Exception e) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Socket " + socket + " closed on read.  Probably -1 returned: " + e.getMessage());
            }
            closeAutoCloseable(socketChannel, "accepting socketChannel");
            closeAutoCloseable(socket, "opened socket");
            return;
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("SSL: Handshake done");
        }
        socketChannel.configureBlocking(false);
        InetSocketAddress saddr = (InetSocketAddress)socket.getRemoteSocketAddress();
        Link link = new Link(saddr, this);
        link.setSSLEngine(sslEngine);
        link.setKey(socketChannel.register(key.selector(), SelectionKey.OP_READ, link));
        Task task = _factory.create(Task.Type.CONNECT, link, null);
        registerLink(saddr, link);
        _executor.execute(task);
    }

    protected void terminate(SelectionKey key) {
        Link link = (Link)key.attachment();
        closeConnection(key);
        if (link != null) {
            link.terminated();
            Task task = _factory.create(Task.Type.DISCONNECT, link, null);
            unregisterLink(link.getSocketAddress());
            _executor.execute(task);
        }
    }

    protected void read(SelectionKey key) throws IOException {
        Link link = (Link)key.attachment();
        try {
            SocketChannel socketChannel = (SocketChannel)key.channel();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Reading from: " + socketChannel.socket().toString());
            }
            byte[] data = link.read(socketChannel);
            if (data == null) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Packet is incomplete.  Waiting for more.");
                }
                return;
            }
            Task task = _factory.create(Task.Type.DATA, link, data);
            _executor.execute(task);
        } catch (Exception e) {
            logDebug(e, key, 1);
            terminate(key);
        }
    }

    protected void logTrace(Exception e, SelectionKey key, int loc) {
        if (s_logger.isTraceEnabled()) {
            Socket socket = null;
            if (key != null) {
                SocketChannel ch = (SocketChannel)key.channel();
                if (ch != null) {
                    socket = ch.socket();
                }
            }

            s_logger.trace("Location " + loc + ": Socket " + socket + " closed on read.  Probably -1 returned.");
        }
    }

    protected void logDebug(Exception e, SelectionKey key, int loc) {
        if (s_logger.isDebugEnabled()) {
            Socket socket = null;
            if (key != null) {
                SocketChannel ch = (SocketChannel)key.channel();
                if (ch != null) {
                    socket = ch.socket();
                }
            }

            s_logger.debug("Location " + loc + ": Socket " + socket + " closed on read.  Probably -1 returned: " + e.getMessage());
        }
    }

    protected void processTodos() {
        List<ChangeRequest> todos;
        if (_todos.size() == 0) {
            return;             // Nothing to do.
        }

        synchronized (this) {
            todos = _todos;
            _todos = new ArrayList<ChangeRequest>();
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Todos Processing: " + todos.size());
        }
        SelectionKey key;
        for (ChangeRequest todo : todos) {
            switch (todo.type) {
                case ChangeRequest.CHANGEOPS:
                    try {
                        key = (SelectionKey)todo.key;
                        if (key != null && key.isValid()) {
                            if (todo.att != null) {
                                key.attach(todo.att);
                                Link link = (Link)todo.att;
                                link.setKey(key);
                            }
                            key.interestOps(todo.ops);
                        }
                    } catch (CancelledKeyException e) {
                        s_logger.debug("key has been cancelled");
                    }
                    break;
                case ChangeRequest.REGISTER:
                    try {
                        key = ((SocketChannel)(todo.key)).register(_selector, todo.ops, todo.att);
                        if (todo.att != null) {
                            Link link = (Link)todo.att;
                            link.setKey(key);
                        }
                    } catch (ClosedChannelException e) {
                        s_logger.warn("Couldn't register socket: " + todo.key);
                        try {
                            ((SocketChannel)(todo.key)).close();
                        } catch (IOException ignore) {
                            s_logger.info("[ignored] socket channel");
                        } finally {
                            Link link = (Link)todo.att;
                            link.terminated();
                        }
                    }
                    break;
                case ChangeRequest.CLOSE:
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Trying to close " + todo.key);
                    }
                    key = (SelectionKey)todo.key;
                    closeConnection(key);
                    if (key != null) {
                        Link link = (Link)key.attachment();
                        if (link != null) {
                            link.terminated();
                        }
                    }
                    break;
                default:
                    s_logger.warn("Shouldn't be here");
                    throw new RuntimeException("Shouldn't be here");
            }
        }
        s_logger.trace("Todos Done processing");
    }

    protected void connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        try {
            socketChannel.finishConnect();
            key.interestOps(SelectionKey.OP_READ);
            Socket socket = socketChannel.socket();
            if (!socket.getKeepAlive()) {
                socket.setKeepAlive(true);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Connected to " + socket);
            }
            Link link = new Link((InetSocketAddress)socket.getRemoteSocketAddress(), this);
            link.setKey(key);
            key.attach(link);
            Task task = _factory.create(Task.Type.CONNECT, link, null);
            _executor.execute(task);
        } catch (IOException e) {
            logTrace(e, key, 2);
            terminate(key);
        }
    }

    protected void scheduleTask(Task task) {
        _executor.execute(task);
    }

    protected void write(SelectionKey key) throws IOException {
        Link link = (Link)key.attachment();
        try {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Writing to " + link.getSocketAddress().toString());
            }
            boolean close = link.write((SocketChannel)key.channel());
            if (close) {
                closeConnection(key);
                link.terminated();
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (Exception e) {
            logDebug(e, key, 3);
            terminate(key);
        }
    }

    protected void closeConnection(SelectionKey key) {
        if (key != null) {
            SocketChannel channel = (SocketChannel)key.channel();
            key.cancel();
            try {
                if (channel != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Closing socket " + channel.socket());
                    }
                    channel.close();
                }
            } catch (IOException ignore) {
                s_logger.info("[ignored] channel");
            }
        }
    }

    public void register(int ops, SocketChannel key, Object att) {
        ChangeRequest todo = new ChangeRequest(key, ChangeRequest.REGISTER, ops, att);
        synchronized (this) {
            _todos.add(todo);
        }
        _selector.wakeup();
    }

    public void change(int ops, SelectionKey key, Object att) {
        ChangeRequest todo = new ChangeRequest(key, ChangeRequest.CHANGEOPS, ops, att);
        synchronized (this) {
            _todos.add(todo);
        }
        _selector.wakeup();
    }

    public void close(SelectionKey key) {
        ChangeRequest todo = new ChangeRequest(key, ChangeRequest.CLOSE, 0, null);
        synchronized (this) {
            _todos.add(todo);
        }
        _selector.wakeup();
    }

    /* Release the resource used by the instance */
    public void cleanUp() throws IOException {
        if (_selector != null) {
            _selector.close();
        }
    }

    public class ChangeRequest {
        public static final int REGISTER = 1;
        public static final int CHANGEOPS = 2;
        public static final int CLOSE = 3;

        public Object key;
        public int type;
        public int ops;
        public Object att;

        public ChangeRequest(Object key, int type, int ops, Object att) {
            this.key = key;
            this.type = type;
            this.ops = ops;
            this.att = att;
        }
    }
}
