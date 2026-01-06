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
package com.cloud.cluster;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.cloudstack.framework.ca.CAService;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.StringUtils;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.nio.Link;

public class ClusterServiceServletContainer {

    private ListenerThread listenerThread;

    public ClusterServiceServletContainer() {
    }

    public boolean start(HttpRequestHandler requestHandler, String ip, int port, CAService caService) {

        listenerThread = new ListenerThread(requestHandler, ip, port, caService);
        listenerThread.start();

        return true;
    }

    public void stop() {
        if (listenerThread != null) {
            listenerThread.stopRunning();
        }
    }


    protected static SSLServerSocket getSecuredServerSocket(SSLContext sslContext, String ip, int port)
            throws IOException {
        SSLServerSocketFactory sslFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = null;
        if (StringUtils.isNotEmpty(ip)) {
            serverSocket = (SSLServerSocket) sslFactory.createServerSocket(port, 0,
                    InetAddress.getByName(ip));
        } else {
            serverSocket = (SSLServerSocket) sslFactory.createServerSocket(port);
        }
        serverSocket.setNeedClientAuth(true);
        return serverSocket;
    }

    static class ListenerThread extends Thread {

        private static Logger LOGGER = LogManager.getLogger(ListenerThread.class);
        private HttpService httpService = null;
        private volatile SSLServerSocket serverSocket = null;
        private HttpParams params = null;
        private ExecutorService executor;
        private CAService caService = null;

        public ListenerThread(HttpRequestHandler requestHandler, String ip, int port,
                      CAService caService) {
            this.executor = Executors.newCachedThreadPool(new NamedThreadFactory("Cluster-Listener"));
            this.caService = caService;

            try {
                SSLContext sslContext = Link.initManagementSSLContext(caService);
                serverSocket = getSecuredServerSocket(sslContext, ip, port);
            } catch (IOException | GeneralSecurityException e) {
                LOGGER.error("Error initializing cluster service servlet container for secure connection",
                        e);
                return;
            }

            params = new BasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("/clusterservice", requestHandler);

            // Set up the HTTP service
            httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            httpService.setParams(params);
            httpService.setHandlerResolver(reqistry);
        }

        public void stopRunning() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LOGGER.info("[ignored] error on closing server socket", e);
                }
                serverSocket = null;
            }
        }

        protected boolean isValidPeerConnection(Socket socket) throws SSLPeerUnverifiedException,
                CertificateParsingException {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLSession session = sslSocket.getSession();
            if (session == null || !session.isValid()) {
                return false;
            }
            Certificate[] certs = session.getPeerCertificates();
            if (certs == null || certs.length < 1) {
                return false;
            }
            return caService.isManagementCertificate(certs[0]);
        }

        @Override
        public void run() {
            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("Cluster service servlet container listening on host: %s and port %d",
                        serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort()));

            while (serverSocket != null) {
                try {
                    // Set up HTTP connection
                    Socket socket = serverSocket.accept();
                    final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, params);
                    if (!isValidPeerConnection(socket)) {
                        LOGGER.warn(String.format("Failure during validating cluster request from %s",
                                socket.getInetAddress().getHostAddress()));
                        conn.shutdown();
                        continue;
                    }
                    executor.execute(new ManagedContextRunnable() {
                        @Override
                        protected void runInContext() {
                            HttpContext context = new BasicHttpContext(null);
                            try {
                                while (!Thread.interrupted() && conn.isOpen()) {
                                    if (LOGGER.isTraceEnabled())
                                        LOGGER.trace("dispatching cluster request from " + conn.getRemoteAddress().toString());

                                    httpService.handleRequest(conn, context);

                                    if (LOGGER.isTraceEnabled())
                                        LOGGER.trace("Cluster request from " + conn.getRemoteAddress().toString() + " is processed");
                                }
                            } catch (ConnectionClosedException ex) {
                                // client close and read time out exceptions are expected
                                // when KEEP-AVLIE is enabled
                                LOGGER.trace("Client closed connection", ex);
                            } catch (IOException ex) {
                                LOGGER.trace("I/O error", ex);
                            } catch (HttpException ex) {
                                LOGGER.error("Unrecoverable HTTP protocol violation", ex);
                            } finally {
                                try {
                                    conn.shutdown();
                                } catch (IOException ignore) {
                                    LOGGER.error("unexpected exception", ignore);
                                }
                            }
                        }
                    });

                } catch (Throwable e) {
                    LOGGER.error("Unexpected exception ", e);

                    // back off to avoid spinning if the exception condition keeps coming back
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        LOGGER.debug("[ignored] interrupted while waiting to retry running the servlet container.");
                    }
                }
            }

            executor.shutdown();
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Cluster service servlet container shutdown");
        }
    }
}
