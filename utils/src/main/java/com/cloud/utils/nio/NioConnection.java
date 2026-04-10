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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.apache.cloudstack.framework.ca.CAService;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.NioConnectionException;

/**
 * NioConnection abstracts the NIO socket operations.  The Java implementation
 * provides that.
 */
public abstract class NioConnection implements Callable<Boolean> {
    protected Logger logger = LogManager.getLogger(getClass());
    public static final String SERVER_BUSY_MESSAGE = "Server is busy.";

    protected Selector _selector;
    protected ExecutorService _threadExecutor;
    protected Future<Boolean> _futureTask;

    protected volatile boolean _isRunning;
    protected volatile boolean _isStartup;
    protected int _port;
    protected int _workers;
    protected List<ChangeRequest> _todos;
    protected HandlerFactory _factory;
    protected String _name;
    protected ExecutorService _executor;
    protected ExecutorService _sslHandshakeExecutor;
    protected CAService caService;
    protected Set<SocketChannel> socketChannels;
    protected Integer sslHandshakeTimeout;
    private final int factoryMaxNewConnectionsCount;
    protected volatile boolean blockNewConnections;

    public NioConnection(final String name, final int port, final int workers, final HandlerFactory factory) {
        socketChannels = ConcurrentHashMap.newKeySet();
        _name = name;
        _port = port;
        _workers = workers;
        _factory = factory;
        this.factoryMaxNewConnectionsCount = factory.getMaxConcurrentNewConnectionsCount();
    }

    public void setCAService(CAService caService) {
        this.caService = caService;
    }

    public void start() throws NioConnectionException {
        _todos = new ArrayList<>();

        if (_executor == null || _executor.isShutdown() || _executor.isTerminated()) {
            initWorkersExecutor();
        }

        if (_sslHandshakeExecutor == null || _sslHandshakeExecutor.isShutdown() || _sslHandshakeExecutor.isTerminated()) {
            initSSLHandshakeExecutor();
        }

        try {
            init();
        } catch (final ConnectException e) {
            String msg = String.format("Unable to connect to remote: is there a server running on port %s: %s", _port, e.getMessage());
            throw new NioConnectionException(msg, e);
        } catch (final Exception e) {
            String msg = String.format("Unable to initialize the threads: %s", e.getMessage());
            throw new NioConnectionException(msg, e);
        }
        _isStartup = true;

        _threadExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory(this._name + "-NioConnectionHandler"));
        _isRunning = true;
        // in case start() called after stop()
        blockNewConnections = false;
        _futureTask = _threadExecutor.submit(this);
    }

    public void stop() {
        if (_executor != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Shutting down handler tasks");
            }
            _executor.shutdownNow();
        }
        if (_sslHandshakeExecutor != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Shutting down SSL Handshake executor");
            }
            _sslHandshakeExecutor.shutdownNow();
        }
        if (_threadExecutor != null) {
            _futureTask.cancel(false);
            _threadExecutor.shutdownNow();
        }
        _isRunning = false;
        blockNewConnections = true;
    }

    private void initWorkersExecutor() {
        _executor = new ThreadPoolExecutor(_workers, 5 * _workers, 1, TimeUnit.DAYS,
                new LinkedBlockingQueue<>(5 * _workers), new NamedThreadFactory(_name + "-Handler"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private void initSSLHandshakeExecutor() {
        String sslHandshakeHandlerName = _name + "-SSLHandshakeHandler";
        if (factoryMaxNewConnectionsCount > 0) {
            _sslHandshakeExecutor = new ThreadPoolExecutor(0, this.factoryMaxNewConnectionsCount, 30,
                    TimeUnit.MINUTES, new SynchronousQueue<>(), new NamedThreadFactory(sslHandshakeHandlerName),
                    new ThreadPoolExecutor.AbortPolicy());
        } else {
            _sslHandshakeExecutor = Executors.newCachedThreadPool(new NamedThreadFactory(sslHandshakeHandlerName));
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
                final Set<SelectionKey> keys = _selector.selectedKeys();

                // Walk through the ready keys collection.
                for (final Iterator<SelectionKey> it = keys.iterator(); it.hasNext();) {
                    final SelectionKey key = it.next();
                    it.remove();

                    // isValid() means key is not cancelled and channel is not closed
                    if (!key.isValid()) {
                        logger.trace("Selection Key is invalid: {}", key);
                        final Link link = (Link)key.attachment();
                        if (link != null) {
                            link.terminated();
                        } else {
                            closeConnection(key);
                        }
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                        // this is for NIO Server only to accept new connection
                    } else if (key.isAcceptable()) {
                        accept(key);
                        // this is for NIO Client only to finish connect
                    } else if (key.isConnectable()) {
                        connect(key);
                    }
                }

                processTodos();
                // and we cannot use existing selector anymore
            } catch (final ClosedSelectorException e) {
                /*
                 * Exception occurred when calling java.nio.channels.Selector.selectedKeys() method.
                 * It means the connection has not yet been established. Let's continue trying.
                 * We do not log it here otherwise we will fill the disk with messages.
                 */
            } catch (final Exception e) {
                _isStartup = false;
                logger.error("Agent will die due to this Exception", e);
                throw new NioConnectionException(e.getMessage(), e);
            }
        }
        _isStartup = false;
        return true;
    }

    protected abstract void init() throws IOException;

    abstract void registerLink(InetSocketAddress saddr, Link link);

    abstract void unregisterLink(InetSocketAddress saddr);

    protected boolean rejectConnectionIfBlocked(final SocketChannel socketChannel) throws IOException {
        if (!blockNewConnections) {
            return false;
        }

        try {
            socketChannel.shutdownOutput();
        } catch (IOException ignore) {
            // ignore exception
        }
        try {
            socketChannel.shutdownInput();
        } catch (IOException ignore) {
            // ignore exception
        }
        logger.warn("Rejecting new connection from {} as the server is blocked from accepting new connections", socketChannel.getRemoteAddress());
        socketChannel.close();
        _selector.wakeup();
        return true;
    }

    protected boolean rejectConnectionIfBusy(final SocketChannel socketChannel) throws IOException {
        if (factoryMaxNewConnectionsCount <= 0  || _factory.getNewConnectionsCount() < factoryMaxNewConnectionsCount) {
            return false;
        }
        try {
            socketChannel.shutdownOutput();
        } catch (IOException ignore) {
            // ignore exception
        }
        try {
            socketChannel.shutdownInput();
        } catch (IOException ignore) {
            // ignore exception
        }
        // Reject new connection if the server is busy
        logger.warn("{} Rejecting new connection from {}. {} active connections currently",
                SERVER_BUSY_MESSAGE, socketChannel.getRemoteAddress(), factoryMaxNewConnectionsCount);
        socketChannel.close();
        _selector.wakeup();
        return true;
    }

    protected void accept(final SelectionKey key) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        final SocketChannel socketChannel = serverSocketChannel.accept();
        if (rejectConnectionIfBlocked(socketChannel) || rejectConnectionIfBusy(socketChannel)) {
            return;
        }
        socketChannel.configureBlocking(false);

        final Socket socket = socketChannel.socket();
        socket.setKeepAlive(true);

        logger.trace("Connection accepted for {}", socket);

        try {
            final NioConnection nioConnection = this;
            _sslHandshakeExecutor.submit(() -> {
                final InetSocketAddress socketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Registering new connection for {}", socketAddress.getAddress());
                    }
                    _factory.registerNewConnection(socketAddress);
                    _selector.wakeup();
                    final SSLEngine sslEngine = Link.initServerSSLEngine(caService, socketChannel.getRemoteAddress().toString());
                    sslEngine.setUseClientMode(false);
                    sslEngine.setEnabledProtocols(SSLUtils.getSupportedProtocols(sslEngine.getEnabledProtocols()));
                    sslEngine.beginHandshake();
                    if (!Link.doHandshake(socketChannel, sslEngine, getSslHandshakeTimeout())) {
                        throw new IOException("SSL handshake timed out with " + socketAddress);
                    }
                    logger.trace("SSL Handshake done for {}", socketAddress);
                    final Link link = new Link(socketAddress, nioConnection);
                    link.setSSLEngine(sslEngine);
                    link.setKey(socketChannel.register(key.selector(), SelectionKey.OP_READ, link));
                    final Task task = _factory.create(Task.Type.CONNECT, link, null);
                    registerLink(socketAddress, link);
                    _executor.submit(task);
                } catch (final GeneralSecurityException | IOException | RuntimeException e) {
                    _factory.unregisterNewConnection(socketAddress);
                    logger.trace("Connection closed with {} due to failure: {}", socket.getRemoteSocketAddress(), e.getMessage(), e);
                    closeAutoCloseable(socket, "accepting socket");
                    closeAutoCloseable(socketChannel, "accepting socketChannel");
                } finally {
                    try {
                        _selector.wakeup();
                    } catch (final Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failed to wakeup selector for {}", socketAddress, e);
                        }
                    }
                }
            });
        } catch (final RejectedExecutionException e) {
            logger.trace("{} Accept Task rejected: {}", socket.getRemoteSocketAddress(), e.getMessage());
            closeAutoCloseable(socket, "Rejecting connection - accepting socket");
            closeAutoCloseable(socketChannel, "Rejecting connection - accepting socketChannel");
        } finally {
            try {
                _selector.wakeup();
            } catch (final Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to wakeup selector for {}", socket, e);
                }
            }
        }
    }

    protected void terminate(final SelectionKey key, final Exception cause) {
        final Link link = (Link)key.attachment();
        closeConnection(key);
        if (link != null) {
            logger.trace("Will terminate connection to {} due to: {}", link.getSocketAddress(), cause.getLocalizedMessage(), cause);
            link.terminated();
            final Task task = _factory.create(Task.Type.DISCONNECT, link, null);
            unregisterLink(link.getSocketAddress());
            _factory.unregisterNewConnection(link.getSocketAddress());
            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task for {}", link.getSocketAddress(), e);
            }
        }
    }

    protected void read(final SelectionKey key) throws IOException {
        final Link link = (Link)key.attachment();
        try {
            final SocketChannel socketChannel = (SocketChannel)key.channel();
            logger.trace("Reading from: {}", socketChannel.socket());
            final byte[] data = link.read(socketChannel);
            if (data == null) {
                logger.trace("Packet is incomplete. Waiting for more.");
                return;
            }
            final Task task = _factory.create(Task.Type.DATA, link, data);

            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task for {}", link.getSocketAddress(), e);
            }
        } catch (final Exception e) {
            logDebug(e, key, 1);
            terminate(key, e);
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

            if (logger.isTraceEnabled()) {
                logger.trace("Location {}: Socket {} closed on read.  Probably -1 returned.", loc, socket, e);
            }
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

            logger.debug("Location " + loc + ": Socket " + socket + " closed on read.  Probably -1 returned: " + e.getMessage(), e);
        }
    }

    protected void processTodos() {
        List<ChangeRequest> todos;
        if (_todos.isEmpty()) {
            return;             // Nothing to do.
        }

        synchronized (this) {
            todos = _todos;
            _todos = new ArrayList<>();
        }

        logger.trace("Todos Processing: {}", todos.size());

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
                        logger.debug("key has been cancelled", e);
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
                        logger.warn("Couldn't register socket: {}", todo.key);
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
                    logger.trace("Trying to close {}", todo.key);
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
            logger.debug("Connected to {}", socket);
            final Link link = new Link((InetSocketAddress)socket.getRemoteSocketAddress(), this);
            link.setKey(key);
            key.attach(link);
            final Task task = _factory.create(Task.Type.CONNECT, link, null);

            try {
                _executor.submit(task);
            } catch (final Exception e) {
                logger.warn("Exception occurred when submitting the task for connect: {}", socket, e);
            }
        } catch (final IOException e) {
            logTrace(e, key, 2);
            terminate(key, e);
        }
    }

    protected void scheduleTask(final Task task) {
        try {
            _executor.submit(task);
        } catch (final Exception e) {
            InetSocketAddress socketAddress = Optional.ofNullable(task)
                    .map(Task::getLink)
                    .map(Link::getSocketAddress)
                    .orElse(null);
            logger.warn("Exception occurred when submitting the task for {}", socketAddress, e);
        }
    }

    protected void write(final SelectionKey key) throws IOException {
        final Link link = (Link)key.attachment();
        try {
            logger.trace("Writing to {}", link.getSocketAddress());
            final boolean close = link.write((SocketChannel)key.channel());
            if (close) {
                closeConnection(key);
                link.terminated();
            } else {
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (final Exception e) {
            logDebug(e, key, 3);
            terminate(key, e);
        }
    }

    protected void closeConnection(final SelectionKey key) {
        if (key == null) {
            return;
        }

        SocketChannel channel = null;
        try {
            // 1. Check type and handle potential CancelledKeyException
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                channel = (SocketChannel) key.channel();
            }
        } catch (CancelledKeyException e) {
            logger.trace("Key already cancelled when trying to get channel in closeConnection.");
        }

        // 2. Cancel the key (safe to call even if already cancelled)
        key.cancel();

        // 3. Try to close the channel if we obtained it
        if (channel != null) {
            closeChannel(channel);
        } else {
            logger.trace("Channel was null, invalid, or not a SocketChannel for key: {}", key);
        }
    }

    private void closeChannel(SocketChannel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                logger.debug("Closing socket " + channel.socket());
                channel.close();
            } catch (IOException ignore) {
                logger.warn(String.format("[ignored] Exception closing channel: %s, due to %s", channel, ignore.getMessage()));
            } catch (Exception e) {
                logger.warn(String.format("Unexpected exception in closing channel %s", channel), e);
            } finally {
                socketChannels.remove(channel);
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
        for (SocketChannel channel : socketChannels) {
            closeChannel(channel);
        }
        if (_selector != null) {
            _selector.close();
        }
    }

    public void block() {
        blockNewConnections = true;
    }

    public void unblock() {
        blockNewConnections = false;
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

    public Integer getSslHandshakeTimeout() {
        return sslHandshakeTimeout;
    }

    public void setSslHandshakeTimeout(Integer sslHandshakeTimeout) {
        this.sslHandshakeTimeout = sslHandshakeTimeout;
    }
}
