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
import org.apache.log4j.Logger;
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

public class NioTest {

    private static final Logger LOGGER = Logger.getLogger(NioTest.class);

    final private int totalTestCount = 10;
    private int completedTestCount = 0;

    private NioServer server;
    private List<NioClient> clients = new ArrayList<>();
    private List<NioClient> maliciousClients = new ArrayList<>();

    private ExecutorService clientExecutor = Executors.newFixedThreadPool(totalTestCount, new NamedThreadFactory("NioClientHandler"));;
    private ExecutorService maliciousExecutor = Executors.newFixedThreadPool(5*totalTestCount, new NamedThreadFactory("MaliciousNioClientHandler"));;

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
        LOGGER.info("Setting up Benchmark Test");

        completedTestCount = 0;
        testBytes = new byte[1000000];
        randomGenerator.nextBytes(testBytes);

        // Server configured with one worker
        server = new NioServer("NioTestServer", 7777, 1, new NioTestServer());
        try {
            server.start();
        } catch (final NioConnectionException e) {
            Assert.fail(e.getMessage());
        }

        // 5 malicious clients per valid client
        for (int i = 0; i < totalTestCount; i++) {
            for (int j = 0; j < 5; j++) {
                final NioClient maliciousClient = new NioMaliciousClient("NioMaliciousTestClient-" + i, "127.0.0.1", 7777, 1, new NioMaliciousTestClient());
                maliciousClients.add(maliciousClient);
                maliciousExecutor.submit(new ThreadedNioClient(maliciousClient));
            }
            final NioClient client = new NioClient("NioTestClient-" + i, "127.0.0.1", 7777, 1, new NioTestClient());
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
        LOGGER.info("Clients stopped.");
    }

    protected void stopServer() {
        server.stop();
        LOGGER.info("Server stopped.");
    }

    @Test
    public void testConnection() {
        final long currentTime = System.currentTimeMillis();
        while (!isTestsDone()) {
            if (System.currentTimeMillis() - currentTime > 600000) {
                Assert.fail("Failed to complete test within 600s");
            }
            try {
                LOGGER.debug(completedTestCount + "/" + totalTestCount + " tests done. Waiting for completion");
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }
        LOGGER.debug(completedTestCount + "/" + totalTestCount + " tests done.");
    }

    protected void doServerProcess(final byte[] data) {
        oneMoreTestDone();
        Assert.assertArrayEquals(testBytes, data);
        LOGGER.info("Verify data received by server done.");
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
                LOGGER.info("Connecting to " + _host + ":" + _port);
                final InetSocketAddress peerAddr = new InetSocketAddress(_host, _port);
                _clientConnection.connect(peerAddr);
                // Hang in there don't do anything
                Thread.sleep(3600000);
            } catch (final IOException e) {
                _selector.close();
                throw e;
            } catch (InterruptedException e) {
                LOGGER.debug(e.getMessage());
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
                LOGGER.info("Malicious Client: Received task " + task.getType().toString());
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
                    LOGGER.info("Client: Received CONNECT task");
                    try {
                        LOGGER.info("Sending data to server");
                        task.getLink().send(getTestBytes());
                    } catch (ClosedChannelException e) {
                        LOGGER.error(e.getMessage());
                        e.printStackTrace();
                    }
                } else if (task.getType() == Task.Type.DATA) {
                    LOGGER.info("Client: Received DATA task");
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    LOGGER.info("Client: Received DISCONNECT task");
                    stopClient();
                } else if (task.getType() == Task.Type.OTHER) {
                    LOGGER.info("Client: Received OTHER task");
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
                    LOGGER.info("Server: Received CONNECT task");
                } else if (task.getType() == Task.Type.DATA) {
                    LOGGER.info("Server: Received DATA task");
                    doServerProcess(task.getData());
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    LOGGER.info("Server: Received DISCONNECT task");
                    stopServer();
                } else if (task.getType() == Task.Type.OTHER) {
                    LOGGER.info("Server: Received OTHER task");
                }
            }

        }
    }
}
