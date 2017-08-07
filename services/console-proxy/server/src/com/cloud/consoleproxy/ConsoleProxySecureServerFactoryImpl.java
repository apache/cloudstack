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
package com.cloud.consoleproxy;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;

public class ConsoleProxySecureServerFactoryImpl implements ConsoleProxyServerFactory {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxySecureServerFactoryImpl.class);
    private static final int httpPort = 80;
    private SSLContext sslContext = null;
    private SslContextFactory sslContextFactory;

    public ConsoleProxySecureServerFactoryImpl() {
    }

    @Override
    public void init(byte[] ksBits, String ksPassword) {
        s_logger.info("Start initializing SSL");

        if (ksBits == null) {
            // this should not be the case
            s_logger.info("No certificates passed, recheck global configuration and certificates");
        } else {
            char[] passphrase = ksPassword != null ? ksPassword.toCharArray() : null;
            try {
                s_logger.info("Initializing SSL from passed-in certificate");

                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new ByteArrayInputStream(ksBits), passphrase);
                sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStore(ks);
                sslContextFactory.setKeyStorePassword(String.valueOf(passphrase));
                sslContextFactory.setKeyManagerPassword(String.valueOf(passphrase));
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, passphrase);
                s_logger.info("Key manager factory is initialized");

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);
                s_logger.info("Trust manager factory is initialized");

                sslContext = SSLUtils.getSSLContext();
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                s_logger.info("SSL context is initialized");
            } catch (Exception e) {
                s_logger.error("Unable to init factory due to exception ", e);
            }
        }

    }

    @Override
    public Server createHttpServerInstance(int port) throws IOException {
        Server server = new Server(port);
        // HTTP Configuration
        HttpConfiguration http = new HttpConfiguration();
        http.addCustomizer(new SecureRequestCustomizer());

        // Configuration for HTTPS redirect
        http.setSecurePort(port);
        http.setSecureScheme("https");
        ServerConnector connector = new ServerConnector(server);
        connector.addConnectionFactory(new HttpConnectionFactory(http));
        // Setting HTTP port
        connector.setPort(httpPort);

        // HTTPS configuration
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        // Configuring the connector
        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
        sslConnector.setPort(port);

        // Setting HTTP and HTTPS connectors
        server.setConnectors(new Connector[]{connector, sslConnector});
        return server;
    }

    @Override
    public SSLServerSocket createSSLServerSocket(int port) throws IOException {
        try {
            SSLServerSocket srvSock = null;
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            srvSock = (SSLServerSocket)ssf.createServerSocket(port);
            srvSock.setEnabledProtocols(SSLUtils.getSupportedProtocols(srvSock.getEnabledProtocols()));
            srvSock.setEnabledCipherSuites(SSLUtils.getSupportedProtocols(srvSock.getEnabledCipherSuites()));
            s_logger.info("create SSL server socket on port: " + port);
            return srvSock;
        } catch (Exception ioe) {
            s_logger.error(ioe.toString(), ioe);
        }
        return null;
    }
}
