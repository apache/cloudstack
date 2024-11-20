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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.apache.cloudstack.framework.ca.CAService;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.NioConnectionException;

/**
 * NioConnection abstracts the NIO socket operations.  The Java implementation
 * provides that.
 */
public abstract class NioConnection implements Callable<Boolean> {
    protected Logger logger = LogManager.getLogger(getClass());

    protected Selector _selector;
    protected ExecutorService _threadExecutor;
    protected Future<Boolean> _futureTask;

    protected boolean _isRunning;
    protected boolean _isStartup;
    protected int _port;
    protected List<ChangeRequest> _todos;
    protected HandlerFactory _factory;
    protected String _name;
    protected ExecutorService _executor;
    protected ExecutorService _sslHandshakeExecutor;
    protected CAService caService;

    public NioConnection(final String name, final int port, final int workers, final HandlerFactory factory) {
        _name = name;
        _isRunning = false;
        _selector = null;
        _port = port;
        _factory = factory;
        _executor = new ThreadPoolExecutor(workers, 5 * workers, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(name + "-Handler"));
        _sslHandshakeExecutor = Executors.newCachedThreadPool(new NamedThreadFactory(name + "-SSLHandshakeHandler"));
    }

    public void setCAService(final CAService caService) {
        this.caService = caService;
    }

    public void start() throws NioConnectionException {
        _todos = new ArrayList<ChangeRequest>();

        try {
            init();
        } catch (final ConnectException e) {
            logger.warn("Unable to connect to remote: is there a server running on port " + _port);
            return;
        } catch (final IOException e) {
            logger.error("Unable to initialize the threads.", e);
            throw new NioConnectionException(e.getMessage(), e);
        } catch (final Exception e) {
            logger.error("Unable to initialize the threads due to unknown exception.", e);
            throw new NioConnectionException(e.getMessage(), e);
        }
        _isStartup = true;

        _threadExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory(this._name + "-NioConnectionHandler"));
        _isRunning = true;
        _futureTask = _threadExecutor.submit(this);
    }

    public void stop() {
        _executor.shutdown();
        _isRunning = false;
        if (_threadExecutor != null) {
            _futureTask.cancel(false);
            _threadExecutor.shutdown();
        }
    }

    public boolean isRunning() {
        return !_futureTask.isDone();
    }

    public boolean isStartup() {
        return _isStartup;
    }

    @Override
    public Boolean call() throws NioConnectionException {
        while (_isRunning) {
            try {
                _selector.select(50);

                // Someone is ready for I/O, get the ready keys
                final Set<SelectionKey> readyKeys = _selector.selectedKeys();
                final Iterator<SelectionKey> i = readyKeys.iterator();

                if (logger.isTraceEnabled()) {
                    logger.trace("Keys Processing: " + readyKeys.size());
                }
                // Walk through the ready keys collection.
                while (i.hasNext()) {
                    final SelectionKey sk = i.next();
                    i.remove();

                    if (!sk.isValid()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Selection Key is invalid: " + sk.toString());
                        }
                        final Link link = (Link)sk.attachment();
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

                logger.trace("Keys Done Processing.");

                processTodos();
            } catch (final ClosedSelectorException e) {
                /*
                 * Exception occurred when calling java.nio.channels.Selector.selectedKeys() method. It means the connection has not yet been established. Let's continue trying
                 * We do not log it here otherwise we will fill the disk with messages.
                 */
            } catch (final IOException e) {
                logger.error("Agent will die due to this IOException!", e);
                throw new NioConnectionException(e.getMessage(), e);
            }
        }
        _isStartup = false;
        return true;
    }

    abstract void init() throws IOException;

    abstract void registerLink(InetSocketAddress saddr, Link link);

    abstract void unregisterLink(InetSocketAddress saddr);

    protected void accept(final SelectionKey key) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        final SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        final Socket socket = socketChannel.socket();
        socket.setKeepAlive(true);

        if (logger.isTraceEnabled()) {
            logger.trace("Connection accepted for " + socket);
        }

        final SSLEngine sslEngine;
        try {
            sslEngine = Link.initServerSSLEngine(caService, socketChannel.getRemoteAddress().toString());
            sslEngine.setUseClientMode(false);
            sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));
            final NioConnection nioConnection = this;
            _sslHandshakeExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    _selector.wakeup();
                    try {
                        sslEngine.beginHandshake();
                        if (!Link.doHandshake(socketChannel, sslEngine)) {
                            throw new IOException("SSL handshake timed out with " + socketChannel.getRemoteAddress());
                        }
                        if (logger.isTraceEnabled()) {
                            logger.trace("SSL: Handshake done");
                        }
                        final InetSocketAddress saddr = (InetSocketAddress)socket.getRemoteSocketAddress();
                        final Link link = new Link(saddr, nioConnection);
                        link.setSSLEngine(sslEngine);
                        link.setKey(socketChannel.register(key.selector(), SelectionKey.OP_READ, link));
                        final Task task = _factory.create(Task.Type.CONNECT, link, null);
                        registerLink(saddr, link);
                        _executor.submit(task);
                    } catch (IOException e) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Connection closed due to failure: " + e.getMessage());
                        }
                        closeAutoCloseable(socket, "accepting socket");
                        closeAutoCloseable(socketChannel, "accepting socketChannel");
                    } finally {
                        _selector.wakeup();
                    }
                }
            });
        } catch (final Exception e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Connection closed due to failure: " + e.getMessage());
            }
            closeAutoCloseable(socket, "accepting socket");
            closeAutoCloseable(socketChannel, "accepting socketChannel");
        } finally {
            _selector.wakeup();
        }
    }

    protected void terminate(final SelectionKey key) {
        final Link link = (Link)key.attachment();
        closeConnection(key);
        if (link != null) {
            link.terminated();
            final Task task = _factory.create(Task.Type.DISCONNECT, link, null);
            unregisterLink(link.getSocketAddress());

            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task", e);
            }
        }
    }

    protected void read(final SelectionKey key) throws IOException {
        final Link link = (Link)key.attachment();
        try {
            final SocketChannel socketChannel = (SocketChannel)key.channel();
            if (logger.isTraceEnabled()) {
                logger.trace("Reading from: " + socketChannel.socket().toString());
            }
            final byte[] data = link.read(socketChannel);
            if (data == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Packet is incomplete.  Waiting for more.");
                }
                return;
            }
            final Task task = _factory.create(Task.Type.DATA, link, data);

            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task", e);
            }
        } catch (final Exception e) {
            logDebug(e, key, 1);
            terminate(key);
        }
    }

    protected void logTrace(final Exception e, final SelectionKey key, final int loc) {
        if (logger.isTraceEnabled()) {
            Socket socket = null;
            if (key != null) {
                final SocketChannel ch = (SocketChannel)key.channel();
                if (ch != null) {
                    socket = ch.socket();
                }
            }

            logger.trace("Location " + loc + ": Socket " + socket + " closed on read.  Probably -1 returned.");
        }
    }

    protected void logDebug(final Exception e, final SelectionKey key, final int loc) {
        if (logger.isDebugEnabled()) {
            Socket socket = null;
            if (key != null) {
                final SocketChannel ch = (SocketChannel)key.channel();
                if (ch != null) {
                    socket = ch.socket();
                }
            }

            logger.debug("Location " + loc + ": Socket " + socket + " closed on read.  Probably -1 returned: " + e.getMessage());
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

        if (logger.isTraceEnabled()) {
            logger.trace("Todos Processing: " + todos.size());
        }
        SelectionKey key;
        for (final ChangeRequest todo : todos) {
            switch (todo.type) {
            case ChangeRequest.CHANGEOPS:
                try {
                    key = (SelectionKey)todo.key;
                    if (key != null && key.isValid()) {
                        if (todo.att != null) {
                            key.attach(todo.att);
                            final Link link = (Link)todo.att;
                            link.setKey(key);
                        }
                        key.interestOps(todo.ops);
                    }
                } catch (final CancelledKeyException e) {
                    logger.debug("key has been cancelled");
                }
                break;
            case ChangeRequest.REGISTER:
                try {
                    key = ((SocketChannel)todo.key).register(_selector, todo.ops, todo.att);
                    if (todo.att != null) {
                        final Link link = (Link)todo.att;
                        link.setKey(key);
                    }
                } catch (final ClosedChannelException e) {
                    logger.warn("Couldn't register socket: " + todo.key);
                    try {
                        ((SocketChannel)todo.key).close();
                    } catch (final IOException ignore) {
                        logger.info("[ignored] socket channel");
                    } finally {
                        final Link link = (Link)todo.att;
                        link.terminated();
                    }
                }
                break;
            case ChangeRequest.CLOSE:
                if (logger.isTraceEnabled()) {
                    logger.trace("Trying to close " + todo.key);
                }
                key = (SelectionKey)todo.key;
                closeConnection(key);
                if (key != null) {
                    final Link link = (Link)key.attachment();
                    if (link != null) {
                        link.terminated();
                    }
                }
                break;
            default:
                logger.warn("Shouldn't be here");
                throw new RuntimeException("Shouldn't be here");
            }
        }
        logger.trace("Todos Done processing");
    }

    protected void connect(final SelectionKey key) throws IOException {
        final SocketChannel socketChannel = (SocketChannel)key.channel();

        try {
            socketChannel.finishConnect();
            key.interestOps(SelectionKey.OP_READ);
            final Socket socket = socketChannel.socket();
            if (!socket.getKeepAlive()) {
                socket.setKeepAlive(true);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Connected to " + socket);
            }
            final Link link = new Link((InetSocketAddress)socket.getRemoteSocketAddress(), this);
            link.setKey(key);
            key.attach(link);
            final Task task = _factory.create(Task.Type.CONNECT, link, null);

            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task", e);
            }
        } catch (final IOException e) {
            logTrace(e, key, 2);
            terminate(key);
        }
    }

    protected void scheduleTask(final Task task) {
        try {
            _executor.submit(task);
        } catch (final Exception e) {
            logger.warn("Exception occurred when submitting the task", e);
        }
    }

    protected void write(final SelectionKey key) throws IOException {
        final Link link = (Link)key.attachment();
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Writing to " + link.getSocketAddress().toString());
            }
            final boolean close = link.write((SocketChannel)key.channel());
            if (close) {
                closeConnection(key);
                link.terminated();
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (final Exception e) {
            logDebug(e, key, 3);
            terminate(key);
        }
    }

    protected void closeConnection(final SelectionKey key) {
        if (key != null) {
            final SocketChannel channel = (SocketChannel)key.channel();
            key.cancel();
            try {
                if (channel != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Closing socket " + channel.socket());
                    }
                    channel.close();
                }
            } catch (final IOException ignore) {
                logger.info("[ignored] channel");
            }
        }
    }

    public void register(final int ops, final SocketChannel key, final Object att) {
        final ChangeRequest todo = new ChangeRequest(key, ChangeRequest.REGISTER, ops, att);
        synchronized (this) {
            _todos.add(todo);
        }
        _selector.wakeup();
    }

    public void change(final int ops, final SelectionKey key, final Object att) {
        final ChangeRequest todo = new ChangeRequest(key, ChangeRequest.CHANGEOPS, ops, att);
        synchronized (this) {
            _todos.add(todo);
        }
        _selector.wakeup();
    }

    public void close(final SelectionKey key) {
        final ChangeRequest todo = new ChangeRequest(key, ChangeRequest.CLOSE, 0, null);
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

        public ChangeRequest(final Object key, final int type, final int ops, final Object att) {
            this.key = key;
            this.type = type;
            this.ops = ops;
            this.att = att;
        }
    }
}
