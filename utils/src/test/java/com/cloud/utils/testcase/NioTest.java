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

package com.cloud.utils.testcase;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.NioConnectionException;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioClient;
import com.cloud.utils.nio.NioServer;
import com.cloud.utils.nio.Task;
import com.cloud.utils.nio.Task.Type;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NioTest demonstrates that NioServer can function without getting its main IO
 * loop blocked when an aggressive or malicious client connects to the server but
 * fail to participate in SSL handshake. In this test, we run bunch of clients
 * that send a known payload to the server, to which multiple malicious clients
 * also try to connect and hang.
 * A malicious client could cause denial-of-service if the server's main IO loop
 * along with SSL handshake was blocking. A passing tests shows that NioServer
 * can still function in case of connection load and that the main IO loop along
 * with SSL handshake is non-blocking with some internal timeout mechanism.
 */

public class NioTest {

    protected Logger logger = LogManager.getLogger(NioTest.class);

    // Test should fail in due time instead of looping forever
    private static final int TESTTIMEOUT = 60000;

    final private int totalTestCount = 2;
    private int completedTestCount = 0;

    private NioServer server;
    private List<NioClient> clients = new ArrayList<>();
    private List<NioClient> maliciousClients = new ArrayList<>();

    private ExecutorService clientExecutor = Executors.newFixedThreadPool(totalTestCount, new NamedThreadFactory("NioClientHandler"));;
    private ExecutorService maliciousExecutor = Executors.newFixedThreadPool(totalTestCount, new NamedThreadFactory("MaliciousNioClientHandler"));;

    private Random randomGenerator = new Random();
    private byte[] testBytes;

    private boolean isTestsDone() {
        boolean result;
        synchronized (this) {
            result = totalTestCount == completedTestCount;
        }
        return result;
    }

    private void oneMoreTestDone() {
        synchronized (this) {
            completedTestCount++;
        }
    }

    @Before
    public void setUp() {
        logger.info("Setting up Benchmark Test");

        completedTestCount = 0;
        testBytes = new byte[1000000];
        randomGenerator.nextBytes(testBytes);

        server = new NioServer("NioTestServer", 0, 1, new NioTestServer(), null);
        try {
            server.start();
        } catch (final NioConnectionException e) {
            Assert.fail(e.getMessage());
        }

        for (int i = 0; i < totalTestCount; i++) {
            final NioClient maliciousClient = new NioMaliciousClient("NioMaliciousTestClient-" + i, "127.0.0.1", server.getPort(), 1, new NioMaliciousTestClient());
            maliciousClients.add(maliciousClient);
            maliciousExecutor.submit(new ThreadedNioClient(maliciousClient));

            final NioClient client = new NioClient("NioTestClient-" + i, "127.0.0.1", server.getPort(), 1, new NioTestClient());
            clients.add(client);
            clientExecutor.submit(new ThreadedNioClient(client));
        }
    }

    @After
    public void tearDown() {
        stopClient();
        stopServer();
    }

    protected void stopClient() {
        for (NioClient client : clients) {
            client.stop();
        }
        for (NioClient maliciousClient : maliciousClients) {
            maliciousClient.stop();
        }
        logger.info("Clients stopped.");
    }

    protected void stopServer() {
        server.stop();
        logger.info("Server stopped.");
    }

    @Test(timeout=TESTTIMEOUT)
    public void testConnection() {
        while (!isTestsDone()) {
            try {
                logger.debug(completedTestCount + "/" + totalTestCount + " tests done. Waiting for completion");
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }
        logger.debug(completedTestCount + "/" + totalTestCount + " tests done.");
    }

    protected void doServerProcess(final byte[] data) {
        oneMoreTestDone();
        Assert.assertArrayEquals(testBytes, data);
        logger.info("Verify data received by server done.");
    }

    public byte[] getTestBytes() {
        return testBytes;
    }

    public class ThreadedNioClient implements Runnable {
        final private NioClient client;
        ThreadedNioClient(final NioClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                client.start();
            } catch (NioConnectionException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    public class NioMaliciousClient extends NioClient {

        public NioMaliciousClient(String name, String host, int port, int workers, HandlerFactory factory) {
            super(name, host, port, workers, factory);
        }

        @Override
        protected void init() throws IOException {
            _selector = Selector.open();
            try {
                _clientConnection = SocketChannel.open();
                logger.info("Connecting to " + _host + ":" + _port);
                final InetSocketAddress peerAddr = new InetSocketAddress(_host, _port);
                _clientConnection.connect(peerAddr);
                // This is done on purpose, the malicious client would connect
                // to the server and then do nothing, hence using a large sleep value
                Thread.sleep(Long.MAX_VALUE);
            } catch (final IOException e) {
                _selector.close();
                throw e;
            } catch (InterruptedException e) {
                logger.debug(e.getMessage());
            }
        }
    }

    public class NioMaliciousTestClient implements HandlerFactory {

        @Override
        public Task create(final Type type, final Link link, final byte[] data) {
            return new NioMaliciousTestClientHandler(type, link, data);
        }

        public class NioMaliciousTestClientHandler extends Task {

            public NioMaliciousTestClientHandler(final Type type, final Link link, final byte[] data) {
                super(type, link, data);
            }

            @Override
            public void doTask(final Task task) {
                logger.info("Malicious Client: Received task " + task.getType().toString());
            }
        }
    }

    public class NioTestClient implements HandlerFactory {

        @Override
        public Task create(final Type type, final Link link, final byte[] data) {
            return new NioTestClientHandler(type, link, data);
        }

        public class NioTestClientHandler extends Task {

            public NioTestClientHandler(final Type type, final Link link, final byte[] data) {
                super(type, link, data);
            }

            @Override
            public void doTask(final Task task) {
                if (task.getType() == Task.Type.CONNECT) {
                    logger.info("Client: Received CONNECT task");
                    try {
                        logger.info("Sending data to server");
                        task.getLink().send(getTestBytes());
                    } catch (ClosedChannelException e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                } else if (task.getType() == Task.Type.DATA) {
                    logger.info("Client: Received DATA task");
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    logger.info("Client: Received DISCONNECT task");
                    stopClient();
                } else if (task.getType() == Task.Type.OTHER) {
                    logger.info("Client: Received OTHER task");
                }
            }
        }
    }

    public class NioTestServer implements HandlerFactory {

        @Override
        public Task create(final Type type, final Link link, final byte[] data) {
            return new NioTestServerHandler(type, link, data);
        }

        public class NioTestServerHandler extends Task {

            public NioTestServerHandler(final Type type, final Link link, final byte[] data) {
                super(type, link, data);
            }

            @Override
            public void doTask(final Task task) {
                if (task.getType() == Task.Type.CONNECT) {
                    logger.info("Server: Received CONNECT task");
                } else if (task.getType() == Task.Type.DATA) {
                    logger.info("Server: Received DATA task");
                    doServerProcess(task.getData());
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    logger.info("Server: Received DISCONNECT task");
                    stopServer();
                } else if (task.getType() == Task.Type.OTHER) {
                    logger.info("Server: Received OTHER task");
                }
            }
        }
    }
}
